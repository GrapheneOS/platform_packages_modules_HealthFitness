/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.healthconnect.storage;

import static android.health.connect.Constants.DEFAULT_INT;

import android.annotation.UserIdInt;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Slog;

import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A service that is run periodically to handle deletion of stale entries in HC DB.
 *
 * @hide
 */
public class AutoDeleteService extends JobService {
    private static final int MIN_JOB_ID = AutoDeleteService.class.hashCode();
    private static final String AUTO_DELETE_DURATION_RECORDS_KEY =
            "auto_delete_duration_records_key";
    private static final long JOB_RUN_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private static final String TAG = "HealthConnectAutoDelete";
    private static final String EXTRA_USER_ID = "user_id";

    /** Start periodically scheduling this service for this {@code userId} */
    public static void schedule(Context context, @UserIdInt int userId) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        Objects.requireNonNull(jobScheduler);
        ComponentName componentName = new ComponentName(context, AutoDeleteService.class);
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userId);
        JobInfo.Builder builder =
                new JobInfo.Builder(MIN_JOB_ID + userId, componentName)
                        .setExtras(extras)
                        .setRequiresDeviceIdle(true)
                        .setRequiresCharging(true)
                        .setPeriodic(JOB_RUN_INTERVAL, JOB_RUN_INTERVAL / 2);

        int result = jobScheduler.schedule(builder.build());
        if (result != JobScheduler.RESULT_SUCCESS) {
            Slog.e(TAG, "Failed to schedule daily job");
        }
    }

    /** Stop periodically scheduling this service for this {@code userId} */
    public static void stop(Context context, int userId) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        Objects.requireNonNull(jobScheduler).cancel(MIN_JOB_ID + userId);
    }

    /** Sets auto delete period for automatically deleting record entries */
    public static void setRecordRetentionPeriodInDays(int days, @UserIdInt int userId) {
        PreferenceHelper.getInstance()
                .insertPreference(AUTO_DELETE_DURATION_RECORDS_KEY + userId, String.valueOf(days));
    }

    /** Gets auto delete period for automatically deleting record entries */
    public static int getRecordRetentionPeriodInDays(@UserIdInt int userId) {
        String result =
                PreferenceHelper.getInstance()
                        .getPreference(AUTO_DELETE_DURATION_RECORDS_KEY + userId);

        if (result == null) return 0;
        return Integer.parseInt(result);
    }

    /** Called everytime when the operation corresponding to this service is to be performed */
    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            int userId = params.getExtras().getInt(EXTRA_USER_ID, /*defaultValue=*/ DEFAULT_INT);
            if (userId == DEFAULT_INT) {
                return false;
            }

            // Only do transactional operations here - as this job might get cancelled for
            // several reasons, such as: User switch, low battery etc.
            String recordAutoDeletePeriodString =
                    PreferenceHelper.getInstance()
                            .getPreference(AUTO_DELETE_DURATION_RECORDS_KEY + userId);
            int recordAutoDeletePeriod =
                    recordAutoDeletePeriodString == null
                            ? 0
                            : Integer.parseInt(recordAutoDeletePeriodString);
            try {
                TransactionManager.getInitialisedInstance()
                        .deleteStaleRecordEntries(recordAutoDeletePeriod);
            } catch (Exception exception) {
                Slog.e(TAG, "Auto delete for records failed", exception);
                // Don't rethrow as that will crash system_server
            }

            try {
                TransactionManager.getInitialisedInstance().deleteStaleChangeLogEntries();
            } catch (Exception exception) {
                Slog.e(TAG, "Auto delete for change logs failed", exception);
                // Don't rethrow as that will crash system_server
            }

            jobFinished(params, false);
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "Auto delete run failed", e);
            // Don't rethrow as that will crash system_server
            return false;
        }
    }

    /**
     * Called when job needs to be stopped. Don't do anything here and let the job be killed and
     * since we do everything in a transaction, let the transaction fail.
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
