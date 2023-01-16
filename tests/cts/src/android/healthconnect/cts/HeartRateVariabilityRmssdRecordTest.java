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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.HeartRateVariabilityRmssdRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HeartRateVariabilityRmssdRecordTest {
    private static final String TAG = "HeartRateVariabilityRmssdRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                HeartRateVariabilityRmssdRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertHeartRateVariabilityRmssdRecord() throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseHeartRateVariabilityRmssdRecord(),
                        getCompleteHeartRateVariabilityRmssdRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteHeartRateVariabilityRmssdRecord(),
                        getCompleteHeartRateVariabilityRmssdRecord());
        readHeartRateVariabilityRmssdRecordUsingIds(recordList);
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<HeartRateVariabilityRmssdRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeartRateVariabilityRmssdRecord.class)
                        .addId("abc")
                        .build();
        List<HeartRateVariabilityRmssdRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecord_usingClientRecordIds()
            throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteHeartRateVariabilityRmssdRecord(),
                        getCompleteHeartRateVariabilityRmssdRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readHeartRateVariabilityRmssdRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<HeartRateVariabilityRmssdRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeartRateVariabilityRmssdRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<HeartRateVariabilityRmssdRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecordUsingFilters_default()
            throws InterruptedException {
        List<HeartRateVariabilityRmssdRecord> oldHeartRateVariabilityRmssdRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        HeartRateVariabilityRmssdRecord.class)
                                .build());
        HeartRateVariabilityRmssdRecord testRecord = getCompleteHeartRateVariabilityRmssdRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HeartRateVariabilityRmssdRecord> newHeartRateVariabilityRmssdRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        HeartRateVariabilityRmssdRecord.class)
                                .build());
        assertThat(newHeartRateVariabilityRmssdRecords.size())
                .isEqualTo(oldHeartRateVariabilityRmssdRecords.size() + 1);
        assertThat(
                        newHeartRateVariabilityRmssdRecords
                                .get(newHeartRateVariabilityRmssdRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        HeartRateVariabilityRmssdRecord testRecord = getCompleteHeartRateVariabilityRmssdRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HeartRateVariabilityRmssdRecord> newHeartRateVariabilityRmssdRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        HeartRateVariabilityRmssdRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newHeartRateVariabilityRmssdRecords.size()).isEqualTo(1);
        assertThat(
                        newHeartRateVariabilityRmssdRecords
                                .get(newHeartRateVariabilityRmssdRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<HeartRateVariabilityRmssdRecord> oldHeartRateVariabilityRmssdRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        HeartRateVariabilityRmssdRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        HeartRateVariabilityRmssdRecord testRecord = getCompleteHeartRateVariabilityRmssdRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HeartRateVariabilityRmssdRecord> newHeartRateVariabilityRmssdRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        HeartRateVariabilityRmssdRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(
                        newHeartRateVariabilityRmssdRecords.size()
                                - oldHeartRateVariabilityRmssdRecords.size())
                .isEqualTo(1);
        HeartRateVariabilityRmssdRecord newRecord =
                newHeartRateVariabilityRmssdRecords.get(
                        newHeartRateVariabilityRmssdRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadHeartRateVariabilityRmssdRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(
                Collections.singletonList(getCompleteHeartRateVariabilityRmssdRecord()));
        List<HeartRateVariabilityRmssdRecord> newHeartRateVariabilityRmssdRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        HeartRateVariabilityRmssdRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newHeartRateVariabilityRmssdRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteHeartRateVariabilityRmssdRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteHeartRateVariabilityRmssdRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, HeartRateVariabilityRmssdRecord.class);
    }

    @Test
    public void testDeleteHeartRateVariabilityRmssdRecord_time_filters()
            throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteHeartRateVariabilityRmssdRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(HeartRateVariabilityRmssdRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, HeartRateVariabilityRmssdRecord.class);
    }

    @Test
    public void testDeleteHeartRateVariabilityRmssdRecord_recordId_filters()
            throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseHeartRateVariabilityRmssdRecord(),
                        getCompleteHeartRateVariabilityRmssdRecord());
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
    public void testDeleteHeartRateVariabilityRmssdRecord_dataOrigin_filters()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteHeartRateVariabilityRmssdRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, HeartRateVariabilityRmssdRecord.class);
    }

    @Test
    public void testDeleteHeartRateVariabilityRmssdRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteHeartRateVariabilityRmssdRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, HeartRateVariabilityRmssdRecord.class);
    }

    @Test
    public void testDeleteHeartRateVariabilityRmssdRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseHeartRateVariabilityRmssdRecord(),
                        getCompleteHeartRateVariabilityRmssdRecord());
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
    public void testDeleteHeartRateVariabilityRmssdRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteHeartRateVariabilityRmssdRecord());
        TestUtils.verifyDeleteRecords(HeartRateVariabilityRmssdRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, HeartRateVariabilityRmssdRecord.class);
    }

    private void readHeartRateVariabilityRmssdRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<HeartRateVariabilityRmssdRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeartRateVariabilityRmssdRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<HeartRateVariabilityRmssdRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            HeartRateVariabilityRmssdRecord other =
                    (HeartRateVariabilityRmssdRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readHeartRateVariabilityRmssdRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<HeartRateVariabilityRmssdRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeartRateVariabilityRmssdRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<HeartRateVariabilityRmssdRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            HeartRateVariabilityRmssdRecord other =
                    (HeartRateVariabilityRmssdRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static HeartRateVariabilityRmssdRecord getBaseHeartRateVariabilityRmssdRecord() {
        return new HeartRateVariabilityRmssdRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), 9.9)
                .build();
    }

    static HeartRateVariabilityRmssdRecord getHeartRateVariabilityRmssdRecord(double power) {
        return new HeartRateVariabilityRmssdRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), 3.9)
                .build();
    }

    private static HeartRateVariabilityRmssdRecord getCompleteHeartRateVariabilityRmssdRecord() {
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
        testMetadataBuilder.setClientRecordId("HRV" + Math.random());

        return new HeartRateVariabilityRmssdRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), 3.0)
                .build();
    }
}
