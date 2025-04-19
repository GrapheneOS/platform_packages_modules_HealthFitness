/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.Record
import android.icu.text.MessageFormat.format
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.isLessThanOneYearAgo
import com.android.healthconnect.controller.utils.toLocalDate
import com.android.healthconnect.controller.utils.toLocalTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import java.time.Period
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

/** Formatter for printing MenstruationPeriodRecord data. */
class MenstruationPeriodFormatter
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    @ApplicationContext private val context: Context,
    private val timeSource: TimeSource,
) {

    val dateFormatter = LocalDateTimeFormatter(context)

    suspend fun format(
        day: Instant,
        record: MenstruationPeriodRecord,
        period: DateNavigationPeriod,
        showDataOrigin: Boolean = true,
    ): FormattedDataEntry {
        val totalDays = totalDaysOfPeriod(record)
        val appName = if (showDataOrigin) getAppName(record) else ""
        val header = getHeader(record.startTime, record.endTime, appName)
        return when (period) {
            DateNavigationPeriod.PERIOD_DAY -> {
                val dayOfPeriod = dayOfPeriod(record, day)
                val title = context.getString(R.string.period_day, dayOfPeriod, totalDays)
                FormattedDataEntry(
                    uuid = record.metadata.id,
                    title = title,
                    titleA11y = title,
                    header = header,
                    headerA11y = header,
                    dataType = MenstruationPeriodRecord::class,
                    startTime = record.startTime,
                    endTime = record.endTime,
                )
            }

            else -> {
                val title =
                    format(context.getString(R.string.period_length), mapOf("count" to totalDays))
                FormattedDataEntry(
                    uuid = record.metadata.id,
                    title = title,
                    titleA11y = title,
                    header = header,
                    headerA11y = header,
                    dataType = MenstruationPeriodRecord::class,
                    startTime = record.startTime,
                    endTime = record.endTime,
                )
            }
        }
    }

    private fun dayOfPeriod(record: MenstruationPeriodRecord, day: Instant): Int {
        return (Period.between(record.startTime.toLocalDate(), day.toLocalDate()).days +
            1) // + 1 to return a 1-indexed counter (i.e. "Period day 1", not "day 0")
    }

    private fun totalDaysOfPeriod(record: MenstruationPeriodRecord): Int {
        return (DAYS.between(record.startTime.toLocalDate(), record.endTime.toLocalDate()).toInt() +
            1)
    }

    private suspend fun getAppName(record: Record): String {
        return appInfoReader.getAppMetadata(record.metadata.dataOrigin.packageName).appName
    }

    private fun getHeader(startDate: Instant, endDate: Instant, appName: String): String {
        if (appName == "")
            return context.getString(
                R.string.data_entry_header_date_range_without_source_app,
                getDateRange(startDate, endDate),
            )
        return context.getString(
            R.string.data_entry_header_date_range_with_source_app,
            getDateRange(startDate, endDate),
            appName,
        )
    }

    private fun getDateRange(startDate: Instant, endDate: Instant): String {
        return if (endDate != startDate) {
            var localEndDate: Instant = endDate

            // If endDate is midnight, add one millisecond so that DateUtils
            // correctly formats it as a separate date.
            if (endDate.toLocalTime() == LocalTime.MIDNIGHT) {
                localEndDate = endDate.plusMillis(1)
            }
            // display date range
            if (
                startDate.isLessThanOneYearAgo(timeSource) &&
                    localEndDate.isLessThanOneYearAgo(timeSource)
            ) {
                dateFormatter.formatDateRangeWithoutYear(startDate, localEndDate)
            } else {
                dateFormatter.formatDateRangeWithYear(startDate, localEndDate)
            }
        } else {
            // display only one date
            if (startDate.isLessThanOneYearAgo(timeSource)) {
                dateFormatter.formatShortDate(startDate)
            } else {
                dateFormatter.formatLongDate(startDate)
            }
        }
    }
}
