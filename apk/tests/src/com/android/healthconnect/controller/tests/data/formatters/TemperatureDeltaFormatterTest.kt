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
package com.android.healthconnect.controller.tests.data.formatters

import android.health.connect.datatypes.SkinTemperatureRecord.Delta
import android.health.connect.datatypes.units.TemperatureDelta
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.formatters.TemperatureDeltaFormatter
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.units.TemperatureUnit
import com.android.healthconnect.controller.units.UnitPreferences
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class TemperatureDeltaFormatterTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var preferences: UnitPreferences

    private val testInstant: Instant = Instant.EPOCH

    private val allTestTemps =
        listOf(
            Delta(TemperatureDelta.fromCelsius(-2.5), testInstant),
            Delta(TemperatureDelta.fromCelsius(-1.0), testInstant),
            Delta(TemperatureDelta.fromCelsius(0.0), testInstant),
            Delta(TemperatureDelta.fromCelsius(1.0), testInstant),
            Delta(TemperatureDelta.fromCelsius(1.32), testInstant),
            Delta(TemperatureDelta.fromCelsius(2.5), testInstant),
        )

    @Before
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        InstrumentationRegistry.getInstrumentation().context.setLocale(Locale.US)
        hiltRule.inject()
    }

    /** default values */
    @Test
    fun formatSingleValue_celsius() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)
        val expectedFormattedTempStrings = listOf("-2.5℃", "-1℃", "0℃", "+1℃", "+1.3℃", "+2.5℃")

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_singleValue(tempDelta, false)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatSingleValue_fahrenheit() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        val expectedFormattedTempStrings = listOf("-4.5℉", "-1.8℉", "0℉", "+1.8℉", "+2.4℉", "+4.5℉")

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_singleValue(tempDelta, false)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatSingleValue_kelvin() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)
        val expectedFormattedTempStrings = listOf("-2.5K", "-1K", "0K", "+1K", "+1.3K", "+2.5K")

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_singleValue(tempDelta, false)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatAverageValue_celsius() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)
        val expectedFormattedTempStrings =
            listOf(
                "-2.5℃ (avg variation)",
                "-1℃ (avg variation)",
                "0℃ (avg variation)",
                "+1℃ (avg variation)",
                "+1.3℃ (avg variation)",
                "+2.5℃ (avg variation)",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_averageValue(tempDelta, false)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatAverageValue_fahrenheit() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        val expectedFormattedTempStrings =
            listOf(
                "-4.5℉ (avg variation)",
                "-1.8℉ (avg variation)",
                "0℉ (avg variation)",
                "+1.8℉ (avg variation)",
                "+2.4℉ (avg variation)",
                "+4.5℉ (avg variation)",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_averageValue(tempDelta, false)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatAverageValue_kelvin() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)
        val expectedFormattedTempStrings =
            listOf(
                "-2.5K (avg variation)",
                "-1K (avg variation)",
                "0K (avg variation)",
                "+1K (avg variation)",
                "+1.3K (avg variation)",
                "+2.5K (avg variation)",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_averageValue(tempDelta, false)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    /** a11y values */
    @Test
    fun formatSingleValue_celsius_long() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)
        val expectedFormattedTempStrings =
            listOf(
                "-2.5 degrees Celsius",
                "-1 degree Celsius",
                "0 degrees Celsius",
                "+1 degree Celsius",
                "+1.3 degrees Celsius",
                "+2.5 degrees Celsius",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_singleValue(tempDelta, true)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatSingleValue_fahrenheit_long() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        val expectedFormattedTempStrings =
            listOf(
                "-4.5 degrees Fahrenheit",
                "-1.8 degrees Fahrenheit",
                "0 degrees Fahrenheit",
                "+1.8 degrees Fahrenheit",
                "+2.4 degrees Fahrenheit",
                "+4.5 degrees Fahrenheit",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_singleValue(tempDelta, true)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatSingleValue_kelvin_long() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)
        val expectedFormattedTempStrings =
            listOf(
                "-2.5 kelvins",
                "-1 kelvin",
                "0 kelvins",
                "+1 kelvin",
                "+1.3 kelvins",
                "+2.5 kelvins",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_singleValue(tempDelta, true)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatAverageValue_celsius_long() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)
        val expectedFormattedTempStrings =
            listOf(
                "-2.5 degrees Celsius (average variation)",
                "-1 degree Celsius (average variation)",
                "0 degrees Celsius (average variation)",
                "+1 degree Celsius (average variation)",
                "+1.3 degrees Celsius (average variation)",
                "+2.5 degrees Celsius (average variation)",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_averageValue(tempDelta, true)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatAverageValue_fahrenheit_long() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        val expectedFormattedTempStrings =
            listOf(
                "-4.5 degrees Fahrenheit (average variation)",
                "-1.8 degrees Fahrenheit (average variation)",
                "0 degrees Fahrenheit (average variation)",
                "+1.8 degrees Fahrenheit (average variation)",
                "+2.4 degrees Fahrenheit (average variation)",
                "+4.5 degrees Fahrenheit (average variation)",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_averageValue(tempDelta, true)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    @Test
    fun formatAverageValue_kelvin_long() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)
        val expectedFormattedTempStrings =
            listOf(
                "-2.5 kelvins (average variation)",
                "-1 kelvin (average variation)",
                "0 kelvins (average variation)",
                "+1 kelvin (average variation)",
                "+1.3 kelvins (average variation)",
                "+2.5 kelvins (average variation)",
            )

        allTestTemps.forEachIndexed { index: Int, tempDelta: Delta ->
            val actual = getFormattedTemperatureDelta_averageValue(tempDelta, true)
            assertThat(actual).isEqualTo(expectedFormattedTempStrings[index])
        }
    }

    private fun getFormattedTemperatureDelta_singleValue(
        temperatureDelta: Delta,
        isA11y: Boolean,
    ): String {
        return if (isA11y) {
            TemperatureDeltaFormatter.formatSingleDeltaA11yValue(
                InstrumentationRegistry.getInstrumentation().context,
                temperatureDelta.delta,
                preferences,
            )
        } else {
            TemperatureDeltaFormatter.formatSingleDeltaValue(
                InstrumentationRegistry.getInstrumentation().context,
                temperatureDelta.delta,
                preferences,
            )
        }
    }

    private fun getFormattedTemperatureDelta_averageValue(
        temperatureDelta: Delta,
        isA11y: Boolean,
    ): String {
        return if (isA11y) {
            TemperatureDeltaFormatter.formatAverageDeltaA11yValue(
                InstrumentationRegistry.getInstrumentation().context,
                temperatureDelta.delta,
                preferences,
            )
        } else {
            TemperatureDeltaFormatter.formatAverageDeltaValue(
                InstrumentationRegistry.getInstrumentation().context,
                temperatureDelta.delta,
                preferences,
            )
        }
    }
}
