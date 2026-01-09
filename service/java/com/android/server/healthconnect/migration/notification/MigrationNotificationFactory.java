/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.migration.notification;

import static com.android.server.healthconnect.migration.notification.MigrationNotificationSender.NOTIFICATION_TYPE_MIGRATION_MODULE_UPDATE_NEEDED;
import static com.android.server.healthconnect.migration.notification.MigrationNotificationSender.NOTIFICATION_TYPE_MIGRATION_PAUSED;

import android.annotation.Nullable;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.os.Binder;

import androidx.annotation.VisibleForTesting;

/**
 * A factory for creating Health Connect Migration notifications.
 *
 * @hide
 */
// TODO(b/352602201): Refactor to use new HealthConnectNotificationFactory
public class MigrationNotificationFactory {
    private final Context mContext;
    private final HealthConnectResourcesContext mResContext;
    @Nullable private Icon mAppIcon;

    // String names used to fetch resources
    private static final String MIGRATION_MODULE_UPDATE_NEEDED_NOTIFICATION_TITLE =
            "migration_module_update_needed_notification_title";
    private static final String MIGRATION_UPDATE_NEEDED_NOTIFICATION_CONTENT =
            "migration_update_needed_notification_content";

    private static final String MIGRATION_PAUSED_NOTIFICATION_TITLE =
            "migration_paused_notification_title";
    private static final String MIGRATION_PAUSED_NOTIFICATION_CONTENT =
            "migration_paused_notification_content";

    private static final String SHOW_MIGRATION_INFO_ACTION =
            "android.health.connect.action.SHOW_MIGRATION_INFO";
    private static final String MODULE_UPDATE_ACTION = "android.settings.MODULE_UPDATE_SETTINGS";
    private static final String SYSTEM_SETTINGS_FALLBACK_ACTION = "android.settings.SETTINGS";
    private static final Intent FALLBACK_INTENT = new Intent(SYSTEM_SETTINGS_FALLBACK_ACTION);

    @VisibleForTesting static final String APP_ICON_DRAWABLE_NAME = "health_connect_logo";

    public MigrationNotificationFactory(
            Context context, HealthConnectResourcesContext resourcesContext) {
        mContext = context;
        mResContext = resourcesContext;
    }

    /**
     * Creates a notification based on the passed notificationType and assigns it the correct
     * channel ID.
     */
    public Notification createNotification(
            @MigrationNotificationSender.MigrationNotificationType int notificationType,
            String channelId)
            throws IllegalMigrationNotificationStateException {
        Notification notification;

        switch (notificationType) {
            case NOTIFICATION_TYPE_MIGRATION_MODULE_UPDATE_NEEDED:
                notification = getModuleUpdateNeededNotification(channelId);
                break;
            case NOTIFICATION_TYPE_MIGRATION_PAUSED:
                notification = getMigrationPausedNotification(channelId);
                break;
            default:
                throw new IllegalMigrationNotificationStateException(
                        "Notification type not supported");
        }

        return notification;
    }

    /** Retrieves a string resource by name from the Health Connect resources. */
    @Nullable
    public String getStringResource(String name) {
        return mResContext.getStringByName(name);
    }

    private Notification getModuleUpdateNeededNotification(String channelId) {
        PendingIntent pendingIntent = getSystemUpdatePendingIntent();

        String notificationTitle =
                getStringResource(MIGRATION_MODULE_UPDATE_NEEDED_NOTIFICATION_TITLE);
        String notificationContent =
                getStringResource(MIGRATION_UPDATE_NEEDED_NOTIFICATION_CONTENT);

        Notification notification =
                new Notification.Builder(mContext, channelId)
                        .setSmallIcon(getAppIcon())
                        .setContentTitle(notificationTitle)
                        .setContentText(notificationContent)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

        return notification;
    }

    private Notification getMigrationPausedNotification(String channelId) {
        PendingIntent pendingIntent = getMigrationInfoPendingIntent();
        String notificationTitle = getStringResource(MIGRATION_PAUSED_NOTIFICATION_TITLE);
        String notificationContent = getStringResource(MIGRATION_PAUSED_NOTIFICATION_CONTENT);
        Notification notification =
                new Notification.Builder(mContext, channelId)
                        .setSmallIcon(getAppIcon())
                        .setContentTitle(notificationTitle)
                        .setContentText(notificationContent)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

        return notification;
    }

    @Nullable
    private PendingIntent getPendingIntent(Intent intent) {
        // This call requires Binder identity to be cleared for getIntentSender() to be allowed to
        // send as another package.
        final long callingId = Binder.clearCallingIdentity();
        try {
            return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    @Nullable
    private PendingIntent getMigrationInfoPendingIntent() {
        Intent intent = new Intent(SHOW_MIGRATION_INFO_ACTION);
        ResolveInfo result = mContext.getPackageManager().resolveActivity(intent, 0);
        if (result == null) {
            return getPendingIntent(FALLBACK_INTENT);
        }
        return getPendingIntent(intent);
    }

    @Nullable
    private PendingIntent getSystemUpdatePendingIntent() {
        Intent intent = new Intent(MODULE_UPDATE_ACTION);
        ResolveInfo result = mContext.getPackageManager().resolveActivity(intent, 0);
        if (result == null) {
            return getPendingIntent(FALLBACK_INTENT);
        }
        return getPendingIntent(intent);
    }

    @VisibleForTesting
    @Nullable
    Icon getAppIcon() {
        // Caches the first valid appIcon
        if (mAppIcon == null) {
            mAppIcon = mResContext.getIconByDrawableName(APP_ICON_DRAWABLE_NAME);
        }
        return mAppIcon;
    }

    /** Thrown when an illegal notification state is detected. */
    public static final class IllegalMigrationNotificationStateException extends Exception {
        public IllegalMigrationNotificationStateException(String message) {
            super(message);
        }
    }

    @VisibleForTesting
    public static String[] getNotificationStringResources() {
        // Resources referenced here must be explicitly kept in apk/res/raw/keep.xml to avoid
        // removal during shrinking
        return new String[] {
            MIGRATION_MODULE_UPDATE_NEEDED_NOTIFICATION_TITLE,
            MIGRATION_UPDATE_NEEDED_NOTIFICATION_CONTENT,
            MIGRATION_PAUSED_NOTIFICATION_TITLE,
            MIGRATION_PAUSED_NOTIFICATION_CONTENT,
        };
    }
}
