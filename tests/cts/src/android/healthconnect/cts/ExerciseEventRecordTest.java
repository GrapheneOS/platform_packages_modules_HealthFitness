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
import android.healthconnect.datatypes.ExerciseEventRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
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
public class ExerciseEventRecordTest {
    private static final String TAG = "ExerciseEventRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                ExerciseEventRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertExerciseEventRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseExerciseEventRecord(), getCompleteExerciseEventRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadExerciseEventRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteExerciseEventRecord(), getCompleteExerciseEventRecord());
        readExerciseEventRecordUsingIds(recordList);
    }

    @Test
    public void testReadExerciseEventRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<ExerciseEventRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseEventRecord.class)
                        .addId("abc")
                        .build();
        List<ExerciseEventRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadExerciseEventRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteExerciseEventRecord(), getCompleteExerciseEventRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readExerciseEventRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadExerciseEventRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<ExerciseEventRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseEventRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<ExerciseEventRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadExerciseEventRecordUsingFilters_default() throws InterruptedException {
        List<ExerciseEventRecord> oldExerciseEventRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseEventRecord.class)
                                .build());
        ExerciseEventRecord testRecord = getCompleteExerciseEventRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ExerciseEventRecord> newExerciseEventRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseEventRecord.class)
                                .build());
        assertThat(newExerciseEventRecords.size()).isEqualTo(oldExerciseEventRecords.size() + 1);
        assertThat(
                        newExerciseEventRecords
                                .get(newExerciseEventRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadExerciseEventRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        ExerciseEventRecord testRecord = getCompleteExerciseEventRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ExerciseEventRecord> newExerciseEventRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseEventRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newExerciseEventRecords.size()).isEqualTo(1);
        assertThat(
                        newExerciseEventRecords
                                .get(newExerciseEventRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadExerciseEventRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<ExerciseEventRecord> oldExerciseEventRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseEventRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        ExerciseEventRecord testRecord = getCompleteExerciseEventRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ExerciseEventRecord> newExerciseEventRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseEventRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newExerciseEventRecords.size() - oldExerciseEventRecords.size()).isEqualTo(1);
        ExerciseEventRecord newRecord =
                newExerciseEventRecords.get(newExerciseEventRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadExerciseEventRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteExerciseEventRecord()));
        List<ExerciseEventRecord> newExerciseEventRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseEventRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newExerciseEventRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteExerciseEventRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseEventRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, ExerciseEventRecord.class);
    }

    @Test
    public void testDeleteExerciseEventRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseEventRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(ExerciseEventRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, ExerciseEventRecord.class);
    }

    @Test
    public void testDeleteExerciseEventRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseExerciseEventRecord(), getCompleteExerciseEventRecord());
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
    public void testDeleteExerciseEventRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseEventRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, ExerciseEventRecord.class);
    }

    @Test
    public void testDeleteExerciseEventRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseEventRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, ExerciseEventRecord.class);
    }

    @Test
    public void testDeleteExerciseEventRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseExerciseEventRecord(), getCompleteExerciseEventRecord());
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
    public void testDeleteExerciseEventRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteExerciseEventRecord());
        TestUtils.verifyDeleteRecords(ExerciseEventRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, ExerciseEventRecord.class);
    }

    private void readExerciseEventRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<ExerciseEventRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseEventRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<ExerciseEventRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ExerciseEventRecord other = (ExerciseEventRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readExerciseEventRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<ExerciseEventRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseEventRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<ExerciseEventRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ExerciseEventRecord other = (ExerciseEventRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static ExerciseEventRecord getBaseExerciseEventRecord() {
        return new ExerciseEventRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), 1)
                .build();
    }

    static ExerciseEventRecord getCompleteExerciseEventRecord() {

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
        testMetadataBuilder.setClientRecordId("EER" + Math.random());

        return new ExerciseEventRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now(), 1)
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
