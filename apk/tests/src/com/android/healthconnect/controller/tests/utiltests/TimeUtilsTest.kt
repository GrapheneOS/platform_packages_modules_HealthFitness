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
package com.android.healthconnect.controller.tests.utiltests

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.formatDateTimeForTimePeriod
import com.android.healthconnect.controller.utils.formatRecentAccessTime
import com.android.healthconnect.controller.utils.getPeriodStartDate
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeUtilsTest {
    private lateinit var context: Context
    private lateinit var dateFormatter: LocalDateTimeFormatter
    private lateinit var timeSource: TimeSource

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        dateFormatter = LocalDateTimeFormatter(context)
        timeSource = TestTimeSource
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    @After
    fun tearDown() {
        context.setLocale(Locale.US)
        dateFormatter = LocalDateTimeFormatter(context)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    @Test
    fun getPeriodStartDate_periodDay_startTimeSameDateAsLocal() {
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY

        val expectedResult = Instant.parse("2021-09-19T00:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodDay_startTimeDiffDateToLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY

        val expectedResult = Instant.parse("2021-09-19T15:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodWeek_usLocale_SundayStartOfWeek() {
        // US locale, start of week is Sunday
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK

        val expectedResult = Instant.parse("2021-09-19T00:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodWeek_usLocale_startTimeSameDateAsLocal() {
        // US locale, start of week is Sunday
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-18T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK

        val expectedResult = Instant.parse("2021-09-12T00:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodWeek_usLocale_startTimeDiffDateToLocal() {
        // US locale, start of week is Sunday
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-18T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK

        val expectedResult = Instant.parse("2021-09-18T15:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodWeek_frenchLocale_mondayStartOfWeek() {
        // FR locale, start of week is Monday
        context.setLocale(Locale.FRENCH)
        dateFormatter = LocalDateTimeFormatter(context)
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))

        val startTime = Instant.parse("2021-09-20T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK

        val expectedResult = Instant.parse("2021-09-20T00:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodWeek_frenchLocale_startTimeSameDateAsLocal() {
        // FR locale, start of week is Monday
        context.setLocale(Locale.FRENCH)
        dateFormatter = LocalDateTimeFormatter(context)
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))

        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK

        val expectedResult = Instant.parse("2021-09-13T00:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodWeek_frenchLocale_startTimeDiffDateToLocal() {
        // FR locale, start of week is Monday
        context.setLocale(Locale.FRENCH)
        dateFormatter = LocalDateTimeFormatter(context)
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK

        val expectedResult = Instant.parse("2021-09-19T15:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodMonth_startTimeSameDateAsLocal() {
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_MONTH

        val expectedResult = Instant.parse("2021-09-01T00:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun getPeriodStartDate_periodMonth_startTimeDiffDateToLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-30T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_MONTH

        val expectedResult = Instant.parse("2021-09-30T15:00:00.000Z")
        val actualResult = getPeriodStartDate(startTime, period)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withWeekday_pastYear_startTimeSameDateAsLocal() {
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = true

        val expectedResult = "Sun, Sep 19, 2021"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withWeekday_sameYear_startTimeSameDateAsLocal() {
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2022-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = true

        val expectedResult = "Mon, Sep 19"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withoutWeekday_pastYear_startTimeSameDateAsLocal() {
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = false

        val expectedResult = "Sep 19, 2021"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withoutWeekday_sameYear_startTimeSameDateAsLocal() {
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2022-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = false

        val expectedResult = "Sep 19"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withWeekday_pastYear_startTimeDiffDateAsLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = true

        val expectedResult = "Mon, Sep 20, 2021"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withWeekday_sameYear_startTimeDiffDateAsLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2022-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = true

        val expectedResult = "Tue, Sep 20"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withoutWeekday_pastYear_startTimeDiffDateAsLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2021-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = false

        val expectedResult = "Sep 20, 2021"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodDay_withoutWeekday_sameYear_startTimeDiffDateAsLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2022-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_DAY
        val useWeekday = false

        val expectedResult = "Sep 20"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_sameYear_startTimeNotOnMonday_sameTimeZone() {
        // UTC
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2022-09-20T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 18 – 24"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_sameYear_startTimeOnMonday_sameTimeZone() {
        // UTC
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2022-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 18 – 24"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_sameYear_startTimeNotOnMonday_diffTimeZone() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2022-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 18 – 24"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_sameYear_startTimeOnMonday_diffTimeZone() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2022-09-18T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 18 – 24"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_pastYear_startTimeNotOnMonday_sameTimeZone() {
        // UTC
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2020-09-20T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 20 – 26, 2020"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_pastYear_startTimeOnMonday_sameTimeZone() {
        // UTC
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2020-09-21T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 20 – 26, 2020"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_pastYear_startTimeNotOnMonday_diffTimeZone() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2020-09-19T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 20 – 26, 2020"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodWeek_pastYear_startTimeOnMonday_diffTimeZone() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2020-09-20T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_WEEK
        val useWeekday = true

        val expectedResult = "Sep 20 – 26, 2020"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodMonth_sameYear_startTimeSameDateAsLocal() {
        // UTC
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2022-09-21T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_MONTH
        val useWeekday = true

        val expectedResult = "September"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodMonth_sameYear_startTimeDiffDateAsLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2022-09-30T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_MONTH
        val useWeekday = true

        val expectedResult = "October"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodMonth_pastYear_startTimeSameDateAsLocal() {
        // UTC
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("UTC"))
        val startTime = Instant.parse("2020-09-21T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_MONTH
        val useWeekday = true

        val expectedResult = "September 2020"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatDateTimeForTimePeriod_periodMonth_pastYear_startTimeDiffDateAsLocal() {
        // UTC + 9
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Tokyo")))
        (timeSource as TestTimeSource).setNow(NOW)
        (timeSource as TestTimeSource).setDeviceZoneOffset(ZoneId.of("Asia/Tokyo"))
        val startTime = Instant.parse("2020-09-30T20:00:00.000Z")
        val period = DateNavigationPeriod.PERIOD_MONTH
        val useWeekday = true

        val expectedResult = "October 2020"
        val actualResult =
            formatDateTimeForTimePeriod(startTime, period, dateFormatter, timeSource, useWeekday)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun formatRecentAccessTime_24Hour_returns24HourString() {
        (timeSource as TestTimeSource).setIs24Hour(true)
        val time = Instant.parse("2024-11-20T20:15:45.000Z")
        val result = formatRecentAccessTime(time, timeSource, context)

        assertThat(result).isEqualTo("20:15")
    }

    @Test
    fun formatRecentAccessTime_12Hour_korea_returnsSpecial12HourString() {
        (timeSource as TestTimeSource).setIs24Hour(false)
        context.setLocale(Locale.KOREA)
        val time = Instant.parse("2024-11-20T20:15:45.000Z")
        val result = formatRecentAccessTime(time, timeSource, context)

        assertThat(result.substring(3)).isEqualTo("8:15")
    }

    @Test
    fun formatRecentAccessTime_12Hour_korean_returnsSpecial12HourString() {
        (timeSource as TestTimeSource).setIs24Hour(false)
        context.setLocale(Locale.KOREAN)
        val time = Instant.parse("2024-11-20T20:15:45.000Z")
        val result = formatRecentAccessTime(time, timeSource, context)

        assertThat(result.substring(3)).isEqualTo("8:15")
    }

    @Test
    fun formatRecentAccessTime_12Hour_returns12HourString() {
        (timeSource as TestTimeSource).setIs24Hour(false)
        val time = Instant.parse("2024-11-20T20:15:45.000Z")
        val result = formatRecentAccessTime(time, timeSource, context)

        assertThat(result).isEqualTo("8:15 PM")
    }
}
