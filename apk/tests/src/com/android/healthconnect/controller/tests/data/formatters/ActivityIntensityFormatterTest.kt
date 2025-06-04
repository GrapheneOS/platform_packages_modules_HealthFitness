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

import android.health.connect.datatypes.ActivityIntensityRecord
import android.health.connect.datatypes.Metadata
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.ActivityIntensityFormatter
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthfitness.flags.Flags
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(
    Flags.FLAG_ACTIVITY_INTENSITY,
    Flags.FLAG_ACTIVITY_INTENSITY_DB,
    Flags.FLAG_HEALTH_CONNECT_MAPPINGS,
)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ActivityIntensityFormatterTest {
    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: ActivityIntensityFormatter

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()
    }

    @Test
    fun format_moderate() = runBlocking {
        val startTime = Instant.parse("2022-10-20T07:06:05.432Z")
        val record =
            ActivityIntensityRecord.Builder(
                    Metadata.Builder().build(),
                    startTime,
                    startTime.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE,
                )
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "Moderate",
                    titleA11y = "Moderate",
                    dataType = ActivityIntensityRecord::class,
                )
            )
    }

    @Test
    fun format_vigorous() = runBlocking {
        val record =
            ActivityIntensityRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS,
                )
                .setStartZoneOffset(ZoneOffset.ofHours(1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "Vigorous",
                    titleA11y = "Vigorous",
                    dataType = ActivityIntensityRecord::class,
                )
            )
    }
}
