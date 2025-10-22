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

package com.android.healthconnect.controller.matchmaking.api

import android.health.connect.HealthConnectManager
import android.health.connect.MatchmakingRequest
import android.health.connect.datatypes.Record
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.matchmaking.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase.GetMatchMakingAppsInput
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

class GetMatchingAppsUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val appInfoReader: AppInfoReader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseUseCase<GetMatchMakingAppsInput, List<MatchmakingAppData>>(ioDispatcher) {

    override suspend fun execute(input: GetMatchMakingAppsInput): List<MatchmakingAppData> {
        val result =
            suspendCancellableCoroutine<Map<String, Set<String>>> { continuation ->
                healthConnectManager.getMatchingApps(
                    MatchmakingRequest.Builder()
                        .setCallingPackageName(input.packageName)
                        .addRecordTypes(input.recordTypes)
                        .build(),
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        return result
            .map { (packageName, permissions) ->
                MatchmakingAppData(
                    appInfoReader.getAppMetadata(packageName),
                    permissions
                        .map {
                            HealthPermission.fromPermissionString(it)
                                as HealthPermission.FitnessPermission
                        }
                        .toList(),
                )
            }
            .toList()
    }

    data class GetMatchMakingAppsInput(
        val packageName: String,
        val recordTypes: Set<Class<out Record>>,
    )
}
