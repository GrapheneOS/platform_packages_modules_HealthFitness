/**
 * Copyright (C) 2025 The Android Open Source Project
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
import android.health.connect.datatypes.MenstrualCyclePhaseRecord
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.formatters.MenstrualCyclePhaseFormatter
import com.android.healthconnect.controller.tests.utils.getMenstrualCyclePhaseRecord
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Ignore("b/465390591 - Re-enable once the test is fixed.")
@RequiresFlagsEnabled(
    Flags.FLAG_CYCLE_PHASES_FLAG,
    Flags.FLAG_CYCLE_PHASES_DB,
    Flags.FLAG_SMOKING_DB,
    Flags.FLAG_SYMPTOMS_DB,
    Flags.FLAG_ALCOHOL_CONSUMPTION_DB,
)
class MenstrualCyclePhaseFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Inject lateinit var formatter: MenstrualCyclePhaseFormatter
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun formatValue_follicularPhase_showsPhase() = runBlocking {
        val record =
            getMenstrualCyclePhaseRecord(phase = MenstrualCyclePhaseRecord.PHASE_FOLLICULAR)
        assertThat(formatter.formatValue(record)).isEqualTo("Follicular")
    }

    @Test
    fun formatValue_lutealPhase_showsPhase() = runBlocking {
        val record = getMenstrualCyclePhaseRecord(phase = MenstrualCyclePhaseRecord.PHASE_LUTEAL)
        assertThat(formatter.formatValue(record)).isEqualTo("Luteal")
    }

    @Test
    fun formatValue_follicularPhaseWithDay_showsPhaseAndDay() = runBlocking {
        val record =
            getMenstrualCyclePhaseRecord(
                phase = MenstrualCyclePhaseRecord.PHASE_FOLLICULAR,
                dayOfCycle = 5,
            )
        assertThat(formatter.formatValue(record)).isEqualTo("Follicular Day 5")
    }

    @Test
    fun formatValue_lutealPhaseWithDay_showsPhaseAndDay() = runBlocking {
        val record =
            getMenstrualCyclePhaseRecord(
                phase = MenstrualCyclePhaseRecord.PHASE_LUTEAL,
                dayOfCycle = 15,
            )
        assertThat(formatter.formatValue(record)).isEqualTo("Luteal Day 15")
    }
}
