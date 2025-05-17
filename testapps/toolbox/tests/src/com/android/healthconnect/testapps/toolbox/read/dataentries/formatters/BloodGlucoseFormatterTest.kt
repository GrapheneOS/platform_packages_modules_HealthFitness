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
import android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_UNKNOWN
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_CAPILLARY_BLOOD
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_INTERSTITIAL_FLUID
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_PLASMA
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_SERUM
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_TEARS
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_UNKNOWN
import android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_WHOLE_BLOOD
import android.health.connect.datatypes.MealType.MEAL_TYPE_UNKNOWN
import android.health.connect.datatypes.units.BloodGlucose
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BloodGlucoseFormatterTest {
    private val formatter = BloodGlucoseFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatBloodGlucoseValue_empty_returnsFormattedEntry() {
        val record = getBloodGlucoseRecordBuilder(12.222).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails).isEmpty()
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_specimenSource_capillaryBlood() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, specimenSource = SPECIMEN_SOURCE_CAPILLARY_BLOOD)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(
                    FormattedEntry.FormattedDataEntry(header = "Source", value = "Capillary blood")
                )
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_specimenSource_interstitialFluid() {
        val record =
            getBloodGlucoseRecordBuilder(
                    12.222,
                    specimenSource = SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                )
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(
                    FormattedEntry.FormattedDataEntry(
                        header = "Source",
                        value = "Interstitial fluid",
                    )
                )
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_specimenSource_plasma() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, specimenSource = SPECIMEN_SOURCE_PLASMA).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Source", value = "Plasma"))
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_specimenSource_serum() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, specimenSource = SPECIMEN_SOURCE_SERUM).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Source", value = "Serum"))
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_specimenSource_tears() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, specimenSource = SPECIMEN_SOURCE_TEARS).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Source", value = "Tears"))
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_specimenSource_wholeBlood() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, specimenSource = SPECIMEN_SOURCE_WHOLE_BLOOD)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Source", value = "Whole blood"))
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_mealRelation_after() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, relationToMeal = RELATION_TO_MEAL_AFTER_MEAL)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Relation", value = "After"))
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_mealRelation_before() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, relationToMeal = RELATION_TO_MEAL_BEFORE_MEAL)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Relation", value = "Before"))
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_mealRelation_fasting() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, relationToMeal = RELATION_TO_MEAL_FASTING).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Relation", value = "Fasting"))
            )
    }

    @Test
    fun formatBloodGlucoseValue_returnsFormattedEntry_mealRelation_general() {
        val record =
            getBloodGlucoseRecordBuilder(12.222, relationToMeal = RELATION_TO_MEAL_GENERAL).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("12.222 mmol/L")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(FormattedEntry.FormattedDataEntry(header = "Relation", value = "General"))
            )
    }

    private fun getBloodGlucoseRecordBuilder(
        level: Double,
        specimenSource: Int = SPECIMEN_SOURCE_UNKNOWN,
        relationToMeal: Int = RELATION_TO_MEAL_UNKNOWN,
        mealType: Int = MEAL_TYPE_UNKNOWN,
    ): BloodGlucoseRecord.Builder {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return BloodGlucoseRecord.Builder(
            getMetaData(context),
            NOW,
            specimenSource,
            BloodGlucose.fromMillimolesPerLiter(level),
            relationToMeal,
            mealType,
        )
    }
}
