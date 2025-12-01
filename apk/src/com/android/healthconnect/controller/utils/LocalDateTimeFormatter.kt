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
package com.android.healthconnect.controller.utils

import android.content.Context
import android.text.format.DateFormat.getBestDateTimePattern
import android.text.format.DateFormat.getLongDateFormat
import android.text.format.DateFormat.getTimeFormat
import android.text.format.DateFormat.is24HourFormat
import android.text.format.DateUtils
import com.android.healthconnect.controller.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Formatter for printing time and time ranges. */
class LocalDateTimeFormatter @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        // Example: "Sun, Aug 20, 2023"
        private const val WEEKDAY_DATE_FORMAT_FLAGS_WITH_YEAR: Int =
            DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_ABBREV_ALL

        // Example: "Sun, Aug 20"
        private const val WEEKDAY_DATE_FORMAT_FLAGS_WITHOUT_YEAR: Int =
            WEEKDAY_DATE_FORMAT_FLAGS_WITH_YEAR or DateUtils.FORMAT_NO_YEAR

        // Example: "Aug 20, 2023"
        private const val DATE_FORMAT_FLAGS_WITH_YEAR: Int =
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL

        // Example: "Aug 20"
        private const val DATE_FORMAT_FLAGS_WITHOUT_YEAR: Int =
            DATE_FORMAT_FLAGS_WITH_YEAR or DateUtils.FORMAT_NO_YEAR

        // Example: "August 2023".
        private const val MONTH_FORMAT_FLAGS_WITH_YEAR: Int =
            DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_YEAR or
                DateUtils.FORMAT_NO_MONTH_DAY

        // Example: "August".
        private const val MONTH_FORMAT_FLAGS_WITHOUT_YEAR: Int =
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NO_MONTH_DAY
    }

    private val timeFormat: java.text.DateFormat
        get() = getTimeFormat(context)

    private val longDateFormat: java.text.DateFormat
        get() = getLongDateFormat(context)

    private val shortDateFormat: DateTimeFormatter
        get() {
            val systemFormat = getBestDateTimePattern(Locale.getDefault(), "dMMMM")
            return DateTimeFormatter.ofPattern(systemFormat, Locale.getDefault())
        }

    // Example: "Aug 20, 14:06"
    private val dateAndTime24HourFormat: DateTimeFormatter
        get() {
            val systemFormat = getBestDateTimePattern(Locale.getDefault(), "MMMd Hm")
            return DateTimeFormatter.ofPattern(systemFormat, Locale.getDefault())
        }

    // Example: "Aug 20, 2:06PM"
    private val dateAndTime12HourFormat: DateTimeFormatter
        get() {
            val systemFormat = getBestDateTimePattern(Locale.getDefault(), "MMMd hm")
            return DateTimeFormatter.ofPattern(systemFormat, Locale.getDefault())
        }

    /** Returns localized time. */
    fun formatTime(instant: Instant): String {
        return timeFormat.format(instant.toEpochMilli())
    }

    /** Returns localized long versions of date, such as "15 August 2022". */
    fun formatLongDate(instant: Instant): String {
        return longDateFormat.format(instant.toEpochMilli())
    }

    /** Returns localized short versions of date, such as "15 August" */
    fun formatShortDate(instant: Instant): String {
        return instant.atZone(ZoneId.systemDefault()).format(shortDateFormat)
    }

    /** Returns localized short versions of date, such as "15 Aug" */
    fun formatShortDateWithoutYear(startTime: Instant): String {
        return DateUtils.formatDateTime(
            context,
            startTime.toEpochMilli(),
            DATE_FORMAT_FLAGS_WITHOUT_YEAR,
        )
    }

    /** Returns localized short versions of date, such as "15 Aug" */
    fun formatShortDateWithoutYear(date: LocalDate, zoneId: ZoneId): String {
        return formatShortDateWithoutYear(date.atStartOfDay(zoneId).toInstant())
    }

    /** Returns localized short versions of date, such as "15 Aug, 2022" */
    fun formatShortDateWithYear(startTime: Instant): String {
        return DateUtils.formatDateTime(
            context,
            startTime.toEpochMilli(),
            DATE_FORMAT_FLAGS_WITH_YEAR,
        )
    }

    /** Returns localized short versions of date and time, such as "15 Aug, 14:06" */
    fun formatDateAndTime(instant: Instant): String {
        val format =
            if (is24HourFormat(context)) {
                dateAndTime24HourFormat
            } else {
                dateAndTime12HourFormat
            }
        return instant.atZone(ZoneId.systemDefault()).format(format)
    }

    /** Returns localized time range. */
    fun formatTimeRange(start: Instant, end: Instant): String {
        return context.getString(R.string.time_range, formatTime(start), formatTime(end))
    }

    /** Returns accessible and localized time range. */
    fun formatTimeRangeA11y(start: Instant, end: Instant): String {
        return context.getString(R.string.time_range_long, formatTime(start), formatTime(end))
    }

    /** Formats date with weekday and year (e.g. "Sun, Aug 20, 2023"). */
    fun formatWeekdayDateWithYear(time: Instant): String {
        return DateUtils.formatDateTime(
            context,
            time.toEpochMilli(),
            WEEKDAY_DATE_FORMAT_FLAGS_WITH_YEAR or DateUtils.FORMAT_ABBREV_ALL,
        )
    }

    /** Formats date with weekday (e.g. "Sun, Aug 20"). */
    fun formatWeekdayDateWithoutYear(time: Instant): String {
        return DateUtils.formatDateTime(
            context,
            time.toEpochMilli(),
            WEEKDAY_DATE_FORMAT_FLAGS_WITHOUT_YEAR or DateUtils.FORMAT_ABBREV_ALL,
        )
    }

    /** Formats date range with year(e.g. "Aug 21 - 27, 2023", "Aug 28 - Sept 3, 2023"). */
    fun formatDateRangeWithYear(startTime: Instant, endTime: Instant): String {
        return DateUtils.formatDateRange(
            context,
            startTime.toEpochMilli(),
            endTime.toEpochMilli(),
            DATE_FORMAT_FLAGS_WITH_YEAR,
        )
    }

    /** Formats date range (e.g. "Aug 21 - 27", "Aug 28 - Sept 3"). */
    fun formatDateRangeWithoutYear(startTime: Instant, endTime: Instant): String {
        return DateUtils.formatDateRange(
            context,
            startTime.toEpochMilli(),
            endTime.toEpochMilli(),
            DATE_FORMAT_FLAGS_WITHOUT_YEAR,
        )
    }

    /** Formats month and year (e.g. "August 2023"). */
    fun formatMonthWithYear(time: Instant): String {
        return DateUtils.formatDateTime(context, time.toEpochMilli(), MONTH_FORMAT_FLAGS_WITH_YEAR)
    }

    /** Formats month (e.g. "August"). */
    fun formatMonthWithoutYear(time: Instant): String {
        return DateUtils.formatDateTime(
            context,
            time.toEpochMilli(),
            MONTH_FORMAT_FLAGS_WITHOUT_YEAR,
        )
    }

    /** Formats the given start date, or a date range (start/end dates) (e.g. "Mar 15, 2024"). */
    fun formatDate(startDate: Instant, endDate: Instant?, timeSource: TimeSource): String {
        val dateFormatter = LocalDateTimeFormatter(context)
        return if (endDate != null) {
            var localEndDate: Instant = endDate

            // If endDate is midnight, add one millisecond so that DateUtils
            // correctly formats it as a separate date.
            if (endDate.toLocalTime() == LocalTime.MIDNIGHT) {
                localEndDate = endDate.plusMillis(1)
            }
            // display date range
            if (
                startDate.isLessThanOneYearAgo(timeSource) &&
                    startDate.isLessThanOneYearAgo(timeSource)
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
