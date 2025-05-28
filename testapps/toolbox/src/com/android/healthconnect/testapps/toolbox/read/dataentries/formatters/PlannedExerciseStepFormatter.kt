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
import android.health.connect.datatypes.ExerciseCompletionGoal.DistanceGoal
import android.health.connect.datatypes.ExerciseCompletionGoal.DistanceWithVariableRestGoal
import android.health.connect.datatypes.ExerciseCompletionGoal.DurationGoal
import android.health.connect.datatypes.ExerciseCompletionGoal.RepetitionsGoal
import android.health.connect.datatypes.ExerciseCompletionGoal.StepsGoal
import android.health.connect.datatypes.ExerciseCompletionGoal.TotalCaloriesBurnedGoal
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.AmrapGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.CadenceGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.HeartRateGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.PowerGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.RateOfPerceivedExertionGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.SpeedGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.WeightGoal
import android.health.connect.datatypes.PlannedExerciseStep
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_COOLDOWN
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_RECOVERY
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_REST
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_WARMUP
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedPlannedExerciseStep
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.DurationFormatter
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.Unit
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatEnergy
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatLength
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatMass
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatPower
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatVelocity
import java.util.StringJoiner

class PlannedExerciseStepFormatter {

    fun format(step: PlannedExerciseStep, context: Context): FormattedPlannedExerciseStep {
        return FormattedPlannedExerciseStep(
            description = formatDescription(step.description),
            category = formatExerciseCategory(step.exerciseCategory, context),
            type = formatExerciseType(step.exerciseType, context),
            completionGoals = formatCompletionGoal(step.completionGoal, context),
            performanceGoals = formatPerformanceGoals(step.performanceGoals, context),
        )
    }

    private fun formatExerciseCategory(category: Int, context: Context): String {
        return when (category) {
            EXERCISE_CATEGORY_ACTIVE -> context.getString(R.string.active)
            EXERCISE_CATEGORY_COOLDOWN -> context.getString(R.string.cooldown)
            EXERCISE_CATEGORY_RECOVERY -> context.getString(R.string.recovery)
            EXERCISE_CATEGORY_REST -> context.getString(R.string.rest)
            EXERCISE_CATEGORY_WARMUP -> context.getString(R.string.warmup)
            else -> context.getString(R.string.unknown_exercise_category)
        }
    }

    private fun formatDescription(description: CharSequence?): String? {
        if (description.isNullOrEmpty()) {
            return null
        }
        return description.toString()
    }

    private fun formatPerformanceGoals(
        performanceGoals: List<ExercisePerformanceGoal>,
        context: Context,
    ): String? {
        val formattedPerformanceGoals = StringJoiner("\n")
        performanceGoals.forEach { performanceGoal ->
            formattedPerformanceGoals.add(formatPerformanceGoal(performanceGoal, context))
        }
        if (formattedPerformanceGoals.length() == 0) {
            return null
        }
        return formattedPerformanceGoals.toString()
    }

    private fun formatPerformanceGoal(
        performanceGoal: ExercisePerformanceGoal,
        context: Context,
    ): String {
        return when (performanceGoal) {
            is AmrapGoal -> performanceGoal.toString()
            is CadenceGoal ->
                "${performanceGoal.minRpm} - ${performanceGoal.maxRpm} ${context.getString(R.string.revolutions_per_minute_label)}"
            is HeartRateGoal ->
                "${performanceGoal.minBpm} - ${performanceGoal.maxBpm} ${context.getString(R.string.beats_per_minute_label)}"
            is PowerGoal ->
                "${formatPower(performanceGoal.minPower, context)} - ${formatPower(performanceGoal.maxPower, context)}"
            is RateOfPerceivedExertionGoal -> performanceGoal.rpe.toString()
            is SpeedGoal ->
                "${formatVelocity(performanceGoal.minSpeed, context)} - ${formatVelocity(performanceGoal.maxSpeed, context)}"
            is WeightGoal -> formatMass(performanceGoal.mass, Unit.Mass.KILOGRAMS, context)

            else -> context.getString(R.string.unknown_performance_goal)
        }
    }

    private fun formatExerciseType(
        type: Int,
        context: Context,
        exerciseSegmentTypeFormatter: ExerciseSegmentTypeFormatter = ExerciseSegmentTypeFormatter(),
    ): String {
        return exerciseSegmentTypeFormatter.format(type, context)
    }

    private fun formatCompletionGoal(
        completionGoal: ExerciseCompletionGoal,
        context: Context,
    ): String {
        return when (completionGoal) {
            is ActiveCaloriesBurnedGoal -> formatEnergy(completionGoal.activeCalories, context)
            is DistanceGoal ->
                formatLength(completionGoal.distance, Unit.Length.KILOMETERS, context)
            is DistanceWithVariableRestGoal ->
                formatLength(completionGoal.distance, Unit.Length.KILOMETERS, context)
            is DurationGoal -> DurationFormatter.format(completionGoal.duration, context)
            is RepetitionsGoal ->
                "${completionGoal.repetitions} ${context.getString(R.string.repetitions_label)}"
            is StepsGoal -> UnitFormatter.formatSteps(completionGoal.steps.toLong(), context)
            is TotalCaloriesBurnedGoal -> completionGoal.totalCalories.toString()

            else -> context.getString(R.string.unknown_completion_goal)
        }
    }
}
