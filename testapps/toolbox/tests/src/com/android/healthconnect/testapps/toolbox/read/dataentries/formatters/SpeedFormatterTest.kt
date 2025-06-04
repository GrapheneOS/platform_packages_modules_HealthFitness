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
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.units.Velocity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeedFormatterTest {

    private val formatter = SpeedFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatSpeedValue_returnsFormattedEntry() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val record = getSpeedRecord(listOf(16.0, 18.0), NOW)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("61.2 km/h")
        assertThat(formattedEntry.dataDetails[0])
            .isEqualTo(FormattedSample(header = "16:36", value = "57.6 km/h"))
        assertThat(formattedEntry.dataDetails[1])
            .isEqualTo(FormattedSample(header = "16:36", value = "64.8 km/h"))
    }

    private fun getSpeedRecord(samples: List<Double>, startTime: Instant): SpeedRecord {
        return SpeedRecord.Builder(
                getMetaData(context),
                startTime,
                startTime.plusSeconds(samples.size.toLong()),
                samples.map {
                    SpeedRecord.SpeedRecordSample(Velocity.fromMetersPerSecond(it), startTime)
                },
            )
            .build()
    }
}
