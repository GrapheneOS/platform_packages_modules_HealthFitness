/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.healthconnect.datatypes.InstantRecord
import android.healthconnect.datatypes.IntervalRecord
import android.healthconnect.datatypes.Record
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.FormattedDataEntry
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import com.android.healthconnect.controller.shared.AppInfoReader
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter

abstract class DataEntriesFormatter<T : Record>(private val context: Context) {

    private val timeFormatter = LocalDateTimeFormatter(context)
    private val appInfoReader = AppInfoReader(context)
    private val unitPreferences = UnitPreferences(context)

    suspend fun format(record: T): FormattedDataEntry {
        return FormattedDataEntry(
            uuid = record.metadata.id,
            header = getHeader(record),
            headerA11y = getHeaderA11y(record),
            title = formatValue(record, unitPreferences),
            titleA11y = formatA11yValue(record, unitPreferences))
    }

    abstract suspend fun formatValue(record: T, unitPreferences: UnitPreferences): String

    abstract suspend fun formatA11yValue(record: T, unitPreferences: UnitPreferences): String

    private fun getHeader(record: T): String {
        return context.getString(
            R.string.data_entry_header, getFormattedTime(record), getAppName(record))
    }

    private fun getHeaderA11y(record: T): String {
        return context.getString(
            R.string.data_entry_header, getFormattedA11yTime(record), getAppName(record))
    }

    private fun getAppName(record: T): String {
        return appInfoReader.getAppName(record.metadata.dataOrigin.packageName!!)
    }

    private fun getFormattedTime(record: T): String {
        return when (record) {
            is IntervalRecord -> timeFormatter.formatTimeRange(record.startTime, record.endTime)
            is InstantRecord -> timeFormatter.formatTime(record.time)
            else -> throw IllegalArgumentException("${record::class.java} Not supported!")
        }
    }

    private fun getFormattedA11yTime(record: T): String {
        return when (record) {
            is IntervalRecord -> timeFormatter.formatTimeRangeA11y(record.startTime, record.endTime)
            is InstantRecord -> timeFormatter.formatTime(record.time)
            else -> throw IllegalArgumentException("${record::class.java} Not supported!")
        }
    }
}
