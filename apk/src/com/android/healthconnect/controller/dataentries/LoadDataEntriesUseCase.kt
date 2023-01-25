/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries

import android.healthconnect.HealthConnectManager
import android.healthconnect.ReadRecordsRequestUsingFilters
import android.healthconnect.ReadRecordsResponse
import android.healthconnect.TimeRangeFilter
import android.healthconnect.datatypes.Record
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.dataentries.formatters.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper.getDataTypes
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import java.time.Duration.ofHours
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class LoadDataEntriesUseCase
@Inject
constructor(
    private val healthDataEntryFormatter: HealthDataEntryFormatter,
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<LoadDataEntriesInput, List<FormattedEntry>>(dispatcher) {

    override suspend fun execute(input: LoadDataEntriesInput): List<FormattedEntry> {
        val timeFilterRange = getTimeFilter(input.selectedDate)
        val dataTypes = getDataTypes(input.permissionType)
        return dataTypes.map { dataType -> readDataType(dataType, timeFilterRange) }.flatten()
    }

    private fun getTimeFilter(selectedDate: Instant): TimeRangeFilter {
        val start = selectedDate.truncatedTo(ChronoUnit.DAYS)
        val end = start.plus(ofHours(23)).plus(ofMinutes(59))
        return TimeRangeFilter.Builder(start, end).build()
    }

    private suspend fun readDataType(
        data: Class<out Record>,
        timeFilterRange: TimeRangeFilter
    ): List<FormattedEntry> {
        val filter =
            ReadRecordsRequestUsingFilters.Builder(data).setTimeRangeFilter(timeFilterRange).build()
        val records =
            suspendCancellableCoroutine<ReadRecordsResponse<*>> { continuation ->
                    healthConnectManager.readRecords(
                        filter, Runnable::run, continuation.asOutcomeReceiver())
                }
                .records
        return records
            .map { record -> healthDataEntryFormatter.format(record) }
            .sortedBy { it.startTime }
    }
}

data class LoadDataEntriesInput(
    val permissionType: HealthPermissionType,
    val selectedDate: Instant
)
