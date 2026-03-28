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

import static android.Manifest.permission.BACKUP;
import static android.Manifest.permission.BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS;
import static android.Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA;
import static android.Manifest.permission.RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
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
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.RecordTypeSensitivity.INSENSITIVE;

import static com.android.healthfitness.flags.AconfigFlagHelper.isCloudBackupRestoreEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isPhrChangeLogsEnabled;
import static com.android.internal.util.Preconditions.checkArgument;
import static com.android.server.healthconnect.HealthConnectShellCommand.SHELL_PACKAGE_NAME;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.ADVERTISE_DEVICE_DATA_SOURCES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.CREATE_MEDICAL_DATA_SOURCE;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_DATA;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_DEVICE_RECORDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_DATA_SOURCE_WITH_DATA;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_CHANGES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_CHANGES_TOKEN;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_CURRENT_DEVICE_DATA_SOURCE;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_DEVICE_DATA_SOURCES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_DEVICE_DATA_SOURCE_CAPABILITIES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MATCHING_DATA_SOURCES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.HAS_USER_ENABLED_TRACKING;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.INSERT_DATA;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.INSERT_DEVICE_RECORDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_AGGREGATED_DATA;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_DATA;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_DEVICE_RECORDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.UPDATE_DATA;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.UPDATE_DEVICE_RECORDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.UPSERT_MEDICAL_RESOURCES;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.PermissionManuallyEnforced;
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
import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSource;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.DeviceDataTypeSource;
import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.GetDeviceDataSourcesResponse;
import android.health.connect.GetMatchingDataSourcesResponse;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthConnectManager.DataDownloadState;
import android.health.connect.HealthConnectOnboardingState;
import android.health.connect.HealthDataCategory;
import android.health.connect.MatchmakingRequest;
import android.health.connect.MatchmakingResponse;
import android.health.connect.MedicalResourceId;
import android.health.connect.MedicalResourceTypeInfo;
import android.health.connect.PageTokenWrapper;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.RecordTypeInfoResponse;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.accesslog.AccessLogsResponseParcel;
import android.health.connect.aidl.ActivityDatesRequestParcel;
import android.health.connect.aidl.ActivityDatesResponseParcel;
import android.health.connect.aidl.AggregateDataRequestParcel;
import android.health.connect.aidl.AggregateDataResponseParcel;
import android.health.connect.aidl.ApplicationInfoResponseParcel;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.DeviceDataSourceCapabilities;
import android.health.connect.aidl.GetPriorityResponseParcel;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IAccessLogsResponseCallback;
import android.health.connect.aidl.IActivityDatesResponseCallback;
import android.health.connect.aidl.IAggregateRecordsResponseCallback;
import android.health.connect.aidl.IApplicationInfoResponseCallback;
import android.health.connect.aidl.ICanRestoreResponseCallback;
import android.health.connect.aidl.IChangeLogsResponseCallback;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.aidl.IDeviceDataSourceCapabilitiesCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangeLogTokenCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetCurrentDeviceDataSourceCallback;
import android.health.connect.aidl.IGetDeviceDataSourceInfosCallback;
import android.health.connect.aidl.IGetDeviceDataSourcesCallback;
import android.health.connect.aidl.IGetHealthConnectDataStateCallback;
import android.health.connect.aidl.IGetHealthConnectMigrationUiStateCallback;
import android.health.connect.aidl.IGetHealthConnectOnboardingStateCallback;
import android.health.connect.aidl.IGetLatestMetadataForBackupResponseCallback;
import android.health.connect.aidl.IGetMatchingDataSourcesCallback;
import android.health.connect.aidl.IGetPriorityResponseCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IInsertRecordsResponseCallback;
import android.health.connect.aidl.IIsMatchmakingPossibleCallback;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMedicalResourceTypeInfosCallback;
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
import android.health.connect.backuprestore.UpdateBackupAndRestoreSettingsRequest;
import android.health.connect.backuprestore.UpdateHealthConnectBackupStatusRequest;
import android.health.connect.backuprestore.UpdateHealthConnectRestoreStatusRequest;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.changelog.ChangeLogsResponse.DeletedMedicalResource;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.SymptomRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.exportimport.ExportImportDocumentProvider;
import android.health.connect.exportimport.IImportStatusCallback;
import android.health.connect.exportimport.IQueryDocumentProvidersCallback;
import android.health.connect.exportimport.IScheduledExportStatusCallback;
import android.health.connect.exportimport.ImportStatus;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.exportimport.ScheduledExportStatus;
import android.health.connect.internal.datatypes.AppInfoInternal;
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
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.build.SdkLevel;
import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.backuprestore.BackupRestore;
import com.android.server.healthconnect.backuprestore.BackupRestoreLogger;
import com.android.server.healthconnect.backuprestore.CloudBackupManager;
import com.android.server.healthconnect.backuprestore.CloudRestoreManager;
import com.android.server.healthconnect.common.RequestContext;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.logging.HealthConnectServiceLogger;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameResolver;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.common.preferences.PreferencesManager;
import com.android.server.healthconnect.device.DeviceDataProviderDebugUtil;
import com.android.server.healthconnect.device.DeviceDataProviderManager;
import com.android.server.healthconnect.device.tracker.TrackerManager;
import com.android.server.healthconnect.exportimport.DocumentProvidersManager;
import com.android.server.healthconnect.exportimport.ExportImportJobs;
import com.android.server.healthconnect.exportimport.ExportImportSettingsStorage;
import com.android.server.healthconnect.exportimport.ExportManager;
import com.android.server.healthconnect.exportimport.ImportManager;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.aggregation.FitnessRecordAggregateHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataSourcesHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.helpers.RecordDateHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.migration.DataMigrationManager;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationEntityHelper;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.onboarding.OnboardingStateManager;
import com.android.server.healthconnect.onboarding.matchmaking.MatchmakingManager;
import com.android.server.healthconnect.permission.DataPermissionEnforcer;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.MedicalDataPermissionEnforcer;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.phr.UpsertMedicalResourceInternalRequest;
import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.phr.validations.FhirResourceValidator;
import com.android.server.healthconnect.phr.validations.MedicalResourceValidator;
import com.android.server.healthconnect.storage.DatabaseHelper.DatabaseHelpers;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.utils.TimeSource;

import org.json.JSONException;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * IHealthConnectService's implementation
 *
 * @hide
 */
final class HealthConnectServiceImpl extends IHealthConnectService.Stub {
    private static final String TAG = "HealthConnectService";
    // TODO(b/452607006): Replace this with a formal definition of sensitive data types.
    // TODO(b/455620629): Add data type sensitivity to DataTypeDescriptor.
    private static final Set<Integer> NON_SENSITIVE_RECORD_TYPES = Set.of(RECORD_TYPE_STEPS);

    // Permission for test api for deleting staged data
    private static final String DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA_PERMISSION =
            "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA";
    // Allows an application to act as a backup inter-agent to send and receive HealthConnect data
    private static final String HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION =
            "android.permission.HEALTH_CONNECT_BACKUP_INTER_AGENT";

    private final ImportManager mImportManager;

    private final TransactionManager mTransactionManager;
    private final CloudBackupManager mCloudBackupManager;
    private final CloudRestoreManager mCloudRestoreManager;
    private final HealthConnectPermissionHelper mPermissionHelper;
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final Context mContext;
    private final PermissionManager mPermissionManager;

    private final BackupRestore mBackupRestore;
    private final MigrationStateManager mMigrationStateManager;
    private final OnboardingStateManager mOnboardingStateManager;

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
    private final RecordDateHelper mRecordDateHelper;
    private final ChangeLogsHelper mChangeLogsHelper;
    private final ChangeLogsRequestHelper mChangeLogsRequestHelper;
    private final MigrationEntityHelper mMigrationEntityHelper;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final HealthConnectMappings mHealthConnectMappings;
    private final TimeSource mTimeSource;
    private final DatabaseHelpers mDatabaseHelpers;
    private final PreferencesManager mPreferencesManager;
    private final TrackerManager mTrackerManager;
    private final RateLimiter mRateLimiter;
    // Used if PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE is false.
    @Nullable private FhirResourceValidator mFhirResourceValidator;
    // Used if PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE is true.
    WeakReference<FhirResourceValidator> mFhirResourceValidatorWeakReference =
            new WeakReference<>(null);
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final HealthFitnessStatsLog mStatsLog;
    private final MatchmakingManager mMatchmakingManager;
    private final SyntheticPackageNameResolver mSyntheticPackageNameResolver;
    private final DeviceDataSourcesHelper mDeviceDataSourcesHelper;
    private final DeviceDataProviderManager mDeviceDataProviderManager;
    private final DeviceDataProviderMetadataHelper mDeviceDataProviderMetadataHelper;
    private final SyntheticPackageNameCreator mSyntheticPackageNameCreator;
    private final DeviceDataProviderDebugUtil mDeviceDataProviderDebugUtil;

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
            OnboardingStateManager onboardingStateManager,
            FitnessRecordUpsertHelper fitnessRecordUpsertHelper,
            FitnessRecordReadHelper fitnessRecordReadHelper,
            FitnessRecordDeleteHelper fitnessRecordDeleteHelper,
            FitnessRecordAggregateHelper fitnessRecordAggregateHelper,
            MedicalResourceHelper medicalResourceHelper,
            MedicalDataSourceHelper medicalDataSourceHelper,
            ExportManager exportManager,
            ExportImportSettingsStorage exportImportSettingsStorage,
            BackupRestore backupRestore,
            AccessLogsHelper accessLogsHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            RecordDateHelper recordDateHelper,
            ChangeLogsHelper changeLogsHelper,
            ChangeLogsRequestHelper changeLogsRequestHelper,
            PriorityMigrationHelper priorityMigrationHelper,
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper,
            PreferenceHelper preferenceHelper,
            DatabaseHelpers databaseHelpers,
            PreferencesManager preferencesManager,
            AppOpsManagerLocal appOpsManagerLocal,
            HealthConnectThreadScheduler threadScheduler,
            RateLimiter rateLimiter,
            HealthFitnessStatsLog statsLog,
            BackupRestoreLogger backupRestoreLogger,
            TrackerManager trackerManager,
            CloudBackupManager cloudBackupManager,
            CloudRestoreManager cloudRestoreManager,
            MatchmakingManager matchmakingManager,
            SyntheticPackageNameResolver syntheticPackageNameResolver,
            DeviceDataSourcesHelper deviceDataSourcesHelper,
            DeviceDataProviderManager deviceDataProviderManager,
            DeviceDataProviderMetadataHelper deviceDataProviderMetadataHelper,
            SyntheticPackageNameCreator syntheticPackageNameCreator,
            ImportManager importManager,
            DataPermissionEnforcer dataPermissionEnforcer,
            MedicalDataPermissionEnforcer medicalDataPermissionEnforcer,
            DeviceDataProviderDebugUtil deviceDataProviderDebugUtil) {
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

        mOnboardingStateManager = onboardingStateManager;

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
        mRecordDateHelper = recordDateHelper;
        mChangeLogsHelper = changeLogsHelper;
        mChangeLogsRequestHelper = changeLogsRequestHelper;
        mPriorityMigrationHelper = priorityMigrationHelper;
        mAppInfoHelper = appInfoHelper;
        mDeviceInfoHelper = deviceInfoHelper;
        mPreferenceHelper = preferenceHelper;
        mDatabaseHelpers = databaseHelpers;
        mPreferencesManager = preferencesManager;
        mThreadScheduler = threadScheduler;
        mRateLimiter = rateLimiter;
        mDeviceDataProviderMetadataHelper = deviceDataProviderMetadataHelper;
        mSyntheticPackageNameCreator = syntheticPackageNameCreator;

        mPermissionManager = mContext.getSystemService(PermissionManager.class);
        mAppOpsManagerLocal = appOpsManagerLocal;
        mMedicalDataPermissionEnforcer = medicalDataPermissionEnforcer;
        mDataPermissionEnforcer = dataPermissionEnforcer;
        mImportManager = importManager;

        mTrackerManager = trackerManager;
        mCloudBackupManager = cloudBackupManager;
        mCloudRestoreManager = cloudRestoreManager;
        mStatsLog = statsLog;
        mMatchmakingManager = matchmakingManager;
        mSyntheticPackageNameResolver = syntheticPackageNameResolver;
        mDeviceDataSourcesHelper = deviceDataSourcesHelper;
        mDeviceDataProviderManager = deviceDataProviderManager;
        mDeviceDataProviderDebugUtil = deviceDataProviderDebugUtil;
    }

    public void setupForUser(UserHandle currentForegroundUser) {
        mCurrentForegroundUser = currentForegroundUser;
    }

    /** This override is needed to enable testing of multiple device data providers in CTS. */
    @Override
    public int handleShellCommand(
            @NonNull ParcelFileDescriptor in,
            @NonNull ParcelFileDescriptor out,
            @NonNull ParcelFileDescriptor err,
            @NonNull String[] args) {
        if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            return super.handleShellCommand(in, out, err, args);
        }

        return new HealthConnectShellCommand()
                .exec(
                        this,
                        in.getFileDescriptor(),
                        out.getFileDescriptor(),
                        err.getFileDescriptor(),
                        args);
    }

    @Override
    public void grantHealthPermission(String packageName, String permissionName, UserHandle user) {
        checkParamsNonNull(packageName, permissionName, user);

        throwIllegalStateExceptionIfDataSyncInProgress();
        mPermissionHelper.grantHealthPermission(packageName, permissionName, user);
    }

    @Override
    public List<String> grantHealthPermissions(
            String packageName, List<String> permissionNames, UserHandle user) {
        checkParamsNonNull(packageName, permissionNames, user);

        if (!AconfigFlagHelper.isHealthPermissionReaderImprovementsEnabled()) {
            throw new UnsupportedOperationException("grantHealthPermissions is not enabled");
        }

        throwIllegalStateExceptionIfDataSyncInProgress();
        return mPermissionHelper.grantHealthPermissions(packageName, permissionNames, user);
    }

    @Override
    public void revokeHealthPermission(
            String packageName, String permissionName, @Nullable String reason, UserHandle user) {
        checkParamsNonNull(packageName, permissionName, user);

        throwIllegalStateExceptionIfDataSyncInProgress();
        mPermissionHelper.revokeHealthPermission(packageName, permissionName, reason, user);
    }

    @Override
    public List<String> revokeHealthPermissions(
            String packageName,
            List<String> permissionNames,
            @Nullable String reason,
            UserHandle user) {
        checkParamsNonNull(packageName, permissionNames, user);

        if (!AconfigFlagHelper.isHealthPermissionReaderImprovementsEnabled()) {
            throw new UnsupportedOperationException("revokeHealthPermissions is not enabled");
        }

        throwIllegalStateExceptionIfDataSyncInProgress();
        return mPermissionHelper.revokeHealthPermissions(
                packageName, permissionNames, reason, user);
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
     *     are in same order as {@code record}. In case of an error or a permission failure in the
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
                    Set<@RecordTypeIdentifier.RecordType Integer> recordTypeIds =
                            recordInternals.stream()
                                    .map(RecordInternal::getRecordType)
                                    .collect(toSet());
                    List<String> uuids =
                            mFitnessRecordUpsertHelper.insertRecords(
                                    requireNonNull(attributionSource.getPackageName()),
                                    recordInternals,
                                    mDataPermissionEnforcer.collectGrantedPerRecordWritePermissions(
                                            recordTypeIds, attributionSource),
                                    /* shouldGenerateAccessLogs= */ true);
                    tryAndReturnResult(callback, uuids, logger);

                    logRecordTypeSpecificUpsertMetrics(
                            recordInternals, attributionSource.getPackageName());
                    logger.setDataTypesFromRecordInternals(recordInternals);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
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
        final AggregateDataRequestParcel unmaskedRequest =
                request.toUnmasked(getUnmaskingFunction(attributionSource.getPackageName()));

        ErrorCallback errorCallback = callback::onError;
        scheduleLoggingHealthDataApiErrors(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    logger.setNumberOfRecords(unmaskedRequest.getAggregateIds().length);
                    throwExceptionIfDataSyncInProgress();
                    List<Integer> recordTypesToTest = new ArrayList<>();
                    for (int aggregateId : unmaskedRequest.getAggregateIds()) {
                        recordTypesToTest.add(
                                mAggregationTypeIdMapper
                                        .getAggregationTypeFor(aggregateId)
                                        .getApplicableRecordTypeId());
                    }

                    long startDateAccess = unmaskedRequest.getStartTime();
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
                                unmaskedRequest.getPackageFilters(),
                                attributionSource.getPackageName(),
                                enforceSelfRead,
                                "aggregationTypes: "
                                        + Arrays.stream(unmaskedRequest.getAggregateIds())
                                                .mapToObj(
                                                        mAggregationTypeIdMapper
                                                                ::getAggregationTypeFor)
                                                .collect(Collectors.toList()));
                    }
                    boolean shouldRecordAccessLog = !holdsDataManagementPermission;
                    AggregateDataResponseParcel maskedResponse =
                            mFitnessRecordAggregateHelper
                                    .aggregateRecords(
                                            attributionSource.getPackageName(),
                                            unmaskedRequest,
                                            startDateAccess,
                                            shouldRecordAccessLog)
                                    .toMasked(
                                            getMaskingFunction(attributionSource.getPackageName()));
                    callback.onResult(maskedResponse);
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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(holdsDataManagementPermission, READ_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);
        final ReadRecordsRequestParcel unmaskedRequest =
                request.toUnmasked(getUnmaskingFunction(callingPackageName));

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
                                unmaskedRequest.getRecordType(), attributionSource)) {
                            // If read permission is missing but write permission is granted,
                            // then enforce self read
                            enforceSelfRead = true;
                        } else if (!isInForeground) {
                            // If READ_HEALTH_DATA_IN_BACKGROUND permission is not granted,
                            // then enforce self read
                            enforceSelfRead = shouldEnforceSelfRead(uid, pid, attributionSource);
                        }
                        if (unmaskedRequest.getRecordIdFiltersParcel() == null) {
                            // Only enforce requested packages if this is a
                            // ReadRecordsByRequest using filters. Reading by IDs does not have
                            // data origins specified.
                            // TODO(b/309778116): Consider throwing an error when reading by Id
                            maybeEnforceOnlyCallingPackageDataRequested(
                                    unmaskedRequest.getPackageFilters(),
                                    callingPackageName,
                                    enforceSelfRead,
                                    "recordType: "
                                            + mHealthConnectMappings
                                                    .getRecordIdToExternalRecordClassMap()
                                                    .get(unmaskedRequest.getRecordType()));
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
                                    Set.of(unmaskedRequest.getRecordType()), attributionSource);
                    final RecordHelper<?> recordHelper =
                            mInternalHealthConnectMappings.getRecordHelper(
                                    unmaskedRequest.getRecordType());
                    final Set<String> grantedGranularPermissions;
                    if (holdsDataManagementPermission) {
                        grantedGranularPermissions = recordHelper.getGranularReadPermissions();
                    } else {
                        grantedGranularPermissions =
                                recordHelper.getGranularReadPermissions().stream()
                                        .filter(
                                                permission ->
                                                        mDataPermissionEnforcer.isPermissionGranted(
                                                                permission, attributionSource))
                                        .collect(Collectors.toSet());

                        if (enforceSelfRead) {
                            grantedGranularPermissions.addAll(
                                    recordHelper.getAllPerRecordWritePermissions().stream()
                                            .filter(
                                                    permission ->
                                                            mDataPermissionEnforcer
                                                                    .isPermissionGranted(
                                                                            permission,
                                                                            attributionSource))
                                            .collect(Collectors.toSet()));
                        }
                    }

                    try {
                        long startDateAccessEpochMilli = unmaskedRequest.getStartTime();

                        if (!holdsDataManagementPermission
                                && !isPermissionGranted(READ_HEALTH_DATA_HISTORY, uid, pid)) {
                            Instant startDateAccessInstant =
                                    mPermissionHelper.getHealthDataStartDateAccessOrThrow(
                                            callingPackageName, userHandle);

                            // Always set the startDateAccess for local time filter, as for
                            // local date time we use it in conjunction with the time filter
                            // start-time
                            if (unmaskedRequest.usesLocalTimeFilter()
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
                                        unmaskedRequest,
                                        grantedExtraReadPermissions,
                                        grantedGranularPermissions,
                                        startDateAccessEpochMilli,
                                        isInForeground,
                                        shouldRecordAccessLog,
                                        enforceSelfRead);
                        List<RecordInternal<?>> records = readRecordsResponse.first;
                        long pageToken = readRecordsResponse.second.encode();

                        logger.setNumberOfRecords(records.size());

                        if (Constants.DEBUG) {
                            Slog.d(TAG, "pageToken: " + pageToken);
                        }

                        final ReadRecordsResponseParcel maskedResponseParcel =
                                new ReadRecordsResponseParcel(new RecordsParcel(records), pageToken)
                                        .toMasked(getMaskingFunction(callingPackageName));
                        callback.onResult(maskedResponseParcel);

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
                    Set<@RecordTypeIdentifier.RecordType Integer> recordTypeIds =
                            recordInternals.stream()
                                    .map(RecordInternal::getRecordType)
                                    .collect(toSet());
                    mFitnessRecordUpsertHelper.updateRecords(
                            requireNonNull(attributionSource.getPackageName()),
                            recordInternals,
                            mDataPermissionEnforcer.collectGrantedPerRecordWritePermissions(
                                    recordTypeIds, attributionSource),
                            /* shouldGenerateAccessLogs= */ true);
                    tryAndReturnResult(callback, logger);
                    logRecordTypeSpecificUpsertMetrics(
                            recordInternals, attributionSource.getPackageName());
                    logger.setDataTypesFromRecordInternals(recordInternals);
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
        final ChangeLogTokenRequest unmaskedRequest =
                request.toUnmasked(getUnmaskingFunction(attributionSource.getPackageName()));

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
                    if (isPhrChangeLogsEnabled()) {
                        boolean hasRecordTypes = !unmaskedRequest.getRecordTypeIds().isEmpty();
                        boolean hasMedicalResourceTypes =
                                !unmaskedRequest.getMedicalResourceTypes().isEmpty();
                        if (!hasRecordTypes && !hasMedicalResourceTypes) {
                            throw new IllegalArgumentException(
                                    "At least one Record type or Medical Resource type must be"
                                            + " set");
                        } else if (hasRecordTypes && hasMedicalResourceTypes) {
                            throw new IllegalArgumentException(
                                    "Record types and Medical Resource types can't both be set");
                        }
                    } else {
                        if (unmaskedRequest.getRecordTypeIds().isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Requested record types must not be empty.");
                        }
                    }

                    mDataPermissionEnforcer.enforceReadPermissions(
                            unmaskedRequest.getRecordTypeIds(), attributionSource);
                    if (isPhrChangeLogsEnabled()) {
                        mMedicalDataPermissionEnforcer.enforceMedicalResourceTypesReadPermissions(
                                unmaskedRequest.getMedicalResourceTypes(), attributionSource);
                    }

                    callback.onResult(
                            new ChangeLogTokenResponse(
                                    mChangeLogsRequestHelper.getToken(
                                            mChangeLogsHelper.getLatestRowId(),
                                            attributionSource.getPackageName(),
                                            unmaskedRequest)));
                    logger.setHealthDataServiceApiStatusSuccess()
                            .setDataTypesFromRecordTypes(
                                    unmaskedRequest.getRecordTypeIds().stream().toList());
                    if (isPhrChangeLogsEnabled()) {
                        logger.setMedicalResourceTypes(unmaskedRequest.getMedicalResourceTypes());
                    }
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
        final String callerPackageName = requireNonNull(attributionSource.getPackageName());
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

                    // Check that the token request is valid.
                    ChangeLogsRequestHelper.TokenRequest changeLogsTokenRequest =
                            mChangeLogsRequestHelper.getRequest(
                                    callerPackageName, request.getToken());
                    if (isPhrChangeLogsEnabled()) {
                        boolean hasRecordTypes = !changeLogsTokenRequest.getRecordTypes().isEmpty();
                        boolean hasMedicalResourceTypes =
                                !changeLogsTokenRequest.getMedicalResourceTypes().isEmpty();
                        if (!hasRecordTypes && !hasMedicalResourceTypes) {
                            throw new IllegalArgumentException(
                                    "At least one Record type or Medical Resource type must be"
                                            + " set");
                        } else if (hasRecordTypes && hasMedicalResourceTypes) {
                            throw new IllegalArgumentException(
                                    "Record types and Medical Resource types can't both be set");
                        }
                    } else {
                        if (changeLogsTokenRequest.getRecordTypes().isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Requested record types must not be empty.");
                        }
                    }

                    // Permissions check.
                    // This API doesn't support reading own data without read permissions, so
                    // enforce permissions instead of allowing self read.
                    mDataPermissionEnforcer.enforceReadPermissions(
                            changeLogsTokenRequest.getRecordTypes(), attributionSource);
                    if (isPhrChangeLogsEnabled()) {
                        mMedicalDataPermissionEnforcer.enforceMedicalResourceTypesReadPermissions(
                                changeLogsTokenRequest.getMedicalResourceTypes(),
                                attributionSource);
                    }

                    final ChangeLogsHelper.ChangeLogsResponse changeLogsResponse =
                            mChangeLogsHelper.getChangeLogs(
                                    mAppInfoHelper,
                                    changeLogsTokenRequest,
                                    request,
                                    mChangeLogsRequestHelper);

                    // Read upserted records.
                    List<RecordInternal<?>> recordInternals = List.of();
                    Map<Integer, List<UUID>> recordTypeToUpsertedUuids =
                            changeLogsResponse.getRecordTypeToUpsertedUuids();
                    if (!recordTypeToUpsertedUuids.isEmpty()) {
                        long startDateAccessEpochMilli =
                                isPermissionGranted(READ_HEALTH_DATA_HISTORY, uid, pid)
                                        ? DEFAULT_LONG
                                        : mPermissionHelper
                                                .getHealthDataStartDateAccessOrThrow(
                                                        callerPackageName, userHandle)
                                                .toEpochMilli();
                        Set<String> grantedExtraReadPermissions =
                                mDataPermissionEnforcer.collectGrantedExtraReadPermissions(
                                        recordTypeToUpsertedUuids.keySet(), attributionSource);
                        final Set<String> grantedGranularPermissions =
                                recordTypeToUpsertedUuids.keySet().stream()
                                        .map(mInternalHealthConnectMappings::getRecordHelper)
                                        .flatMap(
                                                recordHelper ->
                                                        recordHelper
                                                                .getGranularReadPermissions()
                                                                .stream())
                                        .filter(
                                                permission ->
                                                        mDataPermissionEnforcer.isPermissionGranted(
                                                                permission, attributionSource))
                                        .collect(Collectors.toSet());
                        recordInternals =
                                mFitnessRecordReadHelper.readRecords(
                                        mTransactionManager,
                                        callerPackageName,
                                        recordTypeToUpsertedUuids,
                                        grantedExtraReadPermissions,
                                        grantedGranularPermissions,
                                        startDateAccessEpochMilli,
                                        isInForeground,
                                        /* shouldRecordAccessLogs= */ true);
                    }
                    List<DeletedLog> deletedLogs = changeLogsResponse.getDeletedLogs();

                    // Read upserted medical resources.
                    List<MedicalResourceId> upsertedMedicalResourceIds =
                            changeLogsResponse.getUpsertedMedicalResourceIds();
                    List<MedicalResource> upsertedMedicalResources =
                            (isPhrChangeLogsEnabled() && !upsertedMedicalResourceIds.isEmpty())
                                    ? mMedicalResourceHelper
                                            .readMedicalResourcesByIdsWithoutPermissionChecks(
                                                    upsertedMedicalResourceIds)
                                    : List.of();
                    List<DeletedMedicalResource> deletedMedicalResources =
                            isPhrChangeLogsEnabled()
                                    ? changeLogsResponse.getDeletedMedicalResources()
                                    : List.of();

                    // Masking the ChangeLogsResponse itself would necessitate two redundant
                    // deep-copy operations(to convert to RecordInternal, mask, and convert
                    // back), which is avoided by masking the record list preemptively,
                    // improving efficiency.
                    List<RecordInternal<?>> maskedRecordInternals =
                            recordInternals.stream()
                                    .map(
                                            recordInternal ->
                                                    recordInternal.toMasked(
                                                            getMaskingFunction(callerPackageName)))
                                    .collect(toList());

                    callback.onResult(
                            new ChangeLogsResponse(
                                    maskedRecordInternals,
                                    deletedLogs,
                                    upsertedMedicalResources,
                                    deletedMedicalResources,
                                    changeLogsResponse.getNextPageToken(),
                                    changeLogsResponse.hasMorePages()));

                    var numberOfChanges =
                            isPhrChangeLogsEnabled()
                                    ? recordInternals.size()
                                            + deletedLogs.size()
                                            + upsertedMedicalResources.size()
                                            + deletedMedicalResources.size()
                                    : recordInternals.size() + deletedLogs.size();
                    logger.setHealthDataServiceApiStatusSuccess()
                            .setNumberOfRecords(numberOfChanges)
                            .setDataTypesFromRecordInternals(recordInternals);
                    if (isPhrChangeLogsEnabled()) {
                        logger.setMedicalResourceTypes(
                                upsertedMedicalResources.stream()
                                        .map(MedicalResource::getType)
                                        .collect(Collectors.toSet()));
                    }
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
                    DeleteUsingFiltersRequestParcel unmaskedRequest = request;
                    List<Integer> recordTypeIdsToDelete =
                            (!request.getRecordTypeFilters().isEmpty())
                                    ? request.getRecordTypeFilters()
                                    : new ArrayList<>(
                                            mHealthConnectMappings
                                                    .getRecordIdToExternalRecordClassMap()
                                                    .keySet());

                    Set<String> grantedPerRecordWritePermissions;
                    if (!holdsDataManagementPermission) {
                        tryAcquireApiCallQuota(
                                uid,
                                QuotaCategory.QUOTA_CATEGORY_WRITE,
                                mAppOpsManagerLocal.isUidInForeground(uid),
                                logger);
                        mDataPermissionEnforcer.enforceWritePermissions(
                                recordTypeIdsToDelete, attributionSource);
                        grantedPerRecordWritePermissions =
                                mDataPermissionEnforcer.collectGrantedPerRecordWritePermissions(
                                        request.getRecordTypeFilters(), attributionSource);
                    } else {
                        grantedPerRecordWritePermissions =
                                mInternalHealthConnectMappings.getAllPerRecordWritePermissions();
                        // While we restrict normal apps from deleting device data when they
                        // don't explicitly use record IDs, apps holding the data management
                        // permission (like the controller) get a pass to delete data across
                        // device data providers
                        unmaskedRequest =
                                request.toUnmasked(
                                        getUnmaskingFunction(
                                                requireNonNull(
                                                        attributionSource.getPackageName())));
                    }

                    int numberOfRecordsDeleted =
                            mFitnessRecordDeleteHelper.deleteRecords(
                                    requireNonNull(attributionSource.getPackageName()),
                                    unmaskedRequest,
                                    grantedPerRecordWritePermissions,
                                    /* enforceSelfDelete= */ !holdsDataManagementPermission,
                                    /* shouldRecordAccessLog= */ !holdsDataManagementPermission);
                    tryAndReturnResult(callback, logger);
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
            AttributionSource attributionSource,
            @HealthDataCategory.Type int dataCategory,
            IGetPriorityResponseCallback callback) {
        checkParamsNonNull(callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String callingPackageName = requireNonNull(attributionSource.getPackageName());
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
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
                    GetPriorityResponseParcel maskedResult =
                            new GetPriorityResponseParcel(
                                            new FetchDataOriginsPriorityOrderResponse(
                                                    dataOriginInPriorityOrder))
                                    .toMasked(getMaskingFunction(callingPackageName));

                    callback.onResult(maskedResult);
                },
                errorCallback);
    }

    /** API to update priority for permission category(ies) */
    @Override
    public void updatePriority(
            AttributionSource attributionSource,
            UpdatePriorityRequestParcel updatePriorityRequest,
            IEmptyResponseCallback callback) {
        checkParamsNonNull(updatePriorityRequest, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String callingPackageName = requireNonNull(attributionSource.getPackageName());

        UpdatePriorityRequestParcel unmaskedRequest =
                updatePriorityRequest.toUnmasked(getUnmaskingFunction(callingPackageName));

        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    throwExceptionIfDataSyncInProgress();
                    mHealthDataCategoryPriorityHelper.setPriorityOrder(
                            unmaskedRequest.getDataCategory(),
                            unmaskedRequest.getPackagePriorityOrder());
                    callback.onResult();
                },
                errorCallback);
    }

    @Override
    public void setRecordRetentionPeriodInDays(
            int days, UserHandle user, IEmptyResponseCallback callback) {
        checkParamsNonNull(user, callback);
        ErrorCallback wrappedCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    throwExceptionIfDataSyncInProgress();
                    mPreferencesManager.setRecordRetentionPeriodInDays(days);
                    callback.onResult();
                },
                wrappedCallback);
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
    public void getContributorApplicationsInfo(
            AttributionSource attributionSource, IApplicationInfoResponseCallback callback) {
        checkParamsNonNull(callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String callingPackageName = requireNonNull(attributionSource.getPackageName());
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    throwExceptionIfDataSyncInProgress();
                    // Get AppInfo IDs which has PHR data.
                    Set<Long> appIdsWithPhrData =
                            mMedicalDataSourceHelper.getAllContributorAppInfoIds();
                    // Get all AppInfos which has either Fitness data or PHR data.
                    List<AppInfo> applicationInfosWithData =
                            mAppInfoHelper.getApplicationInfosWithRecordTypesOrInIdsList(
                                    appIdsWithPhrData);
                    ApplicationInfoResponseParcel maskedResult =
                            new ApplicationInfoResponseParcel(applicationInfosWithData)
                                    .toMasked(getMaskingFunction(callingPackageName));
                    callback.onResult(maskedResult);
                },
                errorCallback);
    }

    /** Retrieves {@link RecordTypeInfoResponse} for each RecordType. */
    @Override
    public void queryAllRecordTypesInfo(
            AttributionSource attributionSource, IRecordTypeInfoResponseCallback callback) {
        checkParamsNonNull(callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String callingPackageName = requireNonNull(attributionSource.getPackageName());
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    throwExceptionIfDataSyncInProgress();
                    RecordTypeInfoResponseParcel result =
                            new RecordTypeInfoResponseParcel(getPopulatedRecordTypeInfoResponses());
                    callback.onResult(result.toMasked(getMaskingFunction(callingPackageName)));
                },
                errorCallback);
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

        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    throwExceptionIfDataSyncInProgress();
                    final List<AccessLog> accessLogsList =
                            mAccessLogsHelper.queryAccessLogs(userHandle);
                    callback.onResult(new AccessLogsResponseParcel(accessLogsList));
                },
                errorCallback);
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

        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    throwExceptionIfDataSyncInProgress();
                    List<LocalDate> localDates =
                            mRecordDateHelper.getRecordDates(
                                    activityDatesRequestParcel.getRecordTypes());

                    callback.onResult(new ActivityDatesResponseParcel(localDates));
                },
                errorCallback);
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
                                Slog.e(TAG, "Restore response could not be sent to the caller.", e);
                            }
                            return;
                        }
                        mBackupRestore.stageAllHealthConnectRemoteData(
                                pfdsByFileName, exceptionsByFileName, userHandle, callback);
                    });
        } catch (SecurityException | IllegalStateException e) {
            Slog.e(TAG, "Exception encountered while staging", e);
            try {
                @HealthConnectException.ErrorCode
                int errorCode = (e instanceof SecurityException) ? ERROR_SECURITY : ERROR_INTERNAL;
                exceptionsByFileName.put("", new HealthConnectException(errorCode, e.getMessage()));

                callback.onError(new StageRemoteDataException(exceptionsByFileName));
            } catch (RemoteException remoteException) {
                Slog.e(TAG, "Stage data response could not be sent to the caller.", e);
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
            if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                requireNonNull(mDeviceDataProviderManager);
                mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
                mDeviceDataProviderManager.advertiseCurrentDeviceNativeCapabilities();
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

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void updateHealthConnectBackupAndRestoreSettings(
            UpdateBackupAndRestoreSettingsRequest request) {

        enforceIsForegroundUser(Binder.getCallingUserHandle());

        mDataPermissionEnforcer.enforceAnyOfPermissions(
                BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS, BACKUP);

        // TODO: b/426180714 Introduce settings storage and write the settings into it, similar to
        // configureScheduledExport()
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void updateHealthConnectRestoreStatus(
            @NonNull UpdateHealthConnectRestoreStatusRequest request) {
        enforceIsForegroundUser(Binder.getCallingUserHandle());

        mDataPermissionEnforcer.enforceAnyOfPermissions(
                RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS, BACKUP);

        // TODO(b/427455608): Add implementation, write data into settings etc.
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
                                Slog.e(
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
                                Slog.e(
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
            Slog.e(TAG, "getHealthConnectDataState: Exception encountered", e);
            @HealthConnectException.ErrorCode
            int errorCode = (e instanceof SecurityException) ? ERROR_SECURITY : ERROR_INTERNAL;
            try {
                callback.onError(
                        new HealthConnectExceptionParcel(
                                new HealthConnectException(errorCode, e.getMessage())));
            } catch (RemoteException remoteException) {
                Slog.e(TAG, "getHealthConnectDataState error could not be sent", e);
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
                            Slog.e(
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
                            Slog.e(
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
                            Slog.e(
                                    TAG,
                                    "Exception for HealthConnectMigrationUiState could not be sent"
                                            + " to the caller.",
                                    remoteException);
                        }
                    }
                });
    }

    @Override
    public void getDeviceDataSources(
            AttributionSource attributionSource, IGetDeviceDataSourcesCallback callback) {
        checkParamsNonNull(attributionSource, callback);

        final int uid = Binder.getCallingUid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String callingPackageName = requireNonNull(attributionSource.getPackageName());

        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(false, GET_DEVICE_DATA_SOURCES)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        ErrorCallback errorCallback = callback::onError;

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "getDeviceDataSources is not supported");
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);

                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                    List<DeviceDataSourceInfo> infos =
                            requireNonNull(mDeviceDataProviderManager).getDeviceDataSourceInfos();

                    List<String> grantedPermissionsList =
                            mPermissionHelper.getGrantedHealthPermissions(
                                    callingPackageName, userHandle);
                    Set<String> grantedPermissions = new HashSet<>(grantedPermissionsList);

                    List<DeviceDataSource> result =
                            getVisibleDeviceDataSources(infos, grantedPermissions);

                    Function<String, String> maskingFunction =
                            getMaskingFunction(callingPackageName);

                    List<DeviceDataSource> maskedResult = new ArrayList<>();
                    for (DeviceDataSource dataSource : result) {
                        maskedResult.add(dataSource.toMasked(maskingFunction));
                    }

                    callback.onResult(new GetDeviceDataSourcesResponse(maskedResult));
                    logger.setHealthDataServiceApiStatusSuccess();
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    /**
     * Filters the raw device infos. A device is included only if the caller holds at least one read
     * permission relevant to the data types offered by that device.
     */
    private List<DeviceDataSource> getVisibleDeviceDataSources(
            List<DeviceDataSourceInfo> deviceDataSources, Set<String> grantedPermissions) {

        List<DeviceDataSource> result = new ArrayList<>();
        for (DeviceDataSourceInfo deviceDataSource : deviceDataSources) {
            DeviceDataSource authorizedDevice =
                    processDeviceDataSource(
                            deviceDataSource,
                            grantedPermissions,
                            /* skipPermissionChecks= */ false);

            if (authorizedDevice != null) {
                result.add(authorizedDevice);
            }
        }
        return result;
    }

    @Override
    public void getCurrentDeviceDataSource(
            AttributionSource attributionSource, IGetCurrentDeviceDataSourceCallback callback) {
        checkParamsNonNull(attributionSource, callback);

        final int uid = Binder.getCallingUid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String callingPackageName = requireNonNull(attributionSource.getPackageName());

        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(false, GET_CURRENT_DEVICE_DATA_SOURCE)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        ErrorCallback errorCallback = callback::onError;

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "getCurrentDeviceDataSource is not supported");
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);

                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);

                    List<String> grantedPermissionsList =
                            mPermissionHelper.getGrantedHealthPermissions(
                                    callingPackageName, userHandle);
                    boolean hasGrantedReadPermission = false;
                    for (String grantedPermission : grantedPermissionsList) {
                        if (mHealthConnectMappings.isReadPermission(grantedPermission)) {
                            hasGrantedReadPermission = true;
                            break;
                        }
                    }

                    if (!hasGrantedReadPermission) {
                        throw new SecurityException(
                                "Caller must hold at least one Health Connect permission");
                    }

                    DeviceDataSource result = null;
                    DeviceDataSourceInfo deviceDataSourceInfo = getCurrentDeviceDataSourceInfo();
                    if (deviceDataSourceInfo != null) {
                        // result will never be null here as we skip the permission checks in
                        // #processDeviceDataSource
                        result =
                                processDeviceDataSource(
                                        deviceDataSourceInfo,
                                        /* unused= */ Set.of(),
                                        /* skipPermissionChecks= */ true);
                    }
                    if (result == null) {
                        result =
                                requireNonNull(mDeviceDataProviderManager)
                                        .getDefaultCurrentDeviceDataSource();
                    }
                    Function<String, String> maskingFunction =
                            getMaskingFunction(callingPackageName);
                    DeviceDataSource maskedResult = result.toMasked(maskingFunction);
                    callback.onResult(maskedResult);
                    logger.setHealthDataServiceApiStatusSuccess();
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    @Nullable
    private DeviceDataSourceInfo getCurrentDeviceDataSourceInfo() {
        List<DeviceDataSourceInfo> infos = new ArrayList<>();
        for (DeviceDataSourceInfo info :
                requireNonNull(mDeviceDataProviderManager).getDeviceDataSourceInfos()) {
            if (info.isCurrentDevice()) {
                infos.add(info);
            }
        }
        if (infos.isEmpty()) {
            return null;
        }
        if (infos.size() > 1) {
            Slog.e(TAG, "Illegal state, more than one current device found");
        }
        return infos.get(0);
    }

    /**
     * Processes a single device. Aggregates its data types, calculates status flags, and validates
     * permissions.
     *
     * <p>If {@code skipPermissionChecks} is false, returns null if there is not at least one
     * permission among {@code grantedPermissions} that matches the data types offered by the
     * device.
     *
     * <p>If {@code skipPermissionChecks} is true, {@code grantedPermissions} is unchecked and a
     * DeviceDataSource is always returned.
     */
    @Nullable
    private DeviceDataSource processDeviceDataSource(
            DeviceDataSourceInfo info,
            Set<String> grantedPermissions,
            boolean skipPermissionChecks) {
        Map<Pair<Class<? extends Record>, Integer>, List<DeviceDataTypeAdvertisement>>
                recordTypeToAdvertisements = new ArrayMap<>();

        for (DeviceDataProviderInfo providerInfo : info.getDeviceDataProviderInfos()) {
            for (DeviceDataTypeAdvertisement ad : providerInfo.getDeviceDataTypeAdvertisements()) {
                recordTypeToAdvertisements
                        .computeIfAbsent(
                                new Pair<>(ad.getDataType(), ad.getSymptomType()),
                                k -> new ArrayList<>())
                        .add(ad);
            }
        }

        Set<DeviceDataTypeSource> deviceDataTypeSources = new HashSet<>();
        boolean hasAtLeastOnePermission = false;

        for (Map.Entry<Pair<Class<? extends Record>, Integer>, List<DeviceDataTypeAdvertisement>>
                entry : recordTypeToAdvertisements.entrySet()) {

            Class<? extends Record> dataType = entry.getKey().first;
            int symptomType = entry.getKey().second;
            List<DeviceDataTypeAdvertisement> ads = entry.getValue();

            if (!skipPermissionChecks
                    && !hasAtLeastOnePermission
                    && hasReadPermissionForDataType(dataType, grantedPermissions)) {
                hasAtLeastOnePermission = true;
            }

            deviceDataTypeSources.add(createDeviceDataTypeSource(dataType, symptomType, ads));
        }

        // This is needed when the DDP device is no longer advertised. We check the record types
        // used to determine if the source is visible to the caller.
        if (!skipPermissionChecks && !hasAtLeastOnePermission) {
            String spn = info.getDeviceDataOrigin().getPackageName();
            AppInfoInternal appInfo = mAppInfoHelper.getAppInfoMap().get(spn);
            if (appInfo != null && appInfo.getRecordTypesUsed() != null) {
                for (int recordType : appInfo.getRecordTypesUsed()) {
                    Class<? extends Record> dataType =
                            mHealthConnectMappings
                                    .getRecordIdToExternalRecordClassMap()
                                    .get(recordType);
                    if (dataType != null
                            && hasReadPermissionForDataType(dataType, grantedPermissions)) {
                        hasAtLeastOnePermission = true;
                        break;
                    }
                }
            }
        }

        if (hasAtLeastOnePermission || skipPermissionChecks) {
            return new DeviceDataSource(
                    info.getDeviceDataOrigin(), info.getDevice(), deviceDataTypeSources);
        }

        return null;
    }

    /** Checks if required permission is present to read the provided data type. */
    private boolean hasReadPermissionForDataType(
            Class<? extends Record> dataType, Set<String> grantedPermissions) {
        int recordType = mHealthConnectMappings.getRecordType(dataType);
        Set<Integer> categories =
                mHealthConnectMappings.getHealthPermissionCategoriesForRecordType(recordType);

        for (int category : categories) {
            String permission = mHealthConnectMappings.getHealthReadPermission(category);
            if (grantedPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the source status for a data type. Available if ANY advertisement is available.
     * Enabled if ANY device data providers are enabled.
     */
    private DeviceDataTypeSource createDeviceDataTypeSource(
            Class<? extends Record> dataType,
            int symptomType,
            List<DeviceDataTypeAdvertisement> ads) {

        boolean isAvailable = false;
        boolean isUserEnabled = false;

        for (DeviceDataTypeAdvertisement ad : ads) {
            if (ad.isAvailable()) {
                isAvailable = true;
            }
            if (ad.isUserEnabled()) {
                isUserEnabled = true;
            }
        }

        if (SymptomRecord.class.isAssignableFrom(dataType)) {
            return DeviceDataTypeSource.ofSymptomType(symptomType, isAvailable, isUserEnabled);
        } else {
            return DeviceDataTypeSource.ofDataType(dataType, isAvailable, isUserEnabled);
        }
    }

    @Override
    public void getDeviceDataSourceInfos(
            AttributionSource attributionSource, IGetDeviceDataSourceInfosCallback callback) {
        checkParamsNonNull(attributionSource, callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);

                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        callback.onResult(Collections.emptyList());
                        return;
                    }

                    List<DeviceDataSourceInfo> infos =
                            mDeviceDataProviderManager.getDeviceDataSourceInfos();
                    Function<String, String> maskingFunction =
                            getMaskingFunction(attributionSource.getPackageName());

                    List<DeviceDataSourceInfo> maskedInfos = new ArrayList<>();
                    for (DeviceDataSourceInfo info : infos) {
                        maskedInfos.add(info.toMasked(maskingFunction));
                    }

                    callback.onResult(maskedInfos);
                },
                callback::onError);
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
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    final Context userContext = mContext.createContextAsUser(userHandle, 0);
                    ScheduledExportStatus status =
                            mExportImportSettingsStorage.getScheduledExportStatus(userContext);
                    callback.onResult(status);
                },
                errorCallback);
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
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    ImportStatus status = mExportImportSettingsStorage.getImportStatus();
                    callback.onResult(status);
                },
                errorCallback);
    }

    @Override
    public void runImport(UserHandle user, Uri file, IEmptyResponseCallback callback) {
        checkParamsNonNull(user, file, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    mImportManager.runImport(userHandle, file);
                    callback.onResult();
                },
                errorCallback);
    }

    @Override
    public void runImmediateExport(Uri file, IEmptyResponseCallback callback) {
        checkParamsNonNull(file, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    // TODO(b/370954019): Modify runExport to use specific file.
                    mExportManager.runExport(userHandle);
                    callback.onResult();
                },
                errorCallback);
    }

    /** Queries the document providers available to be used for export/import. */
    @Override
    public void queryDocumentProviders(UserHandle user, IQueryDocumentProvidersCallback callback) {
        checkParamsNonNull(user, callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    final Context userContext = mContext.createContextAsUser(userHandle, 0);
                    final List<ExportImportDocumentProvider> providers =
                            DocumentProvidersManager.queryDocumentProviders(userContext);
                    callback.onResult(providers);
                },
                errorCallback);
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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, GET_MEDICAL_DATA_SOURCES_BY_IDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, GET_MEDICAL_DATA_SOURCES_BY_REQUESTS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, DELETE_MEDICAL_DATA_SOURCE_WITH_DATA)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
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
     * Service implementation of {@link HealthConnectManager#upsertMedicalResources}.
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
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, UPSERT_MEDICAL_RESOURCES)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    List<UpsertMedicalResourceRequest> requests =
                            requestsParcel.getUpsertRequests();
                    if (requests.isEmpty()) {
                        tryAndReturnResult(
                                callback, new MedicalResourceListParcel(List.of()), logger);
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
                            getOrCreateFhirResourceValidator();
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

                    tryAndReturnResult(
                            callback, new MedicalResourceListParcel(medicalResources), logger);
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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, READ_MEDICAL_RESOURCES_BY_IDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    // Stores the timestamp for calls made by ANY client, including the
                    // controller
                    mPreferencesManager.setLastPhrReadMedicalResourcesApiTimeStamp(
                            mTimeSource.getInstantNow());

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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, READ_MEDICAL_RESOURCES_BY_REQUESTS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    // Stores the timestamp for calls made by ANY client, including the
                    // controller
                    mPreferencesManager.setLastPhrReadMedicalResourcesApiTimeStamp(
                            mTimeSource.getInstantNow());

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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, DELETE_MEDICAL_RESOURCES_BY_IDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
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
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, DELETE_MEDICAL_RESOURCES_BY_REQUESTS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        scheduleLoggingHealthDataApiErrors(
                () -> {
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
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    throwExceptionIfDataSyncInProgress();
                    callback.onResult(getPopulatedMedicalResourceTypeInfos());
                },
                errorCallback);
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void updateHealthConnectBackupStatus(
            @NonNull UpdateHealthConnectBackupStatusRequest request) {
        final UserHandle userHandle = Binder.getCallingUserHandle();
        enforceIsForegroundUser(userHandle);

        mDataPermissionEnforcer.enforceAnyOfPermissions(
                BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS, BACKUP);

        // TODO(b/427454680): Add implementation, write the provided data into settings storage
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void getChangesForBackup(
            @Nullable String changeToken, IGetChangesForBackupResponseCallback callback) {
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    // resolved.
                    if (!isCloudBackupRestoreEnabled()) {
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
                },
                errorCallback);
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void getLatestMetadataForBackup(IGetLatestMetadataForBackupResponseCallback callback) {
        checkParamsNonNull(callback);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    if (!isCloudBackupRestoreEnabled()) {
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
                },
                errorCallback);
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
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    if (!isCloudBackupRestoreEnabled()) {
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
                },
                errorCallback);
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void canRestore(int dataVersion, ICanRestoreResponseCallback callback) {
        checkParamsNonNull(dataVersion);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    if (!isCloudBackupRestoreEnabled()) {
                        throw new UnsupportedOperationException("canRestore is not supported.");
                    }
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(
                            RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS,
                            pid,
                            uid,
                            "Caller does not have permission to call canRestore.");
                    callback.onResult(mCloudRestoreManager.canRestore(dataVersion));
                },
                errorCallback);
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public void restoreChanges(List<RestoreChange> changes, IEmptyResponseCallback callback) {
        checkParamsNonNull(changes);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    if (!isCloudBackupRestoreEnabled()) {
                        throw new UnsupportedOperationException("restoreChanges is not supported.");
                    }
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(
                            RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS,
                            pid,
                            uid,
                            "Caller does not have permission to call" + " restoreChanges.");
                    mCloudRestoreManager.restoreChanges(changes);
                    callback.onResult();
                },
                errorCallback);
    }

    /**
     * @see HealthConnectManager#getHealthConnectOnboardingState
     */
    @Override
    public void getHealthConnectOnboardingState(IGetHealthConnectOnboardingStateCallback callback) {
        checkParamsNonNull(callback);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;
        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    if (!Flags.onboarding()) {
                        throw new UnsupportedOperationException(
                                "Getting health connect onboarding state is not supported");
                    }
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(
                            MANAGE_HEALTH_DATA_PERMISSION,
                            pid,
                            uid,
                            "Caller does not have " + MANAGE_HEALTH_DATA_PERMISSION);
                    callback.onResult(
                            new HealthConnectOnboardingState(
                                    mOnboardingStateManager.updateAndGetOnboardingState(
                                            /* bypassInstallTime= */ true)));
                },
                errorCallback);
    }

    /**
     * @see HealthConnectManager#isMatchmakingPossible(MatchmakingRequest, Executor,
     *     OutcomeReceiver)
     */
    @Override
    public void isMatchmakingPossible(
            AttributionSource attributionSource,
            MatchmakingRequest request,
            IIsMatchmakingPossibleCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);
        getMatchingDataSources(
                attributionSource,
                request,
                new IGetMatchingDataSourcesCallback.Stub() {
                    @Override
                    @PermissionManuallyEnforced
                    public void onResult(GetMatchingDataSourcesResponse response)
                            throws RemoteException {
                        callback.onResult(
                                new MatchmakingResponse.Builder(response.hasMatchingDataSources())
                                        .build());
                    }

                    @Override
                    @PermissionManuallyEnforced
                    public void onError(HealthConnectExceptionParcel exception)
                            throws RemoteException {
                        callback.onError(exception);
                    }
                });
    }

    /**
     * @see HealthConnectManager#getMatchingDataSources(Set, String, Executor, OutcomeReceiver)
     */
    @Override
    public void getMatchingDataSources(
            AttributionSource attributionSource,
            MatchmakingRequest request,
            IGetMatchingDataSourcesCallback callback) {
        // TODO(b/451988490): Test SPN masking E2E once matchmaking supports devices
        checkParamsNonNull(attributionSource, request, callback);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        String attributionPackageName = attributionSource.getPackageName();
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, GET_MATCHING_DATA_SOURCES)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionPackageName);
        ErrorCallback errorCallback = callback::onError;

        final String unmaskingPackageName;
        if (holdsDataManagementPermission) {
            unmaskingPackageName = request.getCallingPackageName();
        } else {
            unmaskingPackageName = attributionSource.getPackageName();
        }
        final MatchmakingRequest unmaskedRequest =
                request.toUnmasked(getUnmaskingFunction(unmaskingPackageName));

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!Flags.matchmaking()) {
                        throw new UnsupportedOperationException(
                                "getMatchingDataSources is not supported");
                    }
                    enforceIsForegroundUser(userHandle);
                    throwExceptionIfDataSyncInProgress();
                    String requestPackageName = request.getCallingPackageName();
                    if (holdsDataManagementPermission) {
                        checkArgument(requestPackageName != null, "package name must be provided");
                    } else {
                        checkArgument(
                                requestPackageName == null
                                        || requestPackageName.equals(attributionPackageName),
                                "invalid package name provided");
                        verifyPackageNameFromUid(uid, attributionSource);
                        boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                        tryAcquireApiCallQuota(
                                uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);
                    }
                    String packageName =
                            holdsDataManagementPermission
                                    ? requestPackageName
                                    : attributionPackageName;
                    Set<Class<? extends Record>> recordTypes = unmaskedRequest.getRecordTypes();

                    Map<String, Set<String>> matchingApps;
                    GetMatchingDataSourcesResponse maskedResponse;
                    if (Flags.deviceDataProvidersApi()) {
                        // If DDP feature is on, apply the include/exclude filters
                        Set<DataOrigin> includeDataSources =
                                unmaskedRequest.getIncludedDataSources();
                        Set<DataOrigin> excludeDataSources =
                                unmaskedRequest.getExcludedDataSources();

                        matchingApps =
                                mMatchmakingManager.fetchMatchingApps(
                                        recordTypes,
                                        packageName,
                                        includeDataSources,
                                        excludeDataSources);

                        // Also look for devices
                        Map<String, Set<String>> matchingDevices =
                                mMatchmakingManager.fetchMatchingDevices(
                                        recordTypes,
                                        packageName,
                                        includeDataSources,
                                        excludeDataSources);
                        maskedResponse =
                                new GetMatchingDataSourcesResponse(matchingApps, matchingDevices)
                                        // the masking package name is always the caller of this API
                                        .toMasked(getMaskingFunction(attributionPackageName));
                    } else {
                        matchingApps =
                                mMatchmakingManager.fetchMatchingApps(recordTypes, packageName);
                        maskedResponse =
                                new GetMatchingDataSourcesResponse(matchingApps)
                                        .toMasked(getMaskingFunction(packageName));
                    }

                    logger.setHealthDataServiceApiStatusSuccess();
                    callback.onResult(maskedResponse);
                },
                logger,
                errorCallback,
                uid,
                holdsDataManagementPermission);
    }

    /**
     * @see HealthConnectManager#recordMatchmakingDenial(String, Map, Executor, OutcomeReceiver)
     */
    @Override
    public void recordMatchmakingDenial(
            AttributionSource attributionSource,
            String callingPackageName,
            Map<String, List<String>> matchingDataSources,
            IEmptyResponseCallback callback) {
        checkParamsNonNull(attributionSource, callingPackageName, callback);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final ErrorCallback errorCallback = callback::onError;

        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    if (!Flags.matchmaking()) {
                        throw new UnsupportedOperationException(
                                "recordMatchmakingDenial is not supported");
                    }

                    Map<String, List<String>> unmaskedMatchingDataSources =
                            matchingDataSources.entrySet().stream()
                                    .collect(
                                            Collectors.toMap(
                                                    entry ->
                                                            getUnmaskingFunction(
                                                                            attributionSource
                                                                                    .getPackageName())
                                                                    .apply(entry.getKey()),
                                                    Map.Entry::getValue));

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    if (callingPackageName.isEmpty()) {
                        throw new HealthConnectException(
                                ERROR_INVALID_ARGUMENT, "Calling package name can't be empty.");
                    }
                    throwExceptionIfDataSyncInProgress();
                    mMatchmakingManager.recordMatchmakingDenial(
                            callingPackageName, unmaskedMatchingDataSources);

                    callback.onResult();
                },
                errorCallback);
    }

    /**
     * @see HealthConnectManager#setTrackingEnabled
     */
    @Override
    public void setTrackingEnabled(
            String dataTypePrefKey, boolean enabled, IEmptyResponseCallback callback) {
        checkParamsNonNull(dataTypePrefKey, callback);
        ErrorCallback errorCallback = callback::onError;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();

        scheduleControllerTaskWithExceptionHandling(
                () -> {
                    enforceIsForegroundUser(userHandle);
                    mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);
                    mPreferenceHelper.insertOrReplacePreference(
                            dataTypePrefKey, String.valueOf(enabled));
                    mTrackerManager.initializeOrRefresh();

                    if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        Objects.requireNonNull(mDeviceDataProviderManager);
                        mDeviceDataProviderManager.advertiseCurrentDeviceNativeCapabilities();
                    }

                    callback.onResult();
                },
                errorCallback);
    }

    /**
     * @see HealthConnectManager#isTrackingEnabled
     */
    @Override
    public Map<String, Boolean> isTrackingEnabled(List<String> dataTypePrefKeys) {
        checkParamsNonNull(dataTypePrefKeys);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        enforceIsForegroundUser(userHandle);
        mContext.enforcePermission(MANAGE_HEALTH_DATA_PERMISSION, pid, uid, null);

        return dataTypePrefKeys.stream()
                .collect(
                        Collectors.toMap(
                                key -> key,
                                this::getTrackingPreferenceFor,
                                (a, b) -> b,
                                ArrayMap::new));
    }

    /**
     * @see HealthConnectManager#advertiseDeviceDataSources
     */
    @Override
    public void advertiseDeviceDataSources(
            AttributionSource attributionSource,
            List<DeviceDataAdvertisement> advertisements,
            IEmptyResponseCallback callback) {
        checkParamsNonNull(attributionSource, advertisements, callback);
        ErrorCallback errorCallback = callback::onError;
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        UserHandle userHandle = Binder.getCallingUserHandle();
        String packageName = requireNonNull(attributionSource.getPackageName());
        HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                /* holdsDataManagementPermission= */ false,
                                ADVERTISE_DEVICE_DATA_SOURCES)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(packageName);
        List<DeviceDataAdvertisement> unmaskedAdvertisements =
                advertisements.stream()
                        .map(
                                advertisement ->
                                        advertisement.toUnmasked(getUnmaskingFunction(packageName)))
                        .toList();

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "advertiseDeviceDataSources is not supported");
                    }
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    if (!mDeviceDataProviderManager.isPermittedToProvideDeviceData(
                            requireNonNull(packageName), uid, pid)) {
                        throw new SecurityException(
                                "Caller is not permitted to provide device data");
                    }

                    mDeviceDataProviderManager.handleAdvertisement(
                            new HashSet<>(unmaskedAdvertisements), packageName);

                    tryAndReturnResult(callback, logger);
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    /**
     * Inserts {@code recordsParcel} from a device data type source into the HealthConnect database.
     *
     * <p>Before this method is called, {@link #advertiseDeviceDataSources} must have been called.
     *
     * @param deviceId The identifier for the device that is the source of this data. This must
     *     match the {@code deviceId} used in {@link DeviceDataAdvertisement} in the latest call to
     *     {@link #advertiseDeviceDataSources}.
     * @param recordsParcel Parcel for list of records to be inserted.
     * @param callback Callback to receive result of performing this operation. The keys returned in
     *     {@link InsertRecordsResponseParcel} are the unique IDs of the input records. The values
     *     are in same order as {@code record}. In case of an error or a permission failure in the
     *     HealthConnect service, {@link IInsertRecordsResponseCallback#onError} will be invoked
     *     with a {@link HealthConnectExceptionParcel}.
     */
    @Override
    public void insertDeviceRecords(
            AttributionSource attributionSource,
            String deviceId,
            RecordsParcel recordsParcel,
            IInsertRecordsResponseCallback callback) {
        checkParamsNonNull(attributionSource, recordsParcel, callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String packageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                /* holdsDataManagementPermission= */ false, INSERT_DEVICE_RECORDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(packageName);
        ErrorCallback errorCallback = callback::onError;
        String unmaskedDeviceId = getUnmaskingFunction(packageName).apply(deviceId);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "insertDeviceRecords is not supported");
                    }
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    enforceMemoryRateLimit(
                            recordsParcel.getRecordsSize(), recordsParcel.getRecordsChunkSize());
                    final List<RecordInternal<?>> recordInternals = recordsParcel.getRecords();
                    logger.setNumberOfRecords(recordInternals.size());
                    tryAcquireApiCallQuota(
                            uid,
                            QuotaCategory.QUOTA_CATEGORY_WRITE,
                            mAppOpsManagerLocal.isUidInForeground(uid),
                            logger,
                            recordsParcel.getRecordsChunkSize());

                    DeviceDataProviderManager deviceDataProviderManager =
                            requireNonNull(mDeviceDataProviderManager);
                    if (!deviceDataProviderManager.isPermittedToProvideDeviceData(
                            requireNonNull(packageName), uid, pid)) {
                        throw new SecurityException(
                                "Caller is not permitted to provide device data");
                    }

                    List<String> uuids =
                            deviceDataProviderManager.insertDeviceRecords(
                                    packageName, unmaskedDeviceId, recordInternals);
                    tryAndReturnResult(callback, uuids, logger);
                    // TODO(b/455514553): Add RecordType specific upsert metrics
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    /**
     * Updates {@code recordsParcel} from a device data source in the Health Connect database.
     *
     * <p>Before this method is called, {@link #advertiseDeviceDataSources} must have been called.
     *
     * <p>In case of an error or a permission failure the HealthConnect service, {@link
     * IEmptyResponseCallback#onError} will be invoked with a {@link HealthConnectException}.
     *
     * @param attributionSource attribution source for the data.
     * @param deviceId the identifier for the device that is the source of this data.
     * @param recordsParcel parcel for list of records to be updated.
     * @param callback callback to receive result of performing this operation.
     */
    @Override
    public void updateDeviceRecords(
            AttributionSource attributionSource,
            String deviceId,
            RecordsParcel recordsParcel,
            IEmptyResponseCallback callback) {
        checkParamsNonNull(attributionSource, deviceId, recordsParcel, callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                /* holdsDataManagementPermission= */ false, UPDATE_DEVICE_RECORDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionSource.getPackageName());
        ErrorCallback errorCallback = callback::onError;
        String unmaskedDeviceId = getUnmaskingFunction(callingPackageName).apply(deviceId);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "updateDeviceRecords is not supported");
                    }
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    enforceMemoryRateLimit(
                            recordsParcel.getRecordsSize(), recordsParcel.getRecordsChunkSize());
                    final List<RecordInternal<?>> recordInternals = recordsParcel.getRecords();
                    logger.setNumberOfRecords(recordInternals.size());
                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid,
                            QuotaCategory.QUOTA_CATEGORY_WRITE,
                            isInForeground,
                            logger,
                            recordsParcel.getRecordsChunkSize());

                    DeviceDataProviderManager deviceDataProviderManager =
                            requireNonNull(mDeviceDataProviderManager);
                    if (!deviceDataProviderManager.isPermittedToProvideDeviceData(
                            requireNonNull(callingPackageName), uid, pid)) {
                        throw new SecurityException(
                                "Caller is not permitted to provide or update device data");
                    }

                    deviceDataProviderManager.updateDeviceRecords(
                            callingPackageName, unmaskedDeviceId, recordInternals);

                    tryAndReturnResult(callback, logger);
                    // TODO(b/455514553): Add RecordType specific upsert metrics
                },
                logger,
                errorCallback,
                uid,
                /* isController= */ false);
    }

    /**
     * "dumpsys" infrastructure. This should get included in bug reports.
     *
     * <p>Note: To print, run "adb shell dumpsys healthconnect".
     */
    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println(
                    "Permission Denial: can't dump health connect from pid="
                            + Binder.getCallingPid()
                            + ", uid="
                            + Binder.getCallingUid()
                            + " without permission "
                            + android.Manifest.permission.DUMP);
            return;
        }

        // Storage Dump
        pw.println("Health Connect Storage Status");
        pw.printf(
                Locale.ROOT,
                "Database Version : %d, Database Size : %d kb \n\n",
                mTransactionManager.getDatabaseVersion(),
                mTransactionManager.getDatabaseSize() / 1024);

        // B&R State
        pw.println("Health Connect Backup Status");
        pw.printf(
                Locale.ROOT,
                "Data Restore State : %d, Data Restore Error : %d \n\n",
                mBackupRestore.getDataRestoreState(),
                mBackupRestore.getDataRestoreError());

        try {
            mDeviceDataProviderDebugUtil.dump(pw);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to dump DeviceDataProviderDebugUtil", e);
        }
    }

    @Override
    public boolean hasUserEnabledTracking(
            AttributionSource attributionSource, String recordTypePrefKey) {
        checkParamsNonNull(attributionSource);
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, HAS_USER_ENABLED_TRACKING)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);

        try {
            if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                throw new UnsupportedOperationException(
                        "hasUserEnabledTracking is not supported."
                                + "Make sure to turn on the respective DDP flags.");
            }
            enforceIsForegroundUser(userHandle);
            verifyPackageNameFromUid(uid, attributionSource);
            DeviceDataProviderManager deviceDataProviderManager =
                    requireNonNull(mDeviceDataProviderManager);

            if (!deviceDataProviderManager.isPermittedToProvideDeviceData(
                    callingPackageName, uid, pid)) {
                throw new SecurityException(
                        "Caller does not have permission to call hasUserEnabledTracking.");
            }

            boolean result = getTrackingPreferenceFor(recordTypePrefKey);
            logger.setHealthDataServiceApiStatusSuccess();
            return result;
        } catch (SQLiteException | UnsupportedOperationException | SecurityException e) {
            logger.setHealthDataServiceApiStatusError(getErrorCode(e));
            Slog.e(TAG, "Unable to get current preference for " + recordTypePrefKey);
            throw e;
        } catch (Exception e) {
            logger.setHealthDataServiceApiStatusError(getErrorCode(e));
            throw new RuntimeException();
        } finally {
            logger.build().log();
        }
    }

    /**
     * @see HealthConnectManager#getCurrentDeviceId
     */
    @Override
    public String getCurrentDeviceId(AttributionSource attributionSource) {
        checkParamsNonNull(attributionSource);
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());

        try {
            if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                throw new UnsupportedOperationException(
                        "getCurrentDeviceId is not supported."
                                + "Make sure to turn on the respective DDP flags.");
            }
            enforceIsForegroundUser(userHandle);
            verifyPackageNameFromUid(uid, attributionSource);
            DeviceDataProviderManager deviceDataProviderManager =
                    requireNonNull(mDeviceDataProviderManager);

            if (!deviceDataProviderManager.isPermittedToProvideDeviceData(
                    callingPackageName, uid, pid)) {
                throw new SecurityException(
                        "Caller does not have permission to call getCurrentDeviceId.");
            }

            return getMaskingFunction(callingPackageName)
                    .apply(deviceDataProviderManager.getCurrentDeviceId());
        } catch (Exception e) {
            Slog.e(TAG, "Unable to get current device id for " + userHandle);
            if (e instanceof SQLiteException
                    || e instanceof UnsupportedOperationException
                    || e instanceof SecurityException) {
                throw e;
            }
        }

        throw new RuntimeException();
    }

    /**
     * @see HealthConnectManager#readDeviceRecords
     */
    @Override
    public void readDeviceRecords(
            AttributionSource attributionSource,
            ReadRecordsRequestParcel request,
            IReadRecordsResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                /* holdsDataManagementPermission= */ false, READ_DEVICE_RECORDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);
        final ReadRecordsRequestParcel unmaskedRequest =
                request.toUnmasked(getUnmaskingFunction(callingPackageName));

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "readDeviceRecords is not supported."
                                        + "Make sure to turn on the respective DDP flags.");
                    }
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();

                    DeviceDataProviderManager deviceDataProviderManager =
                            requireNonNull(mDeviceDataProviderManager);

                    if (!deviceDataProviderManager.isPermittedToProvideDeviceData(
                            callingPackageName, uid, pid)) {
                        throw new SecurityException(
                                "Caller does not have permission to call readDeviceRecords.");
                    }

                    Pair<List<RecordInternal<?>>, PageTokenWrapper> readRecordsResponse =
                            deviceDataProviderManager.readDeviceRecords(
                                    mTransactionManager,
                                    attributionSource.getPackageName(),
                                    unmaskedRequest);
                    List<RecordInternal<?>> records = readRecordsResponse.first;
                    long pageToken = readRecordsResponse.second.encode();

                    logger.setNumberOfRecords(records.size());

                    if (Constants.DEBUG) {
                        Slog.d(TAG, "pageToken: " + pageToken);
                    }

                    final ReadRecordsResponseParcel maskedResponseParcel =
                            new ReadRecordsResponseParcel(new RecordsParcel(records), pageToken)
                                    .toMasked(getMaskingFunction(callingPackageName));
                    callback.onResult(maskedResponseParcel);

                    logger.setDataTypesFromRecordInternals(records)
                            .setHealthDataServiceApiStatusSuccess();
                },
                logger,
                callback::onError,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    /**
     * @see HealthConnectManager#deleteDeviceRecords
     */
    @Override
    public void deleteDeviceRecords(
            AttributionSource attributionSource,
            String deviceId,
            DeleteUsingFiltersRequestParcel request,
            IEmptyResponseCallback callback) {
        checkParamsNonNull(attributionSource, request, callback);

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final String callingPackageName = requireNonNull(attributionSource.getPackageName());
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                /* holdsDataManagementPermission= */ false, DELETE_DEVICE_RECORDS)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(callingPackageName);
        String unmaskedDeviceId = getUnmaskingFunction(callingPackageName).apply(deviceId);

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "deleteDeviceRecords is not supported."
                                        + "Make sure to turn on the respective DDP flags.");
                    }
                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();

                    DeviceDataProviderManager deviceDataProviderManager =
                            requireNonNull(mDeviceDataProviderManager);

                    if (!deviceDataProviderManager.isPermittedToProvideDeviceData(
                            callingPackageName, uid, pid)) {
                        throw new SecurityException(
                                "Caller does not have permission to call deleteDeviceRecords.");
                    }

                    deviceDataProviderManager.deleteDeviceRecords(
                            callingPackageName, unmaskedDeviceId, request);
                    callback.onResult();

                    logger.setHealthDataServiceApiStatusSuccess();
                },
                logger,
                callback::onError,
                uid,
                /* isController= */ holdsDataManagementPermission);
    }

    // Cancel BR timeouts - this might be needed when a user is going into background.
    void cancelBackupRestoreTimeouts() {
        mBackupRestore.cancelAllJobs();
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
                    try {
                        task.execute();
                    } catch (Exception exception) {
                        int errorCode = getErrorCode(exception);
                        if (exception instanceof IllegalArgumentException
                                && Flags.logcatCensorIae()) {
                            Slog.e(TAG, getStackTraceOnlyString(exception));
                        } else {
                            Slog.e(TAG, exception.getClass().getSimpleName() + ": ", exception);
                        }
                        if (errorCode == ERROR_UNKNOWN) {
                            Slog.e(TAG, "errorCode should not be ERROR_UNKNOWN!");
                        }
                        logger.setHealthDataServiceApiStatusError(errorCode);
                        tryAndThrowException(errorCallback, exception, errorCode);
                    } finally {
                        logger.build().log();
                    }
                },
                uid,
                isController);
    }

    private void scheduleControllerTaskWithExceptionHandling(
            Task task, ErrorCallback errorCallback) {
        mThreadScheduler.scheduleControllerTask(
                () -> {
                    try {
                        task.execute();
                    } catch (Exception e) {
                        Slog.e(TAG, e.getClass().getSimpleName() + ": ", e);
                        @HealthConnectException.ErrorCode final int errorCode = getErrorCode(e);
                        tryAndThrowException(errorCallback, e, errorCode);
                    }
                });
    }

    private static int getErrorCode(Exception exception) {
        return switch (exception) {
            case JSONException ignored -> ERROR_IO;
            case SQLiteException ignored -> ERROR_IO;
            case SecurityException ignored -> ERROR_SECURITY;
            case IllegalArgumentException ignored -> ERROR_INVALID_ARGUMENT;
            case HealthConnectException hce -> hce.getErrorCode();
            case UnsupportedOperationException ignored -> ERROR_UNSUPPORTED_OPERATION;
            default -> ERROR_INTERNAL;
        };
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
        // IPC size not calculated for local calls (e.g., from HealthConnectShellCommand)
        if (recordsSize != null) {
            recordsSize.forEach(mRateLimiter::checkMaxRecordMemoryUsage);
        }
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
        String callingPackageName = requireNonNull(callerAttributionSource.getPackageName());
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

        // On certain setups (which might be running userdebug/eng builds with adb root enabled),
        // runShellCommand drops into a root shell and executes commands as UID 0 (root) instead of
        // the standard shell user UID 2000 (shell).
        if (claimedCallingPackage.equals(SHELL_PACKAGE_NAME)
                && UserHandle.getAppId(claimedCallingUid) == Process.SHELL_UID
                && actualCallingUid == Process.ROOT_UID) {
            return;
        }

        if (claimedCallingUid != actualCallingUid) {
            throw new SecurityException(
                    "Claimed calling package "
                            + claimedCallingPackage
                            + " belongs to UID "
                            + claimedCallingUid
                            + " which does not match the actual UID of the caller "
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
        if (!SdkLevel.isAtLeastB()) {
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
            Slog.e(TAG, "Unable to send result to the callback", e);
        }
    }

    @Override
    public void getDeviceDataSourceCapabilities(
            AttributionSource attributionSource, IDeviceDataSourceCapabilitiesCallback callback) {
        checkParamsNonNull(callback);
        ErrorCallback errorCallback = callback::onError;

        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        final UserHandle userHandle = Binder.getCallingUserHandle();
        final boolean holdsDataManagementPermission = hasDataManagementPermission(uid, pid);
        final HealthConnectServiceLogger.Builder logger =
                new HealthConnectServiceLogger.Builder(
                                holdsDataManagementPermission, GET_DEVICE_DATA_SOURCE_CAPABILITIES)
                        .setHealthFitnessStatsLog(mStatsLog)
                        .setPackageName(attributionSource.getPackageName());

        scheduleLoggingHealthDataApiErrors(
                () -> {
                    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                        throw new UnsupportedOperationException(
                                "getDeviceDataSourceCapabilities is not supported");
                    }

                    enforceIsForegroundUser(userHandle);
                    verifyPackageNameFromUid(uid, attributionSource);
                    throwExceptionIfDataSyncInProgress();
                    boolean isInForeground = mAppOpsManagerLocal.isUidInForeground(uid);
                    tryAcquireApiCallQuota(
                            uid, QuotaCategory.QUOTA_CATEGORY_READ, isInForeground, logger);
                    Set<Integer> capabilities = getDeviceDataSourceCapabilities(attributionSource);
                    int[] recordTypeIds =
                            capabilities.stream().mapToInt(Integer::intValue).toArray();
                    DeviceDataSourceCapabilities result = new DeviceDataSourceCapabilities();
                    result.recordTypeIds = recordTypeIds;
                    callback.onResult(result);
                    logger.setHealthDataServiceApiStatusSuccess();
                },
                logger,
                errorCallback,
                uid,
                holdsDataManagementPermission);
    }

    private Set<Integer> getDeviceDataSourceCapabilities(AttributionSource attributionSource) {
        if (mDeviceDataSourcesHelper == null) {
            return Set.of();
        }
        // TODO(b/464473056) remove this once native tracking is represented fully in DDP schema.
        // TODO(b/468339751): Have one shared source for all native capability types
        Stream<Integer> nativeTrackingRecordTypes = Stream.of(RECORD_TYPE_STEPS);
        Set<Integer> supportedRecordTypes =
                concat(
                                mDeviceDataSourcesHelper.getAllAdvertisedRecordTypes().stream(),
                                nativeTrackingRecordTypes)
                        .collect(toSet());

        return filterSensitiveDataTypes(supportedRecordTypes, attributionSource);
    }

    private Set<Integer> filterSensitiveDataTypes(
            Set<Integer> recordTypes, AttributionSource attributionSource) {
        return recordTypes.stream()
                .filter(
                        recordType -> {
                            if (mHealthConnectMappings.getSensitivityForRecordType(
                                            recordType.intValue())
                                    == INSENSITIVE) {
                                return true;
                            }

                            Set<String> readPermissions =
                                    mHealthConnectMappings
                                            .getHealthPermissionCategoriesForRecordType(
                                                    recordType.intValue())
                                            .stream()
                                            .map(mHealthConnectMappings::getHealthReadPermission)
                                            .collect(toSet());

                            for (String permission : readPermissions) {
                                if (mDataPermissionEnforcer.isPermissionGranted(
                                        permission, attributionSource)) {
                                    return true;
                                }
                            }

                            return false;
                        })
                .collect(toSet());
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
            Slog.e(TAG, "Unable to send result to the callback", e);
        }
    }

    private static void checkParamsNonNull(Object... params) {
        for (Object param : params) {
            requireNonNull(param);
        }
    }

    private boolean getTrackingPreferenceFor(String recordTypePrefKey) {
        String enabled = mPreferenceHelper.getPreference(recordTypePrefKey);
        if (enabled == null) {
            // User has never toggled the tracking preference, default tracking to on.
            return true;
        } else {
            return Boolean.parseBoolean(enabled);
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

    private Function<String, String> getMaskingFunction(String callingPackageName) {
        return (packageName) -> {
            if (!Flags.deviceDataProvidersApi()) {
                return packageName;
            }

            return mSyntheticPackageNameResolver.mask(packageName, callingPackageName);
        };
    }

    private Function<String, String> getUnmaskingFunction(String callingPackageName) {
        return (packageName) -> {
            if (!Flags.deviceDataProvidersApi()) {
                return packageName;
            }

            return mSyntheticPackageNameResolver.unmask(packageName, callingPackageName);
        };
    }
}
