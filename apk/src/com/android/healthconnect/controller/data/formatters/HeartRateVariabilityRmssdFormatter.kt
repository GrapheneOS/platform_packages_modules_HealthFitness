/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 *
 */

package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord
import android.icu.text.MessageFormat.format
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.formatters.shared.EntryFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing HeartRateVariabilityRmssdRecord data. */
class HeartRateVariabilityRmssdFormatter
@Inject
constructor(
    @ApplicationContext context: Context,
    timeFormatter: LocalDateTimeFormatter,
    unitPreferences: UnitPreferences,
) : EntryFormatter<HeartRateVariabilityRmssdRecord>(context, timeFormatter, unitPreferences) {

    override suspend fun formatValue(record: HeartRateVariabilityRmssdRecord): String {
        return formatHRV(R.string.milliseconds, record.heartRateVariabilityMillis)
    }

    override suspend fun formatA11yValue(record: HeartRateVariabilityRmssdRecord): String {
        return formatHRV(R.string.milliseconds_long, record.heartRateVariabilityMillis)
    }

    private fun formatHRV(@StringRes res: Int, hrv: Double): String {
        return format(context.getString(res), mapOf("count" to hrv))
    }
}
