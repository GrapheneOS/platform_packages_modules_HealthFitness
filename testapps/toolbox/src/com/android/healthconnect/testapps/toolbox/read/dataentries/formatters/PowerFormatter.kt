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
import android.health.connect.datatypes.PowerRecord
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class PowerFormatter {

    fun format(record: PowerRecord, context: Context): FormattedDataDetails {
        return FormattedDataDetails(
            dataEntry = formatDataEntry(record, context),
            dataDetails = formatSamples(record.samples, context),
        )
    }

    private fun formatDataEntry(record: PowerRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(
            header = getHeader(record),
            value = getPowerAverage(record.samples, context),
        )
    }

    private fun getPowerAverage(
        samples: List<PowerRecord.PowerRecordSample>,
        context: Context,
    ): String {
        val powerAvg = samples.sumOf { it.power.inWatts } / samples.size
        return UnitFormatter.formatPower(powerAvg, context)
    }

    private fun formatSamples(
        samples: List<PowerRecord.PowerRecordSample>,
        context: Context,
    ): List<FormattedSample> {
        val formattedSamples = mutableListOf<FormattedSample>()

        samples.forEach {
            formattedSamples.add(
                FormattedSample(
                    header = getHeader(it.time),
                    value = UnitFormatter.formatPower(it.power, context),
                )
            )
        }

        return formattedSamples
    }
}
