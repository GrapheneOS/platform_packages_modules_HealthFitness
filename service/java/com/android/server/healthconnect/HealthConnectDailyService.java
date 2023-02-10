/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect;

import static android.health.connect.Constants.DEFAULT_INT;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Slog;

import com.android.server.healthconnect.logging.DailyLoggingService;
import com.android.server.healthconnect.storage.AutoDeleteService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A service that is run periodically and triggers other periodic tasks..
 *
 * @hide
 */
public class HealthConnectDailyService extends JobService {

    private static final int MIN_JOB_ID = HealthConnectDailyService.class.hashCode();
    private static final long JOB_RUN_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private static final String TAG = "HealthConnectDailyService";
    public static final String EXTRA_USER_ID = "user_id";
    @UserIdInt private static int sCurrentUserId;

    /** Start periodically scheduling this service for {@code userId}. */
    public static void schedule(@NonNull Context context, @UserIdInt int userId) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        Objects.requireNonNull(jobScheduler);
        ComponentName componentName = new ComponentName(context, HealthConnectDailyService.class);
        final PersistableBundle extras = new PersistableBundle();
        sCurrentUserId = userId;
        extras.putInt(EXTRA_USER_ID, sCurrentUserId);
        JobInfo.Builder builder =
                new JobInfo.Builder(MIN_JOB_ID + sCurrentUserId, componentName)
                        .setExtras(extras)
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .setPeriodic(JOB_RUN_INTERVAL, JOB_RUN_INTERVAL / 2);

        int result = jobScheduler.schedule(builder.build());
        if (result != JobScheduler.RESULT_SUCCESS) {
            Slog.e(TAG, "Failed to schedule daily job");
        }
    }

    /** Stop periodically scheduling this service for this {@code userId} */
    public static void stop(@NonNull Context context, @NonNull @UserIdInt int userId) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        Objects.requireNonNull(jobScheduler).cancel(MIN_JOB_ID + userId);
    }

    /**
     * Called everytime when the operation corresponding to this service is to be performed,
     *
     * <p>Please handle exceptions for each task within the task. Do not crash the job as it might
     * result in failure of other tasks being triggered from the job.
     */
    @Override
    public boolean onStartJob(@NonNull JobParameters params) {
        int userId = params.getExtras().getInt(EXTRA_USER_ID, /* defaultValue= */ DEFAULT_INT);
        if (userId == DEFAULT_INT || userId != sCurrentUserId) {
            // This job is no longer valid, the service should have been stopped. Just ignore
            // this request in case we still got the request.
            return false;
        }

        // This service executes each incoming job on a Handler running on the application's
        // main thread. This means that we must offload the execution logic to background executor.
        HealthConnectThreadScheduler.scheduleInternalTask(
                () -> {
                    AutoDeleteService.startAutoDelete();
                    DailyLoggingService.logDailyMetrics(
                            getApplicationContext(), UserHandle.getUserHandleForUid(userId));
                    jobFinished(params, false);
                });

        return true;
    }

    /** Called when job needs to be stopped. Don't do anything here and let the job be killed. */
    @Override
    public boolean onStopJob(@NonNull JobParameters params) {
        return false;
    }
}
