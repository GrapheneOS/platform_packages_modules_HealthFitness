/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.telemetry.dataquality;

import static com.android.server.healthconnect.telemetry.TelemetryJobService.EXTRA_USER_ID;

import static java.util.Objects.requireNonNull;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.telemetry.TelemetryJobService;

import java.util.concurrent.TimeUnit;

/**
 * A job scheduled to run weekly.
 *
 * @hide
 */
public final class DataQualityTelemetryJobScheduler {
    private static final String TAG = "DataQualityTelemetryJobScheduler";
    private static final int MIN_JOB_ID = DataQualityTelemetryJobScheduler.class.hashCode();
    private static final String HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE =
            "HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE";
    @VisibleForTesting static final long JOB_RUN_INTERVAL = TimeUnit.DAYS.toMillis(7);
    @VisibleForTesting static final long JOB_FLEX_INTERVAL = TimeUnit.DAYS.toMillis(1);

    /** Schedule the weekly job */
    public static void schedule(Context context, UserHandle userHandle) {
        if (!Flags.latencyMetricsFlag()) {
            return;
        }
        TelemetryJobService.setCurrentUser(userHandle);
        JobScheduler jobScheduler =
                requireNonNull(context.getSystemService(JobScheduler.class))
                        .forNamespace(HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE);

        int result = jobScheduler.schedule(getJobInfo(context, userHandle));
        if (result != JobScheduler.RESULT_SUCCESS) {
            Slog.e(TAG, "Failed to schedule the DataQualityTelemetryJob");
        }
    }

    /** Cancel the weekly job */
    public static void cancelAllJobs(Context context) {
        requireNonNull(context.getSystemService(JobScheduler.class))
                .forNamespace(HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE)
                .cancelAll();
    }

    /** Uploads critical weekly metrics. */
    public static void execute(LatencyMetricsLogger latencyMetricsLogger) {
        WeeklyLoggingService.logWeeklyMetrics(latencyMetricsLogger);
    }

    private static JobInfo getJobInfo(Context context, UserHandle userHandle) {
        ComponentName componentName = new ComponentName(context, TelemetryJobService.class);
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userHandle.getIdentifier());
        return new JobInfo.Builder(MIN_JOB_ID + userHandle.getIdentifier(), componentName)
                .setExtras(extras)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setPeriodic(JOB_RUN_INTERVAL, JOB_FLEX_INTERVAL)
                .build();
    }
}
