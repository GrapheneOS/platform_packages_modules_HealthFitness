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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import android.app.UiAutomation;
import android.content.Context;
import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.InsertRecordsResponse;
import android.health.connect.ReadRecordsRequest;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Power;
import android.health.connect.migration.MigrationException;
import android.os.OutcomeReceiver;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TestUtils {
    public static final String MANAGE_HEALTH_DATA = HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
    public static final Instant SESSION_START_TIME = Instant.now().minus(10, ChronoUnit.DAYS);
    public static final Instant SESSION_END_TIME =
            Instant.now().minus(10, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);
    private static final String TAG = "HCTestUtils";

    public static ChangeLogTokenResponse getChangeLogToken(ChangeLogTokenRequest request)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ChangeLogTokenResponse> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> exceptionAtomicReference = new AtomicReference<>();
        service.getChangeLogToken(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(ChangeLogTokenResponse result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                        exceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
        return response.get();
    }

    public static String insertRecordAndGetId(Record record) throws InterruptedException {
        return insertRecords(Collections.singletonList(record)).get(0).getMetadata().getId();
    }

    /**
     * Inserts records to the database.
     *
     * @param records records to insert
     * @return inserted records
     */
    public static List<Record> insertRecords(List<Record> records) throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<List<Record>> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> exceptionAtomicReference = new AtomicReference<>();
        service.insertRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(InsertRecordsResponse result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                        exceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
        assertThat(response.get()).hasSize(records.size());

        return response.get();
    }

    public static void updateRecords(List<Record> records) throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> exceptionAtomicReference = new AtomicReference<>();
        service.updateRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        exceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
    }

    public static ChangeLogsResponse getChangeLogs(ChangeLogsRequest changeLogsRequest)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ChangeLogsResponse> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        service.getChangeLogs(
                changeLogsRequest,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(ChangeLogsResponse result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        healthConnectExceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }

        return response.get();
    }

    public static Device buildDevice() {
        return new Device.Builder()
                .setManufacturer("google")
                .setModel("Pixel4a")
                .setType(2)
                .build();
    }

    public static List<Record> getTestRecords() {
        return Arrays.asList(
                getStepsRecord(),
                getHeartRateRecord(),
                getBasalMetabolicRateRecord(),
                buildExerciseSession());
    }

    public static List<RecordAndIdentifier> getRecordsAndIdentifiers() {
        return Arrays.asList(
                new RecordAndIdentifier(RECORD_TYPE_STEPS, getStepsRecord()),
                new RecordAndIdentifier(RECORD_TYPE_HEART_RATE, getHeartRateRecord()),
                new RecordAndIdentifier(
                        RECORD_TYPE_BASAL_METABOLIC_RATE, getBasalMetabolicRateRecord()));
    }

    public static ExerciseRoute.Location buildLocationTimePoint() {
        return new ExerciseRoute.Location.Builder(
                        Instant.ofEpochMilli((long) (1e10 + Math.random() * 50)),
                        Math.random() * 50,
                        Math.random() * 50)
                .build();
    }

    public static ExerciseRoute buildExerciseRoute() {
        return new ExerciseRoute(
                List.of(
                        buildLocationTimePoint(),
                        buildLocationTimePoint(),
                        buildLocationTimePoint()));
    }

    public static StepsRecord getStepsRecord() {
        Context context = ApplicationProvider.getApplicationContext();
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName(context.getPackageName()).build();
        return new StepsRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("SR" + Math.random())
                                .build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10)
                .build();
    }

    public static HeartRateRecord getHeartRateRecord() {
        Context context = ApplicationProvider.getApplicationContext();

        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now().plusMillis(100));
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName(context.getPackageName()).build();

        return new HeartRateRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("HR" + Math.random())
                                .build(),
                        Instant.now(),
                        Instant.now().plusMillis(500),
                        heartRateSamples)
                .build();
    }

    public static HeartRateRecord getHeartRateRecord(int heartRate) {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(heartRate, Instant.now().plusMillis(100));
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);

        return new HeartRateRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(500),
                        heartRateSamples)
                .build();
    }

    public static HeartRateRecord getHeartRateRecord(int heartRate, Instant instant) {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(heartRate, instant);
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);

        return new HeartRateRecord.Builder(
                        new Metadata.Builder().build(),
                        instant,
                        instant.plusMillis(1000),
                        heartRateSamples)
                .build();
    }

    public static BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
        Context context = ApplicationProvider.getApplicationContext();
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName(context.getPackageName()).build();
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

    public static <T> AggregateRecordsResponse<T> getAggregateResponse(
            AggregateRecordsRequest<T> request, List<Record> recordsToInsert)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        insertRecords(recordsToInsert);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AggregateRecordsResponse<T>> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        service.aggregate(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(AggregateRecordsResponse<T> result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException healthConnectException) {
                        healthConnectExceptionAtomicReference.set(healthConnectException);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }

        return response.get();
    }

    public static <T>
            List<AggregateRecordsGroupedByDurationResponse<T>> getAggregateResponseGroupByDuration(
                    AggregateRecordsRequest<T> request, Duration duration)
                    throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<AggregateRecordsGroupedByDurationResponse<T>>> response =
                new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        service.aggregateGroupByDuration(
                request,
                duration,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(
                            List<AggregateRecordsGroupedByDurationResponse<T>> result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException healthConnectException) {
                        healthConnectExceptionAtomicReference.set(healthConnectException);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }
        return response.get();
    }

    public static <T>
            List<AggregateRecordsGroupedByPeriodResponse<T>> getAggregateResponseGroupByPeriod(
                    AggregateRecordsRequest<T> request, Period period) throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<AggregateRecordsGroupedByPeriodResponse<T>>> response =
                new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        service.aggregateGroupByPeriod(
                request,
                period,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(List<AggregateRecordsGroupedByPeriodResponse<T>> result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException healthConnectException) {
                        healthConnectExceptionAtomicReference.set(healthConnectException);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }

        return response.get();
    }

    public static <T extends Record> List<T> readRecords(ReadRecordsRequest<T> request)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(service).isNotNull();
        assertThat(request.getRecordType()).isNotNull();
        AtomicReference<List<T>> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        service.readRecords(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(ReadRecordsResponse<T> result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                        healthConnectExceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }
        return response.get();
    }

    public static <T extends Record> void assertRecordNotFound(String uuid, Class<T> recordType)
            throws InterruptedException {
        assertThat(
                        readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(recordType)
                                        .addId(uuid)
                                        .build()))
                .isEmpty();
    }

    public static <T extends Record> void assertRecordFound(String uuid, Class<T> recordType)
            throws InterruptedException {
        assertThat(
                        readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(recordType)
                                        .addId(uuid)
                                        .build()))
                .isNotEmpty();
    }

    public static <T extends Record> Pair<List<T>, Long> readRecordsWithPagination(
            ReadRecordsRequest<T> request) throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(service).isNotNull();
        AtomicReference<List<T>> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        AtomicReference<Long> pageToken = new AtomicReference<>();
        service.readRecords(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(ReadRecordsResponse<T> result) {
                        response.set(result.getRecords());
                        pageToken.set(result.getNextPageToken());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        healthConnectExceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }
        return Pair.create(response.get(), pageToken.get());
    }

    public static void setAutoDeletePeriod(int period) throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            Context context = ApplicationProvider.getApplicationContext();
            HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
            CountDownLatch latch = new CountDownLatch(1);
            assertThat(service).isNotNull();
            AtomicReference<HealthConnectException> exceptionAtomicReference =
                    new AtomicReference<>();
            service.setRecordRetentionPeriodInDays(
                    period,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException healthConnectException) {
                            exceptionAtomicReference.set(healthConnectException);
                            latch.countDown();
                        }
                    });
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
            if (exceptionAtomicReference.get() != null) {
                throw exceptionAtomicReference.get();
            }
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static void verifyDeleteRecords(DeleteUsingFiltersRequest request)
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            Context context = ApplicationProvider.getApplicationContext();
            HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<HealthConnectException> exceptionAtomicReference =
                    new AtomicReference<>();
            assertThat(service).isNotNull();
            service.deleteRecords(
                    request,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException healthConnectException) {
                            exceptionAtomicReference.set(healthConnectException);
                            latch.countDown();
                        }
                    });
            assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
            if (exceptionAtomicReference.get() != null) {
                throw exceptionAtomicReference.get();
            }
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static void verifyDeleteRecords(List<RecordIdFilter> request)
            throws InterruptedException {

        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectException> exceptionAtomicReference = new AtomicReference<>();
        assertThat(service).isNotNull();
        service.deleteRecords(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException healthConnectException) {
                        exceptionAtomicReference.set(healthConnectException);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
    }

    public static void verifyDeleteRecords(
            Class<? extends Record> recordType, TimeInstantRangeFilter timeRangeFilter)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> exceptionAtomicReference = new AtomicReference<>();

        service.deleteRecords(
                recordType,
                timeRangeFilter,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException healthConnectException) {
                        exceptionAtomicReference.set(healthConnectException);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
    }

    public static void deleteRecords(List<Record> records) throws InterruptedException {
        List<RecordIdFilter> recordIdFilters =
                records.stream()
                        .map(
                                (record ->
                                        RecordIdFilter.fromId(
                                                record.getClass(), record.getMetadata().getId())))
                        .collect(Collectors.toList());
        verifyDeleteRecords(recordIdFilters);
    }

    public static List<AccessLog> queryAccessLogs() throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            Context context = ApplicationProvider.getApplicationContext();
            HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
            assertThat(service).isNotNull();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<List<AccessLog>> response = new AtomicReference<>();
            service.queryAccessLogs(
                    Executors.newSingleThreadExecutor(),
                    result -> {
                        response.set(result);
                        latch.countDown();
                    });
            assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
            return response.get();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static List<LocalDate> getActivityDates(List<Class<? extends Record>> recordTypes)
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            Context context = ApplicationProvider.getApplicationContext();
            HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
            CountDownLatch latch = new CountDownLatch(1);
            assertThat(service).isNotNull();
            AtomicReference<List<LocalDate>> response = new AtomicReference<>();
            AtomicReference<HealthConnectException> exceptionAtomicReference =
                    new AtomicReference<>();
            service.queryActivityDates(
                    recordTypes,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(List<LocalDate> result) {
                            response.set(result);
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException exception) {
                            exceptionAtomicReference.set(exception);
                            latch.countDown();
                        }
                    });

            assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
            if (exceptionAtomicReference.get() != null) {
                throw exceptionAtomicReference.get();
            }

            return response.get();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static ExerciseSessionRecord buildExerciseSession() {
        return buildExerciseSession(buildExerciseRoute(), "Morning training", "rain");
    }

    public static SleepSessionRecord buildSleepSession() {
        return new SleepSessionRecord.Builder(
                        generateMetadata(), SESSION_START_TIME, SESSION_END_TIME)
                .setNotes("warm")
                .setTitle("Afternoon nap")
                .setStages(
                        List.of(
                                new SleepSessionRecord.Stage(
                                        SESSION_START_TIME,
                                        SESSION_START_TIME.plusSeconds(300),
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT),
                                new SleepSessionRecord.Stage(
                                        SESSION_START_TIME.plusSeconds(300),
                                        SESSION_START_TIME.plusSeconds(600),
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_REM),
                                new SleepSessionRecord.Stage(
                                        SESSION_START_TIME.plusSeconds(900),
                                        SESSION_START_TIME.plusSeconds(1200),
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP)))
                .build();
    }

    public static void startMigration() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);

        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        service.startMigration(
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, MigrationException>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(MigrationException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    }

    public static void finishMigration() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);

        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        service.finishMigration(
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, MigrationException>() {

                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(MigrationException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    }

    public static void insertMinDataMigrationSdkExtensionVersion(int version)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        service.insertMinDataMigrationSdkExtensionVersion(
                version,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(MigrationException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    }

    public static void deleteAllStagedRemoteData() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        runWithShellPermissionIdentity(
                () ->
                        // TODO(b/241542162): Avoid reflection once TestApi can be called from CTS
                        service.getClass().getMethod("deleteAllStagedRemoteData").invoke(service),
                "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA");
    }

    public static int getHealthConnectDataMigrationState() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();
        service.getHealthConnectDataState(
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(HealthConnectDataState healthConnectDataState) {
                        returnedHealthConnectDataState.set(healthConnectDataState);
                        latch.countDown();
                    }

                    @Override
                    public void onError(@NonNull HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        if (responseException.get() != null) {
            throw responseException.get();
        }
        return returnedHealthConnectDataState.get().getDataMigrationState();
    }

    static Metadata generateMetadata() {
        Context context = ApplicationProvider.getApplicationContext();
        return new Metadata.Builder()
                .setDevice(buildDevice())
                .setId("recordId" + Math.random())
                .setClientRecordId("clientRecordId" + Math.random())
                .setDataOrigin(
                        new DataOrigin.Builder().setPackageName(context.getPackageName()).build())
                .setDevice(buildDevice())
                .build();
    }

    private static ExerciseSessionRecord buildExerciseSession(
            ExerciseRoute route, String title, String notes) {
        return new ExerciseSessionRecord.Builder(
                        generateMetadata(),
                        SESSION_START_TIME,
                        SESSION_END_TIME,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setRoute(route)
                .setLaps(
                        List.of(
                                new ExerciseLap.Builder(
                                                SESSION_START_TIME,
                                                SESSION_START_TIME.plusSeconds(20))
                                        .setLength(Length.fromMeters(10))
                                        .build(),
                                new ExerciseLap.Builder(
                                                SESSION_END_TIME.minusSeconds(20), SESSION_END_TIME)
                                        .build()))
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                SESSION_START_TIME.plusSeconds(1),
                                                SESSION_START_TIME.plusSeconds(10),
                                                ExerciseSegmentType
                                                        .EXERCISE_SEGMENT_TYPE_BENCH_PRESS)
                                        .build(),
                                new ExerciseSegment.Builder(
                                                SESSION_START_TIME.plusSeconds(21),
                                                SESSION_START_TIME.plusSeconds(124),
                                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE)
                                        .setRepetitionsCount(15)
                                        .build()))
                .setEndZoneOffset(ZoneOffset.MAX)
                .setStartZoneOffset(ZoneOffset.MIN)
                .setNotes(notes)
                .setTitle(title)
                .build();
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
