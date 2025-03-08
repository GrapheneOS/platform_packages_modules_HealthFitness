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

import android.health.HealthFitnessStatsLog;
import android.util.Slog;

import com.android.server.healthconnect.storage.datatypehelpers.DatabaseStatsCollector;

/**
 * Class to log Health Connect metrics logged every 24hrs.
 *
 * @hide
 */
public class DailyLoggingService {

    private static final String HEALTH_CONNECT_DAILY_LOGGING_SERVICE =
            "HealthConnectDailyLoggingService";

    /** Log daily metrics. */
    public static void logDailyMetrics(
            UsageStatsCollector usageStatsCollector,
            DatabaseStatsCollector databaseStatsCollector,
            EcosystemStatsCollector ecosystemStatsCollector,
            HealthFitnessStatsLog statsLog) {
        UsageStatsLogger usageStatsLogger = new UsageStatsLogger(statsLog);
        DatabaseStatsLogger databaseStatsLogger = new DatabaseStatsLogger(statsLog);
        EcosystemStatsLogger ecosystemStatsLogger = new EcosystemStatsLogger(statsLog);
        logDatabaseStats(databaseStatsCollector, usageStatsCollector, databaseStatsLogger);
        logUsageStats(usageStatsCollector, usageStatsLogger);
        logEcosystemStats(ecosystemStatsCollector, ecosystemStatsLogger);
    }

    private static void logDatabaseStats(
            DatabaseStatsCollector databaseStatsCollector,
            UsageStatsCollector usageStatsCollector,
            DatabaseStatsLogger databaseStatsLogger) {
        try {
            databaseStatsLogger.log(databaseStatsCollector, usageStatsCollector);
        } catch (Exception exception) {
            Slog.e(HEALTH_CONNECT_DAILY_LOGGING_SERVICE, "Failed to log database stats", exception);
        }
    }

    private static void logUsageStats(
            UsageStatsCollector usageStatsCollector, UsageStatsLogger usageStatsLogger) {
        try {
            usageStatsLogger.log(usageStatsCollector);
        } catch (Exception exception) {
            Slog.e(HEALTH_CONNECT_DAILY_LOGGING_SERVICE, "Failed to log usage stats", exception);
        }
    }

    private static void logEcosystemStats(
            EcosystemStatsCollector ecosystemStatsCollector,
            EcosystemStatsLogger ecosystemStatsLogger) {
        try {
            ecosystemStatsLogger.log(ecosystemStatsCollector);
        } catch (Exception exception) {
            Slog.e(
                    HEALTH_CONNECT_DAILY_LOGGING_SERVICE,
                    "Failed to log ecosystem stats",
                    exception);
        }
    }
}
