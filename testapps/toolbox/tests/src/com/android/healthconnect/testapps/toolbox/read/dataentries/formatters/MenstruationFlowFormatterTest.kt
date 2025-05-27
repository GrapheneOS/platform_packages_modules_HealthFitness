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
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_HEAVY
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_LIGHT
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_MEDIUM
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_UNKNOWN
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenstruationFlowFormatterTest {
    private val formatter = MenstruationFlowFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatMenstruationFlowValue_returnsFormattedEntry_light() {
        val record = getCervicalMucusFlowRecord(FLOW_LIGHT)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Light")
    }

    @Test
    fun formatMenstruationFlowValue_returnsFormattedEntry_heavy() {
        val record = getCervicalMucusFlowRecord(FLOW_HEAVY)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Heavy")
    }

    @Test
    fun formatMenstruationFlowValue_returnsFormattedEntry_medium() {
        val record = getCervicalMucusFlowRecord(FLOW_MEDIUM)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Medium")
    }

    @Test
    fun formatMenstruationFlowValue_returnsFormattedEntry_unknown() {
        val record = getCervicalMucusFlowRecord(FLOW_UNKNOWN)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("Unknown Flow")
    }

    private fun getCervicalMucusFlowRecord(flowType: Int): MenstruationFlowRecord {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return MenstruationFlowRecord.Builder(getMetaData(context), NOW, flowType).build()
    }
}
