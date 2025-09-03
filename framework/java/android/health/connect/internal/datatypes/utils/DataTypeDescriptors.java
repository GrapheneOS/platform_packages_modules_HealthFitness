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

package android.health.connect.internal.datatypes.utils;

import static android.health.connect.HealthPermissions.READ_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.READ_ACTIVITY_INTENSITY;
import static android.health.connect.HealthPermissions.READ_BASAL_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissions.READ_BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissions.READ_BLOOD_PRESSURE;
import static android.health.connect.HealthPermissions.READ_BODY_FAT;
import static android.health.connect.HealthPermissions.READ_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_BODY_WATER_MASS;
import static android.health.connect.HealthPermissions.READ_BONE_MASS;
import static android.health.connect.HealthPermissions.READ_CERVICAL_MUCUS;
import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_ELEVATION_GAINED;
import static android.health.connect.HealthPermissions.READ_EXERCISE;
import static android.health.connect.HealthPermissions.READ_FLOORS_CLIMBED;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE_VARIABILITY;
import static android.health.connect.HealthPermissions.READ_HEIGHT;
import static android.health.connect.HealthPermissions.READ_HYDRATION;
import static android.health.connect.HealthPermissions.READ_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.HealthPermissions.READ_LEAN_BODY_MASS;
import static android.health.connect.HealthPermissions.READ_MENSTRUATION;
import static android.health.connect.HealthPermissions.READ_MINDFULNESS;
import static android.health.connect.HealthPermissions.READ_NICOTINE_INTAKE;
import static android.health.connect.HealthPermissions.READ_NUTRITION;
import static android.health.connect.HealthPermissions.READ_OVULATION_TEST;
import static android.health.connect.HealthPermissions.READ_OXYGEN_SATURATION;
import static android.health.connect.HealthPermissions.READ_PLANNED_EXERCISE;
import static android.health.connect.HealthPermissions.READ_POWER;
import static android.health.connect.HealthPermissions.READ_RESPIRATORY_RATE;
import static android.health.connect.HealthPermissions.READ_RESTING_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_SEXUAL_ACTIVITY;
import static android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_SLEEP;
import static android.health.connect.HealthPermissions.READ_SPEED;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_COUGH;
import static android.health.connect.HealthPermissions.READ_TOTAL_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.READ_VO2_MAX;
import static android.health.connect.HealthPermissions.READ_WEIGHT;
import static android.health.connect.HealthPermissions.READ_WHEELCHAIR_PUSHES;
import static android.health.connect.HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.WRITE_ACTIVITY_INTENSITY;
import static android.health.connect.HealthPermissions.WRITE_BASAL_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissions.WRITE_BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissions.WRITE_BLOOD_PRESSURE;
import static android.health.connect.HealthPermissions.WRITE_BODY_FAT;
import static android.health.connect.HealthPermissions.WRITE_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_BODY_WATER_MASS;
import static android.health.connect.HealthPermissions.WRITE_BONE_MASS;
import static android.health.connect.HealthPermissions.WRITE_CERVICAL_MUCUS;
import static android.health.connect.HealthPermissions.WRITE_DISTANCE;
import static android.health.connect.HealthPermissions.WRITE_ELEVATION_GAINED;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_FLOORS_CLIMBED;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE_VARIABILITY;
import static android.health.connect.HealthPermissions.WRITE_HEIGHT;
import static android.health.connect.HealthPermissions.WRITE_HYDRATION;
import static android.health.connect.HealthPermissions.WRITE_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.HealthPermissions.WRITE_LEAN_BODY_MASS;
import static android.health.connect.HealthPermissions.WRITE_MENSTRUATION;
import static android.health.connect.HealthPermissions.WRITE_MINDFULNESS;
import static android.health.connect.HealthPermissions.WRITE_NICOTINE_INTAKE;
import static android.health.connect.HealthPermissions.WRITE_NUTRITION;
import static android.health.connect.HealthPermissions.WRITE_OVULATION_TEST;
import static android.health.connect.HealthPermissions.WRITE_OXYGEN_SATURATION;
import static android.health.connect.HealthPermissions.WRITE_PLANNED_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_POWER;
import static android.health.connect.HealthPermissions.WRITE_RESPIRATORY_RATE;
import static android.health.connect.HealthPermissions.WRITE_RESTING_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_SEXUAL_ACTIVITY;
import static android.health.connect.HealthPermissions.WRITE_SKIN_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_SPEED;
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_COUGH;
import static android.health.connect.HealthPermissions.WRITE_TOTAL_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.WRITE_VO2_MAX;
import static android.health.connect.HealthPermissions.WRITE_WEIGHT;
import static android.health.connect.HealthPermissions.WRITE_WHEELCHAIR_PUSHES;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_FAT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BONE_MASS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEIGHT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HYDRATION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NICOTINE_INTAKE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_POWER;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SYMPTOM;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_VO2_MAX;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WEIGHT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES;

import static com.android.internal.annotations.VisibleForTesting.Visibility.PACKAGE;

import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissionCategory;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.BasalBodyTemperatureRecord;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.BodyFatRecord;
import android.health.connect.datatypes.BodyTemperatureRecord;
import android.health.connect.datatypes.BodyWaterMassRecord;
import android.health.connect.datatypes.BoneMassRecord;
import android.health.connect.datatypes.CervicalMucusRecord;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ElevationGainedRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.FloorsClimbedRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.IntermenstrualBleedingRecord;
import android.health.connect.datatypes.LeanBodyMassRecord;
import android.health.connect.datatypes.MenstruationFlowRecord;
import android.health.connect.datatypes.MenstruationPeriodRecord;
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.health.connect.datatypes.NicotineIntakeRecord;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.OvulationTestRecord;
import android.health.connect.datatypes.OxygenSaturationRecord;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.RespiratoryRateRecord;
import android.health.connect.datatypes.RestingHeartRateRecord;
import android.health.connect.datatypes.SexualActivityRecord;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.SymptomRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.Vo2MaxRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.WheelchairPushesRecord;
import android.health.connect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.ActivityIntensityRecordInternal;
import android.health.connect.internal.datatypes.BasalBodyTemperatureRecordInternal;
import android.health.connect.internal.datatypes.BasalMetabolicRateRecordInternal;
import android.health.connect.internal.datatypes.BloodGlucoseRecordInternal;
import android.health.connect.internal.datatypes.BloodPressureRecordInternal;
import android.health.connect.internal.datatypes.BodyFatRecordInternal;
import android.health.connect.internal.datatypes.BodyTemperatureRecordInternal;
import android.health.connect.internal.datatypes.BodyWaterMassRecordInternal;
import android.health.connect.internal.datatypes.BoneMassRecordInternal;
import android.health.connect.internal.datatypes.CervicalMucusRecordInternal;
import android.health.connect.internal.datatypes.CyclingPedalingCadenceRecordInternal;
import android.health.connect.internal.datatypes.DistanceRecordInternal;
import android.health.connect.internal.datatypes.ElevationGainedRecordInternal;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.FloorsClimbedRecordInternal;
import android.health.connect.internal.datatypes.HeartRateRecordInternal;
import android.health.connect.internal.datatypes.HeartRateVariabilityRmssdRecordInternal;
import android.health.connect.internal.datatypes.HeightRecordInternal;
import android.health.connect.internal.datatypes.HydrationRecordInternal;
import android.health.connect.internal.datatypes.IntermenstrualBleedingRecordInternal;
import android.health.connect.internal.datatypes.LeanBodyMassRecordInternal;
import android.health.connect.internal.datatypes.MenstruationFlowRecordInternal;
import android.health.connect.internal.datatypes.MenstruationPeriodRecordInternal;
import android.health.connect.internal.datatypes.MindfulnessSessionRecordInternal;
import android.health.connect.internal.datatypes.NicotineIntakeRecordInternal;
import android.health.connect.internal.datatypes.NutritionRecordInternal;
import android.health.connect.internal.datatypes.OvulationTestRecordInternal;
import android.health.connect.internal.datatypes.OxygenSaturationRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.PowerRecordInternal;
import android.health.connect.internal.datatypes.RespiratoryRateRecordInternal;
import android.health.connect.internal.datatypes.RestingHeartRateRecordInternal;
import android.health.connect.internal.datatypes.SexualActivityRecordInternal;
import android.health.connect.internal.datatypes.SkinTemperatureRecordInternal;
import android.health.connect.internal.datatypes.SleepSessionRecordInternal;
import android.health.connect.internal.datatypes.SpeedRecordInternal;
import android.health.connect.internal.datatypes.StepsCadenceRecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.health.connect.internal.datatypes.SymptomRecordInternal;
import android.health.connect.internal.datatypes.TotalCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.Vo2MaxRecordInternal;
import android.health.connect.internal.datatypes.WeightRecordInternal;
import android.health.connect.internal.datatypes.WheelchairPushesRecordInternal;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** @hide */
@VisibleForTesting(visibility = PACKAGE)
public class DataTypeDescriptors {

    /** Returns descriptors for all supported data types. */
    @VisibleForTesting(visibility = PACKAGE)
    public static List<DataTypeDescriptor> getAllDataTypeDescriptors() {
        return Stream.of(
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_ACTIVE_CALORIES_BURNED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(ActiveCaloriesBurnedRecord.class)
                                .setRecordInternalClass(ActiveCaloriesBurnedRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.ACTIVE_CALORIES_BURNED,
                                        READ_ACTIVE_CALORIES_BURNED,
                                        WRITE_ACTIVE_CALORIES_BURNED)
                                .build(),
                        // Redundantly explicitly checking the flag to satisfy the linter.
                        Flags.activityIntensity() && AconfigFlagHelper.isActivityIntensityEnabled()
                                ? DataTypeDescriptor.builder()
                                        .setRecordTypeIdentifier(RECORD_TYPE_ACTIVITY_INTENSITY)
                                        .setDataCategory(HealthDataCategory.ACTIVITY)
                                        .setRecordClass(ActivityIntensityRecord.class)
                                        .setRecordInternalClass(
                                                ActivityIntensityRecordInternal.class)
                                        .addPermissionCategory(
                                                HealthPermissionCategory.ACTIVITY_INTENSITY,
                                                READ_ACTIVITY_INTENSITY,
                                                WRITE_ACTIVITY_INTENSITY)
                                        .build()
                                : null,
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BASAL_BODY_TEMPERATURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(BasalBodyTemperatureRecord.class)
                                .setRecordInternalClass(BasalBodyTemperatureRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BASAL_BODY_TEMPERATURE,
                                        READ_BASAL_BODY_TEMPERATURE,
                                        WRITE_BASAL_BODY_TEMPERATURE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BASAL_METABOLIC_RATE)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setRecordClass(BasalMetabolicRateRecord.class)
                                .setRecordInternalClass(BasalMetabolicRateRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BASAL_METABOLIC_RATE,
                                        READ_BASAL_METABOLIC_RATE,
                                        WRITE_BASAL_METABOLIC_RATE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BLOOD_GLUCOSE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(BloodGlucoseRecord.class)
                                .setRecordInternalClass(BloodGlucoseRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BLOOD_GLUCOSE,
                                        READ_BLOOD_GLUCOSE,
                                        WRITE_BLOOD_GLUCOSE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BLOOD_PRESSURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(BloodPressureRecord.class)
                                .setRecordInternalClass(BloodPressureRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BLOOD_PRESSURE,
                                        READ_BLOOD_PRESSURE,
                                        WRITE_BLOOD_PRESSURE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BODY_FAT)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setRecordClass(BodyFatRecord.class)
                                .setRecordInternalClass(BodyFatRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BODY_FAT,
                                        READ_BODY_FAT,
                                        WRITE_BODY_FAT)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BODY_TEMPERATURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(BodyTemperatureRecord.class)
                                .setRecordInternalClass(BodyTemperatureRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BODY_TEMPERATURE,
                                        READ_BODY_TEMPERATURE,
                                        WRITE_BODY_TEMPERATURE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BODY_WATER_MASS)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setRecordClass(BodyWaterMassRecord.class)
                                .setRecordInternalClass(BodyWaterMassRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BODY_WATER_MASS,
                                        READ_BODY_WATER_MASS,
                                        WRITE_BODY_WATER_MASS)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_BONE_MASS)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setRecordClass(BoneMassRecord.class)
                                .setRecordInternalClass(BoneMassRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.BONE_MASS,
                                        READ_BONE_MASS,
                                        WRITE_BONE_MASS)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_CERVICAL_MUCUS)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setRecordClass(CervicalMucusRecord.class)
                                .setRecordInternalClass(CervicalMucusRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.CERVICAL_MUCUS,
                                        READ_CERVICAL_MUCUS,
                                        WRITE_CERVICAL_MUCUS)
                                .build(),
                        Flags.smoking() && AconfigFlagHelper.isNicotineIntakeEnabled()
                                ? DataTypeDescriptor.builder()
                                        .setRecordTypeIdentifier(RECORD_TYPE_NICOTINE_INTAKE)
                                        .setDataCategory(HealthDataCategory.WELLNESS)
                                        .setRecordClass(NicotineIntakeRecord.class)
                                        .setRecordInternalClass(NicotineIntakeRecordInternal.class)
                                        .addPermissionCategory(
                                                HealthPermissionCategory.NICOTINE_INTAKE,
                                                READ_NICOTINE_INTAKE,
                                                WRITE_NICOTINE_INTAKE)
                                        .build()
                                : null,
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_CYCLING_PEDALING_CADENCE)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(CyclingPedalingCadenceRecord.class)
                                .setRecordInternalClass(CyclingPedalingCadenceRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.EXERCISE,
                                        READ_EXERCISE,
                                        WRITE_EXERCISE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_DISTANCE)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(DistanceRecord.class)
                                .setRecordInternalClass(DistanceRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.DISTANCE,
                                        READ_DISTANCE,
                                        WRITE_DISTANCE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_ELEVATION_GAINED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(ElevationGainedRecord.class)
                                .setRecordInternalClass(ElevationGainedRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.ELEVATION_GAINED,
                                        READ_ELEVATION_GAINED,
                                        WRITE_ELEVATION_GAINED)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_EXERCISE_SESSION)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(ExerciseSessionRecord.class)
                                .setRecordInternalClass(ExerciseSessionRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.EXERCISE,
                                        READ_EXERCISE,
                                        WRITE_EXERCISE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_FLOORS_CLIMBED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(FloorsClimbedRecord.class)
                                .setRecordInternalClass(FloorsClimbedRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.FLOORS_CLIMBED,
                                        READ_FLOORS_CLIMBED,
                                        WRITE_FLOORS_CLIMBED)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HEART_RATE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(HeartRateRecord.class)
                                .setRecordInternalClass(HeartRateRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.HEART_RATE,
                                        READ_HEART_RATE,
                                        WRITE_HEART_RATE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(HeartRateVariabilityRmssdRecord.class)
                                .setRecordInternalClass(
                                        HeartRateVariabilityRmssdRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.HEART_RATE_VARIABILITY,
                                        READ_HEART_RATE_VARIABILITY,
                                        WRITE_HEART_RATE_VARIABILITY)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HEIGHT)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setRecordClass(HeightRecord.class)
                                .setRecordInternalClass(HeightRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.HEIGHT, READ_HEIGHT, WRITE_HEIGHT)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_HYDRATION)
                                .setDataCategory(HealthDataCategory.NUTRITION)
                                .setRecordClass(HydrationRecord.class)
                                .setRecordInternalClass(HydrationRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.HYDRATION,
                                        READ_HYDRATION,
                                        WRITE_HYDRATION)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_INTERMENSTRUAL_BLEEDING)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setRecordClass(IntermenstrualBleedingRecord.class)
                                .setRecordInternalClass(IntermenstrualBleedingRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.INTERMENSTRUAL_BLEEDING,
                                        READ_INTERMENSTRUAL_BLEEDING,
                                        WRITE_INTERMENSTRUAL_BLEEDING)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_LEAN_BODY_MASS)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setRecordClass(LeanBodyMassRecord.class)
                                .setRecordInternalClass(LeanBodyMassRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.LEAN_BODY_MASS,
                                        READ_LEAN_BODY_MASS,
                                        WRITE_LEAN_BODY_MASS)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_MENSTRUATION_FLOW)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setRecordClass(MenstruationFlowRecord.class)
                                .setRecordInternalClass(MenstruationFlowRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.MENSTRUATION,
                                        READ_MENSTRUATION,
                                        WRITE_MENSTRUATION)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_MENSTRUATION_PERIOD)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setRecordClass(MenstruationPeriodRecord.class)
                                .setRecordInternalClass(MenstruationPeriodRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.MENSTRUATION,
                                        READ_MENSTRUATION,
                                        WRITE_MENSTRUATION)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_MINDFULNESS_SESSION)
                                .setDataCategory(HealthDataCategory.WELLNESS)
                                .setRecordClass(MindfulnessSessionRecord.class)
                                .setRecordInternalClass(MindfulnessSessionRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.MINDFULNESS,
                                        READ_MINDFULNESS,
                                        WRITE_MINDFULNESS)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_NUTRITION)
                                .setDataCategory(HealthDataCategory.NUTRITION)
                                .setRecordClass(NutritionRecord.class)
                                .setRecordInternalClass(NutritionRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.NUTRITION,
                                        READ_NUTRITION,
                                        WRITE_NUTRITION)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_OVULATION_TEST)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setRecordClass(OvulationTestRecord.class)
                                .setRecordInternalClass(OvulationTestRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.OVULATION_TEST,
                                        READ_OVULATION_TEST,
                                        WRITE_OVULATION_TEST)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_OXYGEN_SATURATION)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(OxygenSaturationRecord.class)
                                .setRecordInternalClass(OxygenSaturationRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.OXYGEN_SATURATION,
                                        READ_OXYGEN_SATURATION,
                                        WRITE_OXYGEN_SATURATION)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_PLANNED_EXERCISE_SESSION)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(PlannedExerciseSessionRecord.class)
                                .setRecordInternalClass(PlannedExerciseSessionRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.PLANNED_EXERCISE,
                                        READ_PLANNED_EXERCISE,
                                        WRITE_PLANNED_EXERCISE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_POWER)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(PowerRecord.class)
                                .setRecordInternalClass(PowerRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.POWER, READ_POWER, WRITE_POWER)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_RESPIRATORY_RATE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(RespiratoryRateRecord.class)
                                .setRecordInternalClass(RespiratoryRateRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.RESPIRATORY_RATE,
                                        READ_RESPIRATORY_RATE,
                                        WRITE_RESPIRATORY_RATE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_RESTING_HEART_RATE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(RestingHeartRateRecord.class)
                                .setRecordInternalClass(RestingHeartRateRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.RESTING_HEART_RATE,
                                        READ_RESTING_HEART_RATE,
                                        WRITE_RESTING_HEART_RATE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SEXUAL_ACTIVITY)
                                .setDataCategory(HealthDataCategory.CYCLE_TRACKING)
                                .setRecordClass(SexualActivityRecord.class)
                                .setRecordInternalClass(SexualActivityRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.SEXUAL_ACTIVITY,
                                        READ_SEXUAL_ACTIVITY,
                                        WRITE_SEXUAL_ACTIVITY)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SKIN_TEMPERATURE)
                                .setDataCategory(HealthDataCategory.VITALS)
                                .setRecordClass(SkinTemperatureRecord.class)
                                .setRecordInternalClass(SkinTemperatureRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.SKIN_TEMPERATURE,
                                        READ_SKIN_TEMPERATURE,
                                        WRITE_SKIN_TEMPERATURE)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SLEEP_SESSION)
                                .setDataCategory(HealthDataCategory.SLEEP)
                                .setRecordClass(SleepSessionRecord.class)
                                .setRecordInternalClass(SleepSessionRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.SLEEP, READ_SLEEP, WRITE_SLEEP)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SPEED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(SpeedRecord.class)
                                .setRecordInternalClass(SpeedRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.SPEED, READ_SPEED, WRITE_SPEED)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_STEPS)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(StepsRecord.class)
                                .setRecordInternalClass(StepsRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.STEPS, READ_STEPS, WRITE_STEPS)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_STEPS_CADENCE)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(StepsCadenceRecord.class)
                                .setRecordInternalClass(StepsCadenceRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.STEPS, READ_STEPS, WRITE_STEPS)
                                .build(),
                        Flags.symptoms() && AconfigFlagHelper.isSymptomsEnabled()
                                ? DataTypeDescriptor.builder()
                                        .setRecordTypeIdentifier(RECORD_TYPE_SYMPTOM)
                                        .setDataCategory(HealthDataCategory.SYMPTOMS)
                                        .setRecordClass(SymptomRecord.class)
                                        .setRecordInternalClass(SymptomRecordInternal.class)
                                        .addPermissionCategory(
                                                HealthPermissionCategory.SYMPTOM_COUGH,
                                                READ_SYMPTOM_COUGH,
                                                WRITE_SYMPTOM_COUGH)
                                        .build()
                                : null,
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_TOTAL_CALORIES_BURNED)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(TotalCaloriesBurnedRecord.class)
                                .setRecordInternalClass(TotalCaloriesBurnedRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.TOTAL_CALORIES_BURNED,
                                        READ_TOTAL_CALORIES_BURNED,
                                        WRITE_TOTAL_CALORIES_BURNED)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_VO2_MAX)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(Vo2MaxRecord.class)
                                .setRecordInternalClass(Vo2MaxRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.VO2_MAX,
                                        READ_VO2_MAX,
                                        WRITE_VO2_MAX)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_WEIGHT)
                                .setDataCategory(HealthDataCategory.BODY_MEASUREMENTS)
                                .setRecordClass(WeightRecord.class)
                                .setRecordInternalClass(WeightRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.WEIGHT, READ_WEIGHT, WRITE_WEIGHT)
                                .build(),
                        DataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_WHEELCHAIR_PUSHES)
                                .setDataCategory(HealthDataCategory.ACTIVITY)
                                .setRecordClass(WheelchairPushesRecord.class)
                                .setRecordInternalClass(WheelchairPushesRecordInternal.class)
                                .addPermissionCategory(
                                        HealthPermissionCategory.WHEELCHAIR_PUSHES,
                                        READ_WHEELCHAIR_PUSHES,
                                        WRITE_WHEELCHAIR_PUSHES)
                                .build())
                .filter(Objects::nonNull)
                .toList();
    }
}
