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
import android.health.connect.datatypes.ActivityIntensityRecord
import android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE
import android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class ActivityIntensityFormatter {

    fun format(record: ActivityIntensityRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(
            header = getHeader(record),
            value = formatIntensityType(record.activityIntensityType, context),
        )
    }

    private fun formatIntensityType(intensityType: Int, context: Context): String {
        return when (intensityType) {
            ACTIVITY_INTENSITY_TYPE_MODERATE ->
                context.getString(R.string.activity_intensity_moderate)
            ACTIVITY_INTENSITY_TYPE_VIGOROUS ->
                context.getString(R.string.activity_intensity_vigorous)
            else -> context.getString(R.string.activity_intensity_unknown)
        }
    }
}
