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
package com.android.healthconnect.testapps.toolbox.read.dataentries

import android.health.connect.datatypes.ExerciseRoute

sealed class FormattedEntry {

    data class FormattedDataEntry(val header: String, val value: String) : FormattedEntry()

    data class FormattedSample(val header: String, val value: String) : FormattedEntry()

    data class FormattedAggregation(val aggregation: String, val contributingApps: String) :
        FormattedEntry()

    data class FormattedNutritionEntry(
        val header: String,
        val value: String,
        val mealType: String?,
        val samples: String,
    ) : FormattedEntry()

    data class FormattedDataDetails(
        val dataEntry: FormattedDataEntry,
        val dataDetails: List<FormattedEntry>,
    ) : FormattedEntry()

    data class FormattedDataEntryMirrored(val header: String, val value: String) : FormattedEntry()

    data class Header(val value: String) : FormattedEntry()

    data class FormattedExerciseSession(
        val header: String,
        val title: String?,
        val type: String,
        val note: String?,
        val route: ExerciseRoute?,
        val sessionDetails: List<FormattedEntry>,
    ) : FormattedEntry()

    data class FormattedPlannedExerciseSession(
        val header: String,
        val title: String?,
        val type: String,
        val note: String?,
        val duration: String,
        val exerciseBlocks: List<FormattedPlannedExerciseBlock>,
    ) : FormattedEntry()

    data class FormattedPlannedExerciseBlock(
        val description: String?,
        val repetitions: String,
        val steps: List<FormattedPlannedExerciseStep>,
    ) : FormattedEntry()

    data class FormattedPlannedExerciseStep(
        val description: String?,
        val category: String,
        val type: String,
        val completionGoals: String,
        val performanceGoals: String?,
    ) : FormattedEntry()
}
