/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.healthconnect.datatypes.StepsCadenceRecord
import android.healthconnect.datatypes.StepsCadenceRecord.StepsCadenceRecordSample
import android.icu.text.MessageFormat
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.formatters.shared.EntryFormatter
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing StepsCadence series data. */
class StepsCadenceFormatter @Inject constructor(@ApplicationContext private val context: Context) :
    EntryFormatter<StepsCadenceRecord>(context) {

    /** Returns localized average StepsCadence from multiple data points. */
    override suspend fun formatValue(
        record: StepsCadenceRecord,
        unitPreferences: UnitPreferences
    ): String {
        return formatRange(R.string.steps_per_minute, record.samples)
    }

    /** Returns localized StepsCadence value. */
    override suspend fun formatA11yValue(
        record: StepsCadenceRecord,
        unitPreferences: UnitPreferences
    ): String {
        return formatRange(R.string.steps_per_minute_long, record.samples)
    }

    private fun formatRange(@StringRes res: Int, samples: List<StepsCadenceRecordSample>): String {
        return if (samples.isEmpty()) {
            context.getString(R.string.no_data)
        } else {
            val avrStepsCadence = samples.sumOf { it.rate } / samples.size
            MessageFormat.format(context.getString(res), mapOf("value" to avrStepsCadence))
        }
    }
}
