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

import static android.Manifest.permission.BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS;
import static android.Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA;
import static android.Manifest.permission.RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.health.connect.Constants.READ;
import static android.health.connect.HealthConnectException.ERROR_INTERNAL;
import static android.health.connect.HealthConnectException.ERROR_INVALID_ARGUMENT;
import static android.health.connect.HealthConnectException.ERROR_IO;
import static android.health.connect.HealthConnectException.ERROR_SECURITY;
import static android.health.connect.HealthConnectException.ERROR_UNKNOWN;
import static android.health.connect.HealthConnectException.ERROR_UNSUPPORTED_OPERATION;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_HISTORY;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.datatypes.MedicalDataSource.validateMedicalDataSourceIds;

import static com.android.healthfitness.flags.AconfigFlagHelper.isCloudBackupRestoreEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled;
import static com.android.healthfitness.flags.Flags.personalHealthRecordTelemetry;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.CREATE_MEDICAL_DATA_SOURCE;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.DELETE_DATA;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_DATA_SOURCE_WITH_DATA;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.GET_CHANGES;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.GET_CHANGES_TOKEN;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_IDS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.INSERT_DATA;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.READ_AGGREGATED_DATA;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.READ_DATA;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.UPDATE_DATA;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.UPSERT_MEDICAL_RESOURCES;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import android.Manifest;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.content.AttributionSource;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteException;
import android.health.HealthFitnessStatsLog;
import android.health.connect.Constants;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthConnectManager.DataDownloadState;
import android.health.connect.HealthDataCategory;
import android.health.connect.MedicalResourceId;
import android.health.connect.MedicalResourceTypeInfo;
import android.health.connect.PageTokenWrapper;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.accesslog.AccessLogsResponseParcel;
import android.health.connect.aidl.ActivityDatesRequestParcel;
import android.health.connect.aidl.ActivityDatesResponseParcel;
import android.health.connect.aidl.AggregateDataRequestParcel;
import android.health.connect.aidl.ApplicationInfoResponseParcel;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.GetPriorityResponseParcel;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IAccessLogsResponseCallback;
import android.health.connect.aidl.IActivityDatesResponseCallback;
import android.health.connect.aidl.IAggregateRecordsResponseCallback;
import android.health.connect.aidl.IApplicationInfoResponseCallback;
import android.health.connect.aidl.ICanRestoreResponseCallback;
import android.health.connect.aidl.IChangeLogsResponseCallback;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangeLogTokenCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetHealthConnectDataStateCallback;
import android.health.connect.aidl.IGetHealthConnectMigrationUiStateCallback;
import android.health.connect.aidl.IGetLatestMetadataForBackupResponseCallback;
import android.health.connect.aidl.IGetPriorityResponseCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IInsertRecordsResponseCallback;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMedicalResourceTypeInfosCallback;
import android.health.connect.aidl.IMedicalResourcesResponseCallback;
import android.health.connect.aidl.IMigrationCallback;
import android.health.connect.aidl.IReadMedicalResourcesResponseCallback;
import android.health.connect.aidl.IReadRecordsResponseCallback;
import android.health.connect.aidl.IRecordTypeInfoResponseCallback;
import android.health.connect.aidl.InsertRecordsResponseParcel;
import android.health.connect.aidl.MedicalResourceListParcel;
import android.health.connect.aidl.ReadMedicalResourcesRequestParcel;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.aidl.ReadRecordsResponseParcel;
import android.health.connect.aidl.RecordTypeInfoResponseParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.aidl.UpdatePriorityRequestParcel;
import android.health.connect.aidl.UpsertMedicalResourceRequestsParcel;
import android.health.connect.backuprestore.BackupMetadata;
import android.health.connect.backuprestore.RestoreChange;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.exportimport.ExportImportDocumentProvider;
import android.health.connect.exportimport.IImportStatusCallback;
import android.health.connect.exportimport.IQueryDocumentProvidersCallback;
import android.health.connect.exportimport.IScheduledExportStatusCallback;
import android.health.connect.exportimport.ImportStatus;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.exportimport.ScheduledExportStatus;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.health.connect.internal.datatypes.utils.MedicalResourceTypePermissionMapper;
import android.health.connect.migration.HealthConnectMigrationUiState;
import android.health.connect.migration.MigrationEntityParcel;
import android.health.connect.migration.MigrationException;
import android.health.connect.ratelimiter.RateLimiter;
import android.health.connect.ratelimiter.RateLimiter.QuotaCategory;
import android.health.connect.ratelimiter.RateLimiterException;
import android.health.connect.restore.BackupFileNamesSet;
import android.health.connect.restore.StageRemoteDataException;
import android.health.connect.restore.StageRemoteDataRequest;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.backuprestore.BackupRestore;
import com.android.server.healthconnect.backuprestore.CloudBackupManager;
import com.android.server.healthconnect.backuprestore.CloudRestoreManager;
import com.android.server.healthconnect.common.RequestContext;
import com.android.server.healthconnect.exportimport.DocumentProvidersManager;
import com.android.server.healthconnect.exportimport.ExportImportJobs;
import com.android.server.healthconnect.exportimport.ExportManager;
import com.android.server.healthconnect.exportimport.ImportManager;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.aggregation.FitnessRecordAggregateHelper;
import com.android.server.healthconnect.logging.BackupRestoreLogger;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.logging.HealthConnectServiceLogger;
import com.android.server.healthconnect.migration.DataMigrationManager;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.permission.DataPermissionEnforcer;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.MedicalDataPermissionEnforcer;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.phr.validations.FhirResourceValidator;
import com.android.server.healthconnect.phr.validations.MedicalResourceValidator;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ActivityDateHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseHelper.DatabaseHelpers;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MigrationEntityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.UpsertMedicalResourceInternalRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.utils.PreferencesManager;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.utils.TimeSource;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * IHealthConnectService's implementation
 *
 * @hide
 */
final class HealthConnectServiceImpl extends IHealthConnectService.Stub {
    private static final String TAG = "HealthConnectService";
    // Permission for test api for deleting staged data
    private static final String DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA_PERMISSION =
            "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA";
    // Allows an application to act as a backup inter-agent to send and receive HealthConnect data
    private static final String HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION =
            "android.permission.HEALTH_CONNECT_BACKUP_INTER_AGENT";

    @Nullable private final ImportManager mImportManager;

    private final TransactionManager mTransactionManager;
    @Nullable private final CloudBackupManager mCloudBackupManager;
    @Nullable private final CloudRestoreManager mCloudRestoreManager;
    private final HealthConnectPermissionHelper mPermissionHelper;
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final Context mContext;
    private final PermissionManager mPermissionManager;

    private final BackupRestore mBackupRestore;
    private final MigrationStateManager mMigrationStateManager;

    private final DataPermissionEnforcer mDataPermissionEnforcer;

    private final MedicalDataPermissionEnforcer mMedicalDataPermissionEnforcer;

    private final AppOpsManagerLocal mAppOpsManagerLocal;
    private final MigrationUiStateManager mMigrationUiStateManager;

    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final PriorityMigrationHelper mPriorityMigrationHelper;
    private final AggregationTypeIdMapper mAggregationTypeIdMapper;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final ExportImportSettingsStorage mExportImportSettingsStorage;
    private final PreferenceHelper mPreferenceHelper;
    private final FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
    private final FitnessRecordReadHelper mFitnessRecordReadHelper;
    private final FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    private final FitnessRecordAggregateHelper mFitnessRecordAggregateHelper;
    private final MedicalResourceHelper mMedicalResourceHelper;
    private final MedicalDataSourceHelper mMedicalDataSourceHelper;
    private final ExportManager mExportManager;
    private final AccessLogsHelper mAccessLogsHelper;
    private final ActivityDateHelper mActivityDateHelper;
    private final ChangeLogsHelper mChangeLogsHelper;
    private final ChangeLogsRequestHelper mChangeLogsRequestHelper;
    private final MigrationEntityHelper mMigrationEntityHelper;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final HealthConnectMappings mHealthConnectMappings;
    private final TimeSource mTimeSource;
    private final DatabaseHelpers mDatabaseHelpers;
    private final PreferencesManager mPreferencesManager;
    private final ReadAccessLogsHelper mReadAccessLogsHelper;
    private final RateLimiter mRateLimiter;
    // Used if PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE is false.
    @Nullable private FhirResourceValidator mFhirResourceValidator;
    // Used if PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE is true.
    WeakReference<FhirResourceValidator> mFhirResourceValidatorWeakReference =
            new WeakReference<>(null);
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final HealthFitnessStatsLog mStatsLog;

    private volatile UserHandle mCurrentForegroundUser;

    HealthConnectServiceImpl(
            Context context,
            TimeSource timeSource,
            InternalHealthConnectMappings internalHealthConnectMappings,
            TransactionManager transactionManager,
            HealthConnectPermissionHelper permissionHelper,
            FirstGrantTimeManager firstGrantTimeManager,
            MigrationEntityHelper migrationEntityHelper,
            MigrationStateManager migrationStateManager,
            MigrationUiStateManager migrationUiStateManager,
            MigrationCleaner migrationCleaner,
            FitnessRecordUpsertHelper fitnessRecordUpsertHelper,
            FitnessRecordReadHelper fitnessRecordReadHelper,
            FitnessRecordDeleteHelper fitnessRecordDeleteHelper,
            FitnessRecordAggregateHelper fitnessRecordAggregateHelper,
            MedicalResourceHelper medicalResourceHelper,
            MedicalDataSourceHelper medicalDataSourceHelper,
            ExportManager exportManager,
            ExportImportSettingsStorage exportImportSettingsStorage,
            HealthConnectNotificationSender exportImportNotificationSender,
            BackupRestore backupRestore,
            AccessLogsHelper accessLogsHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            ActivityDateHelper activityDateHelper,
            ChangeLogsHelper changeLogsHelper,
            ChangeLogsRequestHelper changeLogsRequestHelper,
            PriorityMigrationHelper priorityMigrationHelper,
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper,
            PreferenceHelper preferenceHelper,
            DatabaseHelpers databaseHelpers,
            PreferencesManager preferencesManager,
            ReadAccessLogsHelper readAccessLogsHelper,
            AppOpsManagerLocal appOpsManagerLocal,
            HealthConnectThreadScheduler threadScheduler,
            RateLimiter rateLimiter,
            File environmentDataDirectory,
            ExportImportLogger exportImportLogger,
            HealthFitnessStatsLog statsLog,
            BackupRestoreLogger backupRestoreLogger) {
        mContext = context;
        mCurrentForegroundUser = context.getUser();
        mTimeSource = timeSource;

        mInternalHealthConnectMappings = internalHealthConnectMappings;
        mHealthConnectMappings = internalHealthConnectMappings.getExternalMappings();
        mAggregationTypeIdMapper = AggregationTypeIdMapper.getInstance();

        mTransactionManager = transactionManager;
        mPermissionHelper = permissionHelper;
        mFirstGrantTimeManager = firstGrantTimeManager;

        mMigrationEntityHelper = migrationEntityHelper;
        mMigrationStateManager = migrationStateManager;
        mMigrationUiStateManager = migrationUiStateManager;
        mMigrationUiStateManager.attachTo(migrationStateManager);
        migrationCleaner.attachTo(migrationStateManager);

        mFitnessRecordUpsertHelper = fitnessRecordUpsertHelper;
        mFitnessRecordReadHelper = fitnessRecordReadHelper;
        mFitnessRecordDeleteHelper = fitnessRecordDeleteHelper;
        mFitnessRecordAggregateHelper = fitnessRecordAggregateHelper;
        mMedicalResourceHelper = medicalResourceHelper;
        mMedicalDataSourceHelper = medicalDataSourceHelper;

        mExportManager = exportManager;
        mExportImportSettingsStorage = exportImportSettingsStorage;
        mBackupRestore = backupRestore;

        mAccessLogsHelper = accessLogsHelper;
        mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
        mActivityDateHelper = activityDateHelper;
        mChangeLogsHelper = changeLogsHelper;
        mChangeLogsRequestHelper = changeLogsRequestHelper;
        mPriorityMigrationHelper = priorityMigrationHelper;
        mAppInfoHelper = appInfoHelper;
        mDeviceInfoHelper = deviceInfoHelper;
        mPreferenceHelper = preferenceHelper;
        mDatabaseHelpers = databaseHelpers;
        mPreferencesManager = preferencesManager;
        mReadAccessLogsHelper = readAccessLogsHelper;
        mThreadScheduler = threadScheduler;
        mRateLimiter = rateLimiter;

        mPermissionManager = mContext.getSystemService(PermissionManager.class);
        mAppOpsManagerLocal = appOpsManagerLocal;
        mMedicalDataPermissionEnforcer = new MedicalDataPermissionEnforcer(mPermissionManager);
        mDataPermissionEnforcer =
                new DataPermissionEnforcer(
                        mPermissionManager, mContext, internalHealthConnectMappings);
        Clock clockForLogging = Clock.systemUTC();
        mImportManager =
                new ImportManager(
                        mAppInfoHelper,
                        mContext,
                        mExportImportSettingsStorage,
                        mTransactionManager,
                        mFitnessRecordUpsertHelper,
                        mFitnessRecordReadHelper,
                        mDeviceInfoHelper,
                        mHealthDataCategoryPriorityHelper,
                        Flags.exportImportFastFollow() ? clockForLogging : null,
                        exportImportNotificationSender,
                        environmentDataDirectory,
                        exportImportLogger);

        mCloudBackupManager =
                // TODO(b/400105647): Remove duplicate flag check once excess code size is resolved.
                Flags.cloudBackupAndRestore() && isCloudBackupRestoreEnabled()
                        ? new CloudBackupManager(
                                mTransactionManager,
                                mFitnessRecordReadHelper,
                                mAppInfoHelper,
                                mDeviceInfoHelper,
                                mHealthConnectMappings,
                                mInternalHealthConnectMappings,
                                mChangeLogsHelper,
                                mChangeLogsRequestHelper,
                                mHealthDataCategoryPriorityHelper,
                                mPreferenceHelper,
                                clockForLogging,
                                backupRestoreLogger)
                        : null;
        mCloudRestoreManager =
                // TODO(b/400105647): Remove duplicate flag check once excess code size is resolved.
                Flags.cloudBackupAndRestore() && isCloudBackupRestoreEnabled()
                        ? new CloudRestoreManager(
                                mTransactionManager,
                                mFitnessRecordUpsertHelper,
                                mFitnessRecordReadHelper,
                                mInternalHealthConnectMappings,
                                mDeviceInfoHelper,
                                mAppInfoHelper,
                                mHealthDataCategoryPriorityHelper,
                                mPreferenceHelper,
                                clockForLogging,
                                backupRestoreLogger)
                        : null;
        mStatsLog = statsLog;
    }

    public void setupForUser(UserHandle currentForegroundUser) {
        mCurrentForegroundUser = currentForegroundUser;
    }

    @Override
    public void grantHealthPermission(String packageName, String permissionName, UserHandle user) {
        checkParamsNonNull(packageName, permissionName, user);

        throwIllegalStateExceptionIfDataSyncInProgress();
        mPermissionHelper.grantHealthPermission(packageName, permissionName, user);
    }

    @Override
    public void revokeHealthPermission(
            String packageName, String permissionName, @Nullable String reason, UserHandle user) {
        checkParamsNonNull(packageName, permissionName, user);

        throwIllegalStateExceptionIfDataSyncInProgress();
        mPermissionHelper.revokeHealthPermission(packageName, permissionName, reason, user);
    }

    @Override
    public void revokeAllHealthPermissions(
            String packageName, @Nullable String reason, UserHandle user) {
        checkParamsNonNull(packageName, user);

        throwIllegalStateExceptionIfDataSyncInProgress();
        mPermissionHelper.revokeAllHealthPermissions(packageName, reason, user);
    }

    @Override
    public List<String> getGrantedHealthPermissions(String packageName, UserHandle user) {
        checkParamsNonNull(packageName, user);

        throwIllegalStateExceptionIfDataSyncInProgress();
        List<String> grantedPermissions =
                mPermissionHelper.getGrantedHealthPermissions(packageName, user);
        return grantedPermissions;
    }

    @Override
    public Map<String, Integer> getHealthPermissionsFlags(
            String packageName, UserHandle user, List<String> permissions) {
        checkParamsNonNull(packageName, user);
        throwIllegalStateExceptionIfDataSyncInProgress();

        Map<String, Integer> response =
                mPermissionHelper.getHealthPermissionsFlags(packageName, user, permissions);
        return response;
    }

    @Override
    public void setHealthPermissionsUserFixedFlagValue(
            String packageName, UserHandle user, List<String> permissions, boolean value) {
        checkParamsNonNull(packageName, user);
        throwIllegalStateExceptionIfDataSyncInProgress();

        mPermissionHelper.setHealthPermissionsUserFixedFlagValue(
                packageName, user, permissions, value);
    }

    @Override
    public long getHistoricalAccessStartDateInMilliseconds(
            String packageName, UserHandle userHandle) {
        checkParamsNonNull(packageName, userHandle);

        throwIllegalStateExceptionIfDataSyncInProgress();
        Optional<Instant> date =
                mPermissionHelper.getHealthDataStartDateAccess(packageName, userHandle);
        return date.map(Instant::toEpochMilli).orElse(Constants.DEFAULT_LONG);
    }

    /**
     * Inserts {@code recordsParcel} into the HealthConnect database.
     *
     * @param recordsParcel parcel for list of records to be inserted.
     * @param callback Callback to receive result of performing this operation. The keys returned in
     *     {@link InsertRecordsResponseParcel} are the unique IDs of the input records. The values
     *     are in same order as {@code record}. In case of an error or a permission failure the
     *     HealthConnect service, {@link IInsertRecordsResponseCallback#onError} will be invoked
     *     with a {@link HealthConnectExceptionParcel}.
     */
    @Override
    public void insertRecords(
            AttributionSource attributionSource,
            RecordsParcel recordsParcel,
            IInsertRecordsResponseCallback callback) {
        checkParamsNonNull(attributionSource, recordsParcel, callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(false, INSERT_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionSource.getPackageName());

        ErrorCallback errorCallback = callback::onError;

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    if (hasDataManagementPermission(uid, pid)) {
                        throw new SecurityException(
                                "Apps with android.permission.MANAGE_HEALTH_DATA permission are"
                                        + " not allowed to insert records");
                    }
                    enforceMemoryRateLimit(
                            recordsParcel.getRecordsSize(), recordsParcel.getRecordsChunkSize());
                    final List<RecordInternal<?>> recordInternals = recordsParcel.getRecords();
                    logger.setNumberOfRecords(recordInternals.size());
                    throwExceptionIfDataSyncInProgress();
                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid,
                            QuotaCategory.QUOTA_CATEGORY_WRITE,
                            isInForeground,
                            logger,
                            recordsParcel.getRecordsChunkSize());
                    mDataPermissionEnforcer.enforceRecordsWritePermissions(
                            recordInternals, attributionSource);
                    List<String> uuids =
                            mFitnessRecordUpsertHelper.insertRecords(
                                    Objects.requireNonNull(attributionSource.getPackageName()),
                                    recordInternals,
                                    mDataPermissionEnforcer.collectExtraWritePermissionStateMapping(
                                            recordInternals, attributionSource));
                    tryAndReturnResult(callback, uuids, logger);

                    mThreadScheduler.scheduleInternalTask(
                            () -> postInsertTasks(attributionSource, recordsParcel));

                    logRecordTypeSpecificUpsertMetrics(
                            recordInternals, attributionSource.getPackageName());
                    logger.setDataTypesFromRecordInternals(recordInternals);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    private void postInsertTasks(AttributionSource attributionSource, RecordsParcel recordsParcel) {
        mActivityDateHelper.insertRecordDate(recordsParcel.getRecords());
        Set<Integer> recordsTypesInsertedSet =
                recordsParcel.getRecords().stream()
                        .map(RecordInternal::getRecordType)
                        .collect(toSet());
        // Update AppInfo table with the record types of records inserted in the request for the
        // current package.
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                recordsTypesInsertedSet, attributionSource.getPackageName());
    }

    /**
     * Returns aggregation results based on the {@code request} into the HealthConnect database.
     *
     * @param request represents the request using which the aggregation is to be performed.
     * @param callback Callback to receive result of performing this operation.
     */
    public void aggregateRecords(
            AttributionSource attributionSource,
            AggregateDataRequestParcel request,
            IAggregateRecordsResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, READ_AGGREGATED_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionSource.getPackageName());

        ErrorCallback errorCallback = callback::onError;
        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    logger.setNumberOfRecords(request.getAggregateIds().length);
                    throwExceptionIfDataSyncInProgress();
                    List<Integer> recordTypesToTest = new ArrayList<>();
                    for (int aggregateId : request.getAggregateIds()) {
                        recordTypesToTest.add(
                                mAggregationTypeIdMapper
                                        .getAggregationTypeFor(aggregateId)
                                        .getApplicableRecordTypeId());
                    }

                    long startDateAccess = request.getStartTime();
                    // TODO(b/309776578): Consider making background reads possible for
                    // aggregations when only using own data
                    if (!holdsDataManagementPermission) {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        logger.setCallerForegroundState(isInForeground);

                        if (!isInForeground) {
                            mContext.enforcePermission(
                                    READ_HEALTH_DATA_IN_BACKGROUND,
                                    pid,
                                    uid,
                                    attributionSource.getPackageName()
                                            + "must be in foreground to call aggregate method");
                            if (isBackgroundPermissionFromSplit(attributionSource)) {
                                throw new SecurityException(
                                        "READ_HEALTH_DATA_IN_BACKGROUND is from split permission,"
                                                + " must be explicitly requested and granted");
                            }
                        }
                        tryAcquireApiCallQuota(
                                uid,
                                RateLimiter.QuotaCategory.QUOTA_CATEGORY_READ,
                                isInForeground,
                                logger);
                        boolean enforceSelfRead =
                                mDataPermissionEnforcer.enforceReadAccessAndGetEnforceSelfRead(
                                        recordTypesToTest, attributionSource);
                        if (!isPermissionGranted(READ_HEALTH_DATA_HISTORY, uid, pid)) {
                            startDateAccess =
                                    mPermissionHelper
                                            .getHealthDataStartDateAccessOrThrow(
                                                    attributionSource.getPackageName(), userHandle)
                                            .toEpochMilli();
                        }
                        maybeEnforceOnlyCallingPackageDataRequested(
                                request.getPackageFilters(),
                                attributionSource.getPackageName(),
                                enforceSelfRead,
                                "aggregationTypes: "
                                        + Arrays.stream(request.getAggregateIds())
                                                .mapToObj(
                                                        mAggregationTypeIdMapper
                                                                ::getAggregationTypeFor)
                                                .collect(Collectors.toList()));
                    }
                    boolean shouldRecordAccessLog = !holdsDataManagementPermission;
                    callback.onResult(
                            mFitnessRecordAggregateHelper.aggregateRecords(
                                    attributionSource.getPackageName(),
                                    request,
                                    startDateAccess,
                                    shouldRecordAccessLog));
                    logger.setDataTypesFromRecordTypes(recordTypesToTest)
                            .setHealthDataServiceApiStatusSuccess();
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    /**
     * Read records {@code recordsParcel} from HealthConnect database.
     *
     * @param request ReadRecordsRequestParcel is parcel for the request object containing {@link
     *     RecordIdFiltersParcel}.
     * @param callback Callback to receive result of performing this operation. The records are
     *     returned in {@link RecordsParcel} . In case of an error or a permission failure the
     *     HealthConnect service, {@link IReadRecordsResponseCallback#onError} will be invoked with
     *     a {@link HealthConnectExceptionParcel}.
     */
    @Override
    public void readRecords(
            AttributionSource attributionSource,
            ReadRecordsRequestParcel request,
            IReadRecordsResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);

        ErrorCallback errorCallback = error -> callback.onError(error);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(holdsDataManagementPermission, READ_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();

                    boolean enforceSelfRead = false;

                    final boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);

                    if (!holdsDataManagementPermission) {
                        logger.setCallerForegroundState(isInForeground);

                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                        if (mDataPermissionEnforcer.enforceReadAccessAndGetEnforceSelfRead(
                                request.getRecordType(), attributionSource)) {
                            // If read permission is missing but write permission is granted,
                            // then enforce self read
                            enforceSelfRead = true;
                        } else if (!isInForeground) {
                            // If READ_HEALTH_DATA_IN_BACKGROUND permission is not granted,
                            // then enforce self read
                            enforceSelfRead = shouldEnforceSelfRead(uid, pid, attributionSource);
                        }
                        if (request.getRecordIdFiltersParcel() == null) {
                            // Only enforce requested packages if this is a
                            // ReadRecordsByRequest using filters. Reading by IDs does not have
                            // data origins specified.
                            // TODO(b/309778116): Consider throwing an error when reading by Id
                            maybeEnforceOnlyCallingPackageDataRequested(
                                    request.getPackageFilters(),
                                    callingPackageName,
                                    enforceSelfRead,
                                    "recordType: "
                                            + mHealthConnectMappings
                                                    .getRecordIdToExternalRecordClassMap()
                                                    .get(request.getRecordType()));
                        }

                        if (Constants.DEBUG) {
                            Slog.d(
                                    TAG,
                                    "Enforce self read for package "
                                            + callingPackageName
                                            + ":"
                                            + enforceSelfRead);
                        }
                    }
                    final Set<String> grantedExtraReadPermissions =
                            mDataPermissionEnforcer.collectGrantedExtraReadPermissions(
                                    Set.of(request.getRecordType()), attributionSource);

                    try {
                        long startDateAccessEpochMilli = request.getStartTime();

                        if (!holdsDataManagementPermission
                                && !isPermissionGranted(READ_HEALTH_DATA_HISTORY, uid, pid)) {
                            Instant startDateAccessInstant =
                                    mPermissionHelper.getHealthDataStartDateAccessOrThrow(
                                            callingPackageName, userHandle);

                            // Always set the startDateAccess for local time filter, as for
                            // local date time we use it in conjunction with the time filter
                            // start-time
                            if (request.usesLocalTimeFilter()
                                    || startDateAccessInstant.toEpochMilli()
                                            > startDateAccessEpochMilli) {
                                startDateAccessEpochMilli = startDateAccessInstant.toEpochMilli();
                            }
                        }

                        boolean shouldRecordAccessLog =
                                !holdsDataManagementPermission && !enforceSelfRead;
                        Pair<List<RecordInternal<?>>, PageTokenWrapper> readRecordsResponse =
                                mFitnessRecordReadHelper.readRecords(
                                        mTransactionManager,
                                        callingPackageName,
                                        request,
                                        grantedExtraReadPermissions,
                                        startDateAccessEpochMilli,
                                        isInForeground,
                                        shouldRecordAccessLog,
                                        enforceSelfRead,
                                        /* packageNamesByAppIds= */ null);
                        List<RecordInternal<?>> records = readRecordsResponse.first;
                        long pageToken = readRecordsResponse.second.encode();

                        logger.setNumberOfRecords(records.size());

                        if (Constants.DEBUG) {
                            Slog.d(TAG, "pageToken: " + pageToken);
                        }

                        if (!Flags.addMissingAccessLogs()) {
                            // Calls from controller APK should not be recorded in access logs
                            // If an app is reading only its own data then it is not recorded in
                            // access logs.
                            if (!holdsDataManagementPermission && !enforceSelfRead) {
                                final List<Integer> recordTypes =
                                        singletonList(request.getRecordType());
                                mAccessLogsHelper.addAccessLog(
                                        callingPackageName, recordTypes, READ);
                            }
                        }

                        callback.onResult(
                                new ReadRecordsResponseParcel(
                                        new RecordsParcel(records), pageToken));
                        logRecordTypeSpecificReadMetrics(records, callingPackageName);
                        logger.setDataTypesFromRecordInternals(records)
                                .setHealthDataServiceApiStatusSuccess();
                    } catch (TypeNotPresentException exception) {
                        // All the requested package names are not present, so simply
                        // return an empty list
                        if (FitnessRecordReadHelper.TYPE_NOT_PRESENT_PACKAGE_NAME.equals(
                                exception.typeName())) {
                            if (Constants.DEBUG) {
                                Slog.d(TAG, "No app info recorded for " + callingPackageName);
                            }
                            callback.onResult(
                                    new ReadRecordsResponseParcel(
                                            new RecordsParcel(new ArrayList<>()), DEFAULT_LONG));
                            logger.setHealthDataServiceApiStatusSuccess();
                        } else {
                            logger.setHealthDataServiceApiStatusError(
                                    HealthConnectException.ERROR_UNKNOWN);
                            throw exception;
                        }
                    }
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    private void maybeEnforceOnlyCallingPackageDataRequested(
            List<String> packageFilters,
            String callingPackageName,
            boolean enforceSelfRead,
            String entityFailureMessage) {
        if (enforceSelfRead
                && (packageFilters.size() != 1
                        || !packageFilters.get(0).equals(callingPackageName))) {
            throw new SecurityException(
                    "Caller does not have permission to read data for the following ("
                            + entityFailureMessage
                            + ") from other applications.");
        }
    }

    /**
     * Updates {@code recordsParcel} into the HealthConnect database.
     *
     * @param recordsParcel parcel for list of records to be updated.
     * @param callback Callback to receive result of performing this operation. In case of an error
     *     or a permission failure the HealthConnect service, {@link IEmptyResponseCallback#onError}
     *     will be invoked with a {@link HealthConnectException}.
     */
    @Override
    public void updateRecords(
            AttributionSource attributionSource,
            RecordsParcel recordsParcel,
            IEmptyResponseCallback callback) {
        checkParamsNonNull(attributionSource, recordsParcel, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(false, UPDATE_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionSource.getPackageName());
        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    if (hasDataManagementPermission(uid, pid)) {
                        throw new SecurityException(
                                "Apps with android.permission.MANAGE_HEALTH_DATA permission are"
                                        + " not allowed to insert records");
                    }
                    enforceMemoryRateLimit(
                            recordsParcel.getRecordsSize(), recordsParcel.getRecordsChunkSize());
                    final List<RecordInternal<?>> recordInternals = recordsParcel.getRecords();
                    logger.setNumberOfRecords(recordInternals.size());
                    throwExceptionIfDataSyncInProgress();
                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid,
                            QuotaCategory.QUOTA_CATEGORY_WRITE,
                            isInForeground,
                            logger,
                            recordsParcel.getRecordsChunkSize());
                    mDataPermissionEnforcer.enforceRecordsWritePermissions(
                            recordInternals, attributionSource);
                    mFitnessRecordUpsertHelper.updateRecords(
                            Objects.requireNonNull(attributionSource.getPackageName()),
                            recordInternals,
                            mDataPermissionEnforcer.collectExtraWritePermissionStateMapping(
                                    recordInternals, attributionSource));
                    tryAndReturnResult(callback, logger);
                    logRecordTypeSpecificUpsertMetrics(
                            recordInternals, attributionSource.getPackageName());
                    logger.setDataTypesFromRecordInternals(recordInternals);
                    // Update activity dates table
                    mThreadScheduler.scheduleInternalTask(
                            () ->
                                    mActivityDateHelper.reSyncByRecordTypeIds(
                                            recordInternals.stream()
                                                    .map(RecordInternal::getRecordType)
                                                    .toList()));
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    /**
     * @see HealthConnectManager#getChangeLogToken
     */
    @Override
    public void getChangeLogToken(
            AttributionSource attributionSource,
            ChangeLogTokenRequest request,
            IGetChangeLogTokenCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);

        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(false, GET_CHANGES_TOKEN)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionSource.getPackageName());
        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    tryAcquireApiCallQuota(
                            uid,
                            QuotaCategory.QUOTA_CATEGORY_READ,
                            mAppOpsManagerLocal.isUidInForeground(uid),
                            logger);
                    throwExceptionIfDataSyncInProgress();
                    if (request.getRecordTypes().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Requested record types must not be empty.");
                    }
                    mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                            request.getRecordTypesList(), attributionSource);
                    callback.onResult(
                            new ChangeLogTokenResponse(
                                    mChangeLogsRequestHelper.getToken(
                                            mChangeLogsHelper.getLatestRowId(),
                                            attributionSource.getPackageName(),
                                            request)));
                    logger.setHealthDataServiceApiStatusSuccess();
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    /**
     * @hide
     * @see HealthConnectManager#getChangeLogs
     */
    @Override
    public void getChangeLogs(
            AttributionSource attributionSource,
            ChangeLogsRequest request,
            IChangeLogsResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);

        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final String callerPackageName = Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(false, GET_CHANGES)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callerPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();

                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    logger.setCallerForegroundState(isInForeground);

                    if (!isInForeground) {
                        mContext.enforcePermission(
                                READ_HEALTH_DATA_IN_BACKGROUND,
                                pid,
                                uid,
                                callerPackageName
                                        + "must be in foreground to call getChangeLogs method");
                        if (isBackgroundPermissionFromSplit(attributionSource)) {
                            throw new SecurityException(
                                    "READ_HEALTH_DATA_IN_BACKGROUND is from split permission,"
                                            + " must be explicitly requested and granted");
                        }
                    }

                    tryAcquireApiCallQuota(
                            uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                    ChangeLogsRequestHelper.TokenRequest changeLogsTokenRequest =
                            mChangeLogsRequestHelper.getRequest(
                                    callerPackageName, request.getToken());
                    if (changeLogsTokenRequest.getRecordTypes().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Requested record types must not be empty.");
                    }
                    // This API doesn't support reading own data without read permissions.
                    mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                            changeLogsTokenRequest.getRecordTypes(), attributionSource);
                    long startDateAccessEpochMilli = DEFAULT_LONG;
                    if (!isPermissionGranted(READ_HEALTH_DATA_HISTORY, uid, pid)) {
                        startDateAccessEpochMilli =
                                mPermissionHelper
                                        .getHealthDataStartDateAccessOrThrow(
                                                callerPackageName, userHandle)
                                        .toEpochMilli();
                    }
                    final ChangeLogsHelper.ChangeLogsResponse changeLogsResponse =
                            mChangeLogsHelper.getChangeLogs(
                                    mAppInfoHelper,
                                    changeLogsTokenRequest,
                                    request,
                                    mChangeLogsRequestHelper);

                    Map<Integer, List<UUID>> recordTypeToInsertedUuids =
                            ChangeLogsHelper.getRecordTypeToInsertedUuids(
                                    changeLogsResponse.getChangeLogsMap());

                    Set<String> grantedExtraReadPermissions =
                            mDataPermissionEnforcer.collectGrantedExtraReadPermissions(
                                    recordTypeToInsertedUuids.keySet(), attributionSource);

                    List<RecordInternal<?>> recordInternals =
                            mFitnessRecordReadHelper.readRecords(
                                    mTransactionManager,
                                    callerPackageName,
                                    recordTypeToInsertedUuids,
                                    grantedExtraReadPermissions,
                                    startDateAccessEpochMilli,
                                    isInForeground,
                                    /* shouldRecordAccessLog= */ true);
                    List<DeletedLog> deletedLogs =
                            ChangeLogsHelper.getDeletedLogs(changeLogsResponse.getChangeLogsMap());

                    callback.onResult(
                            new ChangeLogsResponse(
                                    new RecordsParcel(recordInternals),
                                    deletedLogs,
                                    changeLogsResponse.getNextPageToken(),
                                    changeLogsResponse.hasMorePages()));
                    logger.setHealthDataServiceApiStatusSuccess()
                            .setNumberOfRecords(recordInternals.size() + deletedLogs.size())
                            .setDataTypesFromRecordInternals(recordInternals);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    /** API to delete records based on {@code request}. */
    @Override
    public void deleteUsingFilters(
            AttributionSource attributionSource,
            DeleteUsingFiltersRequestParcel request,
            IEmptyResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(holdsDataManagementPermission, DELETE_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionSource.getPackageName());

        final RequestContext requestContext = RequestContext.create();

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(requestContext.getCallingUser());
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    List<Integer> recordTypeIdsToDelete =
                            (!request.getRecordTypeFilters().isEmpty())
                                    ? request.getRecordTypeFilters()
                                    : new ArrayList<>(
                                            mHealthConnectMappings
                                                    .getRecordIdToExternalRecordClassMap()
                                                    .keySet());

                    if (!holdsDataManagementPermission) {
                        tryAcquireApiCallQuota(
                                uid,
                                QuotaCategory.QUOTA_CATEGORY_WRITE,
                                mAppOpsManagerLocal.isUidInForeground(uid),
                                logger);
                        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                                recordTypeIdsToDelete, attributionSource);
                    }

                    int numberOfRecordsDeleted =
                            mFitnessRecordDeleteHelper.deleteRecords(
                                    Objects.requireNonNull(attributionSource.getPackageName()),
                                    request,
                                    /* enforceSelfDelete= */ !holdsDataManagementPermission,
                                    /* shouldRecordAccessLog= */ !holdsDataManagementPermission);
                    tryAndReturnResult(callback, logger);
                    mThreadScheduler.scheduleInternalTask(
                            () -> postDeleteTasks(recordTypeIdsToDelete));
                    logger.setNumberOfRecords(numberOfRecordsDeleted)
                            .setDataTypesFromRecordTypes(recordTypeIdsToDelete);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    /** API to get Priority for {@code dataCategory} */
    @Override
    public void getCurrentPriority(
            @HealthDataCategory.Type int dataCategory, IGetPriorityResponseCallback callback) {
        checkParamsNonNull(callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        List<DataOrigin> dataOriginInPriorityOrder =
                                mHealthDataCategoryPriorityHelper
                                        .syncAndGetPriorityOrder(dataCategory)
                                        .stream()
                                        .map(
                                                (name) ->
                                                        new DataOrigin.Builder()
                                                                .setPackageName(name)
                                                                .build())
                                        .collect(toList());
                        callback.onResult(
                                new GetPriorityResponseParcel(
                                        new FetchDataOriginsPriorityOrderResponse(
                                                dataOriginInPriorityOrder)));
                    } catch (SQLiteException sqLiteException) {
                        Slog.e(TAG, "SQLiteException: ", sqLiteException);
                        tryAndThrowException(errorCallback, sqLiteException, ERROR_IO);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(errorCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        Slog.e(TAG, "Exception: ", exception);
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    /** API to update priority for permission category(ies) */
    @Override
    public void updatePriority(
            UpdatePriorityRequestParcel updatePriorityRequest, IEmptyResponseCallback callback) {
        checkParamsNonNull(updatePriorityRequest, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                                updatePriorityRequest.getDataCategory(),
                                updatePriorityRequest.getPackagePriorityOrder());
                        callback.onResult();
                    } catch (SQLiteException sqLiteException) {
                        Slog.e(TAG, "SQLiteException: ", sqLiteException);
                        tryAndThrowException(errorCallback, sqLiteException, ERROR_IO);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(errorCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        Slog.e(TAG, "Exception: ", exception);
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    public void setRecordRetentionPeriodInDays(
            int days, UserHandle user, IEmptyResponseCallback callback) {
        checkParamsNonNull(user, callback);
        ErrorCallback wrappedCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        mPreferencesManager.setRecordRetentionPeriodInDays(days);
                        callback.onResult();
                    } catch (SQLiteException sqLiteException) {
                        Slog.e(TAG, "SQLiteException: ", sqLiteException);
                        tryAndThrowException(wrappedCallback, sqLiteException, ERROR_IO);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(wrappedCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                wrappedCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        Slog.e(TAG, "Exception: ", exception);
                        tryAndThrowException(wrappedCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    public int getRecordRetentionPeriodInDays(UserHandle user) {
        checkParamsNonNull(user);

        enforceIsForegroundUser(getCallingUserHandle());
        throwExceptionIfDataSyncInProgress();
        try {
            mContext.enforceCallingPermission(MANAGE_HEALTH_DATA_PERMISSION, null);
            return mPreferencesManager.getRecordRetentionPeriodInDays();
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                throw e;
            }
            Slog.e(TAG, "Unable to get record retention period for " + user);
        }

        throw new RuntimeException();
    }

    /**
     * Returns information, represented by {@code ApplicationInfoResponse}, for all the packages
     * that have contributed to the health connect DB.
     *
     * @param callback Callback to receive result of performing this operation. In case of an error
     *     or a permission failure the HealthConnect service, {@link IEmptyResponseCallback#onError}
     *     will be invoked with a {@link HealthConnectException}.
     */
    @Override
    public void getContributorApplicationsInfo(IApplicationInfoResponseCallback callback) {
        checkParamsNonNull(callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        // Get AppInfo IDs which has PHR data.
                        Set<Long> appIdsWithPhrData = Set.of();
                        if (isPersonalHealthRecordEnabled()) {
                            appIdsWithPhrData =
                                    mMedicalDataSourceHelper.getAllContributorAppInfoIds();
                        }
                        // Get all AppInfos which has either Fitness data or PHR data.
                        List<AppInfo> applicationInfosWithData =
                                mAppInfoHelper.getApplicationInfosWithRecordTypesOrInIdsList(
                                        appIdsWithPhrData);
                        callback.onResult(
                                new ApplicationInfoResponseParcel(applicationInfosWithData));
                    } catch (SQLiteException sqLiteException) {
                        Slog.e(TAG, "SqlException: ", sqLiteException);
                        tryAndThrowException(errorCallback, sqLiteException, ERROR_IO);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(errorCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(errorCallback, e, ERROR_INTERNAL);
                    }
                });
    }

    /** Retrieves {@link RecordTypeInfoResponse} for each RecordType. */
    @Override
    public void queryAllRecordTypesInfo(IRecordTypeInfoResponseCallback callback) {
        checkParamsNonNull(callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        callback.onResult(
                                new RecordTypeInfoResponseParcel(
                                        getPopulatedRecordTypeInfoResponses()));
                    } catch (SQLiteException sqLiteException) {
                        tryAndThrowException(errorCallback, sqLiteException, ERROR_IO);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(errorCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    /**
     * @see HealthConnectManager#queryAccessLogs
     */
    @Override
    public void queryAccessLogs(String packageName, IAccessLogsResponseCallback callback) {
        checkParamsNonNull(packageName, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        final List<AccessLog> accessLogsList =
                                mAccessLogsHelper.queryAccessLogs(userHandle);
                        callback.onResult(new AccessLogsResponseParcel(accessLogsList));
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(errorCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        Slog.e(TAG, "Exception: ", exception);
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    /**
     * Returns a list of unique dates for which the database has at least one entry
     *
     * @param activityDatesRequestParcel Parcel request containing records classes
     * @param callback Callback to receive result of performing this operation. The results are
     *     returned in {@link List<LocalDate>} . In case of an error or a permission failure the
     *     HealthConnect service, {@link IActivityDatesResponseCallback#onError} will be invoked
     *     with a {@link HealthConnectExceptionParcel}.
     */
    @Override
    public void getActivityDates(
            ActivityDatesRequestParcel activityDatesRequestParcel,
            IActivityDatesResponseCallback callback) {
        checkParamsNonNull(activityDatesRequestParcel, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        List<LocalDate> localDates =
                                mActivityDateHelper.getActivityDates(
                                        activityDatesRequestParcel.getRecordTypes());

                        callback.onResult(new ActivityDatesResponseParcel(localDates));
                    } catch (SQLiteException sqLiteException) {
                        Slog.e(TAG, "SqlException: ", sqLiteException);
                        tryAndThrowException(errorCallback, sqLiteException, ERROR_IO);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(errorCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(errorCallback, e, ERROR_INTERNAL);
                    }
                });
    }

    /**
     * Changes migration state to {@link HealthConnectDataState#MIGRATION_STATE_IN_PROGRESS} if the
     * current state allows migration to be started.
     *
     * @param packageName calling package name
     * @param callback Callback to receive a result or an error encountered while performing this
     *     operation.
     */
    @Override
    public void startMigration(String packageName, IMigrationCallback callback) {
        checkParamsNonNull(packageName, callback);

        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        mThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(
                                MIGRATE_HEALTH_CONNECT_DATA,
                                pid,
                                uid,
                                "Caller does not have " + MIGRATE_HEALTH_CONNECT_DATA);
                        enforceShowMigrationInfoIntent(packageName, uid);
                        mBackupRestore.runWithStatesReadLock(
                                () -> {
                                    if (mBackupRestore.isRestoreMergingInProgress()) {
                                        throw new MigrationException(
                                                "Cannot start data migration. Backup and restore in"
                                                        + " progress.",
                                                MigrationException.ERROR_INTERNAL,
                                                null);
                                    }
                                    mMigrationStateManager.startMigration(mContext);
                                });
                        mPriorityMigrationHelper.populatePreMigrationPriority();
                        callback.onSuccess();
                    } catch (Exception e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(callback, e, MigrationException.ERROR_INTERNAL, null);
                    }
                });
    }

    /**
     * Changes migration state to {@link HealthConnectDataState#MIGRATION_STATE_COMPLETE} if
     * migration is not already complete.
     *
     * @param packageName calling package name
     * @param callback Callback to receive a result or an error encountered while performing this
     *     operation.
     */
    @Override
    public void finishMigration(String packageName, IMigrationCallback callback) {
        checkParamsNonNull(packageName, callback);

        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        mThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(
                                MIGRATE_HEALTH_CONNECT_DATA,
                                pid,
                                uid,
                                "Caller does not have " + MIGRATE_HEALTH_CONNECT_DATA);
                        enforceShowMigrationInfoIntent(packageName, uid);
                        mMigrationStateManager.finishMigration(mContext);
                        mAppInfoHelper.syncAppInfoRecordTypesUsed();
                        callback.onSuccess();
                    } catch (Exception e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(callback, e, MigrationException.ERROR_INTERNAL, null);
                    }
                });
    }

    /**
     * Write data to module storage. The migration state must be {@link
     * HealthConnectDataState#MIGRATION_STATE_IN_PROGRESS} to be able to write data.
     *
     * @param packageName calling package name
     * @param parcel Migration entity containing the data being migrated.
     * @param callback Callback to receive a result or an error encountered while performing this
     *     operation.
     */
    @Override
    public void writeMigrationData(
            String packageName, MigrationEntityParcel parcel, IMigrationCallback callback) {
        checkParamsNonNull(packageName, parcel, callback);

        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        UserHandle callingUserHandle = getCallingUserHandle();

        mThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        enforceIsForegroundUser(callingUserHandle);
                        mContext.enforcePermission(
                                MIGRATE_HEALTH_CONNECT_DATA,
                                pid,
                                uid,
                                "Caller does not have " + MIGRATE_HEALTH_CONNECT_DATA);
                        enforceShowMigrationInfoIntent(packageName, uid);
                        mMigrationStateManager.validateWriteMigrationData();
                        getDataMigrationManager(callingUserHandle)
                                .apply(parcel.getMigrationEntities());
                        callback.onSuccess();
                    } catch (DataMigrationManager.EntityWriteException e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(
                                callback,
                                e,
                                MigrationException.ERROR_MIGRATE_ENTITY,
                                e.getEntityId());
                    } catch (Exception e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(callback, e, MigrationException.ERROR_INTERNAL, null);
                    }
                });
    }

    /**
     * @param packageName calling package name
     * @param requiredSdkExtension The minimum sdk extension version for module to be ready for data
     *     migration from the apk.
     * @param callback Callback to receive a result or an error encountered while performing this
     *     operation.
     */
    public void insertMinDataMigrationSdkExtensionVersion(
            String packageName, int requiredSdkExtension, IMigrationCallback callback) {
        checkParamsNonNull(packageName, callback);

        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        mThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(
                                MIGRATE_HEALTH_CONNECT_DATA,
                                pid,
                                uid,
                                "Caller does not have " + MIGRATE_HEALTH_CONNECT_DATA);
                        enforceShowMigrationInfoIntent(packageName, uid);
                        mMigrationStateManager.validateSetMinSdkVersion();
                        mMigrationStateManager.setMinDataMigrationSdkExtensionVersion(
                                mContext, requiredSdkExtension);

                        callback.onSuccess();
                    } catch (Exception e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(callback, e, MigrationException.ERROR_INTERNAL, null);
                    }
                });
    }

    /**
     * @see HealthConnectManager#stageAllHealthConnectRemoteData
     */
    @Override
    public void stageAllHealthConnectRemoteData(
            StageRemoteDataRequest stageRemoteDataRequest,
            UserHandle userHandle,
            IDataStagingFinishedCallback callback) {
        checkParamsNonNull(stageRemoteDataRequest, userHandle, callback);

        Map<String, ParcelFileDescriptor> origPfdsByFileName =
                stageRemoteDataRequest.getPfdsByFileName();
        Map<String, HealthConnectException> exceptionsByFileName =
                new ArrayMap<>(origPfdsByFileName.size());
        Map<String, ParcelFileDescriptor> pfdsByFileName =
                new ArrayMap<>(origPfdsByFileName.size());

        try {
            mDataPermissionEnforcer.enforceAnyOfPermissions(
                    Manifest.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA,
                    HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION);

            enforceIsForegroundUser(Binder.getCallingUserHandle());

            for (Entry<String, ParcelFileDescriptor> entry : origPfdsByFileName.entrySet()) {
                try {
                    pfdsByFileName.put(entry.getKey(), entry.getValue().dup());
                } catch (IOException e) {
                    Slog.e(TAG, "IOException: ", e);
                    exceptionsByFileName.put(
                            entry.getKey(), new HealthConnectException(ERROR_IO, e.getMessage()));
                }
            }

            mThreadScheduler.scheduleInternalTask(
                    () -> {
                        if (!mBackupRestore.prepForStagingIfNotAlreadyDone()) {
                            try {
                                callback.onResult();
                            } catch (RemoteException e) {
                                Log.e(TAG, "Restore response could not be sent to the caller.", e);
                            }
                            return;
                        }
                        mBackupRestore.stageAllHealthConnectRemoteData(
                                pfdsByFileName, exceptionsByFileName, userHandle, callback);
                    });
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Exception encountered while staging", e);
            try {
                @HealthConnectException.ErrorCode
                int errorCode = (e instanceof SecurityException) ? ERROR_SECURITY : ERROR_INTERNAL;
                exceptionsByFileName.put("", new HealthConnectException(errorCode, e.getMessage()));

                callback.onError(new StageRemoteDataException(exceptionsByFileName));
            } catch (RemoteException remoteException) {
                Log.e(TAG, "Stage data response could not be sent to the caller.", e);
            }
        }
    }

    /**
     * @see HealthConnectManager#getAllDataForBackup
     */
    @Override
    public void getAllDataForBackup(
            StageRemoteDataRequest stageRemoteDataRequest, UserHandle userHandle) {
        checkParamsNonNull(stageRemoteDataRequest, userHandle);

        mContext.enforceCallingPermission(HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION, null);
        final long token = Binder.clearCallingIdentity();
        try {
            mBackupRestore.getAllDataForBackup(stageRemoteDataRequest, userHandle);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * @see HealthConnectManager#getAllBackupFileNames
     */
    @Override
    public BackupFileNamesSet getAllBackupFileNames(boolean forDeviceToDevice) {
        mContext.enforceCallingPermission(HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION, null);
        return mBackupRestore.getAllBackupFileNames(forDeviceToDevice);
    }

    /**
     * @see HealthConnectManager#deleteAllStagedRemoteData
     */
    @Override
    public void deleteAllStagedRemoteData(UserHandle userHandle) {
        checkParamsNonNull(userHandle);

        mContext.enforceCallingPermission(
                DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA_PERMISSION, null);

        int uid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            mBackupRestore.deleteAndResetEverything(userHandle);
            mMigrationStateManager.clearCaches(mContext);
            mDatabaseHelpers.clearAllData(mTransactionManager);
            mRateLimiter.clearCache();
            String[] packageNames = mContext.getPackageManager().getPackagesForUid(uid);
            for (String packageName : packageNames) {
                mFirstGrantTimeManager.setFirstGrantTime(packageName, Instant.now(), userHandle);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * @see HealthConnectManager#setLowerRateLimitsForTesting
     */
    @Override
    public void setLowerRateLimitsForTesting(boolean enabled) {
        // Continue using the existing test permission because we can't grant new permissions
        // to shell in a mainline update.
        mContext.enforceCallingPermission(
                DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA_PERMISSION, null);
        mRateLimiter.setLowerRateLimitsForTesting(enabled);
    }

    /**
     * @see HealthConnectManager#updateDataDownloadState
     */
    @Override
    public void updateDataDownloadState(@DataDownloadState int downloadState) {
        mContext.enforceCallingPermission(
                Manifest.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA, null);
        enforceIsForegroundUser(getCallingUserHandle());
        mBackupRestore.updateDataDownloadState(downloadState);
    }

    /**
     * @see HealthConnectManager#getHealthConnectDataState
     */
    @Override
    public void getHealthConnectDataState(IGetHealthConnectDataStateCallback callback) {
        checkParamsNonNull(callback);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);

        try {
            mDataPermissionEnforcer.enforceAnyOfPermissions(
                    MANAGE_HEALTH_DATA_PERMISSION, Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
            final UserHandle userHandle = Binder.getCallingUserHandle();
            enforceIsForegroundUser(userHandle);
            mThreadScheduler.schedule(
                    mContext,
                    () -> {
                        try {
                            @HealthConnectDataState.DataRestoreError
                            int dataRestoreError = mBackupRestore.getDataRestoreError();
                            @HealthConnectDataState.DataRestoreState
                            int dataRestoreState = mBackupRestore.getDataRestoreState();

                            try {
                                callback.onResult(
                                        new HealthConnectDataState(
                                                dataRestoreState,
                                                dataRestoreError,
                                                mMigrationStateManager.getMigrationState()));
                            } catch (RemoteException remoteException) {
                                Log.e(
                                        TAG,
                                        "HealthConnectDataState could not be sent to the caller.",
                                        remoteException);
                            }
                        } catch (RuntimeException e) {
                            // exception getting the state from the disk
                            try {
                                callback.onError(
                                        new HealthConnectExceptionParcel(
                                                new HealthConnectException(
                                                        ERROR_IO, e.getMessage())));
                            } catch (RemoteException remoteException) {
                                Log.e(
                                        TAG,
                                        "Exception for getHealthConnectDataState could not be sent"
                                                + " to the caller.",
                                        remoteException);
                            }
                        }
                    },
                    uid,
                    holdsDataManagementPermission);
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "getHealthConnectDataState: Exception encountered", e);
            @HealthConnectException.ErrorCode
            int errorCode = (e instanceof SecurityException) ? ERROR_SECURITY : ERROR_INTERNAL;
            try {
                callback.onError(
                        new HealthConnectExceptionParcel(
                                new HealthConnectException(errorCode, e.getMessage())));
            } catch (RemoteException remoteException) {
                Log.e(TAG, "getHealthConnectDataState error could not be sent", e);
            }
        }
    }

    /**
     * @see HealthConnectManager#getHealthConnectMigrationUiState
     */
    @Override
    public void getHealthConnectMigrationUiState(
            IGetHealthConnectMigrationUiStateCallback callback) {
        checkParamsNonNull(callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);

                        try {
                            callback.onResult(
                                    new HealthConnectMigrationUiState(
                                            mMigrationUiStateManager
                                                    .getHealthConnectMigrationUiState()));
                        } catch (RemoteException remoteException) {
                            Log.e(
                                    TAG,
                                    "HealthConnectMigrationUiState could not be sent to the"
                                            + " caller.",
                                    remoteException);
                        }
                    } catch (SecurityException securityException) {
                        try {
                            callback.onError(
                                    new HealthConnectExceptionParcel(
                                            new HealthConnectException(
                                                    ERROR_SECURITY,
                                                    securityException.getMessage())));
                        } catch (RemoteException remoteException) {
                            Log.e(
                                    TAG,
                                    "Exception for HealthConnectMigrationUiState could not be sent"
                                            + " to the caller.",
                                    remoteException);
                        }
                    } catch (RuntimeException e) {
                        // exception getting the state from the disk
                        try {
                            callback.onError(
                                    new HealthConnectExceptionParcel(
                                            new HealthConnectException(ERROR_IO, e.getMessage())));
                        } catch (RemoteException remoteException) {
                            Log.e(
                                    TAG,
                                    "Exception for HealthConnectMigrationUiState could not be sent"
                                            + " to the caller.",
                                    remoteException);
                        }
                    }
                });
    }

    @Override
    public void configureScheduledExport(
            @Nullable ScheduledExportSettings settings, UserHandle user) {
        checkParamsNonNull(user);

        UserHandle userHandle = Binder.getCallingUserHandle();
        enforceIsForegroundUser(userHandle);
        throwExceptionIfDataSyncInProgress();

        try {
            mContext.enforceCallingPermission(MANAGE_HEALTH_DATA_PERMISSION, null);
            mExportImportSettingsStorage.configure(settings);

            // Trigger a one time immediate export when periodic export is scheduled.
            if (Flags.immediateExport() && settings != null && settings.getPeriodInDays() > 0) {
                mThreadScheduler.scheduleInternalTask(
                        () -> {
                            try {
                                final int uid = Binder.getCallingUid();
                                final int pid = Binder.getCallingPid();

                                mContext.enforcePermission(
                                        MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                                mExportManager.runExport(userHandle);
                            } catch (Exception e) {
                                Slog.e(TAG, "Failed to trigger a one time immediate export.", e);
                            }
                        });
            }

            mThreadScheduler.scheduleInternalTask(
                    () -> {
                        try {
                            ExportImportJobs.schedulePeriodicExportJob(
                                    userHandle,
                                    mContext,
                                    mExportImportSettingsStorage,
                                    mExportManager);
                        } catch (Exception e) {
                            Slog.e(TAG, "Failed to schedule periodic export job.", e);
                        }
                    });
        } catch (SQLiteException sqLiteException) {
            Slog.e(TAG, "SQLiteException: ", sqLiteException);
            throw new HealthConnectException(ERROR_IO, sqLiteException.toString());
        } catch (SecurityException securityException) {
            Slog.e(TAG, "SecurityException: ", securityException);
            throw new HealthConnectException(
                    HealthConnectException.ERROR_SECURITY, securityException.toString());
        } catch (HealthConnectException healthConnectException) {
            Slog.e(TAG, "HealthConnectException: ", healthConnectException);
            throw new HealthConnectException(
                    healthConnectException.getErrorCode(), healthConnectException.toString());
        } catch (Exception exception) {
            Slog.e(TAG, "Exception: ", exception);
            throw new HealthConnectException(ERROR_INTERNAL, exception.toString());
        }
    }

    /** Queries status for a scheduled export */
    @Override
    public void getScheduledExportStatus(UserHandle user, IScheduledExportStatusCallback callback) {
        checkParamsNonNull(user, callback);
        ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        final Context userContext = mContext.createContextAsUser(userHandle, 0);
                        ScheduledExportStatus status =
                                mExportImportSettingsStorage.getScheduledExportStatus(userContext);
                        callback.onResult(status);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        Slog.e(TAG, "Exception: ", exception);
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    public int getScheduledExportPeriodInDays(UserHandle user) {
        checkParamsNonNull(user);

        enforceIsForegroundUser(getCallingUserHandle());
        throwExceptionIfDataSyncInProgress();
        try {
            mContext.enforceCallingPermission(MANAGE_HEALTH_DATA_PERMISSION, null);
            return mExportImportSettingsStorage.getScheduledExportPeriodInDays();
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                throw e;
            }
            Slog.e(TAG, "Unable to get period between scheduled exports for " + user);
        }

        throw new RuntimeException();
    }

    /** Queries the status for a data import */
    @Override
    public void getImportStatus(UserHandle user, IImportStatusCallback callback) {
        checkParamsNonNull(user, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        ImportStatus status = mExportImportSettingsStorage.getImportStatus();
                        callback.onResult(status);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        Slog.e(TAG, "Exception: ", exception);
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    public void runImport(UserHandle user, Uri file, IEmptyResponseCallback callback) {
        if (mImportManager == null) return;
        checkParamsNonNull(file);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        mImportManager.runImport(userHandle, file);
                        callback.onResult();
                    } catch (Exception exception) {
                        throw new HealthConnectException(ERROR_IO, exception.toString());
                    }
                });
    }

    @Override
    public void runImmediateExport(Uri file, IEmptyResponseCallback callback) {
        checkParamsNonNull(file);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        // TODO(b/370954019): Modify runExport to use specific file.
                        mExportManager.runExport(userHandle);
                        callback.onResult();
                    } catch (Exception exception) {
                        throw new HealthConnectException(ERROR_IO, exception.toString());
                    }
                });
    }

    /** Queries the document providers available to be used for export/import. */
    @Override
    public void queryDocumentProviders(UserHandle user, IQueryDocumentProvidersCallback callback) {
        checkParamsNonNull(user, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        final Context userContext = mContext.createContextAsUser(userHandle, 0);
                        final List<ExportImportDocumentProvider> providers =
                                DocumentProvidersManager.queryDocumentProviders(userContext);
                        callback.onResult(providers);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        throw new HealthConnectException(
                                HealthConnectException.ERROR_SECURITY,
                                securityException.toString());
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        Slog.e(TAG, "Exception: ", exception);
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    /** Service implementation of {@link HealthConnectManager#createMedicalDataSource} */
    @Override
    public void createMedicalDataSource(
            AttributionSource attributionSource,
            CreateMedicalDataSourceRequest request,
            IMedicalDataSourceResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);
        ErrorCallback errorCallback = callback::onError;
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        UserHandle userHandle = Binder.getCallingUserHandle();
        boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        String packageName = attributionSource.getPackageName();
        HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, CREATE_MEDICAL_DATA_SOURCE)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(packageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Creating MedicalDataSource is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);

                    if (holdsDataManagementPermission) {
                        throw new SecurityException(
                                "Apps with android.permission.MANAGE_HEALTH_DATA permission are"
                                        + " not allowed to insert data");
                    }
                    enforceMemoryRateLimit(List.of(request.getDataSize()), request.getDataSize());
                    throwExceptionIfDataSyncInProgress();
                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid,
                            QuotaCategory.QUOTA_CATEGORY_WRITE,
                            isInForeground,
                            logger,
                            request.getDataSize());

                    mMedicalDataPermissionEnforcer.enforceWriteMedicalDataPermission(
                            attributionSource);

                    MedicalDataSource dataSource =
                            mMedicalDataSourceHelper.createMedicalDataSource(request, packageName);

                    tryAndReturnResult(callback, dataSource, logger);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    /**
     * Service implementation of {@link HealthConnectManager#getMedicalDataSources(List, Executor,
     * OutcomeReceiver)}.
     */
    @Override
    public void getMedicalDataSourcesByIds(
            AttributionSource attributionSource,
            List<String> ids,
            IMedicalDataSourcesResponseCallback callback) {
        checkParamsNonNull(attributionSource, ids, callback);
        final ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, GET_MEDICAL_DATA_SOURCES_BY_IDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Creating MedicalDataSource by ids is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    if (ids.size() > MAXIMUM_PAGE_SIZE) {
                        HealthConnectException invalidSizeException =
                                new HealthConnectException(
                                        ERROR_INVALID_ARGUMENT,
                                        "The number of requested IDs must be <= "
                                                + MAXIMUM_PAGE_SIZE);
                        tryAndThrowException(
                                errorCallback,
                                invalidSizeException,
                                invalidSizeException.getErrorCode());
                        return;
                    }
                    List<UUID> dataSourceUuids =
                            validateMedicalDataSourceIds(ids.stream().collect(toSet())).stream()
                                    .toList();
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    List<MedicalDataSource> medicalDataSources;
                    if (holdsDataManagementPermission) {
                        medicalDataSources =
                                mMedicalDataSourceHelper
                                        .getMedicalDataSourcesByIdsWithoutPermissionChecks(
                                                dataSourceUuids);
                    } else {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        logger.setCallerForegroundState(isInForeground);

                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                        Set<String> grantedMedicalPermissions =
                                mMedicalDataPermissionEnforcer
                                        .getGrantedMedicalPermissionsForPreflight(
                                                attributionSource);

                        // Enforce caller has permission granted to at least one PHR permission
                        // before reading from DB.
                        if (grantedMedicalPermissions.isEmpty()) {
                            throw new SecurityException(
                                    "Caller doesn't have permission to read or write medical"
                                            + " data");
                        }

                        // If reading from background while READ_HEALTH_DATA_IN_BACKGROUND
                        // permission is not granted, then enforce self read.
                        boolean isCalledFromBgWithoutBgRead =
                                !isInForeground
                                        && shouldEnforceSelfRead(uid, pid, attributionSource);

                        if (Constants.DEBUG) {
                            Slog.d(
                                    TAG,
                                    "Enforce self read for package "
                                            + callingPackageName
                                            + ":"
                                            + isCalledFromBgWithoutBgRead);
                        }

                        // Pass related fields to DB to filter results.
                        medicalDataSources =
                                mMedicalDataSourceHelper
                                        .getMedicalDataSourcesByIdsWithPermissionChecks(
                                                dataSourceUuids,
                                                getPopulatedMedicalResourceTypesWithReadPermissions(
                                                        grantedMedicalPermissions),
                                                callingPackageName,
                                                grantedMedicalPermissions.contains(
                                                        WRITE_MEDICAL_DATA),
                                                isCalledFromBgWithoutBgRead,
                                                mAppInfoHelper);
                    }
                    logger.setNumberOfRecords(medicalDataSources.size());
                    tryAndReturnResult(callback, medicalDataSources, logger);
                },
                logger,
                errorCallback,
                uid,
                holdsDataManagementPermission);
    }

    /**
     * Service implementation of {@link
     * HealthConnectManager#getMedicalDataSources(GetMedicalDataSourcesRequest, Executor,
     * OutcomeReceiver)}.
     */
    @Override
    public void getMedicalDataSourcesByRequest(
            AttributionSource attributionSource,
            GetMedicalDataSourcesRequest request,
            IMedicalDataSourcesResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);
        ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, GET_MEDICAL_DATA_SOURCES_BY_REQUESTS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Getting MedicalDataSources by request is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    List<MedicalDataSource> medicalDataSources;
                    if (holdsDataManagementPermission) {
                        medicalDataSources =
                                mMedicalDataSourceHelper
                                        .getMedicalDataSourcesByPackageWithoutPermissionChecks(
                                                request.getPackageNames());
                    } else {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        logger.setCallerForegroundState(isInForeground);

                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                        Set<String> grantedMedicalPermissions =
                                mMedicalDataPermissionEnforcer
                                        .getGrantedMedicalPermissionsForPreflight(
                                                attributionSource);

                        // Enforce caller has permission granted to at least one PHR permission
                        // before reading from DB.
                        if (grantedMedicalPermissions.isEmpty()) {
                            throw new SecurityException(
                                    "Caller doesn't have permission to read or write medical"
                                            + " data");
                        }

                        // If reading from background while READ_HEALTH_DATA_IN_BACKGROUND
                        // permission is not granted, then enforce self read.
                        boolean isCalledFromBgWithoutBgRead =
                                !isInForeground
                                        && shouldEnforceSelfRead(uid, pid, attributionSource);

                        if (Constants.DEBUG) {
                            Slog.d(
                                    TAG,
                                    "Enforce self read for package "
                                            + callingPackageName
                                            + ":"
                                            + isCalledFromBgWithoutBgRead);
                        }

                        // Pass related fields to DB to filter results.
                        medicalDataSources =
                                mMedicalDataSourceHelper
                                        .getMedicalDataSourcesByPackageWithPermissionChecks(
                                                request.getPackageNames(),
                                                getPopulatedMedicalResourceTypesWithReadPermissions(
                                                        grantedMedicalPermissions),
                                                callingPackageName,
                                                grantedMedicalPermissions.contains(
                                                        WRITE_MEDICAL_DATA),
                                                isCalledFromBgWithoutBgRead);
                    }
                    logger.setNumberOfRecords(medicalDataSources.size());
                    tryAndReturnResult(callback, medicalDataSources, logger);
                },
                logger,
                errorCallback,
                uid,
                holdsDataManagementPermission);
    }

    /** Service implementation of {@link HealthConnectManager#deleteMedicalDataSourceWithData} */
    @Override
    public void deleteMedicalDataSourceWithData(
            AttributionSource attributionSource, String id, IEmptyResponseCallback callback) {
        checkParamsNonNull(attributionSource, id, callback);
        final ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, DELETE_MEDICAL_DATA_SOURCE_WITH_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Deleting MedicalDataSource is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    if (id.trim().isEmpty()) {
                        tryAndThrowException(
                                errorCallback,
                                new IllegalArgumentException("Empty datasource id"),
                                ERROR_INVALID_ARGUMENT);
                        return;
                    }
                    validateMedicalDataSourceIds(Set.of(id));
                    UUID uuid = UUID.fromString(id);
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    if (holdsDataManagementPermission) {
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                                uuid);
                    } else {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        logger.setCallerForegroundState(isInForeground);
                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_WRITE, isInForeground, logger);
                        mMedicalDataPermissionEnforcer.enforceWriteMedicalDataPermission(
                                attributionSource);
                        // This also deletes the contained data, because they are referenced
                        // by foreign key, and so are handled by ON DELETE CASCADE in the db.
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                                uuid, attributionSource.getPackageName());
                    }
                    tryAndReturnResult(callback, logger);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    /**
     * Service implementation of {@link HealthConnectManager#upsertMedicalResources} when the flag
     * PHR_UPSERT_FIX_USE_SHARED_MEMORY is enabled.
     *
     * <p>The {@link UpsertMedicalResourceRequestsParcel} will be written to shared memory if
     * required, so more data can be sent.
     */
    @Override
    public void upsertMedicalResourcesFromRequestsParcel(
            AttributionSource attributionSource,
            UpsertMedicalResourceRequestsParcel requestsParcel,
            IMedicalResourceListParcelResponseCallback callback) {
        checkParamsNonNull(attributionSource, requestsParcel, callback);

        final ErrorCallback errorCallback = callback::onError;

        upsertMedicalResources(
                attributionSource, requestsParcel.getUpsertRequests(), callback, errorCallback);
    }

    /**
     * Service implementation of {@link HealthConnectManager#upsertMedicalResources} when the flag
     * PHR_UPSERT_FIX_USE_SHARED_MEMORY is disabled.
     */
    @Override
    public void upsertMedicalResources(
            AttributionSource attributionSource,
            List<UpsertMedicalResourceRequest> requests,
            IMedicalResourcesResponseCallback callback) {
        checkParamsNonNull(attributionSource, requests, callback);

        final ErrorCallback errorCallback = callback::onError;

        upsertMedicalResources(attributionSource, requests, callback, errorCallback);
    }

    private void upsertMedicalResources(
            AttributionSource attributionSource,
            List<UpsertMedicalResourceRequest> requests,
            android.os.IInterface callback,
            ErrorCallback errorCallback) {
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, UPSERT_MEDICAL_RESOURCES)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Upsert MedicalResources is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    if (requests.isEmpty()) {
                        tryAndReturnMedicalResourcesResult(callback, List.of(), logger);
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    if (holdsDataManagementPermission) {
                        throw new SecurityException(
                                "Apps with android.permission.MANAGE_HEALTH_DATA permission are"
                                        + " not allowed to insert data");
                    }
                    List<Long> requestsSize =
                            requests.stream()
                                    .map(UpsertMedicalResourceRequest::getDataSize)
                                    .toList();
                    long requestsTotalSize = requestsSize.stream().mapToLong(Long::valueOf).sum();
                    enforceMemoryRateLimit(requestsSize, requestsTotalSize);
                    throwExceptionIfDataSyncInProgress();
                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid,
                            QuotaCategory.QUOTA_CATEGORY_WRITE,
                            isInForeground,
                            logger,
                            requestsTotalSize);

                    mMedicalDataPermissionEnforcer.enforceWriteMedicalDataPermission(
                            attributionSource);

                    List<UpsertMedicalResourceInternalRequest> validatedMedicalResourcesToUpsert =
                            new ArrayList<>();
                    FhirResourceValidator fhirResourceValidator =
                            Flags.phrFhirStructuralValidation()
                                    ? getOrCreateFhirResourceValidator()
                                    : null;
                    for (UpsertMedicalResourceRequest upsertMedicalResourceRequest : requests) {
                        MedicalResourceValidator validator =
                                new MedicalResourceValidator(
                                        upsertMedicalResourceRequest, fhirResourceValidator);
                        validatedMedicalResourcesToUpsert.add(
                                validator.validateAndCreateInternalRequest());
                    }

                    // Check that ids within the list of upsert requests are unique
                    Set<MedicalResourceId> idsToUpsert =
                            validatedMedicalResourcesToUpsert.stream()
                                    .map(UpsertMedicalResourceInternalRequest::getMedicalResourceId)
                                    .collect(toSet());
                    if (idsToUpsert.size() != validatedMedicalResourcesToUpsert.size()) {
                        throw new IllegalArgumentException(
                                "Found multiple upsert requests with the same FHIR resource id,"
                                        + " type and data source id.");
                    }

                    List<MedicalResource> medicalResources =
                            mMedicalResourceHelper.upsertMedicalResources(
                                    callingPackageName, validatedMedicalResourcesToUpsert);
                    logger.setMedicalResourceTypes(
                            medicalResources.stream()
                                    .map(MedicalResource::getType)
                                    .collect(toSet()));
                    logger.setNumberOfRecords(medicalResources.size());

                    tryAndReturnMedicalResourcesResult(callback, medicalResources, logger);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    /** Creates a FhirResourceValidator if it does not exist and returns it. */
    private FhirResourceValidator getOrCreateFhirResourceValidator() {
        // If the flag FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE is enabled, we use a
        // WeakReference so that the FhirResourceValidator can be shared between API
        // calls but garbage collected when not in use, due to its size.
        if (Flags.phrFhirResourceValidatorUseWeakReference()) {
            FhirResourceValidator fhirResourceValidator = mFhirResourceValidatorWeakReference.get();
            if (fhirResourceValidator == null) {
                fhirResourceValidator = new FhirResourceValidator();
                mFhirResourceValidatorWeakReference = new WeakReference<>(fhirResourceValidator);
            }
            return fhirResourceValidator;
        } else {
            if (mFhirResourceValidator == null) {
                // The FhirResourceValidator is initialised here if null, to avoid
                // unnecessary initialisation when PHR APIs are not used.
                mFhirResourceValidator = new FhirResourceValidator();
            }
            return mFhirResourceValidator;
        }
    }

    @Override
    public void readMedicalResourcesByIds(
            AttributionSource attributionSource,
            List<MedicalResourceId> medicalResourceIds,
            IReadMedicalResourcesResponseCallback callback) {
        checkParamsNonNull(attributionSource, medicalResourceIds, callback);
        final ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, READ_MEDICAL_RESOURCES_BY_IDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Reading MedicalResources by ids is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    if (personalHealthRecordTelemetry()) {
                        // Stores the timestamp for calls made by ANY client, including the
                        // controller
                        mPreferencesManager.setLastPhrReadMedicalResourcesApiTimeStamp(
                                mTimeSource.getInstantNow());
                    }

                    if (medicalResourceIds.isEmpty()) {
                        callback.onResult(new ReadMedicalResourcesResponse(List.of(), null, 0));
                        return;
                    }

                    if (medicalResourceIds.size() > MAXIMUM_PAGE_SIZE) {
                        HealthConnectException invalidSizeException =
                                new HealthConnectException(
                                        ERROR_INVALID_ARGUMENT,
                                        "The number of requested IDs must be <= "
                                                + MAXIMUM_PAGE_SIZE);
                        tryAndThrowException(
                                errorCallback,
                                invalidSizeException,
                                invalidSizeException.getErrorCode());
                        return;
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();

                    List<MedicalResource> medicalResources;

                    if (holdsDataManagementPermission) {
                        medicalResources =
                                mMedicalResourceHelper
                                        .readMedicalResourcesByIdsWithoutPermissionChecks(
                                                medicalResourceIds);
                    } else {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        logger.setCallerForegroundState(isInForeground);

                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                        Set<String> grantedMedicalPermissions =
                                mMedicalDataPermissionEnforcer
                                        .getGrantedMedicalPermissionsForPreflight(
                                                attributionSource);

                        // Enforce caller has permission granted to at least one PHR permission
                        // before reading from DB.
                        if (grantedMedicalPermissions.isEmpty()) {
                            throw new SecurityException(
                                    "Caller doesn't have permission to read or write medical"
                                            + " data");
                        }

                        // If reading from background while READ_HEALTH_DATA_IN_BACKGROUND
                        // permission is not granted, then enforce self read.
                        boolean isCalledFromBgWithoutBgRead =
                                !isInForeground
                                        && shouldEnforceSelfRead(uid, pid, attributionSource);

                        if (Constants.DEBUG) {
                            Slog.d(
                                    TAG,
                                    "Enforce self read for package "
                                            + callingPackageName
                                            + ":"
                                            + isCalledFromBgWithoutBgRead);
                        }

                        // Pass related fields to DB to filter results.
                        medicalResources =
                                mMedicalResourceHelper
                                        .readMedicalResourcesByIdsWithPermissionChecks(
                                                medicalResourceIds,
                                                getPopulatedMedicalResourceTypesWithReadPermissions(
                                                        grantedMedicalPermissions),
                                                callingPackageName,
                                                grantedMedicalPermissions.contains(
                                                        WRITE_MEDICAL_DATA),
                                                isCalledFromBgWithoutBgRead);
                    }

                    logger.setNumberOfRecords(medicalResources.size());
                    callback.onResult(new ReadMedicalResourcesResponse(medicalResources, null, 0));
                    logger.setHealthDataServiceApiStatusSuccess();
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    @Override
    public void readMedicalResourcesByRequest(
            AttributionSource attributionSource,
            ReadMedicalResourcesRequestParcel request,
            IReadMedicalResourcesResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);
        final ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, READ_MEDICAL_RESOURCES_BY_REQUESTS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Reading MedicalResources by request is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    if (personalHealthRecordTelemetry()) {
                        // Stores the timestamp for calls made by ANY client, including the
                        // controller
                        mPreferencesManager.setLastPhrReadMedicalResourcesApiTimeStamp(
                                mTimeSource.getInstantNow());
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();

                    PhrPageTokenWrapper pageTokenWrapper = PhrPageTokenWrapper.from(request);
                    if (pageTokenWrapper.getRequest() == null) {
                        throw new IllegalStateException("The request can not be null.");
                    }
                    logger.setMedicalResourceTypes(
                            Set.of(pageTokenWrapper.getRequest().getMedicalResourceType()));
                    ReadMedicalResourcesInternalResponse response;

                    if (holdsDataManagementPermission) {
                        response =
                                mMedicalResourceHelper
                                        .readMedicalResourcesByRequestWithoutPermissionChecks(
                                                pageTokenWrapper, request.getPageSize());
                    } else {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        logger.setCallerForegroundState(isInForeground);

                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                        boolean enforceSelfRead = false;
                        // If both read and write permissions are missing, inside the if condition
                        // the statement throws SecurityException.
                        if (mMedicalDataPermissionEnforcer
                                .enforceMedicalReadAccessAndGetEnforceSelfRead(
                                        pageTokenWrapper.getRequest().getMedicalResourceType(),
                                        attributionSource)) {
                            // If read permission is missing but write permission is granted,
                            // then enforce self read.
                            enforceSelfRead = true;
                        } else if (!isInForeground) {
                            // This is when read permission is granted but the app is reading from
                            // the background. Then we enforce self read if
                            // READ_HEALTH_DATA_IN_BACKGROUND permission is not granted.
                            enforceSelfRead = shouldEnforceSelfRead(uid, pid, attributionSource);
                        }
                        if (Constants.DEBUG) {
                            Slog.d(
                                    TAG,
                                    "Enforce self read for package "
                                            + callingPackageName
                                            + ":"
                                            + enforceSelfRead);
                        }

                        response =
                                mMedicalResourceHelper
                                        .readMedicalResourcesByRequestWithPermissionChecks(
                                                pageTokenWrapper,
                                                request.getPageSize(),
                                                callingPackageName,
                                                enforceSelfRead);
                    }

                    List<MedicalResource> medicalResources = response.getMedicalResources();
                    logger.setNumberOfRecords(medicalResources.size());

                    callback.onResult(
                            new ReadMedicalResourcesResponse(
                                    medicalResources,
                                    response.getPageToken(),
                                    response.getRemainingCount()));
                    logger.setHealthDataServiceApiStatusSuccess();
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    @Override
    public void deleteMedicalResourcesByIds(
            AttributionSource attributionSource,
            List<MedicalResourceId> medicalResourceIds,
            IEmptyResponseCallback callback) {

        // Permissions expectations:
        // - Apps with data management permissions can delete anything
        // - Other apps can only delete data written by the calling package itself.
        // - Background deletes are permitted
        // - No deletion can happen while data sync is in progress
        // - delete shares quota with write.
        // - on multi-user devices, calls will only be allowed from the foreground user.

        checkParamsNonNull(attributionSource, medicalResourceIds, callback);
        final ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, DELETE_MEDICAL_RESOURCES_BY_IDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Deleting MedicalResources by ids is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    if (medicalResourceIds.isEmpty()) {
                        tryAndReturnResult(callback, logger);
                        logger.build().log();
                        return;
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    if (holdsDataManagementPermission) {
                        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                                medicalResourceIds);
                    } else {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_WRITE, isInForeground, logger);
                        mMedicalDataPermissionEnforcer.enforceWriteMedicalDataPermission(
                                attributionSource);
                        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithPermissionChecks(
                                medicalResourceIds, callingPackageName);
                    }
                    tryAndReturnResult(callback, logger);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    @Override
    public void deleteMedicalResourcesByRequest(
            AttributionSource attributionSource,
            DeleteMedicalResourcesRequest request,
            IEmptyResponseCallback callback) {

        // Permissions expectations:
        // - Apps with data management permissions can delete anything
        // - Other apps can only delete data written by the calling package itself.
        // - Background deletes are permitted
        // - No deletion can happen while data sync is in progress
        // - delete shares quota with write.
        // - on multi-user devices, calls will only be allowed from the foreground user.

        checkParamsNonNull(attributionSource, request, callback);
        final ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName =
                Objects.requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, DELETE_MEDICAL_RESOURCES_BY_REQUESTS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Deleting MedicalResources by request is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    Set<Integer> medicalResourceTypes = request.getMedicalResourceTypes();
                    logger.setMedicalResourceTypes(medicalResourceTypes);
                    if (request.getDataSourceIds().isEmpty() && medicalResourceTypes.isEmpty()) {
                        tryAndReturnResult(callback, logger);
                        return;
                    }
                    List<UUID> dataSourceUuids = StorageUtils.toUuids(request.getDataSourceIds());
                    if (dataSourceUuids.isEmpty() && !request.getDataSourceIds().isEmpty()) {
                        throw new IllegalArgumentException("Invalid data source id used");
                    }
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    if (holdsDataManagementPermission) {
                        mMedicalResourceHelper
                                .deleteMedicalResourcesByRequestWithoutPermissionChecks(request);
                    } else {
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_WRITE, isInForeground, logger);
                        mMedicalDataPermissionEnforcer.enforceWriteMedicalDataPermission(
                                attributionSource);
                        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                                request, callingPackageName);
                    }
                    tryAndReturnResult(callback, logger);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    private void scheduleLoggingHealthDataApiErrors(
            Task task,
            HealthConnectServiceLogger.Builder logger,
            ErrorCallback errorCallback,
            int uid,
            boolean isController) {
        mThreadScheduler.schedule(
                mContext,
                () -> {
                    int errorCode = ERROR_UNKNOWN;
                    Exception exception = null;
                    try {
                        task.execute();
                    } catch (JSONException | SQLiteException jsonException) {
                        errorCode = ERROR_IO;
                        exception = jsonException;
                    } catch (SecurityException securityException) {
                        errorCode = ERROR_SECURITY;
                        exception = securityException;
                    } catch (IllegalArgumentException illegalArgumentException) {
                        errorCode = ERROR_INVALID_ARGUMENT;
                        exception = illegalArgumentException;
                    } catch (HealthConnectException healthConnectException) {
                        errorCode = healthConnectException.getErrorCode();
                        exception = healthConnectException;
                    } catch (Exception e) { // including IllegalStateException
                        errorCode = ERROR_INTERNAL;
                        exception = e;
                    } finally {
                        try {
                            if (exception != null) {
                                String msg = exception.getClass().getSimpleName() + ": ";
                                if (exception instanceof IllegalArgumentException
                                        && Flags.logcatCensorIae()) {
                                    Slog.e(TAG, getStackTraceOnlyString(exception));
                                } else {
                                    Slog.e(TAG, msg, exception);
                                }
                                if (errorCode == ERROR_UNKNOWN) {
                                    Slog.e(TAG, "errorCode should not be ERROR_UNKNOWN!");
                                }
                                logger.setHealthDataServiceApiStatusError(errorCode);
                                tryAndThrowException(errorCallback, exception, errorCode);
                            }
                        } finally {
                            logger.build().log();
                        }
                    }
                },
                uid,
                isController);
    }

    /**
     * Returns a string from an exception that contains the stack trace but not the message.
     *
     * <p>The message for an exception may reveal privacy sensitive information. So this method
     * returns the stack trace as a string including the cause chain for the exception, if it
     * exists. The stack trace is not communicated through Binder, so is lost to if it is not
     * logged.
     */
    private static String getStackTraceOnlyString(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, false);
        pw.println(ex.getClass().getName());
        printStackTrace(ex, pw);
        Throwable cause = ex.getCause();
        while (cause != null) {
            pw.println(String.format("Caused by: %s", cause.getClass().getName()));
            printStackTrace(cause, pw);
            cause = cause.getCause();
        }
        pw.flush();
        return sw.toString();
    }

    private static void printStackTrace(Throwable ex, PrintWriter pw) {
        StackTraceElement[] stackTraceElements = ex.getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            pw.println(
                    String.format(
                            " at %s.%s(%s:%s)",
                            element.getClassName(),
                            element.getMethodName(),
                            element.getFileName(),
                            element.getLineNumber()));
        }
    }

    /**
     * Retrieves {@link MedicalResourceTypeInfo} for each {@link
     * MedicalResource.MedicalResourceType}.
     */
    @Override
    public void queryAllMedicalResourceTypeInfos(IMedicalResourceTypeInfosCallback callback) {
        checkParamsNonNull(callback);
        final ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    if (!isPersonalHealthRecordEnabled()) {
                        HealthConnectException unsupportedException =
                                new HealthConnectException(
                                        ERROR_UNSUPPORTED_OPERATION,
                                        "Querying MedicalResource types info is not supported.");
                        Slog.e(TAG, "HealthConnectException: ", unsupportedException);
                        tryAndThrowException(
                                errorCallback,
                                unsupportedException,
                                unsupportedException.getErrorCode());
                        return;
                    }

                    try {
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                        throwExceptionIfDataSyncInProgress();
                        callback.onResult(getPopulatedMedicalResourceTypeInfos());
                    } catch (SQLiteException sqLiteException) {
                        tryAndThrowException(errorCallback, sqLiteException, ERROR_IO);
                    } catch (SecurityException securityException) {
                        Slog.e(TAG, "SecurityException: ", securityException);
                        tryAndThrowException(errorCallback, securityException, ERROR_SECURITY);
                    } catch (HealthConnectException healthConnectException) {
                        Slog.e(TAG, "HealthConnectException: ", healthConnectException);
                        tryAndThrowException(
                                errorCallback,
                                healthConnectException,
                                healthConnectException.getErrorCode());
                    } catch (Exception exception) {
                        tryAndThrowException(errorCallback, exception, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void getChangesForBackup(
            @Nullable String changeToken, IGetChangesForBackupResponseCallback callback) {
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        // TODO(b/400105647): Remove duplicate flag check once excess code size is
                        // resolved.
                        if (mCloudBackupManager == null
                                || !Flags.cloudBackupAndRestore()
                                || !isCloudBackupRestoreEnabled()) {
                            throw new UnsupportedOperationException(
                                    "getChangesForBackup is not supported.");
                        }
                        enforceIsForegroundUser(userHandle);

                        mContext.enforcePermission(
                                BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS,
                                pid,
                                uid,
                                "Caller does not have permission to call getChangesForBackup.");
                        callback.onResult(mCloudBackupManager.getChangesForBackup(changeToken));
                    } catch (UnsupportedOperationException e) {
                        tryAndThrowException(errorCallback, e, ERROR_UNSUPPORTED_OPERATION);
                    } catch (SecurityException e) {
                        tryAndThrowException(errorCallback, e, ERROR_SECURITY);
                    } catch (IllegalArgumentException e) {
                        tryAndThrowException(errorCallback, e, ERROR_INVALID_ARGUMENT);
                    } catch (Exception e) {
                        tryAndThrowException(errorCallback, e, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void getLatestMetadataForBackup(IGetLatestMetadataForBackupResponseCallback callback) {
        checkParamsNonNull(callback);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        // TODO(b/400105647): Remove duplicate flag check once excess code size is
                        // resolved.
                        if (mCloudBackupManager == null
                                || !Flags.cloudBackupAndRestore()
                                || !isCloudBackupRestoreEnabled()) {
                            throw new UnsupportedOperationException(
                                    "getLatestMetadataForBackup is not supported.");
                        }
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(
                                BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS,
                                pid,
                                uid,
                                "Caller does not have permission to call"
                                        + " getLatestMetadataForBackup.");
                        callback.onResult(mCloudBackupManager.getSettingsForBackup());
                    } catch (UnsupportedOperationException e) {
                        tryAndThrowException(errorCallback, e, ERROR_UNSUPPORTED_OPERATION);
                    } catch (SecurityException e) {
                        tryAndThrowException(errorCallback, e, ERROR_SECURITY);
                    } catch (Exception e) {
                        tryAndThrowException(errorCallback, e, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void restoreLatestMetadata(
            BackupMetadata backupMetadata, IEmptyResponseCallback callback) {
        checkParamsNonNull(backupMetadata);
        checkParamsNonNull(callback);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        // TODO(b/400105647): Remove duplicate flag check once excess code size is
                        // resolved.
                        if (mCloudRestoreManager == null
                                || !Flags.cloudBackupAndRestore()
                                || !isCloudBackupRestoreEnabled()) {
                            throw new UnsupportedOperationException(
                                    "restoreSettings is not supported.");
                        }
                        enforceIsForegroundUser(userHandle);

                        mContext.enforcePermission(
                                RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS,
                                pid,
                                uid,
                                "Caller does not have permission to call restoreSettings.");
                        mCloudRestoreManager.restoreSettings(backupMetadata);
                        callback.onResult();
                    } catch (UnsupportedOperationException e) {
                        tryAndThrowException(errorCallback, e, ERROR_UNSUPPORTED_OPERATION);
                    } catch (SecurityException e) {
                        tryAndThrowException(errorCallback, e, ERROR_SECURITY);
                    } catch (IllegalArgumentException e) {
                        tryAndThrowException(errorCallback, e, ERROR_INVALID_ARGUMENT);
                    } catch (Exception e) {
                        tryAndThrowException(errorCallback, e, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void canRestore(int dataVersion, ICanRestoreResponseCallback callback) {
        checkParamsNonNull(dataVersion);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        // TODO(b/400105647): Remove duplicate flag check once excess code size is
                        // resolved.
                        if (mCloudRestoreManager == null
                                || !Flags.cloudBackupAndRestore()
                                || !isCloudBackupRestoreEnabled()) {
                            throw new UnsupportedOperationException("canRestore is not supported.");
                        }
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(
                                RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS,
                                pid,
                                uid,
                                "Caller does not have permission to call canRestore.");
                        callback.onResult(mCloudRestoreManager.canRestore(dataVersion));
                    } catch (UnsupportedOperationException e) {
                        tryAndThrowException(errorCallback, e, ERROR_UNSUPPORTED_OPERATION);
                    } catch (SecurityException e) {
                        tryAndThrowException(errorCallback, e, ERROR_SECURITY);
                    } catch (Exception e) {
                        tryAndThrowException(errorCallback, e, ERROR_INTERNAL);
                    }
                });
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void restoreChanges(List<RestoreChange> changes, IEmptyResponseCallback callback) {
        checkParamsNonNull(changes);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        // TODO(b/400105647): Remove duplicate flag check once excess code size is
                        // resolved.
                        if (mCloudRestoreManager == null
                                || !Flags.cloudBackupAndRestore()
                                || !isCloudBackupRestoreEnabled()) {
                            throw new UnsupportedOperationException(
                                    "restoreChanges is not supported.");
                        }
                        enforceIsForegroundUser(userHandle);
                        mContext.enforcePermission(
                                RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS,
                                pid,
                                uid,
                                "Caller does not have permission to call" + " restoreChanges.");
                        mCloudRestoreManager.restoreChanges(changes);
                        callback.onResult();
                    } catch (UnsupportedOperationException e) {
                        tryAndThrowException(errorCallback, e, ERROR_UNSUPPORTED_OPERATION);
                    } catch (SecurityException e) {
                        tryAndThrowException(errorCallback, e, ERROR_SECURITY);
                    } catch (IllegalArgumentException e) {
                        tryAndThrowException(errorCallback, e, ERROR_INVALID_ARGUMENT);
                    } catch (Exception e) {
                        tryAndThrowException(errorCallback, e, ERROR_INTERNAL);
                    }
                });
    }

    // Cancel BR timeouts - this might be needed when a user is going into background.
    void cancelBackupRestoreTimeouts() {
        mBackupRestore.cancelAllJobs();
    }

    private void tryAcquireApiCallQuota(
            int uid,
            @QuotaCategory.Type int quotaCategory,
            boolean isInForeground,
            HealthConnectServiceLogger.Builder logger) {
        try {
            mRateLimiter.tryAcquireApiCallQuota(uid, quotaCategory, isInForeground);
        } catch (RateLimiterException rateLimiterException) {
            logger.setRateLimit(
                    rateLimiterException.getRateLimiterQuotaBucket(),
                    rateLimiterException.getRateLimiterQuotaLimit());
            throw new HealthConnectException(
                    rateLimiterException.getErrorCode(), rateLimiterException.getMessage());
        }
    }

    private void tryAcquireApiCallQuota(
            int uid,
            @QuotaCategory.Type int quotaCategory,
            boolean isInForeground,
            HealthConnectServiceLogger.Builder logger,
            long memoryCost) {
        try {
            mRateLimiter.tryAcquireApiCallQuota(uid, quotaCategory, isInForeground, memoryCost);
        } catch (RateLimiterException rateLimiterException) {
            logger.setRateLimit(
                    rateLimiterException.getRateLimiterQuotaBucket(),
                    rateLimiterException.getRateLimiterQuotaLimit());
            throw new HealthConnectException(
                    rateLimiterException.getErrorCode(), rateLimiterException.getMessage());
        }
    }

    private void enforceMemoryRateLimit(List<Long> recordsSize, long recordsChunkSize) {
        recordsSize.forEach(mRateLimiter::checkMaxRecordMemoryUsage);
        mRateLimiter.checkMaxChunkMemoryUsage(recordsChunkSize);
    }

    /**
     * On a multi-user device, enforce that the calling user handle (user account) is the same as
     * the current foreground user (account).
     */
    private void enforceIsForegroundUser(UserHandle callingUserHandle) {
        if (!callingUserHandle.equals(mCurrentForegroundUser)) {
            throw new IllegalStateException(
                    "Calling user: "
                            + callingUserHandle.getIdentifier()
                            + "is not the current foreground user: "
                            + mCurrentForegroundUser.getIdentifier()
                            + ". HC request must be called"
                            + " from the current foreground user.");
        }
    }

    private boolean isDataSyncInProgress() {
        return mMigrationStateManager.isMigrationInProgress()
                || mBackupRestore.isRestoreMergingInProgress();
    }

    private DataMigrationManager getDataMigrationManager(UserHandle userHandle) {
        final Context userContext = mContext.createContextAsUser(userHandle, 0);

        return new DataMigrationManager(
                userContext,
                mTransactionManager,
                mPermissionHelper,
                mFirstGrantTimeManager,
                mDeviceInfoHelper,
                mAppInfoHelper,
                mHealthDataCategoryPriorityHelper,
                mPriorityMigrationHelper,
                mMigrationEntityHelper,
                mPreferencesManager);
    }

    private void enforceCallingPackageBelongsToUid(String packageName, int callingUid) {
        int packageUid;
        try {
            packageUid = mContext.getPackageManager().getPackageUid(packageName, /* flags */ 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(packageName + " not found");
        }
        if (UserHandle.getAppId(packageUid) != UserHandle.getAppId(callingUid)) {
            throw new SecurityException(packageName + " does not belong to uid " + callingUid);
        }
    }

    /**
     * Verify various aspects of the calling user.
     *
     * @param callingUid Uid of the caller, usually retrieved from Binder for authenticity.
     * @param callerAttributionSource The permission identity of the caller
     */
    private void verifyPackageNameFromUid(
            int callingUid, AttributionSource callerAttributionSource) {
        // Check does the attribution source is one for the calling app.
        callerAttributionSource.enforceCallingUid();
        // Obtain the user where the client is running in.
        UserHandle callingUserHandle = UserHandle.getUserHandleForUid(callingUid);
        Context callingUserContext = mContext.createContextAsUser(callingUserHandle, 0);
        String callingPackageName =
                Objects.requireNonNull(callerAttributionSource.getPackageName());
        verifyCallingPackage(callingUserContext, callingUid, callingPackageName);
    }

    /**
     * Check that the caller's supposed package name matches the uid making the call.
     *
     * @throws SecurityException if the package name and uid don't match.
     */
    private void verifyCallingPackage(
            Context actualCallingUserContext, int actualCallingUid, String claimedCallingPackage) {
        int claimedCallingUid = getPackageUid(actualCallingUserContext, claimedCallingPackage);
        if (claimedCallingUid != actualCallingUid) {
            throw new SecurityException(
                    claimedCallingPackage
                            + ", with uid "
                            + claimedCallingUid
                            + " does not belong to uid "
                            + actualCallingUid);
        }
    }

    /** Finds the UID of the {@code packageName} in the given {@code context}. */
    private int getPackageUid(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageUid(packageName, /* flags= */ 0);
        } catch (PackageManager.NameNotFoundException e) {
            return Process.INVALID_UID;
        }
    }

    private void enforceShowMigrationInfoIntent(String packageName, int callingUid) {
        enforceCallingPackageBelongsToUid(packageName, callingUid);

        Intent intentToCheck =
                new Intent(HealthConnectManager.ACTION_SHOW_MIGRATION_INFO).setPackage(packageName);

        ResolveInfo resolveResult =
                mContext.getPackageManager()
                        .resolveActivity(
                                intentToCheck,
                                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL));

        if (Objects.isNull(resolveResult)) {
            throw new IllegalArgumentException(
                    packageName
                            + " does not handle intent "
                            + HealthConnectManager.ACTION_SHOW_MIGRATION_INFO);
        }
    }

    private Map<Integer, List<DataOrigin>> getPopulatedRecordTypeInfoResponses() {
        Map<Integer, Class<? extends Record>> recordIdToExternalRecordClassMap =
                mHealthConnectMappings.getRecordIdToExternalRecordClassMap();
        Map<Integer, List<DataOrigin>> recordTypeInfoResponses =
                new ArrayMap<>(recordIdToExternalRecordClassMap.size());
        Map<Integer, Set<String>> recordTypeToContributingPackagesMap =
                mAppInfoHelper.getRecordTypesToContributingPackagesMap();
        recordIdToExternalRecordClassMap
                .keySet()
                .forEach(
                        (recordType) -> {
                            if (recordTypeToContributingPackagesMap.containsKey(recordType)) {
                                List<DataOrigin> packages =
                                        recordTypeToContributingPackagesMap.get(recordType).stream()
                                                .map(
                                                        (packageName) ->
                                                                new DataOrigin.Builder()
                                                                        .setPackageName(packageName)
                                                                        .build())
                                                .toList();
                                recordTypeInfoResponses.put(recordType, packages);
                            } else {
                                recordTypeInfoResponses.put(recordType, Collections.emptyList());
                            }
                        });
        return recordTypeInfoResponses;
    }

    private List<MedicalResourceTypeInfo> getPopulatedMedicalResourceTypeInfos() {
        Map<Integer, Set<MedicalDataSource>> resourceTypeToDataSourcesMap =
                mMedicalResourceHelper.getMedicalResourceTypeToContributingDataSourcesMap();
        return MedicalResource.VALID_TYPES.stream()
                .map(
                        medicalResourceType ->
                                new MedicalResourceTypeInfo(
                                        medicalResourceType,
                                        resourceTypeToDataSourcesMap.getOrDefault(
                                                medicalResourceType, Set.of())))
                .collect(toList());
    }

    private Set<Integer> getPopulatedMedicalResourceTypesWithReadPermissions(
            Set<String> grantedMedicalPermissions) {
        return grantedMedicalPermissions.stream()
                .filter(permissionString -> !permissionString.equals(WRITE_MEDICAL_DATA))
                .map(MedicalResourceTypePermissionMapper::getMedicalResourceType)
                .collect(toSet());
    }

    private boolean hasDataManagementPermission(int uid, int pid) {
        return isPermissionGranted(MANAGE_HEALTH_DATA_PERMISSION, uid, pid);
    }

    private boolean isPermissionGranted(String permission, int uid, int pid) {
        return mContext.checkPermission(permission, pid, uid) == PERMISSION_GRANTED;
    }

    private void logRecordTypeSpecificUpsertMetrics(
            List<RecordInternal<?>> recordInternals, String packageName) {
        checkParamsNonNull(recordInternals, packageName);

        Map<Integer, List<RecordInternal<?>>> recordTypeToRecordInternals =
                getRecordTypeToListOfRecords(recordInternals);
        for (Entry<Integer, List<RecordInternal<?>>> recordTypeToRecordInternalsEntry :
                recordTypeToRecordInternals.entrySet()) {
            RecordHelper<?> recordHelper =
                    mInternalHealthConnectMappings.getRecordHelper(
                            recordTypeToRecordInternalsEntry.getKey());
            recordHelper.logUpsertMetrics(
                    mStatsLog, recordTypeToRecordInternalsEntry.getValue(), packageName);
        }
    }

    private void logRecordTypeSpecificReadMetrics(
            List<RecordInternal<?>> recordInternals, String packageName) {
        checkParamsNonNull(recordInternals, packageName);

        Map<Integer, List<RecordInternal<?>>> recordTypeToRecordInternals =
                getRecordTypeToListOfRecords(recordInternals);
        for (Entry<Integer, List<RecordInternal<?>>> recordTypeToRecordInternalsEntry :
                recordTypeToRecordInternals.entrySet()) {
            RecordHelper<?> recordHelper =
                    mInternalHealthConnectMappings.getRecordHelper(
                            recordTypeToRecordInternalsEntry.getKey());
            recordHelper.logReadMetrics(
                    mStatsLog, recordTypeToRecordInternalsEntry.getValue(), packageName);
        }
    }

    private Map<Integer, List<RecordInternal<?>>> getRecordTypeToListOfRecords(
            List<RecordInternal<?>> recordInternals) {

        return recordInternals.stream()
                .collect(Collectors.groupingBy(RecordInternal::getRecordType));
    }

    private void throwExceptionIfDataSyncInProgress() {
        if (isDataSyncInProgress()) {
            throw new HealthConnectException(
                    HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS,
                    "Storage data sync in progress. API calls are blocked");
        }
    }

    /**
     * Throws an IllegalState Exception if data migration or restore is in process. This is only
     * used by HealthConnect synchronous APIs as {@link HealthConnectException} is lost between
     * processes on synchronous APIs and can only be returned to the caller for the APIs with a
     * callback.
     */
    private void throwIllegalStateExceptionIfDataSyncInProgress() {
        if (isDataSyncInProgress()) {
            throw new IllegalStateException("Storage data sync in progress. API calls are blocked");
        }
    }

    private void postDeleteTasks(List<Integer> recordTypeIdsToDelete) {
        if (recordTypeIdsToDelete != null && !recordTypeIdsToDelete.isEmpty()) {
            mAppInfoHelper.syncAppInfoRecordTypesUsed(new HashSet<>(recordTypeIdsToDelete));
            mActivityDateHelper.reSyncByRecordTypeIds(recordTypeIdsToDelete);
        }
    }

    /**
     * When read permission is granted but the app is reading from the background, enforce self read
     * for this client if READ_HEALTH_DATA_IN_BACKGROUND permission is not granted.
     */
    private boolean shouldEnforceSelfRead(int uid, int pid, AttributionSource attributionSource) {
        return !isPermissionGranted(READ_HEALTH_DATA_IN_BACKGROUND, uid, pid)
                || isBackgroundPermissionFromSplit(attributionSource);
    }

    /** Returns true if READ_HEALTH_DATA_IN_BACKGROUND is from split permission. */
    private boolean isBackgroundPermissionFromSplit(AttributionSource attributionSource) {
        if (!Flags.replaceBodySensorPermissionEnabled()) {
            return false;
        }

        String packageName = attributionSource.getPackageName();
        if (packageName == null) {
            // Cannot read permission flag from null package name, return default false.
            return false;
        }
        UserHandle user = UserHandle.getUserHandleForUid(attributionSource.getUid());
        int permissionFlags =
                mContext.getPackageManager()
                        .getPermissionFlags(READ_HEALTH_DATA_IN_BACKGROUND, packageName, user);

        int targetSdk;
        try {
            targetSdk =
                    PackageInfoUtils.getPackageInfoUnchecked(
                                    packageName,
                                    user,
                                    PackageManager.PackageInfoFlags.of(0),
                                    mContext)
                            .applicationInfo
                            .targetSdkVersion;
        } catch (Exception e) {
            // Cannot find the package, default false.
            return false;
        }

        return HealthConnectPermissionHelper.isFromSplitPermission(permissionFlags, targetSdk);
    }

    private static void tryAndReturnResult(
            IEmptyResponseCallback callback, HealthConnectServiceLogger.Builder logger) {
        try {
            callback.onResult();
            logger.setHealthDataServiceApiStatusSuccess();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote call failed", e);
            logger.setHealthDataServiceApiStatusError(ERROR_INTERNAL);
        }
    }

    private static void tryAndReturnResult(
            IInsertRecordsResponseCallback callback,
            List<String> uuids,
            HealthConnectServiceLogger.Builder logger) {
        try {
            callback.onResult(new InsertRecordsResponseParcel(uuids));
            logger.setHealthDataServiceApiStatusSuccess();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote call failed", e);
            logger.setHealthDataServiceApiStatusError(ERROR_INTERNAL);
        }
    }

    private static void tryAndReturnResult(
            IMedicalDataSourcesResponseCallback callback,
            List<MedicalDataSource> response,
            HealthConnectServiceLogger.Builder logger) {
        try {
            callback.onResult(response);
            logger.setHealthDataServiceApiStatusSuccess();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote call failed when returning GetMedicalDataSources response", e);
            logger.setHealthDataServiceApiStatusError(ERROR_INTERNAL);
        }
    }

    private static void tryAndReturnResult(
            IMedicalDataSourceResponseCallback callback,
            MedicalDataSource medicalDataSource,
            HealthConnectServiceLogger.Builder logger) {
        try {
            callback.onResult(medicalDataSource);
            logger.setHealthDataServiceApiStatusSuccess();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote call failed when returning MedicalDataSource response", e);
            logger.setHealthDataServiceApiStatusError(ERROR_INTERNAL);
        }
    }

    private static void tryAndReturnMedicalResourcesResult(
            android.os.IInterface callback,
            List<MedicalResource> medicalResources,
            HealthConnectServiceLogger.Builder logger) {
        if (callback instanceof IMedicalResourcesResponseCallback) {
            tryAndReturnResult(
                    (IMedicalResourcesResponseCallback) callback, medicalResources, logger);
        } else if (callback instanceof IMedicalResourceListParcelResponseCallback) {
            tryAndReturnResult(
                    (IMedicalResourceListParcelResponseCallback) callback,
                    new MedicalResourceListParcel(medicalResources),
                    logger);
        } else {
            throw new IllegalStateException("Unexpected callback type for upsertMedicalResources");
        }
    }

    private static void tryAndReturnResult(
            IMedicalResourcesResponseCallback callback,
            List<MedicalResource> medicalResources,
            HealthConnectServiceLogger.Builder logger) {
        try {
            callback.onResult(medicalResources);
            logger.setHealthDataServiceApiStatusSuccess();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote call to return UpsertMedicalResourcesResponse failed", e);
            logger.setHealthDataServiceApiStatusError(ERROR_INTERNAL);
        }
    }

    private static void tryAndReturnResult(
            IMedicalResourceListParcelResponseCallback callback,
            MedicalResourceListParcel medicalResourceListParcel,
            HealthConnectServiceLogger.Builder logger) {
        try {
            callback.onResult(medicalResourceListParcel);
            logger.setHealthDataServiceApiStatusSuccess();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote call to return UpsertMedicalResourcesResponse failed", e);
            logger.setHealthDataServiceApiStatusError(ERROR_INTERNAL);
        }
    }

    private static void tryAndThrowException(
            IMigrationCallback callback,
            Exception exception,
            @MigrationException.ErrorCode int errorCode,
            @Nullable String failedEntityId) {
        try {
            callback.onError(
                    new MigrationException(exception.toString(), errorCode, failedEntityId));
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send result to the callback", e);
        }
    }

    private static void tryAndThrowException(
            ErrorCallback callback,
            Exception exception,
            @HealthConnectException.ErrorCode int errorCode) {
        try {
            callback.onError(
                    new HealthConnectExceptionParcel(
                            new HealthConnectException(errorCode, exception.toString())));
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send result to the callback", e);
        }
    }

    private static void checkParamsNonNull(Object... params) {
        for (Object param : params) {
            Objects.requireNonNull(param);
        }
    }

    /** A task to run in {@link #scheduleLoggingHealthDataApiErrors}. */
    private interface Task {
        /**
         * The code to run.
         *
         * <p>As well as the listed exception types which may be thrown, runtime exceptions
         * including {@link SQLiteException}, {@link IllegalArgumentException}, {@link
         * IllegalStateException}, {@link SecurityException} and {@link HealthConnectException} are
         * expected.
         */
        void execute() throws RemoteException, JSONException;
    }

    /**
     * A wrapper interface to put around a callback to HealthConnect. It allows very similar code to
     * be written for multiple similar AIDL interfaces.
     */
    private interface ErrorCallback {

        /** Sends an error to the caller. */
        void onError(HealthConnectExceptionParcel error) throws RemoteException;
    }
}
