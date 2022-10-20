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

import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.ChangeLogTokenRequest;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.ReadRecordsResponse;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestUtils {
    private static final String TAG = "HCTestUtils";

    public static Long getChangeLogToken(ChangeLogTokenRequest request)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Long> response = new AtomicReference<>();
        service.getChangeLogToken(
                request,
                Executors.newSingleThreadExecutor(),
                result -> {
                    response.set(result);
                    latch.countDown();
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        return response.get();
    }

    public static List<Record> insertRecords(List<Record> records) throws InterruptedException {
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
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(response.get()).hasSize(records.size());

        return response.get();
    }

    //    public static ChangeLogsResponse getChangeLogs(long token) throws InterruptedException {
    //        Context context = ApplicationProvider.getApplicationContext();
    //        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
    //        assertThat(service).isNotNull();
    //
    //        CountDownLatch latch = new CountDownLatch(1);
    //        AtomicReference<ChangeLogsResponse> response = new AtomicReference<>();
    //        service.getChangeLogs(
    //                token,
    //                Executors.newSingleThreadExecutor(),
    //                result -> {
    //                    response.set(result);
    //                    latch.countDown();
    //                });
    //        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
    //        return response.get();
    //    }

    public static List<Record> getTestRecords() {
        return Arrays.asList(getStepsRecord(), getHeartRateRecord(), getBasalMetabolicRateRecord());
    }

    public static List<RecordAndIdentifier> getRecordsAndIdentifiers() {
        return Arrays.asList(
                new RecordAndIdentifier(RECORD_TYPE_STEPS, getStepsRecord()),
                new RecordAndIdentifier(RECORD_TYPE_HEART_RATE, getHeartRateRecord()),
                new RecordAndIdentifier(
                        RECORD_TYPE_BASAL_METABOLIC_RATE, getBasalMetabolicRateRecord()));
    }

    public static StepsRecord getStepsRecord() {
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        return new StepsRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("SR" + Math.random())
                                .build(),
                        Instant.now(),
                        Instant.now(),
                        10)
                .build();
    }

    public static HeartRateRecord getHeartRateRecord() {
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
                                .setClientRecordId("HR" + Math.random())
                                .build(),
                        Instant.now(),
                        Instant.now(),
                        heartRateSamples)
                .build();
    }

    public static BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
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
                                .setClientRecordId("BMR" + Math.random())
                                .build(),
                        Instant.now(),
                        Power.fromWatts(100.0))
                .build();
    }

    public static <T extends Record> List<T> readRecords(ReadRecordsRequestUsingIds<T> request)
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

    public static <T extends Record> List<T> readRecords(ReadRecordsRequestUsingFilters<T> request)
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
}
