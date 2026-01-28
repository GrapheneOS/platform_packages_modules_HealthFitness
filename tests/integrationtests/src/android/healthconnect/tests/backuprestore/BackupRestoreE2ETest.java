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

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_PERMISSIONS;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING;
import static android.healthconnect.testing.cts.PermissionUtils.grantHealthPermission;
import static android.healthconnect.testing.cts.PermissionUtils.revokeAllHealthPermissions;
import static android.healthconnect.testing.cts.TestUtils.countAllRecords;
import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;
import static android.healthconnect.testing.cts.TestUtils.getHealthConnectDataRestoreState;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.readAllRecords;
import static android.healthconnect.testing.cts.TestUtils.readRecords;
import static android.healthconnect.testing.cts.TestUtils.verifyDeleteRecords;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.android.compatibility.common.util.BackupUtils.LOCAL_TRANSPORT_TOKEN;
import static com.android.compatibility.common.util.SystemUtil.eventually;
import static com.android.compatibility.common.util.SystemUtil.getEventually;
import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.AlcoholConsumptionRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseRoute.Location;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.InstantRecord;
import android.health.connect.datatypes.IntervalRecord;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.MenstrualCyclePhaseRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Percentage;
import android.health.connect.datatypes.units.Volume;
import android.healthconnect.testing.cts.PhrCtsTestUtils;
import android.healthconnect.testing.cts.testapphelpers.TestAppConstants;
import android.healthconnect.testing.shared.DataFactory;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.compatibility.common.util.BackupUtils;
import com.android.healthfitness.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    /** A permission that HC BR APK declares. This is used to find the package name of that APK. */
    private static final String HEALTH_CONNECT_BACKUP_INTER_AGENT_PERMISSION =
            "android.permission.HEALTH_CONNECT_BACKUP_INTER_AGENT";

    private static final String TEST_APP_1_PACKAGE_NAME =
            TestAppConstants.TEST_APP_WITH_READ_WRITE_PERMS_A;
    private static final String TEST_APP_2_PACKAGE_NAME =
            TestAppConstants.TEST_APP_WITH_READ_WRITE_PERMS_B;
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
    private final HealthConnectManager mHealthConnectManager =
            requireNonNull(mContext.getSystemService(HealthConnectManager.class));
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

        deleteAllDataFromHealthConnect();
        mPhrTestUtil = new PhrCtsTestUtils(mHealthConnectManager);
    }

    @Test
    public void testBackupThenRestore_1000MedicalResources_expectDataIsRestoredCorrectly()
            throws Exception {
        assumeTrue(DeviceSupportUtils.isHealthConnectFullySupported());

        // Insert records.
        int numOfRecords = 100;
        List<Record> insertedRecords =
                insertRecordsWithChunking(
                        this::getCompleteActiveCaloriesBurnedRecord, numOfRecords);
        assertThat(insertedRecords).hasSize(numOfRecords);
        // Insert medical resources.
        int numOfMedicalResources = 1000;
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

        eventually(
                () -> readAndAssertMedicalResourcesExistUsingIds(medicalResources),
                ASSERT_TIMEOUT_MILLIS);
        eventually(() -> readAndAssertRecordsExistUsingIds(insertedRecords), ASSERT_TIMEOUT_MILLIS);
        log("Data Restore state = " + getHealthConnectDataRestoreState());
    }

    @Test
    public void testBackupThenRestore_over2000Records_expectDataIsRestoredCorrectly()
            throws Exception {
        assumeTrue(DeviceSupportUtils.isHealthConnectFullySupported());

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

        eventually(() -> readAndAssertRecordsExistUsingIds(insertedRecords), ASSERT_TIMEOUT_MILLIS);
        log("Data Restore state = " + getHealthConnectDataRestoreState());
    }

    @Test
    public void testBackupThenRestore_trainingPlans_expectDataIsRestoredCorrectly()
            throws Exception {
        assumeTrue(DeviceSupportUtils.isHealthConnectFullySupported());

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

        eventually(
                () -> {
                    List<?> records1 = readRecordsUsingIds(insertedTrainingPlans);
                    PlannedExerciseSessionRecord restoredTrainingPlan =
                            (PlannedExerciseSessionRecord) records1.get(0);
                    assertThat(trainingPlanWithAutogeneratedReferenceToExerciseSession)
                            .isEqualTo(restoredTrainingPlan);
                },
                ASSERT_TIMEOUT_MILLIS);
        eventually(
                () -> {
                    List<?> records = readRecordsUsingIds(insertedExercises);
                    ExerciseSessionRecord restoredExercise = (ExerciseSessionRecord) records.get(0);
                    assertThat(restoredExercise).isEqualTo(insertedExerciseSession);
                },
                ASSERT_TIMEOUT_MILLIS);
        log("Data Restore state = " + getHealthConnectDataRestoreState());
    }

    // b/443928914
    @Test
    public void testBackupThenRestore_manyExerciseRoutes_expectedRestoreSuccessful()
            throws Exception {
        assumeTrue(DeviceSupportUtils.isHealthConnectFullySupported());

        // TODO(b/444399641): Increase number of sessions once LocalTransport supports larger quota.
        int numberOfSessions = 75;
        insertRecordsWithChunking(
                i -> {
                    // Create a one hour session with 1/s route.
                    Instant endTime = Instant.now().minus(Duration.ofHours(i));
                    Instant startTime = endTime.minus(Duration.ofHours(1));

                    ArrayList<Location> locations = new ArrayList<>();
                    for (Instant locationTime = startTime;
                            locationTime.isBefore(endTime);
                            locationTime = locationTime.plusSeconds(1)) {
                        locations.add(new Location.Builder(locationTime, 51.51, 0.12).build());
                    }

                    return new ExerciseSessionRecord.Builder(
                                    new Metadata.Builder().build(),
                                    startTime,
                                    endTime,
                                    EXERCISE_SESSION_TYPE_RUNNING)
                            .setRoute(new ExerciseRoute(locations))
                            .build();
                },
                numberOfSessions,
                /* chunkSize= */ 25);

        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());

        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        eventually(
                () ->
                        assertThat(countAllRecords(ExerciseSessionRecord.class))
                                .isEqualTo(numberOfSessions),
                ASSERT_TIMEOUT_MILLIS);
    }

    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION
    })
    public void testBackupThenRestore_alcoholConsumptionRecords_expectDataIsRestoredCorrectly()
            throws Exception {
        assumeTrue(DeviceSupportUtils.isHealthConnectFullySupported());

        int numOfRecords = 90;
        List<Record> insertedRecords =
                insertRecordsWithChunking(
                        (i) -> {
                            LocalDate date = LocalDate.now(ZoneId.systemDefault()).minusDays(i);
                            return new AlcoholConsumptionRecord.Builder(
                                            new Metadata.Builder().build(),
                                            date,
                                            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER)
                                    .setAlcoholByVolume(Percentage.fromValue(5.2))
                                    .setServingVolume(Volume.fromLiters(0.122))
                                    .setNotes("Drinking Notes")
                                    .build();
                        },
                        numOfRecords);
        assertThat(insertedRecords).hasSize(numOfRecords);

        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        readAndAssertRecordsNotExistUsingIds(insertedRecords);

        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        eventually(() -> readAndAssertRecordsExistUsingIds(insertedRecords), ASSERT_TIMEOUT_MILLIS);
    }

    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_CYCLE_PHASES_FLAG,
        Flags.FLAG_CYCLE_PHASES_DB,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB
    })
    public void testBackupThenRestore_menstrualCyclePhaseRecords_expectDataIsRestoredCorrectly()
            throws Exception {
        assumeTrue(DeviceSupportUtils.isHealthConnectFullySupported());

        int numOfRecords = 90;
        List<Record> insertedRecords =
                insertRecordsWithChunking(
                        (i) -> {
                            LocalDate date = LocalDate.now(ZoneId.systemDefault()).minusDays(i);
                            int phase =
                                    ((i / 14) % 2 == 0)
                                            ? MenstrualCyclePhaseRecord.PHASE_FOLLICULAR
                                            : MenstrualCyclePhaseRecord.PHASE_LUTEAL;
                            int dayOfCycle = (i % 14) + 1;
                            return new MenstrualCyclePhaseRecord.Builder(
                                            new Metadata.Builder().build(), date, phase)
                                    .setDayOfCycle(dayOfCycle)
                                    .build();
                        },
                        numOfRecords);
        assertThat(insertedRecords).hasSize(numOfRecords);

        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        readAndAssertRecordsNotExistUsingIds(insertedRecords);

        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        eventually(() -> readAndAssertRecordsExistUsingIds(insertedRecords), ASSERT_TIMEOUT_MILLIS);
    }

    @Test
    public void testPermissionRestoredBeforeHCRestore_expectGrantTimeIsRestoredCorrectly()
            throws Exception {
        // revoke all permissions for both test apps to remove all stored grant time as setup step
        revokeAllHealthPermissionsAndWait(TEST_APP_1_PACKAGE_NAME);
        revokeAllHealthPermissionsAndWait(TEST_APP_2_PACKAGE_NAME);

        // grant a permission to test app 1 to create grant time
        grantHealthPermission(TEST_APP_1_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);
        Instant historicAccessStartDate =
                getEventually(
                        () -> {
                            Instant result2 =
                                    getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME);
                            assertThat(result2).isNotNull();
                            return result2;
                        },
                        ASSERT_TIMEOUT_MILLIS);

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
        eventually(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isNotNull(),
                ASSERT_TIMEOUT_MILLIS);
        Instant historicAccessStartDate2 =
                getEventually(
                        () -> {
                            Instant result =
                                    getHealthDataHistoricalAccessStartDate(TEST_APP_2_PACKAGE_NAME);
                            assertThat(result).isNotNull();
                            return result;
                        },
                        ASSERT_TIMEOUT_MILLIS);
        // trigger restore, now the staged backup file which contains grant time for test app 1
        // should override ONLY the newly created grant time of test app 1, but not the
        // test app 2's.
        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        // assert that test app 1's grant time is restored correctly
        eventually(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isEqualTo(historicAccessStartDate),
                ASSERT_TIMEOUT_MILLIS);
        // assert that test app 2's grant time stay the same
        eventually(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_2_PACKAGE_NAME))
                                .isEqualTo(historicAccessStartDate2),
                ASSERT_TIMEOUT_MILLIS);
    }

    @Test
    public void testPermissionsRestoredAfterHCRestore_expectGrantTimeIsRestoredCorrectly()
            throws Exception {
        // revoke all permissions for the test app to remove all stored grant time as setup step
        revokeAllHealthPermissionsAndWait(TEST_APP_1_PACKAGE_NAME);

        // grant a permission to test app to create grant time
        grantHealthPermission(TEST_APP_1_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);
        Instant historicAccessStartDate =
                getEventually(
                        () -> {
                            Instant result =
                                    getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME);
                            assertThat(result).isNotNull();
                            return result;
                        },
                        ASSERT_TIMEOUT_MILLIS);

        // trigger backup, the staged backup file should contains  grant time for test app 1.
        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());
        Thread.sleep(1000); // add some delay so second grant time is definitely different

        // revoke all permissions and then restore the grant time file.
        revokeAllHealthPermissions(TEST_APP_1_PACKAGE_NAME, "");
        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        // Since permissions haven't been restored, expect null access date.
        eventually(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isNull(),
                ASSERT_TIMEOUT_MILLIS);

        // grant permissions to simulate the case where permission controller module is restored
        // after HC module.
        grantHealthPermission(TEST_APP_1_PACKAGE_NAME, TEST_APP_DECLARED_PERMISSION);

        // assert that test app 1's grant time is restored correctly
        eventually(
                () ->
                        assertThat(getHealthDataHistoricalAccessStartDate(TEST_APP_1_PACKAGE_NAME))
                                .isEqualTo(historicAccessStartDate),
                ASSERT_TIMEOUT_MILLIS);
    }

    @Test
    public void testBackupRestore_withMultipleDevices_expectDataIsRestoredCorrectly()
            throws Exception {
        assumeTrue(DeviceSupportUtils.isHealthConnectFullySupported());

        Device deviceA =
                new Device.Builder().setManufacturer("ManA").setModel("ModA").setType(1).build();
        Device deviceB =
                new Device.Builder().setManufacturer("ManB").setModel("ModB").setType(1).build();
        Device deviceC =
                new Device.Builder().setManufacturer("ManC").setModel("ModC").setType(1).build();
        Device deviceD =
                new Device.Builder().setManufacturer("ManD").setModel("ModD").setType(1).build();

        // Inserting 3 records, all with different device Ids.
        // Inserting a 4th record with no device.
        Record recordA = createActiveCaloriesBurnedRecordWithDevice(deviceA, 1000);
        Record recordB = createActiveCaloriesBurnedRecordWithDevice(deviceB, 2000);
        Record recordC = createActiveCaloriesBurnedRecordWithDevice(deviceC, 3000);
        Record recordNoDevice = createActiveCaloriesBurnedRecordWithDevice(deviceC, 3000);
        List<Record> insertedRecordsBeforeRestore =
                insertRecords(List.of(recordA, recordB, recordC, recordNoDevice));
        readAndAssertRecordsExistUsingIds(insertedRecordsBeforeRestore);

        mBackupUtils.backupNowAndAssertSuccessForUser(
                mBackupRestoreApkPackageName, UserHandle.myUserId());

        // Simulate new device state (clean then added data)
        deleteAllDataFromHealthConnect();

        // Inserting 2 records after backup, one with a new device, and one common as before backup.
        // This ensures that there were more device ids in the backed up data then now.
        Record recordB2 = createActiveCaloriesBurnedRecordWithDevice(deviceB, 4000);
        Record recordD = createActiveCaloriesBurnedRecordWithDevice(deviceD, 5000);
        List<Record> insertedRecordsAfterRestore = insertRecords(List.of(recordB2, recordD));

        mBackupUtils.restoreAndAssertSuccessForUser(
                LOCAL_TRANSPORT_TOKEN, mBackupRestoreApkPackageName, UserHandle.myUserId());

        // Verify original backed up records are restored
        eventually(
                () -> readAndAssertRecordsExistUsingIds(insertedRecordsBeforeRestore),
                ASSERT_TIMEOUT_MILLIS);
        // Verify records inserted after backup are not changed.
        readAndAssertRecordsExistUsingIds(insertedRecordsAfterRestore);
    }

    private ActiveCaloriesBurnedRecord createActiveCaloriesBurnedRecordWithDevice(
            Device device, long offsetMillis) {
        DataOrigin dataOrigin = DataFactory.getDataOrigin(mContext.getPackageName());
        Metadata.Builder metadataBuilder = new Metadata.Builder();
        metadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        metadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);
        metadataBuilder.setClientRecordId("ClientRecordId" + UUID.randomUUID());

        Instant now = Instant.now().minusMillis(offsetMillis);
        ZoneOffset zoneOffset = ZoneOffset.systemDefault().getRules().getOffset(now);
        return new ActiveCaloriesBurnedRecord.Builder(
                        metadataBuilder.build(),
                        now,
                        now.plusMillis(1000),
                        Energy.fromCalories(10.0))
                .setStartZoneOffset(zoneOffset)
                .setEndZoneOffset(zoneOffset)
                .build();
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
        return insertRecordsWithChunking(
                creator, numOfRecords, MAX_NUMBER_OF_RECORD_PER_INSERT_REQUEST);
    }

    /**
     * Chunking is needed otherwise the insertion will fail with {@code
     * android.os.TransactionTooLargeException: data parcel size 1xxxxxx bytes}.
     */
    private List<Record> insertRecordsWithChunking(
            RecordCreator creator, int numOfRecords, int chunkSize) throws InterruptedException {
        List<Record> insertedRecords = new ArrayList<>();

        for (int chunk = 0; chunk <= numOfRecords / chunkSize; chunk++) {
            List<Record> recordsToInsert = new ArrayList<>();

            for (int indexWithinChunk = 0; indexWithinChunk < chunkSize; indexWithinChunk++) {
                int index = chunk * chunkSize + indexWithinChunk;
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

    private Instant getHealthDataHistoricalAccessStartDate(String packageName) {
        return runWithShellPermissionIdentity(
                () -> mHealthConnectManager.getHealthDataHistoricalAccessStartDate(packageName),
                MANAGE_HEALTH_PERMISSIONS);
    }

    private void revokeAllHealthPermissionsAndWait(String packageName) throws Exception {
        revokeAllHealthPermissions(packageName, "BackupRestoreE2ETest");
        // Wait for grant time to be cleared.
        eventually(() -> assertThat(getHealthDataHistoricalAccessStartDate(packageName)).isNull());
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
