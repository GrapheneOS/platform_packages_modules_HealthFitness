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

import static android.health.connect.datatypes.ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Energy;
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
public class ActiveCaloriesBurnedRecordTest {
    private static final String TAG = "ActiveCaloriesBurnedRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                ActiveCaloriesBurnedRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void testInsertActiveCaloriesBurnedRecord() throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseActiveCaloriesBurnedRecord(),
                        getCompleteActiveCaloriesBurnedRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadActiveCaloriesBurnedRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteActiveCaloriesBurnedRecord(),
                        getCompleteActiveCaloriesBurnedRecord());
        readActiveCaloriesBurnedRecordUsingIds(recordList);
    }

    @Test
    public void testReadActiveCaloriesBurnedRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<ActiveCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ActiveCaloriesBurnedRecord.class)
                        .addId("abc")
                        .build();
        List<ActiveCaloriesBurnedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadActiveCaloriesBurnedRecord_usingClientRecordIds()
            throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteActiveCaloriesBurnedRecord(),
                        getCompleteActiveCaloriesBurnedRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readActiveCaloriesBurnedRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadActiveCaloriesBurnedRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<ActiveCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ActiveCaloriesBurnedRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<ActiveCaloriesBurnedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadActiveCaloriesBurnedRecordUsingFilters_default()
            throws InterruptedException {
        List<ActiveCaloriesBurnedRecord> oldActiveCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        ActiveCaloriesBurnedRecord.class)
                                .build());
        ActiveCaloriesBurnedRecord testRecord = getCompleteActiveCaloriesBurnedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ActiveCaloriesBurnedRecord> newActiveCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        ActiveCaloriesBurnedRecord.class)
                                .build());
        assertThat(newActiveCaloriesBurnedRecords.size())
                .isEqualTo(oldActiveCaloriesBurnedRecords.size() + 1);
        assertThat(
                        newActiveCaloriesBurnedRecords
                                .get(newActiveCaloriesBurnedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadActiveCaloriesBurnedRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        ActiveCaloriesBurnedRecord testRecord = getCompleteActiveCaloriesBurnedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ActiveCaloriesBurnedRecord> newActiveCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        ActiveCaloriesBurnedRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newActiveCaloriesBurnedRecords.size()).isEqualTo(1);
        assertThat(
                        newActiveCaloriesBurnedRecords
                                .get(newActiveCaloriesBurnedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadActiveCaloriesBurnedRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<ActiveCaloriesBurnedRecord> oldActiveCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        ActiveCaloriesBurnedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        ActiveCaloriesBurnedRecord testRecord = getCompleteActiveCaloriesBurnedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ActiveCaloriesBurnedRecord> newActiveCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        ActiveCaloriesBurnedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newActiveCaloriesBurnedRecords.size() - oldActiveCaloriesBurnedRecords.size())
                .isEqualTo(1);
        ActiveCaloriesBurnedRecord newRecord =
                newActiveCaloriesBurnedRecords.get(newActiveCaloriesBurnedRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadActiveCaloriesBurnedRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteActiveCaloriesBurnedRecord()));
        List<ActiveCaloriesBurnedRecord> newActiveCaloriesBurnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        ActiveCaloriesBurnedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newActiveCaloriesBurnedRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteActiveCaloriesBurnedRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteActiveCaloriesBurnedRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void testDeleteActiveCaloriesBurnedRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteActiveCaloriesBurnedRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(ActiveCaloriesBurnedRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void testDeleteActiveCaloriesBurnedRecord_recordId_filters()
            throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseActiveCaloriesBurnedRecord(),
                        getCompleteActiveCaloriesBurnedRecord());
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
    public void testDeleteActiveCaloriesBurnedRecord_dataOrigin_filters()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteActiveCaloriesBurnedRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void testDeleteActiveCaloriesBurnedRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteActiveCaloriesBurnedRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void testDeleteActiveCaloriesBurnedRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(
                        getBaseActiveCaloriesBurnedRecord(),
                        getCompleteActiveCaloriesBurnedRecord());
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
    public void testAggregation_ActiveCaloriesBurntTotal() throws Exception {
        List<Record> records =
                Arrays.asList(
                        ActiveCaloriesBurnedRecordTest.getBaseActiveCaloriesBurnedRecord(74.0),
                        ActiveCaloriesBurnedRecordTest.getBaseActiveCaloriesBurnedRecord(100.5));
        AggregateRecordsResponse<Energy> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(ACTIVE_CALORIES_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew =
                Arrays.asList(
                        ActiveCaloriesBurnedRecordTest.getBaseActiveCaloriesBurnedRecord(45.5));
        AggregateRecordsResponse<Energy> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(ACTIVE_CALORIES_TOTAL)
                                .build(),
                        recordNew);
        assertThat(newResponse.get(ACTIVE_CALORIES_TOTAL)).isNotNull();
        Energy newEnergy = newResponse.get(ACTIVE_CALORIES_TOTAL);
        Energy oldEnergy = oldResponse.get(ACTIVE_CALORIES_TOTAL);
        assertThat(newEnergy.getInJoules() - oldEnergy.getInJoules()).isEqualTo(45.5);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(ACTIVE_CALORIES_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(ACTIVE_CALORIES_TOTAL);
        for (DataOrigin itr : oldDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testDeleteActiveCaloriesBurnedRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteActiveCaloriesBurnedRecord());
        TestUtils.verifyDeleteRecords(ActiveCaloriesBurnedRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        ActiveCaloriesBurnedRecord.Builder builder =
                new ActiveCaloriesBurnedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromJoules(10.0));

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void readActiveCaloriesBurnedRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<ActiveCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ActiveCaloriesBurnedRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<ActiveCaloriesBurnedRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ActiveCaloriesBurnedRecord other = (ActiveCaloriesBurnedRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readActiveCaloriesBurnedRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<ActiveCaloriesBurnedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ActiveCaloriesBurnedRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(ActiveCaloriesBurnedRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<ActiveCaloriesBurnedRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ActiveCaloriesBurnedRecord other = (ActiveCaloriesBurnedRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    static ActiveCaloriesBurnedRecord getBaseActiveCaloriesBurnedRecord() {
        return new ActiveCaloriesBurnedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromJoules(10.0))
                .build();
    }

    static ActiveCaloriesBurnedRecord getBaseActiveCaloriesBurnedRecord(double energy) {
        return new ActiveCaloriesBurnedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromJoules(energy))
                .build();
    }

    static ActiveCaloriesBurnedRecord getCompleteActiveCaloriesBurnedRecord() {
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
        testMetadataBuilder.setClientRecordId("ACBR" + Math.random());

        return new ActiveCaloriesBurnedRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        Energy.fromJoules(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
