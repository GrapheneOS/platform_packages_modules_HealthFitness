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
package com.android.healthconnect.testapps.toolbox.read.utils

import android.health.connect.datatypes.InstantRecord
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.Record
import com.android.healthconnect.testapps.toolbox.read.components.ComposableView
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.AggregationHeaderEntry
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.DataDetailsEntry
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.DataEntry
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.ExerciseSessionEntry
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.HeaderEntry
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.NutritionEntry
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.PlannedExerciseSessionEntry
import com.android.healthconnect.testapps.toolbox.read.components.baseentries.SampleEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedAggregation
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedExerciseSession
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedNutritionEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseSession
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.Header
import com.android.healthconnect.testapps.toolbox.utils.asString
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DataEntryUtils {

    companion object {

        fun getHeader(record: Record): String {
            return getTimeField(record)
        }

        fun getHeader(start: Instant, end: Instant): String {
            return "${formatTime(start)} - ${formatTime(end)}"
        }

        fun getHeader(time: Instant): String {
            return "${formatTime(time)}"
        }

        private fun getTimeField(record: Record): String {
            return when (record) {
                is IntervalRecord ->
                    "${formatTime(record.startTime)} - ${formatTime(record.endTime)}"
                is InstantRecord -> "${formatTime(record.time)}"
                else -> record.asString()
            }
        }

        private fun formatTime(instant: Instant): String? {
            val localTime: LocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
            return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        // Maps the given formatted entry to its corresponding view.
        fun mapEntryToComposable(entry: FormattedEntry): ComposableView {

            when (entry) {
                is FormattedAggregation -> return { AggregationHeaderEntry(entry) }
                is FormattedDataDetails -> return { DataDetailsEntry(entry) }
                is FormattedDataEntry -> return { DataEntry(entry) }
                is FormattedExerciseSession -> return { ExerciseSessionEntry(entry) }
                is FormattedNutritionEntry -> return { NutritionEntry(entry) }
                is FormattedPlannedExerciseSession -> return { PlannedExerciseSessionEntry(entry) }
                is FormattedSample -> return { SampleEntry(entry) }
                is Header -> return { HeaderEntry(entry) }

                else -> throw IllegalArgumentException("Unsupported entry `${entry.javaClass}`")
            }
        }
    }
}
