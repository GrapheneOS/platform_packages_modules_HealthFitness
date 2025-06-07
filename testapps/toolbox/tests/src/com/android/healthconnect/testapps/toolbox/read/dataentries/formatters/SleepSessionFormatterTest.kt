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
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_AWAKE
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_IN_BED
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_OUT_OF_BED
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_REM
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_UNKNOWN
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepSessionFormatterTest {
    private val formatter = SleepSessionFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatSleepSessionValue_returnsFormattedEntry_noStages() {
        val record = buildSleepSessionRecord().build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Sleep session")
        assertThat(formattedEntry.dataDetails).isEqualTo(listOf(FormattedEntry.Header("No stages")))
    }

    @Test
    fun formatSleepSessionValue_returnsFormattedEntry_withNotes() {
        val record = buildSleepSessionRecord().setNotes("Test Notes").build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Sleep session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(
                    FormattedDataEntry(header = "Notes", value = "Test Notes"),
                    FormattedEntry.Header("No stages"),
                )
            )
    }

    @Test
    fun formatSleepSessionValue_returnsFormattedEntry_withTitle() {
        val record = buildSleepSessionRecord().setTitle("Test Sleep Title").build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Test Sleep Title")
        assertThat(formattedEntry.dataDetails).isEqualTo(listOf(FormattedEntry.Header("No stages")))
    }

    @Test
    fun formatSleepSessionValue_returnsFormattedEntry_withStages() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val stages =
            listOf(
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(30),
                    NOW.minusSeconds(28),
                    STAGE_TYPE_AWAKE,
                ),
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(28),
                    NOW.minusSeconds(26),
                    STAGE_TYPE_AWAKE_IN_BED,
                ),
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(26),
                    NOW.minusSeconds(24),
                    STAGE_TYPE_SLEEPING_DEEP,
                ),
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(24),
                    NOW.minusSeconds(22),
                    STAGE_TYPE_SLEEPING_LIGHT,
                ),
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(22),
                    NOW.minusSeconds(20),
                    STAGE_TYPE_AWAKE_OUT_OF_BED,
                ),
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(20),
                    NOW.minusSeconds(18),
                    STAGE_TYPE_SLEEPING_REM,
                ),
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(18),
                    NOW.minusSeconds(16),
                    STAGE_TYPE_SLEEPING,
                ),
                SleepSessionRecord.Stage(
                    NOW.minusSeconds(16),
                    NOW.minusSeconds(14),
                    STAGE_TYPE_UNKNOWN,
                ),
            )
        val record = buildSleepSessionRecord().setStages(stages).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Sleep session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(
                listOf(
                    FormattedEntry.FormattedSample(header = "16:35 - 16:35", value = "Awake"),
                    FormattedEntry.FormattedSample(
                        header = "16:35 - 16:35",
                        value = "Awake in bed",
                    ),
                    FormattedEntry.FormattedSample(header = "16:35 - 16:35", value = "Deep sleep"),
                    FormattedEntry.FormattedSample(header = "16:35 - 16:35", value = "Light sleep"),
                    FormattedEntry.FormattedSample(
                        header = "16:35 - 16:35",
                        value = "Awake out of bed",
                    ),
                    FormattedEntry.FormattedSample(header = "16:35 - 16:36", value = "REM sleep"),
                    FormattedEntry.FormattedSample(header = "16:36 - 16:36", value = "Sleeping"),
                    FormattedEntry.FormattedSample(header = "16:36 - 16:36", value = "Unknown"),
                )
            )
    }

    private fun buildSleepSessionRecord(): SleepSessionRecord.Builder {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return SleepSessionRecord.Builder(getMetaData(context), NOW.minusSeconds(30), NOW)
    }
}
