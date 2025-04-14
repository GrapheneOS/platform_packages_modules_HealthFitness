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

import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.SkinTemperatureRecord.Delta
import android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_TOE
import android.health.connect.datatypes.units.Temperature
import android.health.connect.datatypes.units.TemperatureDelta
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSectionTitle
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSessionDetail
import com.android.healthconnect.controller.data.entries.FormattedEntry.ReverseSessionDetail
import com.android.healthconnect.controller.data.entries.FormattedEntry.SeriesDataEntry
import com.android.healthconnect.controller.data.formatters.SkinTemperatureFormatter
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.units.TemperatureUnit
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SkinTemperatureFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var skinTemperatureFormatter: SkinTemperatureFormatter
    @Inject lateinit var preferences: UnitPreferences

    private lateinit var dateTimeFormatter: LocalDateTimeFormatter

    val startTime: Instant = Instant.parse("2024-01-14T16:45:00.00Z")
    val endTime: Instant = Instant.parse("2024-01-15T05:30:00.00Z")

    private val appNameUnknown: String = ""
    private val appNameTestApp: String = "Test App"

    private val deltaMinusTwoAndHalf: Delta =
        Delta(TemperatureDelta.fromCelsius(-2.5), startTime.plus(3, ChronoUnit.HOURS))

    private val deltaZero: Delta =
        Delta(TemperatureDelta.fromCelsius(0.0), startTime.plus(7, ChronoUnit.HOURS))

    private val deltaPlusOne: Delta =
        Delta(TemperatureDelta.fromCelsius(1.0), startTime.plus(9, ChronoUnit.HOURS))

    private val skinTempRecordSingleDelta: SkinTemperatureRecord =
        SkinTemperatureRecord.Builder(getMetaData(), startTime, endTime)
            .setMeasurementLocation(MEASUREMENT_LOCATION_TOE)
            .setBaseline(Temperature.fromCelsius(25.0))
            .setDeltas(listOf(deltaMinusTwoAndHalf))
            .build()

    private val skinTempRecordMultipleDeltas: SkinTemperatureRecord =
        SkinTemperatureRecord.Builder(getMetaData(), startTime, endTime)
            .setMeasurementLocation(MEASUREMENT_LOCATION_TOE)
            .setBaseline(Temperature.fromCelsius(25.0))
            .setDeltas(listOf(deltaMinusTwoAndHalf, deltaZero, deltaPlusOne))
            .build()

    @Before
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        InstrumentationRegistry.getInstrumentation().context.setLocale(Locale.US)
        dateTimeFormatter =
            LocalDateTimeFormatter(InstrumentationRegistry.getInstrumentation().context)
        hiltRule.inject()
    }

    @Test
    fun getRecord_singleDeltaInCelsius_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)

        val formattedTimeRange: String = dateTimeFormatter.formatTimeRange(startTime, endTime)
        val formattedTimeRangeA11y: String =
            dateTimeFormatter.formatTimeRangeA11y(startTime, endTime)

        val formattedEntryUnknownApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordSingleDelta, appNameUnknown)
                as SeriesDataEntry
        val formattedEntryNamedApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordSingleDelta, appNameTestApp)
                as SeriesDataEntry

        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryUnknownApp,
                    "-2.5℃ (avg variation)",
                    "-2.5 degrees Celsius (average variation)",
                    formattedTimeRange,
                    formattedTimeRangeA11y,
                )
            )
            .isTrue()
        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryNamedApp,
                    "-2.5℃ (avg variation)",
                    "-2.5 degrees Celsius (average variation)",
                    "$formattedTimeRange • $appNameTestApp",
                    "$formattedTimeRangeA11y • $appNameTestApp",
                )
            )
            .isTrue()
    }

    @Test
    fun getRecord_multipleDeltaInCelsius_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)

        val formattedTimeRange: String = dateTimeFormatter.formatTimeRange(startTime, endTime)
        val formattedTimeRangeA11y: String =
            dateTimeFormatter.formatTimeRangeA11y(startTime, endTime)

        val formattedEntryUnknownApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordMultipleDeltas, appNameUnknown)
                as SeriesDataEntry
        val formattedEntryNamedApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordMultipleDeltas, appNameTestApp)
                as SeriesDataEntry

        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryUnknownApp,
                    "-0.5℃ (avg variation)",
                    "-0.5 degrees Celsius (average variation)",
                    formattedTimeRange,
                    formattedTimeRangeA11y,
                )
            )
            .isTrue()
        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryNamedApp,
                    "-0.5℃ (avg variation)",
                    "-0.5 degrees Celsius (average variation)",
                    "$formattedTimeRange • $appNameTestApp",
                    "$formattedTimeRangeA11y • $appNameTestApp",
                )
            )
            .isTrue()
    }

    @Test
    fun getRecord_singleDeltaInFahrenheit_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)

        val formattedTimeRange: String = dateTimeFormatter.formatTimeRange(startTime, endTime)
        val formattedTimeRangeA11y: String =
            dateTimeFormatter.formatTimeRangeA11y(startTime, endTime)

        val formattedEntryUnknownApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordSingleDelta, appNameUnknown)
                as SeriesDataEntry
        val formattedEntryNamedApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordSingleDelta, appNameTestApp)
                as SeriesDataEntry

        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryUnknownApp,
                    "-4.5℉ (avg variation)",
                    "-4.5 degrees Fahrenheit (average variation)",
                    formattedTimeRange,
                    formattedTimeRangeA11y,
                )
            )
            .isTrue()
        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryNamedApp,
                    "-4.5℉ (avg variation)",
                    "-4.5 degrees Fahrenheit (average variation)",
                    "$formattedTimeRange • $appNameTestApp",
                    "$formattedTimeRangeA11y • $appNameTestApp",
                )
            )
            .isTrue()
    }

    @Test
    fun getRecord_multipleDeltaInFahrenheit_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)

        val formattedTimeRange: String = dateTimeFormatter.formatTimeRange(startTime, endTime)
        val formattedTimeRangeA11y: String =
            dateTimeFormatter.formatTimeRangeA11y(startTime, endTime)

        val formattedEntryUnknownApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordMultipleDeltas, appNameUnknown)
                as SeriesDataEntry
        val formattedEntryNamedApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordMultipleDeltas, appNameTestApp)
                as SeriesDataEntry

        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryUnknownApp,
                    "-0.9℉ (avg variation)",
                    "-0.9 degrees Fahrenheit (average variation)",
                    formattedTimeRange,
                    formattedTimeRangeA11y,
                )
            )
            .isTrue()
        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryNamedApp,
                    "-0.9℉ (avg variation)",
                    "-0.9 degrees Fahrenheit (average variation)",
                    "$formattedTimeRange • $appNameTestApp",
                    "$formattedTimeRangeA11y • $appNameTestApp",
                )
            )
            .isTrue()
    }

    @Test
    fun getRecord_singleDeltaInKelvin_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)

        val formattedTimeRange: String = dateTimeFormatter.formatTimeRange(startTime, endTime)
        val formattedTimeRangeA11y: String =
            dateTimeFormatter.formatTimeRangeA11y(startTime, endTime)

        val formattedEntryUnknownApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordSingleDelta, appNameUnknown)
                as SeriesDataEntry
        val formattedEntryNamedApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordSingleDelta, appNameTestApp)
                as SeriesDataEntry

        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryUnknownApp,
                    "-2.5K (avg variation)",
                    "-2.5 kelvins (average variation)",
                    formattedTimeRange,
                    formattedTimeRangeA11y,
                )
            )
            .isTrue()
        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryNamedApp,
                    "-2.5K (avg variation)",
                    "-2.5 kelvins (average variation)",
                    "$formattedTimeRange • $appNameTestApp",
                    "$formattedTimeRangeA11y • $appNameTestApp",
                )
            )
            .isTrue()
    }

    @Test
    fun getRecord_multipleDeltaInKelvin_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)

        val formattedTimeRange: String = dateTimeFormatter.formatTimeRange(startTime, endTime)
        val formattedTimeRangeA11y: String =
            dateTimeFormatter.formatTimeRangeA11y(startTime, endTime)

        val formattedEntryUnknownApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordMultipleDeltas, appNameUnknown)
                as SeriesDataEntry
        val formattedEntryNamedApp: SeriesDataEntry =
            skinTemperatureFormatter.format(skinTempRecordMultipleDeltas, appNameTestApp)
                as SeriesDataEntry

        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryUnknownApp,
                    "-0.5K (avg variation)",
                    "-0.5 kelvins (average variation)",
                    formattedTimeRange,
                    formattedTimeRangeA11y,
                )
            )
            .isTrue()
        assertThat(
                isSeriesDataEntryCorrect(
                    formattedEntryNamedApp,
                    "-0.5K (avg variation)",
                    "-0.5 kelvins (average variation)",
                    "$formattedTimeRange • $appNameTestApp",
                    "$formattedTimeRangeA11y • $appNameTestApp",
                )
            )
            .isTrue()
    }

    @Test
    fun getRecordDetails_singleDeltaInCelsius_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)

        val formattedEntries: List<FormattedEntry> =
            skinTemperatureFormatter.formatRecordDetails(skinTempRecordSingleDelta)

        assertThat(formattedEntries).hasSize(4)
        assertThat(
                isSessionDetailsOverviewCorrect(
                    formattedEntries[0] as ReverseSessionDetail,
                    formattedEntries[1] as ReverseSessionDetail,
                    formattedEntries[2] as FormattedSectionTitle,
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[3] as FormattedSessionDetail,
                    "-2.5℃",
                    "-2.5 degrees Celsius",
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                )
            )
            .isTrue()
    }

    @Test
    fun getRecordDetails_multipleDeltaInCelsius_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.CELSIUS)

        val formattedEntries: List<FormattedEntry> =
            skinTemperatureFormatter.formatRecordDetails(skinTempRecordMultipleDeltas)

        assertThat(formattedEntries).hasSize(6)
        assertThat(
                isSessionDetailsOverviewCorrect(
                    formattedEntries[0] as ReverseSessionDetail,
                    formattedEntries[1] as ReverseSessionDetail,
                    formattedEntries[2] as FormattedSectionTitle,
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[3] as FormattedSessionDetail,
                    "-2.5℃",
                    "-2.5 degrees Celsius",
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[4] as FormattedSessionDetail,
                    "0℃",
                    "0 degrees Celsius",
                    dateTimeFormatter.formatTime(deltaZero.time),
                    dateTimeFormatter.formatTime(deltaZero.time),
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[5] as FormattedSessionDetail,
                    "+1℃",
                    "+1 degree Celsius",
                    dateTimeFormatter.formatTime(deltaPlusOne.time),
                    dateTimeFormatter.formatTime(deltaPlusOne.time),
                )
            )
            .isTrue()
    }

    @Test
    fun getRecordDetails_singleDeltaInFahrenheit_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)

        val formattedEntries: List<FormattedEntry> =
            skinTemperatureFormatter.formatRecordDetails(skinTempRecordSingleDelta)

        assertThat(formattedEntries).hasSize(4)
        assertThat(
                isSessionDetailsOverviewCorrect(
                    formattedEntries[0] as ReverseSessionDetail,
                    formattedEntries[1] as ReverseSessionDetail,
                    formattedEntries[2] as FormattedSectionTitle,
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[3] as FormattedSessionDetail,
                    "-4.5℉",
                    "-4.5 degrees Fahrenheit",
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                )
            )
            .isTrue()
    }

    @Test
    fun getRecordDetails_multipleDeltaInFahrenheit_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)

        val formattedEntries: List<FormattedEntry> =
            skinTemperatureFormatter.formatRecordDetails(skinTempRecordMultipleDeltas)

        assertThat(formattedEntries).hasSize(6)
        assertThat(
                isSessionDetailsOverviewCorrect(
                    formattedEntries[0] as ReverseSessionDetail,
                    formattedEntries[1] as ReverseSessionDetail,
                    formattedEntries[2] as FormattedSectionTitle,
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[3] as FormattedSessionDetail,
                    "-4.5℉",
                    "-4.5 degrees Fahrenheit",
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[4] as FormattedSessionDetail,
                    "0℉",
                    "0 degrees Fahrenheit",
                    dateTimeFormatter.formatTime(deltaZero.time),
                    dateTimeFormatter.formatTime(deltaZero.time),
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[5] as FormattedSessionDetail,
                    "+1.8℉",
                    "+1.8 degrees Fahrenheit",
                    dateTimeFormatter.formatTime(deltaPlusOne.time),
                    dateTimeFormatter.formatTime(deltaPlusOne.time),
                )
            )
            .isTrue()
    }

    @Test
    fun getRecordDetails_singleDeltaInKelvin_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)

        val formattedEntries: List<FormattedEntry> =
            skinTemperatureFormatter.formatRecordDetails(skinTempRecordSingleDelta)

        assertThat(formattedEntries).hasSize(4)
        assertThat(
                isSessionDetailsOverviewCorrect(
                    formattedEntries[0] as ReverseSessionDetail,
                    formattedEntries[1] as ReverseSessionDetail,
                    formattedEntries[2] as FormattedSectionTitle,
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[3] as FormattedSessionDetail,
                    "-2.5K",
                    "-2.5 kelvins",
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                )
            )
            .isTrue()
    }

    @Test
    fun getRecordDetails_multipleDeltaInKelvin_formattedCorrectly() = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.KELVIN)

        val formattedEntries: List<FormattedEntry> =
            skinTemperatureFormatter.formatRecordDetails(skinTempRecordMultipleDeltas)

        assertThat(formattedEntries).hasSize(6)
        assertThat(
                isSessionDetailsOverviewCorrect(
                    formattedEntries[0] as ReverseSessionDetail,
                    formattedEntries[1] as ReverseSessionDetail,
                    formattedEntries[2] as FormattedSectionTitle,
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[3] as FormattedSessionDetail,
                    "-2.5K",
                    "-2.5 kelvins",
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                    dateTimeFormatter.formatTime(deltaMinusTwoAndHalf.time),
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[4] as FormattedSessionDetail,
                    "0K",
                    "0 kelvins",
                    dateTimeFormatter.formatTime(deltaZero.time),
                    dateTimeFormatter.formatTime(deltaZero.time),
                )
            )
            .isTrue()
        assertThat(
                isFormattedSessionDetailCorrect(
                    formattedEntries[5] as FormattedSessionDetail,
                    "+1K",
                    "+1 kelvin",
                    dateTimeFormatter.formatTime(deltaPlusOne.time),
                    dateTimeFormatter.formatTime(deltaPlusOne.time),
                )
            )
            .isTrue()
    }

    private enum class ReverseEntryType(val title: String) {
        BASELINE_TEMP("Baseline"),
        MEASUREMENT_LOCATION("Measurement location"),
    }

    private fun isReverseSessionDetailCorrect(
        entryToCheck: ReverseSessionDetail,
        header: String,
        headerA11y: String,
        entryType: ReverseEntryType,
    ): Boolean {
        return when (entryType) {
            ReverseEntryType.MEASUREMENT_LOCATION ->
                (entryToCheck.title == ReverseEntryType.MEASUREMENT_LOCATION.title) &&
                    (entryToCheck.titleA11y == ReverseEntryType.MEASUREMENT_LOCATION.title) &&
                    (entryToCheck.header == header) &&
                    (entryToCheck.headerA11y == headerA11y)
            ReverseEntryType.BASELINE_TEMP ->
                (entryToCheck.title == ReverseEntryType.BASELINE_TEMP.title) &&
                    (entryToCheck.titleA11y == ReverseEntryType.BASELINE_TEMP.title) &&
                    (entryToCheck.header == header) &&
                    (entryToCheck.headerA11y == headerA11y)
        }
    }

    private fun isFormattedSessionDetailCorrect(
        entryToCheck: FormattedSessionDetail,
        title: String,
        titleA11y: String,
        header: String,
        headerA11y: String,
    ): Boolean {
        return (entryToCheck.title == title) &&
            (entryToCheck.titleA11y == titleA11y) &&
            (entryToCheck.header == header) &&
            (entryToCheck.headerA11y == headerA11y)
    }

    private fun isSeriesDataEntryCorrect(
        entryToCheck: SeriesDataEntry,
        title: String,
        titleA11y: String,
        header: String,
        headerA11y: String,
    ): Boolean {
        return (entryToCheck.title == title) &&
            (entryToCheck.titleA11y == titleA11y) &&
            (entryToCheck.header == header) &&
            (entryToCheck.headerA11y == headerA11y)
    }

    private fun isSessionDetailsOverviewCorrect(
        locationEntry: ReverseSessionDetail,
        baselineEntry: ReverseSessionDetail,
        titleEntry: FormattedSectionTitle,
    ): Boolean {
        return isReverseSessionDetailCorrect(
            locationEntry,
            "Toe",
            "Toe",
            ReverseEntryType.MEASUREMENT_LOCATION,
        ) &&
            when (preferences.getTemperatureUnit()) {
                TemperatureUnit.CELSIUS ->
                    isReverseSessionDetailCorrect(
                        baselineEntry,
                        "25℃",
                        "25 degrees Celsius",
                        ReverseEntryType.BASELINE_TEMP,
                    )
                TemperatureUnit.FAHRENHEIT ->
                    isReverseSessionDetailCorrect(
                        baselineEntry,
                        "77℉",
                        "77 degrees Fahrenheit",
                        ReverseEntryType.BASELINE_TEMP,
                    )
                TemperatureUnit.KELVIN ->
                    isReverseSessionDetailCorrect(
                        baselineEntry,
                        "298.15K",
                        "298.15 kelvins",
                        ReverseEntryType.BASELINE_TEMP,
                    )
            } &&
            (titleEntry.title == "Variation from baseline")
    }
}
