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
import android.healthconnect.TimeInstantRangeFilter;
import android.healthconnect.datatypes.BloodPressureRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Pressure;
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
public class BloodPressureRecordTest {
    private static final String TAG = "BloodPressureRecordTest";

    @Test
    public void testInsertBloodPressureRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBloodPressureRecord(), getCompleteBloodPressureRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadBloodPressureRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBloodPressureRecord(), getCompleteBloodPressureRecord());
        readBloodPressureRecordUsingIds(recordList);
    }

    @Test
    public void testReadBloodPressureRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class)
                        .addId("abc")
                        .build();
        List<BloodPressureRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBloodPressureRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBloodPressureRecord(), getCompleteBloodPressureRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readBloodPressureRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadBloodPressureRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<BloodPressureRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBloodPressureRecordUsingFilters_default() throws InterruptedException {
        List<BloodPressureRecord> oldBloodPressureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BloodPressureRecord.class)
                                .build());
        BloodPressureRecord testRecord = getCompleteBloodPressureRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BloodPressureRecord> newBloodPressureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BloodPressureRecord.class)
                                .build());
        assertThat(newBloodPressureRecords.size()).isEqualTo(oldBloodPressureRecords.size() + 1);
        assertThat(
                        newBloodPressureRecords
                                .get(newBloodPressureRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBloodPressureRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        BloodPressureRecord testRecord = getCompleteBloodPressureRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BloodPressureRecord> newBloodPressureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BloodPressureRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newBloodPressureRecords.size()).isEqualTo(1);
        assertThat(
                        newBloodPressureRecords
                                .get(newBloodPressureRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBloodPressureRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<BloodPressureRecord> oldBloodPressureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BloodPressureRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        BloodPressureRecord testRecord = getCompleteBloodPressureRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BloodPressureRecord> newBloodPressureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BloodPressureRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newBloodPressureRecords.size() - oldBloodPressureRecords.size()).isEqualTo(1);
        BloodPressureRecord newRecord =
                newBloodPressureRecords.get(newBloodPressureRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadBloodPressureRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteBloodPressureRecord()));
        List<BloodPressureRecord> newBloodPressureRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BloodPressureRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newBloodPressureRecords.size()).isEqualTo(0);
    }

    private void readBloodPressureRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<BloodPressureRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BloodPressureRecord other = (BloodPressureRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readBloodPressureRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(BloodPressureRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<BloodPressureRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BloodPressureRecord other = (BloodPressureRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteBloodPressureRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBloodPressureRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, BloodPressureRecord.class);
    }

    @Test
    public void testDeleteBloodPressureRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBloodPressureRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BloodPressureRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, BloodPressureRecord.class);
    }

    @Test
    public void testDeleteBloodPressureRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBloodPressureRecord(), getCompleteBloodPressureRecord());
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
    public void testDeleteBloodPressureRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteBloodPressureRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, BloodPressureRecord.class);
    }

    @Test
    public void testDeleteBloodPressureRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBloodPressureRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, BloodPressureRecord.class);
    }

    @Test
    public void testDeleteBloodPressureRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBloodPressureRecord(), getCompleteBloodPressureRecord());
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
    public void testDeleteBloodPressureRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBloodPressureRecord());
        TestUtils.verifyDeleteRecords(BloodPressureRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, BloodPressureRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        BloodPressureRecord.Builder builder =
                new BloodPressureRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        1,
                        Pressure.fromMillimetersOfMercury(10.0),
                        Pressure.fromMillimetersOfMercury(10.0),
                        1);

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static BloodPressureRecord getBaseBloodPressureRecord() {
        return new BloodPressureRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        1,
                        Pressure.fromMillimetersOfMercury(10.0),
                        Pressure.fromMillimetersOfMercury(10.0),
                        1)
                .build();
    }

    private static BloodPressureRecord getCompleteBloodPressureRecord() {
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
        testMetadataBuilder.setClientRecordId("BPR" + Math.random());

        return new BloodPressureRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        1,
                        Pressure.fromMillimetersOfMercury(10.0),
                        Pressure.fromMillimetersOfMercury(10.0),
                        1)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
