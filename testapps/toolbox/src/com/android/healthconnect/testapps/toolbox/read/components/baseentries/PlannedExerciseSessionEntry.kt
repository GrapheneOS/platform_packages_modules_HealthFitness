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
package com.android.healthconnect.testapps.toolbox.read.components.baseentries

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.UIConstants.PADDING_LARGE
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseBlock
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseSession
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseStep
import java.util.StringJoiner

@Composable
fun PlannedExerciseSessionEntry(plannedExerciseSession: FormattedPlannedExerciseSession) {
    var dropDownExpanded by remember { mutableStateOf(false) }
    val rotationAngle by
        animateFloatAsState(
            targetValue = if (dropDownExpanded) 180f else 0f,
            animationSpec = tween(durationMillis = 300),
        )

    Column {
        // Data entry / header
        Row(
            modifier = Modifier.fillMaxWidth().clickable { dropDownExpanded = !dropDownExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                val title = StringJoiner(" ")
                title.add(plannedExerciseSession.type)
                if (!plannedExerciseSession.title.isNullOrBlank()) {
                    title.add(plannedExerciseSession.title)
                }

                DataEntry(header = plannedExerciseSession.header, value = title.toString())
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
            )
        }

        // Drop down section
        AnimatedVisibility(visible = dropDownExpanded) {
            Column(modifier = Modifier.padding(start = 24.dp)) {
                DataEntry(
                    header = stringResource(id = R.string.duration),
                    value = plannedExerciseSession.duration,
                )
                if (!plannedExerciseSession.note.isNullOrEmpty()) {
                    DataEntry(
                        header = stringResource(id = R.string.notes),
                        value = plannedExerciseSession.note,
                    )
                }
                HeaderEntry(header = stringResource(id = R.string.exercise_blocks))
                plannedExerciseSession.exerciseBlocks.forEach { exerciseBlock ->
                    PlannedExerciseBlockEntry(exerciseBlock)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PlannedExerciseBlockEntry(block: FormattedPlannedExerciseBlock) {
    var dropDownExpanded by remember { mutableStateOf(false) }
    val rotationAngle by
        animateFloatAsState(
            targetValue = if (dropDownExpanded) 180f else 0f,
            animationSpec = tween(durationMillis = 300),
        )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { dropDownExpanded = !dropDownExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SampleEntry(
                header = stringResource(id = R.string.repetitions_label),
                value = block.repetitions,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
            )
        }
        AnimatedVisibility(visible = dropDownExpanded) {
            Column(modifier = Modifier.padding(start = PADDING_LARGE)) {
                if (!block.description.isNullOrEmpty()) {
                    DataEntry(header = stringResource(id = R.string.desc), block.description)
                }
                if (block.steps.isEmpty()) {
                    HeaderEntry(header = stringResource(id = R.string.no_steps))
                } else {
                    block.steps.forEach { step ->
                        PlannedExerciseStepEntry(step)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun PlannedExerciseStepEntry(step: FormattedPlannedExerciseStep) {
    Column {
        SampleEntry(header = stringResource(id = R.string.type), value = step.type)
        SampleEntry(header = stringResource(id = R.string.category), value = step.category)
        SampleEntry(
            header = stringResource(id = R.string.completion_goal),
            value = step.completionGoals,
        )
        if (step.performanceGoals != null) {
            SampleEntry(
                header = stringResource(id = R.string.performance_goal),
                value = step.performanceGoals,
            )
        }
        if (step.description != null) {
            SampleEntry(header = stringResource(id = R.string.desc), value = step.description)
        }
    }
}
