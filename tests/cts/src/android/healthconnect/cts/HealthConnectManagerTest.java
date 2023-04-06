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
package android.healthconnect.cts;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_FETCHING_DATA;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_NONE;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_PENDING;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_COMPLETE;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_FAILED;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STARTED;
import static android.health.connect.HealthConnectManager.isHealthPermission;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.cts.TestUtils.MANAGE_HEALTH_DATA;
import static android.healthconnect.cts.TestUtils.getRecordById;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import static com.google.common.truth.Truth.assertThat;

import android.app.UiAutomation;
import android.content.Context;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordTypeInfoResponse;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.BodyFatRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Volume;
import android.health.connect.restore.StageRemoteDataException;
import android.os.FileUtils;
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.platform.test.annotations.AppModeFull;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {
    private static final String TAG = "HealthConnectManagerTest";
    private static final String APP_PACKAGE_NAME = "android.healthconnect.cts";

    private static Device getWatchDevice() {
        return new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
    }

    private static Device getPhoneDevice() {
        return new Device.Builder()
                .setManufacturer("google")
                .setModel("Pixel4a")
                .setType(2)
                .build();
    }

    private static DataOrigin getDataOrigin() {
        return getDataOrigin(/*packageName=*/ "");
    }

    private static DataOrigin getDataOrigin(String packageName) {
        return new DataOrigin.Builder()
                .setPackageName(packageName.isEmpty() ? APP_PACKAGE_NAME : packageName)
                .build();
    }

    @Before
    public void before() throws InterruptedException {
        deleteAllRecords();
    }

    @After
    public void after() throws InterruptedException {
        deleteAllRecords();
    }

    private void deleteAllRecords() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder().setPackageName(APP_PACKAGE_NAME).build())
                        .build());
    }

    @Before
    public void setUp() {
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testHCManagerIsAccessible_viaHCManager() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
    }

    @Test
    public void testHCManagerIsAccessible_viaContextConstant() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
    }

    @Test
    public void testRecordIdentifiers() {
        for (TestUtils.RecordAndIdentifier recordAndIdentifier :
                TestUtils.getRecordsAndIdentifiers()) {
            assertThat(recordAndIdentifier.getRecordClass().getRecordType())
                    .isEqualTo(recordAndIdentifier.getId());
        }
    }

    @Test
    public void testIsHealthPermission_forHealthPermission_returnsTrue() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertThat(isHealthPermission(context, HealthPermissions.READ_ACTIVE_CALORIES_BURNED))
                .isTrue();
        assertThat(isHealthPermission(context, HealthPermissions.READ_ACTIVE_CALORIES_BURNED))
                .isTrue();
    }

    @Test
    public void testIsHealthPermission_forNonHealthGroupPermission_returnsFalse() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertThat(isHealthPermission(context, HealthPermissions.MANAGE_HEALTH_PERMISSIONS))
                .isFalse();
        assertThat(isHealthPermission(context, CAMERA)).isFalse();
    }

    /**
     * Test to verify the working of {@link HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, update them and check by reading them.
     */
    @Test
    public void testUpdateRecords_validInput_dataBaseUpdatedSuccessfully()
            throws InterruptedException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException,
                    NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords();

        // Modify the Uid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    setTestRecordId(
                            updateRecords.get(itr), insertRecords.get(itr).getMetadata().getId()));
        }

        service.updateRecords(
                updateRecords,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, HealthConnectException>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                        Log.e(
                                TAG,
                                "Exception: "
                                        + exception.getMessage()
                                        + ", error code: "
                                        + exception.getErrorCode());
                    }
                });

        // assert the inserted data has been modified per the updateRecords.
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        assertThat(responseException.get()).isNull();
    }

    /**
     * Test to verify the working of {@link HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating provide input with a few invalid
     * records. These records will have UUIDs that are not present in the table. Since this record
     * won't be updated, the transaction should fail and revert and no other record(even though
     * valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_invalidInputRecords_noChangeInDataBase()
            throws InterruptedException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException,
                    NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords();

        // Modify the Uid of the updateRecords to the UUID that was present in the insert records,
        // leaving out alternate records so that they have a new UUID which is not present in the
        // dataBase.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    setTestRecordId(
                            updateRecords.get(itr),
                            itr % 2 == 0
                                    ? insertRecords.get(itr).getMetadata().getId()
                                    : String.valueOf(Math.random())));
        }

        // perform the update operation.
        service.updateRecords(
                updateRecords,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, HealthConnectException>() {
                    @Override
                    public void onResult(Void result) {}

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                        Log.e(
                                TAG,
                                "Exception: "
                                        + exception.getMessage()
                                        + ", error code: "
                                        + exception.getErrorCode());
                    }
                });

        assertThat(latch.await(/* timeout */ 3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has not been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        // verify that the testcase failed due to invalid argument exception.
        assertThat(responseException.get()).isNotNull();
        assertThat(responseException.get().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    /**
     * Test to verify the working of {@link HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating add an input record with an
     * invalid packageName. Since this is an invalid record the transaction should fail and revert
     * and no other record(even though valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_recordWithInvalidPackageName_noChangeInDataBase()
            throws InterruptedException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException,
                    NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<Exception> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords();

        // Modify the Uuid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    setTestRecordId(
                            updateRecords.get(itr), insertRecords.get(itr).getMetadata().getId()));
            //             adding an entry with invalid packageName.
            if (updateRecords.get(itr).getRecordType() == RECORD_TYPE_STEPS) {
                updateRecords.set(
                        itr,
                        getStepsRecord(/*clientRecordId=*/ null, /*packageName=*/ "abc.xyz.pqr"));
            }
        }

        try {
            // perform the update operation.
            service.updateRecords(
                    updateRecords,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<Void, HealthConnectException>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException exception) {
                            responseException.set(exception);
                            latch.countDown();
                            Log.e(
                                    TAG,
                                    "Exception: "
                                            + exception.getMessage()
                                            + ", error code: "
                                            + exception.getErrorCode());
                        }
                    });

        } catch (Exception exception) {
            latch.countDown();
            responseException.set(exception);
        }
        assertThat(latch.await(/* timeout */ 3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has not been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        // verify that the testcase failed due to invalid argument exception.
        assertThat(responseException.get()).isNotNull();
        assertThat(responseException.get().getClass()).isEqualTo(IllegalArgumentException.class);
    }

    @Test
    public void testInsertRecords_intervalWithSameClientId_overwrites()
            throws InterruptedException {
        final String clientId = "stepsClientId";
        final int count1 = 10;
        final int count2 = 10;
        final Instant endTime1 = Instant.now();
        final Instant startTime1 = endTime1.minusMillis(1000L);
        final Instant endTime2 = endTime1.minusMillis(100L);
        final Instant startTime2 = startTime1.minusMillis(100L);

        TestUtils.insertRecordAndGetId(
                getStepsRecord(clientId, /*packageName=*/ "", count1, startTime1, endTime1));
        TestUtils.insertRecordAndGetId(
                getStepsRecord(clientId, /*packageName=*/ "", count2, startTime2, endTime2));

        final List<StepsRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                .addClientRecordId(clientId)
                                .build());

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCount()).isEqualTo(count2);
    }

    @Test
    public void testInsertRecords_intervalNoClientIdsAndSameTime_overwrites()
            throws InterruptedException {
        final int count1 = 10;
        final int count2 = 20;
        final Instant endTime = Instant.now();
        final Instant startTime = endTime.minusMillis(1000L);

        final String id1 =
                TestUtils.insertRecordAndGetId(
                        getStepsRecord(
                                /*clientRecordId=*/ null,
                                /*packageName=*/ "",
                                count1,
                                startTime,
                                endTime));
        final String id2 =
                TestUtils.insertRecordAndGetId(
                        getStepsRecord(
                                /*clientRecordId=*/ null,
                                /*packageName=*/ "",
                                count2,
                                startTime,
                                endTime));

        final List<StepsRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                .addId(id1)
                                .addId(id2)
                                .build());

        assertThat(records).hasSize(1);
        assertThat(getRecordById(records, id2).getCount()).isEqualTo(count2);
    }

    @Test
    public void testInsertRecords_intervalDifferentClientIdsAndSameTime_doesNotOverwrite()
            throws InterruptedException {
        final int count1 = 10;
        final int count2 = 20;
        final Instant endTime = Instant.now();
        final Instant startTime = endTime.minusMillis(1000L);

        final String id1 =
                TestUtils.insertRecordAndGetId(
                        getStepsRecord(
                                "stepsClientId1", /*packageName=*/ "", count1, startTime, endTime));
        final String id2 =
                TestUtils.insertRecordAndGetId(
                        getStepsRecord(
                                "stepsClientId2", /*packageName=*/ "", count2, startTime, endTime));

        final List<StepsRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                .addId(id1)
                                .addId(id2)
                                .build());

        assertThat(records).hasSize(2);
        assertThat(getRecordById(records, id1).getCount()).isEqualTo(count1);
        assertThat(getRecordById(records, id2).getCount()).isEqualTo(count2);
    }

    @Test
    public void testInsertRecords_instantWithSameClientId_overwrites() throws InterruptedException {
        final String clientId = "bmrClientId";
        final Power bmr1 = Power.fromWatts(100.0);
        final Power bmr2 = Power.fromWatts(110.0);
        final Instant time1 = Instant.now();
        final Instant time2 = time1.minusMillis(100L);

        TestUtils.insertRecordAndGetId(getBasalMetabolicRateRecord(clientId, bmr1, time1));
        TestUtils.insertRecordAndGetId(getBasalMetabolicRateRecord(clientId, bmr2, time2));

        final List<BasalMetabolicRateRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class)
                                .addClientRecordId(clientId)
                                .build());

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBasalMetabolicRate()).isEqualTo(bmr2);
    }

    @Test
    public void testInsertRecords_instantNoClientIdsAndSameTime_overwrites()
            throws InterruptedException {
        final Power bmr1 = Power.fromWatts(100.0);
        final Power bmr2 = Power.fromWatts(110.0);
        final Instant time = Instant.now();

        final String id1 =
                TestUtils.insertRecordAndGetId(
                        getBasalMetabolicRateRecord(/*clientRecordId=*/ null, bmr1, time));
        final String id2 =
                TestUtils.insertRecordAndGetId(
                        getBasalMetabolicRateRecord(/*clientRecordId=*/ null, bmr2, time));

        final List<BasalMetabolicRateRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class)
                                .addId(id1)
                                .addId(id2)
                                .build());

        assertThat(records).hasSize(1);
        assertThat(getRecordById(records, id2).getBasalMetabolicRate()).isEqualTo(bmr2);
    }

    @Test
    public void testInsertRecords_instantDifferentClientIdsAndSameTime_doesNotOwerwrite()
            throws InterruptedException {
        final Power bmr1 = Power.fromWatts(100.0);
        final Power bmr2 = Power.fromWatts(110.0);
        final Instant time = Instant.now();

        final String id1 =
                TestUtils.insertRecordAndGetId(
                        getBasalMetabolicRateRecord(
                                /*clientRecordId=*/ "bmrClientId1", bmr1, time));
        final String id2 =
                TestUtils.insertRecordAndGetId(
                        getBasalMetabolicRateRecord(
                                /*clientRecordId=*/ "bmrClientId2", bmr2, time));

        final List<BasalMetabolicRateRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class)
                                .addId(id1)
                                .addId(id2)
                                .build());

        assertThat(records).hasSize(2);
        assertThat(getRecordById(records, id1).getBasalMetabolicRate()).isEqualTo(bmr1);
        assertThat(getRecordById(records, id2).getBasalMetabolicRate()).isEqualTo(bmr2);
    }

    // Special case for hydration, must not override
    @Test
    public void testInsertRecords_hydrationNoClientIdsAndSameTime_doesNotOverwrite()
            throws InterruptedException {
        final Volume volume1 = Volume.fromLiters(0.1);
        final Volume volume2 = Volume.fromLiters(0.2);
        final Instant endTime = Instant.now();
        final Instant startTime = endTime.minusMillis(1000L);

        final String id1 =
                TestUtils.insertRecordAndGetId(
                        getHydrationRecord(/*clientRecordId=*/ null, startTime, endTime, volume1));
        final String id2 =
                TestUtils.insertRecordAndGetId(
                        getHydrationRecord(/*clientRecordId=*/ null, startTime, endTime, volume2));

        final List<HydrationRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(HydrationRecord.class)
                                .addId(id1)
                                .addId(id2)
                                .build());

        assertThat(records).hasSize(2);
        assertThat(getRecordById(records, id1).getVolume()).isEqualTo(volume1);
        assertThat(getRecordById(records, id2).getVolume()).isEqualTo(volume2);
    }

    // Special case for nutrition, must not override
    @Test
    public void testInsertRecords_nutritionNoClientIdsAndSameTime_doesNotOverwrite()
            throws InterruptedException {
        final Mass protein1 = Mass.fromGrams(1.0);
        final Mass protein2 = Mass.fromGrams(1.0);
        final Instant endTime = Instant.now();
        final Instant startTime = endTime.minusMillis(1000L);

        final String id1 =
                TestUtils.insertRecordAndGetId(
                        getNutritionRecord(/*clientRecordId=*/ null, startTime, endTime, protein1));
        final String id2 =
                TestUtils.insertRecordAndGetId(
                        getNutritionRecord(/*clientRecordId=*/ null, startTime, endTime, protein2));

        final List<NutritionRecord> records =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(NutritionRecord.class)
                                .addId(id1)
                                .addId(id2)
                                .build());

        assertThat(records).hasSize(2);
        assertThat(getRecordById(records, id1).getProtein()).isEqualTo(protein1);
        assertThat(getRecordById(records, id2).getProtein()).isEqualTo(protein2);
    }

    @Test
    public void testAutoDeleteApis() throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        TestUtils.setAutoDeletePeriod(30);
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            assertThat(service.getRecordRetentionPeriodInDays()).isEqualTo(30);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }

        TestUtils.setAutoDeletePeriod(0);
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            assertThat(service.getRecordRetentionPeriodInDays()).isEqualTo(0);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    public void testStageRemoteData_withValidInput_noExceptionsReturned() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA");
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testStageRemoteData_whenNotReadMode_errorIoReturned() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, HealthConnectException>> observedExceptionsByFileName =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_WRITE_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA");
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void unused) {}

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {
                            observedExceptionsByFileName.set(error.getExceptionsByFileNames());
                            latch.countDown();
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(observedExceptionsByFileName.get()).isNotNull();
        assertThat(observedExceptionsByFileName.get().size()).isEqualTo(1);
        assertThat(
                        observedExceptionsByFileName.get().entrySet().stream()
                                .findFirst()
                                .get()
                                .getKey())
                .isEqualTo("testRestoreFile1");
        assertThat(
                        observedExceptionsByFileName.get().entrySet().stream()
                                .findFirst()
                                .get()
                                .getValue()
                                .getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_IO);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testStageRemoteData_whenStagingStagedData_noExceptionsReturned() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch retryLatch = new CountDownLatch(1);
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA");
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);

            // send the files again
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            retryLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(retryLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testStageRemoteData_withoutPermission_throwsSecurityException() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {}

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
        } catch (SecurityException e) {
            /* pass */
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        }

        deleteAllStagedRemoteData();
    }

    @Test
    public void testUpdateDataDownloadState_withoutPermission_throwsSecurityException() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            service.updateDataDownloadState(DATA_DOWNLOAD_STARTED);
        } catch (SecurityException e) {
            /* pass */
        }
    }

    @Test
    public void testGetHealthConnectDataState_beforeDownload_returnsIdleState() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_IDLE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void
            testGetHealthConnectDataState_beforeDownload_withMigrationPermission_returnsIdleState()
                    throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(MIGRATE_HEALTH_CONNECT_DATA);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_IDLE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_duringDownload_returnsRestorePendingState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_STARTED);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_PENDING);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_whenDownloadDone_returnsRestorePendingState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_COMPLETE);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_PENDING);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_whenDownloadFailed_returnsIdleState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_FAILED);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_IDLE);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_afterStaging_returnsRestorePendingState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch stateLatch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            stateLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(stateLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_PENDING);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetDataRestoreError_onErrorDuringStaging_returnsErrorFetching()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch stateLatch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_WRITE_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {}

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {
                            latch.countDown();
                        }
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            stateLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(stateLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreError())
                .isEqualTo(RESTORE_ERROR_FETCHING_DATA);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetDataRestoreError_onDownloadFailed_returnsErrorFetching() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_FAILED);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreError())
                .isEqualTo(RESTORE_ERROR_FETCHING_DATA);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetDataRestoreError_onNoErrorDuringRestore_returnsNoError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch stateLatch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            stateLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(stateLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreError())
                .isEqualTo(RESTORE_ERROR_NONE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testDataMigrationState_byDefault_returnsIdleState() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataMigrationState())
                .isEqualTo(MIGRATION_STATE_IDLE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_withoutPermission_throwsSecurityException() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(), healthConnectDataState -> {});
        } catch (SecurityException e) {
            /* pass */
        }
    }

    // TODO(b/274494950): Uncomment this when this bug is fixed as it's blocking the Presubmits.
    // This is a temporary solution to unblock others until this bug if fixed.
    //    @Test
    //    public void testDataApis_migrationInProgress_apisBlocked() throws InterruptedException {
    //        UiAutomation uiAutomation =
    // InstrumentationRegistry.getInstrumentation().getUiAutomation();
    //        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
    //
    //        runWithShellPermissionIdentity(
    //                () -> {
    //                    TestUtils.startMigration();
    //                    assertThat(TestUtils.getHealthConnectDataMigrationState())
    //                            .isEqualTo(HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS);
    //                },
    //                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    //
    //        StepsRecord testRecord = TestUtils.getStepsRecord();
    //
    //        try {
    //            TestUtils.insertRecords(Collections.singletonList(testRecord));
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //            ReadRecordsRequestUsingIds<StepsRecord> request =
    //                    new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
    //                            .addId(testRecord.getMetadata().getId())
    //                            .build();
    //            TestUtils.readRecords(request);
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //            TestUtils.updateRecords(Collections.singletonList(testRecord));
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //            TestUtils.deleteRecords(Collections.singletonList(testRecord));
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //            TestUtils.getActivityDates(Collections.singletonList(testRecord.getClass()));
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //            uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
    //            TestUtils.getApplicationInfo();
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //            uiAutomation.dropShellPermissionIdentity();
    //        }
    //
    //        try {
    //            uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
    //
    //            TestUtils.queryAccessLogs();
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //            uiAutomation.dropShellPermissionIdentity();
    //        }
    //
    //        try {
    //            uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
    //
    //            TestUtils.setAutoDeletePeriod(1);
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //            uiAutomation.dropShellPermissionIdentity();
    //        }
    //
    //        try {
    //            TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //
    //            TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(/* token */ "").build());
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        AggregateRecordsRequest<Long> aggregateRecordsRequest =
    //                new AggregateRecordsRequest.Builder<Long>(
    //                                new TimeInstantRangeFilter.Builder()
    //                                        .setStartTime(Instant.now().minus(3, ChronoUnit.DAYS))
    //                                        .setEndTime(Instant.now())
    //                                        .build())
    //                        .addAggregationType(STEPS_COUNT_TOTAL)
    //                        .build();
    //
    //        try {
    //            TestUtils.getAggregateResponse(
    //                    aggregateRecordsRequest, Collections.singletonList(testRecord));
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //            TestUtils.getAggregateResponseGroupByDuration(
    //                    aggregateRecordsRequest, Duration.ofDays(1));
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        try {
    //            TestUtils.getAggregateResponseGroupByPeriod(aggregateRecordsRequest,
    // Period.ofDays(1));
    //            Assert.fail();
    //        } catch (HealthConnectException exception) {
    //            assertThat(exception).isNotNull();
    //            assertThat(exception.getErrorCode())
    //                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
    //        }
    //
    //        runWithShellPermissionIdentity(
    //                TestUtils::finishMigration, Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    //    }

    @Test
    public void testGetRecordTypeInfo_InsertRecords_correctContributingPackages() throws Exception {
        // Insert a set of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        List<Record> testRecords = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecords);

        // Populate expected records. This method puts empty lists as contributing packages for all
        // records.
        HashMap<Class<? extends Record>, TestUtils.RecordTypeInfoTestResponse> expectedResponseMap =
                new HashMap<>();
        TestUtils.populateAndResetExpectedResponseMap(expectedResponseMap);
        // Populate contributing packages list for expected records by adding the current cts
        // package.
        expectedResponseMap.get(StepsRecord.class).getContributingPackages().add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(ExerciseSessionRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(HeartRateRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(BasalMetabolicRateRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);

        // When recordTypes are modified the appInfo also gets updated and this update happens on
        // a background thread. To ensure the test has the latest values for appInfo, add a wait
        // time before fetching it.
        Thread.sleep(500);
        // since test records contains the following records
        Map<Class<? extends Record>, RecordTypeInfoResponse> response =
                TestUtils.queryAllRecordTypesInfo();

        // verify response data is correct.
        verifyRecordTypeResponse(response, expectedResponseMap);

        // delete first set inserted records.
        TestUtils.deleteRecords(testRecords);

        // clear out contributing packages.
        TestUtils.populateAndResetExpectedResponseMap(expectedResponseMap);
        // delete inserted records.

        // When recordTypes are modified the appInfo also gets updated and this update happens on
        // a background thread. To ensure the test has the latest values for appInfo, add a wait
        // time before fetching it.
        Thread.sleep(500);
        response = TestUtils.queryAllRecordTypesInfo();
        // verify that the API still returns all record types with the cts packages as contributing
        // package. this is because only one of the inserted record for each record type was
        // deleted.
        verifyRecordTypeResponse(response, expectedResponseMap);
    }

    @Test
    public void testGetRecordTypeInfo_partiallyDeleteInsertedRecords_correctContributingPackages()
            throws Exception {
        // Insert a sets of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        List<Record> testRecords = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecords);

        // Populate expected records. This method puts empty lists as contributing packages for all
        // records.
        HashMap<Class<? extends Record>, TestUtils.RecordTypeInfoTestResponse> expectedResponseMap =
                new HashMap<>();
        TestUtils.populateAndResetExpectedResponseMap(expectedResponseMap);
        // Populate contributing packages list for expected records by adding the current cts
        // package.
        expectedResponseMap.get(StepsRecord.class).getContributingPackages().add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(ExerciseSessionRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(HeartRateRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(BasalMetabolicRateRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);

        // When recordTypes are modified the appInfo also gets updated and this update happens on
        // a background thread. To ensure the test has the latest values for appInfo, add a wait
        // time before fetching it.
        Thread.sleep(500);
        // since test records contains the following records
        Map<Class<? extends Record>, RecordTypeInfoResponse> response =
                TestUtils.queryAllRecordTypesInfo();

        // verify response data is correct.
        verifyRecordTypeResponse(response, expectedResponseMap);

        // delete 2 of the inserted records.
        ArrayList<Record> recordsToBeDeleted = new ArrayList<>();
        for (int itr = 0; itr < testRecords.size() / 2; itr++) {
            recordsToBeDeleted.add(testRecords.get(itr));
            expectedResponseMap
                    .get(testRecords.get(itr).getClass())
                    .getContributingPackages()
                    .clear();
        }

        TestUtils.deleteRecords(recordsToBeDeleted);

        // When recordTypes are modified the appInfo also gets updated and this update happens on
        // a background thread. To ensure the test has the latest values for appInfo, add a wait
        // time before fetching it.
        Thread.sleep(500);
        response = TestUtils.queryAllRecordTypesInfo();
        verifyRecordTypeResponse(response, expectedResponseMap);
    }

    @Test
    public void testGetRecordTypeInfo_MultipleInsertedRecords_correctContributingPackages()
            throws Exception {
        // Insert 2 sets of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        List<Record> testRecords = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecords);

        List<Record> testRecords2 = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecords2);

        // When recordTypes are modified the appInfo also gets updated and this update happens on
        // a background thread. To ensure the test has the latest values for appInfo, add a wait
        // time before fetching it.
        Thread.sleep(500);
        // Populate expected records. This method puts empty lists as contributing packages for all
        // records.
        HashMap<Class<? extends Record>, TestUtils.RecordTypeInfoTestResponse> expectedResponseMap =
                new HashMap<>();
        TestUtils.populateAndResetExpectedResponseMap(expectedResponseMap);
        // Populate contributing packages list for expected records by adding the current cts
        // package.
        expectedResponseMap.get(StepsRecord.class).getContributingPackages().add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(ExerciseSessionRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(HeartRateRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);
        expectedResponseMap
                .get(BasalMetabolicRateRecord.class)
                .getContributingPackages()
                .add(APP_PACKAGE_NAME);

        // since test records contains the following records
        Map<Class<? extends Record>, RecordTypeInfoResponse> response =
                TestUtils.queryAllRecordTypesInfo();

        // verify response data is correct.
        verifyRecordTypeResponse(response, expectedResponseMap);

        // delete only one set of inserted records.
        TestUtils.deleteRecords(testRecords);

        // When recordTypes are modified the appInfo also gets updated and this update happens on
        // a background thread. To ensure the test has the latest values for appInfo, add a wait
        // time before fetching it.
        Thread.sleep(500);
        response = TestUtils.queryAllRecordTypesInfo();

        verifyRecordTypeResponse(response, expectedResponseMap);
    }

    // This test uses a db created using DATABASE_VERSION 7 (Last bumped on 2023-03-17T17:23:29Z).
    // The health db is sitting directly in the CTS app and is staged directly with the HC service.
    // And then the records from this staged db are merged.
    // Ideally this db should stay good forever, as the newer versions of HC code are guaranteed to
    // work with the older versions of the health db.
    // However, if for some reason there's any issue, please try by creating another db (probably
    // using a newer version of HF module) and replacing the db in the cts app's resources.
    @Test
    public void testMergeStagedData_withEmptyHealthDb_mergesAllData() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        // Step 0: reset everything as some cts tests leave stuff behind.
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        deleteAllStagedRemoteData();

        List<BodyFatRecord> bodyFatRecordsRead =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class).build());
        List<HeightRecord> heightRecordsRead =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class).build());

        assertThat(bodyFatRecordsRead).isEmpty();
        assertThat(heightRecordsRead).isEmpty();

        // Step 1: Restore the db with the cts app.
        restoreBackupData();
        Thread.sleep(TimeUnit.SECONDS.toMillis(10)); // give some time for merge to finish.

        // Step 2: Assert that the restored db (with the service) has the records from the db with
        // the cts app.
        heightRecordsRead =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class).build());
        assertThat(heightRecordsRead.size()).isEqualTo(1);
        bodyFatRecordsRead =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class).build());
        assertThat(bodyFatRecordsRead.size()).isEqualTo(2);

        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        deleteAllStagedRemoteData();
    }

    private void deleteAllStagedRemoteData()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
            assertThat(service).isNotNull();

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA");
            // TODO(b/241542162): Avoid using reflection as a workaround once test apis can be
            //  run in CTS tests.
            service.getClass().getMethod("deleteAllStagedRemoteData").invoke(service);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
    }

    private void restoreBackupData() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File backupDataDir = new File(context.getFilesDir(), "backup_data");
            backupDataDir.mkdirs();
            try (InputStream in = context.getResources().openRawResource(R.raw.healthconnect);
                    FileOutputStream out =
                            new FileOutputStream(new File(backupDataDir, "healthconnect.db"))) {
                FileUtils.copy(in, out);
                out.getFD().sync();
            }
            File[] filesToRestore = backupDataDir.listFiles();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            for (var file : filesToRestore) {
                pfdsByFileName.put(file.getName(), ParcelFileDescriptor.open(file, MODE_READ_ONLY));
            }

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA");
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
    }

    private static File createAndGetNonEmptyFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Contents of file " + fileName);
        fileWriter.close();
        return file;
    }

    private void verifyRecordTypeResponse(
            Map<Class<? extends Record>, RecordTypeInfoResponse> responses,
            HashMap<Class<? extends Record>, TestUtils.RecordTypeInfoTestResponse>
                    expectedResponse) {
        responses.forEach(
                (recordTypeClass, recordTypeInfoResponse) -> {
                    TestUtils.RecordTypeInfoTestResponse expectedTestResponse =
                            expectedResponse.get(recordTypeClass);
                    assertThat(expectedTestResponse).isNotNull();
                    assertThat(recordTypeInfoResponse.getPermissionCategory())
                            .isEqualTo(expectedTestResponse.getRecordTypePermission());
                    assertThat(recordTypeInfoResponse.getDataCategory())
                            .isEqualTo(expectedTestResponse.getRecordTypeCategory());
                    ArrayList<String> contributingPackagesAsStrings = new ArrayList<>();
                    for (DataOrigin pck : recordTypeInfoResponse.getContributingPackages()) {
                        contributingPackagesAsStrings.add(pck.getPackageName());
                    }
                    Collections.sort(contributingPackagesAsStrings);
                    Collections.sort(expectedTestResponse.getContributingPackages());
                    assertThat(contributingPackagesAsStrings)
                            .isEqualTo(expectedTestResponse.getContributingPackages());
                });
    }

    private List<Record> getTestRecords() {
        return Arrays.asList(
                getStepsRecord(/*clientRecordId=*/ null, /*packageName=*/ ""),
                getHeartRateRecord(),
                getBasalMetabolicRateRecord());
    }

    private Record setTestRecordId(Record record, String id) {
        Metadata metadata = record.getMetadata();
        Metadata metadataWithId =
                new Metadata.Builder()
                        .setId(id)
                        .setClientRecordId(metadata.getClientRecordId())
                        .setClientRecordVersion(metadata.getClientRecordVersion())
                        .setDataOrigin(metadata.getDataOrigin())
                        .setDevice(metadata.getDevice())
                        .setLastModifiedTime(metadata.getLastModifiedTime())
                        .build();
        switch (record.getRecordType()) {
            case RECORD_TYPE_STEPS:
                return new StepsRecord.Builder(
                                metadataWithId, Instant.now(), Instant.now().plusMillis(1000), 10)
                        .build();
            case RECORD_TYPE_HEART_RATE:
                HeartRateRecord.HeartRateSample heartRateSample =
                        new HeartRateRecord.HeartRateSample(72, Instant.now().plusMillis(100));
                ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
                heartRateSamples.add(heartRateSample);
                heartRateSamples.add(heartRateSample);
                return new HeartRateRecord.Builder(
                                metadataWithId,
                                Instant.now(),
                                Instant.now().plusMillis(1000),
                                heartRateSamples)
                        .build();
            case RECORD_TYPE_BASAL_METABOLIC_RATE:
                return new BasalMetabolicRateRecord.Builder(
                                metadataWithId, Instant.now(), Power.fromWatts(100.0))
                        .setZoneOffset(
                                ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                        .build();
            default:
                throw new IllegalStateException("Invalid record type.");
        }
    }

    private StepsRecord getStepsRecord(String clientRecordId, String packageName) {
        return getStepsRecord(
                clientRecordId,
                packageName,
                /*count=*/ 10,
                Instant.now(),
                Instant.now().plusMillis(1000));
    }

    private StepsRecord getStepsRecord(
            String clientRecordId,
            String packageName,
            int count,
            Instant startTime,
            Instant endTime) {
        Device device = getWatchDevice();
        DataOrigin dataOrigin = getDataOrigin(packageName);
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (clientRecordId != null) {
            testMetadataBuilder.setClientRecordId(clientRecordId);
        }
        return new StepsRecord.Builder(testMetadataBuilder.build(), startTime, endTime, count)
                .build();
    }

    private HeartRateRecord getHeartRateRecord() {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now().plusMillis(100));
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device = getWatchDevice();
        DataOrigin dataOrigin = getDataOrigin();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        return new HeartRateRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        heartRateSamples)
                .build();
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
        return getBasalMetabolicRateRecord(
                /*clientRecordId=*/ null, /*bmr=*/ Power.fromWatts(100.0), Instant.now());
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord(
            String clientRecordId, Power bmr, Instant time) {
        Device device = getPhoneDevice();
        DataOrigin dataOrigin = getDataOrigin();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (clientRecordId != null) {
            testMetadataBuilder.setClientRecordId(clientRecordId);
        }
        return new BasalMetabolicRateRecord.Builder(testMetadataBuilder.build(), time, bmr).build();
    }

    private HydrationRecord getHydrationRecord(
            String clientRecordId, Instant startTime, Instant endTime, Volume volume) {
        Device device = getPhoneDevice();
        DataOrigin dataOrigin = getDataOrigin();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (clientRecordId != null) {
            testMetadataBuilder.setClientRecordId(clientRecordId);
        }
        return new HydrationRecord.Builder(testMetadataBuilder.build(), startTime, endTime, volume)
                .build();
    }

    private NutritionRecord getNutritionRecord(
            String clientRecordId, Instant startTime, Instant endTime, Mass protein) {
        Device device = getPhoneDevice();
        DataOrigin dataOrigin = getDataOrigin();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (clientRecordId != null) {
            testMetadataBuilder.setClientRecordId(clientRecordId);
        }
        return new NutritionRecord.Builder(testMetadataBuilder.build(), startTime, endTime)
                .setProtein(protein)
                .build();
    }
}
