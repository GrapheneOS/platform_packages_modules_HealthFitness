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

import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_HIDE;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;

import static com.android.server.healthconnect.HealthConnectDailyService.EXTRA_JOB_NAME_KEY;
import static com.android.server.healthconnect.HealthConnectDailyService.EXTRA_USER_ID;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_COMPLETE_JOB_NAME;

import static java.util.Objects.requireNonNull;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.health.connect.HealthConnectOnboardingState;
import android.os.PersistableBundle;
import android.os.UserHandle;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.HealthConnectDailyService;

import java.time.Duration;

/**
 * Defines jobs related to Health Connect onboarding.
 *
 * @hide
 */
public final class OnboardingNotificationJob {
    public static final String ONBOARDING_NOTIFICATION_JOB_NAME = "onboarding_notification_job";
    public static final String ONBOARDING_NOTIFICATION_JOB_NAMESPACE =
            "HEALTH_CONNECT_ONBOARDING_NOTIFICATION_JOB";
    private static final int MIN_JOB_ID = OnboardingNotificationJob.class.hashCode();

    /** Schedule the onboarding notification job if it's not yet scheduled. */
    public static void scheduleJobIfNotScheduled(UserHandle userHandle, Context context) {
        if (!Flags.onboarding()) {
            return;
        }
        if (!requireNonNull(context.getSystemService(JobScheduler.class))
                .forNamespace(ONBOARDING_NOTIFICATION_JOB_NAMESPACE)
                .getAllPendingJobs()
                .isEmpty()) {
            return;
        }
        scheduleOnboardingNotificationJob(userHandle, context);
    }

    /** Cancel the onboarding notification job */
    public static void cancelAllJobs(Context context) {
        requireNonNull(context.getSystemService(JobScheduler.class))
                .forNamespace(ONBOARDING_NOTIFICATION_JOB_NAMESPACE)
                .cancelAll();
    }

    /** Schedule the onboarding notification job. */
    private static void scheduleOnboardingNotificationJob(UserHandle userHandle, Context context) {
        if (!Flags.onboarding()) {
            return;
        }
        ComponentName componentName = new ComponentName(context, HealthConnectDailyService.class);
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userHandle.getIdentifier());
        extras.putString(EXTRA_JOB_NAME_KEY, MIGRATION_COMPLETE_JOB_NAME);
        JobInfo.Builder builder =
                new JobInfo.Builder(MIN_JOB_ID + userHandle.getIdentifier(), componentName)
                        .setPeriodic(Duration.ofDays(1).toMillis(), Duration.ofHours(6).toMillis())
                        .setExtras(extras);
        HealthConnectDailyService.schedule(
                requireNonNull(context.getSystemService(JobScheduler.class))
                        .forNamespace(ONBOARDING_NOTIFICATION_JOB_NAMESPACE),
                userHandle,
                builder.build());
    }

    /**
     * Execute the onboarding notification job to update onboarding state and send notification if
     * required.
     */
    public static void executeOnboardingNotificationJob(
            OnboardingStateManager onboardingStateManager) {
        // TODO(b/404803574): Don't show notification if users have dismissed it

        @HealthConnectOnboardingState.OnboardingState
        int onboardingState = onboardingStateManager.updateAndGetOnboardingState();

        switch (onboardingState) {
            case ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED:
                // TODO(b/403257033): send notification
                break;
            case ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED:
                // TODO(b/403257033): send notification
                break;
            case ONBOARDING_BANNER_STATE_HIDE:
            default:
                // fall out
        }
    }
}
