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
package com.android.healthconnect.controller.data.entries

import android.health.connect.MedicalResourceId
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExerciseRoute
import android.health.connect.datatypes.PlannedExerciseBlock
import android.health.connect.datatypes.PlannedExerciseStep
import com.android.healthconnect.controller.shared.DataType
import java.time.Instant

sealed class FormattedEntry(open val uuid: String) {
    interface HasDataType {
        val dataType: DataType
    }

    data class FormattedDataEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        override val dataType: DataType,
        val startTime: Instant? = null,
        val endTime: Instant? = null,
    ) : FormattedEntry(uuid), HasDataType

    data class FormattedMedicalDataEntry(
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        val time: Instant? = null,
        val medicalResourceId: MedicalResourceId,
    ) : FormattedEntry(uuid = "")

    data class SleepSessionEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        override val dataType: DataType,
        val notes: String?,
    ) : FormattedEntry(uuid), HasDataType

    data class ExerciseSessionEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        override val dataType: DataType,
        val notes: String?,
        val route: ExerciseRoute? = null,
        val isClickable: Boolean = true,
    ) : FormattedEntry(uuid), HasDataType

    data class SeriesDataEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        override val dataType: DataType,
    ) : FormattedEntry(uuid), HasDataType

    data class SessionHeader(val header: String) : FormattedEntry(uuid = "")

    data class FormattedSectionTitle(val title: String) : FormattedEntry(uuid = "")

    data class FormattedSectionContent(val title: String, val bulleted: Boolean = false) :
        FormattedEntry(uuid = "")

    data class ItemDataEntrySeparator(val title: String = "") : FormattedEntry(uuid = "")

    data class SelectAllHeader(val title: String = "Select all") : FormattedEntry(uuid = "")

    data class ReverseSessionDetail(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid)

    data class FormattedSessionDetail(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid)

    data class FormattedAggregation(
        val aggregation: String,
        val aggregationA11y: String,
        val contributingApps: String,
    ) : FormattedEntry(aggregation)

    data class EntryDateSectionHeader(val date: String) : FormattedEntry(date)

    data class PlannedExerciseSessionEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        override val dataType: DataType,
        val titleA11y: String,
        val notes: String?,
    ) : FormattedEntry(uuid), HasDataType

    data class PlannedExerciseBlockEntry(
        val block: PlannedExerciseBlock,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid = "")

    data class PlannedExerciseStepEntry(
        val step: PlannedExerciseStep,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid = "")

    data class ExercisePerformanceGoalEntry(
        val goal: ExercisePerformanceGoal,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid = "")
}
