/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.connectedapps

import android.healthconnect.AccessLog
import android.healthconnect.HealthConnectManager
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.service.IoDispatcher
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Query recent access logs for health connect connected apps. */
@Singleton
class QueryRecentAccessLogsUseCase
@Inject
constructor(
    private val manager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend fun invoke(): Map<String, Instant> =
        withContext(dispatcher) {
            val accessLogs =
                suspendCancellableCoroutine<List<AccessLog>> { continuation ->
                    manager.queryAccessLogs(Runnable::run, continuation.asOutcomeReceiver())
                }
            accessLogs.associate { it.packageName to it.accessTime }
        }
}
