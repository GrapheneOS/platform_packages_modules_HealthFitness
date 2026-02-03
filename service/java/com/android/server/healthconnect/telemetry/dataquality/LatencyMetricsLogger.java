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

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_EXERCISE;

import android.health.HealthFitnessStatsLog;
import android.health.connect.device.SyntheticPackageNameMatcher;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.telemetry.dataquality.LatencyMetricsCollector.LatencyMetricsData;

import java.util.List;

/**
 * Logs Health Connect latency stats.
 *
 * @hide
 */
public final class LatencyMetricsLogger {

    private final HealthFitnessStatsLog mStatsLog;
    private final LatencyMetricsCollector mLatencyMetricsCollector;

    public LatencyMetricsLogger(
            HealthFitnessStatsLog statsLog, LatencyMetricsCollector latencyMetricsCollector) {
        mStatsLog = statsLog;
        mLatencyMetricsCollector = latencyMetricsCollector;
    }

    /** Write Health Connect latency stats to statsd. */
    public void log() {
        if (!Flags.latencyMetricsFlag()) {
            return;
        }
        List<LatencyMetricsData> exerciseSessionLatencyMetrics =
                mLatencyMetricsCollector.readLastWeekExerciseSessions();
        List<LatencyMetricsData> sleepSessionLatencyMetrics =
                mLatencyMetricsCollector.readLastWeekSleepSessions();
        logLatency(
                HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_EXERCISE,
                exerciseSessionLatencyMetrics);
        logLatency(
                HealthFitnessStatsLog
                        .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_SLEEP,
                sleepSessionLatencyMetrics);
    }

    private void logLatency(
            int recordTypeForLogging, List<LatencyMetricsData> latencyMetricsDataList) {
        for (LatencyMetricsData latencyMetricsData : latencyMetricsDataList) {
            mStatsLog.write(
                    HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS,
                    SyntheticPackageNameMatcher.replaceAllCanonicalIn(
                            latencyMetricsData.packageName()),
                    recordTypeForLogging,
                    latencyMetricsData.latency().toMillis());
        }
    }
}
