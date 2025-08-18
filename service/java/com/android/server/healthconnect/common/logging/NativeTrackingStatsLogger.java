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

import android.health.HealthFitnessStatsLog;
import android.os.SystemClock;

/**
 * Class to log Health Connect native tracking metrics.
 *
 * @hide
 */
public class NativeTrackingStatsLogger {
    private final HealthFitnessStatsLog mStatsLog;
    private final NativeTrackingStatsCollector mCollector;

    NativeTrackingStatsLogger(
            HealthFitnessStatsLog statsLog, NativeTrackingStatsCollector collector) {
        mStatsLog = statsLog;
        mCollector = collector;
    }

    /** Write Health Connect native tracking stats to statsd. */
    void log() {
        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED,
                mCollector.getNativeDataTypesActive(),
                mCollector.getNativeDataTypesDisabled(),
                mCollector.getNumberOfWrites(),
                // We record seconds since boot for lower storage footprint.
                (int) SystemClock.elapsedRealtime() / 1000,
                mCollector.getLastErrorCode(),
                mCollector.getStepsReadersCount(),
                mCollector.getStepsWritersCount());
    }
}
