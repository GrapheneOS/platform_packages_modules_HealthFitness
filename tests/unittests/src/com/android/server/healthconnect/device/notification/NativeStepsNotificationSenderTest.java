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
package com.android.server.healthconnect.device.notification;

import static android.app.Notification.EXTRA_BIG_TEXT;
import static android.app.Notification.EXTRA_TITLE;

import static com.android.server.healthconnect.device.notification.NativeStepsNotificationSender.NOTIFICATION_CONTENT;
import static com.android.server.healthconnect.device.notification.NativeStepsNotificationSender.NOTIFICATION_TITLE;
import static com.android.server.healthconnect.device.notification.NativeStepsNotificationStateManager.NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
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
public class NativeStepsNotificationSenderTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private HealthConnectNotificationSender mNotificationSender;
    @Mock private HealthConnectResourcesContext mResourcesContext;
    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private UserHandle mUserHandle;
    private Context mContext;
    private com.android.server.healthconnect.device.notification.NativeStepsNotificationSender
            mNativeStepsNotificationSender;
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
        mNativeStepsNotificationSender =
                new NativeStepsNotificationSender(
                        mContext,
                        mResourcesContext,
                        new NativeStepsNotificationStateManager(mPreferenceHelper, mUserHandle));
        mNativeStepsNotificationSender.setNotificationSenderForTesting(mNotificationSender);
    }

    @After
    public void tearDown() {
        clearInvocations(mPreferenceHelper, mNotificationSender);
    }

    @Test
    public void sendNotification_success() {
        mNativeStepsNotificationSender.sendNotification(mUserHandle);
        verify(mNotificationSender)
                .sendNotificationAsUser(mNotificationCaptor.capture(), eq(mUserHandle));

        Notification notification = mNotificationCaptor.getValue();
        assertThat(notification.extras.getString(EXTRA_TITLE)).isEqualTo(NOTIFICATION_TITLE);
        assertThat(notification.extras.getString(EXTRA_BIG_TEXT)).isEqualTo(NOTIFICATION_CONTENT);
        assertThat(notification.actions).isNull();
    }

    @Test
    public void sendNotification_notificationStateUpdated() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(String.valueOf(false));

        mNativeStepsNotificationSender.sendNotification(mUserHandle);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREF_KEY), eq(String.valueOf(true)));
    }

    @Test
    public void sendNotification_channelBlocked_noOp() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(String.valueOf(false));
        when(mNotificationSender.sendNotificationAsUser(any(), eq(mUserHandle))).thenReturn(false);

        mNativeStepsNotificationSender.sendNotification(mUserHandle);

        verify(mPreferenceHelper, never()).insertOrReplacePreference(any(), any());
    }
}
