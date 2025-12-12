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
package com.android.healthconnect.controller.tests.data.formatters

import android.content.Context
import android.health.connect.datatypes.BasalMetabolicRateRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MenstrualCyclePhaseRecord
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.SeriesDataEntry
import com.android.healthconnect.controller.data.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.getBasalMetabolicRateRecord
import com.android.healthconnect.controller.tests.utils.getHeartRateRecord
import com.android.healthconnect.controller.tests.utils.getMenstrualCyclePhaseRecord
import com.android.healthconnect.controller.tests.utils.getSamplePlannedExerciseSessionRecord
import com.android.healthconnect.controller.tests.utils.getStepsRecord
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthDataEntryFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @BindValue lateinit var appInfoReader: AppInfoReader
    @Inject lateinit var formatter: HealthDataEntryFormatter

    private lateinit var context: Context

    @Before
    fun setup() = runTest {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        appInfoReader = createFakeAppInfoReader()

        hiltRule.inject()
    }

    @Test
    fun format_formatsHeartRateRecord() {
        val heartRateRecord = getHeartRateRecord(listOf(80, 81, 100))
        runBlocking {
            assertThat(formatter.format(heartRateRecord))
                .isEqualTo(
                    SeriesDataEntry(
                        uuid = "test_id",
                        header = "07:06 - 07:06 • Health Connect test app",
                        headerA11y = "from 07:06 to 07:06 • Health Connect test app",
                        title = "80 bpm - 100 bpm",
                        titleA11y = "from 80 beats per minute to 100 beats per minute",
                        dataType = HeartRateRecord::class,
                    )
                )
        }
    }

    @Test
    fun format_formatsStepsRateRecord() {
        val stepsRecord = getStepsRecord(12)
        runBlocking {
            assertThat(formatter.format(stepsRecord))
                .isEqualTo(
                    FormattedDataEntry(
                        uuid = "test_id",
                        header = "07:06 - 07:06 • Health Connect test app",
                        headerA11y = "from 07:06 to 07:06 • Health Connect test app",
                        title = "12 steps",
                        titleA11y = "12 steps",
                        dataType = StepsRecord::class,
                    )
                )
        }
    }

    @Test
    fun format_formatsBasalMetabolicRateRecord() = runBlocking {
        val record = getBasalMetabolicRateRecord(calories = 1548)

        assertThat(formatter.format(record))
            .isEqualTo(
                FormattedDataEntry(
                    uuid = "test_id",
                    header = "07:06 • Health Connect test app",
                    headerA11y = "07:06 • Health Connect test app",
                    title = "1,548 cal",
                    titleA11y = "1,548 calories",
                    dataType = BasalMetabolicRateRecord::class,
                )
            )
    }

    @Test
    fun format_formatsPlannedExerciseSessionRecord() {
        val plannedExerciseSessionRecord = getSamplePlannedExerciseSessionRecord()
        runBlocking {
            assertThat(formatter.format(plannedExerciseSessionRecord))
                .isEqualTo(
                    FormattedEntry.PlannedExerciseSessionEntry(
                        uuid = "test_id",
                        header = "07:06 - 08:06 • Health Connect test app",
                        headerA11y = "from 07:06 to 08:06 • Health Connect test app",
                        title = "Running • Morning Run",
                        titleA11y = "Running • Morning Run",
                        dataType = PlannedExerciseSessionRecord::class,
                        notes = "Morning quick run by the park",
                    )
                )
        }
    }

    @Test
    @RequiresFlagsEnabled(
        Flags.FLAG_CYCLE_PHASES_FLAG,
        Flags.FLAG_CYCLE_PHASES_DB,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB,
    )
    fun format_formatsMenstrualCyclePhaseRecord() {
        val record =
            getMenstrualCyclePhaseRecord(
                phase = MenstrualCyclePhaseRecord.PHASE_FOLLICULAR,
                dayOfCycle = 5,
            )
        runBlocking {
            assertThat(formatter.format(record))
                .isEqualTo(
                    FormattedDataEntry(
                        uuid = "test_id",
                        header = "20 Oct • Health Connect test app",
                        headerA11y = "20 October • Health Connect test app",
                        title = "Follicular Day 5",
                        titleA11y = "Follicular Day 5",
                        dataType = MenstrualCyclePhaseRecord::class,
                    )
                )
        }
    }
}
