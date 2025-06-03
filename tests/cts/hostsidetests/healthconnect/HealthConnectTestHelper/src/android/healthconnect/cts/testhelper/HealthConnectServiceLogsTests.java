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
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.NutritionRecord.BIOTIN_TOTAL;
import static android.healthconnect.testing.cts.TestUtils.deleteRecordsOfType;
import static android.healthconnect.testing.cts.TestUtils.deleteRecordsOfTypes;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.shared.DataFactory.getBloodPressureRecord;
import static android.healthconnect.testing.shared.DataFactory.getEmptyMetadata;
import static android.healthconnect.testing.shared.DataFactory.getHeartRateRecord;
import static android.healthconnect.testing.shared.DataFactory.getHeightRecord;
import static android.healthconnect.testing.shared.DataFactory.getMetadataForId;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.time.Instant.EPOCH;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.testing.cts.PermissionUtils;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.aggregation.TimeFilterFactory;
import android.os.OutcomeReceiver;

import androidx.test.core.app.ApplicationProvider;

import com.android.compatibility.common.util.NonApiTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
@NonApiTest(
        exemptionReasons = {},
        justification = "METRIC")
public class HealthConnectServiceLogsTests {

    public static final int TIMEOUT_SECONDS = 5;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final HealthConnectManager mHealthConnectManager =
            requireNonNull(mContext.getSystemService(HealthConnectManager.class));
    private final PhrCtsTestUtils mPhrTestUtils = new PhrCtsTestUtils(mHealthConnectManager);

    @Before
    public void before() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();

        // b/372766760: In theory, declared permissions are meant to be auto granted. However,
        // this seems to be unreliable and has led to test failures where the permissions don't
        // get granted as expected. We do it explicitly here as a precaution.
        PermissionUtils.grantAllHealthPermissions(mContext.getPackageName());
        // insert a record so the test app gets an app id in HC
        Record record =
                new StepsRecord.Builder(getEmptyMetadata(), EPOCH, Instant.now(), 123).build();
        insertRecords(List.of(record));
        TestUtils.deleteAllFitnessData();
    }

    @After
    public void after() throws InterruptedException {
        TestUtils.deleteAllFitnessData();
        mPhrTestUtils.deleteAllMedicalData();
    }

    @Test
    public void testCreateMedicalDataSourceSuccess() throws InterruptedException {
        mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
    }

    @Test
    public void testCreateMedicalDataSourceError() {
        callApiWhileMigrationInProgress(
                () -> mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1")));
    }

    @Test
    public void testGetMedicalDataSourcesByIdsSuccess() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));

        mPhrTestUtils.getMedicalDataSourcesByIds(List.of(dataSource.getId()));
    }

    @Test
    public void testGetMedicalDataSourcesByIdsError() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));

        callApiWhileMigrationInProgress(
                () -> mPhrTestUtils.getMedicalDataSourcesByIds(List.of(dataSource.getId())));
    }

    @Test
    public void testGetMedicalDataSourcesByRequestSuccess() throws InterruptedException {
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(mContext.getPackageName())
                        .build();

        mPhrTestUtils.getMedicalDataSourcesByRequest(request);
    }

    @Test
    public void testGetMedicalDataSourcesByRequestError() {
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(mContext.getPackageName())
                        .build();

        callApiWhileMigrationInProgress(
                () -> mPhrTestUtils.getMedicalDataSourcesByRequest(request));
    }

    @Test
    public void testDeleteMedicalDataSourceWithDataSuccess() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));

        mPhrTestUtils.deleteMedicalDataSourceWithData(dataSource.getId());
    }

    @Test
    public void testDeleteMedicalDataSourceWithDataError() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        callApiWhileMigrationInProgress(
                () -> mPhrTestUtils.deleteMedicalDataSourceWithData(dataSource.getId()));
    }

    @Test
    public void testUpsertMedicalResourcesSuccess() throws InterruptedException {
        MedicalDataSource medicalDataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        mPhrTestUtils.upsertMedicalData(medicalDataSource.getId(), FHIR_DATA_IMMUNIZATION);
    }

    @Test
    public void testUpsertMedicalResourcesError() throws InterruptedException {
        MedicalDataSource medicalDataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));

        callApiWhileMigrationInProgress(
                () ->
                        mPhrTestUtils.upsertMedicalData(
                                medicalDataSource.getId(), FHIR_DATA_IMMUNIZATION));
    }

    @Test
    public void testReadMedicalResourcesByIdsSuccess() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource resource =
                mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        mPhrTestUtils.readMedicalResourcesByIds(List.of(resource.getId()));
    }

    @Test
    public void testReadMedicalResourcesByIdsError() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource resource =
                mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        callApiWhileMigrationInProgress(
                () -> mPhrTestUtils.readMedicalResourcesByIds(List.of(resource.getId())));
    }

    @Test
    public void testReadMedicalResourcesByRequestsSuccess() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        mPhrTestUtils.readMedicalResourcesByRequest(request);
    }

    @Test
    public void testReadMedicalResourcesByRequestsError() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        callApiWhileMigrationInProgress(
                () -> {
                    ReadMedicalResourcesInitialRequest request =
                            new ReadMedicalResourcesInitialRequest.Builder(
                                            MEDICAL_RESOURCE_TYPE_VACCINES)
                                    .build();
                    mPhrTestUtils.readMedicalResourcesByRequest(request);
                });
    }

    @Test
    public void testDeleteMedicalResourcesByIdsSuccess() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource medicalResource =
                mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        mPhrTestUtils.deleteMedicalResourcesByIds(List.of(medicalResource.getId()));
    }

    @Test
    public void testDeleteMedicalResourcesByIdsError() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource medicalResource =
                mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        callApiWhileMigrationInProgress(
                () -> mPhrTestUtils.deleteMedicalResourcesByIds(List.of(medicalResource.getId())));
    }

    @Test
    public void testDeleteMedicalResourcesByRequestSuccess() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource medicalResource =
                mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        mPhrTestUtils.deleteMedicalResourcesByRequest(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(medicalResource.getType())
                        .build());
    }

    @Test
    public void testDeleteMedicalResourcesByRequestError() throws InterruptedException {
        MedicalDataSource dataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource medicalResource =
                mPhrTestUtils.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        callApiWhileMigrationInProgress(
                () ->
                        mPhrTestUtils.deleteMedicalResourcesByRequest(
                                new DeleteMedicalResourcesRequest.Builder()
                                        .addMedicalResourceType(medicalResource.getType())
                                        .build()));
    }

    @Test
    public void testHealthConnectInsertRecords() throws Exception {
        insertRecords(List.of(getBloodPressureRecord(), getHeartRateRecord()));
    }

    @Test
    public void testHealthConnectInsertRecordsError() throws Exception {
        // No permission for Height so it should throw Security Exception
        assertThrows(
                HealthConnectException.class,
                () -> insertRecords(List.of(getBloodPressureRecord(), getHeightRecord())));
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
                                        getMetadataForId(
                                                insertRecords.get(0).getMetadata().getId()),
                                        Instant.now(),
                                        Length.fromMeters(1.5))
                                .build()));
    }

    @Test
    public void testHealthConnectDeleteRecords() throws Exception {
        insertRecords(List.of(getBloodPressureRecord(), getStepsRecord()));

        deleteRecordsOfTypes(List.of(BloodPressureRecord.class, StepsRecord.class));
    }

    @Test
    public void testHealthConnectDeleteRecordsError() throws Exception {
        insertRecords(List.of(getBloodPressureRecord(), getStepsRecord()));

        assertThrows(HealthConnectException.class, () -> deleteRecordsOfType(HeightRecord.class));
    }

    @Test
    public void testHealthConnectReadRecords() throws Exception {
        insertRecords(List.of(getStepsRecord()));

        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(getDefaultTimeRangeFilter())
                        .addDataOrigins(
                                new DataOrigin.Builder()
                                        .setPackageName(mContext.getPackageName())
                                        .build())
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectGetChangeLogs() throws Exception {
        String token = getChangeLogToken();

        insertRecords(List.of(getBloodPressureRecord(), getHeartRateRecord()));

        deleteRecordsOfTypes(List.of(BloodPressureRecord.class));

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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testHealthConnectDatabaseStats() throws Exception {
        insertRecords(List.of(getStepsRecord(), getBloodPressureRecord(), getHeartRateRecord()));
    }

    private String getChangeLogToken() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(mHealthConnectManager).isNotNull();
        mHealthConnectManager.getChangeLogToken(
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(BloodPressureRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .build(),
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        return token.get();
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

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    private static TimeRangeFilter getDefaultTimeRangeFilter() {
        Instant now = Instant.now();
        Instant start = now.minus(Duration.ofHours(24)).truncatedTo(ChronoUnit.DAYS);
        Instant end = now.plus(Duration.ofHours(24)).truncatedTo(ChronoUnit.DAYS);
        return TimeFilterFactory.getTimeFilter(start, end);
    }

    /**
     * Executes {code apiCall} when migration is in progress.
     *
     * <p>This is mainly used to emulate API call failures.
     */
    private static void callApiWhileMigrationInProgress(Task apiCall) {
        try {
            logErrorIfAny(TestUtils::startMigrationWithShellPermissionIdentity);

            apiCall.execute();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            logErrorIfAny(TestUtils::finishMigrationWithShellPermissionIdentity);
        }
    }

    /** Executes a {@link Task} and logs error if any. */
    private static void logErrorIfAny(Task task) {
        try {
            task.execute();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private interface Task {
        void execute() throws Exception;
    }
}
