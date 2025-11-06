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

import android.health.connect.DeleteUsingFiltersRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.SymptomRecord
import android.health.connect.internal.datatypes.utils.SymptomTypePermissionMapper
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteHealthPermissionTypes
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Use case to delete all fitness records from the given permission type (e.g. Steps). */
@Singleton
class DeleteFitnessPermissionTypesUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(deletePermissionTypes: DeleteHealthPermissionTypes) {
        val deleteRequest = DeleteUsingFiltersRequest.Builder()
        var deleteAllSymptoms = false

        deletePermissionTypes.healthPermissionTypes.forEach { permissionType ->
            when (permissionType) {
                is FitnessPermissionType -> {
                    if (SymptomTypePermissionMapper.isSymptomCategory(permissionType.category)) {
                        deleteAllSymptoms = true
                    } else {
                        HealthPermissionToDatatypeMapper.getDataTypes(permissionType).forEach {
                            recordType ->
                            deleteRequest.addRecordType(recordType)
                        }
                    }
                }
                is MedicalPermissionType -> {
                    // Medical types are not handled by this use case
                }
            }
        }

        if (deleteAllSymptoms) {
            deleteRequest.addRecordType(SymptomRecord::class.java)
        }

        withContext(dispatcher) {
            healthConnectManager.deleteRecords(deleteRequest.build(), Runnable::run) {}
        }
    }
}
