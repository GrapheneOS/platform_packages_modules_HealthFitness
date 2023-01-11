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
import android.healthconnect.datatypes.NutritionRecord
import android.healthconnect.datatypes.units.Energy.*
import android.healthconnect.datatypes.units.Mass.*
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
                .setChloride(fromKilograms(.111))
                .setCholesterol(fromKilograms(.22))
                .setChromium(fromKilograms(.01))
                .setCopper(fromKilograms(.2))
                .setDietaryFiber(fromKilograms(34.0))
                .setFolate(fromKilograms(.12))
                .setFolicAcid(fromKilograms(.22))
                .setIodine(fromKilograms(.1))
                .setIron(fromKilograms(1.3))
                .setMagnesium(fromKilograms(2.3))
                .setManganese(fromKilograms(0.41))
                .setMolybdenum(fromKilograms(3.0))
                .setMonounsaturatedFat(fromKilograms(123.4))
                .setNiacin(fromKilograms(0.32))
                .setPantothenicAcid(fromKilograms(0.56))
                .setPhosphorus(fromKilograms(0.67))
                .setPolyunsaturatedFat(fromKilograms(67.8))
                .setPotassium(fromKilograms(23.4))
                .setProtein(fromKilograms(89.0))
                .setRiboflavin(fromKilograms(22.33))
                .setSaturatedFat(fromKilograms(45.0))
                .setSelenium(fromKilograms(0.43))
                .setSodium(fromKilograms(22.1))
                .setSugar(fromKilograms(32.1))
                .setThiamin(fromKilograms(0.98))
                .setTotalCarbohydrate(fromKilograms(0.98))
                .setTotalFat(fromKilograms(234.0))
                .setTransFat(fromKilograms(233.3))
                .setUnsaturatedFat(fromKilograms(123.4))
                .setVitaminA(fromKilograms(0.4))
                .setVitaminB12(fromKilograms(0.5))
                .setVitaminB6(fromKilograms(1.0))
                .setVitaminC(fromKilograms(0.6))
                .setVitaminD(fromKilograms(0.7))
                .setVitaminE(fromKilograms(0.8))
                .setVitaminK(fromKilograms(0.9))
                .setZinc(fromKilograms(12.0))
                .build()
        assertThat(formatter.formatValue(record, preferences))
            .isEqualTo(
                "Name: Custom meal\n" +
                    "Biotin: 40 g\n" +
                    "Caffeine: 12 g\n" +
                    "Calcium: 5 g\n" +
                    "Chloride: 111 g\n" +
                    "Cholesterol: 220 g\n" +
                    "Chromium: 10 g\n" +
                    "Copper: 200 g\n" +
                    "Dietary fiber: 34,000 g\n" +
                    "Energy: 30 Cal\n" +
                    "Energy from fat: 30 Cal\n" +
                    "Folate: 120 g\n" +
                    "Folic acid: 220 g\n" +
                    "Iodine: 100 g\n" +
                    "Iron: 1,300 g\n" +
                    "Magnesium: 2,300 g\n" +
                    "manganese: 410 g\n" +
                    "Molybdenum: 3,000 g\n" +
                    "Monounsaturated fat: 123,400 g\n" +
                    "Niacin: 320 g\n" +
                    "Pantothenic acid: 560 g\n" +
                    "Phosphorus: 670 g\n" +
                    "Polyunsaturated fat: 67,800 g\n" +
                    "Potassium: 23,400 g\n" +
                    "Riboflavin: 22,330 g\n" +
                    "Saturated fat: 45,000 g\n" +
                    "Selenium: 430 g\n" +
                    "Sodium: 22,100 g\n" +
                    "Sugar: 32,100 g\n" +
                    "Thiamin: 980 g\n" +
                    "Total carbohydrate: 980 g\n" +
                    "Total fat: 234,000 g\n" +
                    "Trans fat: 233,300 g\n" +
                    "Unsaturated fat: 123,400 g\n" +
                    "Vitamin A: 400 g\n" +
                    "Vitamin B12: 500 g\n" +
                    "Vitamin B6: 1,000 g\n" +
                    "Vitamin C: 600 g\n" +
                    "Vitamin D: 700 g\n" +
                    "Vitamin E: 800 g\n" +
                    "Vitamin K: 900 g\n" +
                    "Zinc: 12,000 g")
    }

    private fun getBuilder(): NutritionRecord.Builder {
        return NutritionRecord.Builder(getMetaData(), NOW, NOW.plusSeconds(10))
    }
}
