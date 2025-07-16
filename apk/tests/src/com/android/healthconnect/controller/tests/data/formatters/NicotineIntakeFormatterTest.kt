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
package com.android.healthconnect.controller.tests.data.formatters

import android.content.Context
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE
import android.health.connect.datatypes.units.Mass
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.NicotineIntakeFormatter
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Duration
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@RequiresFlagsEnabled(Flags.FLAG_SMOKING, Flags.FLAG_SMOKING_DB)
@HiltAndroidTest
class NicotineIntakeFormatterTest {
    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: NicotineIntakeFormatter
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun formatValue_formatsAllFields() = runBlocking {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    10,
                    NICOTINE_INTAKE_TYPE_VAPE,
                )
                .setNicotineIntake(Mass.fromGrams(0.5))
                .setStartZoneOffset(ZoneOffset.ofHours(1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "10 vape puffs • 500 mg",
                    titleA11y = "10 vape puffs • 500 milligrams",
                    dataType = NicotineIntakeRecord::class,
                )
            )
    }

    @Test
    fun formatValue_formatsQuantityAndType() = runBlocking {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    10,
                    NICOTINE_INTAKE_TYPE_CIGARETTE,
                )
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "10 cigarettes • 0 mg",
                    titleA11y = "10 cigarettes • 0 milligrams",
                    dataType = NicotineIntakeRecord::class,
                )
            )
    }

    @Test
    fun formatValue_withOneQuantity_formatsTypeCorrectly() = runBlocking {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    1,
                    NICOTINE_INTAKE_TYPE_CIGARETTE,
                )
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "1 cigarette • 0 mg",
                    titleA11y = "1 cigarette • 0 milligrams",
                    dataType = NicotineIntakeRecord::class,
                )
            )
    }

    @Test
    fun formatValue_formatsDecimalCorrectly() = runBlocking {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    1,
                    NICOTINE_INTAKE_TYPE_VAPE,
                )
                .setNicotineIntake(Mass.fromGrams(0.0005))
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "1 vape puff • 0.5 mg",
                    titleA11y = "1 vape puff • 0.5 milligrams",
                    dataType = NicotineIntakeRecord::class,
                )
            )
    }

    @Test
    fun formatValue_formatsMilligramUnitCorrectly() = runBlocking {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    1,
                    NICOTINE_INTAKE_TYPE_VAPE,
                )
                .setNicotineIntake(Mass.fromGrams(0.001))
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "1 vape puff • 1 mg",
                    titleA11y = "1 vape puff • 1 milligram",
                    dataType = NicotineIntakeRecord::class,
                )
            )
    }

    @Test
    fun formatValue_formatsIntakeValueToTwoDecimalPlaces() = runBlocking {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(),
                    NOW,
                    NOW.plus(Duration.ofMinutes(16).plusSeconds(40)),
                    3,
                    NICOTINE_INTAKE_TYPE_VAPE,
                )
                .setNicotineIntake(Mass.fromGrams(0.0015554))
                .build()

        assertThat(formatter.format(record, "com.app.name"))
            .isEqualTo(
                FormattedEntry.FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 - 07:22 • com.app.name",
                    headerA11y = "from 07:06 to 07:22 • com.app.name",
                    title = "3 vape puffs • 1.56 mg",
                    titleA11y = "3 vape puffs • 1.56 milligrams",
                    dataType = NicotineIntakeRecord::class,
                )
            )
    }
}
