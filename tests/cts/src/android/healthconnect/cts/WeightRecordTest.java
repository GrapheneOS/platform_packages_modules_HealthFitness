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

import static android.healthconnect.datatypes.WeightRecord.WEIGHT_AVG;
import static android.healthconnect.datatypes.WeightRecord.WEIGHT_MAX;
import static android.healthconnect.datatypes.WeightRecord.WEIGHT_MIN;

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
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.WeightRecord;
import android.healthconnect.datatypes.units.Mass;
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

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class WeightRecordTest {
    private static final String TAG = "WeightRecordTest";

    @Test
    public void testInsertWeightRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseWeightRecord(), getCompleteWeightRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadWeightRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord());
        readWeightRecordUsingIds(recordList);
    }

    @Test
    public void testReadWeightRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class).addId("abc").build();
        List<WeightRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadWeightRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteWeightRecord(), getCompleteWeightRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readWeightRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadWeightRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<WeightRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadWeightRecordUsingFilters_default() throws InterruptedException {
        List<WeightRecord> oldWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class).build());
        WeightRecord testRecord = getCompleteWeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class).build());
        assertThat(newWeightRecords.size()).isEqualTo(oldWeightRecords.size() + 1);
        assertThat(newWeightRecords.get(newWeightRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadWeightRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        WeightRecord testRecord = getCompleteWeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newWeightRecords.size()).isEqualTo(1);
        assertThat(newWeightRecords.get(newWeightRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadWeightRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<WeightRecord> oldWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        WeightRecord testRecord = getCompleteWeightRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newWeightRecords.size() - oldWeightRecords.size()).isEqualTo(1);
        WeightRecord newRecord = newWeightRecords.get(newWeightRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadWeightRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteWeightRecord()));
        List<WeightRecord> newWeightRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newWeightRecords.size()).isEqualTo(0);
    }

    @Test
    public void testAggregation_weight() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(
                        getBaseWeightRecord(5.0),
                        getBaseWeightRecord(10.0),
                        getBaseWeightRecord(15.0));
        AggregateRecordsResponse<Mass> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(WEIGHT_AVG)
                                .addAggregationType(WEIGHT_MAX)
                                .addAggregationType(WEIGHT_MIN)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        Mass maxWeight = response.get(WEIGHT_MAX);
        Mass minWeight = response.get(WEIGHT_MIN);
        Mass avgWeight = response.get(WEIGHT_AVG);
        assertThat(maxWeight).isNotNull();
        assertThat(maxWeight.getInKilograms()).isEqualTo(15.0);
        assertThat(minWeight).isNotNull();
        assertThat(minWeight.getInKilograms()).isEqualTo(5.0);
        assertThat(avgWeight).isNotNull();
        assertThat(avgWeight.getInKilograms()).isEqualTo(10.0);
    }

    @Test
    public void testDeleteWeightRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(WeightRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseWeightRecord(), getCompleteWeightRecord());
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
    public void testDeleteWeightRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, WeightRecord.class);
    }

    @Test
    public void testDeleteWeightRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseWeightRecord(), getCompleteWeightRecord());
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
    public void testDeleteWeightRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteWeightRecord());
        TestUtils.verifyDeleteRecords(WeightRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, WeightRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        WeightRecord.Builder builder =
                new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private void readWeightRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<WeightRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            WeightRecord other = (WeightRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readWeightRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<WeightRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<WeightRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            WeightRecord other = (WeightRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private static WeightRecord getBaseWeightRecord() {
        return new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0))
                .build();
    }

    static WeightRecord getBaseWeightRecord(double weight) {
        return new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(weight))
                .build();
    }

    private static WeightRecord getCompleteWeightRecord() {
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
        testMetadataBuilder.setClientRecordId("WR" + Math.random());

        return new WeightRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Mass.fromKilograms(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
