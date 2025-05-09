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

import android.app.AppOpsManager;
import android.content.Context;
import android.health.HealthFitnessStatsLog;
import android.health.connect.Constants;
import android.health.connect.HealthConnectManager;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.Nullable;

import com.android.server.LocalManagerRegistry;
import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.backuprestore.BackupRestore;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.jobs.DailyCleanupJob;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.common.preferences.PreferencesManager;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.device.tracker.TrackerManager;
import com.android.server.healthconnect.device.tracker.TrackerManagerImpl;
import com.android.server.healthconnect.exportimport.ExportImportNotificationFactory;
import com.android.server.healthconnect.exportimport.ExportImportNotificationSender;
import com.android.server.healthconnect.exportimport.ExportImportSettingsStorage;
import com.android.server.healthconnect.exportimport.ExportManager;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.aggregation.FitnessRecordAggregateHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.helpers.RecordDateHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.logging.BackupRestoreLogger;
import com.android.server.healthconnect.logging.DatabaseStatsCollector;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.logging.UsageStatsCollector;
import com.android.server.healthconnect.migration.MigrationBroadcastScheduler;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationEntityHelper;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.migration.MigrationUtils;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;
import com.android.server.healthconnect.migration.notification.MigrationNotificationSender;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.onboarding.OnboardingStateManager;
import com.android.server.healthconnect.permission.FirstGrantTimeDatastore;
import com.android.server.healthconnect.permission.FirstGrantTimeDatastoreXmlPersistence;
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
import com.android.server.healthconnect.utils.TimeSource;
import com.android.server.healthconnect.utils.TimeSourceImpl;

import java.io.File;
import java.time.Clock;
import java.util.Objects;

/**
 * Injector implementation of HealthConnectInjector containing dependencies to be used in production
 * version of the module.
 *
 * @hide
 */
public class HealthConnectInjectorImpl extends HealthConnectInjector {

    private final Builder mBuilder;

    private final PackageInfoUtils mPackageInfoUtils;
    private final TransactionManager mTransactionManager;
    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final PriorityMigrationHelper mPriorityMigrationHelper;
    private final PreferenceHelper mPreferenceHelper;
    private final ExportImportSettingsStorage mExportImportSettingsStorage;
    private final ExportManager mExportManager;
    private final MigrationStateManager mMigrationStateManager;
    private final OnboardingStateManager mOnboardingStateManager;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final AppOpLogsHelper mAppOpLogsHelper;
    private final AccessLogsHelper mAccessLogsHelper;
    private final RecordDateHelper mRecordDateHelper;
    private final HealthConnectMappings mHealthConnectMappings;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final ChangeLogsHelper mChangeLogsHelper;
    private final ChangeLogsRequestHelper mChangeLogsRequestHelper;
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    private final PermissionPackageChangesOrchestrator mPermissionPackageChangesOrchestrator;
    private final HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    private final MigrationCleaner mMigrationCleaner;
    private final TimeSource mTimeSource;
    private final FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
    private final FitnessRecordReadHelper mFitnessRecordReadHelper;
    private final FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    private final FitnessRecordAggregateHelper mFitnessRecordAggregateHelper;
    private final MedicalDataSourceHelper mMedicalDataSourceHelper;
    private final MedicalResourceHelper mMedicalResourceHelper;
    private final MigrationBroadcastScheduler mMigrationBroadcastScheduler;
    private final MigrationUiStateManager mMigrationUiStateManager;
    private final DatabaseHelpers mDatabaseHelpers;
    private final MigrationEntityHelper mMigrationEntityHelper;
    private final BackupRestore mBackupRestore;
    private final PreferencesManager mPreferencesManager;
    private final ReadAccessLogsHelper mReadAccessLogsHelper;
    private final ExportImportNotificationFactory mExportImportNotificationFactory;
    private final HealthConnectNotificationSender mExportImportNotificationSender;
    private final AppOpsManagerLocal mAppOpsManagerLocal;
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final DeviceDataSourcesHelper mDeviceDataSourcesHelper;
    private final File mEnvironmentDataDirectory;
    private final HealthFitnessStatsLog mHealthFitnesssStatsLog;
    private final ExportImportLogger mExportImportLogger;
    private final TrackerManager mTrackerManager;
    private final GrantTimeXmlHelper mGrantTimeXmlHelper;
    private final BackupRestoreLogger mBackupRestoreLogger;
    private final FirstGrantTimeDatastore mFirstGrantTimeDatastore;
    private final DeviceRecordHelper mDeviceRecordHelper;

    public HealthConnectInjectorImpl(Context context) {
        this(new Builder(context));
    }

    private HealthConnectInjectorImpl(Builder builder) {
        Context context = builder.mContext;
        mBuilder = builder;

        // Don't store the user and make it available via the injector, as this user is always
        // the first / system user, and doesn't change after that.
        // Any class that is using this user below are responsible for making sure that they
        // update any reference to user when it changes.
        UserHandle userHandle = builder.mUserHandle;
        mEnvironmentDataDirectory =
                builder.mEnvironmentDataDirectory == null
                        ? Environment.getDataDirectory()
                        : builder.mEnvironmentDataDirectory;
        mHealthFitnesssStatsLog =
                builder.mStatsLog == null ? new HealthFitnessStatsLog() : builder.mStatsLog;
        mExportImportLogger = new ExportImportLogger(mHealthFitnesssStatsLog);
        mBackupRestoreLogger = new BackupRestoreLogger(mHealthFitnesssStatsLog);

        HealthConnectContext hcContext =
                HealthConnectContext.create(
                        context,
                        userHandle,
                        /* databaseDirName= */ null,
                        mEnvironmentDataDirectory);
        HealthConnectResourcesContext resourcesContext =
                builder.mResourcesContext == null
                        ? new HealthConnectResourcesContext(context)
                        : builder.mResourcesContext;

        mDatabaseHelpers = new DatabaseHelpers();
        mInternalHealthConnectMappings = InternalHealthConnectMappings.getInstance();
        mHealthConnectMappings = HealthConnectMappings.getInstance();
        mTimeSource = builder.mTimeSource == null ? new TimeSourceImpl() : builder.mTimeSource;
        mThreadScheduler =
                builder.mThreadScheduler == null
                        ? new HealthConnectThreadScheduler()
                        : builder.mThreadScheduler;
        mDeviceDataSourcesHelper =
                builder.mDeviceDataSourcesHelper == null
                        ? new DeviceDataSourcesHelper()
                        : builder.mDeviceDataSourcesHelper;
        mMigrationEntityHelper =
                builder.mMigrationEntityHelper == null
                        ? new MigrationEntityHelper(mDatabaseHelpers)
                        : builder.mMigrationEntityHelper;
        mExportImportNotificationSender =
                builder.mExportImportNotificationSender == null
                        ? ExportImportNotificationSender.createSender(context, resourcesContext)
                        : builder.mExportImportNotificationSender;

        mTransactionManager =
                builder.mTransactionManager == null
                        ? TransactionManager.create(hcContext)
                        : builder.mTransactionManager;
        mAppInfoHelper =
                builder.mAppInfoHelper == null
                        ? new AppInfoHelper(
                                hcContext,
                                mTransactionManager,
                                mInternalHealthConnectMappings,
                                mDatabaseHelpers)
                        : builder.mAppInfoHelper;
        mPackageInfoUtils =
                builder.mPackageInfoUtils == null
                        ? new PackageInfoUtils()
                        : builder.mPackageInfoUtils;
        mPreferenceHelper =
                builder.mPreferenceHelper == null
                        ? new PreferenceHelper(mTransactionManager, mDatabaseHelpers)
                        : builder.mPreferenceHelper;
        mHealthDataCategoryPriorityHelper =
                builder.mHealthDataCategoryPriorityHelper == null
                        ? new HealthDataCategoryPriorityHelper(
                                hcContext,
                                mAppInfoHelper,
                                mTransactionManager,
                                mPreferenceHelper,
                                mPackageInfoUtils,
                                mHealthConnectMappings,
                                mDatabaseHelpers,
                                mThreadScheduler,
                                builder.mUserManager == null
                                        ? hcContext.getSystemService(UserManager.class)
                                        : builder.mUserManager)
                        : builder.mHealthDataCategoryPriorityHelper;
        mPriorityMigrationHelper =
                builder.mPriorityMigrationHelper == null
                        ? new PriorityMigrationHelper(
                                mHealthDataCategoryPriorityHelper,
                                mTransactionManager,
                                mDatabaseHelpers)
                        : builder.mPriorityMigrationHelper;
        mExportImportSettingsStorage =
                builder.mExportImportSettingsStorage == null
                        ? new ExportImportSettingsStorage(mPreferenceHelper)
                        : builder.mExportImportSettingsStorage;
        mExportImportNotificationFactory =
                builder.mExportImportNotificationFactory == null
                        ? new ExportImportNotificationFactory(
                                context, resourcesContext, Constants.NOTIFICATION_CHANNEL_ID)
                        : builder.mExportImportNotificationFactory;
        mExportManager =
                builder.mExportManager == null
                        ? new ExportManager(
                                context,
                                Clock.systemUTC(),
                                mExportImportSettingsStorage,
                                mTransactionManager,
                                mExportImportNotificationSender,
                                mEnvironmentDataDirectory,
                                mExportImportLogger,
                                mExportImportNotificationFactory)
                        : builder.mExportManager;
        mMigrationBroadcastScheduler =
                builder.mMigrationBroadcastScheduler == null
                        ? new MigrationBroadcastScheduler(userHandle)
                        : builder.mMigrationBroadcastScheduler;
        MigrationUtils migrationUtils =
                builder.mMigrationUtils == null ? new MigrationUtils() : builder.mMigrationUtils;
        mMigrationStateManager =
                builder.mMigrationStateManager == null
                        ? new MigrationStateManager(
                                userHandle,
                                mPreferenceHelper,
                                mMigrationBroadcastScheduler,
                                mThreadScheduler,
                                migrationUtils)
                        : builder.mMigrationStateManager;
        mDeviceInfoHelper =
                builder.mDeviceInfoHelper == null
                        ? new DeviceInfoHelper(mTransactionManager, mDatabaseHelpers)
                        : builder.mDeviceInfoHelper;
        mAppOpLogsHelper =
                builder.mAppOpLogsHelper == null
                        ? new AppOpLogsHelper(
                                context.getSystemService(AppOpsManager.class),
                                context.getPackageManager(),
                                HealthConnectManager.getHealthPermissions(context))
                        : builder.mAppOpLogsHelper;
        mAccessLogsHelper =
                builder.mAccessLogsHelper == null
                        ? new AccessLogsHelper(
                                mTransactionManager,
                                mAppInfoHelper,
                                mAppOpLogsHelper,
                                mDatabaseHelpers)
                        : builder.mAccessLogsHelper;
        mReadAccessLogsHelper =
                builder.mReadAccessLogsHelper == null
                        ? new ReadAccessLogsHelper(
                                mAppInfoHelper, mTransactionManager, mDatabaseHelpers)
                        : builder.mReadAccessLogsHelper;
        mRecordDateHelper =
                builder.mActivityDateHelper == null
                        ? new RecordDateHelper(
                                mTransactionManager,
                                mInternalHealthConnectMappings,
                                mDatabaseHelpers)
                        : builder.mActivityDateHelper;
        mChangeLogsHelper =
                builder.mChangeLogsHelper == null
                        ? new ChangeLogsHelper(mTransactionManager, mDatabaseHelpers)
                        : builder.mChangeLogsHelper;
        mChangeLogsRequestHelper =
                builder.mChangeLogsRequestHelper == null
                        ? new ChangeLogsRequestHelper(mTransactionManager, mDatabaseHelpers)
                        : builder.mChangeLogsRequestHelper;
        mPermissionIntentAppsTracker =
                builder.mPermissionIntentAppsTracker == null
                        ? new HealthPermissionIntentAppsTracker(context)
                        : builder.mPermissionIntentAppsTracker;
        mGrantTimeXmlHelper = new GrantTimeXmlHelper();
        mFirstGrantTimeDatastore =
                builder.mFirstGrantTimeDatastore == null
                        ? new FirstGrantTimeDatastoreXmlPersistence(
                                mEnvironmentDataDirectory, mGrantTimeXmlHelper)
                        : builder.mFirstGrantTimeDatastore;
        mFirstGrantTimeManager =
                builder.mFirstGrantTimeManager == null
                        ? new FirstGrantTimeManager(
                                context,
                                mPermissionIntentAppsTracker,
                                mFirstGrantTimeDatastore,
                                mPackageInfoUtils,
                                mHealthDataCategoryPriorityHelper,
                                mMigrationStateManager,
                                mThreadScheduler)
                        : builder.mFirstGrantTimeManager;
        mHealthConnectPermissionHelper =
                builder.mHealthConnectPermissionHelper == null
                        ? new HealthConnectPermissionHelper(
                                context,
                                context.getPackageManager(),
                                mPermissionIntentAppsTracker,
                                mFirstGrantTimeManager,
                                mHealthDataCategoryPriorityHelper,
                                mAppInfoHelper,
                                mHealthConnectMappings)
                        : builder.mHealthConnectPermissionHelper;
        mPermissionPackageChangesOrchestrator =
                builder.mPermissionPackageChangesOrchestrator == null
                        ? new PermissionPackageChangesOrchestrator(
                                mPermissionIntentAppsTracker,
                                mFirstGrantTimeManager,
                                mHealthConnectPermissionHelper,
                                userHandle,
                                mHealthDataCategoryPriorityHelper,
                                mThreadScheduler)
                        : builder.mPermissionPackageChangesOrchestrator;
        mMigrationCleaner =
                builder.mMigrationCleaner == null
                        ? new MigrationCleaner(
                                mTransactionManager,
                                mPriorityMigrationHelper,
                                mMigrationEntityHelper)
                        : builder.mMigrationCleaner;
        mFitnessRecordUpsertHelper =
                builder.mFitnessRecordUpsertHelper == null
                        ? new FitnessRecordUpsertHelper(
                                mTransactionManager,
                                mDeviceInfoHelper,
                                mAppInfoHelper,
                                mAccessLogsHelper,
                                mRecordDateHelper,
                                mThreadScheduler,
                                mInternalHealthConnectMappings)
                        : builder.mFitnessRecordUpsertHelper;
        mFitnessRecordReadHelper =
                builder.mFitnessRecordReadHelper == null
                        ? new FitnessRecordReadHelper(
                                mDeviceInfoHelper,
                                mAppInfoHelper,
                                mAccessLogsHelper,
                                mReadAccessLogsHelper,
                                mInternalHealthConnectMappings)
                        : builder.mFitnessRecordReadHelper;
        mFitnessRecordDeleteHelper =
                builder.mFitnessRecordDeleteHelper == null
                        ? new FitnessRecordDeleteHelper(
                                mTransactionManager,
                                mAppInfoHelper,
                                mAccessLogsHelper,
                                mRecordDateHelper,
                                mThreadScheduler,
                                mInternalHealthConnectMappings)
                        : builder.mFitnessRecordDeleteHelper;
        mFitnessRecordAggregateHelper =
                builder.mFitnessRecordAggregateHelper == null
                        ? new FitnessRecordAggregateHelper(
                                mTransactionManager,
                                mAppInfoHelper,
                                mHealthDataCategoryPriorityHelper,
                                mAccessLogsHelper,
                                mReadAccessLogsHelper,
                                mInternalHealthConnectMappings)
                        : builder.mFitnessRecordAggregateHelper;
        mMedicalDataSourceHelper =
                builder.mMedicalDataSourceHelper == null
                        ? new MedicalDataSourceHelper(
                                mTransactionManager, mAppInfoHelper, mTimeSource, mAccessLogsHelper)
                        : builder.mMedicalDataSourceHelper;
        mMedicalResourceHelper =
                builder.mMedicalResourceHelper == null
                        ? new MedicalResourceHelper(
                                mTransactionManager,
                                mAppInfoHelper,
                                mMedicalDataSourceHelper,
                                mTimeSource,
                                getAccessLogsHelper())
                        : builder.mMedicalResourceHelper;
        mMigrationUiStateManager =
                builder.mMigrationUiStateManager == null
                        ? new MigrationUiStateManager(
                                context,
                                userHandle,
                                mMigrationStateManager,
                                new MigrationNotificationSender(context, resourcesContext))
                        : builder.mMigrationUiStateManager;
        mBackupRestore =
                new BackupRestore(
                        mAppInfoHelper,
                        mFirstGrantTimeManager,
                        mMigrationStateManager,
                        mPreferenceHelper,
                        mTransactionManager,
                        mFitnessRecordUpsertHelper,
                        mFitnessRecordReadHelper,
                        context,
                        mDeviceInfoHelper,
                        mHealthDataCategoryPriorityHelper,
                        mThreadScheduler,
                        mEnvironmentDataDirectory,
                        mGrantTimeXmlHelper);
        mPreferencesManager =
                builder.mPreferencesManager == null
                        ? new PreferencesManager(mPreferenceHelper)
                        : builder.mPreferencesManager;
        mAppOpsManagerLocal =
                builder.mAppOpsManagerLocal == null
                        ? LocalManagerRegistry.getManager(AppOpsManagerLocal.class)
                        : builder.mAppOpsManagerLocal;
        mOnboardingStateManager =
                builder.mOnboardingStateManager == null
                        ? new OnboardingStateManager(getPreferenceHelper(), userHandle)
                        : builder.mOnboardingStateManager;
        mDeviceRecordHelper = new DeviceRecordHelper(mFitnessRecordUpsertHelper);
        mTrackerManager =
                builder.mTrackerManager == null
                        ? new TrackerManagerImpl(
                                context,
                                mHealthConnectPermissionHelper,
                                mThreadScheduler,
                                mDeviceRecordHelper,
                                mDeviceDataSourcesHelper)
                        : builder.mTrackerManager;
    }

    @Override
    public PackageInfoUtils getPackageInfoUtils() {
        return mPackageInfoUtils;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return mTransactionManager;
    }

    @Override
    public HealthDataCategoryPriorityHelper getHealthDataCategoryPriorityHelper() {
        return mHealthDataCategoryPriorityHelper;
    }

    @Override
    public PriorityMigrationHelper getPriorityMigrationHelper() {
        return mPriorityMigrationHelper;
    }

    @Override
    public PreferenceHelper getPreferenceHelper() {
        return mPreferenceHelper;
    }

    @Override
    public ExportImportSettingsStorage getExportImportSettingsStorage() {
        return mExportImportSettingsStorage;
    }

    @Override
    public ExportManager getExportManager() {
        return mExportManager;
    }

    @Override
    public HealthConnectNotificationSender getExportImportNotificationSender() {
        return mExportImportNotificationSender;
    }

    @Override
    public MigrationStateManager getMigrationStateManager() {
        return mMigrationStateManager;
    }

    @Override
    public OnboardingStateManager getOnboardingStateManager() {
        return mOnboardingStateManager;
    }

    @Override
    public DeviceInfoHelper getDeviceInfoHelper() {
        return mDeviceInfoHelper;
    }

    @Override
    public AppInfoHelper getAppInfoHelper() {
        return mAppInfoHelper;
    }

    @Override
    public AccessLogsHelper getAccessLogsHelper() {
        return mAccessLogsHelper;
    }

    @Override
    public RecordDateHelper getRecordDateHelper() {
        return mRecordDateHelper;
    }

    @Override
    public ChangeLogsHelper getChangeLogsHelper() {
        return mChangeLogsHelper;
    }

    @Override
    public ChangeLogsRequestHelper getChangeLogsRequestHelper() {
        return mChangeLogsRequestHelper;
    }

    @Override
    public HealthConnectMappings getHealthConnectMappings() {
        return mHealthConnectMappings;
    }

    @Override
    public InternalHealthConnectMappings getInternalHealthConnectMappings() {
        return mInternalHealthConnectMappings;
    }

    @Override
    public FirstGrantTimeManager getFirstGrantTimeManager() {
        return mFirstGrantTimeManager;
    }

    @Override
    public FirstGrantTimeDatastore getFirstGrantTimeDatastore() {
        return mFirstGrantTimeDatastore;
    }

    @Override
    public HealthPermissionIntentAppsTracker getHealthPermissionIntentAppsTracker() {
        return mPermissionIntentAppsTracker;
    }

    @Override
    public PermissionPackageChangesOrchestrator getPermissionPackageChangesOrchestrator() {
        return mPermissionPackageChangesOrchestrator;
    }

    @Override
    public HealthConnectPermissionHelper getHealthConnectPermissionHelper() {
        return mHealthConnectPermissionHelper;
    }

    @Override
    public MigrationCleaner getMigrationCleaner() {
        return mMigrationCleaner;
    }

    @Override
    public FitnessRecordUpsertHelper getFitnessRecordUpsertHelper() {
        return mFitnessRecordUpsertHelper;
    }

    @Override
    public FitnessRecordReadHelper getFitnessRecordReadHelper() {
        return mFitnessRecordReadHelper;
    }

    @Override
    public FitnessRecordDeleteHelper getFitnessRecordDeleteHelper() {
        return mFitnessRecordDeleteHelper;
    }

    @Override
    public FitnessRecordAggregateHelper getFitnessRecordAggregateHelper() {
        return mFitnessRecordAggregateHelper;
    }

    @Override
    public MedicalDataSourceHelper getMedicalDataSourceHelper() {
        return mMedicalDataSourceHelper;
    }

    @Override
    public DeviceRecordHelper getDeviceRecordHelper() {
        return mDeviceRecordHelper;
    }

    @Override
    public MedicalResourceHelper getMedicalResourceHelper() {
        return mMedicalResourceHelper;
    }

    @Override
    public TimeSource getTimeSource() {
        return mTimeSource;
    }

    @Override
    public MigrationBroadcastScheduler getMigrationBroadcastScheduler() {
        return mMigrationBroadcastScheduler;
    }

    @Override
    public MigrationUiStateManager getMigrationUiStateManager() {
        return mMigrationUiStateManager;
    }

    @Override
    public DatabaseHelpers getDatabaseHelpers() {
        return mDatabaseHelpers;
    }

    @Override
    public MigrationEntityHelper getMigrationEntityHelper() {
        return mMigrationEntityHelper;
    }

    @Override
    public BackupRestore getBackupRestore() {
        return mBackupRestore;
    }

    @Override
    public PreferencesManager getPreferencesManager() {
        return mPreferencesManager;
    }

    @Override
    public DailyCleanupJob getDailyCleanupJob() {
        return new DailyCleanupJob(
                getHealthDataCategoryPriorityHelper(),
                getPreferencesManager(),
                getAppInfoHelper(),
                getTransactionManager(),
                getFitnessRecordDeleteHelper(),
                getRecordDateHelper());
    }

    @Override
    public DatabaseStatsCollector getDatabaseStatsCollector() {
        return mBuilder.mDatabaseStatsCollector == null
                ? new DatabaseStatsCollector(getTransactionManager())
                : mBuilder.mDatabaseStatsCollector;
    }

    @Override
    public UsageStatsCollector getUsageStatsCollector(HealthConnectContext hcContext) {
        return mBuilder.mUsageStatsCollector == null
                ? new UsageStatsCollector(
                        hcContext,
                        mPreferenceHelper,
                        mPreferencesManager,
                        mAccessLogsHelper,
                        mTimeSource,
                        mMedicalResourceHelper,
                        mMedicalDataSourceHelper,
                        mPackageInfoUtils)
                : mBuilder.mUsageStatsCollector;
    }

    @Override
    public ReadAccessLogsHelper getReadAccessLogsHelper() {
        return mReadAccessLogsHelper;
    }

    @Override
    public AppOpsManagerLocal getAppOpsManagerLocal() {
        return mAppOpsManagerLocal;
    }

    @Override
    public HealthConnectThreadScheduler getThreadScheduler() {
        return mThreadScheduler;
    }

    @Override
    public DeviceDataSourcesHelper getDeviceDataSourcesHelper() {
        return mDeviceDataSourcesHelper;
    }

    @Override
    public File getEnvironmentDataDirectory() {
        return mEnvironmentDataDirectory;
    }

    @Override
    public HealthFitnessStatsLog getHealthFitnessStatsLog() {
        return mHealthFitnesssStatsLog;
    }

    @Override
    public ExportImportLogger getExportImportLogger() {
        return mExportImportLogger;
    }

    @Override
    public BackupRestoreLogger getBackupRestoreLogger() {
        return mBackupRestoreLogger;
    }

    @Override
    public TrackerManager getTrackerManager() {
        return mTrackerManager;
    }

    @Override
    public GrantTimeXmlHelper getGrantTimeXmlHelper() {
        return mGrantTimeXmlHelper;
    }

    @Override
    public ExportImportNotificationFactory getExportImportNotificationFactory() {
        return mExportImportNotificationFactory;
    }

    /**
     * Returns a new Builder of Health Connect Injector
     *
     * <p>USE ONLY DURING TESTING.
     */
    public static Builder newBuilderForTest(Context context) {
        return new Builder(context);
    }

    /**
     * Used to build injector.
     *
     * <p>The setters are used only when we need a custom implementation of any dependency which is
     * ONLY for testing. Do not use setters if we need default implementation of a dependency.
     */
    public static class Builder {

        private final Context mContext;
        private final UserHandle mUserHandle;

        @Nullable private PackageInfoUtils mPackageInfoUtils;
        @Nullable private TransactionManager mTransactionManager;
        @Nullable private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
        @Nullable private PriorityMigrationHelper mPriorityMigrationHelper;
        @Nullable private PreferenceHelper mPreferenceHelper;
        @Nullable private ExportImportSettingsStorage mExportImportSettingsStorage;
        @Nullable private ExportManager mExportManager;
        @Nullable private ExportImportNotificationFactory mExportImportNotificationFactory;
        @Nullable private MigrationStateManager mMigrationStateManager;
        @Nullable private DeviceInfoHelper mDeviceInfoHelper;
        @Nullable private AppInfoHelper mAppInfoHelper;
        @Nullable private AppOpLogsHelper mAppOpLogsHelper;
        @Nullable private AccessLogsHelper mAccessLogsHelper;
        @Nullable private RecordDateHelper mActivityDateHelper;
        @Nullable private ChangeLogsHelper mChangeLogsHelper;
        @Nullable private ChangeLogsRequestHelper mChangeLogsRequestHelper;
        @Nullable private FirstGrantTimeManager mFirstGrantTimeManager;
        @Nullable private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
        @Nullable private FirstGrantTimeDatastore mFirstGrantTimeDatastore;

        @Nullable
        private PermissionPackageChangesOrchestrator mPermissionPackageChangesOrchestrator;

        @Nullable private HealthConnectPermissionHelper mHealthConnectPermissionHelper;
        @Nullable private HealthConnectNotificationSender mExportImportNotificationSender;
        @Nullable private MigrationCleaner mMigrationCleaner;
        @Nullable private TimeSource mTimeSource;
        @Nullable private FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
        @Nullable private FitnessRecordReadHelper mFitnessRecordReadHelper;
        @Nullable private FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
        @Nullable private FitnessRecordAggregateHelper mFitnessRecordAggregateHelper;
        @Nullable private MedicalDataSourceHelper mMedicalDataSourceHelper;
        @Nullable private MedicalResourceHelper mMedicalResourceHelper;
        @Nullable private MigrationBroadcastScheduler mMigrationBroadcastScheduler;
        @Nullable private MigrationUiStateManager mMigrationUiStateManager;
        @Nullable private MigrationEntityHelper mMigrationEntityHelper;
        @Nullable private OnboardingStateManager mOnboardingStateManager;
        @Nullable private PreferencesManager mPreferencesManager;
        @Nullable private DatabaseStatsCollector mDatabaseStatsCollector;
        @Nullable private UsageStatsCollector mUsageStatsCollector;
        @Nullable private ReadAccessLogsHelper mReadAccessLogsHelper;
        @Nullable private File mEnvironmentDataDirectory;
        @Nullable private AppOpsManagerLocal mAppOpsManagerLocal;
        @Nullable private HealthConnectThreadScheduler mThreadScheduler;
        @Nullable private DeviceDataSourcesHelper mDeviceDataSourcesHelper;
        @Nullable private HealthFitnessStatsLog mStatsLog;
        @Nullable private TrackerManager mTrackerManager;
        @Nullable private MigrationUtils mMigrationUtils;
        @Nullable private HealthConnectResourcesContext mResourcesContext;
        @Nullable private UserManager mUserManager;

        private Builder(Context context) {
            mContext = context;
            mUserHandle = context.getUser();
        }

        /** Set fake or custom {@link PackageInfoUtils} */
        public Builder setPackageInfoUtils(PackageInfoUtils packageInfoUtils) {
            Objects.requireNonNull(packageInfoUtils);
            mPackageInfoUtils = packageInfoUtils;
            return this;
        }

        /** Set fake or custom {@link TransactionManager} */
        public Builder setTransactionManager(TransactionManager transactionManager) {
            Objects.requireNonNull(transactionManager);
            mTransactionManager = transactionManager;
            return this;
        }

        /** Set fake or custom {@link HealthDataCategoryPriorityHelper} */
        public Builder setHealthDataCategoryPriorityHelper(
                HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper) {
            Objects.requireNonNull(healthDataCategoryPriorityHelper);
            mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
            return this;
        }

        /** Set fake or custom {@link PriorityMigrationHelper} */
        public Builder setPriorityMigrationHelper(PriorityMigrationHelper priorityMigrationHelper) {
            Objects.requireNonNull(priorityMigrationHelper);
            mPriorityMigrationHelper = priorityMigrationHelper;
            return this;
        }

        /** Set fake or custom {@link PreferenceHelper} */
        public Builder setPreferenceHelper(PreferenceHelper preferenceHelper) {
            Objects.requireNonNull(preferenceHelper);
            mPreferenceHelper = preferenceHelper;
            return this;
        }

        /** Set fake or custom {@link ExportImportSettingsStorage} */
        public Builder setExportImportSettingsStorage(
                ExportImportSettingsStorage exportImportSettingsStorage) {
            Objects.requireNonNull(exportImportSettingsStorage);
            mExportImportSettingsStorage = exportImportSettingsStorage;
            return this;
        }

        /** Set fake or custom {@link ExportManager} */
        public Builder setExportManager(ExportManager exportManager) {
            Objects.requireNonNull(exportManager);
            mExportManager = exportManager;
            return this;
        }

        /** Set fake or custom {@link ExportImportNotificationFactory} */
        public Builder setExportImportNotificationFactory(ExportImportNotificationFactory factory) {
            Objects.requireNonNull(factory);
            mExportImportNotificationFactory = factory;
            return this;
        }

        /** Set fake or custom {@link MigrationStateManager} */
        public Builder setMigrationStateManager(MigrationStateManager migrationStateManager) {
            Objects.requireNonNull(migrationStateManager);
            mMigrationStateManager = migrationStateManager;
            return this;
        }

        /** Set fake or custom {@link DeviceInfoHelper} */
        public Builder setDeviceInfoHelper(DeviceInfoHelper deviceInfoHelper) {
            Objects.requireNonNull(deviceInfoHelper);
            mDeviceInfoHelper = deviceInfoHelper;
            return this;
        }

        /** Set fake or custom {@link AppInfoHelper} */
        public Builder setAppInfoHelper(AppInfoHelper appInfoHelper) {
            Objects.requireNonNull(appInfoHelper);
            mAppInfoHelper = appInfoHelper;
            return this;
        }

        /** Set fake or custom {@link AppOpLogsHelper} */
        public Builder setAppOpLogsHelper(AppOpLogsHelper appOpLogsHelper) {
            Objects.requireNonNull(appOpLogsHelper);
            mAppOpLogsHelper = appOpLogsHelper;
            return this;
        }

        /** Set fake or custom {@link AccessLogsHelper} */
        public Builder setAccessLogsHelper(AccessLogsHelper accessLogsHelper) {
            Objects.requireNonNull(accessLogsHelper);
            mAccessLogsHelper = accessLogsHelper;
            return this;
        }

        /** Set fake or custom {@link RecordDateHelper} */
        public Builder setActivityDateHelper(RecordDateHelper recordDateHelper) {
            Objects.requireNonNull(recordDateHelper);
            mActivityDateHelper = recordDateHelper;
            return this;
        }

        /** Set fake or custom {@link ChangeLogsHelper} */
        public Builder setChangeLogsHelper(ChangeLogsHelper changeLogsHelper) {
            Objects.requireNonNull(changeLogsHelper);
            mChangeLogsHelper = changeLogsHelper;
            return this;
        }

        /** Set fake or custom {@link ChangeLogsRequestHelper} */
        public Builder setChangeLogsRequestHelper(ChangeLogsRequestHelper changeLogsRequestHelper) {
            Objects.requireNonNull(changeLogsRequestHelper);
            mChangeLogsRequestHelper = changeLogsRequestHelper;
            return this;
        }

        /** Set fake or custom {@link FirstGrantTimeManager} */
        public Builder setFirstGrantTimeManager(FirstGrantTimeManager firstGrantTimeManager) {
            Objects.requireNonNull(firstGrantTimeManager);
            mFirstGrantTimeManager = firstGrantTimeManager;
            return this;
        }

        /** Set fake or custom {@link FirstGrantTimeDatastore} */
        public Builder setFirstGrantTimeDatastore(FirstGrantTimeDatastore firstGrantTimeDatastore) {
            Objects.requireNonNull(firstGrantTimeDatastore);
            mFirstGrantTimeDatastore = firstGrantTimeDatastore;
            return this;
        }

        /** Set fake or custom {@link HealthPermissionIntentAppsTracker} */
        public Builder setHealthPermissionIntentAppsTracker(
                HealthPermissionIntentAppsTracker healthPermissionIntentAppsTracker) {
            Objects.requireNonNull(healthPermissionIntentAppsTracker);
            mPermissionIntentAppsTracker = healthPermissionIntentAppsTracker;
            return this;
        }

        /** Set fake or custom {@link PermissionPackageChangesOrchestrator} */
        public Builder setPermissionPackageChangesOrchestrator(
                PermissionPackageChangesOrchestrator permissionPackageChangesOrchestrator) {
            Objects.requireNonNull(permissionPackageChangesOrchestrator);
            mPermissionPackageChangesOrchestrator = permissionPackageChangesOrchestrator;
            return this;
        }

        /** Set fake or custom {@link HealthConnectPermissionHelper} */
        public Builder setHealthConnectPermissionHelper(
                HealthConnectPermissionHelper healthConnectPermissionHelper) {
            Objects.requireNonNull(healthConnectPermissionHelper);
            mHealthConnectPermissionHelper = healthConnectPermissionHelper;
            return this;
        }

        /** Set fake or custom {@link HealthConnectNotificationSender} for export/import. */
        public Builder setExportImportNotificationSender(
                HealthConnectNotificationSender notificationSender) {
            mExportImportNotificationSender = Objects.requireNonNull(notificationSender);
            return this;
        }

        /** Set fake or custom {@link MigrationCleaner} */
        public Builder setMigrationCleaner(MigrationCleaner migrationCleaner) {
            Objects.requireNonNull(migrationCleaner);
            mMigrationCleaner = migrationCleaner;
            return this;
        }

        /** Set fake or custom {@link TimeSource} */
        public Builder setTimeSource(TimeSource timeSource) {
            Objects.requireNonNull(timeSource);
            mTimeSource = timeSource;
            return this;
        }

        /** Set fake or custom {@link FitnessRecordUpsertHelper} */
        public Builder setFitnessRecordUpsertHelper(
                FitnessRecordUpsertHelper fitnessRecordUpsertHelper) {
            Objects.requireNonNull(fitnessRecordUpsertHelper);
            mFitnessRecordUpsertHelper = fitnessRecordUpsertHelper;
            return this;
        }

        /** Set fake or custom {@link FitnessRecordReadHelper} */
        public Builder setFitnessReadRequestHandler(
                FitnessRecordReadHelper fitnessRecordReadHelper) {
            Objects.requireNonNull(fitnessRecordReadHelper);
            mFitnessRecordReadHelper = fitnessRecordReadHelper;
            return this;
        }

        /** Set fake or custom {@link FitnessRecordDeleteHelper} */
        public Builder setFitnessRecordDeleteHelper(
                FitnessRecordDeleteHelper fitnessRecordDeleteHelper) {
            Objects.requireNonNull(fitnessRecordDeleteHelper);
            mFitnessRecordDeleteHelper = fitnessRecordDeleteHelper;
            return this;
        }

        /** Set fake or custom {@link FitnessRecordDeleteHelper} */
        public Builder setFitnessRecordDeleteHelper(
                FitnessRecordAggregateHelper fitnessRecordAggregateHelper) {
            Objects.requireNonNull(fitnessRecordAggregateHelper);
            mFitnessRecordAggregateHelper = fitnessRecordAggregateHelper;
            return this;
        }

        /** Set fake or custom {@link MedicalDataSourceHelper} */
        public Builder setMedicalDataSourceHelper(MedicalDataSourceHelper medicalDataSourceHelper) {
            Objects.requireNonNull(medicalDataSourceHelper);
            mMedicalDataSourceHelper = medicalDataSourceHelper;
            return this;
        }

        /** Set fake or custom {@link MedicalResourceHelper} */
        public Builder setMedicalResourceHelper(MedicalResourceHelper medicalResourceHelper) {
            Objects.requireNonNull(medicalResourceHelper);
            mMedicalResourceHelper = medicalResourceHelper;
            return this;
        }

        /** Set fake or custom {@link MigrationBroadcastScheduler} */
        public Builder setMigrationBroadcastScheduler(
                MigrationBroadcastScheduler migrationBroadcastScheduler) {
            Objects.requireNonNull(migrationBroadcastScheduler);
            mMigrationBroadcastScheduler = migrationBroadcastScheduler;
            return this;
        }

        /** Set fake or custom {@link MigrationUiStateManager} */
        public Builder setMigrationUiStateManager(MigrationUiStateManager migrationUiStateManager) {
            Objects.requireNonNull(migrationUiStateManager);
            mMigrationUiStateManager = migrationUiStateManager;
            return this;
        }

        /** Set fake or custom {@link MigrationEntityHelper} */
        public Builder setMigrationEntityHelper(MigrationEntityHelper migrationEntityHelper) {
            Objects.requireNonNull(migrationEntityHelper);
            mMigrationEntityHelper = migrationEntityHelper;
            return this;
        }

        /** Set fake or custom {@link OnboardingStateManager} */
        public Builder setOnboardingStateManager(OnboardingStateManager onboardingStateManager) {
            Objects.requireNonNull(onboardingStateManager);
            mOnboardingStateManager = onboardingStateManager;
            return this;
        }

        /** Set fake or custom {@link PreferencesManager} */
        public Builder setPreferencesManager(PreferencesManager preferencesManager) {
            Objects.requireNonNull(preferencesManager);
            mPreferencesManager = preferencesManager;
            return this;
        }

        /** Set fake or custom {@link DatabaseStatsCollector} */
        public Builder setDatabaseStatsCollector(DatabaseStatsCollector databaseStatsCollector) {
            Objects.requireNonNull(databaseStatsCollector);
            mDatabaseStatsCollector = databaseStatsCollector;
            return this;
        }

        /** Set fake or custom {@link UsageStatsCollector} */
        public Builder setUsageStatsCollector(UsageStatsCollector usageStatsCollector) {
            Objects.requireNonNull(usageStatsCollector);
            mUsageStatsCollector = usageStatsCollector;
            return this;
        }

        /** Set fake or custom {@link ReadAccessLogsHelper} */
        public Builder setReadAccessLogsHelper(ReadAccessLogsHelper readAccessLogsHelper) {
            Objects.requireNonNull(readAccessLogsHelper);
            mReadAccessLogsHelper = readAccessLogsHelper;
            return this;
        }

        /** Set a custom directory to use instead of {@link Environment#getDataDirectory()}. */
        public Builder setEnvironmentDataDirectory(File environmentDataDirectory) {
            mEnvironmentDataDirectory = Objects.requireNonNull(environmentDataDirectory);
            return this;
        }

        /** Set fake or custom {@link AppOpsManagerLocal}. */
        public Builder setAppOpsManagerLocal(AppOpsManagerLocal appOpsManagerLocal) {
            mAppOpsManagerLocal = Objects.requireNonNull(appOpsManagerLocal);
            return this;
        }

        /** Set fake or custom {@link HealthConnectThreadScheduler}. */
        public Builder setThreadScheduler(HealthConnectThreadScheduler threadScheduler) {
            mThreadScheduler = Objects.requireNonNull(threadScheduler);
            return this;
        }

        /** Set fake or custom {@link DeviceDataSourcesHelper}. */
        public Builder setDeviceDataSourcesHelper(DeviceDataSourcesHelper deviceDataSourcesHelper) {
            mDeviceDataSourcesHelper = Objects.requireNonNull(deviceDataSourcesHelper);
            return this;
        }

        /** Set fake or custom {@link HealthFitnessStatsLog}. */
        public Builder setHealthFitnessStatsLog(HealthFitnessStatsLog statsLog) {
            mStatsLog = Objects.requireNonNull(statsLog);
            return this;
        }

        /** Set fake or custom {@link TrackerManager}. */
        public Builder setTrackerManager(TrackerManager trackerManager) {
            mTrackerManager = Objects.requireNonNull(trackerManager);
            return this;
        }

        /** Set a fake or custom {@link MigrationUtils}. */
        public Builder setMigrationUtils(MigrationUtils migrationUtils) {
            mMigrationUtils = migrationUtils;
            return this;
        }

        /** Set fake or custom {@link TrackerManager}. */
        public Builder setHealthConnectResourcesContext(
                HealthConnectResourcesContext resourcesContext) {
            mResourcesContext = Objects.requireNonNull(resourcesContext);
            return this;
        }

        /** Set fake or custom {@link UserManager}. */
        public Builder setUserManager(UserManager userManager) {
            mUserManager = Objects.requireNonNull(userManager);
            return this;
        }

        /** Build HealthConnectInjector */
        public HealthConnectInjector build() {
            return new HealthConnectInjectorImpl(this);
        }
    }
}
