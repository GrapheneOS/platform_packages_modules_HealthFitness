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
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_APP_UPGRADE_REQUIRED;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_COMPLETE;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_MODULE_UPGRADE_REQUIRED;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.verify;
import static com.android.server.healthconnect.HealthConnectDailyService.EXTRA_JOB_NAME_KEY;
import static com.android.server.healthconnect.HealthConnectDailyService.EXTRA_USER_ID;
import static com.android.server.healthconnect.migration.MigrationConstants.ALLOWED_STATE_TIMEOUT_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.CURRENT_STATE_START_TIME_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.IDLE_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationConstants.IN_PROGRESS_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_COMPLETE_JOB_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_COMPLETION_JOB_RUN_INTERVAL;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_PAUSE_JOB_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_PAUSE_JOB_RUN_INTERVAL;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_STATE_CHANGE_NAMESPACE;
import static com.android.server.healthconnect.migration.MigrationConstants.NON_IDLE_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationStateChangeJob.MIN_JOB_ID;
import static com.android.server.healthconnect.migration.MigrationTestUtils.MOCK_CONFIGURED_PACKAGE;
import static com.android.server.healthconnect.migration.MigrationTestUtils.TIMEOUT_PERIOD_BUFFER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserHandle;

import androidx.test.runner.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.server.healthconnect.HealthConnectDailyService;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class MigrationStateChangeJobTest {
    @Mock MigrationStateManager mMigrationStateManager;
    @Mock PreferenceHelper mPreferenceHelper;
    @Mock private Context mContext;
    @Mock private JobScheduler mJobScheduler;
    @Mock private PersistableBundle mPersistableBundle;
    @Mock private JobInfo mJobInfo;
    private static final UserHandle DEFAULT_USER_HANDLE = UserHandle.of(UserHandle.myUserId());
    private MockitoSession mStaticMockSession;

    @Before
    public void setUp() {
        mStaticMockSession =
                ExtendedMockito.mockitoSession()
                        .mockStatic(PreferenceHelper.class)
                        .mockStatic(MigrationStateManager.class)
                        .mockStatic(HealthConnectDailyService.class)
                        .strictness(Strictness.LENIENT)
                        .startMocking();
        MockitoAnnotations.initMocks(this);
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(PreferenceHelper.getInstance()).thenReturn(mPreferenceHelper);
        when(mJobScheduler.forNamespace(MIGRATION_STATE_CHANGE_NAMESPACE))
                .thenReturn(mJobScheduler);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mContext.getPackageName()).thenReturn(MOCK_CONFIGURED_PACKAGE);
    }

    @After
    public void tearDown() {
        clearInvocations(mPreferenceHelper);
        clearInvocations(mMigrationStateManager);
        mStaticMockSession.finishMocking();
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecutePauseJob_timeNotExpired() {
        long mockElapsedTime = IN_PROGRESS_STATE_TIMEOUT_PERIOD.toMillis() - TIMEOUT_PERIOD_BUFFER;
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        when(mPreferenceHelper.getPreference(eq(CURRENT_STATE_START_TIME_KEY)))
                .thenReturn(Instant.now().minusMillis(mockElapsedTime).toString());
        MigrationStateChangeJob.executeMigrationPauseJob(mContext);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to allowed */
    @Test
    public void testExecutePauseJob_timeExpired_shouldChangeState() {
        long mockElapsedTime =
                IN_PROGRESS_STATE_TIMEOUT_PERIOD.plusMillis(TIMEOUT_PERIOD_BUFFER).toMillis();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        when(mPreferenceHelper.getPreference(eq(CURRENT_STATE_START_TIME_KEY)))
                .thenReturn(Instant.now().minusMillis(mockElapsedTime).toString());
        MigrationStateChangeJob.executeMigrationPauseJob(mContext);
        verifyStateChange(MIGRATION_STATE_ALLOWED, true);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecutePauseJob_inAllowedState() {
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationPauseJob(mContext);
        verifyZeroInteractions(mPreferenceHelper);
        verifyNoStateChange();
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecutePauseJob_inCompleteState() {
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_COMPLETE);
        MigrationStateChangeJob.executeMigrationPauseJob(mContext);
        verifyZeroInteractions(mPreferenceHelper);
        verifyNoStateChange();
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_fromIdleState_timeNotExpired() {
        long mockElapsedTime = IDLE_STATE_TIMEOUT_PERIOD.toMillis() - TIMEOUT_PERIOD_BUFFER;
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IDLE);
        when(mPreferenceHelper.getPreference(eq(CURRENT_STATE_START_TIME_KEY)))
                .thenReturn(Instant.now().minusMillis(mockElapsedTime).toString());
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_fromIdleState() {
        long mockElapsedTime =
                IDLE_STATE_TIMEOUT_PERIOD.plusMillis(TIMEOUT_PERIOD_BUFFER).toMillis();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IDLE);
        when(mPreferenceHelper.getPreference(eq(CURRENT_STATE_START_TIME_KEY)))
                .thenReturn(Instant.now().minusMillis(mockElapsedTime).toString());
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyStateChange(MIGRATION_STATE_COMPLETE, true);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_fromAllowedState_timeNotExpired() {
        setStartTime_notExpired_nonIdleState();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_fromAllowedState() {
        setStartTime_expired_nonIdleState();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyStateChange(MIGRATION_STATE_COMPLETE, true);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_fromAppUpgradeRequiredState_timeNotExpired() {
        setStartTime_notExpired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_APP_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_fromAppUpgradeRequiredState() {
        setStartTime_expired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_APP_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyStateChange(MIGRATION_STATE_COMPLETE, true);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_fromModuleUpgradeRequiredState_timeNotExpired() {
        setStartTime_notExpired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_MODULE_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_fromModuleUpgradeRequiredState() {
        setStartTime_expired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_MODULE_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyStateChange(MIGRATION_STATE_COMPLETE, true);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_alreadyComplete() {
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_COMPLETE);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyZeroInteractions(mPreferenceHelper);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_afterAllowedStateTimeoutPeriod_fromAllowedState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeAfterAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyStateChange(MIGRATION_STATE_COMPLETE, true);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_beforeAllowedStateTimeoutPeriod_fromAllowedState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeBeforeAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_afterAllowedStateTimeoutPeriod_fromInProgressState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeAfterAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyStateChange(MIGRATION_STATE_COMPLETE, true);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_beforeAllowedStateTimeoutPeriod_fromInProgressState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeBeforeAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext);
        verifyNoStateChange();
    }

    private void verifyNoStateChange() {
        verify(mPreferenceHelper, never()).insertOrReplacePreferencesTransaction(any());
        verify(mMigrationStateManager, never()).updateMigrationState(any(Context.class), anyInt());
    }

    private void verifyStateChange(int state, boolean timeoutReached) {
        verify(mMigrationStateManager)
                .updateMigrationState(eq(mContext), eq(state), eq(timeoutReached));
    }

    private void setStartTime_notExpired_nonIdleState() {
        long mockElapsedTime = NON_IDLE_STATE_TIMEOUT_PERIOD.toMillis() - TIMEOUT_PERIOD_BUFFER;
        when(mPreferenceHelper.getPreference(eq(CURRENT_STATE_START_TIME_KEY)))
                .thenReturn(Instant.now().minusMillis(mockElapsedTime).toString());
    }

    private void setStartTime_expired_nonIdleState() {
        long mockElapsedTime =
                NON_IDLE_STATE_TIMEOUT_PERIOD.plusMillis(TIMEOUT_PERIOD_BUFFER).toMillis();
        when(mPreferenceHelper.getPreference(eq(CURRENT_STATE_START_TIME_KEY)))
                .thenReturn(Instant.now().minusMillis(mockElapsedTime).toString());
    }

    private void setStartTimeAfterAllowedStateTimeout() {
        long mockAllowedStateTimeout =
                NON_IDLE_STATE_TIMEOUT_PERIOD.toMillis() - TIMEOUT_PERIOD_BUFFER;
        when(mPreferenceHelper.getPreference(eq(ALLOWED_STATE_TIMEOUT_KEY)))
                .thenReturn(Instant.now().minusMillis(mockAllowedStateTimeout).toString());
    }

    private void setStartTimeBeforeAllowedStateTimeout() {
        when(mPreferenceHelper.getPreference(eq(ALLOWED_STATE_TIMEOUT_KEY)))
                .thenReturn(Instant.now().plusMillis(TIMEOUT_PERIOD_BUFFER).toString());
    }

    @Test
    public void testScheduleCompletionJob() {
        MigrationStateChangeJob.scheduleMigrationCompletionJob(
                mContext, DEFAULT_USER_HANDLE.getIdentifier());
        verify(
                () ->
                        HealthConnectDailyService.schedule(
                                any(JobScheduler.class),
                                anyInt(),
                                argThat(
                                        job ->
                                                hasExpectedParameters(
                                                        job,
                                                        MIGRATION_COMPLETE_JOB_NAME,
                                                        MIGRATION_COMPLETION_JOB_RUN_INTERVAL))));
    }

    @Test
    public void testSchedulePauseJob() {
        MigrationStateChangeJob.scheduleMigrationPauseJob(
                mContext, DEFAULT_USER_HANDLE.getIdentifier());
        when(mContext.getSystemService(eq(JobScheduler.class))).thenReturn(mJobScheduler);

        verify(
                () ->
                        HealthConnectDailyService.schedule(
                                any(JobScheduler.class),
                                anyInt(),
                                argThat(
                                        job ->
                                                hasExpectedParameters(
                                                        job,
                                                        MIGRATION_PAUSE_JOB_NAME,
                                                        MIGRATION_PAUSE_JOB_RUN_INTERVAL))));
    }

    @Test
    public void testExistsMigrationPauseJob() {
        configureExistsAMigrationPauseJob();
        assertThat(
                        MigrationStateChangeJob.existsAStateChangeJob(
                                mContext, MIGRATION_PAUSE_JOB_NAME))
                .isTrue();
        assertThat(
                        MigrationStateChangeJob.existsAStateChangeJob(
                                mContext, MIGRATION_COMPLETE_JOB_NAME))
                .isFalse();
    }

    @Test
    public void testExistsMigrationCompleteJob() {
        configureExistsAMigrationCompleteJob();
        assertThat(
                        MigrationStateChangeJob.existsAStateChangeJob(
                                mContext, MIGRATION_COMPLETE_JOB_NAME))
                .isTrue();

        assertThat(
                        MigrationStateChangeJob.existsAStateChangeJob(
                                mContext, MIGRATION_PAUSE_JOB_NAME))
                .isFalse();
    }

    @Test
    public void testCancelAllJobs() {
        when(mJobInfo.getId()).thenReturn(MIN_JOB_ID);
        when(mJobScheduler.getAllPendingJobs()).thenReturn(List.of(mJobInfo));
        MigrationStateChangeJob.cancelAllJobs(mContext);
        verify(mJobScheduler).cancel(eq(MIN_JOB_ID));
    }

    private void configureExistsAMigrationPauseJob() {
        when(mPersistableBundle.getInt(eq(EXTRA_USER_ID)))
                .thenReturn(DEFAULT_USER_HANDLE.getIdentifier());
        when(mPersistableBundle.getString(eq(EXTRA_JOB_NAME_KEY)))
                .thenReturn(MIGRATION_PAUSE_JOB_NAME);
        when(mJobInfo.getExtras()).thenReturn(mPersistableBundle);
        when(mJobScheduler.getAllPendingJobs()).thenReturn(List.of(mJobInfo));
    }

    private void configureExistsAMigrationCompleteJob() {
        when(mPersistableBundle.getInt(eq(EXTRA_USER_ID)))
                .thenReturn(DEFAULT_USER_HANDLE.getIdentifier());
        when(mPersistableBundle.getString(eq(EXTRA_JOB_NAME_KEY)))
                .thenReturn(MIGRATION_COMPLETE_JOB_NAME);
        when(mJobInfo.getExtras()).thenReturn(mPersistableBundle);
        when(mJobScheduler.getAllPendingJobs()).thenReturn(List.of(mJobInfo));
    }

    private boolean hasExpectedParameters(JobInfo job, String jobName, long jobRunInterval) {
        return !job.isPersisted()
                && isExpectedComponent(job)
                && hasCorrectName(job, jobName)
                && hasCorrectUserId(job)
                && isJobIdSet(job)
                && job.isPeriodic()
                && job.getIntervalMillis() == jobRunInterval;
    }

    private boolean hasCorrectUserId(JobInfo job) {
        return !Objects.isNull(job.getExtras())
                && Objects.equals(
                        job.getExtras().getInt(EXTRA_USER_ID), DEFAULT_USER_HANDLE.getIdentifier());
    }

    private boolean hasCorrectName(JobInfo job, String name) {
        return !Objects.isNull(job.getExtras())
                && Objects.equals(job.getExtras().getString(EXTRA_JOB_NAME_KEY), name);
    }

    private boolean isExpectedComponent(JobInfo job) {
        return !Objects.isNull(job.getService())
                && job.getService()
                        .equals(new ComponentName(mContext, HealthConnectDailyService.class));
    }

    private boolean isJobIdSet(JobInfo job) {
        return job.getId() == MIN_JOB_ID + DEFAULT_USER_HANDLE.getIdentifier();
    }
}
