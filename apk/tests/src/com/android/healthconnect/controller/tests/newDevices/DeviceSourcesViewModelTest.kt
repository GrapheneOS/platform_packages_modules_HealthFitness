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

package com.android.healthconnect.controller.tests.newDevices

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectException
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.api.GetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.matchmaking.api.SetTrackingEnabledUseCase
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel.DeviceSourcesState
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel.SelectedDeviceSourceInfoState
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_SPN
import com.android.healthconnect.controller.tests.utils.getDeviceDataSourcesInfo
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
class DeviceSourcesViewModelTest {

    @get:Rule(order = 0) val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    private val getDeviceDataSourcesInfoUseCase: GetDeviceDataSourcesInfoUseCase = mock()
    private val setTrackingEnabledUseCase: SetTrackingEnabledUseCase = mock()

    private lateinit var viewModel: DeviceSourcesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDeviceSourcesInfos_success_updatesState() = runTest {
        val state = loadSuccessfulDeviceSourcesState(getDeviceDataSourcesInfo())
        assertThat(state).isInstanceOf(DeviceSourcesState.WithData::class.java)
        val dataState = state as DeviceSourcesState.WithData
        assertThat(dataState.deviceSourcesInfos).isEqualTo(getDeviceDataSourcesInfo())
    }

    @Test
    fun loadDeviceSourcesInfos_error_updatesState() = runTest {
        val state = loadFailedDeviceSourcesState()
        assertThat(state).isEqualTo(DeviceSourcesState.Error)
    }

    @Test
    fun loadSelectedDeviceSourceInfo_success_updatesState() = runTest {
        val state = loadSelectedDeviceSourceInfoState(getDeviceDataSourcesInfo(), TEST_PHONE_SPN)
        assertThat(state).isInstanceOf(SelectedDeviceSourceInfoState.WithData::class.java)
        val dataState = state as SelectedDeviceSourceInfoState.WithData
        assertThat(dataState.selectedDeviceSourceInfo)
            .isEqualTo(
                getDeviceDataSourcesInfo().find {
                    it.deviceDataOrigin.packageName == TEST_PHONE_SPN
                }
            )
    }

    @Test
    fun loadSelectedDeviceSourceInfo_noMatchingDevice_updatesState() = runTest {
        val state = loadSelectedDeviceSourceInfoState(getDeviceDataSourcesInfo(), "some.package")
        assertThat(state).isEqualTo(SelectedDeviceSourceInfoState.Error)
    }

    @Test
    fun loadSelectedDeviceSourceInfo_error_updatesState() = runTest {
        val state = loadFailedSelectedDeviceState()
        assertThat(state).isEqualTo(SelectedDeviceSourceInfoState.Error)
    }

    private suspend fun stubGetDeviceDataSourcesInfoUseCase(
        expectedInfos: Set<DeviceDataSourceInfo>
    ) {
        whenever(getDeviceDataSourcesInfoUseCase.invoke(any()))
            .doReturn(UseCaseResults.Success(expectedInfos))
    }

    private suspend fun TestScope.loadSuccessfulDeviceSourcesState(
        expectedInfos: Set<DeviceDataSourceInfo> = setOf()
    ): DeviceSourcesState {
        stubGetDeviceDataSourcesInfoUseCase(expectedInfos)

        viewModel =
            DeviceSourcesViewModel(getDeviceDataSourcesInfoUseCase, setTrackingEnabledUseCase)
        advanceUntilIdle()

        return getDeviceSourcesState()
    }

    private suspend fun TestScope.loadFailedDeviceSourcesState(): DeviceSourcesState {
        whenever(getDeviceDataSourcesInfoUseCase.invoke(any()))
            .doReturn(UseCaseResults.Failed(HealthConnectException(1)))
        viewModel =
            DeviceSourcesViewModel(getDeviceDataSourcesInfoUseCase, setTrackingEnabledUseCase)
        advanceUntilIdle()

        return getDeviceSourcesState()
    }

    private suspend fun TestScope.loadSelectedDeviceSourceInfoState(
        expectedInfos: Set<DeviceDataSourceInfo> = setOf(),
        selectedPackageName: String = "",
    ): SelectedDeviceSourceInfoState {
        stubGetDeviceDataSourcesInfoUseCase(expectedInfos)

        viewModel =
            DeviceSourcesViewModel(getDeviceDataSourcesInfoUseCase, setTrackingEnabledUseCase)
        advanceUntilIdle()

        return getSelectedDeviceSourceState(selectedPackageName)
    }

    private suspend fun TestScope.loadFailedSelectedDeviceState(): SelectedDeviceSourceInfoState {
        whenever(getDeviceDataSourcesInfoUseCase.invoke(any()))
            .doReturn(UseCaseResults.Failed(HealthConnectException(1)))
        viewModel =
            DeviceSourcesViewModel(getDeviceDataSourcesInfoUseCase, setTrackingEnabledUseCase)
        advanceUntilIdle()

        return getSelectedDeviceSourceState()
    }

    private fun TestScope.getDeviceSourcesState(): DeviceSourcesState {
        val actualState = mutableListOf<DeviceSourcesState>()
        val stateCollectJob = launch {
            viewModel.deviceSourcesState.collect { value -> actualState.add(value) }
        }
        advanceUntilIdle()

        viewModel.loadDeviceSourcesInfos()
        advanceUntilIdle()

        stateCollectJob.cancel()
        return actualState.last()
    }

    private fun TestScope.getSelectedDeviceSourceState(
        selectedPackageName: String = ""
    ): SelectedDeviceSourceInfoState {
        val actualState = mutableListOf<SelectedDeviceSourceInfoState>()
        val stateCollectJob = launch {
            viewModel.selectedDeviceSourceInfoState.collect { value -> actualState.add(value) }
        }
        advanceUntilIdle()

        viewModel.loadSelectedDeviceSourceInfo(selectedPackageName)
        advanceUntilIdle()

        stateCollectJob.cancel()
        return actualState.last()
    }
}
