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
import android.health.connect.datatypes.HeartRateRecord
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeartRateFormatterTest {
    private val formatter = HeartRateFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatHeartRateEntry_returnsFormattedEntry() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val samples =
            listOf(
                HeartRateRecord.HeartRateSample(70, NOW.minusSeconds(30)),
                HeartRateRecord.HeartRateSample(85, NOW.minusSeconds(20)),
                HeartRateRecord.HeartRateSample(90, NOW.minusSeconds(10)),
            )

        val record = getHeartRateRecordBuilder(NOW, samples).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("70 - 90 bpm")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(
                    FormattedEntry.FormattedSample(header = "16:35", value = "70 bpm"),
                    FormattedEntry.FormattedSample(header = "16:35", value = "85 bpm"),
                    FormattedEntry.FormattedSample(header = "16:36", value = "90 bpm"),
                )
            )
    }

    private fun getHeartRateRecordBuilder(
        endTime: Instant,
        samples: List<HeartRateRecord.HeartRateSample>,
    ): HeartRateRecord.Builder {
        return HeartRateRecord.Builder(
            getMetaData(context),
            endTime.minusSeconds(35),
            endTime,
            samples,
        )
    }
}
