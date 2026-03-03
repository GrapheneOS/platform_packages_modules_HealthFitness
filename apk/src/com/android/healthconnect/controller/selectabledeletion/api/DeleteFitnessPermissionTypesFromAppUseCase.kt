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
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Use case to delete all fitness records from the given permission type (e.g. Steps) written by a
 * given app.
 */
@Singleton
class DeleteFitnessPermissionTypesFromAppUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val getCurrentDeviceIdUseCase: IGetCurrentDeviceIdUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(packageName: String, permissions: Set<FitnessPermissionType>) {

        val deleteRequest = DeleteUsingFiltersRequest.Builder()

        permissions.map { permission ->
            HealthPermissionToDatatypeMapper.getDataTypes(permission).map { recordType ->
                deleteRequest.addRecordType(recordType)
            }
        }

        deleteRequest.addDataOrigin(DataOrigin.Builder().setPackageName(packageName).build())

        if (deviceDataProvidersApi() && getCurrentDeviceIdUseCase.isCurrentDevice(packageName)) {
            deleteRequest.addDataOrigin(
                DataOrigin.Builder().setPackageName(DEVICE_DATA_PROVIDER_PACKAGE).build()
            )
        }
        if (deviceDataProvidersApi() && packageName == DEVICE_DATA_PROVIDER_PACKAGE) {
            getCurrentDeviceIdUseCase.getOrNull()?.let {
                deleteRequest.addDataOrigin(DataOrigin.Builder().setPackageName(it).build())
            }
        }

        withContext(dispatcher) {
            healthConnectManager.deleteRecords(deleteRequest.build(), Runnable::run) {}
        }
    }
}
