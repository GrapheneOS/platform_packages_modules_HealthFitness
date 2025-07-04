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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.health.HealthFitnessStatsLog;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WeeklyLoggingServiceTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Mock private LatencyMetricsCollector mLatencyMetricsCollector;
    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;

    private HealthConnectInjector mHealthConnectInjector;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mHealthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setEnvironmentDataDirectory(mTemporaryFolder.getRoot())
                        .setHealthFitnessStatsLog(mHealthFitnessStatsLog)
                        .setLatencyMetricsCollector(mLatencyMetricsCollector)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .build();
    }

    @Test
    public void testWeeklyLoggingService_logsLatencyMetrics() {
        WeeklyLoggingService.logWeeklyMetrics(mHealthConnectInjector.getLatencyMetricsLogger());

        verify(mLatencyMetricsCollector, times(1)).readLastWeekExerciseSessions();
        verify(mLatencyMetricsCollector, times(1)).readLastWeekSleepSessions();
    }

    @Test
    public void testWeeklyLoggingService_exceptionCaught() {
        doThrow(new RuntimeException("Test exception"))
                .when(mLatencyMetricsCollector)
                .readLastWeekExerciseSessions();

        WeeklyLoggingService.logWeeklyMetrics(mHealthConnectInjector.getLatencyMetricsLogger());

        verify(mLatencyMetricsCollector, times(1)).readLastWeekExerciseSessions();
        verify(mLatencyMetricsCollector, never()).readLastWeekSleepSessions();
    }
}
