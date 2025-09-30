/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.shared

import android.content.Context
import android.graphics.drawable.Drawable
import android.health.connect.HealthDataCategory
import android.health.connect.internal.datatypes.utils.HealthConnectMappings
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.fromHealthPermissionCategory
import com.android.healthconnect.controller.shared.CategoriesMappers.ACTIVITY_PERMISSION_GROUPS
import com.android.healthconnect.controller.shared.CategoriesMappers.BODY_MEASUREMENTS_PERMISSION_GROUPS
import com.android.healthconnect.controller.shared.CategoriesMappers.CYCLE_TRACKING_PERMISSION_GROUPS
import com.android.healthconnect.controller.shared.CategoriesMappers.NUTRITION_PERMISSION_GROUPS
import com.android.healthconnect.controller.shared.CategoriesMappers.SLEEP_PERMISSION_GROUPS
import com.android.healthconnect.controller.shared.CategoriesMappers.VITALS_PERMISSION_GROUPS
import com.android.healthconnect.controller.shared.CategoriesMappers.WELLNESS_PERMISSION_GROUPS
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale

object HealthDataCategoryExtensions {
    /** Additional category for medical permission types. */
    const val MEDICAL = 1000

    private val DATA_CATEGORY_TO_HEALTH_PERMISSION_TYPE_MAP =
        createDataCategoryToHealthPermissionTypeMap()

    private fun createDataCategoryToHealthPermissionTypeMap():
        Map<Int, List<HealthPermissionType>> {

        val specialCases =
            mapOf(
                HealthDataCategory.ACTIVITY to listOf(FitnessPermissionType.EXERCISE_ROUTE),
                MEDICAL to MedicalPermissionType.entries,
            )

        val healthConnectMappings = HealthConnectMappings.getInstance()

        return healthConnectMappings.allRecordTypeIdentifiers
            .flatMap { recordTypeId ->
                val dataCategory =
                    healthConnectMappings.getRecordCategoryForRecordType(recordTypeId)
                val permissionCategories =
                    healthConnectMappings.getHealthPermissionCategoriesForRecordType(recordTypeId)
                permissionCategories.map { permissionCategory ->
                    dataCategory to fromHealthPermissionCategory(permissionCategory)
                }
            }
            .groupBy({ it.first }, { it.second })
            .toMutableMap()
            .apply { specialCases.forEach { merge(it.key, it.value) { a, b -> a + b } } }
            .mapValues { it.value.distinct() }
            .toMap()
    }

    fun @receiver:HealthDataCategoryInt Int.healthPermissionTypes(): List<HealthPermissionType> {
        return DATA_CATEGORY_TO_HEALTH_PERMISSION_TYPE_MAP[this]
            ?: throw IllegalArgumentException("Category $this is not supported.")
    }

    private fun @receiver:HealthDataCategoryInt Int.healthPermissionTypesLegacy():
        List<HealthPermissionType> {
        return when (this) {
            HealthDataCategory.ACTIVITY -> ACTIVITY_PERMISSION_GROUPS
            HealthDataCategory.BODY_MEASUREMENTS -> BODY_MEASUREMENTS_PERMISSION_GROUPS
            HealthDataCategory.CYCLE_TRACKING -> CYCLE_TRACKING_PERMISSION_GROUPS
            HealthDataCategory.NUTRITION -> NUTRITION_PERMISSION_GROUPS
            HealthDataCategory.SLEEP -> SLEEP_PERMISSION_GROUPS
            HealthDataCategory.VITALS -> VITALS_PERMISSION_GROUPS
            HealthDataCategory.WELLNESS -> WELLNESS_PERMISSION_GROUPS
            MEDICAL -> MedicalPermissionType.entries
            else -> throw IllegalArgumentException("Category $this is not supported.")
        }
    }

    @StringRes
    fun @receiver:HealthDataCategoryInt Int.lowercaseTitle(): Int {
        return when (this) {
            HealthDataCategory.ACTIVITY -> R.string.activity_category_lowercase
            HealthDataCategory.BODY_MEASUREMENTS -> R.string.body_measurements_category_lowercase
            HealthDataCategory.CYCLE_TRACKING -> R.string.cycle_tracking_category_lowercase
            HealthDataCategory.NUTRITION -> R.string.nutrition_category_lowercase
            HealthDataCategory.SLEEP -> R.string.sleep_category_lowercase
            HealthDataCategory.VITALS -> R.string.vitals_category_lowercase
            HealthDataCategory.WELLNESS -> R.string.wellness_category_lowercase
            HealthDataCategory.SYMPTOMS -> R.string.symptoms_category_lowercase
            MEDICAL -> R.string.medical_permissions_lowercase
            else -> throw IllegalArgumentException("Category $this is not supported.")
        }
    }

    @StringRes
    fun @receiver:HealthDataCategoryInt Int.uppercaseTitle(): Int {
        return when (this) {
            HealthDataCategory.ACTIVITY -> R.string.activity_category_uppercase
            HealthDataCategory.BODY_MEASUREMENTS -> R.string.body_measurements_category_uppercase
            HealthDataCategory.CYCLE_TRACKING -> R.string.cycle_tracking_category_uppercase
            HealthDataCategory.NUTRITION -> R.string.nutrition_category_uppercase
            HealthDataCategory.SLEEP -> R.string.sleep_category_uppercase
            HealthDataCategory.VITALS -> R.string.vitals_category_uppercase
            HealthDataCategory.WELLNESS -> R.string.wellness_category_uppercase
            HealthDataCategory.SYMPTOMS -> R.string.symptoms_category_uppercase
            MEDICAL -> R.string.medical_permissions
            else -> throw IllegalArgumentException("Category $this is not supported.")
        }
    }

    fun @receiver:HealthDataCategoryInt Int.icon(context: Context): Drawable? {
        val attrRes: Int =
            when (this) {
                HealthDataCategory.ACTIVITY -> R.attr.activityCategoryIcon
                HealthDataCategory.BODY_MEASUREMENTS -> R.attr.bodyMeasurementsCategoryIcon
                HealthDataCategory.CYCLE_TRACKING -> R.attr.cycleTrackingCategoryIcon
                HealthDataCategory.NUTRITION -> R.attr.nutritionCategoryIcon
                HealthDataCategory.SLEEP -> R.attr.sleepCategoryIcon
                HealthDataCategory.VITALS -> R.attr.vitalsCategoryIcon
                HealthDataCategory.WELLNESS -> R.attr.wellnessCategoryIcon
                HealthDataCategory.SYMPTOMS -> R.attr.symptomsCategoryIcon
                // TODO(b/342156345): Add default medical icon.
                MEDICAL -> R.attr.vitalsCategoryIcon
                else -> throw IllegalArgumentException("Category $this is not supported.")
            }
        return AttributeResolver.getDrawable(context, attrRes)
    }

    @HealthDataCategoryInt
    fun fromFitnessPermissionType(type: FitnessPermissionType): Int {
        val result = safelyFromFitnessPermissionType(type)
        return result
            ?: throw IllegalArgumentException("No Category for fitness permission type $type")
    }

    @HealthDataCategoryInt
    fun safelyFromFitnessPermissionType(type: FitnessPermissionType): Int? {
        return getAllFitnessDataCategories().firstOrNull {
            it.healthPermissionTypes().contains(type)
        }
    }

    fun getSortedDataCategoryToStringMap(context: Context): Map<Int, String> {
        return DATA_CATEGORY_TO_HEALTH_PERMISSION_TYPE_MAP.keys
            .mapNotNull { category ->
                try {
                    category to context.getString(category.uppercaseTitle())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            .sortByLocale { (_, value) -> value }
            .toMap()
    }
}

/** Permission groups for each {@link HealthDataCategory}. */
private object CategoriesMappers {
    val ACTIVITY_PERMISSION_GROUPS =
        listOf(
            FitnessPermissionType.ACTIVE_CALORIES_BURNED,
            FitnessPermissionType.DISTANCE,
            FitnessPermissionType.ELEVATION_GAINED,
            FitnessPermissionType.EXERCISE,
            FitnessPermissionType.EXERCISE_ROUTE,
            FitnessPermissionType.FLOORS_CLIMBED,
            FitnessPermissionType.POWER,
            FitnessPermissionType.SPEED,
            FitnessPermissionType.STEPS,
            FitnessPermissionType.TOTAL_CALORIES_BURNED,
            FitnessPermissionType.VO2_MAX,
            FitnessPermissionType.WHEELCHAIR_PUSHES,
            FitnessPermissionType.PLANNED_EXERCISE,
        )

    val BODY_MEASUREMENTS_PERMISSION_GROUPS =
        listOf(
            FitnessPermissionType.BASAL_METABOLIC_RATE,
            FitnessPermissionType.BODY_FAT,
            FitnessPermissionType.BODY_WATER_MASS,
            FitnessPermissionType.BONE_MASS,
            FitnessPermissionType.HEIGHT,
            FitnessPermissionType.LEAN_BODY_MASS,
            FitnessPermissionType.WEIGHT,
        )

    val CYCLE_TRACKING_PERMISSION_GROUPS =
        listOf(
            FitnessPermissionType.CERVICAL_MUCUS,
            FitnessPermissionType.INTERMENSTRUAL_BLEEDING,
            FitnessPermissionType.MENSTRUATION,
            FitnessPermissionType.OVULATION_TEST,
            FitnessPermissionType.SEXUAL_ACTIVITY,
        )

    val NUTRITION_PERMISSION_GROUPS =
        listOf(FitnessPermissionType.HYDRATION, FitnessPermissionType.NUTRITION)

    val SLEEP_PERMISSION_GROUPS = listOf(FitnessPermissionType.SLEEP)

    val VITALS_PERMISSION_GROUPS =
        listOf(
            FitnessPermissionType.BASAL_BODY_TEMPERATURE,
            FitnessPermissionType.BLOOD_GLUCOSE,
            FitnessPermissionType.BLOOD_PRESSURE,
            FitnessPermissionType.BODY_TEMPERATURE,
            FitnessPermissionType.HEART_RATE,
            FitnessPermissionType.HEART_RATE_VARIABILITY,
            FitnessPermissionType.OXYGEN_SATURATION,
            FitnessPermissionType.RESPIRATORY_RATE,
            FitnessPermissionType.RESTING_HEART_RATE,
            FitnessPermissionType.SKIN_TEMPERATURE,
        )

    val WELLNESS_PERMISSION_GROUPS =
        listOf(FitnessPermissionType.MINDFULNESS, FitnessPermissionType.ALCOHOL_CONSUMPTION)
}

/** List of available Health data categories. */
val FITNESS_DATA_CATEGORIES = getAllFitnessDataCategories()

/**
 * List of available Health data categories.
 *
 * Allows code being unit tested with different flag values.
 */
fun getAllFitnessDataCategories() = HealthConnectMappings.getInstance().allHealthDataCategories

/** Denotes that the annotated [Integer] represents a [HealthDataCategory]. */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FIELD,
    AnnotationTarget.TYPE,
)
annotation class HealthDataCategoryInt
