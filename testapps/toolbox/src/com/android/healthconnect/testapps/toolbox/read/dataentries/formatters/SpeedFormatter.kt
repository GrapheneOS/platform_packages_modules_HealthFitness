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
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.round
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class SpeedFormatter {

    fun format(record: SpeedRecord, context: Context): FormattedDataDetails {
        return FormattedDataDetails(
            dataEntry = formatDataEntry(record, context),
            dataDetails = formatSamples(record.samples, context),
        )
    }

    private fun formatDataEntry(record: SpeedRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(
            header = getHeader(record),
            value = getSpeedAverage(record.samples, context),
        )
    }

    private fun getSpeedAverage(
        samples: List<SpeedRecord.SpeedRecordSample>,
        context: Context,
    ): String {
        val speedAvg =
            round(
                value = samples.sumOf { it.speed.inMetersPerSecond } / samples.size * 3.6,
                scale = 1,
            )
        return "$speedAvg ${context.getString(R.string.kilometers_per_hour_label)}"
    }

    private fun formatSamples(
        samples: List<SpeedRecord.SpeedRecordSample>,
        context: Context,
    ): List<FormattedSample> {
        val formattedSamples = mutableListOf<FormattedSample>()

        samples.forEach {
            formattedSamples.add(
                FormattedSample(
                    header = getHeader(it.time),
                    value = UnitFormatter.formatVelocity(it.speed, context),
                )
            )
        }

        return formattedSamples
    }
}
