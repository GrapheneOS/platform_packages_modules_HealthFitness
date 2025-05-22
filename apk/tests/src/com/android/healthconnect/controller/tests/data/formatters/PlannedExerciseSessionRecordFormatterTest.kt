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
package com.android.healthconnect.controller.tests.data.formatters

import android.content.Context
import android.health.connect.datatypes.ExerciseCompletionGoal
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExerciseSegmentType
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Velocity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry.ExercisePerformanceGoalEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSectionContent
import com.android.healthconnect.controller.data.entries.FormattedEntry.ItemDataEntrySeparator
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseBlockEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseStepEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.SessionHeader
import com.android.healthconnect.controller.data.formatters.PlannedExerciseSessionRecordFormatter
import com.android.healthconnect.controller.tests.utils.getPlannedExerciseBlock
import com.android.healthconnect.controller.tests.utils.getPlannedExerciseSessionRecord
import com.android.healthconnect.controller.tests.utils.getPlannedExerciseStep
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PlannedExerciseSessionRecordFormatterTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: PlannedExerciseSessionRecordFormatter
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun formatTitle() = runBlocking {
        val exerciseBlocks =
            listOf(
                getPlannedExerciseBlock(
                    repetitions = 1,
                    description = "Warm up",
                    exerciseSteps =
                        listOf(
                            getPlannedExerciseStep(
                                exerciseSegmentType =
                                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                                completionGoal =
                                    ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(1000.0)),
                                performanceGoals =
                                    listOf(
                                        ExercisePerformanceGoal.HeartRateGoal(100, 150),
                                        ExercisePerformanceGoal.SpeedGoal(
                                            Velocity.fromMetersPerSecond(15.0),
                                            Velocity.fromMetersPerSecond(25.0),
                                        ),
                                    ),
                            )
                        ),
                ),
                getPlannedExerciseBlock(
                    repetitions = 1,
                    description = "Main set",
                    exerciseSteps =
                        listOf(
                            getPlannedExerciseStep(
                                exerciseSegmentType =
                                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                                completionGoal =
                                    ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(4000.0)),
                                performanceGoals =
                                    listOf(
                                        ExercisePerformanceGoal.HeartRateGoal(150, 180),
                                        ExercisePerformanceGoal.SpeedGoal(
                                            Velocity.fromMetersPerSecond(25.0),
                                            Velocity.fromMetersPerSecond(50.0),
                                        ),
                                    ),
                            )
                        ),
                ),
            )

        assertThat(
                formatter.formatTitle(
                    getPlannedExerciseSessionRecord(
                        title = "Morning Run",
                        note = "Morning quick run by the park",
                        exerciseBlocks = exerciseBlocks,
                    )
                )
            )
            .isEqualTo("Running • Morning Run")
    }

    @Test
    fun formatTitle_whenEmpty() = runBlocking {
        val exerciseBlocks =
            listOf(
                getPlannedExerciseBlock(
                    repetitions = 1,
                    description = "Warm up",
                    exerciseSteps =
                        listOf(
                            getPlannedExerciseStep(
                                exerciseSegmentType =
                                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                                completionGoal =
                                    ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(1000.0)),
                                performanceGoals =
                                    listOf(
                                        ExercisePerformanceGoal.HeartRateGoal(100, 150),
                                        ExercisePerformanceGoal.SpeedGoal(
                                            Velocity.fromMetersPerSecond(15.0),
                                            Velocity.fromMetersPerSecond(25.0),
                                        ),
                                    ),
                            )
                        ),
                ),
                getPlannedExerciseBlock(
                    repetitions = 1,
                    description = "Main set",
                    exerciseSteps =
                        listOf(
                            getPlannedExerciseStep(
                                exerciseSegmentType =
                                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                                completionGoal =
                                    ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(4000.0)),
                                performanceGoals =
                                    listOf(
                                        ExercisePerformanceGoal.HeartRateGoal(150, 180),
                                        ExercisePerformanceGoal.SpeedGoal(
                                            Velocity.fromMetersPerSecond(25.0),
                                            Velocity.fromMetersPerSecond(50.0),
                                        ),
                                    ),
                            )
                        ),
                ),
            )

        assertThat(
                formatter.formatTitle(
                    getPlannedExerciseSessionRecord(
                        title = null,
                        note = null,
                        exerciseBlocks = exerciseBlocks,
                    )
                )
            )
            .isEqualTo("Running • Unknown type")
    }

    @Test
    fun formatRecordDetails() = runBlocking {
        val exerciseBlock1 =
            getPlannedExerciseBlock(
                repetitions = 1,
                description = "Warm up",
                exerciseSteps =
                    listOf(
                        getPlannedExerciseStep(
                            exerciseSegmentType = ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                            completionGoal =
                                ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(1000.0)),
                            performanceGoals =
                                listOf(
                                    ExercisePerformanceGoal.HeartRateGoal(100, 150),
                                    ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(15.0),
                                        Velocity.fromMetersPerSecond(25.0),
                                    ),
                                ),
                        )
                    ),
            )
        val exerciseBlock2 =
            getPlannedExerciseBlock(
                repetitions = 1,
                description = "Main set",
                exerciseSteps =
                    listOf(
                        getPlannedExerciseStep(
                            exerciseSegmentType = ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                            completionGoal =
                                ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(4000.0)),
                            performanceGoals =
                                listOf(
                                    ExercisePerformanceGoal.HeartRateGoal(150, 180),
                                    ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(25.0),
                                        Velocity.fromMetersPerSecond(50.0),
                                    ),
                                ),
                        )
                    ),
            )
        val exerciseBlocks = listOf(exerciseBlock1, exerciseBlock2)

        assertThat(
                formatter.formatRecordDetails(
                    getPlannedExerciseSessionRecord(
                        title = "Morning Run",
                        note = "Morning quick run by the park",
                        exerciseBlocks = exerciseBlocks,
                    )
                )
            )
            .isEqualTo(
                listOf(
                    ItemDataEntrySeparator(),
                    SessionHeader("Notes"),
                    FormattedSectionContent("Morning quick run by the park"),
                    ItemDataEntrySeparator(),
                    PlannedExerciseBlockEntry(
                        block = exerciseBlock1,
                        title = "Warm up: 1 time",
                        titleA11y = "Warm up 1 time",
                    ),
                    PlannedExerciseStepEntry(
                        step =
                            getPlannedExerciseStep(
                                exerciseSegmentType =
                                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                                completionGoal =
                                    ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(1000.0)),
                                performanceGoals =
                                    listOf(
                                        ExercisePerformanceGoal.HeartRateGoal(100, 150),
                                        ExercisePerformanceGoal.SpeedGoal(
                                            Velocity.fromMetersPerSecond(15.0),
                                            Velocity.fromMetersPerSecond(25.0),
                                        ),
                                    ),
                            ),
                        title = "1 km Running",
                        titleA11y = "1 kilometre Running",
                    ),
                    ExercisePerformanceGoalEntry(
                        goal = ExercisePerformanceGoal.HeartRateGoal(100, 150),
                        title = "100 bpm - 150 bpm",
                        titleA11y = "100 beats per minute - 150 beats per minute",
                    ),
                    ExercisePerformanceGoalEntry(
                        goal =
                            ExercisePerformanceGoal.SpeedGoal(
                                Velocity.fromMetersPerSecond(15.0),
                                Velocity.fromMetersPerSecond(25.0),
                            ),
                        title = "00:40 min/km - 01:06 min/km",
                        titleA11y = "00:40 minute per kilometre - 01:06 minute per kilometre",
                    ),
                    ItemDataEntrySeparator(),
                    PlannedExerciseBlockEntry(
                        block = exerciseBlock2,
                        title = "Main set: 1 time",
                        titleA11y = "Main set 1 time",
                    ),
                    PlannedExerciseStepEntry(
                        step =
                            getPlannedExerciseStep(
                                exerciseSegmentType =
                                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                                completionGoal =
                                    ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(4000.0)),
                                performanceGoals =
                                    listOf(
                                        ExercisePerformanceGoal.HeartRateGoal(150, 180),
                                        ExercisePerformanceGoal.SpeedGoal(
                                            Velocity.fromMetersPerSecond(25.0),
                                            Velocity.fromMetersPerSecond(50.0),
                                        ),
                                    ),
                            ),
                        title = "4 km Running",
                        titleA11y = "4 kilometres Running",
                    ),
                    ExercisePerformanceGoalEntry(
                        goal = ExercisePerformanceGoal.HeartRateGoal(150, 180),
                        title = "150 bpm - 180 bpm",
                        titleA11y = "150 beats per minute - 180 beats per minute",
                    ),
                    ExercisePerformanceGoalEntry(
                        goal =
                            ExercisePerformanceGoal.SpeedGoal(
                                Velocity.fromMetersPerSecond(25.0),
                                Velocity.fromMetersPerSecond(50.0),
                            ),
                        title = "00:20 min/km - 00:40 min/km",
                        titleA11y = "00:20 minute per kilometre - 00:40 minute per kilometre",
                    ),
                    ItemDataEntrySeparator(),
                )
            )
    }
}
