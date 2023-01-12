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
package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.healthconnect.datatypes.MealType
import android.healthconnect.datatypes.NutritionRecord
import android.healthconnect.datatypes.units.Energy
import android.healthconnect.datatypes.units.Mass
import android.icu.text.MessageFormat.*
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.formatters.EnergyFormatter.formatEnergyA11yValue
import com.android.healthconnect.controller.dataentries.formatters.EnergyFormatter.formatEnergyValue
import com.android.healthconnect.controller.dataentries.formatters.MealFormatter.formatMealType
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.StringJoiner
import javax.inject.Inject

/** Formatter for printing NutritionRecord data. */
class NutritionFormatter @Inject constructor(@ApplicationContext private val context: Context) :
    DataEntriesFormatter<NutritionRecord>(context) {

    override suspend fun formatValue(
        record: NutritionRecord,
        unitPreferences: UnitPreferences
    ): String {
        val nutritionData =
            getAggregations(
                record,
                { mass ->
                    val grams = mass.inKilograms * 1000.0
                    format(context.getString(R.string.gram_short_format), mapOf("count" to grams))
                },
                { energy -> formatEnergyValue(context, energy, unitPreferences) })
        return nutritionData.ifEmpty { "-" }
    }

    override suspend fun formatA11yValue(
        record: NutritionRecord,
        unitPreferences: UnitPreferences
    ): String {
        val nutritionData =
            getAggregations(
                record,
                { mass ->
                    val grams = mass.inKilograms * 1000.0
                    format(context.getString(R.string.gram_long_format), mapOf("count" to grams))
                },
                { energy -> formatEnergyA11yValue(context, energy, unitPreferences) })
        return nutritionData.ifEmpty { "-" }
    }

    private fun getAggregations(
        record: NutritionRecord,
        formatMass: (mass: Mass) -> String,
        formatEnergy: (energy: Energy) -> String,
    ): String {
        val stringJoiner = StringJoiner("\n")
        if (record.mealName != null) {
            stringJoiner.addAggregation(R.string.meal_name, record.mealName)
        }
        if (record.mealType != MealType.MEAL_TYPE_UNKNOWN) {
            stringJoiner.addAggregation(
                R.string.mealtype_label, formatMealType(context, record.mealType))
        }
        if (record.biotin != null) {
            stringJoiner.addAggregation(R.string.biotin, formatMass(record.biotin))
        }
        if (record.caffeine != null) {
            stringJoiner.addAggregation(R.string.caffeine, formatMass(record.caffeine))
        }
        if (record.calcium != null) {
            stringJoiner.addAggregation(R.string.calcium, formatMass(record.calcium))
        }
        if (record.chloride != null) {
            stringJoiner.addAggregation(R.string.chloride, formatMass(record.chloride))
        }
        if (record.cholesterol != null) {
            stringJoiner.addAggregation(R.string.cholesterol, formatMass(record.cholesterol))
        }
        if (record.chromium != null) {
            stringJoiner.addAggregation(R.string.chromium, formatMass(record.chromium))
        }
        if (record.copper != null) {
            stringJoiner.addAggregation(R.string.copper, formatMass(record.copper))
        }
        if (record.dietaryFiber != null) {
            stringJoiner.addAggregation(R.string.dietary_fiber, formatMass(record.dietaryFiber))
        }
        if (record.energy != null) {
            stringJoiner.addAggregation(R.string.energy_consumed_total, formatEnergy(record.energy))
        }
        if (record.energyFromFat != null) {
            stringJoiner.addAggregation(
                R.string.energy_consumed_from_fat, formatEnergy(record.energyFromFat))
        }
        if (record.folate != null) {
            stringJoiner.addAggregation(R.string.folate, formatMass(record.folate))
        }
        if (record.folicAcid != null) {
            stringJoiner.addAggregation(R.string.folic_acid, formatMass(record.folicAcid))
        }
        if (record.iodine != null) {
            stringJoiner.addAggregation(R.string.iodine, formatMass(record.iodine))
        }
        if (record.iron != null) {
            stringJoiner.addAggregation(R.string.iron, formatMass(record.iron))
        }
        if (record.magnesium != null) {
            stringJoiner.addAggregation(R.string.magnesium, formatMass(record.magnesium))
        }
        if (record.manganese != null) {
            stringJoiner.addAggregation(R.string.manganese, formatMass(record.manganese))
        }
        if (record.molybdenum != null) {
            stringJoiner.addAggregation(R.string.molybdenum, formatMass(record.molybdenum))
        }
        if (record.monounsaturatedFat != null) {
            stringJoiner.addAggregation(
                R.string.monounsaturated_fat, formatMass(record.monounsaturatedFat))
        }
        if (record.niacin != null) {
            stringJoiner.addAggregation(R.string.niacin, formatMass(record.niacin))
        }
        if (record.pantothenicAcid != null) {
            stringJoiner.addAggregation(
                R.string.pantothenic_acid, formatMass(record.pantothenicAcid))
        }
        if (record.phosphorus != null) {
            stringJoiner.addAggregation(R.string.phosphorus, formatMass(record.phosphorus))
        }
        if (record.polyunsaturatedFat != null) {
            stringJoiner.addAggregation(
                R.string.polyunsaturated_fat, formatMass(record.polyunsaturatedFat))
        }
        if (record.potassium != null) {
            stringJoiner.addAggregation(R.string.potassium, formatMass(record.potassium))
        }
        if (record.riboflavin != null) {
            stringJoiner.addAggregation(R.string.riboflavin, formatMass(record.riboflavin))
        }
        if (record.saturatedFat != null) {
            stringJoiner.addAggregation(R.string.saturated_fat, formatMass(record.saturatedFat))
        }
        if (record.selenium != null) {
            stringJoiner.addAggregation(R.string.selenium, formatMass(record.selenium))
        }
        if (record.sodium != null) {
            stringJoiner.addAggregation(R.string.sodium, formatMass(record.sodium))
        }
        if (record.sugar != null) {
            stringJoiner.addAggregation(R.string.sugar, formatMass(record.sugar))
        }
        if (record.thiamin != null) {
            stringJoiner.addAggregation(R.string.thiamin, formatMass(record.thiamin))
        }
        if (record.totalCarbohydrate != null) {
            stringJoiner.addAggregation(
                R.string.total_carbohydrate, formatMass(record.totalCarbohydrate))
        }
        if (record.totalFat != null) {
            stringJoiner.addAggregation(R.string.total_fat, formatMass(record.totalFat))
        }
        if (record.transFat != null) {
            stringJoiner.addAggregation(R.string.trans_fat, formatMass(record.transFat))
        }
        if (record.unsaturatedFat != null) {
            stringJoiner.addAggregation(R.string.unsaturated_fat, formatMass(record.unsaturatedFat))
        }
        if (record.vitaminA != null) {
            stringJoiner.addAggregation(R.string.vitamin_a, formatMass(record.vitaminA))
        }
        if (record.vitaminB12 != null) {
            stringJoiner.addAggregation(R.string.vitamin_b12, formatMass(record.vitaminB12))
        }
        if (record.vitaminB6 != null) {
            stringJoiner.addAggregation(R.string.vitamin_b6, formatMass(record.vitaminB6))
        }
        if (record.vitaminC != null) {
            stringJoiner.addAggregation(R.string.vitamin_c, formatMass(record.vitaminC))
        }
        if (record.vitaminD != null) {
            stringJoiner.addAggregation(R.string.vitamin_d, formatMass(record.vitaminD))
        }
        if (record.vitaminE != null) {
            stringJoiner.addAggregation(R.string.vitamin_e, formatMass(record.vitaminE))
        }
        if (record.vitaminK != null) {
            stringJoiner.addAggregation(R.string.vitamin_k, formatMass(record.vitaminK))
        }
        if (record.zinc != null) {
            stringJoiner.addAggregation(R.string.zinc, formatMass(record.zinc))
        }
        return stringJoiner.toString()
    }

    private fun StringJoiner.addAggregation(@StringRes labelRes: Int, value: String) {
        val label = context.getString(labelRes)
        add(context.getString(R.string.nutrient_with_value, label, value))
    }
}
