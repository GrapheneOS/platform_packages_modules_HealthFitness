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
import android.healthconnect.datatypes.BodyWaterMassRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Mass;
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
public class BodyWaterMassRecordTest {
    private static final String TAG = "BodyWaterMassRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                BodyWaterMassRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertBodyWaterMassRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBodyWaterMassRecord(), getCompleteBodyWaterMassRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadBodyWaterMassRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBodyWaterMassRecord(), getCompleteBodyWaterMassRecord());
        readBodyWaterMassRecordUsingIds(recordList);
    }

    @Test
    public void testReadBodyWaterMassRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BodyWaterMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyWaterMassRecord.class)
                        .addId("abc")
                        .build();
        List<BodyWaterMassRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBodyWaterMassRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBodyWaterMassRecord(), getCompleteBodyWaterMassRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readBodyWaterMassRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadBodyWaterMassRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BodyWaterMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyWaterMassRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<BodyWaterMassRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBodyWaterMassRecordUsingFilters_default() throws InterruptedException {
        List<BodyWaterMassRecord> oldBodyWaterMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyWaterMassRecord.class)
                                .build());
        BodyWaterMassRecord testRecord = getCompleteBodyWaterMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyWaterMassRecord> newBodyWaterMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyWaterMassRecord.class)
                                .build());
        assertThat(newBodyWaterMassRecords.size()).isEqualTo(oldBodyWaterMassRecords.size() + 1);
        assertThat(
                        newBodyWaterMassRecords
                                .get(newBodyWaterMassRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBodyWaterMassRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        BodyWaterMassRecord testRecord = getCompleteBodyWaterMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyWaterMassRecord> newBodyWaterMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyWaterMassRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newBodyWaterMassRecords.size()).isEqualTo(1);
        assertThat(
                        newBodyWaterMassRecords
                                .get(newBodyWaterMassRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBodyWaterMassRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<BodyWaterMassRecord> oldBodyWaterMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyWaterMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        BodyWaterMassRecord testRecord = getCompleteBodyWaterMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BodyWaterMassRecord> newBodyWaterMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyWaterMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newBodyWaterMassRecords.size() - oldBodyWaterMassRecords.size()).isEqualTo(1);
        BodyWaterMassRecord newRecord =
                newBodyWaterMassRecords.get(newBodyWaterMassRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadBodyWaterMassRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteBodyWaterMassRecord()));
        List<BodyWaterMassRecord> newBodyWaterMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BodyWaterMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newBodyWaterMassRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteBodyWaterMassRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyWaterMassRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, BodyWaterMassRecord.class);
    }

    @Test
    public void testDeleteBodyWaterMassRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyWaterMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BodyWaterMassRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, BodyWaterMassRecord.class);
    }

    @Test
    public void testDeleteBodyWaterMassRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBodyWaterMassRecord(), getCompleteBodyWaterMassRecord());
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
    public void testDeleteBodyWaterMassRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyWaterMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, BodyWaterMassRecord.class);
    }

    @Test
    public void testDeleteBodyWaterMassRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyWaterMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, BodyWaterMassRecord.class);
    }

    @Test
    public void testDeleteBodyWaterMassRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBodyWaterMassRecord(), getCompleteBodyWaterMassRecord());
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
    public void testDeleteBodyWaterMassRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBodyWaterMassRecord());
        TestUtils.verifyDeleteRecords(BodyWaterMassRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, BodyWaterMassRecord.class);
    }

    private void readBodyWaterMassRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BodyWaterMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyWaterMassRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<BodyWaterMassRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BodyWaterMassRecord other = (BodyWaterMassRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readBodyWaterMassRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BodyWaterMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BodyWaterMassRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<BodyWaterMassRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BodyWaterMassRecord other = (BodyWaterMassRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static BodyWaterMassRecord getBaseBodyWaterMassRecord() {
        return new BodyWaterMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(100.0))
                .build();
    }

    static BodyWaterMassRecord getBodyWaterMassRecord(double mass) {
        return new BodyWaterMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(mass))
                .build();
    }

    private static BodyWaterMassRecord getCompleteBodyWaterMassRecord() {
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
        testMetadataBuilder.setClientRecordId("BWM" + Math.random());

        return new BodyWaterMassRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Mass.fromKilograms(100.0))
                .build();
    }
}
