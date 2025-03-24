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

package android.healthconnect.cts.datatypes;

import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.google.common.truth.Truth.assertThat;

import static java.time.Month.APRIL;
import static java.time.temporal.ChronoUnit.HOURS;

import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.ExerciseCompletionGoal;
import android.health.connect.datatypes.ExercisePerformanceGoal;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PlannedExerciseBlock;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PlannedExerciseStep;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Velocity;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ExercisePerformanceGoalTest {
    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @After
    public void tearDown() throws InterruptedException {
        verifyDeleteRecords(
                PlannedExerciseSessionRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void powerGoal_equalsAndHashCode() {
        assertThat(
                        new ExercisePerformanceGoal.PowerGoal(
                                Power.fromWatts(100), Power.fromWatts(100)))
                .isNotEqualTo(
                        new ExercisePerformanceGoal.PowerGoal(
                                Power.fromWatts(200), Power.fromWatts(200)));
        assertThat(
                        new ExercisePerformanceGoal.PowerGoal(
                                Power.fromWatts(100), Power.fromWatts(100)))
                .isEqualTo(
                        new ExercisePerformanceGoal.PowerGoal(
                                Power.fromWatts(100), Power.fromWatts(100)));

        assertThat(
                        new ExercisePerformanceGoal.PowerGoal(
                                        Power.fromWatts(100), Power.fromWatts(100))
                                .hashCode())
                .isNotEqualTo(
                        new ExercisePerformanceGoal.PowerGoal(
                                        Power.fromWatts(200), Power.fromWatts(200))
                                .hashCode());
        assertThat(
                        new ExercisePerformanceGoal.PowerGoal(
                                        Power.fromWatts(100), Power.fromWatts(100))
                                .hashCode())
                .isEqualTo(
                        new ExercisePerformanceGoal.PowerGoal(
                                        Power.fromWatts(100), Power.fromWatts(100))
                                .hashCode());
    }

    @Test
    public void powerGoal_insertAndRead() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder record =
                createPlannedSessionWithPerformanceGoal(
                        new ExercisePerformanceGoal.PowerGoal(
                                Power.fromWatts(200), Power.fromWatts(240)));

        String id = TestUtils.insertRecordAndGetId(record.build());

        assertThat(
                        Iterables.getOnlyElement(
                                TestUtils.readAllRecords(PlannedExerciseSessionRecord.class)))
                .isEqualTo(record.setMetadata(createMetadata(id)).build());
    }

    @Test
    public void powerGoal_getters() {
        ExercisePerformanceGoal.PowerGoal goal =
                new ExercisePerformanceGoal.PowerGoal(Power.fromWatts(200), Power.fromWatts(240));

        assertThat(goal.getMinPower()).isEqualTo(Power.fromWatts(200));
        assertThat(goal.getMaxPower()).isEqualTo(Power.fromWatts(240));
    }

    @Test
    public void speedGoal_equalsAndHashCode() {
        assertThat(
                        new ExercisePerformanceGoal.SpeedGoal(
                                Velocity.fromMetersPerSecond(10), Velocity.fromMetersPerSecond(10)))
                .isNotEqualTo(
                        new ExercisePerformanceGoal.SpeedGoal(
                                Velocity.fromMetersPerSecond(20),
                                Velocity.fromMetersPerSecond(20)));
        assertThat(
                        new ExercisePerformanceGoal.SpeedGoal(
                                Velocity.fromMetersPerSecond(10), Velocity.fromMetersPerSecond(10)))
                .isEqualTo(
                        new ExercisePerformanceGoal.SpeedGoal(
                                Velocity.fromMetersPerSecond(10),
                                Velocity.fromMetersPerSecond(10)));

        assertThat(
                        new ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(10),
                                        Velocity.fromMetersPerSecond(10))
                                .hashCode())
                .isNotEqualTo(
                        new ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(20),
                                        Velocity.fromMetersPerSecond(20))
                                .hashCode());
        assertThat(
                        new ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(10),
                                        Velocity.fromMetersPerSecond(10))
                                .hashCode())
                .isEqualTo(
                        new ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(10),
                                        Velocity.fromMetersPerSecond(10))
                                .hashCode());
    }

    @Test
    public void speedGoal_insertAndRead() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder record =
                createPlannedSessionWithPerformanceGoal(
                        new ExercisePerformanceGoal.SpeedGoal(
                                Velocity.fromMetersPerSecond(10.0),
                                Velocity.fromMetersPerSecond(12.0)));

        String id = TestUtils.insertRecordAndGetId(record.build());

        assertThat(
                        Iterables.getOnlyElement(
                                TestUtils.readAllRecords(PlannedExerciseSessionRecord.class)))
                .isEqualTo(record.setMetadata(createMetadata(id)).build());
    }

    @Test
    public void speedGoal_getters() {
        ExercisePerformanceGoal.SpeedGoal goal =
                new ExercisePerformanceGoal.SpeedGoal(
                        Velocity.fromMetersPerSecond(10.5), Velocity.fromMetersPerSecond(12.5));

        assertThat(goal.getMinSpeed()).isEqualTo(Velocity.fromMetersPerSecond(10.5));
        assertThat(goal.getMaxSpeed()).isEqualTo(Velocity.fromMetersPerSecond(12.5));
    }

    @Test
    public void cadenceGoal_equalsAndHashCode() {
        assertThat(new ExercisePerformanceGoal.CadenceGoal(10, 10))
                .isNotEqualTo(new ExercisePerformanceGoal.CadenceGoal(20, 20));
        assertThat(new ExercisePerformanceGoal.CadenceGoal(10, 10))
                .isEqualTo(new ExercisePerformanceGoal.CadenceGoal(10, 10));

        assertThat(new ExercisePerformanceGoal.CadenceGoal(10, 10).hashCode())
                .isNotEqualTo(new ExercisePerformanceGoal.CadenceGoal(20, 20).hashCode());
        assertThat(new ExercisePerformanceGoal.CadenceGoal(10, 10).hashCode())
                .isEqualTo(new ExercisePerformanceGoal.CadenceGoal(10, 10).hashCode());
    }

    @Test
    public void cadenceGoal_insertAndRead() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder record =
                createPlannedSessionWithPerformanceGoal(
                        new ExercisePerformanceGoal.CadenceGoal(80.0, 85.0));

        String id = TestUtils.insertRecordAndGetId(record.build());

        assertThat(
                        Iterables.getOnlyElement(
                                TestUtils.readAllRecords(PlannedExerciseSessionRecord.class)))
                .isEqualTo(record.setMetadata(createMetadata(id)).build());
    }

    @Test
    public void cadenceGoal_getters() {
        ExercisePerformanceGoal.CadenceGoal goal =
                new ExercisePerformanceGoal.CadenceGoal(80.0, 90.0);

        assertThat(goal.getMinRpm()).isEqualTo(80.0);
        assertThat(goal.getMaxRpm()).isEqualTo(90.0);
    }

    @Test
    public void heartRateGoal_equalsAndHashCode() {
        assertThat(new ExercisePerformanceGoal.HeartRateGoal(100, 100))
                .isNotEqualTo(new ExercisePerformanceGoal.HeartRateGoal(200, 200));
        assertThat(new ExercisePerformanceGoal.HeartRateGoal(100, 100))
                .isEqualTo(new ExercisePerformanceGoal.HeartRateGoal(100, 100));

        assertThat(new ExercisePerformanceGoal.HeartRateGoal(100, 100).hashCode())
                .isNotEqualTo(new ExercisePerformanceGoal.HeartRateGoal(200, 200).hashCode());
        assertThat(new ExercisePerformanceGoal.HeartRateGoal(100, 100).hashCode())
                .isEqualTo(new ExercisePerformanceGoal.HeartRateGoal(100, 100).hashCode());
    }

    @Test
    public void heartRateGoal_insertAndRead() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder record =
                createPlannedSessionWithPerformanceGoal(
                        new ExercisePerformanceGoal.HeartRateGoal(120, 130));

        String id = TestUtils.insertRecordAndGetId(record.build());

        assertThat(
                        Iterables.getOnlyElement(
                                TestUtils.readAllRecords(PlannedExerciseSessionRecord.class)))
                .isEqualTo(record.setMetadata(createMetadata(id)).build());
    }

    @Test
    public void heartRateGoal_getters() {
        ExercisePerformanceGoal.HeartRateGoal goal =
                new ExercisePerformanceGoal.HeartRateGoal(140, 155);

        assertThat(goal.getMinBpm()).isEqualTo(140);
        assertThat(goal.getMaxBpm()).isEqualTo(155);
    }

    @Test
    public void weightGoal_equalsAndHashCode() {
        assertThat(new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(100_000)))
                .isNotEqualTo(new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(200_000)));
        assertThat(new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(100_000)))
                .isEqualTo(new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(100_000)));

        assertThat(new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(100_000)).hashCode())
                .isNotEqualTo(
                        new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(200_000)).hashCode());
        assertThat(new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(100_000)).hashCode())
                .isEqualTo(
                        new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(100_000)).hashCode());
    }

    @Test
    public void weightGoal_insertAndRead() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder record =
                createPlannedSessionWithPerformanceGoal(
                        new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(80_000)));

        String id = TestUtils.insertRecordAndGetId(record.build());

        assertThat(
                        Iterables.getOnlyElement(
                                TestUtils.readAllRecords(PlannedExerciseSessionRecord.class)))
                .isEqualTo(record.setMetadata(createMetadata(id)).build());
    }

    @Test
    public void weightGoal_getters() {
        ExercisePerformanceGoal.WeightGoal goal =
                new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(20_000));

        assertThat(goal.getMass()).isEqualTo(Mass.fromGrams(20_000));
    }

    @Test
    public void rpeGoal_equalsAndHashCode() {
        assertThat(new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(1))
                .isNotEqualTo(new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(2));
        assertThat(new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(1))
                .isEqualTo(new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(1));

        assertThat(new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(1).hashCode())
                .isNotEqualTo(
                        new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(2).hashCode());
        assertThat(new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(1).hashCode())
                .isEqualTo(new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(1).hashCode());
    }

    @Test
    public void rpeGoal_insertAndRead() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder record =
                createPlannedSessionWithPerformanceGoal(
                        new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(6));

        String id = TestUtils.insertRecordAndGetId(record.build());

        assertThat(
                        Iterables.getOnlyElement(
                                TestUtils.readAllRecords(PlannedExerciseSessionRecord.class)))
                .isEqualTo(record.setMetadata(createMetadata(id)).build());
    }

    @Test
    public void rpeGoal_getters() {
        ExercisePerformanceGoal.RateOfPerceivedExertionGoal goal =
                new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(5);

        assertThat(goal.getRpe()).isEqualTo(5);
    }

    @Test
    public void amrapGoal_insertAndRead() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder record =
                createPlannedSessionWithPerformanceGoal(ExercisePerformanceGoal.AmrapGoal.INSTANCE);

        String id = TestUtils.insertRecordAndGetId(record.build());

        assertThat(
                        Iterables.getOnlyElement(
                                TestUtils.readAllRecords(PlannedExerciseSessionRecord.class)))
                .isEqualTo(record.setMetadata(createMetadata(id)).build());
    }

    private Metadata createMetadata(String id) {
        return new Metadata.Builder()
                .setDataOrigin(
                        new DataOrigin.Builder()
                                .setPackageName("android.healthconnect.cts")
                                .build())
                .setId(id)
                .setClientRecordId(null)
                .setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
                .build();
    }

    private PlannedExerciseSessionRecord.Builder createPlannedSessionWithPerformanceGoal(
            ExercisePerformanceGoal goal) {
        return new PlannedExerciseSessionRecord.Builder(
                        createMetadata(UUID.randomUUID().toString()),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.of(1, HOURS))
                .addBlock(
                        new PlannedExerciseBlock.Builder(1)
                                .addStep(createStepWithPerformanceGoal(goal))
                                .build());
    }

    private PlannedExerciseStep createStepWithPerformanceGoal(ExercisePerformanceGoal goal) {
        return new PlannedExerciseStep.Builder(
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE,
                        new ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(200)))
                .addPerformanceGoal(goal)
                .build();
    }
}
