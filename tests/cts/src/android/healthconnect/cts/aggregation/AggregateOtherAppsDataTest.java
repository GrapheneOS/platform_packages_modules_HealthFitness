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

import static android.health.connect.datatypes.DistanceRecord.DISTANCE_TOTAL;
import static android.health.connect.datatypes.ExerciseSessionRecord.EXERCISE_DURATION_TOTAL;
import static android.health.connect.datatypes.HeartRateRecord.BPM_MAX;
import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.health.connect.datatypes.TotalCaloriesBurnedRecord.ENERGY_TOTAL;
import static android.healthconnect.cts.utils.DataFactory.NOW;
import static android.healthconnect.cts.utils.DataFactory.buildExerciseSessionWithEmptyMetadata;
import static android.healthconnect.cts.utils.DataFactory.buildSleepSessionWithEmptyMetadata;
import static android.healthconnect.cts.utils.DataFactory.getDistanceRecordWithEmptyMetadata;
import static android.healthconnect.cts.utils.DataFactory.getHeartRateRecordWithEmptyMetadata;
import static android.healthconnect.cts.utils.DataFactory.getStepsRecordWithEmptyMetaData;
import static android.healthconnect.cts.utils.DataFactory.getTotalCaloriesBurnedRecordWithEmptyMetadata;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.setupAggregation;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.DAYS;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthDataCategory;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;

public class AggregateOtherAppsDataTest {
    private static final TestAppProxy APP_WITH_WRITE_PERMS_ONLY =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.writePermsOnly");

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        deleteAllStagedRemoteData();
    }

    @Test
    public void testAppWithReadPerms_aggregateOtherAppsDistance_expectSuccess() throws Exception {
        setupAggregation(
                APP_WITH_WRITE_PERMS_ONLY::insertRecord,
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(),
                HealthDataCategory.ACTIVITY);
        APP_WITH_WRITE_PERMS_ONLY.insertRecord(getDistanceRecordWithEmptyMetadata());

        assertAggregation(DISTANCE_TOTAL, Length.fromMeters(10));
    }

    @Test
    public void testAppWithReadPerms_aggregateOtherAppsSteps_expectSuccess() throws Exception {
        setupAggregation(
                APP_WITH_WRITE_PERMS_ONLY::insertRecord,
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(),
                HealthDataCategory.ACTIVITY);
        APP_WITH_WRITE_PERMS_ONLY.insertRecord(getStepsRecordWithEmptyMetaData());

        assertAggregation(STEPS_COUNT_TOTAL, 10L);
    }

    @Test
    public void testAppWithReadPerms_aggregateOtherAppsHeartRate_expectSuccess() throws Exception {
        setupAggregation(
                APP_WITH_WRITE_PERMS_ONLY::insertRecord,
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(),
                HealthDataCategory.VITALS);
        APP_WITH_WRITE_PERMS_ONLY.insertRecord(getHeartRateRecordWithEmptyMetadata());

        assertAggregation(BPM_MAX, 72L);
    }

    @Test
    public void testAppWithReadPerms_aggregateOtherAppsTotalCaloriesBurned_expectSuccess()
            throws Exception {
        setupAggregation(
                APP_WITH_WRITE_PERMS_ONLY::insertRecord,
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(),
                HealthDataCategory.ACTIVITY);
        TotalCaloriesBurnedRecord record = getTotalCaloriesBurnedRecordWithEmptyMetadata();
        APP_WITH_WRITE_PERMS_ONLY.insertRecord(record);

        assertAggregation(
                ENERGY_TOTAL, Energy.fromCalories(10), record.getStartTime(), record.getEndTime());
    }

    @Test
    public void testAppWithReadPerms_aggregateOtherAppsExercise_expectSuccess() throws Exception {
        setupAggregation(
                APP_WITH_WRITE_PERMS_ONLY::insertRecord,
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(),
                HealthDataCategory.ACTIVITY);
        ExerciseSessionRecord exerciseSessionRecord = buildExerciseSessionWithEmptyMetadata();
        APP_WITH_WRITE_PERMS_ONLY.insertRecord(exerciseSessionRecord);

        assertAggregation(
                EXERCISE_DURATION_TOTAL,
                exerciseSessionRecord.getEndTime().toEpochMilli()
                        - exerciseSessionRecord.getStartTime().toEpochMilli());
    }

    @Test
    public void testAppWithReadPerms_aggregateOtherAppsSleep_expectSuccess() throws Exception {
        setupAggregation(
                APP_WITH_WRITE_PERMS_ONLY::insertRecord,
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(),
                HealthDataCategory.SLEEP);
        SleepSessionRecord sleepSessionRecord = buildSleepSessionWithEmptyMetadata();
        APP_WITH_WRITE_PERMS_ONLY.insertRecord(sleepSessionRecord);

        assertAggregation(
                SleepSessionRecord.SLEEP_DURATION_TOTAL,
                sleepSessionRecord.getEndTime().toEpochMilli()
                        - sleepSessionRecord.getStartTime().toEpochMilli());
    }

    private static <T> void assertAggregation(AggregationType<T> aggregationType, T expectedValue)
            throws InterruptedException {
        assertAggregation(
                aggregationType, expectedValue, NOW.minus(1000, DAYS), NOW.plus(1000, DAYS));
    }

    private static <T> void assertAggregation(
            AggregationType<T> aggregationType, T expectedValue, Instant startTime, Instant endTime)
            throws InterruptedException {
        AggregateRecordsResponse<T> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<T>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(startTime)
                                                .setEndTime(endTime)
                                                .build())
                                .addAggregationType(aggregationType)
                                .build());

        assertThat(response.get(aggregationType)).isEqualTo(expectedValue);
    }
}
