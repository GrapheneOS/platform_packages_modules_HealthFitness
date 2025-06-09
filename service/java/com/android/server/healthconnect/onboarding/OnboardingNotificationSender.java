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
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;

import static com.android.server.healthconnect.logging.NotificationStatsLogger.ACTION_NOTIFICATION_SENT;
import static com.android.server.healthconnect.onboarding.HealthConnectOnboardingReceiver.ACTION_ONBOARDING_NOTIFICATION_CLICKED;
import static com.android.server.healthconnect.onboarding.HealthConnectOnboardingReceiver.ACTION_ONBOARDING_NOTIFICATION_DISMISSED;
import static com.android.server.healthconnect.onboarding.HealthConnectOnboardingReceiver.EXTRA_ONBOARDING_STATE;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.health.connect.HealthConnectOnboardingState;
import android.os.UserHandle;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.logging.NotificationStatsLogger;
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
    static final String START_USING_HC_NOTIFICATION_TITLE =
            "zero_apps_onboarding_notification_title";

    @VisibleForTesting
    static final String START_USING_HC_NOTIFICATION_CONTENT =
            "zero_apps_onboarding_notification_summary";

    @VisibleForTesting
    static final String CONNECT_MORE_APPS_NOTIFICATION_TITLE =
            "one_app_onboarding_notification_title";

    @VisibleForTesting
    static final String CONNECT_MORE_APPS_NOTIFICATION_CONTENT =
            "one_app_onboarding_notification_summary";

    // Unique random ID for onboarding notifications, which makes sure we only have one onboarding
    // notification at a time.
    // TODO(b/414949807): Move to a central place
    private static final int FIXED_NOTIFICATION_ID = 9878;
    private static final String NOTIFICATION_TAG = "HcOnboardingTag";

    private final Context mContext;
    private final HealthConnectResourcesContext mResContext;
    private final NotificationUtils mNotificationUtils;
    private final OnboardingNotificationStateManager mNotificationStateManager;
    private final NotificationStatsLogger mNotificationStatsLogger;

    // TODO(b/414949807): Move to NotificationUtils
    private Optional<Icon> mAppIcon = Optional.empty();
    private HealthConnectNotificationSender mHealthConnectNotificationSender;

    public OnboardingNotificationSender(
            Context context,
            HealthConnectResourcesContext resContext,
            OnboardingNotificationStateManager notificationStateManager,
            NotificationStatsLogger notificationStatsLogger) {
        mContext = context;
        mResContext = resContext;
        mNotificationUtils = new NotificationUtils(context, NOTIFICATION_CHANNEL_ID);
        mNotificationStateManager = notificationStateManager;
        mNotificationStatsLogger = notificationStatsLogger;
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
        sendNotification(
                userHandle,
                createNoAppConnectedNotification(),
                SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION,
                ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
    }

    /** Sends a notification for onboarding scenario where there's one app connected to HC. */
    public void sendOneAppConnectedNotification(UserHandle userHandle) {
        sendNotification(
                userHandle,
                createOneAppConnectedNotification(),
                SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION,
                ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
    }

    private void sendNotification(
            UserHandle userHandle,
            Notification notification,
            int flag,
            @HealthConnectOnboardingState.OnboardingState int onboardingState) {
        if (!Flags.onboardingNotification()) {
            return;
        }
        if (mHealthConnectNotificationSender.sendNotificationAsUser(notification, userHandle)) {
            mNotificationStateManager.unsetFlags(flag);
            mNotificationStatsLogger.logAction(onboardingState, ACTION_NOTIFICATION_SENT);
        }
    }

    private Notification createNoAppConnectedNotification() {
        return createNotification(
                mResContext.getStringByNameOrThrow(START_USING_HC_NOTIFICATION_TITLE),
                mResContext.getStringByNameOrThrow(START_USING_HC_NOTIFICATION_CONTENT),
                ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
    }

    private Notification createOneAppConnectedNotification() {
        return createNotification(
                mResContext.getStringByNameOrThrow(CONNECT_MORE_APPS_NOTIFICATION_TITLE),
                mResContext.getStringByNameOrThrow(CONNECT_MORE_APPS_NOTIFICATION_CONTENT),
                ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
    }

    private Notification createNotification(
            String title,
            String content,
            @HealthConnectOnboardingState.OnboardingState int onboardingState) {
        return mNotificationUtils
                .createNotificationTitleAndBodyText(title, content, getAppIcon().orElse(null))
                .setContentIntent(
                        getBroadcastPendingIntent(
                                ACTION_ONBOARDING_NOTIFICATION_CLICKED, onboardingState))
                .setDeleteIntent(
                        getBroadcastPendingIntent(
                                ACTION_ONBOARDING_NOTIFICATION_DISMISSED, onboardingState))
                .setAutoCancel(true)
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

    private PendingIntent getBroadcastPendingIntent(
            String action, @HealthConnectOnboardingState.OnboardingState int onboardingState) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ONBOARDING_STATE, onboardingState);
        return PendingIntent.getBroadcast(
                mContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
