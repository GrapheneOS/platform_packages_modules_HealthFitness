/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.ui

import android.health.connect.HealthPermissions.READ_MINDFULNESS
import android.health.connect.HealthPermissions.WRITE_MINDFULNESS
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN
import android.healthconnect.testing.shared.recordfactory.RecordFactory.YESTERDAY_11AM
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.text.format.DateFormat.is24HourFormat

class MindfulnessTest : BaseDataTypeTest<MindfulnessSessionRecord>() {
    override val dataTypeString = "Mindfulness"
    override val dataCategoryString = "Wellness"
    override val permissionString = "Mindfulness"
    override val permissions = listOf(READ_MINDFULNESS, WRITE_MINDFULNESS)
    override val sameCategoryDataTypeString = null
    override val anotherCategoryString = "Activity"

    override fun createRecord() =
        MindfulnessSessionRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.toInstant(),
                YESTERDAY_11AM.plusMinutes(15).toInstant(),
                MINDFULNESS_SESSION_TYPE_MEDITATION,
            )
            .setTitle("foo-title")
            .setNotes("foo-notes")
            .build()

    override val expectedRecordHeader =
        if (is24HourFormat(context)) "11:00 - 11:15 • ${context.packageName}"
        else "11:00 AM - 11:15 AM • ${context.packageName}"
    override val expectedRecordTitle = "Meditation • foo-title"
    override val expectedRecordSubtitle = "foo-notes"

    override fun createRecordToBeDeleted() =
        MindfulnessSessionRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.plusHours(3).toInstant(),
                YESTERDAY_11AM.plusHours(4).plusMinutes(29).toInstant(),
                MINDFULNESS_SESSION_TYPE_UNKNOWN,
            )
            .build()

    override val expectedRecordToBeDeletedHeader =
        if (is24HourFormat(context)) "14:00 - 15:29 • ${context.packageName}"
        else "2:00 PM - 3:29 PM • ${context.packageName}"
    override val expectedRecordToBeDeletedTitle = "Unknown type • 1h${NBSP}29m"

    override fun createSameCategoryRecord() = null

    override fun createAnotherCategoryRecord() =
        ExerciseSessionRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.minusDays(1).minusHours(2).toInstant(),
                YESTERDAY_11AM.minusDays(1).minusHours(1).toInstant(),
                EXERCISE_SESSION_TYPE_RUNNING,
            )
            .build()

    private companion object {
        const val NBSP = "\u00A0" // no break space
    }
}
