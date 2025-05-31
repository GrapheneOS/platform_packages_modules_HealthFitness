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
import android.health.connect.datatypes.ExerciseCompletionGoal
import android.health.connect.datatypes.ExerciseCompletionGoal.ActiveCaloriesBurnedGoal
import android.health.connect.datatypes.ExerciseCompletionGoal.StepsGoal
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UNKNOWN
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WALKING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT
import android.health.connect.datatypes.PlannedExerciseBlock
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.PlannedExerciseStep
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_RECOVERY
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_REST
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_WARMUP
import android.health.connect.datatypes.units.Energy
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannedExerciseSessionFormatterTest {
    private val formatter = PlannedExerciseSessionFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatedPlannedExerciseSessionValue_returnsFormattedEntry_minimumData() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val record =
            buildPlannedExerciseSession(EXERCISE_SESSION_TYPE_BIKING, NOW, NOW.plusSeconds(30))
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedPlannedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Biking",
                    note = null,
                    title = null,
                    duration = "30 Seconds",
                    exerciseBlocks = listOf(),
                )
            )
    }

    @Test
    fun formatedPlannedExerciseSessionValue_returnsFormattedEntry_withBlocks() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val blocks =
            listOf(buildPlannedExerciseBlock(10).build(), buildPlannedExerciseBlock(15).build())
        val record =
            buildPlannedExerciseSession(EXERCISE_SESSION_TYPE_BIKING, NOW, NOW.plusSeconds(30))
                .setBlocks(blocks)
                .build()

        val formattedEntry = formatter.format(record, context)

        val expectedPlannedExerciseBlocks =
            listOf(
                FormattedEntry.FormattedPlannedExerciseBlock(
                    description = null,
                    repetitions = "10",
                    steps = listOf(),
                ),
                FormattedEntry.FormattedPlannedExerciseBlock(
                    description = null,
                    repetitions = "15",
                    steps = listOf(),
                ),
            )
        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedPlannedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Biking",
                    note = null,
                    title = null,
                    duration = "30 Seconds",
                    exerciseBlocks = expectedPlannedExerciseBlocks,
                )
            )
    }

    @Test
    fun formatedPlannedExerciseSessionValue_returnsFormattedEntry_withBlocksAndSteps() {
        val setOfSteps =
            listOf(
                listOf(
                    buildPlannedExerciseStep(
                            EXERCISE_SEGMENT_TYPE_BIKING,
                            EXERCISE_CATEGORY_RECOVERY,
                            ActiveCaloriesBurnedGoal(Energy.fromCalories(600.0)),
                        )
                        .build(),
                    buildPlannedExerciseStep(
                            EXERCISE_SEGMENT_TYPE_BIKING,
                            EXERCISE_CATEGORY_ACTIVE,
                            ActiveCaloriesBurnedGoal(Energy.fromCalories(12300.0)),
                        )
                        .build(),
                ),
                listOf(
                    buildPlannedExerciseStep(
                            EXERCISE_SEGMENT_TYPE_UNKNOWN,
                            EXERCISE_CATEGORY_WARMUP,
                            StepsGoal(522),
                        )
                        .build(),
                    buildPlannedExerciseStep(
                            EXERCISE_SEGMENT_TYPE_WALKING,
                            EXERCISE_CATEGORY_REST,
                            StepsGoal(201),
                        )
                        .build(),
                ),
            )
        val blocks =
            listOf(
                buildPlannedExerciseBlock(2).setSteps(setOfSteps[0]).build(),
                buildPlannedExerciseBlock(1).setSteps(setOfSteps[1]).build(),
            )
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val record =
            buildPlannedExerciseSession(
                    EXERCISE_SESSION_TYPE_OTHER_WORKOUT,
                    NOW,
                    NOW.plusSeconds(30),
                )
                .setBlocks(blocks)
                .build()

        val formattedEntry = formatter.format(record, context)

        val expectedPlannedExerciseBlocks =
            listOf(
                FormattedEntry.FormattedPlannedExerciseBlock(
                    description = null,
                    repetitions = "2",
                    steps =
                        listOf(
                            FormattedEntry.FormattedPlannedExerciseStep(
                                category = "Recovery",
                                type = "Biking",
                                completionGoals = "1 cal",
                                description = null,
                                performanceGoals = null,
                            ),
                            FormattedEntry.FormattedPlannedExerciseStep(
                                category = "Active",
                                type = "Biking",
                                completionGoals = "13 cal",
                                performanceGoals = null,
                                description = null,
                            ),
                        ),
                ),
                FormattedEntry.FormattedPlannedExerciseBlock(
                    description = null,
                    repetitions = "1",
                    steps =
                        listOf(
                            FormattedEntry.FormattedPlannedExerciseStep(
                                category = "Warmup",
                                type = "Unknown",
                                completionGoals = "522 Steps",
                                performanceGoals = null,
                                description = null,
                            ),
                            FormattedEntry.FormattedPlannedExerciseStep(
                                category = "Rest",
                                type = "Walking",
                                completionGoals = "201 Steps",
                                performanceGoals = null,
                                description = null,
                            ),
                        ),
                ),
            )
        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedPlannedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Other",
                    note = null,
                    title = null,
                    duration = "30 Seconds",
                    exerciseBlocks = expectedPlannedExerciseBlocks,
                )
            )
    }

    private fun buildPlannedExerciseStep(
        exerciseType: Int,
        exerciseCategory: Int,
        completionGoal: ExerciseCompletionGoal,
    ): PlannedExerciseStep.Builder {
        return PlannedExerciseStep.Builder(exerciseType, exerciseCategory, completionGoal)
    }

    private fun buildPlannedExerciseBlock(repetitons: Int): PlannedExerciseBlock.Builder {
        return PlannedExerciseBlock.Builder(repetitons)
    }

    private fun buildPlannedExerciseSession(
        exerciseType: Int,
        startTime: Instant,
        endTime: Instant,
    ): PlannedExerciseSessionRecord.Builder {
        return PlannedExerciseSessionRecord.Builder(
            getMetaData(context),
            exerciseType,
            startTime,
            endTime,
        )
    }
}
