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
import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.SkinTemperatureRecord.Delta
import android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST
import android.health.connect.datatypes.units.Temperature
import android.health.connect.datatypes.units.TemperatureDelta
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkinTemperatureFormatterTest {
    private val formatter = SkinTemperatureFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatSkinTemperature_returnsFormattedEntry_noMeasurementLocation() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val baseline = 25.2
        val deltas =
            listOf(
                Delta(TemperatureDelta.fromCelsius(-0.7), NOW.minusSeconds(30)),
                Delta(TemperatureDelta.fromCelsius(0.0), NOW.minusSeconds(25)),
                Delta(TemperatureDelta.fromCelsius(1.4), NOW.minusSeconds(20)),
            )
        val record =
            getSkinTemperatureRecordBuilder(NOW.minusSeconds(35), NOW, baseline, deltas).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("0.3 celsius")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(
                    FormattedDataEntry("Baseline", "25.2 celsius"),
                    FormattedSample("16:35", "-0.7 celsius"),
                    FormattedSample("16:35", "0.0 celsius"),
                    FormattedSample("16:35", "1.4 celsius"),
                )
            )
    }

    @Test
    fun formatSkinTemperature_returnsFormattedEntry_withMeasurementLocation() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val baseline = 25.2
        val deltas =
            listOf(
                Delta(TemperatureDelta.fromCelsius(-0.7), NOW.minusSeconds(30)),
                Delta(TemperatureDelta.fromCelsius(0.0), NOW.minusSeconds(25)),
                Delta(TemperatureDelta.fromCelsius(1.4), NOW.minusSeconds(20)),
            )
        val record =
            getSkinTemperatureRecordBuilder(NOW.minusSeconds(35), NOW, baseline, deltas)
                .setMeasurementLocation(MEASUREMENT_LOCATION_WRIST)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("0.3 celsius")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(
                    FormattedDataEntry("Measurement Location", "Wrist"),
                    FormattedDataEntry("Baseline", "25.2 celsius"),
                    FormattedSample("16:35", "-0.7 celsius"),
                    FormattedSample("16:35", "0.0 celsius"),
                    FormattedSample("16:35", "1.4 celsius"),
                )
            )
    }

    private fun getSkinTemperatureRecordBuilder(
        startTime: Instant,
        endTime: Instant,
        baseLine: Double,
        deltas: List<Delta>,
    ): SkinTemperatureRecord.Builder {
        return SkinTemperatureRecord.Builder(getMetaData(context), startTime, endTime)
            .setBaseline(Temperature.fromCelsius(baseLine))
            .setDeltas(deltas)
    }
}
