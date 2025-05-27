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
import android.health.connect.datatypes.PlannedExerciseBlock
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseBlock
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseSession
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.DurationFormatter
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class PlannedExerciseSessionFormatter {

    fun format(
        record: PlannedExerciseSessionRecord,
        context: Context,
    ): FormattedPlannedExerciseSession {
        return FormattedPlannedExerciseSession(
            header = getHeader(record),
            title = formatTitle(record.title),
            note = formatNote(record.notes),
            type = formatExerciseType(record.exerciseType, context),
            duration = DurationFormatter.format(record.duration, context),
            exerciseBlocks = formatBlocks(record.blocks, context),
        )
    }

    private fun formatTitle(title: CharSequence?): String? {
        if (title.isNullOrEmpty()) {
            return null
        }
        return title.toString()
    }

    private fun formatNote(notes: CharSequence?): String? {
        if (notes.isNullOrEmpty()) {
            return null
        }
        return notes.toString()
    }

    private fun formatExerciseType(type: Int, context: Context): String {
        return ExerciseSessionFormatter.formatExerciseType(type, context)
    }

    private fun formatBlocks(
        blocks: List<PlannedExerciseBlock>,
        context: Context,
        plannedExerciseBlockFormatter: PlannedExerciseBlockFormatter =
            PlannedExerciseBlockFormatter(),
    ): List<FormattedPlannedExerciseBlock> {
        val formattedPlannedExerciseBlocks = mutableListOf<FormattedPlannedExerciseBlock>()

        blocks.forEach { block ->
            formattedPlannedExerciseBlocks.add(plannedExerciseBlockFormatter.format(block, context))
        }

        return formattedPlannedExerciseBlocks
    }
}
