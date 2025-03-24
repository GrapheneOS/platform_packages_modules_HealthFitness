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

package android.healthconnect.tests.backuprestore;

import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.PermissionHelper.getHealthDataHistoricalAccessStartDate;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermission;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissionsWithDelay;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.getHealthConnectDataRestoreState;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readAllRecords;
import static android.healthconnect.cts.utils.TestUtils.readRecords;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.android.compatibility.common.util.BackupUtils.LOCAL_TRANSPORT_TOKEN;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.HOURS;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.InstantRecord;
import android.health.connect.datatypes.IntervalRecord;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Energy;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.utils.DataFactory;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.BackupUtils;
import com.android.healthfitness.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@AppModeFull
public class BackupRestoreE2ETest {
    /** Whether to print out logs in tests for debugging purposes. */
    private static final boolean IS_DEBUGGING_TEST = false;

    private static final String LOG_TAG = BackupRestoreE2ETest.class.getName();

    private static final long ASSERT_TIMEOUT_MILLIS = 20_000;
    private static final long ASSERT_RETRY_INTERVAL_MILLIS = 100;

    /** A permission that HC BR APK declares. This is used to find the package name of that APK. */
    private static final String HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION =
            "android.permission.HEALTH_CONNECT_BACKUP_INTER_AGENT";

    private static final String TEST_APP_1_PACKAGE_NAME = "android.healthconnect.cts.app";
    private static final String TEST_APP_2_PACKAGE_NAME = "android.healthconnect.cts.app2";
    private static final String TEST_APP_DECLARED_PERMISSION =
            "android.permission.health.READ_HEIGHT";

    private static final int MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST = 500;

    private final BackupUtils mBackupUtils =
            new BackupUtils() {
                @Override
                protected InputStream executeShellCommand(String command) {
                    final ParcelFileDescriptor pfd =
                            InstrumentationRegistry.getInstrumentation()
                                    .getUiAutomation()
                                    .executeShellCommand(command);
                    return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                }
            };
    private final Context mContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private PhrCtsTestUtils mPhrTestUtil;

    private String mBackupRestoreApkPackageName;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() throws Exception {
        if (!DeviceSupportUtils.isHealthConnectFullySupported()) {
            return;
        }
        mBackupRestoreApkPackageName = getBackupRestoreApkPackageName();
        // enable backup on the test device
        mBackupUtils.enableBackupForUser(true, UserHandle.myUserId());
        // switch backup transport to local
        mBackupUtils.setBackupTransportForUser(
                mBackupUtils.getLocalTransportName(), UserHandle.myUserId());
        // enable D2D backup flag so HealthConnectBackupAgent includes DB file in the backup file
        // list
        Settings.Secure.putString(
                mContext.getContentResolver(),
                "backup_local_transport_parameters",
                "is_device_transfer=true");

        deleteAllStagedRemoteData();
        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        mPhrTestUtil = new PhrCtsTestUtils(TestUtils.getHealthConnectManager());
    }

    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_PERSONAL_HEALTH_RECORD,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_ENABLE_D2D_AND_EXPORT_IMPORT
    })
    @RequiresFlagsDisabled(Flags.FLAG_PERSONAL_HEALTH_RECORD_DISABLE_D2D)
    public void testBackupThenRestore_over5000MedicalResources_expectDataIsRestoredCorrectly()
            throws Exception {
        if (!DeviceSupportUtils.isHealthConnectFullySupported()) {
            return;
        }

        // Insert records.
        int numOfRecords = 100;
        List<Record> insertedRecords =
                insertRecordsWithChunking(
                        this::getCompleteActiveCaloriesBurnedRecord, numOfRecords);
        assertThat(insertedRecords).hasSize(numOfRecords);
        // Insert medical resources.
        int numOfMedicalResources = 7000;
        String dataSourceId =
                mPhrTestUtil.createDataSource(getCreateMedicalDataSourceRequest("1")).getId();
        List<MedicalResource> medicalResources =
                mPhrTestUtil.upsertVaccineMedicalResources(dataSourceId, numOfMedicalResources);
        assertThat(medicalResources).hasSize(numOfMedicalResources);

        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        // Delete records.
        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        readAndAssertRecordsNotExistUsingIds(insertedRecords);
        // Delete medical data source that would delete all of the medical resources
        // with it.
        mPhrTestUtil.deleteMedicalDataSourceWithData(dataSourceId);
        readMedicalResourcesAndAssertEmpty(medicalResources);

        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        assertWithTimeout(() -> readAndAssertMedicalResourcesExistUsingIds(medicalResources));
        assertWithTimeout(() -> readAndAssertRecordsExistUsingIds(insertedRecords));
        log("Data Restore state = " + getHealthConnectDataRestoreState());
    }

    @Test
    public void testBackupThenRestore_over2000Records_expectDataIsRestoredCorrectly()
            throws Exception {
        if (!DeviceSupportUtils.isHealthConnectFullySupported()) {
            return;
        }
        int numOfRecords = 2050;
        List<Record> insertedRecords =
                insertRecordsWithChunking(
                        this::getCompleteActiveCaloriesBurnedRecord, numOfRecords);
        assertThat(insertedRecords).hasSize(numOfRecords);

        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        readAndAssertRecordsNotExistUsingIds(insertedRecords);

        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        assertWithTimeout(() -> readAndAssertRecordsExistUsingIds(insertedRecords));
        log("Data Restore state = " + getHealthConnectDataRestoreState());
    }

    @Test
    public void testBackupThenRestore_trainingPlans_expectDataIsRestoredCorrectly()
            throws Exception {
        if (!DeviceSupportUtils.isHealthConnectFullySupported()) {
            return;
        }
        DataOrigin dataOrigin = DataFactory.getDataOrigin(mContext.getPackageName());
        Metadata.Builder metadataBuilder = new Metadata.Builder();
        metadataBuilder.setDataOrigin(dataOrigin);
        List<Record> insertedTrainingPlans = new ArrayList<>();
        List<Record> insertedExercises = new ArrayList<>();
        PlannedExerciseSessionRecord.Builder trainingPlan =
                DataFactory.plannedExerciseSession(metadataBuilder.build());
        PlannedExerciseSessionRecord insertedTrainingPlan =
                (PlannedExerciseSessionRecord) insertRecords(List.of(trainingPlan.build())).get(0);
        assertThat(insertedTrainingPlan)
                .isEqualTo(trainingPlan.setMetadata(insertedTrainingPlan.getMetadata()).build());
        insertedTrainingPlans.add(insertedTrainingPlan);
        assertThat(insertedTrainingPlan.getMetadata().getId()).isNotNull();

        ExerciseSessionRecord.Builder exerciseSessionRecordBuilder =
                new ExerciseSessionRecord.Builder(
                        metadataBuilder.build(),
                        Instant.now().minus(3, HOURS),
                        Instant.now().minus(1, HOURS),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        exerciseSessionRecordBuilder.setPlannedExerciseSessionId(
                insertedTrainingPlan.getMetadata().getId());
        ExerciseSessionRecord insertedExerciseSession =
                (ExerciseSessionRecord)
                        insertRecords(List.of(exerciseSessionRecordBuilder.build())).get(0);
        insertedExercises.add(insertedExerciseSession);
        // We need to read out the training plan again - this is because the insertion of the
        // exercise session has augmented it with a UUID reference to that session.
        PlannedExerciseSessionRecord trainingPlanWithAutogeneratedReferenceToExerciseSession =
                readAllRecords(PlannedExerciseSessionRecord.class).get(0);
        assertThat(
                        trainingPlanWithAutogeneratedReferenceToExerciseSession
                                .getCompletedExerciseSessionId())
                .isEqualTo(insertedExerciseSession.getMetadata().getId());

        // Proceed with backup.
        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        // Delete the now backed up records from the database.
        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        readAndAssertRecordsNotExistUsingIds(insertedTrainingPlans);
        readAndAssertRecordsNotExistUsingIds(insertedExercises);

        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        assertWithTimeout(
                () -> {
                    List<?> records = readRecordsUsingIds(insertedTrainingPlans);
                    PlannedExerciseSessionRecord restoredTrainingPlan =
                            (PlannedExerciseSessionRecord) records.get(0);
                    assertThat(trainingPlanWithAutogeneratedReferenceToExerciseSession)
                            .isEqualTo(restoredTrainingPlan);
                });
        assertWithTimeout(
                () -> {
                    List<?> records = readRecordsUsingIds(insertedExercises);
                    ExerciseSessionRecord restoredExercise = (ExerciseSessionRecord) records.get(0);
                    assertThat(restoredExercise).isEqualTo(insertedExerciseSession);
                });
        log("Data Restore state = " + getHealthConnectDataRestoreState());
    }

    @Test
    public void testPermissionRestoredBeforeHCRestore_expectGrantTimeIsRestoredCorrectly()
            throws Exception {
        // revoke all permissions for both test apps to remove all stored grant time as setup step
        revokeAllHealthPermissionsWithDelay(TEST_APP_1_PACKAGE_NAME, "");
        revokeAllHealthPermissionsWithDelay(TEST_APP_2_PACKAGE_NAME, "");

        // grant a permission to test app 1 to create grant time
        grantHealthPermission(TEST_APP_1_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);
        Instant historicAccessStartDate =
                assertWithTimeoutAndReturn(
                        () -> {
                            Instant result =
                                    getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME);
                            assertThat(result).isNotNull();
                            return result;
                        });

        // trigger backup, only test app 1 has grant time now, so the staged backup file should
        // contains only grant time for test app 1, not test app 2
        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        // grant permissions to simulate the case where permission controller module is restored
        // before HC module.
        Thread.sleep(1000); // add some delay so second grant time is definitely different
        revokeAllHealthPermissions(TEST_APP_1_PACKAGE_NAME, "");
        grantHealthPermission(TEST_APP_1_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);
        grantHealthPermission(TEST_APP_2_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);
        assertWithTimeout(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isNotNull());
        Instant historicAccessStartDate2 =
                assertWithTimeoutAndReturn(
                        () -> {
                            Instant result =
                                    getHealthDataHistoricalAccessStartDate(TEST_APP_2_PACKAGE_NAME);
                            assertThat(result).isNotNull();
                            return result;
                        });
        // trigger restore, now the staged backup file which contains grant time for test app 1
        // should override ONLY the newly created grant time of test app 1, but not the
        // test app 2's.
        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        // assert that test app 1's grant time is restored correctly
        assertWithTimeout(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isEqualTo(historicAccessStartDate));
        // assert that test app 2's grant time stay the same
        assertWithTimeout(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_2_PACKAGE_NAME))
                                .isEqualTo(historicAccessStartDate2));
    }

    @Test
    public void testPermissionsRestoredAfterHCRestore_expectGrantTimeIsRestoredCorrectly()
            throws Exception {
        // revoke all permissions for the test app to remove all stored grant time as setup step
        revokeAllHealthPermissionsWithDelay(TEST_APP_1_PACKAGE_NAME, "");

        // grant a permission to test app to create grant time
        grantHealthPermission(TEST_APP_1_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);
        Instant historicAccessStartDate =
                assertWithTimeoutAndReturn(
                        () -> {
                            Instant result =
                                    getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME);
                            assertThat(result).isNotNull();
                            return result;
                        });

        // trigger backup, the staged backup file should contains  grant time for test app 1.
        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());
        Thread.sleep(1000); // add some delay so second grant time is definitely different

        // revoke all permissions and then restore the grant time file.
        revokeAllHealthPermissions(TEST_APP_1_PACKAGE_NAME, "");
        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        // Since permissions haven't been restored, expect null access date.
        assertWithTimeout(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isNull());

        // grant permissions to simulate the case where permission controller module is restored
        // after HC module.
        grantHealthPermission(TEST_APP_1_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);

        // assert that test app 1's grant time is restored correctly
        assertWithTimeout(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isEqualTo(historicAccessStartDate));
    }

    private ActiveCaloriesBurnedRecord getCompleteActiveCaloriesBurnedRecord(long i) {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin = DataFactory.getDataOrigin(mContext.getPackageName());
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);
        testMetadataBuilder.setClientRecordId("ClientRecordId" + UUID.randomUUID());

        Instant now = Instant.now();
        ZoneOffset zoneOffset = ZoneOffset.systemDefault().getRules().getOffset(now);
        return new ActiveCaloriesBurnedRecord.Builder(
                        testMetadataBuilder.build(),
                        now.minusMillis(i * 10),
                        now.minusMillis(i * 10 - 1000),
                        Energy.fromCalories(10.0))
                .setStartZoneOffset(zoneOffset)
                .setEndZoneOffset(zoneOffset)
                .build();
    }

    private void readMedicalResourcesAndAssertEmpty(List<MedicalResource> medicalResources)
            throws InterruptedException {
        List<MedicalResource> result = mPhrTestUtil.readMedicalResources(medicalResources);

        assertThat(result).isEmpty();
    }

    private void readAndAssertMedicalResourcesExistUsingIds(List<MedicalResource> medicalResources)
            throws InterruptedException {
        List<MedicalResource> result = mPhrTestUtil.readMedicalResources(medicalResources);

        assertThat(result.size()).isEqualTo(medicalResources.size());
        assertThat(result).containsExactlyElementsIn(medicalResources);
    }

    private static void readAndAssertRecordsExistUsingIds(List<Record> insertedRecords)
            throws InterruptedException {
        String logTag = "readAndAssertRecordsExistUsingIds";
        logRecords(logTag + " INPUT", insertedRecords);

        List<? extends Record> result = readRecordsUsingIds(insertedRecords);

        logRecords(logTag + " OUTPUT", result);
        assertThat(result).hasSize(insertedRecords.size());
        assertThat(result).containsExactlyElementsIn(insertedRecords);
    }

    private static void readAndAssertRecordsNotExistUsingIds(List<Record> insertedRecords)
            throws InterruptedException {
        String logTag = "readAndAssertRecordsNotExistUsingIds";
        logRecords(logTag + " INPUT", insertedRecords);

        List<? extends Record> result = readRecordsUsingIds(insertedRecords);

        logRecords(logTag + " OUTPUT", result);
        assertThat(result).isEmpty();
    }

    private static List<? extends Record> readRecordsUsingIds(List<Record> insertedRecords)
            throws InterruptedException {
        Class<? extends Record> clazz = insertedRecords.get(0).getClass();
        for (Record record : insertedRecords) {
            if (record.getClass() != clazz) {
                throw new IllegalArgumentException("Records must all be of the same type.");
            }
        }
        ReadRecordsRequestUsingIds.Builder<? extends Record> requestBuilder =
                new ReadRecordsRequestUsingIds.Builder<>(clazz);
        for (Record record : insertedRecords) {
            requestBuilder.addId(record.getMetadata().getId());
        }

        return readRecords(requestBuilder.build());
    }

    /**
     * Chunking is needed otherwise the insertion will fail with {@code
     * android.os.TransactionTooLargeException: data parcel size 1xxxxxx bytes}.
     */
    private List<Record> insertRecordsWithChunking(RecordCreator creator, int numOfRecords)
            throws InterruptedException {
        List<Record> insertedRecords = new ArrayList<>();

        for (int chunk = 0;
                chunk <= numOfRecords / MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST;
                chunk++) {
            List<Record> recordsToInsert = new ArrayList<>();

            for (int indexWithinChunk = 0;
                    indexWithinChunk < MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST;
                    indexWithinChunk++) {
                int index = chunk * MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST + indexWithinChunk;
                if (index >= numOfRecords) {
                    break;
                }

                recordsToInsert.add(creator.create(index));
            }
            if (!recordsToInsert.isEmpty()) {
                insertedRecords.addAll(insertRecords(recordsToInsert));
            }
        }

        return insertedRecords;
    }

    private interface RecordCreator {
        Record create(int index);
    }

    private String getBackupRestoreApkPackageName() {
        PackageManager packageManager = mContext.getPackageManager();
        List<PackageInfo> packageInfoList =
                packageManager.getInstalledPackages(
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        for (PackageInfo packageInfo : packageInfoList) {
            if (containsBackupRestoreInterAgentPermission(packageInfo.requestedPermissions)) {
                return packageInfo.packageName;
            }
        }
        throw new IllegalStateException("Backup Restore APK not found!");
    }

    private static boolean containsBackupRestoreInterAgentPermission(String[] array) {
        if (array == null) {
            return false;
        }
        for (String e : array) {
            if (HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION.equals(e)) {
                return true;
            }
        }
        return false;
    }

    /** Same as {@link #assertWithTimeout(Assertion)} except this method also returns a value. */
    private static <T> T assertWithTimeoutAndReturn(@NonNull AssertionWithReturnValue<T> assertion)
            throws Exception {
        long start = System.currentTimeMillis();

        while (true) {
            try {
                return assertion.run();
            } catch (Throwable e) {
                if (System.currentTimeMillis() - start < ASSERT_TIMEOUT_MILLIS) {
                    try {
                        Thread.sleep(ASSERT_RETRY_INTERVAL_MILLIS);
                    } catch (InterruptedException ignored) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Repeatedly run {@code assertion} until either it succeeds or timed out. This is needed for
     * cases such as waiting for async restore to be done.
     *
     * @param assertion The {@link Assertion assertion} to run.
     */
    private static void assertWithTimeout(@NonNull Assertion assertion) throws Exception {
        assertWithTimeoutAndReturn(
                () -> {
                    assertion.run();
                    return null;
                });
    }

    private interface AssertionWithReturnValue<T> {
        T run() throws Exception;
    }

    private interface Assertion {
        void run() throws Exception;
    }

    private static void logRecords(String tag, List<? extends Record> records) {
        if (!IS_DEBUGGING_TEST) {
            return;
        }
        log("======================" + tag + "======================");
        for (int index = 0; index < records.size(); index++) {
            log("Record " + index + ":" + recordToString(records.get(index)));
        }
    }

    private static String recordToString(Record record) {
        StringBuilder stringBuilder = new StringBuilder();
        appendToLog(stringBuilder, 0, record.getClass().getSimpleName());

        if (record instanceof ActiveCaloriesBurnedRecord) {
            appendToLog(
                    stringBuilder,
                    1,
                    "Energy: ",
                    ((ActiveCaloriesBurnedRecord) record).getEnergy().getInCalories());
        }

        if (record instanceof IntervalRecord) {
            appendToLog(stringBuilder, 1, IntervalRecord.class.getSimpleName());
            appendToLog(stringBuilder, 2, "Start Time: ", ((IntervalRecord) record).getStartTime());
            appendToLog(stringBuilder, 2, "End Time: ", ((IntervalRecord) record).getEndTime());
        } else if (record instanceof InstantRecord) {
            appendToLog(stringBuilder, 1, InstantRecord.class.getSimpleName());
            appendToLog(stringBuilder, 2, "Time: ", ((InstantRecord) record).getTime());
        }

        Metadata metadata = record.getMetadata();
        appendToLog(stringBuilder, 1, "Metadata:");
        appendToLog(stringBuilder, 2, "getId: ", metadata.getId());
        appendToLog(stringBuilder, 2, "getClientRecordId: ", metadata.getClientRecordId());
        appendToLog(stringBuilder, 2, "device model: ", metadata.getDevice().getModel());
        appendToLog(stringBuilder, 2, "data origin: ", metadata.getDataOrigin().getPackageName());
        appendToLog(stringBuilder, 2, "getLastModifiedTime: ", metadata.getLastModifiedTime());

        return stringBuilder.toString();
    }

    private static void appendToLog(
            StringBuilder stringBuilder, int numberOfSpacePrefix, Object... values) {
        stringBuilder.append("\n");
        if (numberOfSpacePrefix > 0) {
            stringBuilder.append("-");
            stringBuilder.append(" ".repeat(numberOfSpacePrefix));
        }
        for (Object value : values) {
            stringBuilder.append(value);
        }
    }

    private static void log(String msg) {
        if (IS_DEBUGGING_TEST) {
            System.out.println(LOG_TAG + ": " + msg);
        }
    }
}
