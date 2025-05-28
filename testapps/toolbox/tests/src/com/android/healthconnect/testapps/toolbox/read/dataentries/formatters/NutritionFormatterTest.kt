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
import android.health.connect.datatypes.MealType.MEAL_TYPE_LUNCH
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.units.Energy.fromCalories
import android.health.connect.datatypes.units.Mass.fromGrams
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NutritionFormatterTest {
    private val formatter = NutritionFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatNutritionValue_returnsFormattedEntry_empty() {
        val record = buildNutritionRecord().build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Meal")
        assertThat(formattedEntry.samples).isEqualTo("No nutritional data recorded")
    }

    @Test
    fun formatNutritionValue_returnsFormattedEntry_withMealType() {
        val record = buildNutritionRecord().setMealType(MEAL_TYPE_LUNCH).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.mealType).isEqualTo("Lunch")
    }

    @Test
    fun formatNutritionValue_returnsFormattedEntry_withTitle() {
        val record = buildNutritionRecord().setMealName("Test Meal Name").build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Test Meal Name")
    }

    @Test
    fun formatNutritionValue_returnsFormattedEntry_withDataMass() {
        val record = buildNutritionRecord().setIron(fromGrams(23.5)).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Meal")
        assertThat(formattedEntry.samples).isEqualTo("Iron 23.5 g")
    }

    @Test
    fun formatNutritionValue_returnsFormattedEntry_withDataEnergy() {
        val record = buildNutritionRecord().setEnergy(fromCalories(1200.0)).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Meal")
        assertThat(formattedEntry.samples).isEqualTo("Energy 2 cal")
    }

    @Test
    fun formatNutritionValue_returnsFormattedEntry_fullyPopulated() {
        val record =
            buildNutritionRecord()
                .setBiotin(fromGrams(4.0))
                .setCaffeine(fromGrams(12.0))
                .setCalcium(fromGrams(5.0))
                .setEnergyFromFat(fromCalories(30000.0))
                .setEnergy(fromCalories(30000.0))
                .setChloride(fromGrams(11.0))
                .setCholesterol(fromGrams(20.0))
                .setChromium(fromGrams(10.0))
                .setCopper(fromGrams(20.0))
                .setDietaryFiber(fromGrams(34.0))
                .setFolate(fromGrams(100.0))
                .setFolicAcid(fromGrams(90.0))
                .setIodine(fromGrams(100.0))
                .setIron(fromGrams(30.0))
                .setMagnesium(fromGrams(30.0))
                .setManganese(fromGrams(41.0))
                .setMolybdenum(fromGrams(2.0))
                .setMonounsaturatedFat(fromGrams(40.0))
                .setNiacin(fromGrams(20.0))
                .setPantothenicAcid(fromGrams(56.0))
                .setPhosphorus(fromGrams(67.0))
                .setPolyunsaturatedFat(fromGrams(67.0))
                .setPotassium(fromGrams(23.0))
                .setProtein(fromGrams(89.0))
                .setRiboflavin(fromGrams(22.0))
                .setSaturatedFat(fromGrams(45.0))
                .setSelenium(fromGrams(43.0))
                .setSodium(fromGrams(22.0))
                .setSugar(fromGrams(32.0))
                .setThiamin(fromGrams(98.0))
                .setTotalCarbohydrate(fromGrams(98.0))
                .setTotalFat(fromGrams(23.0))
                .setTransFat(fromGrams(23.0))
                .setUnsaturatedFat(fromGrams(12.0))
                .setVitaminA(fromGrams(40.0))
                .setVitaminB12(fromGrams(50.0))
                .setVitaminB6(fromGrams(10.0))
                .setVitaminC(fromGrams(60.0))
                .setVitaminD(fromGrams(70.0))
                .setVitaminE(fromGrams(80.0))
                .setVitaminK(fromGrams(90.0))
                .setZinc(fromGrams(12.0))
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Meal")
        assertThat(formattedEntry.samples)
            .isEqualTo(
                "Biotin 4.0 g\n" +
                    "Caffeine 12.0 g\n" +
                    "Calcium 5.0 g\n" +
                    "Chloride 11.0 g\n" +
                    "Cholesterol 20.0 g\n" +
                    "Chromium 10.0 g\n" +
                    "Copper 20.0 g\n" +
                    "Dietary Fiber 34.0 g\n" +
                    "Energy From Fat 30 cal\n" +
                    "Energy 30 cal\n" +
                    "Folate 100.0 g\n" +
                    "Folic Acid 90.0 g\n" +
                    "Iodine 100.0 g\n" +
                    "Iron 30.0 g\n" +
                    "Magnesium 30.0 g\n" +
                    "Manganese 41.0 g\n" +
                    "Molybdenum 2.0 g\n" +
                    "Monounsaturated Fat 40.0 g\n" +
                    "Niacin 20.0 g\n" +
                    "Pantothenic Acid 56.0 g\n" +
                    "Phosphorus 67.0 g\n" +
                    "Polyunsaturated Fat 67.0 g\n" +
                    "Potassium 23.0 g\n" +
                    "Protein 89.0 g\n" +
                    "Riboflavin 22.0 g\n" +
                    "Saturated Fat 45.0 g\n" +
                    "Selenium 43.0 g\n" +
                    "Sodium 22.0 g\n" +
                    "Sugar 32.0 g\n" +
                    "Thiamin 98.0 g\n" +
                    "Total Carbohydrate 98.0 g\n" +
                    "Total Fat 23.0 g\n" +
                    "Trans Fat 23.0 g\n" +
                    "Unsaturated Fat 12.0 g\n" +
                    "Vitamin A 40.0 g\n" +
                    "Vitamin B12 50.0 g\n" +
                    "Vitamin B6 10.0 g\n" +
                    "Vitamin C 60.0 g\n" +
                    "Vitamin D 70.0 g\n" +
                    "Vitamin E 80.0 g\n" +
                    "Vitamin K 90.0 g\n" +
                    "Zinc 12.0 g"
            )
    }

    private fun buildNutritionRecord(): NutritionRecord.Builder {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return NutritionRecord.Builder(getMetaData(context), NOW, NOW.plusSeconds(1))
    }
}
