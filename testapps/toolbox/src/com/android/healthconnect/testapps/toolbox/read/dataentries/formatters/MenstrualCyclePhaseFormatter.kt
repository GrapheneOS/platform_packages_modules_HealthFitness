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
import android.health.connect.datatypes.MenstrualCyclePhaseRecord
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class MenstrualCyclePhaseFormatter {
    val MenstrualCyclePhaseRecord.dayOfCycleOrNull: Int?
        get() = if (isDayOfCycleSet) dayOfCycle else null

    fun format(
        record: MenstrualCyclePhaseRecord,
        context: Context,
    ): FormattedEntry.FormattedDataEntry {
        return FormattedEntry.FormattedDataEntry(
            header = getHeader(record.date),
            value = formatPhase(record.phase, record.dayOfCycleOrNull),
        )
    }

    private fun formatPhase(phase: Int, dayOfCycle: Int?): String {
        val phaseString =
            when (phase) {
                MenstrualCyclePhaseRecord.PHASE_FOLLICULAR -> "Follicular"
                MenstrualCyclePhaseRecord.PHASE_LUTEAL -> "Luteal"
                else -> "Unknown"
            }
        return dayOfCycle?.let { day -> "$phaseString Day $day" } ?: phaseString
    }
}
