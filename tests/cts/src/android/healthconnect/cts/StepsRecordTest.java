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

import static android.healthconnect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class StepsRecordTest {
    private static final String TAG = "StepsRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                StepsRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
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
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        StepsRecord testRecord = getCompleteStepsRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<StepsRecord> newStepsRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        assertThat(newStepsRecords.size()).isEqualTo(oldStepsRecords.size() + 1);
        assertThat(newStepsRecords.get(newStepsRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadStepsRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
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
                                .build());
        assertThat(newStepsRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteStepsRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    @Test
    public void testDeleteStepsRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
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
    public void testDeleteStepsRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsRecord());
        TestUtils.verifyDeleteRecords(StepsRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, StepsRecord.class);
    }

    private void readStepsRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<StepsRecord> result = TestUtils.readRecords(request.build());
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
        List<StepsRecord> result = TestUtils.readRecords(request.build());
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
        List<Record> records = Arrays.asList(getStepsRecord(1000), getStepsRecord(1000));
        AggregateRecordsResponse<Long> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(STEPS_COUNT_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew = Arrays.asList(getStepsRecord(1000), getStepsRecord(1000));
        AggregateRecordsResponse<Long> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(STEPS_COUNT_TOTAL)
                                .build(),
                        recordNew);
        assertThat(newResponse.get(STEPS_COUNT_TOTAL)).isNotNull();
        assertThat(newResponse.get(STEPS_COUNT_TOTAL))
                .isEqualTo(oldResponse.get(STEPS_COUNT_TOTAL) + 2000);
    }

    static StepsRecord getBaseStepsRecord() {
        return new StepsRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), 10)
                .build();
    }

    static StepsRecord getStepsRecord(int count) {
        return new StepsRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), count)
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
                        testMetadataBuilder.build(), Instant.now(), Instant.now(), 10)
                .build();
    }
}
