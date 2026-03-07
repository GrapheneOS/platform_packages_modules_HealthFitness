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

import android.health.connect.GetMatchingDataSourcesResponse
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.MatchmakingRequest
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Record
import android.os.OutcomeReceiver
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase.MatchingDataSources
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.findSystemInfo
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import com.android.healthfitness.flags.Flags.deviceDataProvidersUiMatchmakingScreen
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

/** Use case to get matching data sources for a given set of record types. */
class GetMatchingDataSourcesUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    private val appInfoReader: AppInfoReader,
    private val getMatchingDeviceDataSourcesUseCase: GetMatchingDeviceDataSourcesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseUseCase<GetMatchingDataSourcesInput, MatchingDataSources>(ioDispatcher) {

    override suspend fun execute(input: GetMatchingDataSourcesInput): MatchingDataSources {
        val requestBuilder =
            MatchmakingRequest.Builder()
                .addRecordTypes(input.recordTypes)
                .setCallingPackageName(input.packageName)
        if (deviceDataProvidersApi() && deviceDataProvidersUiMatchmakingScreen()) {
            requestBuilder.setIncludedDataSources(input.includedDataOrigins)
            requestBuilder.setExcludedDataSources(input.excludedDataOrigins)
        }
        val result =
            suspendCancellableCoroutine<GetMatchingDataSourcesResponse> { continuation ->
                healthConnectManager.getMatchingDataSources(
                    requestBuilder.build(),
                    Executor { it.run() },
                    object :
                        OutcomeReceiver<GetMatchingDataSourcesResponse, HealthConnectException> {
                        override fun onResult(result: GetMatchingDataSourcesResponse) {
                            continuation.resume(result)
                        }

                        override fun onError(error: HealthConnectException) {
                            continuation.resumeWithException(error)
                        }
                    },
                )
            }

        val matchingApps =
            result.matchingApps
                .mapNotNull { (packageName, permissions) ->
                    try {
                        val appMetadata = appInfoReader.getAppMetadata(packageName)
                        MatchmakingAppData(
                            appMetadata,
                            permissions
                                .map {
                                    HealthPermission.fromPermissionString(it)
                                        as HealthPermission.FitnessPermission
                                }
                                .toList(),
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .toList()
        var matchingDevices: List<MatchmakingDeviceData> = emptyList()
        if (deviceDataProvidersApi() && deviceDataProvidersUiMatchmakingScreen()) {
            when (
                val deviceResult =
                    getMatchingDeviceDataSourcesUseCase.invoke(
                        GetMatchingDeviceDataSourcesUseCase.Input(result.matchingDevices)
                    )
            ) {
                is UseCaseResults.Success -> {
                    matchingDevices =
                        deviceResult.data
                            // TODO(b/469717403): Device how to handle native tracking properly
                            // Filter out devices advertised by the system
                            .filterNot { it.deviceDataSourceInfo.findSystemInfo() != null }
                            .toList()
                }
                is UseCaseResults.Failed -> {
                    throw deviceResult.exception
                }
            }
        }

        return MatchingDataSources(matchingApps, matchingDevices)
    }

    data class GetMatchingDataSourcesInput(
        val packageName: String,
        val recordTypes: Set<Class<out Record>>,
        val includedDataOrigins: Set<DataOrigin> = emptySet(),
        val excludedDataOrigins: Set<DataOrigin> = emptySet(),
    )

    data class MatchingDataSources(
        val matchingApps: List<MatchmakingAppData>,
        val matchingDevices: List<MatchmakingDeviceData>,
    )
}
