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
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsCadenceRecord;
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
public class StepsCadenceRecordTest {

    private static final String TAG = "StepsCadenceRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                StepsCadenceRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertStepsCadenceRecord() throws InterruptedException {
        TestUtils.insertRecords(
                Arrays.asList(getBaseStepsCadenceRecord(), getCompleteStepsCadenceRecord()));
    }

    @Test
    public void testReadStepsCadenceRecord_usingIds() throws InterruptedException {
        testReadStepsCadenceRecordIds();
    }

    @Test
    public void testReadStepsCadenceRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsCadenceRecord.class)
                        .addId("abc")
                        .build();
        List<StepsCadenceRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadStepsCadenceRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteStepsCadenceRecord(), getCompleteStepsCadenceRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readStepsCadenceRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadStepsCadenceRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<StepsCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsCadenceRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<StepsCadenceRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadStepsCadenceRecordUsingFilters_default() throws InterruptedException {
        List<StepsCadenceRecord> oldStepsCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsCadenceRecord.class)
                                .build());
        StepsCadenceRecord testRecord = getCompleteStepsCadenceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<StepsCadenceRecord> newStepsCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsCadenceRecord.class)
                                .build());
        assertThat(newStepsCadenceRecords.size()).isEqualTo(oldStepsCadenceRecords.size() + 1);
        assertThat(newStepsCadenceRecords.get(newStepsCadenceRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadStepsCadenceRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        StepsCadenceRecord testRecord = getCompleteStepsCadenceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<StepsCadenceRecord> newStepsCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsCadenceRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newStepsCadenceRecords.size()).isEqualTo(1);
        assertThat(newStepsCadenceRecords.get(newStepsCadenceRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadStepsCadenceRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<StepsCadenceRecord> oldStepsCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsCadenceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        StepsCadenceRecord testRecord = getCompleteStepsCadenceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<StepsCadenceRecord> newStepsCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsCadenceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newStepsCadenceRecords.size() - oldStepsCadenceRecords.size()).isEqualTo(1);
        assertThat(newStepsCadenceRecords.get(newStepsCadenceRecords.size() - 1).equals(testRecord))
                .isTrue();
        StepsCadenceRecord newRecord =
                newStepsCadenceRecords.get(newStepsCadenceRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
        for (int idx = 0; idx < newRecord.getSamples().size(); idx++) {
            assertThat(newRecord.getSamples().get(idx).getTime().toEpochMilli())
                    .isEqualTo(testRecord.getSamples().get(idx).getTime().toEpochMilli());
            assertThat(newRecord.getSamples().get(idx).getRate())
                    .isEqualTo(testRecord.getSamples().get(idx).getRate());
        }
    }

    @Test
    public void testReadStepsCadenceRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteStepsCadenceRecord()));
        List<StepsCadenceRecord> newStepsCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsCadenceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newStepsCadenceRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteStepsCadenceRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsCadenceRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, StepsCadenceRecord.class);
    }

    @Test
    public void testDeleteStepsCadenceRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsCadenceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsCadenceRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, StepsCadenceRecord.class);
    }

    @Test
    public void testDeleteStepsCadenceRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseStepsCadenceRecord(), getCompleteStepsCadenceRecord());
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
    public void testDeleteStepsCadenceRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsCadenceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, StepsCadenceRecord.class);
    }

    @Test
    public void testDeleteStepsCadenceRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsCadenceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, StepsCadenceRecord.class);
    }

    @Test
    public void testDeleteStepsCadenceRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseStepsCadenceRecord(), getCompleteStepsCadenceRecord());
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
    public void testDeleteStepsCadenceRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteStepsCadenceRecord());
        TestUtils.verifyDeleteRecords(StepsCadenceRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, StepsCadenceRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        StepsCadenceRecord.Builder builder =
                new StepsCadenceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Collections.emptyList());

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void testReadStepsCadenceRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteStepsCadenceRecord(), getCompleteStepsCadenceRecord());
        readStepsCadenceRecordUsingIds(recordList);
    }

    private void readStepsCadenceRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<StepsCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsCadenceRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<StepsCadenceRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            StepsCadenceRecord other = (StepsCadenceRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readStepsCadenceRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<StepsCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsCadenceRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(StepsCadenceRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<StepsCadenceRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        assertThat(result.containsAll(insertedRecords)).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStepsCadenceRecord_invalidValue() {
        new StepsCadenceRecord.StepsCadenceRecordSample(10001.0, Instant.now().plusMillis(100));
    }

    private static StepsCadenceRecord getBaseStepsCadenceRecord() {
        StepsCadenceRecord.StepsCadenceRecordSample stepsCadenceRecord =
                new StepsCadenceRecord.StepsCadenceRecordSample(1, Instant.now().plusMillis(100));
        ArrayList<StepsCadenceRecord.StepsCadenceRecordSample> stepsCadenceRecords =
                new ArrayList<>();
        stepsCadenceRecords.add(stepsCadenceRecord);
        stepsCadenceRecords.add(stepsCadenceRecord);

        return new StepsCadenceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        stepsCadenceRecords)
                .build();
    }

    private static StepsCadenceRecord getCompleteStepsCadenceRecord() {
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
        testMetadataBuilder.setClientRecordId("SCR" + Math.random());
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);
        StepsCadenceRecord.StepsCadenceRecordSample stepsCadenceRecord =
                new StepsCadenceRecord.StepsCadenceRecordSample(1, Instant.now().plusMillis(100));
        ArrayList<StepsCadenceRecord.StepsCadenceRecordSample> stepsCadenceRecords =
                new ArrayList<>();
        stepsCadenceRecords.add(stepsCadenceRecord);
        stepsCadenceRecords.add(stepsCadenceRecord);

        return new StepsCadenceRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        stepsCadenceRecords)
                .build();
    }
}
