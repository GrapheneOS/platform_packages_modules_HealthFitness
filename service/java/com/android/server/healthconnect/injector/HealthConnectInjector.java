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

package com.android.server.healthconnect.injector;

import android.health.HealthFitnessStatsLog;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.backuprestore.BackupRestore;
import com.android.server.healthconnect.exportimport.ExportManager;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.aggregation.FitnessRecordAggregateHelper;
import com.android.server.healthconnect.logging.BackupRestoreLogger;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.logging.UsageStatsCollector;
import com.android.server.healthconnect.migration.MigrationBroadcastScheduler;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.permission.PermissionPackageChangesOrchestrator;
import com.android.server.healthconnect.storage.DailyCleanupJob;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ActivityDateHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseHelper.DatabaseHelpers;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseStatsCollector;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MigrationEntityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.utils.PreferencesManager;
import com.android.server.healthconnect.tracker.TrackerManager;
import com.android.server.healthconnect.utils.TimeSource;

import java.io.File;

/**
 * Interface for Health Connect Dependency Injector.
 *
 * @hide
 */
public abstract class HealthConnectInjector {

    @Nullable private static HealthConnectInjector sHealthConnectInjector;

    /** Getter for {@link PackageInfoUtils} instance initialised by the Health Connect Injector. */
    public abstract PackageInfoUtils getPackageInfoUtils();

    /**
     * Getter for {@link TransactionManager} instance initialised by the Health Connect Injector.
     */
    public abstract TransactionManager getTransactionManager();

    /**
     * Getter for {@link HealthDataCategoryPriorityHelper} instance initialised by the Health
     * Connect Injector.
     */
    public abstract HealthDataCategoryPriorityHelper getHealthDataCategoryPriorityHelper();

    /**
     * Getter for {@link PriorityMigrationHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract PriorityMigrationHelper getPriorityMigrationHelper();

    /** Getter for {@link PreferenceHelper} instance initialised by the Health Connect Injector. */
    public abstract PreferenceHelper getPreferenceHelper();

    /**
     * Getter for {@link ExportImportSettingsStorage} instance initialised by the Health Connect
     * Injector.
     */
    public abstract ExportImportSettingsStorage getExportImportSettingsStorage();

    /** Getter for {@link ExportManager} instance initialised by the Health Connect Injector. */
    public abstract ExportManager getExportManager();

    /**
     * Getter for {@link MigrationStateManager} instance initialised by the Health Connect Injector.
     */
    public abstract MigrationStateManager getMigrationStateManager();

    /** Getter for {@link DeviceInfoHelper} instance initialised by the Health Connect Injector. */
    public abstract DeviceInfoHelper getDeviceInfoHelper();

    /** Getter for {@link AppInfoHelper} instance initialised by the Health Connect Injector. */
    public abstract AppInfoHelper getAppInfoHelper();

    /** Getter for {@link AccessLogsHelper} instance initialised by the Health Connect Injector. */
    public abstract AccessLogsHelper getAccessLogsHelper();

    /**
     * Getter for {@link ActivityDateHelper} instance initialised by the Health Connect Injector.
     */
    public abstract ActivityDateHelper getActivityDateHelper();

    /** Getter for {@link ChangeLogsHelper} instance initialised by the Health Connect Injector. */
    public abstract ChangeLogsHelper getChangeLogsHelper();

    /**
     * Getter for {@link ChangeLogsRequestHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract ChangeLogsRequestHelper getChangeLogsRequestHelper();

    /**
     * Returns an instance of {@link HealthConnectMappings} initialised by the Health Connect
     * Injector.
     */
    public abstract HealthConnectMappings getHealthConnectMappings();

    /**
     * Returns an instance of {@link InternalHealthConnectMappings} initialised by the Health
     * Connect Injector.
     */
    public abstract InternalHealthConnectMappings getInternalHealthConnectMappings();

    /**
     * Getter for {@link FirstGrantTimeManager} instance initialised by the Health Connect Injector.
     */
    public abstract FirstGrantTimeManager getFirstGrantTimeManager();

    /**
     * Getter for {@link HealthPermissionIntentAppsTracker} instance initialised by the Health
     * Connect Injector.
     */
    public abstract HealthPermissionIntentAppsTracker getHealthPermissionIntentAppsTracker();

    /**
     * Getter for {@link PermissionPackageChangesOrchestrator} instance initialised by the Health
     * Connect Injector.
     */
    public abstract PermissionPackageChangesOrchestrator getPermissionPackageChangesOrchestrator();

    /**
     * Getter for {@link HealthConnectPermissionHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract HealthConnectPermissionHelper getHealthConnectPermissionHelper();

    /** Getter for {@link MigrationCleaner} instance initialised by the Health Connect Injector. */
    public abstract MigrationCleaner getMigrationCleaner();

    /**
     * Getter for {@link FitnessRecordUpsertHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract FitnessRecordUpsertHelper getFitnessRecordUpsertHelper();

    /**
     * Getter for {@link FitnessRecordReadHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract FitnessRecordReadHelper getFitnessRecordReadHelper();

    /**
     * Getter for {@link FitnessRecordDeleteHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract FitnessRecordDeleteHelper getFitnessRecordDeleteHelper();

    /**
     * Getter for {@link FitnessRecordAggregateHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract FitnessRecordAggregateHelper getFitnessRecordAggregateHelper();

    /**
     * Getter for {@link MedicalResourceHelper} instance initialised by the Health Connect Injector.
     */
    public abstract MedicalResourceHelper getMedicalResourceHelper();

    /**
     * Getter for {@link MedicalDataSourceHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract MedicalDataSourceHelper getMedicalDataSourceHelper();

    /** Getter for {@link TimeSource} instance initialised by the Health Connect Injector. */
    public abstract TimeSource getTimeSource();

    /**
     * Getter for {@link MigrationBroadcastScheduler} instance initialised by the Health Connect
     * Injector.
     */
    public abstract MigrationBroadcastScheduler getMigrationBroadcastScheduler();

    /**
     * Getter for {@link MigrationUiStateManager} instance initialised by the Health Connect
     * Injector.
     */
    public abstract MigrationUiStateManager getMigrationUiStateManager();

    /** Getter for {@link DatabaseHelpers} instance initialised by the Health Connect Injector. */
    public abstract DatabaseHelpers getDatabaseHelpers();

    /**
     * Getter for {@link MigrationEntityHelper} instance initialised by the Health Connect Injector.
     */
    public abstract MigrationEntityHelper getMigrationEntityHelper();

    /** Getter for {@link BackupRestore} instance initialised by the Health Connect Injector. */
    public abstract BackupRestore getBackupRestore();

    /**
     * Getter for {@link PreferencesManager} instance initialised by the Health Connect Injector.
     */
    public abstract PreferencesManager getPreferencesManager();

    /** Getter for {@link DailyCleanupJob} instance initialised by the Health Connect Injector. */
    public abstract DailyCleanupJob getDailyCleanupJob();

    /**
     * Getter for {@link DatabaseStatsCollector} instance initialised by the Health Connect
     * Injector.
     */
    public abstract DatabaseStatsCollector getDatabaseStatsCollector();

    /**
     * Getter for {@link UsageStatsCollector} instance initialised by the Health Connect Injector.
     */
    public abstract UsageStatsCollector getUsageStatsCollector(HealthConnectContext hcContext);

    /**
     * Getter for {@link ReadAccessLogsHelper} instance initialised by the Health Connect Injector.
     */
    public abstract ReadAccessLogsHelper getReadAccessLogsHelper();

    /**
     * Getter for {@link HealthConnectNotificationSender} instance for export or import initialised
     * by the Health Connect Injector.
     */
    public abstract HealthConnectNotificationSender getExportImportNotificationSender();

    /**
     * Getter for {@link AppOpsManagerLocal} instance initialised by the Health Connect Injector.
     */
    public abstract AppOpsManagerLocal getAppOpsManagerLocal();

    /**
     * Getter for {@link HealthConnectThreadScheduler} instance initialised by the Health Connect
     * Injector.
     */
    public abstract HealthConnectThreadScheduler getThreadScheduler();

    /**
     * Getter for {@link File} instance representing root directory where Health Connect data should
     * be stored. Use this instead of {@link Environment#getDataDirectory}.
     */
    public abstract File getEnvironmentDataDirectory();

    /**
     * Getter for {@link HealthFitnessStatsLog} instance initialised by the Health Connect Injector.
     */
    public abstract HealthFitnessStatsLog getHealthFitnessStatsLog();

    /**
     * Getter for {@link ExportImportLogger} instance initialised by the Health Connect Injector.
     */
    public abstract ExportImportLogger getExportImportLogger();

    /**
     * Getter for {@link BackupRestoreLogger} instance initialised by the Health Connect Injector.
     */
    public abstract BackupRestoreLogger getBackupRestoreLogger();

    /** Getter for {@link TrackerManager} instance initialised by the Health Connect Injector. */
    public abstract TrackerManager getTrackerManager();

    /** Used to initialize the Injector. */
    public static void setInstance(HealthConnectInjector healthConnectInjector) {
        if (sHealthConnectInjector != null) {
            throw new IllegalStateException(
                    "An instance of injector has already been initialized.");
        }
        sHealthConnectInjector = healthConnectInjector;
    }

    /**
     * Used to getInstance of the Injector so that it can be used statically by other base services.
     */
    public static HealthConnectInjector getInstance() {
        if (sHealthConnectInjector == null) {
            throw new IllegalStateException(
                    "Please initialize an instance of injector and call setInstance.");
        }
        return sHealthConnectInjector;
    }

    /** Used to reset instance of the Injector for testing. */
    @VisibleForTesting
    public static void resetInstanceForTest() {
        sHealthConnectInjector = null;
    }
}
