/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.health.connect.HealthPermissionCategory
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromFitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon

// TODO (b/299880830) possibly rename "category" to something else
enum class FitnessPermissionType(val category: Int) : HealthPermissionType {
    // ACTIVITY
    ACTIVE_CALORIES_BURNED(HealthPermissionCategory.ACTIVE_CALORIES_BURNED),
    ACTIVITY_INTENSITY(HealthPermissionCategory.ACTIVITY_INTENSITY),
    DISTANCE(HealthPermissionCategory.DISTANCE),
    ELEVATION_GAINED(HealthPermissionCategory.ELEVATION_GAINED),
    EXERCISE(HealthPermissionCategory.EXERCISE),
    PLANNED_EXERCISE(HealthPermissionCategory.PLANNED_EXERCISE),
    FLOORS_CLIMBED(HealthPermissionCategory.FLOORS_CLIMBED),
    STEPS(HealthPermissionCategory.STEPS),
    TOTAL_CALORIES_BURNED(HealthPermissionCategory.TOTAL_CALORIES_BURNED),
    VO2_MAX(HealthPermissionCategory.VO2_MAX),
    WHEELCHAIR_PUSHES(HealthPermissionCategory.WHEELCHAIR_PUSHES),
    POWER(HealthPermissionCategory.POWER),
    SPEED(HealthPermissionCategory.SPEED),
    EXERCISE_ROUTE(HealthPermissionCategory.EXERCISE),

    // BODY_MEASUREMENTS
    BASAL_METABOLIC_RATE(HealthPermissionCategory.BASAL_METABOLIC_RATE),
    BODY_FAT(HealthPermissionCategory.BODY_FAT),
    BODY_WATER_MASS(HealthPermissionCategory.BODY_WATER_MASS),
    BONE_MASS(HealthPermissionCategory.BONE_MASS),
    HEIGHT(HealthPermissionCategory.HEIGHT),
    LEAN_BODY_MASS(HealthPermissionCategory.LEAN_BODY_MASS),
    WEIGHT(HealthPermissionCategory.WEIGHT),

    // CYCLE_TRACKING
    CERVICAL_MUCUS(HealthPermissionCategory.CERVICAL_MUCUS),
    MENSTRUATION(HealthPermissionCategory.MENSTRUATION),
    OVULATION_TEST(HealthPermissionCategory.OVULATION_TEST),
    SEXUAL_ACTIVITY(HealthPermissionCategory.SEXUAL_ACTIVITY),
    INTERMENSTRUAL_BLEEDING(HealthPermissionCategory.INTERMENSTRUAL_BLEEDING),

    // NUTRITION
    HYDRATION(HealthPermissionCategory.HYDRATION),
    NUTRITION(HealthPermissionCategory.NUTRITION),

    // SLEEP
    SLEEP(HealthPermissionCategory.SLEEP),

    // VITALS
    BASAL_BODY_TEMPERATURE(HealthPermissionCategory.BASAL_BODY_TEMPERATURE),
    BLOOD_GLUCOSE(HealthPermissionCategory.BLOOD_GLUCOSE),
    BLOOD_PRESSURE(HealthPermissionCategory.BLOOD_PRESSURE),
    BODY_TEMPERATURE(HealthPermissionCategory.BODY_TEMPERATURE),
    HEART_RATE(HealthPermissionCategory.HEART_RATE),
    HEART_RATE_VARIABILITY(HealthPermissionCategory.HEART_RATE_VARIABILITY),
    OXYGEN_SATURATION(HealthPermissionCategory.OXYGEN_SATURATION),
    RESPIRATORY_RATE(HealthPermissionCategory.RESPIRATORY_RATE),
    RESTING_HEART_RATE(HealthPermissionCategory.RESTING_HEART_RATE),
    SKIN_TEMPERATURE(HealthPermissionCategory.SKIN_TEMPERATURE),

    // WELLNESS
    MINDFULNESS(HealthPermissionCategory.MINDFULNESS),
    NICOTINE_INTAKE(HealthPermissionCategory.NICOTINE_INTAKE);

    override fun lowerCaseLabel(): Int =
        FitnessPermissionStrings.fromPermissionType(this).lowercaseLabel

    override fun upperCaseLabel(): Int =
        FitnessPermissionStrings.fromPermissionType(this).uppercaseLabel

    override fun icon(context: Context): Drawable? = fromFitnessPermissionType(this).icon(context)
}

fun isValidFitnessPermissionType(permissionTypeString: String): Boolean {
    try {
        FitnessPermissionType.valueOf(permissionTypeString)
    } catch (e: IllegalArgumentException) {
        return false
    }
    return true
}

fun fromHealthPermissionCategory(healthPermissionCategory: Int): HealthPermissionType {
    return when (healthPermissionCategory) {
        HealthPermissionCategory.UNKNOWN ->
            throw IllegalArgumentException("PermissionType is UNKNOWN.")
        // ACTIVITY
        HealthPermissionCategory.ACTIVE_CALORIES_BURNED ->
            FitnessPermissionType.ACTIVE_CALORIES_BURNED
        HealthPermissionCategory.ACTIVITY_INTENSITY -> FitnessPermissionType.ACTIVITY_INTENSITY
        HealthPermissionCategory.DISTANCE -> FitnessPermissionType.DISTANCE
        HealthPermissionCategory.ELEVATION_GAINED -> FitnessPermissionType.ELEVATION_GAINED
        HealthPermissionCategory.EXERCISE -> FitnessPermissionType.EXERCISE
        HealthPermissionCategory.PLANNED_EXERCISE -> FitnessPermissionType.PLANNED_EXERCISE
        HealthPermissionCategory.FLOORS_CLIMBED -> FitnessPermissionType.FLOORS_CLIMBED
        HealthPermissionCategory.STEPS -> FitnessPermissionType.STEPS
        HealthPermissionCategory.TOTAL_CALORIES_BURNED ->
            FitnessPermissionType.TOTAL_CALORIES_BURNED
        HealthPermissionCategory.VO2_MAX -> FitnessPermissionType.VO2_MAX
        HealthPermissionCategory.WHEELCHAIR_PUSHES -> FitnessPermissionType.WHEELCHAIR_PUSHES
        HealthPermissionCategory.POWER -> FitnessPermissionType.POWER
        HealthPermissionCategory.SPEED -> FitnessPermissionType.SPEED
        // BODY_MEASUREMENTS
        HealthPermissionCategory.BASAL_METABOLIC_RATE -> FitnessPermissionType.BASAL_METABOLIC_RATE
        HealthPermissionCategory.BODY_FAT -> FitnessPermissionType.BODY_FAT
        HealthPermissionCategory.BODY_WATER_MASS -> FitnessPermissionType.BODY_WATER_MASS
        HealthPermissionCategory.BONE_MASS -> FitnessPermissionType.BONE_MASS
        HealthPermissionCategory.HEIGHT -> FitnessPermissionType.HEIGHT
        HealthPermissionCategory.LEAN_BODY_MASS -> FitnessPermissionType.LEAN_BODY_MASS
        HealthPermissionCategory.WEIGHT -> FitnessPermissionType.WEIGHT
        // CYCLE_TRACKING
        HealthPermissionCategory.CERVICAL_MUCUS -> FitnessPermissionType.CERVICAL_MUCUS
        HealthPermissionCategory.MENSTRUATION -> FitnessPermissionType.MENSTRUATION
        HealthPermissionCategory.OVULATION_TEST -> FitnessPermissionType.OVULATION_TEST
        HealthPermissionCategory.SEXUAL_ACTIVITY -> FitnessPermissionType.SEXUAL_ACTIVITY
        HealthPermissionCategory.INTERMENSTRUAL_BLEEDING ->
            FitnessPermissionType.INTERMENSTRUAL_BLEEDING
        // NUTRITION
        HealthPermissionCategory.HYDRATION -> FitnessPermissionType.HYDRATION
        HealthPermissionCategory.NUTRITION -> FitnessPermissionType.NUTRITION
        // SLEEP
        HealthPermissionCategory.SLEEP -> FitnessPermissionType.SLEEP
        // VITALS
        HealthPermissionCategory.BASAL_BODY_TEMPERATURE ->
            FitnessPermissionType.BASAL_BODY_TEMPERATURE
        HealthPermissionCategory.BLOOD_GLUCOSE -> FitnessPermissionType.BLOOD_GLUCOSE
        HealthPermissionCategory.BLOOD_PRESSURE -> FitnessPermissionType.BLOOD_PRESSURE
        HealthPermissionCategory.BODY_TEMPERATURE -> FitnessPermissionType.BODY_TEMPERATURE
        HealthPermissionCategory.HEART_RATE -> FitnessPermissionType.HEART_RATE
        HealthPermissionCategory.HEART_RATE_VARIABILITY ->
            FitnessPermissionType.HEART_RATE_VARIABILITY
        HealthPermissionCategory.OXYGEN_SATURATION -> FitnessPermissionType.OXYGEN_SATURATION
        HealthPermissionCategory.RESPIRATORY_RATE -> FitnessPermissionType.RESPIRATORY_RATE
        HealthPermissionCategory.RESTING_HEART_RATE -> FitnessPermissionType.RESTING_HEART_RATE
        HealthPermissionCategory.SKIN_TEMPERATURE -> FitnessPermissionType.SKIN_TEMPERATURE

        // WELLNESS
        HealthPermissionCategory.MINDFULNESS -> FitnessPermissionType.MINDFULNESS
        HealthPermissionCategory.NICOTINE_INTAKE -> FitnessPermissionType.NICOTINE_INTAKE
        else -> throw IllegalArgumentException("PermissionType is not supported.")
    }
}
