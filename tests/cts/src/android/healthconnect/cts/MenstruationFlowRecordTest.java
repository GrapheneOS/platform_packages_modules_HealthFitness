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
import android.healthconnect.datatypes.MenstruationFlowRecord;
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
public class MenstruationFlowRecordTest {
    private static final String TAG = "MenstruationFlowRecordTest";

    @Test
    public void testInsertMenstruationFlowRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseMenstruationFlowRecord(), getCompleteMenstruationFlowRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadMenstruationFlowRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteMenstruationFlowRecord(), getCompleteMenstruationFlowRecord());
        readMenstruationFlowRecordUsingIds(recordList);
    }

    @Test
    public void testReadMenstruationFlowRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<MenstruationFlowRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(MenstruationFlowRecord.class)
                        .addId("abc")
                        .build();
        List<MenstruationFlowRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadMenstruationFlowRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteMenstruationFlowRecord(), getCompleteMenstruationFlowRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readMenstruationFlowRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadMenstruationFlowRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<MenstruationFlowRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(MenstruationFlowRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<MenstruationFlowRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadMenstruationFlowRecordUsingFilters_default() throws InterruptedException {
        List<MenstruationFlowRecord> oldMenstruationFlowRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(MenstruationFlowRecord.class)
                                .build());
        MenstruationFlowRecord testRecord = getCompleteMenstruationFlowRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<MenstruationFlowRecord> newMenstruationFlowRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(MenstruationFlowRecord.class)
                                .build());
        assertThat(newMenstruationFlowRecords.size())
                .isEqualTo(oldMenstruationFlowRecords.size() + 1);
        assertThat(
                        newMenstruationFlowRecords
                                .get(newMenstruationFlowRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadMenstruationFlowRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        MenstruationFlowRecord testRecord = getCompleteMenstruationFlowRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<MenstruationFlowRecord> newMenstruationFlowRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(MenstruationFlowRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newMenstruationFlowRecords.size()).isEqualTo(1);
        assertThat(
                        newMenstruationFlowRecords
                                .get(newMenstruationFlowRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadMenstruationFlowRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<MenstruationFlowRecord> oldMenstruationFlowRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(MenstruationFlowRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        MenstruationFlowRecord testRecord = getCompleteMenstruationFlowRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<MenstruationFlowRecord> newMenstruationFlowRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(MenstruationFlowRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newMenstruationFlowRecords.size() - oldMenstruationFlowRecords.size())
                .isEqualTo(1);
        MenstruationFlowRecord newRecord =
                newMenstruationFlowRecords.get(newMenstruationFlowRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadMenstruationFlowRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteMenstruationFlowRecord()));
        List<MenstruationFlowRecord> newMenstruationFlowRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(MenstruationFlowRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newMenstruationFlowRecords.size()).isEqualTo(0);
    }

    private void readMenstruationFlowRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<MenstruationFlowRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(MenstruationFlowRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<MenstruationFlowRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            MenstruationFlowRecord other = (MenstruationFlowRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readMenstruationFlowRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<MenstruationFlowRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(MenstruationFlowRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<MenstruationFlowRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            MenstruationFlowRecord other = (MenstruationFlowRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteMenstruationFlowRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteMenstruationFlowRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, MenstruationFlowRecord.class);
    }

    @Test
    public void testDeleteMenstruationFlowRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteMenstruationFlowRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(MenstruationFlowRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, MenstruationFlowRecord.class);
    }

    @Test
    public void testDeleteMenstruationFlowRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseMenstruationFlowRecord(), getCompleteMenstruationFlowRecord());
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
    public void testDeleteMenstruationFlowRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteMenstruationFlowRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, MenstruationFlowRecord.class);
    }

    @Test
    public void testDeleteMenstruationFlowRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteMenstruationFlowRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, MenstruationFlowRecord.class);
    }

    @Test
    public void testDeleteMenstruationFlowRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseMenstruationFlowRecord(), getCompleteMenstruationFlowRecord());
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
    public void testDeleteMenstruationFlowRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteMenstruationFlowRecord());
        TestUtils.verifyDeleteRecords(MenstruationFlowRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, MenstruationFlowRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        MenstruationFlowRecord.Builder builder =
                new MenstruationFlowRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), 0);

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static MenstruationFlowRecord getBaseMenstruationFlowRecord() {
        return new MenstruationFlowRecord.Builder(new Metadata.Builder().build(), Instant.now(), 1)
                .build();
    }

    private static MenstruationFlowRecord getCompleteMenstruationFlowRecord() {
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
        testMetadataBuilder.setClientRecordId("MFR" + Math.random());

        return new MenstruationFlowRecord.Builder(testMetadataBuilder.build(), Instant.now(), 1)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
