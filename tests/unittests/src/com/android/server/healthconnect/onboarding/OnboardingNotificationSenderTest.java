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

import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.CONNECT_MORE_APPS_NOTIFICATION_BUTTON;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.CONNECT_MORE_APPS_NOTIFICATION_CONTENT;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.CONNECT_MORE_APPS_NOTIFICATION_TITLE;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.START_USING_HC_NOTIFICATION_BUTTON;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.START_USING_HC_NOTIFICATION_CONTENT;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationSender.START_USING_HC_NOTIFICATION_TITLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.content.Context;
import android.os.UserHandle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;

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

    @Mock private HealthConnectNotificationSender mNotificationSender;
    @Mock private HealthConnectResourcesContext mResourcesContext;
    private Context mContext;
    private UserHandle mUserHandle;
    private OnboardingNotificationSender mOnboardingNotificationSender;
    @Captor ArgumentCaptor<Notification> mNotificationCaptor;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mUserHandle = mContext.getUser();
        mOnboardingNotificationSender =
                new OnboardingNotificationSender(mContext, mResourcesContext);
        mOnboardingNotificationSender.setNotificationSenderForTesting(mNotificationSender);
        when(mResourcesContext.getStringByNameOrThrow(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void sendNoAppConnectedNotification_success() {
        mOnboardingNotificationSender.sendNoAppConnectedNotification(mUserHandle);
        verify(mNotificationSender)
                .sendNotificationAsUser(mNotificationCaptor.capture(), eq(mUserHandle));

        Notification notification = mNotificationCaptor.getValue();
        assertThat(notification.extras.getString(EXTRA_TITLE))
                .isEqualTo(START_USING_HC_NOTIFICATION_TITLE);
        assertThat(notification.extras.getString(EXTRA_BIG_TEXT))
                .isEqualTo(START_USING_HC_NOTIFICATION_CONTENT);

        Notification.Action action = notification.actions[0];
        assertThat(action.title.toString()).isEqualTo(START_USING_HC_NOTIFICATION_BUTTON);

        // TODO(b/403257033): test pending intent
    }

    @Test
    public void sendOneAppConnectedNotification_success() {
        mOnboardingNotificationSender.sendOneAppConnectedNotification(mUserHandle);
        verify(mNotificationSender)
                .sendNotificationAsUser(mNotificationCaptor.capture(), eq(mUserHandle));

        Notification notification = mNotificationCaptor.getValue();
        assertThat(notification.extras.getString(EXTRA_TITLE))
                .isEqualTo(CONNECT_MORE_APPS_NOTIFICATION_TITLE);
        assertThat(notification.extras.getString(EXTRA_BIG_TEXT))
                .isEqualTo(CONNECT_MORE_APPS_NOTIFICATION_CONTENT);

        Notification.Action action = notification.actions[0];
        assertThat(action.title.toString()).isEqualTo(CONNECT_MORE_APPS_NOTIFICATION_BUTTON);

        // TODO(b/403257033): test pending intent
    }
}
