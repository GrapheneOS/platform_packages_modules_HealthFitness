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
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermission;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.deleteRecords;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readAllRecords;
import static android.healthconnect.tests.exportimport.HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.net.Uri;
import android.os.Environment;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Slog;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.SystemUtil;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.HealthConnectContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Integration test for the export/import functionality of HealthConnect service. */
@RunWith(AndroidJUnit4.class)
public class ExportImportApiTest {
    private static final String TAG = "ExportImportApiTest";
    private static final String DEFAULT_APP_PACKAGE = "android.healthconnect.cts.app";
    private static final String DEFAULT_PERM = WRITE_STEPS;
    private static final String REMOTE_EXPORT_DATABASE_DIR_NAME = "export_import";
    private static final String REMOTE_EXPORT_ZIP_FILE_NAME = "remote_file.zip";
    private static final String REMOTE_EXPORT_DATABASE_FILE_NAME = "remote_file.db";
    private static final int SLEEP_TIME_MS = 1000;

    private static final int TIMEOUT_MS = 10000;

    private Context mContext;
    private HealthConnectManager mHealthConnectManager;
    private HealthConnectContext mExportedDbContext;
    private Uri mRemoteExportFileUri;
    private PhrCtsTestUtils mPhrCtsTestUtils;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mHealthConnectManager = mContext.getSystemService(HealthConnectManager.class);
        mPhrCtsTestUtils = new PhrCtsTestUtils(mHealthConnectManager);

        deleteAllStagedRemoteData();
        runShellCommandForHCJob("cancel -n");
        mExportedDbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        REMOTE_EXPORT_DATABASE_DIR_NAME,
                        Environment.getDataDirectory());
        // TODO(b/318484678): Improve tests using Uri from a different app.
        mRemoteExportFileUri =
                Uri.fromFile(
                        new File(mExportedDbContext.getDataDir(), REMOTE_EXPORT_ZIP_FILE_NAME));
    }

    @After
    public void tearDown() throws Exception {
        deleteAllStagedRemoteData();
        runShellCommandForHCJob("cancel -n");
        SQLiteDatabase.deleteDatabase(
                mExportedDbContext.getDatabasePath(REMOTE_EXPORT_DATABASE_FILE_NAME));
        mExportedDbContext.getDatabasePath(REMOTE_EXPORT_ZIP_FILE_NAME).delete();
    }

    @Test
    public void exportDeleteDataAndThenImport_dataIsRestored() throws Exception {
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        insertRecords(getTestStepRecords());
        List<StepsRecord> stepsRecords = readAllRecords(StepsRecord.class);
        assertThat(stepsRecords).isNotEmpty();

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
                                .that(isExportImportJobScheduled())
                                .isTrue(),
                TIMEOUT_MS);
        runShellCommandForHCJob("run -f -n");
        // TODO: b/375190993 - Improve tests (as possible) replacing sleep by conditions.
        Thread.sleep(SLEEP_TIME_MS);

        deleteRecords(stepsRecords);
        List<StepsRecord> stepsRecordsAfterDeletion = readAllRecords(StepsRecord.class);
        assertThat(stepsRecordsAfterDeletion).isEmpty();

        callAndGetResponseWithShellPermissionIdentity(
                (executor, receiver) ->
                        mHealthConnectManager.runImport(mRemoteExportFileUri, executor, receiver),
                MANAGE_HEALTH_DATA_PERMISSION);

        List<StepsRecord> stepsRecordsAfterImport = readAllRecords(StepsRecord.class);
        assertThat(stepsRecordsAfterImport).isEqualTo(stepsRecords);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_ENABLE_EXPORT_IMPORT
    })
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
                                .that(isExportImportJobScheduled())
                                .isTrue(),
                TIMEOUT_MS);
        runShellCommandForHCJob("run -f -n");
        // TODO: b/375190993 - Improve tests (as possible) replacing sleep by conditions.
        Thread.sleep(SLEEP_TIME_MS);

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
        callAndGetResponseWithShellPermissionIdentity(
                (executor, receiver) ->
                        mHealthConnectManager.runImport(mRemoteExportFileUri, executor, receiver),
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
                                .that(isExportImportJobScheduled())
                                .isTrue(),
                TIMEOUT_MS);
        runShellCommandForHCJob("run -f -n");

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
                                .that(isExportImportJobScheduled())
                                .isFalse(),
                TIMEOUT_MS);
    }

    // TODO(b/370954019): Add test for immediate export.

    private boolean isExportImportJobScheduled() throws Exception {
        String dumpsysOutput = TestUtils.runShellCommand("dumpsys jobscheduler");
        return dumpsysOutput.contains("HEALTH_CONNECT_IMPORT_EXPORT_JOBS:");
    }

    private void runShellCommandForHCJob(String command) throws Exception {
        String dumpsysOutput = TestUtils.runShellCommand("dumpsys jobscheduler");
        if (!isExportImportJobScheduled()) {
            Slog.i(TAG, "No HC jobs scheduled!");
            return;
        }

        String filteredOutput =
                dumpsysOutput.substring(
                        dumpsysOutput.indexOf("HEALTH_CONNECT_IMPORT_EXPORT_JOBS"),
                        dumpsysOutput.indexOf("HEALTH_CONNECT_IMPORT_EXPORT_JOBS") + 100);
        String jobId =
                filteredOutput.substring(
                        filteredOutput.indexOf("/") + 1, filteredOutput.indexOf(": "));
        String commandOutput =
                TestUtils.runShellCommand(
                        String.format(
                                "cmd jobscheduler %s HEALTH_CONNECT_IMPORT_EXPORT_JOBS android %s",
                                command, jobId));
        Slog.i(TAG, "Run output: " + commandOutput);
    }

    private List<Record> getTestStepRecords() {
        Metadata.Builder metadata =
                new Metadata.Builder()
                        .setDevice(
                                new Device.Builder()
                                        .setManufacturer("google")
                                        .setModel("Pixel")
                                        .setType(1)
                                        .build())
                        .setDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(mContext.getPackageName())
                                        .build());

        return new ArrayList<>(
                Arrays.asList(
                        new StepsRecord.Builder(
                                        metadata.setId(String.valueOf(Math.random())).build(),
                                        Instant.now().minusSeconds(2000000),
                                        Instant.now().minusSeconds(1900000),
                                        10)
                                .build(),
                        new StepsRecord.Builder(
                                        metadata.setId(String.valueOf(Math.random())).build(),
                                        Instant.now().minusSeconds(1000000),
                                        Instant.now().minusSeconds(900000),
                                        10)
                                .build()));
    }
}
