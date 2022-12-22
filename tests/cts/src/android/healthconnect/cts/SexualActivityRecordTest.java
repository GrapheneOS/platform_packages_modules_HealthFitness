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
import android.healthconnect.datatypes.SexualActivityRecord;
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
public class SexualActivityRecordTest {
    private static final String TAG = "SexualActivityRecordTest";

    @Test
    public void testInsertSexualActivityRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseSexualActivityRecord(), getCompleteSexualActivityRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadSexualActivityRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteSexualActivityRecord(), getCompleteSexualActivityRecord());
        readSexualActivityRecordUsingIds(recordList);
    }

    @Test
    public void testReadSexualActivityRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<SexualActivityRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(SexualActivityRecord.class)
                        .addId("abc")
                        .build();
        List<SexualActivityRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadSexualActivityRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteSexualActivityRecord(), getCompleteSexualActivityRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readSexualActivityRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadSexualActivityRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<SexualActivityRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(SexualActivityRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<SexualActivityRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadSexualActivityRecordUsingFilters_default() throws InterruptedException {
        List<SexualActivityRecord> oldSexualActivityRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SexualActivityRecord.class)
                                .build());
        SexualActivityRecord testRecord = getCompleteSexualActivityRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<SexualActivityRecord> newSexualActivityRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SexualActivityRecord.class)
                                .build());
        assertThat(newSexualActivityRecords.size()).isEqualTo(oldSexualActivityRecords.size() + 1);
        assertThat(
                        newSexualActivityRecords
                                .get(newSexualActivityRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadSexualActivityRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        SexualActivityRecord testRecord = getCompleteSexualActivityRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<SexualActivityRecord> newSexualActivityRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SexualActivityRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newSexualActivityRecords.size()).isEqualTo(1);
        assertThat(
                        newSexualActivityRecords
                                .get(newSexualActivityRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadSexualActivityRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<SexualActivityRecord> oldSexualActivityRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SexualActivityRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        SexualActivityRecord testRecord = getCompleteSexualActivityRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<SexualActivityRecord> newSexualActivityRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SexualActivityRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newSexualActivityRecords.size() - oldSexualActivityRecords.size()).isEqualTo(1);
        SexualActivityRecord newRecord =
                newSexualActivityRecords.get(newSexualActivityRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadSexualActivityRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteSexualActivityRecord()));
        List<SexualActivityRecord> newSexualActivityRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SexualActivityRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newSexualActivityRecords.size()).isEqualTo(0);
    }

    private void readSexualActivityRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<SexualActivityRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(SexualActivityRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<SexualActivityRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            SexualActivityRecord other = (SexualActivityRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readSexualActivityRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<SexualActivityRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(SexualActivityRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<SexualActivityRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            SexualActivityRecord other = (SexualActivityRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteSexualActivityRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteSexualActivityRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, SexualActivityRecord.class);
    }

    @Test
    public void testDeleteSexualActivityRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteSexualActivityRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(SexualActivityRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, SexualActivityRecord.class);
    }

    @Test
    public void testDeleteSexualActivityRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseSexualActivityRecord(), getCompleteSexualActivityRecord());
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
    public void testDeleteSexualActivityRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteSexualActivityRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, SexualActivityRecord.class);
    }

    @Test
    public void testDeleteSexualActivityRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteSexualActivityRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, SexualActivityRecord.class);
    }

    @Test
    public void testDeleteSexualActivityRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseSexualActivityRecord(), getCompleteSexualActivityRecord());
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
    public void testDeleteSexualActivityRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteSexualActivityRecord());
        TestUtils.verifyDeleteRecords(SexualActivityRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, SexualActivityRecord.class);
    }

    private static SexualActivityRecord getBaseSexualActivityRecord() {
        return new SexualActivityRecord.Builder(new Metadata.Builder().build(), Instant.now(), 1)
                .build();
    }

    private static SexualActivityRecord getCompleteSexualActivityRecord() {
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
        testMetadataBuilder.setClientRecordId("SAR" + Math.random());

        return new SexualActivityRecord.Builder(testMetadataBuilder.build(), Instant.now(), 1)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
