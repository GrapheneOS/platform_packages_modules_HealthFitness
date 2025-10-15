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
import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_IMMEDIATE_EXPORT;
import static com.android.healthfitness.flags.Flags.FLAG_MATCHMAKING;
import static com.android.healthfitness.flags.Flags.FLAG_ONBOARDING;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS_DB;
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
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.ApiMethods.UPSERT_MEDICAL_RESOURCES;
import static com.android.server.healthconnect.common.logging.HealthConnectServiceLogger.MEDICAL_RESOURCE_TYPE_NOT_ASSIGNED_DEFAULT_VALUE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import android.health.HealthFitnessStatsLog;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMatchingAppsRequest;
import android.health.connect.GetMatchingAppsResponse;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectOnboardingState;
import android.health.connect.HealthPermissions;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IApplicationInfoResponseCallback;
import android.health.connect.aidl.ICanRestoreResponseCallback;
import android.health.connect.aidl.IChangeLogsResponseCallback;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangeLogTokenCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetHealthConnectOnboardingStateCallback;
import android.health.connect.aidl.IGetLatestMetadataForBackupResponseCallback;
import android.health.connect.aidl.IGetMatchingAppsCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IIsMatchmakingPossibleCallback;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMigrationCallback;
import android.health.connect.aidl.IReadMedicalResourcesResponseCallback;
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
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
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

import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.backuprestore.BackupRestore;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.logging.HealthConnectServiceLogger;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.common.preferences.PreferencesManager;
import com.android.server.healthconnect.device.tracker.TrackerManager;
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
import com.android.server.healthconnect.storage.TransactionManager;

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
                    "aggregateRecords",
                    "readRecords",
                    "updateRecords",
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
                    "getMatchingApps",
                    "recordMatchmakingDenial");

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
                    "getHealthConnectOnboardingState",
                    "updateHealthConnectBackupAndRestoreSettings",
                    "updateHealthConnectRestoreStatus",
                    "updateHealthConnectBackupStatus");

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

    @Mock private TransactionManager mTransactionManager;
    @Mock private AppInfoHelper mAppInfoHelper;
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
    @Mock IGetMatchingAppsCallback mGetMatchingAppsCallback;
    @Mock IIsMatchmakingPossibleCallback mIsMatchmakingPossibleCallback;

    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Mock private ChangeLogsHelper mChangeLogsHelper;
    @Mock private ChangeLogsRequestHelper mChangeLogsRequestHelper;
    @Mock private IGetChangeLogTokenCallback mGetChangeLogTokenCallback;
    @Mock private IChangeLogsResponseCallback mChangeLogsResponseCallback;
    @Mock private OnboardingStateManager mOnboardingStateManager;
    @Mock private MatchmakingManager mMatchmakingManager;
    @Captor ArgumentCaptor<HealthConnectExceptionParcel> mErrorCaptor;
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
    private final Instant mNow = DataFactory.now();

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
        setUpHealthPermissions();

        mFakeTimeSource = new FakeTimeSource(mNow);
        mAttributionSource = mContext.getAttributionSource();
        mTestPackageName = mAttributionSource.getPackageName();
        setUpAllMedicalPermissionChecksHardDenied();
        when(mPackageManager.getPackageInfo(eq(mAttributionSource.getPackageName()), any()))
                .thenReturn(
                        buildPackageInfo(mAttributionSource.getPackageName(), /* targetSdk= */ 34));

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mServiceContext)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setPreferencesManager(mPreferencesManager)
                        .setTransactionManager(mTransactionManager)
                        .setHealthDataCategoryPriorityHelper(mHealthDataCategoryPriorityHelper)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setHealthConnectPermissionHelper(mHealthConnectPermissionHelper)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setMigrationCleaner(mMigrationCleaner)
                        .setMedicalDataSourceHelper(mMedicalDataSourceHelper)
                        .setMedicalResourceHelper(mMedicalResourceHelper)
                        .setMigrationStateManager(mMigrationStateManager)
                        .setMigrationUiStateManager(mMigrationUiStateManager)
                        .setAppInfoHelper(mAppInfoHelper)
                        .setTimeSource(mFakeTimeSource)
                        .setAppOpsManagerLocal(mAppOpsManagerLocal)
                        .setHealthFitnessStatsLog(mHealthFitnessStatsLog)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .setChangeLogsHelper(mChangeLogsHelper)
                        .setChangeLogsRequestHelper(mChangeLogsRequestHelper)
                        .setOnboardingStateManager(mOnboardingStateManager)
                        .setMatchingAppsManager(mMatchmakingManager)
                        .build();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();
        mInternalTaskScheduler = mThreadScheduler.mInternalBackgroundExecutor;

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
                        healthConnectInjector.getSyntheticPackageNameResolver());
        mBackupRestore = healthConnectInjector.getBackupRestore();
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
        when(mAppInfoHelper.getAppInfoId(any())).thenReturn(DEFAULT_PACKAGE_APP_INFO);
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

        mHealthConnectService.getContributorApplicationsInfo(callback);

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
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
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
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
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
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
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
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
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
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
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
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
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
    public void getMatchingApps_flagOff_exception() throws Exception {
        GetMatchingAppsRequest request = new GetMatchingAppsRequest.Builder().build();

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingApps_noDMPermission_emptySetRequest_emptyMapReturned_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of();
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(5000).times(1))
                .onResult(new GetMatchingAppsResponse(Map.of()));
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingApps_noDMPermission_nonEmptySetRequest_emptyMapReturned_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(5000).times(1))
                .onResult(new GetMatchingAppsResponse(Map.of()));
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingApps_noDMPermission_emptySetRequest_nonEmptyMapReturned_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(5000).times(1))
                .onResult(new GetMatchingAppsResponse(matchingApps));
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingApps_noDMPermission_nonEmptySetRequest_nonEmptyMapReturned_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(5000).times(1))
                .onResult(new GetMatchingAppsResponse(matchingApps));
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingApps_noDMPermission_invalidPackageNameInRequest_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName("invalid.package.name")
                        .addRecordTypes(recordTypes)
                        .build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("invalid package name provided");
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingApps_noDMPermission_packageNameSameAsCalling_success() throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .addRecordTypes(recordTypes)
                        .setPackageName(mTestPackageName)
                        .build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(5000).times(1))
                .onResult(new GetMatchingAppsResponse(matchingApps));
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void getMatchingApps_hasDMPermission_noPackageNameInRequest_throws() throws Exception {
        setDataManagementPermission(PERMISSION_GRANTED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, THIS_TEST_PACKAGE_NAME))
                .thenReturn(matchingApps);

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(TIMEOUT_MILLIS)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_INVALID_ARGUMENT);
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getMessage())
                .contains("package name must be provided");
    }

    @EnableFlags({FLAG_MATCHMAKING})
    @Test
    public void getMatchingApps_hasDMPermission_packageProvided_recordsProvided_success()
            throws Exception {
        setDataManagementPermission(PERMISSION_GRANTED);

        Set<Class<? extends Record>> recordTypes = Set.of();
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(THIS_TEST_PACKAGE_NAME)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, THIS_TEST_PACKAGE_NAME))
                .thenReturn(Map.of());

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        verify(mGetMatchingAppsCallback, timeout(5000).times(1))
                .onResult(new GetMatchingAppsResponse(Map.of()));
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void getMatchingApps_packageProvided_noRecordsProvided_success() throws Exception {
        Set<Class<? extends Record>> recordTypes = Set.of();
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(mTestPackageName)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        ArgumentCaptor<GetMatchingAppsResponse> responseCaptor =
                ArgumentCaptor.forClass(GetMatchingAppsResponse.class);
        verify(mGetMatchingAppsCallback, timeout(TIMEOUT_MILLIS))
                .onResult(responseCaptor.capture());
        GetMatchingAppsResponse actualResponse = responseCaptor.getValue();
        assertThat(actualResponse.getMatchingApps()).isEqualTo(Map.of());

        verify(mGetMatchingAppsCallback, never()).onError(any());
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void getMatchingApps_packageProvided_areAvailableApps_success() throws Exception {
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(mTestPackageName)
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

        mHealthConnectService.getMatchingApps(
                mAttributionSource, request, mGetMatchingAppsCallback);

        ArgumentCaptor<GetMatchingAppsResponse> responseCaptor =
                ArgumentCaptor.forClass(GetMatchingAppsResponse.class);
        verify(mGetMatchingAppsCallback, timeout(TIMEOUT_MILLIS))
                .onResult(responseCaptor.capture());
        GetMatchingAppsResponse actualResponse = responseCaptor.getValue();
        assertThat(actualResponse.getMatchingApps()).isEqualTo(matchingApps);
        verifyNoMoreInteractions(mGetMatchingAppsCallback);
    }

    @Test
    @DisableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_flagOff_exception() throws Exception {
        GetMatchingAppsRequest request = new GetMatchingAppsRequest.Builder().build();

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
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1)).onResult(false);
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_nonEmptySetRequest_emptyMapReturned_false()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1)).onResult(false);
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_emptySetRequest_nonEmptyMapReturned_true()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of();
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1)).onResult(true);
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_nonEmptySetRequest_nonEmptyMapReturned_true()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1)).onResult(true);
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_invalidPackageNameInRequest_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName("invalid.package.name")
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
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_noDMPermission_packageNameSameAsCalling_true()
            throws Exception {
        setDataManagementPermission(PERMISSION_DENIED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .addRecordTypes(recordTypes)
                        .setPackageName(mTestPackageName)
                        .build();
        Map<String, Set<String>> matchingApps = Map.of(THIS_TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(matchingApps);

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1)).onResult(true);
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags(FLAG_MATCHMAKING)
    public void isMatchmakingPossible_hasDMPermission_noPackageNameInRequest_throws()
            throws Exception {
        setDataManagementPermission(PERMISSION_GRANTED);
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
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
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(THIS_TEST_PACKAGE_NAME)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, THIS_TEST_PACKAGE_NAME))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(5000).times(1)).onResult(false);
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void isMatchmakingPossible_packageProvided_noRecordsProvided_false() throws Exception {
        Set<Class<? extends Record>> recordTypes = Set.of();
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(mTestPackageName)
                        .addRecordTypes(recordTypes)
                        .build();
        when(mMatchmakingManager.fetchMatchingApps(recordTypes, mTestPackageName))
                .thenReturn(Map.of());

        mHealthConnectService.isMatchmakingPossible(
                mAttributionSource, request, mIsMatchmakingPossibleCallback);

        verify(mIsMatchmakingPossibleCallback, timeout(TIMEOUT_MILLIS)).onResult(false);
        verify(mIsMatchmakingPossibleCallback, never()).onError(any());
        verifyNoMoreInteractions(mIsMatchmakingPossibleCallback);
    }

    @Test
    @EnableFlags({FLAG_MATCHMAKING})
    public void isMatchmakingPossible_packageProvided_areAvailableApps_true() throws Exception {
        Set<Class<? extends Record>> recordTypes = Set.of(SleepSessionRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(mTestPackageName)
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

        verify(mIsMatchmakingPossibleCallback, timeout(TIMEOUT_MILLIS)).onResult(true);
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
}
