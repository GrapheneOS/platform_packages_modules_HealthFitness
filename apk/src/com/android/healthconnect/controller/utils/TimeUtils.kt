/**
 * Copyright (C) 2024 The Android Open Source Project
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
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod.PERIOD_DAY
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod.PERIOD_MONTH
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod.PERIOD_WEEK
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Returns the localized instant start time of a period: Day: start of day Week: start of Monday of
 * that week Month: start of the first day of the month
 */
fun getPeriodStartDate(selectedDate: Instant, period: DateNavigationPeriod): Instant {
    return when (period) {
        PERIOD_DAY -> {
            selectedDate
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        }

        PERIOD_WEEK -> {
            val locale = Locale.getDefault()
            val weekFields = WeekFields.of(locale)
            val firstDayOfWeek = weekFields.firstDayOfWeek
            val dayOfWeek: DayOfWeek =
                selectedDate.atZone(ZoneId.systemDefault()).toLocalDate().dayOfWeek
            val dayOfWeekOffset: Int = (dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            selectedDate
                .atZone(ZoneId.systemDefault())
                .minusDays(dayOfWeekOffset.toLong())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        }

        PERIOD_MONTH -> {
            val dayOfMonth = selectedDate.atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
            val dayOfMonthOffset: Int = dayOfMonth - 1
            selectedDate
                .atZone(ZoneId.systemDefault())
                .minus(Period.ofDays(dayOfMonthOffset))
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        }
    }
}

/**
 * Formats [startTime] and [period] as follows:
 * * Ensures startTime is at the localized start of the current period
 * * * e.g. for week, it will always be on the Monday of the selected week
 * * Day (if useWeekday): "Sun, Aug 20" or "Mon, Aug 20, 2022"
 * * Day (if not useWeekday): "Aug 20" or "Aug 20, 2022"
 * * Week: "Aug 21-27" or "Aug 21-27, 2022"
 * * Month: "August" or "August 2022"
 */
fun formatDateTimeForTimePeriod(
    startTime: Instant,
    period: DateNavigationPeriod,
    dateFormatter: LocalDateTimeFormatter,
    timeSource: TimeSource,
    useWeekday: Boolean = true,
): String {
    val modifiedStartDate = getPeriodStartDate(startTime, period)

    if (
        areInSameYear(
            modifiedStartDate,
            Instant.ofEpochMilli(timeSource.currentTimeMillis()),
            timeSource,
        )
    ) {
        return when (period) {
            PERIOD_DAY -> {
                if (useWeekday) {
                    dateFormatter.formatWeekdayDateWithoutYear(modifiedStartDate)
                } else {
                    dateFormatter.formatShortDateWithoutYear(modifiedStartDate)
                }
            }
            PERIOD_WEEK -> {
                dateFormatter.formatDateRangeWithoutYear(
                    modifiedStartDate,
                    modifiedStartDate
                        .plus(Period.ofWeeks(1))
                        .minusMillis(1), // to ensure we are always showing Mon-Sun
                )
            }
            PERIOD_MONTH -> {
                dateFormatter.formatMonthWithoutYear(modifiedStartDate)
            }
        }
    }

    return when (period) {
        PERIOD_DAY -> {
            if (useWeekday) {
                dateFormatter.formatWeekdayDateWithYear(modifiedStartDate)
            } else {
                dateFormatter.formatShortDateWithYear(modifiedStartDate)
            }
        }
        PERIOD_WEEK -> {
            dateFormatter.formatDateRangeWithYear(
                modifiedStartDate,
                modifiedStartDate.plus(Period.ofWeeks(1)).minusMillis(1),
            )
        }
        PERIOD_MONTH -> {
            dateFormatter.formatMonthWithYear(modifiedStartDate)
        }
    }
}

/** Whether [instant1] and [instant2] are inn the same calendar year. */
private fun areInSameYear(instant1: Instant, instant2: Instant, timeSource: TimeSource): Boolean {
    val year1 = instant1.atZone(timeSource.deviceZoneOffset()).toLocalDate().year
    val year2 = instant2.atZone(timeSource.deviceZoneOffset()).toLocalDate().year
    return year1 == year2
}

/** Formats an [Instant] to a time in the local time format of the device, e.g. 13:45 or 9:25am. */
fun formatRecentAccessTime(instant: Instant, timeSource: TimeSource, context: Context): String {
    val localTime: LocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
    return if (timeSource.is24Hour(context)) {
        localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        if (Locale.getDefault() == Locale.KOREA || Locale.getDefault() == Locale.KOREAN) {
            localTime.format(DateTimeFormatter.ofPattern("a h:mm"))
        } else {
            localTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    }
}
