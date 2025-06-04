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
import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER
import android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_TOE
import android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST
import android.health.connect.datatypes.units.Temperature
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.round
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class SkinTemperatureFormatter {

    fun format(record: SkinTemperatureRecord, context: Context): FormattedDataDetails {
        return FormattedDataDetails(
            dataEntry = formatDataEntry(record, context),
            dataDetails = formatDataDetails(record, context),
        )
    }

    private fun formatDataDetails(
        record: SkinTemperatureRecord,
        context: Context,
    ): List<FormattedEntry> {
        val formattedEntries = mutableListOf<FormattedEntry>()

        val measurementLocation = formatMeasurementLocation(record.measurementLocation, context)
        if (measurementLocation.value.isNotEmpty()) {
            formattedEntries.add(measurementLocation)
        }

        if (record.baseline != null) {
            val baseline = formatBaseline(record.baseline!!, context)
            formattedEntries.add(baseline)
        }

        val deltas = record.deltas.map { formatDelta(it) }
        formattedEntries.addAll(deltas)

        return formattedEntries
    }

    private fun formatDelta(delta: SkinTemperatureRecord.Delta): FormattedSample {
        return FormattedSample(header = getHeader(delta.time), value = delta.delta.toString())
    }

    private fun formatBaseline(baseline: Temperature, context: Context): FormattedDataEntry {
        return FormattedDataEntry(
            header = context.getString(R.string.baseline),
            value = baseline.toString(),
        )
    }

    private fun formatMeasurementLocation(location: Int, context: Context): FormattedDataEntry {
        return FormattedDataEntry(
            header = context.getString(R.string.measurement_location),
            value = getMeasurementLocation(location, context),
        )
    }

    private fun getMeasurementLocation(location: Int, context: Context): String {
        return when (location) {
            MEASUREMENT_LOCATION_FINGER -> context.getString(R.string.finger)
            MEASUREMENT_LOCATION_TOE -> context.getString(R.string.toe)
            MEASUREMENT_LOCATION_WRIST -> context.getString(R.string.wrist)
            else -> ""
        }
    }

    private fun formatDataEntry(
        record: SkinTemperatureRecord,
        context: Context,
    ): FormattedDataEntry {
        return FormattedDataEntry(
            header = getHeader(record),
            value = "${getAvgVariation(record.deltas)} ${context.getString(R.string.celsius)}",
        )
    }

    private fun getAvgVariation(deltas: List<SkinTemperatureRecord.Delta>): String {
        return round(deltas.sumOf { it.delta.inCelsius } / deltas.size, 1)
    }
}
