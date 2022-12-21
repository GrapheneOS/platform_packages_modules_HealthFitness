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

package android.healthconnect.datatypes;

import android.annotation.IntDef;
import android.annotation.SystemApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Identifier for each data type, as returned by {@link Record#getRecordType()}. This is used at
 * various places to efficiently determine operations to perform on a data type.
 *
 * @hide
 */
@SystemApi
public final class RecordTypeIdentifier {
    public static final int RECORD_TYPE_UNKNOWN = 0;

    // Interval Records
    public static final int RECORD_TYPE_STEPS = 1;
    public static final int RECORD_TYPE_EXERCISE_LAP = 2;
    public static final int RECORD_TYPE_ACTIVE_CALORIES_BURNED = 3;
    public static final int RECORD_TYPE_HYDRATION = 4;
    public static final int RECORD_TYPE_ELEVATION_GAINED = 5;
    public static final int RECORD_TYPE_SWIMMING_STROKES = 6;
    public static final int RECORD_TYPE_EXERCISE_EVENT = 7;
    public static final int RECORD_TYPE_FLOORS_CLIMBED = 8;
    public static final int RECORD_TYPE_WHEELCHAIR_PUSHES = 9;
    public static final int RECORD_TYPE_DISTANCE = 10;
    public static final int RECORD_TYPE_NUTRITION = 12;
    public static final int RECORD_TYPE_TOTAL_CALORIES_BURNED = 13;

    // Series Records
    public static final int RECORD_TYPE_HEART_RATE = 17;
    public static final int RECORD_TYPE_CYCLING_PEDALING_CADENCE = 18;
    public static final int RECORD_TYPE_POWER = 19;
    public static final int RECORD_TYPE_SPEED = 20;
    public static final int RECORD_TYPE_STEPS_CADENCE = 21;
    // Instant records
    public static final int RECORD_TYPE_BASAL_METABOLIC_RATE = 22;
    public static final int RECORD_TYPE_BODY_FAT = 23;
    public static final int RECORD_TYPE_VO2_MAX = 24;
    public static final int RECORD_TYPE_CERVICAL_MUCUS = 25;
    public static final int RECORD_TYPE_BASAL_BODY_TEMPERATURE = 26;
    public static final int RECORD_TYPE_MENSTRUATION_FLOW = 27;
    public static final int RECORD_TYPE_OXYGEN_SATURATION = 28;
    public static final int RECORD_TYPE_BLOOD_PRESSURE = 29;
    public static final int RECORD_TYPE_HEIGHT = 30;
    public static final int RECORD_TYPE_BLOOD_GLUCOSE = 31;
    public static final int RECORD_TYPE_WEIGHT = 32;
    public static final int RECORD_TYPE_LEAN_BODY_MASS = 33;
    public static final int RECORD_TYPE_SEXUAL_ACTIVITY = 34;
    public static final int RECORD_TYPE_BODY_TEMPERATURE = 35;
    public static final int RECORD_TYPE_OVULATION_TEST = 36;
    public static final int RECORD_TYPE_RESPIRATORY_RATE = 37;
    public static final int RECORD_TYPE_BONE_MASS = 38;
    public static final int RECORD_TYPE_RESTING_HEART_RATE = 39;

    // Session records
    public static final int RECORD_TYPE_EXERCISE_SESSION = 40;
    public static final int RECORD_TYPE_SLEEP_SESSION = 41;

    private RecordTypeIdentifier() {}

    /** @hide */
    @IntDef({
        RECORD_TYPE_UNKNOWN,
        RECORD_TYPE_STEPS,
        RECORD_TYPE_HEART_RATE,
        RECORD_TYPE_BASAL_METABOLIC_RATE,
        RECORD_TYPE_CYCLING_PEDALING_CADENCE,
        RECORD_TYPE_POWER,
        RECORD_TYPE_SPEED,
        RECORD_TYPE_STEPS_CADENCE,
        RECORD_TYPE_DISTANCE,
        RECORD_TYPE_WHEELCHAIR_PUSHES,
        RECORD_TYPE_TOTAL_CALORIES_BURNED,
        RECORD_TYPE_SWIMMING_STROKES,
        RECORD_TYPE_FLOORS_CLIMBED,
        RECORD_TYPE_ELEVATION_GAINED,
        RECORD_TYPE_ACTIVE_CALORIES_BURNED,
        RECORD_TYPE_HYDRATION,
        RECORD_TYPE_EXERCISE_EVENT,
        RECORD_TYPE_EXERCISE_LAP,
        RECORD_TYPE_NUTRITION,
        RECORD_TYPE_RESPIRATORY_RATE,
        RECORD_TYPE_BONE_MASS,
        RECORD_TYPE_RESTING_HEART_RATE,
        RECORD_TYPE_BODY_FAT,
        RECORD_TYPE_VO2_MAX,
        RECORD_TYPE_CERVICAL_MUCUS,
        RECORD_TYPE_BASAL_BODY_TEMPERATURE,
        RECORD_TYPE_MENSTRUATION_FLOW,
        RECORD_TYPE_OXYGEN_SATURATION,
        RECORD_TYPE_BLOOD_PRESSURE,
        RECORD_TYPE_HEIGHT,
        RECORD_TYPE_BLOOD_GLUCOSE,
        RECORD_TYPE_WEIGHT,
        RECORD_TYPE_LEAN_BODY_MASS,
        RECORD_TYPE_SEXUAL_ACTIVITY,
        RECORD_TYPE_BODY_TEMPERATURE,
        RECORD_TYPE_OVULATION_TEST,
        RECORD_TYPE_EXERCISE_SESSION
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecordType {}
}
