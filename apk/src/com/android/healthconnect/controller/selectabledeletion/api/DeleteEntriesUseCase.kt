/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.selectabledeletion.api

import android.health.connect.HealthConnectManager
import android.health.connect.RecordIdFilter
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteEntries
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Use case to delete a list of records by record ID. */
@Singleton
class DeleteEntriesUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    // TODO (b/369108829) Optimise deletion based on selected data and date ranges
    suspend fun invoke(deleteEntries: DeleteEntries) =
        withContext(dispatcher) {
            val recordIdFilters =
                deleteEntries.idsToDataTypes.entries.map { (id, dataType) ->
                    RecordIdFilter.fromId(dataType.java, id)
                }

            healthConnectManager.deleteRecords(recordIdFilters, Runnable::run) {}
        }
}
