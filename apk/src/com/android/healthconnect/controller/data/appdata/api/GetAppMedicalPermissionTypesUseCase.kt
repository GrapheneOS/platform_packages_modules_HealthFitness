/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.healthconnect.controller.data.appdata.api

import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceTypeInfo
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.fromMedicalResourceType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Returns list of medical categories and permission types written by the given app to be shown on
 * the HC UI.
 */
@Singleton
class GetAppMedicalPermissionTypesUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<String, List<PermissionTypesPerCategory>>(dispatcher),
    IGetAppMedicalPermissionTypesUseCase {

    override suspend fun execute(input: String): List<PermissionTypesPerCategory> {
        val medicalResourceTypeInfos = suspendCancellableCoroutine { continuation ->
            healthConnectManager.queryAllMedicalResourceTypeInfos(
                dispatcher.asExecutor(),
                continuation.asOutcomeReceiver(),
            )
        }

        val medicalPermissionTypes = filterMedicalPermissionTypes(medicalResourceTypeInfos, input)

        return if (medicalPermissionTypes.isEmpty()) {
            emptyList()
        } else {
            listOf(PermissionTypesPerCategory(MEDICAL, medicalPermissionTypes))
        }
    }

    private fun filterMedicalPermissionTypes(
        medicalResourceTypeInfos: List<MedicalResourceTypeInfo>,
        packageName: String,
    ): List<MedicalPermissionType> =
        medicalResourceTypeInfos
            .filter { medicalResourceTypeInfo ->
                medicalResourceTypeInfo.contributingDataSources.any {
                    it.packageName == packageName
                }
            }
            .map { fromMedicalResourceType(it.medicalResourceType) }
}

interface IGetAppMedicalPermissionTypesUseCase :
    UseCaseContract<String, List<PermissionTypesPerCategory>>
