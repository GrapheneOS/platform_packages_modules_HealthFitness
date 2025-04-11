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
package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.units.TemperatureDelta
import android.icu.number.NumberFormatter
import android.icu.text.MessageFormat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.units.TemperatureUnit
import com.android.healthconnect.controller.units.UnitPreferences
import kotlin.math.round

object TemperatureDeltaFormatter {

    fun formatSingleDeltaValue(
        context: Context,
        temperatureDelta: TemperatureDelta,
        unitPreferences: UnitPreferences,
    ): String {
        val res =
            when (unitPreferences.getTemperatureUnit()) {
                TemperatureUnit.CELSIUS -> R.string.temperatureDelta_celsius
                TemperatureUnit.FAHRENHEIT -> R.string.temperatureDelta_fahrenheit
                TemperatureUnit.KELVIN -> R.string.temperatureDelta_kelvin
            }
        return formatTemperatureDelta(context, res, temperatureDelta, unitPreferences)
    }

    fun formatAverageDeltaValue(
        context: Context,
        temperatureDelta: TemperatureDelta,
        unitPreferences: UnitPreferences,
    ): String {
        val res =
            when (unitPreferences.getTemperatureUnit()) {
                TemperatureUnit.CELSIUS -> R.string.temperatureDelta_average_celsius
                TemperatureUnit.FAHRENHEIT -> R.string.temperatureDelta_average_fahrenheit
                TemperatureUnit.KELVIN -> R.string.temperatureDelta_average_kelvin
            }
        return formatTemperatureDelta(context, res, temperatureDelta, unitPreferences)
    }

    fun formatSingleDeltaA11yValue(
        context: Context,
        temperatureDelta: TemperatureDelta,
        unitPreferences: UnitPreferences,
    ): String {
        val res =
            when (unitPreferences.getTemperatureUnit()) {
                TemperatureUnit.CELSIUS -> R.string.temperatureDelta_celsius_long
                TemperatureUnit.FAHRENHEIT -> R.string.temperatureDelta_fahrenheit_long
                TemperatureUnit.KELVIN -> R.string.temperatureDelta_kelvin_long
            }
        return formatTemperatureDelta(context, res, temperatureDelta, unitPreferences)
    }

    fun formatAverageDeltaA11yValue(
        context: Context,
        temperatureDelta: TemperatureDelta,
        unitPreferences: UnitPreferences,
    ): String {
        val res =
            when (unitPreferences.getTemperatureUnit()) {
                TemperatureUnit.CELSIUS -> R.string.temperatureDelta_average_celsius_long
                TemperatureUnit.FAHRENHEIT -> R.string.temperatureDelta_average_fahrenheit_long
                TemperatureUnit.KELVIN -> R.string.temperatureDelta_average_kelvin_long
            }
        return formatTemperatureDelta(context, res, temperatureDelta, unitPreferences)
    }

    private fun formatTemperatureDelta(
        context: Context,
        res: Int,
        temperatureDelta: TemperatureDelta,
        unitPreferences: UnitPreferences,
    ): String {
        // when implemented can use TemperatureDelta methods for conversion
        val temp =
            round(
                convertTemperatureDelta(
                    temperatureDelta.inCelsius,
                    unitPreferences.getTemperatureUnit(),
                ) * 10
            ) / 10
        val formattedTemp =
            NumberFormatter.with()
                .sign(NumberFormatter.SignDisplay.EXCEPT_ZERO)
                .locale(context.resources.configuration.locale)
                .format(temp)
                .toString()
        return MessageFormat.format(
            context.getString(res),
            mapOf("value" to (temp * temp), "formattedValue" to formattedTemp),
        )
    }

    private fun convertTemperatureDelta(
        temperatureDelta: Double,
        unitPreference: TemperatureUnit,
    ): Double {
        return when (unitPreference) {
            TemperatureUnit.CELSIUS -> temperatureDelta
            TemperatureUnit.FAHRENHEIT -> temperatureDelta * 1.8
            TemperatureUnit.KELVIN -> temperatureDelta
        }
    }
}
