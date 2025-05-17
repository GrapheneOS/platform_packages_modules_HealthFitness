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

import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteHealthPermissionTypes
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** Use case to delete all fitness and medical resources from the given permission types. */
@Singleton
class DeletePermissionTypesUseCase
@Inject
constructor(
    private val deleteFitnessPermissionTypesUseCase: DeleteFitnessPermissionTypesUseCase,
    private val deleteMedicalPermissionTypesUseCase: DeleteMedicalPermissionTypesUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(deletePermissionTypes: DeleteHealthPermissionTypes) {
        withContext(dispatcher) {
            val deleteFitness = async { maybeDeleteFitnessData(deletePermissionTypes) }
            val deleteMedical = async { maybeDeleteMedicalData(deletePermissionTypes) }
            deleteFitness.await()
            deleteMedical.await()
        }
    }

    private suspend fun maybeDeleteFitnessData(deletionRequest: DeleteHealthPermissionTypes) {
        val isFitnessDataEmpty =
            deletionRequest.healthPermissionTypes
                .filterIsInstance<FitnessPermissionType>()
                .isEmpty()
        if (isFitnessDataEmpty) {
            return
        }
        deleteFitnessPermissionTypesUseCase.invoke(deletionRequest)
    }

    private suspend fun maybeDeleteMedicalData(deletionRequest: DeleteHealthPermissionTypes) {
        val isMedicalDataEmpty =
            deletionRequest.healthPermissionTypes
                .filterIsInstance<MedicalPermissionType>()
                .isEmpty()
        if (isMedicalDataEmpty) {
            return
        }
        deleteMedicalPermissionTypesUseCase.invoke(deletionRequest)
    }
}
