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

import static android.health.connect.datatypes.HydrationRecord.VOLUME_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Volume;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class HydrationRecordTest {
    private static final String TAG = "HydrationRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                HydrationRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertHydrationRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseHydrationRecord(), getCompleteHydrationRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadHydrationRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteHydrationRecord(), getCompleteHydrationRecord());
        readHydrationRecordUsingIds(recordList);
    }

    @Test
    public void testReadHydrationRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<HydrationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HydrationRecord.class)
                        .addId("abc")
                        .build();
        List<HydrationRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadHydrationRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteHydrationRecord(), getCompleteHydrationRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readHydrationRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadHydrationRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<HydrationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HydrationRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<HydrationRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadHydrationRecordUsingFilters_default() throws InterruptedException {
        List<HydrationRecord> oldHydrationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HydrationRecord.class)
                                .build());
        HydrationRecord testRecord = getCompleteHydrationRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HydrationRecord> newHydrationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HydrationRecord.class)
                                .build());
        assertThat(newHydrationRecords.size()).isEqualTo(oldHydrationRecords.size() + 1);
        assertThat(newHydrationRecords.get(newHydrationRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadHydrationRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        HydrationRecord testRecord = getCompleteHydrationRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HydrationRecord> newHydrationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HydrationRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newHydrationRecords.size()).isEqualTo(1);
        assertThat(newHydrationRecords.get(newHydrationRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadHydrationRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<HydrationRecord> oldHydrationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HydrationRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        HydrationRecord testRecord = getCompleteHydrationRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HydrationRecord> newHydrationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HydrationRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newHydrationRecords.size() - oldHydrationRecords.size()).isEqualTo(1);
        HydrationRecord newRecord = newHydrationRecords.get(newHydrationRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
        assertThat(newRecord.getVolume()).isEqualTo(testRecord.getVolume());
    }

    @Test
    public void testReadHydrationRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteHydrationRecord()));
        List<HydrationRecord> newHydrationRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HydrationRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newHydrationRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteHydrationRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteHydrationRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, HydrationRecord.class);
    }

    @Test
    public void testDeleteHydrationRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteHydrationRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(HydrationRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, HydrationRecord.class);
    }

    @Test
    public void testDeleteHydrationRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseHydrationRecord(), getCompleteHydrationRecord());
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
    public void testDeleteHydrationRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteHydrationRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, HydrationRecord.class);
    }

    @Test
    public void testDeleteHydrationRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteHydrationRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, HydrationRecord.class);
    }

    @Test
    public void testDeleteHydrationRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseHydrationRecord(), getCompleteHydrationRecord());
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
    public void testDeleteHydrationRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteHydrationRecord());
        TestUtils.verifyDeleteRecords(HydrationRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, HydrationRecord.class);
    }

    @Test
    public void testAggregation_VolumeTotal() throws Exception {
        List<Record> records =
                Arrays.asList(getCompleteHydrationRecord(), getCompleteHydrationRecord());
        AggregateRecordsResponse<Volume> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Volume>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(VOLUME_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew =
                Arrays.asList(getCompleteHydrationRecord(), getCompleteHydrationRecord());
        AggregateRecordsResponse<Volume> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Volume>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(VOLUME_TOTAL)
                                .build(),
                        recordNew);
        Volume newVolume = newResponse.get(VOLUME_TOTAL);
        Volume oldVolume = oldResponse.get(VOLUME_TOTAL);
        assertThat(newVolume).isNotNull();
        assertThat(oldVolume).isNotNull();
        assertThat(newVolume.getInMilliliters() - oldVolume.getInMilliliters()).isEqualTo(20);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(VOLUME_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(VOLUME_TOTAL);
        for (DataOrigin itr : oldDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        HydrationRecord.Builder builder =
                new HydrationRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Volume.fromMilliliters(10.0));

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void readHydrationRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<HydrationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HydrationRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<HydrationRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            HydrationRecord other = (HydrationRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readHydrationRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<HydrationRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HydrationRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(HydrationRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<HydrationRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            HydrationRecord other = (HydrationRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static HydrationRecord getBaseHydrationRecord() {
        return new HydrationRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Volume.fromMilliliters(10.0))
                .build();
    }

    static HydrationRecord getCompleteHydrationRecord() {
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
        testMetadataBuilder.setClientRecordId("HDR" + Math.random());
        return new HydrationRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Volume.fromMilliliters(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
