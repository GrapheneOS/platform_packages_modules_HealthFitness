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

package com.android.server.healthconnect.backuprestore;

import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS;
import static android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_AFTER_MEAL;
import static android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_TEARS;
import static android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST;
import static android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_LYING_DOWN;
import static android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM;
import static android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TOE;
import static android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_UNUSUAL;
import static android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusSensation.SENSATION_HEAVY;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DEADLIFT;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SQUAT;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STRENGTH_TRAINING;
import static android.health.connect.datatypes.MealType.MEAL_TYPE_DINNER;
import static android.health.connect.datatypes.MealType.MEAL_TYPE_SNACK;
import static android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_HEAVY;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED;
import static android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult.RESULT_HIGH;
import static android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE;
import static android.health.connect.datatypes.SexualActivityRecord.SexualActivityProtectionUsed.PROTECTION_USED_PROTECTED;
import static android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP;

import android.health.connect.datatypes.RecordTypeIdentifier;

import com.android.server.healthconnect.proto.backuprestore.ActiveCaloriesBurned;
import com.android.server.healthconnect.proto.backuprestore.ActivityIntensity;
import com.android.server.healthconnect.proto.backuprestore.BasalBodyTemperature;
import com.android.server.healthconnect.proto.backuprestore.BasalMetabolicRate;
import com.android.server.healthconnect.proto.backuprestore.BloodGlucose;
import com.android.server.healthconnect.proto.backuprestore.BloodPressure;
import com.android.server.healthconnect.proto.backuprestore.BodyFat;
import com.android.server.healthconnect.proto.backuprestore.BodyTemperature;
import com.android.server.healthconnect.proto.backuprestore.BodyWaterMass;
import com.android.server.healthconnect.proto.backuprestore.BoneMass;
import com.android.server.healthconnect.proto.backuprestore.CervicalMucus;
import com.android.server.healthconnect.proto.backuprestore.CyclingPedalingCadence;
import com.android.server.healthconnect.proto.backuprestore.CyclingPedalingCadence.CyclingPedalingCadenceSample;
import com.android.server.healthconnect.proto.backuprestore.Distance;
import com.android.server.healthconnect.proto.backuprestore.ElevationGained;
import com.android.server.healthconnect.proto.backuprestore.ExerciseSession;
import com.android.server.healthconnect.proto.backuprestore.ExerciseSession.ExerciseLap;
import com.android.server.healthconnect.proto.backuprestore.ExerciseSession.ExerciseRoute;
import com.android.server.healthconnect.proto.backuprestore.ExerciseSession.ExerciseRoute.Location;
import com.android.server.healthconnect.proto.backuprestore.ExerciseSession.ExerciseSegment;
import com.android.server.healthconnect.proto.backuprestore.FloorsClimbed;
import com.android.server.healthconnect.proto.backuprestore.HeartRate;
import com.android.server.healthconnect.proto.backuprestore.HeartRate.HeartRateSample;
import com.android.server.healthconnect.proto.backuprestore.HeartRateVariabilityRmssd;
import com.android.server.healthconnect.proto.backuprestore.Height;
import com.android.server.healthconnect.proto.backuprestore.Hydration;
import com.android.server.healthconnect.proto.backuprestore.InstantRecord;
import com.android.server.healthconnect.proto.backuprestore.IntermenstrualBleeding;
import com.android.server.healthconnect.proto.backuprestore.IntervalRecord;
import com.android.server.healthconnect.proto.backuprestore.LeanBodyMass;
import com.android.server.healthconnect.proto.backuprestore.MenstruationFlow;
import com.android.server.healthconnect.proto.backuprestore.MenstruationPeriod;
import com.android.server.healthconnect.proto.backuprestore.MindfulnessSession;
import com.android.server.healthconnect.proto.backuprestore.Nutrition;
import com.android.server.healthconnect.proto.backuprestore.OvulationTest;
import com.android.server.healthconnect.proto.backuprestore.OxygenSaturation;
import com.android.server.healthconnect.proto.backuprestore.PlannedExerciseSession;
import com.android.server.healthconnect.proto.backuprestore.PlannedExerciseSession.PlannedExerciseBlock;
import com.android.server.healthconnect.proto.backuprestore.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep;
import com.android.server.healthconnect.proto.backuprestore.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep.ExerciseCompletionGoal;
import com.android.server.healthconnect.proto.backuprestore.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep.ExerciseCompletionGoal.UnspecifiedGoal;
import com.android.server.healthconnect.proto.backuprestore.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep.ExercisePerformanceGoal;
import com.android.server.healthconnect.proto.backuprestore.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep.ExercisePerformanceGoal.WeightGoal;
import com.android.server.healthconnect.proto.backuprestore.Power;
import com.android.server.healthconnect.proto.backuprestore.Power.PowerSample;
import com.android.server.healthconnect.proto.backuprestore.Record;
import com.android.server.healthconnect.proto.backuprestore.RespiratoryRate;
import com.android.server.healthconnect.proto.backuprestore.RestingHeartRate;
import com.android.server.healthconnect.proto.backuprestore.SexualActivity;
import com.android.server.healthconnect.proto.backuprestore.SkinTemperature;
import com.android.server.healthconnect.proto.backuprestore.SkinTemperature.SkinTemperatureDeltaSample;
import com.android.server.healthconnect.proto.backuprestore.SleepSession;
import com.android.server.healthconnect.proto.backuprestore.Speed;
import com.android.server.healthconnect.proto.backuprestore.Speed.SpeedSample;
import com.android.server.healthconnect.proto.backuprestore.Steps;
import com.android.server.healthconnect.proto.backuprestore.StepsCadence;
import com.android.server.healthconnect.proto.backuprestore.StepsCadence.StepsCadenceSample;
import com.android.server.healthconnect.proto.backuprestore.TotalCaloriesBurned;
import com.android.server.healthconnect.proto.backuprestore.Vo2Max;
import com.android.server.healthconnect.proto.backuprestore.Weight;
import com.android.server.healthconnect.proto.backuprestore.WheelchairPushes;

import java.util.UUID;

final class ProtoTestData {

    static final String TEST_PACKAGE_NAME = "test.package.name";

    static Record generateRecord(@RecordTypeIdentifier.RecordType int recordType) {
        return switch (recordType) {
            case RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setActiveCaloriesBurned(
                                                    generateActiveCaloriesBurned()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setActivityIntensity(generateActivityIntensity()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setBasalBodyTemperature(
                                                    generateBasalBodyTemperature()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setBasalMetabolicRate(generateBasalMetabolicRate()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord().setBloodGlucose(generateBloodGlucose()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setBloodPressure(generateBloodPressure()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BODY_FAT ->
                    generateCoreRecord()
                            .setInstantRecord(generateInstantRecord().setBodyFat(generateBodyFat()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setBodyTemperature(generateBodyTemperature()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setBodyWaterMass(generateBodyWaterMass()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_BONE_MASS ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord().setBoneMass(generateBoneMass()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setCervicalMucus(generateCervicalMucus()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setCyclingPedalingCadence(
                                                    generateCyclingPedalingCadence()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_DISTANCE ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord().setDistance(generateDistance()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setElevationGained(generateElevationGained()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setExerciseSession(generateExerciseSession()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setFloorsClimbed(generateFloorsClimbed()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_HEART_RATE ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord().setHeartRate(generateHeartRate()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setHeartRateVariabilityRmssd(
                                                    generateHeartRateVariabilityRmssd()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_HEIGHT ->
                    generateCoreRecord()
                            .setInstantRecord(generateInstantRecord().setHeight(generateHeight()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_HYDRATION ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord().setHydration(generateHydration()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setIntermenstrualBleeding(
                                                    generateIntermenstrualBleeding()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord().setLeanBodyMass(generateLeanBodyMass()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setMenstruationFlow(generateMenstruationFlow()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setMenstruationPeriod(generateMenstruationPeriod()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setMindfulnessSession(generateMindfulnessSession()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_NUTRITION ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord().setNutrition(generateNutrition()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setOvulationTest(generateOvulationTest()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setOxygenSaturation(generateOxygenSaturation()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setPlannedExerciseSession(
                                                    generatePlannedExerciseSession()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_POWER ->
                    generateCoreRecord()
                            .setIntervalRecord(generateIntervalRecord().setPower(generatePower()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setRespiratoryRate(generateRespiratoryRate()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setRestingHeartRate(generateRestingHeartRate()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY ->
                    generateCoreRecord()
                            .setInstantRecord(
                                    generateInstantRecord()
                                            .setSexualActivity(generateSexualActivity()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setSkinTemperature(generateSkinTemperature()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setSleepSession(generateSleepSession()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_SPEED ->
                    generateCoreRecord()
                            .setIntervalRecord(generateIntervalRecord().setSpeed(generateSpeed()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_STEPS ->
                    generateCoreRecord()
                            .setIntervalRecord(generateIntervalRecord().setSteps(generateSteps()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setStepsCadence(generateStepsCadence()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setTotalCaloriesBurned(generateTotalCaloriesBurned()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_VO2_MAX ->
                    generateCoreRecord()
                            .setInstantRecord(generateInstantRecord().setVo2Max(generateVo2Max()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_WEIGHT ->
                    generateCoreRecord()
                            .setInstantRecord(generateInstantRecord().setWeight(generateWeight()))
                            .build();
            case RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES ->
                    generateCoreRecord()
                            .setIntervalRecord(
                                    generateIntervalRecord()
                                            .setWheelchairPushes(generateWheelchairPushes()))
                            .build();
            default -> throw new IllegalArgumentException("Unexpected value: " + recordType);
        };
    }

    static ActiveCaloriesBurned generateActiveCaloriesBurned() {
        return ActiveCaloriesBurned.newBuilder().setEnergy(123).build();
    }

    static ActivityIntensity generateActivityIntensity() {
        return ActivityIntensity.newBuilder()
                .setActivityIntensityType(ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                .build();
    }

    static BasalBodyTemperature generateBasalBodyTemperature() {
        return BasalBodyTemperature.newBuilder()
                .setMeasurementLocation(MEASUREMENT_LOCATION_RECTUM)
                .setTemperature(123)
                .build();
    }

    static BasalMetabolicRate generateBasalMetabolicRate() {
        return BasalMetabolicRate.newBuilder().setBasalMetabolicRate(123.45).build();
    }

    static BloodGlucose generateBloodGlucose() {
        return BloodGlucose.newBuilder()
                .setSpecimenSource(SPECIMEN_SOURCE_TEARS)
                .setLevel(123.45)
                .setRelationToMeal(RELATION_TO_MEAL_AFTER_MEAL)
                .setMealType(MEAL_TYPE_SNACK)
                .build();
    }

    static BloodPressure generateBloodPressure() {
        return BloodPressure.newBuilder()
                .setMeasurementLocation(BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST)
                .setSystolic(120)
                .setDiastolic(80)
                .setBodyPosition(BODY_POSITION_LYING_DOWN)
                .build();
    }

    static BodyFat generateBodyFat() {
        return BodyFat.newBuilder().setPercentage(12.34).build();
    }

    static BodyTemperature generateBodyTemperature() {
        return BodyTemperature.newBuilder()
                .setMeasurementLocation(MEASUREMENT_LOCATION_TOE)
                .setTemperature(321)
                .build();
    }

    static BodyWaterMass generateBodyWaterMass() {
        return BodyWaterMass.newBuilder().setBodyWaterMass(12.345).build();
    }

    static BoneMass generateBoneMass() {
        return BoneMass.newBuilder().setMass(12.345).build();
    }

    static CervicalMucus generateCervicalMucus() {
        return CervicalMucus.newBuilder()
                .setSensation(SENSATION_HEAVY)
                .setAppearance(APPEARANCE_UNUSUAL)
                .build();
    }

    static CyclingPedalingCadence generateCyclingPedalingCadence() {
        return CyclingPedalingCadence.newBuilder()
                .addSample(
                        CyclingPedalingCadenceSample.newBuilder()
                                .setRevolutionsPerMinute(12.34)
                                .setEpochMillis(123456)
                                .build())
                .build();
    }

    static Distance generateDistance() {
        return Distance.newBuilder().setDistance(123.456).build();
    }

    static ElevationGained generateElevationGained() {
        return ElevationGained.newBuilder().setElevation(123.456).build();
    }

    static ExerciseSession generateExerciseSession() {
        return ExerciseSession.newBuilder()
                .setExerciseType(EXERCISE_SESSION_TYPE_STRENGTH_TRAINING)
                .setHasRoute(true)
                .setRoute(
                        ExerciseRoute.newBuilder()
                                .addRouteLocation(
                                        Location.newBuilder()
                                                .setTime(123456)
                                                .setLatitude(60.321)
                                                .setLongitude(59.123)
                                                .setVerticalAccuracy(1.2)
                                                .setHorizontalAccuracy(20)
                                                .setAltitude(-12)))
                .setTitle("SICK DEADLIFTS")
                .setNotes("LIGHTWEIGHT BABY!")
                .addLap(
                        ExerciseLap.newBuilder()
                                .setStartTime(123456)
                                .setEndTime(654321)
                                .setLength(10))
                .addSegment(
                        ExerciseSegment.newBuilder()
                                .setStartTime(123456)
                                .setEndTime(654321)
                                .setSegmentType(EXERCISE_SEGMENT_TYPE_DEADLIFT)
                                .setRepetitionsCount(10))
                .build();
    }

    static FloorsClimbed generateFloorsClimbed() {
        return FloorsClimbed.newBuilder().setFloors(12.34).build();
    }

    static HeartRate generateHeartRate() {
        return HeartRate.newBuilder()
                .addSample(
                        HeartRateSample.newBuilder()
                                .setBeatsPerMinute(12)
                                .setEpochMillis(123456)
                                .build())
                .build();
    }

    static HeartRateVariabilityRmssd generateHeartRateVariabilityRmssd() {
        return HeartRateVariabilityRmssd.newBuilder()
                .setHeartRateVariabilityMillis(123.456)
                .build();
    }

    static Height generateHeight() {
        return Height.newBuilder().setHeight(123.45).build();
    }

    static Hydration generateHydration() {
        return Hydration.newBuilder().setVolume(12.345).build();
    }

    static IntermenstrualBleeding generateIntermenstrualBleeding() {
        return IntermenstrualBleeding.getDefaultInstance();
    }

    static LeanBodyMass generateLeanBodyMass() {
        return LeanBodyMass.newBuilder().setMass(123.45).build();
    }

    static MenstruationFlow generateMenstruationFlow() {
        return MenstruationFlow.newBuilder().setFlow(FLOW_HEAVY).build();
    }

    static MenstruationPeriod generateMenstruationPeriod() {
        return MenstruationPeriod.getDefaultInstance();
    }

    static MindfulnessSession generateMindfulnessSession() {
        return MindfulnessSession.newBuilder().build();
    }

    static Nutrition generateNutrition() {
        return Nutrition.newBuilder()
                .setMealType(MEAL_TYPE_DINNER)
                .setMealName("mealName")
                .setUnsaturatedFat(123.45)
                .setPotassium(123.45)
                .setThiamin(123.45)
                .setTransFat(123.45)
                .setManganese(123.45)
                .setEnergyFromFat(123.45)
                .setCaffeine(123.45)
                .setDietaryFiber(123.45)
                .setSelenium(123.45)
                .setVitaminB6(123.45)
                .setProtein(123.45)
                .setChloride(123.45)
                .setCholesterol(123.45)
                .setCopper(123.45)
                .setIodine(123.45)
                .setVitaminB12(123.45)
                .setZinc(123.45)
                .setRiboflavin(123.45)
                .setEnergy(123.45)
                .setMolybdenum(123.45)
                .setPhosphorus(123.45)
                .setChromium(123.45)
                .setTotalFat(123.45)
                .setCalcium(123.45)
                .setVitaminC(123.45)
                .setVitaminE(123.45)
                .setBiotin(123.45)
                .setVitaminD(123.45)
                .setNiacin(123.45)
                .setMagnesium(123.45)
                .setTotalCarbohydrate(123.45)
                .setVitaminK(123.45)
                .setPolyunsaturatedFat(123.45)
                .setSaturatedFat(123.45)
                .setSodium(123.45)
                .setFolate(123.45)
                .setMonounsaturatedFat(123.45)
                .setPantothenicAcid(123.45)
                .setIron(123.45)
                .setVitaminA(123.45)
                .setFolicAcid(123.45)
                .setSugar(123.45)
                .build();
    }

    static OvulationTest generateOvulationTest() {
        return OvulationTest.newBuilder().setResult(RESULT_HIGH).build();
    }

    static OxygenSaturation generateOxygenSaturation() {
        return OxygenSaturation.newBuilder().setPercentage(12.34).build();
    }

    static PlannedExerciseSession generatePlannedExerciseSession() {
        return PlannedExerciseSession.newBuilder()
                .setTitle("Powerlifting Meet")
                .setNotes("PR OR ER")
                .setExerciseType(EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING)
                .setHasExplicitTime(true)
                .addExerciseBlock(
                        PlannedExerciseBlock.newBuilder()
                                .setDescription("Squat")
                                .setRepetitions(3)
                                .addStep(generatePlannedExerciseStep()))
                .build();
    }

    static PlannedExerciseStep generatePlannedExerciseStep() {
        return PlannedExerciseStep.newBuilder()
                .setExerciseType(EXERCISE_SEGMENT_TYPE_SQUAT)
                .setExerciseCategory(EXERCISE_CATEGORY_ACTIVE)
                .setDescription("Opener")
                .setCompletionGoal(
                        ExerciseCompletionGoal.newBuilder()
                                .setUnspecifiedGoal(UnspecifiedGoal.getDefaultInstance()))
                .addPerformanceGoal(
                        ExercisePerformanceGoal.newBuilder()
                                .setWeightGoal(WeightGoal.newBuilder().setMass(180)))
                .build();
    }

    static Power generatePower() {
        return Power.newBuilder()
                .addSample(PowerSample.newBuilder().setPower(123.45).setEpochMillis(123456).build())
                .build();
    }

    static RespiratoryRate generateRespiratoryRate() {
        return RespiratoryRate.newBuilder().setRate(12.345).build();
    }

    static RestingHeartRate generateRestingHeartRate() {
        return RestingHeartRate.newBuilder().setBeatsPerMinute(123).build();
    }

    static SexualActivity generateSexualActivity() {
        return SexualActivity.newBuilder().setProtectionUsed(PROTECTION_USED_PROTECTED).build();
    }

    static SkinTemperature generateSkinTemperature() {
        return SkinTemperature.newBuilder()
                .setMeasurementLocation(MEASUREMENT_LOCATION_TOE)
                .setBaseline(123.45)
                .addSample(
                        SkinTemperatureDeltaSample.newBuilder()
                                .setTemperatureDeltaInCelsius(12.345)
                                .setEpochMillis(123456)
                                .build())
                .build();
    }

    static SleepSession generateSleepSession() {
        return SleepSession.newBuilder()
                .setTitle("Big nap")
                .setNotes("I was tired")
                .addStage(
                        SleepSession.SleepStage.newBuilder()
                                .setStartTime(123456)
                                .setEndTime(654321)
                                .setStageType(STAGE_TYPE_SLEEPING_DEEP)
                                .build())
                .build();
    }

    static Speed generateSpeed() {
        return Speed.newBuilder()
                .addSample(SpeedSample.newBuilder().setSpeed(12.345).setEpochMillis(123456).build())
                .build();
    }

    static Steps generateSteps() {
        return Steps.newBuilder().setCount(123).build();
    }

    static StepsCadence generateStepsCadence() {
        return StepsCadence.newBuilder()
                .addSample(
                        StepsCadenceSample.newBuilder()
                                .setRate(12.345)
                                .setEpochMillis(123456)
                                .build())
                .build();
    }

    static TotalCaloriesBurned generateTotalCaloriesBurned() {
        return TotalCaloriesBurned.newBuilder().setEnergy(123.45).build();
    }

    static Vo2Max generateVo2Max() {
        return Vo2Max.newBuilder().setVo2MillilitersPerMinuteKilogram(123.45).build();
    }

    static Weight generateWeight() {
        return Weight.newBuilder().setWeight(123.45).build();
    }

    static WheelchairPushes generateWheelchairPushes() {
        return WheelchairPushes.newBuilder().setCount(12345).build();
    }

    static IntervalRecord.Builder generateIntervalRecord() {
        return IntervalRecord.newBuilder()
                .setStartTime(12345)
                .setStartZoneOffset(3600)
                .setEndTime(54321)
                .setEndZoneOffset(3600);
    }

    static InstantRecord.Builder generateInstantRecord() {
        return InstantRecord.newBuilder().setTime(12345).setZoneOffset(3600);
    }

    static Record.Builder generateCoreRecord() {
        return Record.newBuilder()
                .setUuid(UUID.randomUUID().toString())
                .setPackageName(TEST_PACKAGE_NAME)
                .setLastModifiedTime(123456)
                .setClientRecordId("clientId")
                .setClientRecordVersion(3)
                .setManufacturer("manufacturer")
                .setModel("model")
                .setDeviceType(DEVICE_TYPE_PHONE)
                .setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED);
    }
}
