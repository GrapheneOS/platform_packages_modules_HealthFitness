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
import android.healthconnect.datatypes.IntermenstrualBleedingRecord;
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
public class IntermenstrualBleedingRecordTest {
    private static final String TAG = "IntermenstrualBleedingRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                IntermenstrualBleedingRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertIntermenstrualBleedingRecord() throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseIntermenstrualBleedingRecord(),
                        getCompleteIntermenstrualBleedingRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadIntermenstrualBleedingRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteIntermenstrualBleedingRecord(),
                        getCompleteIntermenstrualBleedingRecord());
        readIntermenstrualBleedingRecordUsingIds(recordList);
    }

    @Test
    public void testReadIntermenstrualBleedingRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<IntermenstrualBleedingRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(IntermenstrualBleedingRecord.class)
                        .addId("abc")
                        .build();
        List<IntermenstrualBleedingRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadIntermenstrualBleedingRecord_usingClientRecordIds()
            throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteIntermenstrualBleedingRecord(),
                        getCompleteIntermenstrualBleedingRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readIntermenstrualBleedingRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadIntermenstrualBleedingRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<IntermenstrualBleedingRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(IntermenstrualBleedingRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<IntermenstrualBleedingRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadIntermenstrualBleedingRecordUsingFilters_default()
            throws InterruptedException {
        List<IntermenstrualBleedingRecord> oldIntermenstrualBleedingRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        IntermenstrualBleedingRecord.class)
                                .build());
        IntermenstrualBleedingRecord testRecord = getCompleteIntermenstrualBleedingRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<IntermenstrualBleedingRecord> newIntermenstrualBleedingRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        IntermenstrualBleedingRecord.class)
                                .build());
        assertThat(newIntermenstrualBleedingRecords.size())
                .isEqualTo(oldIntermenstrualBleedingRecords.size() + 1);
        assertThat(
                        newIntermenstrualBleedingRecords
                                .get(newIntermenstrualBleedingRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadIntermenstrualBleedingRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        IntermenstrualBleedingRecord testRecord = getCompleteIntermenstrualBleedingRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<IntermenstrualBleedingRecord> newIntermenstrualBleedingRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        IntermenstrualBleedingRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newIntermenstrualBleedingRecords.size()).isEqualTo(1);
        assertThat(
                        newIntermenstrualBleedingRecords
                                .get(newIntermenstrualBleedingRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadIntermenstrualBleedingRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<IntermenstrualBleedingRecord> oldIntermenstrualBleedingRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        IntermenstrualBleedingRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        IntermenstrualBleedingRecord testRecord = getCompleteIntermenstrualBleedingRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<IntermenstrualBleedingRecord> newIntermenstrualBleedingRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        IntermenstrualBleedingRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(
                        newIntermenstrualBleedingRecords.size()
                                - oldIntermenstrualBleedingRecords.size())
                .isEqualTo(1);
        IntermenstrualBleedingRecord newRecord =
                newIntermenstrualBleedingRecords.get(newIntermenstrualBleedingRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadIntermenstrualBleedingRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(
                Collections.singletonList(getCompleteIntermenstrualBleedingRecord()));
        List<IntermenstrualBleedingRecord> newIntermenstrualBleedingRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        IntermenstrualBleedingRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newIntermenstrualBleedingRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteIntermenstrualBleedingRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteIntermenstrualBleedingRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, IntermenstrualBleedingRecord.class);
    }

    @Test
    public void testDeleteIntermenstrualBleedingRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteIntermenstrualBleedingRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(IntermenstrualBleedingRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, IntermenstrualBleedingRecord.class);
    }

    @Test
    public void testDeleteIntermenstrualBleedingRecord_recordId_filters()
            throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseIntermenstrualBleedingRecord(),
                        getCompleteIntermenstrualBleedingRecord());
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
    public void testDeleteIntermenstrualBleedingRecord_dataOrigin_filters()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteIntermenstrualBleedingRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, IntermenstrualBleedingRecord.class);
    }

    @Test
    public void testDeleteIntermenstrualBleedingRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteIntermenstrualBleedingRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, IntermenstrualBleedingRecord.class);
    }

    @Test
    public void testDeleteIntermenstrualBleedingRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseIntermenstrualBleedingRecord(),
                        getCompleteIntermenstrualBleedingRecord());
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
    public void testDeleteIntermenstrualBleedingRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteIntermenstrualBleedingRecord());
        TestUtils.verifyDeleteRecords(IntermenstrualBleedingRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, IntermenstrualBleedingRecord.class);
    }

    private void readIntermenstrualBleedingRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<IntermenstrualBleedingRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(IntermenstrualBleedingRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<IntermenstrualBleedingRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            IntermenstrualBleedingRecord other =
                    (IntermenstrualBleedingRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readIntermenstrualBleedingRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<IntermenstrualBleedingRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(IntermenstrualBleedingRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(IntermenstrualBleedingRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<IntermenstrualBleedingRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            IntermenstrualBleedingRecord other =
                    (IntermenstrualBleedingRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static IntermenstrualBleedingRecord getBaseIntermenstrualBleedingRecord() {
        return new IntermenstrualBleedingRecord.Builder(
                        new Metadata.Builder().build(), Instant.now())
                .build();
    }

    static IntermenstrualBleedingRecord getIntermenstrualBleedingRecord(double power) {
        return new IntermenstrualBleedingRecord.Builder(
                        new Metadata.Builder().build(), Instant.now())
                .build();
    }

    private static IntermenstrualBleedingRecord getCompleteIntermenstrualBleedingRecord() {
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
        testMetadataBuilder.setClientRecordId("IMB" + Math.random());

        return new IntermenstrualBleedingRecord.Builder(testMetadataBuilder.build(), Instant.now())
                .build();
    }
}
