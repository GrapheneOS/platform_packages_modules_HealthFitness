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

package com.android.healthconnect.controller.data.alldata.api

import android.health.connect.HealthConnectManager
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
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
 * UseCase to retrieve a list of [PermissionTypesPerCategory] of all
 * [com.android.healthconnect.controller.permissions.data.MedicalPermissionType]s with stored data.
 */
@Singleton
class GetMedicalPermissionTypesWithDataUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<Unit, List<PermissionTypesPerCategory>>(dispatcher),
    IGetMedicalPermissionTypesWithDataUseCase {

    override suspend fun execute(input: Unit): List<PermissionTypesPerCategory> {
        val medicalResourceTypeInfos = suspendCancellableCoroutine { continuation ->
            healthConnectManager.queryAllMedicalResourceTypeInfos(
                dispatcher.asExecutor(),
                continuation.asOutcomeReceiver(),
            )
        }
        val medicalPermissionTypes =
            medicalResourceTypeInfos
                .filter { it.contributingDataSources.isNotEmpty() }
                .map { fromMedicalResourceType(it.medicalResourceType) }

        return if (medicalPermissionTypes.isEmpty()) emptyList()
        else listOf(PermissionTypesPerCategory(MEDICAL, medicalPermissionTypes))
    }
}

interface IGetMedicalPermissionTypesWithDataUseCase :
    UseCaseContract<Unit, List<PermissionTypesPerCategory>>
