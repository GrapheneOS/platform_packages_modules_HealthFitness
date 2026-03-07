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

package com.android.healthconnect.controller.devices.api

import android.health.connect.HealthConnectManager
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class GetCurrentDeviceIdUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<Unit, String>(dispatcher), IGetCurrentDeviceIdUseCase {

    // The id of the current device will be stable during its runtime. Instead of repeatedly
    // calling the manager, calling once and caching suffices.
    private var cachedDeviceId: String? = null

    override suspend fun execute(input: Unit): String {
        cachedDeviceId?.let {
            return it
        }
        val deviceId = healthConnectManager.currentDeviceId
        cachedDeviceId = deviceId
        return deviceId
    }

    override suspend fun isCurrentDevice(packageName: String?): Boolean {
        return when (val result = this.invoke(Unit)) {
            is UseCaseResults.Success -> result.data == packageName
            is UseCaseResults.Failed -> false
        }
    }

    override suspend fun getOrNull(): String? {
        return when (val result = this(Unit)) {
            is UseCaseResults.Success -> result.data
            is UseCaseResults.Failed -> null
        }
    }
}

interface IGetCurrentDeviceIdUseCase : UseCaseContract<Unit, String> {
    suspend fun isCurrentDevice(packageName: String?): Boolean

    suspend fun getOrNull(): String?
}
