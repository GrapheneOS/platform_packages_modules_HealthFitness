/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.healthconnect.datatypes.HeartRateRecord
import android.icu.text.MessageFormat
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Formatter for printing HeartRate data. */
@Singleton
class HeartRateFormatter @Inject constructor(@ApplicationContext private val context: Context) :
    DataEntriesFormatter<HeartRateRecord>(context) {

    override suspend fun formatValue(record: HeartRateRecord): String {
        return if (record.samples.size == 1) {
            formatSampleValue(R.string.heart_rate_value, record.samples.first().beatsPerMinute)
        } else {
            formatRange(R.string.heart_rate_series_range, record) { heartRate: Long ->
                formatSampleValue(R.string.heart_rate_value, heartRate)
            }
        }
    }

    override suspend fun formatA11yValue(record: HeartRateRecord): String {
        return if (record.samples.size == 1) {
            formatSampleValue(R.string.heart_rate_long_value, record.samples.first().beatsPerMinute)
        } else {
            return formatRange(
                R.string.heart_rate_series_range_long,
                record,
            ) { heartRate: Long ->
                formatSampleValue(R.string.heart_rate_long_value, heartRate)
            }
        }
    }

    private fun formatSampleValue(@StringRes res: Int, heartRate: Long): String {
        return MessageFormat.format(context.getString(res), mapOf("count" to heartRate))
    }

    private fun formatRange(
        @StringRes res: Int,
        record: HeartRateRecord,
        getSample: (heartRate: Long) -> String
    ): String {
        val min = record.samples.minOf { it.beatsPerMinute }
        val max = record.samples.maxOf { it.beatsPerMinute }
        return context.getString(res, getSample(min), getSample(max))
    }
}
