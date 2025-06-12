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
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_UNKNOWN;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_HIDE;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;

import static com.android.server.healthconnect.notifications.NotificationStatsLogger.ACTION_NOTIFICATION_CLICKED;
import static com.android.server.healthconnect.notifications.NotificationStatsLogger.ACTION_NOTIFICATION_DISMISSED;
import static com.android.server.healthconnect.notifications.NotificationStatsLogger.ACTION_NOTIFICATION_SENT;
import static com.android.server.healthconnect.notifications.NotificationStatsLogger.onboardingStateToNotificationId;
import static com.android.server.healthconnect.notifications.NotificationStatsTestUtils.verifyEventLogged;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.HealthFitnessStatsLog;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NotificationStatsLoggerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    private NotificationStatsLogger mLogger;

    @Before
    public void setUp() {
        mLogger = new NotificationStatsLogger(mHealthFitnessStatsLog);
    }

    @Test
    public void logAction_logged() {
        List<Pair<Integer, Integer>> validOnboardingStateToActionPairs =
                List.of(
                        Pair.create(
                                ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED,
                                ACTION_NOTIFICATION_SENT),
                        Pair.create(
                                ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED,
                                ACTION_NOTIFICATION_SENT),
                        Pair.create(
                                ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED,
                                ACTION_NOTIFICATION_CLICKED),
                        Pair.create(
                                ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED,
                                ACTION_NOTIFICATION_CLICKED),
                        Pair.create(
                                ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED,
                                ACTION_NOTIFICATION_DISMISSED),
                        Pair.create(
                                ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED,
                                ACTION_NOTIFICATION_DISMISSED));
        validOnboardingStateToActionPairs.forEach(
                pair -> {
                    int onboardingState = pair.first;
                    int notificationAction = pair.second;
                    mLogger.logAction(onboardingState, notificationAction);

                    int notificationId = onboardingStateToNotificationId(onboardingState);
                    verifyEventLogged(mHealthFitnessStatsLog, notificationId, notificationAction);
                });
    }

    @Test
    public void logAction_onboardingStateHide_throws() {
        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mLogger.logAction(
                                        ONBOARDING_BANNER_STATE_HIDE, ACTION_NOTIFICATION_SENT));
        assertThat(thrown).hasMessageThat().isEqualTo("We don't send notification for hide state");
    }

    @Test
    public void logChannelBlocked_logged() {
        mLogger.logChannelBlocked();
        verifyEventLogged(
                mHealthFitnessStatsLog,
                HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ID__NOTIFICATION_ID_UNKNOWN,
                HEALTH_CONNECT_NOTIFICATION__NOTIFICATION_ACTION__NOTIFICATION_ACTION_CHANNEL_BLOCKED);
    }
}
