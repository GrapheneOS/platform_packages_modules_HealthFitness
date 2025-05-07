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
package com.android.healthconnect.testapps.toolbox.read.dataentries.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DurationFormatterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatDuration_returnsCorrectValue_withDays() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "4 days"

        val durationString = formatter.format(Duration.ofDays(4), context)

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    @Test
    fun formatDuration_returnsCorrectValue_withHours() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "2 hrs"

        val durationString = formatter.format(Duration.ofHours(2), context)

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    @Test
    fun formatDuration_returnsCorrectValue_withMinutes() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "32 mins"

        val durationString = formatter.format(Duration.ofMinutes(32), context)

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    @Test
    fun formatDuration_returnsCorrectValue_withSeconds() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "24 seconds"

        val durationString = formatter.format(Duration.ofSeconds(24), context)

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    @Test
    fun formatDuration_returnsCorrectValue_withHoursAndMinutes() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "2 hrs 20 mins"

        val durationString = formatter.format(Duration.ofHours(2) + Duration.ofMinutes(20), context)

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    fun formatDuration_returnsCorrectValue_withMinutesAndSeconds() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "3 mins 21 seconds"

        val durationString =
            formatter.format(Duration.ofMinutes(3) + Duration.ofSeconds(21), context)

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    @Test
    fun formatDuration_returnsCorrectValue_withHoursAndMinutesAndSeconds() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "4 hrs 22 mins 4 seconds"

        val durationString =
            formatter.format(
                Duration.ofHours(4) + Duration.ofMinutes(22) + Duration.ofSeconds(4),
                context,
            )

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    @Test
    fun formatDuration_returnsCorrectValue_withDaysAndHoursAndMinutesAndSeconds() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "7 days 5 hrs 55 mins 12 seconds"

        val durationString =
            formatter.format(
                Duration.ofDays(7) +
                    Duration.ofHours(5) +
                    Duration.ofMinutes(55) +
                    Duration.ofSeconds(12),
                context,
            )

        assertThat(durationString).isEqualTo(expectedDurationString)
    }

    @Test
    fun formatDuration_returnsCorrectValue_empty() {
        val formatter = DurationFormatter.Companion
        val expectedDurationString = "0 seconds"

        val durationString = formatter.format(Duration.ofSeconds(0), context)

        assertThat(durationString).isEqualTo(expectedDurationString)
    }
}
