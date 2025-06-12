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

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.health.HealthFitnessStatsLog;
import android.health.connect.HealthConnectOnboardingState;

/** Util class for notification stats logger tests. */
public final class NotificationStatsTestUtils {
    /**
     * Verifies the desired event is logged through {@link android.health.HealthFitnessStatsLog}.
     */
    public static void verifyEventLogged(
            HealthFitnessStatsLog logger, int notificationId, int notificationAction) {
        verify(logger)
                .write(eq(HEALTH_CONNECT_NOTIFICATION), eq(notificationId), eq(notificationAction));
    }

    /** Verifies the desired event is logged through {@link NotificationStatsLogger}. */
    public static void verifyEventLogged(
            NotificationStatsLogger logger,
            @HealthConnectOnboardingState.OnboardingState int onboardingState,
            @NotificationStatsLogger.NotificationAction int action) {
        verify(logger).logAction(eq(onboardingState), eq(action));
    }

    /**
     * Verifies the notification channel blocked event is logged through {@link
     * NotificationStatsLogger}.
     */
    public static void verifyChannelBlockedLogged(NotificationStatsLogger logger) {
        verify(logger).logChannelBlocked();
    }

    /** Verifies nothing is logged through {@link NotificationStatsLogger}. */
    public static void verifyNothingLogged(NotificationStatsLogger logger) {
        verifyNoMoreInteractions(logger);
    }
}
