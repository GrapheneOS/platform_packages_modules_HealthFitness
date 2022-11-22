/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
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
import java.time.Duration.ofDays
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LoadDataEntriesUseCase
@Inject
constructor(
    private val healthDataEntryFormatter: HealthDataEntryFormatter,
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        permissionType: HealthPermissionType,
        selectedDate: Instant
    ): List<FormattedDataEntry> =
        withContext(dispatcher) {
            val timeFilterRange = getTimeFilter(selectedDate)
            val dataTypes = getDataTypes(permissionType)
            dataTypes.map { dataType -> readDataType(dataType, timeFilterRange) }.flatten()
        }
    private fun getTimeFilter(selectedDate: Instant): TimeRangeFilter {
        val start = selectedDate.truncatedTo(ChronoUnit.DAYS)
        val end = start.plus(ofDays(1))
        return TimeRangeFilter.Builder(start, end).build()
    }
    private suspend fun readDataType(
        data: Class<out Record>,
        timeFilterRange: TimeRangeFilter
    ): List<FormattedDataEntry> {
        val filter =
            ReadRecordsRequestUsingFilters.Builder(data).setTimeRangeFilter(timeFilterRange).build()
        val response =
            suspendCancellableCoroutine<ReadRecordsResponse<*>> { continuation ->
                healthConnectManager.readRecords(
                    filter, Runnable::run, continuation.asOutcomeReceiver())
            }
        return response.records.map { record -> healthDataEntryFormatter.format(record) }
    }
}
