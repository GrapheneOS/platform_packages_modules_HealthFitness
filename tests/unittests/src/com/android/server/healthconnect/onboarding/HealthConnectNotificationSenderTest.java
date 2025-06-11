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

import static android.health.connect.Constants.CHANNEL_GROUP_ID;
import static android.health.connect.Constants.CHANNEL_GROUP_NAME_RESOURCE;
import static android.health.connect.Constants.CHANNEL_NAME_RESOURCE;
import static android.health.connect.Constants.NOTIFICATION_CHANNEL_ID;

import static com.android.server.healthconnect.notifications.NotificationStatsTestUtils.verifyChannelBlockedLogged;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserHandle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.notifications.NotificationStatsLogger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class HealthConnectNotificationSenderTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final NotificationChannel TEST_CHANNEL =
            new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, "testChannel", NotificationManager.IMPORTANCE_HIGH);
    private static final NotificationChannel BLOCKED_CHANNEL =
            new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, "blockedChannel", NotificationManager.IMPORTANCE_NONE);

    @Mock private Context mContext;
    @Mock private HealthConnectResourcesContext mResourcesContext;
    @Mock private UserHandle mUserHandle;
    @Mock private NotificationManager mNotificationManager;
    @Mock private NotificationStatsLogger mNotificationStatsLogger;
    private HealthConnectNotificationSender mHealthConnectNotificationSender;

    @Before
    public void setUp() {
        when(mContext.getSystemService(NotificationManager.class)).thenReturn(mNotificationManager);
        when(mContext.createContextAsUser(eq(mUserHandle), anyInt())).thenReturn(mContext);
        when(mNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID))
                .thenReturn(TEST_CHANNEL);
        when(mResourcesContext.getStringByNameOrThrow(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mHealthConnectNotificationSender =
                new HealthConnectNotificationSender.Builder()
                        .setContext(mContext)
                        .setResourcesContext(mResourcesContext)
                        .setChannelGroupId(CHANNEL_GROUP_ID)
                        .setChannelNameResource(CHANNEL_NAME_RESOURCE)
                        .setChannelGroupNameResource(CHANNEL_GROUP_NAME_RESOURCE)
                        .setChannelId(NOTIFICATION_CHANNEL_ID)
                        .setFixedNotificationId(1234)
                        .setNotificationTag("test_tag")
                        .setIsEnabled(true)
                        .setLoggerForTesting(mNotificationStatsLogger)
                        .build();
    }

    @Test
    public void sendNotificationAsUser_channelBlocked_logged() {
        when(mNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID))
                .thenReturn(BLOCKED_CHANNEL);

        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        Notification dummyNotification =
                new Notification.Builder(context, NOTIFICATION_CHANNEL_ID).build();
        mHealthConnectNotificationSender.sendNotificationAsUser(dummyNotification, mUserHandle);

        verifyChannelBlockedLogged(mNotificationStatsLogger);
    }
}
