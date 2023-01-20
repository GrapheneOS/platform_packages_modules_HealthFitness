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
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.Vo2MaxRecord;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

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
public class Vo2MaxRecordTest {
    private static final String TAG = "Vo2MaxRecordTest";

    @Test
    public void testInsertVo2MaxRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseVo2MaxRecord(), getCompleteVo2MaxRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadVo2MaxRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteVo2MaxRecord(), getCompleteVo2MaxRecord());
        readVo2MaxRecordUsingIds(recordList);
    }

    @Test
    public void testReadVo2MaxRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<Vo2MaxRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(Vo2MaxRecord.class).addId("abc").build();
        List<Vo2MaxRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadVo2MaxRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteVo2MaxRecord(), getCompleteVo2MaxRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readVo2MaxRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadVo2MaxRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<Vo2MaxRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(Vo2MaxRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<Vo2MaxRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadVo2MaxRecordUsingFilters_default() throws InterruptedException {
        List<Vo2MaxRecord> oldVo2MaxRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(Vo2MaxRecord.class).build());
        Vo2MaxRecord testRecord = getCompleteVo2MaxRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<Vo2MaxRecord> newVo2MaxRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(Vo2MaxRecord.class).build());
        assertThat(newVo2MaxRecords.size()).isEqualTo(oldVo2MaxRecords.size() + 1);
        assertThat(newVo2MaxRecords.get(newVo2MaxRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadVo2MaxRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        Vo2MaxRecord testRecord = getCompleteVo2MaxRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<Vo2MaxRecord> newVo2MaxRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(Vo2MaxRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newVo2MaxRecords.size()).isEqualTo(1);
        assertThat(newVo2MaxRecords.get(newVo2MaxRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadVo2MaxRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<Vo2MaxRecord> oldVo2MaxRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(Vo2MaxRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        Vo2MaxRecord testRecord = getCompleteVo2MaxRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<Vo2MaxRecord> newVo2MaxRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(Vo2MaxRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newVo2MaxRecords.size() - oldVo2MaxRecords.size()).isEqualTo(1);
        Vo2MaxRecord newRecord = newVo2MaxRecords.get(newVo2MaxRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadVo2MaxRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteVo2MaxRecord()));
        List<Vo2MaxRecord> newVo2MaxRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(Vo2MaxRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newVo2MaxRecords.size()).isEqualTo(0);
    }

    private void readVo2MaxRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<Vo2MaxRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(Vo2MaxRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<Vo2MaxRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            Vo2MaxRecord other = (Vo2MaxRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readVo2MaxRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<Vo2MaxRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(Vo2MaxRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(Vo2MaxRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<Vo2MaxRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            Vo2MaxRecord other = (Vo2MaxRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteVo2MaxRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteVo2MaxRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, Vo2MaxRecord.class);
    }

    @Test
    public void testDeleteVo2MaxRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteVo2MaxRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(Vo2MaxRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, Vo2MaxRecord.class);
    }

    @Test
    public void testDeleteVo2MaxRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseVo2MaxRecord(), getCompleteVo2MaxRecord());
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
    public void testDeleteVo2MaxRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteVo2MaxRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, Vo2MaxRecord.class);
    }

    @Test
    public void testDeleteVo2MaxRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteVo2MaxRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, Vo2MaxRecord.class);
    }

    @Test
    public void testDeleteVo2MaxRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseVo2MaxRecord(), getCompleteVo2MaxRecord());
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
    public void testDeleteVo2MaxRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteVo2MaxRecord());
        TestUtils.verifyDeleteRecords(Vo2MaxRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, Vo2MaxRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        Vo2MaxRecord.Builder builder =
                new Vo2MaxRecord.Builder(new Metadata.Builder().build(), Instant.now(), 0, 10.0);

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static Vo2MaxRecord getBaseVo2MaxRecord() {
        return new Vo2MaxRecord.Builder(new Metadata.Builder().build(), Instant.now(), 1, 10.0)
                .build();
    }

    private static Vo2MaxRecord getCompleteVo2MaxRecord() {
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
        testMetadataBuilder.setClientRecordId("VMR" + Math.random());

        return new Vo2MaxRecord.Builder(testMetadataBuilder.build(), Instant.now(), 1, 10.0)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
