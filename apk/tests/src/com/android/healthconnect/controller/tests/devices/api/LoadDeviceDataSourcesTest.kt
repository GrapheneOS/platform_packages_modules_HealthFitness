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
package com.android.healthconnect.controller.tests.devices.api

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.EnableFlags
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.devices.DeviceDataSource
import com.android.healthconnect.controller.devices.api.LoadDeviceDataSourcesUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
class LoadDeviceDataSourcesTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var loadDeviceDataSources: LoadDeviceDataSourcesUseCase
    private var manager: HealthConnectManager = mock()

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        // Required for modifying Settings.Global
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        loadDeviceDataSources = LoadDeviceDataSourcesUseCase(context, manager, Dispatchers.IO)
    }

    @Test
    fun invoke_whenDeviceNameIsSet_returnsDeviceName() {
        runBlocking {
            val testDeviceName = "Test Device"
            Settings.Global.putString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME,
                testDeviceName,
            )
            whenever(manager.isTrackingEnabled(listOf(StepsRecord::class.java))).then {
                mapOf(StepsRecord::class.java to true)
            }

            val result = loadDeviceDataSources.invoke(Unit)

            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .containsExactly(
                    DeviceDataSource(
                        deviceName = testDeviceName,
                        isCurrentDevice = true,
                        trackerStatus = mapOf(StepsRecord::class.java to true),
                    )
                )
        }
    }

    @Test
    fun invoke_whenDeviceNameIsNotSet_returnsUnknownDevice() {
        runBlocking {
            Settings.Global.putString(context.contentResolver, Settings.Global.DEVICE_NAME, null)
            whenever(manager.isTrackingEnabled(listOf(StepsRecord::class.java))).then {
                mapOf(StepsRecord::class.java to true)
            }

            val result = loadDeviceDataSources.invoke(Unit)

            val expectedDeviceName = context.getString(R.string.devices_unknown_device)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .containsExactly(
                    DeviceDataSource(
                        deviceName = expectedDeviceName,
                        isCurrentDevice = true,
                        trackerStatus = mapOf(StepsRecord::class.java to true),
                    )
                )
        }
    }

    @Test
    fun invoke_whenTrackingIsDisabled_returnsTrackerStatusFalse() {
        runBlocking {
            Settings.Global.putString(context.contentResolver, Settings.Global.DEVICE_NAME, null)
            whenever(manager.isTrackingEnabled(listOf(StepsRecord::class.java))).then {
                mapOf(StepsRecord::class.java to false)
            }

            val result = loadDeviceDataSources.invoke(Unit)

            val expectedDeviceName = context.getString(R.string.devices_unknown_device)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .containsExactly(
                    DeviceDataSource(
                        deviceName = expectedDeviceName,
                        isCurrentDevice = true,
                        trackerStatus = mapOf(StepsRecord::class.java to false),
                    )
                )
        }
    }

    @Test
    fun invoke_onException_returnsFailed() {
        runBlocking {
            whenever(manager.isTrackingEnabled(listOf(StepsRecord::class.java)))
                .thenThrow(RuntimeException("Error"))

            val result = loadDeviceDataSources.invoke(Unit)

            assertThat(result is UseCaseResults.Failed).isTrue()
            assertThat((result as UseCaseResults.Failed).exception)
                .isInstanceOf(RuntimeException::class.java)
        }
    }
}
