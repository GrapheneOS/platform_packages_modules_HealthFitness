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

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_ACTIVE_CALORIES_BURNED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_CYCLING_CADENCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_DISTANCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_ELEVATION_GAINED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_FLOORS_CLIMBED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_HEART_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_HRV_RMSSD;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_OXYGEN_SATURATION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_POWER;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_RESPIRATORY_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SKIN_TEMPERATURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SPEED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_STEPS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_STEPS_CADENCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_TOTAL_CALORIES_BURNED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_UNKNOWN;

import static com.android.healthfitness.flags.Flags.latencyMetricsFlag;

import android.health.HealthFitnessStatsLog;
import android.health.connect.datatypes.RecordTypeIdentifier;

/**
 * Logs Health Connect data granularity stats.
 *
 * @hide
 */
public final class DataGranularityStatsLogger {

    private final HealthFitnessStatsLog mHealthFitnessStatsLog;
    private final DataGranularityStatsCollector mDataGranularityStatsCollector;

    public DataGranularityStatsLogger(
            HealthFitnessStatsLog healthFitnessStatsLog,
            DataGranularityStatsCollector dataGranularityStatsCollector) {
        mHealthFitnessStatsLog = healthFitnessStatsLog;
        mDataGranularityStatsCollector = dataGranularityStatsCollector;
    }

    void logGranularityStats() {
        if (!latencyMetricsFlag()) {
            return;
        }
        DataGranularityStatsCollector.AllGranularityStats allStats =
                mDataGranularityStatsCollector.getAllGranularityStatsForLastWeek();

        for (DataGranularityStatsCollector.GranularityStats stat : allStats.activeStats()) {
            logGranularityStat(stat.packageName(), stat.recordIdentifier(), stat.granularity());
        }
    }

    private void logGranularityStat(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordTypeId,
            long granularity) {
        mHealthFitnessStatsLog.write(
                HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                packageName,
                mapDataTypeToLoggingEnum(recordTypeId),
                granularity);
    }

    private static int mapDataTypeToLoggingEnum(@RecordTypeIdentifier.RecordType int recordTypeId) {
        switch (recordTypeId) {
            case RecordTypeIdentifier.RECORD_TYPE_HEART_RATE -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_HEART_RATE;
            }
            case RecordTypeIdentifier.RECORD_TYPE_SPEED -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SPEED;
            }
            case RecordTypeIdentifier.RECORD_TYPE_POWER -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_POWER;
            }
            case RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_STEPS_CADENCE;
            }
            case RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_CYCLING_CADENCE;
            }
            case RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SKIN_TEMPERATURE;
            }
            case RecordTypeIdentifier.RECORD_TYPE_STEPS -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_STEPS;
            }
            case RecordTypeIdentifier.RECORD_TYPE_DISTANCE -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_DISTANCE;
            }
            case RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_ELEVATION_GAINED;
            }
            case RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_ACTIVE_CALORIES_BURNED;
            }
            case RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_TOTAL_CALORIES_BURNED;
            }
            case RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_FLOORS_CLIMBED;
            }
            case RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_HRV_RMSSD;
            }
            case RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_OXYGEN_SATURATION;
            }
            case RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_RESPIRATORY_RATE;
            }
            default -> {
                return HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_UNKNOWN;
            }
        }
    }
}
