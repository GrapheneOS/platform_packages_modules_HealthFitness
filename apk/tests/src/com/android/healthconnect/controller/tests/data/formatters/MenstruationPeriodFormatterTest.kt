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

package com.android.healthconnect.controller.tests.data.formatters

import android.content.Context
import android.health.connect.datatypes.MenstruationPeriodRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.data.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.units.UnitPreferences
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Duration.ofDays
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MenstruationPeriodFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: MenstruationPeriodFormatter
    @Inject lateinit var preferences: UnitPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()
    }

    @Test
    fun formatForDay_dayOne_formatsMenstruationPeriod() = runBlocking {
        val start = NOW
        val end = NOW.plus(ofDays(5))
        val day = NOW
        val record = MenstruationPeriodRecord.Builder(getMetaData(), start, end).build()

        assertThat(formatter.format(day, record, period = DateNavigationPeriod.PERIOD_DAY))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = record.metadata.id,
                    header = "Oct 20 – 25 • $TEST_APP_NAME",
                    headerA11y = "Oct 20 – 25 • $TEST_APP_NAME",
                    title = "Period day 1 of 6",
                    titleA11y = "Period day 1 of 6",
                    dataType = MenstruationPeriodRecord::class,
                    startTime = start,
                    endTime = end,
                )
            )
    }

    @Test
    fun formatForDay_lastDay_formatsMenstruationPeriod() = runBlocking {
        val start = NOW
        val end = NOW.plus(ofDays(5))
        val record = MenstruationPeriodRecord.Builder(getMetaData(), start, end).build()

        assertThat(formatter.format(end, record, DateNavigationPeriod.PERIOD_DAY))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = record.metadata.id,
                    header = "Oct 20 – 25 • $TEST_APP_NAME",
                    headerA11y = "Oct 20 – 25 • $TEST_APP_NAME",
                    title = "Period day 6 of 6",
                    titleA11y = "Period day 6 of 6",
                    dataType = MenstruationPeriodRecord::class,
                    startTime = start,
                    endTime = end,
                )
            )
    }

    @Test
    fun formatForWeek_formatsMenstruationPeriod() = runBlocking {
        val start = NOW
        val end = NOW.plus(ofDays(5))
        val record = MenstruationPeriodRecord.Builder(getMetaData(), start, end).build()
        val header = "Oct 20 – 25 • $TEST_APP_NAME"
        assertThat(
                formatter.format(
                    end,
                    record,
                    DateNavigationPeriod.PERIOD_WEEK,
                    showDataOrigin = true,
                )
            )
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = record.metadata.id,
                    header = header,
                    headerA11y = header,
                    title = "Period (6 days)",
                    titleA11y = "Period (6 days)",
                    dataType = MenstruationPeriodRecord::class,
                    startTime = start,
                    endTime = end,
                )
            )
    }

    @Test
    fun formatForMonth_formatsMenstruationPeriod() = runBlocking {
        val start = NOW
        val end = NOW.plus(ofDays(0))
        val record = MenstruationPeriodRecord.Builder(getMetaData(), start, end).build()
        val header = "October 20 • $TEST_APP_NAME"
        assertThat(
                formatter.format(
                    end,
                    record,
                    DateNavigationPeriod.PERIOD_MONTH,
                    showDataOrigin = true,
                )
            )
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = record.metadata.id,
                    header = header,
                    headerA11y = header,
                    title = "Period (1 day)",
                    titleA11y = "Period (1 day)",
                    dataType = MenstruationPeriodRecord::class,
                    startTime = start,
                    endTime = end,
                )
            )
    }
}
