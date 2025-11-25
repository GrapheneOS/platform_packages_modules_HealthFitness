/**
 * Copyright (C) 2025 The Android Open Source Project
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

import android.health.connect.DeleteUsingFiltersRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Use case to delete all symptom records from a specific inactive app. */
@Singleton
class DeleteAllSymptomsDataFromInactiveAppUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<DeletionType.DeleteAllSymptomsDataFromInactiveApp, Unit>(dispatcher) {

    override suspend fun execute(input: DeletionType.DeleteAllSymptomsDataFromInactiveApp) {
        val deleteRequest =
            DeleteUsingFiltersRequest.Builder()
                .addRecordType(SymptomRecord::class.java)
                .addDataOrigin(DataOrigin.Builder().setPackageName(input.packageName).build())
                .build()

        withContext(dispatcher) {
            healthConnectManager.deleteRecords(deleteRequest, Runnable::run) {}
        }
    }
}
