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
package com.android.healthconnect.controller.data.rawfhir

import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceId
import android.health.connect.datatypes.FhirResource
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class RawFhirUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /** Returns the corresponding [FhirResource] for given [MedicalResourceId]. */
    suspend fun loadFhirResource(
        medicalResourceId: MedicalResourceId
    ): UseCaseResults<FhirResource> =
        withContext(dispatcher) {
            try {
                val medicalResources = suspendCancellableCoroutine { continuation ->
                    healthConnectManager.readMedicalResources(
                        listOf(medicalResourceId),
                        Runnable::run,
                        continuation.asOutcomeReceiver(),
                    )
                }
                if (medicalResources.isEmpty()) {
                    UseCaseResults.Failed(
                        IllegalStateException("No FHIR resource found for given $medicalResourceId")
                    )
                } else {
                    UseCaseResults.Success(medicalResources[0].fhirResource)
                }
            } catch (e: Exception) {
                UseCaseResults.Failed(e)
            }
        }
}
