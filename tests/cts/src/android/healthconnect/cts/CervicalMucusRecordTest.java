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
import android.healthconnect.datatypes.CervicalMucusRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
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
public class CervicalMucusRecordTest {
    private static final String TAG = "CervicalMucusRecordTest";

    @Test
    public void testInsertCervicalMucusRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseCervicalMucusRecord(), getCompleteCervicalMucusRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadCervicalMucusRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteCervicalMucusRecord(), getCompleteCervicalMucusRecord());
        readCervicalMucusRecordUsingIds(recordList);
    }

    @Test
    public void testReadCervicalMucusRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<CervicalMucusRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CervicalMucusRecord.class)
                        .addId("abc")
                        .build();
        List<CervicalMucusRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadCervicalMucusRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteCervicalMucusRecord(), getCompleteCervicalMucusRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readCervicalMucusRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadCervicalMucusRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<CervicalMucusRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CervicalMucusRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<CervicalMucusRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadCervicalMucusRecordUsingFilters_default() throws InterruptedException {
        List<CervicalMucusRecord> oldCervicalMucusRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(CervicalMucusRecord.class)
                                .build());
        CervicalMucusRecord testRecord = getCompleteCervicalMucusRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<CervicalMucusRecord> newCervicalMucusRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(CervicalMucusRecord.class)
                                .build());
        assertThat(newCervicalMucusRecords.size()).isEqualTo(oldCervicalMucusRecords.size() + 1);
        assertThat(
                        newCervicalMucusRecords
                                .get(newCervicalMucusRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadCervicalMucusRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        CervicalMucusRecord testRecord = getCompleteCervicalMucusRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<CervicalMucusRecord> newCervicalMucusRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(CervicalMucusRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newCervicalMucusRecords.size()).isEqualTo(1);
        assertThat(
                        newCervicalMucusRecords
                                .get(newCervicalMucusRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadCervicalMucusRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<CervicalMucusRecord> oldCervicalMucusRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(CervicalMucusRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        CervicalMucusRecord testRecord = getCompleteCervicalMucusRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<CervicalMucusRecord> newCervicalMucusRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(CervicalMucusRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newCervicalMucusRecords.size() - oldCervicalMucusRecords.size()).isEqualTo(1);
        CervicalMucusRecord newRecord =
                newCervicalMucusRecords.get(newCervicalMucusRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadCervicalMucusRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteCervicalMucusRecord()));
        List<CervicalMucusRecord> newCervicalMucusRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(CervicalMucusRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newCervicalMucusRecords.size()).isEqualTo(0);
    }

    private void readCervicalMucusRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<CervicalMucusRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CervicalMucusRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<CervicalMucusRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            CervicalMucusRecord other = (CervicalMucusRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readCervicalMucusRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<CervicalMucusRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CervicalMucusRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<CervicalMucusRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            CervicalMucusRecord other = (CervicalMucusRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteCervicalMucusRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteCervicalMucusRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, CervicalMucusRecord.class);
    }

    @Test
    public void testDeleteCervicalMucusRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteCervicalMucusRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(CervicalMucusRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, CervicalMucusRecord.class);
    }

    @Test
    public void testDeleteCervicalMucusRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseCervicalMucusRecord(), getCompleteCervicalMucusRecord());
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
    public void testDeleteCervicalMucusRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteCervicalMucusRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, CervicalMucusRecord.class);
    }

    @Test
    public void testDeleteCervicalMucusRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteCervicalMucusRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, CervicalMucusRecord.class);
    }

    @Test
    public void testDeleteCervicalMucusRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseCervicalMucusRecord(), getCompleteCervicalMucusRecord());
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
    public void testDeleteCervicalMucusRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteCervicalMucusRecord());
        TestUtils.verifyDeleteRecords(CervicalMucusRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, CervicalMucusRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        CervicalMucusRecord.Builder builder =
                new CervicalMucusRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), 1, 1);

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static CervicalMucusRecord getBaseCervicalMucusRecord() {
        return new CervicalMucusRecord.Builder(new Metadata.Builder().build(), Instant.now(), 1, 1)
                .build();
    }

    private static CervicalMucusRecord getCompleteCervicalMucusRecord() {
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
        testMetadataBuilder.setClientRecordId("CMR" + Math.random());

        return new CervicalMucusRecord.Builder(testMetadataBuilder.build(), Instant.now(), 1, 1)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
