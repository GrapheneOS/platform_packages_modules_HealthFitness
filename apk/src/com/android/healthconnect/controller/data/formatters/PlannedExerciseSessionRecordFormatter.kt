/**
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.ItemDataEntrySeparator
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseFormattedSectionContent
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseFormattedSectionTitle
import com.android.healthconnect.controller.data.formatters.ExerciseSessionFormatter.Companion.getExerciseType
import com.android.healthconnect.controller.data.formatters.shared.BaseFormatter
import com.android.healthconnect.controller.data.formatters.shared.RecordDetailsFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing PlannedExerciseSessionRecord data. */
class PlannedExerciseSessionRecordFormatter
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val plannedExerciseBlockFormatter: PlannedExerciseBlockFormatter,
) :
    BaseFormatter<PlannedExerciseSessionRecord>(context),
    RecordDetailsFormatter<PlannedExerciseSessionRecord> {

    override suspend fun formatRecord(
        record: PlannedExerciseSessionRecord,
        header: String,
        headerA11y: String,
        unitPreferences: UnitPreferences,
    ): FormattedEntry {
        return FormattedEntry.PlannedExerciseSessionEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatTitle(record),
            titleA11y = formatTitle(record),
            notes = getNotes(record),
            dataType = PlannedExerciseSessionRecord::class,
        )
    }

    fun formatTitle(record: PlannedExerciseSessionRecord): String {
        return context.getString(
            R.string.planned_exercise_session_title,
            getExerciseType(context, record.exerciseType),
            record.title ?: context.getString(R.string.unknown_type),
        )
    }

    private fun getNotes(record: PlannedExerciseSessionRecord): String? {
        return record.notes?.toString()
    }

    override suspend fun formatRecordDetails(
        record: PlannedExerciseSessionRecord
    ): List<FormattedEntry> {
        val exerciseBlock = record.blocks
        return buildList {
            if (!record.notes.isNullOrBlank()) {
                add(ItemDataEntrySeparator())
                add(
                    PlannedExerciseFormattedSectionTitle(
                        context.getString(R.string.planned_exercise_session_notes_title)
                    )
                )
                add(PlannedExerciseFormattedSectionContent(record.notes.toString()))
                add(ItemDataEntrySeparator())
            }
            exerciseBlock.forEach { plannedExerciseBlock ->
                add(plannedExerciseBlockFormatter.formatBlock(plannedExerciseBlock))
                addAll(
                    plannedExerciseBlockFormatter.formatBlockDetails(
                        plannedExerciseBlock,
                        unitPreferences,
                    )
                )
                add(ItemDataEntrySeparator())
            }
        }
    }
}
