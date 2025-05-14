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
import android.health.connect.datatypes.BodyTemperatureRecord
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.formatters.shared.MeasurementLocationFormatter
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class BodyTemperatureFormatter {

    fun format(record: BodyTemperatureRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(header = getHeader(record), value = formatValue(record, context))
    }

    private fun formatValue(record: BodyTemperatureRecord, context: Context): String {
        val measurementLocationFormatter = MeasurementLocationFormatter()
        return "${record.temperature} ${measurementLocationFormatter.formatBodyTemperature(record.measurementLocation, context)}"
    }
}
