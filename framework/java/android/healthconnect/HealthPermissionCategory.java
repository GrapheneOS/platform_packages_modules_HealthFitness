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

package android.healthconnect;

import android.annotation.IntDef;
import android.annotation.SystemApi;
import android.healthconnect.datatypes.Record;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the permission category of a {@link Record}. A record can only belong to one and only
 * one {@link HealthPermissionCategory}
 *
 * @hide
 */
@SystemApi
public class HealthPermissionCategory {
    public static final int UNKNOWN = 0;
    // ACTIVITY
    /**
     * Permission category for {@link android.healthconnect.datatypes.ActiveCaloriesBurnedRecord}
     */
    public static final int ACTIVE_CALORIES_BURNED = 1;
    /** Permission category for {@link android.healthconnect.datatypes.DistanceRecord} */
    public static final int DISTANCE = 2;
    /** Permission category for {@link android.healthconnect.datatypes.ElevationGainedRecord} */
    public static final int ELEVATION_GAINED = 3;
    /**
     * Permission category for {@link android.healthconnect.datatypes.ExerciseEventRecord} and
     * {@link android.healthconnect.datatypes.ExerciseLapRecord}
     */
    public static final int EXERCISE = 4;

    /** Permission category for {FloorsClimbedRecord} */
    public static final int FLOORS_CLIMBED = 5;

    /** Permission category for {@link android.healthconnect.datatypes.StepsRecord} */
    public static final int STEPS = 6;
    // BODY_MEASUREMENTS
    /** Permission category for {@link android.healthconnect.datatypes.BasalMetabolicRateRecord} */
    public static final int BASAL_METABOLIC_RATE = 9;
    /** Permission category for {BodyFatRecord} */
    public static final int BODY_FAT = 10;
    /** Permission category for {BodyWaterMassRecord} */
    public static final int BODY_WATER_MASS = 11;

    /** Permission category for {BoneMassRecord} */
    public static final int BONE_MASS = 12;
    /** Permission category for {HeightRecord} */
    public static final int HEIGHT = 13;
    /** Permission category for {HipCircumferenceRecord} */
    public static final int HIP_CIRCUMFERENCE = 14;
    /** Permission category for {LeanBodyMassRecord} */
    public static final int LEAN_BODY_MASS = 15;
    /** Permission category for {@link android.healthconnect.datatypes.PowerRecord} */
    public static final int POWER = 36;
    /** Permission category for {@link android.healthconnect.datatypes.SpeedRecord} */
    public static final int SPEED = 37;
    /** Permission category for {TotalCaloriesBurnedRecord} */
    public static final int TOTAL_CALORIES_BURNED = 35;
    /** Permission category for {Vo2MaxRecord} */
    public static final int VO2_MAX = 7;
    /** Permission category for {WaistCircumferenceRecord} */
    public static final int WAIST_CIRCUMFERENCE = 16;
    /** Permission category for {WeightRecord} */
    public static final int WEIGHT = 17;
    /** Permission category for {WheelChairPushesRecord} */
    public static final int WHEELCHAIR_PUSHES = 8;
    // CYCLE_TRACKING
    /** Permission category for {CervicalMucusRecord} */
    public static final int CERVICAL_MUCUS = 18;
    /** Permission category for {IntermenstrualBleedingRecord} */
    public static final int INTERMENSTRUAL_BLEEDING = 38;
    /** Permission category for {MenstruationRecord} */
    public static final int MENSTRUATION = 20;
    /** Permission category for {OvulationTestRecord} */
    public static final int OVULATION_TEST = 21;
    /** Permission category for {SexualActivityRecord} */
    public static final int SEXUAL_ACTIVITY = 22;
    // NUTRITION
    /** Permission category for {HydrationRecord} */
    public static final int HYDRATION = 23;
    /** Permission category for {NutritionRecord} */
    public static final int NUTRITION = 24;
    // SLEEP
    /** Permission category for {BasalBodyTemperatureRecord} */
    public static final int BASAL_BODY_TEMPERATURE = 33;
    /** Permission category for {SleepRecord} */
    public static final int SLEEP = 25;
    // VITALS
    /** Permission category for {BloodGlucose} */
    public static final int BLOOD_GLUCOSE = 26;
    /** Permission category for {BloodPressure} */
    public static final int BLOOD_PRESSURE = 27;
    /** Permission category for {BodyTemperature} */
    public static final int BODY_TEMPERATURE = 28;
    /** Permission category for {@link android.healthconnect.datatypes.HeartRateRecord} */
    public static final int HEART_RATE = 29;
    /** Permission category for {HeartRateVariability} */
    public static final int HEART_RATE_VARIABILITY = 30;
    /** Permission category for {OxygenSaturation} */
    public static final int OXYGEN_SATURATION = 31;
    /** Permission category for {RespiratoryRate} */
    public static final int RESPIRATORY_RATE = 32;
    /** Permission category for {RestingHeartRate} */
    public static final int RESTING_HEART_RATE = 34;

    private HealthPermissionCategory() {}

    /** @hide */
    @IntDef({
        UNKNOWN,
        ACTIVE_CALORIES_BURNED,
        DISTANCE,
        ELEVATION_GAINED,
        EXERCISE,
        FLOORS_CLIMBED,
        STEPS,
        TOTAL_CALORIES_BURNED,
        VO2_MAX,
        WHEELCHAIR_PUSHES,
        POWER,
        SPEED,
        BASAL_METABOLIC_RATE,
        BODY_FAT,
        BODY_WATER_MASS,
        BONE_MASS,
        HEIGHT,
        LEAN_BODY_MASS,
        WEIGHT,
        CERVICAL_MUCUS,
        MENSTRUATION,
        OVULATION_TEST,
        SEXUAL_ACTIVITY,
        INTERMENSTRUAL_BLEEDING,
        HYDRATION,
        NUTRITION,
        SLEEP,
        BASAL_BODY_TEMPERATURE,
        BLOOD_GLUCOSE,
        BLOOD_PRESSURE,
        BODY_TEMPERATURE,
        HEART_RATE,
        HEART_RATE_VARIABILITY,
        OXYGEN_SATURATION,
        RESPIRATORY_RATE,
        RESTING_HEART_RATE,
        HIP_CIRCUMFERENCE,
        WAIST_CIRCUMFERENCE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}
}
