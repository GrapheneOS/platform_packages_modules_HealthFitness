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
import android.health.connect.datatypes.OvulationTestRecord
import android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult.RESULT_HIGH
import android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult.RESULT_INCONCLUSIVE
import android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult.RESULT_NEGATIVE
import android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult.RESULT_POSITIVE
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OvulationTestFormatterTest {
    private val formatter = OvulationTestFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatOvulationTestValue_returnsFormattedEntry_inconclusive() {
        val record = buildOvulationTestRecord(RESULT_INCONCLUSIVE).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Inconclusive result")
    }

    @Test
    fun formatOvulationTestValue_returnsFormattedEntry_positive() {
        val record = buildOvulationTestRecord(RESULT_POSITIVE).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Positive / peak fertility")
    }

    @Test
    fun formatOvulationTestValue_returnsFormattedEntry_high() {
        val record = buildOvulationTestRecord(RESULT_HIGH).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("High fertility")
    }

    @Test
    fun formatOvulationTestValue_returnsFormattedEntry_negative() {
        val record = buildOvulationTestRecord(RESULT_NEGATIVE).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Negative / low fertility")
    }

    private fun buildOvulationTestRecord(result: Int): OvulationTestRecord.Builder {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return OvulationTestRecord.Builder(getMetaData(context), NOW, result)
    }
}
