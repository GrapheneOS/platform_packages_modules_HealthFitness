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

import static android.health.connect.datatypes.FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.FloorsClimbedRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class FloorsClimbedRecordTest {
    private static final String TAG = "FloorsClimbedRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                FloorsClimbedRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertFloorsClimbedRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseFloorsClimbedRecord(1), getCompleteFloorsClimbedRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadFloorsClimbedRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteFloorsClimbedRecord(), getCompleteFloorsClimbedRecord());
        readFloorsClimbedRecordUsingIds(recordList);
    }

    @Test
    public void testReadFloorsClimbedRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<FloorsClimbedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(FloorsClimbedRecord.class)
                        .addId("abc")
                        .build();
        List<FloorsClimbedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadFloorsClimbedRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteFloorsClimbedRecord(), getCompleteFloorsClimbedRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readFloorsClimbedRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadFloorsClimbedRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<FloorsClimbedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(FloorsClimbedRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<FloorsClimbedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadFloorsClimbedRecordUsingFilters_default() throws InterruptedException {
        List<FloorsClimbedRecord> oldFloorsClimbedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(FloorsClimbedRecord.class)
                                .build());
        FloorsClimbedRecord testRecord = getCompleteFloorsClimbedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<FloorsClimbedRecord> newFloorsClimbedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(FloorsClimbedRecord.class)
                                .build());
        assertThat(newFloorsClimbedRecords.size()).isEqualTo(oldFloorsClimbedRecords.size() + 1);
        assertThat(
                        newFloorsClimbedRecords
                                .get(newFloorsClimbedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadFloorsClimbedRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        FloorsClimbedRecord testRecord = getCompleteFloorsClimbedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<FloorsClimbedRecord> newFloorsClimbedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(FloorsClimbedRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newFloorsClimbedRecords.size()).isEqualTo(1);
        assertThat(
                        newFloorsClimbedRecords
                                .get(newFloorsClimbedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadFloorsClimbedRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<FloorsClimbedRecord> oldFloorsClimbedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(FloorsClimbedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        FloorsClimbedRecord testRecord = getCompleteFloorsClimbedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<FloorsClimbedRecord> newFloorsClimbedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(FloorsClimbedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newFloorsClimbedRecords.size() - oldFloorsClimbedRecords.size()).isEqualTo(1);
        FloorsClimbedRecord newRecord =
                newFloorsClimbedRecords.get(newFloorsClimbedRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
        assertThat(newRecord.getFloors()).isEqualTo(testRecord.getFloors());
    }

    @Test
    public void testReadFloorsClimbedRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteFloorsClimbedRecord()));
        List<FloorsClimbedRecord> newFloorsClimbedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(FloorsClimbedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newFloorsClimbedRecords.size()).isEqualTo(0);
    }

    @Test
    public void testAggregation_FloorsClimbedTotal() throws Exception {
        List<Record> records =
                Arrays.asList(getBaseFloorsClimbedRecord(1), getBaseFloorsClimbedRecord(2));
        AggregateRecordsResponse<Double> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Double>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(FLOORS_CLIMBED_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew =
                Arrays.asList(getBaseFloorsClimbedRecord(3), getBaseFloorsClimbedRecord(4));
        AggregateRecordsResponse<Double> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Double>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(FLOORS_CLIMBED_TOTAL)
                                .build(),
                        recordNew);
        double newFloorsTotal = newResponse.get(FLOORS_CLIMBED_TOTAL);
        double oldFloorsTotal = oldResponse.get(FLOORS_CLIMBED_TOTAL);
        assertThat(newFloorsTotal).isNotNull();
        assertThat(oldFloorsTotal).isNotNull();
        assertThat(newFloorsTotal - oldFloorsTotal).isEqualTo(20);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(FLOORS_CLIMBED_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(FLOORS_CLIMBED_TOTAL);
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
        FloorsClimbedRecord.Builder builder =
                new FloorsClimbedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10);

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void readFloorsClimbedRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<FloorsClimbedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(FloorsClimbedRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(FloorsClimbedRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<FloorsClimbedRecord> result = TestUtils.readRecords(requestUsingIds);
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            FloorsClimbedRecord other = (FloorsClimbedRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readFloorsClimbedRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<FloorsClimbedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(FloorsClimbedRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<FloorsClimbedRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            FloorsClimbedRecord other = (FloorsClimbedRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
            assertThat(result.get(i).getFloors()).isEqualTo(other.getFloors());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFloorsClimbedRecord_invalidValue() {
        new FloorsClimbedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plus(30, ChronoUnit.MINUTES),
                        1000001.0)
                .build();
    }

    static FloorsClimbedRecord getBaseFloorsClimbedRecord(int days) {
        return new FloorsClimbedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minus(days, ChronoUnit.DAYS),
                        Instant.now().minus(days, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
                        10)
                .build();
    }

    static FloorsClimbedRecord getCompleteFloorsClimbedRecord() {
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
        testMetadataBuilder.setClientRecordId("FCR" + Math.random());
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);
        return new FloorsClimbedRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10)
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
