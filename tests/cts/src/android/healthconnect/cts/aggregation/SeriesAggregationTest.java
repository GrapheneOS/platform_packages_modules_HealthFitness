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

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.datatypes.CyclingPedalingCadenceRecord.RPM_AVG;
import static android.health.connect.datatypes.CyclingPedalingCadenceRecord.RPM_MAX;
import static android.health.connect.datatypes.CyclingPedalingCadenceRecord.RPM_MIN;
import static android.health.connect.datatypes.HeartRateRecord.BPM_AVG;
import static android.health.connect.datatypes.HeartRateRecord.BPM_MAX;
import static android.health.connect.datatypes.HeartRateRecord.BPM_MIN;
import static android.health.connect.datatypes.HeartRateRecord.HEART_MEASUREMENTS_COUNT;
import static android.health.connect.datatypes.PowerRecord.POWER_AVG;
import static android.health.connect.datatypes.PowerRecord.POWER_MAX;
import static android.health.connect.datatypes.PowerRecord.POWER_MIN;
import static android.health.connect.datatypes.SpeedRecord.SPEED_AVG;
import static android.health.connect.datatypes.SpeedRecord.SPEED_MAX;
import static android.health.connect.datatypes.SpeedRecord.SPEED_MIN;
import static android.healthconnect.cts.aggregation.DataFactory.getTimeFilter;
import static android.healthconnect.cts.aggregation.Utils.assertDoubleWithTolerance;
import static android.healthconnect.cts.utils.DataFactory.getEmptyMetadata;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponse;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.setupAggregation;

import static com.google.common.truth.Truth.assertThat;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Velocity;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

public class SeriesAggregationTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final String mPackageName = mContext.getPackageName();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        deleteAllStagedRemoteData();
        setupAggregation(mPackageName, ACTIVITY);
    }

    @After
    public void tearDown() {
        deleteAllStagedRemoteData();
    }

    @Test
    public void aggregateWithInstantFilter_speed() throws Exception {
        Instant time = Instant.now().minus(1, DAYS);
        insertRecords(
                List.of(
                        getSpeedRecord(
                                time,
                                time.plus(8, HOURS),
                                UTC,
                                getSpeedRecordSample(Velocity.fromMetersPerSecond(12), time),
                                getSpeedRecordSample(
                                        Velocity.fromMetersPerSecond(50), time.plus(1, HOURS)),
                                getSpeedRecordSample(
                                        Velocity.fromMetersPerSecond(10), time.plus(2, HOURS)),
                                getSpeedRecordSample(
                                        Velocity.fromMetersPerSecond(6), time.plus(3, HOURS)),
                                getSpeedRecordSample(
                                        Velocity.fromMetersPerSecond(2), time.plus(4, HOURS)),
                                getSpeedRecordSample(
                                        Velocity.fromMetersPerSecond(12), time.plus(5, HOURS)))));

        TimeInstantRangeFilter timeFilter =
                getTimeFilter(time.plus(1, MINUTES), time.plus(3, HOURS).plus(1, MINUTES));
        AggregateRecordsRequest<Velocity> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Velocity>(timeFilter)
                        .addAggregationType(SPEED_MIN)
                        .addAggregationType(SPEED_MAX)
                        .addAggregationType(SPEED_AVG)
                        .build();

        AggregateRecordsResponse<Velocity> response = getAggregateResponse(aggregateRecordsRequest);
        assertDoubleWithTolerance(response.get(SPEED_MIN).getInMetersPerSecond(), 6);
        assertDoubleWithTolerance(response.get(SPEED_MAX).getInMetersPerSecond(), 50);
        // Expect (50+10+6)/3=22
        assertDoubleWithTolerance(response.get(SPEED_AVG).getInMetersPerSecond(), 22);
    }

    @Test
    public void aggregateWithInstantFilter_power() throws Exception {
        Instant time = Instant.now().minus(3, DAYS);
        insertRecords(
                List.of(
                        getPowerRecord(
                                time,
                                time.plus(2, HOURS),
                                UTC,
                                getPowerRecordSample(Power.fromWatts(120), time),
                                getPowerRecordSample(Power.fromWatts(30), time.plus(5, MINUTES)),
                                getPowerRecordSample(Power.fromWatts(4), time.plus(20, MINUTES)),
                                getPowerRecordSample(Power.fromWatts(70), time.plus(60, MINUTES)),
                                getPowerRecordSample(Power.fromWatts(12), time.plus(119, MINUTES)),
                                getPowerRecordSample(
                                        Power.fromWatts(90), time.plus(120, MINUTES)))));

        TimeInstantRangeFilter timeFilter =
                getTimeFilter(time.plus(1, MINUTES), time.plus(120, MINUTES));
        AggregateRecordsRequest<Power> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Power>(timeFilter)
                        .addAggregationType(POWER_MAX)
                        .addAggregationType(POWER_MIN)
                        .addAggregationType(POWER_AVG)
                        .build();

        AggregateRecordsResponse<Power> response = getAggregateResponse(aggregateRecordsRequest);
        assertDoubleWithTolerance(response.get(POWER_MAX).getInWatts(), 70);
        assertDoubleWithTolerance(response.get(POWER_MIN).getInWatts(), 4);
        // Expect (30+4+70+12)/4=29
        assertDoubleWithTolerance(response.get(POWER_AVG).getInWatts(), 29);
    }

    @Test
    public void aggregateWithInstantFilter_heartRate() throws Exception {
        Instant time = Instant.now().minus(1, DAYS);
        insertRecords(
                List.of(
                        getHeartRateRecord(
                                time,
                                time.plus(8, HOURS),
                                UTC,
                                getHeartRateRecordSample(62, time.plus(30, MINUTES)),
                                getHeartRateRecordSample(65, time.plus(1, HOURS)),
                                getHeartRateRecordSample(75, time.plus(2, HOURS)),
                                getHeartRateRecordSample(60, time.plus(3, HOURS)),
                                getHeartRateRecordSample(49, time.plus(4, HOURS)),
                                getHeartRateRecordSample(200, time.plus(6, HOURS)))));

        TimeInstantRangeFilter timeFilter =
                getTimeFilter(time.plus(31, MINUTES), time.plus(4, HOURS));
        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(timeFilter)
                        .addAggregationType(BPM_MIN)
                        .addAggregationType(BPM_MAX)
                        .addAggregationType(BPM_AVG)
                        .addAggregationType(HEART_MEASUREMENTS_COUNT)
                        .build();

        AggregateRecordsResponse<Long> response = getAggregateResponse(aggregateRecordsRequest);
        assertThat(response.get(BPM_MIN)).isEqualTo(60);
        assertThat(response.get(BPM_MAX)).isEqualTo(75);
        assertThat(response.get(BPM_AVG)).isEqualTo(66);
        assertThat(response.get(HEART_MEASUREMENTS_COUNT)).isEqualTo(3);
    }

    @Test
    public void aggregateWithInstantFilter_cyclingPedalingCadence() throws Exception {
        Instant time = Instant.now().minus(3, DAYS);
        insertRecords(
                List.of(
                        getCyclingPedalingCadenceRecord(
                                time,
                                time.plus(8, HOURS),
                                UTC,
                                getCyclingPedalingCadenceSample(100, time),
                                getCyclingPedalingCadenceSample(63, time.plus(1, HOURS)),
                                getCyclingPedalingCadenceSample(20, time.plus(2, HOURS)),
                                getCyclingPedalingCadenceSample(30, time.plus(3, HOURS)),
                                getCyclingPedalingCadenceSample(5, time.plus(4, HOURS)),
                                getCyclingPedalingCadenceSample(50, time.plus(6, HOURS)))));

        TimeInstantRangeFilter timeFilter =
                getTimeFilter(time.plus(1, MINUTES), time.plus(3, HOURS));
        AggregateRecordsRequest<Double> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Double>(timeFilter)
                        .addAggregationType(RPM_AVG)
                        .addAggregationType(RPM_MIN)
                        .addAggregationType(RPM_MAX)
                        .build();

        AggregateRecordsResponse<Double> response = getAggregateResponse(aggregateRecordsRequest);
        assertDoubleWithTolerance(response.get(RPM_AVG), 41.5);
        assertDoubleWithTolerance(response.get(RPM_MIN), 20);
        assertDoubleWithTolerance(response.get(RPM_MAX), 63);
    }

    private static SpeedRecord getSpeedRecord(
            Instant start,
            Instant end,
            ZoneOffset offset,
            SpeedRecord.SpeedRecordSample... samples) {
        return new SpeedRecord.Builder(getEmptyMetadata(), start, end, Arrays.asList(samples))
                .setStartZoneOffset(offset)
                .setEndZoneOffset(offset)
                .build();
    }

    private static SpeedRecord.SpeedRecordSample getSpeedRecordSample(
            Velocity velocity, Instant time) {
        return new SpeedRecord.SpeedRecordSample(velocity, time);
    }

    private static PowerRecord getPowerRecord(
            Instant start,
            Instant end,
            ZoneOffset offset,
            PowerRecord.PowerRecordSample... samples) {
        return new PowerRecord.Builder(getEmptyMetadata(), start, end, Arrays.asList(samples))
                .setStartZoneOffset(offset)
                .setEndZoneOffset(offset)
                .build();
    }

    private static PowerRecord.PowerRecordSample getPowerRecordSample(Power power, Instant time) {
        return new PowerRecord.PowerRecordSample(power, time);
    }

    private static HeartRateRecord getHeartRateRecord(
            Instant start,
            Instant end,
            ZoneOffset offset,
            HeartRateRecord.HeartRateSample... samples) {
        return new HeartRateRecord.Builder(getEmptyMetadata(), start, end, Arrays.asList(samples))
                .setStartZoneOffset(offset)
                .setEndZoneOffset(offset)
                .build();
    }

    private static HeartRateRecord.HeartRateSample getHeartRateRecordSample(
            long rate, Instant time) {
        return new HeartRateRecord.HeartRateSample(rate, time);
    }

    private static CyclingPedalingCadenceRecord getCyclingPedalingCadenceRecord(
            Instant start,
            Instant end,
            ZoneOffset offset,
            CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample... samples) {
        return new CyclingPedalingCadenceRecord.Builder(
                        getEmptyMetadata(), start, end, Arrays.asList(samples))
                .setStartZoneOffset(offset)
                .setEndZoneOffset(offset)
                .build();
    }

    private static CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
            getCyclingPedalingCadenceSample(double revolutionsPerMinute, Instant time) {
        return new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                revolutionsPerMinute, time);
    }
}
