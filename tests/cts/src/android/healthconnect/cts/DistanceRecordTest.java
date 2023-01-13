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

import static android.healthconnect.datatypes.DistanceRecord.DISTANCE_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.DistanceRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Length;
import android.platform.test.annotations.AppModeFull;

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

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class DistanceRecordTest {
    private static final String TAG = "DistanceRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                DistanceRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertDistanceRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseDistanceRecord(), getCompleteDistanceRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadDistanceRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteDistanceRecord(), getCompleteDistanceRecord());
        readDistanceRecordUsingIds(recordList);
    }

    @Test
    public void testReadDistanceRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<DistanceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(DistanceRecord.class).addId("abc").build();
        List<DistanceRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadDistanceRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteDistanceRecord(), getCompleteDistanceRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readDistanceRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadDistanceRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<DistanceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(DistanceRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<DistanceRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadDistanceRecordUsingFilters_default() throws InterruptedException {
        List<DistanceRecord> oldDistanceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class).build());
        DistanceRecord testRecord = getCompleteDistanceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<DistanceRecord> newDistanceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class).build());
        assertThat(newDistanceRecords.size()).isEqualTo(oldDistanceRecords.size() + 1);
        assertThat(newDistanceRecords.get(newDistanceRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadDistanceRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        DistanceRecord testRecord = getCompleteDistanceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<DistanceRecord> newDistanceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newDistanceRecords.size()).isEqualTo(1);
        assertThat(newDistanceRecords.get(newDistanceRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadDistanceRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<DistanceRecord> oldDistanceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        DistanceRecord testRecord = getCompleteDistanceRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<DistanceRecord> newDistanceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newDistanceRecords.size() - oldDistanceRecords.size()).isEqualTo(1);
        DistanceRecord newRecord = newDistanceRecords.get(newDistanceRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadDistanceRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteDistanceRecord()));
        List<DistanceRecord> newDistanceRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newDistanceRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteDistanceRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteDistanceRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, DistanceRecord.class);
    }

    @Test
    public void testDeleteDistanceRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteDistanceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(DistanceRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, DistanceRecord.class);
    }

    @Test
    public void testDeleteDistanceRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseDistanceRecord(), getCompleteDistanceRecord());
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
    public void testDeleteDistanceRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteDistanceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, DistanceRecord.class);
    }

    @Test
    public void testDeleteDistanceRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteDistanceRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, DistanceRecord.class);
    }

    @Test
    public void testDeleteDistanceRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseDistanceRecord(), getCompleteDistanceRecord());
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
    public void testDeleteDistanceRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteDistanceRecord());
        TestUtils.verifyDeleteRecords(DistanceRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, DistanceRecord.class);
    }

    private void readDistanceRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<DistanceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(DistanceRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<DistanceRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            DistanceRecord other = (DistanceRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readDistanceRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<DistanceRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(DistanceRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<DistanceRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            DistanceRecord other = (DistanceRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testAggregation_DistanceTotal() throws Exception {
        List<Record> records =
                Arrays.asList(
                        DistanceRecordTest.getBaseDistanceRecord(74.0),
                        DistanceRecordTest.getBaseDistanceRecord(100.5));
        AggregateRecordsResponse<Length> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Length>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(DISTANCE_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew = Arrays.asList(DistanceRecordTest.getBaseDistanceRecord(100.5));
        AggregateRecordsResponse<Length> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Length>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(DISTANCE_TOTAL)
                                .build(),
                        recordNew);
        Length oldLength = oldResponse.get(DISTANCE_TOTAL);
        Length newLength = newResponse.get(DISTANCE_TOTAL);
        assertThat(oldLength).isNotNull();
        assertThat(newLength).isNotNull();
        assertThat(newLength.getInMeters() - oldLength.getInMeters()).isEqualTo(100.5);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(DISTANCE_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(DISTANCE_TOTAL);
        for (DataOrigin itr : oldDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    static DistanceRecord getBaseDistanceRecord() {
        return new DistanceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        Length.fromMeters(10.0))
                .build();
    }

    static DistanceRecord getBaseDistanceRecord(double distance) {
        return new DistanceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        Length.fromMeters(distance))
                .build();
    }

    static DistanceRecord getCompleteDistanceRecord() {

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
        testMetadataBuilder.setClientRecordId("DR" + Math.random());

        return new DistanceRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now(),
                        Length.fromMeters(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
