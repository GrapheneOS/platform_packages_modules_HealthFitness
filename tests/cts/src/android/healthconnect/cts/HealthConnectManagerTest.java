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
import static android.healthconnect.HealthConnectManager.isHealthPermission;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.HealthPermissions;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;
import android.os.OutcomeReceiver;
import android.platform.test.annotations.AppModeFull;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {
    private static final String TAG = "HealthConnectManagerTest";

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
    public void testInsertRecords() throws Exception {
        TestUtils.insertRecords(TestUtils.getTestRecords());
    }

    @Test
    public void testDeleteRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(TestUtils.getStepsRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(TestUtils.getStepsRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteRecord_recordId_filters() throws InterruptedException {
        List<Record> records = TestUtils.getTestRecords();
        TestUtils.insertRecords(records);

        for (Record record : records) {
            TestUtils.verifyDeleteRecords(
                    new DeleteUsingFiltersRequest.Builder()
                            .addRecordType(record.getClass())
                            .build());
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(TestUtils.getStepsRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(TestUtils.getStepsRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteRecord_usingIds() throws InterruptedException {
        List<Record> records = TestUtils.getTestRecords();
        List<Record> insertedRecord = TestUtils.insertRecords(records);
        List<RecordIdFilter> recordIds = new ArrayList<>(records.size());
        for (Record record : insertedRecord) {
            recordIds.add(
                    new RecordIdFilter.Builder(record.getClass())
                            .setId(record.getMetadata().getId())
                            .build());
        }

        TestUtils.verifyDeleteRecords(recordIds);
        for (Record record : records) {
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(TestUtils.getStepsRecord());
        TestUtils.verifyDeleteRecords(StepsRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testReadRecord_usingIds() throws InterruptedException {
        testRead_StepsRecordIds();
        testRead_HeartRateRecord();
        testRead_BasalMetabolicRateRecord();
    }

    @Test
    public void testReadRecord_usingClientRecordIds() throws InterruptedException {
        testRead_StepsRecordClientIds();
    }

    @Test
    public void testReadRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId("abc").build();
        List<StepsRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<StepsRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
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

    @Test
    public void testReadStepsRecordUsingFilters_default() throws InterruptedException {
        List<StepsRecord> oldStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        TestUtils.insertRecords(Collections.singletonList(TestUtils.getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        assertThat(newStepsRecords.size()).isEqualTo(oldStepsRecords.size() + 1);
        assertThat(newStepsRecords.get(newStepsRecords.size() - 1).getCount())
                .isEqualTo(TestUtils.getStepsRecord().getCount());
    }

    @Test
    public void testReadStepsRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        TestUtils.insertRecords(Collections.singletonList(TestUtils.getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(1);
        assertThat(newStepsRecords.get(0).getCount())
                .isEqualTo(TestUtils.getStepsRecord().getCount());
    }

    @Test
    public void testReadStepsRecordUsingFilters_dataFilter_incorrect() throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(TestUtils.getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(0);
    }

    // TODO(b/257796081): Move read tests to respective record type classes, verify that the correct
    // details are being fetched, and add tests for all record type

    @Test
    public void testReadStepsRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<StepsRecord> oldStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        TestUtils.insertRecords(Collections.singletonList(TestUtils.getStepsRecord()));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newStepsRecords.size() - oldStepsRecords.size()).isEqualTo(1);
        assertThat(newStepsRecords.get(0).getCount())
                .isEqualTo(TestUtils.getStepsRecord().getCount());
    }

    /*
    @Test
    public void test_getRecordTypeInfo() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<Map<Class<? extends Record>, RecordTypeInfoResponse>> response =
                new AtomicReference<>();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        service.queryAllRecordTypesInfo(
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<
                        Map<Class<? extends Record>, RecordTypeInfoResponse>,
                        HealthConnectException>() {
                    @Override
                    public void onResult(
                            Map<Class<? extends Record>, RecordTypeInfoResponse> result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(responseException.get()).isNull();
        assertThat(response).isNotNull();
        Log.d(TAG, "GetDataTypeInfoResponse : \n");
        response.get()
                .forEach(
                        (recordTypeClass, recordTypeInfoResponse) -> {
                            StringBuilder builder =
                                    new StringBuilder(recordTypeClass.getTypeName() + " : ");
                            builder.append(
                                    " HealthPermissionCategory : "
                                            + recordTypeInfoResponse.getPermissionCategory());
                            builder.append(
                                    " HealthDataCategory : "
                                            + recordTypeInfoResponse.getDataCategory());
                            builder.append(
                                    " Contributing Packages : "
                                            + String.join(
                                                    ",",
                                                    recordTypeInfoResponse
                                                            .getContributingPackages()
                                                            .stream()
                                                            .map(DataOrigin::getPackageName)
                                                            .collect(Collectors.toList())));
                            builder.append("\n");
                            Log.d(TAG, builder.toString());
                        });
    }
    */

    /**
     * Test to verify the working of {@link
     * android.healthconnect.HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, update them and check by reading them.
     */
    @Test
    public void testUpdateRecords_validInput_dataBaseUpdatedSuccessfully()
            throws InterruptedException, InvocationTargetException, InstantiationException,
                    IllegalAccessException, NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords
                    .get(itr)
                    .getMetadata()
                    .setId(insertRecords.get(itr).getMetadata().getId());
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
     * Test to verify the working of {@link
     * android.healthconnect.HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating provide input with a few invalid
     * records. These records will have UUIDs that are not present in the table. Since this record
     * won't be updated, the transaction should fail and revert and no other record(even though
     * valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_invalidInputRecords_noChangeInDataBase()
            throws InterruptedException, InvocationTargetException, InstantiationException,
                    IllegalAccessException, NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uid of the updateRecords to the UUID that was present in the insert records,
        // leaving out alternate records so that they have a new UUID which is not present in the
        // dataBase.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords
                    .get(itr)
                    .getMetadata()
                    .setId(
                            itr % 2 == 0
                                    ? insertRecords.get(itr).getMetadata().getId()
                                    : String.valueOf(Math.random()));
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
     * Test to verify the working of {@link
     * android.healthconnect.HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating add an input record with an
     * invalid packageName. Since this is an invalid record the transaction should fail and revert
     * and no other record(even though valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_recordWithInvalidPackageName_noChangeInDataBase()
            throws InterruptedException, InvocationTargetException, InstantiationException,
                    IllegalAccessException, NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<Exception> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uuid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords
                    .get(itr)
                    .getMetadata()
                    .setId(insertRecords.get(itr).getMetadata().getId());

            //             adding an entry with invalid packageName.
            if (updateRecords.get(itr).getRecordType() == RECORD_TYPE_STEPS) {
                updateRecords.set(
                        itr, getStepsRecord(false, /* incorrectPackageName */ "abc.xyz.pqr"));
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

    private void testRead_StepsRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(TestUtils.getStepsRecord(), TestUtils.getStepsRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readStepsRecordUsingIds(insertedRecords);
    }

    private void testRead_StepsRecordClientIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(TestUtils.getStepsRecord(), TestUtils.getStepsRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readStepsRecordUsingClientId(insertedRecords);
    }

    private void testRead_HeartRateRecord() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(TestUtils.getHeartRateRecord(), TestUtils.getHeartRateRecord());
        readHeartRateRecordUsingIds(recordList);
    }

    private void testRead_BasalMetabolicRateRecord() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        TestUtils.getBasalMetabolicRateRecord(),
                        TestUtils.getBasalMetabolicRateRecord());
        readBasalMetabolicRateRecordUsingIds(recordList);
    }

    private void readStepsRecordUsingIds(List<Record> insertedRecord) throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addId(record.getMetadata().getId());
        }
        List<StepsRecord> result = TestUtils.readRecords(request.build());
        verifyStepsRecordReadResults(insertedRecord, result);
    }

    private void readStepsRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<StepsRecord> result = TestUtils.readRecords(request.build());
        verifyStepsRecordReadResults(insertedRecord, result);
    }

    private void verifyStepsRecordReadResults(
            List<Record> insertedRecords, List<StepsRecord> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            StepsRecord stepsRecord = readResult.get(i);
            StepsRecord input = (StepsRecord) insertedRecords.get(i);
            assertThat(stepsRecord.getRecordType()).isEqualTo(input.getRecordType());
            assertThat(stepsRecord.getMetadata().getDevice().getManufacturer())
                    .isEqualTo(input.getMetadata().getDevice().getManufacturer());
            assertThat(stepsRecord.getMetadata().getDevice().getModel())
                    .isEqualTo(input.getMetadata().getDevice().getModel());
            assertThat(stepsRecord.getMetadata().getDevice().getType())
                    .isEqualTo(input.getMetadata().getDevice().getType());
            assertThat(stepsRecord.getMetadata().getDataOrigin().getPackageName())
                    .isEqualTo(input.getMetadata().getDataOrigin().getPackageName());
            assertThat(stepsRecord.getCount()).isEqualTo(input.getCount());
        }
    }

    private void readHeartRateRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<HeartRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeartRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<HeartRateRecord> result = TestUtils.readRecords(request.build());
        verifyHeartRateRecordReadResults(insertedRecords, result);
    }

    private void verifyHeartRateRecordReadResults(
            List<Record> insertedRecords, List<HeartRateRecord> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            HeartRateRecord heartRateRecord = readResult.get(i);
            HeartRateRecord input = (HeartRateRecord) insertedRecords.get(i);
            assertThat(heartRateRecord.getRecordType()).isEqualTo(input.getRecordType());
            assertThat(heartRateRecord.getMetadata().getDevice().getManufacturer())
                    .isEqualTo(input.getMetadata().getDevice().getManufacturer());
            assertThat(heartRateRecord.getMetadata().getDevice().getModel())
                    .isEqualTo(input.getMetadata().getDevice().getModel());
            assertThat(heartRateRecord.getMetadata().getDevice().getType())
                    .isEqualTo(input.getMetadata().getDevice().getType());
            assertThat(heartRateRecord.getMetadata().getDataOrigin().getPackageName())
                    .isEqualTo(input.getMetadata().getDataOrigin().getPackageName());
        }
    }

    private void readBasalMetabolicRateRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<BasalMetabolicRateRecord> result = TestUtils.readRecords(request.build());
        verifyBMRRecordReadResults(insertedRecords, result);
    }

    private void verifyBMRRecordReadResults(
            List<Record> insertedRecords, List<BasalMetabolicRateRecord> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            BasalMetabolicRateRecord bmrRecord = readResult.get(i);
            BasalMetabolicRateRecord input = (BasalMetabolicRateRecord) insertedRecords.get(i);
            assertThat(bmrRecord.getRecordType()).isEqualTo(input.getRecordType());
            assertThat(bmrRecord.getMetadata().getDevice().getManufacturer())
                    .isEqualTo(input.getMetadata().getDevice().getManufacturer());
            assertThat(bmrRecord.getMetadata().getDevice().getModel())
                    .isEqualTo(input.getMetadata().getDevice().getModel());
            assertThat(bmrRecord.getMetadata().getDevice().getType())
                    .isEqualTo(input.getMetadata().getDevice().getType());
            assertThat(bmrRecord.getMetadata().getDataOrigin().getPackageName())
                    .isEqualTo(input.getMetadata().getDataOrigin().getPackageName());
            assertThat(bmrRecord.getBasalMetabolicRate().getInWatts())
                    .isEqualTo(input.getBasalMetabolicRate().getInWatts());
        }
    }

    private List<Record> getTestRecords(boolean isSetClientRecordId) {
        return Arrays.asList(
                getStepsRecord(isSetClientRecordId, ""),
                getHeartRateRecord(isSetClientRecordId),
                getBasalMetabolicRateRecord(isSetClientRecordId));
    }

    private StepsRecord getStepsRecord() {
        return getStepsRecord(true, "");
    }

    private StepsRecord getStepsRecord(boolean isSetClientRecordId, String packageName) {
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder()
                        .setPackageName(
                                packageName.isEmpty() ? "android.healthconnect.cts" : packageName)
                        .build();

        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("SR" + Math.random());
        }
        return new StepsRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now(), 10)
                .build();
    }

    private HeartRateRecord getHeartRateRecord() {
        return getHeartRateRecord(true);
    }

    private HeartRateRecord getHeartRateRecord(boolean isSetClientRecordId) {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now());
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("HR" + Math.random());
        }
        return new HeartRateRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now(), heartRateSamples)
                .build();
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
        return getBasalMetabolicRateRecord(true);
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord(boolean isSetClientRecordId) {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("BMR" + Math.random());
        }
        return new BasalMetabolicRateRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Power.fromWatts(100.0))
                .build();
    }
}
