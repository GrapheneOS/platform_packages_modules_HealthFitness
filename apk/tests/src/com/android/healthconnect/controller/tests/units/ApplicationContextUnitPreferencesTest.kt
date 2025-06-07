/**
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.units

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.preference.PreferenceManager.getDefaultSharedPreferencesName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.units.DistanceUnit
import com.android.healthconnect.controller.units.EnergyUnit
import com.android.healthconnect.controller.units.HeightUnit
import com.android.healthconnect.controller.units.TemperatureUnit
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.units.WeightUnit
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ApplicationContextUnitPreferencesTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    private lateinit var context: Context
    @Inject lateinit var unitPreferences: UnitPreferences

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        hiltRule.inject()
        context.setLocale(Locale.US)
        val pref =
            context.getSharedPreferences(getDefaultSharedPreferencesName(context), MODE_PRIVATE)
        pref.edit().clear().apply()
    }

    @After
    fun tearDown() {
        val pref =
            context.getSharedPreferences(getDefaultSharedPreferencesName(context), MODE_PRIVATE)
        pref.edit().clear().apply()
    }

    @Test
    fun defaultUnits_localeFR() {
        context.setLocale(Locale.FRANCE)

        assertThat(unitPreferences.distanceUnit).isEqualTo(DistanceUnit.KILOMETERS)
        assertThat(unitPreferences.heightUnit).isEqualTo(HeightUnit.CENTIMETERS)
        assertThat(unitPreferences.weightUnit).isEqualTo(WeightUnit.KILOGRAM)
        assertThat(unitPreferences.energyUnit).isEqualTo(EnergyUnit.CALORIE)
        assertThat(unitPreferences.temperatureUnit).isEqualTo(TemperatureUnit.CELSIUS)
    }

    @Test
    fun defaultUnits_localeGB() {
        context.setLocale(Locale.UK)

        assertThat(unitPreferences.distanceUnit).isEqualTo(DistanceUnit.MILES)
        assertThat(unitPreferences.heightUnit).isEqualTo(HeightUnit.FEET)
        assertThat(unitPreferences.weightUnit).isEqualTo(WeightUnit.STONE)
        assertThat(unitPreferences.energyUnit).isEqualTo(EnergyUnit.CALORIE)
        assertThat(unitPreferences.temperatureUnit).isEqualTo(TemperatureUnit.CELSIUS)
    }

    @Test
    fun defaultUnits_localeUS() {
        context.setLocale(Locale.US)

        assertThat(unitPreferences.distanceUnit).isEqualTo(DistanceUnit.MILES)
        assertThat(unitPreferences.heightUnit).isEqualTo(HeightUnit.FEET)
        assertThat(unitPreferences.weightUnit).isEqualTo(WeightUnit.POUND)
        assertThat(unitPreferences.energyUnit).isEqualTo(EnergyUnit.CALORIE)
        assertThat(unitPreferences.temperatureUnit).isEqualTo(TemperatureUnit.FAHRENHEIT)
    }

    @Test
    fun setDistanceUnit_updatesValue() {
        context.setLocale(Locale.US)

        unitPreferences.distanceUnit = DistanceUnit.KILOMETERS

        assertThat(unitPreferences.distanceUnit).isEqualTo(DistanceUnit.KILOMETERS)
    }

    @Test
    fun setHeightUnit_updatesValue() {
        context.setLocale(Locale.US)

        unitPreferences.heightUnit = HeightUnit.CENTIMETERS

        assertThat(unitPreferences.heightUnit).isEqualTo(HeightUnit.CENTIMETERS)
    }

    @Test
    fun setWeightUnit_updatesValue() {
        context.setLocale(Locale.US)

        unitPreferences.weightUnit = WeightUnit.KILOGRAM

        assertThat(unitPreferences.weightUnit).isEqualTo(WeightUnit.KILOGRAM)
    }

    @Test
    fun setEnergyUnit_updatesValue() {
        context.setLocale(Locale.US)

        unitPreferences.energyUnit = EnergyUnit.KILOJOULE

        assertThat(unitPreferences.energyUnit).isEqualTo(EnergyUnit.KILOJOULE)
    }

    @Test
    fun setTemperatureUnit_updatesValue() {
        context.setLocale(Locale.US)

        unitPreferences.temperatureUnit = TemperatureUnit.CELSIUS

        assertThat(unitPreferences.temperatureUnit).isEqualTo(TemperatureUnit.CELSIUS)
    }
}
