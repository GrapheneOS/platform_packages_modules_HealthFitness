/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Constant variables used across the app. */
object Constants {

    val ALL_PERMISSIONS =
        arrayOf(
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
            "android.permission.health.READ_BASAL_BODY_TEMPERATURE",
            "android.permission.health.READ_BASAL_METABOLIC_RATE",
            "android.permission.health.READ_BLOOD_GLUCOSE",
            "android.permission.health.READ_BLOOD_PRESSURE",
            "android.permission.health.READ_BODY_FAT",
            "android.permission.health.READ_BODY_TEMPERATURE",
            "android.permission.health.READ_BODY_WATER_MASS",
            "android.permission.health.READ_BONE_MASS",
            "android.permission.health.READ_CERVICAL_MUCUS",
            "android.permission.health.READ_DISTANCE",
            "android.permission.health.READ_ELEVATION_GAINED",
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_FLOORS_CLIMBED",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_HEART_RATE_VARIABILITY",
            "android.permission.health.READ_HEIGHT",
            "android.permission.health.READ_HIP_CIRCUMFERENCE",
            "android.permission.health.READ_HYDRATION",
            "android.permission.health.READ_LEAN_BODY_MASS",
            "android.permission.health.READ_MENSTRUATION",
            "android.permission.health.READ_NUTRITION",
            "android.permission.health.READ_OVULATION_TEST",
            "android.permission.health.READ_OXYGEN_SATURATION",
            "android.permission.health.READ_POWER",
            "android.permission.health.READ_RESPIRATORY_RATE",
            "android.permission.health.READ_RESTING_HEART_RATE",
            "android.permission.health.READ_SEXUAL_ACTIVITY",
            "android.permission.health.READ_SLEEP",
            "android.permission.health.READ_SPEED",
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED",
            "android.permission.health.READ_VO2_MAX",
            "android.permission.health.READ_WAIST_CIRCUMFERENCE",
            "android.permission.health.READ_WEIGHT",
            "android.permission.health.READ_WHEELCHAIR_PUSHES",
            "android.permission.health.WRITE_ACTIVE_CALORIES_BURNED",
            "android.permission.health.WRITE_BASAL_BODY_TEMPERATURE",
            "android.permission.health.WRITE_BASAL_METABOLIC_RATE",
            "android.permission.health.WRITE_BLOOD_GLUCOSE",
            "android.permission.health.WRITE_BLOOD_PRESSURE",
            "android.permission.health.WRITE_BODY_FAT",
            "android.permission.health.WRITE_BODY_TEMPERATURE",
            "android.permission.health.WRITE_BODY_WATER_MASS",
            "android.permission.health.WRITE_BONE_MASS",
            "android.permission.health.WRITE_CERVICAL_MUCUS",
            "android.permission.health.WRITE_DISTANCE",
            "android.permission.health.WRITE_ELEVATION_GAINED",
            "android.permission.health.WRITE_EXERCISE",
            "android.permission.health.WRITE_FLOORS_CLIMBED",
            "android.permission.health.WRITE_HEART_RATE",
            "android.permission.health.WRITE_HEART_RATE_VARIABILITY",
            "android.permission.health.WRITE_HEIGHT",
            "android.permission.health.WRITE_HIP_CIRCUMFERENCE",
            "android.permission.health.WRITE_HYDRATION",
            "android.permission.health.WRITE_LEAN_BODY_MASS",
            "android.permission.health.WRITE_MENSTRUATION",
            "android.permission.health.WRITE_NUTRITION",
            "android.permission.health.WRITE_OVULATION_TEST",
            "android.permission.health.WRITE_OXYGEN_SATURATION",
            "android.permission.health.WRITE_POWER",
            "android.permission.health.WRITE_RESPIRATORY_RATE",
            "android.permission.health.WRITE_RESTING_HEART_RATE",
            "android.permission.health.WRITE_SEXUAL_ACTIVITY",
            "android.permission.health.WRITE_SLEEP",
            "android.permission.health.WRITE_SPEED",
            "android.permission.health.WRITE_STEPS",
            "android.permission.health.WRITE_TOTAL_CALORIES_BURNED",
            "android.permission.health.WRITE_VO2_MAX",
            "android.permission.health.WRITE_WAIST_CIRCUMFERENCE",
            "android.permission.health.WRITE_WEIGHT",
            "android.permission.health.WRITE_WHEELCHAIR_PUSHES",
        )

    /** Represents Category group for HealthConnect data. */
    enum class HealthDataCategory(
        val healthPermissionTypes: List<HealthPermissionType>,
        @StringRes val title: Int,
        @DrawableRes val icon: Int,
    ) {
        ACTIVITY(
            CategoriesMappers.ACTIVITY_PERMISSION_GROUPS,
            R.string.activity_category,
            R.drawable.quantum_gm_ic_directions_run_vd_theme_24),
        BODY_MEASUREMENTS(
            CategoriesMappers.BODY_MEASUREMENTS_PERMISSION_GROUPS,
            R.string.body_measurements_category,
            R.drawable.quantum_gm_ic_straighten_vd_theme_24),
        SLEEP(
            CategoriesMappers.SLEEP_PERMISSION_GROUPS,
            R.string.sleep_category,
            R.drawable.ic_sleep),
        VITALS(
            CategoriesMappers.VITALS_PERMISSION_GROUPS,
            R.string.vitals_category,
            R.drawable.ic_vitals),
        CYCLE_TRACKING(
            CategoriesMappers.CYCLE_TRACKING_PERMISSION_GROUPS,
            R.string.cycle_tracking_category,
            R.drawable.ic_cycle_tracking),
        NUTRITION(
            CategoriesMappers.NUTRITION_PERMISSION_GROUPS,
            R.string.nutrition_category,
            R.drawable.quantum_gm_ic_grocery_vd_theme_24),
    }

    /** Permission groups for each {@link HealthDataCategory}. */
    object CategoriesMappers {
        val ACTIVITY_PERMISSION_GROUPS =
            listOf(
                HealthPermissionType.ACTIVE_CALORIES_BURNED,
                HealthPermissionType.DISTANCE,
                HealthPermissionType.ELEVATION_GAINED,
                HealthPermissionType.EXERCISE,
                HealthPermissionType.FLOORS_CLIMBED,
                HealthPermissionType.POWER,
                HealthPermissionType.SPEED,
                HealthPermissionType.STEPS,
                HealthPermissionType.TOTAL_CALORIES_BURNED,
                HealthPermissionType.VO2_MAX,
                HealthPermissionType.WHEELCHAIR_PUSHES,
            )

        val BODY_MEASUREMENTS_PERMISSION_GROUPS =
            listOf(
                HealthPermissionType.BASAL_METABOLIC_RATE,
                HealthPermissionType.BODY_FAT,
                HealthPermissionType.BODY_WATER_MASS,
                HealthPermissionType.BONE_MASS,
                HealthPermissionType.HEIGHT,
                HealthPermissionType.HIP_CIRCUMFERENCE,
                HealthPermissionType.LEAN_BODY_MASS,
                HealthPermissionType.WAIST_CIRCUMFERENCE,
                HealthPermissionType.WEIGHT)

        val CYCLE_TRACKING_PERMISSION_GROUPS =
            listOf(
                HealthPermissionType.CERVICAL_MUCUS,
                // TODO: Uncomment when clarity on its progress
                // HealthPermissionType.INTERMENSTRUAL_BLEEDING,
                HealthPermissionType.MENSTRUATION,
                HealthPermissionType.OVULATION_TEST,
                HealthPermissionType.SEXUAL_ACTIVITY)

        val NUTRITION_PERMISSION_GROUPS =
            listOf(HealthPermissionType.HYDRATION, HealthPermissionType.NUTRITION)

        val SLEEP_PERMISSION_GROUPS = listOf(HealthPermissionType.SLEEP)

        val VITALS_PERMISSION_GROUPS =
            listOf(
                HealthPermissionType.BASAL_BODY_TEMPERATURE,
                HealthPermissionType.BLOOD_GLUCOSE,
                HealthPermissionType.BLOOD_PRESSURE,
                HealthPermissionType.BODY_TEMPERATURE,
                HealthPermissionType.HEART_RATE,
                HealthPermissionType.HEART_RATE_VARIABILITY,
                HealthPermissionType.OXYGEN_SATURATION,
                HealthPermissionType.RESPIRATORY_RATE,
                HealthPermissionType.RESTING_HEART_RATE)
    }

    enum class HealthPermissionType(@StringRes val title: Int) {
        // ACTIVITY
        ACTIVE_CALORIES_BURNED(R.string.active_calories_burned_label),
        DISTANCE(R.string.distance_label),
        ELEVATION_GAINED(R.string.elevation_gained_label),
        EXERCISE(R.string.exercise_label),
        FLOORS_CLIMBED(R.string.floors_climbed_label),
        STEPS(R.string.steps_label),
        TOTAL_CALORIES_BURNED(R.string.total_calories_burned_label),
        VO2_MAX(R.string.vo2_max_label),
        WHEELCHAIR_PUSHES(R.string.wheelchair_pushes_label),
        POWER(R.string.power_label),
        SPEED(R.string.speed_label),

        // BODY_MEASUREMENTS
        BASAL_METABOLIC_RATE(R.string.basal_metabolic_rate_label),
        BODY_FAT(R.string.body_fat_label),
        BODY_WATER_MASS(R.string.body_water_mass_label),
        BONE_MASS(R.string.bone_mass_label),
        HEIGHT(R.string.height_label),
        HIP_CIRCUMFERENCE(R.string.hip_circumference_label),
        LEAN_BODY_MASS(R.string.lean_body_mass_label),
        WAIST_CIRCUMFERENCE(R.string.waist_circumference_label),
        WEIGHT(R.string.weight_label),

        // CYCLE_TRACKING
        CERVICAL_MUCUS(R.string.cervical_mucus_label),
        MENSTRUATION(R.string.menstruation_label),
        OVULATION_TEST(R.string.ovulation_test_label),
        SEXUAL_ACTIVITY(R.string.sexual_activity_label),
        // TODO: Uncomment when clarity on its progress
        // INTERMENSTRUAL_BLEEDING(R.string.basal_body_temperature_label),

        // NUTRITION
        HYDRATION(R.string.hydration_label),
        NUTRITION(R.string.nutrition_label),

        // SLEEP
        SLEEP(R.string.sleep_label),

        // VITALS
        BASAL_BODY_TEMPERATURE(R.string.basal_body_temperature_label),
        BLOOD_GLUCOSE(R.string.blood_glucose_label),
        BLOOD_PRESSURE(R.string.blood_pressure_label),
        BODY_TEMPERATURE(R.string.body_temperature_label),
        HEART_RATE(R.string.heart_rate_label),
        HEART_RATE_VARIABILITY(R.string.heart_rate_variability_label),
        OXYGEN_SATURATION(R.string.oxygen_saturation_label),
        RESPIRATORY_RATE(R.string.respiratory_rate_label),
        RESTING_HEART_RATE(R.string.resting_heart_rate_label),
    }
}
