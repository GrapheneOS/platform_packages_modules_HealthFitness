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

import static com.android.healthfitness.flags.Flags.FLAG_LATENCY_METRICS_FLAG;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.health.HealthFitnessStatsLog;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.telemetry.dataquality.LatencyMetricsCollector.LatencyMetricsData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class LatencyMetricsLoggerTest {
    private static final String TEST_SPN_PACKAGE_CANONICAL =
            "com.android.healthconnect.phone.d59341472a9253c16b986840a324ec594";
    private static final String TEST_SPN_PACKAGE = "com.android.healthconnect.phone";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthFitnessStatsLog mStatsLog;
    @Mock private LatencyMetricsCollector mLatencyMetricsCollector;

    private LatencyMetricsLogger mLatencyMetricsLogger;

    @Before
    public void setUp() {
        mLatencyMetricsLogger = new LatencyMetricsLogger(mStatsLog, mLatencyMetricsCollector);
    }

    @Test
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void testLog_flagOff_doesNothing() {
        mLatencyMetricsLogger.log();
        verify(mStatsLog, never())
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS),
                        eq(null),
                        anyInt(),
                        anyLong());
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void testLog_flagOn_logsLatencyMetrics() {
        when(mLatencyMetricsCollector.readLastWeekExerciseSessions())
                .thenReturn(
                        List.of(
                                new LatencyMetricsData("package.a", Duration.ofMillis(10)),
                                new LatencyMetricsData("package.b", Duration.ofMillis(20))));
        when(mLatencyMetricsCollector.readLastWeekSleepSessions())
                .thenReturn(List.of(new LatencyMetricsData("package.c", Duration.ofMillis(30))));

        mLatencyMetricsLogger.log();

        verify(mStatsLog, times(1))
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS),
                        eq("package.a"),
                        eq(
                                HealthFitnessStatsLog
                                        .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_EXERCISE),
                        eq(10L));

        verify(mStatsLog)
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS),
                        eq("package.b"),
                        eq(
                                HealthFitnessStatsLog
                                        .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_EXERCISE),
                        eq(20L));

        verify(mStatsLog)
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS),
                        eq("package.c"),
                        eq(
                                HealthFitnessStatsLog
                                        .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_SLEEP),
                        eq(30L));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_LATENCY_METRICS_FLAG,
    })
    public void logsLatencyMetrics_logsCanonicalSpn() {
        when(mLatencyMetricsCollector.readLastWeekExerciseSessions())
                .thenReturn(
                        List.of(
                                new LatencyMetricsData(
                                        TEST_SPN_PACKAGE_CANONICAL, Duration.ofMillis(10))));
        when(mLatencyMetricsCollector.readLastWeekSleepSessions())
                .thenReturn(
                        List.of(
                                new LatencyMetricsData(
                                        TEST_SPN_PACKAGE_CANONICAL, Duration.ofMillis(30))));

        mLatencyMetricsLogger.log();

        verify(mStatsLog, times(1))
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS),
                        eq(TEST_SPN_PACKAGE),
                        eq(
                                HealthFitnessStatsLog
                                        .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_EXERCISE),
                        eq(10L));

        verify(mStatsLog)
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS),
                        eq(TEST_SPN_PACKAGE),
                        eq(
                                HealthFitnessStatsLog
                                        .HEALTH_CONNECT_LATENCY_STATS__SESSION_DATA_TYPE__SESSION_DATA_TYPE_SLEEP),
                        eq(30L));
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void testLog_noLatencyMetrics_doesNothing() {
        when(mLatencyMetricsCollector.readLastWeekExerciseSessions()).thenReturn(List.of());
        when(mLatencyMetricsCollector.readLastWeekSleepSessions()).thenReturn(List.of());

        mLatencyMetricsLogger.log();

        verify(mStatsLog, never())
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_LATENCY_STATS),
                        anyString(),
                        anyInt(),
                        anyLong());
    }
}
