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

package com.android.healthconnect.controller.tests.matchmaking.api

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.DataOrigin
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDeviceDataSourcesUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.devices.api.FakeGetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GetMatchingDeviceDataSourcesUseCaseTest {

    @get:Rule val hiltRule = dagger.hilt.android.testing.HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    // TODO(b/487238102): Add failing scenarios

    private lateinit var useCase: GetMatchingDeviceDataSourcesUseCase
    private val fakeLoadDeviceDataSourcesInfosUseCase =
        fakeUseCaseRule.watch(FakeGetDeviceDataSourcesInfoUseCase())

    @Before
    fun setup() {
        hiltRule.inject()
        useCase =
            GetMatchingDeviceDataSourcesUseCase(
                fakeLoadDeviceDataSourcesInfosUseCase,
                Dispatchers.Main,
            )
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun execute_filtersAndMapsDeviceDataCorrectly() = runTest {
        val matchedDevicePackageName = "com.example.device"
        val unmatchedDevicePackageName = "com.example.unmatched"
        val allDevices =
            setOf(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName(matchedDevicePackageName).build(),
                    android.health.connect.datatypes.Device.Builder().build(),
                    false,
                    emptyList(),
                ),
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName(unmatchedDevicePackageName).build(),
                    android.health.connect.datatypes.Device.Builder().build(),
                    false,
                    emptyList(),
                ),
            )
        fakeLoadDeviceDataSourcesInfosUseCase.updateSet(allDevices)
        val matchedDevicesMap = mapOf(matchedDevicePackageName to setOf<String>())
        val input = GetMatchingDeviceDataSourcesUseCase.Input(matchedDevicesMap)

        val result = useCase.invoke(input) as UseCaseResults.Success

        assertThat(result.data).hasSize(1)
        assertThat(result.data.first().deviceDataSourceInfo.deviceDataOrigin.packageName)
            .isEqualTo(matchedDevicePackageName)
        assertThat(result.data.first().permissions).isEmpty()
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun execute_flagsDisabled_returnsEmptyList() = runTest {
        val matchedDevicePackageName = "com.example.device"
        val matchedDevicesMap = mapOf(matchedDevicePackageName to setOf<String>())
        val input = GetMatchingDeviceDataSourcesUseCase.Input(matchedDevicesMap)
        fakeLoadDeviceDataSourcesInfosUseCase.setForceFail(true)

        val result = useCase.invoke(input) as UseCaseResults.Success

        assertThat(result.data).isEmpty()
    }
}
