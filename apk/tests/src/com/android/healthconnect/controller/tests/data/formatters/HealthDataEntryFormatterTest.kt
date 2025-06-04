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
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.StepsRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.SeriesDataEntry
import com.android.healthconnect.controller.data.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.getBasalMetabolicRateRecord
import com.android.healthconnect.controller.tests.utils.getHeartRateRecord
import com.android.healthconnect.controller.tests.utils.getSamplePlannedExerciseSessionRecord
import com.android.healthconnect.controller.tests.utils.getStepsRecord
import com.android.healthconnect.controller.tests.utils.setLocale
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthDataEntryFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val appInfoReader: AppInfoReader = mock()
    @Inject lateinit var formatter: HealthDataEntryFormatter

    private lateinit var context: Context

    @Before
    fun setup() = runTest {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        whenever(appInfoReader.getAppMetadata(any(), any()))
            .thenReturn(AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null, false))

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
}
