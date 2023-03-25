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

import static com.android.server.healthconnect.migration.MigrationConstants.ALLOWED_STATE_TIMEOUT_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.CURRENT_STATE_START_TIME_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.IDLE_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationConstants.IN_PROGRESS_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationConstants.NON_IDLE_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationTestUtils.TIMEOUT_PERIOD_BUFFER;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
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

@RunWith(AndroidJUnit4.class)
public class MigrationStateChangeJobTest {
    @Mock MigrationStateManager mMigrationStateManager;
    @Mock PreferenceHelper mPreferenceHelper;
    @Mock private Context mContext;
    private MockitoSession mStaticMockSession;

    @Before
    public void setUp() {
        mStaticMockSession =
                ExtendedMockito.mockitoSession()
                        .mockStatic(PreferenceHelper.class)
                        .mockStatic(MigrationStateManager.class)
                        .strictness(Strictness.LENIENT)
                        .startMocking();
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        when(MigrationStateManager.getInitialisedInstance()).thenReturn(mMigrationStateManager);
        when(PreferenceHelper.getInstance()).thenReturn(mPreferenceHelper);
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
        MigrationStateChangeJob.executeMigrationPauseJob(mContext, null);
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
        MigrationStateChangeJob.executeMigrationPauseJob(mContext, null);
        verifyStateChange(MIGRATION_STATE_ALLOWED);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecutePauseJob_inAllowedState() {
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationPauseJob(mContext, null);
        verifyZeroInteractions(mPreferenceHelper);
        verifyNoStateChange();
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecutePauseJob_inCompleteState() {
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_COMPLETE);
        MigrationStateChangeJob.executeMigrationPauseJob(mContext, null);
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
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
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
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyStateChange(MIGRATION_STATE_COMPLETE);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_fromAllowedState_timeNotExpired() {
        setStartTime_notExpired_nonIdleState();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_fromAllowedState() {
        setStartTime_expired_nonIdleState();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyStateChange(MIGRATION_STATE_COMPLETE);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_fromAppUpgradeRequiredState_timeNotExpired() {
        setStartTime_notExpired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_APP_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_fromAppUpgradeRequiredState() {
        setStartTime_expired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_APP_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyStateChange(MIGRATION_STATE_COMPLETE);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_fromModuleUpgradeRequiredState_timeNotExpired() {
        setStartTime_notExpired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_MODULE_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_fromModuleUpgradeRequiredState() {
        setStartTime_expired_nonIdleState();
        when(mMigrationStateManager.getMigrationState())
                .thenReturn(MIGRATION_STATE_MODULE_UPGRADE_REQUIRED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyStateChange(MIGRATION_STATE_COMPLETE);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_alreadyComplete() {
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_COMPLETE);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyZeroInteractions(mPreferenceHelper);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_afterAllowedStateTimeoutPeriod_fromAllowedState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeAfterAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyStateChange(MIGRATION_STATE_COMPLETE);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_beforeAllowedStateTimeoutPeriod_fromAllowedState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeBeforeAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_ALLOWED);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyNoStateChange();
    }

    /** Expected behavior: Change state to complete */
    @Test
    public void testExecuteCompleteJob_afterAllowedStateTimeoutPeriod_fromInProgressState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeAfterAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyStateChange(MIGRATION_STATE_COMPLETE);
    }

    /** Expected behavior: No changes to the state */
    @Test
    public void testExecuteCompleteJob_beforeAllowedStateTimeoutPeriod_fromInProgressState() {
        setStartTime_notExpired_nonIdleState();
        setStartTimeBeforeAllowedStateTimeout();
        when(mMigrationStateManager.getMigrationState()).thenReturn(MIGRATION_STATE_IN_PROGRESS);
        MigrationStateChangeJob.executeMigrationCompletionJob(mContext, null);
        verifyNoStateChange();
    }

    private void verifyNoStateChange() {
        verify(mPreferenceHelper, never()).insertOrReplacePreferencesTransaction(any());
        verify(mMigrationStateManager, never()).updateMigrationState(any(Context.class), anyInt());
    }

    private void verifyStateChange(int state) {
        verify(mMigrationStateManager).updateMigrationState(eq(mContext), eq(state));
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
}
