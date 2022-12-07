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

import android.healthconnect.datatypes.Record;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the permission category of a {@link Record}. A record can only belong to one and only
 * one {@link HealthPermissionCategory}
 *
 * @hide
 */
public class HealthPermissionCategory {
    public static final int UNKNOWN = 0;
    // ACTIVITY
    public static final int ACTIVE_CALORIES_BURNED = 1;
    public static final int DISTANCE = 2;
    public static final int ELEVATION_GAINED = 3;
    public static final int EXERCISE = 4;
    public static final int FLOORS_CLIMBED = 5;
    public static final int STEPS = 6;
    // BODY_MEASUREMENTS
    public static final int BASAL_METABOLIC_RATE = 9;
    public static final int BODY_FAT = 10;
    public static final int BODY_WATER_MASS = 11;
    public static final int BONE_MASS = 12;
    public static final int HEIGHT = 13;
    public static final int HIP_CIRCUMFERENCE = 14;
    public static final int LEAN_BODY_MASS = 15;
    public static final int POWER = 36;
    public static final int SPEED = 37;
    public static final int TOTAL_CALORIES_BURNED = 35;
    public static final int VO2_MAX = 7;
    public static final int WAIST_CIRCUMFERENCE = 16;
    public static final int WEIGHT = 17;
    public static final int WHEELCHAIR_PUSHES = 8;
    // CYCLE_TRACKING
    public static final int CERVICAL_MUCUS = 18;
    public static final int INTERMENSTRUAL_BLEEDING = 38;
    public static final int MENSTRUATION = 20;
    public static final int OVULATION_TEST = 21;
    public static final int SEXUAL_ACTIVITY = 22;
    // NUTRITION
    public static final int HYDRATION = 23;
    public static final int NUTRITION = 24;
    // SLEEP
    public static final int BASAL_BODY_TEMPERATURE = 33;
    public static final int SLEEP = 25;
    // VITALS
    public static final int BLOOD_GLUCOSE = 26;
    public static final int BLOOD_PRESSURE = 27;
    public static final int BODY_TEMPERATURE = 28;
    public static final int HEART_RATE = 29;
    public static final int HEART_RATE_VARIABILITY = 30;
    public static final int OXYGEN_SATURATION = 31;
    public static final int RESPIRATORY_RATE = 32;
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
        HIP_CIRCUMFERENCE,
        LEAN_BODY_MASS,
        WAIST_CIRCUMFERENCE,
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
        RESTING_HEART_RATE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}
}
