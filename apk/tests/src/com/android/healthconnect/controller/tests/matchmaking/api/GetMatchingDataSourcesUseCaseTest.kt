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

package com.android.healthconnect.controller.tests.matchmaking.api

import android.health.connect.GetMatchingDataSourcesResponse
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions
import android.health.connect.MatchmakingRequest
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDeviceDataSourcesUseCase
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class GetMatchingDataSourcesUseCaseTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var useCase: GetMatchingDataSourcesUseCase
    private val healthConnectManager: HealthConnectManager = mock()
    private val appInfoReader: AppInfoReader = mock()
    private val getMatchingDeviceDataSourcesUseCase: GetMatchingDeviceDataSourcesUseCase = mock()

    @Before
    fun setup() {
        useCase =
            GetMatchingDataSourcesUseCase(
                healthConnectManager,
                appInfoReader,
                getMatchingDeviceDataSourcesUseCase,
                Dispatchers.Main,
            )
    }

    @Test
    fun execute_returnsMatchingAppsAndDevices() = runTest {
        val appPackageName = "com.example.app"
        val devicePackageName = "com.example.device"
        val response =
            GetMatchingDataSourcesResponse(
                mapOf(appPackageName to setOf(HealthPermissions.READ_STEPS)),
                mapOf(devicePackageName to setOf()),
            )
        whenever(healthConnectManager.getMatchingDataSources(any(), any(), any())).thenAnswer {
            val receiver =
                it.arguments[2]
                    as
                    android.os.OutcomeReceiver<
                        GetMatchingDataSourcesResponse,
                        HealthConnectException,
                    >
            receiver.onResult(response)
            null
        }
        whenever(appInfoReader.getAppMetadata(appPackageName))
            .thenReturn(AppMetadata(appPackageName, "App", null))
        whenever(getMatchingDeviceDataSourcesUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(emptyList()))

        val input =
            GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput(
                "calling.app",
                setOf(StepsRecord::class.java),
            )
        val result = useCase.invoke(input) as UseCaseResults.Success

        assertThat(result.data.matchingApps).hasSize(1)
        assertThat(result.data.matchingApps[0].metadata.packageName).isEqualTo(appPackageName)
        assertThat(result.data.matchingDevices).isEmpty()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun execute_withDeviceFlagsEnabled_sendsIncludedDataSourcesInRequest() = runTest {
        val appPackageName = "com.example.app"
        val devicePackageName = "com.example.device"
        val includedDataOrigin = DataOrigin.Builder().setPackageName("com.example.included").build()
        val response =
            GetMatchingDataSourcesResponse(
                mapOf(appPackageName to setOf(HealthPermissions.READ_STEPS)),
                mapOf(devicePackageName to setOf()),
            )
        val matchmakingRequestCaptor = argumentCaptor<MatchmakingRequest>()
        whenever(
                healthConnectManager.getMatchingDataSources(
                    matchmakingRequestCaptor.capture(),
                    any(),
                    any(),
                )
            )
            .thenAnswer {
                val receiver =
                    it.arguments[2]
                        as
                        android.os.OutcomeReceiver<
                            GetMatchingDataSourcesResponse,
                            HealthConnectException,
                        >
                receiver.onResult(response)
                null
            }
        whenever(appInfoReader.getAppMetadata(appPackageName))
            .thenReturn(AppMetadata(appPackageName, "App", null))
        whenever(getMatchingDeviceDataSourcesUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(emptyList()))

        val input =
            GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput(
                "calling.app",
                setOf(StepsRecord::class.java),
                includedDataOrigins = setOf(includedDataOrigin),
            )
        val result = useCase.invoke(input) as UseCaseResults.Success

        assertThat(result.data.matchingApps).hasSize(1)
        assertThat(result.data.matchingApps[0].metadata.packageName).isEqualTo(appPackageName)
        assertThat(result.data.matchingDevices).isEmpty()
        assertThat(matchmakingRequestCaptor.firstValue.getIncludedDataSources())
            .containsExactly(includedDataOrigin)
        assertThat(matchmakingRequestCaptor.firstValue.getExcludedDataSources()).isEmpty()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun execute_withDeviceFlagsEnabled_sendsExcludedDataSourcesInRequest() = runTest {
        val appPackageName = "com.example.app"
        val devicePackageName = "com.example.device"
        val excludedDataOrigin = DataOrigin.Builder().setPackageName("com.example.excluded").build()
        val response =
            GetMatchingDataSourcesResponse(
                mapOf(appPackageName to setOf(HealthPermissions.READ_STEPS)),
                mapOf(devicePackageName to setOf()),
            )
        val matchmakingRequestCaptor = argumentCaptor<MatchmakingRequest>()
        whenever(
                healthConnectManager.getMatchingDataSources(
                    matchmakingRequestCaptor.capture(),
                    any(),
                    any(),
                )
            )
            .thenAnswer {
                val receiver =
                    it.arguments[2]
                        as
                        android.os.OutcomeReceiver<
                            GetMatchingDataSourcesResponse,
                            HealthConnectException,
                        >
                receiver.onResult(response)
                null
            }
        whenever(appInfoReader.getAppMetadata(appPackageName))
            .thenReturn(AppMetadata(appPackageName, "App", null))
        whenever(getMatchingDeviceDataSourcesUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(emptyList()))

        val input =
            GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput(
                "calling.app",
                setOf(StepsRecord::class.java),
                excludedDataOrigins = setOf(excludedDataOrigin),
            )
        val result = useCase.invoke(input) as UseCaseResults.Success

        assertThat(result.data.matchingApps).hasSize(1)
        assertThat(result.data.matchingApps[0].metadata.packageName).isEqualTo(appPackageName)
        assertThat(result.data.matchingDevices).isEmpty()
        assertThat(matchmakingRequestCaptor.firstValue.getIncludedDataSources()).isEmpty()
        assertThat(matchmakingRequestCaptor.firstValue.getExcludedDataSources())
            .containsExactly(excludedDataOrigin)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun execute_withDeviceFlagsDisabled_ignoresIncludedDataSources() = runTest {
        val appPackageName = "com.example.app"
        val response =
            GetMatchingDataSourcesResponse(
                mapOf(appPackageName to setOf(HealthPermissions.READ_STEPS)),
                // When flags are off, the service would return an empty map for devices
                mapOf(),
            )
        val matchmakingRequestCaptor = argumentCaptor<MatchmakingRequest>()
        whenever(
                healthConnectManager.getMatchingDataSources(
                    matchmakingRequestCaptor.capture(),
                    any(),
                    any(),
                )
            )
            .thenAnswer {
                val receiver =
                    it.arguments[2]
                        as
                        android.os.OutcomeReceiver<
                            GetMatchingDataSourcesResponse,
                            HealthConnectException,
                        >
                receiver.onResult(response)
                null
            }
        whenever(appInfoReader.getAppMetadata(appPackageName))
            .thenReturn(AppMetadata(appPackageName, "App", null))
        whenever(getMatchingDeviceDataSourcesUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(emptyList()))

        val input =
            GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput(
                "calling.app",
                setOf(StepsRecord::class.java),
                includedDataOrigins =
                    setOf(DataOrigin.Builder().setPackageName("com.example.included").build()),
            )
        val result = useCase.invoke(input) as UseCaseResults.Success

        assertThat(result.data.matchingApps).hasSize(1)
        assertThat(result.data.matchingApps[0].metadata.packageName).isEqualTo(appPackageName)
        assertThat(result.data.matchingDevices).isEmpty()
        assertThat(matchmakingRequestCaptor.firstValue.getIncludedDataSources()).isEmpty()
        assertThat(matchmakingRequestCaptor.firstValue.getExcludedDataSources()).isEmpty()
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun execute_withDeviceFlagsDisabled_ignoresExcludedDataSources() = runTest {
        val appPackageName = "com.example.app"
        val response =
            GetMatchingDataSourcesResponse(
                mapOf(appPackageName to setOf(HealthPermissions.READ_STEPS)),
                // When flags are off, the service would return an empty map for devices
                mapOf(),
            )
        val matchmakingRequestCaptor = argumentCaptor<MatchmakingRequest>()
        whenever(
                healthConnectManager.getMatchingDataSources(
                    matchmakingRequestCaptor.capture(),
                    any(),
                    any(),
                )
            )
            .thenAnswer {
                val receiver =
                    it.arguments[2]
                        as
                        android.os.OutcomeReceiver<
                            GetMatchingDataSourcesResponse,
                            HealthConnectException,
                        >
                receiver.onResult(response)
                null
            }
        whenever(appInfoReader.getAppMetadata(appPackageName))
            .thenReturn(AppMetadata(appPackageName, "App", null))
        whenever(getMatchingDeviceDataSourcesUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(emptyList()))

        val input =
            GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput(
                "calling.app",
                setOf(StepsRecord::class.java),
                excludedDataOrigins =
                    setOf(DataOrigin.Builder().setPackageName("com.example.excluded").build()),
            )
        val result = useCase.invoke(input) as UseCaseResults.Success

        // Verify the main data is correct
        assertThat(result.data.matchingApps).hasSize(1)
        assertThat(result.data.matchingApps[0].metadata.packageName).isEqualTo(appPackageName)
        assertThat(result.data.matchingDevices).isEmpty()

        // Crucially, verify that the sources were NOT added to the request
        assertThat(matchmakingRequestCaptor.firstValue.getIncludedDataSources()).isEmpty()
        assertThat(matchmakingRequestCaptor.firstValue.getExcludedDataSources()).isEmpty()
    }

    @Test
    fun execute_healthConnectException_returnsFailed() = runTest {
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
        whenever(healthConnectManager.getMatchingDataSources(any(), any(), any())).thenAnswer {
            val receiver =
                it.arguments[2]
                    as
                    android.os.OutcomeReceiver<
                        GetMatchingDataSourcesResponse,
                        HealthConnectException,
                    >
            receiver.onError(exception)
            null
        }
        whenever(getMatchingDeviceDataSourcesUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(emptyList()))

        val input =
            GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput(
                "calling.app",
                setOf(StepsRecord::class.java),
            )
        val result = useCase.invoke(input) as UseCaseResults.Failed

        assertThat(result.exception).isEqualTo(exception)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun execute_getMatchingDeviceDataSourcesUseCaseReturnsFailed_returnsFailed() = runTest {
        val exception = RuntimeException("Test exception")
        whenever(healthConnectManager.getMatchingDataSources(any(), any(), any())).thenAnswer {
            val receiver =
                it.arguments[2]
                    as
                    android.os.OutcomeReceiver<
                        GetMatchingDataSourcesResponse,
                        HealthConnectException,
                    >
            receiver.onResult(GetMatchingDataSourcesResponse(emptyMap(), emptyMap()))
            null
        }
        whenever(getMatchingDeviceDataSourcesUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Failed(exception))

        val input =
            GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput(
                "calling.app",
                setOf(StepsRecord::class.java),
            )
        val result = useCase.invoke(input) as UseCaseResults.Failed

        assertThat(result.exception).isEqualTo(exception)
    }
}
