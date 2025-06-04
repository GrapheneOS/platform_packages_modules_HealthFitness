/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.healthconnect.controller.data.formatters

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
import android.health.connect.datatypes.units.Mass
import android.icu.text.MessageFormat.format
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.ExerciseSessionEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedHeaderlessSessionDetail
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSegment
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSessionDetail
import com.android.healthconnect.controller.data.entries.FormattedEntry.SessionHeader
import com.android.healthconnect.controller.data.formatters.DurationFormatter.formatDurationLong
import com.android.healthconnect.controller.data.formatters.DurationFormatter.formatDurationShort
import com.android.healthconnect.controller.data.formatters.shared.BaseFormatter
import com.android.healthconnect.controller.data.formatters.shared.LengthFormatter
import com.android.healthconnect.controller.data.formatters.shared.RecordDetailsFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthfitness.flags.AconfigFlagHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import kotlin.math.floor

/** Formatter for printing ExerciseSessionRecord data. */
class ExerciseSessionFormatter
@Inject
constructor(
    @ApplicationContext context: Context,
    timeFormatter: LocalDateTimeFormatter,
    unitPreferences: UnitPreferences,
    private val exerciseSegmentTypeFormatter: ExerciseSegmentTypeFormatter,
) :
    BaseFormatter<ExerciseSessionRecord>(context, timeFormatter, unitPreferences),
    RecordDetailsFormatter<ExerciseSessionRecord> {

    override suspend fun formatRecord(
        record: ExerciseSessionRecord,
        header: String,
        headerA11y: String,
    ): FormattedEntry {
        return ExerciseSessionEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatValue(record),
            titleA11y = formatA11yValue(record),
            dataType = record::class,
            notes = getNotes(record),
            route = record.route,
        )
    }

    fun formatValue(record: ExerciseSessionRecord): String {
        return formatSession(record) { duration -> formatDurationShort(context, duration) }
    }

    fun formatA11yValue(record: ExerciseSessionRecord): String {
        return formatSession(record) { duration -> formatDurationLong(context, duration) }
    }

    fun getNotes(record: ExerciseSessionRecord): String? {
        return record.notes?.toString()
    }

    override suspend fun formatRecordDetails(record: ExerciseSessionRecord): List<FormattedEntry> {
        val sortedSegments =
            record.segments
                .sortedBy { it.startTime }
                .map {
                    if (AconfigFlagHelper.isExerciseSegmentImprovementsEnabled()) {
                        formatSegmentWithNewFields(record.metadata.id, it)
                    } else {
                        formatSegment(record.metadata.id, it)
                    }
                }
        val sortedLaps =
            record.laps.sortedBy { it.startTime }.map { formatLaps(record.metadata.id, it) }
        return buildList {
            if (
                AconfigFlagHelper.isExerciseSegmentImprovementsEnabled() &&
                    record.hasRateOfPerceivedExertion()
            ) {
                add(SessionHeader(context.getString(R.string.session_rpe_header)))
                add(
                    buildSessionRpeFormattedEntry(
                        record.metadata.id,
                        record.rateOfPerceivedExertion,
                    )
                )
            }
            if (sortedSegments.isNotEmpty()) {
                add(SessionHeader(context.getString(R.string.exercise_segments_header)))
                addAll(sortedSegments)
            }
            if (sortedLaps.isNotEmpty()) {
                add(SessionHeader(context.getString(R.string.exercise_laps_header)))
                addAll(sortedLaps)
            }
        }
    }

    private fun formatSession(
        record: ExerciseSessionRecord,
        formatDuration: (duration: Duration) -> String,
    ): String {
        val type = getExerciseType(context, record.exerciseType)
        return if (!record.title.isNullOrBlank()) {
            context.getString(R.string.session_title, record.title, type)
        } else {
            val duration = Duration.between(record.startTime, record.endTime)
            context.getString(R.string.session_title, formatDuration(duration), type)
        }
    }

    private fun formatSegment(id: String, segment: ExerciseSegment): FormattedSessionDetail {
        return FormattedSessionDetail(
            uuid = id,
            header = timeFormatter.formatTimeRange(segment.startTime, segment.endTime),
            headerA11y = timeFormatter.formatTimeRangeA11y(segment.startTime, segment.endTime),
            title = formatSegmentTitle(segment.repetitionsCount, segment.segmentType),
            titleA11y = formatSegmentTitleA11y(segment.repetitionsCount, segment.segmentType),
        )
    }

    private fun formatSegmentWithNewFields(id: String, segment: ExerciseSegment): FormattedSegment {
        return FormattedSegment(
            uuid = id,
            header = timeFormatter.formatTimeRange(segment.startTime, segment.endTime),
            headerA11y = timeFormatter.formatTimeRangeA11y(segment.startTime, segment.endTime),
            title = formatSegmentTitle(segment.repetitionsCount, segment.segmentType),
            titleA11y = formatSegmentTitleA11y(segment.repetitionsCount, segment.segmentType),
            setIndex = if (segment.hasSetIndex()) formatSegmentSetIndex(segment.setIndex) else null,
            setIndexA11y =
                if (segment.hasSetIndex()) formatSegmentSetIndex(segment.setIndex) else null,
            weight = if (segment.weight != null) formatSegmentWeight(segment.weight!!) else null,
            weightA11y =
                if (segment.weight != null) formatSegmentWeightA11y(segment.weight!!) else null,
            rpe =
                if (segment.hasRateOfPerceivedExertion())
                    formatSegmentRpe(segment.rateOfPerceivedExertion)
                else null,
            rpeA11y =
                if (segment.hasRateOfPerceivedExertion())
                    formatSegmentRpeA11y(segment.rateOfPerceivedExertion)
                else null,
        )
    }

    private fun formatLaps(id: String, lap: ExerciseLap): FormattedSessionDetail {
        return FormattedSessionDetail(
            uuid = id,
            header = timeFormatter.formatTimeRange(lap.startTime, lap.endTime),
            headerA11y = timeFormatter.formatTimeRangeA11y(lap.startTime, lap.endTime),
            title = LengthFormatter.formatValue(context, lap.length, unitPreferences),
            titleA11y = LengthFormatter.formatA11yValue(context, lap.length, unitPreferences),
        )
    }

    private fun formatSegmentTitle(repetitionsCount: Int, type: Int): String {
        val segmentType = exerciseSegmentTypeFormatter.getSegmentType(type)
        if (repetitionsCount != 0 || !AconfigFlagHelper.isExerciseSegmentImprovementsEnabled()) {
            val repetitions =
                format(context.getString(R.string.repetitions), mapOf("count" to repetitionsCount))
            return context.getString(R.string.repetitions_format, segmentType, repetitions)
        }
        return segmentType
    }

    private fun formatSegmentTitleA11y(repetitionsCount: Int, type: Int): String {
        val segmentType = exerciseSegmentTypeFormatter.getSegmentType(type)
        if (repetitionsCount != 0 || !AconfigFlagHelper.isExerciseSegmentImprovementsEnabled()) {
            val repetitions =
                format(
                    context.getString(R.string.repetitions_long),
                    mapOf("count" to repetitionsCount),
                )
            return context.getString(R.string.repetitions_format, segmentType, repetitions)
        }
        return segmentType
    }

    private fun buildSessionRpeFormattedEntry(
        id: String,
        rpe: Float,
    ): FormattedHeaderlessSessionDetail {
        val formattedRpeValue = formatRpeValue(rpe)
        return FormattedHeaderlessSessionDetail(
            uuid = id,
            title = formattedRpeValue,
            titleA11y = formattedRpeValue,
        )
    }

    private fun formatSegmentSetIndex(setIndex: Int) =
        context.getString(R.string.segment_set_index_format, setIndex)

    private fun formatSegmentWeight(mass: Mass) =
        MassFormatter.formatValue(context, mass, unitPreferences.weightUnit)

    private fun formatSegmentWeightA11y(mass: Mass) =
        MassFormatter.formatA11yValue(context, mass, unitPreferences.weightUnit)

    private fun formatSegmentRpe(rpe: Float) =
        context.getString(R.string.segment_rpe_format, formatRpeValue(rpe))

    private fun formatSegmentRpeA11y(rpe: Float) =
        context.getString(R.string.segment_rpe_format_long, formatRpeValue(rpe))

    private fun formatRpeValue(rpe: Float) =
        if (floor(rpe) == rpe) rpe.toInt().toString() else rpe.toString()

    companion object {
        fun getExerciseType(context: Context, type: Int): String {
            return when (type) {
                EXERCISE_SESSION_TYPE_BADMINTON -> context.getString(R.string.badminton)
                EXERCISE_SESSION_TYPE_BASEBALL -> context.getString(R.string.baseball)
                EXERCISE_SESSION_TYPE_BASKETBALL -> context.getString(R.string.basketball)
                EXERCISE_SESSION_TYPE_BIKING -> context.getString(R.string.biking)
                EXERCISE_SESSION_TYPE_BIKING_STATIONARY ->
                    context.getString(R.string.biking_stationary)

                EXERCISE_SESSION_TYPE_BOOT_CAMP -> context.getString(R.string.boot_camp)
                EXERCISE_SESSION_TYPE_BOXING -> context.getString(R.string.boxing)
                EXERCISE_SESSION_TYPE_CALISTHENICS -> context.getString(R.string.calisthenics)
                EXERCISE_SESSION_TYPE_CRICKET -> context.getString(R.string.cricket)
                EXERCISE_SESSION_TYPE_DANCING -> context.getString(R.string.dancing)
                EXERCISE_SESSION_TYPE_ELLIPTICAL -> context.getString(R.string.elliptical)
                EXERCISE_SESSION_TYPE_EXERCISE_CLASS -> context.getString(R.string.exercise_class)
                EXERCISE_SESSION_TYPE_FENCING -> context.getString(R.string.fencing)
                EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN ->
                    context.getString(R.string.football_american)

                EXERCISE_SESSION_TYPE_FOOTBALL_AUSTRALIAN ->
                    context.getString(R.string.activity_type_australian_football)

                EXERCISE_SESSION_TYPE_FRISBEE_DISC -> context.getString(R.string.frisbee_disc)
                EXERCISE_SESSION_TYPE_GOLF -> context.getString(R.string.golf)
                EXERCISE_SESSION_TYPE_GUIDED_BREATHING ->
                    context.getString(R.string.guided_breathing)

                EXERCISE_SESSION_TYPE_GYMNASTICS -> context.getString(R.string.gymnastics)
                EXERCISE_SESSION_TYPE_HANDBALL -> context.getString(R.string.handball)
                EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
                    context.getString(R.string.high_intensity_interval_training)

                EXERCISE_SESSION_TYPE_HIKING -> context.getString(R.string.hiking)
                EXERCISE_SESSION_TYPE_ICE_HOCKEY -> context.getString(R.string.ice_hockey)
                EXERCISE_SESSION_TYPE_ICE_SKATING -> context.getString(R.string.ice_skating)
                EXERCISE_SESSION_TYPE_MARTIAL_ARTS -> context.getString(R.string.martial_arts)
                EXERCISE_SESSION_TYPE_OTHER_WORKOUT -> context.getString(R.string.workout)
                EXERCISE_SESSION_TYPE_PADDLING -> context.getString(R.string.paddling)
                EXERCISE_SESSION_TYPE_PILATES -> context.getString(R.string.pilates)
                EXERCISE_SESSION_TYPE_RACQUETBALL -> context.getString(R.string.racquetball)
                EXERCISE_SESSION_TYPE_ROCK_CLIMBING -> context.getString(R.string.rock_climbing)
                EXERCISE_SESSION_TYPE_ROLLER_HOCKEY -> context.getString(R.string.roller_hockey)
                EXERCISE_SESSION_TYPE_ROWING -> context.getString(R.string.rowing)
                EXERCISE_SESSION_TYPE_ROWING_MACHINE -> context.getString(R.string.rowing_machine)
                EXERCISE_SESSION_TYPE_RUGBY -> context.getString(R.string.rugby)
                EXERCISE_SESSION_TYPE_RUNNING -> context.getString(R.string.running)
                EXERCISE_SESSION_TYPE_RUNNING_TREADMILL ->
                    context.getString(R.string.running_treadmill)

                EXERCISE_SESSION_TYPE_SAILING -> context.getString(R.string.sailing)
                EXERCISE_SESSION_TYPE_SCUBA_DIVING -> context.getString(R.string.scuba_diving)
                EXERCISE_SESSION_TYPE_SKATING -> context.getString(R.string.skating)
                EXERCISE_SESSION_TYPE_SKIING -> context.getString(R.string.skiing)
                EXERCISE_SESSION_TYPE_SNOWBOARDING -> context.getString(R.string.snowboarding)
                EXERCISE_SESSION_TYPE_SNOWSHOEING -> context.getString(R.string.snowshoeing)
                EXERCISE_SESSION_TYPE_SOCCER -> context.getString(R.string.soccer)
                EXERCISE_SESSION_TYPE_SOFTBALL -> context.getString(R.string.softball)
                EXERCISE_SESSION_TYPE_SQUASH -> context.getString(R.string.squash)
                EXERCISE_SESSION_TYPE_STAIR_CLIMBING -> context.getString(R.string.stair_climbing)
                EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE ->
                    context.getString(R.string.stair_climbing_machine)

                EXERCISE_SESSION_TYPE_STRENGTH_TRAINING ->
                    context.getString(R.string.strength_training)

                EXERCISE_SESSION_TYPE_STRETCHING -> context.getString(R.string.stretching)
                EXERCISE_SESSION_TYPE_SURFING -> context.getString(R.string.surfing)
                EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER ->
                    context.getString(R.string.swimming_open_water)

                EXERCISE_SESSION_TYPE_SWIMMING_POOL -> context.getString(R.string.swimming_pool)
                EXERCISE_SESSION_TYPE_TABLE_TENNIS -> context.getString(R.string.table_tennis)
                EXERCISE_SESSION_TYPE_TENNIS -> context.getString(R.string.tennis)
                EXERCISE_SESSION_TYPE_VOLLEYBALL -> context.getString(R.string.volleyball)
                EXERCISE_SESSION_TYPE_WALKING -> context.getString(R.string.walking)
                EXERCISE_SESSION_TYPE_WATER_POLO -> context.getString(R.string.water_polo)
                EXERCISE_SESSION_TYPE_WEIGHTLIFTING -> context.getString(R.string.weightlifting)
                EXERCISE_SESSION_TYPE_WHEELCHAIR -> context.getString(R.string.wheelchair)
                EXERCISE_SESSION_TYPE_YOGA -> context.getString(R.string.yoga)
                EXERCISE_SESSION_TYPE_PARAGLIDING -> context.getString(R.string.paragliding)
                else -> context.getString(R.string.unknown_type)
            }
        }
    }
}
