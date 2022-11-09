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
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.HealthPermissions;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.ReadRecordsResponse;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;
import android.os.OutcomeReceiver;
import android.platform.test.annotations.AppModeFull;
import android.util.ArraySet;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {
    static final class RecordAndIdentifier {
        private final int id;
        private final Record recordClass;

        public RecordAndIdentifier(int id, Record recordClass) {
            this.id = id;
            this.recordClass = recordClass;
        }

        public int getId() {
            return id;
        }

        public Record getRecordClass() {
            return recordClass;
        }
    }

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
    public void testAllRecordsAreBeingTested() {
        List<Record> records = getTestRecords();
        assertThat(records.size()).isEqualTo(RecordTypeIdentifier.class.getDeclaredFields().length);
        Set<Integer> recordIdSet = new ArraySet<>();
        records.forEach(
                (record -> {
                    assertThat(recordIdSet.contains(record.getRecordType())).isEqualTo(false);
                    recordIdSet.add(record.getRecordType());
                }));
    }

    @Test
    public void testAllIdentifiersAreBeingTested() {
        List<RecordAndIdentifier> recordAndIdentifiers = getRecordsAndIdentifiers();
        assertThat(recordAndIdentifiers.size())
                .isEqualTo(RecordTypeIdentifier.class.getDeclaredFields().length);
    }

    @Test
    public void testRecordIdentifiers() {
        for (RecordAndIdentifier recordAndIdentifier : getRecordsAndIdentifiers()) {
            assertThat(recordAndIdentifier.getRecordClass().getRecordType())
                    .isEqualTo(recordAndIdentifier.getId());
        }
    }

    @Test
    public void testInsertRecords() throws Exception {
        insertRecords(getTestRecords());
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
        ReadRecordsRequestUsingIds request =
                new ReadRecordsRequestUsingIds.Builder(StepsRecord.class)
                        .addClientRecordId("abc")
                        .addClientRecordId("xyz")
                        .build();
        List<Record> result = readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds request =
                new ReadRecordsRequestUsingIds.Builder(StepsRecord.class)
                        .addId("abc")
                        .addId("xyz")
                        .build();
        List<Record> result = readRecords(request);
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

    private List<Record> insertRecords(List<Record> records) throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<List<Record>> response = new AtomicReference<>();
        service.insertRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<InsertRecordsResponse, HealthConnectException>() {
                    @Override
                    public void onResult(InsertRecordsResponse result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(response.get()).hasSize(records.size());

        return response.get();
    }

    private void testRead_StepsRecordIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getStepsRecord(), getStepsRecord());
        List<Record> insertedRecords = insertRecords(recordList);
        readStepsRecordUsingIds(insertedRecords);
    }

    private void testRead_StepsRecordClientIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getStepsRecord(), getStepsRecord());
        List<Record> insertedRecords = insertRecords(recordList);
        readStepsRecordUsingClientId(insertedRecords);
    }

    private void testRead_HeartRateRecord() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getHeartRateRecord(), getHeartRateRecord());
        readHeartRateRecordUsingIds(recordList);
    }

    private void testRead_BasalMetabolicRateRecord() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getBasalMetabolicRateRecord(), getBasalMetabolicRateRecord());
        readBasalMetabolicRateRecordUsingIds(recordList);
    }

    private void readStepsRecordUsingIds(List<Record> insertedRecord) throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder request =
                new ReadRecordsRequestUsingIds.Builder(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addId(record.getMetadata().getId());
        }
        List<Record> result = readRecords(request.build());
        verifyStepsRecordReadResults(insertedRecord, result);
    }

    private void readStepsRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder request =
                new ReadRecordsRequestUsingIds.Builder(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<Record> result = readRecords(request.build());
        verifyStepsRecordReadResults(insertedRecord, result);
    }

    private void verifyStepsRecordReadResults(
            List<Record> insertedRecords, List<Record> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            StepsRecord stepsRecord = (StepsRecord) readResult.get(i);
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
        List<Record> insertedRecords = insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder request =
                new ReadRecordsRequestUsingIds.Builder(HeartRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<Record> result = readRecords(request.build());
        verifyHeartRateRecordReadResults(insertedRecords, result);
    }

    private void verifyHeartRateRecordReadResults(
            List<Record> insertedRecords, List<Record> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            HeartRateRecord heartRateRecord = (HeartRateRecord) readResult.get(i);
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
        List<Record> insertedRecords = insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder request =
                new ReadRecordsRequestUsingIds.Builder(BasalMetabolicRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<Record> result = readRecords(request.build());
        verifyBMRRecordReadResults(insertedRecords, result);
    }

    private void verifyBMRRecordReadResults(List<Record> insertedRecords, List<Record> readResult) {
        assertThat(readResult).hasSize(insertedRecords.size());
        for (int i = 0; i < readResult.size(); i++) {
            BasalMetabolicRateRecord bmrRecord = (BasalMetabolicRateRecord) readResult.get(i);
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

    private <T extends Record> List<T> readRecords(ReadRecordsRequestUsingIds<T> request)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(service).isNotNull();
        AtomicReference<List<T>> response = new AtomicReference<>();
        service.readRecords(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException>() {
                    @Override
                    public void onResult(ReadRecordsResponse<T> result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        return response.get();
    }

    private List<Record> getTestRecords() {
        return Arrays.asList(getStepsRecord(), getHeartRateRecord(), getBasalMetabolicRateRecord());
    }

    private List<RecordAndIdentifier> getRecordsAndIdentifiers() {
        return Arrays.asList(
                new RecordAndIdentifier(RECORD_TYPE_STEPS, getStepsRecord()),
                new RecordAndIdentifier(RECORD_TYPE_HEART_RATE, getHeartRateRecord()),
                new RecordAndIdentifier(
                        RECORD_TYPE_BASAL_METABOLIC_RATE, getBasalMetabolicRateRecord()));
    }

    private StepsRecord getStepsRecord() {
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        return new StepsRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("SR" + String.valueOf(Math.random()))
                                .build(),
                        Instant.now(),
                        Instant.now(),
                        10)
                .build();
    }

    private HeartRateRecord getHeartRateRecord() {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now());
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();

        return new HeartRateRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("HR" + String.valueOf(Math.random()))
                                .build(),
                        Instant.now(),
                        Instant.now(),
                        heartRateSamples)
                .build();
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("BMR" + String.valueOf(Math.random()))
                                .build(),
                        Instant.now(),
                        Power.fromWatts(100.0))
                .build();
    }
}
