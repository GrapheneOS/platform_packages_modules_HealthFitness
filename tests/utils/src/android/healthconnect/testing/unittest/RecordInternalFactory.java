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

package android.healthconnect.testing.unittest;

import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INTERVAL;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING;

import static java.time.Duration.ofMinutes;

import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.PlannedExerciseStep;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Power;
import android.health.connect.internal.datatypes.AlcoholConsumptionRecordInternal;
import android.health.connect.internal.datatypes.BloodPressureRecordInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal;
import android.health.connect.internal.datatypes.ExerciseLapInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal;
import android.health.connect.internal.datatypes.ExerciseRouteInternal;
import android.health.connect.internal.datatypes.ExerciseSegmentInternal;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.NutritionRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseBlockInternal;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseStepInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.SleepSessionRecordInternal;
import android.health.connect.internal.datatypes.SleepStageInternal;
import android.health.connect.internal.datatypes.SpeedRecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;

import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public class RecordInternalFactory {

    public static long START_TIME = Instant.now().minus(Period.ofDays(1)).toEpochMilli();
    public static long END_TIME = Instant.now().toEpochMilli();

    public static ExerciseRouteInternal.LocationInternal buildInternalLocationAllFields() {
        return new ExerciseRouteInternal.LocationInternal()
                .setTime(START_TIME + 2)
                .setLatitude(60.321)
                .setLongitude(59.123)
                .setVerticalAccuracy(1.2)
                .setHorizontalAccuracy(20)
                .setAltitude(-12);
    }

    public static ExerciseRouteInternal.LocationInternal buildInternalLocation() {
        return new ExerciseRouteInternal.LocationInternal()
                .setTime(START_TIME + 1)
                .setLatitude(60.321)
                .setLongitude(59.123);
    }

    public static ExerciseRouteInternal buildExerciseRouteInternal() {
        return new ExerciseRouteInternal(
                List.of(buildInternalLocationAllFields(), buildInternalLocation()));
    }

    public static ExerciseSessionRecordInternal buildExerciseSessionInternal() {
        return (ExerciseSessionRecordInternal)
                new ExerciseSessionRecordInternal()
                        .setExerciseType(ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT)
                        .setRoute(buildExerciseRouteInternal())
                        .setTitle("Morning walk")
                        .setNotes("Sunny weather")
                        .setExerciseLaps(Collections.singletonList(buildExerciseLap()))
                        .setExerciseSegments(Collections.singletonList(buildExerciseSegment()))
                        .setStartTime(START_TIME)
                        .setEndTime(END_TIME)
                        .setEndZoneOffset(1)
                        .setStartZoneOffset(1)
                        .setAppInfoId(1)
                        .setClientRecordId("client_id")
                        .setManufacturer("manufacturer")
                        .setClientRecordVersion(12)
                        .setUuid(UUID.randomUUID())
                        .setPackageName("android.healthconnect.unittests")
                        .setModel("Pixel4a");
    }

    /** Creates an exercise sessions with a route. */
    public static ExerciseSessionRecordInternal buildExerciseSessionRecordWithRoute(
            Instant startTime) {
        return (ExerciseSessionRecordInternal)
                new ExerciseSessionRecordInternal()
                        .setExerciseType(EXERCISE_SESSION_TYPE_RUNNING)
                        .setRoute(buildExerciseRoute(startTime))
                        .setStartTime(startTime.toEpochMilli())
                        .setEndTime(startTime.plus(ofMinutes(10)).toEpochMilli());
    }

    /** Creates an exercise sessions with a route. */
    public static ExerciseSessionRecordInternal buildExerciseSessionRecordWithSegment(
            Instant startTime) {
        return (ExerciseSessionRecordInternal)
                new ExerciseSessionRecordInternal()
                        .setExerciseType(EXERCISE_SESSION_TYPE_RUNNING)
                        .setExerciseSegments(List.of(buildExerciseSegment(startTime)))
                        .setStartTime(startTime.toEpochMilli())
                        .setEndTime(startTime.plus(ofMinutes(10)).toEpochMilli());
    }

    /** Returns an internal exercise session instance with rate of perceived exertion set. */
    public static ExerciseSessionRecordInternal buildExerciseSessionInternalWithRpe() {
        return (ExerciseSessionRecordInternal)
                new ExerciseSessionRecordInternal()
                        .setExerciseType(ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT)
                        .setRoute(buildExerciseRouteInternal())
                        .setTitle("Morning walk")
                        .setNotes("Sunny weather")
                        .setRateOfPerceivedExertion(5.0f)
                        .setExerciseLaps(Collections.singletonList(buildExerciseLap()))
                        .setExerciseSegments(Collections.singletonList(buildExerciseSegment()))
                        .setStartTime(START_TIME)
                        .setEndTime(END_TIME)
                        .setEndZoneOffset(1)
                        .setStartZoneOffset(1)
                        .setAppInfoId(1)
                        .setClientRecordId("client_id")
                        .setManufacturer("manufacturer")
                        .setClientRecordVersion(12)
                        .setUuid(UUID.randomUUID())
                        .setPackageName("android.healthconnect.unittests")
                        .setModel("Pixel4a");
    }

    /** Returns an internal planned exercise session instance. */
    public static PlannedExerciseSessionRecordInternal buildPlannedExerciseSessionInternal() {
        return (PlannedExerciseSessionRecordInternal)
                new PlannedExerciseSessionRecordInternal(Collections.emptyList())
                        .setTitle("Sunday easy run")
                        .setNotes("Don't push yourself too hard for this one.")
                        .setExerciseType(ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING)
                        .setHasExplicitTime(true)
                        .setStartTime(START_TIME)
                        .setEndTime(START_TIME)
                        .setStartZoneOffset(1)
                        .setEndZoneOffset(1)
                        .setAppInfoId(1)
                        .setClientRecordId("client_id")
                        .setManufacturer("manufacturer")
                        .setClientRecordVersion(12)
                        .setUuid(UUID.randomUUID())
                        .setPackageName("android.healthconnect.unittests")
                        .setModel("Pixel4a");
    }

    /** Returns an internal exercise block instance. */
    public static PlannedExerciseBlockInternal buildExerciseBlockInternal() {
        PlannedExerciseBlockInternal result = new PlannedExerciseBlockInternal(1);
        result.setDescription("Warmup");
        return result;
    }

    /** Returns an internal exercise step instance. */
    public static PlannedExerciseStepInternal buildExerciseStepInternal() {
        PlannedExerciseStepInternal result =
                new PlannedExerciseStepInternal(
                        ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                        PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE,
                        new ExerciseCompletionGoalInternal.DistanceGoalInternal(
                                Length.fromMeters(1_500)));
        result.setPerformanceGoals(
                Collections.singletonList(
                        new ExercisePerformanceGoalInternal.PowerGoalInternal(
                                Power.fromWatts(180), Power.fromWatts((220)))));
        return result;
    }

    public static SleepSessionRecordInternal buildSleepSessionInternal() {
        return (SleepSessionRecordInternal)
                new SleepSessionRecordInternal()
                        .setSleepStages(Collections.singletonList(buildSleepStage()))
                        .setTitle("Morning walk")
                        .setNotes("Sunny weather")
                        .setStartTime(START_TIME)
                        .setEndTime(END_TIME)
                        .setEndZoneOffset(1)
                        .setStartZoneOffset(1)
                        .setAppInfoId(1)
                        .setClientRecordId("client_id")
                        .setManufacturer("manufacturer")
                        .setClientRecordVersion(12)
                        .setUuid(UUID.randomUUID())
                        .setPackageName("android.healthconnect.unittests")
                        .setModel("Pixel4a");
    }

    public static ExerciseLapInternal buildExerciseLap() {
        return new ExerciseLapInternal()
                .setStartTime(START_TIME)
                .setEndTime(END_TIME)
                .setLength(10);
    }

    public static SleepStageInternal buildSleepStage() {
        return new SleepStageInternal()
                .setStartTime(START_TIME)
                .setEndTime(END_TIME)
                .setStageType(SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_OUT_OF_BED);
    }

    public static ExerciseSegmentInternal buildExerciseSegment() {
        return new ExerciseSegmentInternal()
                .setStartTime(START_TIME)
                .setEndTime(END_TIME)
                .setSegmentType(ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                .setRepetitionsCount(10);
    }

    public static ExerciseSessionRecordInternal buildExerciseSessionInternalNoExtraFields() {
        return (ExerciseSessionRecordInternal)
                new ExerciseSessionRecordInternal()
                        .setExerciseType(ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT)
                        .setStartTime((long) 1e9)
                        .setEndTime((long) 1e10)
                        .setUuid(UUID.randomUUID())
                        .setPackageName("android.healthconnect.unittests");
    }

    public static SleepSessionRecordInternal buildSleepSessionInternalNoExtraFields() {
        return (SleepSessionRecordInternal)
                new SleepSessionRecordInternal()
                        .setStartTime((long) 1e9)
                        .setEndTime((long) 1e10)
                        .setUuid(UUID.randomUUID())
                        .setPackageName("android.healthconnect.unittests");
    }

    public static RecordInternal<StepsRecord> buildStepsRecord(
            long startTimeMillis, long endTimeMillis, int stepsCount) {
        return buildStepsRecord(/* clientId= */ null, startTimeMillis, endTimeMillis, stepsCount);
    }

    public static RecordInternal<StepsRecord> buildStepsRecord(
            String clientId, long startTimeMillis, long endTimeMillis, int stepsCount) {
        return new StepsRecordInternal()
                .setCount(stepsCount)
                .setStartTime(startTimeMillis)
                .setEndTime(endTimeMillis)
                .setClientRecordId(clientId);
    }

    public static RecordInternal<StepsRecord> buildStepsRecord(
            long appInfoId, long startTimeMillis, long endTimeMillis, int stepsCount) {
        return new StepsRecordInternal()
                .setCount(stepsCount)
                .setStartTime(startTimeMillis)
                .setEndTime(endTimeMillis)
                .setAppInfoId(appInfoId);
    }

    public static RecordInternal<BloodPressureRecord> buildBloodPressureRecord(
            long timeMillis, double systolic, double diastolic) {
        return new BloodPressureRecordInternal()
                .setSystolic(systolic)
                .setDiastolic(diastolic)
                .setTime(timeMillis);
    }

    public static RecordInternal<BloodPressureRecord> buildBloodPressureRecord(
            long appInfoId, long timeMillis, double systolic, double diastolic) {
        return new BloodPressureRecordInternal()
                .setSystolic(systolic)
                .setDiastolic(diastolic)
                .setTime(timeMillis)
                .setAppInfoId(appInfoId);
    }

    public static SpeedRecordInternal buildSpeedRecordInternal(Instant startTime) {
        return (SpeedRecordInternal)
                new SpeedRecordInternal(
                                Set.of(
                                        new SpeedRecordInternal.SpeedRecordSample(
                                                100, startTime.plus(ofMinutes(1)).toEpochMilli())))
                        .setStartTime(startTime.toEpochMilli())
                        .setEndTime(startTime.plus(ofMinutes(10)).toEpochMilli());
    }

    public static NutritionRecordInternal buildNutritionRecordInternal(
            long startTimeMillis, long endTimeMillis) {
        return (NutritionRecordInternal)
                new NutritionRecordInternal()
                        .setStartTime(startTimeMillis)
                        .setEndTime(endTimeMillis);
    }

    /** Returns an internal alcohol consumption record instance. */
    public static AlcoholConsumptionRecordInternal buildAlcoholConsumptionRecordInternal(
            long startTimeMillis, long endTimeMillis) {
        return buildAlcoholConsumptionRecordInternal(
                startTimeMillis,
                endTimeMillis,
                RECORD_TEMPORAL_TYPE_INTERVAL,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER,
                0.5,
                5.0,
                "note");
    }

    /** Returns an internal alcohol consumption record instance. */
    public static AlcoholConsumptionRecordInternal buildAlcoholConsumptionRecordInternal(
            long startTimeMillis,
            long endTimeMillis,
            int temporalType,
            int beverageType,
            double servingVolume,
            double alcoholByVolume,
            String notes) {
        return (AlcoholConsumptionRecordInternal)
                new AlcoholConsumptionRecordInternal()
                        .setBeverageType(beverageType)
                        .setServingVolumeLiters(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNotes(notes)
                        .setTemporalType(temporalType)
                        .setStartTime(startTimeMillis)
                        .setEndTime(endTimeMillis);
    }

    private static ExerciseRouteInternal buildExerciseRoute(Instant startTime) {
        int numberOfLocations = 3;
        double latitude = 52.13;
        double longitude = 0.14;

        return new ExerciseRouteInternal(
                IntStream.range(0, numberOfLocations)
                        .mapToObj(
                                i ->
                                        new ExerciseRouteInternal.LocationInternal()
                                                .setTime(startTime.plusSeconds(i).toEpochMilli())
                                                .setLatitude(latitude + 0.001 * i)
                                                .setLongitude(longitude + 0.001 * i))
                        .toList());
    }

    private static ExerciseSegmentInternal buildExerciseSegment(Instant startTime) {
        return new ExerciseSegmentInternal()
                .setStartTime(startTime.plusSeconds(1).toEpochMilli())
                .setStartTime(startTime.plusSeconds(2).toEpochMilli())
                .setSegmentType(ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                .setRepetitionsCount(5);
    }
}
