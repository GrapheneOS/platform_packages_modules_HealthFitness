/*
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
package com.android.healthconnect.controller.data.entries.api

import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper.getDataTypes
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class LoadDataEntriesUseCase
@Inject
constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val loadEntriesSharedUseCase: LoadEntriesSharedUseCase
) : BaseUseCase<LoadDataEntriesInput, List<FormattedEntry>>(dispatcher) {

    companion object {
        private const val TAG = "LoadDataEntriesUseCase"
    }

    override suspend fun execute(input: LoadDataEntriesInput): List<FormattedEntry> {
        val timeFilterRange =
            loadEntriesSharedUseCase.getTimeFilter(
                input.displayedStartTime, input.period, endTimeExclusive = true)
        val dataTypes = getDataTypes(input.permissionType)

        val entryRecords =
            dataTypes
                .map { dataType ->
                    loadEntriesSharedUseCase.readDataType(
                        dataType, timeFilterRange, input.packageName)
                }
                .flatten()

        return loadEntriesSharedUseCase.maybeAddDateSectionHeaders(
            entryRecords, input.period, input.showDataOrigin)
    }
}

data class LoadDataEntriesInput(
    val permissionType: HealthPermissionType,
    val packageName: String?,
    val displayedStartTime: Instant,
    val period: DateNavigationPeriod,
    val showDataOrigin: Boolean
)
