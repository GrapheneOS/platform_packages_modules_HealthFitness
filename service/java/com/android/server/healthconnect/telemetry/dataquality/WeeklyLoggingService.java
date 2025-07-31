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

import android.util.Slog;

/**
 * Class to log Health Connect metrics logged every week.
 *
 * @hide
 */
public class WeeklyLoggingService {

    private static final String HEALTH_CONNECT_WEEKLY_LOGGING_SERVICE =
            "HealthConnectWeeklyLoggingService";

    /** Log weekly metrics. */
    public static void logWeeklyMetrics(LatencyMetricsLogger latencyMetricsLogger) {
        logLatencyMetrics(latencyMetricsLogger);
    }

    private static void logLatencyMetrics(LatencyMetricsLogger latencyMetricsLogger) {
        try {
            latencyMetricsLogger.log();
        } catch (Exception exception) {
            Slog.e(
                    HEALTH_CONNECT_WEEKLY_LOGGING_SERVICE,
                    "Failed to log latency metrics",
                    exception);
        }
    }
}
