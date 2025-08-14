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
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_CYCLING_CADENCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_HEART_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_POWER;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SKIN_TEMPERATURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SPEED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_STEPS_CADENCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_UNKNOWN;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_POWER;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import android.health.HealthFitnessStatsLog;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DataGranularityStatsLoggerTest {
    private static final String TEST_PACKAGE = "test.package";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Mock private DataGranularityStatsCollector mDataGranularityStatsCollector;

    private DataGranularityStatsLogger mDataGranularityStatsLogger;

    @Before
    public void setUp() throws Exception {
        openMocks(this).close();
        mDataGranularityStatsLogger =
                new DataGranularityStatsLogger(
                        mHealthFitnessStatsLog, mDataGranularityStatsCollector);
    }

    @After
    public void tearDown() {
        Mockito.clearInvocations(mHealthFitnessStatsLog);
    }

    @Test
    @EnableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void logGranularityStats_flagEnabled_logged() {
        when(mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats())
                .thenReturn(getGranularityStats());

        mDataGranularityStatsLogger.logGranularityStats();

        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                        TEST_PACKAGE,
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_HEART_RATE,
                        /* granularity= */ 1000L);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                        TEST_PACKAGE,
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SPEED,
                        /* granularity= */ 2000L);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                        TEST_PACKAGE,
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_POWER,
                        /* granularity= */ 3000L);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                        TEST_PACKAGE,
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_STEPS_CADENCE,
                        /* granularity= */ 4000L);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                        TEST_PACKAGE,
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_CYCLING_CADENCE,
                        /* granularity= */ 5000L);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                        TEST_PACKAGE,
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_SKIN_TEMPERATURE,
                        /* granularity= */ 6000L);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS,
                        TEST_PACKAGE,
                        HEALTH_CONNECT_DATA_GRANULARITY_STATS__GRANULARITY_DATA_TYPE__GRANULARITY_DATA_TYPE_UNKNOWN,
                        /* granularity= */ 7000L);
    }

    @Test
    @DisableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void logGranularityStats_flagDisabled_noOp() {
        when(mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats())
                .thenReturn(getGranularityStats());

        mDataGranularityStatsLogger.logGranularityStats();

        verify(mHealthFitnessStatsLog, never())
                .write(eq(HEALTH_CONNECT_DATA_GRANULARITY_STATS), anyString(), anyInt(), anyLong());
    }

    @NonNull
    private static List<DataGranularityStatsCollector.GranularityStats> getGranularityStats() {
        List<DataGranularityStatsCollector.GranularityStats> stats = new ArrayList<>();
        stats.add(
                new DataGranularityStatsCollector.GranularityStats(
                        TEST_PACKAGE, RECORD_TYPE_HEART_RATE, /* granularity= */ 1000));
        stats.add(
                new DataGranularityStatsCollector.GranularityStats(
                        TEST_PACKAGE, RECORD_TYPE_SPEED, /* granularity= */ 2000));
        stats.add(
                new DataGranularityStatsCollector.GranularityStats(
                        TEST_PACKAGE, RECORD_TYPE_POWER, /* granularity= */ 3000));
        stats.add(
                new DataGranularityStatsCollector.GranularityStats(
                        TEST_PACKAGE, RECORD_TYPE_STEPS_CADENCE, /* granularity= */ 4000));
        stats.add(
                new DataGranularityStatsCollector.GranularityStats(
                        TEST_PACKAGE,
                        RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                        /* granularity= */ 5000));
        stats.add(
                new DataGranularityStatsCollector.GranularityStats(
                        TEST_PACKAGE, RECORD_TYPE_SKIN_TEMPERATURE, /* granularity= */ 6000));
        stats.add(
                new DataGranularityStatsCollector.GranularityStats(
                        TEST_PACKAGE,
                        RECORD_TYPE_STEPS,
                        /* granularity= */ 7000)); // This should map to UNKNOWN
        return stats;
    }
}
