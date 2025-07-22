/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.MindfulnessSessionFormatter
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MindfulnessSessionFormatterTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: MindfulnessSessionFormatter
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun format_emptyRecord() = runBlocking {
        val startTime = Instant.parse("2022-10-20T07:06:05.432Z")
        val record =
            MindfulnessSessionRecord.Builder(
                    Metadata.Builder().build(),
                    startTime,
                    startTime.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN,
                )
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.ExerciseSessionEntry(
                    uuid = "",
                    header = "7:06 AM - 7:22 AM • com.app.name",
                    headerA11y = "from 7:06 AM to 7:22 AM • com.app.name",
                    title = "Unknown type • 16m",
                    titleA11y = "Unknown type • 16 minutes",
                    dataType = MindfulnessSessionRecord::class,
                    notes = null,
                    route = null,
                    isClickable = false,
                )
            )
    }

    @Test
    fun fullRecord() = runBlocking {
        val record =
            MindfulnessSessionRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
                )
                .setTitle("foo-title")
                .setNotes("foo-notes")
                .setStartZoneOffset(ZoneOffset.ofHours(1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.ExerciseSessionEntry(
                    uuid = "test_id",
                    header = "7:06 AM - 7:22 AM • com.app.name",
                    headerA11y = "from 7:06 AM to 7:22 AM • com.app.name",
                    title = "Meditation • foo-title",
                    titleA11y = "Meditation • foo-title",
                    dataType = MindfulnessSessionRecord::class,
                    notes = "foo-notes",
                    route = null,
                    isClickable = false,
                )
            )
    }

    @Test
    fun formatUnit_returnsMindfulnessUnit() {
        runTest {
            val duration = Duration.ofHours(10).plusMinutes(54).toMillis()

            val formattedValue = formatter.formatUnit(duration)

            assertThat(formattedValue).isEqualTo("10h 54m")
        }
    }

    @Test
    fun formatA11yUnit_returnsA11yMindfulnessUnit() {
        runTest {
            val duration = Duration.ofHours(10).plusMinutes(54).toMillis()

            val formattedValue = formatter.formatA11yUnit(duration)

            assertThat(formattedValue).isEqualTo("Total mindfulness time of 10 hours 54 minutes")
        }
    }

    @Test
    fun formatUnit_returnsMindfulnessUnitLargeNumber() {
        runTest {
            val duration = Duration.ofHours(200).plusMinutes(59).toMillis()

            val formattedValue = formatter.formatUnit(duration)

            assertThat(formattedValue).isEqualTo("200h 59m")
        }
    }

    @Test
    fun formatA11yUnit_returnsA11yMindfulnessUnitLargeNumber() {
        runTest {
            val duration = Duration.ofHours(200).plusMinutes(59).toMillis()

            val formattedValue = formatter.formatA11yUnit(duration)

            assertThat(formattedValue).isEqualTo("Total mindfulness time of 200 hours 59 minutes")
        }
    }

    @Test
    fun formatUnitMinutes_returnsMindfulnessUnitMinutes() {
        runTest {
            val duration = Duration.ofMinutes(14).toMillis()

            val formattedValue = formatter.formatUnit(duration)

            assertThat(formattedValue).isEqualTo("14m")
        }
    }

    @Test
    fun formatA11yUnitMinutes_returnsA11yMindfulnessUnitMinutes() {
        runTest {
            val duration = Duration.ofMinutes(14).toMillis()

            val formattedValue = formatter.formatA11yUnit(duration)

            assertThat(formattedValue).isEqualTo("Total mindfulness time of 14 minutes")
        }
    }

    @Test
    fun formatUnitMinutes_returnsMindfulnessUnitZeroMinutes() {
        runTest {
            val duration = Duration.ofMinutes(0).toMillis()

            val formattedValue = formatter.formatUnit(duration)

            assertThat(formattedValue).isEqualTo("0m")
        }
    }

    @Test
    fun formatA11yUnitMinutes_returnsA11yMindfulnessUnitZeroMinutes() {
        runTest {
            val duration = Duration.ofMinutes(0).toMillis()

            val formattedValue = formatter.formatA11yUnit(duration)

            assertThat(formattedValue).isEqualTo("Total mindfulness time of 0 minutes")
        }
    }

    @Test
    fun formatUnitHours_returnsMindfulnessUnitHours() {
        runTest {
            val duration = Duration.ofHours(14).toMillis()

            val formattedValue = formatter.formatUnit(duration)

            assertThat(formattedValue).isEqualTo("14h")
        }
    }

    @Test
    fun formatA11yUnitHours_returnsA11yMindfulnessUnitHours() {
        runTest {
            val duration = Duration.ofHours(14).toMillis()

            val formattedValue = formatter.formatA11yUnit(duration)

            assertThat(formattedValue).isEqualTo("Total mindfulness time of 14 hours")
        }
    }
}
