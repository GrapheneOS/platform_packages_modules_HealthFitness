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
package com.android.healthconnect.controller.tests.devices

import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.EnableFlags
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel.ConnectedDevicesState
import com.android.healthconnect.controller.devices.DeviceDataSource
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeLoadDeviceDataSourcesUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeSetTrackingEnabledUseCase
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
class ConnectedDevicesViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val loadDeviceDataSourcesUseCase = FakeLoadDeviceDataSourcesUseCase()
    private val setTrackingEnabledUseCase = FakeSetTrackingEnabledUseCase()

    private lateinit var viewModel: ConnectedDevicesViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel =
            ConnectedDevicesViewModel(
                loadDeviceDataSourcesUseCase,
                setTrackingEnabledUseCase,
                testDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setSelectedDevice_setsSelectedDevice() {
        val testObserver = TestObserver<DeviceDataSource>()
        viewModel.selectedDevice.observeForever(testObserver)
        viewModel.setSelectedDevice(
            DeviceDataSource(
                deviceName = "Pixel 8",
                isCurrentDevice = true,
                trackerStatus = mapOf(StepsRecord::class.java to true),
            )
        )

        val actual = testObserver.getLastValue()
        assertThat(actual)
            .isEqualTo(
                DeviceDataSource(
                    deviceName = "Pixel 8",
                    isCurrentDevice = true,
                    trackerStatus = mapOf(StepsRecord::class.java to true),
                )
            )
    }

    @Test
    fun setTrackingEnabled_callsUseCase() {
        viewModel.setTrackingEnabled(StepsRecord::class.java, true)

        val latestInput = setTrackingEnabledUseCase.latestInput
        assertThat(latestInput?.recordType).isEqualTo(StepsRecord::class.java)
        assertThat(latestInput?.isEnabled).isEqualTo(true)
    }

    @Test
    fun setTrackingEnabled_updatesSelectedDevice() {
        val testObserver = TestObserver<DeviceDataSource>()
        val initialDevice =
            DeviceDataSource(
                deviceName = "Pixel 8",
                isCurrentDevice = true,
                trackerStatus = mapOf(StepsRecord::class.java to false),
            )
        viewModel.setSelectedDevice(initialDevice)
        viewModel.selectedDevice.observeForever(testObserver)

        viewModel.setTrackingEnabled(StepsRecord::class.java, true)

        val actual = testObserver.getLastValue()
        assertThat(actual)
            .isEqualTo(
                DeviceDataSource(
                    deviceName = "Pixel 8",
                    isCurrentDevice = true,
                    trackerStatus = mapOf(StepsRecord::class.java to true),
                )
            )
    }

    @Test
    fun loadDeviceDataSources_success_loadsDeviceDataSources() = runTest {
        val testObserver = TestObserver<ConnectedDevicesState>()
        loadDeviceDataSourcesUseCase.updateList(
            listOf(
                DeviceDataSource(
                    deviceName = "Pixel 8",
                    isCurrentDevice = true,
                    trackerStatus = mapOf(StepsRecord::class.java to true),
                )
            )
        )
        viewModel.connectedDevicesState.observeForever(testObserver)
        viewModel.loadDeviceDataSources()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual)
            .isEqualTo(
                ConnectedDevicesState.Success(
                    listOf(
                        DeviceDataSource(
                            deviceName = "Pixel 8",
                            isCurrentDevice = true,
                            trackerStatus = mapOf(StepsRecord::class.java to true),
                        )
                    )
                )
            )
    }

    @Test
    fun loadDeviceDataSources_error_returnsErrorState() = runTest {
        val testObserver = TestObserver<ConnectedDevicesState>()
        loadDeviceDataSourcesUseCase.setForceFail(true)
        viewModel.connectedDevicesState.observeForever(testObserver)
        viewModel.loadDeviceDataSources()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual).isEqualTo(ConnectedDevicesState.Error)
    }
}
