/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Mass
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedNutritionEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.formatters.shared.MealTypeFormatter
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.Unit
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatEnergy
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatMass
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader
import java.util.StringJoiner

class NutritionFormatter {

    fun format(record: NutritionRecord, context: Context): FormattedNutritionEntry {
        val mealTypeFormatter = MealTypeFormatter()
        return FormattedNutritionEntry(
            header = getHeader(record),
            value = record.mealName ?: context.getString(R.string.meal),
            mealType = mealTypeFormatter.format(record.mealType, context),
            samples = getNutritionalData(record, context),
        )
    }

    private fun getNutritionalData(record: NutritionRecord, context: Context): String {

        val stringJoiner = StringJoiner("\n")
        val unit = Unit.Mass.GRAMS

        record.biotin?.addMetric(
            context.getString(R.string.nutrition_biotin),
            stringJoiner,
            unit,
            context,
        )
        record.caffeine?.addMetric(
            context.getString(R.string.nutrition_caffeine),
            stringJoiner,
            unit,
            context,
        )
        record.calcium?.addMetric(
            context.getString(R.string.nutrition_calcium),
            stringJoiner,
            unit,
            context,
        )
        record.chloride?.addMetric(
            context.getString(R.string.nutrition_chloride),
            stringJoiner,
            unit,
            context,
        )
        record.cholesterol?.addMetric(
            context.getString(R.string.nutrition_cholesterol),
            stringJoiner,
            unit,
            context,
        )
        record.chromium?.addMetric(
            context.getString(R.string.nutrition_chromium),
            stringJoiner,
            unit,
            context,
        )
        record.copper?.addMetric(
            context.getString(R.string.nutrition_copper),
            stringJoiner,
            unit,
            context,
        )
        record.dietaryFiber?.addMetric(
            context.getString(R.string.nutrition_dietary_fiber),
            stringJoiner,
            unit,
            context,
        )
        record.energyFromFat?.addMetric(
            context.getString(R.string.nutrition_energy_from_fat),
            stringJoiner,
            context,
        )
        record.energy?.addMetric(
            context.getString(R.string.nutrition_energy),
            stringJoiner,
            context,
        )
        record.folate?.addMetric(
            context.getString(R.string.nutrition_folate),
            stringJoiner,
            unit,
            context,
        )
        record.folicAcid?.addMetric(
            context.getString(R.string.nutrition_folic_acid),
            stringJoiner,
            unit,
            context,
        )
        record.iodine?.addMetric(
            context.getString(R.string.nutrition_iodine),
            stringJoiner,
            unit,
            context,
        )
        record.iron?.addMetric(
            context.getString(R.string.nutrition_iron),
            stringJoiner,
            unit,
            context,
        )
        record.magnesium?.addMetric(
            context.getString(R.string.nutrition_magnesium),
            stringJoiner,
            unit,
            context,
        )
        record.manganese?.addMetric(
            context.getString(R.string.nutrition_manganese),
            stringJoiner,
            unit,
            context,
        )
        record.molybdenum?.addMetric(
            context.getString(R.string.nutrition_molybdenum),
            stringJoiner,
            unit,
            context,
        )
        record.monounsaturatedFat?.addMetric(
            context.getString(R.string.nutrition_monounsaturated_fat),
            stringJoiner,
            unit,
            context,
        )
        record.niacin?.addMetric(
            context.getString(R.string.nutrition_niacin),
            stringJoiner,
            unit,
            context,
        )
        record.pantothenicAcid?.addMetric(
            context.getString(R.string.nutrition_pantothenic_acid),
            stringJoiner,
            unit,
            context,
        )
        record.phosphorus?.addMetric(
            context.getString(R.string.nutrition_phosphorus),
            stringJoiner,
            unit,
            context,
        )
        record.polyunsaturatedFat?.addMetric(
            context.getString(R.string.nutrition_polyunsaturated_fat),
            stringJoiner,
            unit,
            context,
        )
        record.potassium?.addMetric(
            context.getString(R.string.nutrition_potassium),
            stringJoiner,
            unit,
            context,
        )
        record.protein?.addMetric(
            context.getString(R.string.nutrition_protein),
            stringJoiner,
            unit,
            context,
        )
        record.riboflavin?.addMetric(
            context.getString(R.string.nutrition_riboflavin),
            stringJoiner,
            unit,
            context,
        )
        record.saturatedFat?.addMetric(
            context.getString(R.string.nutrition_saturated_fat),
            stringJoiner,
            unit,
            context,
        )
        record.selenium?.addMetric(
            context.getString(R.string.nutrition_selenium),
            stringJoiner,
            unit,
            context,
        )
        record.sodium?.addMetric(
            context.getString(R.string.nutrition_sodium),
            stringJoiner,
            unit,
            context,
        )
        record.sugar?.addMetric(
            context.getString(R.string.nutrition_sugar),
            stringJoiner,
            unit,
            context,
        )
        record.thiamin?.addMetric(
            context.getString(R.string.nutrition_thiamin),
            stringJoiner,
            unit,
            context,
        )
        record.totalCarbohydrate?.addMetric(
            context.getString(R.string.nutrition_total_carbohydrate),
            stringJoiner,
            unit,
            context,
        )
        record.totalFat?.addMetric(
            context.getString(R.string.nutrition_total_fat),
            stringJoiner,
            unit,
            context,
        )
        record.transFat?.addMetric(
            context.getString(R.string.nutrition_trans_fat),
            stringJoiner,
            unit,
            context,
        )
        record.unsaturatedFat?.addMetric(
            context.getString(R.string.nutrition_unsaturated_fat),
            stringJoiner,
            unit,
            context,
        )
        record.vitaminA?.addMetric(
            context.getString(R.string.nutrition_vitamin_a),
            stringJoiner,
            unit,
            context,
        )
        record.vitaminB12?.addMetric(
            context.getString(R.string.nutrition_vitamin_b12),
            stringJoiner,
            unit,
            context,
        )
        record.vitaminB6?.addMetric(
            context.getString(R.string.nutrition_vitamin_b6),
            stringJoiner,
            unit,
            context,
        )
        record.vitaminC?.addMetric(
            context.getString(R.string.nutrition_vitamin_c),
            stringJoiner,
            unit,
            context,
        )
        record.vitaminD?.addMetric(
            context.getString(R.string.nutrition_vitamin_d),
            stringJoiner,
            unit,
            context,
        )
        record.vitaminE?.addMetric(
            context.getString(R.string.nutrition_vitamin_e),
            stringJoiner,
            unit,
            context,
        )
        record.vitaminK?.addMetric(
            context.getString(R.string.nutrition_vitamin_k),
            stringJoiner,
            unit,
            context,
        )
        record.zinc?.addMetric(
            context.getString(R.string.nutrition_zinc),
            stringJoiner,
            unit,
            context,
        )

        if (stringJoiner.length() == 0) {
            return context.getString(R.string.no_nutritional_data_recorded)
        }
        return stringJoiner.toString()
    }

    private fun Energy.addMetric(label: String, stringJoiner: StringJoiner, context: Context) {
        if (this > Energy.fromCalories(0.01)) {
            stringJoiner.add("$label ${formatEnergy(this, context)}")
        } else {
            return
        }
    }

    private fun Mass.addMetric(
        label: String,
        stringJoiner: StringJoiner,
        unit: Unit.Mass,
        context: Context,
    ) {
        if (this > Mass.fromGrams(0.01)) {
            stringJoiner.add("$label ${formatMass(this,unit, context)}")
        } else {
            return
        }
    }
}
