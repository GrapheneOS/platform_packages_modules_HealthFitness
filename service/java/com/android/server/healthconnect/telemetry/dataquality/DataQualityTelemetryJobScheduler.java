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
    @VisibleForTesting
    public static final String HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE =
            "HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE";

    private static final String TAG = "DataQualityTelemetryJobScheduler";
    private static final int MIN_JOB_ID = DataQualityTelemetryJobScheduler.class.hashCode();
    @VisibleForTesting static final long JOB_RUN_INTERVAL = TimeUnit.DAYS.toMillis(7);
    @VisibleForTesting static final long JOB_FLEX_INTERVAL = TimeUnit.DAYS.toMillis(1);

    private final Context mContext;
    private final LatencyMetricsLogger mLatencyMetricsLogger;
    private final CompletenessStatsCollector mCompletenessStatsCollector;
    private final CompletenessStatsLogger mCompletenessStatsLogger;
    private final DataGranularityStatsLogger mDataGranularityStatsLogger;

    public DataQualityTelemetryJobScheduler(
            Context context,
            LatencyMetricsLogger latencyMetricsLogger,
            CompletenessStatsCollector completenessStatsCollector,
            CompletenessStatsLogger completenessStatsLogger,
            DataGranularityStatsLogger dataGranularityStatsLogger) {
        mContext = context;
        mLatencyMetricsLogger = latencyMetricsLogger;
        mCompletenessStatsCollector = completenessStatsCollector;
        mCompletenessStatsLogger = completenessStatsLogger;
        mDataGranularityStatsLogger = dataGranularityStatsLogger;
    }

    /** Schedule the weekly job */
    public void schedule() {
        if (!Flags.latencyMetricsFlag()
                && !Flags.dataCompleteness()
                && !Flags.activeDataGranularity()) {
            return;
        }
        JobScheduler jobScheduler =
                requireNonNull(mContext.getSystemService(JobScheduler.class))
                        .forNamespace(HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE);

        int userId = mContext.getUser().getIdentifier();
        int result = jobScheduler.schedule(getJobInfo(userId));
        if (result != JobScheduler.RESULT_SUCCESS) {
            Slog.e(TAG, "Failed to schedule the DataQualityTelemetryJob");
        }
    }

    /** Cancel the weekly job */
    public void cancelAllJobs() {
        requireNonNull(mContext.getSystemService(JobScheduler.class))
                .forNamespace(HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE)
                .cancelAll();
    }

    /** Uploads critical weekly metrics. */
    public void execute() {
        if (Flags.latencyMetricsFlag()) {
            logLatencyMetrics();
        }
        if (Flags.dataCompleteness()) {
            logCompletenessStats();
        }
        if (Flags.activeDataGranularity()) {
            logGranularityStats();
        }
    }

    private JobInfo getJobInfo(int userId) {
        ComponentName componentName = new ComponentName(mContext, TelemetryJobService.class);
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userId);
        return new JobInfo.Builder(MIN_JOB_ID + userId, componentName)
                .setExtras(extras)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setPeriodic(JOB_RUN_INTERVAL, JOB_FLEX_INTERVAL)
                .build();
    }

    private void logLatencyMetrics() {
        try {
            mLatencyMetricsLogger.log();
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to log latency metrics", exception);
        }
    }

    private void logCompletenessStats() {
        try {
            mCompletenessStatsLogger.logRecordingMethodStats(
                    mCompletenessStatsCollector.readRecordingMethodStats());
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to log recording method stats", exception);
        }
        try {
            mCompletenessStatsLogger.logDeviceInfoStats(
                    mCompletenessStatsCollector.readDeviceInfoStats());
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to log device info stats", exception);
        }
    }

    private void logGranularityStats() {
        try {
            mDataGranularityStatsLogger.logGranularityStats();
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to log active data granularity stats", exception);
        }
    }
}
