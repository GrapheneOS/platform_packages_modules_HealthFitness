/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.data

enum class HealthPermissionType {
    // ACTIVITY
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

    // BODY_MEASUREMENTS
    BASAL_METABOLIC_RATE,
    BODY_FAT,
    BODY_WATER_MASS,
    BONE_MASS,
    HEIGHT,
    HIP_CIRCUMFERENCE,
    LEAN_BODY_MASS,
    WAIST_CIRCUMFERENCE,
    WEIGHT,

    // CYCLE_TRACKING
    CERVICAL_MUCUS,
    MENSTRUATION,
    OVULATION_TEST,
    SEXUAL_ACTIVITY,
    INTERMENSTRUAL_BLEEDING,

    // NUTRITION
    HYDRATION,
    NUTRITION,

    // SLEEP
    SLEEP,

    // VITALS
    BASAL_BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    BLOOD_PRESSURE,
    BODY_TEMPERATURE,
    HEART_RATE,
    HEART_RATE_VARIABILITY,
    OXYGEN_SATURATION,
    RESPIRATORY_RATE,
    RESTING_HEART_RATE,
}
