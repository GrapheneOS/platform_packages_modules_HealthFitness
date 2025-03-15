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
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.GROUP_ITEM
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.HEADER_ITEM
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.SPACE
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.STANDALONE_ITEM
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.UNKNOWN
import java.time.Instant

sealed class FormattedEntry(
    open val uuid: String,
    val displayType: RecyclerViewItemDisplayType = UNKNOWN,
) {
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
    ) : FormattedEntry(uuid, GROUP_ITEM), HasDataType

    data class FormattedMedicalDataEntry(
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        val time: Instant? = null,
        val medicalResourceId: MedicalResourceId,
    ) : FormattedEntry(uuid = "", GROUP_ITEM)

    data class SleepSessionEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        override val dataType: DataType,
        val notes: String?,
    ) : FormattedEntry(uuid, GROUP_ITEM), HasDataType

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
    ) : FormattedEntry(uuid, GROUP_ITEM), HasDataType

    data class SeriesDataEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        override val dataType: DataType,
    ) : FormattedEntry(uuid, GROUP_ITEM), HasDataType

    data class SessionHeader(val header: String) : FormattedEntry(uuid = "", HEADER_ITEM)

    data class FormattedSectionTitle(val title: String) : FormattedEntry(uuid = "", HEADER_ITEM)

    data class FormattedSectionContent(val title: String, val bulleted: Boolean = false) :
        FormattedEntry(uuid = "", GROUP_ITEM)

    data class ItemDataEntrySeparator(val title: String = "") : FormattedEntry(uuid = "", SPACE)

    data class SelectAllHeader(val title: String = "Select all") :
        FormattedEntry(uuid = "", STANDALONE_ITEM)

    data class ReverseSessionDetail(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid, GROUP_ITEM)

    data class FormattedSessionDetail(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid, GROUP_ITEM)

    data class FormattedAggregation(
        val aggregation: String,
        val aggregationA11y: String,
        val contributingApps: String,
    ) : FormattedEntry(aggregation, STANDALONE_ITEM)

    data class EntryDateSectionHeader(val date: String) : FormattedEntry(date, HEADER_ITEM)

    data class PlannedExerciseSessionEntry(
        override val uuid: String,
        val header: String,
        val headerA11y: String,
        val title: String,
        override val dataType: DataType,
        val titleA11y: String,
        val notes: String?,
    ) : FormattedEntry(uuid, GROUP_ITEM), HasDataType

    data class PlannedExerciseBlockEntry(
        val block: PlannedExerciseBlock,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid = "", UNKNOWN)

    data class PlannedExerciseStepEntry(
        val step: PlannedExerciseStep,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid = "", UNKNOWN)

    data class ExercisePerformanceGoalEntry(
        val goal: ExercisePerformanceGoal,
        val title: String,
        val titleA11y: String,
    ) : FormattedEntry(uuid = "", UNKNOWN)
}
