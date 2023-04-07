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
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS;

import static com.android.server.healthconnect.migration.MigrationConstants.COUNT_MIGRATION_STATE_ALLOWED;
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
    private final long mMinPeriodMillis = JobInfo.getMinPeriodMillis();
    private final long mIntervalGreaterThanMinPeriod = mMinPeriodMillis + 1000;
    private final long mIntervalLessThanMinPeriod = mMinPeriodMillis - 1000;

    @Before
    public void setUp() {
        mStaticMockSession =
                ExtendedMockito.mockitoSession()
                        .mockStatic(MigrationStateManager.class)
                        .startMocking();

        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
    }

    @After
    public void tearDown() {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void
            testScheduling_migrationInProgressIntervalGreaterThanMinimum_periodicJobScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_IN_PROGRESS))
                .thenReturn(mIntervalGreaterThanMinPeriod);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(1));
    }

    @Test
    public void testScheduling_migrationInProgressIntervalEqualToMinimum_periodicJobScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_IN_PROGRESS))
                .thenReturn(mMinPeriodMillis);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(1));
    }

    @Test
    public void
            testScheduling_migrationInProgressIntervalLessThanMinimum_requiredCountJobsScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_IN_PROGRESS))
                .thenReturn(mIntervalLessThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_IN_PROGRESS, mIntervalLessThanMinPeriod, times(1));
    }

    @Test
    public void testScheduling_migrationAllowedIntervalGreaterThanMinimum_periodicJobScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalGreaterThanMinPeriod);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(1));
    }

    @Test
    public void testScheduling_requiredCountEqualToZeroIntervalGreaterThanMinimum_noJobScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredCount(MIGRATION_STATE_ALLOWED)).thenReturn(0);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalGreaterThanMinPeriod);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(0));
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalGreaterThanMinPeriod, times(0));
    }

    @Test
    public void testScheduling_requiredCountEqualToZeroIntervalEqualToMinimum_noJobScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredCount(MIGRATION_STATE_ALLOWED)).thenReturn(0);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mMinPeriodMillis);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(0));
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mMinPeriodMillis, times(0));
    }

    @Test
    public void testScheduling_requiredCountEqualToZeroIntervalLessThanMinimum_noJobScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredCount(MIGRATION_STATE_ALLOWED)).thenReturn(0);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalLessThanMinPeriod);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyPeriodicJobSchedulerInvocation(mIntervalLessThanMinPeriod, times(0));
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(0));
    }

    @Test
    public void testScheduling_migrationAllowedIntervalEqualToMinimum_periodicJobScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mMinPeriodMillis);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(1));
    }

    @Test
    public void
            testScheduling_migrationAllowedIntervalLessThanMinimum_requiredCountJobsScheduled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalLessThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(1));
    }

    @Test
    public void
            testReinvocation_origAndNewIntervalsGreaterThanMin_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalGreaterThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(1));
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(1)).cancel(anyInt());
        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(2));
    }

    @Test
    public void
            testReinvocation_origAndNewIntervalsEqualToMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mMinPeriodMillis);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(1));
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(1)).cancel(anyInt());
        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(2));
    }

    @Test
    public void
            testReinvocation_origAndNewIntervalsLessThanMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalLessThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(1));
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(COUNT_MIGRATION_STATE_ALLOWED)).cancel(anyInt());
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(2));
    }

    @Test
    public void testReinvocation_origGreaterNewLessThanMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalGreaterThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(1));
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalLessThanMinPeriod);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(1)).cancel(anyInt());
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(1));
    }

    @Test
    public void testReinvocation_origGreaterNewEqualToMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalGreaterThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(1));
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mMinPeriodMillis);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(1)).cancel(anyInt());
        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(1));
    }

    @Test
    public void
            testReinvocation_origEqualToNewGreaterThanMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mMinPeriodMillis);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(1));
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalGreaterThanMinPeriod);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(1)).cancel(anyInt());
        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(1));
    }

    @Test
    public void testReinvocation_origEqualToNewLessThanMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mMinPeriodMillis);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(1));
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalLessThanMinPeriod);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(1)).cancel(anyInt());
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(1));
    }

    @Test
    public void testReinvocation_origLessNewGreaterThanMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalLessThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(1));
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalGreaterThanMinPeriod);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(COUNT_MIGRATION_STATE_ALLOWED)).cancel(anyInt());
        verifyPeriodicJobSchedulerInvocation(mIntervalGreaterThanMinPeriod, times(1));
    }

    @Test
    public void testReinvocation_origLessNewEqualToMinimum_previouslyScheduledJobsCancelled() {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mIntervalLessThanMinPeriod);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_SUCCESS);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verifyNonPeriodicJobSchedulerInvocation(
                COUNT_MIGRATION_STATE_ALLOWED, mIntervalLessThanMinPeriod, times(1));
        when(mMigrationBroadcastScheduler.getRequiredInterval(MIGRATION_STATE_ALLOWED))
                .thenReturn(mMinPeriodMillis);
        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);
        verify(mJobScheduler, times(COUNT_MIGRATION_STATE_ALLOWED)).cancel(anyInt());
        verifyPeriodicJobSchedulerInvocation(mMinPeriodMillis, times(1));
    }

    @Test
    public void testScheduling_schedulingFails_noFurtherScheduling() throws Exception {
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        when(mJobScheduler.schedule(any(JobInfo.class))).thenReturn(JobScheduler.RESULT_FAILURE);

        mMigrationBroadcastScheduler.scheduleNewJobs(mContext);

        verify(mJobScheduler, atMost(1))
                .schedule(
                        argThat(
                                jobInfo ->
                                        (Objects.equals(
                                                jobInfo.getService().getClassName(),
                                                MigrationBroadcastJobService.class.getName()))));
        verifyNoMoreInteractions(mJobScheduler);
    }

    private void verifyPeriodicJobSchedulerInvocation(
            long interval, VerificationMode verificationMode) {
        verify(mJobScheduler, verificationMode)
                .schedule(
                        argThat(
                                jobInfo ->
                                        (Objects.equals(
                                                        jobInfo.getService().getClassName(),
                                                        MigrationBroadcastJobService.class
                                                                .getName()))
                                                && jobInfo.isPeriodic()
                                                && jobInfo.getIntervalMillis() == interval));
    }

    private void verifyNonPeriodicJobSchedulerInvocation(
            int count, long requiredInterval, VerificationMode verificationMode) {
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
                                                    && (!jobInfo.isPeriodic())
                                                    && (jobInfo.getMinLatencyMillis() == interval)
                                                    && (jobInfo.getMaxExecutionDelayMillis()
                                                            == interval)));
        }
    }
}
