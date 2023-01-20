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
import android.healthconnect.datatypes.BodyTemperatureRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Temperature;
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
public class BodyTemperatureRecordTest {
    private static final String TAG = "BodyTemperatureRecordTest";

    @Test
    public void testInsertBodyTemperatureRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBodyTemperatureRecord(), getCompleteBodyTemperatureRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadBodyTemperatureRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteBodyTemperatureRecord(), getCompleteBodyTemperatureRecord());
        readBodyTemperatureRecordUsingIds(recordList);
    }

    @Test
    public void testReadBodyTemperatureRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BodyTemperatureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyTemperatureRecord.class)
                        .addId("abc")
                        .build();
        List<BodyTemperatureRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBodyTemperatureRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteBodyTemperatureRecord(), getCompleteBodyTemperatureRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readBodyTemperatureRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadBodyTemperatureRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BodyTemperatureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyTemperatureRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<BodyTemperatureRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBodyTemperatureRecordUsingFilters_default() throws InterruptedException {
        List<BodyTemperatureRecord> oldBodyTemperatureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyTemperatureRecord.class)
                                .build());
        BodyTemperatureRecord testRecord = getCompleteBodyTemperatureRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyTemperatureRecord> newBodyTemperatureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyTemperatureRecord.class)
                                .build());
        assertThat(newBodyTemperatureRecords.size())
                .isEqualTo(oldBodyTemperatureRecords.size() + 1);
        assertThat(
                        newBodyTemperatureRecords
                                .get(newBodyTemperatureRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBodyTemperatureRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        BodyTemperatureRecord testRecord = getCompleteBodyTemperatureRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyTemperatureRecord> newBodyTemperatureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyTemperatureRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newBodyTemperatureRecords.size()).isEqualTo(1);
        assertThat(
                        newBodyTemperatureRecords
                                .get(newBodyTemperatureRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBodyTemperatureRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<BodyTemperatureRecord> oldBodyTemperatureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyTemperatureRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        BodyTemperatureRecord testRecord = getCompleteBodyTemperatureRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyTemperatureRecord> newBodyTemperatureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyTemperatureRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newBodyTemperatureRecords.size() - oldBodyTemperatureRecords.size())
                .isEqualTo(1);
        BodyTemperatureRecord newRecord =
                newBodyTemperatureRecords.get(newBodyTemperatureRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadBodyTemperatureRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteBodyTemperatureRecord()));
        List<BodyTemperatureRecord> newBodyTemperatureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyTemperatureRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newBodyTemperatureRecords.size()).isEqualTo(0);
    }

    private void readBodyTemperatureRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BodyTemperatureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyTemperatureRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<BodyTemperatureRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BodyTemperatureRecord other = (BodyTemperatureRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readBodyTemperatureRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BodyTemperatureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyTemperatureRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(BodyTemperatureRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<BodyTemperatureRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BodyTemperatureRecord other = (BodyTemperatureRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteBodyTemperatureRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyTemperatureRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, BodyTemperatureRecord.class);
    }

    @Test
    public void testDeleteBodyTemperatureRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyTemperatureRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BodyTemperatureRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, BodyTemperatureRecord.class);
    }

    @Test
    public void testDeleteBodyTemperatureRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBodyTemperatureRecord(), getCompleteBodyTemperatureRecord());
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
    public void testDeleteBodyTemperatureRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyTemperatureRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, BodyTemperatureRecord.class);
    }

    @Test
    public void testDeleteBodyTemperatureRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyTemperatureRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, BodyTemperatureRecord.class);
    }

    @Test
    public void testDeleteBodyTemperatureRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBodyTemperatureRecord(), getCompleteBodyTemperatureRecord());
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
    public void testDeleteBodyTemperatureRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyTemperatureRecord());
        TestUtils.verifyDeleteRecords(BodyTemperatureRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, BodyTemperatureRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        BodyTemperatureRecord.Builder builder =
                new BodyTemperatureRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        1,
                        Temperature.fromCelsius(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static BodyTemperatureRecord getBaseBodyTemperatureRecord() {
        return new BodyTemperatureRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        1,
                        Temperature.fromCelsius(10.0))
                .build();
    }

    private static BodyTemperatureRecord getCompleteBodyTemperatureRecord() {
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
        testMetadataBuilder.setClientRecordId("BTR" + Math.random());

        return new BodyTemperatureRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        1,
                        Temperature.fromCelsius(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
