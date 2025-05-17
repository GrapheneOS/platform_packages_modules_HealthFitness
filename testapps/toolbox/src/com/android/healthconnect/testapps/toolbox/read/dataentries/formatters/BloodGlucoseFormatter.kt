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
import android.health.connect.datatypes.BloodGlucoseRecord
import android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_AFTER_MEAL
import android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_BEFORE_MEAL
import android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_FASTING
import android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_GENERAL
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_CAPILLARY_BLOOD
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_INTERSTITIAL_FLUID
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_PLASMA
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_SERUM
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_TEARS
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_WHOLE_BLOOD
import android.health.connect.datatypes.units.BloodGlucose
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.formatters.shared.MealTypeFormatter
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatBloodGlucose
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class BloodGlucoseFormatter {

    fun format(record: BloodGlucoseRecord, context: Context): FormattedDataDetails {
        return FormattedDataDetails(
            dataEntry = formatDataEntry(record, context),
            dataDetails = formatDataDetails(record, context),
        )
    }

    private fun formatDataEntry(record: BloodGlucoseRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(
            header = getHeader(record),
            value = formatLevel(record.level, context),
        )
    }

    private fun formatDataDetails(
        record: BloodGlucoseRecord,
        context: Context,
    ): List<FormattedEntry> {
        val formattedEntries = mutableListOf<FormattedEntry>()
        val mealTypeFormatter = MealTypeFormatter()
        val mealType = mealTypeFormatter.format(record.mealType, context)
        val specimenSource = formatSpecimenSource(record.specimenSource, context)
        val relationToMeal = formatRelationToMeal(record.relationToMeal, context)

        if (!relationToMeal.isNullOrEmpty()) {
            formattedEntries.add(
                FormattedDataEntry(
                    header = context.getString(R.string.relation),
                    value = relationToMeal,
                )
            )
        }
        if (!mealType.isNullOrEmpty()) {
            formattedEntries.add(
                FormattedDataEntry(header = context.getString(R.string.type), value = mealType)
            )
        }
        if (!specimenSource.isNullOrEmpty()) {
            formattedEntries.add(
                FormattedDataEntry(
                    header = context.getString(R.string.source),
                    value = specimenSource,
                )
            )
        }
        return formattedEntries
    }

    private fun formatRelationToMeal(relation: Int, context: Context): String? {
        return when (relation) {
            RELATION_TO_MEAL_AFTER_MEAL -> context.getString(R.string.relation_to_meal_after_meal)
            RELATION_TO_MEAL_BEFORE_MEAL -> context.getString(R.string.relation_to_meal_before_meal)
            RELATION_TO_MEAL_FASTING -> context.getString(R.string.relation_to_meal_fasting)
            RELATION_TO_MEAL_GENERAL -> context.getString(R.string.relation_to_meal_general)
            else -> null
        }
    }

    private fun formatSpecimenSource(source: Int, context: Context): String? {
        return when (source) {
            SPECIMEN_SOURCE_CAPILLARY_BLOOD ->
                context.getString(R.string.specimen_source_capillary_blood)
            SPECIMEN_SOURCE_INTERSTITIAL_FLUID ->
                context.getString(R.string.specimen_source_interstitial_fluid)
            SPECIMEN_SOURCE_PLASMA -> context.getString(R.string.specimen_source_plasma)
            SPECIMEN_SOURCE_SERUM -> context.getString(R.string.specimen_source_serum)
            SPECIMEN_SOURCE_TEARS -> context.getString(R.string.specimen_source_tears)
            SPECIMEN_SOURCE_WHOLE_BLOOD -> context.getString(R.string.specimen_source_whole_blood)
            else -> null
        }
    }

    private fun formatLevel(level: BloodGlucose, context: Context): String {
        return formatBloodGlucose(level, context)
    }
}
