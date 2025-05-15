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

import static android.health.connect.Constants.APP_ICON_DRAWABLE_NAME;
import static android.health.connect.Constants.CHANNEL_GROUP_ID;
import static android.health.connect.Constants.CHANNEL_GROUP_NAME_RESOURCE;
import static android.health.connect.Constants.CHANNEL_NAME_RESOURCE;
import static android.health.connect.Constants.NOTIFICATION_CHANNEL_ID;
import static android.health.connect.HealthConnectManager.ACTION_SYNC_MORE_APPS;

import static com.android.server.healthconnect.notifications.NotificationUtils.getPendingIntent;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.notifications.NotificationUtils;

import java.util.Optional;

/**
 * Onboarding specific implementation of the HealthConnectNotificationSender.
 *
 * @hide
 */
public final class OnboardingNotificationSender {
    @VisibleForTesting
    static final String START_USING_HC_NOTIFICATION_TITLE = "start_using_hc_banner_title";

    @VisibleForTesting
    static final String START_USING_HC_NOTIFICATION_CONTENT = "start_using_hc_banner_content";

    @VisibleForTesting
    static final String START_USING_HC_NOTIFICATION_BUTTON = "start_using_hc_set_up_button";

    @VisibleForTesting
    static final String CONNECT_MORE_APPS_NOTIFICATION_TITLE = "connect_more_apps_banner_title";

    @VisibleForTesting
    static final String CONNECT_MORE_APPS_NOTIFICATION_CONTENT = "connect_more_apps_banner_content";

    @VisibleForTesting
    static final String CONNECT_MORE_APPS_NOTIFICATION_BUTTON = "connect_more_apps_set_up_button";

    // Unique random ID for onboarding notifications, which makes sure we only have one onboarding
    // notification at a time.
    // TODO(b/414949807): Move to a central place
    private static final int FIXED_NOTIFICATION_ID = 9878;
    private static final String NOTIFICATION_TAG = "HcOnboardingTag";

    private static final Intent FALLBACK_INTENT =
            new Intent(HealthConnectManager.ACTION_HEALTH_HOME_SETTINGS);

    private final Context mContext;
    private final HealthConnectResourcesContext mResContext;
    private final NotificationUtils mNotificationUtils;

    // TODO(b/414949807): Move to NotificationUtils
    private Optional<Icon> mAppIcon = Optional.empty();
    private HealthConnectNotificationSender mHealthConnectNotificationSender;

    public OnboardingNotificationSender(Context context, HealthConnectResourcesContext resContext) {
        mContext = context;
        mResContext = resContext;
        mNotificationUtils = new NotificationUtils(context, NOTIFICATION_CHANNEL_ID);
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

    // TODO(b/414949807): Use injector
    @VisibleForTesting
    void setNotificationSenderForTesting(HealthConnectNotificationSender notificationSender) {
        mHealthConnectNotificationSender = notificationSender;
    }

    /** Sends a notification for onboarding scenario where there's no app connected to HC. */
    public void sendNoAppConnectedNotification(UserHandle userHandle) {
        mHealthConnectNotificationSender.sendNotificationAsUser(
                getNoAppConnectedNotification(), userHandle);
    }

    /** Sends a notification for onboarding scenario where there's one app connected to HC. */
    public void sendOneAppConnectedNotification(UserHandle userHandle) {
        mHealthConnectNotificationSender.sendNotificationAsUser(
                getOneAppConnectedNotification(), userHandle);
    }

    private Notification getNoAppConnectedNotification() {
        String title = mResContext.getStringByNameOrThrow(START_USING_HC_NOTIFICATION_TITLE);
        String content = mResContext.getStringByNameOrThrow(START_USING_HC_NOTIFICATION_CONTENT);
        String button = mResContext.getStringByNameOrThrow(START_USING_HC_NOTIFICATION_BUTTON);

        Icon icon = getAppIcon().orElse(null);
        Notification.Action action =
                new Notification.Action.Builder(icon, button, getSyncMoreAppsPendingIntent())
                        .build();

        return mNotificationUtils
                .createNotificationTitleAndBodyText(title, content, icon)
                .setActions(action)
                .build();
    }

    private Notification getOneAppConnectedNotification() {
        String title = mResContext.getStringByNameOrThrow(CONNECT_MORE_APPS_NOTIFICATION_TITLE);
        String content = mResContext.getStringByNameOrThrow(CONNECT_MORE_APPS_NOTIFICATION_CONTENT);
        String button = mResContext.getStringByNameOrThrow(CONNECT_MORE_APPS_NOTIFICATION_BUTTON);

        Icon icon = getAppIcon().orElse(null);
        Notification.Action action =
                new Notification.Action.Builder(icon, button, getSyncMoreAppsPendingIntent())
                        .build();

        return mNotificationUtils
                .createNotificationTitleAndBodyText(title, content, icon)
                .setActions(action)
                .build();
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

    private PendingIntent getSyncMoreAppsPendingIntent() {
        Intent intent = new Intent(ACTION_SYNC_MORE_APPS);
        ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null
                ? getPendingIntent(mContext, intent)
                : getPendingIntent(mContext, FALLBACK_INTENT);
    }
}
