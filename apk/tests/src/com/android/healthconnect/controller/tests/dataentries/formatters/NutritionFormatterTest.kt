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
package com.android.healthconnect.controller.tests.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.units.Energy.*
import android.health.connect.datatypes.units.Mass.*
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.dataentries.formatters.NutritionFormatter
import com.android.healthconnect.controller.dataentries.units.EnergyUnit
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NutritionFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: NutritionFormatter
    @Inject lateinit var preferences: UnitPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun formatValue_noData() = runBlocking {
        val record = getBuilder().build()
        assertThat(formatter.formatValue(record, preferences)).isEqualTo("-")
    }

    @Test
    fun formatValue_formatsMass() = runBlocking {
        val record = getBuilder().setCaffeine(fromKilograms(.032)).build()

        assertThat(formatter.formatValue(record, preferences)).isEqualTo("Caffeine: 32 g")
    }

    @Test
    fun formatA11yValue_formatsMass() = runBlocking {
        val record = getBuilder().setCaffeine(fromKilograms(.032)).build()

        assertThat(formatter.formatA11yValue(record, preferences)).isEqualTo("Caffeine: 32 grams")
    }

    @Test
    fun formatValue_kj_formatsEnergy() = runBlocking {
        preferences.setEnergyUnit(EnergyUnit.KILOJOULE)
        val record = getBuilder().setEnergy(fromJoules(1234567.0)).build()

        assertThat(formatter.formatValue(record, preferences)).isEqualTo("Energy: 1,235 kJ")
    }

    @Test
    fun formatA11yValue_kj_formatsMass() = runBlocking {
        preferences.setEnergyUnit(EnergyUnit.KILOJOULE)
        val record = getBuilder().setEnergy(fromJoules(1234567.0)).build()

        assertThat(formatter.formatA11yValue(record, preferences))
            .isEqualTo("Energy: 1,235 kilojoules")
    }

    @Test
    fun formatValue_cal_formatsEnergy() = runBlocking {
        preferences.setEnergyUnit(EnergyUnit.CALORIE)
        val record = getBuilder().setEnergy(fromJoules(1234567.0)).build()

        assertThat(formatter.formatValue(record, preferences)).isEqualTo("Energy: 295 Cal")
    }

    @Test
    fun formatA11yValue_cal_formatsMass() = runBlocking {
        preferences.setEnergyUnit(EnergyUnit.CALORIE)
        val record = getBuilder().setEnergy(fromJoules(1234567.0)).build()

        assertThat(formatter.formatA11yValue(record, preferences)).isEqualTo("Energy: 295 calories")
    }

    @Test
    fun formatValue_formatsAllFields() = runBlocking {
        preferences.setEnergyUnit(EnergyUnit.CALORIE)
        val record =
            getBuilder()
                .setMealName("Custom meal")
                .setBiotin(fromKilograms(0.04))
                .setCaffeine(fromKilograms(.012))
                .setCalcium(fromKilograms(.005))
                .setEnergyFromFat(fromJoules(123456.0))
                .setEnergy(fromJoules(123457.0))
                .setChloride(fromKilograms(.011))
                .setCholesterol(fromKilograms(.02))
                .setChromium(fromKilograms(.01))
                .setCopper(fromKilograms(.02))
                .setDietaryFiber(fromKilograms(0.034))
                .setFolate(fromKilograms(.1))
                .setFolicAcid(fromKilograms(.09))
                .setIodine(fromKilograms(.1))
                .setIron(fromKilograms(.03))
                .setMagnesium(fromKilograms(0.03))
                .setManganese(fromKilograms(0.041))
                .setMolybdenum(fromKilograms(0.002))
                .setMonounsaturatedFat(fromKilograms(0.04))
                .setNiacin(fromKilograms(0.02))
                .setPantothenicAcid(fromKilograms(0.056))
                .setPhosphorus(fromKilograms(0.067))
                .setPolyunsaturatedFat(fromKilograms(0.067))
                .setPotassium(fromKilograms(0.023))
                .setProtein(fromKilograms(0.089))
                .setRiboflavin(fromKilograms(0.022))
                .setSaturatedFat(fromKilograms(0.045))
                .setSelenium(fromKilograms(0.043))
                .setSodium(fromKilograms(0.022))
                .setSugar(fromKilograms(0.032))
                .setThiamin(fromKilograms(0.098))
                .setTotalCarbohydrate(fromKilograms(0.098))
                .setTotalFat(fromKilograms(0.023))
                .setTransFat(fromKilograms(0.023))
                .setUnsaturatedFat(fromKilograms(0.012))
                .setVitaminA(fromKilograms(0.04))
                .setVitaminB12(fromKilograms(0.05))
                .setVitaminB6(fromKilograms(0.01))
                .setVitaminC(fromKilograms(0.06))
                .setVitaminD(fromKilograms(0.07))
                .setVitaminE(fromKilograms(0.08))
                .setVitaminK(fromKilograms(0.09))
                .setZinc(fromKilograms(0.012))
                .build()
        assertThat(formatter.formatValue(record, preferences))
            .isEqualTo(
                "Name: Custom meal\n" +
                    "Biotin: 40 g\n" +
                    "Caffeine: 12 g\n" +
                    "Calcium: 5 g\n" +
                    "Chloride: 11 g\n" +
                    "Cholesterol: 20 g\n" +
                    "Chromium: 10 g\n" +
                    "Copper: 20 g\n" +
                    "Dietary fiber: 34 g\n" +
                    "Energy: 30 Cal\n" +
                    "Energy from fat: 30 Cal\n" +
                    "Folate: 100 g\n" +
                    "Folic acid: 90 g\n" +
                    "Iodine: 100 g\n" +
                    "Iron: 30 g\n" +
                    "Magnesium: 30 g\n" +
                    "manganese: 41 g\n" +
                    "Molybdenum: 2 g\n" +
                    "Monounsaturated fat: 40 g\n" +
                    "Niacin: 20 g\n" +
                    "Pantothenic acid: 56 g\n" +
                    "Phosphorus: 67 g\n" +
                    "Polyunsaturated fat: 67 g\n" +
                    "Potassium: 23 g\n" +
                    "Riboflavin: 22 g\n" +
                    "Saturated fat: 45 g\n" +
                    "Selenium: 43 g\n" +
                    "Sodium: 22 g\n" +
                    "Sugar: 32 g\n" +
                    "Thiamin: 98 g\n" +
                    "Total carbohydrate: 98 g\n" +
                    "Total fat: 23 g\n" +
                    "Trans fat: 23 g\n" +
                    "Unsaturated fat: 12 g\n" +
                    "Vitamin A: 40 g\n" +
                    "Vitamin B12: 50 g\n" +
                    "Vitamin B6: 10 g\n" +
                    "Vitamin C: 60 g\n" +
                    "Vitamin D: 70 g\n" +
                    "Vitamin E: 80 g\n" +
                    "Vitamin K: 90 g\n" +
                    "Zinc: 12 g")
    }

    private fun getBuilder(): NutritionRecord.Builder {
        return NutritionRecord.Builder(getMetaData(), NOW, NOW.plusSeconds(10))
    }
}
