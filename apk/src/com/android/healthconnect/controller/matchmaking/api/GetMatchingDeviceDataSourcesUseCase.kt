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

import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import com.android.healthfitness.flags.Flags.deviceDataProvidersUiMatchmakingScreen
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/** Use case to get matching device data sources info. */
class GetMatchingDeviceDataSourcesUseCase
@Inject
constructor(
    private val getDeviceDataSourcesInfoUseCase: GetDeviceDataSourcesInfoUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) :
    BaseUseCase<GetMatchingDeviceDataSourcesUseCase.Input, List<MatchmakingDeviceData>>(
        ioDispatcher
    ) {

    override suspend fun execute(input: Input): List<MatchmakingDeviceData> {
        if (!deviceDataProvidersApi() || !deviceDataProvidersUiMatchmakingScreen()) {
            return emptyList()
        }
        return when (val result = getDeviceDataSourcesInfoUseCase.invoke(Unit)) {
            is UseCaseResults.Success -> {
                val matchedDevicePackageNames = input.matchedDevices.keys
                result.data
                    .filter { matchedDevicePackageNames.contains(it.deviceDataOrigin.packageName) }
                    .map { deviceDataSourceInfo ->
                        val permissionsForDevice =
                            input.matchedDevices[deviceDataSourceInfo.deviceDataOrigin.packageName]
                                ?.map {
                                    HealthPermission.fromPermissionString(it)
                                        as HealthPermission.FitnessPermission
                                }
                                ?.toList() ?: emptyList()
                        MatchmakingDeviceData(deviceDataSourceInfo, permissionsForDevice)
                    }
                    .toList()
            }
            is UseCaseResults.Failed -> {
                throw result.exception
            }
        }
    }

    data class Input(val matchedDevices: Map<String, Set<String>>)
}
