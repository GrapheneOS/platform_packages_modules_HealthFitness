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

import static android.health.connect.datatypes.WheelchairPushesRecord.WHEEL_CHAIR_PUSHES_COUNT_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.WheelchairPushesRecord;

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
public class WheelchairPushesRecordTest {
    private static final String TAG = "WheelchairPushesRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                WheelchairPushesRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertWheelchairPushesRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseWheelchairPushesRecord(), getCompleteWheelchairPushesRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadWheelchairPushesRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteWheelchairPushesRecord(), getCompleteWheelchairPushesRecord());
        readWheelchairPushesRecordUsingIds(recordList);
    }

    @Test
    public void testReadWheelchairPushesRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<WheelchairPushesRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WheelchairPushesRecord.class)
                        .addId("abc")
                        .build();
        List<WheelchairPushesRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadWheelchairPushesRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteWheelchairPushesRecord(), getCompleteWheelchairPushesRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readWheelchairPushesRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadWheelchairPushesRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<WheelchairPushesRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WheelchairPushesRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<WheelchairPushesRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadWheelchairPushesRecordUsingFilters_default() throws InterruptedException {
        List<WheelchairPushesRecord> oldWheelchairPushesRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WheelchairPushesRecord.class)
                                .build());
        WheelchairPushesRecord testRecord = getCompleteWheelchairPushesRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WheelchairPushesRecord> newWheelchairPushesRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WheelchairPushesRecord.class)
                                .build());
        assertThat(newWheelchairPushesRecords.size())
                .isEqualTo(oldWheelchairPushesRecords.size() + 1);
        assertThat(
                        newWheelchairPushesRecords
                                .get(newWheelchairPushesRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadWheelchairPushesRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        WheelchairPushesRecord testRecord = getCompleteWheelchairPushesRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WheelchairPushesRecord> newWheelchairPushesRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WheelchairPushesRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newWheelchairPushesRecords.size()).isEqualTo(1);
        assertThat(
                        newWheelchairPushesRecords
                                .get(newWheelchairPushesRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadWheelchairPushesRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<WheelchairPushesRecord> oldWheelchairPushesRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WheelchairPushesRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        WheelchairPushesRecord testRecord = getCompleteWheelchairPushesRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WheelchairPushesRecord> newWheelchairPushesRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WheelchairPushesRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newWheelchairPushesRecords.size() - oldWheelchairPushesRecords.size())
                .isEqualTo(1);
        WheelchairPushesRecord newRecord =
                newWheelchairPushesRecords.get(newWheelchairPushesRecords.size() - 1);
        assertThat(
                        newWheelchairPushesRecords
                                .get(newWheelchairPushesRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
        assertThat(newRecord.getCount()).isEqualTo(testRecord.getCount());
    }

    @Test
    public void testReadWheelchairPushesRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteWheelchairPushesRecord()));
        List<WheelchairPushesRecord> newWheelchairPushesRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WheelchairPushesRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newWheelchairPushesRecords.size()).isEqualTo(0);
    }

    @Test
    public void testAggregation_countTotal() throws Exception {
        List<Record> records =
                Arrays.asList(getBaseWheelchairPushesRecord(1), getBaseWheelchairPushesRecord(2));
        AggregateRecordsResponse<Long> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(WHEEL_CHAIR_PUSHES_COUNT_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew =
                Arrays.asList(getBaseWheelchairPushesRecord(3), getBaseWheelchairPushesRecord(4));
        AggregateRecordsResponse<Long> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(WHEEL_CHAIR_PUSHES_COUNT_TOTAL)
                                .build(),
                        recordNew);
        assertThat(newResponse.get(WHEEL_CHAIR_PUSHES_COUNT_TOTAL)).isNotNull();
        assertThat(newResponse.get(WHEEL_CHAIR_PUSHES_COUNT_TOTAL))
                .isEqualTo(oldResponse.get(WHEEL_CHAIR_PUSHES_COUNT_TOTAL) + 20);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(WHEEL_CHAIR_PUSHES_COUNT_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(WHEEL_CHAIR_PUSHES_COUNT_TOTAL);
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
        WheelchairPushesRecord.Builder builder =
                new WheelchairPushesRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        1);

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void readWheelchairPushesRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<WheelchairPushesRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WheelchairPushesRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<WheelchairPushesRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            WheelchairPushesRecord other = (WheelchairPushesRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readWheelchairPushesRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<WheelchairPushesRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WheelchairPushesRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<WheelchairPushesRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            WheelchairPushesRecord other = (WheelchairPushesRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static WheelchairPushesRecord getCompleteWheelchairPushesRecord() {

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
        testMetadataBuilder.setClientRecordId("WPR" + Math.random());

        return new WheelchairPushesRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        1)
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWheelchairPushesRecord_invalidValue() {
        new WheelchairPushesRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        1000001)
                .build();
    }

    static WheelchairPushesRecord getBaseWheelchairPushesRecord() {
        return new WheelchairPushesRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10)
                .build();
    }

    static WheelchairPushesRecord getBaseWheelchairPushesRecord(int days) {
        Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);
        return new WheelchairPushesRecord.Builder(
                        new Metadata.Builder().build(), startTime, startTime.plusMillis(1000), 10)
                .build();
    }
}
