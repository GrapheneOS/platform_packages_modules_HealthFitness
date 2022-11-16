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
import android.healthconnect.datatypes.BoneMassRecord;
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord;
import android.healthconnect.datatypes.DistanceRecord;
import android.healthconnect.datatypes.ElevationGainedRecord;
import android.healthconnect.datatypes.ExerciseEventRecord;
import android.healthconnect.datatypes.ExerciseLapRecord;
import android.healthconnect.datatypes.FloorsClimbedRecord;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.HydrationRecord;
import android.healthconnect.datatypes.NutritionRecord;
import android.healthconnect.datatypes.PowerRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.SpeedRecord;
import android.healthconnect.datatypes.StepsCadenceRecord;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.SwimmingStrokesRecord;
import android.healthconnect.datatypes.TotalCaloriesBurnedRecord;
import android.healthconnect.datatypes.WheelchairPushesRecord;
import android.healthconnect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;
import android.healthconnect.internal.datatypes.BasalBodyTemperatureRecordInternal;
import android.healthconnect.internal.datatypes.BasalMetabolicRateRecordInternal;
import android.healthconnect.internal.datatypes.BloodGlucoseRecordInternal;
import android.healthconnect.internal.datatypes.BloodPressureRecordInternal;
import android.healthconnect.internal.datatypes.BodyFatRecordInternal;
import android.healthconnect.internal.datatypes.BodyTemperatureRecordInternal;
import android.healthconnect.internal.datatypes.BoneMassRecordInternal;
import android.healthconnect.internal.datatypes.CyclingPedalingCadenceRecordInternal;
import android.healthconnect.internal.datatypes.DistanceRecordInternal;
import android.healthconnect.internal.datatypes.ElevationGainedRecordInternal;
import android.healthconnect.internal.datatypes.ExerciseEventRecordInternal;
import android.healthconnect.internal.datatypes.ExerciseLapRecordInternal;
import android.healthconnect.internal.datatypes.FloorsClimbedRecordInternal;
import android.healthconnect.internal.datatypes.HeartRateRecordInternal;
import android.healthconnect.internal.datatypes.HydrationRecordInternal;
import android.healthconnect.internal.datatypes.NutritionRecordInternal;
import android.healthconnect.internal.datatypes.PowerRecordInternal;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.SpeedRecordInternal;
import android.healthconnect.internal.datatypes.StepsCadenceRecordInternal;
import android.healthconnect.internal.datatypes.StepsRecordInternal;
import android.healthconnect.internal.datatypes.SwimmingStrokesRecordInternal;
import android.healthconnect.internal.datatypes.TotalCaloriesBurnedRecordInternal;
import android.healthconnect.internal.datatypes.WheelchairPushesRecordInternal;
import android.util.ArrayMap;

import java.util.Map;

/** @hide */
public final class RecordMapper {
    private static final int NUM_ENTRIES = 21;
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
                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_LAP, ExerciseLapRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED,
                ElevationGainedRecordInternal.class);
        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_EVENT, ExerciseEventRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES,
                WheelchairPushesRecordInternal.class);

        mRecordIdToInternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_SWIMMING_STROKES,
                SwimmingStrokesRecordInternal.class);
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
                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_LAP, ExerciseLapRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED, ElevationGainedRecord.class);
        mRecordIdToExternalRecordClassMap.put(
                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_EVENT, ExerciseEventRecord.class);
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
                RecordTypeIdentifier.RECORD_TYPE_SWIMMING_STROKES, SwimmingStrokesRecord.class);

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
