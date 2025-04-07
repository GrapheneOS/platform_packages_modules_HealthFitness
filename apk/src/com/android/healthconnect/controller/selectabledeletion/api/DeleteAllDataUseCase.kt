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

import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthfitness.flags.Flags.personalHealthRecord
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** Use case to delete all records stored in Health Connect without any filter. */
@Singleton
class DeleteAllDataUseCase
@Inject
constructor(
    private val deleteAllFitnessDataUseCase: DeleteAllFitnessDataUseCase,
    private val deleteAllMedicalDataUseCase: DeleteAllMedicalDataUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun invoke() =
        withContext(dispatcher) {
            val deleteFitnessData = async { deleteAllFitnessDataUseCase.invoke() }
            val deleteMedicalData = async { deleteAllMedicalDataUseCase.invoke() }
            deleteFitnessData.await()
            deleteMedicalData.await()
        }
}
