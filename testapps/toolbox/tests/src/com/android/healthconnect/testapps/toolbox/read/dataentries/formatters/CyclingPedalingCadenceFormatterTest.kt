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
import android.health.connect.datatypes.CyclingPedalingCadenceRecord
import android.health.connect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CyclingPedalingCadenceFormatterTest {

    private val formatter = CyclingPedalingCadenceFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatCyclingPedalingCadenceValue_returnsFormattedEntry() {

        val record = getCyclingPedalingCadenceRecord(listOf(30.0, 42.0))

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("36.0 rpm")
        assertThat(formattedEntry.dataDetails[0])
            .isEqualTo(FormattedSample(header = "16:36", value = "30.0 rpm"))
        assertThat(formattedEntry.dataDetails[1])
            .isEqualTo(FormattedSample(header = "16:36", value = "42.0 rpm"))
    }

    private fun getCyclingPedalingCadenceRecord(
        samples: List<Double>
    ): CyclingPedalingCadenceRecord {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return CyclingPedalingCadenceRecord.Builder(
                getMetaData(context),
                NOW,
                NOW.plusSeconds(samples.size.toLong()),
                samples.map { CyclingPedalingCadenceRecordSample(it, NOW) },
            )
            .build()
    }
}
