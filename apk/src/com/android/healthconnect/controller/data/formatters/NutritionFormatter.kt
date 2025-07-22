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
package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.MealType
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Mass
import android.icu.text.MessageFormat.*
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.formatters.EnergyFormatter.formatEnergyA11yValue
import com.android.healthconnect.controller.data.formatters.EnergyFormatter.formatEnergyValue
import com.android.healthconnect.controller.data.formatters.MealFormatter.formatMealType
import com.android.healthconnect.controller.data.formatters.shared.EntryFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.doubleEquals
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.StringJoiner
import javax.inject.Inject

/** Formatter for printing NutritionRecord data. */
class NutritionFormatter
@Inject
constructor(
    @ApplicationContext context: Context,
    timeFormatter: LocalDateTimeFormatter,
    unitPreferences: UnitPreferences,
) : EntryFormatter<NutritionRecord>(context, timeFormatter, unitPreferences) {

    override suspend fun formatValue(record: NutritionRecord): String {
        val nutritionData =
            getAggregations(
                record,
                { mass ->
                    val grams = mass.inGrams
                    format(context.getString(R.string.gram_short_format), mapOf("count" to grams))
                },
                { energy -> formatEnergyValue(context, energy, unitPreferences) },
            )
        return nutritionData.ifEmpty { "-" }
    }

    override suspend fun formatA11yValue(record: NutritionRecord): String {
        val nutritionData =
            getAggregations(
                record,
                { mass ->
                    val grams = mass.inGrams
                    format(context.getString(R.string.gram_long_format), mapOf("count" to grams))
                },
                { energy -> formatEnergyA11yValue(context, energy, unitPreferences) },
            )
        return nutritionData.ifEmpty { "-" }
    }

    private fun getAggregations(
        record: NutritionRecord,
        formatMass: (mass: Mass) -> String,
        formatEnergy: (energy: Energy) -> String,
    ): String {
        val stringJoiner = StringJoiner("\n")
        record.mealName?.run { stringJoiner.addAggregation(R.string.meal_name, this) }
        if (record.mealType != MealType.MEAL_TYPE_UNKNOWN) {
            stringJoiner.addAggregation(
                R.string.mealtype_label,
                formatMealType(context, record.mealType),
            )
        }
        record.biotin?.addAggregationIfSet(R.string.biotin, stringJoiner, formatMass)
        record.caffeine?.addAggregationIfSet(R.string.caffeine, stringJoiner, formatMass)
        record.calcium?.addAggregationIfSet(R.string.calcium, stringJoiner, formatMass)
        record.chloride?.addAggregationIfSet(R.string.chloride, stringJoiner, formatMass)
        record.cholesterol?.addAggregationIfSet(R.string.cholesterol, stringJoiner, formatMass)
        record.chromium?.addAggregationIfSet(R.string.chromium, stringJoiner, formatMass)
        record.copper?.addAggregationIfSet(R.string.copper, stringJoiner, formatMass)
        record.dietaryFiber?.addAggregationIfSet(R.string.dietary_fiber, stringJoiner, formatMass)
        record.energy?.addAggregationIfSet(
            R.string.energy_consumed_total,
            stringJoiner,
            formatEnergy,
        )
        record.energyFromFat?.addAggregationIfSet(
            R.string.energy_consumed_from_fat,
            stringJoiner,
            formatEnergy,
        )
        record.folate?.addAggregationIfSet(R.string.folate, stringJoiner, formatMass)
        record.folicAcid?.addAggregationIfSet(R.string.folic_acid, stringJoiner, formatMass)
        record.iodine?.addAggregationIfSet(R.string.iodine, stringJoiner, formatMass)
        record.iron?.addAggregationIfSet(R.string.iron, stringJoiner, formatMass)
        record.magnesium?.addAggregationIfSet(R.string.magnesium, stringJoiner, formatMass)
        record.manganese?.addAggregationIfSet(R.string.manganese, stringJoiner, formatMass)
        record.molybdenum?.addAggregationIfSet(R.string.molybdenum, stringJoiner, formatMass)
        record.monounsaturatedFat?.addAggregationIfSet(
            R.string.monounsaturated_fat,
            stringJoiner,
            formatMass,
        )
        record.niacin?.addAggregationIfSet(R.string.niacin, stringJoiner, formatMass)
        record.pantothenicAcid?.addAggregationIfSet(
            R.string.pantothenic_acid,
            stringJoiner,
            formatMass,
        )
        record.phosphorus?.addAggregationIfSet(R.string.phosphorus, stringJoiner, formatMass)
        record.polyunsaturatedFat?.addAggregationIfSet(
            R.string.polyunsaturated_fat,
            stringJoiner,
            formatMass,
        )
        record.potassium?.addAggregationIfSet(R.string.potassium, stringJoiner, formatMass)
        record.protein?.addAggregationIfSet(R.string.protein, stringJoiner, formatMass)
        record.riboflavin?.addAggregationIfSet(R.string.riboflavin, stringJoiner, formatMass)
        record.saturatedFat?.addAggregationIfSet(R.string.saturated_fat, stringJoiner, formatMass)
        record.selenium?.addAggregationIfSet(R.string.selenium, stringJoiner, formatMass)
        record.sodium?.addAggregationIfSet(R.string.sodium, stringJoiner, formatMass)
        record.sugar?.addAggregationIfSet(R.string.sugar, stringJoiner, formatMass)
        record.thiamin?.addAggregationIfSet(R.string.thiamin, stringJoiner, formatMass)
        record.totalCarbohydrate?.addAggregationIfSet(
            R.string.total_carbohydrate,
            stringJoiner,
            formatMass,
        )
        record.totalFat?.addAggregationIfSet(R.string.total_fat, stringJoiner, formatMass)
        record.transFat?.addAggregationIfSet(R.string.trans_fat, stringJoiner, formatMass)
        record.unsaturatedFat?.addAggregationIfSet(
            R.string.unsaturated_fat,
            stringJoiner,
            formatMass,
        )
        record.vitaminA?.addAggregationIfSet(R.string.vitamin_a, stringJoiner, formatMass)
        record.vitaminB12?.addAggregationIfSet(R.string.vitamin_b12, stringJoiner, formatMass)
        record.vitaminB6?.addAggregationIfSet(R.string.vitamin_b6, stringJoiner, formatMass)
        record.vitaminC?.addAggregationIfSet(R.string.vitamin_c, stringJoiner, formatMass)
        record.vitaminD?.addAggregationIfSet(R.string.vitamin_d, stringJoiner, formatMass)
        record.vitaminE?.addAggregationIfSet(R.string.vitamin_e, stringJoiner, formatMass)
        record.vitaminK?.addAggregationIfSet(R.string.vitamin_k, stringJoiner, formatMass)
        record.zinc?.addAggregationIfSet(R.string.zinc, stringJoiner, formatMass)

        return stringJoiner.toString()
    }

    private fun StringJoiner.addAggregation(@StringRes labelRes: Int, value: String) {
        val label = context.getString(labelRes)
        add(context.getString(R.string.nutrient_with_value, label, value))
    }

    private fun Mass.addAggregationIfSet(
        @StringRes labelRes: Int,
        stringJoiner: StringJoiner,
        formatMass: (mass: Mass) -> String,
    ) {
        if (doubleEquals(this.inGrams, 0.0)) return
        stringJoiner.addAggregation(labelRes, formatMass(this))
    }

    private fun Energy.addAggregationIfSet(
        @StringRes labelRes: Int,
        stringJoiner: StringJoiner,
        formatEnergy: (energy: Energy) -> String,
    ) {
        if (doubleEquals(this.inCalories, 0.0)) return
        stringJoiner.addAggregation(labelRes, formatEnergy(this))
    }
}
