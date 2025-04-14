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
package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.SkinTemperatureRecord.Delta
import android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_UNKNOWN
import android.health.connect.datatypes.units.Temperature
import android.health.connect.datatypes.units.TemperatureDelta
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.shared.EntryFormatter
import com.android.healthconnect.controller.data.formatters.shared.RecordDetailsFormatter
import com.android.healthconnect.controller.data.formatters.shared.UnitFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Formatter for printing SkinTemperatureRecord data. */
@Singleton
class SkinTemperatureFormatter
@Inject
constructor(@ApplicationContext private val context: Context) :
    EntryFormatter<SkinTemperatureRecord>(context),
    RecordDetailsFormatter<SkinTemperatureRecord>,
    UnitFormatter<TemperatureDelta> {

    private val timeFormatter = LocalDateTimeFormatter(context)

    override suspend fun formatRecord(
        record: SkinTemperatureRecord,
        header: String,
        headerA11y: String,
        unitPreferences: UnitPreferences,
    ): FormattedEntry {
        return FormattedEntry.SeriesDataEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatValue(record, unitPreferences),
            titleA11y = formatA11yValue(record, unitPreferences),
            dataType = record::class,
        )
    }

    override suspend fun formatRecordDetails(record: SkinTemperatureRecord): List<FormattedEntry> {

        val measurementLocationEntry: List<FormattedEntry> =
            if (record.measurementLocation == MEASUREMENT_LOCATION_UNKNOWN) {
                listOf()
            } else {
                listOf(
                    FormattedEntry.ReverseSessionDetail(
                        uuid = record.metadata.id,
                        title =
                            context.getString(R.string.skin_temperature_measurement_location_title),
                        titleA11y =
                            context.getString(R.string.skin_temperature_measurement_location_title),
                        header = formatLocation(context, record.measurementLocation),
                        headerA11y = formatLocation(context, record.measurementLocation),
                    )
                )
            }

        val baselineEntry: List<FormattedEntry> =
            if (record.baseline == null) {
                listOf()
            } else {
                listOf(
                    FormattedEntry.ReverseSessionDetail(
                        uuid = record.metadata.id,
                        title = context.getString(R.string.skin_temperature_baseline_title),
                        titleA11y = context.getString(R.string.skin_temperature_baseline_title),
                        header = formatNullableTemperature(record.baseline, false),
                        headerA11y = formatNullableTemperature(record.baseline, true),
                    )
                )
            }

        val deltasTitle: List<FormattedEntry> =
            listOf(
                FormattedEntry.FormattedSectionTitle(
                    context.getString(R.string.skin_temperature_delta_details_heading)
                )
            )

        val deltas =
            record.deltas
                .sortedBy { it.time }
                .map { formatDelta(record.metadata.id, it, unitPreferences) }
        val formattedSectionDetails: List<FormattedEntry.FormattedSessionDetail> = buildList {
            if (deltas.isNotEmpty()) {
                addAll(deltas)
            }
        }

        return measurementLocationEntry
            .plus(baselineEntry)
            .plus(deltasTitle)
            .plus(formattedSectionDetails)
    }

    private fun formatNullableTemperature(temperature: Temperature?, isA11y: Boolean): String {
        if (temperature == null) return ""
        return if (isA11y) {
            TemperatureFormatter.formatA11tValue(
                context,
                temperature,
                MEASUREMENT_LOCATION_UNKNOWN,
                unitPreferences,
            )
        } else {
            TemperatureFormatter.formatValue(
                context,
                temperature,
                MEASUREMENT_LOCATION_UNKNOWN,
                unitPreferences,
            )
        }
    }

    private fun formatDelta(
        id: String,
        delta: Delta,
        unitPreferences: UnitPreferences,
    ): FormattedEntry.FormattedSessionDetail {
        return FormattedEntry.FormattedSessionDetail(
            uuid = id,
            header = timeFormatter.formatTime(delta.time),
            headerA11y = timeFormatter.formatTime(delta.time),
            title =
                TemperatureDeltaFormatter.formatSingleDeltaValue(
                    context,
                    delta.delta,
                    unitPreferences,
                ),
            titleA11y =
                TemperatureDeltaFormatter.formatSingleDeltaA11yValue(
                    context,
                    delta.delta,
                    unitPreferences,
                ),
        )
    }

    override suspend fun formatValue(
        record: SkinTemperatureRecord,
        unitPreferences: UnitPreferences,
    ): String {
        return if (record.deltas.size == 1) {
            formatUnit(record.deltas.first().delta)
        } else {
            format(record, unitPreferences, false)
        }
    }

    override suspend fun formatA11yValue(
        record: SkinTemperatureRecord,
        unitPreferences: UnitPreferences,
    ): String {
        return if (record.deltas.size == 1) {
            formatA11yUnit(record.deltas.first().delta)
        } else {
            format(record, unitPreferences, true)
        }
    }

    override fun formatUnit(unit: TemperatureDelta): String {
        return TemperatureDeltaFormatter.formatAverageDeltaValue(context, unit, unitPreferences)
    }

    override fun formatA11yUnit(unit: TemperatureDelta): String {
        return TemperatureDeltaFormatter.formatAverageDeltaA11yValue(context, unit, unitPreferences)
    }

    private fun format(
        record: SkinTemperatureRecord,
        unitPreferences: UnitPreferences,
        isA11y: Boolean,
    ): String {
        if (record.deltas.isEmpty()) {
            return context.getString(R.string.no_data)
        }
        val averageDelta =
            TemperatureDelta.fromCelsius(
                record.deltas.sumOf { it.delta.inCelsius } / record.deltas.size
            )
        return if (isA11y) {
            TemperatureDeltaFormatter.formatAverageDeltaA11yValue(
                context,
                averageDelta,
                unitPreferences,
            )
        } else {
            TemperatureDeltaFormatter.formatAverageDeltaValue(
                context,
                averageDelta,
                unitPreferences,
            )
        }
    }

    private fun formatLocation(context: Context, location: Int): String {
        return when (location) {
            SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER ->
                context.getString(R.string.temperature_location_finger)
            SkinTemperatureRecord.MEASUREMENT_LOCATION_TOE ->
                context.getString(R.string.temperature_location_toe)
            SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST ->
                context.getString(R.string.temperature_location_wrist)
            else -> {
                throw IllegalArgumentException(
                    "Unrecognised skin temperature measurement location: $location"
                )
            }
        }
    }
}
