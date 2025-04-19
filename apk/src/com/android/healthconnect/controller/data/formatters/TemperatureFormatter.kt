/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.*
import android.health.connect.datatypes.units.Temperature
import android.icu.text.MessageFormat
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.units.TemperatureConverter
import com.android.healthconnect.controller.units.TemperatureUnit
import com.android.healthconnect.controller.units.UnitPreferences

object TemperatureFormatter {
    fun formatValue(
        context: Context,
        temperature: Temperature,
        measurementLocation: Int,
        unitPreferences: UnitPreferences,
    ): String {
        val res =
            when (unitPreferences.getTemperatureUnit()) {
                TemperatureUnit.CELSIUS -> R.string.temperature_celsius
                TemperatureUnit.FAHRENHEIT -> R.string.temperature_fahrenheit
                TemperatureUnit.KELVIN -> R.string.temperature_kelvin
            }

        val tempString = formatTemperature(context, res, temperature, unitPreferences)
        return if (measurementLocation != MEASUREMENT_LOCATION_UNKNOWN) {
            val tempLocation = formatLocation(context, measurementLocation)
            "$tempString $tempLocation"
        } else {
            tempString
        }
    }

    fun formatA11tValue(
        context: Context,
        temperature: Temperature,
        measurementLocation: Int,
        unitPreferences: UnitPreferences,
    ): String {
        val res =
            when (unitPreferences.getTemperatureUnit()) {
                TemperatureUnit.CELSIUS -> R.string.temperature_celsius_long
                TemperatureUnit.FAHRENHEIT -> R.string.temperature_fahrenheit_long
                TemperatureUnit.KELVIN -> R.string.temperature_kelvin_long
            }

        val tempString = formatTemperature(context, res, temperature, unitPreferences)
        return if (measurementLocation != MEASUREMENT_LOCATION_UNKNOWN) {
            val tempLocation = formatLocation(context, measurementLocation)
            "$tempString $tempLocation"
        } else {
            tempString
        }
    }

    private fun formatLocation(context: Context, location: Int): String {
        return when (location) {
            MEASUREMENT_LOCATION_ARMPIT -> context.getString(R.string.temperature_location_armpit)
            MEASUREMENT_LOCATION_FINGER -> context.getString(R.string.temperature_location_finger)
            MEASUREMENT_LOCATION_FOREHEAD ->
                context.getString(R.string.temperature_location_forehead)
            MEASUREMENT_LOCATION_MOUTH -> context.getString(R.string.temperature_location_mouth)
            MEASUREMENT_LOCATION_RECTUM -> context.getString(R.string.temperature_location_rectum)
            MEASUREMENT_LOCATION_TEMPORAL_ARTERY ->
                context.getString(R.string.temperature_location_temporal_artery)
            MEASUREMENT_LOCATION_TOE -> context.getString(R.string.temperature_location_toe)
            MEASUREMENT_LOCATION_EAR -> context.getString(R.string.temperature_location_ear)
            MEASUREMENT_LOCATION_WRIST -> context.getString(R.string.temperature_location_wrist)
            MEASUREMENT_LOCATION_VAGINA -> context.getString(R.string.temperature_location_vagina)
            else -> {
                throw IllegalArgumentException(
                    "Unrecognised body temperature measurement location: $location"
                )
            }
        }
    }

    private fun formatTemperature(
        context: Context,
        @StringRes res: Int,
        temperature: Temperature,
        unitPreferences: UnitPreferences,
    ): String {
        val temp =
            TemperatureConverter.convertFromCelsius(
                temperature.inCelsius,
                unitPreferences.getTemperatureUnit(),
            )
        return MessageFormat.format(context.getString(res), mapOf("value" to temp))
    }
}
