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

package android.healthconnect.cts.testhelper;

import static android.health.connect.datatypes.HeartRateRecord.BPM_MAX;
import static android.health.connect.datatypes.NutritionRecord.BIOTIN_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.InsertRecordsResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.TimeRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeartRateRecord.HeartRateSample;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Pressure;
import android.os.OutcomeReceiver;

import androidx.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * These tests are run by statsdatom/healthconnect to log atoms by triggering Health Connect APIs.
 *
 * <p>They only trigger the APIs, but don't test anything themselves.
 */
public class LoggingTests {

    private final HealthConnectManager mHealthConnectManager =
            InstrumentationRegistry.getContext().getSystemService(HealthConnectManager.class);
    private static final String MY_PACKAGE_NAME =
            InstrumentationRegistry.getContext().getPackageName();

    @Before
    public void before() throws InterruptedException {
        deleteAllRecordsBeforeTest();
    }

    @Test
    public void testHealthConnectInsertRecords() throws Exception {
        insertRecords(List.of(getBloodPressureRecord(), getHeartRateRecord()));
    }

    @Test
    public void testHealthConnectInsertRecordsError() throws Exception {
        // No permission for Height so it should throw Security Exception
        insertRecords(List.of(getBloodPressureRecord(), getHeightRecord()));
    }

    @Test
    public void testHealthConnectUpdateRecords() throws Exception {
        List<Record> records =
                insertRecords(
                        List.of(getBloodPressureRecord(), getHeartRateRecord(), getStepsRecord()));
        updateRecords(records);
    }

    @Test
    public void testHealthConnectUpdateRecordsError() throws Exception {
        List<Record> insertRecords = insertRecords(List.of(getBloodPressureRecord()));

        updateRecords(
                List.of(
                        new HeightRecord.Builder(
                                        getMetadata(insertRecords.get(0).getMetadata().getId()),
                                        Instant.now(),
                                        Length.fromMeters(1.5))
                                .build()));
    }

    @Test
    public void testHealthConnectDeleteRecords() throws Exception {
        insertRecords(List.of(getBloodPressureRecord(), getStepsRecord()));
        DeleteUsingFiltersRequest deleteUsingFiltersRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BloodPressureRecord.class)
                        .addRecordType(StepsRecord.class)
                        .addDataOrigin(getDataOrigin())
                        .build();

        assertThat(mHealthConnectManager).isNotNull();
        deleteRecords(deleteUsingFiltersRequest);
    }

    @Test
    public void testHealthConnectDeleteRecordsError() throws Exception {
        insertRecords(List.of(getBloodPressureRecord(), getStepsRecord()));
        DeleteUsingFiltersRequest deleteUsingFiltersRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(HeightRecord.class)
                        .addDataOrigin(getDataOrigin())
                        .build();

        assertThat(mHealthConnectManager).isNotNull();
        deleteRecords(deleteUsingFiltersRequest);
    }

    @Test
    public void testHealthConnectReadRecords() throws Exception {

        insertRecords(List.of(getStepsRecord()));
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(getDefaultTimeRangeFilter())
                        .setPageSize(1)
                        .build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(ReadRecordsResponse<StepsRecord> result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectReadRecordsError() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class)
                        .setTimeRangeFilter(getDefaultTimeRangeFilter())
                        .setPageSize(1)
                        .build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(ReadRecordsResponse<HeightRecord> result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectGetChangeLogToken() throws Exception {
        getChangeLogToken();
    }

    @Test
    public void testHealthConnectGetChangeLogTokenError() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.getChangeLogToken(
                new ChangeLogTokenRequest.Builder().addRecordType(HeightRecord.class).build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(ChangeLogTokenResponse result) {
                        token.set(result.getToken());
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectGetChangeLogs() throws Exception {
        String token = getChangeLogToken();

        insertRecords(List.of(getBloodPressureRecord(), getHeartRateRecord()));

        deleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BloodPressureRecord.class)
                        .addDataOrigin(getDataOrigin())
                        .build());

        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.getChangeLogs(
                new ChangeLogsRequest.Builder(token).build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(ChangeLogsResponse result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectGetChangeLogsError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.getChangeLogs(
                new ChangeLogsRequest.Builder("FAIL").build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(ChangeLogsResponse result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectAggregatedData() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.aggregate(
                new AggregateRecordsRequest.Builder<Long>(getDefaultTimeRangeFilter())
                        .addAggregationType(BPM_MAX)
                        .build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(AggregateRecordsResponse<Long> result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectAggregatedDataError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.aggregate(
                new AggregateRecordsRequest.Builder<Mass>(getDefaultTimeRangeFilter())
                        .addAggregationType(BIOTIN_TOTAL)
                        .build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(AggregateRecordsResponse<Mass> result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectDatabaseStats() throws Exception {
        insertRecords(List.of(getStepsRecord(), getBloodPressureRecord(), getHeartRateRecord()));
    }

    private static Metadata getMetadata() {
        return new Metadata.Builder().setDataOrigin(getDataOrigin()).build();
    }

    private static Metadata getMetadata(String id) {
        return new Metadata.Builder().setDataOrigin(getDataOrigin()).setId(id).build();
    }

    private static DataOrigin getDataOrigin() {
        return new DataOrigin.Builder().setPackageName(MY_PACKAGE_NAME).build();
    }

    private static BloodPressureRecord getBloodPressureRecord() {
        return new BloodPressureRecord.Builder(
                        getMetadata(),
                        Instant.now(),
                        1,
                        Pressure.fromMillimetersOfMercury(22.0),
                        Pressure.fromMillimetersOfMercury(24.0),
                        1)
                .build();
    }

    private static StepsRecord getStepsRecord() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.HOURS);
        return new StepsRecord.Builder(getMetadata(), startTime, endTime, 100).build();
    }

    private static HeartRateRecord getHeartRateRecord() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.HOURS);
        return new HeartRateRecord.Builder(
                        getMetadata(),
                        startTime,
                        endTime,
                        List.of(new HeartRateSample(100, startTime)))
                .build();
    }

    /**
     * Insertion/Reading of Height Record should fail as HealthConnectTestHelper do not have
     * permissions for Height.
     */
    private static HeightRecord getHeightRecord() {
        return new HeightRecord.Builder(getMetadata(), Instant.now(), Length.fromMeters(1.9))
                .build();
    }

    private static TimeRangeFilter getDefaultTimeRangeFilter() {
        Instant now = Instant.now();
        Instant start = now.minus(Duration.ofHours(24)).truncatedTo(ChronoUnit.DAYS);
        Instant end = now.plus(Duration.ofHours(24)).truncatedTo(ChronoUnit.DAYS);
        return new TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build();
    }

    private List<Record> insertRecords(List<Record> records) throws InterruptedException {
        AtomicReference<List<Record>> response = new AtomicReference<>(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();

        mHealthConnectManager.insertRecords(
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
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        return response.get();
    }

    private String getChangeLogToken() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.getChangeLogToken(
                new ChangeLogTokenRequest.Builder().build(),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(ChangeLogTokenResponse result) {
                        token.set(result.getToken());
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        return token.get();
    }

    /** Deletes only records the test app has permission to write. */
    private void deleteAllRecordsBeforeTest() throws InterruptedException {
        assertThat(mHealthConnectManager).isNotNull();

        DeleteUsingFiltersRequest deleteUsingFiltersRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BloodPressureRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .addRecordType(StepsRecord.class)
                        .addDataOrigin(getDataOrigin())
                        .build();
        deleteRecords(deleteUsingFiltersRequest);
    }

    private void updateRecords(List<Record> records) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        mHealthConnectManager.updateRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    private void deleteRecords(DeleteUsingFiltersRequest deleteUsingFiltersRequest)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        mHealthConnectManager.deleteRecords(
                deleteUsingFiltersRequest,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {

                    @Override
                    public void onError(HealthConnectException exception) {
                        latch.countDown();
                    }

                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }
}
