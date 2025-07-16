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

import android.content.Context
import android.platform.test.annotations.EnableFlags
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.devices.DeviceDataSource
import com.android.healthconnect.controller.devices.LoadDeviceDataSources
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
class LoadDeviceDataSourcesTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var loadDeviceDataSources: LoadDeviceDataSources

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun execute_whenDeviceNameIsSet_returnsDeviceName() {
        runBlocking {
            val testDeviceName = "Test Device"
            Settings.Global.putString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME,
                testDeviceName,
            )

            val result = loadDeviceDataSources.execute(Unit)

            assertThat(result).containsExactly(DeviceDataSource(testDeviceName, true))
        }
    }

    @Test
    fun execute_whenDeviceNameIsNotSet_returnsUnknownDevice() {
        runBlocking {
            Settings.Global.putString(context.contentResolver, Settings.Global.DEVICE_NAME, null)

            val result = loadDeviceDataSources.execute(Unit)

            val expectedDeviceName = context.getString(R.string.devices_unknown_device)
            assertThat(result).containsExactly(DeviceDataSource(expectedDeviceName, true))
        }
    }
}
