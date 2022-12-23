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
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.OxygenSaturationRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Percentage;
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
public class OxygenSaturationRecordTest {
    private static final String TAG = "OxygenSaturationRecordTest";

    @Test
    public void testInsertOxygenSaturationRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseOxygenSaturationRecord(), getCompleteOxygenSaturationRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadOxygenSaturationRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteOxygenSaturationRecord(), getCompleteOxygenSaturationRecord());
        readOxygenSaturationRecordUsingIds(recordList);
    }

    @Test
    public void testReadOxygenSaturationRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<OxygenSaturationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OxygenSaturationRecord.class)
                        .addId("abc")
                        .build();
        List<OxygenSaturationRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadOxygenSaturationRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteOxygenSaturationRecord(), getCompleteOxygenSaturationRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readOxygenSaturationRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadOxygenSaturationRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<OxygenSaturationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OxygenSaturationRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<OxygenSaturationRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadOxygenSaturationRecordUsingFilters_default() throws InterruptedException {
        List<OxygenSaturationRecord> oldOxygenSaturationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OxygenSaturationRecord.class)
                                .build());
        OxygenSaturationRecord testRecord = getCompleteOxygenSaturationRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<OxygenSaturationRecord> newOxygenSaturationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OxygenSaturationRecord.class)
                                .build());
        assertThat(newOxygenSaturationRecords.size())
                .isEqualTo(oldOxygenSaturationRecords.size() + 1);
        assertThat(
                        newOxygenSaturationRecords
                                .get(newOxygenSaturationRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadOxygenSaturationRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        OxygenSaturationRecord testRecord = getCompleteOxygenSaturationRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<OxygenSaturationRecord> newOxygenSaturationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OxygenSaturationRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newOxygenSaturationRecords.size()).isEqualTo(1);
        assertThat(
                        newOxygenSaturationRecords
                                .get(newOxygenSaturationRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadOxygenSaturationRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<OxygenSaturationRecord> oldOxygenSaturationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OxygenSaturationRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        OxygenSaturationRecord testRecord = getCompleteOxygenSaturationRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<OxygenSaturationRecord> newOxygenSaturationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OxygenSaturationRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newOxygenSaturationRecords.size() - oldOxygenSaturationRecords.size())
                .isEqualTo(1);
        OxygenSaturationRecord newRecord =
                newOxygenSaturationRecords.get(newOxygenSaturationRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadOxygenSaturationRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteOxygenSaturationRecord()));
        List<OxygenSaturationRecord> newOxygenSaturationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OxygenSaturationRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newOxygenSaturationRecords.size()).isEqualTo(0);
    }

    private void readOxygenSaturationRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<OxygenSaturationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OxygenSaturationRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<OxygenSaturationRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            OxygenSaturationRecord other = (OxygenSaturationRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readOxygenSaturationRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<OxygenSaturationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OxygenSaturationRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(OxygenSaturationRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<OxygenSaturationRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            OxygenSaturationRecord other = (OxygenSaturationRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteOxygenSaturationRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteOxygenSaturationRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, OxygenSaturationRecord.class);
    }

    @Test
    public void testDeleteOxygenSaturationRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteOxygenSaturationRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(OxygenSaturationRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, OxygenSaturationRecord.class);
    }

    @Test
    public void testDeleteOxygenSaturationRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseOxygenSaturationRecord(), getCompleteOxygenSaturationRecord());
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
    public void testDeleteOxygenSaturationRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteOxygenSaturationRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, OxygenSaturationRecord.class);
    }

    @Test
    public void testDeleteOxygenSaturationRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteOxygenSaturationRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, OxygenSaturationRecord.class);
    }

    @Test
    public void testDeleteOxygenSaturationRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseOxygenSaturationRecord(), getCompleteOxygenSaturationRecord());
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
    public void testDeleteOxygenSaturationRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteOxygenSaturationRecord());
        TestUtils.verifyDeleteRecords(OxygenSaturationRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, OxygenSaturationRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        OxygenSaturationRecord.Builder builder =
                new OxygenSaturationRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Percentage.fromValue(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static OxygenSaturationRecord getBaseOxygenSaturationRecord() {
        return new OxygenSaturationRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Percentage.fromValue(10.0))
                .build();
    }

    private static OxygenSaturationRecord getCompleteOxygenSaturationRecord() {
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
        testMetadataBuilder.setClientRecordId("OSR" + Math.random());

        return new OxygenSaturationRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Percentage.fromValue(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
