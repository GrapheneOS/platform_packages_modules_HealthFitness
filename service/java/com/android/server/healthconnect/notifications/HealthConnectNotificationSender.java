/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.health.HealthFitnessStatsLog;
import android.os.Binder;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.logging.NotificationStatsLogger;
import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;

import java.util.Objects;

/**
 * Abstraction for the Health Connect NotificationSender
 *
 * @hide
 */
public final class HealthConnectNotificationSender {

    private static final String TAG = "HCNotificationSender";

    private final Context mContext;
    private final HealthConnectResourcesContext mResourcesContext;
    private final int mFixedNotificationId;
    private final String mNotificationTag;
    private final String mChannelId;
    private final String mChannelGroupId;
    private final String mChannelNameResource;
    private final String mChannelGroupNameResource;
    private final boolean mIsEnabled;
    private final NotificationStatsLogger mNotificationStatsLogger;

    private HealthConnectNotificationSender(Builder builder) {
        if (builder.mContext == null
                || builder.mResourcesContext == null
                || builder.mNotificationTag == null
                || builder.mChannelId == null
                || builder.mChannelGroupId == null
                || builder.mChannelNameResource == null
                || builder.mChannelGroupNameResource == null) {
            throw new IllegalArgumentException("Values cannot be null");
        }
        this.mContext = builder.mContext;
        this.mResourcesContext = builder.mResourcesContext;
        this.mFixedNotificationId = builder.mFixedNotificationId;
        this.mNotificationTag = builder.mNotificationTag;
        this.mChannelId = builder.mChannelId;
        this.mChannelGroupId = builder.mChannelGroupId;
        this.mChannelNameResource = builder.mChannelNameResource;
        this.mChannelGroupNameResource = builder.mChannelGroupNameResource;
        this.mIsEnabled = builder.mIsEnabled;
        // TODO(b/414949807): Use injector
        mNotificationStatsLogger =
                builder.mNotificationStatsLogger == null
                        ? new NotificationStatsLogger(new HealthFitnessStatsLog())
                        : builder.mNotificationStatsLogger;
    }

    public static final class Builder {
        @Nullable private Context mContext;
        @Nullable private HealthConnectResourcesContext mResourcesContext;
        private int mFixedNotificationId;
        @Nullable private String mNotificationTag;
        @Nullable private String mChannelId;
        @Nullable private String mChannelGroupId;
        @Nullable private String mChannelNameResource;
        @Nullable private String mChannelGroupNameResource;
        @Nullable private NotificationStatsLogger mNotificationStatsLogger;
        private boolean mIsEnabled = false;

        /** provide notification sender with context */
        public Builder setContext(Context context) {
            this.mContext = context;
            return this;
        }

        /** provide notification sender with resource context */
        public Builder setResourcesContext(HealthConnectResourcesContext resourcesContext) {
            this.mResourcesContext = resourcesContext;
            return this;
        }

        /**
         * set the current status of the notification sender e.g. may want to only enable
         * notifications if a specific Flag is enabled
         */
        public Builder setIsEnabled(boolean isEnabled) {
            this.mIsEnabled = isEnabled;
            return this;
        }

        /** set notification ID */
        public Builder setFixedNotificationId(int fixedNotificationId) {
            this.mFixedNotificationId = fixedNotificationId;
            return this;
        }

        /** set the identifying tag for notifications */
        public Builder setNotificationTag(String notificationTag) {
            this.mNotificationTag = notificationTag;
            return this;
        }

        /** set the notification channel ID */
        public Builder setChannelId(String channelId) {
            this.mChannelId = channelId;
            return this;
        }

        /** set the notification channel group ID */
        public Builder setChannelGroupId(String channelGroupId) {
            this.mChannelGroupId = channelGroupId;
            return this;
        }

        /** set the name of the notification channel */
        public Builder setChannelNameResource(String channelNameResource) {
            this.mChannelNameResource = channelNameResource;
            return this;
        }

        /** set the name of the notification channel group */
        public Builder setChannelGroupNameResource(String channelGroupNameResource) {
            this.mChannelGroupNameResource = channelGroupNameResource;
            return this;
        }

        /** set notification state logger for testing */
        @VisibleForTesting
        public Builder setLoggerForTesting(NotificationStatsLogger logger) {
            this.mNotificationStatsLogger = logger;
            return this;
        }

        /** build the notification sender */
        public HealthConnectNotificationSender build() {
            if (mContext == null) {
                throw new IllegalArgumentException("mContext cannot be null");
            }
            if (mResourcesContext == null) {
                throw new IllegalArgumentException("mResourcesContext cannot be null");
            }
            if (mNotificationTag == null) {
                throw new IllegalArgumentException("mNotificationTag cannot be null");
            }
            if (mChannelId == null) {
                throw new IllegalArgumentException("mChannelId cannot be null");
            }
            if (mChannelGroupId == null) {
                throw new IllegalArgumentException("mChannelGroupId cannot be null");
            }
            if (mChannelNameResource == null) {
                throw new IllegalArgumentException("mChannelName cannot be null");
            }
            if (mChannelGroupNameResource == null) {
                throw new IllegalArgumentException("mChannelGroupName cannot be null");
            }
            return new HealthConnectNotificationSender(this);
        }
    }

    /**
     * Sends the passed-in {@code notification} to the user.
     *
     * <p>Attempting to send a notification doesn't guarantee a successful delivery. For example,
     * the user could block notification channel from settings, in which case the notification would
     * not actually be posted by the system.
     *
     * @return true if the notification is actually sent, false otherwise.
     */
    public boolean sendNotificationAsUser(Notification notification, UserHandle userHandle) {
        Slog.i(TAG, "Sending notification as user.");

        if (!mIsEnabled) {
            Slog.i(TAG, "Notifications have been disabled.");
            return false;
        }

        final long callingId = Binder.clearCallingIdentity();
        NotificationManager notificationManager = getNotificationManagerForUser(userHandle);

        try {
            try {
                createNotificationChannel(notificationManager);
            } catch (Throwable e) {
                Slog.w(TAG, "Unable to create notification channel", e);
                return false;
            }

            try {
                if (isChannelBlocked(notificationManager, notification.getChannelId())) {
                    mNotificationStatsLogger.logChannelBlocked();
                    return false;
                }
            } catch (Throwable e) {
                Slog.w(TAG, "Unable to get notification channel", e);
                return false;
            }

            try {
                notificationManager.notify(mNotificationTag, mFixedNotificationId, notification);
            } catch (Throwable e) {
                Slog.w(TAG, "Unable to send system notification", e);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }

        return true;
    }

    /** Cancels all Health Connect notifications on this channel. */
    public void clearNotificationsAsUser(UserHandle userHandle) {
        if (!mIsEnabled) return;
        NotificationManager notificationManager = getNotificationManagerForUser(userHandle);
        cancelFromSystem(notificationManager);
    }

    /** Returns a {@link NotificationManager} which will send notifications to the given user. */
    private NotificationManager getNotificationManagerForUser(UserHandle userHandle) {
        Context contextAsUser = mContext.createContextAsUser(userHandle, 0);
        return Objects.requireNonNull(contextAsUser.getSystemService(NotificationManager.class));
    }

    private void cancelFromSystem(NotificationManager notificationManager) {
        final long callingId = Binder.clearCallingIdentity();
        try {
            notificationManager.cancel(mNotificationTag, mFixedNotificationId);
        } catch (Throwable e) {
            Log.w(TAG, "Unable to cancel system notification", e);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        CharSequence channelGroupName =
                mResourcesContext.getStringByNameOrThrow(mChannelGroupNameResource);
        CharSequence channelName = mResourcesContext.getStringByNameOrThrow(mChannelNameResource);

        NotificationChannelGroup group =
                new NotificationChannelGroup(mChannelGroupId, channelGroupName);

        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel notificationChannel =
                new NotificationChannel(mChannelId, channelName, importance);
        notificationChannel.setGroup(mChannelGroupId);
        notificationChannel.setBlockable(true);

        notificationManager.createNotificationChannelGroup(group);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    private boolean isChannelBlocked(NotificationManager notificationManager, String channelId) {
        NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
        if (channel.getImportance() == IMPORTANCE_NONE) {
            Slog.i(TAG, "Notifications channel " + channel.getName() + " is blocked by user");
            return true;
        }
        return false;
    }

    /** @hide */
    public @interface HealthConnectNotificationType {}
}
