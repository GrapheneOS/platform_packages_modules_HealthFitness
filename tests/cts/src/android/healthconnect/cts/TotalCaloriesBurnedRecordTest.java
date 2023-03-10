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
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.units.Energy;

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
public class TotalCaloriesBurnedRecordTest {
    private static final String TAG = "TotalCaloriesBurnedRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                TotalCaloriesBurnedRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertTotalCaloriesBurnedRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseTotalCaloriesBurnedRecord(), getCompleteTotalCaloriesBurnedRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadTotalCaloriesBurnedRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteTotalCaloriesBurnedRecord(),
                        getCompleteTotalCaloriesBurnedRecord());
        readTotalCaloriesBurnedRecordUsingIds(recordList);
    }

    @Test
    public void testReadTotalCaloriesBurnedRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<TotalCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(TotalCaloriesBurnedRecord.class)
                        .addId("abc")
                        .build();
        List<TotalCaloriesBurnedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadTotalCaloriesBurnedRecord_usingClientRecordIds()
            throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteTotalCaloriesBurnedRecord(),
                        getCompleteTotalCaloriesBurnedRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readTotalCaloriesBurnedRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadTotalCaloriesBurnedRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<TotalCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(TotalCaloriesBurnedRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<TotalCaloriesBurnedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadTotalCaloriesBurnedRecordUsingFilters_default()
            throws InterruptedException {
        List<TotalCaloriesBurnedRecord> oldTotalCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        TotalCaloriesBurnedRecord.class)
                                .build());
        TotalCaloriesBurnedRecord testRecord = getCompleteTotalCaloriesBurnedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<TotalCaloriesBurnedRecord> newTotalCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        TotalCaloriesBurnedRecord.class)
                                .build());
        assertThat(newTotalCaloriesBurnedRecords.size())
                .isEqualTo(oldTotalCaloriesBurnedRecords.size() + 1);
        assertThat(
                        newTotalCaloriesBurnedRecords
                                .get(newTotalCaloriesBurnedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadTotalCaloriesBurnedRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        TotalCaloriesBurnedRecord testRecord = getCompleteTotalCaloriesBurnedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<TotalCaloriesBurnedRecord> newTotalCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        TotalCaloriesBurnedRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newTotalCaloriesBurnedRecords.size()).isEqualTo(1);
        assertThat(
                        newTotalCaloriesBurnedRecords
                                .get(newTotalCaloriesBurnedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadTotalCaloriesBurnedRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<TotalCaloriesBurnedRecord> oldTotalCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        TotalCaloriesBurnedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        TotalCaloriesBurnedRecord testRecord = getCompleteTotalCaloriesBurnedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<TotalCaloriesBurnedRecord> newTotalCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        TotalCaloriesBurnedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newTotalCaloriesBurnedRecords.size() - oldTotalCaloriesBurnedRecords.size())
                .isEqualTo(1);
        TotalCaloriesBurnedRecord newRecord =
                newTotalCaloriesBurnedRecords.get(newTotalCaloriesBurnedRecords.size() - 1);
        assertThat(
                        newTotalCaloriesBurnedRecords
                                .get(newTotalCaloriesBurnedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
        assertThat(newRecord.getEnergy()).isEqualTo(testRecord.getEnergy());
    }

    @Test
    public void testReadTotalCaloriesBurnedRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteTotalCaloriesBurnedRecord()));
        List<TotalCaloriesBurnedRecord> newTotalCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        TotalCaloriesBurnedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newTotalCaloriesBurnedRecords.size()).isEqualTo(0);
    }

    @Test
    public void testAggregation_totalCalriesBurnt() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(
                        getBaseTotalCaloriesBurnedRecord(1), getBaseTotalCaloriesBurnedRecord(2));
        AggregateRecordsResponse<Energy> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        List<Record> newRecords =
                Arrays.asList(
                        getBaseTotalCaloriesBurnedRecord(3), getBaseTotalCaloriesBurnedRecord(4));
        AggregateRecordsResponse<Energy> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        newRecords);
        Energy totEnergyBefore = oldResponse.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL);
        Energy totEnergyAfter = newResponse.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL);
        assertThat(totEnergyBefore).isNotNull();
        assertThat(totEnergyAfter).isNotNull();
        assertThat(totEnergyAfter.getInCalories())
                .isEqualTo(totEnergyBefore.getInCalories() + 20.0);
        Set<DataOrigin> newDataOrigin =
                newResponse.getDataOrigins(TotalCaloriesBurnedRecord.ENERGY_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin =
                oldResponse.getDataOrigins(TotalCaloriesBurnedRecord.ENERGY_TOTAL);
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
        TotalCaloriesBurnedRecord.Builder builder =
                new TotalCaloriesBurnedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromCalories(10.0));

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void readTotalCaloriesBurnedRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<TotalCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(TotalCaloriesBurnedRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<TotalCaloriesBurnedRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            TotalCaloriesBurnedRecord other = (TotalCaloriesBurnedRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readTotalCaloriesBurnedRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<TotalCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(TotalCaloriesBurnedRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<TotalCaloriesBurnedRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            TotalCaloriesBurnedRecord other = (TotalCaloriesBurnedRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static TotalCaloriesBurnedRecord getCompleteTotalCaloriesBurnedRecord() {

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
        testMetadataBuilder.setClientRecordId("TCBR" + Math.random());

        return new TotalCaloriesBurnedRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromCalories(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }

    static TotalCaloriesBurnedRecord getBaseTotalCaloriesBurnedRecord() {
        return new TotalCaloriesBurnedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromCalories(10.0))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTotalCaloriesBurnedRecord_invalidValue() {
        new TotalCaloriesBurnedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromCalories(1000000001.0))
                .build();
    }

    static TotalCaloriesBurnedRecord getBaseTotalCaloriesBurnedRecord(int days) {
        Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);
        return new TotalCaloriesBurnedRecord.Builder(
                        new Metadata.Builder().build(),
                        startTime,
                        startTime.plusMillis(1000),
                        Energy.fromCalories(10.0))
                .build();
    }
}
