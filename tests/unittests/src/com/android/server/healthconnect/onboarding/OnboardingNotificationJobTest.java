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

import static com.android.healthfitness.flags.Flags.FLAG_ONBOARDING;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationJob.ONBOARDING_NOTIFICATION_JOB_NAMESPACE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@EnableFlags(FLAG_ONBOARDING)
public class OnboardingNotificationJobTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private Context mContext;
    @Mock private JobScheduler mMainJobScheduler;
    @Mock private JobScheduler mOnboardingNotificationJobScheduler;

    @Before
    public void setUp() {
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mMainJobScheduler);
        when(mContext.getUser()).thenReturn(UserHandle.CURRENT);
        when(mMainJobScheduler.forNamespace(ONBOARDING_NOTIFICATION_JOB_NAMESPACE))
                .thenReturn(mOnboardingNotificationJobScheduler);
    }

    @After
    public void tearDown() {
        HealthConnectInjector.resetInstanceForTest();
    }

    @Test
    public void scheduleJobIfNotScheduled_noExistingJob_scheduled() {
        when(mOnboardingNotificationJobScheduler.getAllPendingJobs()).thenReturn(List.of());

        OnboardingNotificationJob.scheduleJobIfNotScheduled(UserHandle.CURRENT, mContext);
        verify(mOnboardingNotificationJobScheduler).schedule(any());
    }

    @Test
    public void scheduleJobIfNotScheduled_existingJob_notScheduled() {
        JobInfo dummyJob =
                new JobInfo.Builder(
                                123, new ComponentName(mContext, HealthConnectDailyService.class))
                        .build();
        when(mOnboardingNotificationJobScheduler.getAllPendingJobs()).thenReturn(List.of(dummyJob));

        OnboardingNotificationJob.scheduleJobIfNotScheduled(UserHandle.CURRENT, mContext);
        verify(mOnboardingNotificationJobScheduler, never()).schedule(any());
    }

    @Test
    public void cancelAllJobs_cancelled() {
        OnboardingNotificationJob.cancelAllJobs(mContext);

        verify(mOnboardingNotificationJobScheduler).cancelAll();
    }
}
