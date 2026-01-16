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
package com.android.healthconnect.controller.matchmaking.api

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.os.OutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

/** Use case to get device data sources info. */
class GetDeviceDataSourcesInfoUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseUseCase<Unit, Set<DeviceDataSourceInfo>>(ioDispatcher) {

    override suspend fun execute(input: Unit): Set<DeviceDataSourceInfo> {
        return suspendCancellableCoroutine { continuation ->
            healthConnectManager.getDeviceDataSourceInfos(
                Executor { it.run() },
                object : OutcomeReceiver<List<DeviceDataSourceInfo>, HealthConnectException> {
                    override fun onResult(result: List<DeviceDataSourceInfo>) {
                        continuation.resume(result.toSet())
                    }

                    override fun onError(error: HealthConnectException) {
                        continuation.resumeWithException(error)
                    }
                },
            )
        }
    }
}
