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
import android.health.connect.HealthDataCategory
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.Record
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadSleepDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.atStartOfDay
import com.android.healthconnect.controller.utils.isAtLeastOneDayAfter
import com.android.healthconnect.controller.utils.isOnDayAfter
import com.android.healthconnect.controller.utils.isOnSameDay
import com.android.healthconnect.controller.utils.toInstantAtStartOfDay
import com.android.healthconnect.controller.utils.toLocalDate
import com.google.common.collect.Comparators.max
import com.google.common.collect.Comparators.min
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LoadMostRecentAggregationsUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val loadDataAggregationsUseCase: ILoadDataAggregationsUseCase,
    private val loadSleepDataUseCase: ILoadSleepDataUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadMostRecentAggregationsUseCase {
    /** Invoked to provide [AggregationDataCard]s info for Activity and Sleep */
    override suspend operator fun invoke(
        healthDataCategory: @HealthDataCategoryInt Int
    ): UseCaseResults<List<AggregationCardInfo>> =
        withContext(dispatcher) {
            try {
                val resultsList = mutableListOf<AggregationCardInfo>()
                if (healthDataCategory == HealthDataCategory.ACTIVITY) {
                    val stepsRecordTypes =
                        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.STEPS)
                    val datesWithStepsData = suspendCancellableCoroutine { continuation ->
                        healthConnectManager.queryActivityDates(
                            stepsRecordTypes, Runnable::run, continuation.asOutcomeReceiver())
                    }

                    if (datesWithStepsData.isNotEmpty()) {
                        val stepsCardInfo =
                            getLastAvailableAggregation(
                                datesWithStepsData, HealthPermissionType.STEPS)
                        stepsCardInfo?.let { resultsList.add(it) }
                    }

                    val distanceRecordTypes =
                        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.DISTANCE)
                    val datesWithDistanceData = suspendCancellableCoroutine { continuation ->
                        healthConnectManager.queryActivityDates(
                            distanceRecordTypes, Runnable::run, continuation.asOutcomeReceiver())
                    }

                    if (datesWithDistanceData.isNotEmpty()) {
                        val distanceCardInfo =
                            getLastAvailableAggregation(
                                datesWithDistanceData, HealthPermissionType.DISTANCE)
                        distanceCardInfo?.let { resultsList.add(it) }
                    }

                    val caloriesRecordTypes =
                        HealthPermissionToDatatypeMapper.getDataTypes(
                            HealthPermissionType.TOTAL_CALORIES_BURNED)
                    val datesWithCaloriesData = suspendCancellableCoroutine { continuation ->
                        healthConnectManager.queryActivityDates(
                            caloriesRecordTypes, Runnable::run, continuation.asOutcomeReceiver())
                    }

                    if (datesWithCaloriesData.isNotEmpty()) {
                        val caloriesCardInfo =
                            getLastAvailableAggregation(
                                datesWithCaloriesData, HealthPermissionType.TOTAL_CALORIES_BURNED)
                        caloriesCardInfo?.let { resultsList.add(it) }
                    }
                } else if (healthDataCategory == HealthDataCategory.SLEEP) {
                    val sleepRecordTypes =
                        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.SLEEP)
                    val datesWithSleepData = suspendCancellableCoroutine { continuation ->
                        healthConnectManager.queryActivityDates(
                            sleepRecordTypes, Runnable::run, continuation.asOutcomeReceiver())
                    }
                    if (datesWithSleepData.isNotEmpty()) {
                        val sleepCardInfo = getLastAvailableSleepAggregation(datesWithSleepData)
                        sleepCardInfo?.let { resultsList.add(it) }
                    }
                }

                UseCaseResults.Success(resultsList.toList())
            } catch (e: Exception) {
                UseCaseResults.Failed(e)
            }
        }

    private suspend fun getLastAvailableAggregation(
        datesWithData: List<LocalDate>,
        healthPermissionType: HealthPermissionType
    ): AggregationCardInfo? {
        // Get aggregate for last day
        val lastDate = datesWithData.maxOf { it }
        val lastDateInstant = lastDate.atStartOfDay(ZoneId.systemDefault()).toInstant()

        // call for aggregate
        val input =
            LoadAggregationInput.PeriodAggregation(
                permissionType = healthPermissionType,
                packageName = null,
                displayedStartTime = lastDateInstant,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = false)

        return when (val useCaseResult = loadDataAggregationsUseCase.invoke(input)) {
            is UseCaseResults.Success -> {
                // use this aggregation value to construct the card
                AggregationCardInfo(healthPermissionType, useCaseResult.data, lastDateInstant)
            }
            is UseCaseResults.Failed -> {
                // Something went wrong here, so return nothing
                null
            }
        }
    }

    private suspend fun getLastAvailableSleepAggregation(
        datesWithData: List<LocalDate>
    ): AggregationCardInfo? {
        // Get last date with data (the start date of sleep sessions)
        val lastDateWithData = datesWithData.last()
        val lastDateInstant = lastDateWithData.toInstantAtStartOfDay()

        // Get all sleep sessions starting on that date
        val input =
            LoadDataEntriesInput(
                HealthPermissionType.SLEEP,
                packageName = null,
                displayedStartTime = lastDateInstant,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = false)

        return when (val result = loadSleepDataUseCase.invoke(input)) {
            is UseCaseResults.Success -> {
                val sleepRecords = result.data
                val (minStartTime, maxEndTime) =
                    clusterSleepSessions(sleepRecords, lastDateWithData)
                computeSleepAggregation(minStartTime, maxEndTime)
            }
            is UseCaseResults.Failed -> {
                null
            }
        }
    }

    /**
     * Given a list of sleep session records starting on the last date with data, returns a pair of
     * Instants representing a time interval [minStartTime, maxEndTime] between which we will query
     * the aggregated time of sleep sessions.
     */
    private suspend fun clusterSleepSessions(
        entries: List<Record>,
        lastDateWithData: LocalDate
    ): Pair<Instant, Instant> {

        var minStartTime: Instant = Instant.MAX
        var maxEndTime: Instant = Instant.MIN

        // Determine if there is at least one session starting on Day 2 and finishing on Day 3
        // (Case 3)
        val sessionsCrossingMidnight =
            entries.any { record ->
                val currentSleepSession = (record as IntervalRecord)
                (currentSleepSession.endTime.isAtLeastOneDayAfter(currentSleepSession.startTime))
            }

        // Handle Case 3 - at least one sleep session starts on Day 2 and finishes on Day 3
        if (sessionsCrossingMidnight) {
            return handleSessionsCrossingMidnight(entries)
        }

        // case 1 - start and end times on the same day (Day 2)
        // case 2 - there might be sessions starting on Day 1 and finishing on Day 2
        // All sessions start and end on this day
        // now we look at the date before to see if there is a session
        // that ends today
        val secondToLastDateInstant =
            lastDateWithData.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val lastDateWithDataInstant = lastDateWithData.toInstantAtStartOfDay()

        // Get all sleep sessions starting on secondToLastDate
        val input =
            LoadDataEntriesInput(
                HealthPermissionType.SLEEP,
                packageName = null,
                displayedStartTime = secondToLastDateInstant,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = false)

        when (val result = loadSleepDataUseCase.invoke(input)) {
            is UseCaseResults.Success -> {
                val previousDaySleepData = result.data
                // For each session check if the end date is last date
                // If we find it, extend minStartTime to the start time of that session

                if (previousDaySleepData.isEmpty()) {
                    // Case 1 - All sessions start and end on this day (Day 2)
                    minStartTime = entries.minOf { (it as IntervalRecord).startTime }
                    maxEndTime = entries.maxOf { (it as IntervalRecord).endTime }
                } else {
                    // Case 2 - At least one session starts on Day 1 and finishes on Day 2 or later
                    return handleSessionsStartingOnSecondToLastDate(
                        previousDaySleepData, lastDateWithDataInstant)
                }
            }
            is UseCaseResults.Failed -> {
                Pair(Instant.MAX, Instant.MAX)
            }
        }

        return Pair(minStartTime, maxEndTime)
    }

    /** Handles sleep session case 3 - At least one session crosses midnight into Day 3. */
    private fun handleSessionsCrossingMidnight(entries: List<Record>): Pair<Instant, Instant> {
        // We show aggregation for all sessions ending on day 3
        // Find the max end time from all sessions crossing midnight
        // and the min start time from all sessions that end on day 3
        // There can be no session starting on day 3, otherwise that would be the latest date
        var minStartTime: Instant = Instant.MAX
        var maxEndTime: Instant = Instant.MIN

        entries.forEach { record ->
            val currentSleepSession = (record as IntervalRecord)
            // Start day = Day 2
            // We look at most 2 calendar days in the future, so the max possible end time
            // is Day 4 at 12:00am
            val maxPossibleEnd =
                currentSleepSession.startTime
                    .toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault())
                    .plusDays(2)
                    .toInstant()

            if (currentSleepSession.endTime.isOnSameDay(currentSleepSession.startTime)) {
                // This sleep session starts and ends on Day 2
                // So we do not count this for either min or max
                // As it belongs to the aggregations for Day 2
            } else if (currentSleepSession.endTime.isOnDayAfter(currentSleepSession.startTime)) {
                // This is a session [Day 2 - Day 3]
                // min and max candidate
                minStartTime = min(minStartTime, currentSleepSession.startTime)
                maxEndTime = max(maxEndTime, currentSleepSession.endTime)
            } else {
                // currentSleepSession.endTime is further than Day 3
                // Max End time should be Day 4 at 12am
                minStartTime = min(minStartTime, currentSleepSession.startTime)
                maxEndTime = max(maxEndTime, maxPossibleEnd)
            }
        }

        return Pair(minStartTime, maxEndTime)
    }

    /**
     * Handles sleep session Case 2 - At least one session starts on Day 1 and finishes on Day 2 or
     * later.
     */
    private fun handleSessionsStartingOnSecondToLastDate(
        previousDaySleepData: List<Record>,
        lastDateWithDataInstant: Instant
    ): Pair<Instant, Instant> {
        var minStartTime: Instant = Instant.MAX
        var maxEndTime: Instant = Instant.MIN

        previousDaySleepData.forEach { record ->
            val currentSleepSession = (record as IntervalRecord)

            // Start date is Day 1, so the max possible end date is Day 3 12am
            val maxPossibleEnd =
                currentSleepSession.startTime
                    .toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault())
                    .plusDays(2)
                    .toInstant()

            if (currentSleepSession.endTime.isOnSameDay(lastDateWithDataInstant)) {
                // This is a sleep session that starts on Day 1 and finishes on Day 2
                // min/max candidate
                minStartTime = min(minStartTime, currentSleepSession.startTime)
                maxEndTime = max(maxEndTime, currentSleepSession.endTime)
            } else if (currentSleepSession.endTime.isOnSameDay(currentSleepSession.startTime)) {
                // This is a sleep session that starts and ends on Day 1
                // We do not count it for min/max because this belongs to Day 1
                // aggregation
            } else {
                // This is a sleep session that start on Day 1 and ends after Day 2
                // Then the max end time should be Day 3 at 12am
                minStartTime = min(minStartTime, currentSleepSession.startTime)
                maxEndTime = max(maxEndTime, maxPossibleEnd)
            }
        }

        return Pair(minStartTime, maxEndTime)
    }

    /**
     * Returns an [AggregationCardInfo] representing the total sleep time from a list of sleep
     * sessions starting on a particular day.
     */
    private suspend fun computeSleepAggregation(
        minStartTime: Instant,
        maxEndTime: Instant
    ): AggregationCardInfo? {
        val aggregationInput =
            LoadAggregationInput.CustomAggregation(
                permissionType = HealthPermissionType.SLEEP,
                packageName = null,
                startTime = minStartTime,
                endTime = maxEndTime,
                showDataOrigin = false)

        return when (val useCaseResult = loadDataAggregationsUseCase.invoke(aggregationInput)) {
            is UseCaseResults.Success -> {
                // use this aggregation value to construct the card
                AggregationCardInfo(
                    HealthPermissionType.SLEEP,
                    useCaseResult.data,
                    minStartTime.atStartOfDay(),
                    maxEndTime.atStartOfDay())
            }
            is UseCaseResults.Failed -> {
                // Something went wrong here, so return nothing
                null
            }
        }
    }
}

interface ILoadMostRecentAggregationsUseCase {
    suspend fun invoke(
        healthDataCategory: @HealthDataCategoryInt Int
    ): UseCaseResults<List<AggregationCardInfo>>
}
