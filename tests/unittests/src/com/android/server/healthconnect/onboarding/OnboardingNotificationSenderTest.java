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

import static android.app.Notification.EXTRA_BIG_TEXT;
import static android.app.Notification.EXTRA_TITLE;
import static android.health.connect.Constants.NOTIFICATION_CHANNEL_ID;

import static com.android.healthfitness.flags.Flags.FLAG_ONBOARDING_NOTIFICATION;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.CONNECT_MORE_APPS_NOTIFICATION_CONTENT;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.CONNECT_MORE_APPS_NOTIFICATION_TITLE;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.START_USING_HC_NOTIFICATION_CONTENT;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.START_USING_HC_NOTIFICATION_TITLE;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_ALL_NOTIFICATIONS;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_NO_NOTIFICATION;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;

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

@RunWith(AndroidJUnit4.class)
public class OnboardingNotificationSenderTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private HealthConnectNotificationSender mNotificationSender;
    @Mock private HealthConnectResourcesContext mResourcesContext;
    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private UserHandle mUserHandle;
    private Context mContext;
    private OnboardingNotificationSender mOnboardingNotificationSender;
    @Captor ArgumentCaptor<Notification> mNotificationCaptor;

    private static final int USER_ID_INT = (int) (Math.random() * 100);
    private static final String PREF_KEY = NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX + USER_ID_INT;

    @Before
    public void setUp() throws Exception {
        when(mUserHandle.getIdentifier()).thenReturn(USER_ID_INT);
        when(mResourcesContext.getStringByNameOrThrow(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mNotificationSender.sendNotificationAsUser(any(), eq(mUserHandle))).thenReturn(true);

        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mOnboardingNotificationSender =
                new OnboardingNotificationSender(
                        mContext,
                        mResourcesContext,
                        new OnboardingNotificationStateManager(mPreferenceHelper, mUserHandle));
        mOnboardingNotificationSender.setNotificationSenderForTesting(mNotificationSender);
    }

    @After
    public void tearDown() {
        clearInvocations(mPreferenceHelper, mNotificationSender);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendNoAppConnectedNotification_success() {
        mOnboardingNotificationSender.sendNoAppConnectedNotification(mUserHandle);
        verify(mNotificationSender)
                .sendNotificationAsUser(mNotificationCaptor.capture(), eq(mUserHandle));

        Notification notification = mNotificationCaptor.getValue();
        assertThat(notification.extras.getString(EXTRA_TITLE))
                .isEqualTo(START_USING_HC_NOTIFICATION_TITLE);
        assertThat(notification.extras.getString(EXTRA_BIG_TEXT))
                .isEqualTo(START_USING_HC_NOTIFICATION_CONTENT);
        assertThat(notification.actions).isNull();
        assertThat(notification.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);

        PendingIntent contentIntent = notification.contentIntent;
        assertThat(contentIntent.getCreatorPackage()).isEqualTo(mContext.getPackageName());
        assertThat(contentIntent.isBroadcast()).isTrue();
        assertThat(contentIntent.isImmutable()).isTrue();

        PendingIntent deleteIntent = notification.deleteIntent;
        assertThat(deleteIntent.getCreatorPackage()).isEqualTo(mContext.getPackageName());
        assertThat(deleteIntent.isBroadcast()).isTrue();
        assertThat(deleteIntent.isImmutable()).isTrue();
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendNoAppConnectedNotification_notificationStateUpdated() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenReturn(String.valueOf(SHOULD_SHOW_ALL_NOTIFICATIONS));

        mOnboardingNotificationSender.sendNoAppConnectedNotification(mUserHandle);

        verify(mPreferenceHelper)
                .insertOrReplacePreference(
                        eq(PREF_KEY),
                        eq(String.valueOf(SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION)));
    }

    @Test
    @DisableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendNoAppConnectedNotification_flagDisabled_noOp() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenReturn(String.valueOf(SHOULD_SHOW_ALL_NOTIFICATIONS));

        mOnboardingNotificationSender.sendNoAppConnectedNotification(mUserHandle);

        verify(mNotificationSender, never()).sendNotificationAsUser(any(), eq(mUserHandle));
        verify(mPreferenceHelper, never()).insertOrReplacePreference(any(), any());
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendNoAppConnectedNotification_channelBlocked_noOp() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenReturn(String.valueOf(SHOULD_SHOW_ALL_NOTIFICATIONS));
        when(mNotificationSender.sendNotificationAsUser(any(), eq(mUserHandle))).thenReturn(false);

        mOnboardingNotificationSender.sendNoAppConnectedNotification(mUserHandle);

        verify(mPreferenceHelper, never()).insertOrReplacePreference(any(), any());
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendOneAppConnectedNotification_success() {
        mOnboardingNotificationSender.sendOneAppConnectedNotification(mUserHandle);
        verify(mNotificationSender)
                .sendNotificationAsUser(mNotificationCaptor.capture(), eq(mUserHandle));

        Notification notification = mNotificationCaptor.getValue();
        assertThat(notification.extras.getString(EXTRA_TITLE))
                .isEqualTo(CONNECT_MORE_APPS_NOTIFICATION_TITLE);
        assertThat(notification.extras.getString(EXTRA_BIG_TEXT))
                .isEqualTo(CONNECT_MORE_APPS_NOTIFICATION_CONTENT);
        assertThat(notification.actions).isNull();
        assertThat(notification.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);

        PendingIntent contentIntent = notification.contentIntent;
        assertThat(contentIntent.getCreatorPackage()).isEqualTo(mContext.getPackageName());
        assertThat(contentIntent.isBroadcast()).isTrue();
        assertThat(contentIntent.isImmutable()).isTrue();

        PendingIntent deleteIntent = notification.deleteIntent;
        assertThat(deleteIntent.getCreatorPackage()).isEqualTo(mContext.getPackageName());
        assertThat(deleteIntent.isBroadcast()).isTrue();
        assertThat(deleteIntent.isImmutable()).isTrue();
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendOneAppConnectedNotification_notificationStateUpdated() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenReturn(String.valueOf(SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION));

        mOnboardingNotificationSender.sendOneAppConnectedNotification(mUserHandle);

        verify(mPreferenceHelper)
                .insertOrReplacePreference(
                        eq(PREF_KEY), eq(String.valueOf(SHOULD_SHOW_NO_NOTIFICATION)));
    }

    @Test
    @DisableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendOneAppConnectedNotification_flagDisabled_noOp() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenReturn(String.valueOf(SHOULD_SHOW_ALL_NOTIFICATIONS));

        mOnboardingNotificationSender.sendOneAppConnectedNotification(mUserHandle);

        verify(mNotificationSender, never()).sendNotificationAsUser(any(), eq(mUserHandle));
        verify(mPreferenceHelper, never()).insertOrReplacePreference(any(), any());
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING_NOTIFICATION)
    public void sendOneAppConnectedNotification_channelBlocked_noOp() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenReturn(String.valueOf(SHOULD_SHOW_ALL_NOTIFICATIONS));
        when(mNotificationSender.sendNotificationAsUser(any(), eq(mUserHandle))).thenReturn(false);

        mOnboardingNotificationSender.sendOneAppConnectedNotification(mUserHandle);

        verify(mPreferenceHelper, never()).insertOrReplacePreference(any(), any());
    }
}
