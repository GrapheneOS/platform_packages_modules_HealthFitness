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

import static android.health.connect.HealthConnectManager.ACTION_SYNC_MORE_APPS;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_HIDE;

import static com.android.server.healthconnect.logging.NotificationStatsLogger.ACTION_NOTIFICATION_CLICKED;
import static com.android.server.healthconnect.logging.NotificationStatsLogger.ACTION_NOTIFICATION_DISMISSED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.health.connect.Constants;
import android.health.connect.HealthConnectManager;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.BackgroundThread;
import com.android.server.healthconnect.logging.NotificationStatsLogger;

/**
 * Receiver class for onboarding notification clicked or dismissed events.
 *
 * @hide
 */
public final class HealthConnectOnboardingReceiver extends BroadcastReceiver {
    /**
     * Broadcast Action: Health Connect notification is dismissed.
     *
     * <p class="note">This is a protected intent that can only be sent by the system.
     */
    static final String ACTION_ONBOARDING_NOTIFICATION_DISMISSED =
            "android.health.connect.action.ONBOARDING_NOTIFICATION_DISMISSED";

    /**
     * Broadcast Action: Health Connect notification is clicked.
     *
     * <p class="note">This is a protected intent that can only be sent by the system.
     */
    static final String ACTION_ONBOARDING_NOTIFICATION_CLICKED =
            "android.health.connect.action.ONBOARDING_NOTIFICATION_CLICKED";

    static final String EXTRA_ONBOARDING_STATE = "android.health.connect.extra.ONBOARDING_STATE";

    private static final String TAG = "HealthConnectOnboardingReceiver";
    private static final IntentFilter sPackageFilter = buildPackageChangeFilter();

    private final NotificationStatsLogger mNotificationStatsLogger;

    public HealthConnectOnboardingReceiver(NotificationStatsLogger notificationStatsLogger) {
        mNotificationStatsLogger = notificationStatsLogger;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Flags.onboarding()) {
            return;
        }
        String action = intent.getAction();
        if (Constants.DEBUG) {
            Slog.d(TAG, "HCOnboardingReceiver onReceive: " + action);
        }
        if (action == null) {
            return;
        }
        int onboardingState =
                intent.getIntExtra(EXTRA_ONBOARDING_STATE, ONBOARDING_BANNER_STATE_HIDE);
        if (action.equals(ACTION_ONBOARDING_NOTIFICATION_DISMISSED)) {
            Slog.d(TAG, "Onboarding notification dismissed, onboarding state: " + onboardingState);
            mNotificationStatsLogger.logAction(onboardingState, ACTION_NOTIFICATION_DISMISSED);
        } else if (action.equals(ACTION_ONBOARDING_NOTIFICATION_CLICKED)) {
            Slog.d(TAG, "Onboarding notification clicked, onboarding state: " + onboardingState);
            context.startActivity(getIntentForOnboardingFlow(context));
            mNotificationStatsLogger.logAction(onboardingState, ACTION_NOTIFICATION_CLICKED);
        }
    }

    /**
     * Register broadcast receiver to track onboarding notification interactions.
     *
     * @hide
     */
    public void registerBroadcastReceiver(Context context) {
        context.registerReceiverForAllUsers(
                this,
                sPackageFilter,
                null,
                BackgroundThread.getHandler(),
                Context.RECEIVER_NOT_EXPORTED);
    }

    private Intent getIntentForOnboardingFlow(Context context) {
        Intent intent = new Intent(ACTION_SYNC_MORE_APPS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null
                ? intent
                : new Intent(HealthConnectManager.ACTION_HEALTH_HOME_SETTINGS);
    }

    private static IntentFilter buildPackageChangeFilter() {
        IntentFilter filter = new IntentFilter(ACTION_ONBOARDING_NOTIFICATION_DISMISSED);
        filter.addAction(ACTION_ONBOARDING_NOTIFICATION_CLICKED);
        return filter;
    }
}
