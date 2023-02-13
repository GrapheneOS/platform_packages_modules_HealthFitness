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

import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.platform.test.annotations.AppModeFull;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class StepsRecordTest {
    private static final String TAG = "StepsRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void testInsertStepsRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseStepsRecord(), getCompleteStepsRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadStepsRecord_usingIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getCompleteStepsRecord(), getCompleteStepsRecord());
        readStepsRecordUsingIds(recordList);
    }

    @Test
    public void testReadStepsRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId("abc").build();
        assertThat(request.getRecordType()).isEqualTo(StepsRecord.class);
        assertThat(request.getRecordIdFilters()).isNotNull();
        List<StepsRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadStepsRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getCompleteStepsRecord(), getCompleteStepsRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readStepsRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadStepsRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<StepsRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadStepsRecordUsingFilters_default() throws InterruptedException {
        List<StepsRecord> oldStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setAscending(true)
                                .build());
        StepsRecord testRecord = getCompleteStepsRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setAscending(true)
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(oldStepsRecords.size() + 1);
        assertThat(newStepsRecords.get(newStepsRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadStepsRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        StepsRecord testRecord = getCompleteStepsRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(1);
        assertThat(newStepsRecords.get(newStepsRecords.size() - 1).equals(testRecord)).isTrue();
    }

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
        StepsRecord testRecord = getCompleteStepsRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newStepsRecords.size() - oldStepsRecords.size()).isEqualTo(1);
        StepsRecord newRecord = newStepsRecords.get(newStepsRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadStepsRecordUsingFilters_dataFilter_incorrect() throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteStepsRecord()));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .setAscending(false)
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(0);
    }

    @Test
    public void testReadStepsRecordUsingFilters_withPageSize() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getStepsRecord_minusDays(1), getStepsRecord_minusDays(2));
        TestUtils.insertRecords(recordList);
        Pair<List<StepsRecord>, Long> newStepsRecords =
                TestUtils.readRecordsWithPagination(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setPageSize(1)
                                .build());
        assertThat(newStepsRecords.first.size()).isEqualTo(1);
    }

    @Test
    public void testReadStepsRecordUsingFilters_withPageToken() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getStepsRecord_minusDays(1),
                        getStepsRecord_minusDays(2),
                        getStepsRecord_minusDays(3),
                        getStepsRecord_minusDays(4));
        TestUtils.insertRecords(recordList);
        Pair<List<StepsRecord>, Long> oldStepsRecord =
                TestUtils.readRecordsWithPagination(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setPageSize(1)
                                .setAscending(true)
                                .build());
        assertThat(oldStepsRecord.first.size()).isEqualTo(1);
        Pair<List<StepsRecord>, Long> newStepsRecords =
                TestUtils.readRecordsWithPagination(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setPageSize(1)
                                .setPageToken(oldStepsRecord.second)
                                .build());
        assertThat(newStepsRecords.first.size()).isEqualTo(1);
    }

    @Test
    public void testReadStepsRecordUsingFilters_withPageTokenReverse() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getStepsRecord_minusDays(1),
                        getStepsRecord_minusDays(2),
                        getStepsRecord_minusDays(3),
                        getStepsRecord_minusDays(4));
        TestUtils.insertRecords(recordList);
        Pair<List<StepsRecord>, Long> oldStepsRecord =
                TestUtils.readRecordsWithPagination(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setPageSize(1)
                                .setAscending(false)
                                .build());
        assertThat(oldStepsRecord.first.size()).isEqualTo(1);
        Pair<List<StepsRecord>, Long> newStepsRecords =
                TestUtils.readRecordsWithPagination(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setPageSize(1)
                                .setPageToken(oldStepsRecord.second)
                                .build());
        assertThat(newStepsRecords.first.size()).isEqualTo(1);
        assertThat(newStepsRecords.second).isNotEqualTo(oldStepsRecord.second);
        assertThat(newStepsRecords.second).isLessThan(oldStepsRecord.second);
    }

    @Test
    public void testStepsRecordUsingFilters_nextPageTokenEnd() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(TestUtils.getStepsRecord(), TestUtils.getStepsRecord());
        TestUtils.insertRecords(recordList);
        Pair<List<StepsRecord>, Long> oldStepsRecord =
                TestUtils.readRecordsWithPagination(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        Pair<List<StepsRecord>, Long> newStepsRecord;
        while (oldStepsRecord.second != -1) {
            newStepsRecord =
                    TestUtils.readRecordsWithPagination(
                            new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                    .setPageToken(oldStepsRecord.second)
                                    .build());
            if (newStepsRecord.second != -1) {
                assertThat(newStepsRecord.second).isGreaterThan(oldStepsRecord.second);
            }
            oldStepsRecord = newStepsRecord;
        }
        assertThat(oldStepsRecord.second).isEqualTo(-1);
    }

    @Test
    public void testReadStepsRecord_beforePermissionGrant() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getStepsRecord_minusDays(45),
                        getStepsRecord_minusDays(20),
                        getStepsRecord(10));
        TestUtils.insertRecords(recordList);
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        assertThat(newStepsRecords.size()).isEqualTo(2);
    }

    @Test
    public void testDeleteStepsRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteStepsRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteStepsRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseStepsRecord(), getCompleteStepsRecord());
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
    public void testDeleteStepsRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsRecord());
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
    public void testDeleteStepsRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteStepsRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseStepsRecord(), getCompleteStepsRecord());
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
    public void testDeleteStepsRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsRecord());
        TestUtils.verifyDeleteRecords(StepsRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        StepsRecord.Builder builder =
                new StepsRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10);

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void readStepsRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        ReadRecordsRequestUsingIds<StepsRecord> readRequest = request.build();
        assertThat(readRequest.getRecordType()).isNotNull();
        assertThat(readRequest.getRecordType()).isEqualTo(StepsRecord.class);
        List<StepsRecord> result = TestUtils.readRecords(readRequest);
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            StepsRecord other = (StepsRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readStepsRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(StepsRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<StepsRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            StepsRecord other = (StepsRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testAggregation_StepsCountTotal() throws Exception {
        List<Record> records = Arrays.asList(getStepsRecord(9), getStepsRecord(9));
        AggregateRecordsResponse<Long> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(STEPS_COUNT_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew = Arrays.asList(getStepsRecord(9), getStepsRecord(9));
        AggregateRecordsResponse<Long> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(STEPS_COUNT_TOTAL)
                                .build(),
                        recordNew);
        assertThat(newResponse.get(STEPS_COUNT_TOTAL)).isNotNull();
        assertThat(newResponse.get(STEPS_COUNT_TOTAL))
                .isEqualTo(oldResponse.get(STEPS_COUNT_TOTAL) + 18);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(STEPS_COUNT_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(STEPS_COUNT_TOTAL);
        for (DataOrigin itr : oldDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    static StepsRecord getBaseStepsRecord() {
        return new StepsRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10)
                .build();
    }

    static StepsRecord getStepsRecord(int count) {
        return new StepsRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        count)
                .build();
    }

    static StepsRecord getCompleteStepsRecord() {
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();

        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        testMetadataBuilder.setClientRecordId("SR" + Math.random());
        return new StepsRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10)
                .build();
    }

    static StepsRecord getStepsRecord_minusDays(int days) {
        return new StepsRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minus(days, ChronoUnit.DAYS),
                        Instant.now().minus(days, ChronoUnit.DAYS).plusMillis(1000),
                        10)
                .build();
    }
}
