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

package com.android.server.healthconnect.notifications;

import static android.health.connect.Constants.NOTIFICATION_CHANNEL_ID;

import static com.google.common.truth.Truth.assertThat;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class NotificationUtilsTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final Bitmap BITMAP = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    private static final Icon APP_ICON = Icon.createWithBitmap(BITMAP);

    private NotificationUtils mNotificationUtils;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mNotificationUtils = new NotificationUtils(context, NOTIFICATION_CHANNEL_ID);
    }

    @Test
    public void createNotificationOnlyTitle_textSetCorrectly() {
        String expectedTitle = "test_title";
        Notification notification =
                mNotificationUtils.createNotificationOnlyTitle(expectedTitle, APP_ICON).build();

        assertThat(notification.extras.getString(Notification.EXTRA_TITLE))
                .isEqualTo(expectedTitle);
        assertThat(notification.getSmallIcon()).isEqualTo(APP_ICON);
    }

    @Test
    public void createNotificationTitleAndBodyText_textSetCorrectly() {
        String expectedTitle = "test_title";
        String expectedContent = "test_body";
        Notification notification =
                mNotificationUtils
                        .createNotificationTitleAndBodyText(
                                expectedTitle, expectedContent, APP_ICON)
                        .build();

        assertThat(notification.extras.getString(Notification.EXTRA_TITLE))
                .isEqualTo(expectedTitle);
        assertThat(notification.extras.getString(Notification.EXTRA_BIG_TEXT))
                .isEqualTo(expectedContent);
        assertThat(notification.getSmallIcon()).isEqualTo(APP_ICON);
    }
}
