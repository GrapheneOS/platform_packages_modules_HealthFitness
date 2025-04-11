/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.ExerciseCompletionGoal.DistanceGoal
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.HeartRateGoal
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING
import android.health.connect.datatypes.PlannedExerciseStep
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Velocity
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.dataentries.formatters.PlannedExerciseStepFormatter
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.units.UnitPreferences
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

@HiltAndroidTest
class PlannedExerciseStepFormatterTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: PlannedExerciseStepFormatter
    @Inject lateinit var unitPreferences: UnitPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun formatStep() = runBlocking {
        assertThat(
                formatter.formatStep(
                    getPlannedExerciseStepBuilder()
                        .setPerformanceGoals(
                            listOf(
                                HeartRateGoal(100, 150),
                                ExercisePerformanceGoal.SpeedGoal(
                                    Velocity.fromMetersPerSecond(15.0),
                                    Velocity.fromMetersPerSecond(25.0),
                                ),
                            )
                        )
                        .build(),
                    unitPreferences,
                )
            )
            .isEqualTo(
                FormattedEntry.PlannedExerciseStepEntry(
                    step =
                        getPlannedExerciseStepBuilder()
                            .setPerformanceGoals(
                                listOf(
                                    HeartRateGoal(100, 150),
                                    ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(15.0),
                                        Velocity.fromMetersPerSecond(25.0),
                                    ),
                                )
                            )
                            .build(),
                    title = "1 km Running",
                    titleA11y = "1 kilometre Running",
                )
            )
    }

    @Test
    fun formatStepDetails() = runBlocking {
        assertThat(
                formatter.formatStepDetails(
                    getPlannedExerciseStepBuilder()
                        .setPerformanceGoals(
                            listOf(
                                HeartRateGoal(100, 150),
                                ExercisePerformanceGoal.SpeedGoal(
                                    Velocity.fromMetersPerSecond(15.0),
                                    Velocity.fromMetersPerSecond(25.0),
                                ),
                            )
                        )
                        .build(),
                    unitPreferences,
                )
            )
            .isEqualTo(
                listOf(
                    FormattedEntry.ExercisePerformanceGoalEntry(
                        HeartRateGoal(100, 150),
                        title = "100 bpm - 150 bpm",
                        titleA11y = "100 beats per minute - 150 beats per minute",
                    ),
                    FormattedEntry.ExercisePerformanceGoalEntry(
                        ExercisePerformanceGoal.SpeedGoal(
                            Velocity.fromMetersPerSecond(15.0),
                            Velocity.fromMetersPerSecond(25.0),
                        ),
                        title = "00:40 min/km - 01:06 min/km",
                        titleA11y = "00:40 minute per kilometre - 01:06 minute per kilometre",
                    ),
                )
            )
    }

    private fun getPlannedExerciseStepBuilder(): PlannedExerciseStep.Builder {
        return PlannedExerciseStep.Builder(
            EXERCISE_SEGMENT_TYPE_RUNNING,
            EXERCISE_CATEGORY_ACTIVE,
            DistanceGoal(Length.fromMeters(1000.0)),
        )
    }
}
