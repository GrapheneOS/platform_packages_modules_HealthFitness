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

import static android.health.connect.datatypes.WeightRecord.WEIGHT_AVG;
import static android.health.connect.datatypes.WeightRecord.WEIGHT_MAX;
import static android.health.connect.datatypes.WeightRecord.WEIGHT_MIN;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.units.Mass;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class WeightRecordTest {
    private static final String TAG = "WeightRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                WeightRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertWeightRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseWeightRecord(), getCompleteWeightRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadWeightRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);

        readWeightRecordUsingIds(insertedRecords);
    }

    @Test
    public void testReadWeightRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class)
                        .addId(UUID.randomUUID().toString())
                        .build();
        List<WeightRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadWeightRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readWeightRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadWeightRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<WeightRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadWeightRecordUsingFilters_default() throws InterruptedException {
        List<WeightRecord> oldWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class).build());
        WeightRecord testRecord = getCompleteWeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class).build());
        assertThat(newWeightRecords.size()).isEqualTo(oldWeightRecords.size() + 1);
        assertThat(newWeightRecords.get(newWeightRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadWeightRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        WeightRecord testRecord = getCompleteWeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newWeightRecords.size()).isEqualTo(1);
        assertThat(newWeightRecords.get(newWeightRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadWeightRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<WeightRecord> oldWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        WeightRecord testRecord = getCompleteWeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newWeightRecords.size() - oldWeightRecords.size()).isEqualTo(1);
        WeightRecord newRecord = newWeightRecords.get(newWeightRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
        assertThat(newRecord.getWeight()).isEqualTo(testRecord.getWeight());
    }

    @Test
    public void testReadWeightRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteWeightRecord()));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newWeightRecords.size()).isEqualTo(0);
    }

    @Test
    public void testAggregation_weight() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(
                        getBaseWeightRecord(5.0),
                        getBaseWeightRecord(10.0),
                        getBaseWeightRecord(15.0));
        AggregateRecordsResponse<Mass> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(WEIGHT_AVG)
                                .addAggregationType(WEIGHT_MAX)
                                .addAggregationType(WEIGHT_MIN)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        Mass maxWeight = response.get(WEIGHT_MAX);
        Mass minWeight = response.get(WEIGHT_MIN);
        Mass avgWeight = response.get(WEIGHT_AVG);
        assertThat(maxWeight).isNotNull();
        assertThat(maxWeight.getInGrams()).isEqualTo(15.0);
        assertThat(minWeight).isNotNull();
        assertThat(minWeight.getInGrams()).isEqualTo(5.0);
        assertThat(avgWeight).isNotNull();
        assertThat(avgWeight.getInGrams()).isEqualTo(10.0);
        Set<DataOrigin> dataOrigins = response.getDataOrigins(WEIGHT_AVG);
        for (DataOrigin itr : dataOrigins) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testDeleteWeightRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(WeightRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    static WeightRecord getBaseWeightRecord(Instant time, ZoneOffset zoneOffset) {
        return new WeightRecord.Builder(new Metadata.Builder().build(), time, Mass.fromGrams(50))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Test
    public void testDeleteWeightRecord_time_filters_local() throws InterruptedException {
        LocalTimeRangeFilter timeRangeFilter =
                new LocalTimeRangeFilter.Builder()
                        .setStartTime(LocalDateTime.of(2023, Month.APRIL, 29, 8, 30, 29))
                        .setEndTime(LocalDateTime.of(2023, Month.APRIL, 29, 8, 30, 31))
                        .build();
        LocalDateTime recordTime = LocalDateTime.of(2023, Month.APRIL, 29, 8, 30, 30);
        String id1 =
                TestUtils.insertRecordAndGetId(
                        getBaseWeightRecord(recordTime.toInstant(ZoneOffset.MIN), ZoneOffset.MIN));
        String id2 =
                TestUtils.insertRecordAndGetId(
                        getBaseWeightRecord(recordTime.toInstant(ZoneOffset.MAX), ZoneOffset.MAX));
        TestUtils.assertRecordFound(id1, WeightRecord.class);
        TestUtils.assertRecordFound(id2, WeightRecord.class);
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(WeightRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id1, WeightRecord.class);
        TestUtils.assertRecordNotFound(id2, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseWeightRecord(), getCompleteWeightRecord());
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
    public void testDeleteWeightRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseWeightRecord(), getCompleteWeightRecord());
        List<Record> insertedRecord = TestUtils.insertRecords(records);
        List<RecordIdFilter> recordIds = new ArrayList<>(records.size());
        for (Record record : insertedRecord) {
            recordIds.add(RecordIdFilter.fromId(record.getClass(), record.getMetadata().getId()));
        }

        TestUtils.verifyDeleteRecords(recordIds);
        for (Record record : records) {
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteWeightRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(WeightRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        WeightRecord.Builder builder =
                new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromGrams(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    @Test
    public void testUpdateRecords_validInput_dataBaseUpdatedSuccessfully()
            throws InterruptedException {

        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord()));

        // read inserted records and verify that the data is same as inserted.
        readWeightRecordUsingIds(insertedRecords);

        // Generate a new set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord());

        // Modify the uid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getWeightRecord_update(
                            updateRecords.get(itr),
                            insertedRecords.get(itr).getMetadata().getId(),
                            insertedRecords.get(itr).getMetadata().getClientRecordId()));
        }

        TestUtils.updateRecords(updateRecords);

        // assert the inserted data has been modified by reading the data.
        readWeightRecordUsingIds(updateRecords);
    }

    @Test
    public void testUpdateRecords_invalidInputRecords_noChangeInDataBase()
            throws InterruptedException {
        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord()));

        // read inserted records and verify that the data is same as inserted.
        readWeightRecordUsingIds(insertedRecords);

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord());

        // Modify the Uid of the updateRecords to the UUID that was present in the insert records,
        // leaving out alternate records so that they have a new UUID which is not present in the
        // dataBase.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getWeightRecord_update(
                            updateRecords.get(itr),
                            itr % 2 == 0
                                    ? insertedRecords.get(itr).getMetadata().getId()
                                    : UUID.randomUUID().toString(),
                            itr % 2 == 0
                                    ? insertedRecords.get(itr).getMetadata().getId()
                                    : UUID.randomUUID().toString()));
        }

        try {
            TestUtils.updateRecords(updateRecords);
            Assert.fail("Expected to fail due to invalid records ids.");
        } catch (HealthConnectException exception) {
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        }

        // assert the inserted data has not been modified by reading the data.
        readWeightRecordUsingIds(insertedRecords);
    }

    @Test
    public void testUpdateRecords_recordWithInvalidPackageName_noChangeInDataBase()
            throws InterruptedException {
        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord()));

        // read inserted records and verify that the data is same as inserted.
        readWeightRecordUsingIds(insertedRecords);

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord());

        // Modify the Uuid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getWeightRecord_update(
                            updateRecords.get(itr),
                            insertedRecords.get(itr).getMetadata().getId(),
                            insertedRecords.get(itr).getMetadata().getClientRecordId()));
            //             adding an entry with invalid packageName.
            updateRecords.set(itr, getCompleteWeightRecord());
        }

        try {
            TestUtils.updateRecords(updateRecords);
            Assert.fail("Expected to fail due to invalid package.");
        } catch (Exception exception) {
            // verify that the testcase failed due to invalid argument exception.
            assertThat(exception).isNotNull();
        }

        // assert the inserted data has not been modified by reading the data.
        readWeightRecordUsingIds(insertedRecords);
    }

    @Test
    public void testInsertAndDeleteRecord_changelogs() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        ChangeLogTokenResponse tokenResponse =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .addRecordType(WeightRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest =
                new ChangeLogsRequest.Builder(tokenResponse.getToken()).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedLogs().size()).isEqualTo(0);

        List<Record> testRecord = Collections.singletonList(getCompleteWeightRecord());
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
        assertThat(
                        response.getUpsertedRecords().stream()
                                .map(Record::getMetadata)
                                .map(Metadata::getId)
                                .toList())
                .containsExactlyElementsIn(
                        testRecord.stream().map(Record::getMetadata).map(Metadata::getId).toList());
        assertThat(response.getDeletedLogs().size()).isEqualTo(0);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder().addRecordType(WeightRecord.class).build());
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testAggregatePeriod_withLocalDateTime() throws Exception {
        LocalDateTime endTimeLocal = LocalDateTime.now(ZoneOffset.UTC);
        Instant endTime = Instant.now();

        TestUtils.insertRecords(
                List.of(
                        getWeightRecordWithTime(endTime.minus(1, ChronoUnit.HOURS)),
                        getWeightRecordWithTime(endTime.minus(27, ChronoUnit.HOURS)),
                        getWeightRecordWithTime(endTime.minus(55, ChronoUnit.HOURS))));

        ZoneOffset currentOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());

        LocalTimeRangeFilter filter =
                new LocalTimeRangeFilter.Builder()
                        .setStartTime(endTimeLocal.minusDays(3))
                        .setEndTime(endTimeLocal)
                        .build();

        List<AggregateRecordsGroupedByPeriodResponse<Mass>> responses =
                TestUtils.getAggregateResponseGroupByPeriod(
                        new AggregateRecordsRequest.Builder<Mass>(filter)
                                .addAggregationType(WEIGHT_MIN)
                                .build(),
                        Period.ofDays(1));

        assertThat(responses).hasSize(3);
        LocalDateTime groupBoundary = endTimeLocal.minusDays(3);
        for (int i = 0; i < 3; i++) {
            assertThat(responses.get(i).get(WEIGHT_MIN)).isEqualTo(Mass.fromGrams(10.0));
            assertThat(responses.get(i).getZoneOffset(WEIGHT_MIN)).isEqualTo(currentOffset);
            assertThat(responses.get(i).getStartTime().getDayOfYear())
                    .isEqualTo(groupBoundary.getDayOfYear());
            groupBoundary = groupBoundary.plus(1, ChronoUnit.DAYS);
            assertThat(responses.get(i).getEndTime().getDayOfYear())
                    .isEqualTo(groupBoundary.getDayOfYear());
        }
    }


    @Test
    public void testAggregateDuration_withLocalDateTime_responsesAnswerAndBoundariesCorrect()
            throws Exception {
        LocalDateTime endTimeLocal = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startTimeLocal = endTimeLocal.minusDays(3);
        Instant endTime = Instant.now();

        TestUtils.insertRecords(
                List.of(
                        getWeightRecordWithTime(endTime.minus(1, ChronoUnit.HOURS)),
                        getWeightRecordWithTime(endTime.minus(27, ChronoUnit.HOURS)),
                        getWeightRecordWithTime(endTime.minus(55, ChronoUnit.HOURS))));

        List<AggregateRecordsGroupedByDurationResponse<Mass>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(startTimeLocal)
                                                .setEndTime(endTimeLocal)
                                                .build())
                                .addAggregationType(WEIGHT_MAX)
                                .build(),
                        Duration.ofDays(1));

        assertThat(responses).hasSize(3);
        Instant groupBoundary =
                startTimeLocal.toInstant(
                        ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        for (int i = 0; i < 3; i++) {
            assertThat(responses.get(i).get(WEIGHT_MAX)).isEqualTo(Mass.fromGrams(10.0));
            assertThat(responses.get(i).getZoneOffset(WEIGHT_MAX))
                    .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
            assertThat(responses.get(i).getStartTime().getEpochSecond())
                    .isEqualTo(groupBoundary.getEpochSecond());
            groupBoundary = groupBoundary.plus(1, ChronoUnit.DAYS);
            assertThat(responses.get(i).getEndTime().getEpochSecond())
                    .isEqualTo(groupBoundary.getEpochSecond());
        }
    }

    @Test
    public void testAggregateLocalFilter_minOffsetRecord() throws Exception {
        LocalDateTime endTimeLocal = LocalDateTime.now(ZoneOffset.UTC);
        Instant endTimeInstant = Instant.now();

        AggregateRecordsResponse<Mass> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(endTimeLocal.minusHours(25))
                                                .setEndTime(endTimeLocal.minusHours(15))
                                                .build())
                                .addAggregationType(WEIGHT_MAX)
                                .addAggregationType(WEIGHT_MIN)
                                .addAggregationType(WEIGHT_AVG)
                                .build(),
                        List.of(
                                new WeightRecord.Builder(
                                                TestUtils.generateMetadata(),
                                                endTimeInstant,
                                                Mass.fromGrams(10.0))
                                        .setZoneOffset(ZoneOffset.MIN)
                                        .build(),
                                new WeightRecord.Builder(
                                                TestUtils.generateMetadata(),
                                                endTimeInstant,
                                                Mass.fromGrams(20.0))
                                        .setZoneOffset(ZoneOffset.MIN)
                                        .build()));

        assertThat(response.get(WEIGHT_MAX)).isNotNull();
        assertThat(response.get(WEIGHT_MAX)).isEqualTo(Mass.fromGrams(20.0));
        assertThat(response.get(WEIGHT_MIN)).isNotNull();
        assertThat(response.get(WEIGHT_MIN)).isEqualTo(Mass.fromGrams(10.0));
        assertThat(response.get(WEIGHT_AVG)).isNotNull();
        assertThat(response.get(WEIGHT_AVG)).isEqualTo(Mass.fromGrams(15.0));
    }

    @Test
    public void testAggregateLocalFilter_offsetRecordAndFilter() throws Exception {
        testOffset(ZoneOffset.MAX);
        testOffset(ZoneOffset.ofHours(1));
        testOffset(ZoneOffset.UTC);
        testOffset(ZoneOffset.ofHours(-1));
        testOffset(ZoneOffset.MIN);
    }

    private void testOffset(ZoneOffset offset) throws InterruptedException {
        Instant endTimeInstant = Instant.now();
        LocalDateTime endTimeLocal = LocalDateTime.ofInstant(endTimeInstant, offset);

        AggregateRecordsResponse<Mass> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(endTimeLocal.minusMinutes(1))
                                                .setEndTime(endTimeLocal.plusMinutes(1))
                                                .build())
                                .addAggregationType(WEIGHT_MAX)
                                .addAggregationType(WEIGHT_MIN)
                                .addAggregationType(WEIGHT_AVG)
                                .build(),
                        List.of(
                                new WeightRecord.Builder(
                                                TestUtils.generateMetadata(),
                                                endTimeInstant,
                                                Mass.fromGrams(10.0))
                                        .setZoneOffset(offset)
                                        .build(),
                                new WeightRecord.Builder(
                                                TestUtils.generateMetadata(),
                                                endTimeInstant,
                                                Mass.fromGrams(20.0))
                                        .setZoneOffset(offset)
                                        .build()));

        assertThat(response.get(WEIGHT_MAX)).isNotNull();
        assertThat(response.get(WEIGHT_MAX)).isEqualTo(Mass.fromGrams(20.0));
        assertThat(response.get(WEIGHT_MIN)).isNotNull();
        assertThat(response.get(WEIGHT_MIN)).isEqualTo(Mass.fromGrams(10.0));
        assertThat(response.get(WEIGHT_AVG)).isNotNull();
        assertThat(response.get(WEIGHT_AVG)).isEqualTo(Mass.fromGrams(15.0));
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder().addRecordType(WeightRecord.class).build());
    }

    @Test
    public void testAggregateLocalFilter_yearPeriod() throws Exception {
        LocalDateTime endTimeLocal = LocalDateTime.now(ZoneOffset.UTC);
        Instant endTimeInstant = Instant.now();

        TestUtils.insertRecords(
                List.of(
                        new WeightRecord.Builder(
                                        TestUtils.generateMetadata(),
                                        endTimeInstant.minus(400, ChronoUnit.DAYS),
                                        Mass.fromGrams(10.0))
                                .setZoneOffset(ZoneOffset.MIN)
                                .build()));
        List<AggregateRecordsGroupedByPeriodResponse<Mass>> responses =
                TestUtils.getAggregateResponseGroupByPeriod(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(endTimeLocal.minusYears(2))
                                                .setEndTime(endTimeLocal)
                                                .build())
                                .addAggregationType(WEIGHT_MAX)
                                .build(),
                        Period.ofYears(1));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).get(WEIGHT_MAX)).isEqualTo(Mass.fromGrams(10.0));
        assertThat(responses.get(1).get(WEIGHT_MAX)).isNull();
    }

    private void readWeightRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<WeightRecord> result = TestUtils.readRecords(request.build());
        assertThat(result.size()).isEqualTo(insertedRecord.size());
        assertThat(result).containsExactlyElementsIn(insertedRecord);
    }

    private void readWeightRecordUsingIds(List<Record> recordList) throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class);
        for (Record record : recordList) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(WeightRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<WeightRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(recordList.size());
        assertThat(result).containsExactlyElementsIn(recordList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWeightRecord_invalidValue() {
        new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromGrams(1000000.1))
                .build();
    }

    WeightRecord getWeightRecord_update(Record record, String id, String clientRecordId) {
        Metadata metadata = record.getMetadata();
        Metadata metadataWithId =
                new Metadata.Builder()
                        .setId(id)
                        .setClientRecordId(clientRecordId)
                        .setClientRecordVersion(metadata.getClientRecordVersion())
                        .setDataOrigin(metadata.getDataOrigin())
                        .setDevice(metadata.getDevice())
                        .setLastModifiedTime(metadata.getLastModifiedTime())
                        .build();
        return new WeightRecord.Builder(metadataWithId, Instant.now(), Mass.fromGrams(20.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }

    private static WeightRecord getBaseWeightRecord() {
        return new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromGrams(10.0))
                .build();
    }

    static WeightRecord getBaseWeightRecord(Instant time, double weight) {
        return new WeightRecord.Builder(
                        new Metadata.Builder().build(), time, Mass.fromGrams(weight))
                .build();
    }

    static WeightRecord getBaseWeightRecord(double weight) {
        return new WeightRecord.Builder(
                        new Metadata.Builder().setClientRecordId("WR" + Math.random()).build(),
                        Instant.now(),
                        Mass.fromGrams(weight))
                .build();
    }

    private static WeightRecord getCompleteWeightRecord() {
        return getWeightRecordWithTime(Instant.now());
    }

    private static WeightRecord getWeightRecordWithTime(Instant time) {
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
        testMetadataBuilder.setClientRecordId("WR" + Math.random());
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);

        return new WeightRecord.Builder(testMetadataBuilder.build(), time, Mass.fromGrams(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
