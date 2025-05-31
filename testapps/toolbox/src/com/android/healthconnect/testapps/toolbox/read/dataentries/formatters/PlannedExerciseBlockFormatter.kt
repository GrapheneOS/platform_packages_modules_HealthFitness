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
import android.health.connect.datatypes.PlannedExerciseStep
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseBlock
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseStep

class PlannedExerciseBlockFormatter {

    fun format(block: PlannedExerciseBlock, context: Context): FormattedPlannedExerciseBlock {
        return FormattedPlannedExerciseBlock(
            description = formatDescription(block.description),
            repetitions = formatRepetitions(block.repetitions),
            steps = formatPlannedExerciseSteps(block.steps, context),
        )
    }

    private fun formatDescription(description: CharSequence?): String? {
        if (description.isNullOrEmpty()) {
            return null
        }
        return description.toString()
    }

    private fun formatRepetitions(repetitions: Int): String {
        return repetitions.toString()
    }

    private fun formatPlannedExerciseSteps(
        steps: List<PlannedExerciseStep>,
        context: Context,
        plannedExerciseStepFormatter: PlannedExerciseStepFormatter = PlannedExerciseStepFormatter(),
    ): List<FormattedPlannedExerciseStep> {
        val formattedPlannedExerciseSteps = mutableListOf<FormattedPlannedExerciseStep>()

        steps.forEach { step ->
            formattedPlannedExerciseSteps.add(plannedExerciseStepFormatter.format(step, context))
        }

        return formattedPlannedExerciseSteps
    }
}
