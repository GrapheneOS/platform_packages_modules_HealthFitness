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
import static com.android.server.healthconnect.telemetry.TelemetryJobService.EXTRA_USER_ID;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityTelemetryJobScheduler.JOB_FLEX_INTERVAL;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityTelemetryJobScheduler.JOB_RUN_INTERVAL;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.any;
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

@RunWith(AndroidJUnit4.class)
public class DataQualityTelemetryJobSchedulerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private Context mContext;
    @Mock private JobScheduler mJobScheduler;
    @Mock private LatencyMetricsLogger mLatencyMetricsLogger;
    @Captor private ArgumentCaptor<JobInfo> mJobInfoArgumentCaptor;

    private static final UserHandle USER_HANDLE = UserHandle.of(1);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mJobScheduler.forNamespace(any())).thenReturn(mJobScheduler);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void testSchedule_flagOn_schedulesJob() {
        DataQualityTelemetryJobScheduler.schedule(mContext, USER_HANDLE);
        verify(mJobScheduler).schedule(any(JobInfo.class));
    }

    @Test
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void testSchedule_flagOff_doesNotScheduleJob() {
        DataQualityTelemetryJobScheduler.schedule(mContext, USER_HANDLE);
        verify(mJobScheduler, never()).schedule(any(JobInfo.class));
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void testCancelAllJobs_cancelsAllJobs() {
        DataQualityTelemetryJobScheduler.cancelAllJobs(mContext);
        verify(mJobScheduler).cancelAll();
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void execute_logsWeeklyMetrics() {
        DataQualityTelemetryJobScheduler.execute(mLatencyMetricsLogger);
        verify(mLatencyMetricsLogger).log();
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void schedule_jobInfoIsCorrect() {
        DataQualityTelemetryJobScheduler.schedule(mContext, USER_HANDLE);
        verify(mJobScheduler).schedule(mJobInfoArgumentCaptor.capture());
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.isRequireCharging()).isTrue();
        assertThat(jobInfo.isRequireDeviceIdle()).isTrue();
        assertThat(jobInfo.getExtras().getInt(EXTRA_USER_ID))
                .isEqualTo(USER_HANDLE.getIdentifier());
        assertTrue(jobInfo.isPeriodic());
        assertThat(jobInfo.getIntervalMillis()).isEqualTo(JOB_RUN_INTERVAL);
        assertThat(jobInfo.getFlexMillis()).isEqualTo(JOB_FLEX_INTERVAL);
    }
}
