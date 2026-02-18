/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.datasources.api

import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.UpdateDataOriginPriorityOrderRequest
import android.health.connect.datatypes.DataOrigin
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class UpdatePriorityListUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<UpdatePriorityListInput, Unit>(dispatcher), IUpdatePriorityListUseCase {

    /** Updates the priority list of the stored [DataOrigin]s for given [HealthDataCategory]. */
    override suspend fun execute(input: UpdatePriorityListInput): Unit {
        val dataOrigins: List<DataOrigin> =
            input.priorityList
                .stream()
                .map { packageName -> DataOrigin.Builder().setPackageName(packageName).build() }
                .toList()

        suspendCancellableCoroutine { continuation ->
            healthConnectManager.updateDataOriginPriorityOrder(
                UpdateDataOriginPriorityOrderRequest(dataOrigins, input.category),
                dispatcher.asExecutor(),
                continuation.asOutcomeReceiver(),
            )
        }
    }
}

data class UpdatePriorityListInput(
    val priorityList: List<String>,
    val category: @HealthDataCategoryInt Int,
)

interface IUpdatePriorityListUseCase : UseCaseContract<UpdatePriorityListInput, Unit>
