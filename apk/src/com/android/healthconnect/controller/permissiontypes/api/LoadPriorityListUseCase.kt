/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.permissiontypes.api

import android.healthconnect.GetDataOriginPriorityOrderResponse
import android.healthconnect.HealthConnectManager
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.categories.toSdkHealthDataCategory
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.AppInfoReader
import com.android.healthconnect.controller.shared.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class LoadPriorityListUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val appInfoReader: AppInfoReader,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<HealthDataCategory, List<AppMetadata>>(dispatcher) {

    /** Returns list of [AppMetadata]s for given [HealthDataCategory] in priority order. */
    override suspend fun execute(input: HealthDataCategory): List<AppMetadata> {
        val dataOriginPriorityOrderResponse: GetDataOriginPriorityOrderResponse =
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.getDataOriginsInPriorityOrder(
                    toSdkHealthDataCategory(input), Runnable::run, continuation.asOutcomeReceiver())
            }
        return dataOriginPriorityOrderResponse.dataOriginInPriorityOrder.map { dataOrigin ->
            appInfoReader.getAppMetadata(dataOrigin.packageName)
        }
    }
}
