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

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.api.GetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class GetDeviceDataSourcesInfoUseCaseTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var getDeviceDataSourcesInfoUseCase: GetDeviceDataSourcesInfoUseCase
    private val healthConnectManager: HealthConnectManager = mock()

    @Before
    fun setup() {
        getDeviceDataSourcesInfoUseCase =
            GetDeviceDataSourcesInfoUseCase(
                healthConnectManager,
                kotlinx.coroutines.Dispatchers.Main,
            )
    }

    @Test
    fun execute_returnsDeviceDataSources() = runTest {
        val deviceDataSourceInfo =
            DeviceDataSourceInfo(
                DataOrigin.Builder().setPackageName("com.example.watch").build(),
                Device.Builder()
                    .setManufacturer("Google")
                    .setModel("Pixel Watch")
                    .setType(Device.DEVICE_TYPE_WATCH)
                    .build(),
                false,
                listOf(),
            )
        whenever(healthConnectManager.getDeviceDataSourceInfos(any(), any())).thenAnswer {
            val receiver =
                it.arguments[1]
                    as
                    android.os.OutcomeReceiver<List<DeviceDataSourceInfo>, HealthConnectException>
            receiver.onResult(listOf(deviceDataSourceInfo))
            null
        }

        val result = getDeviceDataSourcesInfoUseCase.invoke(Unit) as UseCaseResults.Success

        assertThat(result.data).containsExactly(deviceDataSourceInfo)
    }

    @Test
    fun execute_healthConnectException_returnsFailed() = runTest {
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
        whenever(healthConnectManager.getDeviceDataSourceInfos(any(), any())).thenAnswer {
            val receiver =
                it.arguments[1]
                    as
                    android.os.OutcomeReceiver<List<DeviceDataSourceInfo>, HealthConnectException>
            receiver.onError(exception)
            null
        }

        val result = getDeviceDataSourcesInfoUseCase.invoke(Unit) as UseCaseResults.Failed

        assertThat(result.exception).isEqualTo(exception)
    }
}
