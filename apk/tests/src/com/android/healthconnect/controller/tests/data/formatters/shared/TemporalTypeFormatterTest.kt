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
package com.android.healthconnect.controller.tests.data.formatters.shared

import android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INSTANT
import android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INTERVAL
import android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_LOCAL_DATE
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.formatters.shared.TemporalTypeFormatter
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TemporalTypeFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var timeFormatter: LocalDateTimeFormatter

    private val startTime: Instant = Instant.parse("2025-10-07T08:00:00.00Z")
    private val endTime: Instant = Instant.parse("2025-10-15T09:00:00.00Z")

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val previousYear = currentYear - 1
    private val dateCurrentYear = LocalDate.of(currentYear, 10, 7)
    private val datePreviousYear = LocalDate.of(previousYear, 10, 7)

    @Before
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        InstrumentationRegistry.getInstrumentation().context.setLocale(Locale.US)
        timeFormatter = LocalDateTimeFormatter(InstrumentationRegistry.getInstrumentation().context)
        hiltRule.inject()
    }

    @Test
    fun format_interval_returnsTimeRange() {

        val formatted =
            TemporalTypeFormatter.format(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_INTERVAL,
                startTime,
                endTime,
                null,
            )

        assertThat(formatted).isEqualTo("8:00 AM - 9:00 AM")
    }

    @Test
    fun formatA11y_interval_returnsA11yTimeRange() {

        val formatted =
            TemporalTypeFormatter.formatA11y(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_INTERVAL,
                startTime,
                endTime,
                null,
            )

        assertThat(formatted).isEqualTo("from 8:00 AM to 9:00 AM")
    }

    @Test
    fun format_instant_returnsTime() {

        val formatted =
            TemporalTypeFormatter.format(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_INSTANT,
                startTime,
                endTime,
                null,
            )

        assertThat(formatted).isEqualTo("8:00 AM")
    }

    @Test
    fun formatA11y_instant_returnsTime() {

        val formatted =
            TemporalTypeFormatter.formatA11y(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_INSTANT,
                startTime,
                endTime,
                null,
            )

        assertThat(formatted).isEqualTo("8:00 AM")
    }

    @Test
    fun format_localDate_noDate_returnsDate() {
        val startOfDay = datePreviousYear.atStartOfDay(ZoneOffset.UTC).toInstant()

        val formatted =
            TemporalTypeFormatter.format(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_LOCAL_DATE,
                startOfDay,
                endTime,
                null,
            )

        assertThat(formatted).isEqualTo("Oct 7, $previousYear")
    }

    @Test
    fun format_localDate_withPreviousYearDate_returnsDate() {
        val startOfDay = datePreviousYear.atStartOfDay(ZoneOffset.UTC).toInstant()
        val differentDate = datePreviousYear.minusDays(1)

        val formatted =
            TemporalTypeFormatter.format(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_LOCAL_DATE,
                startOfDay, // Oct 7, previousYear
                endTime,
                differentDate, // Oct 6, previousYear
            )

        assertThat(formatted).isEqualTo("Oct 6, $previousYear")
    }

    @Test
    fun format_localDate_sameYear_returnsDateNoYear() {

        val formatted =
            TemporalTypeFormatter.format(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_LOCAL_DATE,
                dateCurrentYear.atStartOfDay(ZoneOffset.UTC).toInstant(),
                endTime,
                dateCurrentYear,
            )

        assertThat(formatted).isEqualTo("Oct 7")
    }

    @Test
    fun formatA11y_localDate_returnsDate() {
        val startOfDay = datePreviousYear.atStartOfDay(ZoneOffset.UTC).toInstant()

        val formatted =
            TemporalTypeFormatter.formatA11y(
                timeFormatter,
                RECORD_TEMPORAL_TYPE_LOCAL_DATE,
                startOfDay,
                endTime,
                datePreviousYear,
            )

        assertThat(formatted).isEqualTo("October 7, $previousYear")
    }

    @Test
    fun format_unsupportedTemporalType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            TemporalTypeFormatter.format(timeFormatter, -1, startTime, endTime, null)
        }
    }

    @Test
    fun formatA11y_unsupportedTemporalType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            TemporalTypeFormatter.formatA11y(timeFormatter, -1, startTime, endTime, null)
        }
    }
}
