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
import android.health.connect.datatypes.DataOrigin
import com.android.healthconnect.controller.devices.api.IGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** Use case to delete all records written by a given app. */
@Singleton
class DeleteAppDataUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val medicalDataSourceReader: MedicalDataSourceReader,
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase,
    private val getCurrentDeviceIdUseCase: IGetCurrentDeviceIdUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun invoke(
        deleteAppData: DeletionType.DeleteAppData,
        removePermissions: Boolean = false,
    ) {
        val packageName = deleteAppData.packageName

        withContext(dispatcher) {
            val deleteFitnessData = async {
                val deleteFitnessRequest = deleteUsingFilterRequest(packageName)
                healthConnectManager.deleteRecords(deleteFitnessRequest, Runnable::run) {}
            }
            val deleteMedicalData = async {
                val medicalDataSources = medicalDataSourceReader.fromPackageName(packageName)
                medicalDataSources.forEach {
                    healthConnectManager.deleteMedicalDataSourceWithData(it.id, Runnable::run) {}
                }
            }
            deleteFitnessData.await()
            deleteMedicalData.await()

            if (removePermissions) {
                revokeAllHealthPermissionsUseCase.invoke(deleteAppData.packageName)
            }
        }
    }

    private suspend fun deleteUsingFilterRequest(packageName: String): DeleteUsingFiltersRequest {
        val requestBuilder =
            DeleteUsingFiltersRequest.Builder()
                .addDataOrigin(DataOrigin.Builder().setPackageName(packageName).build())

        if (deviceDataProvidersApi() && getCurrentDeviceIdUseCase.isCurrentDevice(packageName)) {
            requestBuilder.addDataOrigin(
                DataOrigin.Builder().setPackageName(DEVICE_DATA_PROVIDER_PACKAGE).build()
            )
        }

        return requestBuilder.build()
    }
}
