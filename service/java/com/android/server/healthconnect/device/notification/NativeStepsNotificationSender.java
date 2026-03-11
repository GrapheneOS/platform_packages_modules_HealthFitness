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

import static android.health.connect.Constants.APP_ICON_DRAWABLE_NAME;
import static android.health.connect.Constants.CHANNEL_GROUP_ID;
import static android.health.connect.Constants.CHANNEL_GROUP_NAME_RESOURCE;
import static android.health.connect.Constants.CHANNEL_NAME_RESOURCE;
import static android.health.connect.Constants.NOTIFICATION_CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.health.connect.HealthConnectManager;
import android.os.Binder;
import android.os.UserHandle;
import android.util.Slog;

import androidx.annotation.VisibleForTesting;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.notifications.NotificationUtils;

import java.util.Optional;

/**
 * Native steps specific implementation of the HealthConnectNotificationSender.
 *
 * @hide
 */
public final class NativeStepsNotificationSender {
    static final String NOTIFICATION_TITLE = "native_steps_notification_title";
    static final String NOTIFICATION_CONTENT = "native_steps_notification_content";

    // Unique random ID for step notifications, which makes sure we only have one step
    // notification at a time.
    // TODO(b/414949807): Move to a central place
    private static final int FIXED_NOTIFICATION_ID = 4129;
    private static final String NOTIFICATION_TAG = "HcNativeStepsNotificationTag";
    private static final String TAG = "NativeStepsNotificationSender";

    private final Context mContext;
    private final HealthConnectResourcesContext mResContext;
    private final NotificationUtils mNotificationUtils;
    private final NativeStepsNotificationStateManager mNotificationStateManager;

    // TODO(b/414949807): Move to NotificationUtils
    private Optional<Icon> mAppIcon = Optional.empty();
    private HealthConnectNotificationSender mHealthConnectNotificationSender;

    public NativeStepsNotificationSender(
            Context context,
            HealthConnectResourcesContext resContext,
            NativeStepsNotificationStateManager notificationStateManager) {
        mContext = context;
        mResContext = resContext;
        mNotificationUtils = new NotificationUtils(context, NOTIFICATION_CHANNEL_ID);
        mNotificationStateManager = notificationStateManager;
        mHealthConnectNotificationSender =
                new HealthConnectNotificationSender.Builder()
                        .setContext(context)
                        .setResourcesContext(resContext)
                        .setChannelGroupId(CHANNEL_GROUP_ID)
                        .setChannelNameResource(CHANNEL_NAME_RESOURCE)
                        .setChannelGroupNameResource(CHANNEL_GROUP_NAME_RESOURCE)
                        .setChannelId(NOTIFICATION_CHANNEL_ID)
                        .setFixedNotificationId(FIXED_NOTIFICATION_ID)
                        .setNotificationTag(NOTIFICATION_TAG)
                        .setIsEnabled(true)
                        .build();
    }

    /** Sends a notification for native steps available in HC. */
    public void sendNotification(UserHandle userHandle) {
        Notification notification = createNotification();
        if (mHealthConnectNotificationSender.sendNotificationAsUser(notification, userHandle)) {
            mNotificationStateManager.disable();
        }
    }

    private Notification createNotification() {
        return mNotificationUtils
                .createNotificationTitleAndBodyText(
                        mResContext.getStringByNameOrThrow(NOTIFICATION_TITLE),
                        mResContext.getStringByNameOrThrow(NOTIFICATION_CONTENT),
                        getAppIcon().orElse(null))
                .setContentIntent(getNativeStepsInfoPendingIntent())
                .setAutoCancel(true)
                .build();
    }

    // TODO(b/414949807): Use injector
    @VisibleForTesting
    void setNotificationSenderForTesting(HealthConnectNotificationSender notificationSender) {
        mHealthConnectNotificationSender = notificationSender;
    }

    // TODO(b/414949807): Move to NotificationUtils
    /** Returns an {@link Icon} to be displayed on the notification. */
    private Optional<Icon> getAppIcon() {
        if (mAppIcon.isEmpty()) {
            Icon maybeIcon = mResContext.getIconByDrawableName(APP_ICON_DRAWABLE_NAME);
            mAppIcon = maybeIcon == null ? Optional.empty() : Optional.of(maybeIcon);
        }
        return mAppIcon;
    }

    private PendingIntent getNativeStepsInfoPendingIntent() {
        // TODO(b/435354542): Navigate directly to device management
        Intent intent = new Intent(HealthConnectManager.ACTION_HEALTH_HOME_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final long callingId = Binder.clearCallingIdentity();
        try {
            return PendingIntent.getActivity(
                    mContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }
}
