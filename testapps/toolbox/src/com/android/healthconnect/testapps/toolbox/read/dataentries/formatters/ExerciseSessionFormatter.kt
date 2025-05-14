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
import android.health.connect.datatypes.ExerciseLap
import android.health.connect.datatypes.ExerciseSegment
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BASEBALL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BASKETBALL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BOOT_CAMP
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BOXING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_CALISTHENICS
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_CRICKET
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_DANCING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ELLIPTICAL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_EXERCISE_CLASS
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_FENCING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AUSTRALIAN
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_FRISBEE_DISC
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_GOLF
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_GUIDED_BREATHING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_GYMNASTICS
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_HANDBALL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_HIKING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ICE_HOCKEY
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ICE_SKATING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_MARTIAL_ARTS
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_PADDLING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_PARAGLIDING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_PILATES
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RACQUETBALL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ROCK_CLIMBING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ROLLER_HOCKEY
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ROWING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ROWING_MACHINE
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUGBY
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING_TREADMILL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SAILING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SCUBA_DIVING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SKATING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SKIING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SNOWBOARDING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SNOWSHOEING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SOCCER
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SOFTBALL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SQUASH
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STRENGTH_TRAINING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STRETCHING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SURFING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_POOL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_TABLE_TENNIS
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_TENNIS
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_VOLLEYBALL
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_WATER_POLO
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_WHEELCHAIR
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_YOGA
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedExerciseSession
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.Unit
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class ExerciseSessionFormatter {

    fun format(record: ExerciseSessionRecord, context: Context): FormattedExerciseSession {

        return FormattedExerciseSession(
            header = getHeader(record),
            title = formatTitle(record.title),
            type = formatExerciseType(record.exerciseType, context),
            note = formatNote(record.notes),
            route = record.route,
            sessionDetails = formatDetails(record, context),
        )
    }

    private fun formatNote(note: CharSequence?): String? {
        if (note.isNullOrEmpty()) {
            return null
        }
        return note.toString()
    }

    private fun formatTitle(title: CharSequence?): String? {
        if (title.isNullOrEmpty()) {
            return null
        }
        return title.toString()
    }

    private fun formatDetails(
        record: ExerciseSessionRecord,
        context: Context,
    ): List<FormattedEntry> {
        val formattedEntries = mutableListOf<FormattedEntry>()

        if (!record.segments.isNullOrEmpty()) {
            val formattedSegments = formatSegments(record.segments, context)
            formattedEntries.add(FormattedEntry.Header(context.getString(R.string.segments)))
            formattedEntries.addAll(formattedSegments)
        }
        if (!record.laps.isNullOrEmpty()) {
            val formattedLaps = formatLaps(record.laps, context)
            formattedEntries.add(FormattedEntry.Header(context.getString(R.string.laps)))
            formattedEntries.addAll(formattedLaps)
        }

        return formattedEntries
    }

    private fun formatSegments(
        segments: List<ExerciseSegment>,
        context: Context,
    ): List<FormattedSample> {
        val formattedSegments = mutableListOf<FormattedSample>()

        segments.forEach { segment ->
            formattedSegments.add(
                FormattedSample(
                    header = getHeader(segment.startTime, segment.endTime),
                    value = formatSegment(segment, context),
                )
            )
        }
        return formattedSegments
    }

    private fun formatSegment(
        segment: ExerciseSegment,
        context: Context,
        exerciseSegmentTypeFormatter: ExerciseSegmentTypeFormatter = ExerciseSegmentTypeFormatter(),
    ): String {
        return "${exerciseSegmentTypeFormatter.format(segment.segmentType,context)}: ${segment.repetitionsCount} ${context.getString(R.string.repetitions_label)}"
    }

    private fun formatLaps(laps: List<ExerciseLap>, context: Context): List<FormattedSample> {
        val formattedLaps = mutableListOf<FormattedSample>()

        laps.forEach { lap ->
            formattedLaps.add(
                FormattedSample(
                    header = getHeader(lap.startTime, lap.endTime),
                    value =
                        UnitFormatter.formatLength(lap.length!!, Unit.Length.KILOMETERS, context),
                )
            )
        }

        return formattedLaps
    }

    companion object {
        fun formatExerciseType(exerciseType: Int, context: Context): String {

            return when (exerciseType) {
                EXERCISE_SESSION_TYPE_BADMINTON ->
                    context.getString(R.string.exercise_type_badminton)
                EXERCISE_SESSION_TYPE_BIKING -> context.getString(R.string.exercise_type_biking)
                EXERCISE_SESSION_TYPE_BASEBALL -> context.getString(R.string.exercise_type_baseball)
                EXERCISE_SESSION_TYPE_BASKETBALL ->
                    context.getString(R.string.exercise_type_basketball)
                EXERCISE_SESSION_TYPE_BIKING_STATIONARY ->
                    context.getString(R.string.exercise_type_biking_stationary)
                EXERCISE_SESSION_TYPE_BOOT_CAMP ->
                    context.getString(R.string.exercise_type_bootcamp)
                EXERCISE_SESSION_TYPE_BOXING -> context.getString(R.string.exercise_type_boxing)
                EXERCISE_SESSION_TYPE_CALISTHENICS ->
                    context.getString(R.string.exercise_type_calisthenics)
                EXERCISE_SESSION_TYPE_CRICKET -> context.getString(R.string.exercise_type_cricket)
                EXERCISE_SESSION_TYPE_DANCING -> context.getString(R.string.exercise_type_dancing)
                EXERCISE_SESSION_TYPE_ELLIPTICAL ->
                    context.getString(R.string.exercise_type_elliptical)
                EXERCISE_SESSION_TYPE_EXERCISE_CLASS ->
                    context.getString(R.string.exercise_type_exercise_class)
                EXERCISE_SESSION_TYPE_FENCING -> context.getString(R.string.exercise_type_fencing)
                EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN ->
                    context.getString(R.string.exercise_type_football_american)
                EXERCISE_SESSION_TYPE_FOOTBALL_AUSTRALIAN ->
                    context.getString(R.string.exercise_type_football_australian)
                EXERCISE_SESSION_TYPE_FRISBEE_DISC ->
                    context.getString(R.string.exercise_type_frisbee)
                EXERCISE_SESSION_TYPE_GOLF -> context.getString(R.string.exercise_type_golf)
                EXERCISE_SESSION_TYPE_GUIDED_BREATHING ->
                    context.getString(R.string.exercise_type_guided_breathing)
                EXERCISE_SESSION_TYPE_GYMNASTICS ->
                    context.getString(R.string.exercise_type_gymnastics)
                EXERCISE_SESSION_TYPE_HANDBALL -> context.getString(R.string.exercise_type_handball)
                EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
                    context.getString(R.string.exercise_type_high_intensity_interval_training)
                EXERCISE_SESSION_TYPE_HIKING -> context.getString(R.string.exercise_type_hiking)
                EXERCISE_SESSION_TYPE_ICE_HOCKEY ->
                    context.getString(R.string.exercise_type_ice_hockey)
                EXERCISE_SESSION_TYPE_ICE_SKATING ->
                    context.getString(R.string.exercise_type_ice_skating)
                EXERCISE_SESSION_TYPE_MARTIAL_ARTS ->
                    context.getString(R.string.exercise_type_martial_arts)
                EXERCISE_SESSION_TYPE_PADDLING -> context.getString(R.string.exercise_type_paddling)
                EXERCISE_SESSION_TYPE_PARAGLIDING ->
                    context.getString(R.string.exercise_type_paragliding)
                EXERCISE_SESSION_TYPE_PILATES -> context.getString(R.string.exercise_type_pilates)
                EXERCISE_SESSION_TYPE_RACQUETBALL ->
                    context.getString(R.string.exercise_type_racquetball)
                EXERCISE_SESSION_TYPE_ROCK_CLIMBING ->
                    context.getString(R.string.exercise_type_rock_climbing)
                EXERCISE_SESSION_TYPE_ROLLER_HOCKEY ->
                    context.getString(R.string.exercise_type_roller_hockey)
                EXERCISE_SESSION_TYPE_ROWING -> context.getString(R.string.exercise_type_rowing)
                EXERCISE_SESSION_TYPE_ROWING_MACHINE ->
                    context.getString(R.string.exercise_type_rowing_machine)
                EXERCISE_SESSION_TYPE_RUGBY -> context.getString(R.string.exercise_type_rugby)
                EXERCISE_SESSION_TYPE_RUNNING -> context.getString(R.string.exercise_type_running)
                EXERCISE_SESSION_TYPE_RUNNING_TREADMILL ->
                    context.getString(R.string.exercise_type_treadmill)
                EXERCISE_SESSION_TYPE_SAILING -> context.getString(R.string.exercise_type_sailing)
                EXERCISE_SESSION_TYPE_SCUBA_DIVING ->
                    context.getString(R.string.exercise_type_scuba_diving)
                EXERCISE_SESSION_TYPE_SKATING -> context.getString(R.string.exercise_type_skating)
                EXERCISE_SESSION_TYPE_SKIING -> context.getString(R.string.exercise_type_skiing)
                EXERCISE_SESSION_TYPE_SNOWBOARDING ->
                    context.getString(R.string.exercise_type_snowboarding)
                EXERCISE_SESSION_TYPE_SNOWSHOEING ->
                    context.getString(R.string.exercise_type_snowshoeing)
                EXERCISE_SESSION_TYPE_SOCCER -> context.getString(R.string.exercise_type_soccer)
                EXERCISE_SESSION_TYPE_SOFTBALL -> context.getString(R.string.exercise_type_softball)
                EXERCISE_SESSION_TYPE_SQUASH -> context.getString(R.string.exercise_type_squash)
                EXERCISE_SESSION_TYPE_STAIR_CLIMBING ->
                    context.getString(R.string.exercise_type_stair_climbing)
                EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE ->
                    context.getString(R.string.exercise_type_stair_climbing_machine)
                EXERCISE_SESSION_TYPE_STRENGTH_TRAINING ->
                    context.getString(R.string.exercise_type_strength_training)
                EXERCISE_SESSION_TYPE_STRETCHING ->
                    context.getString(R.string.exercise_type_stretching)
                EXERCISE_SESSION_TYPE_SURFING -> context.getString(R.string.exercise_type_surfing)
                EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER ->
                    context.getString(R.string.exercise_type_swimming_open_water)
                EXERCISE_SESSION_TYPE_SWIMMING_POOL ->
                    context.getString(R.string.exercise_type_swimming)
                EXERCISE_SESSION_TYPE_TABLE_TENNIS ->
                    context.getString(R.string.exercise_type_table_tennis)
                EXERCISE_SESSION_TYPE_TENNIS -> context.getString(R.string.exercise_type_tennis)
                EXERCISE_SESSION_TYPE_VOLLEYBALL ->
                    context.getString(R.string.exercise_type_volleyball)
                EXERCISE_SESSION_TYPE_WALKING -> context.getString(R.string.exercise_type_walking)
                EXERCISE_SESSION_TYPE_WATER_POLO ->
                    context.getString(R.string.exercise_type_water_polo)
                EXERCISE_SESSION_TYPE_WEIGHTLIFTING ->
                    context.getString(R.string.exercise_type_weightlifting)
                EXERCISE_SESSION_TYPE_WHEELCHAIR ->
                    context.getString(R.string.exercise_type_wheelchair)
                EXERCISE_SESSION_TYPE_YOGA -> context.getString(R.string.exercise_type_yoga)
                EXERCISE_SESSION_TYPE_OTHER_WORKOUT ->
                    context.getString(R.string.exercise_type_other)
                else -> context.getString(R.string.exercise_type_unknown_exercise_type)
            }
        }
    }
}
