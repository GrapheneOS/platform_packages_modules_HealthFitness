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
package com.android.healthconnect.testapps.toolbox.read.controller

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import com.android.healthconnect.testapps.toolbox.Constants.HealthPermissionType
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedAggregation
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.Unit
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils

class LoadAggregation : ILoadAggregation {

    override suspend fun invoke(
        input: LoadEntriesInput,
        healthConnectManager: HealthConnectManager,
        context: Context,
    ): FormattedAggregation {

        val timeFilter =
            TimeInstantRangeFilter.Builder()
                .setStartTime(input.startTime)
                .setEndTime(input.endTime)
                .build()

        val dataType = input.dataType
        when (dataType) {
            HealthPermissionType.STEPS -> {
                val totalSteps =
                    GeneralUtils.aggregate(
                            manager = healthConnectManager,
                            metrics = setOf(StepsRecord.STEPS_COUNT_TOTAL),
                            timeRangeFilter = timeFilter,
                        )
                        .get(StepsRecord.STEPS_COUNT_TOTAL)
                return FormattedAggregation(
                    aggregation = UnitFormatter.formatSteps(totalSteps!!, context),
                    contributingApps = "",
                )
            }
            HealthPermissionType.DISTANCE -> {
                val totalDistance =
                    GeneralUtils.aggregate(
                            manager = healthConnectManager,
                            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                            timeRangeFilter = timeFilter,
                        )
                        .get(DistanceRecord.DISTANCE_TOTAL)
                return FormattedAggregation(
                    aggregation =
                        UnitFormatter.formatLength(
                            totalDistance!!,
                            Unit.Length.KILOMETERS,
                            context,
                        ),
                    contributingApps = "",
                )
            }
            HealthPermissionType.TOTAL_CALORIES_BURNED -> {
                val totalCaloriesBurned =
                    GeneralUtils.aggregate(
                            manager = healthConnectManager,
                            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                            timeRangeFilter = timeFilter,
                        )
                        .get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                return FormattedAggregation(
                    aggregation = UnitFormatter.formatEnergy(totalCaloriesBurned!!, context),
                    contributingApps = "",
                )
            }
            else -> {
                throw IllegalArgumentException("Aggregation not supported: $dataType")
            }
        }
    }
}

interface ILoadAggregation {
    suspend fun invoke(
        input: LoadEntriesInput,
        healthConnectManager: HealthConnectManager,
        context: Context,
    ): FormattedAggregation
}
