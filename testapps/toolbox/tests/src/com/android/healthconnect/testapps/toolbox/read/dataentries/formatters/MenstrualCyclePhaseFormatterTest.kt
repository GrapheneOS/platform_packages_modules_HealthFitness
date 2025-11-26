/*
 * Copyright (C) 2025 The Android Open Source Project
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
 */
package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.MenstrualCyclePhaseRecord
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenstrualCyclePhaseFormatterTest {
    private val formatter = MenstrualCyclePhaseFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatMenstrualCyclePhaseValue_returnsFormattedEntry_follicular() {
        val record =
            buildMenstrualCyclePhaseRecord(MenstrualCyclePhaseRecord.PHASE_FOLLICULAR, 1).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.header).isEqualTo("10/04")
        assertThat(formattedEntry.value).isEqualTo("Follicular Day 1")
    }

    @Test
    fun formatMenstrualCyclePhaseValue_returnsFormattedEntry_luteal() {
        val record = buildMenstrualCyclePhaseRecord(MenstrualCyclePhaseRecord.PHASE_LUTEAL).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.header).isEqualTo("10/04")
        assertThat(formattedEntry.value).isEqualTo("Luteal")
    }

    private fun buildMenstrualCyclePhaseRecord(
        phase: Int,
        dayOfCycle: Int? = null,
    ): MenstrualCyclePhaseRecord.Builder {
        val date = LocalDate.parse("2024-04-10")
        return MenstrualCyclePhaseRecord.Builder(getMetaData(context), date, phase).apply {
            dayOfCycle?.let { setDayOfCycle(it) }
        }
    }
}
