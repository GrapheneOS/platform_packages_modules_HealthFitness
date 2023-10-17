/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.datasources.api

import android.health.connect.HealthConnectManager
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadMostRecentAggregationsUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val loadDataAggregationsUseCase: ILoadDataAggregationsUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadMostRecentAggregationsUseCase {
    /**
     * Invoked only for the Activity category to provide [AggregationDataCard]s info
     */
    override suspend operator fun invoke(): UseCaseResults<List<AggregationCardInfo>> =
            withContext(dispatcher) {
                try {
                    val resultsList = mutableListOf<AggregationCardInfo>()
                    val stepsRecordTypes = HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.STEPS)
                    val datesWithStepsData = suspendCancellableCoroutine {  continuation ->
                        healthConnectManager.queryActivityDates(stepsRecordTypes,
                                Runnable::run, continuation.asOutcomeReceiver())
                    }

                    if (datesWithStepsData.isNotEmpty()) {
                        val stepsCardInfo = getLastAvailableAggregation(datesWithStepsData, HealthPermissionType.STEPS)
                        stepsCardInfo?.let { resultsList.add(it) }
                    }

                    val distanceRecordTypes = HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.DISTANCE)
                    val datesWithDistanceData = suspendCancellableCoroutine {  continuation ->
                        healthConnectManager.queryActivityDates(distanceRecordTypes,
                                Runnable::run, continuation.asOutcomeReceiver())
                    }

                    if (datesWithDistanceData.isNotEmpty()) {
                        val distanceCardInfo = getLastAvailableAggregation(datesWithDistanceData, HealthPermissionType.DISTANCE)
                        distanceCardInfo?.let { resultsList.add(it) }
                    }

                    val caloriesRecordTypes = HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.TOTAL_CALORIES_BURNED)
                    val datesWithCaloriesData = suspendCancellableCoroutine {  continuation ->
                        healthConnectManager.queryActivityDates(caloriesRecordTypes,
                                Runnable::run, continuation.asOutcomeReceiver())
                    }

                    if (datesWithCaloriesData.isNotEmpty()) {
                        val caloriesCardInfo = getLastAvailableAggregation(datesWithCaloriesData, HealthPermissionType.TOTAL_CALORIES_BURNED)
                        caloriesCardInfo?.let { resultsList.add(it) }
                    }

                    UseCaseResults.Success(resultsList.toList())

                } catch (e: Exception) {
                    UseCaseResults.Failed(e)
                }
            }

    private suspend fun getLastAvailableAggregation(
            datesWithData: List<LocalDate>,
            healthPermissionType: HealthPermissionType): AggregationCardInfo? {
        // Get aggregate for last day
        val lastDate = datesWithData.maxOf { it }
        val lastDateInstant = lastDate.atStartOfDay(ZoneId.systemDefault()).toInstant()

        // call for aggregate
        val input = LoadAggregationInput(
            healthPermissionType,
            packageName = null,
            displayedStartTime = lastDateInstant,
            period = DateNavigationPeriod.PERIOD_DAY,
            showDataOrigin = false)

        return when (val useCaseResult = loadDataAggregationsUseCase.invoke(input)) {
            is UseCaseResults.Success -> {
                // use this aggregation value to construct the card
                AggregationCardInfo(
                        healthPermissionType,
                        useCaseResult.data,
                        lastDateInstant)
            }
            is UseCaseResults.Failed -> {
                // Something went wrong here, so return nothing
                null
            }
        }
    }
}

interface ILoadMostRecentAggregationsUseCase {
    suspend fun invoke(): UseCaseResults<List<AggregationCardInfo>>
}