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

package com.android.server.healthconnect;

import static com.android.server.healthconnect.HealthConnectDailyService.EXTRA_JOB_NAME_KEY;
import static com.android.server.healthconnect.HealthConnectDailyService.EXTRA_USER_ID;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.health.HealthFitnessStatsLog;
import android.os.PersistableBundle;
import android.os.UserHandle;

import com.android.server.healthconnect.logging.DailyLoggingService;
import com.android.server.healthconnect.logging.EcosystemStatsCollector;
import com.android.server.healthconnect.logging.UsageStatsCollector;
import com.android.server.healthconnect.storage.DailyCleanupJob;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseStatsCollector;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** @hide */
public class HealthConnectDailyJobs {
    public static final String HC_DAILY_JOB = "hc_daily_job";
    private static final int MIN_JOB_ID = HealthConnectDailyJobs.class.hashCode();
    private static final long JOB_RUN_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private static final String HEALTH_CONNECT_NAMESPACE = "HEALTH_CONNECT_DAILY_JOB";

    /** Schedule the daily job */
    public static void schedule(Context context, UserHandle userHandle) {
        ComponentName componentName = new ComponentName(context, HealthConnectDailyService.class);
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userHandle.getIdentifier());
        extras.putString(EXTRA_JOB_NAME_KEY, HC_DAILY_JOB);
        JobInfo.Builder builder =
                new JobInfo.Builder(MIN_JOB_ID + userHandle.getIdentifier(), componentName)
                        .setExtras(extras)
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .setPeriodic(JOB_RUN_INTERVAL, JOB_RUN_INTERVAL / 2);

        HealthConnectDailyService.schedule(
                Objects.requireNonNull(context.getSystemService(JobScheduler.class))
                        .forNamespace(HEALTH_CONNECT_NAMESPACE),
                userHandle,
                builder.build());
    }

    public static void cancelAllJobs(Context context) {
        Objects.requireNonNull(context.getSystemService(JobScheduler.class))
                .forNamespace(HEALTH_CONNECT_NAMESPACE)
                .cancelAll();
    }

    /** Auto deletes the data and uploads critical daily metrics. */
    public static void execute(
            UsageStatsCollector usageStatsCollector,
            DatabaseStatsCollector databaseStatsCollector,
            DailyCleanupJob dailyCleanupJob,
            EcosystemStatsCollector ecosystemStatsCollector,
            HealthFitnessStatsLog statsLog) {
        dailyCleanupJob.startDailyCleanup();
        DailyLoggingService.logDailyMetrics(
                usageStatsCollector, databaseStatsCollector,
                ecosystemStatsCollector, statsLog);
    }
}
