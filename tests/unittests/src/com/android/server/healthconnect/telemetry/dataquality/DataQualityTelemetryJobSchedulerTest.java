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

import static com.android.healthfitness.flags.Flags.FLAG_DATA_COMPLETENESS;
import static com.android.healthfitness.flags.Flags.FLAG_LATENCY_METRICS_FLAG;
import static com.android.server.healthconnect.telemetry.TelemetryJobService.EXTRA_USER_ID;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityTelemetryJobScheduler.JOB_FLEX_INTERVAL;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityTelemetryJobScheduler.JOB_RUN_INTERVAL;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class DataQualityTelemetryJobSchedulerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final UserHandle USER_HANDLE = UserHandle.of(1);

    @Mock private Context mContext;
    @Mock private JobScheduler mJobScheduler;
    @Mock private LatencyMetricsLogger mLatencyMetricsLogger;
    @Mock private CompletenessStatsCollector mCompletenessStatsCollector;
    @Mock private CompletenessStatsLogger mCompletenessStatsLogger;
    @Captor private ArgumentCaptor<JobInfo> mJobInfoArgumentCaptor;
    private DataQualityTelemetryJobScheduler mDataQualityTelemetryJobScheduler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mContext.getUser()).thenReturn(USER_HANDLE);
        when(mJobScheduler.forNamespace(anyString())).thenReturn(mJobScheduler);

        mDataQualityTelemetryJobScheduler =
                new DataQualityTelemetryJobScheduler(
                        mContext,
                        mLatencyMetricsLogger,
                        mCompletenessStatsCollector,
                        mCompletenessStatsLogger);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    @DisableFlags(FLAG_DATA_COMPLETENESS)
    public void schedule_latencyFlagOn_schedulesJobWithCorrectInfo() {
        mDataQualityTelemetryJobScheduler.schedule();
        verify(mJobScheduler).schedule(mJobInfoArgumentCaptor.capture());
        assertJobInfoIsCorrect(mJobInfoArgumentCaptor.getValue());
    }

    @Test
    @EnableFlags(FLAG_DATA_COMPLETENESS)
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void schedule_completenessFlagOn_schedulesJobWithCorrectInfo() {
        mDataQualityTelemetryJobScheduler.schedule();
        verify(mJobScheduler).schedule(mJobInfoArgumentCaptor.capture());
        assertJobInfoIsCorrect(mJobInfoArgumentCaptor.getValue());
    }

    @Test
    @DisableFlags({FLAG_LATENCY_METRICS_FLAG, FLAG_DATA_COMPLETENESS})
    public void schedule_bothFlagsOff_doesNotScheduleJob() {
        mDataQualityTelemetryJobScheduler.schedule();
        verify(mJobScheduler, never()).schedule(any(JobInfo.class));
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    @DisableFlags(FLAG_DATA_COMPLETENESS)
    public void execute_latencyMetricsEnabled_logsLatencyMetrics() {
        mDataQualityTelemetryJobScheduler.execute();
        verify(mLatencyMetricsLogger).log();
        verify(mCompletenessStatsLogger, never()).logRecordingMethodStats(anyList());
        verify(mCompletenessStatsLogger, never()).logDeviceInfoStats(anySet());
    }

    @Test
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    @EnableFlags(FLAG_DATA_COMPLETENESS)
    public void execute_dataCompletenessEnabled_logsCompletenessStats() {
        when(mCompletenessStatsCollector.readRecordingMethodStats())
                .thenReturn(Collections.emptyList());
        when(mCompletenessStatsCollector.readDeviceInfoStats()).thenReturn(Collections.emptySet());

        mDataQualityTelemetryJobScheduler.execute();

        verify(mLatencyMetricsLogger, never()).log();
        verify(mCompletenessStatsLogger).logRecordingMethodStats(Collections.emptyList());
        verify(mCompletenessStatsLogger).logDeviceInfoStats(Collections.emptySet());
    }

    @Test
    @EnableFlags({FLAG_LATENCY_METRICS_FLAG, FLAG_DATA_COMPLETENESS})
    public void execute_bothFlagsEnabled_logsBoth() {
        when(mCompletenessStatsCollector.readRecordingMethodStats())
                .thenReturn(Collections.emptyList());
        when(mCompletenessStatsCollector.readDeviceInfoStats()).thenReturn(Collections.emptySet());

        mDataQualityTelemetryJobScheduler.execute();

        verify(mLatencyMetricsLogger).log();
        verify(mCompletenessStatsLogger).logRecordingMethodStats(Collections.emptyList());
        verify(mCompletenessStatsLogger).logDeviceInfoStats(Collections.emptySet());
    }

    @Test
    @DisableFlags({FLAG_LATENCY_METRICS_FLAG, FLAG_DATA_COMPLETENESS})
    public void execute_bothFlagsDisabled_logsNothing() {
        mDataQualityTelemetryJobScheduler.execute();

        verify(mLatencyMetricsLogger, never()).log();
        verify(mCompletenessStatsLogger, never()).logRecordingMethodStats(anyList());
        verify(mCompletenessStatsLogger, never()).logDeviceInfoStats(anySet());
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    @DisableFlags(FLAG_DATA_COMPLETENESS)
    public void execute_latencyMetricException_exceptionCaught() {
        doThrow(new RuntimeException("Test exception")).when(mLatencyMetricsLogger).log();

        mDataQualityTelemetryJobScheduler.execute();

        // Verify the method was still called, even though it threw an exception.
        verify(mLatencyMetricsLogger).log();
    }

    @Test
    @EnableFlags(FLAG_DATA_COMPLETENESS)
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void execute_completenessRecordingMethodCollectorException_exceptionCaught() {
        when(mCompletenessStatsCollector.readRecordingMethodStats())
                .thenThrow(new RuntimeException("Test exception"));
        when(mCompletenessStatsCollector.readDeviceInfoStats()).thenReturn(Collections.emptySet());

        mDataQualityTelemetryJobScheduler.execute();

        verify(mCompletenessStatsLogger, never()).logRecordingMethodStats(anyList());
        verify(mCompletenessStatsLogger).logDeviceInfoStats(Collections.emptySet());
    }

    @Test
    @EnableFlags(FLAG_DATA_COMPLETENESS)
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void execute_completenessDeviceInfoCollectorException_exceptionCaught() {
        when(mCompletenessStatsCollector.readRecordingMethodStats())
                .thenReturn(Collections.emptyList());
        when(mCompletenessStatsCollector.readDeviceInfoStats())
                .thenThrow(new RuntimeException("Test exception"));

        mDataQualityTelemetryJobScheduler.execute();

        verify(mCompletenessStatsLogger).logRecordingMethodStats(Collections.emptyList());
        verify(mCompletenessStatsLogger, never()).logDeviceInfoStats(anySet());
    }

    private void assertJobInfoIsCorrect(JobInfo jobInfo) {
        assertThat(jobInfo.isRequireCharging()).isTrue();
        assertThat(jobInfo.isRequireDeviceIdle()).isTrue();
        assertThat(jobInfo.getExtras().getInt(EXTRA_USER_ID))
                .isEqualTo(USER_HANDLE.getIdentifier());
        assertTrue(jobInfo.isPeriodic());
        assertThat(jobInfo.getIntervalMillis()).isEqualTo(JOB_RUN_INTERVAL);
        assertThat(jobInfo.getFlexMillis()).isEqualTo(JOB_FLEX_INTERVAL);
    }
}
