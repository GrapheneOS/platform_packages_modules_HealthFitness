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
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.DurationFormatter.formatDurationLong
import com.android.healthconnect.controller.data.formatters.DurationFormatter.formatDurationShort
import com.android.healthconnect.controller.data.formatters.shared.BaseFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject

/** Formatter for printing MindfulnessSessionRecord data. */
class MindfulnessSessionFormatter
@Inject
constructor(@ApplicationContext private val context: Context) :
    BaseFormatter<MindfulnessSessionRecord>(context) {

    override suspend fun formatRecord(
        record: MindfulnessSessionRecord,
        header: String,
        headerA11y: String,
        unitPreferences: UnitPreferences,
    ): FormattedEntry {
        return FormattedEntry.ExerciseSessionEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatValue(record),
            titleA11y = formatA11yValue(record),
            dataType = record::class,
            notes = record.notes?.toString(),
            isClickable = false,
        )
    }

    private fun formatValue(record: MindfulnessSessionRecord): String {
        return formatSession(record) { duration -> formatDurationShort(context, duration) }
    }

    private fun formatA11yValue(record: MindfulnessSessionRecord): String {
        return formatSession(record) { duration -> formatDurationLong(context, duration) }
    }

    private fun formatSession(
        record: MindfulnessSessionRecord,
        formatDuration: (duration: Duration) -> String,
    ): String {
        val type = getMindfulnessSessionType(context, record.mindfulnessSessionType)
        return if (!record.title.isNullOrBlank()) {
            context.getString(R.string.session_title, record.title, type)
        } else {
            val duration = Duration.between(record.startTime, record.endTime)
            context.getString(R.string.session_title, formatDuration(duration), type)
        }
    }

    companion object {
        fun getMindfulnessSessionType(context: Context, type: Int): String {
            return context.getString(
                when (type) {
                    MINDFULNESS_SESSION_TYPE_UNKNOWN -> R.string.unknown_type
                    MINDFULNESS_SESSION_TYPE_BREATHING -> R.string.mindfulness_type_breathing
                    MINDFULNESS_SESSION_TYPE_MEDITATION -> R.string.mindfulness_type_meditation
                    MINDFULNESS_SESSION_TYPE_MOVEMENT -> R.string.mindfulness_type_movement
                    MINDFULNESS_SESSION_TYPE_MUSIC -> R.string.mindfulness_type_music
                    MINDFULNESS_SESSION_TYPE_OTHER -> R.string.mindfulness_type_other
                    MINDFULNESS_SESSION_TYPE_UNGUIDED -> R.string.mindfulness_type_unguided
                    else -> throw IllegalArgumentException("Unknown mindfulness session type $type")
                }
            )
        }
    }
}
