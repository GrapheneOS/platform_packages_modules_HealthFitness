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

package com.android.server.healthconnect.exportimport;

import static com.android.healthfitness.flags.Flags.exportImportFastFollow;
import static com.android.healthfitness.flags.Flags.extendExportImportTelemetry;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.HealthConnectDailyService;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines jobs related to Health Connect export and imports.
 *
 * @hide
 */
public class ExportImportJobs {
    private static final String TAG = "HealthConnectExportJobs";
    private static final int MIN_JOB_ID = ExportImportJobs.class.hashCode();

    @VisibleForTesting static final String IS_FIRST_EXPORT = "is_first_export";
    @VisibleForTesting static final String NAMESPACE = "HEALTH_CONNECT_IMPORT_EXPORT_JOBS";

    public static final String PERIODIC_EXPORT_JOB_NAME = "periodic_export_job";

    /**
     * Checks if the rescheduling is needed and schedules the periodic export job if so.
     *
     * <p>Needed in particular for device restarts and user switches. The job is persisted in those
     * cases and we want to avoid rescheduling, because otherwise, if a user restarts their phone
     * often, the job may never complete.
     */
    public static void schedulePeriodicJobIfNotScheduled(
            UserHandle userHandle,
            Context context,
            ExportImportSettingsStorage exportImportSettingsStorage,
            ExportManager exportManager) {
        if (!exportImportFastFollow()
                || Objects.requireNonNull(context.getSystemService(JobScheduler.class))
                        .forNamespace(NAMESPACE)
                        .getAllPendingJobs()
                        .isEmpty()) {
            schedulePeriodicExportJob(
                    userHandle, context, exportImportSettingsStorage, exportManager);
        }
    }

    /** Schedule the periodic export job. */
    public static void schedulePeriodicExportJob(
            UserHandle userHandle,
            Context context,
            ExportImportSettingsStorage exportImportSettingsStorage,
            ExportManager exportManager) {
        int periodInDays = exportImportSettingsStorage.getScheduledExportPeriodInDays();

        if (extendExportImportTelemetry()) {
            // A new export is set up, reset the retry count for logging
            exportImportSettingsStorage.resetExportRepeatErrorOnRetryCount();
        }

        if (exportImportFastFollow()) {
            // We should always cancel the job as we are persisting the job now.
            Objects.requireNonNull(context.getSystemService(JobScheduler.class))
                    .forNamespace(NAMESPACE)
                    .cancelAll();

            // TODO(b/364855153): Move to next condition once fast follow flag is enabled.
            // If export is off we try to delete the local files, just in case it happened the
            // rare case where those files weren't deleted after the last export.
            if (periodInDays <= 0) {
                exportManager.deleteLocalExportFiles(userHandle);
            }
        }
        // If period is 0 the user has turned export off, we should no longer schedule a new job
        if (periodInDays <= 0) {
            return;
        }

        PersistableBundle extras = new PersistableBundle();
        extras.putInt(HealthConnectDailyService.EXTRA_USER_ID, userHandle.getIdentifier());
        extras.putString(HealthConnectDailyService.EXTRA_JOB_NAME_KEY, PERIODIC_EXPORT_JOB_NAME);

        long periodInMillis = Duration.ofDays(periodInDays).toMillis();
        long flexInMillis = Duration.ofHours(6).toMillis();
        if (periodInDays >= 7) {
            // For weekly / monthly export, allow export to happen at least one day before.
            flexInMillis = Duration.ofDays(1).toMillis();
        }
        if (exportImportSettingsStorage.getLastSuccessfulExportTime() == null
                || !exportImportSettingsStorage
                        .getUri()
                        .equals(exportImportSettingsStorage.getLastSuccessfulExportUri())) {

            // Shorten the period for the first export to any new URI.
            // This will be changed back once the first export to any new location is done.
            periodInMillis = Duration.ofHours(1).toMillis();
            flexInMillis = periodInMillis;

            extras.putBoolean(IS_FIRST_EXPORT, true);
        }
        Slog.i(
                TAG,
                "Scheduling export job with period (millis) = "
                        + periodInMillis
                        + " flex = "
                        + flexInMillis);

        ComponentName componentName = new ComponentName(context, HealthConnectDailyService.class);
        JobInfo.Builder builder =
                new JobInfo.Builder(MIN_JOB_ID + userHandle.getIdentifier(), componentName)
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .setPeriodic(
                                periodInMillis,
                                // Flex interval.
                                // The flex period begins after (periodInMillis - flexInMillis)
                                // Flex is the max of the specified time, or 5% of periodInMillis.
                                flexInMillis)
                        .setExtras(extras);
        if (exportImportFastFollow()) {
            // Persist the job to avoid rescheduling when restarting the device.
            // Otherwise if the user repeatedly restarts their phone, an export may never happen.
            builder = builder.setPersisted(true);
        }

        HealthConnectDailyService.schedule(
                Objects.requireNonNull(context.getSystemService(JobScheduler.class))
                        .forNamespace(NAMESPACE),
                userHandle,
                builder.build());
    }

    /**
     * Execute the periodic export job. It returns true if the export was successful (no need to
     * reschedule the job). False otherwise.
     */
    // TODO(b/318484778): Use dependency injection instead of passing an instance to the method.
    public static boolean executePeriodicExportJob(
            Context context,
            UserHandle userHandle,
            PersistableBundle extras,
            ExportManager exportManager,
            ExportImportSettingsStorage exportImportSettingsStorage) {
        if (exportImportSettingsStorage.getScheduledExportPeriodInDays() <= 0) {
            // If there is no need to run the export, it counts like a success regarding job
            // reschedule.
            return true;
        }

        boolean exportSuccess = exportManager.runExport(userHandle);
        boolean firstExport = extras.getBoolean(IS_FIRST_EXPORT, false);
        if (exportSuccess && firstExport) {
            schedulePeriodicExportJob(
                    userHandle, context, exportImportSettingsStorage, exportManager);
        }
        return exportSuccess;

        // TODO(b/325599089): Do we need an additional periodic / one-off task to make sure a single
        //  export completes? We need to test if JobScheduler will call the job again if jobFinished
        //  is never called.

        // TODO(b/325599089): Consider if we need to do any checkpointing here in case the job
        //  doesn't complete and we need to pick it up again.
    }
}
