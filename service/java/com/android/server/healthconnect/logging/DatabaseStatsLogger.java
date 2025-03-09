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

package com.android.server.healthconnect.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_STORAGE_STATS;

import static com.android.healthfitness.flags.Flags.personalHealthRecordTelemetry;

import android.health.HealthFitnessStatsLog;

import com.android.server.healthconnect.storage.datatypehelpers.DatabaseStatsCollector;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;

import java.util.Set;

/**
 * Class to log Health Connect database stats.
 *
 * @hide
 */
class DatabaseStatsLogger {
    private final HealthFitnessStatsLog mStatsLog;

    DatabaseStatsLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    /** Write Health Connect database stats to statsd. */
    void log(
            DatabaseStatsCollector databaseStatsCollector,
            UsageStatsCollector usageStatsCollector) {
        logGeneralDatabaseStats(databaseStatsCollector);
        logPhrDatabaseStats(databaseStatsCollector, usageStatsCollector);
    }

    private void logGeneralDatabaseStats(DatabaseStatsCollector databaseStatsCollector) {
        long numberOfInstantRecords = databaseStatsCollector.getNumberOfInstantRecordRows();
        long numberOfIntervalRecords = databaseStatsCollector.getNumberOfIntervalRecordRows();
        long numberOfSeriesRecords = databaseStatsCollector.getNumberOfSeriesRecordRows();
        long numberOfChangeLogs = databaseStatsCollector.getNumberOfChangeLogs();

        // If this condition is true then the user does not uses HC and we should not collect data.
        // This will reduce the load on logging service otherwise we will get daily data from
        // billions of Android devices.
        if (numberOfInstantRecords == 0
                && numberOfIntervalRecords == 0
                && numberOfSeriesRecords == 0
                && numberOfChangeLogs == 0) {
            return;
        }

        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_STORAGE_STATS,
                databaseStatsCollector.getDatabaseSize(),
                numberOfInstantRecords,
                numberOfIntervalRecords,
                numberOfSeriesRecords,
                numberOfChangeLogs);
    }

    /** Writes PHR database stats to statsd. */
    private void logPhrDatabaseStats(
            DatabaseStatsCollector databaseStatsCollector,
            UsageStatsCollector usageStatsCollector) {
        if (!personalHealthRecordTelemetry()
                || (usageStatsCollector.getMedicalResourcesCount() == 0
                        && usageStatsCollector.getMedicalDataSourcesCount() == 0)) {
            return;
        }

        Long phrDbSizeLong =
                databaseStatsCollector.getFileBytes(
                        Set.of(
                                MedicalDataSourceHelper.getMainTableName(),
                                MedicalResourceHelper.getMainTableName(),
                                MedicalResourceIndicesHelper.getTableName()));
        if (phrDbSizeLong != null) {
            mStatsLog.write(HEALTH_CONNECT_PHR_STORAGE_STATS, phrDbSizeLong);
        }
    }
}
