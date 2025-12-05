/*
 * Copyright (C) 2022 The Android Open Source Project
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
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_STATUS__ERROR;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_API_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_API_INVOKED__MEDICAL_RESOURCE_TYPE__MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_API_INVOKED__MEDICAL_RESOURCE_TYPE__MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.HealthConnectException.ERROR_INVALID_ARGUMENT;
import static android.health.connect.HealthConnectException.ERROR_SECURITY;
import static android.health.connect.HealthConnectException.ERROR_UNSUPPORTED_OPERATION;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STARTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.HealthPermissions.WRITE_NUTRITION;
import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.health.connect.HealthPermissions.getAllMedicalPermissions;
import static android.health.connect.backuprestore.UpdateHealthConnectBackupStatusRequest.BACKUP_STATUS_STARTED;
import static android.health.connect.backuprestore.UpdateHealthConnectRestoreStatusRequest.RESTORE_STATUS_STARTED;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.testing.shared.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.testing.shared.DataFactory.buildDevice;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_FHIR_VERSION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_UUID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DIFFERENT_DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createAllergyMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createVaccineMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getGetMedicalDataSourceRequest;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalDataSourceRequiredFieldsOnly;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalResourceId;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getUpsertMedicalResourceRequest;
import static android.healthconnect.testing.unittest.TaskUtils.waitForAllScheduledTasksToComplete;
import static android.permission.PermissionManager.PERMISSION_HARD_DENIED;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API;
import static com.android.healthfitness.flags.Flags.FLAG_IMMEDIATE_EXPORT;
import static com.android.healthfitness.flags.Flags.FLAG_MATCHMAKING;
import static com.android.healthfitness.flags.Flags.FLAG_ONBOARDING;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_STATE_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_RESTORE_STATE_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_STAGING_DONE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.CREATE_MEDICAL_DATA_SOURCE;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_DATA_SOURCE_WITH_DATA;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_CHANGES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_CHANGES_TOKEN;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MATCHING_DATA_SOURCES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.UPSERT_MEDICAL_RESOURCES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.MEDICAL_RESOURCE_TYPE_NOT_ASSIGNED_DEFAULT_VALUE;
import static com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator.SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.ActivityManager;
import android.content.AttributionSource;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.health.HealthFitnessStatsLog;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.GetMatchingDataSourcesResponse;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthConnectOnboardingState;
import android.health.connect.HealthPermissions;
import android.health.connect.MatchmakingRequest;
import android.health.connect.MatchmakingResponse;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.DeviceDataSourceCapabilities;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IApplicationInfoResponseCallback;
import android.health.connect.aidl.ICanRestoreResponseCallback;
import android.health.connect.aidl.IChangeLogsResponseCallback;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.aidl.IDeviceDataSourceCapabilitiesCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangeLogTokenCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetDeviceDataSourceInfosCallback;
import android.health.connect.aidl.IGetHealthConnectOnboardingStateCallback;
import android.health.connect.aidl.IGetLatestMetadataForBackupResponseCallback;
import android.health.connect.aidl.IGetMatchingDataSourcesCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IInsertRecordsResponseCallback;
import android.health.connect.aidl.IIsMatchmakingPossibleCallback;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMigrationCallback;
import android.health.connect.aidl.IReadMedicalResourcesResponseCallback;
import android.health.connect.aidl.IReadRecordsResponseCallback;
import android.health.connect.aidl.InsertRecordsResponseParcel;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.aidl.ReadRecordsResponseParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.aidl.UpsertMedicalResourceRequestsParcel;
import android.health.connect.backuprestore.BackupMetadata;
import android.health.connect.backuprestore.UpdateBackupAndRestoreSettingsRequest;
import android.health.connect.backuprestore.UpdateHealthConnectBackupStatusRequest;
import android.health.connect.backuprestore.UpdateHealthConnectRestoreStatusRequest;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.migration.MigrationEntityParcel;
import android.health.connect.migration.MigrationException;
import android.health.connect.ratelimiter.RateLimiter;
import android.health.connect.restore.StageRemoteDataRequest;
import android.healthconnect.testing.shared.DataFactory;
import android.healthconnect.testing.unittest.fakes.FakeTimeSource;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.ArrayMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.backuprestore.BackupRestore;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.logging.HealthConnectServiceLogger;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameResolver;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.common.preferences.PreferencesManager;
import com.android.server.healthconnect.device.FakeSerialDeviceDataProviderManager;
import com.android.server.healthconnect.device.tracker.TrackerManager;
import com.android.server.healthconnect.fitness.helpers.DeviceDataSourcesHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigrationTestUtils;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.onboarding.OnboardingStateManager;
import com.android.server.healthconnect.onboarding.matchmaking.MatchmakingManager;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/** Unit test class for {@link HealthConnectServiceImpl} */
@RunWith(AndroidJUnit4.class)
public class HealthConnectServiceImplTest {
    /**
     * Health connect service APIs that blocks calls when data sync (ex: backup and restore, data
     * migration) is in progress.
     *
     * <p><b>Before adding a method name to this list, make sure the method implementation contains
     * the blocking part (i.e: {@link HealthConnectServiceImpl#throwExceptionIfDataSyncInProgress}
     * for asynchronous APIs and {@link
     * HealthConnectServiceImpl#throwIllegalStateExceptionIfDataSyncInProgress} for synchronous
     * APIs). </b>
     *
     * <p>Also, consider adding the method to {@link
     * android.healthconnect.cts.HealthConnectManagerTest#testDataApis_migrationInProgress_apisBlocked}
     * cts test.
     */
    public static final Set<String> BLOCK_CALLS_DURING_DATA_SYNC_LIST =
            Set.of(
                    "grantHealthPermission",
                    "revokeHealthPermission",
                    "revokeAllHealthPermissions",
                    "getGrantedHealthPermissions",
                    "getHealthPermissionsFlags",
                    "setHealthPermissionsUserFixedFlagValue",
                    "getHistoricalAccessStartDateInMilliseconds",
                    "insertRecords",
                    "insertDeviceRecords",
                    "aggregateRecords",
                    "readRecords",
                    "updateRecords",
                    "updateDeviceRecords",
                    "getChangeLogToken",
                    "getChangeLogs",
                    "deleteUsingFilters",
                    "deleteUsingFiltersForSelf",
                    "getCurrentPriority",
                    "updatePriority",
                    "setRecordRetentionPeriodInDays",
                    "getRecordRetentionPeriodInDays",
                    "getContributorApplicationsInfo",
                    "queryAllRecordTypesInfo",
                    "queryAccessLogs",
                    "getActivityDates",
                    "configureScheduledExport",
                    "getScheduledExportStatus",
                    "getScheduledExportPeriodInDays",
                    "getImportStatus",
                    "runImport",
                    "createMedicalDataSource",
                    "deleteMedicalDataSourceWithData",
                    "getMedicalDataSourcesByIds",
                    "getMedicalDataSourcesByRequest",
                    "deleteMedicalResourcesByIds",
                    "deleteMedicalResourcesByRequest",
                    "upsertMedicalResourcesFromRequestsParcel",
                    "upsertMedicalResources",
                    "readMedicalResourcesByIds",
                    "readMedicalResourcesByRequest",
                    "queryAllMedicalResourceTypeInfos",
                    "runImmediateExport",
                    "getChangesForBackup",
                    "getLatestMetadataForBackup",
                    "restoreLatestMetadata",
                    "canRestore",
                    "restoreChanges",
                    "isMatchmakingPossible",
                    "getMatchingDataSources",
                    "recordMatchmakingDenial",
                    "readDeviceRecords",
                    "deleteDeviceRecords");

    /** Health connect service APIs that do not block calls when data sync is in progress. */
    public static final Set<String> DO_NOT_BLOCK_CALLS_DURING_DATA_SYNC_LIST =
            Set.of(
                    "startMigration",
                    "finishMigration",
                    "writeMigrationData",
                    "stageAllHealthConnectRemoteData",
                    "getAllDataForBackup",
                    "getAllBackupFileNames",
                    "deleteAllStagedRemoteData",
                    "setLowerRateLimitsForTesting",
                    "updateDataDownloadState",
                    "getHealthConnectDataState",
                    "getHealthConnectMigrationUiState",
                    "insertMinDataMigrationSdkExtensionVersion",
                    "asBinder",
                    "queryDocumentProviders",
                    "setTrackingEnabled",
                    "isTrackingEnabled",
                    "getCurrentDeviceId",
                    "advertiseDeviceDataSources",
                    "getDeviceDataSourceInfos",
                    "getHealthConnectOnboardingState",
                    "updateHealthConnectBackupAndRestoreSettings",
                    "updateHealthConnectRestoreStatus",
                    "updateHealthConnectBackupStatus",
                    "getDeviceDataSourceCapabilities");

    static final String ONBOARDING_STATE_PREFERENCE_KEY = "onboarding_state_";
    private static final String TEST_URI = "content://com.android.server.healthconnect/testuri";
    private static final long DEFAULT_PACKAGE_APP_INFO = 123L;

    private static final String HC_PACKAGE_NAME = "com.android.healthconnect";
    private static final String TEST_PACKAGE_NAME = "com.test.package";
    private static final String TEST_PACKAGE_NAME_2 = "com.test.package2";

    /** Package name where {@link HealthConnectServiceImplTest this test} runs in. */
    private static final String THIS_TEST_PACKAGE_NAME = "com.android.healthconnect.unittests";

    private static final int TIMEOUT_MILLIS = 10_000;
    private static final int VACCINES_INVOKED =
            HEALTH_CONNECT_PHR_API_INVOKED__MEDICAL_RESOURCE_TYPE__MEDICAL_RESOURCE_TYPE_VACCINES;
    private static final int ALLERGIES_INVOKED =
            HEALTH_CONNECT_PHR_API_INVOKED__MEDICAL_RESOURCE_TYPE__MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Mock private HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    @Mock private MigrationCleaner mMigrationCleaner;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private MigrationStateManager mMigrationStateManager;
    @Mock private MigrationUiStateManager mMigrationUiStateManager;
    @Mock private Context mServiceContext;
    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private PreferencesManager mPreferencesManager;
    @Mock private AppOpsManagerLocal mAppOpsManagerLocal;
    @Mock private RateLimiter mRateLimiter;
    @Mock private PackageManager mPackageManager;
    @Mock private PermissionManager mPermissionManager;
    @Mock private MedicalDataSourceHelper mMedicalDataSourceHelper;
    @Mock private MedicalResourceHelper mMedicalResourceHelper;
    @Mock private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private TrackerManager mTrackerManager;
    @Mock IMigrationCallback mMigrationCallback;
    @Mock IMedicalDataSourceResponseCallback mMedicalDataSourceCallback;
    @Mock IMedicalDataSourcesResponseCallback mMedicalDataSourcesResponseCallback;
    @Mock IGetHealthConnectOnboardingStateCallback mGetHealthConnectOnboardingStateCallback;
    @Mock IReadMedicalResourcesResponseCallback mReadMedicalResourcesResponseCallback;
    @Mock IEmptyResponseCallback mEmptyResponseCallback;
    @Mock IMedicalResourceListParcelResponseCallback mMedicalResourceListParcelResponseCallback;
    @Mock IGetMatchingDataSourcesCallback mGetMatchingDataSourcesCallback;
    @Mock IIsMatchmakingPossibleCallback mIsMatchmakingPossibleCallback;
    @Mock IReadRecordsResponseCallback mReadRecordsResponseCallback;
    @Mock IGetDeviceDataSourceInfosCallback mGetDeviceDataSourceInfosCallback;
    @Mock private Drawable mDrawable;
    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Mock private ChangeLogsHelper mChangeLogsHelper;
    @Mock private ChangeLogsRequestHelper mChangeLogsRequestHelper;
    @Mock private IGetChangeLogTokenCallback mGetChangeLogTokenCallback;
    @Mock private IChangeLogsResponseCallback mChangeLogsResponseCallback;
    @Mock private OnboardingStateManager mOnboardingStateManager;
    @Mock private MatchmakingManager mMatchmakingManager;
    @Mock private DeviceDataSourcesHelper mDeviceDataSourcesHelper;
    @Captor ArgumentCaptor<HealthConnectExceptionParcel> mErrorCaptor;
    @Captor ArgumentCaptor<InsertRecordsResponseParcel> mInsertResultCaptor;
    @Captor private ArgumentCaptor<HealthConnectOnboardingState> mOnboardingStateCaptor;
    private FakeTimeSource mFakeTimeSource;
    private Context mContext;
    private AttributionSource mAttributionSource;
    private HealthConnectServiceImpl mHealthConnectService;
    private UserHandle mUserHandle;
    private BackupRestore mBackupRestore;
    private ThreadPoolExecutor mInternalTaskScheduler;
    private String mTestPackageName;
    private HealthConnectThreadScheduler mThreadScheduler;
    private FakeSerialDeviceDataProviderManager mDeviceDataProviderManager;
    private SyntheticPackageNameResolver mSyntheticPackageNameResolver;
    private final Instant mNow = DataFactory.now();
    private AppInfoHelper mAppInfoHelper;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mUserHandle = mContext.getUser();

        when(mPackageManager.getPackageUid(anyString(), anyInt())).thenReturn(Process.myUid());
        when(mServiceContext.getApplicationContext()).thenReturn(mServiceContext);
        when(mServiceContext.getPackageManager()).thenReturn(mPackageManager);
        when(mServiceContext.getUser()).thenReturn(mUserHandle);
        when(mServiceContext.createContextAsUser(mUserHandle, 0)).thenReturn(mServiceContext);
        when(mServiceContext.getSystemService(ActivityManager.class))
                .thenReturn(mContext.getSystemService(ActivityManager.class));
        when(mServiceContext.getSystemService(PermissionManager.class))
                .thenReturn(mPermissionManager);
        when(mPreferenceHelper.getPreference(eq(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY)))
                .thenReturn(UUID.randomUUID().toString());
        setUpHealthPermissions();

        mFakeTimeSource = new FakeTimeSource(mNow);
        mAttributionSource = mContext.getAttributionSource();
        mTestPackageName = mAttributionSource.getPackageName();
        setUpAllMedicalPermissionChecksHardDenied();
        when(mPackageManager.getPackageInfo(eq(mAttributionSource.getPackageName()), any()))
                .thenReturn(
                        buildPackageInfo(mAttributionSource.getPackageName(), /* targetSdk= */ 34));
        when(mDrawable.getIntrinsicHeight()).thenReturn(200);
        when(mDrawable.getIntrinsicWidth()).thenReturn(200);
        when(mPackageManager.getApplicationIcon(anyString()))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mPackageManager.getDefaultActivityIcon()).thenReturn(mDrawable);

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mServiceContext)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setPreferencesManager(mPreferencesManager)
                        .setHealthDataCategoryPriorityHelper(mHealthDataCategoryPriorityHelper)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setHealthConnectPermissionHelper(mHealthConnectPermissionHelper)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setMigrationCleaner(mMigrationCleaner)
                        .setMedicalDataSourceHelper(mMedicalDataSourceHelper)
                        .setMedicalResourceHelper(mMedicalResourceHelper)
                        .setMigrationStateManager(mMigrationStateManager)
                        .setMigrationUiStateManager(mMigrationUiStateManager)
                        .setTimeSource(mFakeTimeSource)
                        .setAppOpsManagerLocal(mAppOpsManagerLocal)
                        .setHealthFitnessStatsLog(mHealthFitnessStatsLog)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .setChangeLogsHelper(mChangeLogsHelper)
                        .setChangeLogsRequestHelper(mChangeLogsRequestHelper)
                        .setOnboardingStateManager(mOnboardingStateManager)
                        .setMatchingAppsManager(mMatchmakingManager)
                        .setDeviceDataProviderManager(mDeviceDataProviderManager)
                        .setSyntheticPackageNameResolver(mSyntheticPackageNameResolver)
                        .build();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();
        mBackupRestore = healthConnectInjector.getBackupRestore();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();

        mInternalTaskScheduler = mThreadScheduler.mInternalBackgroundExecutor;

        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            mDeviceDataProviderManager =
                    spy(
                            new FakeSerialDeviceDataProviderManager(
                                    mServiceContext,
                                    healthConnectInjector.getDeviceInfoHelper(),
                                    healthConnectInjector.getAppInfoHelper(),
                                    healthConnectInjector.getDeviceDataSourcesHelper(),
                                    healthConnectInjector.getDeviceDataProviderMetadataHelper(),
                                    healthConnectInjector.getFitnessRecordUpsertHelper(),
                                    healthConnectInjector.getFitnessRecordReadHelper(),
                                    healthConnectInjector.getFitnessRecordDeleteHelper(),
                                    healthConnectInjector.getSyntheticPackageNameCreator()));

            mDeviceDataSourcesHelper = spy(healthConnectInjector.getDeviceDataSourcesHelper());
        }

        mSyntheticPackageNameResolver =
                new SyntheticPackageNameResolver(mAppInfoHelper, mDeviceDataProviderManager);

        mHealthConnectService =
                new HealthConnectServiceImpl(
                        mServiceContext,
                        healthConnectInjector.getTimeSource(),
                        healthConnectInjector.getInternalHealthConnectMappings(),
                        healthConnectInjector.getTransactionManager(),
                        healthConnectInjector.getHealthConnectPermissionHelper(),
                        healthConnectInjector.getFirstGrantTimeManager(),
                        healthConnectInjector.getMigrationEntityHelper(),
                        healthConnectInjector.getMigrationStateManager(),
                        healthConnectInjector.getMigrationUiStateManager(),
                        healthConnectInjector.getMigrationCleaner(),
                        healthConnectInjector.getOnboardingStateManager(),
                        healthConnectInjector.getFitnessRecordUpsertHelper(),
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        healthConnectInjector.getFitnessRecordDeleteHelper(),
                        healthConnectInjector.getFitnessRecordAggregateHelper(),
                        healthConnectInjector.getMedicalResourceHelper(),
                        healthConnectInjector.getMedicalDataSourceHelper(),
                        healthConnectInjector.getExportManager(),
                        healthConnectInjector.getExportImportSettingsStorage(),
                        healthConnectInjector.getExportImportNotificationSender(),
                        healthConnectInjector.getBackupRestore(),
                        healthConnectInjector.getAccessLogsHelper(),
                        healthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        healthConnectInjector.getRecordDateHelper(),
                        healthConnectInjector.getChangeLogsHelper(),
                        healthConnectInjector.getChangeLogsRequestHelper(),
                        healthConnectInjector.getPriorityMigrationHelper(),
                        healthConnectInjector.getAppInfoHelper(),
                        healthConnectInjector.getDeviceInfoHelper(),
                        healthConnectInjector.getPreferenceHelper(),
                        healthConnectInjector.getDatabaseHelpers(),
                        healthConnectInjector.getPreferencesManager(),
                        healthConnectInjector.getAppOpsManagerLocal(),
                        healthConnectInjector.getThreadScheduler(),
                        mRateLimiter,
                        healthConnectInjector.getEnvironmentDataDirectory(),
                        healthConnectInjector.getExportImportLogger(),
                        healthConnectInjector.getHealthFitnessStatsLog(),
                        healthConnectInjector.getBackupRestoreLogger(),
                        healthConnectInjector.getExportImportNotificationFactory(),
                        mTrackerManager,
                        healthConnectInjector.getCloudBackupManager(),
                        healthConnectInjector.getCloudRestoreManager(),
                        healthConnectInjector.getMatchingAppsManager(),
                        mSyntheticPackageNameResolver,
                        mDeviceDataSourcesHelper,
                        mDeviceDataProviderManager);
    }

    @After
    public void tearDown() throws TimeoutException {
        waitForAllScheduledTasksToComplete(mThreadScheduler);
        clearInvocations(mPreferenceHelper);
        clearInvocations(mPreferencesManager);
    }

    @Test
    public void testInstantiated_attachesMigrationCleanerToMigrationStateManager() {
        verify(mMigrationCleaner).attachTo(mMigrationStateManager);
    }

    @Test
    public void testStageRemoteData_withValidInput_allFilesStaged() throws Exception {
        File dataDir = mContext.getDataDir();
        File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
        File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

        assertThat(testRestoreFile1.exists()).isTrue();
        assertThat(testRestoreFile2.exists()).isTrue();

        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        pfdsByFileName.put(
                testRestoreFile1.getName(),
                ParcelFileDescriptor.open(testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
        pfdsByFileName.put(
                testRestoreFile2.getName(),
                ParcelFileDescriptor.open(testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

        final IDataStagingFinishedCallback callback = mock(IDataStagingFinishedCallback.class);
        mHealthConnectService.stageAllHealthConnectRemoteData(
                new StageRemoteDataRequest(pfdsByFileName), mUserHandle, callback);

        verify(callback, timeout(5000).times(1)).onResult();
        var stagedFileNames = mBackupRestore.getStagedRemoteFileNames(mUserHandle);
        assertThat(stagedFileNames.size()).isEqualTo(2);
        assertThat(stagedFileNames.contains(testRestoreFile1.getName())).isTrue();
        assertThat(stagedFileNames.contains(testRestoreFile2.getName())).isTrue();
    }

    @Test
    public void testStageRemoteData_withNotReadMode_onlyValidFilesStaged() throws Exception {
        File dataDir = mContext.getDataDir();
        File writeOnlyFile = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
        File readOnlyFile = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");
        try {

            assertThat(writeOnlyFile.exists()).isTrue();
            assertThat(readOnlyFile.exists()).isTrue();

            ParcelFileDescriptor writeOnlyFd =
                    ParcelFileDescriptor.open(writeOnlyFile, ParcelFileDescriptor.MODE_WRITE_ONLY);
            // The MODE_WRITE_ONLY is enough to cause an error when running on device.
            // But when running under Robolectric this does not error for
            // ShadowParcelFileDescriptor. So make the file unreadable.
            writeOnlyFile.setReadable(false);

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(writeOnlyFile.getName(), writeOnlyFd);
            pfdsByFileName.put(
                    readOnlyFile.getName(),
                    ParcelFileDescriptor.open(readOnlyFile, ParcelFileDescriptor.MODE_READ_ONLY));

            final IDataStagingFinishedCallback callback = mock(IDataStagingFinishedCallback.class);
            mHealthConnectService.stageAllHealthConnectRemoteData(
                    new StageRemoteDataRequest(pfdsByFileName), mUserHandle, callback);

            verify(callback, timeout(5000).times(1)).onError(any());
            var stagedFileNames = mBackupRestore.getStagedRemoteFileNames(mUserHandle);
            assertThat(stagedFileNames.size()).isEqualTo(1);
            assertThat(stagedFileNames.contains(readOnlyFile.getName())).isTrue();
        } finally {
            writeOnlyFile.delete();
            readOnlyFile.delete();
        }
    }

    // Imitates the state when we are not actively staging but the disk reflects that.
    // Which means we were interrupted, and therefore we should stage.
    @Test
    public void testStageRemoteData_whenStagingProgress_doesStage() throws Exception {
        File dataDir = mContext.getDataDir();
        File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
        File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

        assertThat(testRestoreFile1.exists()).isTrue();
        assertThat(testRestoreFile2.exists()).isTrue();

        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        pfdsByFileName.put(
                testRestoreFile1.getName(),
                ParcelFileDescriptor.open(testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
        pfdsByFileName.put(
                testRestoreFile2.getName(),
                ParcelFileDescriptor.open(testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

        when(mPreferenceHelper.getPreference(eq(DATA_RESTORE_STATE_KEY)))
                .thenReturn(String.valueOf(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS));

        final IDataStagingFinishedCallback callback = mock(IDataStagingFinishedCallback.class);
        mHealthConnectService.stageAllHealthConnectRemoteData(
                new StageRemoteDataRequest(pfdsByFileName), mUserHandle, callback);

        verify(callback, timeout(5000)).onResult();
        var stagedFileNames = mBackupRestore.getStagedRemoteFileNames(mUserHandle);
        assertThat(stagedFileNames.size()).isEqualTo(2);
        assertThat(stagedFileNames.contains(testRestoreFile1.getName())).isTrue();
        assertThat(stagedFileNames.contains(testRestoreFile2.getName())).isTrue();
    }

    @Test
    public void testStageRemoteData_whenStagingDone_doesNotStage() throws Exception {
        File dataDir = mContext.getDataDir();
        File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
        File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

        assertThat(testRestoreFile1.exists()).isTrue();
        assertThat(testRestoreFile2.exists()).isTrue();

        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        pfdsByFileName.put(
                testRestoreFile1.getName(),
                ParcelFileDescriptor.open(testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
        pfdsByFileName.put(
                testRestoreFile2.getName(),
                ParcelFileDescriptor.open(testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

        when(mPreferenceHelper.getPreference(eq(DATA_RESTORE_STATE_KEY)))
                .thenReturn(String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        final IDataStagingFinishedCallback callback = mock(IDataStagingFinishedCallback.class);
        mHealthConnectService.stageAllHealthConnectRemoteData(
                new StageRemoteDataRequest(pfdsByFileName), mUserHandle, callback);

        verify(callback, timeout(5000)).onResult();
        var stagedFileNames = mBackupRestore.getStagedRemoteFileNames(mUserHandle);
        assertThat(stagedFileNames.size()).isEqualTo(0);
    }

    @Test
    public void testUpdateDataDownloadState_settingValidState_setsState() {
        mHealthConnectService.updateDataDownloadState(DATA_DOWNLOAD_STARTED);
        verify(mPreferenceHelper, times(1))
                .insertOrReplacePreference(
                        eq(DATA_DOWNLOAD_STATE_KEY), eq(String.valueOf(DATA_DOWNLOAD_STARTED)));
    }

    @Test
    public void testStartMigration_noShowMigrationInfoIntentAvailable_returnsError()
            throws InterruptedException, RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        mHealthConnectService.startMigration(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE, mMigrationCallback);
        awaitAllExecutorsIdle();
        verifyNoMoreInteractions(mMigrationStateManager);
        verify(mMigrationCallback).onError(any(MigrationException.class));
    }

    @Test
    public void testStartMigration_showMigrationInfoIntentAvailable()
            throws MigrationStateManager.IllegalMigrationStateException,
                    InterruptedException,
                    RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        MigrationTestUtils.setResolveActivityResult(new ResolveInfo(), mPackageManager);
        mHealthConnectService.startMigration(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE, mMigrationCallback);
        awaitAllExecutorsIdle();
        verify(mMigrationStateManager).startMigration(mServiceContext);
    }

    @Test
    public void testFinishMigration_noShowMigrationInfoIntentAvailable_returnsError()
            throws InterruptedException, RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        mHealthConnectService.finishMigration(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE, mMigrationCallback);
        awaitAllExecutorsIdle();
        verifyNoMoreInteractions(mMigrationStateManager);
        verify(mMigrationCallback).onError(any(MigrationException.class));
    }

    @Test
    public void testFinishMigration_showMigrationInfoIntentAvailable()
            throws MigrationStateManager.IllegalMigrationStateException,
                    InterruptedException,
                    RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        MigrationTestUtils.setResolveActivityResult(new ResolveInfo(), mPackageManager);
        mHealthConnectService.finishMigration(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE, mMigrationCallback);
        awaitAllExecutorsIdle();
        verify(mMigrationStateManager).finishMigration(mServiceContext);
    }

    @Test
    public void testWriteMigration_noShowMigrationInfoIntentAvailable_returnsError()
            throws InterruptedException, RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        mHealthConnectService.writeMigrationData(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE,
                mock(MigrationEntityParcel.class),
                mMigrationCallback);
        awaitAllExecutorsIdle();
        verifyNoMoreInteractions(mMigrationStateManager);
        verify(mMigrationCallback).onError(any(MigrationException.class));
    }

    @Test
    public void testWriteMigration_showMigrationInfoIntentAvailable()
            throws MigrationStateManager.IllegalMigrationStateException,
                    InterruptedException,
                    RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        MigrationTestUtils.setResolveActivityResult(new ResolveInfo(), mPackageManager);
        mHealthConnectService.writeMigrationData(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE,
                mock(MigrationEntityParcel.class),
                mMigrationCallback);
        awaitAllExecutorsIdle();
        verify(mMigrationStateManager).validateWriteMigrationData();
        verify(mMigrationCallback).onSuccess();
    }

    @Test
    public void testInsertMinSdkExtVersion_noShowMigrationInfoIntentAvailable_returnsError()
            throws InterruptedException, RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        mHealthConnectService.insertMinDataMigrationSdkExtensionVersion(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE, 0, mMigrationCallback);
        awaitAllExecutorsIdle();
        verifyNoMoreInteractions(mMigrationStateManager);
        verify(mMigrationCallback).onError(any(MigrationException.class));
    }

    @Test
    public void testInsertMinSdkExtVersion_showMigrationInfoIntentAvailable()
            throws MigrationStateManager.IllegalMigrationStateException,
                    InterruptedException,
                    RemoteException {
        setUpPassingPermissionCheckFor(MIGRATE_HEALTH_CONNECT_DATA);
        MigrationTestUtils.setResolveActivityResult(new ResolveInfo(), mPackageManager);
        mHealthConnectService.insertMinDataMigrationSdkExtensionVersion(
                MigrationTestUtils.MOCK_CONFIGURED_PACKAGE, 0, mMigrationCallback);
        awaitAllExecutorsIdle();
        verify(mMigrationStateManager).validateSetMinSdkVersion();
        verify(mMigrationCallback).onSuccess();
    }

    @Test
    @EnableFlags({FLAG_IMMEDIATE_EXPORT})
    public void testConfigureScheduledExport_withPeriodZero_schedulesOneInternalTask()
            throws Exception {
        long taskCount = mInternalTaskScheduler.getCompletedTaskCount();
        mHealthConnectService.configureScheduledExport(
                new ScheduledExportSettings.Builder()
                        .setUri(Uri.parse(TEST_URI))
                        .setPeriodInDays(0)
                        .build(),
                mUserHandle);
        awaitAllExecutorsIdle();

        assertThat(mInternalTaskScheduler.getCompletedTaskCount()).isEqualTo(taskCount + 1);
    }

    @Test
    @EnableFlags({FLAG_IMMEDIATE_EXPORT})
    public void testConfigureScheduledExport_withPeriodGreaterThanZero_schedulesTwoInternalTask()
            throws Exception {
        long taskCount = mInternalTaskScheduler.getCompletedTaskCount();
        mHealthConnectService.configureScheduledExport(
                new ScheduledExportSettings.Builder()
                        .setUri(Uri.parse(TEST_URI))
                        .setPeriodInDays(1)
                        .build(),
                mUserHandle);
        awaitAllExecutorsIdle();

        // 2 internal tasks are scheduled: 1 for immediate export and 1 for periodic export.
        assertThat(mInternalTaskScheduler.getCompletedTaskCount()).isEqualTo(taskCount + 2);
    }

    /**
     * Tests that new HealthConnect APIs block API calls during data sync using {@link
     * HealthConnectServiceImpl.BlockCallsDuringDataSync} annotation.
     *
     * <p>If the API doesn't need to block API calls during data sync(ex: backup and restore, data
     * migration), add it to the allowedApisList list yo pass this test.
     */
    @Test
    public void testHealthConnectServiceApis_blocksCallsDuringDataSync() {
        // These APIs are not expected to block API calls during data sync.

        Method[] allMethods = IHealthConnectService.class.getMethods();
        for (Method m : allMethods) {
            assertWithMessage(
                            "Method '%s' does not belong to either"
                                    + " BLOCK_CALLS_DURING_DATA_SYNC_LIST or"
                                    + " DO_NOT_BLOCK_CALLS_DURING_DATA_SYNC_LIST. Make sure the"
                                    + " method implementation includes a section blocking calls"
                                    + " during data sync, then add the method to"
                                    + " BLOCK_CALLS_DURING_DATA_SYNC_LIST (check the Javadoc for"
                                    + " this constant for more details). If the method must allow"
                                    + " calls during data sync, add it to"
                                    + " DO_NOT_BLOCK_CALLS_DURING_DATA_SYNC_LIST.",
                            m.getName())
                    .that(
                            DO_NOT_BLOCK_CALLS_DURING_DATA_SYNC_LIST.contains(m.getName())
                                    || BLOCK_CALLS_DURING_DATA_SYNC_LIST.contains(m.getName()))
                    .isTrue();

            assertWithMessage(
                            "Method '%s' can not belong to both BLOCK_CALLS_DURING_DATA_SYNC_LIST"
                                    + " and DO_NOT_BLOCK_CALLS_DURING_DATA_SYNC_LIST.",
                            m.getName())
                    .that(
                            DO_NOT_BLOCK_CALLS_DURING_DATA_SYNC_LIST.contains(m.getName())
                                    && BLOCK_CALLS_DURING_DATA_SYNC_LIST.contains(m.getName()))
                    .isFalse();
        }
    }

    @Test
    public void testGetMedicalDataSourcesByIds_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource,
                List.of(UUID.randomUUID().toString()),
                mMedicalDataSourcesResponseCallback);

        // Wait for callback before making assertions.
        verify(mMedicalDataSourcesResponseCallback, timeout(TIMEOUT_MILLIS))
                .onResult(Collections.EMPTY_LIST);
        assertPhrApiWestWorldWrites(
                () -> eq(GET_MEDICAL_DATA_SOURCES_BY_IDS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(GET_MEDICAL_DATA_SOURCES_BY_IDS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
    }

    @Test
    public void testGetMedicalDataSources_byIds_hasDataManagementPermission_callsHelper()
            throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithoutPermissionChecks(eq(List.of(DATA_SOURCE_UUID)));
    }

    @Test
    public void testGetMedicalDataSources_byIds_noReadWritePermissions_throws() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testGetMedicalDataSources_byIds_onlyWritePermission_callsHelper()
            throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithPermissionChecks(
                        eq(List.of(DATA_SOURCE_UUID)),
                        /* GrantedReadMedicalResourceTypes= */ eq(Set.of()),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(true),
                        anyBoolean(),
                        eq(mAppInfoHelper));
    }

    @Test
    public void testGetMedicalDataSources_byIds_bothReadWritePermissions_callsHelper()
            throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithPermissionChecks(
                        eq(List.of(DATA_SOURCE_UUID)),
                        /* GrantedReadMedicalResourceTypes= */ eq(
                                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES)),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(true),
                        anyBoolean(),
                        eq(mAppInfoHelper));
    }

    @Test
    public void testGetMedicalDataSources_byIds_onlyReadPermissions_callsHelper() throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithPermissionChecks(
                        eq(List.of(DATA_SOURCE_UUID)),
                        /* GrantedReadMedicalResourceTypes= */ eq(
                                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES)),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(false),
                        anyBoolean(),
                        eq(mAppInfoHelper));
    }

    @Test
    public void testGetMedicalDataSources_byIds_fromForeground_callsHelper() throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithPermissionChecks(
                        eq(List.of(DATA_SOURCE_UUID)),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(false),
                        eq(mAppInfoHelper));
    }

    @Test
    public void testGetMedicalDataSources_byIds_fromBgNoBgReadPerm_callsHelper() throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PERMISSION_DENIED);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithPermissionChecks(
                        eq(List.of(DATA_SOURCE_UUID)),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(true),
                        eq(mAppInfoHelper));
    }

    @Test
    public void testGetMedicalDataSources_byIds_fromBgWithBgReadPerm_callsHelper()
            throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithPermissionChecks(
                        eq(List.of(DATA_SOURCE_UUID)),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(false),
                        eq(mAppInfoHelper));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    public void
            testGetMedicalDataSources_byIds_fromBgWithBgReadPermFromSplit_callsHelperWithoutBgRead()
                    throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mPackageManager.getPermissionFlags(
                        eq(READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(mAttributionSource.getPackageName()),
                        any()))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(DATA_SOURCE_ID), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByIdsWithPermissionChecks(
                        eq(List.of(DATA_SOURCE_UUID)),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(true),
                        eq(mAppInfoHelper));
    }

    @Test
    public void testGetMedicalDataSourcesByIds_maxPageSizeExceeded_throws() throws RemoteException {
        List<String> ids = new ArrayList<>(MAXIMUM_PAGE_SIZE + 1);
        for (int i = 0; i < MAXIMUM_PAGE_SIZE + 1; i++) {
            ids.add(UUID.randomUUID().toString());
        }

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, ids, mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        HealthConnectException exception = mErrorCaptor.getValue().getHealthConnectException();
        assertThat(exception.getErrorCode()).isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(exception.getMessage()).contains("The number of requested IDs must be <= 5000");
    }

    @Test
    public void testGetMedicalDataSourcesByRequests_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(Set.of("com.abc")),
                mMedicalDataSourcesResponseCallback);

        // wait for callback before asserting logs
        verify(mMedicalDataSourcesResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(any());
        assertPhrApiWestWorldWrites(
                () -> eq(GET_MEDICAL_DATA_SOURCES_BY_REQUESTS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(GET_MEDICAL_DATA_SOURCES_BY_REQUESTS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
    }

    @Test
    public void testGetMedicalDataSources_byRequest_hasDataManagementPermission_callsHelper()
            throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        Set<String> packageNames = Set.of("com.foo", "com.bar");

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithoutPermissionChecks(eq(packageNames));
    }

    @Test
    public void testGetMedicalDataSources_byRequest_noReadWritePermissions_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                new GetMedicalDataSourcesRequest.Builder().build(),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testGetMedicalDataSources_byRequest_onlyWritePermission_callsHelper()
            throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        Set<String> packageNames = Set.of("com.foo", "com.bar");

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithPermissionChecks(
                        eq(packageNames),
                        /* GrantedReadMedicalResourceTypes= */ eq(Set.of()),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(true),
                        anyBoolean());
    }

    @Test
    public void testGetMedicalDataSources_byRequest_bothReadWritePermissions_callsHelper()
            throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        Set<String> packageNames = Set.of("com.foo", "com.bar");

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithPermissionChecks(
                        eq(packageNames),
                        /* GrantedReadMedicalResourceTypes= */ eq(
                                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES)),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(true),
                        anyBoolean());
    }

    @Test
    public void testGetMedicalDataSources_byRequest_onlyReadPermissions_callsHelper()
            throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        Set<String> packageNames = Set.of("com.foo", "com.bar");

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithPermissionChecks(
                        eq(packageNames),
                        /* GrantedReadMedicalResourceTypes= */ eq(
                                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES)),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(false),
                        anyBoolean());
    }

    @Test
    public void testGetMedicalDataSources_byRequest_fromForeground_callsHelper() throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        Set<String> packageNames = Set.of("com.foo", "com.bar");

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithPermissionChecks(
                        eq(packageNames),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(false));
    }

    @Test
    public void testGetMedicalDataSources_byRequest_fromBgNoBgReadPerm_callsHelper()
            throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PERMISSION_DENIED);
        Set<String> packageNames = Set.of("com.foo", "com.bar");

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithPermissionChecks(
                        eq(packageNames),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(true));
    }

    @Test
    public void testGetMedicalDataSources_byRequest_fromBgWithBgReadPerm_callsHelper()
            throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        Set<String> packageNames = Set.of("com.foo", "com.bar");

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithPermissionChecks(
                        eq(packageNames),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(false));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    public void
            testGetMedicalDataSources_byRequest_fromBgWithBgReadPermFromSplit_callsHelperWithoutBgRead()
                    throws Exception {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        Set<String> packageNames = Set.of("com.foo", "com.bar");
        when(mPackageManager.getPermissionFlags(
                        eq(READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(mAttributionSource.getPackageName()),
                        any()))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(packageNames),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mMedicalDataSourcesResponseCallback, never()).onError(any());
        verify(mMedicalDataSourceHelper, times(1))
                .getMedicalDataSourcesByPackageWithPermissionChecks(
                        eq(packageNames),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(true));
    }

    @Test
    @DisableFlags({FLAG_PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE})
    public void testUpsertMedicalResourcesFromRequestsParcel_weakReferenceFlagOff_succeeds()
            throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);

        mHealthConnectService.upsertMedicalResourcesFromRequestsParcel(
                mAttributionSource,
                new UpsertMedicalResourceRequestsParcel(List.of(getUpsertMedicalResourceRequest())),
                mMedicalResourceListParcelResponseCallback);

        verify(mMedicalResourceListParcelResponseCallback, timeout(5000)).onResult(any());
    }

    @Test
    @EnableFlags({FLAG_PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE})
    public void testUpsertMedicalResourcesFromRequestsParcel_weakReferenceFlagOn_succeeds()
            throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);

        mHealthConnectService.upsertMedicalResourcesFromRequestsParcel(
                mAttributionSource,
                new UpsertMedicalResourceRequestsParcel(List.of(getUpsertMedicalResourceRequest())),
                mMedicalResourceListParcelResponseCallback);

        verify(mMedicalResourceListParcelResponseCallback, timeout(5000)).onResult(any());
    }

    @Test
    public void testUpsertMedicalResourcesFromRequestsParcel_expectCorrectLogs()
            throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.upsertMedicalResourcesFromRequestsParcel(
                mAttributionSource,
                new UpsertMedicalResourceRequestsParcel(
                        List.of(
                                new UpsertMedicalResourceRequest.Builder(
                                                DATA_SOURCE_ID,
                                                FHIR_VERSION_R4,
                                                FHIR_DATA_IMMUNIZATION)
                                        .build())),
                mMedicalResourceListParcelResponseCallback);

        // wait for callback before asserting logs
        verify(mMedicalResourceListParcelResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(any());
        assertPhrApiWestWorldWrites(
                () -> eq(UPSERT_MEDICAL_RESOURCES),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(UPSERT_MEDICAL_RESOURCES),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
    }

    @Test
    public void testReadMedicalResourcesByRequests_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(mNow);

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource,
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build()
                        .toParcel(),
                mReadMedicalResourcesResponseCallback);

        // wait for callback before asserting logs
        verify(mReadMedicalResourcesResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(any());
        assertPhrApiWestWorldWrites(
                () -> eq(READ_MEDICAL_RESOURCES_BY_REQUESTS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(READ_MEDICAL_RESOURCES_BY_REQUESTS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                List.of(VACCINES_INVOKED),
                1);
        verify(mPreferencesManager, times(1)).setLastPhrReadMedicalResourcesApiTimeStamp(eq(mNow));
    }

    @Test
    public void
            testReadMedicalResourcesByRequests_hasDataManagementPermission_expectMonthlyTimeStamp()
                    throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(mNow);
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource,
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build()
                        .toParcel(),
                mReadMedicalResourcesResponseCallback);

        awaitAllExecutorsIdle();
        assertThat(mPreferencesManager.getPhrLastReadMedicalResourcesApiTimeStamp()).isNull();
        verify(mPreferencesManager, times(1)).setLastPhrReadMedicalResourcesApiTimeStamp(eq(mNow));
    }

    @Test
    public void testReadMedicalResourcesByIds_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(mNow);

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource,
                List.of(getMedicalResourceId()),
                mReadMedicalResourcesResponseCallback);

        // wait for callback before asserting logs
        verify(mReadMedicalResourcesResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(any());
        assertPhrApiWestWorldWrites(
                () -> eq(READ_MEDICAL_RESOURCES_BY_IDS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(READ_MEDICAL_RESOURCES_BY_IDS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        verify(mPreferencesManager, times(1)).setLastPhrReadMedicalResourcesApiTimeStamp(eq(mNow));
    }

    @Test
    public void testReadMedicalResourcesByIds_hasDataManagementPermission_expectMonthlyTimeStamp() {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(mNow);
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource,
                List.of(getMedicalResourceId()),
                mReadMedicalResourcesResponseCallback);

        verify(mPreferencesManager, timeout(5000).times(1))
                .setLastPhrReadMedicalResourcesApiTimeStamp(eq(mNow));
    }

    @Test
    public void testReadMedicalResources_byIds_hasDataManagementPermission_callsHelper()
            throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        setUpPhrMocksWithIrrelevantResponses();
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithoutPermissionChecks(eq(ids));
    }

    @Test
    public void testReadMedicalResources_byIds_noReadWritePermissions_throws() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource,
                List.of(getMedicalResourceId()),
                mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testReadMedicalResources_byIds_onlyWritePermission_callsHelper()
            throws RemoteException {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithPermissionChecks(
                        eq(ids),
                        /* grantedReadMedicalResourceTypes= */ eq(Set.of()),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(true),
                        anyBoolean());
    }

    @Test
    public void testReadMedicalResources_byIds_numberOfIdsTooLarge_expectException()
            throws RemoteException {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        List<MedicalResourceId> ids = new ArrayList<>();
        for (int i = 0; i <= 5000; i++) {
            ids.add(getMedicalResourceId());
        }

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void testReadMedicalResources_byIds_bothReadWritePermissions_callsHelper()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithPermissionChecks(
                        eq(ids),
                        /* grantedReadMedicalResourceTypes= */ eq(
                                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES)),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(true),
                        anyBoolean());
    }

    @Test
    public void testReadMedicalResources_byIds_onlyReadPermissions_callsHelper() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setUpPhrMocksWithIrrelevantResponses();
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithPermissionChecks(
                        eq(ids),
                        /* grantedReadMedicalResourceTypes= */ eq(
                                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES)),
                        eq(mTestPackageName),
                        /* hasWritePermission= */ eq(false),
                        anyBoolean());
    }

    @Test
    public void testReadMedicalResources_byIds_fromForeground_callsHelper() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithPermissionChecks(
                        eq(ids),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(false));
    }

    @Test
    public void testReadMedicalResources_byIds_fromBgNoBgReadPerm_callsHelper() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PERMISSION_DENIED);
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithPermissionChecks(
                        eq(ids),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(true));
    }

    @Test
    public void testReadMedicalResources_byIds_fromBgWithBgReadPerm_callsHelper() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithPermissionChecks(
                        eq(ids),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(false));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    public void
            testReadMedicalResources_byIds_fromBgWithBgReadPermFromSplit_callsHelperWithoutBgRead()
                    throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());
        when(mPackageManager.getPermissionFlags(
                        eq(READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(mAttributionSource.getPackageName()),
                        any()))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource, ids, mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByIdsWithPermissionChecks(
                        eq(ids),
                        any(),
                        eq(mTestPackageName),
                        anyBoolean(),
                        /* isCalledFromBgWithoutBgRead= */ eq(true));
    }

    @Test
    public void testReadMedicalResources_byRequest_hasDataManagementPermission_callsHelper()
            throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        setUpPhrMocksWithIrrelevantResponses();

        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource, request.toParcel(), mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByRequestWithoutPermissionChecks(
                        eq(PhrPageTokenWrapper.from(request.toParcel())),
                        eq(request.getPageSize()));
    }

    @Test
    public void testReadMedicalResources_byRequest_noReadWritePermissions_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource,
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build()
                        .toParcel(),
                mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testReadMedicalResources_byRequest_onlyWritePermission_selfReads()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource, request.toParcel(), mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByRequestWithPermissionChecks(
                        eq(PhrPageTokenWrapper.from(request.toParcel())),
                        eq(request.getPageSize()),
                        eq(mTestPackageName),
                        /* enforceSelfRead= */ eq(true));
    }

    @Test
    public void testReadMedicalResources_byRequest_bothReadWritePermissions_selfReads()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        setUpPhrMocksWithIrrelevantResponses();
        setBackgroundReadPermission(PERMISSION_DENIED);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource, request.toParcel(), mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByRequestWithPermissionChecks(
                        eq(PhrPageTokenWrapper.from(request.toParcel())),
                        eq(request.getPageSize()),
                        eq(mTestPackageName),
                        /* enforceSelfRead= */ eq(true));
    }

    @Test
    public void testReadMedicalResources_byRequest_onlyReadPermission_foreground_noSelfReads()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource, request.toParcel(), mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByRequestWithPermissionChecks(
                        eq(PhrPageTokenWrapper.from(request.toParcel())),
                        eq(request.getPageSize()),
                        eq(mTestPackageName),
                        /* enforceSelfRead= */ eq(false));
    }

    @Test
    public void testReadMedicalResources_byRequest_onlyReadPermission_bgNoReadPerm_selfReads()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PERMISSION_DENIED);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource, request.toParcel(), mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByRequestWithPermissionChecks(
                        eq(PhrPageTokenWrapper.from(request.toParcel())),
                        eq(request.getPageSize()),
                        eq(mTestPackageName),
                        /* enforceSelfRead= */ eq(true));
    }

    @Test
    public void testReadMedicalResources_byRequest_onlyReadPermission_withBgRead_noSelfReads()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource, request.toParcel(), mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByRequestWithPermissionChecks(
                        eq(PhrPageTokenWrapper.from(request.toParcel())),
                        eq(request.getPageSize()),
                        eq(mTestPackageName),
                        /* enforceSelfRead= */ eq(false));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    public void
            testReadMedicalResources_byRequest_onlyReadPermission_withBgReadFromSplitPermission_enforceSelfRead()
                    throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(READ_MEDICAL_DATA_VACCINES);
        setUpPhrMocksWithIrrelevantResponses();
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(false);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        when(mPackageManager.getPermissionFlags(
                        eq(READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(mAttributionSource.getPackageName()),
                        any()))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource, request.toParcel(), mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
        verify(mReadMedicalResourcesResponseCallback, never()).onError(any());
        verify(mMedicalResourceHelper, times(1))
                .readMedicalResourcesByRequestWithPermissionChecks(
                        eq(PhrPageTokenWrapper.from(request.toParcel())),
                        eq(request.getPageSize()),
                        eq(mTestPackageName),
                        /* enforceSelfRead= */ eq(true));
    }

    @Test
    public void testUpsertMedicalResourcesFromRequestsParcel_hasDataManagementPermission_throws()
            throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        mHealthConnectService.upsertMedicalResourcesFromRequestsParcel(
                mAttributionSource,
                new UpsertMedicalResourceRequestsParcel(
                        List.of(
                                new UpsertMedicalResourceRequest.Builder(
                                                DATA_SOURCE_ID,
                                                FHIR_VERSION_R4,
                                                FHIR_DATA_IMMUNIZATION)
                                        .build())),
                mMedicalResourceListParcelResponseCallback);

        verify(mMedicalResourceListParcelResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testUpsertMedicalResourcesFromRequestsParcel_noWriteMedicalDataPermission_throws()
            throws Exception {
        mHealthConnectService.upsertMedicalResourcesFromRequestsParcel(
                mAttributionSource,
                new UpsertMedicalResourceRequestsParcel(
                        List.of(
                                new UpsertMedicalResourceRequest.Builder(
                                                DATA_SOURCE_ID,
                                                FHIR_VERSION_R4,
                                                FHIR_DATA_IMMUNIZATION)
                                        .build())),
                mMedicalResourceListParcelResponseCallback);

        verify(mMedicalResourceListParcelResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testCreateMedicalDataSource_hasDataManagementPermission_throws()
            throws RemoteException {
        setUpCreateMedicalDataSourceDefaultMocks();
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        mHealthConnectService.createMedicalDataSource(
                mAttributionSource,
                getCreateMedicalDataSourceRequest(),
                mMedicalDataSourceCallback);

        verify(mMedicalDataSourceCallback, timeout(5000)).onError(mErrorCaptor.capture());
        HealthConnectException exception = mErrorCaptor.getValue().getHealthConnectException();
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testCreateMedicalDataSource_transactionManagerSqlLiteException_throws()
            throws RemoteException {
        setUpCreateMedicalDataSourceDefaultMocks();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        when(mMedicalDataSourceHelper.createMedicalDataSource(any(), any()))
                .thenThrow(SQLiteException.class);

        mHealthConnectService.createMedicalDataSource(
                mAttributionSource,
                getCreateMedicalDataSourceRequest(),
                mMedicalDataSourceCallback);

        verify(mMedicalDataSourceCallback, timeout(5000)).onError(mErrorCaptor.capture());
        HealthConnectException exception = mErrorCaptor.getValue().getHealthConnectException();
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_IO);
    }

    @Test
    public void testCreateMedicalDataSource_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.createMedicalDataSource(
                mAttributionSource,
                getCreateMedicalDataSourceRequest(),
                mMedicalDataSourceCallback);

        // wait for callback before asserting logs
        verify(mMedicalDataSourceCallback, timeout(TIMEOUT_MILLIS)).onResult(any());
        assertPhrApiWestWorldWrites(
                () -> eq(CREATE_MEDICAL_DATA_SOURCE),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(CREATE_MEDICAL_DATA_SOURCE),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
    }

    @Test
    public void testCreateMedicalDataSource_noWriteMedicalDataPermission_throws()
            throws RemoteException {
        setUpCreateMedicalDataSourceDefaultMocks();

        mHealthConnectService.createMedicalDataSource(
                mAttributionSource,
                getCreateMedicalDataSourceRequest(),
                mMedicalDataSourceCallback);

        verify(mMedicalDataSourceCallback, timeout(5000)).onError(mErrorCaptor.capture());
        HealthConnectException exception = mErrorCaptor.getValue().getHealthConnectException();
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testDeleteMedicalDataSourceWithData_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.deleteMedicalDataSourceWithData(
                mAttributionSource, UUID.randomUUID().toString(), mEmptyResponseCallback);

        // Wait for the callback before making assertions
        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();
        assertPhrApiWestWorldWrites(
                () -> eq(DELETE_MEDICAL_DATA_SOURCE_WITH_DATA),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(DELETE_MEDICAL_DATA_SOURCE_WITH_DATA),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
    }

    @Test
    public void testDeleteMedicalDataSourceWithData_badId_fails() throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        doThrow(new IllegalArgumentException())
                .when(mMedicalDataSourceHelper)
                .deleteMedicalDataSourceWithoutPermissionChecks(any());

        mHealthConnectService.deleteMedicalDataSourceWithData(mAttributionSource, "foo", callback);

        verify(callback, timeout(5000)).onError(mErrorCaptor.capture());
        HealthConnectException exception = mErrorCaptor.getValue().getHealthConnectException();
        assertThat(exception.getErrorCode()).isEqualTo(ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void testDeleteMedicalDataSourceWithData_noPermission_fails() throws RemoteException {
        setDataManagementPermission(PERMISSION_DENIED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        UUID id = UUID.randomUUID();
        MedicalDataSource datasource =
                new MedicalDataSource.Builder(
                                id.toString(),
                                DATA_SOURCE_PACKAGE_NAME,
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .build();
        when(mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(id)))
                .thenReturn(List.of(datasource));

        mHealthConnectService.deleteMedicalDataSourceWithData(
                mAttributionSource, id.toString(), callback);

        verify(callback, timeout(5000)).onError(mErrorCaptor.capture());
        HealthConnectException exception = mErrorCaptor.getValue().getHealthConnectException();
        assertThat(exception.getErrorCode()).isEqualTo(ERROR_SECURITY);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testDeleteMedicalDataSourceWithData_wrongPackage_fails() throws RemoteException {
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        UUID id = UUID.randomUUID();
        MedicalDataSource datasource =
                new MedicalDataSource.Builder(
                                id.toString(),
                                DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .build();
        when(mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(id)))
                .thenReturn(List.of(datasource));
        doThrow(new IllegalArgumentException())
                .when(mMedicalDataSourceHelper)
                .deleteMedicalDataSourceWithPermissionChecks(any(), any());

        mHealthConnectService.deleteMedicalDataSourceWithData(
                mAttributionSource, id.toString(), callback);

        verify(callback, timeout(5000)).onError(mErrorCaptor.capture());
        verifyNoMoreInteractions(callback);
        HealthConnectException exception = mErrorCaptor.getValue().getHealthConnectException();
        assertThat(exception.getErrorCode()).isEqualTo(ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void testDeleteMedicalDataSourceWithData_existingId_succeeds() throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        UUID id = UUID.randomUUID();
        MedicalDataSource datasource =
                new MedicalDataSource.Builder(
                                id.toString(),
                                DATA_SOURCE_PACKAGE_NAME,
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .build();
        when(mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(id)))
                .thenReturn(List.of(datasource));

        mHealthConnectService.deleteMedicalDataSourceWithData(
                mAttributionSource, id.toString(), callback);

        verify(callback, timeout(5000)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testDeleteMedicalResources_noIds_returns() throws RemoteException {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);

        mHealthConnectService.deleteMedicalResourcesByIds(mAttributionSource, List.of(), callback);

        verify(callback, timeout(5000).times(1)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testDeleteMedicalResourcesByIds_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.deleteMedicalResourcesByIds(
                mAttributionSource, List.of(getMedicalResourceId()), mEmptyResponseCallback);

        // wait for callback before asserting logs
        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();
        assertPhrApiWestWorldWrites(
                () -> eq(DELETE_MEDICAL_RESOURCES_BY_IDS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(DELETE_MEDICAL_RESOURCES_BY_IDS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
    }

    @Test
    public void testDeleteMedicalResources_someIds_success() throws RemoteException {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);

        mHealthConnectService.deleteMedicalResourcesByIds(
                mAttributionSource,
                List.of(
                        new MedicalResourceId(
                                DATA_SOURCE_ID,
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION)),
                callback);

        verify(callback, timeout(5000).times(1)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testDeleteMedicalResources_noWriteMedicalDataPermission_throws() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);

        mHealthConnectService.deleteMedicalResourcesByIds(
                mAttributionSource,
                List.of(
                        new MedicalResourceId(
                                DATA_SOURCE_ID,
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION)),
                callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testDeleteMedicalResources_dataManagementPermissionNothingThere_success()
            throws Exception {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);

        mHealthConnectService.deleteMedicalResourcesByIds(
                mAttributionSource,
                List.of(
                        new MedicalResourceId(
                                DATA_SOURCE_ID,
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION)),
                callback);

        verify(callback, timeout(5000).times(1)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testDeleteMedicalResourcesByRequests_expectCorrectLogs() throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mHealthConnectService.deleteMedicalResourcesByRequest(
                mAttributionSource, request, mEmptyResponseCallback);

        // wait for callback before asserting logs
        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();
        assertPhrApiWestWorldWrites(
                () -> eq(DELETE_MEDICAL_RESOURCES_BY_REQUESTS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(DELETE_MEDICAL_RESOURCES_BY_REQUESTS),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                1);
    }

    @Test
    public void testDeleteMedicalResourcesByRequest_noPermission_securityError()
            throws RemoteException {
        setDataManagementPermission(PERMISSION_DENIED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);

        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mHealthConnectService.deleteMedicalResourcesByRequest(
                mAttributionSource, request, callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
    }

    @Test
    public void testDeleteMedicalResourcesByRequest_nonExistentRequest_success()
            throws RemoteException {
        AppInfoHelper appInfoHelper = spy(mAppInfoHelper);
        when(appInfoHelper.getAppInfoId(any())).thenReturn(DEFAULT_PACKAGE_APP_INFO);
        when(mServiceContext.checkPermission(eq(MANAGE_HEALTH_DATA_PERMISSION), anyInt(), anyInt()))
                .thenReturn(PERMISSION_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_MEDICAL_DATA, mAttributionSource, null))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mHealthConnectService.deleteMedicalResourcesByRequest(
                mAttributionSource, request, callback);

        verify(callback, timeout(5000).times(1)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testDeleteMedicalResourcesByRequest_nonExistentRequestHasManagement_success()
            throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mHealthConnectService.deleteMedicalResourcesByRequest(
                mAttributionSource, request, callback);

        verify(callback, timeout(5000).times(1)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testDeleteMedicalResourcesByRequest_requestWithFhirType_success()
            throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mHealthConnectService.deleteMedicalResourcesByRequest(
                mAttributionSource, request, callback);

        verify(callback, timeout(5000).times(1)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testGetAllContributorAppInfoIds_noDataManagementPermission_throws()
            throws Exception {
        doThrow(SecurityException.class)
                .when(mServiceContext)
                .enforcePermission(eq(MANAGE_HEALTH_DATA_PERMISSION), anyInt(), anyInt(), isNull());
        IApplicationInfoResponseCallback callback = mock(IApplicationInfoResponseCallback.class);

        mHealthConnectService.getContributorApplicationsInfo(
                mContext.getAttributionSource(), callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testUserSwitching() throws TimeoutException {
        mHealthConnectService.setupForUser(mUserHandle);

        waitForAllScheduledTasksToComplete(mThreadScheduler);
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateHealthConnectBackupAndRestoreStatus_noPermissions_throwsException() {

        setBackupPermission(PERMISSION_DENIED);
        setBackupHCDataAndSettingsPermission(PERMISSION_DENIED);

        assertThrows(
                SecurityException.class,
                () ->
                        mHealthConnectService.updateHealthConnectBackupStatus(
                                new UpdateHealthConnectBackupStatusRequest.Builder(
                                                BACKUP_STATUS_STARTED, Instant.now().toEpochMilli())
                                        .setStatusMessage("test")
                                        .setStatusTitle("test")
                                        .build()));
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateHealthConnectBackupStatus_with_Backup_Permission_succeeds() {

        setBackupPermission(PERMISSION_GRANTED);
        setBackupHCDataAndSettingsPermission(PERMISSION_DENIED);

        // No security exception is thrown, because the caller has the BACKUP permission.
        mHealthConnectService.updateHealthConnectBackupStatus(
                new UpdateHealthConnectBackupStatusRequest.Builder(
                                BACKUP_STATUS_STARTED, Instant.now().toEpochMilli())
                        .setStatusMessage("test")
                        .setStatusTitle("test")
                        .build());
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateHealthConnectBackupStatus_with_HCBackup_Permission_succeeds() {
        setBackupPermission(PERMISSION_GRANTED);
        setBackupHCDataAndSettingsPermission(PERMISSION_DENIED);

        // No security exception, caller has the BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS permission.
        mHealthConnectService.updateHealthConnectBackupStatus(
                new UpdateHealthConnectBackupStatusRequest.Builder(
                                BACKUP_STATUS_STARTED, Instant.now().toEpochMilli())
                        .setStatusMessage("test")
                        .setStatusTitle("test")
                        .build());
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateHealthConnectBackupSettings_noPermissions_throwsSecurityException() {

        setBackupPermission(PERMISSION_DENIED);
        setBackupHCDataAndSettingsPermission(PERMISSION_DENIED);

        assertThrows(
                SecurityException.class,
                () ->
                        mHealthConnectService.updateHealthConnectBackupAndRestoreSettings(
                                new UpdateBackupAndRestoreSettingsRequest.Builder()
                                        .setBackupSettingsLabel("test")
                                        .setTurnOnBackupsInvitationText("test")
                                        .setAccountName("test")
                                        .setBackupsEnabledState(
                                                UpdateBackupAndRestoreSettingsRequest
                                                        .BACKUPS_ENABLED_TRUE)
                                        .build()));
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateBackupAndRestoreSettings_withBackupPermissions_succeeds() {

        setBackupPermission(PERMISSION_GRANTED);
        setBackupHCDataAndSettingsPermission(PERMISSION_DENIED);

        // No security exception is thrown, because the caller has the BACKUP permission.
        mHealthConnectService.updateHealthConnectBackupAndRestoreSettings(
                new UpdateBackupAndRestoreSettingsRequest.Builder()
                        .setBackupSettingsLabel("test")
                        .setTurnOnBackupsInvitationText("test")
                        .setAccountName("test")
                        .setBackupsEnabledState(
                                UpdateBackupAndRestoreSettingsRequest.BACKUPS_ENABLED_TRUE)
                        .build());
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateBackupAndRestoreSettings_withHCBackupPermissions_succeeds() {

        setBackupPermission(PERMISSION_DENIED);
        setBackupHCDataAndSettingsPermission(PERMISSION_GRANTED);

        // No security exception, caller has the BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS permission.
        mHealthConnectService.updateHealthConnectBackupAndRestoreSettings(
                new UpdateBackupAndRestoreSettingsRequest.Builder()
                        .setBackupSettingsLabel("test")
                        .setTurnOnBackupsInvitationText("test")
                        .setAccountName("test")
                        .setBackupsEnabledState(
                                UpdateBackupAndRestoreSettingsRequest.BACKUPS_ENABLED_TRUE)
                        .build());
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateHealthConnectRestoreStatus_noPermissions_throwsException() {

        setBackupPermission(PERMISSION_DENIED);
        setRestoreHCDataAndSettingsPermission(PERMISSION_DENIED);

        assertThrows(
                SecurityException.class,
                () ->
                        mHealthConnectService.updateHealthConnectRestoreStatus(
                                new UpdateHealthConnectRestoreStatusRequest.Builder(
                                                RESTORE_STATUS_STARTED,
                                                Instant.now().toEpochMilli())
                                        .setStatusMessage("test")
                                        .setStatusTitle("test")
                                        .build()));
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateHealthConnectRestoreStatus_with_Backup_Permission_succeeds() {

        setBackupPermission(PERMISSION_GRANTED);
        setRestoreHCDataAndSettingsPermission(PERMISSION_DENIED);

        // No security exception is thrown, because the caller has the BACKUP permission.
        mHealthConnectService.updateHealthConnectRestoreStatus(
                new UpdateHealthConnectRestoreStatusRequest.Builder(
                                RESTORE_STATUS_STARTED, Instant.now().toEpochMilli())
                        .setStatusMessage("test")
                        .setStatusTitle("test")
                        .build());
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testUpdateHealthConnectRestoreStatus_with_HCBackup_Permission_succeeds() {
        setBackupPermission(PERMISSION_GRANTED);
        setRestoreHCDataAndSettingsPermission(PERMISSION_DENIED);

        // No security exception, caller has the BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS permission.
        mHealthConnectService.updateHealthConnectRestoreStatus(
                new UpdateHealthConnectRestoreStatusRequest.Builder(
                                RESTORE_STATUS_STARTED, Instant.now().toEpochMilli())
                        .setStatusMessage("test")
                        .setStatusTitle("test")
                        .build());
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void getChangesForBackup_flagDisabled_unsupportedOperation() throws RemoteException {
        IGetChangesForBackupResponseCallback callback =
                mock(IGetChangesForBackupResponseCallback.class);
        mHealthConnectService.getChangesForBackup(null, callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void getLatestMetadataForBackup_flagDisabled_unsupportedOperation()
            throws RemoteException {
        IGetLatestMetadataForBackupResponseCallback callback =
                mock(IGetLatestMetadataForBackupResponseCallback.class);
        mHealthConnectService.getLatestMetadataForBackup(callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void canRestore_flagDisabled_unsupportedOperation() throws RemoteException {
        ICanRestoreResponseCallback callback = mock(ICanRestoreResponseCallback.class);
        mHealthConnectService.canRestore(0, callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void restoreLatestMetadata_flagDisabled_unsupportedOperation() throws RemoteException {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        Settings settings =
                Settings.newBuilder()
                        .setWeightUnitSetting(Settings.WeightUnitProto.KILOGRAM)
                        .setDistanceUnitSetting(Settings.DistanceUnitProto.MILES)
                        .build();

        mHealthConnectService.restoreLatestMetadata(
                new BackupMetadata(settings.toByteArray()), callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void restoreChanges_flagDisabled_unsupportedOperation() throws RemoteException {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        mHealthConnectService.restoreChanges(List.of(), callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void restore_flagDisabled_unsupportedOperation() throws RemoteException {}

    /**
     * Sets up the mocks so all checks are bypassed and all PHR API calls are successful. Although,
     * notably data management permission is denied so the logs are logged in {@link
     * HealthConnectServiceLogger}.
     */
    private void setUpSuccessfulMocksForPhrTelemetry() {
        setDataManagementPermission(PERMISSION_DENIED);
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        setUpPhrMocksWithIrrelevantResponses();
    }

    @Test
    @EnableFlags({FLAG_ONBOARDING})
    public void testGetOnboardingStatus_noAppConnected_returnsTheUpdatedOnboardingStatus()
            throws Exception {
        int onboardingState = ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;
        when(mOnboardingStateManager.updateAndGetOnboardingState(/* bypassInstallTime= */ true))
                .thenReturn(onboardingState);

        mHealthConnectService.getHealthConnectOnboardingState(
                mGetHealthConnectOnboardingStateCallback);

        verifyOnboardingState(onboardingState);
    }

    @Test
    @EnableFlags(FLAG_ONBOARDING)
    public void testGetOnboardingStatus_oneAppConnected_returnsTheUpdatedOnboardingStatus()
            throws Exception {
        int onboardingState = ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
        when(mOnboardingStateManager.updateAndGetOnboardingState(/* bypassInstallTime= */ true))
                .thenReturn(onboardingState);

        mHealthConnectService.getHealthConnectOnboardingState(
                mGetHealthConnectOnboardingStateCallback);

        verifyOnboardingState(onboardingState);
    }

    @Test
    @DisableFlags(FLAG_ONBOARDING)
    public void testGetOnboardingStatus_flagDisabled_unsupportedOperation() throws Exception {
        mHealthConnectService.getHealthConnectOnboardingState(
                mGetHealthConnectOnboardingStateCallback);

        verify(mGetHealthConnectOnboardingStateCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    public void testGetChangeLogToken_noPermissions_throwsSecurityException() throws Exception {
        // Deny necessary permissions
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        ChangeLogTokenRequest request =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(HeartRateRecord.class)
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName(mTestPackageName).build())
                        .build();

        mHealthConnectService.getChangeLogToken(
                mAttributionSource, request, mGetChangeLogTokenCallback);

        verify(mGetChangeLogTokenCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
        verify(mChangeLogsRequestHelper, never()).getToken(anyLong(), anyString(), any());
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
    })
    public void testGetChangeLogToken_noPermissions_throwsSecurityException_phr() throws Exception {
        // Deny necessary permissions
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        ChangeLogTokenRequest request =
                new ChangeLogTokenRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName(mTestPackageName).build())
                        .build();

        mHealthConnectService.getChangeLogToken(
                mAttributionSource, request, mGetChangeLogTokenCallback);

        verify(mGetChangeLogTokenCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
        verify(mChangeLogsRequestHelper, never()).getToken(anyLong(), anyString(), any());
    }

    @Test
    public void testGetChangeLogToken_validRequest_returnsToken() throws Exception {
        // Grant necessary permissions
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        long latestRowId = 12345L;
        ChangeLogTokenRequest request =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(HeartRateRecord.class)
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName(mTestPackageName).build())
                        .build();
        when(mChangeLogsHelper.getLatestRowId()).thenReturn(latestRowId);
        String expectedToken = "test-token-123";
        when(mChangeLogsRequestHelper.getToken(latestRowId, mTestPackageName, request))
                .thenReturn(expectedToken);

        mHealthConnectService.getChangeLogToken(
                mAttributionSource, request, mGetChangeLogTokenCallback);

        ArgumentCaptor<ChangeLogTokenResponse> responseCaptor =
                ArgumentCaptor.forClass(ChangeLogTokenResponse.class);
        verify(mGetChangeLogTokenCallback, timeout(TIMEOUT_MILLIS))
                .onResult(responseCaptor.capture());
        ChangeLogTokenResponse actualResponse = responseCaptor.getValue();
        assertThat(actualResponse.getToken()).isEqualTo(expectedToken);

        verify(mGetChangeLogTokenCallback, never()).onError(any());
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        eq(GET_CHANGES_TOKEN),
                        eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                        anyInt(),
                        anyLong(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        eq(mTestPackageName));
    }

    @Test
    public void testGetChangeLogs_noPermissions_throwsSecurityException() throws Exception {
        // Deny necessary permissions
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        String token = "test-token-123";
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(token).build();
        // Mock getRequest to return a valid TokenRequest to proceed further before permission check
        ChangeLogsRequestHelper.TokenRequest tokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of(mTestPackageName),
                        List.of(RECORD_TYPE_HEART_RATE),
                        List.of(), // No medical types
                        mTestPackageName,
                        100L); // Example row ID
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, token)).thenReturn(tokenRequest);

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
    })
    public void testGetChangeLogs_noPermissions_throwsSecurityException_phr() throws Exception {
        // Deny necessary permissions
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PERMISSION_HARD_DENIED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        String token = "test-token-123";
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(token).build();
        // Mock getRequest to return a valid TokenRequest to proceed further before permission check
        ChangeLogsRequestHelper.TokenRequest tokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of(mTestPackageName),
                        List.of(),
                        List.of(MEDICAL_RESOURCE_TYPE_VACCINES), // No medical types
                        mTestPackageName,
                        100L); // Example row ID
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, token)).thenReturn(tokenRequest);

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
    }

    @Test
    public void testGetChangeLogs_invalidToken_throwsIllegalArgumentException() throws Exception {
        // Grant permissions to pass initial checks
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt()))
                .thenReturn(true); // Simulate foreground call
        String invalidToken = "invalid-token";
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(invalidToken).build();
        // Mock getRequest to throw IllegalArgumentException for the invalid token
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, invalidToken))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("Invalid token");
        verify(mChangeLogsHelper, never()).getChangeLogs(any(), any(), any(), any());
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
    })
    public void testGetChangeLogs_emptyToken_phrFlagOn_throwsIllegalArgumentException()
            throws Exception {
        // Grant permissions to pass initial checks
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt()))
                .thenReturn(true); // Simulate foreground call
        String emptyToken = "empty-token";
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(emptyToken).build();
        // Token request with no data types defined.
        ChangeLogsRequestHelper.TokenRequest tokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of(),
                        List.of(), // No record types
                        List.of(), // No medical types
                        mTestPackageName,
                        100L);
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, emptyToken))
                .thenReturn(tokenRequest);

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("At least one Record type or Medical Resource type must be set");
        verify(mChangeLogsHelper, never()).getChangeLogs(any(), any(), any(), any());
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
    })
    public void testGetChangeLogs_bothTypesToken_phrFlagOn_throwsIllegalArgumentException()
            throws Exception {
        // Grant permissions to pass initial checks
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt()))
                .thenReturn(true); // Simulate foreground call
        String emptyToken = "empty-token";
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(emptyToken).build();
        // Token request with no data types defined.
        ChangeLogsRequestHelper.TokenRequest tokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of(),
                        List.of(RECORD_TYPE_HEART_RATE, RECORD_TYPE_STEPS),
                        List.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        mTestPackageName,
                        100L);
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, emptyToken))
                .thenReturn(tokenRequest);

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("Record types and Medical Resource types can't both be set");
        verify(mChangeLogsHelper, never()).getChangeLogs(any(), any(), any(), any());
    }

    @Test
    @DisableFlags({
        FLAG_PHR_CHANGE_LOGS,
    })
    public void testGetChangeLogs_emptyToken_phrFlagOff_throwsIllegalArgumentException()
            throws Exception {
        // Grant permissions to pass initial checks
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt()))
                .thenReturn(true); // Simulate foreground call
        String emptyToken = "empty-token";
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(emptyToken).build();
        // Token request with no data types defined.
        ChangeLogsRequestHelper.TokenRequest tokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of(),
                        List.of(), // No record types
                        List.of(), // No medical types
                        mTestPackageName,
                        100L);
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, emptyToken))
                .thenReturn(tokenRequest);

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("Requested record types must not be empty.");
        verify(mChangeLogsHelper, never()).getChangeLogs(any(), any(), any(), any());
    }

    @Test
    public void testGetChangeLogs_validRequest_returnsChangeLogs() throws Exception {
        // Grant necessary permissions
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt()))
                .thenReturn(true); // Simulate foreground call
        when(mHealthConnectPermissionHelper.getHealthDataStartDateAccessOrThrow(anyString(), any()))
                .thenReturn(Instant.EPOCH); // Grant history access implicitly
        String token = "test-token-valid";
        String nextToken = "test-token-next";
        long initialRowId = 100L;
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsRequestHelper.TokenRequest tokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of(), // No package filter
                        List.of(RECORD_TYPE_HEART_RATE, RECORD_TYPE_STEPS),
                        List.of(), // No medical types
                        mTestPackageName,
                        initialRowId);
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, token)).thenReturn(tokenRequest);
        ChangeLogsHelper.ChangeLogsResponse mockChangeLogsResponse =
                mock(ChangeLogsHelper.ChangeLogsResponse.class);
        when(mockChangeLogsResponse.getRecordTypeToUpsertedUuids()).thenReturn(Map.of());
        when(mockChangeLogsResponse.getDeletedLogs()).thenReturn(List.of());
        when(mockChangeLogsResponse.getUpsertedMedicalResourceIds()).thenReturn(List.of());
        when(mockChangeLogsResponse.getDeletedMedicalResources()).thenReturn(List.of());
        when(mockChangeLogsResponse.getNextPageToken()).thenReturn(nextToken);
        when(mockChangeLogsResponse.hasMorePages()).thenReturn(false);
        when(mChangeLogsHelper.getChangeLogs(
                        eq(mAppInfoHelper),
                        eq(tokenRequest),
                        eq(request),
                        eq(mChangeLogsRequestHelper)))
                .thenReturn(mockChangeLogsResponse);

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        ChangeLogsResponse expectedResponse =
                new ChangeLogsResponse(
                        List.of(), List.of(), List.of(), List.of(), nextToken, false);
        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(expectedResponse);
        verify(mChangeLogsResponseCallback, never()).onError(any());
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        eq(GET_CHANGES),
                        eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                        anyInt(),
                        anyLong(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        eq(mTestPackageName));
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
    })
    public void testGetChangeLogs_validRequest_returnsChangeLogs_phr() throws Exception {
        // Grant necessary permissions
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        setBackgroundReadPermission(PackageManager.PERMISSION_GRANTED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt()))
                .thenReturn(true); // Simulate foreground call
        when(mHealthConnectPermissionHelper.getHealthDataStartDateAccessOrThrow(anyString(), any()))
                .thenReturn(Instant.EPOCH); // Grant history access implicitly
        String token = "test-token-valid";
        String nextToken = "test-token-next";
        long initialRowId = 100L;
        List<MedicalResource> resources =
                List.of(
                        createAllergyMedicalResource(DATA_SOURCE_ID),
                        createVaccineMedicalResource(DATA_SOURCE_ID));
        List<MedicalResourceId> resourceIds =
                resources.stream().map(MedicalResource::getId).toList();
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(token).build();
        ChangeLogsRequestHelper.TokenRequest tokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of(), // No package filter
                        List.of(), // No record types
                        List.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        mTestPackageName,
                        initialRowId);
        when(mChangeLogsRequestHelper.getRequest(mTestPackageName, token)).thenReturn(tokenRequest);
        ChangeLogsHelper.ChangeLogsResponse mockChangeLogsResponse =
                mock(ChangeLogsHelper.ChangeLogsResponse.class);
        when(mockChangeLogsResponse.getRecordTypeToUpsertedUuids()).thenReturn(Map.of());
        when(mockChangeLogsResponse.getDeletedLogs()).thenReturn(List.of());
        when(mockChangeLogsResponse.getUpsertedMedicalResourceIds()).thenReturn(resourceIds);
        when(mockChangeLogsResponse.getDeletedMedicalResources()).thenReturn(List.of());
        when(mockChangeLogsResponse.getNextPageToken()).thenReturn(nextToken);
        when(mockChangeLogsResponse.hasMorePages()).thenReturn(false);
        when(mChangeLogsHelper.getChangeLogs(
                        eq(mAppInfoHelper),
                        eq(tokenRequest),
                        eq(request),
                        eq(mChangeLogsRequestHelper)))
                .thenReturn(mockChangeLogsResponse);
        when(mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        eq(resourceIds)))
                .thenReturn(resources);

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        ChangeLogsResponse expectedResponse =
                new ChangeLogsResponse(
                        List.of(), List.of(), resources, List.of(), nextToken, false);
        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(expectedResponse);
        verify(mChangeLogsResponseCallback, never()).onError(any());
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        eq(GET_CHANGES),
                        eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                        anyInt(),
                        anyLong(),
                        eq(2), // number of changes
                        anyInt(),
                        anyInt(),
                        eq(mTestPackageName));
        assertPhrApiPrivateWestWorldWrites(
                () -> eq(GET_CHANGES),
                () -> eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                List.of(VACCINES_INVOKED, ALLERGIES_INVOKED),
                1);
    }

    @Test
    public void testGetChangeLogs_backgroundReadDenied_throwsSecurityException() throws Exception {
        // Grant basic read permissions but deny background read
        when(mPermissionManager.checkPermissionForPreflight(any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(any(), any(), any()))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        setBackgroundReadPermission(PERMISSION_DENIED);
        doThrow(SecurityException.class)
                .when(mServiceContext)
                .enforcePermission(
                        eq(READ_HEALTH_DATA_IN_BACKGROUND), anyInt(), anyInt(), anyString());
        when(mAppOpsManagerLocal.isUidInForeground(anyInt()))
                .thenReturn(false); // Simulate background call
        String token = "test-token-bg-denied";
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(token).build();

        mHealthConnectService.getChangeLogs(
                mAttributionSource, request, mChangeLogsResponseCallback);

        verify(mChangeLogsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
        verify(mChangeLogsHelper, never()).getChangeLogs(any(), any(), any(), any());
    }

    @Test
    @DisableFlags(FLAG_MATCHMAKING)
    public void getMatchingDataSources_flagOff_exception() throws Exception {
        MatchmakingRequest request = new MatchmakingRequest.Builder().build();

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingDataSources_noDMPermission_emptySetRequest_emptyMapReturned_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(5000).times(1))
                .onResult(new GetMatchingDataSourcesResponse(Map.of()));
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingDataSources_noDMPermission_nonEmptySetRequest_emptyMapReturned_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(5000).times(1))
                .onResult(new GetMatchingDataSourcesResponse(Map.of()));
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingDataSources_noDMPermission_emptySetRequest_nonEmptyMapReturned_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(5000).times(1))
                .onResult(new GetMatchingDataSourcesResponse(matchingApps));
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void
            getMatchingDataSources_noDMPermission_nonEmptySetRequest_nonEmptyMapReturned_success()
                    throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(5000).times(1))
                .onResult(new GetMatchingDataSourcesResponse(matchingApps));
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingDataSources_noDMPermission_invalidPackageNameInRequest_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName("invalid.package.name")
                        .addRecordTypes(recordTypes)
                        .build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("invalid package name provided");
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        eq(GET_MATCHING_DATA_SOURCES),
                        eq(HEALTH_CONNECT_API_CALLED__API_STATUS__ERROR),
                        anyInt(),
                        anyLong(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        eq(mTestPackageName));
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingDataSources_noDMPermission_packageNameSameAsCalling_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .addRecordTypes(recordTypes)
                        .setCallingPackageName(mTestPackageName)
                        .build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(5000).times(1))
                .onResult(new GetMatchingDataSourcesResponse(matchingApps));
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingDataSources_hasDMPermission_noPackageNameInRequest_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_GRANTED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, THIS_TEST_PACKAGE_NAME))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("package name must be provided");
    }

    @EnableFlags({FLAG_MATCHMAKING})
    @Test
    public void getMatchingDataSources_hasDMPermission_packageProvided_recordsProvided_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_GRANTED);

        Set<Class<? extends Record>> recordTypes = Set.of();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(THIS_TEST_PACKAGE_NAME)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, THIS_TEST_PACKAGE_NAME))
                .thenReturn(Map.of());

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        verify(mGetMatchingDataSourcesCallback, timeout(5000).times(1))
                .onResult(new GetMatchingDataSourcesResponse(Map.of()));
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void getMatchingDataSources_packageProvided_noRecordsProvided_success()
            throws Exception {
        Set<Class<? extends Record>> recordTypes = Set.of();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(mTestPackageName)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        ArgumentCaptor<GetMatchingDataSourcesResponse> responseCaptor =
                ArgumentCaptor.forClass(GetMatchingDataSourcesResponse.class);
        verify(mGetMatchingDataSourcesCallback, timeout(TIMEOUT_MILLIS))
                .onResult(responseCaptor.capture());
        GetMatchingDataSourcesResponse actualResponse = responseCaptor.getValue();
        assertThat(actualResponse.getMatchingApps()).isEqualTo(Map.of());

        verify(mGetMatchingDataSourcesCallback, never()).onError(any());
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void getMatchingDataSources_packageProvided_areAvailableApps_success() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(mTestPackageName)
                        .addRecordTypes(recordTypes)
                        .build();
        Map<String, Set<String>> matchingApps =
                Map.of(
                        "package.name.a",
                        Set.of(WRITE_STEPS, WRITE_NUTRITION),
                        "package.name.b",
                        Set.of(WRITE_SLEEP));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingDataSources(
                mAttributionSource, request, mGetMatchingDataSourcesCallback);

        ArgumentCaptor<GetMatchingDataSourcesResponse> responseCaptor =
                ArgumentCaptor.forClass(GetMatchingDataSourcesResponse.class);
        verify(mGetMatchingDataSourcesCallback, timeout(TIMEOUT_MILLIS))
                .onResult(responseCaptor.capture());
        GetMatchingDataSourcesResponse actualResponse = responseCaptor.getValue();
        assertThat(actualResponse.getMatchingApps()).isEqualTo(matchingApps);
        verifyNoMoreInteractions(mGetMatchingDataSourcesCallback);
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        eq(GET_MATCHING_DATA_SOURCES),
                        eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                        anyInt(),
                        anyLong(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        eq(mTestPackageName));
    }

    @Test
    @DisableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_flagOff_exception() throws Exception {
        MatchmakingRequest request = new MatchmakingRequest.Builder().build();

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_emptySetRequest_emptyMapReturned_false()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1))
                .onResult(new MatchmakingResponse.Builder(false).build());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_nonEmptySetRequest_emptyMapReturned_false()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1))
                .onResult(new MatchmakingResponse.Builder(false).build());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_emptySetRequest_nonEmptyMapReturned_true()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1))
                .onResult(new MatchmakingResponse.Builder(true).build());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_nonEmptySetRequest_nonEmptyMapReturned_true()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1))
                .onResult(new MatchmakingResponse.Builder(true).build());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_invalidPackageNameInRequest_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName("invalid.package.name")
                        .addRecordTypes(recordTypes)
                        .build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("invalid package name provided");
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        eq(GET_MATCHING_DATA_SOURCES),
                        eq(HEALTH_CONNECT_API_CALLED__API_STATUS__ERROR),
                        anyInt(),
                        anyLong(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        eq(mTestPackageName));
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_packageNameSameAsCalling_true()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .addRecordTypes(recordTypes)
                        .setCallingPackageName(mTestPackageName)
                        .build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1))
                .onResult(new MatchmakingResponse.Builder(true).build());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        eq(GET_MATCHING_DATA_SOURCES),
                        eq(HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS),
                        anyInt(),
                        anyLong(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        eq(mTestPackageName));
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_hasDMPermission_noPackageNameInRequest_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_GRANTED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, THIS_TEST_PACKAGE_NAME))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("package name must be provided");
    }

    @EnableFlags({FLAG_MATCHMAKING})
    @Test
    public void isMatchmakingPossible_hasDMPermission_packageProvided_recordsProvided_false()
            throws Exception {
        setDataManagementPermission(PERMISSION_GRANTED);

        Set<Class<? extends Record>> recordTypes = Set.of();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(THIS_TEST_PACKAGE_NAME)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, THIS_TEST_PACKAGE_NAME))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1))
                .onResult(new MatchmakingResponse.Builder(false).build());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void isMatchmakingPossible_packageProvided_noRecordsProvided_false() throws Exception {
        Set<Class<? extends Record>> recordTypes = Set.of();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(mTestPackageName)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(TIMEOUT_MILLIS))
                .onResult(new MatchmakingResponse.Builder(false).build());
        verify(mIsMatchmakingPossibleCallback, never()).onError(any());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void isMatchmakingPossible_packageProvided_areAvailableApps_true() throws Exception {
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(mTestPackageName)
                        .addRecordTypes(recordTypes)
                        .build();
        Map<String, Set<String>> matchingApps =
                Map.of(
                        "package.name.a",
                        Set.of(WRITE_STEPS, WRITE_NUTRITION),
                        "package.name.b",
                        Set.of(WRITE_SLEEP));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(TIMEOUT_MILLIS))
                .onResult(new MatchmakingResponse.Builder(true).build());
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void recordMatchmakingDenial_noPermission_throws() throws Exception {
        doThrow(SecurityException.class)
                .when(mServiceContext)
                .enforcePermission(eq(MANAGE_HEALTH_DATA_PERMISSION), anyInt(), anyInt(), any());

        mHealthConnectService.recordMatchmakingDenial(
                mAttributionSource,
                TEST_PACKAGE_NAME,
                Map.of(TEST_PACKAGE_NAME_2, List.of(WRITE_STEPS)),
                mEmptyResponseCallback);
        awaitAllExecutorsIdle();

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void recordMatchmakingDenial_withPermission_callsManager() throws RemoteException {
        setDataManagementPermission(PERMISSION_GRANTED);

        mHealthConnectService.recordMatchmakingDenial(
                mAttributionSource,
                TEST_PACKAGE_NAME,
                Map.of(TEST_PACKAGE_NAME_2, List.of(WRITE_STEPS)),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();
    }

    @Test
    @EnableFlags({
        FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void recordMatchmakingDenial_withMaskedNames_unmasks() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        Device device =
                new Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        advertiseStepsDeviceDataSource("device_id", device);

        mHealthConnectService.getDeviceDataSourceInfos(
                mAttributionSource, mGetDeviceDataSourceInfosCallback);

        ArgumentCaptor<List<DeviceDataSourceInfo>> deviceSourceCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mGetDeviceDataSourceInfosCallback, timeout(TIMEOUT_MILLIS))
                .onResult(deviceSourceCaptor.capture());

        String spn = deviceSourceCaptor.getValue().get(0).getDeviceDataOrigin().getPackageName();

        ArgumentCaptor<Map<String, List<String>>> deniedAppsCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> callingPackageCaptor = ArgumentCaptor.forClass(String.class);

        mHealthConnectService.recordMatchmakingDenial(
                mAttributionSource,
                mAttributionSource.getPackageName(),
                Map.of(spn, List.of(WRITE_STEPS)),
                mEmptyResponseCallback);

        verify(mMatchmakingManager, timeout(TIMEOUT_MILLIS))
                .recordMatchmakingDenial(
                        callingPackageCaptor.capture(), deniedAppsCaptor.capture());

        // Calling package is not changed - needs to be equal to the caller used during
        // advertisement, as unmasking the SPN will fail otherwise
        assertThat(callingPackageCaptor.getValue()).isEqualTo(mAttributionSource.getPackageName());
        // Inserted spn app is unmasked
        String deniedAppName =
                deniedAppsCaptor.getValue().entrySet().stream().iterator().next().getKey();
        assertThat(deniedAppName).isNotEqualTo(spn);
        assertThat(SyntheticPackageNameCreator.isCanonicalSpn(deniedAppName)).isTrue();
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void recordMatchmakingDenial_noMatchingApps_callsManager() throws RemoteException {
        setDataManagementPermission(PERMISSION_GRANTED);

        mHealthConnectService.recordMatchmakingDenial(
                mAttributionSource, TEST_PACKAGE_NAME, Map.of(), mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void recordMatchmakingDenial_emptyRecordedPermissionList_callsManager()
            throws RemoteException {
        setDataManagementPermission(PERMISSION_GRANTED);

        mHealthConnectService.recordMatchmakingDenial(
                mAttributionSource,
                TEST_PACKAGE_NAME,
                Map.of(TEST_PACKAGE_NAME_2, List.of()),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();
    }

    @Test
    public void setTrackingEnabled_noPermissions_throwsSecurityException() throws Exception {
        doThrow(SecurityException.class)
                .when(mServiceContext)
                .enforcePermission(eq(MANAGE_HEALTH_DATA_PERMISSION), anyInt(), anyInt(), any());

        mHealthConnectService.setTrackingEnabled("TRACKING_PREF_1", true, mEmptyResponseCallback);
        awaitAllExecutorsIdle();

        verify(mEmptyResponseCallback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
    }

    @Test
    public void setTrackingEnabled_sqliteException_throwsIOException() throws Exception {
        doThrow(new SQLiteException())
                .when(mPreferenceHelper)
                .insertOrReplacePreference(anyString(), anyString());

        mHealthConnectService.setTrackingEnabled("TRACKING_PREF_1", true, mEmptyResponseCallback);
        awaitAllExecutorsIdle();

        verify(mEmptyResponseCallback).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_IO);
    }

    @Test
    public void setTrackingEnabled_true_setsPreference() throws Exception {
        mHealthConnectService.setTrackingEnabled("TRACKING_PREF_1", true, mEmptyResponseCallback);
        awaitAllExecutorsIdle();

        verify(mPreferenceHelper).insertOrReplacePreference("TRACKING_PREF_1", "true");
        verify(mEmptyResponseCallback).onResult();
    }

    @Test
    public void setTrackingEnabled_false_setsPreference() throws Exception {
        mHealthConnectService.setTrackingEnabled("TRACKING_PREF_1", false, mEmptyResponseCallback);
        awaitAllExecutorsIdle();

        verify(mPreferenceHelper).insertOrReplacePreference("TRACKING_PREF_1", "false");
        verify(mEmptyResponseCallback).onResult();
    }

    @Test
    public void setTrackingEnabled_refreshesTrackerManager() throws Exception {
        mHealthConnectService.setTrackingEnabled("TRACKING_PREF_1", true, mEmptyResponseCallback);
        awaitAllExecutorsIdle();

        verify(mTrackerManager).initializeOrRefresh();
    }

    @Test
    public void isTrackingEnabled_noPermissions_throwsSecurityException() {
        doThrow(SecurityException.class)
                .when(mServiceContext)
                .enforcePermission(eq(MANAGE_HEALTH_DATA_PERMISSION), anyInt(), anyInt(), any());

        assertThrows(
                SecurityException.class,
                () -> mHealthConnectService.isTrackingEnabled(List.of("TRACKING_PREF_1")));
    }

    @Test
    public void isTrackingEnabled_returnsPreferences() {
        when(mPreferenceHelper.getPreference("TRACKING_PREF_1")).thenReturn("true");
        when(mPreferenceHelper.getPreference("TRACKING_PREF_2")).thenReturn("false");

        Map<String, Boolean> result =
                mHealthConnectService.isTrackingEnabled(
                        List.of("TRACKING_PREF_1", "TRACKING_PREF_2"));

        assertThat(result.get("TRACKING_PREF_1")).isTrue();
        assertThat(result.get("TRACKING_PREF_2")).isFalse();
    }

    @Test
    public void isTrackingEnabled_noPreferenceSet_defaultsToTrue() {
        when(mPreferenceHelper.getPreference("TRACKING_PREF_1")).thenReturn(null);

        Map<String, Boolean> result =
                mHealthConnectService.isTrackingEnabled(List.of("TRACKING_PREF_1"));

        assertThat(result.get("TRACKING_PREF_1")).isTrue();
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deviceDataProviderManagerIsNull_advertiseDeviceDataSources_throwsException()
            throws RemoteException {
        mHealthConnectService.advertiseDeviceDataSources(
                mAttributionSource, Collections.emptyList(), mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void advertiseDeviceDataSources_doesNotThrow() throws RemoteException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, "test_device_id", deviceDataTypeAdvertisements);

        mHealthConnectService.advertiseDeviceDataSources(
                mAttributionSource, List.of(advertisement), mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(5000).times(1)).onResult();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void advertiseDeviceDataSources_withCurrentDeviceId_unmasks() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        String clientExposedId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        ArgumentCaptor<Set<DeviceDataAdvertisement>> advertisementArgumentCaptor =
                ArgumentCaptor.forClass(Set.class);

        advertiseStepsDeviceDataSource(clientExposedId, buildDevice());

        String internalDeviceId = mDeviceDataProviderManager.getStableCurrentDeviceId();
        verify(mDeviceDataProviderManager)
                .handleAdvertisement(advertisementArgumentCaptor.capture(), any());

        List<DeviceDataAdvertisement> calledAdvertisements =
                advertisementArgumentCaptor.getValue().stream().toList();

        assertThat(calledAdvertisements.size()).isEqualTo(1);
        assertThat(calledAdvertisements.get(0).getDeviceId()).isEqualTo(internalDeviceId);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void insertDeviceRecords_withoutAdvertisement_throws() throws RemoteException {
        Instant now = mFakeTimeSource.getInstantNow();
        String recordId = UUID.randomUUID().toString();
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        StepsRecord stepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(recordId).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                100)
                        .build();
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        when(mPreferenceHelper.getPreference(eq(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY)))
                .thenReturn(UUID.randomUUID().toString());
        IInsertRecordsResponseCallback.Stub callback =
                mock(IInsertRecordsResponseCallback.Stub.class);

        mHealthConnectService.insertDeviceRecords(
                mAttributionSource, deviceId, recordsParcel, callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .isEqualTo(
                        "java.lang.IllegalArgumentException: appInfoId not found for calling"
                                + " package com.android.healthconnect.unittests, ensure an"
                                + " advertisement has been made");
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void ddpApisDisabled_insertDeviceRecords_throwsException() throws RemoteException {
        Instant now = mFakeTimeSource.getInstantNow();
        String recordId = UUID.randomUUID().toString();
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        StepsRecord stepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(recordId).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                100)
                        .build();
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        IInsertRecordsResponseCallback.Stub callback =
                mock(IInsertRecordsResponseCallback.Stub.class);
        when(mPreferenceHelper.getPreference(eq(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY)))
                .thenReturn(UUID.randomUUID().toString());

        // Normally, advertisement should be made first, however that API would also throw
        // unsupported operation exception, so we skip that and just verify this API correctly
        // throws.
        mHealthConnectService.insertDeviceRecords(
                mAttributionSource, deviceId, recordsParcel, callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void insertDeviceRecords_afterAdvertisement_doesNotThrow() throws RemoteException {
        Instant now = mFakeTimeSource.getInstantNow();
        String recordId = UUID.randomUUID().toString();
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        StepsRecord stepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(recordId).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                100)
                        .build();
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        IInsertRecordsResponseCallback.Stub callback =
                mock(IInsertRecordsResponseCallback.Stub.class);
        advertiseStepsDeviceDataSource(deviceId, device);

        mHealthConnectService.insertDeviceRecords(
                mAttributionSource, deviceId, recordsParcel, callback);

        verify(callback, timeout(TIMEOUT_MILLIS)).onResult(mInsertResultCaptor.capture());
        assertThat(mInsertResultCaptor.getValue().getUids()).hasSize(1);
        assertThat(mInsertResultCaptor.getValue().getUids().get(0)).isNotNull();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void updateDeviceRecords_afterAdvertisementAndInsert_doesNotThrow() throws Exception {
        Instant now = mFakeTimeSource.getInstantNow();
        String recordId = UUID.randomUUID().toString();
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        StepsRecord stepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(recordId).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                100)
                        .build();
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        IInsertRecordsResponseCallback.Stub callback =
                mock(IInsertRecordsResponseCallback.Stub.class);
        when(mPreferenceHelper.getPreference(eq(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY)))
                .thenReturn(UUID.randomUUID().toString());
        advertiseStepsDeviceDataSource(deviceId, device);
        mHealthConnectService.insertDeviceRecords(
                mAttributionSource, deviceId, recordsParcel, callback);
        verify(callback, timeout(TIMEOUT_MILLIS)).onResult(mInsertResultCaptor.capture());
        String uuid = mInsertResultCaptor.getValue().getUids().get(0);

        StepsRecord updatedStepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(uuid).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                200)
                        .build();
        RecordsParcel updateRecordsParcel = getRestoredStepsRecordsParcel(updatedStepsRecord);
        IEmptyResponseCallback.Stub updateCallback = mock(IEmptyResponseCallback.Stub.class);
        mHealthConnectService.updateDeviceRecords(
                mAttributionSource, deviceId, updateRecordsParcel, updateCallback);

        verify(updateCallback, timeout(TIMEOUT_MILLIS)).onResult();
        verify(updateCallback, never()).onError(any());
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void updateDeviceRecords_wrongUuidInUpdatedRecord_throws() throws Exception {
        Instant now = mFakeTimeSource.getInstantNow();
        String recordId = UUID.randomUUID().toString();
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        StepsRecord stepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(recordId).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                100)
                        .build();
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        IInsertRecordsResponseCallback.Stub callback =
                mock(IInsertRecordsResponseCallback.Stub.class);
        when(mPreferenceHelper.getPreference(eq(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY)))
                .thenReturn(UUID.randomUUID().toString());
        advertiseStepsDeviceDataSource(deviceId, device);
        mHealthConnectService.insertDeviceRecords(
                mAttributionSource, deviceId, recordsParcel, callback);
        verify(callback, timeout(TIMEOUT_MILLIS)).onResult(any());

        String wrongRecordId = UUID.randomUUID().toString();
        StepsRecord updatedStepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder()
                                        .setId(wrongRecordId)
                                        .setDevice(device)
                                        .build(),
                                now,
                                now.plusSeconds(1),
                                200)
                        .build();
        RecordsParcel updateRecordsParcel = getRestoredStepsRecordsParcel(updatedStepsRecord);
        IEmptyResponseCallback.Stub updateCallback = mock(IEmptyResponseCallback.Stub.class);
        mHealthConnectService.updateDeviceRecords(
                mAttributionSource, deviceId, updateRecordsParcel, updateCallback);

        verify(updateCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .startsWith(
                        "java.lang.IllegalArgumentException: No record found for the following"
                                + " input : uuid : ");
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void updateDeviceRecords_withoutInsert_throws() throws RemoteException {
        Instant now = mFakeTimeSource.getInstantNow();
        String recordId = UUID.randomUUID().toString();
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        StepsRecord stepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(recordId).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                100)
                        .build();
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        when(mPreferenceHelper.getPreference(eq(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY)))
                .thenReturn(UUID.randomUUID().toString());
        advertiseStepsDeviceDataSource(deviceId, device);

        IEmptyResponseCallback.Stub updateCallback = mock(IEmptyResponseCallback.Stub.class);
        mHealthConnectService.updateDeviceRecords(
                mAttributionSource, deviceId, recordsParcel, updateCallback);

        verify(updateCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .startsWith(
                        "java.lang.IllegalArgumentException: No record found for the following"
                                + " input : uuid : ");
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deviceDataProviderManagerIsNull_updateDeviceRecords_throwsException()
            throws RemoteException {
        Instant now = mFakeTimeSource.getInstantNow();
        String recordId = UUID.randomUUID().toString();
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        StepsRecord stepsRecord =
                new StepsRecord.Builder(
                                new Metadata.Builder().setId(recordId).setDevice(device).build(),
                                now,
                                now.plusSeconds(1),
                                100)
                        .build();
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        IEmptyResponseCallback.Stub callback = mock(IEmptyResponseCallback.Stub.class);
        when(mPreferenceHelper.getPreference(eq(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY)))
                .thenReturn(UUID.randomUUID().toString());

        mHealthConnectService.updateDeviceRecords(
                mAttributionSource, deviceId, recordsParcel, callback);

        verify(callback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void updateDeviceRecords_withCurrentDeviceId_unmasks() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        Device device = buildDevice();
        String clientExposedId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        advertiseStepsDeviceDataSource(clientExposedId, device);

        String recordId = UUID.randomUUID().toString();
        StepsRecord stepsRecord =
                getStepsRecord(
                        100, new Metadata.Builder().setId(recordId).setDevice(device).build());
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        IEmptyResponseCallback.Stub callback = mock(IEmptyResponseCallback.Stub.class);

        mHealthConnectService.insertDeviceRecords(
                mAttributionSource,
                clientExposedId,
                recordsParcel,
                mock(IInsertRecordsResponseCallback.Stub.class));
        mHealthConnectService.updateDeviceRecords(
                mAttributionSource, clientExposedId, recordsParcel, callback);
        verify(callback, timeout(TIMEOUT_MILLIS)).onResult();

        String internalDeviceId = mDeviceDataProviderManager.getStableCurrentDeviceId();
        verify(mDeviceDataProviderManager).updateDeviceRecords(any(), eq(internalDeviceId), any());
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void insertDeviceRecords_withCurrentDeviceId_unmasks() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        String recordId = UUID.randomUUID().toString();
        Device device = buildDevice();
        StepsRecord stepsRecord =
                getStepsRecord(
                        100, new Metadata.Builder().setId(recordId).setDevice(device).build());
        RecordsParcel recordsParcel = getRestoredStepsRecordsParcel(stepsRecord);
        IInsertRecordsResponseCallback.Stub callback =
                mock(IInsertRecordsResponseCallback.Stub.class);
        String clientExposedId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);

        advertiseStepsDeviceDataSource(clientExposedId, device);
        mHealthConnectService.insertDeviceRecords(
                mAttributionSource, clientExposedId, recordsParcel, callback);
        verify(callback, timeout(5000).times(1)).onResult(any());

        String internalDeviceId = mDeviceDataProviderManager.getStableCurrentDeviceId();
        verify(mDeviceDataProviderManager).insertDeviceRecords(any(), eq(internalDeviceId), any());
    }

    @Test
    public void testDump_doesNotCrash() throws Exception {
        mHealthConnectService.dump(
                new FileDescriptor(),
                new PrintWriter(
                        new OutputStream() {
                            @Override
                            public void write(int i) throws IOException {}
                        }),
                null);
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getCurrentDeviceIdWithDisabledFlags_NullException_throwsUnsupportedError() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> mHealthConnectService.getCurrentDeviceId(mAttributionSource));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getCurrentDeviceIdWithoutInit_NullException_throwsRuntimeError() {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        assertThrows(
                RuntimeException.class,
                () -> mHealthConnectService.getCurrentDeviceId(mAttributionSource));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getCurrentDeviceIdWithoutPermission_SecurityException_throwsSecurityError() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PackageManager.PERMISSION_DENIED);

        assertThrows(
                RuntimeException.class,
                () -> mHealthConnectService.getCurrentDeviceId(mAttributionSource));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getCurrentDeviceId_returnsMaskedSpn() throws Exception {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        String currentDeviceId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        awaitAllExecutorsIdle();

        assertTrue(SyntheticPackageNameCreator.isMaskedSpn(currentDeviceId));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getCurrentDeviceId_multipleCalls_returnsSameSpn() throws Exception {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        String firstId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        awaitAllExecutorsIdle();
        String secondId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        awaitAllExecutorsIdle();

        assertEquals(firstId, secondId);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getCurrentDeviceId_returnValue_resolvesToStableIdWhenUnmasked() throws Exception {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        String maskedRuntimeId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        awaitAllExecutorsIdle();

        String actualUnmaskedStableId =
                mSyntheticPackageNameResolver.unmask(
                        maskedRuntimeId, mAttributionSource.getPackageName());

        assertEquals(mDeviceDataProviderManager.getStableCurrentDeviceId(), actualUnmaskedStableId);
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void readDeviceRecords_disabledDdpFlags_exception() throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        mHealthConnectService.readDeviceRecords(
                mAttributionSource,
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId("some id")
                        .build()
                        .toReadRecordsRequestParcel(),
                mReadRecordsResponseCallback);

        verify(mReadRecordsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void readDeviceRecords_noDdpPermission_exception() throws RemoteException {
        setDeviceDataProviderPermission(PackageManager.PERMISSION_DENIED);

        mHealthConnectService.readDeviceRecords(
                mAttributionSource,
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId("some id")
                        .build()
                        .toReadRecordsRequestParcel(),
                mReadRecordsResponseCallback);

        verify(mReadRecordsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void readDeviceRecords_noAdvertising_exception() throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        mHealthConnectService.readDeviceRecords(
                mAttributionSource,
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId("some unknown id")
                        .build()
                        .toReadRecordsRequestParcel(),
                mReadRecordsResponseCallback);

        verify(mReadRecordsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void readDeviceRecords_advertised_noData_emptyListReturned_success()
            throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        advertiseStepsDeviceDataSource("test_device_id", buildDevice());

        mHealthConnectService.readDeviceRecords(
                mAttributionSource,
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId("test_device_id")
                        .build()
                        .toReadRecordsRequestParcel(),
                mReadRecordsResponseCallback);

        ArgumentCaptor<ReadRecordsResponseParcel> responseCaptor =
                ArgumentCaptor.forClass(ReadRecordsResponseParcel.class);
        verify(mReadRecordsResponseCallback, timeout(TIMEOUT_MILLIS))
                .onResult(responseCaptor.capture());
        ReadRecordsResponseParcel actualResponse = responseCaptor.getValue();
        assertThat(actualResponse.getRecordsParcel().getRecords().size()).isEqualTo(0);
        verifyNoMoreInteractions(mReadRecordsResponseCallback);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void readDeviceRecords_currentDeviceId_unmasksId() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        String clientExposedId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        advertiseStepsDeviceDataSource(clientExposedId, buildDevice());

        mHealthConnectService.readDeviceRecords(
                mAttributionSource,
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId(clientExposedId)
                        .build()
                        .toReadRecordsRequestParcel(),
                mReadRecordsResponseCallback);

        verify(mReadRecordsResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(any());

        ArgumentCaptor<ReadRecordsRequestParcel> parcelCaptor =
                ArgumentCaptor.forClass(ReadRecordsRequestParcel.class);
        String internalDeviceId = mDeviceDataProviderManager.getStableCurrentDeviceId();
        verify(mDeviceDataProviderManager).readDeviceRecords(any(), any(), parcelCaptor.capture());
        ReadRecordsRequestParcel capturedParcel = parcelCaptor.getValue();
        assertThat(capturedParcel.getPackageFilters()).containsExactly(internalDeviceId);
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecords_disabledDdpFlags_throws() throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        mHealthConnectService.deleteDeviceRecords(
                mAttributionSource,
                "some device id",
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder().build()),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecords_npDdpPermission_throws() throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_DENIED);

        mHealthConnectService.deleteDeviceRecords(
                mAttributionSource,
                "some device id",
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder().build()),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_SECURITY);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecords_noAdvertising_throws() throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        mHealthConnectService.deleteDeviceRecords(
                mAttributionSource,
                "some device id",
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder().build()),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecords_withPackageNameFilter_throws() throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        mHealthConnectService.deleteDeviceRecords(
                mAttributionSource,
                "some device id",
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder()
                                .addDataOrigin(
                                        new DataOrigin.Builder().setPackageName("Foo").build())
                                .build()),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecords_advertised_emptyRequest_success() throws RemoteException {
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        advertiseStepsDeviceDataSource("test device id", buildDevice());
        mHealthConnectService.deleteDeviceRecords(
                mAttributionSource,
                "test device id",
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder().build()),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecords_currentDeviceId_unmasks() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDeviceDataProviderPermission(PERMISSION_GRANTED);

        String clientExposedId = mHealthConnectService.getCurrentDeviceId(mAttributionSource);
        advertiseStepsDeviceDataSource(clientExposedId, buildDevice());

        mHealthConnectService.deleteDeviceRecords(
                mAttributionSource,
                clientExposedId,
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder().build()),
                mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(TIMEOUT_MILLIS)).onResult();

        String internalDeviceId = mDeviceDataProviderManager.getStableCurrentDeviceId();
        verify(mDeviceDataProviderManager, timeout(TIMEOUT_MILLIS))
                .deleteDeviceRecords(any(), eq(internalDeviceId), any());
    }

    private void setDeviceDataProviderPermission(int result) {
        when(mServiceContext.checkPermission(eq(MANAGE_HEALTH_DATA_PERMISSION), anyInt(), anyInt()))
                .thenReturn(result);
        when(mServiceContext.checkPermission(
                        eq(Manifest.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA),
                        anyInt(),
                        anyInt()))
                .thenReturn(result);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getDeviceDataSourceCapabilities_noAdvertisedData_returnsOnlySteps()
            throws Exception {
        IDeviceDataSourceCapabilitiesCallback.Stub callback =
                mock(IDeviceDataSourceCapabilitiesCallback.Stub.class);
        when(mDeviceDataSourcesHelper.getAllAdvertisedRecordTypes()).thenReturn(Set.of());

        mHealthConnectService.getDeviceDataSourceCapabilities(mAttributionSource, callback);
        awaitAllExecutorsIdle();

        ArgumentCaptor<DeviceDataSourceCapabilities> captor =
                ArgumentCaptor.forClass(DeviceDataSourceCapabilities.class);
        verify(callback).onResult(captor.capture());
        // Steps is always included because Health Connect can provide passive steps
        assertThat(captor.getValue().recordTypeIds).asList().containsExactly(RECORD_TYPE_STEPS);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getDeviceDataSourceCapabilities_withAdvertisedData_returnsCapabilities()
            throws Exception {
        IDeviceDataSourceCapabilitiesCallback.Stub callback =
                mock(IDeviceDataSourceCapabilitiesCallback.Stub.class);

        when(mDeviceDataSourcesHelper.getAllAdvertisedRecordTypes())
                .thenReturn(Set.of(RECORD_TYPE_HEART_RATE, RECORD_TYPE_STEPS));

        mHealthConnectService.getDeviceDataSourceCapabilities(mAttributionSource, callback);
        awaitAllExecutorsIdle();

        ArgumentCaptor<DeviceDataSourceCapabilities> captor =
                ArgumentCaptor.forClass(DeviceDataSourceCapabilities.class);
        verify(callback).onResult(captor.capture());
        assertThat(captor.getValue().recordTypeIds)
                .asList()
                .containsExactly(RECORD_TYPE_STEPS, RECORD_TYPE_HEART_RATE);
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getDeviceDataSourceCapabilities_flagDisabled_throwsUnsupportedOperation()
            throws Exception {
        IDeviceDataSourceCapabilitiesCallback.Stub callback =
                mock(IDeviceDataSourceCapabilitiesCallback.Stub.class);

        mHealthConnectService.getDeviceDataSourceCapabilities(mAttributionSource, callback);
        awaitAllExecutorsIdle();

        verify(callback).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    private void setUpCreateMedicalDataSourceDefaultMocks() {
        setDataManagementPermission(PERMISSION_DENIED);
        when(mAppOpsManagerLocal.isUidInForeground(anyInt())).thenReturn(true);
        when(mMedicalDataSourceHelper.createMedicalDataSource(
                        eq(getCreateMedicalDataSourceRequest()), any()))
                .thenReturn(getMedicalDataSourceRequiredFieldsOnly());
    }

    private void setUpPhrMocksWithIrrelevantResponses() {
        when(mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(any()))
                .thenReturn(List.of());
        when(mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of());
        when(mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        any(), anyInt()))
                .thenReturn(new ReadMedicalResourcesInternalResponse(List.of(), null, 0));
        when(mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        any(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(new ReadMedicalResourcesInternalResponse(List.of(), null, 0));
        when(mMedicalResourceHelper.upsertMedicalResources(any(), any())).thenReturn(List.of());
        doNothing()
                .when(mMedicalResourceHelper)
                .deleteMedicalResourcesByIdsWithoutPermissionChecks(any());
        doNothing()
                .when(mMedicalResourceHelper)
                .deleteMedicalResourcesByIdsWithPermissionChecks(any(), any());
        doNothing()
                .when(mMedicalResourceHelper)
                .deleteMedicalResourcesByRequestWithoutPermissionChecks(any());
        doNothing()
                .when(mMedicalResourceHelper)
                .deleteMedicalResourcesByRequestWithPermissionChecks(any(), any());

        when(mMedicalDataSourceHelper.createMedicalDataSource(any(), any()))
                .thenReturn(getMedicalDataSourceRequiredFieldsOnly());
        when(mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(any()))
                .thenReturn(List.of());
        when(mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        any(), any(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(List.of());
        when(mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(any()))
                .thenReturn(List.of());
        when(mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        any(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of());
    }

    // Suppliers must to be used because Matchers can't be passed directly through method calls.
    // See https://stackoverflow.com/a/55297901
    private void assertPhrApiWestWorldWrites(
            Supplier<Integer> apiMethodMatcherSupplier,
            Supplier<Integer> apiStatusMatcherSupplier,
            int wantedNumberOfInvocations) {
        verify(mHealthFitnessStatsLog, times(wantedNumberOfInvocations))
                .write(
                        eq(HEALTH_CONNECT_API_CALLED),
                        apiMethodMatcherSupplier.get(),
                        apiStatusMatcherSupplier.get(),
                        anyInt(),
                        anyLong(),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        eq(THIS_TEST_PACKAGE_NAME));
    }

    /**
     * {@code wantedNumberOfInvocations} indicates the wanted number of invocations for <b>each
     * item</b> in {@code medicalResourceTypes}.
     */
    // Suppliers must to be used because Matchers can't be passed directly through method calls.
    // See https://stackoverflow.com/a/55297901
    private void assertPhrApiPrivateWestWorldWrites(
            Supplier<Integer> apiMethodMatcherSupplier,
            Supplier<Integer> apiStatusMatcherSupplier,
            Collection<Integer> medicalResourceTypes,
            int wantedNumberOfInvocations) {
        for (int medicalResourceType : medicalResourceTypes) {
            verify(mHealthFitnessStatsLog, times(wantedNumberOfInvocations))
                    .write(
                            eq(HEALTH_CONNECT_PHR_API_INVOKED),
                            apiMethodMatcherSupplier.get(),
                            apiStatusMatcherSupplier.get(),
                            eq(THIS_TEST_PACKAGE_NAME),
                            eq(medicalResourceType));
        }
    }

    /**
     * This method should be used instead of {@link #assertPhrApiPrivateWestWorldWrites(Supplier,
     * Supplier, Collection, int)} when medical resource type is irrelevant.
     */
    // Suppliers must to be used because Matchers can't be passed directly through method calls.
    // See https://stackoverflow.com/a/55297901
    private void assertPhrApiPrivateWestWorldWrites(
            Supplier<Integer> apiMethodMatcherSupplier,
            Supplier<Integer> apiStatusMatcherSupplier,
            int wantedNumberOfInvocations) {
        verify(mHealthFitnessStatsLog, times(wantedNumberOfInvocations))
                .write(
                        eq(HEALTH_CONNECT_PHR_API_INVOKED),
                        apiMethodMatcherSupplier.get(),
                        apiStatusMatcherSupplier.get(),
                        eq(THIS_TEST_PACKAGE_NAME),
                        eq(MEDICAL_RESOURCE_TYPE_NOT_ASSIGNED_DEFAULT_VALUE));
    }

    private void setDataManagementPermission(int result) {
        when(mServiceContext.checkPermission(eq(MANAGE_HEALTH_DATA_PERMISSION), anyInt(), anyInt()))
                .thenReturn(result);
    }

    private void setBackupPermission(int result) {
        when(mServiceContext.checkCallingPermission(eq(BACKUP))).thenReturn(result);
    }

    private void setBackupHCDataAndSettingsPermission(int result) {
        when(mServiceContext.checkCallingPermission(eq(BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS)))
                .thenReturn(result);
    }

    private void setRestoreHCDataAndSettingsPermission(int result) {
        when(mServiceContext.checkCallingPermission(eq(RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS)))
                .thenReturn(result);
    }

    private void setBackgroundReadPermission(int result) {
        when(mServiceContext.checkPermission(
                        eq(READ_HEALTH_DATA_IN_BACKGROUND), anyInt(), anyInt()))
                .thenReturn(result);
    }

    /**
     * Sets permission check result for all medical permissions to be {@link
     * PermissionManager#PERMISSION_HARD_DENIED}. This is called in the {@link
     * HealthConnectServiceImplTest#setUp()}. Then each test can enable the permissions the test
     * needs.
     */
    private void setUpAllMedicalPermissionChecksHardDenied() {
        for (String permission : getAllMedicalPermissions()) {
            // Some methods use ForPreflight while others use ForDataDelivery. Set both here.
            when(mPermissionManager.checkPermissionForPreflight(permission, mAttributionSource))
                    .thenReturn(PERMISSION_HARD_DENIED);
            when(mPermissionManager.checkPermissionForDataDelivery(
                            permission, mAttributionSource, null))
                    .thenReturn(PERMISSION_HARD_DENIED);
        }
    }

    private void setDataReadWritePermissionGranted(String permission) {
        // Some methods use ForPreflight while others use ForDataDelivery. Set both here.
        when(mPermissionManager.checkPermissionForPreflight(permission, mAttributionSource))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        permission, mAttributionSource, null))
                .thenReturn(PermissionManager.PERMISSION_GRANTED);
    }

    private void setUpHealthPermissions() throws PackageManager.NameNotFoundException {
        PermissionGroupInfo info = new PermissionGroupInfo();
        info.packageName = HC_PACKAGE_NAME;
        when(mPackageManager.getPermissionGroupInfo(
                        eq(HealthPermissions.HEALTH_PERMISSION_GROUP), eq(0)))
                .thenReturn(info);

        PackageInfo mockPackageInfo = new PackageInfo();
        // For now add a few of the HealthPermissions just for the test.
        mockPackageInfo.permissions =
                new PermissionInfo[] {
                    createPermissionInfo(HealthPermissions.READ_HEART_RATE),
                    createPermissionInfo(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                    createPermissionInfo(HealthPermissions.READ_SKIN_TEMPERATURE),
                    createPermissionInfo(HealthPermissions.READ_OXYGEN_SATURATION),
                };
        when(mPackageManager.getPackageInfo(eq(HC_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
    }

    private PermissionInfo createPermissionInfo(String permissionName) {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.name = permissionName;
        permissionInfo.group = HealthPermissions.HEALTH_PERMISSION_GROUP;
        return permissionInfo;
    }

    private void setUpPassingPermissionCheckFor(String permission) {
        doNothing()
                .when(mServiceContext)
                .enforcePermission(eq(permission), anyInt(), anyInt(), anyString());
    }

    private void verifyOnboardingState(@HealthConnectOnboardingState.OnboardingState int expected)
            throws Exception {
        verify(mGetHealthConnectOnboardingStateCallback, timeout(5000))
                .onResult(any(HealthConnectOnboardingState.class));
        verify(mGetHealthConnectOnboardingStateCallback).onResult(mOnboardingStateCaptor.capture());

        assertEquals(mOnboardingStateCaptor.getValue().getOnboardingState(), expected);
    }

    private static File createAndGetNonEmptyFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Contents of file " + fileName);
        fileWriter.close();
        return file;
    }

    /**
     * Waits until all executors in {@link HealthConnectServiceImpl} idle. For now this just waits
     * for a fixed duration with {@link Thread#sleep(long)}, this could be improved later.
     */
    private static void awaitAllExecutorsIdle() throws InterruptedException {
        Thread.sleep(500);
    }

    private static PackageInfo buildPackageInfo(String packageName, int targetSdk) {
        PackageInfo info = new PackageInfo();
        ApplicationInfo aInfo = new ApplicationInfo();
        aInfo.targetSdkVersion = targetSdk;
        info.applicationInfo = aInfo;
        info.packageName = info.applicationInfo.packageName = packageName;
        return info;
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testGetDeviceDataSourceInfos_masksDataOrigin() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        Device device =
                new Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();

        advertiseStepsDeviceDataSource("device_id", device);

        mHealthConnectService.getDeviceDataSourceInfos(
                mAttributionSource, mGetDeviceDataSourceInfosCallback);

        verify(mGetDeviceDataSourceInfosCallback, timeout(5000)).onResult(any());
        ArgumentCaptor<List<DeviceDataSourceInfo>> captor = ArgumentCaptor.forClass(List.class);
        verify(mGetDeviceDataSourceInfosCallback).onResult(captor.capture());

        List<DeviceDataSourceInfo> result = captor.getValue();
        assertThat(result).hasSize(1);
        String spn = result.get(0).getDeviceDataOrigin().getPackageName();
        assertTrue(SyntheticPackageNameCreator.isMaskedSpn(spn));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testGetDeviceDataSourceInfos_populatesActivityLabels() throws RemoteException {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        Device device =
                new Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();

        advertiseStepsDeviceDataSource("device_id", device);

        ResolveInfo onboardingResolveInfo = new ResolveInfo();
        onboardingResolveInfo.nonLocalizedLabel = "Onboarding";
        String onboardingIntent = HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING;
        when(mPackageManager.resolveActivity(
                        argThat(
                                intent ->
                                        intent != null
                                                && onboardingIntent.equals(intent.getAction())),
                        eq(0)))
                .thenReturn(onboardingResolveInfo);

        ResolveInfo managementResolveInfo = new ResolveInfo();
        managementResolveInfo.nonLocalizedLabel = "Management";
        String managementIntent = HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT;
        when(mPackageManager.resolveActivity(
                        argThat(
                                intent ->
                                        intent != null
                                                && managementIntent.equals(intent.getAction())),
                        eq(0)))
                .thenReturn(managementResolveInfo);

        mHealthConnectService.getDeviceDataSourceInfos(
                mAttributionSource, mGetDeviceDataSourceInfosCallback);

        verify(mGetDeviceDataSourceInfosCallback, timeout(5000)).onResult(any());
        ArgumentCaptor<List<DeviceDataSourceInfo>> captor = ArgumentCaptor.forClass(List.class);
        verify(mGetDeviceDataSourceInfosCallback).onResult(captor.capture());

        List<DeviceDataSourceInfo> result = captor.getValue();
        assertThat(result).hasSize(1);
        DeviceDataProviderInfo info = result.get(0).getDeviceDataProviderInfos().get(0);
        assertThat(info.getOnboardingActivityLabel()).isEqualTo("Onboarding");
        assertThat(info.getManagementActivityLabel()).isEqualTo("Management");
    }

    private void advertiseStepsDeviceDataSource(String deviceId, Device device)
            throws RemoteException {
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);

        mHealthConnectService.advertiseDeviceDataSources(
                mAttributionSource, List.of(advertisement), mEmptyResponseCallback);

        verify(mEmptyResponseCallback, timeout(5000).times(1)).onResult();
    }

    private RecordsParcel getRestoredStepsRecordsParcel(StepsRecord stepsRecord) {
        RecordsParcel recordsParcel =
                new RecordsParcel(Collections.singletonList(stepsRecord.toRecordInternal()));
        android.os.Parcel parcel = android.os.Parcel.obtain();
        recordsParcel.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        // The mRecordsSize field in RecordsParcel is only populated when a parcel is restored
        RecordsParcel restoredRecordsParcel = RecordsParcel.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return restoredRecordsParcel;
    }
}
