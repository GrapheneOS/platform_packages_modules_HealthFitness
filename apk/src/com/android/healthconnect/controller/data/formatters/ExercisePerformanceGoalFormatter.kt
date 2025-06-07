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
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExerciseSegmentType
import android.icu.text.MessageFormat
import android.util.Log
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.ExercisePerformanceGoalEntry
import com.android.healthconnect.controller.data.formatters.SpeedFormatter.Companion.ACTIVITY_TYPES_WITH_PACE_VELOCITY
import com.android.healthconnect.controller.data.formatters.SpeedFormatter.Companion.SWIMMING_ACTIVITY_TYPES
import com.android.healthconnect.controller.units.UnitPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing ExercisePerformanceGoal data. */
class ExercisePerformanceGoalFormatter
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val speedFormatter: SpeedFormatter,
    private val unitPreferences: UnitPreferences,
) {
    private val ACTIVITY_TYPES_WITH_CADENCE_MOTION =
        listOf(
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING,
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
        )

    fun formatGoal(goal: ExercisePerformanceGoal, exerciseSegmentType: Int): FormattedEntry {
        return ExercisePerformanceGoalEntry(
            goal = goal,
            title = formatPerformanceGoal(goal, exerciseSegmentType),
            titleA11y = formatPerformanceGoalA11y(goal, exerciseSegmentType),
        )
    }

    private fun formatPerformanceGoal(
        performanceGoal: ExercisePerformanceGoal,
        exerciseSegmentType: Int,
    ): String {
        return when (performanceGoal) {
            is ExercisePerformanceGoal.PowerGoal ->
                context.getString(
                    R.string.performance_goals_range,
                    MessageFormat.format(
                        context.getString(R.string.watt_format),
                        mapOf("value" to performanceGoal.minPower.inWatts),
                    ),
                    MessageFormat.format(
                        context.getString(R.string.watt_format),
                        mapOf("value" to performanceGoal.maxPower.inWatts),
                    ),
                )
            is ExercisePerformanceGoal.AmrapGoal ->
                context.getString(R.string.amrap_performance_goal)
            is ExercisePerformanceGoal.CadenceGoal -> {
                if (ACTIVITY_TYPES_WITH_CADENCE_MOTION.contains(exerciseSegmentType)) {
                    return context.getString(
                        R.string.performance_goals_range,
                        MessageFormat.format(
                            context.getString(R.string.cycling_rpm),
                            mapOf("count" to performanceGoal.minRpm),
                        ),
                        MessageFormat.format(
                            context.getString(R.string.cycling_rpm),
                            mapOf("count" to performanceGoal.maxRpm),
                        ),
                    )
                }
                return context.getString(
                    R.string.performance_goals_range,
                    MessageFormat.format(
                        context.getString(R.string.steps_per_minute),
                        mapOf("value" to performanceGoal.minRpm),
                    ),
                    MessageFormat.format(
                        context.getString(R.string.steps_per_minute),
                        mapOf("value" to performanceGoal.maxRpm),
                    ),
                )
            }
            is ExercisePerformanceGoal.SpeedGoal ->
                if (
                    ACTIVITY_TYPES_WITH_PACE_VELOCITY.contains(exerciseSegmentType) ||
                        SWIMMING_ACTIVITY_TYPES.contains(exerciseSegmentType)
                )
                    context.getString(
                        R.string.performance_goals_range,
                        speedFormatter.formatSpeedValue(
                            performanceGoal.maxSpeed,
                            exerciseSegmentType,
                        ),
                        speedFormatter.formatSpeedValue(
                            performanceGoal.minSpeed,
                            exerciseSegmentType,
                        ),
                    )
                else
                    context.getString(
                        R.string.performance_goals_range,
                        speedFormatter.formatSpeedValue(
                            performanceGoal.minSpeed,
                            exerciseSegmentType,
                        ),
                        speedFormatter.formatSpeedValue(
                            performanceGoal.maxSpeed,
                            exerciseSegmentType,
                        ),
                    )
            is ExercisePerformanceGoal.HeartRateGoal ->
                context.getString(
                    R.string.performance_goals_range,
                    MessageFormat.format(
                        context.getString(R.string.heart_rate_value),
                        mapOf("count" to performanceGoal.minBpm),
                    ),
                    MessageFormat.format(
                        context.getString(R.string.heart_rate_value),
                        mapOf("count" to performanceGoal.maxBpm),
                    ),
                )
            is ExercisePerformanceGoal.WeightGoal ->
                MassFormatter.formatValue(context, performanceGoal.mass, unitPreferences.weightUnit)
            is ExercisePerformanceGoal.RateOfPerceivedExertionGoal ->
                context.getString(R.string.rate_of_perceived_exertion_goal, performanceGoal.rpe)
            else -> {
                Log.e("Error", "Unknown performance goal $performanceGoal")
                return ""
            }
        }
    }

    private fun formatPerformanceGoalA11y(
        performanceGoal: ExercisePerformanceGoal,
        exerciseSegmentType: Int,
    ): String {
        return when (performanceGoal) {
            is ExercisePerformanceGoal.PowerGoal ->
                context.getString(
                    R.string.performance_goals_range,
                    MessageFormat.format(
                        context.getString(R.string.watt_format_long),
                        mapOf("value" to performanceGoal.minPower.inWatts),
                    ),
                    MessageFormat.format(
                        context.getString(R.string.watt_format_long),
                        mapOf("value" to performanceGoal.maxPower.inWatts),
                    ),
                )
            is ExercisePerformanceGoal.AmrapGoal ->
                context.getString(R.string.amrap_performance_goal)
            is ExercisePerformanceGoal.CadenceGoal -> {
                if (ACTIVITY_TYPES_WITH_CADENCE_MOTION.contains(exerciseSegmentType)) {
                    return context.getString(
                        R.string.performance_goals_range,
                        MessageFormat.format(
                            context.getString(R.string.cycling_rpm_long),
                            mapOf("count" to performanceGoal.minRpm),
                        ),
                        MessageFormat.format(
                            context.getString(R.string.cycling_rpm_long),
                            mapOf("count" to performanceGoal.maxRpm),
                        ),
                    )
                }
                return context.getString(
                    R.string.performance_goals_range,
                    MessageFormat.format(
                        context.getString(R.string.steps_per_minute_long),
                        mapOf("value" to performanceGoal.minRpm),
                    ),
                    MessageFormat.format(
                        context.getString(R.string.steps_per_minute_long),
                        mapOf("value" to performanceGoal.maxRpm),
                    ),
                )
            }
            is ExercisePerformanceGoal.SpeedGoal ->
                if (
                    ACTIVITY_TYPES_WITH_PACE_VELOCITY.contains(exerciseSegmentType) ||
                        SWIMMING_ACTIVITY_TYPES.contains(exerciseSegmentType)
                )
                    context.getString(
                        R.string.performance_goals_range,
                        speedFormatter.formatA11ySpeedValue(
                            performanceGoal.maxSpeed,
                            exerciseSegmentType,
                        ),
                        speedFormatter.formatA11ySpeedValue(
                            performanceGoal.minSpeed,
                            exerciseSegmentType,
                        ),
                    )
                else
                    context.getString(
                        R.string.performance_goals_range,
                        speedFormatter.formatA11ySpeedValue(
                            performanceGoal.minSpeed,
                            exerciseSegmentType,
                        ),
                        speedFormatter.formatA11ySpeedValue(
                            performanceGoal.maxSpeed,
                            exerciseSegmentType,
                        ),
                    )
            is ExercisePerformanceGoal.HeartRateGoal ->
                context.getString(
                    R.string.performance_goals_range,
                    MessageFormat.format(
                        context.getString(R.string.heart_rate_long_value),
                        mapOf("count" to performanceGoal.minBpm),
                    ),
                    MessageFormat.format(
                        context.getString(R.string.heart_rate_long_value),
                        mapOf("count" to performanceGoal.maxBpm),
                    ),
                )
            is ExercisePerformanceGoal.WeightGoal ->
                MassFormatter.formatA11yValue(
                    context,
                    performanceGoal.mass,
                    unitPreferences.weightUnit,
                )
            is ExercisePerformanceGoal.RateOfPerceivedExertionGoal ->
                context.getString(R.string.rate_of_perceived_exertion_goal, performanceGoal.rpe)
            else -> {
                Log.e("Error", "Unknown performance goal $performanceGoal")
                return ""
            }
        }
    }
}
