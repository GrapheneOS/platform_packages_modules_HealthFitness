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

package com.android.server.healthconnect.exportimport;

import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_NONE;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_VERSION_MISMATCH;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_WRONG_FILE;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_STARTED;

import static com.android.server.healthconnect.exportimport.ExportManager.LOCAL_EXPORT_DATABASE_FILE_NAME;
import static com.android.server.healthconnect.exportimport.ImportManager.IMPORT_DATABASE_DIR_NAME;
import static com.android.server.healthconnect.exportimport.ImportManager.IMPORT_DATABASE_FILE_NAME;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createBloodPressureRecord;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthDataCategory;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.net.Uri;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseHelper.DatabaseHelpers;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.testing.TestUtils;
import com.android.server.healthconnect.testing.fakes.FakePreferenceHelper;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ImportManagerTest {

    private static final String TAG = "ImportManagerTest";
    private static final String TEST_PACKAGE_NAME = "package.name";
    private static final String TEST_DIRECTORY_NAME = "test";
    private static final UserHandle DEFAULT_USER_HANDLE = UserHandle.of(UserHandle.myUserId());

    private static final String TEST_PACKAGE_NAME_2 = "other.app";
    private static final String TEST_PACKAGE_NAME_3 = "another.app";

    private static final int TEST_COMPRESSED_FILE_SIZE = 1042;

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule(order = 2)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(HealthConnectManager.class)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    @Rule public final TemporaryFolder mEnvironmentDataDirectory = new TemporaryFolder();

    private ImportManager mImportManagerSpy;

    private Context mContext;
    private TransactionManager mTransactionManager;
    private TransactionTestUtils mTransactionTestUtils;
    private HealthDataCategoryPriorityHelper mPriorityHelper;
    private ExportImportSettingsStorage mExportImportSettingsStorage;
    private AppInfoHelper mAppInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private DatabaseHelpers mDatabaseHelpers;
    private DeviceInfoHelper mDeviceInfoHelper;
    private ReadAccessLogsHelper mReadAccessLogsHelper;
    private InternalHealthConnectMappings mInternalHealthConnectMappings;
    private HealthConnectThreadScheduler mThreadScheduler;

    @Mock private HealthConnectNotificationSender mNotificationSender;
    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private ExportImportLogger mExportImportLogger;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPreferenceHelper(new FakePreferenceHelper())
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mDatabaseHelpers = healthConnectInjector.getDatabaseHelpers();
        mExportImportSettingsStorage = healthConnectInjector.getExportImportSettingsStorage();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mInternalHealthConnectMappings = healthConnectInjector.getInternalHealthConnectMappings();
        mReadAccessLogsHelper = healthConnectInjector.getReadAccessLogsHelper();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();

        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME_2);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME_3);

        mPriorityHelper = healthConnectInjector.getHealthDataCategoryPriorityHelper();
        mPriorityHelper.setPriorityOrder(HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME));

        Instant timeStamp = Instant.parse("2024-06-04T16:39:12Z");
        Clock fakeClock = Clock.fixed(timeStamp, ZoneId.of("UTC"));

        ImportManager importManager =
                new ImportManager(
                        mAppInfoHelper,
                        mContext,
                        mExportImportSettingsStorage,
                        mTransactionManager,
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        mDeviceInfoHelper,
                        mPriorityHelper,
                        fakeClock,
                        mNotificationSender,
                        mEnvironmentDataDirectory.getRoot(),
                        mExportImportLogger);
        mImportManagerSpy = ExtendedMockito.spy(importManager);
        doReturn(TEST_COMPRESSED_FILE_SIZE)
                .when(mImportManagerSpy)
                .getFileSizeInKb(any(ContentResolver.class), any(Uri.class));
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.waitForAllScheduledTasksToComplete(mThreadScheduler);

        File testDir = mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE);
        File[] allContents = testDir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                file.delete();
            }
        }
        testDir.delete();
    }

    @Test
    public void copiesAllData() throws Exception {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        createStepsRecord(123, 345, 100),
                        createBloodPressureRecord(234, 120.0, 80.0));

        File zipToImport = zipExportedDb(exportCurrentDb());

        mDatabaseHelpers.clearAllData(mTransactionManager);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE,
                        DEFAULT_USER_HANDLE);

        List<UUID> stepsUuids = ImmutableList.of(UUID.fromString(uuids.get(0)));
        List<UUID> bloodPressureUuids = ImmutableList.of(UUID.fromString(uuids.get(1)));

        List<RecordInternal<?>> records =
                mTransactionTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                stepsUuids,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                bloodPressureUuids));
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getUuid()).isEqualTo(stepsUuids.get(0));
        assertThat(records.get(1).getUuid()).isEqualTo(bloodPressureUuids.get(0));
        assertThat(mExportImportSettingsStorage.getImportStatus().getDataImportState())
                .isEqualTo(DATA_IMPORT_ERROR_NONE);
    }

    @Test
    public void mergesPriorityList() throws Exception {
        mPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME, TEST_PACKAGE_NAME_2));
        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.ACTIVITY)))
                .containsExactly(TEST_PACKAGE_NAME, TEST_PACKAGE_NAME_2)
                .inOrder();

        File zipToImport = zipExportedDb(exportCurrentDb());

        mPriorityHelper.setPriorityOrder(HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME_2));
        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.ACTIVITY)))
                .containsExactly(TEST_PACKAGE_NAME_2)
                .inOrder();

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE,
                        DEFAULT_USER_HANDLE);

        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.ACTIVITY)))
                .containsExactly(TEST_PACKAGE_NAME_2, TEST_PACKAGE_NAME)
                .inOrder();
    }

    @Test
    public void mergesPriorityList_handlesDifferentPackageNames() throws Exception {
        mPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME, TEST_PACKAGE_NAME_2));
        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.ACTIVITY)))
                .containsExactly(TEST_PACKAGE_NAME, TEST_PACKAGE_NAME_2)
                .inOrder();

        File zipToImport = zipExportedDb(exportCurrentDb());

        mPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME_2, TEST_PACKAGE_NAME_3));
        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.ACTIVITY)))
                .containsExactly(TEST_PACKAGE_NAME_2, TEST_PACKAGE_NAME_3)
                .inOrder();

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE,
                        DEFAULT_USER_HANDLE);

        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.ACTIVITY)))
                .containsExactly(TEST_PACKAGE_NAME_2, TEST_PACKAGE_NAME_3, TEST_PACKAGE_NAME)
                .inOrder();
    }

    @Test
    public void skipsMissingTables() throws Exception {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        createStepsRecord(123, 345, 100),
                        createBloodPressureRecord(234, 120.0, 80.0));

        File dbToImport = exportCurrentDb();

        // Delete steps record table in import db.
        String stepsRecordTableName =
                mInternalHealthConnectMappings
                        .getRecordHelper(RecordTypeIdentifier.RECORD_TYPE_STEPS)
                        .getMainTableName();
        try (SQLiteDatabase importDb =
                SQLiteDatabase.openDatabase(
                        dbToImport, new SQLiteDatabase.OpenParams.Builder().build())) {
            importDb.execSQL("DROP TABLE " + stepsRecordTableName);
        }

        File zipToImport = zipExportedDb(dbToImport);

        mDatabaseHelpers.clearAllData(mTransactionManager);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE,
                        DEFAULT_USER_HANDLE);

        List<UUID> stepsUuids = ImmutableList.of(UUID.fromString(uuids.get(0)));
        List<UUID> bloodPressureUuids = ImmutableList.of(UUID.fromString(uuids.get(1)));

        List<RecordInternal<?>> records =
                mTransactionTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                stepsUuids,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                bloodPressureUuids));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getUuid()).isEqualTo(bloodPressureUuids.get(0));
    }

    @Test
    public void deletesTheDatabase() throws Exception {
        File dbToImport = exportCurrentDb();

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(dbToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender
                                .NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE,
                        DEFAULT_USER_HANDLE);

        File databaseDir =
                HealthConnectContext.create(
                                mContext,
                                mContext.getUser(),
                                IMPORT_DATABASE_DIR_NAME,
                                mEnvironmentDataDirectory.getRoot())
                        .getDataDir();
        assertThat(new File(databaseDir, IMPORT_DATABASE_FILE_NAME).exists()).isFalse();
    }

    @Test
    public void importNotADatabase_logsWrongFileError() throws Exception {
        File textFileToImport =
                createTextFile(
                        mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE), "export.txt");
        File zipToImport = zipExportedDb(textFileToImport);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mExportImportLogger, times(1))
                .logImportStatus(
                        eq(DATA_IMPORT_STARTED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED));
        verify(mExportImportLogger)
                .logImportStatus(DATA_IMPORT_ERROR_WRONG_FILE, 0, 0, TEST_COMPRESSED_FILE_SIZE);
    }

    @Test
    public void importNotADatabase_setsWrongFileError() throws Exception {
        File textFileToImport =
                createTextFile(
                        mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE), "export.txt");
        File zipToImport = zipExportedDb(textFileToImport);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender
                                .NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE,
                        DEFAULT_USER_HANDLE);

        assertThat(mExportImportSettingsStorage.getImportStatus().getDataImportState())
                .isEqualTo(DATA_IMPORT_ERROR_WRONG_FILE);
    }

    @Test
    public void importWrongFileName_setsWrongFileError() throws Exception {
        File textFileToImport =
                createTextFile(
                        mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE),
                        "wrong_name.txt");
        File zipToImport =
                new File(mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE), "export.zip");
        Compressor.compress(textFileToImport, "wrong_name.txt", zipToImport);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender
                                .NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE,
                        DEFAULT_USER_HANDLE);

        assertThat(mExportImportSettingsStorage.getImportStatus().getDataImportState())
                .isEqualTo(DATA_IMPORT_ERROR_WRONG_FILE);
    }

    @Test
    public void importWrongFileName_logsWrongFileError() throws Exception {
        doReturn(0)
                .when(mImportManagerSpy)
                .getFileSizeInKb(any(ContentResolver.class), any(Uri.class));

        File textFileToImport =
                createTextFile(
                        mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE),
                        "wrong_name.txt");
        File zipToImport =
                new File(mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE), "export.zip");
        Compressor.compress(textFileToImport, "wrong_name.txt", zipToImport);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mExportImportLogger, times(1))
                .logImportStatus(
                        eq(DATA_IMPORT_STARTED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED));
        verify(mExportImportLogger).logImportStatus(DATA_IMPORT_ERROR_WRONG_FILE, 0, 0, 0);
    }

    @Test
    public void versionMismatch_setsVersionMismatchError() throws Exception {
        File dbToImport = exportCurrentDb();
        try (SQLiteDatabase sqlDbToImport =
                SQLiteDatabase.openDatabase(
                        dbToImport, new SQLiteDatabase.OpenParams.Builder().build())) {
            sqlDbToImport.setVersion(100);
        }
        File zipToImport = zipExportedDb(dbToImport);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender
                                .NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_VERSION_MISMATCH,
                        DEFAULT_USER_HANDLE);

        assertThat(mExportImportSettingsStorage.getImportStatus().getDataImportState())
                .isEqualTo(DATA_IMPORT_ERROR_VERSION_MISMATCH);
    }

    @Test
    public void successfulImport_setsNoError() throws Exception {
        File zipToImport = zipExportedDb(exportCurrentDb());

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE,
                        DEFAULT_USER_HANDLE);

        assertThat(mExportImportSettingsStorage.getImportStatus().getDataImportState())
                .isEqualTo(DATA_IMPORT_ERROR_NONE);
    }

    @Test
    public void importedStarted_logsNoError() throws Exception {
        File zipToImport = zipExportedDb(exportCurrentDb());

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mExportImportLogger, times(1))
                .logImportStatus(
                        eq(DATA_IMPORT_STARTED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED));
    }

    @Test
    public void successfulImport_logsNoError() throws Exception {
        File currentDb = exportCurrentDb();
        File zipToImport = zipExportedDb(currentDb);
        int expectedOriginalFileSize = intSizeInKb(currentDb);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mExportImportLogger, times(1))
                .logImportStatus(
                        eq(DATA_IMPORT_STARTED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED),
                        eq(ExportImportLogger.NO_VALUE_RECORDED));

        verify(mExportImportLogger, times(1))
                .logImportStatus(
                        DATA_IMPORT_ERROR_NONE,
                        0,
                        expectedOriginalFileSize,
                        TEST_COMPRESSED_FILE_SIZE);
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void copiesAllData_usingInsertAllWithoutAccessLogs() throws Exception {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        createStepsRecord(123, 345, 100),
                        createBloodPressureRecord(234, 120.0, 80.0));

        File zipToImport = zipExportedDb(exportCurrentDb());

        mDatabaseHelpers.clearAllData(mTransactionManager);

        // Insert a change log so insertAllWithoutAccessLogs is called instead of insertAll.
        mTransactionTestUtils.insertChangeLog();

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
                        DEFAULT_USER_HANDLE);
        verify(mNotificationSender, times(1))
                .sendNotificationAsUser(
                        ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE,
                        DEFAULT_USER_HANDLE);

        List<UUID> stepsUuids = ImmutableList.of(UUID.fromString(uuids.get(0)));
        List<UUID> bloodPressureUuids = ImmutableList.of(UUID.fromString(uuids.get(1)));

        List<RecordInternal<?>> records =
                mTransactionTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                stepsUuids,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                bloodPressureUuids));
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getUuid()).isEqualTo(stepsUuids.get(0));
        assertThat(records.get(1).getUuid()).isEqualTo(bloodPressureUuids.get(0));
        assertThat(mExportImportSettingsStorage.getImportStatus().getDataImportState())
                .isEqualTo(DATA_IMPORT_ERROR_NONE);
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void copiesAllData_changeLogsTokenExists_generateChangeLogs() throws Exception {
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(123, 345, 100));
        File zipToImport = zipExportedDb(exportCurrentDb());
        mDatabaseHelpers.clearAllData(mTransactionManager);

        // Insert a change log.
        mTransactionTestUtils.insertChangeLog();

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        assertThat(mTransactionTestUtils.queryNumEntries(ChangeLogsHelper.TABLE_NAME)).isEqualTo(1);
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void copiesAllData_noChangeLogsToken_noChangeLogs() throws Exception {
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(123, 345, 100));
        File zipToImport = zipExportedDb(exportCurrentDb());
        mDatabaseHelpers.clearAllData(mTransactionManager);

        mImportManagerSpy.runImport(mContext.getUser(), Uri.fromFile(zipToImport));

        assertThat(mTransactionTestUtils.queryNumEntries(ChangeLogsHelper.TABLE_NAME)).isEqualTo(0);
    }

    private File exportCurrentDb() throws Exception {
        File originalDb = mTransactionManager.getDatabasePath();
        File dbToImport =
                new File(mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE), "export.db");
        Files.copy(originalDb.toPath(), dbToImport.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return dbToImport;
    }

    private File zipExportedDb(File dbToImport) throws Exception {
        File zipToImport =
                new File(mContext.getDir(TEST_DIRECTORY_NAME, Context.MODE_PRIVATE), "export.zip");
        Compressor.compress(dbToImport, LOCAL_EXPORT_DATABASE_FILE_NAME, zipToImport);
        return zipToImport;
    }

    private static File createTextFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Contents of file " + fileName);
        fileWriter.close();
        return file;
    }

    private int intSizeInKb(File file) {
        return (int) (file.length() / 1024.0);
    }
}
