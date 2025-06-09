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

package android.healthconnect.tests.exportimport;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;
import static android.healthconnect.testing.cts.TestUtils.deleteRecords;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.readAllRecords;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newFullMetadataWithClientIdAndVersion;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.testing.cts.HealthConnectReceiver;
import android.healthconnect.testing.cts.JobUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.shared.recordfactory.RecordFactory;
import android.net.Uri;
import android.os.Environment;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.SystemUtil;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.HealthConnectContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

/** Integration test for the export/import functionality of HealthConnect service. */
@RunWith(AndroidJUnit4.class)
public class ExportImportApiTest {
    private static final String TAG = "HealthConnectExportImportApiTest";
    private static final String FILE_PROVIDER_AUTHORITY =
            "android.healthconnect.tests.exportimport.fileprovider";
    private static final String JOB_NAMESPACE = "HEALTH_CONNECT_IMPORT_EXPORT_JOBS";
    private static final String REMOTE_EXPORT_DATABASE_DIR_NAME = "export_import";
    private static final String REMOTE_EXPORT_ZIP_FILE_NAME = "remote_file.zip";
    private static final String REMOTE_EXPORT_DATABASE_FILE_NAME = "remote_file.db";
    private static final int TIMEOUT_MS = 10000;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private HealthConnectManager mHealthConnectManager;
    private HealthConnectContext mExportedDbContext;
    private Uri mRemoteExportFileUri;
    private File mExportFile;
    private PhrCtsTestUtils mPhrCtsTestUtils;

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder(mContext.getCacheDir());

    @Before
    public void setUp() throws Exception {
        mHealthConnectManager = mContext.getSystemService(HealthConnectManager.class);
        mPhrCtsTestUtils = new PhrCtsTestUtils(mHealthConnectManager);

        deleteAllDataFromHealthConnect();
        JobUtils.cancelJobIfScheduled(JOB_NAMESPACE);
        mExportedDbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        REMOTE_EXPORT_DATABASE_DIR_NAME,
                        Environment.getDataDirectory());

        mExportFile = mTemporaryFolder.newFile(REMOTE_EXPORT_ZIP_FILE_NAME);
        mRemoteExportFileUri =
                FileProvider.getUriForFile(mContext, FILE_PROVIDER_AUTHORITY, mExportFile);
    }

    @After
    public void tearDown() throws Exception {
        deleteAllDataFromHealthConnect();
        JobUtils.cancelJobIfScheduled(JOB_NAMESPACE);
        SQLiteDatabase.deleteDatabase(
                mExportedDbContext.getDatabasePath(REMOTE_EXPORT_DATABASE_FILE_NAME));
    }

    @Test
    public void exportDeleteDataAndThenImport_dataIsRestored() throws Exception {
        RecordFactory<? extends Record> recordFactory =
                RecordFactory.forDataType(StepsRecord.class);
        insertRecords(
                recordFactory.newFullRecord(
                        newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                        Instant.now().minusSeconds(2000000),
                        Instant.now().minusSeconds(1900000)),
                recordFactory.newFullRecord(
                        newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                        Instant.now().minusSeconds(1000000),
                        Instant.now().minusSeconds(900000)));
        List<StepsRecord> readRecords = readAllRecords(StepsRecord.class);
        assertThat(readRecords).isNotEmpty();

        SystemUtil.runWithShellPermissionIdentity(
                () ->
                        mHealthConnectManager.configureScheduledExport(
                                new ScheduledExportSettings.Builder()
                                        .setUri(mRemoteExportFileUri)
                                        .setPeriodInDays(1)
                                        .build()),
                MANAGE_HEALTH_DATA_PERMISSION);
        SystemUtil.eventually(
                () ->
                        assertWithMessage("The job is still not scheduled after 10 secs")
                                .that(JobUtils.isJobScheduled(JOB_NAMESPACE))
                                .isTrue(),
                TIMEOUT_MS);
        if (!Flags.immediateExport()) {
            // If immediate export is enabled, it is run before the job is scheduled anyway.
            JobUtils.runJobIfScheduled(JOB_NAMESPACE);
        }
        SystemUtil.eventually(
                () -> {
                    assertWithMessage(
                                    "Export file " + mExportFile + " was not ready or accessible.")
                            .that(isFileAccessibleForReading(mExportFile))
                            .isTrue();
                },
                TIMEOUT_MS);

        deleteRecords(readRecords);
        List<StepsRecord> stepsRecordsAfterDeletion = readAllRecords(StepsRecord.class);
        assertThat(stepsRecordsAfterDeletion).isEmpty();

        Void unused =
                HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity(
                        (executor, receiver) ->
                                mHealthConnectManager.runImport(
                                        mRemoteExportFileUri, executor, receiver),
                        MANAGE_HEALTH_DATA_PERMISSION);

        List<StepsRecord> readRecordsAfterImport = readAllRecords(StepsRecord.class);
        assertThat(readRecordsAfterImport).isEqualTo(readRecords);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT})
    public void phr_exportDeleteDataAndThenImport_dataIsRestored() throws Exception {
        // insert some medical data
        String medicalDataSourceId =
                mPhrCtsTestUtils.createDataSource(getCreateMedicalDataSourceRequest()).getId();
        List<MedicalResource> insertedMedicalResources =
                mPhrCtsTestUtils.upsertVaccineMedicalResources(medicalDataSourceId, 10);
        assertThat(
                        mPhrCtsTestUtils.readMedicalResourcesByIds(
                                insertedMedicalResources.stream()
                                        .map(MedicalResource::getId)
                                        .toList()))
                .containsExactlyElementsIn(insertedMedicalResources);

        // trigger export
        SystemUtil.runWithShellPermissionIdentity(
                () ->
                        mHealthConnectManager.configureScheduledExport(
                                new ScheduledExportSettings.Builder()
                                        .setUri(mRemoteExportFileUri)
                                        .setPeriodInDays(1)
                                        .build()),
                MANAGE_HEALTH_DATA_PERMISSION);
        SystemUtil.eventually(
                () ->
                        assertWithMessage("The job is still not scheduled after 10 secs")
                                .that(JobUtils.isJobScheduled(JOB_NAMESPACE))
                                .isTrue(),
                TIMEOUT_MS);
        if (!Flags.immediateExport()) {
            // If immediate export is enabled, it is run before the job is scheduled anyway.
            JobUtils.runJobIfScheduled(JOB_NAMESPACE);
        }
        SystemUtil.eventually(
                () -> {
                    assertWithMessage(
                                    "Export file " + mExportFile + " was not ready or accessible.")
                            .that(isFileAccessibleForReading(mExportFile))
                            .isTrue();
                },
                TIMEOUT_MS);

        // delete all medical data
        mPhrCtsTestUtils.deleteAllMedicalData();
        assertThat(mPhrCtsTestUtils.getMedicalDataSourcesByIds(List.of(medicalDataSourceId)))
                .isEmpty();
        assertThat(
                        mPhrCtsTestUtils.readMedicalResourcesByIds(
                                insertedMedicalResources.stream()
                                        .map(MedicalResource::getId)
                                        .toList()))
                .isEmpty();

        // trigger import
        Void unused =
                HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity(
                        (executor, receiver) ->
                                mHealthConnectManager.runImport(
                                        mRemoteExportFileUri, executor, receiver),
                        MANAGE_HEALTH_DATA_PERMISSION);

        // assert that exported medical data is imported correctly
        assertThat(
                        mPhrCtsTestUtils
                                .getMedicalDataSourcesByIds(List.of(medicalDataSourceId))
                                .stream()
                                .map(MedicalDataSource::getId)
                                .toList())
                .containsExactly(medicalDataSourceId);
        assertThat(
                        mPhrCtsTestUtils.readMedicalResourcesByIds(
                                insertedMedicalResources.stream()
                                        .map(MedicalResource::getId)
                                        .toList()))
                .containsExactlyElementsIn(insertedMedicalResources);
    }

    @Test
    @EnableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void exportOn_thenExportOff_noJobScheduled() throws Exception {
        SystemUtil.runWithShellPermissionIdentity(
                () ->
                        mHealthConnectManager.configureScheduledExport(
                                new ScheduledExportSettings.Builder()
                                        .setUri(mRemoteExportFileUri)
                                        .setPeriodInDays(1)
                                        .build()),
                MANAGE_HEALTH_DATA_PERMISSION);
        // TODO: b/375190993 - Improve tests (as possible) by replacing polling checks.
        SystemUtil.eventually(
                () ->
                        assertWithMessage("The job is still not scheduled after 10 secs")
                                .that(JobUtils.isJobScheduled(JOB_NAMESPACE))
                                .isTrue(),
                TIMEOUT_MS);
        JobUtils.runJobIfScheduled(JOB_NAMESPACE);

        SystemUtil.runWithShellPermissionIdentity(
                () -> {
                    mHealthConnectManager.configureScheduledExport(
                            new ScheduledExportSettings.Builder()
                                    .setUri(mRemoteExportFileUri)
                                    .setPeriodInDays(0)
                                    .build());
                },
                MANAGE_HEALTH_DATA_PERMISSION);
        // TODO: b/375190993 - Improve tests (as possible) by replacing polling checks.
        SystemUtil.eventually(
                () ->
                        assertWithMessage("The job is still scheduled after 10 secs")
                                .that(JobUtils.isJobScheduled(JOB_NAMESPACE))
                                .isFalse(),
                TIMEOUT_MS);
    }

    /** Returns true iff the file exists and is done being written. */
    private static boolean isFileAccessibleForReading(File file) {
        if (!file.exists() || !file.isFile()) {
            Log.e(TAG, "File does not exist or is not a regular file: " + file.getAbsolutePath());
            return false;
        }

        // Try to acquire a shared lock. If another process holds an exclusive write lock,
        // this should fail or block. tryLock is non-blocking.
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                FileLock lock = channel.tryLock(0L, Long.MAX_VALUE, /* shared= */ true)) {
            if (lock == null) {
                Log.i(
                        TAG,
                        "isFileAccessibleForReading: File is likely being written "
                                + "(cannot acquire shared lock): "
                                + file.getAbsolutePath());
                return false;
            }
            // Lock acquired (and shared), means no exclusive write lock is held.
            lock.release(); // Release immediately
            Log.i(
                    TAG,
                    "isFileAccessibleForReading: File exists and shared lock acquired "
                            + "(likely done writing): "
                            + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            // This can happen due to concurrent access, or if the file is exclusively
            // locked in a way that even prevents opening for shared read lock attempt.
            Log.e(
                    TAG,
                    "isFileAccessibleForReading: IOException while trying to lock underlying file "
                            + file.getAbsolutePath()
                            + " - "
                            + e.getMessage());
            return false; // Treat as "not ready"
        } catch (SecurityException se) {
            Log.e(
                    TAG,
                    "isFileAccessibleForReading: SecurityException for underlying file "
                            + file.getAbsolutePath()
                            + " - "
                            + se.getMessage());
            return false;
        }
    }

    // TODO(b/370954019): Add test for immediate export.
}
