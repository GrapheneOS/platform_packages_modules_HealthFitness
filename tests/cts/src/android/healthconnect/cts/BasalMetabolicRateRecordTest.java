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

import static android.health.connect.datatypes.BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Power;
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
public class BasalMetabolicRateRecordTest {
    private static final String TAG = "BasalMetabolicRateRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                BasalMetabolicRateRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void testInsertBasalMetabolicRateRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBasalMetabolicRateRecord(), getCompleteBasalMetabolicRateRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadBasalMetabolicRateRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteBasalMetabolicRateRecord(),
                        getCompleteBasalMetabolicRateRecord());
        readBasalMetabolicRateRecordUsingIds(recordList);
    }

    @Test
    public void testReadBasalMetabolicRateRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class)
                        .addId("abc")
                        .build();
        List<BasalMetabolicRateRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBasalMetabolicRateRecord_usingClientRecordIds()
            throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteBasalMetabolicRateRecord(),
                        getCompleteBasalMetabolicRateRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readBasalMetabolicRateRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadBasalMetabolicRateRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<BasalMetabolicRateRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBasalMetabolicRateRecordUsingFilters_default() throws InterruptedException {
        List<BasalMetabolicRateRecord> oldBasalMetabolicRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BasalMetabolicRateRecord.class)
                                .build());
        BasalMetabolicRateRecord testRecord = getCompleteBasalMetabolicRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BasalMetabolicRateRecord> newBasalMetabolicRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BasalMetabolicRateRecord.class)
                                .build());
        assertThat(newBasalMetabolicRateRecords.size())
                .isEqualTo(oldBasalMetabolicRateRecords.size() + 1);
        assertThat(
                        newBasalMetabolicRateRecords
                                .get(newBasalMetabolicRateRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBasalMetabolicRateRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        BasalMetabolicRateRecord testRecord = getCompleteBasalMetabolicRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BasalMetabolicRateRecord> newBasalMetabolicRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BasalMetabolicRateRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newBasalMetabolicRateRecords.size()).isEqualTo(1);
        assertThat(
                        newBasalMetabolicRateRecords
                                .get(newBasalMetabolicRateRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBasalMetabolicRateRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<BasalMetabolicRateRecord> oldBasalMetabolicRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BasalMetabolicRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        BasalMetabolicRateRecord testRecord = getCompleteBasalMetabolicRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BasalMetabolicRateRecord> newBasalMetabolicRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BasalMetabolicRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newBasalMetabolicRateRecords.size() - oldBasalMetabolicRateRecords.size())
                .isEqualTo(1);
        BasalMetabolicRateRecord newRecord =
                newBasalMetabolicRateRecords.get(newBasalMetabolicRateRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
        assertThat(newRecord.getBasalMetabolicRate()).isEqualTo(testRecord.getBasalMetabolicRate());
    }

    @Test
    public void testReadBasalMetabolicRateRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteBasalMetabolicRateRecord()));
        List<BasalMetabolicRateRecord> newBasalMetabolicRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BasalMetabolicRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newBasalMetabolicRateRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteBasalMetabolicRateRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBasalMetabolicRateRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, BasalMetabolicRateRecord.class);
    }

    @Test
    public void testDeleteBasalMetabolicRateRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBasalMetabolicRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BasalMetabolicRateRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, BasalMetabolicRateRecord.class);
    }

    @Test
    public void testDeleteBasalMetabolicRateRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBasalMetabolicRateRecord(), getCompleteBasalMetabolicRateRecord());
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
    public void testDeleteBasalMetabolicRateRecord_dataOrigin_filters()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteBasalMetabolicRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, BasalMetabolicRateRecord.class);
    }

    @Test
    public void testDeleteBasalMetabolicRateRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBasalMetabolicRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, BasalMetabolicRateRecord.class);
    }

    @Test
    public void testDeleteBasalMetabolicRateRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseBasalMetabolicRateRecord(), getCompleteBasalMetabolicRateRecord());
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
    public void testDeleteBasalMetabolicRateRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBasalMetabolicRateRecord());
        TestUtils.verifyDeleteRecords(BasalMetabolicRateRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, BasalMetabolicRateRecord.class);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal() throws Exception {
        List<Record> records =
                Arrays.asList(getBasalMetabolicRateRecord(25.5), getBasalMetabolicRateRecord(71.5));
        AggregateRecordsResponse<Energy> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(3, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew = Arrays.asList(getBasalMetabolicRateRecord(45.5));
        AggregateRecordsResponse<Energy> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(3, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        recordNew);
        assertThat(newResponse.get(BASAL_CALORIES_TOTAL)).isNotNull();
        Energy newEnergy = newResponse.get(BASAL_CALORIES_TOTAL);
        Energy oldEnergy = oldResponse.get(BASAL_CALORIES_TOTAL);
        assertThat(newEnergy.getInCalories() - oldEnergy.getInCalories()).isEqualTo(45.5);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(BASAL_CALORIES_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(BASAL_CALORIES_TOTAL);
        for (DataOrigin itr : oldDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        BasalMetabolicRateRecord.Builder builder =
                new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Power.fromWatts(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private void readBasalMetabolicRateRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<BasalMetabolicRateRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BasalMetabolicRateRecord other = (BasalMetabolicRateRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readBasalMetabolicRateRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(BasalMetabolicRateRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<BasalMetabolicRateRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BasalMetabolicRateRecord other = (BasalMetabolicRateRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBasalMetabolicRateRecord_invalidValue() {
        new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Power.fromWatts(484.26))
                .build();
    }

    static BasalMetabolicRateRecord getBaseBasalMetabolicRateRecord() {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Power.fromWatts(100.0))
                .build();
    }

    static BasalMetabolicRateRecord getBasalMetabolicRateRecord(double power) {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Power.fromWatts(power))
                .build();
    }

    private static BasalMetabolicRateRecord getCompleteBasalMetabolicRateRecord() {
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
        testMetadataBuilder.setClientRecordId("BMR" + Math.random());
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);

        return new BasalMetabolicRateRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Power.fromWatts(100.0))
                .build();
    }
}
