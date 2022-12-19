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
import android.healthconnect.datatypes.ExerciseLapRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Length;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class ExerciseLapRecordTest {
    private static final String TAG = "ExerciseLapRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                ExerciseLapRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertExerciseLapRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseExerciseLapRecord(), getCompleteExerciseLapRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadExerciseLapRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteExerciseLapRecord(), getCompleteExerciseLapRecord());
        readExerciseLapRecordUsingIds(recordList);
    }

    @Test
    public void testReadExerciseLapRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<ExerciseLapRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseLapRecord.class)
                        .addId("abc")
                        .build();
        List<ExerciseLapRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadExerciseLapRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteExerciseLapRecord(), getCompleteExerciseLapRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readExerciseLapRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadExerciseLapRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<ExerciseLapRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseLapRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<ExerciseLapRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadExerciseLapRecordUsingFilters_default() throws InterruptedException {
        List<ExerciseLapRecord> oldExerciseLapRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseLapRecord.class)
                                .build());
        ExerciseLapRecord testRecord = getCompleteExerciseLapRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ExerciseLapRecord> newExerciseLapRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseLapRecord.class)
                                .build());
        assertThat(newExerciseLapRecords.size()).isEqualTo(oldExerciseLapRecords.size() + 1);
        assertThat(newExerciseLapRecords.get(newExerciseLapRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadExerciseLapRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        ExerciseLapRecord testRecord = getCompleteExerciseLapRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ExerciseLapRecord> newExerciseLapRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseLapRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newExerciseLapRecords.size()).isEqualTo(1);
        assertThat(newExerciseLapRecords.get(newExerciseLapRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadExerciseLapRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<ExerciseLapRecord> oldExerciseLapRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseLapRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        ExerciseLapRecord testRecord = getCompleteExerciseLapRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ExerciseLapRecord> newExerciseLapRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseLapRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newExerciseLapRecords.size() - oldExerciseLapRecords.size()).isEqualTo(1);
        ExerciseLapRecord newRecord = newExerciseLapRecords.get(newExerciseLapRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadExerciseLapRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteExerciseLapRecord()));
        List<ExerciseLapRecord> newExerciseLapRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseLapRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newExerciseLapRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteExerciseLapRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseLapRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, ExerciseLapRecord.class);
    }

    @Test
    public void testDeleteExerciseLapRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseLapRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(ExerciseLapRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, ExerciseLapRecord.class);
    }

    @Test
    public void testDeleteExerciseLapRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseExerciseLapRecord(), getCompleteExerciseLapRecord());
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
    public void testDeleteExerciseLapRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseLapRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, ExerciseLapRecord.class);
    }

    @Test
    public void testDeleteExerciseLapRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseLapRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, ExerciseLapRecord.class);
    }

    @Test
    public void testDeleteExerciseLapRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseExerciseLapRecord(), getCompleteExerciseLapRecord());
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
    public void testDeleteExerciseLapRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseLapRecord());
        TestUtils.verifyDeleteRecords(ExerciseLapRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, ExerciseLapRecord.class);
    }

    private void readExerciseLapRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<ExerciseLapRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseLapRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<ExerciseLapRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ExerciseLapRecord other = (ExerciseLapRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readExerciseLapRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<ExerciseLapRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseLapRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<ExerciseLapRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ExerciseLapRecord other = (ExerciseLapRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static ExerciseLapRecord getBaseExerciseLapRecord() {
        return new ExerciseLapRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now())
                .build();
    }

    static ExerciseLapRecord getCompleteExerciseLapRecord() {

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
        testMetadataBuilder.setClientRecordId("ELR" + Math.random());

        return new ExerciseLapRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now())
                .setLength(Length.fromMeters(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
