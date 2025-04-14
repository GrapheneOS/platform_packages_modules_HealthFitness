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
import android.health.connect.datatypes.ExerciseSegmentType
import com.android.healthconnect.controller.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for ExerciseSegmentType data. */
class ExerciseSegmentTypeFormatter
@Inject
constructor(@ApplicationContext private val context: Context) {
    fun getSegmentType(segmentType: Int): String {
        return when (segmentType) {
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION ->
                context.getString(R.string.back_extension)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS ->
                context.getString(R.string.barbell_shoulder_press)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_PRESS ->
                context.getString(R.string.bench_press)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP ->
                context.getString(R.string.bench_sit_up)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE -> context.getString(R.string.burpee)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_CRUNCH -> context.getString(R.string.crunch)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DEADLIFT ->
                context.getString(R.string.deadlift)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM ->
                context.getString(R.string.dumbbell_curl_left_arm)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM ->
                context.getString(R.string.dumbbell_curl_right_arm)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE ->
                context.getString(R.string.dumbbell_front_raise)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE ->
                context.getString(R.string.dumbbell_lateral_raise)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM ->
                context.getString(R.string.dumbbell_triceps_extension_left_arm)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM ->
                context.getString(R.string.dumbbell_triceps_extension_right_arm)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM ->
                context.getString(R.string.dumbbell_triceps_extension_two_arm)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST ->
                context.getString(R.string.forward_twist)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMPING_JACK ->
                context.getString(R.string.jumping_jack)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMP_ROPE ->
                context.getString(R.string.jump_rope)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN ->
                context.getString(R.string.lat_pull_down)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LUNGE -> context.getString(R.string.lunge)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PLANK -> context.getString(R.string.plank)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SQUAT -> context.getString(R.string.squat)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING -> context.getString(R.string.biking)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY ->
                context.getString(R.string.biking_stationary)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PILATES -> context.getString(R.string.pilates)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ELLIPTICAL ->
                context.getString(R.string.elliptical)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE ->
                context.getString(R.string.rowing_machine)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING -> context.getString(R.string.running)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL ->
                context.getString(R.string.running_treadmill)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING ->
                context.getString(R.string.stair_climbing)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE ->
                context.getString(R.string.stair_climbing_machine)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STRETCHING ->
                context.getString(R.string.stretching)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER ->
                context.getString(R.string.swimming_open_water)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE ->
                context.getString(R.string.swimming_backstroke)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE ->
                context.getString(R.string.swimming_breaststroke)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY ->
                context.getString(R.string.swimming_butterfly)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE ->
                context.getString(R.string.swimming_freestyle)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED ->
                context.getString(R.string.swimming_mixed)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL ->
                context.getString(R.string.swimming_pool)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER ->
                context.getString(R.string.swimming_other)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WALKING -> context.getString(R.string.walking)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WHEELCHAIR ->
                context.getString(R.string.wheelchair)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING ->
                context.getString(R.string.weightlifting)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_YOGA -> context.getString(R.string.yoga)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL ->
                context.getString(R.string.arm_curl)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BALL_SLAM ->
                context.getString(R.string.ball_slam)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION ->
                context.getString(R.string.double_arm_triceps_extension)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW ->
                context.getString(R.string.dumbbell_row)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FRONT_RAISE ->
                context.getString(R.string.front_raise)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIP_THRUST ->
                context.getString(R.string.hip_thrust)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HULA_HOOP ->
                context.getString(R.string.hula_hoop)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING ->
                context.getString(R.string.kettlebell_swing)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE ->
                context.getString(R.string.lateral_raise)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_CURL ->
                context.getString(R.string.leg_curl)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION ->
                context.getString(R.string.leg_extension)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_PRESS ->
                context.getString(R.string.leg_press)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_RAISE ->
                context.getString(R.string.leg_raise)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER ->
                context.getString(R.string.mountain_climber)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PULL_UP -> context.getString(R.string.pull_up)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PUNCH -> context.getString(R.string.punch)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS ->
                context.getString(R.string.shoulder_press)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION ->
                context.getString(R.string.single_arm_triceps_extension)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
                context.getString(R.string.high_intensity_interval_training)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST -> context.getString(R.string.rest)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PAUSE -> context.getString(R.string.pause)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT ->
                context.getString(R.string.workout)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SIT_UP -> context.getString(R.string.sit_up)
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UPPER_TWIST ->
                context.getString(R.string.upper_twist)
            else -> throw IllegalArgumentException("Unknown exercise segment type $segmentType")
        }
    }
}
