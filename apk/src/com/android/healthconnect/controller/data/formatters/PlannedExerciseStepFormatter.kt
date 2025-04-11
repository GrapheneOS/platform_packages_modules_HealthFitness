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
import android.health.connect.datatypes.ExerciseCompletionGoal
import android.health.connect.datatypes.PlannedExerciseStep
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSectionContent
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseStepEntry
import com.android.healthconnect.controller.data.formatters.shared.LengthFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing PlannedExerciseStep data. */
class PlannedExerciseStepFormatter
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val exercisePerformanceGoalFormatter: ExercisePerformanceGoalFormatter,
    private val exerciseSegmentTypeFormatter: ExerciseSegmentTypeFormatter,
) {

    fun formatStep(step: PlannedExerciseStep, unitPreferences: UnitPreferences): FormattedEntry {
        return PlannedExerciseStepEntry(
            step = step,
            title = formatStepTitle(step, unitPreferences),
            titleA11y = formatStepTitleA11y(step, unitPreferences),
        )
    }

    fun formatStepDetails(
        step: PlannedExerciseStep,
        unitPreferences: UnitPreferences,
    ): List<FormattedEntry> {
        val performanceGoals = step.performanceGoals
        return buildList {
            if (!step.description.isNullOrBlank()) {
                add(FormattedSectionContent(title = step.description.toString(), bulleted = true))
            }

            if (performanceGoals.isNotEmpty()) {
                performanceGoals.forEach { performanceGoal ->
                    add(
                        exercisePerformanceGoalFormatter.formatGoal(
                            performanceGoal,
                            unitPreferences,
                            step.exerciseType,
                        )
                    )
                }
            }
        }
    }

    private fun formatStepTitle(
        step: PlannedExerciseStep,
        unitPreferences: UnitPreferences,
    ): String {
        val completionGoal = step.completionGoal
        val exerciseSegmentType = step.exerciseType
        return context.getString(
            R.string.planned_exercise_step_title,
            formatCompletionGoal(completionGoal, unitPreferences),
            exerciseSegmentTypeFormatter.getSegmentType(exerciseSegmentType),
        )
    }

    private fun formatStepTitleA11y(
        step: PlannedExerciseStep,
        unitPreferences: UnitPreferences,
    ): String {
        val completionGoal = step.completionGoal
        val exerciseSegmentType = step.exerciseType
        return context.getString(
            R.string.planned_exercise_step_title,
            formatCompletionGoalA11y(completionGoal, unitPreferences),
            exerciseSegmentTypeFormatter.getSegmentType(exerciseSegmentType),
        )
    }

    private fun formatCompletionGoal(
        completionGoal: ExerciseCompletionGoal,
        unitPreferences: UnitPreferences,
    ): String {
        return when (completionGoal) {
            is ExerciseCompletionGoal.DistanceGoal ->
                LengthFormatter.formatValue(context, completionGoal.distance, unitPreferences)
            is ExerciseCompletionGoal.DurationGoal ->
                DurationFormatter.formatDurationShort(context, completionGoal.duration)
            is ExerciseCompletionGoal.StepsGoal ->
                StepsFormatter(context).formatUnit(completionGoal.steps.toLong())
            is ExerciseCompletionGoal.RepetitionsGoal -> completionGoal.repetitions.toString()
            is ExerciseCompletionGoal.ActiveCaloriesBurnedGoal ->
                context.getString(
                    R.string.active_calories_burned,
                    EnergyFormatter.formatEnergyValue(
                        context,
                        completionGoal.activeCalories,
                        unitPreferences,
                    ),
                )
            is ExerciseCompletionGoal.DistanceWithVariableRestGoal ->
                context.getString(
                    R.string.distance_with_variable_rest_goal_formatted,
                    LengthFormatter.formatValue(context, completionGoal.distance, unitPreferences),
                    DurationFormatter.formatDurationShort(context, completionGoal.duration),
                )
            is ExerciseCompletionGoal.TotalCaloriesBurnedGoal ->
                context.getString(
                    R.string.total_calories_burned,
                    EnergyFormatter.formatEnergyValue(
                        context,
                        completionGoal.totalCalories,
                        unitPreferences,
                    ),
                )
            else -> throw IllegalArgumentException("Unknown completion goal $completionGoal")
        }
    }

    private fun formatCompletionGoalA11y(
        completionGoal: ExerciseCompletionGoal,
        unitPreferences: UnitPreferences,
    ): String {
        return when (completionGoal) {
            is ExerciseCompletionGoal.DistanceGoal ->
                LengthFormatter.formatA11yValue(context, completionGoal.distance, unitPreferences)
            is ExerciseCompletionGoal.DurationGoal ->
                DurationFormatter.formatDurationLong(context, completionGoal.duration)
            is ExerciseCompletionGoal.StepsGoal ->
                StepsFormatter(context).formatA11yUnit(completionGoal.steps.toLong())
            is ExerciseCompletionGoal.RepetitionsGoal -> completionGoal.repetitions.toString()
            is ExerciseCompletionGoal.ActiveCaloriesBurnedGoal ->
                context.getString(
                    R.string.active_calories_burned,
                    EnergyFormatter.formatEnergyA11yValue(
                        context,
                        completionGoal.activeCalories,
                        unitPreferences,
                    ),
                )
            is ExerciseCompletionGoal.DistanceWithVariableRestGoal ->
                context.getString(
                    R.string.distance_with_variable_rest_goal_formatted,
                    LengthFormatter.formatA11yValue(
                        context,
                        completionGoal.distance,
                        unitPreferences,
                    ),
                    DurationFormatter.formatDurationLong(context, completionGoal.duration),
                )
            is ExerciseCompletionGoal.TotalCaloriesBurnedGoal ->
                context.getString(
                    R.string.total_calories_burned,
                    EnergyFormatter.formatEnergyA11yValue(
                        context,
                        completionGoal.totalCalories,
                        unitPreferences,
                    ),
                )
            else -> throw IllegalArgumentException("Unknown completion goal $completionGoal")
        }
    }
}
