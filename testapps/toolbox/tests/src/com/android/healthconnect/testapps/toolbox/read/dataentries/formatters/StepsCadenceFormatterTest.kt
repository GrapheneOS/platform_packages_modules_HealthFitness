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
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsCadenceRecord.StepsCadenceRecordSample
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepsCadenceFormatterTest {

    private val formatter = StepsCadenceFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatStepsCadenceValue_returnsFormattedEntry() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val stepCadenceSample1 = 123.0
        val stepCadenceSample2 = 321.0

        val record = getStepsCadenceRecord(listOf(stepCadenceSample1, stepCadenceSample2), NOW)

        val formattedEntry = formatter.format(record, context)

        // 222.0 is derived from the sample's average
        assertThat(formattedEntry.dataEntry.value).isEqualTo("222.0 steps/min")
        assertThat(formattedEntry.dataDetails[0])
            .isEqualTo(FormattedSample(header = "16:36", value = "123.0 steps/min"))
        assertThat(formattedEntry.dataDetails[1])
            .isEqualTo(FormattedSample(header = "16:36", value = "321.0 steps/min"))
    }

    private fun getStepsCadenceRecord(
        samples: List<Double>,
        startTime: Instant,
    ): StepsCadenceRecord {
        return StepsCadenceRecord.Builder(
                getMetaData(context),
                startTime,
                startTime.plusSeconds(samples.size.toLong()),
                samples.map { StepsCadenceRecordSample(it, startTime) },
            )
            .build()
    }
}
