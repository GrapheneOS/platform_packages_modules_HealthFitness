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
import com.android.server.healthconnect.backuprestore.BackupRestoreLogger;
import com.android.server.healthconnect.backuprestore.CloudBackupManager;
import com.android.server.healthconnect.backuprestore.CloudRestoreManager;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.jobs.DailyCleanupJob;
import com.android.server.healthconnect.common.logging.CompletenessStatsLogger;
import com.android.server.healthconnect.common.logging.DatabaseStatsCollector;
import com.android.server.healthconnect.common.logging.UsageStatsCollector;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.common.preferences.PreferencesManager;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.device.tracker.TrackerManager;
import com.android.server.healthconnect.exportimport.ExportImportLogger;
import com.android.server.healthconnect.exportimport.ExportImportNotificationFactory;
import com.android.server.healthconnect.exportimport.ExportImportSettingsStorage;
import com.android.server.healthconnect.exportimport.ExportManager;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.aggregation.FitnessRecordAggregateHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.helpers.RecordDateHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.migration.MigrationBroadcastScheduler;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationEntityHelper;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.notifications.NotificationStatsLogger;
import com.android.server.healthconnect.onboarding.OnboardingNotificationSender;
import com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager;
import com.android.server.healthconnect.onboarding.OnboardingStateManager;
import com.android.server.healthconnect.onboarding.matchmaking.MatchmakingDenialStateManager;
import com.android.server.healthconnect.onboarding.matchmaking.MatchmakingManager;
import com.android.server.healthconnect.permission.FirstGrantTimeDatastore;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.GrantTimeXmlHelper;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.permission.PermissionPackageChangesOrchestrator;
import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.storage.DatabaseHelper.DatabaseHelpers;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.telemetry.dataquality.LatencyMetricsCollector;
import com.android.server.healthconnect.telemetry.dataquality.LatencyMetricsLogger;
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

    /**
     * Getter for {@link OnboardingStateManager} instance initialised by the Health Connect
     * Injector.
     */
    public abstract OnboardingStateManager getOnboardingStateManager();

    /**
     * Getter for {@link OnboardingNotificationStateManager} instance initialised by the Health
     * Connect Injector.
     */
    public abstract OnboardingNotificationStateManager getOnboardingNotificationStateManager();

    /**
     * Getter for {@link OnboardingNotificationSender} instance initialised by the Health Connect
     * Injector.
     */
    public abstract OnboardingNotificationSender getOnboardingNotificationSender();

    /** Getter for {@link DeviceInfoHelper} instance initialised by the Health Connect Injector. */
    public abstract DeviceInfoHelper getDeviceInfoHelper();

    /** Getter for {@link AppInfoHelper} instance initialised by the Health Connect Injector. */
    public abstract AppInfoHelper getAppInfoHelper();

    /** Getter for {@link AccessLogsHelper} instance initialised by the Health Connect Injector. */
    public abstract AccessLogsHelper getAccessLogsHelper();

    /** Getter for {@link RecordDateHelper} instance initialised by the Health Connect Injector. */
    public abstract RecordDateHelper getRecordDateHelper();

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
     * Getter for {@link FirstGrantTimeDatastore} instance initialised by the Health Connect
     * Injector.
     */
    public abstract FirstGrantTimeDatastore getFirstGrantTimeDatastore();

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

    /**
     * Getter for {@link DeviceRecordHelper} instance initialised by the Health Connect Injector.
     */
    public abstract DeviceRecordHelper getDeviceRecordHelper();

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
     * Getter for {@link LatencyMetricsCollector} instance initialised by the Health Connect
     * Injector.
     */
    public abstract LatencyMetricsCollector getLatencyMetricsCollector();

    /**
     * Getter for {@link LatencyMetricsLogger} instance initialised by the Health Connect Injector.
     */
    public abstract LatencyMetricsLogger getLatencyMetricsLogger();

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
     * Getter for {@link DeviceDataSourcesHelper} instance initialised by the Health Connect
     * Injector.
     */
    public abstract DeviceDataSourcesHelper getDeviceDataSourcesHelper();

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
     * Getter for {@link NotificationStatsLogger} instance initialised by the Health Connect
     * Injector.
     */
    public abstract NotificationStatsLogger getNotificationStatsLogger();

    /**
     * Getter for {@link BackupRestoreLogger} instance initialised by the Health Connect Injector.
     */
    public abstract BackupRestoreLogger getBackupRestoreLogger();

    /**
     * Getter for {@link CompletenessStatsLogger} instance initialised by the Health Connect
     * Injector.
     */
    public abstract CompletenessStatsLogger getCompletenessStatsLogger();

    /** Getter for {@link TrackerManager} instance initialised by the Health Connect Injector. */
    public abstract TrackerManager getTrackerManager();

    /**
     * Getter for {@link GrantTimeXmlHelper} instance initialised by the Health Connect Injector.
     */
    public abstract GrantTimeXmlHelper getGrantTimeXmlHelper();

    /**
     * Getter for {@link ExportImportNotificationFactory} instance initialised by the Health Connect
     * Injector.
     */
    public abstract ExportImportNotificationFactory getExportImportNotificationFactory();

    /**
     * Getter for {@link CloudBackupManager} instance initialised by the Health Connect Injector.
     */
    @Nullable
    public abstract CloudBackupManager getCloudBackupManager();

    /**
     * Getter for {@link CloudRestoreManager} instance initialised by the Health Connect Injector.
     */
    @Nullable
    public abstract CloudRestoreManager getCloudRestoreManager();

    /**
     * Getter for {@link MatchmakingManager} instance initialised by the Health Connect Injector.
     */
    @Nullable
    public abstract MatchmakingManager getMatchingAppsManager();

    /**
     * Getter for {@link MatchmakingDenialStateManager} instance initialised by the Health Connect
     * Injector.
     */
    @Nullable
    public abstract MatchmakingDenialStateManager getMatchmakingDenialStateManager();

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
