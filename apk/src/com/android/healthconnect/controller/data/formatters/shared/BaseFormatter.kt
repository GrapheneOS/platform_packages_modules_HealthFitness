/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.data.formatters.shared

import android.content.Context
import android.health.connect.datatypes.AlcoholConsumptionRecord
import android.health.connect.datatypes.InstantRecord
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.MenstrualCyclePhaseRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import java.time.Instant

/** Abstract formatter for Record types. This formatter handles header for FormattedEntries. */
abstract class BaseFormatter<T : Record>(
    protected val context: Context,
    protected val timeFormatter: LocalDateTimeFormatter,
    protected val unitPreferences: UnitPreferences,
) : Formatter<T> {

    override suspend fun format(record: T, appName: String): FormattedEntry {
        return formatRecord(
            record = record,
            header = getHeader(record, appName),
            headerA11y = getHeaderA11y(record, appName),
        )
    }

    abstract suspend fun formatRecord(record: T, header: String, headerA11y: String): FormattedEntry

    protected fun getStartTime(record: T): Instant {
        return when (record) {
            is IntervalRecord -> record.startTime
            is InstantRecord -> record.time
            else -> throw IllegalArgumentException("${record::class.java} Not supported!")
        }
    }

    private fun getHeader(record: T, appName: String): String {
        if (appName == "")
            return context.getString(
                R.string.data_entry_header_without_source_app,
                getFormattedTime(record),
            )
        return context.getString(
            R.string.data_entry_header_with_source_app,
            getFormattedTime(record),
            appName,
        )
    }

    private fun getHeaderA11y(record: T, appName: String): String {
        if (appName == "")
            return context.getString(
                R.string.data_entry_header_without_source_app,
                getFormattedA11yTime(record),
            )
        return context.getString(
            R.string.data_entry_header_with_source_app,
            getFormattedA11yTime(record),
            appName,
        )
    }

    private fun getFormattedTime(record: T): String {
        return when (record) {
            is AlcoholConsumptionRecord ->
                TemporalTypeFormatter.format(
                    timeFormatter,
                    record.temporalType,
                    record.startTime,
                    record.endTime,
                    record.date,
                )
            is SymptomRecord ->
                TemporalTypeFormatter.format(
                    timeFormatter,
                    record.temporalType,
                    record.startTime,
                    record.endTime,
                    record.date,
                )
            is MenstrualCyclePhaseRecord ->
                timeFormatter.formatShortDateWithoutYear(record.date, record.startZoneOffset)
            is IntervalRecord -> timeFormatter.formatTimeRange(record.startTime, record.endTime)
            is InstantRecord -> timeFormatter.formatTime(record.time)
            else -> throw IllegalArgumentException("${record::class.java} Not supported!")
        }
    }

    private fun getFormattedA11yTime(record: T): String {
        return when (record) {
            is AlcoholConsumptionRecord ->
                TemporalTypeFormatter.formatA11y(
                    timeFormatter,
                    record.temporalType,
                    record.startTime,
                    record.endTime,
                    record.date,
                )
            is SymptomRecord ->
                TemporalTypeFormatter.formatA11y(
                    timeFormatter,
                    record.temporalType,
                    record.startTime,
                    record.endTime,
                    record.date,
                )
            is MenstrualCyclePhaseRecord -> timeFormatter.formatShortDate(record.startTime)
            is IntervalRecord -> timeFormatter.formatTimeRangeA11y(record.startTime, record.endTime)
            is InstantRecord -> timeFormatter.formatTime(record.time)
            else -> throw IllegalArgumentException("${record::class.java} Not supported!")
        }
    }
}
