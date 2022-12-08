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
import android.healthconnect.datatypes.DistanceRecord
import android.healthconnect.datatypes.units.Length
import android.icu.text.MessageFormat
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.units.DistanceUnit.KILOMETERS
import com.android.healthconnect.controller.dataentries.units.DistanceUnit.MILES
import com.android.healthconnect.controller.dataentries.units.LengthConverter
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing Distance data. */
class DistanceFormatter @Inject constructor(@ApplicationContext private val context: Context) :
    DataEntriesFormatter<DistanceRecord>(context) {

    override suspend fun formatValue(
        record: DistanceRecord,
        unitPreferences: UnitPreferences
    ): String {
        val res =
            when (unitPreferences.getDistanceUnit()) {
                KILOMETERS -> R.string.distance_km
                MILES -> R.string.distance_miles
            }
        return formatSample(res, record.distance, unitPreferences)
    }

    override suspend fun formatA11yValue(
        record: DistanceRecord,
        unitPreferences: UnitPreferences
    ): String {
        val res =
            when (unitPreferences.getDistanceUnit()) {
                KILOMETERS -> R.string.distance_km_long
                MILES -> R.string.distance_miles_long
            }
        return formatSample(res, record.distance, unitPreferences)
    }

    private fun formatSample(
        @StringRes res: Int,
        length: Length,
        unitPreferences: UnitPreferences
    ): String {
        val value =
            LengthConverter.convertDistanceFromMeters(
                unitPreferences.getDistanceUnit(), length.inMeters)
        return MessageFormat.format(context.getString(res), mapOf("dist" to value))
    }
}
