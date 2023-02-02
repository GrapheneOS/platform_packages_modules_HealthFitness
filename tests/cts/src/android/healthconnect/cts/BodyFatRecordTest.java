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
import android.health.connect.datatypes.BodyFatRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Percentage;
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
public class BodyFatRecordTest {
    private static final String TAG = "BodyFatRecordTest";

    @Test
    public void testInsertBodyFatRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseBodyFatRecord(), getCompleteBodyFatRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadBodyFatRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBodyFatRecord(), getCompleteBodyFatRecord());
        readBodyFatRecordUsingIds(recordList);
    }

    @Test
    public void testReadBodyFatRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BodyFatRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyFatRecord.class).addId("abc").build();
        List<BodyFatRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBodyFatRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBodyFatRecord(), getCompleteBodyFatRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readBodyFatRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadBodyFatRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BodyFatRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyFatRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<BodyFatRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBodyFatRecordUsingFilters_default() throws InterruptedException {
        List<BodyFatRecord> oldBodyFatRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class).build());
        BodyFatRecord testRecord = getCompleteBodyFatRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyFatRecord> newBodyFatRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class).build());
        assertThat(newBodyFatRecords.size()).isEqualTo(oldBodyFatRecords.size() + 1);
        assertThat(newBodyFatRecords.get(newBodyFatRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadBodyFatRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        BodyFatRecord testRecord = getCompleteBodyFatRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyFatRecord> newBodyFatRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newBodyFatRecords.size()).isEqualTo(1);
        assertThat(newBodyFatRecords.get(newBodyFatRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadBodyFatRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<BodyFatRecord> oldBodyFatRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        BodyFatRecord testRecord = getCompleteBodyFatRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyFatRecord> newBodyFatRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newBodyFatRecords.size() - oldBodyFatRecords.size()).isEqualTo(1);
        BodyFatRecord newRecord = newBodyFatRecords.get(newBodyFatRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
        assertThat(newRecord.getPercentage()).isEqualTo(testRecord.getPercentage());
    }

    @Test
    public void testReadBodyFatRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteBodyFatRecord()));
        List<BodyFatRecord> newBodyFatRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyFatRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newBodyFatRecords.size()).isEqualTo(0);
    }

    private void readBodyFatRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BodyFatRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyFatRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<BodyFatRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BodyFatRecord other = (BodyFatRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readBodyFatRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BodyFatRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyFatRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(BodyFatRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<BodyFatRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BodyFatRecord other = (BodyFatRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteBodyFatRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyFatRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, BodyFatRecord.class);
    }

    @Test
    public void testDeleteBodyFatRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyFatRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BodyFatRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, BodyFatRecord.class);
    }

    @Test
    public void testDeleteBodyFatRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseBodyFatRecord(), getCompleteBodyFatRecord());
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
    public void testDeleteBodyFatRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyFatRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, BodyFatRecord.class);
    }

    @Test
    public void testDeleteBodyFatRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyFatRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, BodyFatRecord.class);
    }

    @Test
    public void testDeleteBodyFatRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseBodyFatRecord(), getCompleteBodyFatRecord());
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
    public void testDeleteBodyFatRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyFatRecord());
        TestUtils.verifyDeleteRecords(BodyFatRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, BodyFatRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        BodyFatRecord.Builder builder =
                new BodyFatRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Percentage.fromValue(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static BodyFatRecord getBaseBodyFatRecord() {
        return new BodyFatRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Percentage.fromValue(10.0))
                .build();
    }

    private static BodyFatRecord getCompleteBodyFatRecord() {
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
        testMetadataBuilder.setClientRecordId("BFR" + Math.random());

        return new BodyFatRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Percentage.fromValue(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
