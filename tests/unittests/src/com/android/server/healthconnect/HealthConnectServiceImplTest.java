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

import static android.Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_API_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_API_INVOKED__MEDICAL_RESOURCE_TYPE__MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.HealthConnectException.ERROR_INVALID_ARGUMENT;
import static android.health.connect.HealthConnectException.ERROR_SECURITY;
import static android.health.connect.HealthConnectException.ERROR_UNSUPPORTED_OPERATION;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STARTED;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.HealthPermissions.getAllMedicalPermissions;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_VERSION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_UUID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getGetMedicalDataSourceRequest;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalDataSourceRequiredFieldsOnly;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalResourceId;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getUpsertMedicalResourceRequest;
import static android.healthconnect.cts.utils.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.cts.utils.DataFactory.NOW;

import static com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_IMMEDIATE_EXPORT;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_STRUCTURAL_VALIDATION;
import static com.android.healthfitness.flags.Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_STATE_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_RESTORE_STATE_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_STAGING_DONE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.CREATE_MEDICAL_DATA_SOURCE;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_DATA_SOURCE_WITH_DATA;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.DELETE_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_IDS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.GET_MEDICAL_DATA_SOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_IDS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.READ_MEDICAL_RESOURCES_BY_REQUESTS;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.ApiMethods.UPSERT_MEDICAL_RESOURCES;
import static com.android.server.healthconnect.logging.HealthConnectServiceLogger.MEDICAL_RESOURCE_TYPE_NOT_ASSIGNED_DEFAULT_VALUE;
import static com.android.server.healthconnect.testing.TestUtils.waitForAllScheduledTasksToComplete;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteException;
import android.health.HealthFitnessStatsLog;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthPermissions;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IApplicationInfoResponseCallback;
import android.health.connect.aidl.ICanRestoreResponseCallback;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetSettingsForBackupResponseCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMedicalResourceTypeInfosCallback;
import android.health.connect.aidl.IMedicalResourcesResponseCallback;
import android.health.connect.aidl.IMigrationCallback;
import android.health.connect.aidl.IReadMedicalResourcesResponseCallback;
import android.health.connect.aidl.UpsertMedicalResourceRequestsParcel;
import android.health.connect.backuprestore.BackupSettings;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.migration.MigrationEntityParcel;
import android.health.connect.migration.MigrationException;
import android.health.connect.ratelimiter.RateLimiter;
import android.health.connect.restore.StageRemoteDataRequest;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
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
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigrationTestUtils;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.proto.backuprestore.AppInfoMap;
import com.android.server.healthconnect.proto.backuprestore.Settings;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.utils.PreferencesManager;
import com.android.server.healthconnect.testing.fakes.FakeTimeSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
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
                    "getSettingsForBackup",
                    "restoreSettings",
                    "canRestore",
                    "restoreChanges");

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
                    "queryDocumentProviders");

    private static final String TEST_URI = "content://com.android.server.healthconnect/testuri";
    private static final long DEFAULT_PACKAGE_APP_INFO = 123L;

    private static final String HC_PACKAGE_NAME = "com.android.healthconnect";

    /** Package name where {@link HealthConnectServiceImplTest this test} runs in. */
    private static final String THIS_TEST_PACKAGE_NAME = "com.android.healthconnect.unittests";

    private static final int TIMEOUT_MILLIS = 10_000;
    private static final int VACCINES_INVOKED =
            HEALTH_CONNECT_PHR_API_INVOKED__MEDICAL_RESOURCE_TYPE__MEDICAL_RESOURCE_TYPE_VACCINES;

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
    @Mock IMigrationCallback mMigrationCallback;
    @Mock IMedicalDataSourceResponseCallback mMedicalDataSourceCallback;
    @Mock IMedicalDataSourcesResponseCallback mMedicalDataSourcesResponseCallback;
    @Mock IReadMedicalResourcesResponseCallback mReadMedicalResourcesResponseCallback;
    @Mock IEmptyResponseCallback mEmptyResponseCallback;
    @Mock IMedicalResourcesResponseCallback mMedicalResourcesResponseCallback;
    @Mock IMedicalResourceListParcelResponseCallback mMedicalResourceListParcelResponseCallback;
    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Captor ArgumentCaptor<HealthConnectExceptionParcel> mErrorCaptor;
    @Captor ArgumentCaptor<List<MedicalDataSource>> mMedicalDataSourcesResponseCaptor;
    private FakeTimeSource mFakeTimeSource;
    private Context mContext;
    private AttributionSource mAttributionSource;
    private HealthConnectServiceImpl mHealthConnectService;
    private UserHandle mUserHandle;
    private BackupRestore mBackupRestore;
    private ThreadPoolExecutor mInternalTaskScheduler;
    private String mTestPackageName;
    private HealthConnectThreadScheduler mThreadScheduler;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    android.healthconnect.cts.utils.TestUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

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

        mFakeTimeSource = new FakeTimeSource(NOW);
        mAttributionSource = mContext.getAttributionSource();
        mTestPackageName = mAttributionSource.getPackageName();
        setUpAllMedicalPermissionChecksHardDenied();

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
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        healthConnectInjector.getFitnessRecordDeleteHelper(),
                        healthConnectInjector.getMedicalResourceHelper(),
                        healthConnectInjector.getMedicalDataSourceHelper(),
                        healthConnectInjector.getExportManager(),
                        healthConnectInjector.getExportImportSettingsStorage(),
                        healthConnectInjector.getExportImportNotificationSender(),
                        healthConnectInjector.getBackupRestore(),
                        healthConnectInjector.getAccessLogsHelper(),
                        healthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        healthConnectInjector.getActivityDateHelper(),
                        healthConnectInjector.getChangeLogsHelper(),
                        healthConnectInjector.getChangeLogsRequestHelper(),
                        healthConnectInjector.getPriorityMigrationHelper(),
                        healthConnectInjector.getAppInfoHelper(),
                        healthConnectInjector.getDeviceInfoHelper(),
                        healthConnectInjector.getPreferenceHelper(),
                        healthConnectInjector.getDatabaseHelpers(),
                        healthConnectInjector.getPreferencesManager(),
                        healthConnectInjector.getReadAccessLogsHelper(),
                        healthConnectInjector.getAppOpsManagerLocal(),
                        healthConnectInjector.getThreadScheduler(),
                        mRateLimiter,
                        healthConnectInjector.getEnvironmentDataDirectory(),
                        healthConnectInjector.getExportImportLogger(),
                        healthConnectInjector.getHealthFitnessStatsLog(),
                        healthConnectInjector.getBackupRestoreLogger());
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
        File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
        File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

        assertThat(testRestoreFile1.exists()).isTrue();
        assertThat(testRestoreFile2.exists()).isTrue();

        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        pfdsByFileName.put(
                testRestoreFile1.getName(),
                ParcelFileDescriptor.open(testRestoreFile1, ParcelFileDescriptor.MODE_WRITE_ONLY));
        pfdsByFileName.put(
                testRestoreFile2.getName(),
                ParcelFileDescriptor.open(testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

        final IDataStagingFinishedCallback callback = mock(IDataStagingFinishedCallback.class);
        mHealthConnectService.stageAllHealthConnectRemoteData(
                new StageRemoteDataRequest(pfdsByFileName), mUserHandle, callback);

        verify(callback, timeout(5000).times(1)).onError(any());
        var stagedFileNames = mBackupRestore.getStagedRemoteFileNames(mUserHandle);
        assertThat(stagedFileNames.size()).isEqualTo(1);
        assertThat(stagedFileNames.contains(testRestoreFile2.getName())).isTrue();
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testCreateMedicalDataSource_flagOff_throws() throws Exception {
        mHealthConnectService.createMedicalDataSource(
                mAttributionSource,
                getCreateMedicalDataSourceRequest(),
                mMedicalDataSourceCallback);

        verify(mMedicalDataSourceCallback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testGetMedicalDataSourcesByIds_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource,
                List.of(UUID.randomUUID().toString()),
                mMedicalDataSourcesResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testGetMedicalDataSourcesByIds_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testGetMedicalDataSources_byIds_flagOff_throws() throws Exception {
        mHealthConnectService.getMedicalDataSourcesByIds(
                mAttributionSource, List.of(), mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED
    })
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testGetMedicalDataSourcesByRequests_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                getGetMedicalDataSourceRequest(Set.of("com.abc")),
                mMedicalDataSourcesResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testGetMedicalDataSourcesByRequests_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testGetMedicalDataSource_byRequest_flagOff_throws() throws Exception {
        mHealthConnectService.getMedicalDataSourcesByRequest(
                mAttributionSource,
                new GetMedicalDataSourcesRequest.Builder().build(),
                mMedicalDataSourcesResponseCallback);

        verify(mMedicalDataSourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED
    })
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testDeleteMedicalDataSource_flagOff_throws() throws Exception {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        mHealthConnectService.deleteMedicalDataSourceWithData(mAttributionSource, "foo", callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PHR_FHIR_STRUCTURAL_VALIDATION
    })
    @DisableFlags({FLAG_PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE})
    public void testUpsertMedicalResources_weakReferenceFlagOff_succeeds() throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);

        mHealthConnectService.upsertMedicalResources(
                mAttributionSource,
                List.of(getUpsertMedicalResourceRequest()),
                mMedicalResourcesResponseCallback);

        verify(mMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PHR_FHIR_STRUCTURAL_VALIDATION,
        FLAG_PHR_FHIR_RESOURCE_VALIDATOR_USE_WEAK_REFERENCE
    })
    public void testUpsertMedicalResources_weakReferenceFlagOn_succeeds() throws RemoteException {
        setUpPhrMocksWithIrrelevantResponses();
        setDataManagementPermission(PERMISSION_DENIED);
        setDataReadWritePermissionGranted(WRITE_MEDICAL_DATA);

        mHealthConnectService.upsertMedicalResources(
                mAttributionSource,
                List.of(getUpsertMedicalResourceRequest()),
                mMedicalResourcesResponseCallback);

        verify(mMedicalResourcesResponseCallback, timeout(5000)).onResult(any());
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testUpsertMedicalResources_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.upsertMedicalResources(
                mAttributionSource,
                List.of(
                        new UpsertMedicalResourceRequest.Builder(
                                        DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION)
                                .build()),
                mMedicalResourcesResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testUpsertMedicalResourcesFromRequestsParcel_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
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

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testUpsertMedicalResources_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.upsertMedicalResources(
                mAttributionSource,
                List.of(
                        new UpsertMedicalResourceRequest.Builder(
                                        DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION)
                                .build()),
                mMedicalResourcesResponseCallback);

        // wait for callback before asserting logs
        verify(mMedicalResourcesResponseCallback, timeout(TIMEOUT_MILLIS)).onResult(any());
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
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testUpsertMedicalResourcesFromRequestsParcel_telemetryFlagOn_expectCorrectLogs()
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testUpsertMedicalResources_flagOff_throws() throws Exception {
        IMedicalResourcesResponseCallback callback = mock(IMedicalResourcesResponseCallback.class);

        mHealthConnectService.upsertMedicalResources(
                mAttributionSource,
                List.of(
                        new UpsertMedicalResourceRequest.Builder(
                                        DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION)
                                .build()),
                callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testUpsertMedicalResourcesFromRequestsParcel_flagOff_throws() throws Exception {

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
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testReadMedicalResourcesByRequests_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(NOW);

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource,
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build()
                        .toParcel(),
                mReadMedicalResourcesResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        verify(mPreferencesManager, never()).setLastPhrReadMedicalResourcesApiTimeStamp(any());
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testReadMedicalResourcesByRequests_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(NOW);

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
        verify(mPreferencesManager, times(1)).setLastPhrReadMedicalResourcesApiTimeStamp(eq(NOW));
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void
            testReadMedicalResourcesByRequests_telemetryFlagOnAndHasDataManagementPermission_expectMonthlyTimeStamp()
                    throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(NOW);
        setDataManagementPermission(PERMISSION_GRANTED);

        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource,
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build()
                        .toParcel(),
                mReadMedicalResourcesResponseCallback);

        awaitAllExecutorsIdle();
        assertThat(mPreferencesManager.getPhrLastReadMedicalResourcesApiTimeStamp()).isNull();
        verify(mPreferencesManager, times(1)).setLastPhrReadMedicalResourcesApiTimeStamp(eq(NOW));
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testReadMedicalResourcesByIds_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(NOW);

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource,
                List.of(getMedicalResourceId()),
                mReadMedicalResourcesResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        verify(mPreferencesManager, never()).setLastPhrReadMedicalResourcesApiTimeStamp(any());
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testReadMedicalResourcesByIds_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(NOW);

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
        verify(mPreferencesManager, times(1)).setLastPhrReadMedicalResourcesApiTimeStamp(eq(NOW));
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void
            testReadMedicalResourcesByIds_telemetryFlagOnAndHasDataManagementPermission_expectMonthlyTimeStamp() {
        setUpSuccessfulMocksForPhrTelemetry();
        mFakeTimeSource.setInstant(NOW);
        setDataManagementPermission(PERMISSION_GRANTED);

        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource,
                List.of(getMedicalResourceId()),
                mReadMedicalResourcesResponseCallback);

        verify(mPreferencesManager, timeout(5000).times(1))
                .setLastPhrReadMedicalResourcesApiTimeStamp(eq(NOW));
    }

    @Test
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testReadMedicalResources_byIds_flagOff_throws() throws Exception {
        mHealthConnectService.readMedicalResourcesByIds(
                mAttributionSource,
                List.of(getMedicalResourceId()),
                mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED
    })
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testReadMedicalResources_byRequest_flagOff_throws() throws Exception {
        mHealthConnectService.readMedicalResourcesByRequest(
                mAttributionSource,
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build()
                        .toParcel(),
                mReadMedicalResourcesResponseCallback);

        verify(mReadMedicalResourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED
    })
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testUpsertMedicalResources_hasDataManagementPermission_throws()
            throws RemoteException {
        setDataManagementPermission(PackageManager.PERMISSION_GRANTED);

        mHealthConnectService.upsertMedicalResources(
                mAttributionSource,
                List.of(
                        new UpsertMedicalResourceRequest.Builder(
                                        DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION)
                                .build()),
                mMedicalResourcesResponseCallback);

        verify(mMedicalResourcesResponseCallback, timeout(5000).times(1))
                .onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testUpsertMedicalResources_noWriteMedicalDataPermission_throws() throws Exception {
        IMedicalResourcesResponseCallback callback = mock(IMedicalResourcesResponseCallback.class);

        mHealthConnectService.upsertMedicalResources(
                mAttributionSource,
                List.of(
                        new UpsertMedicalResourceRequest.Builder(
                                        DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION)
                                .build()),
                callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testCreateMedicalDataSource_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.createMedicalDataSource(
                mAttributionSource,
                getCreateMedicalDataSourceRequest(),
                mMedicalDataSourceCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testCreateMedicalDataSource_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testDeleteMedicalDataSourceWithData_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.deleteMedicalDataSourceWithData(
                mAttributionSource, UUID.randomUUID().toString(), mEmptyResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testDeleteMedicalDataSourceWithData_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
        verifyNoMoreInteractions(callback);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testDeleteMedicalResources_byIds_flagOff_throws() throws Exception {
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
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResources_noIds_returns() throws RemoteException {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);

        mHealthConnectService.deleteMedicalResourcesByIds(mAttributionSource, List.of(), callback);

        verify(callback, timeout(5000).times(1)).onResult();
        verifyNoMoreInteractions(callback);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testDeleteMedicalResourcesByIds_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();

        mHealthConnectService.deleteMedicalResourcesByIds(
                mAttributionSource, List.of(getMedicalResourceId()), mEmptyResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testDeleteMedicalResourcesByIds_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testDeleteMedicalResourcesByRequests_telemetryFlagOff_expectNoLogs()
            throws InterruptedException {
        setUpSuccessfulMocksForPhrTelemetry();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mHealthConnectService.deleteMedicalResourcesByRequest(
                mAttributionSource, request, mEmptyResponseCallback);

        awaitAllExecutorsIdle();
        assertPhrApiWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
        assertPhrApiPrivateWestWorldWrites(ArgumentMatchers::anyInt, ArgumentMatchers::anyInt, 0);
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY_PRIVATE_WW
    })
    public void testDeleteMedicalResourcesByRequests_telemetryFlagOn_expectCorrectLogs()
            throws RemoteException {
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testDeleteMedicalResourcesByRequest_flagOff_throws() throws Exception {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mHealthConnectService.deleteMedicalResourcesByRequest(
                mAttributionSource, request, callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD)
    public void testQueryAllMedicalResourceTypeInfos_flagOff_throws() throws Exception {
        IMedicalResourceTypeInfosCallback callback = mock(IMedicalResourceTypeInfosCallback.class);

        mHealthConnectService.queryAllMedicalResourceTypeInfos(callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
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
    public void getSettingsForBackup_flagDisabled_unsupportedOperation() throws RemoteException {
        IGetSettingsForBackupResponseCallback callback =
                mock(IGetSettingsForBackupResponseCallback.class);
        mHealthConnectService.getSettingsForBackup(callback);

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
    public void restoreSettings_flagDisabled_unsupportedOperation() throws RemoteException {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        Settings settings =
                Settings.newBuilder()
                        .setWeightUnitSetting(Settings.WeightUnitProto.KILOGRAM)
                        .setDistanceUnitSetting(Settings.DistanceUnitProto.MILES)
                        .build();

        mHealthConnectService.restoreSettings(new BackupSettings(settings.toByteArray()), callback);

        verify(callback, timeout(5000).times(1)).onError(mErrorCaptor.capture());
        assertThat(mErrorCaptor.getValue().getHealthConnectException().getErrorCode())
                .isEqualTo(ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void restoreChanges_flagDisabled_unsupportedOperation() throws RemoteException {
        IEmptyResponseCallback callback = mock(IEmptyResponseCallback.class);
        AppInfoMap appInfoMap =
                AppInfoMap.newBuilder()
                        .putAppInfo(
                                "random.package.name",
                                Settings.AppInfo.newBuilder().setAppName("app name 1").build())
                        .build();
        mHealthConnectService.restoreChanges(List.of(), appInfoMap.toByteArray(), callback);

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
     * com.android.server.healthconnect.logging.HealthConnectServiceLogger}.
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
        if (!isPersonalHealthRecordEnabled()) {
            return;
        }
        for (String permission : getAllMedicalPermissions()) {
            // Some methods use ForPreflight while others use ForDataDelivery. Set both here.
            when(mPermissionManager.checkPermissionForPreflight(permission, mAttributionSource))
                    .thenReturn(PermissionManager.PERMISSION_HARD_DENIED);
            when(mPermissionManager.checkPermissionForDataDelivery(
                            permission, mAttributionSource, null))
                    .thenReturn(PermissionManager.PERMISSION_HARD_DENIED);
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
}
