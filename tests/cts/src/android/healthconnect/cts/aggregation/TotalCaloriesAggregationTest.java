/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.aggregation;

import static android.health.connect.datatypes.TotalCaloriesBurnedRecord.ENERGY_TOTAL;
import static android.healthconnect.cts.aggregation.DataFactory.getActiveCaloriesBurnedRecord;
import static android.healthconnect.cts.aggregation.DataFactory.getBasalMetabolicRateRecord;
import static android.healthconnect.cts.aggregation.DataFactory.getBaseHeightRecord;
import static android.healthconnect.cts.aggregation.DataFactory.getBaseLeanBodyMassRecord;
import static android.healthconnect.cts.aggregation.DataFactory.getBaseWeightRecord;
import static android.healthconnect.cts.aggregation.DataFactory.getTimeFilter;
import static android.healthconnect.cts.aggregation.Utils.assertEnergyWithTolerance;
import static android.healthconnect.cts.utils.DataFactory.getDataOrigin;
import static android.healthconnect.cts.utils.DataFactory.getEmptyMetadata;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponse;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponseGroupByDuration;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponseGroupByPeriod;
import static android.healthconnect.cts.utils.TestUtils.insertRecord;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.setupAggregation;

import static com.google.common.truth.Truth.assertThat;

import static java.time.Instant.EPOCH;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthDataCategory;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.units.Energy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

public final class TotalCaloriesAggregationTest {
    private static final double DEFAULT_BASAL_CALORIES_PER_DAY =
            getBasalCaloriesPerDay(/* weightKg= */ 73, /* heightCm= */ 170);

    private final String mPackageName =
            ApplicationProvider.getApplicationContext().getPackageName();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        deleteAllStagedRemoteData();
        setupAggregation(mPackageName, HealthDataCategory.ACTIVITY);
    }

    @After
    public void tearDown() {
        deleteAllStagedRemoteData();
    }

    @Test
    public void totalCaloriesBurned_derivedFromDefaultBasalCalories() throws Exception {
        Instant now = Instant.now();

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        assertEnergyWithTolerance(response.get(ENERGY_TOTAL), DEFAULT_BASAL_CALORIES_PER_DAY);
    }

    @Test
    public void totalCaloriesBurned_derivedFromWeightAndHeight() throws Exception {
        Instant now = Instant.now();
        double heightCm = 180;
        double weightKg = 85;
        insertRecords(
                List.of(
                        getBaseHeightRecord(EPOCH, heightCm / 100),
                        getBaseWeightRecord(EPOCH, weightKg)));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        assertEnergyWithTolerance(
                response.get(ENERGY_TOTAL), getBasalCaloriesPerDay(weightKg, heightCm));
    }

    @Test
    public void totalCaloriesBurned_derivedFromLbm() throws Exception {
        Instant now = Instant.now();
        double lbmKg = 50;
        insertRecord(getBaseLeanBodyMassRecord(EPOCH, lbmKg * 1000));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        assertEnergyWithTolerance(response.get(ENERGY_TOTAL), getBasalCaloriesPerDay(lbmKg));
    }

    @Test
    public void totalCaloriesBurned_derivedFromBmr() throws Exception {
        Instant now = Instant.now();
        double bmrWatt = 35;
        insertRecord(getBasalMetabolicRateRecord(bmrWatt, EPOCH));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        assertEnergyWithTolerance(response.get(ENERGY_TOTAL), wattToCalPerDay(bmrWatt));
    }

    @Test
    public void totalCaloriesBurned_hasActiveCaloriesData_sumActiveAndBasalCalories()
            throws Exception {
        Instant now = Instant.now();
        double activeCalories = 201230.3;
        insertRecord(
                getActiveCaloriesBurnedRecord(
                        activeCalories, now.minus(3, HOURS), now.minus(2, HOURS)));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        assertEnergyWithTolerance(
                response.get(ENERGY_TOTAL), DEFAULT_BASAL_CALORIES_PER_DAY + activeCalories);
    }

    @Test
    public void totalCaloriesBurned_hasTotalCaloriesData_addBasalCaloriesAtGaps() throws Exception {
        Instant now = Instant.now();
        double totalCalories = 204560.3;
        insertRecord(
                getTotalCaloriesBurnedRecord(
                        totalCalories, now.minus(3, HOURS), now.minus(2, HOURS)));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        assertEnergyWithTolerance(
                response.get(ENERGY_TOTAL),
                DEFAULT_BASAL_CALORIES_PER_DAY * 23 / 24 + totalCalories);
    }

    @Test
    public void totalCaloriesBurned_hasActiveAndTotalCaloriesData_addBasalCaloriesAtGaps()
            throws Exception {
        Instant now = Instant.now();
        double totalCalories = 2009870.3;
        double activeCalories = 15120.6;
        double overlappingActiveCalories = 30000;
        insertRecords(
                getTotalCaloriesBurnedRecord(
                        totalCalories, now.minus(24, HOURS), now.minus(2, HOURS)),
                getActiveCaloriesBurnedRecord(
                        overlappingActiveCalories, now.minus(150, MINUTES), now.minus(1, HOURS)),
                getActiveCaloriesBurnedRecord(activeCalories, now.minus(1, HOURS), now));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        // overlappingActiveCalories overlaps with totalCalories by 30 minutes out of 90 minutes
        // for the overlapping part, we use total calories directly, not derive from active + basal
        double partialActiveCalories = overlappingActiveCalories * 2 / 3;
        double expected =
                DEFAULT_BASAL_CALORIES_PER_DAY * 2 / 24
                        + totalCalories
                        + activeCalories
                        + partialActiveCalories;
        assertEnergyWithTolerance(response.get(ENERGY_TOTAL), expected);
    }

    @Test
    public void totalCaloriesBurned_totalCaloriesDataWithoutGap_equalsToTotalCalories()
            throws Exception {
        Instant now = Instant.now();
        double totalCalories = 2009870.3;
        insertRecords(getTotalCaloriesBurnedRecord(totalCalories, now.minus(1, DAYS), now));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(1, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        assertEnergyWithTolerance(response.get(ENERGY_TOTAL), totalCalories);
    }

    @Test
    public void totalCaloriesBurned_deriveBasalAndActiveAndTotalCalories() throws Exception {
        Instant now = Instant.now();
        insertRecords(
                Arrays.asList(
                        getTotalCaloriesBurnedRecord(10, now.minus(1, DAYS), now),
                        getTotalCaloriesBurnedRecord(10, now.minus(2, DAYS), now.minus(1, DAYS)),
                        getActiveCaloriesBurnedRecord(20, now.minus(4, DAYS), now.minus(3, DAYS))));

        AggregateRecordsRequest<Energy> request =
                new AggregateRecordsRequest.Builder<Energy>(getTimeFilter(now.minus(5, DAYS), now))
                        .addAggregationType(ENERGY_TOTAL)
                        .build();
        AggregateRecordsResponse<Energy> response = getAggregateResponse(request);

        // -5    -4    -3    -2    -1    now (days)
        // |_____|_____|_____|_____|_____|
        // basal basal+ basal total total
        //       active       (10)  (10)
        //        (20)
        assertEnergyWithTolerance(
                response.get(ENERGY_TOTAL), DEFAULT_BASAL_CALORIES_PER_DAY * 3 + 40);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAggregation_totalCaloriesBurned_activeCalories_groupBy() throws Exception {
        Instant now = Instant.now();
        getAggregateResponseGroupByPeriod(
                new AggregateRecordsRequest.Builder<Energy>(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(now.minus(5, DAYS))
                                        .setEndTime(now)
                                        .build())
                        .addAggregationType(ENERGY_TOTAL)
                        .addDataOriginsFilter(getDataOrigin(mPackageName))
                        .build(),
                Period.ofDays(1));
    }

    @Test
    public void testAggregation_totalCaloriesBurned_activeCalories_groupBy_duration()
            throws Exception {
        Instant now = Instant.now();
        insertRecords(
                List.of(
                        getBaseTotalCaloriesBurnedRecord(now.minus(1, DAYS), 10),
                        getBaseTotalCaloriesBurnedRecord(now.minus(2, DAYS), 20),
                        getBaseActiveCaloriesBurnedRecord(now.minus(4, DAYS), 20),
                        getBasalMetabolicRateRecord(30, now.minus(3, DAYS))));

        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(now.minus(5, DAYS))
                                                .setEndTime(now)
                                                .build())
                                .addAggregationType(ENERGY_TOTAL)
                                .addDataOriginsFilter(getDataOrigin(mPackageName))
                                .build(),
                        Duration.ofDays(1));

        assertThat(responses).hasSize(5);
        assertEnergyWithTolerance(responses.get(0).get(ENERGY_TOTAL), 1564500);
        assertEnergyWithTolerance(responses.get(1).get(ENERGY_TOTAL), 1564520);
        assertEnergyWithTolerance(responses.get(2).get(ENERGY_TOTAL), 619200);
        assertEnergyWithTolerance(responses.get(3).get(ENERGY_TOTAL), 20);
        assertEnergyWithTolerance(responses.get(4).get(ENERGY_TOTAL), 10);
    }

    @Test
    public void testAggregation_groupByDurationLocalFilter_shiftRecordsAndFilterWithOffset()
            throws Exception {
        Instant now = Instant.now();
        ZoneOffset offset = ZoneOffset.ofHours(-1);
        LocalDateTime localNow = LocalDateTime.ofInstant(now, offset);

        insertRecords(
                Arrays.asList(
                        getBaseTotalCaloriesBurnedRecord(now.minus(1, DAYS), 10, offset),
                        getBaseTotalCaloriesBurnedRecord(now.minus(2, DAYS), 20, offset),
                        getBaseActiveCaloriesBurnedRecord(now.minus(4, DAYS), 20, offset),
                        getBasalMetabolicRateRecord(30, now.minus(3, DAYS), offset)));

        List<AggregateRecordsGroupedByDurationResponse<Energy>> responses =
                getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(localNow.minusDays(5))
                                                .setEndTime(localNow)
                                                .build())
                                .addAggregationType(ENERGY_TOTAL)
                                .addDataOriginsFilter(getDataOrigin(mPackageName))
                                .build(),
                        Duration.ofDays(1));

        assertThat(responses).hasSize(5);
        assertEnergyWithTolerance(responses.get(0).get(ENERGY_TOTAL), 1564500);
        assertEnergyWithTolerance(responses.get(1).get(ENERGY_TOTAL), 1564520);
        assertEnergyWithTolerance(responses.get(2).get(ENERGY_TOTAL), 619200);
        assertEnergyWithTolerance(responses.get(3).get(ENERGY_TOTAL), 20);
        assertEnergyWithTolerance(responses.get(4).get(ENERGY_TOTAL), 10);
    }

    private static TotalCaloriesBurnedRecord getTotalCaloriesBurnedRecord(
            double calories, Instant start, Instant end) {
        return new TotalCaloriesBurnedRecord.Builder(
                        getEmptyMetadata(), start, end, Energy.fromCalories(calories))
                .build();
    }

    private static TotalCaloriesBurnedRecord getBaseTotalCaloriesBurnedRecord(
            Instant startTime, double value) {
        return getBaseTotalCaloriesBurnedRecord(startTime, value, null);
    }

    private static TotalCaloriesBurnedRecord getBaseTotalCaloriesBurnedRecord(
            Instant startTime, double value, ZoneOffset offset) {
        TotalCaloriesBurnedRecord.Builder builder =
                new TotalCaloriesBurnedRecord.Builder(
                        getEmptyMetadata(),
                        startTime,
                        startTime.plus(1, DAYS),
                        Energy.fromCalories(value));

        if (offset != null) {
            builder.setStartZoneOffset(offset).setEndZoneOffset(offset);
        }
        return builder.build();
    }

    private static ActiveCaloriesBurnedRecord getBaseActiveCaloriesBurnedRecord(
            Instant startTime, double energy) {
        return new ActiveCaloriesBurnedRecord.Builder(
                        getEmptyMetadata(),
                        startTime,
                        startTime.plus(1, DAYS),
                        Energy.fromCalories(energy))
                .build();
    }

    private static ActiveCaloriesBurnedRecord getBaseActiveCaloriesBurnedRecord(
            Instant startTime, double energy, ZoneOffset offset) {
        return new ActiveCaloriesBurnedRecord.Builder(
                        getEmptyMetadata(),
                        startTime,
                        startTime.plus(1, DAYS),
                        Energy.fromCalories(energy))
                .setStartZoneOffset(offset)
                .setEndZoneOffset(offset)
                .build();
    }

    private static double getBasalCaloriesPerDay(double weightKg, double heightCm) {
        // We use Mifflin-St Jeor Equation to calculate BMR
        // BMR (kcal/day) = 10 * weight in kg + 6.25 * height in cm
        //                  -5 * age in years + gender constant
        // gender constant: Men(5), Women(-161), Unspecified(-78)
        double defaultAge = 30;
        return (10 * weightKg + 6.25 * heightCm - 5 * defaultAge - 78) * 1000;
    }

    private static double getBasalCaloriesPerDay(double lbmKg) {
        return (370 + 21.6 * lbmKg) * 1000;
    }

    private static double wattToCalPerDay(double watt) {
        return watt * 860 * 24;
    }
}
