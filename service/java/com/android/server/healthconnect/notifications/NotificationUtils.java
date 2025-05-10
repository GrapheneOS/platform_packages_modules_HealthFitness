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

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Icon;

import androidx.annotation.Nullable;

/**
 * Utility methods for facilitating health connect notifications.
 *
 * @hide
 */
public final class NotificationUtils {
    private final Context mContext;
    private final String mChannelId;

    public NotificationUtils(Context context, String channelId) {
        mContext = context;
        mChannelId = channelId;
    }

    /** Creates a notification with title and an optional icon. */
    public Notification.Builder createNotificationOnlyTitle(String title, @Nullable Icon icon) {
        Notification.Builder notificationBuilder =
                new Notification.Builder(mContext, mChannelId)
                        .setContentTitle(title)
                        .setAutoCancel(true);
        if (icon != null) {
            notificationBuilder.setSmallIcon(icon);
        }
        return notificationBuilder;
    }

    /** Creates a notification with title and body, and an optional icon. */
    public Notification.Builder createNotificationTitleAndBodyText(
            String notificationTitle, String notificationTextBody, @Nullable Icon icon) {
        Notification.Builder notificationBuilder =
                createNotificationOnlyTitle(notificationTitle, icon);
        notificationBuilder.setStyle(new Notification.BigTextStyle().bigText(notificationTextBody));
        return notificationBuilder;
    }
}
