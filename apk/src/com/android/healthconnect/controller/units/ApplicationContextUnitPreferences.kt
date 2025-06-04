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
 *
 *
 */
package com.android.healthconnect.controller.units

import android.content.Context
import android.content.SharedPreferences
import android.icu.util.LocaleData
import android.icu.util.ULocale
import androidx.core.text.util.LocalePreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Preferences wrapper for health data units. */
@Singleton
class ApplicationContextUnitPreferences
@Inject
constructor(@ApplicationContext private val context: Context) : UnitPreferences {

    companion object {
        const val DISTANCE_UNIT_PREF_KEY = "DISTANCE_UNIT_KEY"
        const val HEIGHT_UNIT_PREF_KEY = "HEIGHT_UNIT_KEY"
        const val WEIGHT_UNIT_PREF_KEY = "WEIGHT_UNIT_KEY"
        const val ENERGY_UNIT_PREF_KEY = "ENERGY_UNIT_KEY"
        const val TEMPERATURE_UNIT_PREF_KEY = "TEMPERATURE_UNIT_KEY"
    }

    private val unitSharedPreference: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    override var distanceUnit: DistanceUnit
        get() {
            return unitSharedPreference
                .getString(DISTANCE_UNIT_PREF_KEY, null)
                ?.let(DistanceUnit::valueOf) ?: getDefaultDistanceUnit()
        }
        set(value) {
            with(unitSharedPreference.edit()) {
                putString(DISTANCE_UNIT_PREF_KEY, value.toString())
                apply()
            }
        }

    private fun getDefaultDistanceUnit(): DistanceUnit {
        val measurementSystem = LocaleData.getMeasurementSystem(ULocale.getDefault())
        return when (measurementSystem) {
            LocaleData.MeasurementSystem.SI -> DistanceUnit.KILOMETERS
            LocaleData.MeasurementSystem.UK -> DistanceUnit.MILES
            LocaleData.MeasurementSystem.US -> DistanceUnit.MILES
            else -> DistanceUnit.KILOMETERS
        }
    }

    override var heightUnit: HeightUnit
        get() {
            return unitSharedPreference
                .getString(HEIGHT_UNIT_PREF_KEY, null)
                ?.let(HeightUnit::valueOf) ?: getDefaultHeightUnit()
        }
        set(value) {
            with(unitSharedPreference.edit()) {
                putString(HEIGHT_UNIT_PREF_KEY, value.toString())
                apply()
            }
        }

    private fun getDefaultHeightUnit(): HeightUnit {
        val measurementSystem = LocaleData.getMeasurementSystem(ULocale.getDefault())
        return when (measurementSystem) {
            LocaleData.MeasurementSystem.SI -> HeightUnit.CENTIMETERS
            LocaleData.MeasurementSystem.UK -> HeightUnit.FEET
            LocaleData.MeasurementSystem.US -> HeightUnit.FEET
            else -> HeightUnit.CENTIMETERS
        }
    }

    override var weightUnit: WeightUnit
        get() {
            return unitSharedPreference
                .getString(WEIGHT_UNIT_PREF_KEY, null)
                ?.let(WeightUnit::valueOf) ?: getDefaultWeightUnit()
        }
        set(value) {
            with(unitSharedPreference.edit()) {
                putString(WEIGHT_UNIT_PREF_KEY, value.toString())
                apply()
            }
        }

    private fun getDefaultWeightUnit(): WeightUnit {
        val measurementSystem = LocaleData.getMeasurementSystem(ULocale.getDefault())
        return when (measurementSystem) {
            LocaleData.MeasurementSystem.SI -> WeightUnit.KILOGRAM
            LocaleData.MeasurementSystem.UK -> WeightUnit.STONE
            LocaleData.MeasurementSystem.US -> WeightUnit.POUND
            else -> WeightUnit.POUND
        }
    }

    override var energyUnit: EnergyUnit
        get() {
            return unitSharedPreference
                .getString(ENERGY_UNIT_PREF_KEY, null)
                ?.let(EnergyUnit::valueOf) ?: EnergyUnit.CALORIE
        }
        set(value) {
            with(unitSharedPreference.edit()) {
                putString(ENERGY_UNIT_PREF_KEY, value.toString())
                apply()
            }
        }

    override var temperatureUnit: TemperatureUnit
        get() {
            return unitSharedPreference
                .getString(TEMPERATURE_UNIT_PREF_KEY, null)
                ?.let(TemperatureUnit::valueOf) ?: getDefaultTemperatureUnit()
        }
        set(value) {
            with(unitSharedPreference.edit()) {
                putString(TEMPERATURE_UNIT_PREF_KEY, value.toString())
                apply()
            }
        }

    private fun getDefaultTemperatureUnit(): TemperatureUnit {
        val temperatureUnit = LocalePreferences.getTemperatureUnit()
        return when (temperatureUnit) {
            LocalePreferences.TemperatureUnit.FAHRENHEIT -> TemperatureUnit.FAHRENHEIT
            LocalePreferences.TemperatureUnit.CELSIUS -> TemperatureUnit.CELSIUS
            LocalePreferences.TemperatureUnit.KELVIN -> TemperatureUnit.KELVIN
            else -> TemperatureUnit.FAHRENHEIT
        }
    }
}
