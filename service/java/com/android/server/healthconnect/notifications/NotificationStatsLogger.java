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

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_CHANNEL_BLOCKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_CLICKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_DISMISSED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_SENT;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_ONE_APP_CONNECTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_ZERO_APPS_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_HIDE;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;

import android.annotation.IntDef;
import android.health.HealthFitnessStatsLog;
import android.health.connect.HealthConnectOnboardingState;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class to log Health Connect notification stats.
 *
 * @hide
 */
public final class NotificationStatsLogger {
    public static final int ACTION_NOTIFICATION_SENT =
            HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_SENT;
    public static final int ACTION_NOTIFICATION_CLICKED =
            HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_DISMISSED;
    public static final int ACTION_NOTIFICATION_DISMISSED =
            HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_CLICKED;
    public static final int ACTION_NOTIFICATION_CHANNEL_BLOCKED =
            HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_CHANNEL_BLOCKED;

    @IntDef({
        ACTION_NOTIFICATION_SENT,
        ACTION_NOTIFICATION_CLICKED,
        ACTION_NOTIFICATION_DISMISSED,
        ACTION_NOTIFICATION_CHANNEL_BLOCKED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NotificationAction {}

    private final HealthFitnessStatsLog mStatsLog;

    public NotificationStatsLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    /**
     * Logs onboarding notification metrics.
     *
     * @hide
     */
    public void logAction(
            @HealthConnectOnboardingState.OnboardingState int onboardingState,
            @NotificationAction int notificationAction) {
        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION,
                onboardingStateToNotificationId(onboardingState),
                notificationAction);
    }

    /**
     * Logs notification channel blocked metric.
     *
     * @hide
     */
    public void logChannelBlocked() {
        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION,
                HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_UNKNOWN,
                ACTION_NOTIFICATION_CHANNEL_BLOCKED);
    }

    @VisibleForTesting
    static int onboardingStateToNotificationId(
            @HealthConnectOnboardingState.OnboardingState int onboardingState) {
        return switch (onboardingState) {
            case ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED ->
                    HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_ZERO_APPS_CONNECTED;
            case ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED ->
                    HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_ONE_APP_CONNECTED;
            case ONBOARDING_BANNER_STATE_HIDE ->
                    throw new IllegalArgumentException("We don't send notification for hide state");
            default -> throw new IllegalArgumentException("Invalid state " + onboardingState);
        };
    }
}
