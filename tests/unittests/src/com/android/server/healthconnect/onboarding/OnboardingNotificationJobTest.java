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
package com.android.server.healthconnect.onboarding;

import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_HIDE;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;

import static com.android.healthfitness.flags.Flags.FLAG_ONBOARDING;
import static com.android.server.healthconnect.HealthConnectDailyService.EXTRA_USER_ID;
import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.EXTRA_JOB_NAME_KEY;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationJob.ONBOARDING_NOTIFICATION_JOB_NAME;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationJob.ONBOARDING_NOTIFICATION_JOB_NAMESPACE;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationJob.executeOnboardingNotificationJob;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_ALL_NOTIFICATIONS;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_NO_NOTIFICATION;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.HealthConnectDailyService;
import com.android.server.healthconnect.injector.HealthConnectInjector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class OnboardingNotificationJobTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private Context mContext;
    @Mock private JobScheduler mMainJobScheduler;
    @Mock private JobScheduler mOnboardingNotificationJobScheduler;
    @Mock private OnboardingStateManager mOnboardingStateManager;
    @Mock private OnboardingNotificationSender mOnboardingNotificationSender;
    @Mock private OnboardingNotificationStateManager mOnboardingNotificationStateManager;
    @Mock private UserHandle mUserHandle;
    @Captor ArgumentCaptor<JobInfo> mJobInfoArgumentCaptor;
    private static final int USER_ID_INT = (int) (Math.random() * 100);

    @Before
    public void setUp() {
        when(mUserHandle.getIdentifier()).thenReturn(USER_ID_INT);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mMainJobScheduler);
        when(mContext.getUser()).thenReturn(mUserHandle);
        when(mMainJobScheduler.forNamespace(ONBOARDING_NOTIFICATION_JOB_NAMESPACE))
                .thenReturn(mOnboardingNotificationJobScheduler);
    }

    @After
    public void tearDown() {
        HealthConnectInjector.resetInstanceForTest();
        clearInvocations(mOnboardingNotificationJobScheduler, mOnboardingNotificationSender);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void scheduleJobIfNotScheduled_noExistingJob_scheduled() {
        when(mOnboardingNotificationJobScheduler.getAllPendingJobs()).thenReturn(List.of());

        OnboardingNotificationJob.scheduleJobIfNotScheduled(mContext, mUserHandle);
        verify(mOnboardingNotificationJobScheduler).schedule(mJobInfoArgumentCaptor.capture());

        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getInt(EXTRA_USER_ID)).isEqualTo(USER_ID_INT);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(ONBOARDING_NOTIFICATION_JOB_NAME);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void scheduleJobIfNotScheduled_existingJob_notScheduled() {
        JobInfo dummyJob =
                new JobInfo.Builder(
                                123, new ComponentName(mContext, HealthConnectDailyService.class))
                        .build();
        when(mOnboardingNotificationJobScheduler.getAllPendingJobs()).thenReturn(List.of(dummyJob));

        OnboardingNotificationJob.scheduleJobIfNotScheduled(mContext, mUserHandle);
        verify(mOnboardingNotificationJobScheduler, never()).schedule(any());
    }

    @Test
    public void cancelAllJobs_cancelled() {
        OnboardingNotificationJob.cancelAllJobs(mContext);

        verify(mOnboardingNotificationJobScheduler).cancelAll();
    }

    @Test
    @DisableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_flagDisabled_noOp() {
        when(mOnboardingStateManager.updateAndGetOnboardingState())
                .thenReturn(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        when(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .thenReturn(SHOULD_SHOW_ALL_NOTIFICATIONS);

        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);

        verifyNoNotificationSent();
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_callsUpdateAndGetOnboardingState_withoutBypass() {
        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);
        verify(mOnboardingStateManager).updateAndGetOnboardingState();
        verify(mOnboardingStateManager, never())
                .updateAndGetOnboardingState(/* bypassInstallTime= */ true);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_noAppConnected_notificationSent() {
        when(mOnboardingStateManager.updateAndGetOnboardingState())
                .thenReturn(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        when(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .thenReturn(SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION);

        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);

        verify(mOnboardingNotificationSender).sendNoAppConnectedNotification(eq(mUserHandle));
        verifyNoMoreInteractions(mOnboardingNotificationSender);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_noAppConnected_shouldNotShow_noNotification() {
        when(mOnboardingStateManager.updateAndGetOnboardingState())
                .thenReturn(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        when(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .thenReturn(
                        SHOULD_SHOW_ALL_NOTIFICATIONS
                                & (~SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION));

        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);

        verifyNoNotificationSent();
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_oneAppConnected_notificationSent() {
        when(mOnboardingStateManager.updateAndGetOnboardingState())
                .thenReturn(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        when(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .thenReturn(SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION);

        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);

        verify(mOnboardingNotificationSender).sendOneAppConnectedNotification(eq(mUserHandle));
        verifyNoMoreInteractions(mOnboardingNotificationSender);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_oneAppConnected_shouldNotShow_noNotification() {
        when(mOnboardingStateManager.updateAndGetOnboardingState())
                .thenReturn(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        when(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .thenReturn(
                        SHOULD_SHOW_ALL_NOTIFICATIONS
                                & (~SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION));

        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);

        verifyNoNotificationSent();
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_hide_noNotification() {
        when(mOnboardingStateManager.updateAndGetOnboardingState())
                .thenReturn(ONBOARDING_BANNER_STATE_HIDE);

        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);

        verifyNoNotificationSent();
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void executeOnboardingNotificationJob_shouldShowNoNotification_jobCancelled() {
        when(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .thenReturn(SHOULD_SHOW_NO_NOTIFICATION);

        executeOnboardingNotificationJob(
                mContext,
                mOnboardingStateManager,
                mOnboardingNotificationSender,
                mOnboardingNotificationStateManager,
                mUserHandle);

        verifyNoNotificationSent();
        verify(mOnboardingNotificationJobScheduler).cancelAll();
    }

    private void verifyNoNotificationSent() {
        verify(mOnboardingNotificationSender, never()).sendNoAppConnectedNotification(any());
        verify(mOnboardingNotificationSender, never()).sendOneAppConnectedNotification(any());
    }
}
