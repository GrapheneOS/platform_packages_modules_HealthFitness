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

package com.android.server.healthconnect.common.logging;

import android.annotation.SuppressLint;
import android.health.HealthFitnessStatsLog;
import android.health.connect.datatypes.RecordTypeIdentifier;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.logging.LatencyMetricsCollector.LatencyMetricsData;
import com.android.server.healthconnect.common.logging.LatencyMetricsCollector.LatencyMetricsPerRecord;

import java.util.List;

/**
 * Logs Health Connect latency stats.
 *
 * @hide
 */
final class LatencyMetricsLogger {

    private final HealthFitnessStatsLog mStatsLog;

    LatencyMetricsLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    /** Write Health Connect latency stats to statsd. */
    void log(LatencyMetricsCollector latencyMetricsCollector) {
        if (!Flags.latencyMetricsFlag()) {
            return;
        }
        LatencyMetricsData exerciseSessionLatencyMetrics =
                latencyMetricsCollector.readLastWeekExerciseSessions();
        LatencyMetricsData sleepSessionLatencyMetrics =
                latencyMetricsCollector.readLastWeekSleepSessions();
        logLatency(
                getProtoRecordType(exerciseSessionLatencyMetrics.recordType()),
                exerciseSessionLatencyMetrics.latencyMetricsForEachRecord());
        logLatency(
                getProtoRecordType(sleepSessionLatencyMetrics.recordType()),
                sleepSessionLatencyMetrics.latencyMetricsForEachRecord());
    }

    private void logLatency(
            int recordTypeForLogging, List<LatencyMetricsPerRecord> latencyMetricsForEachRecord) {
        for (LatencyMetricsPerRecord latencyMetricsPerRecord : latencyMetricsForEachRecord) {
            mStatsLog.write(
                    HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS,
                    latencyMetricsPerRecord.packageName(),
                    recordTypeForLogging,
                    latencyMetricsPerRecord.latency().toMillis());
        }
    }

    @SuppressLint("SwitchIntDef")
    private int getProtoRecordType(@RecordTypeIdentifier.RecordType int recordType) {
        return switch (recordType) {
            case RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION ->
                    HealthFitnessStatsLog
                            .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_EXERCISE;
            case RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION ->
                    HealthFitnessStatsLog
                            .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_SLEEP;
            default ->
                    HealthFitnessStatsLog
                            .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_UNKNOWN;
        };
    }
}
