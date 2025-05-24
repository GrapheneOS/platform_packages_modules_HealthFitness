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
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UNKNOWN
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING
import android.health.connect.datatypes.units.Length
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSessionFormatterTest {
    private val formatter = ExerciseSessionFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatExerciseSessionValue_returnsFormattedEntry_empty() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val record = buildExerciseSession(context, NOW, EXERCISE_SESSION_TYPE_BIKING).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Biking",
                    note = null,
                    title = null,
                    route = null,
                    sessionDetails = listOf(),
                )
            )
    }

    @Test
    fun formatExerciseSessionValue_returnsFormattedEntry_withSegments() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val exerciseSegments =
            listOf(
                buildExerciseSegment(EXERCISE_SEGMENT_TYPE_BIKING, NOW).build(),
                buildExerciseSegment(EXERCISE_SEGMENT_TYPE_UNKNOWN, NOW.plusSeconds(10)).build(),
            )
        val record =
            buildExerciseSession(context, NOW, EXERCISE_SESSION_TYPE_BIKING)
                .setSegments(exerciseSegments)
                .build()

        val formattedEntry = formatter.format(record, context)

        val expectedSessionDetails =
            listOf(
                FormattedEntry.Header(value = "Segments"),
                FormattedEntry.FormattedSample(header = "16:36 - 16:36", value = "Biking: 0 reps"),
                FormattedEntry.FormattedSample(header = "16:36 - 16:36", value = "Unknown: 0 reps"),
            )
        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Biking",
                    note = null,
                    title = null,
                    route = null,
                    sessionDetails = expectedSessionDetails,
                )
            )
    }

    @Test
    fun formatExerciseSessionValue_returnsFormattedEntry_withLaps() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val exerciseLap =
            listOf(
                buildExerciseLap(NOW).setLength(Length.fromMeters(1000.0)).build(),
                buildExerciseLap(NOW.plusSeconds(10)).setLength(Length.fromMeters(500.0)).build(),
            )
        val record =
            buildExerciseSession(context, NOW, EXERCISE_SESSION_TYPE_BIKING)
                .setLaps(exerciseLap)
                .build()

        val formattedEntry = formatter.format(record, context)

        val expectedSessionDetails =
            listOf(
                FormattedEntry.Header(value = "Laps"),
                FormattedEntry.FormattedSample(header = "16:36 - 16:36", value = "1.0 km"),
                FormattedEntry.FormattedSample(header = "16:36 - 16:36", value = "0.5 km"),
            )
        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Biking",
                    note = null,
                    title = null,
                    route = null,
                    sessionDetails = expectedSessionDetails,
                )
            )
    }

    @Test
    fun formatExerciseSessionValue_returnsFormattedEntry_withNote() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val record =
            buildExerciseSession(context, NOW, EXERCISE_SESSION_TYPE_BIKING)
                .setNotes("Exercise Notes")
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Biking",
                    note = "Exercise Notes",
                    title = null,
                    route = null,
                    sessionDetails = listOf(),
                )
            )
    }

    @Test
    fun formatExerciseSessionValue_returnsFormattedEntry_withTitle() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val record =
            buildExerciseSession(context, NOW, EXERCISE_SESSION_TYPE_BIKING)
                .setTitle("Exercise Title")
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry)
            .isEqualTo(
                FormattedEntry.FormattedExerciseSession(
                    header = "16:36 - 16:36",
                    type = "Biking",
                    note = null,
                    title = "Exercise Title",
                    route = null,
                    sessionDetails = listOf(),
                )
            )
    }

    private fun buildExerciseLap(startTime: Instant): ExerciseLap.Builder {
        return ExerciseLap.Builder(startTime, startTime.plusSeconds(1))
    }

    private fun buildExerciseSegment(
        segmentType: Int,
        startTime: Instant,
    ): ExerciseSegment.Builder {
        return ExerciseSegment.Builder(startTime, startTime.plusSeconds(1), segmentType)
    }

    private fun buildExerciseSession(
        context: Context,
        startTime: Instant,
        exerciseType: Int,
    ): ExerciseSessionRecord.Builder {
        return ExerciseSessionRecord.Builder(
            getMetaData(context),
            startTime,
            startTime.plusSeconds(20),
            exerciseType,
        )
    }
}
