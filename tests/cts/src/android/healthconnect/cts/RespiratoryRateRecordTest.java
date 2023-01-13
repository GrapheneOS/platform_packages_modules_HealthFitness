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
import android.healthconnect.datatypes.RespiratoryRateRecord;
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
public class RespiratoryRateRecordTest {
    private static final String TAG = "RespiratoryRateRecordTest";

    @Test
    public void testInsertRespiratoryRateRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseRespiratoryRateRecord(), getCompleteRespiratoryRateRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadRespiratoryRateRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteRespiratoryRateRecord(), getCompleteRespiratoryRateRecord());
        readRespiratoryRateRecordUsingIds(recordList);
    }

    @Test
    public void testReadRespiratoryRateRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<RespiratoryRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RespiratoryRateRecord.class)
                        .addId("abc")
                        .build();
        List<RespiratoryRateRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadRespiratoryRateRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteRespiratoryRateRecord(), getCompleteRespiratoryRateRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readRespiratoryRateRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadRespiratoryRateRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<RespiratoryRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RespiratoryRateRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<RespiratoryRateRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadRespiratoryRateRecordUsingFilters_default() throws InterruptedException {
        List<RespiratoryRateRecord> oldRespiratoryRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RespiratoryRateRecord.class)
                                .build());
        RespiratoryRateRecord testRecord = getCompleteRespiratoryRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<RespiratoryRateRecord> newRespiratoryRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RespiratoryRateRecord.class)
                                .build());
        assertThat(newRespiratoryRateRecords.size())
                .isEqualTo(oldRespiratoryRateRecords.size() + 1);
        assertThat(
                        newRespiratoryRateRecords
                                .get(newRespiratoryRateRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadRespiratoryRateRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        RespiratoryRateRecord testRecord = getCompleteRespiratoryRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<RespiratoryRateRecord> newRespiratoryRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RespiratoryRateRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newRespiratoryRateRecords.size()).isEqualTo(1);
        assertThat(
                        newRespiratoryRateRecords
                                .get(newRespiratoryRateRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadRespiratoryRateRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<RespiratoryRateRecord> oldRespiratoryRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RespiratoryRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        RespiratoryRateRecord testRecord = getCompleteRespiratoryRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<RespiratoryRateRecord> newRespiratoryRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RespiratoryRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newRespiratoryRateRecords.size() - oldRespiratoryRateRecords.size())
                .isEqualTo(1);
        RespiratoryRateRecord newRecord =
                newRespiratoryRateRecords.get(newRespiratoryRateRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadRespiratoryRateRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteRespiratoryRateRecord()));
        List<RespiratoryRateRecord> newRespiratoryRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RespiratoryRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newRespiratoryRateRecords.size()).isEqualTo(0);
    }

    private void readRespiratoryRateRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<RespiratoryRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RespiratoryRateRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<RespiratoryRateRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            RespiratoryRateRecord other = (RespiratoryRateRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readRespiratoryRateRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<RespiratoryRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RespiratoryRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<RespiratoryRateRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            RespiratoryRateRecord other = (RespiratoryRateRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteRespiratoryRateRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteRespiratoryRateRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, RespiratoryRateRecord.class);
    }

    @Test
    public void testDeleteRespiratoryRateRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteRespiratoryRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(RespiratoryRateRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, RespiratoryRateRecord.class);
    }

    @Test
    public void testDeleteRespiratoryRateRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseRespiratoryRateRecord(), getCompleteRespiratoryRateRecord());
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
    public void testDeleteRespiratoryRateRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteRespiratoryRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, RespiratoryRateRecord.class);
    }

    @Test
    public void testDeleteRespiratoryRateRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteRespiratoryRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, RespiratoryRateRecord.class);
    }

    @Test
    public void testDeleteRespiratoryRateRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseRespiratoryRateRecord(), getCompleteRespiratoryRateRecord());
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
    public void testDeleteRespiratoryRateRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteRespiratoryRateRecord());
        TestUtils.verifyDeleteRecords(RespiratoryRateRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, RespiratoryRateRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        RespiratoryRateRecord.Builder builder =
                new RespiratoryRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), 10.0);

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static RespiratoryRateRecord getBaseRespiratoryRateRecord() {
        return new RespiratoryRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), 10.0)
                .build();
    }

    private static RespiratoryRateRecord getCompleteRespiratoryRateRecord() {
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
        testMetadataBuilder.setClientRecordId("RRR" + Math.random());

        return new RespiratoryRateRecord.Builder(testMetadataBuilder.build(), Instant.now(), 10.0)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
