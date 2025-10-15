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
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.RecordIdFilter
import android.health.connect.datatypes.SymptomRecord
import android.os.OutcomeReceiver
import com.android.healthconnect.controller.data.entries.api.SymptomTypeMapper
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteHealthPermissionTypes
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
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
        withContext(dispatcher) {
            val fitnessPermissionTypes =
                deletePermissionTypes.healthPermissionTypes.filterIsInstance<
                    FitnessPermissionType
                >()

            val symptomPermissionTypes =
                fitnessPermissionTypes.filter { it.name.startsWith("SYMPTOM_") }
            val otherFitnessPermissionTypes =
                fitnessPermissionTypes.filterNot { it.name.startsWith("SYMPTOM_") }

            coroutineScope {
                val otherFitnessJob = async {
                    if (otherFitnessPermissionTypes.isNotEmpty()) {
                        val deleteRequest = DeleteUsingFiltersRequest.Builder()
                        otherFitnessPermissionTypes.map { permissionType ->
                            HealthPermissionToDatatypeMapper.getDataTypes(permissionType).map {
                                recordType ->
                                deleteRequest.addRecordType(recordType)
                            }
                        }
                        suspendCancellableCoroutine<Unit> { continuation ->
                            healthConnectManager.deleteRecords(
                                deleteRequest.build(),
                                dispatcher.asExecutor(),
                                object : OutcomeReceiver<Void, HealthConnectException> {
                                    override fun onResult(result: Void?) {
                                        continuation.resume(Unit)
                                    }

                                    override fun onError(error: HealthConnectException) {
                                        continuation.resumeWithException(error)
                                    }
                                },
                            )
                        }
                    }
                }

                val symptomsJob = async {
                    if (symptomPermissionTypes.isNotEmpty()) {
                        val symptomTypesToDelete =
                            symptomPermissionTypes.map {
                                SymptomTypeMapper.getSymptomType(it.category)
                            }

                        val readRequest =
                            ReadRecordsRequestUsingFilters.Builder(SymptomRecord::class.java)
                                .build()
                        val allSymptomRecords =
                            (suspendCancellableCoroutine<ReadRecordsResponse<SymptomRecord>> {
                                        continuation ->
                                        healthConnectManager.readRecords(
                                            readRequest,
                                            dispatcher.asExecutor(),
                                            object :
                                                OutcomeReceiver<
                                                    ReadRecordsResponse<SymptomRecord>,
                                                    HealthConnectException,
                                                > {
                                                override fun onResult(
                                                    result: ReadRecordsResponse<SymptomRecord>?
                                                ) {
                                                    continuation.resume(result!!)
                                                }

                                                override fun onError(
                                                    error: HealthConnectException
                                                ) {
                                                    continuation.resumeWithException(error)
                                                }
                                            },
                                        )
                                    }
                                    .records)
                                .filterIsInstance<SymptomRecord>()

                        val recordsToDelete =
                            allSymptomRecords.filter { it.symptomType in symptomTypesToDelete }

                        if (recordsToDelete.isNotEmpty()) {
                            val idFilters =
                                recordsToDelete.map {
                                    RecordIdFilter.fromId(it::class.java, it.metadata.id)
                                }
                            suspendCancellableCoroutine<Unit> { continuation ->
                                healthConnectManager.deleteRecords(
                                    idFilters,
                                    dispatcher.asExecutor(),
                                    object : OutcomeReceiver<Void, HealthConnectException> {
                                        override fun onResult(result: Void?) {
                                            continuation.resume(Unit)
                                        }

                                        override fun onError(error: HealthConnectException) {
                                            continuation.resumeWithException(error)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                otherFitnessJob.await()
                symptomsJob.await()
            }
        }
    }
}
