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

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Unique ids for each data type used for generating UUIDs from client record ids.
 *
 * @hide
 */
public class RecordTypeIdForUuid {
    public static final int RECORD_TYPE_ID_FOR_UUID_UNKNOWN = 0;
    public static final int RECORD_TYPE_ID_FOR_UUID_EXERCISE_SESSION = 4;
    public static final int RECORD_TYPE_ID_FOR_UUID_DISTANCE = 6;
    public static final int RECORD_TYPE_ID_FOR_UUID_ELEVATION_GAINED = 7;
    public static final int RECORD_TYPE_ID_FOR_UUID_FLOORS_CLIMBED = 8;
    public static final int RECORD_TYPE_ID_FOR_UUID_HYDRATION = 9;
    public static final int RECORD_TYPE_ID_FOR_UUID_NUTRITION = 10;
    public static final int RECORD_TYPE_ID_FOR_UUID_SLEEP_SESSION = 12;
    public static final int RECORD_TYPE_ID_FOR_UUID_STEPS = 13;
    public static final int RECORD_TYPE_ID_FOR_UUID_BASAL_METABOLIC_RATE = 16;
    public static final int RECORD_TYPE_ID_FOR_UUID_BLOOD_GLUCOSE = 17;
    public static final int RECORD_TYPE_ID_FOR_UUID_BLOOD_PRESSURE = 18;
    public static final int RECORD_TYPE_ID_FOR_UUID_BODY_FAT = 19;
    public static final int RECORD_TYPE_ID_FOR_UUID_BODY_TEMPERATURE = 20;
    public static final int RECORD_TYPE_ID_FOR_UUID_BONE_MASS = 21;
    public static final int RECORD_TYPE_ID_FOR_UUID_CERVICAL_MUCUS = 22;
    public static final int RECORD_TYPE_ID_FOR_UUID_HEIGHT = 28;
    public static final int RECORD_TYPE_ID_FOR_UUID_HEART_RATE_VARIABILITY_RMSSD = 31;
    public static final int RECORD_TYPE_ID_FOR_UUID_LEAN_BODY_MASS = 39;
    public static final int RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_FLOW = 41;
    public static final int RECORD_TYPE_ID_FOR_UUID_OVULATION_TEST = 42;
    public static final int RECORD_TYPE_ID_FOR_UUID_OXYGEN_SATURATION = 43;
    public static final int RECORD_TYPE_ID_FOR_UUID_RESPIRATORY_RATE = 46;
    public static final int RECORD_TYPE_ID_FOR_UUID_RESTING_HEART_RATE = 47;
    public static final int RECORD_TYPE_ID_FOR_UUID_SEXUAL_ACTIVITY = 48;
    public static final int RECORD_TYPE_ID_FOR_UUID_VO2_MAX = 51;
    public static final int RECORD_TYPE_ID_FOR_UUID_WEIGHT = 53;
    public static final int RECORD_TYPE_ID_FOR_UUID_HEART_RATE = 56;
    public static final int RECORD_TYPE_ID_FOR_UUID_CYCLING_PEDALING_CADENCE = 58;
    public static final int RECORD_TYPE_ID_FOR_UUID_POWER = 60;
    public static final int RECORD_TYPE_ID_FOR_UUID_SPEED = 61;
    public static final int RECORD_TYPE_ID_FOR_UUID_STEPS_CADENCE = 62;
    public static final int RECORD_TYPE_ID_FOR_UUID_WHEELCHAIR_PUSHES = 63;
    public static final int RECORD_TYPE_ID_FOR_UUID_BODY_WATER_MASS = 64;
    public static final int RECORD_TYPE_ID_FOR_UUID_BASAL_BODY_TEMPERATURE = 65;
    public static final int RECORD_TYPE_ID_FOR_UUID_TOTAL_CALORIES_BURNED = 66;
    public static final int RECORD_TYPE_ID_FOR_UUID_ACTIVE_CALORIES_BURNED = 67;
    public static final int RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_PERIOD = 69;
    public static final int RECORD_TYPE_ID_FOR_UUID_INTERMENSTRUAL_BLEEDING = 70;
    public static final int RECORD_TYPE_ID_FOR_UUID_SKIN_TEMPERATURE = 71;
    public static final int RECORD_TYPE_ID_FOR_UUID_PLANNED_EXERCISE_SESSION = 72;
    public static final int RECORD_TYPE_ID_FOR_UUID_MINDFULNESS_SESSION = 73;
    public static final int RECORD_TYPE_ID_FOR_UUID_ACTIVITY_INTENSITY = 74;
    public static final int RECORD_TYPE_ID_FOR_UUID_NICOTINE_INTAKE = 75;
    public static final int RECORD_TYPE_ID_FOR_UUID_SYMPTOMS = 76;
    public static final int RECORD_TYPE_ID_FOR_UUID_ALCOHOL_CONSUMPTION = 77;
    public static final int RECORD_TYPE_ID_FOR_UUID_CYCLE_PHASES = 78;

    /** @hide */
    @IntDef({
        RECORD_TYPE_ID_FOR_UUID_ACTIVE_CALORIES_BURNED,
        RECORD_TYPE_ID_FOR_UUID_BASAL_BODY_TEMPERATURE,
        RECORD_TYPE_ID_FOR_UUID_BASAL_METABOLIC_RATE,
        RECORD_TYPE_ID_FOR_UUID_BLOOD_GLUCOSE,
        RECORD_TYPE_ID_FOR_UUID_BLOOD_PRESSURE,
        RECORD_TYPE_ID_FOR_UUID_BODY_FAT,
        RECORD_TYPE_ID_FOR_UUID_BODY_TEMPERATURE,
        RECORD_TYPE_ID_FOR_UUID_BODY_WATER_MASS,
        RECORD_TYPE_ID_FOR_UUID_BONE_MASS,
        RECORD_TYPE_ID_FOR_UUID_CERVICAL_MUCUS,
        RECORD_TYPE_ID_FOR_UUID_CYCLING_PEDALING_CADENCE,
        RECORD_TYPE_ID_FOR_UUID_DISTANCE,
        RECORD_TYPE_ID_FOR_UUID_ELEVATION_GAINED,
        RECORD_TYPE_ID_FOR_UUID_EXERCISE_SESSION,
        RECORD_TYPE_ID_FOR_UUID_FLOORS_CLIMBED,
        RECORD_TYPE_ID_FOR_UUID_HEART_RATE,
        RECORD_TYPE_ID_FOR_UUID_HEART_RATE_VARIABILITY_RMSSD,
        RECORD_TYPE_ID_FOR_UUID_HEIGHT,
        RECORD_TYPE_ID_FOR_UUID_HYDRATION,
        RECORD_TYPE_ID_FOR_UUID_INTERMENSTRUAL_BLEEDING,
        RECORD_TYPE_ID_FOR_UUID_LEAN_BODY_MASS,
        RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_FLOW,
        RECORD_TYPE_ID_FOR_UUID_MENSTRUATION_PERIOD,
        RECORD_TYPE_ID_FOR_UUID_MINDFULNESS_SESSION,
        RECORD_TYPE_ID_FOR_UUID_NUTRITION,
        RECORD_TYPE_ID_FOR_UUID_OVULATION_TEST,
        RECORD_TYPE_ID_FOR_UUID_OXYGEN_SATURATION,
        RECORD_TYPE_ID_FOR_UUID_PLANNED_EXERCISE_SESSION,
        RECORD_TYPE_ID_FOR_UUID_POWER,
        RECORD_TYPE_ID_FOR_UUID_RESPIRATORY_RATE,
        RECORD_TYPE_ID_FOR_UUID_RESTING_HEART_RATE,
        RECORD_TYPE_ID_FOR_UUID_SEXUAL_ACTIVITY,
        RECORD_TYPE_ID_FOR_UUID_SKIN_TEMPERATURE,
        RECORD_TYPE_ID_FOR_UUID_SLEEP_SESSION,
        RECORD_TYPE_ID_FOR_UUID_SPEED,
        RECORD_TYPE_ID_FOR_UUID_STEPS,
        RECORD_TYPE_ID_FOR_UUID_STEPS_CADENCE,
        RECORD_TYPE_ID_FOR_UUID_TOTAL_CALORIES_BURNED,
        RECORD_TYPE_ID_FOR_UUID_UNKNOWN,
        RECORD_TYPE_ID_FOR_UUID_VO2_MAX,
        RECORD_TYPE_ID_FOR_UUID_WEIGHT,
        RECORD_TYPE_ID_FOR_UUID_WHEELCHAIR_PUSHES,
        RECORD_TYPE_ID_FOR_UUID_ACTIVITY_INTENSITY,
        RECORD_TYPE_ID_FOR_UUID_NICOTINE_INTAKE,
        RECORD_TYPE_ID_FOR_UUID_SYMPTOMS,
        RECORD_TYPE_ID_FOR_UUID_ALCOHOL_CONSUMPTION,
        RECORD_TYPE_ID_FOR_UUID_CYCLE_PHASES
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}
}
