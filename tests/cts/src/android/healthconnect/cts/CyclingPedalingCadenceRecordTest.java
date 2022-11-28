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
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class CyclingPedalingCadenceRecordTest {

    private static final String TAG = "CyclingPedalingCadenceRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                CyclingPedalingCadenceRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertCyclingPedalingCadenceRecord() throws InterruptedException {
        TestUtils.insertRecords(
                Arrays.asList(
                        getBaseCyclingPedalingCadenceRecord(),
                        getCompleteCyclingPedalingCadenceRecord()));
    }

    @Test
    public void testReadCyclingPedalingCadenceRecord_usingIds() throws InterruptedException {
        testReadCyclingPedalingCadenceRecordIds();
    }

    @Test
    public void testReadCyclingPedalingCadenceRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<CyclingPedalingCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CyclingPedalingCadenceRecord.class)
                        .addId("abc")
                        .build();
        List<CyclingPedalingCadenceRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadCyclingPedalingCadenceRecord_usingClientRecordIds()
            throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteCyclingPedalingCadenceRecord(),
                        getCompleteCyclingPedalingCadenceRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readCyclingPedalingCadenceRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadCyclingPedalingCadenceRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<CyclingPedalingCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CyclingPedalingCadenceRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<CyclingPedalingCadenceRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadCyclingPedalingCadenceRecordUsingFilters_default()
            throws InterruptedException {
        List<CyclingPedalingCadenceRecord> oldCyclingPedalingCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        CyclingPedalingCadenceRecord.class)
                                .build());
        CyclingPedalingCadenceRecord testRecord = getCompleteCyclingPedalingCadenceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<CyclingPedalingCadenceRecord> newCyclingPedalingCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        CyclingPedalingCadenceRecord.class)
                                .build());
        assertThat(newCyclingPedalingCadenceRecords.size())
                .isEqualTo(oldCyclingPedalingCadenceRecords.size() + 1);
        assertThat(
                        newCyclingPedalingCadenceRecords
                                .get(newCyclingPedalingCadenceRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadCyclingPedalingCadenceRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        CyclingPedalingCadenceRecord testRecord = getCompleteCyclingPedalingCadenceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<CyclingPedalingCadenceRecord> newCyclingPedalingCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        CyclingPedalingCadenceRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newCyclingPedalingCadenceRecords.size()).isEqualTo(1);
        assertThat(
                        newCyclingPedalingCadenceRecords
                                .get(newCyclingPedalingCadenceRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadCyclingPedalingCadenceRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<CyclingPedalingCadenceRecord> oldCyclingPedalingCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        CyclingPedalingCadenceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        CyclingPedalingCadenceRecord testRecord = getCompleteCyclingPedalingCadenceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<CyclingPedalingCadenceRecord> newCyclingPedalingCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        CyclingPedalingCadenceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(
                        newCyclingPedalingCadenceRecords.size()
                                - oldCyclingPedalingCadenceRecords.size())
                .isEqualTo(1);
        assertThat(
                        newCyclingPedalingCadenceRecords
                                .get(newCyclingPedalingCadenceRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadCyclingPedalingCadenceRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(
                Collections.singletonList(getCompleteCyclingPedalingCadenceRecord()));
        List<CyclingPedalingCadenceRecord> newCyclingPedalingCadenceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        CyclingPedalingCadenceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newCyclingPedalingCadenceRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteCyclingPedalingCadenceRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteCyclingPedalingCadenceRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, CyclingPedalingCadenceRecord.class);
    }

    @Test
    public void testDeleteCyclingPedalingCadenceRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteCyclingPedalingCadenceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(CyclingPedalingCadenceRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, CyclingPedalingCadenceRecord.class);
    }

    @Test
    public void testDeleteCyclingPedalingCadenceRecord_recordId_filters()
            throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseCyclingPedalingCadenceRecord(),
                        getCompleteCyclingPedalingCadenceRecord());
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
    public void testDeleteCyclingPedalingCadenceRecord_dataOrigin_filters()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteCyclingPedalingCadenceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, CyclingPedalingCadenceRecord.class);
    }

    @Test
    public void testDeleteCyclingPedalingCadenceRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteCyclingPedalingCadenceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, CyclingPedalingCadenceRecord.class);
    }

    @Test
    public void testDeleteCyclingPedalingCadenceRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseCyclingPedalingCadenceRecord(),
                        getCompleteCyclingPedalingCadenceRecord());
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
    public void testDeleteCyclingPedalingCadenceRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteCyclingPedalingCadenceRecord());
        TestUtils.verifyDeleteRecords(CyclingPedalingCadenceRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, CyclingPedalingCadenceRecord.class);
    }

    private void testReadCyclingPedalingCadenceRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteCyclingPedalingCadenceRecord(),
                        getCompleteCyclingPedalingCadenceRecord());
        readCyclingPedalingCadenceRecordUsingIds(recordList);
    }

    private void readCyclingPedalingCadenceRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<CyclingPedalingCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CyclingPedalingCadenceRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<CyclingPedalingCadenceRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            CyclingPedalingCadenceRecord other =
                    (CyclingPedalingCadenceRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readCyclingPedalingCadenceRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<CyclingPedalingCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CyclingPedalingCadenceRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<CyclingPedalingCadenceRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).equals(insertedRecords.get(i))).isTrue();
        }
    }

    private static CyclingPedalingCadenceRecord getBaseCyclingPedalingCadenceRecord() {
        CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
                cyclingPedalingCadenceRecord =
                        new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                                1, Instant.now());
        ArrayList<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecords = new ArrayList<>();
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);

        return new CyclingPedalingCadenceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        cyclingPedalingCadenceRecords)
                .build();
    }

    private static CyclingPedalingCadenceRecord getCompleteCyclingPedalingCadenceRecord() {

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
        testMetadataBuilder.setClientRecordId("CPCR" + Math.random());

        CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
                cyclingPedalingCadenceRecord =
                        new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                                1, Instant.now());

        ArrayList<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecords = new ArrayList<>();
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);

        return new CyclingPedalingCadenceRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now(),
                        cyclingPedalingCadenceRecords)
                .build();
    }
}
