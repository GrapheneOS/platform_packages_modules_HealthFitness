/**
 * Copyright (C) 2025 The Android Open Source Project
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

import android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INSTANT
import android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INTERVAL
import android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_LOCAL_DATE
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TemporalTypeFormatter {

    fun format(
        timeFormatter: LocalDateTimeFormatter,
        temporalType: Int,
        startTime: Instant,
        endTime: Instant,
        date: LocalDate?,
    ): String {
        return when (temporalType) {
            RECORD_TEMPORAL_TYPE_INTERVAL -> formatInterval(timeFormatter, startTime, endTime)
            RECORD_TEMPORAL_TYPE_INSTANT -> formatInstant(timeFormatter, startTime)
            RECORD_TEMPORAL_TYPE_LOCAL_DATE -> formatDate(timeFormatter, startTime, date)
            else -> throw IllegalArgumentException("$temporalType Not supported!")
        }
    }

    fun formatA11y(
        timeFormatter: LocalDateTimeFormatter,
        temporalType: Int,
        startTime: Instant,
        endTime: Instant,
        date: LocalDate?,
    ): String {
        return when (temporalType) {
            RECORD_TEMPORAL_TYPE_INTERVAL -> formatIntervalA11y(timeFormatter, startTime, endTime)
            RECORD_TEMPORAL_TYPE_INSTANT -> formatInstant(timeFormatter, startTime)
            RECORD_TEMPORAL_TYPE_LOCAL_DATE -> formatDateA11y(timeFormatter, startTime, date)
            else -> throw IllegalArgumentException("$temporalType Not supported!")
        }
    }

    private fun formatInterval(
        timeFormatter: LocalDateTimeFormatter,
        startTime: Instant,
        endTime: Instant,
    ): String {
        return timeFormatter.formatTimeRange(startTime, endTime)
    }

    private fun formatIntervalA11y(
        timeFormatter: LocalDateTimeFormatter,
        startTime: Instant,
        endTime: Instant,
    ): String {
        return timeFormatter.formatTimeRangeA11y(startTime, endTime)
    }

    private fun formatInstant(timeFormatter: LocalDateTimeFormatter, startTime: Instant): String {
        return timeFormatter.formatTime(startTime)
    }

    private fun formatDate(
        timeFormatter: LocalDateTimeFormatter,
        startTime: Instant,
        date: LocalDate?,
    ): String {
        val instantToFormat = getInstantFromDate(startTime, date)
        return timeFormatter.formatShortDateWithYear(instantToFormat)
    }

    private fun formatDateA11y(
        timeFormatter: LocalDateTimeFormatter,
        startTime: Instant,
        date: LocalDate?,
    ): String {
        val instantToFormat = getInstantFromDate(startTime, date)
        return timeFormatter.formatLongDate(instantToFormat)
    }

    private fun getInstantFromDate(startTime: Instant, date: LocalDate?): Instant {
        return if (date != null) {
            date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        } else {
            startTime
        }
    }
}
