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
import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        TestUtils.deleteAllStagedRemoteData();
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
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);

        readBasalMetabolicRateRecordUsingIds(insertedRecords);
    }

    @Test
    public void testReadBasalMetabolicRateRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class)
                        .addId(UUID.randomUUID().toString())
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
                        .setStartTime(Instant.now().minus(1, ChronoUnit.DAYS))
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
                        .setStartTime(Instant.now().minus(1, ChronoUnit.DAYS))
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
                        .setStartTime(Instant.now().minus(1, ChronoUnit.DAYS))
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBasalMetabolicRateRecord());
        TestUtils.verifyDeleteRecords(BasalMetabolicRateRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, BasalMetabolicRateRecord.class);
    }

    static BasalMetabolicRateRecord getBasalMetabolicRateRecord(double power, Instant time) {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), time, Power.fromWatts(power))
                .build();
    }

    static BasalMetabolicRateRecord getBasalMetabolicRateRecord(
            double power, Instant time, ZoneOffset offset) {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), time, Power.fromWatts(power))
                .setZoneOffset(offset)
                .build();
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_noRecord() throws Exception {
        AggregateRecordsResponse<Energy> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        Instant.now().minus(2, ChronoUnit.DAYS))
                                                .setEndTime(Instant.now().minus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        null);

        assertThat(response.get(BASAL_CALORIES_TOTAL)).isNotNull();
        Energy energy = response.get(BASAL_CALORIES_TOTAL);
        assertThat(energy.getInCalories()).isWithin(1).of(1564500);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_lbm() throws Exception {
        List<Record> records =
                List.of(LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(Instant.now(), 50000));
        AggregateRecordsResponse<Energy> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        Instant.now().minus(2, ChronoUnit.DAYS))
                                                .setEndTime(Instant.now().minus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        records);
        assertThat(response.get(BASAL_CALORIES_TOTAL)).isNotNull();
        Energy energy = response.get(BASAL_CALORIES_TOTAL);
        assertThat(energy.getInCalories()).isWithin(1).of(1564500);

        records =
                List.of(
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(
                                Instant.now().minus(2, ChronoUnit.DAYS), 50000));
        response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        Instant.now().minus(2, ChronoUnit.DAYS))
                                                .setEndTime(Instant.now().minus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        records);

        assertThat(response.get(BASAL_CALORIES_TOTAL)).isNotNull();
        energy = response.get(BASAL_CALORIES_TOTAL);
        assertThat(energy.getInCalories()).isWithin(1).of(1450000);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_lbm_group() throws Exception {
        Instant now = Instant.now();
        List<Record> records =
                List.of(
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(now, 50000),
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(
                                now.minus(1, ChronoUnit.DAYS), 40000),
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(
                                now.minus(2, ChronoUnit.DAYS), 30000),
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(
                                now.minus(3, ChronoUnit.DAYS), 20000));
        TestUtils.insertRecords(records);
        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        Duration.ofDays(1));
        assertThat(responses).isNotNull();
        assertThat(responses.get(0).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(802000);
        assertThat(responses.get(1).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1018000);
        assertThat(responses.get(2).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1234000);

        AggregateRecordsResponse<Energy> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        null);
        assertThat(response).isNotNull();
        assertThat(response.get(BASAL_CALORIES_TOTAL).getInCalories()).isWithin(1).of(3054000);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_groupByDuration_lbmDerived()
            throws Exception {
        Instant now = Instant.now();
        List<Record> records =
                List.of(
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(now, 50000),
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(
                                now.minus(1, ChronoUnit.DAYS), 40000),
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(
                                now.minus(2, ChronoUnit.DAYS), 30000),
                        LeanBodyMassRecordTest.getBaseLeanBodyMassRecord(
                                now.minus(3, ChronoUnit.DAYS), 20000));
        TestUtils.insertRecords(records);
        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        Duration.ofDays(1));
        assertThat(responses).isNotNull();
        assertThat(responses.get(0).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(802000);
        assertThat(responses.get(1).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1018000);
        assertThat(responses.get(2).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1234000);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_profile_group() throws Exception {
        Instant now = Instant.now();
        List<Record> records =
                List.of(
                        HeightRecordTest.getBaseHeightRecord(now, 1.8),
                        WeightRecordTest.getBaseWeightRecord(now, 50000),
                        HeightRecordTest.getBaseHeightRecord(now.minus(1, ChronoUnit.DAYS), 1.7),
                        WeightRecordTest.getBaseWeightRecord(now.minus(1, ChronoUnit.DAYS), 40000),
                        HeightRecordTest.getBaseHeightRecord(now.minus(2, ChronoUnit.DAYS), 1.6),
                        WeightRecordTest.getBaseWeightRecord(now.minus(2, ChronoUnit.DAYS), 30000),
                        HeightRecordTest.getBaseHeightRecord(now.minus(3, ChronoUnit.DAYS), 1.5),
                        WeightRecordTest.getBaseWeightRecord(now.minus(3, ChronoUnit.DAYS), 20000));
        TestUtils.insertRecords(records);
        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        Duration.ofDays(1));
        assertThat(responses).isNotNull();
        assertThat(responses.get(0).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(909500);
        assertThat(responses.get(1).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1072000);
        assertThat(responses.get(2).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1234500);

        AggregateRecordsResponse<Energy> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        null);
        assertThat(response).isNotNull();
        assertThat(response.get(BASAL_CALORIES_TOTAL).getInCalories()).isWithin(1).of(3216000);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_groupByDuration_profileDerived()
            throws Exception {
        Instant now = Instant.now();
        List<Record> records =
                List.of(
                        HeightRecordTest.getBaseHeightRecord(now, 1.8),
                        WeightRecordTest.getBaseWeightRecord(now, 50000),
                        HeightRecordTest.getBaseHeightRecord(now.minus(1, ChronoUnit.DAYS), 1.7),
                        WeightRecordTest.getBaseWeightRecord(now.minus(1, ChronoUnit.DAYS), 40000),
                        HeightRecordTest.getBaseHeightRecord(now.minus(2, ChronoUnit.DAYS), 1.6),
                        WeightRecordTest.getBaseWeightRecord(now.minus(2, ChronoUnit.DAYS), 30000),
                        HeightRecordTest.getBaseHeightRecord(now.minus(3, ChronoUnit.DAYS), 1.5),
                        WeightRecordTest.getBaseWeightRecord(now.minus(3, ChronoUnit.DAYS), 20000));
        TestUtils.insertRecords(records);
        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        Duration.ofDays(1));
        assertThat(responses).isNotNull();
        assertThat(responses.get(0).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(909500);
        assertThat(responses.get(1).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1072000);
        assertThat(responses.get(2).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(1234500);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal() throws Exception {
        List<Record> records =
                Arrays.asList(
                        getBasalMetabolicRateRecord(30.0, Instant.now().minus(3, ChronoUnit.DAYS)),
                        getBasalMetabolicRateRecord(75.0, Instant.now().minus(2, ChronoUnit.DAYS)));
        AggregateRecordsResponse<Energy> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        Instant.now().minus(10, ChronoUnit.DAYS))
                                                .setEndTime(Instant.now().minus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew =
                List.of(getBasalMetabolicRateRecord(46, Instant.now().minus(1, ChronoUnit.DAYS)));
        AggregateRecordsResponse<Energy> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        Instant.now().minus(10, ChronoUnit.DAYS))
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        recordNew);
        assertThat(newResponse.get(BASAL_CALORIES_TOTAL)).isNotNull();
        Energy newEnergy = newResponse.get(BASAL_CALORIES_TOTAL);
        Energy oldEnergy = oldResponse.get(BASAL_CALORIES_TOTAL);
        assertThat(oldEnergy.getInCalories() / 1000).isWithin(1).of(13118);
        assertThat(newEnergy.getInCalories() / 1000).isGreaterThan(13118);
        assertThat((double) Math.round(newEnergy.getInCalories() - oldEnergy.getInCalories()))
                .isWithin(1)
                .of(949440);
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
    public void testAggregation_BasalCaloriesBurntTotal_groupDuration() throws Exception {
        Instant now = Instant.now();
        List<Record> records =
                List.of(
                        getBasalMetabolicRateRecord(50.0, now),
                        getBasalMetabolicRateRecord(40.0, now.minus(1, ChronoUnit.DAYS)),
                        getBasalMetabolicRateRecord(30.0, now.minus(2, ChronoUnit.DAYS)),
                        getBasalMetabolicRateRecord(20.0, now.minus(3, ChronoUnit.DAYS)));
        TestUtils.insertRecords(records);
        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        Duration.ofDays(1));
        assertThat(responses).isNotNull();
        assertThat(responses.get(0).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(412800);
        assertThat(responses.get(1).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(619200);
        assertThat(responses.get(2).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(825600);
        AggregateRecordsResponse<Energy> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        null);
        assertThat(response).isNotNull();
        assertThat(response.get(BASAL_CALORIES_TOTAL).getInCalories()).isWithin(1).of(1857600);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_groupDurationLocalFilter()
            throws Exception {
        Instant now = Instant.now();
        ZoneOffset offset = ZoneOffset.MIN;
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, offset);

        List<Record> records =
                List.of(
                        getBasalMetabolicRateRecord(50.0, now, offset),
                        getBasalMetabolicRateRecord(40.0, now.minus(1, ChronoUnit.DAYS), offset),
                        getBasalMetabolicRateRecord(30.0, now.minus(2, ChronoUnit.DAYS), offset),
                        getBasalMetabolicRateRecord(20.0, now.minus(3, ChronoUnit.DAYS), offset));
        var request =
                new AggregateRecordsRequest.Builder<Energy>(
                                new LocalTimeRangeFilter.Builder()
                                        .setStartTime(nowLocal.minusDays(3))
                                        .setEndTime(nowLocal)
                                        .build())
                        .addAggregationType(BASAL_CALORIES_TOTAL)
                        .build();

        TestUtils.insertRecords(records);
        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                TestUtils.getAggregateResponseGroupByDuration(request, Duration.ofDays(1));
        assertThat(responses).isNotNull();
        assertThat(responses.get(0).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(412800);
        assertThat(responses.get(1).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(619200);
        assertThat(responses.get(2).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(825600);

        AggregateRecordsResponse<Energy> response = TestUtils.getAggregateResponse(request, null);
        assertThat(response).isNotNull();
        assertThat(response.get(BASAL_CALORIES_TOTAL).getInCalories()).isWithin(1).of(1857600);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_group() throws Exception {
        Instant now = Instant.now();
        List<Record> records =
                List.of(
                        getBasalMetabolicRateRecord(50.0, now),
                        getBasalMetabolicRateRecord(40.0, now.minus(1, ChronoUnit.DAYS)),
                        getBasalMetabolicRateRecord(30.0, now.minus(2, ChronoUnit.DAYS)),
                        getBasalMetabolicRateRecord(20.0, now.minus(3, ChronoUnit.DAYS)));
        TestUtils.insertRecords(records);
        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(3, ChronoUnit.DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        Duration.ofDays(1));
        assertThat(responses).isNotNull();
        assertThat(responses.get(0).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(412800);
        assertThat(responses.get(1).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(619200);
        assertThat(responses.get(2).get(BASAL_CALORIES_TOTAL).getInCalories())
                .isWithin(1)
                .of(825600);
    }

    @Test
    public void testAggregation_BasalCaloriesBurntTotal_profile() throws Exception {
        List<Record> records =
                List.of(
                        HeightRecordTest.getBaseHeightRecord(
                                Instant.now().minus(2, ChronoUnit.DAYS), 1.8),
                        WeightRecordTest.getBaseWeightRecord(
                                Instant.now().minus(2, ChronoUnit.DAYS), 50000));
        AggregateRecordsResponse<Energy> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        Instant.now().minus(2, ChronoUnit.DAYS))
                                                .setEndTime(Instant.now().minus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BASAL_CALORIES_TOTAL)
                                .build(),
                        records);
        assertThat(response.get(BASAL_CALORIES_TOTAL)).isNotNull();
        Energy energy = response.get(BASAL_CALORIES_TOTAL);
        assertThat(energy.getInCalories()).isWithin(1).of(1397000);
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

    @Test
    public void testUpdateRecords_validInput_dataBaseUpdatedSuccessfully()
            throws InterruptedException {

        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(
                                getCompleteBasalMetabolicRateRecord(),
                                getCompleteBasalMetabolicRateRecord()));

        // read inserted records and verify that the data is same as inserted.
        readBasalMetabolicRateRecordUsingIds(insertedRecords);

        // Generate a new set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(
                        getCompleteBasalMetabolicRateRecord(),
                        getCompleteBasalMetabolicRateRecord());

        // Modify the uid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getBasalMetabolicRateRecord_update(
                            updateRecords.get(itr),
                            insertedRecords.get(itr).getMetadata().getId(),
                            insertedRecords.get(itr).getMetadata().getClientRecordId()));
        }

        TestUtils.updateRecords(updateRecords);

        // assert the inserted data has been modified by reading the data.
        readBasalMetabolicRateRecordUsingIds(updateRecords);
    }

    @Test
    public void testUpdateRecords_invalidInputRecords_noChangeInDataBase()
            throws InterruptedException {
        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(
                                getCompleteBasalMetabolicRateRecord(),
                                getCompleteBasalMetabolicRateRecord()));

        // read inserted records and verify that the data is same as inserted.
        readBasalMetabolicRateRecordUsingIds(insertedRecords);

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(
                        getCompleteBasalMetabolicRateRecord(),
                        getCompleteBasalMetabolicRateRecord());

        // Modify the Uid of the updateRecords to the UUID that was present in the insert records,
        // leaving out alternate records so that they have a new UUID which is not present in the
        // dataBase.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getBasalMetabolicRateRecord_update(
                            updateRecords.get(itr),
                            itr % 2 == 0
                                    ? insertedRecords.get(itr).getMetadata().getId()
                                    : UUID.randomUUID().toString(),
                            itr % 2 == 0
                                    ? insertedRecords.get(itr).getMetadata().getId()
                                    : UUID.randomUUID().toString()));
        }

        try {
            TestUtils.updateRecords(updateRecords);
            Assert.fail("Expected to fail due to invalid records ids.");
        } catch (HealthConnectException exception) {
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        }

        // assert the inserted data has not been modified by reading the data.
        readBasalMetabolicRateRecordUsingIds(insertedRecords);
    }

    @Test
    public void testUpdateRecords_recordWithInvalidPackageName_noChangeInDataBase()
            throws InterruptedException {
        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(
                                getCompleteBasalMetabolicRateRecord(),
                                getCompleteBasalMetabolicRateRecord()));

        // read inserted records and verify that the data is same as inserted.
        readBasalMetabolicRateRecordUsingIds(insertedRecords);

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(
                        getCompleteBasalMetabolicRateRecord(),
                        getCompleteBasalMetabolicRateRecord());

        // Modify the Uuid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getBasalMetabolicRateRecord_update(
                            updateRecords.get(itr),
                            insertedRecords.get(itr).getMetadata().getId(),
                            insertedRecords.get(itr).getMetadata().getClientRecordId()));
            //             adding an entry with invalid packageName.
            updateRecords.set(itr, getCompleteBasalMetabolicRateRecord());
        }

        try {
            TestUtils.updateRecords(updateRecords);
            Assert.fail("Expected to fail due to invalid package.");
        } catch (Exception exception) {
            // verify that the testcase failed due to invalid argument exception.
            assertThat(exception).isNotNull();
        }

        // assert the inserted data has not been modified by reading the data.
        readBasalMetabolicRateRecordUsingIds(insertedRecords);
    }

    @Test
    public void testInsertAndDeleteRecord_changelogs() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        ChangeLogTokenResponse tokenResponse =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .addRecordType(BasalMetabolicRateRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest =
                new ChangeLogsRequest.Builder(tokenResponse.getToken()).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedLogs().size()).isEqualTo(0);

        List<Record> testRecord = Collections.singletonList(getCompleteBasalMetabolicRateRecord());
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
        assertThat(
                        response.getUpsertedRecords().stream()
                                .map(Record::getMetadata)
                                .map(Metadata::getId)
                                .toList())
                .containsExactlyElementsIn(
                        testRecord.stream().map(Record::getMetadata).map(Metadata::getId).toList());
        assertThat(response.getDeletedLogs().size()).isEqualTo(0);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BasalMetabolicRateRecord.class)
                        .build());
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    private void readBasalMetabolicRateRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<BasalMetabolicRateRecord> result = TestUtils.readRecords(request.build());
        assertThat(result.size()).isEqualTo(insertedRecord.size());
        assertThat(result).containsExactlyElementsIn(insertedRecord);
    }

    private void readBasalMetabolicRateRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BasalMetabolicRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BasalMetabolicRateRecord.class);
        for (Record record : recordList) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(BasalMetabolicRateRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<BasalMetabolicRateRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(recordList.size());
        assertThat(result).containsExactlyElementsIn(recordList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBasalMetabolicRateRecord_invalidValue() {
        new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Power.fromWatts(484.26))
                .build();
    }

    BasalMetabolicRateRecord getBasalMetabolicRateRecord_update(
            Record record, String id, String clientRecordId) {
        Metadata metadata = record.getMetadata();
        Metadata metadataWithId =
                new Metadata.Builder()
                        .setId(id)
                        .setClientRecordId(clientRecordId)
                        .setClientRecordVersion(metadata.getClientRecordVersion())
                        .setDataOrigin(metadata.getDataOrigin())
                        .setDevice(metadata.getDevice())
                        .setLastModifiedTime(metadata.getLastModifiedTime())
                        .build();
        return new BasalMetabolicRateRecord.Builder(
                        metadataWithId, Instant.now(), Power.fromWatts(200.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }

    static BasalMetabolicRateRecord getBaseBasalMetabolicRateRecord() {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minus(1, ChronoUnit.DAYS),
                        Power.fromWatts(100.0))
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
                        testMetadataBuilder.build(),
                        Instant.now().minus(1, ChronoUnit.DAYS),
                        Power.fromWatts(100.0))
                .build();
    }
}
