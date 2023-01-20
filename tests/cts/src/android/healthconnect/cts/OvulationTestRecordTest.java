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
import android.healthconnect.datatypes.OvulationTestRecord;
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
public class OvulationTestRecordTest {
    private static final String TAG = "OvulationTestRecordTest";

    @Test
    public void testInsertOvulationTestRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseOvulationTestRecord(), getCompleteOvulationTestRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadOvulationTestRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteOvulationTestRecord(), getCompleteOvulationTestRecord());
        readOvulationTestRecordUsingIds(recordList);
    }

    @Test
    public void testReadOvulationTestRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<OvulationTestRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OvulationTestRecord.class)
                        .addId("abc")
                        .build();
        List<OvulationTestRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadOvulationTestRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteOvulationTestRecord(), getCompleteOvulationTestRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readOvulationTestRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadOvulationTestRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<OvulationTestRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OvulationTestRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<OvulationTestRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadOvulationTestRecordUsingFilters_default() throws InterruptedException {
        List<OvulationTestRecord> oldOvulationTestRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OvulationTestRecord.class)
                                .build());
        OvulationTestRecord testRecord = getCompleteOvulationTestRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<OvulationTestRecord> newOvulationTestRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OvulationTestRecord.class)
                                .build());
        assertThat(newOvulationTestRecords.size()).isEqualTo(oldOvulationTestRecords.size() + 1);
        assertThat(
                        newOvulationTestRecords
                                .get(newOvulationTestRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadOvulationTestRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        OvulationTestRecord testRecord = getCompleteOvulationTestRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<OvulationTestRecord> newOvulationTestRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OvulationTestRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newOvulationTestRecords.size()).isEqualTo(1);
        assertThat(
                        newOvulationTestRecords
                                .get(newOvulationTestRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadOvulationTestRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<OvulationTestRecord> oldOvulationTestRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OvulationTestRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        OvulationTestRecord testRecord = getCompleteOvulationTestRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<OvulationTestRecord> newOvulationTestRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OvulationTestRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newOvulationTestRecords.size() - oldOvulationTestRecords.size()).isEqualTo(1);
        OvulationTestRecord newRecord =
                newOvulationTestRecords.get(newOvulationTestRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadOvulationTestRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteOvulationTestRecord()));
        List<OvulationTestRecord> newOvulationTestRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(OvulationTestRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newOvulationTestRecords.size()).isEqualTo(0);
    }

    private void readOvulationTestRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<OvulationTestRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OvulationTestRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<OvulationTestRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            OvulationTestRecord other = (OvulationTestRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readOvulationTestRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<OvulationTestRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(OvulationTestRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(OvulationTestRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<OvulationTestRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            OvulationTestRecord other = (OvulationTestRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteOvulationTestRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteOvulationTestRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, OvulationTestRecord.class);
    }

    @Test
    public void testDeleteOvulationTestRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteOvulationTestRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(OvulationTestRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, OvulationTestRecord.class);
    }

    @Test
    public void testDeleteOvulationTestRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseOvulationTestRecord(), getCompleteOvulationTestRecord());
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
    public void testDeleteOvulationTestRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteOvulationTestRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, OvulationTestRecord.class);
    }

    @Test
    public void testDeleteOvulationTestRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteOvulationTestRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, OvulationTestRecord.class);
    }

    @Test
    public void testDeleteOvulationTestRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseOvulationTestRecord(), getCompleteOvulationTestRecord());
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
    public void testDeleteOvulationTestRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteOvulationTestRecord());
        TestUtils.verifyDeleteRecords(OvulationTestRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, OvulationTestRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        OvulationTestRecord.Builder builder =
                new OvulationTestRecord.Builder(new Metadata.Builder().build(), Instant.now(), 0);

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static OvulationTestRecord getBaseOvulationTestRecord() {
        return new OvulationTestRecord.Builder(new Metadata.Builder().build(), Instant.now(), 1)
                .build();
    }

    private static OvulationTestRecord getCompleteOvulationTestRecord() {
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
        testMetadataBuilder.setClientRecordId("OTR" + Math.random());

        return new OvulationTestRecord.Builder(testMetadataBuilder.build(), Instant.now(), 1)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
