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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.health.HealthFitnessStatsLog;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class NativeTrackingStatsLoggerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private NativeTrackingStatsCollector mNativeTrackingStatsCollector;
    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;

    @Test
    public void testLogsStats() {
        when(mNativeTrackingStatsCollector.getNativeDataTypesActive()).thenReturn(new int[] {1});
        when(mNativeTrackingStatsCollector.getNativeDataTypesDisabled()).thenReturn(new int[] {2});
        when(mNativeTrackingStatsCollector.getNumberOfWrites()).thenReturn(3);
        when(mNativeTrackingStatsCollector.getLastErrorCode()).thenReturn(4);
        when(mNativeTrackingStatsCollector.getStepsReadersCount()).thenReturn(5);
        when(mNativeTrackingStatsCollector.getStepsWritersCount()).thenReturn(6);

        NativeTrackingStatsLogger logger =
                new NativeTrackingStatsLogger(
                        mHealthFitnessStatsLog, mNativeTrackingStatsCollector);
        logger.log();

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED),
                        eq(new int[] {1}),
                        eq(new int[] {2}),
                        eq(3),
                        anyInt(),
                        eq(4),
                        eq(5),
                        eq(6));
    }
}
