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
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class CyclingPedalingCadenceRecordTest {

    private static final String TAG = "CyclingPedalingCadenceRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                CyclingPedalingCadenceRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void testInsertCyclingPedalingCadenceRecord() throws InterruptedException {
        CyclingPedalingCadenceRecord baseCyclingPedalingCadenceRecord =
                getBaseCyclingPedalingCadenceRecord();

        assertThat(baseCyclingPedalingCadenceRecord.getSamples().get(0).getTime()).isNotNull();
        assertThat(baseCyclingPedalingCadenceRecord.getSamples().get(0).getRevolutionsPerMinute())
                .isNotNull();
        TestUtils.insertRecords(
                Arrays.asList(
                        baseCyclingPedalingCadenceRecord,
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
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
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
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
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
        List<Record> insertedRecords = TestUtils.insertRecords(records);
        List<RecordIdFilter> recordIds = new ArrayList<>(records.size());
        for (Record record : insertedRecords) {
            recordIds.add(RecordIdFilter.fromId(record.getClass(), record.getMetadata().getId()));
        }

        TestUtils.verifyDeleteRecords(recordIds);
        for (Record record : records) {
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteCyclingPedalingCadenceRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteCyclingPedalingCadenceRecord());
        TestUtils.verifyDeleteRecords(CyclingPedalingCadenceRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, CyclingPedalingCadenceRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        CyclingPedalingCadenceRecord.Builder builder =
                new CyclingPedalingCadenceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Collections.emptyList());

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void testReadCyclingPedalingCadenceRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteCyclingPedalingCadenceRecord(),
                        getCompleteCyclingPedalingCadenceRecord());
        readCyclingPedalingCadenceRecordUsingIds(recordList);
    }

    private void readCyclingPedalingCadenceRecordUsingClientId(List<Record> insertedRecords)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<CyclingPedalingCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CyclingPedalingCadenceRecord.class);
        for (Record record : insertedRecords) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<CyclingPedalingCadenceRecord> result = TestUtils.readRecords(request.build());
        assertThat(result.containsAll(insertedRecords)).isTrue();
    }

    private void readCyclingPedalingCadenceRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<CyclingPedalingCadenceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(CyclingPedalingCadenceRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(CyclingPedalingCadenceRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<CyclingPedalingCadenceRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        assertThat(result.containsAll(insertedRecords)).isTrue();
    }

    private static CyclingPedalingCadenceRecord getBaseCyclingPedalingCadenceRecord() {
        CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
                cyclingPedalingCadenceRecord =
                        new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                                1, Instant.now().plusMillis(100));
        ArrayList<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecords = new ArrayList<>();
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);

        return new CyclingPedalingCadenceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
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
                                1, Instant.now().plusMillis(100));

        ArrayList<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecords = new ArrayList<>();
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);

        return new CyclingPedalingCadenceRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        cyclingPedalingCadenceRecords)
                .build();
    }
}
