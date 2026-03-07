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
import android.health.connect.HealthDataCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.data.shared.getPermissionTypesPerCategory
import com.android.healthconnect.controller.devices.api.IGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.shared.FITNESS_DATA_CATEGORIES
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import com.android.healthfitness.flags.Flags
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * UseCase to retrieve a list of [PermissionTypesPerCategory] of all
 * [com.android.healthconnect.controller.permissions.data.FitnessPermissionType]s with stored data.
 */
@Singleton
class GetFitnessPermissionTypesWithDataUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val getCurrentDeviceIdUseCase: IGetCurrentDeviceIdUseCase,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<Unit, List<PermissionTypesPerCategory>>(dispatcher),
    IGetFitnessPermissionTypesWithDataUseCase {

    override suspend fun execute(input: Unit): List<PermissionTypesPerCategory> {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.queryAllRecordTypesInfo(
                    dispatcher.asExecutor(),
                    continuation.asOutcomeReceiver(),
                )
            }

        val currentDeviceId =
            if (deviceDataProvidersApi()) getCurrentDeviceIdUseCase.getOrNull() else null

        val categories =
            FITNESS_DATA_CATEGORIES.map {
                    getPermissionTypesPerCategory(
                        it,
                        recordTypeInfoMap,
                        packageName = null,
                        currentDeviceId,
                    )
                }
                .filter { it.data.isNotEmpty() }
                .filter { it.category != HealthDataCategory.SYMPTOMS || Flags.symptoms() }

        return categories
    }
}

interface IGetFitnessPermissionTypesWithDataUseCase :
    UseCaseContract<Unit, List<PermissionTypesPerCategory>>
