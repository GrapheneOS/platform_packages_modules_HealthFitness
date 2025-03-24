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

import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_CLEARING_LOG_TABLES;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_NONE;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_UNKNOWN;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_UNSPECIFIED;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_STARTED;

import static com.android.server.healthconnect.exportimport.ExportManager.LOCAL_EXPORT_DATABASE_FILE_NAME;
import static com.android.server.healthconnect.exportimport.ExportManager.LOCAL_EXPORT_DIR_NAME;
import static com.android.server.healthconnect.exportimport.ExportManager.LOCAL_EXPORT_ZIP_FILE_NAME;
import static com.android.server.healthconnect.testing.TestUtils.queryNumEntries;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.exportimport.ScheduledExportStatus;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.net.Uri;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.modules.utils.testing.ExtendedMockitoRule.MockStatic;
import com.android.modules.utils.testing.ExtendedMockitoRule.MockStaticClasses;
import com.android.server.healthconnect.exportimport.ExportManager.ErrorReporter;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.testing.fakes.FakePreferenceHelper;
import com.android.server.healthconnect.testing.storage.PhrTestUtils;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@RunWith(AndroidJUnit4.class)
public class ExportManagerTest {
    private static final String TEST_PACKAGE_NAME = "package.name";
    private static final String REMOTE_EXPORT_DATABASE_DIR_NAME = "remote";
    private static final String REMOTE_EXPORT_ZIP_FILE_NAME = "remote_file.zip";
    private static final String REMOTE_EXPORT_DATABASE_FILE_NAME = "remote_file.db";

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule(order = 2)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this).setStrictness(Strictness.LENIENT).build();

    @Rule public final TemporaryFolder mEnvironmentDataDirectory = new TemporaryFolder();

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private Context mContext;
    private TransactionTestUtils mTransactionTestUtils;
    private ExportManager mExportManager;
    private HealthConnectContext mExportedDbContext;
    private Instant mTimeStamp;
    private ExportImportSettingsStorage mExportImportSettingsStorage;
    private PhrTestUtils mPhrTestUtils;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private ExportImportLogger mExportImportLogger;
    @Mock private ErrorReporter mErrorReporter;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPreferenceHelper(new FakePreferenceHelper())
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                        .build();
        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        TransactionManager transactionManager = healthConnectInjector.getTransactionManager();

        mTimeStamp = Instant.parse("2024-06-04T16:39:12Z");
        Clock fakeClock = Clock.fixed(mTimeStamp, ZoneId.of("UTC"));

        mExportImportSettingsStorage = healthConnectInjector.getExportImportSettingsStorage();
        mExportManager =
                new ExportManager(
                        mContext,
                        fakeClock,
                        mExportImportSettingsStorage,
                        transactionManager,
                        healthConnectInjector.getExportImportNotificationSender(),
                        healthConnectInjector.getEnvironmentDataDirectory(),
                        mExportImportLogger,
                        mErrorReporter);

        mPhrTestUtils = new PhrTestUtils(healthConnectInjector);

        mExportedDbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        REMOTE_EXPORT_DATABASE_DIR_NAME,
                        mEnvironmentDataDirectory.getRoot());
        configureExportUri();
    }

    @After
    public void tearDown() throws Exception {
        SQLiteDatabase.deleteDatabase(
                mExportedDbContext.getDatabasePath(REMOTE_EXPORT_DATABASE_FILE_NAME));
        mExportedDbContext.getDatabasePath(REMOTE_EXPORT_ZIP_FILE_NAME).delete();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_PERSONAL_HEALTH_RECORD,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT
    })
    public void testWhenPhrExportImportEnableFlagIsEnabled_tableContentIsExported()
            throws Exception {
        /*
         * When FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT is enabled, regardless whether
         * FLAG_PERSONAL_HEALTH_RECORD_DISABLE_EXPORT_IMPORT is enabled or disabled, PHR data should
         * still be exported.
         */
        MedicalDataSource dataSource =
                mPhrTestUtils.insertR4MedicalDataSource("ds", TEST_PACKAGE_NAME);
        mPhrTestUtils.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_resource_table")).isEqualTo(1);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_resource_indices_table"))
                .isEqualTo(1);

        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();

        decompressExportedZip();
        try (HealthConnectDatabase exportedDatabase =
                new HealthConnectDatabase(mExportedDbContext, REMOTE_EXPORT_DATABASE_FILE_NAME)) {
            assertThat(queryNumEntries(exportedDatabase, "medical_data_source_table")).isEqualTo(1);
            assertThat(queryNumEntries(exportedDatabase, "medical_resource_table")).isEqualTo(1);
            assertThat(queryNumEntries(exportedDatabase, "medical_resource_indices_table"))
                    .isEqualTo(1);
        }
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    @DisableFlags({
        Flags.FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DISABLE_EXPORT_IMPORT
    })
    public void
            testWhenBothPhrExportImportEnableFlagAndDisableFlagAreDisabled_tableContentIsExported()
                    throws Exception {
        /*
         * When FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT is disabled, and if
         * FLAG_PERSONAL_HEALTH_RECORD_DISABLE_EXPORT_IMPORT is also disabled, then PHR data should
         * still be exported.
         */
        MedicalDataSource dataSource =
                mPhrTestUtils.insertR4MedicalDataSource("ds", TEST_PACKAGE_NAME);
        mPhrTestUtils.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_resource_table")).isEqualTo(1);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_resource_indices_table"))
                .isEqualTo(1);

        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();

        decompressExportedZip();
        try (HealthConnectDatabase exportedDatabase =
                new HealthConnectDatabase(mExportedDbContext, REMOTE_EXPORT_DATABASE_FILE_NAME)) {
            assertThat(queryNumEntries(exportedDatabase, "medical_data_source_table")).isEqualTo(1);
            assertThat(queryNumEntries(exportedDatabase, "medical_resource_table")).isEqualTo(1);
            assertThat(queryNumEntries(exportedDatabase, "medical_resource_indices_table"))
                    .isEqualTo(1);
        }
    }

    @Test
    @EnableFlags({
        Flags.FLAG_PERSONAL_HEALTH_RECORD,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DISABLE_EXPORT_IMPORT
    })
    @DisableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT})
    public void
            testPhrExportImportEnableFlagIsDisableAndDisableFlagIsEnabled_deletesPhrTablesContent()
                    throws Exception {
        /*
         * When FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT is disabled, and if
         * FLAG_PERSONAL_HEALTH_RECORD_DISABLE_EXPORT_IMPORT is enabled, then PHR data should
         * be deleted and not exported.
         */
        MedicalDataSource dataSource =
                mPhrTestUtils.insertR4MedicalDataSource("ds", TEST_PACKAGE_NAME);
        mPhrTestUtils.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_resource_table")).isEqualTo(1);
        assertThat(mTransactionTestUtils.queryNumEntries("medical_resource_indices_table"))
                .isEqualTo(1);

        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();

        decompressExportedZip();
        try (HealthConnectDatabase exportedDatabase =
                new HealthConnectDatabase(mExportedDbContext, REMOTE_EXPORT_DATABASE_FILE_NAME)) {
            assertThat(queryNumEntries(exportedDatabase, "medical_data_source_table")).isEqualTo(0);
            assertThat(queryNumEntries(exportedDatabase, "medical_resource_table")).isEqualTo(0);
            assertThat(queryNumEntries(exportedDatabase, "medical_resource_indices_table"))
                    .isEqualTo(0);
        }
    }

    @Test
    public void deletesAccessLogsTableContent() throws Exception {
        mTransactionTestUtils.insertAccessLog();
        mTransactionTestUtils.insertAccessLog();
        assertThat(mTransactionTestUtils.queryNumEntries("access_logs_table")).isEqualTo(2);

        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();

        decompressExportedZip();
        try (HealthConnectDatabase exportedDatabase =
                new HealthConnectDatabase(mExportedDbContext, REMOTE_EXPORT_DATABASE_FILE_NAME)) {
            assertThat(queryNumEntries(exportedDatabase, "access_logs_table")).isEqualTo(0);
        }
    }

    @Test
    public void testTimeToSuccess_loggedCorrectly() {
        // Set start time to 2s before the fixed clock to emulate time that passed
        long exportStartTime = mTimeStamp.toEpochMilli() - 2000;
        mExportManager.recordSuccess(exportStartTime, 100, 50, Uri.parse("uri"));

        assertSuccessRecorded(Instant.parse("2024-06-04T16:39:12Z"), 2000, 100, 50);
    }

    @Test
    public void testTimeToError_loggedCorrectly() {
        // Set start time to 2s before the fixed clock to emulate time that passed
        long exportStartTime = mTimeStamp.toEpochMilli() - 2000;
        mExportManager.recordError(DATA_EXPORT_ERROR_UNKNOWN, exportStartTime, 100, 50);

        assertErrorStatusStored(DATA_EXPORT_ERROR_UNKNOWN, Instant.parse("2024-06-04T16:39:12Z"));
        verify(mExportImportLogger, times(1))
                .logExportStatus(
                        eq(DATA_EXPORT_ERROR_UNKNOWN),
                        eq(/* timeToError= */ 2000),
                        eq(/* originalFileSizeKb= */ 100),
                        eq(/* compressedFileSizeKb= */ 50));
    }

    @Test
    public void deletesChangeLogsTableContent() throws Exception {
        mTransactionTestUtils.insertChangeLog();
        mTransactionTestUtils.insertChangeLog();
        assertThat(mTransactionTestUtils.queryNumEntries("change_logs_table")).isEqualTo(2);

        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();

        decompressExportedZip();
        try (HealthConnectDatabase exportedDatabase =
                new HealthConnectDatabase(mExportedDbContext, REMOTE_EXPORT_DATABASE_FILE_NAME)) {
            assertThat(queryNumEntries(exportedDatabase, "change_logs_table")).isEqualTo(0);
        }
    }

    @Test
    public void runExport_whenCompleted_deletesLocalCopies() {
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(123, 456, 7));
        assertThat(mTransactionTestUtils.queryNumEntries("steps_record_table")).isEqualTo(1);

        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();

        HealthConnectContext hcContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        LOCAL_EXPORT_DIR_NAME,
                        mEnvironmentDataDirectory.getRoot());
        assertThat(hcContext.getDatabasePath(LOCAL_EXPORT_DATABASE_FILE_NAME).exists()).isFalse();
        assertThat(hcContext.getDatabasePath(LOCAL_EXPORT_ZIP_FILE_NAME).exists()).isFalse();
    }

    @Test
    @MockStaticClasses({@MockStatic(Files.class)})
    public void runExport_localExportFails_logsWithGenericError() throws IOException {
        when(Files.copy((Path) any(), any(), any())).thenThrow(new IOException("Copy failed"));

        assertThat(mExportManager.runExport(mContext.getUser())).isFalse();

        // Time not recorded due to fake clock.
        assertErrorStatusStored(DATA_EXPORT_ERROR_UNKNOWN, mTimeStamp);
        verify(mExportImportLogger, times(1))
                .logExportStatus(
                        eq(ScheduledExportStatus.DATA_EXPORT_ERROR_UNKNOWN),
                        eq(/* timeToError= */ 0),
                        /* originalFileSizeKb= */ anyInt(),
                        /* compressedFileSizeKb= */ anyInt());
        verify(mErrorReporter, times(1)).failed(eq(ErrorReporter.LOCAL_FILE), any(), anyInt());
    }

    @Test
    // Compressor is mocked so no zip file will be exported.
    @MockStaticClasses({@MockStatic(Compressor.class)})
    public void runExport_noCompressedFile_logsWithGenericError() {
        assertThat(mExportManager.runExport(mContext.getUser())).isFalse();
        // Time not recorded due to fake clock.
        assertErrorStatusStored(DATA_EXPORT_ERROR_UNKNOWN, mTimeStamp);
        verify(mExportImportLogger, times(1))
                .logExportStatus(
                        eq(ScheduledExportStatus.DATA_EXPORT_ERROR_UNKNOWN),
                        eq(/* timeToError= */ 0),
                        /* originalFileSizeKb= */ anyInt(),
                        /* compressedFileSizeKb= */ anyInt());
        verify(mErrorReporter, times(1)).failed(eq(ErrorReporter.URI_EXPORT), any(), anyInt());
    }

    @Test
    public void deleteLocalExportFiles_deletesLocalCopies() {
        HealthConnectContext hcContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        LOCAL_EXPORT_DIR_NAME,
                        mEnvironmentDataDirectory.getRoot());
        new File(hcContext.getDataDir(), LOCAL_EXPORT_DATABASE_FILE_NAME).mkdirs();
        new File(hcContext.getDataDir(), LOCAL_EXPORT_ZIP_FILE_NAME).mkdirs();

        mExportManager.deleteLocalExportFiles(mContext.getUser());

        assertThat(hcContext.getDatabasePath(LOCAL_EXPORT_DATABASE_FILE_NAME).exists()).isFalse();
        assertThat(hcContext.getDatabasePath(LOCAL_EXPORT_ZIP_FILE_NAME).exists()).isFalse();
    }

    @Test
    public void makesRemoteCopyOfDatabase() throws Exception {
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(123, 456, 7));
        assertThat(mTransactionTestUtils.queryNumEntries("steps_record_table")).isEqualTo(1);

        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();

        decompressExportedZip();
        try (HealthConnectDatabase exportedDatabase =
                new HealthConnectDatabase(mExportedDbContext, REMOTE_EXPORT_DATABASE_FILE_NAME)) {
            assertThat(queryNumEntries(exportedDatabase, "steps_record_table")).isEqualTo(1);
        }
    }

    @Test
    public void destinationUriDoesNotExist_exportFailsWithLostFileAccessError() {
        // Inserting multiple rows to vary the size for testing of size logging
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(123, 456, 7));
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(124, 457, 7));
        assertThat(mTransactionTestUtils.queryNumEntries("steps_record_table")).isEqualTo(2);

        mExportImportSettingsStorage.setLastExportError(
                ScheduledExportStatus.DATA_EXPORT_ERROR_NONE, mTimeStamp);
        // Set export location to inaccessible directory.
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder()
                        .setUri(Uri.fromFile(new File("inaccessible")))
                        .build());

        assertThat(mExportManager.runExport(mContext.getUser())).isFalse();
        assertExportStartRecorded();

        // time not recorded due to fake clock
        assertErrorStatusStored(ScheduledExportStatus.DATA_EXPORT_LOST_FILE_ACCESS, mTimeStamp);
        verify(mExportImportLogger, times(1))
                .logExportStatus(
                        eq(ScheduledExportStatus.DATA_EXPORT_LOST_FILE_ACCESS),
                        eq(/* timeToError= */ 0),
                        /* originalFileSizeKb= */ anyInt(),
                        /* compressedFileSizeKb= */ anyInt());
        verify(mErrorReporter, times(1))
                .failed(eq(ErrorReporter.LOST_EXPORT_LOCATION_ACCESS), any(), anyInt());
    }

    @Test
    public void updatesLastSuccessfulExport_onSuccessOnly() throws Exception {
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(123, 456, 7));
        assertThat(mTransactionTestUtils.queryNumEntries("steps_record_table")).isEqualTo(1);

        // running a successful export records a "last successful export"
        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();
        assertExportStartRecorded();

        // Get the actual size of the files rather than using a fixed size as the size isn't fixed
        // across test runs.
        decompressExportedZip();
        int originalFileSizeInKb =
                getFileSizeInKb(
                        mExportedDbContext.getDatabasePath(REMOTE_EXPORT_DATABASE_FILE_NAME));
        int compressedFileSizeInKb =
                getFileSizeInKb(mExportedDbContext.getDatabasePath(REMOTE_EXPORT_ZIP_FILE_NAME));

        // time not recorded due to fake clock
        assertSuccessRecorded(
                Instant.parse("2024-06-04T16:39:12Z"),
                0,
                originalFileSizeInKb,
                compressedFileSizeInKb);

        // Export running at a later time with an error
        mTimeStamp = Instant.parse("2024-12-12T16:39:12Z");
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder()
                        .setUri(Uri.fromFile(new File("inaccessible")))
                        .build());
        assertThat(mExportManager.runExport(mContext.getUser())).isFalse();

        // Last successful export should hold the previous timestamp as the last export failed
        Instant lastSuccessfulExport =
                mExportImportSettingsStorage
                        .getScheduledExportStatus(mContext)
                        .getLastSuccessfulExportTime();
        assertThat(lastSuccessfulExport).isEqualTo(Instant.parse("2024-06-04T16:39:12Z"));
    }

    @Test
    public void updatesLastExportFileName_onSuccessOnly() {
        Context context = mock(Context.class);
        ContentResolver contentResolver = mock(ContentResolver.class);
        Cursor cursor = mock(Cursor.class);
        when(context.getContentResolver()).thenReturn(contentResolver);
        when(contentResolver.query(any(), any(), any(), any(), any())).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getString(anyInt())).thenReturn(REMOTE_EXPORT_ZIP_FILE_NAME);

        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, createStepsRecord(123, 456, 7));
        assertThat(mTransactionTestUtils.queryNumEntries("steps_record_table")).isEqualTo(1);

        // Running a successful export records a "last successful export".
        assertThat(mExportManager.runExport(mContext.getUser())).isTrue();
        assertThat(
                        mExportImportSettingsStorage
                                .getScheduledExportStatus(context)
                                .getLastExportFileName())
                .isEqualTo(REMOTE_EXPORT_ZIP_FILE_NAME);

        // Export running at a later time with an error
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder()
                        .setUri(Uri.fromFile(new File("inaccessible")))
                        .build());
        assertThat(mExportManager.runExport(mContext.getUser())).isFalse();

        // Last successful export should hold the previous file name as the last export failed
        assertThat(
                        mExportImportSettingsStorage
                                .getScheduledExportStatus(context)
                                .getLastExportFileName())
                .isEqualTo(REMOTE_EXPORT_ZIP_FILE_NAME);
    }

    @Test
    @EnableFlags({Flags.FLAG_EXTEND_EXPORT_IMPORT_TELEMETRY})
    public void repeatErrorCount_updatedCorrectly() {
        assertThat(mExportImportSettingsStorage.getLastExportError())
                .isEqualTo(DATA_EXPORT_ERROR_UNSPECIFIED);
        assertThat(mExportImportSettingsStorage.getExportRepeatErrorOnRetryCount()).isEqualTo(0);

        // The first error isn't counted as a repeat error
        mExportManager.recordError(DATA_EXPORT_ERROR_UNKNOWN, mTimeStamp.toEpochMilli(), 100, 50);
        assertThat(mExportImportSettingsStorage.getLastExportError())
                .isEqualTo(DATA_EXPORT_ERROR_UNKNOWN);
        assertThat(mExportImportSettingsStorage.getExportRepeatErrorOnRetryCount()).isEqualTo(0);

        // If there is more than one of the same error, count the repeat errors
        mExportManager.recordError(
                DATA_EXPORT_ERROR_UNKNOWN, Instant.now().toEpochMilli(), 100, 50);
        mExportManager.recordError(
                DATA_EXPORT_ERROR_UNKNOWN, Instant.now().toEpochMilli(), 100, 50);
        assertThat(mExportImportSettingsStorage.getLastExportError())
                .isEqualTo(DATA_EXPORT_ERROR_UNKNOWN);
        assertThat(mExportImportSettingsStorage.getExportRepeatErrorOnRetryCount()).isEqualTo(2);

        // If it is not a repetition of the same error, reset the repeat error count
        mExportManager.recordError(
                DATA_EXPORT_ERROR_CLEARING_LOG_TABLES, mTimeStamp.toEpochMilli(), 100, 50);
        assertThat(mExportImportSettingsStorage.getLastExportError())
                .isEqualTo(DATA_EXPORT_ERROR_CLEARING_LOG_TABLES);
        mExportManager.recordError(DATA_EXPORT_ERROR_UNKNOWN, mTimeStamp.toEpochMilli(), 100, 50);
        assertThat(mExportImportSettingsStorage.getLastExportError())
                .isEqualTo(DATA_EXPORT_ERROR_UNKNOWN);
        assertThat(mExportImportSettingsStorage.getExportRepeatErrorOnRetryCount()).isEqualTo(0);

        // If an export was successful, the next error is not considered a repeat error
        mExportManager.recordError(DATA_EXPORT_ERROR_UNKNOWN, mTimeStamp.toEpochMilli(), 100, 50);
        assertThat(mExportImportSettingsStorage.getExportRepeatErrorOnRetryCount()).isEqualTo(1);
        mExportManager.recordSuccess(mTimeStamp.toEpochMilli(), 100, 50, Uri.parse("uri"));
        assertThat(mExportImportSettingsStorage.getLastExportError())
                .isEqualTo(DATA_EXPORT_ERROR_UNSPECIFIED);
        assertThat(mExportImportSettingsStorage.getExportRepeatErrorOnRetryCount()).isEqualTo(0);
    }

    private void configureExportUri() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder()
                        .setUri(
                                Uri.fromFile(
                                        (mExportedDbContext.getDatabasePath(
                                                REMOTE_EXPORT_ZIP_FILE_NAME))))
                        .build());
    }

    private void assertExportStartRecorded() {
        verify(mExportImportLogger, times(1))
                .logExportStatus(
                        eq(DATA_EXPORT_STARTED),
                        eq(-1 /* no value recorded*/),
                        eq(-1 /* no value recorded*/),
                        eq(-1 /* no value recorded*/));
    }

    private void assertSuccessRecorded(
            Instant timeOfSuccess,
            int timeToSuccess,
            int originalFileSizeKb,
            int compressedFileSizeKb) {
        verify(mExportImportLogger, times(1))
                .logExportStatus(
                        eq(DATA_EXPORT_ERROR_NONE),
                        eq(timeToSuccess),
                        eq(originalFileSizeKb),
                        eq(compressedFileSizeKb));
        Instant lastSuccessfulExport =
                mExportImportSettingsStorage
                        .getScheduledExportStatus(mContext)
                        .getLastSuccessfulExportTime();
        assertThat(lastSuccessfulExport).isEqualTo(timeOfSuccess);
    }

    private void assertErrorStatusStored(int exportStatus, Instant timeOfError) {
        assertThat(
                        mExportImportSettingsStorage
                                .getScheduledExportStatus(mContext)
                                .getDataExportError())
                .isEqualTo(exportStatus);
        assertThat(
                        mExportImportSettingsStorage
                                .getScheduledExportStatus(mContext)
                                .getLastFailedExportTime())
                .isEqualTo(timeOfError);
    }

    private int getFileSizeInKb(File file) {
        return (int) (file.length() / 1024.0);
    }

    private void decompressExportedZip() throws IOException {
        Compressor.decompress(
                Uri.fromFile(mExportedDbContext.getDatabasePath(REMOTE_EXPORT_ZIP_FILE_NAME)),
                LOCAL_EXPORT_DATABASE_FILE_NAME,
                mExportedDbContext.getDatabasePath(REMOTE_EXPORT_DATABASE_FILE_NAME),
                mContext);
    }
}
