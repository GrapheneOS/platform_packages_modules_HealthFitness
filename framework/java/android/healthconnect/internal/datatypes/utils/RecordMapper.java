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

package android.healthconnect.internal.datatypes.utils;

import android.annotation.NonNull;
import android.healthconnect.datatypes.ActiveCaloriesBurnedRecord;
import android.healthconnect.datatypes.BasalBodyTemperatureRecord;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.BloodGlucoseRecord;
import android.healthconnect.datatypes.BloodPressureRecord;
import android.healthconnect.datatypes.BodyFatRecord;
import android.healthconnect.datatypes.BodyTemperatureRecord;
import android.healthconnect.datatypes.BodyWaterMassRecord;
import android.healthconnect.datatypes.BoneMassRecord;
import android.healthconnect.datatypes.CervicalMucusRecord;
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord;
import android.healthconnect.datatypes.DistanceRecord;
import android.healthconnect.datatypes.ElevationGainedRecord;
import android.healthconnect.datatypes.FloorsClimbedRecord;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.HeartRateVariabilityRmssdRecord;
import android.healthconnect.datatypes.HeightRecord;
import android.healthconnect.datatypes.HydrationRecord;
import android.healthconnect.datatypes.IntermenstrualBleedingRecord;
import android.healthconnect.datatypes.LeanBodyMassRecord;
import android.healthconnect.datatypes.MenstruationFlowRecord;
import android.healthconnect.datatypes.MenstruationPeriodRecord;
import android.healthconnect.datatypes.NutritionRecord;
import android.healthconnect.datatypes.OvulationTestRecord;
import android.healthconnect.datatypes.OxygenSaturationRecord;
import android.healthconnect.datatypes.PowerRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.RespiratoryRateRecord;
import android.healthconnect.datatypes.RestingHeartRateRecord;
import android.healthconnect.datatypes.SexualActivityRecord;
import android.healthconnect.datatypes.SpeedRecord;
import android.healthconnect.datatypes.StepsCadenceRecord;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.TotalCaloriesBurnedRecord;
import android.healthconnect.datatypes.Vo2MaxRecord;
import android.healthconnect.datatypes.WeightRecord;
import android.healthconnect.datatypes.WheelchairPushesRecord;
import android.healthconnect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;
import android.healthconnect.internal.datatypes.BasalBodyTemperatureRecordInternal;
import android.healthconnect.internal.datatypes.BasalMetabolicRateRecordInternal;
import android.healthconnect.internal.datatypes.BloodGlucoseRecordInternal;
import android.healthconnect.internal.datatypes.BloodPressureRecordInternal;
import android.healthconnect.internal.datatypes.BodyFatRecordInternal;
import android.healthconnect.internal.datatypes.BodyTemperatureRecordInternal;
import android.healthconnect.internal.datatypes.BodyWaterMassRecordInternal;
import android.healthconnect.internal.datatypes.BoneMassRecordInternal;
import android.healthconnect.internal.datatypes.CervicalMucusRecordInternal;
import android.healthconnect.internal.datatypes.CyclingPedalingCadenceRecordInternal;
import android.healthconnect.internal.datatypes.DistanceRecordInternal;
import android.healthconnect.internal.datatypes.ElevationGainedRecordInternal;
import android.healthconnect.internal.datatypes.FloorsClimbedRecordInternal;
import android.healthconnect.internal.datatypes.HeartRateRecordInternal;
import android.healthconnect.internal.datatypes.HeartRateVariabilityRmssdRecordInternal;
import android.healthconnect.internal.datatypes.HeightRecordInternal;
import android.healthconnect.internal.datatypes.HydrationRecordInternal;
import android.healthconnect.internal.datatypes.IntermenstrualBleedingRecordInternal;
import android.healthconnect.internal.datatypes.LeanBodyMassRecordInternal;
import android.healthconnect.internal.datatypes.MenstruationFlowRecordInternal;
import android.healthconnect.internal.datatypes.MenstruationPeriodRecordInternal;
import android.healthconnect.internal.datatypes.NutritionRecordInternal;
import android.healthconnect.internal.datatypes.OvulationTestRecordInternal;
import android.healthconnect.internal.datatypes.OxygenSaturationRecordInternal;
import android.healthconnect.internal.datatypes.PowerRecordInternal;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.RespiratoryRateRecordInternal;
import android.healthconnect.internal.datatypes.RestingHeartRateRecordInternal;
import android.healthconnect.internal.datatypes.SexualActivityRecordInternal;
import android.healthconnect.internal.datatypes.SpeedRecordInternal;
import android.healthconnect.internal.datatypes.StepsCadenceRecordInternal;
import android.healthconnect.internal.datatypes.StepsRecordInternal;
import android.healthconnect.internal.datatypes.TotalCaloriesBurnedRecordInternal;
import android.healthconnect.internal.datatypes.Vo2MaxRecordInternal;
import android.healthconnect.internal.datatypes.WeightRecordInternal;
import android.healthconnect.internal.datatypes.WheelchairPushesRecordInternal;
import android.util.ArrayMap;

import java.util.Map;

/** @hide */
public final class RecordMapper {
    private static final int NUM_ENTRIES = 35;
    private static RecordMapper sRecordMapper;
    private final Map<Integer, Class<? extends RecordInternal<?>>>
            mRecordIdToInternalRecordClassMap;
    private final Map<Integer, Class<? extends Record>> mRecordIdToExternalRecordClassMap;
    private final Map<Class<? extends Record>, Integer> mExternalRecordClassToRecordIdMap;

    private RecordMapper() {
        mRecordIdToInternalRecordClassMap = new ArrayMap<>(NUM_ENTRIES);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS, StepsRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE, HeartRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED, FloorsClimbedRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HYDRATION, HydrationRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                ActiveCaloriesBurnedRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED,
                ElevationGainedRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES,
                WheelchairPushesRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED,
                TotalCaloriesBurnedRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_DISTANCE, DistanceRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE,
                BasalMetabolicRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                CyclingPedalingCadenceRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_POWER, PowerRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_NUTRITION, NutritionRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SPEED, SpeedRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE, StepsCadenceRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS,
                BodyWaterMassRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD,
                HeartRateVariabilityRmssdRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD,
                MenstruationPeriodRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING,
                IntermenstrualBleedingRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_VO2_MAX, Vo2MaxRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY,
                SexualActivityRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE,
                RestingHeartRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WEIGHT, WeightRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION,
                OxygenSaturationRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE,
                RespiratoryRateRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE,
                BodyTemperatureRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BONE_MASS, BoneMassRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE, BloodPressureRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_FAT, BodyFatRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE, BloodGlucoseRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE,
                BasalBodyTemperatureRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST, OvulationTestRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW,
                MenstruationFlowRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS, CervicalMucusRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEIGHT, HeightRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS, LeanBodyMassRecordInternal.class);

        mRecordIdToExternalRecordClassMap = new ArrayMap<>(NUM_ENTRIES);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS, StepsRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE, HeartRateRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HYDRATION, HydrationRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                ActiveCaloriesBurnedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED, ElevationGainedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_DISTANCE, DistanceRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_NUTRITION, NutritionRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED, FloorsClimbedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES, WheelchairPushesRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED,
                TotalCaloriesBurnedRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE,
                BasalMetabolicRateRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                CyclingPedalingCadenceRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_POWER, PowerRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SPEED, SpeedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE, StepsCadenceRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE, BodyTemperatureRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BONE_MASS, BoneMassRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE, BloodPressureRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_FAT, BodyFatRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE, BloodGlucoseRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE,
                BasalBodyTemperatureRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS, CervicalMucusRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS, LeanBodyMassRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW, MenstruationFlowRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEIGHT, HeightRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST, OvulationTestRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_VO2_MAX, Vo2MaxRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY, SexualActivityRecord.class);

        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE, RespiratoryRateRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION, OxygenSaturationRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE, RestingHeartRateRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WEIGHT, WeightRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS, BodyWaterMassRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD,
                HeartRateVariabilityRmssdRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD,
                MenstruationPeriodRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING,
                IntermenstrualBleedingRecord.class);

        mExternalRecordClassToRecordIdMap =
                new ArrayMap<>(mRecordIdToExternalRecordClassMap.size());
        mRecordIdToExternalRecordClassMap.forEach(
                (id, recordClass) -> mExternalRecordClassToRecordIdMap.put(recordClass, id));
    }

    @NonNull
    public static RecordMapper getInstance() {
        if (sRecordMapper == null) {
            sRecordMapper = new RecordMapper();
        }

        return sRecordMapper;
    }

    @NonNull
    public Map<Integer, Class<? extends RecordInternal<?>>> getRecordIdToInternalRecordClassMap() {
        return mRecordIdToInternalRecordClassMap;
    }

    @NonNull
    public Map<Integer, Class<? extends Record>> getRecordIdToExternalRecordClassMap() {
        return mRecordIdToExternalRecordClassMap;
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordType(Class<? extends Record> recordClass) {
        return mExternalRecordClassToRecordIdMap.get(recordClass);
    }
}
