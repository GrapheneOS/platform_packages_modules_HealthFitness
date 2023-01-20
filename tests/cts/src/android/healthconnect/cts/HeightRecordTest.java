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

import static android.healthconnect.datatypes.HeightRecord.HEIGHT_AVG;
import static android.healthconnect.datatypes.HeightRecord.HEIGHT_MAX;
import static android.healthconnect.datatypes.HeightRecord.HEIGHT_MIN;

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
import android.healthconnect.datatypes.HeightRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Length;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

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
public class HeightRecordTest {
    private static final String TAG = "HeightRecordTest";

    @Test
    public void testInsertHeightRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseHeightRecord(), getCompleteHeightRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadHeightRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteHeightRecord(), getCompleteHeightRecord());
        readHeightRecordUsingIds(recordList);
    }

    @Test
    public void testReadHeightRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<HeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeightRecord.class).addId("abc").build();
        List<HeightRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadHeightRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteHeightRecord(), getCompleteHeightRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readHeightRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadHeightRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<HeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeightRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<HeightRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadHeightRecordUsingFilters_default() throws InterruptedException {
        List<HeightRecord> oldHeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class).build());
        HeightRecord testRecord = getCompleteHeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HeightRecord> newHeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class).build());
        assertThat(newHeightRecords.size()).isEqualTo(oldHeightRecords.size() + 1);
        assertThat(newHeightRecords.get(newHeightRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadHeightRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        HeightRecord testRecord = getCompleteHeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HeightRecord> newHeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newHeightRecords.size()).isEqualTo(1);
        assertThat(newHeightRecords.get(newHeightRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadHeightRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<HeightRecord> oldHeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        HeightRecord testRecord = getCompleteHeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<HeightRecord> newHeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newHeightRecords.size() - oldHeightRecords.size()).isEqualTo(1);
        HeightRecord newRecord = newHeightRecords.get(newHeightRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadHeightRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteHeightRecord()));
        List<HeightRecord> newHeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newHeightRecords.size()).isEqualTo(0);
    }

    private void readHeightRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<HeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeightRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<HeightRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            HeightRecord other = (HeightRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readHeightRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<HeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(HeightRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(HeightRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<HeightRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            HeightRecord other = (HeightRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteHeightRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteHeightRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, HeightRecord.class);
    }

    @Test
    public void testDeleteHeightRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteHeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(HeightRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, HeightRecord.class);
    }

    @Test
    public void testDeleteHeightRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseHeightRecord(), getCompleteHeightRecord());
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
    public void testDeleteHeightRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteHeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, HeightRecord.class);
    }

    @Test
    public void testDeleteHeightRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteHeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, HeightRecord.class);
    }

    @Test
    public void testDeleteHeightRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseHeightRecord(), getCompleteHeightRecord());
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
    public void testDeleteHeightRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteHeightRecord());
        TestUtils.verifyDeleteRecords(HeightRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, HeightRecord.class);
    }

    @Test
    public void testAggregation_Height() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(
                        getBaseHeightRecord(5.0),
                        getBaseHeightRecord(10.0),
                        getBaseHeightRecord(15.0));
        AggregateRecordsResponse<Length> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Length>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(HEIGHT_MAX)
                                .addAggregationType(HEIGHT_MIN)
                                .addAggregationType(HEIGHT_AVG)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        Length maxHeight = response.get(HEIGHT_MAX);
        Length minHeight = response.get(HEIGHT_MIN);
        Length avgHeight = response.get(HEIGHT_AVG);
        assertThat(maxHeight).isNotNull();
        assertThat(maxHeight.getInMeters()).isEqualTo(15.0);
        assertThat(minHeight).isNotNull();
        assertThat(minHeight.getInMeters()).isEqualTo(5.0);
        assertThat(avgHeight).isNotNull();
        assertThat(avgHeight.getInMeters()).isEqualTo(10.0);
        Set<DataOrigin> dataOrigins = response.getDataOrigins(HEIGHT_AVG);
        for (DataOrigin itr : dataOrigins) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        HeightRecord.Builder builder =
                new HeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Length.fromMeters(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static HeightRecord getBaseHeightRecord() {
        return new HeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Length.fromMeters(10.0))
                .build();
    }

    static HeightRecord getBaseHeightRecord(double height) {
        return new HeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Length.fromMeters(height))
                .build();
    }

    private static HeightRecord getCompleteHeightRecord() {
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
        testMetadataBuilder.setClientRecordId("HR" + Math.random());

        return new HeightRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Length.fromMeters(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
