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
package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.PlannedExerciseBlock
import android.icu.text.MessageFormat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseBlockEntry
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing PlannedExerciseBlock data. */
class PlannedExerciseBlockFormatter
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val plannedExerciseStepFormatter: PlannedExerciseStepFormatter,
) {

    fun formatBlock(block: PlannedExerciseBlock): FormattedEntry {
        return PlannedExerciseBlockEntry(
            block = block,
            title = formatBlockTitle(block),
            titleA11y = formatBlockTitleA11y(block),
        )
    }

    private fun formatBlockTitle(block: PlannedExerciseBlock): String {
        return if (block.description != null) {
            context.getString(
                R.string.planned_exercise_block_title,
                block.description,
                MessageFormat.format(
                    context.getString(R.string.planned_exercise_block_repetitions),
                    mapOf("count" to block.repetitions),
                ),
            )
        } else {
            MessageFormat.format(
                context.getString(R.string.planned_exercise_block_repetitions),
                mapOf("count" to block.repetitions),
            )
        }
    }

    private fun formatBlockTitleA11y(block: PlannedExerciseBlock): String {
        return if (block.description != null) {
            context.getString(
                R.string.planned_exercise_block_a11y_title,
                block.description,
                MessageFormat.format(
                    context.getString(R.string.planned_exercise_block_repetitions),
                    mapOf("count" to block.repetitions),
                ),
            )
        } else {
            MessageFormat.format(
                context.getString(R.string.planned_exercise_block_repetitions),
                mapOf("count" to block.repetitions),
            )
        }
    }

    fun formatBlockDetails(
        block: PlannedExerciseBlock,
        unitPreferences: UnitPreferences,
    ): List<FormattedEntry> {
        val exerciseSteps = block.steps
        return buildList {
            exerciseSteps.forEach { plannedExerciseStep ->
                add(plannedExerciseStepFormatter.formatStep(plannedExerciseStep, unitPreferences))
                addAll(
                    plannedExerciseStepFormatter.formatStepDetails(
                        plannedExerciseStep,
                        unitPreferences,
                    )
                )
            }
        }
    }
}
