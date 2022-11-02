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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R

/** Represents Category group for HealthConnect data. */
enum class HealthDataCategory(@StringRes val title: Int, @DrawableRes val icon: Int) {
    ACTIVITY(R.string.activity_category, R.drawable.quantum_gm_ic_directions_run_vd_theme_24),
    BODY_MEASUREMENTS(
        R.string.body_measurements_category, R.drawable.quantum_gm_ic_straighten_vd_theme_24),
    SLEEP(R.string.sleep_category, R.drawable.ic_sleep),
    VITALS(R.string.vitals_category, R.drawable.ic_vitals),
    CYCLE_TRACKING(R.string.cycle_tracking_category, R.drawable.ic_cycle_tracking),
    NUTRITION(R.string.nutrition_category, R.drawable.quantum_gm_ic_grocery_vd_theme_24),
}

/** Represents Category group for HealthConnect data in All Categories screen. */
enum class AllCategoriesScreenHealthDataCategory(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val noData: Boolean
) {
    ACTIVITY(
        R.string.activity_category, R.drawable.quantum_gm_ic_directions_run_vd_theme_24, false),
    BODY_MEASUREMENTS(
        R.string.body_measurements_category,
        R.drawable.quantum_gm_ic_straighten_vd_theme_24,
        false),
    SLEEP(R.string.sleep_category, R.drawable.ic_sleep, true),
    VITALS(R.string.vitals_category, R.drawable.ic_vitals, true),
    CYCLE_TRACKING(R.string.cycle_tracking_category, R.drawable.ic_cycle_tracking, false),
    NUTRITION(R.string.nutrition_category, R.drawable.quantum_gm_ic_grocery_vd_theme_24, false),
}

/** List of available Health data categories. */
val HEALTH_DATA_CATEGORIES =
    listOf(
        HealthDataCategory.ACTIVITY,
        HealthDataCategory.BODY_MEASUREMENTS,
        HealthDataCategory.NUTRITION,
        HealthDataCategory.CYCLE_TRACKING,
    )

/** List of all Health data categories. */
val HEALTH_DATA_ALL_CATEGORIES =
    listOf(
        AllCategoriesScreenHealthDataCategory.ACTIVITY,
        AllCategoriesScreenHealthDataCategory.BODY_MEASUREMENTS,
        AllCategoriesScreenHealthDataCategory.SLEEP,
        AllCategoriesScreenHealthDataCategory.VITALS,
        AllCategoriesScreenHealthDataCategory.CYCLE_TRACKING,
        AllCategoriesScreenHealthDataCategory.NUTRITION,
    )
