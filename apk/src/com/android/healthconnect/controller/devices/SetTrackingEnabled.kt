/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.devices

import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.Record
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class SetTrackingEnabled
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ISetTrackingEnabled, BaseUseCase<SetTrackingEnabled.Input, Unit>(dispatcher) {

    override suspend fun execute(input: Input) {
        suspendCancellableCoroutine { continuation: CancellableContinuation<Void> ->
            healthConnectManager.setTrackingEnabled(
                input.recordType,
                input.isEnabled,
                Runnable::run,
                continuation.asOutcomeReceiver(),
            )
        }
    }

    data class Input(val recordType: Class<out Record>, val isEnabled: Boolean)
}

interface ISetTrackingEnabled {
    suspend fun invoke(input: SetTrackingEnabled.Input): UseCaseResults<Unit>

    suspend fun execute(input: SetTrackingEnabled.Input)
}
