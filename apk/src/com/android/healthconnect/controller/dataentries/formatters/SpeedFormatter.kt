/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.ExerciseSegmentType
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.SpeedRecord.SpeedRecordSample
import android.health.connect.datatypes.units.Velocity
import android.icu.text.MessageFormat
import android.text.format.DateUtils.formatElapsedTime
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSessionDetail
import com.android.healthconnect.controller.dataentries.formatters.shared.EntryFormatter
import com.android.healthconnect.controller.dataentries.formatters.shared.RecordDetailsFormatter
import com.android.healthconnect.controller.units.DistanceUnit.KILOMETERS
import com.android.healthconnect.controller.units.DistanceUnit.MILES
import com.android.healthconnect.controller.units.SpeedConverter.convertToDistancePerHour
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/** Formatter for printing Speed series data. */
class SpeedFormatter @Inject constructor(@ApplicationContext private val context: Context) :
    EntryFormatter<SpeedRecord>(context), RecordDetailsFormatter<SpeedRecord> {

    private val timeFormatter = LocalDateTimeFormatter(context)

    private val METER_TO_YARD = 1.09361

    override suspend fun formatRecord(
        record: SpeedRecord,
        header: String,
        headerA11y: String,
        unitPreferences: UnitPreferences,
    ): FormattedEntry {
        return FormattedEntry.SeriesDataEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatValue(record, unitPreferences),
            titleA11y = formatA11yValue(record, unitPreferences),
            dataType = record::class,
        )
    }

    override suspend fun formatValue(
        record: SpeedRecord,
        unitPreferences: UnitPreferences,
    ): String {
        val res = getUnitRes(unitPreferences)
        return formatRecord(res, record.samples, unitPreferences)
    }

    override suspend fun formatA11yValue(
        record: SpeedRecord,
        unitPreferences: UnitPreferences,
    ): String {
        val res = getA11yUnitRes(unitPreferences)
        return formatRecord(res, record.samples, unitPreferences)
    }

    override suspend fun formatRecordDetails(record: SpeedRecord): List<FormattedEntry> {
        return record.samples
            .sortedBy { it.time }
            .map { formatSample(record.metadata.id, it, unitPreferences) }
    }

    private fun formatSample(
        id: String,
        sample: SpeedRecordSample,
        unitPreferences: UnitPreferences,
    ): FormattedSessionDetail {
        return FormattedSessionDetail(
            uuid = id,
            header = timeFormatter.formatTime(sample.time),
            headerA11y = timeFormatter.formatTime(sample.time),
            title =
                formatSpeedValue(
                    getUnitRes(unitPreferences),
                    sample.speed.inMetersPerSecond,
                    unitPreferences,
                ),
            titleA11y =
                formatSpeedValue(
                    getA11yUnitRes(unitPreferences),
                    sample.speed.inMetersPerSecond,
                    unitPreferences,
                ),
        )
    }

    private fun formatRecord(
        @StringRes res: Int,
        samples: List<SpeedRecordSample>,
        unitPreferences: UnitPreferences,
    ): String {
        if (samples.isEmpty()) {
            return context.getString(R.string.no_data)
        }
        val averageSpeed = samples.sumOf { it.speed.inMetersPerSecond } / samples.size
        return formatSpeedValue(res, averageSpeed, unitPreferences)
    }

    fun formatSpeedValue(
        @StringRes res: Int,
        speed: Double,
        unitPreferences: UnitPreferences,
    ): String {
        val speedWithUnit = convertToDistancePerHour(unitPreferences.getDistanceUnit(), speed)
        return MessageFormat.format(context.getString(res), mapOf("value" to speedWithUnit))
    }

    fun getUnitRes(unitPreferences: UnitPreferences): Int {
        return when (unitPreferences.getDistanceUnit()) {
            MILES -> R.string.velocity_speed_miles
            KILOMETERS -> R.string.velocity_speed_km
        }
    }

    fun getA11yUnitRes(unitPreferences: UnitPreferences): Int {
        return when (unitPreferences.getDistanceUnit()) {
            MILES -> R.string.velocity_speed_miles_long
            KILOMETERS -> R.string.velocity_speed_km_long
        }
    }

    fun formatSpeedValue(
        speed: Velocity,
        unitPreferences: UnitPreferences,
        exerciseSegmentType: Int,
    ): String {
        if (Companion.ACTIVITY_TYPES_WITH_PACE_VELOCITY.contains(exerciseSegmentType)) {
            return formatSpeedValueToMinPerDistance(
                getUnitResInMinPerDistance(unitPreferences),
                speed,
                unitPreferences,
            )
        } else if (Companion.SWIMMING_ACTIVITY_TYPES.contains(exerciseSegmentType)) {
            return formatSpeedValueToMinPerOneHundredDistance(
                getUnitResInMinPerOneHundredDistance(unitPreferences),
                speed,
                unitPreferences,
            )
        }
        return formatSpeedValue(
            getUnitRes(unitPreferences),
            speed.inMetersPerSecond,
            unitPreferences,
        )
    }

    fun formatA11ySpeedValue(
        speed: Velocity,
        unitPreferences: UnitPreferences,
        exerciseSegmentType: Int,
    ): String {
        if (Companion.ACTIVITY_TYPES_WITH_PACE_VELOCITY.contains(exerciseSegmentType)) {
            return formatSpeedValueToMinPerDistance(
                getA11yUnitResInMinPerDistance(unitPreferences),
                speed,
                unitPreferences,
            )
        }
        if (Companion.SWIMMING_ACTIVITY_TYPES.contains(exerciseSegmentType)) {
            return formatSpeedValueToMinPerOneHundredDistance(
                getA11yUnitResInMinPerOneHundredDistance(unitPreferences),
                speed,
                unitPreferences,
            )
        }
        return formatSpeedValue(
            getA11yUnitRes(unitPreferences),
            speed.inMetersPerSecond,
            unitPreferences,
        )
    }

    private fun formatSpeedValueToMinPerDistance(
        @StringRes res: Int,
        speed: Velocity,
        unitPreferences: UnitPreferences,
    ): String {
        val timePerUnitInSeconds =
            if (speed.inMetersPerSecond != 0.0)
                3600 /
                    convertToDistancePerHour(
                        unitPreferences.getDistanceUnit(),
                        speed.inMetersPerSecond,
                    )
            else speed.inMetersPerSecond

        // Display "--:--" if pace value is unrealistic
        if (timePerUnitInSeconds.toLong() > 32400) {
            return context.getString(R.string.elapsed_time_placeholder)
        }

        return context.getString(res, formatElapsedTime(timePerUnitInSeconds.toLong()))
    }

    private fun getUnitResInMinPerDistance(unitPreferences: UnitPreferences): Int {
        return when (unitPreferences.getDistanceUnit()) {
            MILES -> R.string.velocity_minute_miles
            KILOMETERS -> R.string.velocity_minute_km
        }
    }

    private fun getA11yUnitResInMinPerDistance(unitPreferences: UnitPreferences): Int {
        return when (unitPreferences.getDistanceUnit()) {
            MILES -> R.string.velocity_minute_miles_long
            KILOMETERS -> R.string.velocity_minute_km_long
        }
    }

    private fun formatSpeedValueToMinPerOneHundredDistance(
        @StringRes res: Int,
        speed: Velocity,
        unitPreferences: UnitPreferences,
    ): String {
        val timePerUnitInSeconds =
            if (
                unitPreferences.getDistanceUnit() == MILES && Locale.getDefault().equals(Locale.US)
            ) {
                val yardsPerSecond = speed.inMetersPerSecond * METER_TO_YARD
                if (yardsPerSecond != 0.0) 100 / yardsPerSecond else yardsPerSecond
            } else {
                if (speed.inMetersPerSecond != 0.0) 100 / speed.inMetersPerSecond
                else speed.inMetersPerSecond
            }

        // Display "--:--" if pace value is unrealistic
        if (timePerUnitInSeconds.toLong() > 32400) {
            return context.getString(R.string.elapsed_time_placeholder)
        }

        return context.getString(res, formatElapsedTime(timePerUnitInSeconds.toLong()))
    }

    private fun getUnitResInMinPerOneHundredDistance(unitPreferences: UnitPreferences): Int {
        return when (unitPreferences.getDistanceUnit()) {
            MILES ->
                if (Locale.getDefault().equals(Locale.US))
                    R.string.velocity_minute_per_one_hundred_yards
                else R.string.velocity_minute_per_one_hundred_meters
            KILOMETERS -> R.string.velocity_minute_per_one_hundred_meters
        }
    }

    private fun getA11yUnitResInMinPerOneHundredDistance(unitPreferences: UnitPreferences): Int {
        return when (unitPreferences.getDistanceUnit()) {
            MILES ->
                if (Locale.getDefault().equals(Locale.US))
                    R.string.velocity_minute_per_one_hundred_yards_long
                else R.string.velocity_minute_per_one_hundred_meters_long
            KILOMETERS -> R.string.velocity_minute_per_one_hundred_meters_long
        }
    }

    companion object {
        val ACTIVITY_TYPES_WITH_PACE_VELOCITY =
            listOf(
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WALKING,
            )
        val SWIMMING_ACTIVITY_TYPES =
            listOf(
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
            )
    }
}
