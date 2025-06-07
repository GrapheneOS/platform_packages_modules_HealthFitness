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

import static android.health.connect.HealthConnectManager.ACTION_SYNC_MORE_APPS;

import static com.android.healthfitness.flags.Flags.FLAG_ONBOARDING;
import static com.android.server.healthconnect.onboarding.HealthConnectOnboardingReceiver.ACTION_ONBOARDING_NOTIFICATION_CLICKED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class HealthConnectOnboardingReceiverTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Captor ArgumentCaptor<Intent> mIntentArgumentCaptor;

    private HealthConnectOnboardingReceiver mOnboardingReceiver;

    @Before
    public void setUp() {
        mOnboardingReceiver = new HealthConnectOnboardingReceiver();
        when(mPackageManager.resolveActivity(any(), anyInt())).thenReturn(new ResolveInfo());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void onReceive_notificationClickedIntent_startActivity() {
        mOnboardingReceiver.onReceive(mContext, new Intent(ACTION_ONBOARDING_NOTIFICATION_CLICKED));

        verify(mContext).startActivity(mIntentArgumentCaptor.capture());

        Intent intent = mIntentArgumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(ACTION_SYNC_MORE_APPS);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
    }

    @Test
    @DisableFlags(FLAG_ONBOARDING)
    public void onReceive_flagOff_noOp() {
        mOnboardingReceiver.onReceive(mContext, new Intent(ACTION_ONBOARDING_NOTIFICATION_CLICKED));

        verify(mContext, never()).startActivity(any());
    }
}
