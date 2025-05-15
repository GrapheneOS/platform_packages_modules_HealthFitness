/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.health.connect.DeleteMedicalResourcesRequest
import android.health.connect.HealthConnectManager
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.toMedicalResourceType
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Use case to delete all medical resources the given medical permission type (e.g. Immunization)
 * written by a given app.
 */
@Singleton
class DeleteMedicalPermissionTypesFromAppUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val medicalDataSourceReader: MedicalDataSourceReader,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(packageName: String, dataTypes: Set<MedicalPermissionType>) {
        val deleteRequest = DeleteMedicalResourcesRequest.Builder()

        dataTypes.map { permissionType ->
            deleteRequest.addMedicalResourceType(toMedicalResourceType(permissionType))
        }

        val medicalDataSources = medicalDataSourceReader.fromPackageName(packageName)
        medicalDataSources.forEach { deleteRequest.addDataSourceId(it.id) }

        withContext(dispatcher) {
            healthConnectManager.deleteMedicalResources(deleteRequest.build(), Runnable::run) {}
        }
    }
}
