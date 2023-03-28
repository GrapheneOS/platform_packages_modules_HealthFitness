/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.migration.api

import android.health.connect.HealthConnectDataState
import android.health.connect.HealthConnectManager
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.migration.DataMigrationState
import com.android.healthconnect.controller.service.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LoadMigrationStateUseCase
@Inject
constructor(
    private val manager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    @DataMigrationState
    suspend operator fun invoke(): Int {
        return withContext(dispatcher) {
            val state =
                suspendCancellableCoroutine<HealthConnectDataState> { continuation ->
                    manager.getHealthConnectDataState(
                        Runnable::run, continuation.asOutcomeReceiver())
                }

            // TODO (b/273745755) Expose real UI states
            // state.dataMigrationState
            HealthConnectDataState.MIGRATION_STATE_IDLE
        }
    }
}
