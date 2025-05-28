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
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindfulnessSessionFormatterTest {
    private val formatter = MindfulnessSessionFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatMindfulnessSession_returnsFormattedEntry_breathing() {
        val record = getMindfulnessSessionRecord(MINDFULNESS_SESSION_TYPE_BREATHING)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Mindfulness session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.FormattedDataEntry("Type", "Breathing")))
    }

    @Test
    fun formatMindfulnessSession_returnsFormattedEntry_meditation() {
        val record = getMindfulnessSessionRecord(MINDFULNESS_SESSION_TYPE_MEDITATION)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Mindfulness session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.FormattedDataEntry("Type", "Meditation")))
    }

    @Test
    fun formatMindfulnessSession_returnsFormattedEntry_movement() {
        val record = getMindfulnessSessionRecord(MINDFULNESS_SESSION_TYPE_MOVEMENT)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Mindfulness session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.FormattedDataEntry("Type", "Movement")))
    }

    @Test
    fun formatMindfulnessSession_returnsFormattedEntry_music() {
        val record = getMindfulnessSessionRecord(MINDFULNESS_SESSION_TYPE_MUSIC)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Mindfulness session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.FormattedDataEntry("Type", "Music")))
    }

    @Test
    fun formatMindfulnessSession_returnsFormattedEntry_other() {
        val record = getMindfulnessSessionRecord(MINDFULNESS_SESSION_TYPE_OTHER)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Mindfulness session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.FormattedDataEntry("Type", "Other")))
    }

    @Test
    fun formatMindfulnessSession_returnsFormattedEntry_unguided() {
        val record = getMindfulnessSessionRecord(MINDFULNESS_SESSION_TYPE_UNGUIDED)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Mindfulness session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.FormattedDataEntry("Type", "Unguided")))
    }

    @Test
    fun formatMindfulnessSession_returnsFormattedEntry_unknown() {
        val record = getMindfulnessSessionRecord()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("Mindfulness session")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.FormattedDataEntry("Type", "Unknown")))
    }

    private fun getMindfulnessSessionRecord(
        mindfulnessSessionType: Int = MINDFULNESS_SESSION_TYPE_UNKNOWN
    ): MindfulnessSessionRecord {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return MindfulnessSessionRecord.Builder(
                getMetaData(context),
                NOW,
                NOW.plusSeconds(1),
                mindfulnessSessionType,
            )
            .build()
    }
}
