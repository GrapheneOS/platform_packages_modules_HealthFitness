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

import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Use case to delete all fitness and medical resources from the given permission types written by a
 * given app.
 */
@Singleton
class DeletePermissionTypesFromAppUseCase
@Inject
constructor(
    private val deleteFitnessPermissionTypesFromAppUseCase:
        DeleteFitnessPermissionTypesFromAppUseCase,
    private val deleteMedicalPermissionTypesFromAppUseCase:
        DeleteMedicalPermissionTypesFromAppUseCase,
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        packageName: String,
        permissions: Set<HealthPermissionType>,
        removePermissions: Boolean = false,
    ) =
        withContext(dispatcher) {
            val fitnessPermissions = permissions.filterIsInstance<FitnessPermissionType>().toSet()
            val medicalPermissions = permissions.filterIsInstance<MedicalPermissionType>().toSet()

            val deleteFitness = async { deleteFitnessData(packageName, fitnessPermissions) }
            val deleteMedical = async { deleteMedicalData(packageName, medicalPermissions) }

            if (removePermissions) {
                val revokeAccess = async { revokeAllHealthPermissionsUseCase.invoke(packageName) }
                revokeAccess.await()
            }
            deleteFitness.await()
            deleteMedical.await()
        }

    private suspend fun deleteFitnessData(
        packageName: String,
        permissions: Set<FitnessPermissionType>,
    ) {
        if (permissions.isEmpty()) {
            return
        }
        deleteFitnessPermissionTypesFromAppUseCase.invoke(packageName, permissions)
    }

    private suspend fun deleteMedicalData(
        packageName: String,
        permissions: Set<MedicalPermissionType>,
    ) {
        if (permissions.isEmpty()) {
            return
        }
        deleteMedicalPermissionTypesFromAppUseCase.invoke(packageName, permissions)
    }
}
