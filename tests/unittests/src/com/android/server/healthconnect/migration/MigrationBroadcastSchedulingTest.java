/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.migration;

import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_ALLOWED;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS;

import static com.android.server.healthconnect.migration.MigrationConstants.COUNT_MIGRATION_STATE_ALLOWED;
import static com.android.server.healthconnect.migration.MigrationConstants.COUNT_MIGRATION_STATE_IDLE;
import static com.android.server.healthconnect.migration.MigrationConstants.COUNT_MIGRATION_STATE_IN_PROGRESS;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;

import androidx.test.runner.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.Spy;
import org.mockito.verification.VerificationMode;

import java.util.Objects;

/** Unit tests for broadcast scheduling logic in {@link MigrationBroadcastScheduler} */
@RunWith(AndroidJUnit4.class)
public class MigrationBroadcastSchedulingTest {

    @Mock private Context mContext;
    @Mock private JobScheduler mJobScheduler;
    @Mock private MigrationStateManager mMigrationStateManager;

    @Spy
    private MigrationBroadcastScheduler mMigrationBroadcastScheduler =
            new MigrationBroadcastScheduler(0);

    private MockitoSession mStaticMockSession;

    @Before
    public void setUp() {
        mStaticMockSession =
                ExtendedMockito.mockitoSession()
                        .mockStatic(MigrationStateManager.class)
                        .startMocking();

        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testPrescheduleNewJobs_migrationStateIdle_requiredCountJobsScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IDLE);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);

        verifySchedulerInvocations(MIGRATION_STATE_IDLE, COUNT_MIGRATION_STATE_IDLE, times(1));
    }

    @Test
    public void testPrescheduleNewJobs_migrationStateInProgress_requiredCountJobsScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);

        verifySchedulerInvocations(
                MIGRATION_STATE_IN_PROGRESS, COUNT_MIGRATION_STATE_IN_PROGRESS, times(1));
    }

    @Test
    public void testPrescheduleNewJobs_migrationStateAllowed_requiredCountJobsScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);

        verifySchedulerInvocations(
                MIGRATION_STATE_ALLOWED, COUNT_MIGRATION_STATE_ALLOWED, times(1));
    }

    @Test
    public void testPrescheduleNewJobs_methodReinvocation_allPreviouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);
        verifySchedulerInvocations(
                MIGRATION_STATE_ALLOWED, COUNT_MIGRATION_STATE_ALLOWED, times(1));
        mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);
        verify(mJobScheduler, times(COUNT_MIGRATION_STATE_ALLOWED)).cancel(anyInt());
        verifySchedulerInvocations(
                MIGRATION_STATE_ALLOWED, COUNT_MIGRATION_STATE_ALLOWED, times(2));
    }

    @Test
    public void testPrescheduleNewJobs_schedulingFails_noFurtherScheduling() throws Exception {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_FAILURE);

        mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);

        verify(mJobScheduler, atMost(1))
                .schedule(
                        argThat(
                                jobInfo ->
                                        (Objects.equals(
                                                jobInfo.getService().getClassName(),
                                                MigrationBroadcastJobService.class.getName()))));
        verifyNoMoreInteractions(mJobScheduler);
    }

    private void verifySchedulerInvocations(
            int migrationState, int count, VerificationMode verificationMode) {
        long requiredInterval = MigrationBroadcastScheduler.getRequiredInterval(migrationState);
        for (int i = 0; i < count; i++) {
            long interval = requiredInterval * i;
            verify(mJobScheduler, verificationMode)
                    .schedule(
                            argThat(
                                    jobInfo ->
                                            (Objects.equals(
                                                            jobInfo.getService().getClassName(),
                                                            MigrationBroadcastJobService.class
                                                                    .getName()))
                                                    && (jobInfo.getMinLatencyMillis() == interval)
                                                    && (jobInfo.getMaxExecutionDelayMillis()
                                                            == interval)));
        }
    }
}
