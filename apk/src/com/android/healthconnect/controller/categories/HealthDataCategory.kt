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
package com.android.healthconnect.controller.categories

import android.healthconnect.HealthDataCategory as sdkHealthDataCategory
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.CategoriesMappers.ACTIVITY_PERMISSION_GROUPS
import com.android.healthconnect.controller.categories.CategoriesMappers.BODY_MEASUREMENTS_PERMISSION_GROUPS
import com.android.healthconnect.controller.categories.CategoriesMappers.CYCLE_TRACKING_PERMISSION_GROUPS
import com.android.healthconnect.controller.categories.CategoriesMappers.NUTRITION_PERMISSION_GROUPS
import com.android.healthconnect.controller.categories.CategoriesMappers.SLEEP_PERMISSION_GROUPS
import com.android.healthconnect.controller.categories.CategoriesMappers.VITALS_PERMISSION_GROUPS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType

/** Represents Category group for HealthConnect data. */
enum class HealthDataCategory(
    val healthPermissionTypes: List<HealthPermissionType>,
    @StringRes val uppercaseTitle: Int,
    @StringRes val lowercaseTitle: Int,
    @DrawableRes val icon: Int
) {
    ACTIVITY(
        ACTIVITY_PERMISSION_GROUPS,
        R.string.activity_category_uppercase,
        R.string.activity_category_lowercase,
        R.drawable.quantum_gm_ic_directions_run_vd_theme_24),
    BODY_MEASUREMENTS(
        BODY_MEASUREMENTS_PERMISSION_GROUPS,
        R.string.body_measurements_category_uppercase,
        R.string.body_measurements_category_lowercase,
        R.drawable.quantum_gm_ic_straighten_vd_theme_24),
    SLEEP(
        SLEEP_PERMISSION_GROUPS,
        R.string.sleep_category_uppercase,
        R.string.sleep_category_lowercase,
        R.drawable.ic_sleep),
    VITALS(
        VITALS_PERMISSION_GROUPS,
        R.string.vitals_category_uppercase,
        R.string.vitals_category_lowercase,
        R.drawable.ic_vitals),
    CYCLE_TRACKING(
        CYCLE_TRACKING_PERMISSION_GROUPS,
        R.string.cycle_tracking_category_uppercase,
        R.string.cycle_tracking_category_lowercase,
        R.drawable.ic_cycle_tracking),
    NUTRITION(
        NUTRITION_PERMISSION_GROUPS,
        R.string.nutrition_category_uppercase,
        R.string.nutrition_category_lowercase,
        R.drawable.quantum_gm_ic_grocery_vd_theme_24),
}

fun fromName(categoryName: String): HealthDataCategory =
    when (categoryName) {
        HealthDataCategory.ACTIVITY.name -> HealthDataCategory.ACTIVITY
        HealthDataCategory.BODY_MEASUREMENTS.name -> HealthDataCategory.BODY_MEASUREMENTS
        HealthDataCategory.CYCLE_TRACKING.name -> HealthDataCategory.CYCLE_TRACKING
        HealthDataCategory.NUTRITION.name -> HealthDataCategory.NUTRITION
        HealthDataCategory.SLEEP.name -> HealthDataCategory.SLEEP
        HealthDataCategory.VITALS.name -> HealthDataCategory.VITALS
        else -> throw IllegalArgumentException("Category name is not supported.")
    }

fun fromSdkHealthDataCategory(sdkCategory: Int): HealthDataCategory {
    return when (sdkCategory) {
        sdkHealthDataCategory.ACTIVITY -> HealthDataCategory.ACTIVITY
        sdkHealthDataCategory.BODY_MEASUREMENTS -> HealthDataCategory.BODY_MEASUREMENTS
        sdkHealthDataCategory.CYCLE_TRACKING -> HealthDataCategory.CYCLE_TRACKING
        sdkHealthDataCategory.NUTRITION -> HealthDataCategory.NUTRITION
        sdkHealthDataCategory.SLEEP -> HealthDataCategory.SLEEP
        sdkHealthDataCategory.VITALS -> HealthDataCategory.VITALS
        else -> throw IllegalArgumentException("Category is not supported.")
    }
}

/** Represents Category group for HealthConnect data in All Categories screen. */
data class AllCategoriesScreenHealthDataCategory(
    val category: HealthDataCategory,
    val noData: Boolean
)

/** Permission groups for each {@link HealthDataCategory}. */
private object CategoriesMappers {
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
            HealthPermissionType.INTERMENSTRUAL_BLEEDING,
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

/** List of available Health data categories. */
val HEALTH_DATA_CATEGORIES =
    listOf(
        HealthDataCategory.ACTIVITY,
        HealthDataCategory.BODY_MEASUREMENTS,
        HealthDataCategory.CYCLE_TRACKING,
        HealthDataCategory.NUTRITION,
        HealthDataCategory.SLEEP,
        HealthDataCategory.VITALS,
    )
