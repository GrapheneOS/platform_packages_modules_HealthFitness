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
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BALL_SLAM
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_PRESS
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_CRUNCH
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DEADLIFT
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ELLIPTICAL
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FRONT_RAISE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIP_THRUST
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HULA_HOOP
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMPING_JACK
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMP_ROPE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_CURL
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_PRESS
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_RAISE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LUNGE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PAUSE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PILATES
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PLANK
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PULL_UP
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PUNCH
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SIT_UP
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SQUAT
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STRETCHING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UNKNOWN
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UPPER_TWIST
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WALKING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WHEELCHAIR
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_YOGA
import com.android.healthconnect.testapps.toolbox.R

class ExerciseSegmentTypeFormatter {

    fun format(segmentType: Int, context: Context): String {
        return when (segmentType) {
            EXERCISE_SEGMENT_TYPE_ARM_CURL -> context.getString(R.string.exercise_segment_arm_curl)
            EXERCISE_SEGMENT_TYPE_BACK_EXTENSION ->
                context.getString(R.string.exercise_segment_back_extension)
            EXERCISE_SEGMENT_TYPE_BALL_SLAM ->
                context.getString(R.string.exercise_segment_ball_slam)
            EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS ->
                context.getString(R.string.exercise_segment_barbell_shoulder_press)
            EXERCISE_SEGMENT_TYPE_BENCH_PRESS ->
                context.getString(R.string.exercise_segment_bench_press)
            EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP ->
                context.getString(R.string.exercise_segment_bench_sit_up)
            EXERCISE_SEGMENT_TYPE_BIKING -> context.getString(R.string.exercise_segment_biking)
            EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY ->
                context.getString(R.string.exercise_segment_biking_stationary)
            EXERCISE_SEGMENT_TYPE_BURPEE -> context.getString(R.string.exercise_segment_burpee)
            EXERCISE_SEGMENT_TYPE_CRUNCH -> context.getString(R.string.exercise_segment_crunch)
            EXERCISE_SEGMENT_TYPE_DEADLIFT -> context.getString(R.string.exercise_segment_deadlift)
            EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION ->
                context.getString(R.string.exercise_segment_double_arm_triceps_extension)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM ->
                context.getString(R.string.exercise_segment_dumbbell_curl_left_arm)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM ->
                context.getString(R.string.exercise_segment_dumbbell_curl_right_arm)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE ->
                context.getString(R.string.exercise_segment_dumbbell_front_raise)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE ->
                context.getString(R.string.exercise_segment_dumbbell_lateral_raise)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW ->
                context.getString(R.string.exercise_segment_dumbbell_row)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM ->
                context.getString(R.string.exercise_segment_dumbbell_triceps_extension_left_arm)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM ->
                context.getString(R.string.exercise_segment_dumbbell_triceps_extension_right_arm)
            EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM ->
                context.getString(R.string.exercise_segment_dumbbell_triceps_extension_two_arm)
            EXERCISE_SEGMENT_TYPE_ELLIPTICAL ->
                context.getString(R.string.exercise_segment_elliptical)
            EXERCISE_SEGMENT_TYPE_FORWARD_TWIST ->
                context.getString(R.string.exercise_segment_forward_twist)
            EXERCISE_SEGMENT_TYPE_FRONT_RAISE ->
                context.getString(R.string.exercise_segment_front_raise)
            EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
                context.getString(R.string.exercise_segment_high_intensity_interval_training)
            EXERCISE_SEGMENT_TYPE_HIP_THRUST ->
                context.getString(R.string.exercise_segment_hip_thrust)
            EXERCISE_SEGMENT_TYPE_HULA_HOOP ->
                context.getString(R.string.exercise_segment_hula_hoop)
            EXERCISE_SEGMENT_TYPE_JUMPING_JACK ->
                context.getString(R.string.exercise_segment_jumping_jack)
            EXERCISE_SEGMENT_TYPE_JUMP_ROPE ->
                context.getString(R.string.exercise_segment_jump_rope)
            EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING ->
                context.getString(R.string.exercise_segment_kettlebell_swing)
            EXERCISE_SEGMENT_TYPE_LATERAL_RAISE ->
                context.getString(R.string.exercise_segment_lateral_raise)
            EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN ->
                context.getString(R.string.exercise_segment_lat_pull_down)
            EXERCISE_SEGMENT_TYPE_LEG_CURL -> context.getString(R.string.exercise_segment_leg_curl)
            EXERCISE_SEGMENT_TYPE_LEG_EXTENSION ->
                context.getString(R.string.exercise_segment_leg_extension)
            EXERCISE_SEGMENT_TYPE_LEG_PRESS ->
                context.getString(R.string.exercise_segment_leg_press)
            EXERCISE_SEGMENT_TYPE_LEG_RAISE ->
                context.getString(R.string.exercise_segment_leg_raise)
            EXERCISE_SEGMENT_TYPE_LUNGE -> context.getString(R.string.exercise_segment_lunge)
            EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER ->
                context.getString(R.string.exercise_segment_mountain_climber)
            EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT ->
                context.getString(R.string.exercise_segment_other_workout)
            EXERCISE_SEGMENT_TYPE_PAUSE -> context.getString(R.string.exercise_segment_pause)
            EXERCISE_SEGMENT_TYPE_PILATES -> context.getString(R.string.exercise_segment_pilates)
            EXERCISE_SEGMENT_TYPE_PLANK -> context.getString(R.string.exercise_segment_plank)
            EXERCISE_SEGMENT_TYPE_PULL_UP -> context.getString(R.string.exercise_segment_pull_up)
            EXERCISE_SEGMENT_TYPE_PUNCH -> context.getString(R.string.exercise_segment_punch)
            EXERCISE_SEGMENT_TYPE_REST -> context.getString(R.string.exercise_segment_rest)
            EXERCISE_SEGMENT_TYPE_ROWING_MACHINE ->
                context.getString(R.string.exercise_segment_rowing_machine)
            EXERCISE_SEGMENT_TYPE_RUNNING -> context.getString(R.string.exercise_segment_running)
            EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL ->
                context.getString(R.string.exercise_segment_running_treadmill)
            EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS ->
                context.getString(R.string.exercise_segment_shoulder_press)
            EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION ->
                context.getString(R.string.exercise_segment_single_arm_triceps_extension)
            EXERCISE_SEGMENT_TYPE_SIT_UP -> context.getString(R.string.exercise_segment_sit_up)
            EXERCISE_SEGMENT_TYPE_SQUAT -> context.getString(R.string.exercise_segment_squat)
            EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING ->
                context.getString(R.string.exercise_segment_stair_climbing)
            EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE ->
                context.getString(R.string.exercise_segment_stair_climbing_machine)
            EXERCISE_SEGMENT_TYPE_STRETCHING ->
                context.getString(R.string.exercise_segment_stretching)
            EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE ->
                context.getString(R.string.exercise_segment_swimming_backstroke)
            EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE ->
                context.getString(R.string.exercise_segment_swimming_breaststroke)
            EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY ->
                context.getString(R.string.exercise_segment_swimming_butterfly)
            EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE ->
                context.getString(R.string.exercise_segment_swimming_freestyle)
            EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED ->
                context.getString(R.string.exercise_segment_swimming_mixed)
            EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER ->
                context.getString(R.string.exercise_segment_swimming_open_water)
            EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER ->
                context.getString(R.string.exercise_segment_swimming_other)
            EXERCISE_SEGMENT_TYPE_SWIMMING_POOL ->
                context.getString(R.string.exercise_segment_swimming_pool)
            EXERCISE_SEGMENT_TYPE_UNKNOWN -> context.getString(R.string.exercise_segment_unknown)
            EXERCISE_SEGMENT_TYPE_UPPER_TWIST ->
                context.getString(R.string.exercise_segment_upper_twist)
            EXERCISE_SEGMENT_TYPE_WALKING -> context.getString(R.string.exercise_segment_walking)
            EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING ->
                context.getString(R.string.exercise_segment_weightlifting)
            EXERCISE_SEGMENT_TYPE_WHEELCHAIR ->
                context.getString(R.string.exercise_segment_wheelchair)
            EXERCISE_SEGMENT_TYPE_YOGA -> context.getString(R.string.exercise_segment_yoga)
            else -> context.getString(R.string.exercise_segment_unknown)
        }
    }
}
