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
import android.health.connect.datatypes.BloodPressureRecord
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_LYING_DOWN
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_RECLINING
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_SITTING_DOWN
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_STANDING_UP
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatPressure
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class BloodPressureFormatter {

    fun format(record: BloodPressureRecord, context: Context): FormattedDataDetails {
        return FormattedDataDetails(
            dataEntry = formatDataEntry(record, context),
            dataDetails = formatDataDetails(record, context),
        )
    }

    private fun formatDataEntry(record: BloodPressureRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(header = getHeader(record), value = formatValue(record, context))
    }

    private fun formatValue(record: BloodPressureRecord, context: Context): String {
        return "${formatPressure(record.systolic)}/${formatPressure(record.diastolic)} ${context.getString(R.string.millimetres_of_mercury_label)}"
    }

    private fun formatDataDetails(
        record: BloodPressureRecord,
        context: Context,
    ): List<FormattedEntry> {
        val formattedEntries = mutableListOf<FormattedEntry>()
        val measurementLocation = formatMeasurementLocation(record.measurementLocation, context)
        val bodyPosition = formatBodyPosition(record.bodyPosition, context)

        if (!measurementLocation.isNullOrEmpty()) {
            formattedEntries.add(
                FormattedDataEntry(
                    header = context.getString(R.string.measurement_location),
                    value = measurementLocation,
                )
            )
        }
        if (!bodyPosition.isNullOrEmpty()) {
            formattedEntries.add(
                FormattedDataEntry(
                    header = context.getString(R.string.body_position),
                    value = bodyPosition,
                )
            )
        }

        if (formattedEntries.isEmpty()) {
            formattedEntries.add(FormattedEntry.Header("No details"))
        }

        return formattedEntries
    }

    private fun formatBodyPosition(position: Int, context: Context): String? {
        return when (position) {
            BODY_POSITION_LYING_DOWN -> context.getString(R.string.body_position_lying_down)
            BODY_POSITION_RECLINING -> context.getString(R.string.body_position_reclining)
            BODY_POSITION_SITTING_DOWN -> context.getString(R.string.body_position_sitting_down)
            BODY_POSITION_STANDING_UP -> context.getString(R.string.body_position_standing_up)
            else -> null
        }
    }

    private fun formatMeasurementLocation(location: Int, context: Context): String? {
        return when (location) {
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM ->
                context.getString(R.string.blood_pressure_measurement_location_left_upper_arm)
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST ->
                context.getString(R.string.blood_pressure_measurement_location_left_wrist)
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM ->
                context.getString(R.string.blood_pressure_measurement_location_right_upper_arm)
            BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST ->
                context.getString(R.string.blood_pressure_measurement_location_right_wrist)
            else -> null
        }
    }
}
