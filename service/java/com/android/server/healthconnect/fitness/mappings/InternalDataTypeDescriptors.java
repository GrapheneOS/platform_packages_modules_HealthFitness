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

package com.android.server.healthconnect.fitness.mappings;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__ACTIVE_CALORIES_BURNED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__ACTIVITY_INTENSITY;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BASAL_BODY_TEMPERATURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BASAL_METABOLIC_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BLOOD_GLUCOSE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BLOOD_PRESSURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BODY_FAT;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BODY_TEMPERATURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BODY_WATER_MASS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BONE_MASS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__CERVICAL_MUCUS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__CYCLING_PEDALING_CADENCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_NOT_ASSIGNED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DISTANCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__ELEVATION_GAINED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__EXERCISE_SESSION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__FLOORS_CLIMBED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__HEART_RATE_VARIABILITY_RMSSD;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__HEIGHT;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__HYDRATION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__INTERMENSTRUAL_BLEEDING;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__LEAN_BODY_MASS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__MENSTRUATION_FLOW;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__MENSTRUATION_PERIOD;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__MINDFULNESS_SESSION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__NICOTINE_INTAKE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__NUTRITION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__OVULATION_TEST;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__OXYGEN_SATURATION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__PLANNED_EXERCISE_SESSION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__POWER;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__RESPIRATORY_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__RESTING_HEART_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__SEXUAL_ACTIVITY;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__SKIN_TEMPERATURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__SLEEP_SESSION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__STEPS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__STEPS_CADENCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__TOTAL_CALORIES_BURNED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__VO2_MAX;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__WEIGHT;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__WHEELCHAIR_PUSHES;
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
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_ACTIVE_CALORIES_BURNED;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_ACTIVITY_INTENSITY;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BASAL_BODY_TEMPERATURE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BASAL_METABOLIC_RATE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BLOOD_GLUCOSE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BLOOD_PRESSURE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BODY_FAT;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BODY_TEMPERATURE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BODY_WATER_MASS;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_BONE_MASS;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_CERVICAL_MUCUS;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_CYCLING_PEDALING_CADENCE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_DISTANCE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_ELEVATION_GAINED;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_EXERCISE_SESSION;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_FLOORS_CLIMBED;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_HEART_RATE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_HEART_RATE_VARIABILITY_RMSSD;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_HEIGHT;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_HYDRATION;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_INTERMENSTRUAL_BLEEDING;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_LEAN_BODY_MASS;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_FLOW;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_PERIOD;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_MINDFULNESS_SESSION;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_NICOTINE_INTAKE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_NUTRITION;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_OVULATION_TEST;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_OXYGEN_SATURATION;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_PLANNED_EXERCISE_SESSION;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_POWER;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_RESPIRATORY_RATE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_RESTING_HEART_RATE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_SEXUAL_ACTIVITY;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_SKIN_TEMPERATURE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_SLEEP_SESSION;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_SPEED;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_STEPS;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_STEPS_CADENCE;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_SYMPTOMS;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_TOTAL_CALORIES_BURNED;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_VO2_MAX;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_WEIGHT;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_WHEELCHAIR_PUSHES;

import android.annotation.Nullable;
import android.health.HealthFitnessStatsLog;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.fitness.recordhelpers.ActiveCaloriesBurnedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ActivityIntensityRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BasalBodyTemperatureRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BasalMetabolicRateRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BloodGlucoseRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BloodPressureRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BodyFatRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BodyTemperatureRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BodyWaterMassRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.BoneMassRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.CervicalMucusRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.CyclingPedalingCadenceRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.DistanceRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ElevationGainedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.FloorsClimbedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.HeartRateRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.HeartRateVariabilityRmssdHelper;
import com.android.server.healthconnect.fitness.recordhelpers.HeightRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.HydrationRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.IntermenstrualBleedingRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.LeanBodyMassRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.MenstruationFlowRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.MenstruationPeriodRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.MindfulnessSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.NicotineIntakeRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.OvulationTestRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.OxygenSaturationRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.PlannedExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.PowerRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RespiratoryRateRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RestingHeartRateRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SexualActivityRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SkinTemperatureRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SleepSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SpeedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.StepsCadenceRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.StepsRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SymptomRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.TotalCaloriesBurnedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.Vo2MaxRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.WeightRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.WheelchairPushesRecordHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** @hide */
@VisibleForTesting(visibility = PACKAGE)
public class InternalDataTypeDescriptors {

    // Using aliases to satisfy the Java style line length limit below, otherwise doesn't fit.
    private static final int LOGGING_ENUM_HEART_RATE_VARIABILITY_RMSSD =
            HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__HEART_RATE_VARIABILITY_RMSSD;

    private static final int LOGGING_ENUM_ACTIVITY_INTENSITY =
            HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__ACTIVITY_INTENSITY;

    private static final int LOGGING_ENUM_NICOTINE_INTAKE =
            HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__NICOTINE_INTAKE;

    // TODO(b/425404543): Remove once the correct logging enum for Symptoms is available.
    private static final int LOGGING_ENUM_NOT_ASSIGNED =
            HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_NOT_ASSIGNED;

    @VisibleForTesting(visibility = PACKAGE)
    static List<InternalDataTypeDescriptor> getAllInternalDataTypeDescriptors() {
        return listOfNonNull(
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_ACTIVE_CALORIES_BURNED)
                        .setRecordHelper(new ActiveCaloriesBurnedRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_ACTIVE_CALORIES_BURNED)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__ACTIVE_CALORIES_BURNED)
                        .setSupportGranularityLogging()
                        .build(),
                AconfigFlagHelper.isActivityIntensityEnabled()
                        ? InternalDataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_ACTIVITY_INTENSITY)
                                .setRecordHelper(new ActivityIntensityRecordHelper())
                                .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_ACTIVITY_INTENSITY)
                                .setLoggingEnum(LOGGING_ENUM_ACTIVITY_INTENSITY)
                                .build()
                        : null,
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BASAL_BODY_TEMPERATURE)
                        .setRecordHelper(new BasalBodyTemperatureRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BASAL_BODY_TEMPERATURE)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BASAL_BODY_TEMPERATURE)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BASAL_METABOLIC_RATE)
                        .setRecordHelper(new BasalMetabolicRateRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BASAL_METABOLIC_RATE)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BASAL_METABOLIC_RATE)
                        .setDerived()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BLOOD_GLUCOSE)
                        .setRecordHelper(new BloodGlucoseRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BLOOD_GLUCOSE)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BLOOD_GLUCOSE)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BLOOD_PRESSURE)
                        .setRecordHelper(new BloodPressureRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BLOOD_PRESSURE)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BLOOD_PRESSURE)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BODY_FAT)
                        .setRecordHelper(new BodyFatRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BODY_FAT)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BODY_FAT)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BODY_TEMPERATURE)
                        .setRecordHelper(new BodyTemperatureRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BODY_TEMPERATURE)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BODY_TEMPERATURE)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BODY_WATER_MASS)
                        .setRecordHelper(new BodyWaterMassRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BODY_WATER_MASS)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BODY_WATER_MASS)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_BONE_MASS)
                        .setRecordHelper(new BoneMassRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_BONE_MASS)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__BONE_MASS)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_CERVICAL_MUCUS)
                        .setRecordHelper(new CervicalMucusRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_CERVICAL_MUCUS)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__CERVICAL_MUCUS)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_CYCLING_PEDALING_CADENCE)
                        .setRecordHelper(new CyclingPedalingCadenceRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_CYCLING_PEDALING_CADENCE)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__CYCLING_PEDALING_CADENCE)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_DISTANCE)
                        .setRecordHelper(new DistanceRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_DISTANCE)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DISTANCE)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_ELEVATION_GAINED)
                        .setRecordHelper(new ElevationGainedRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_ELEVATION_GAINED)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__ELEVATION_GAINED)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_EXERCISE_SESSION)
                        .setRecordHelper(new ExerciseSessionRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_EXERCISE_SESSION)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__EXERCISE_SESSION)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_FLOORS_CLIMBED)
                        .setRecordHelper(new FloorsClimbedRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_FLOORS_CLIMBED)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__FLOORS_CLIMBED)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_HEART_RATE)
                        .setRecordHelper(new HeartRateRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_HEART_RATE)
                        .setLoggingEnum(
                                HealthFitnessStatsLog
                                        .HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__HEART_RATE)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD)
                        .setRecordHelper(new HeartRateVariabilityRmssdHelper())
                        .setRecordTypeIdForUuid(
                                RECORD_TYPE_ID_FOR_UUID_HEART_RATE_VARIABILITY_RMSSD)
                        .setLoggingEnum(LOGGING_ENUM_HEART_RATE_VARIABILITY_RMSSD)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_HEIGHT)
                        .setRecordHelper(new HeightRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_HEIGHT)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__HEIGHT)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_HYDRATION)
                        .setRecordHelper(new HydrationRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_HYDRATION)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__HYDRATION)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_INTERMENSTRUAL_BLEEDING)
                        .setRecordHelper(new IntermenstrualBleedingRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_INTERMENSTRUAL_BLEEDING)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__INTERMENSTRUAL_BLEEDING)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_LEAN_BODY_MASS)
                        .setRecordHelper(new LeanBodyMassRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_LEAN_BODY_MASS)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__LEAN_BODY_MASS)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_MENSTRUATION_FLOW)
                        .setRecordHelper(new MenstruationFlowRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_FLOW)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__MENSTRUATION_FLOW)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_MENSTRUATION_PERIOD)
                        .setRecordHelper(new MenstruationPeriodRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_PERIOD)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__MENSTRUATION_PERIOD)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_MINDFULNESS_SESSION)
                        .setRecordHelper(new MindfulnessSessionRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_MINDFULNESS_SESSION)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__MINDFULNESS_SESSION)
                        .build(),
                AconfigFlagHelper.isNicotineIntakeEnabled()
                        ? InternalDataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_NICOTINE_INTAKE)
                                .setRecordHelper(new NicotineIntakeRecordHelper())
                                .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_NICOTINE_INTAKE)
                                .setLoggingEnum(LOGGING_ENUM_NICOTINE_INTAKE)
                                .build()
                        : null,
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_NUTRITION)
                        .setRecordHelper(new NutritionRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_NUTRITION)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__NUTRITION)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_OVULATION_TEST)
                        .setRecordHelper(new OvulationTestRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_OVULATION_TEST)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__OVULATION_TEST)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_OXYGEN_SATURATION)
                        .setRecordHelper(new OxygenSaturationRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_OXYGEN_SATURATION)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__OXYGEN_SATURATION)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_PLANNED_EXERCISE_SESSION)
                        .setRecordHelper(new PlannedExerciseSessionRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_PLANNED_EXERCISE_SESSION)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__PLANNED_EXERCISE_SESSION)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_POWER)
                        .setRecordHelper(new PowerRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_POWER)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__POWER)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_RESPIRATORY_RATE)
                        .setRecordHelper(new RespiratoryRateRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_RESPIRATORY_RATE)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__RESPIRATORY_RATE)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_RESTING_HEART_RATE)
                        .setRecordHelper(new RestingHeartRateRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_RESTING_HEART_RATE)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__RESTING_HEART_RATE)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_SEXUAL_ACTIVITY)
                        .setRecordHelper(new SexualActivityRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_SEXUAL_ACTIVITY)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__SEXUAL_ACTIVITY)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_SKIN_TEMPERATURE)
                        .setRecordHelper(new SkinTemperatureRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_SKIN_TEMPERATURE)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__SKIN_TEMPERATURE)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_SLEEP_SESSION)
                        .setRecordHelper(new SleepSessionRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_SLEEP_SESSION)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__SLEEP_SESSION)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_SPEED)
                        .setRecordHelper(new SpeedRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_SPEED)
                        .setLoggingEnum(
                                HealthFitnessStatsLog
                                        .HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__SPEED)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_STEPS)
                        .setRecordHelper(new StepsRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_STEPS)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__STEPS)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_STEPS_CADENCE)
                        .setRecordHelper(new StepsCadenceRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_STEPS_CADENCE)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__STEPS_CADENCE)
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_TOTAL_CALORIES_BURNED)
                        .setRecordHelper(new TotalCaloriesBurnedRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_TOTAL_CALORIES_BURNED)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__TOTAL_CALORIES_BURNED)
                        .setDerived()
                        .setSupportGranularityLogging()
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_VO2_MAX)
                        .setRecordHelper(new Vo2MaxRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_VO2_MAX)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__VO2_MAX)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_WEIGHT)
                        .setRecordHelper(new WeightRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_WEIGHT)
                        .setLoggingEnum(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__WEIGHT)
                        .build(),
                InternalDataTypeDescriptor.builder()
                        .setRecordTypeIdentifier(RECORD_TYPE_WHEELCHAIR_PUSHES)
                        .setRecordHelper(new WheelchairPushesRecordHelper())
                        .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_WHEELCHAIR_PUSHES)
                        .setLoggingEnum(
                                HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__WHEELCHAIR_PUSHES)
                        .build(),
                AconfigFlagHelper.isSymptomsEnabled()
                        ? InternalDataTypeDescriptor.builder()
                                .setRecordTypeIdentifier(RECORD_TYPE_SYMPTOM)
                                .setRecordHelper(new SymptomRecordHelper())
                                .setRecordTypeIdForUuid(RECORD_TYPE_ID_FOR_UUID_SYMPTOMS)
                                // TODO(b/425404543): Use the correct logging enum for Symptoms once
                                // it is available.
                                .setLoggingEnum(LOGGING_ENUM_NOT_ASSIGNED)
                                .build()
                        : null);
    }

    @SafeVarargs
    private static <T> List<T> listOfNonNull(@Nullable T... values) {
        return Arrays.stream(values).filter(Objects::nonNull).toList();
    }
}
