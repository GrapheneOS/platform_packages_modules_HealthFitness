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
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import androidx.lifecycle.MutableLiveData
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel.ConnectedDevicesState
import com.android.healthconnect.controller.devices.DeviceDataSource
import com.android.healthconnect.controller.devices.DeviceManagementFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
class DeviceManagementFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val viewModel: ConnectedDevicesViewModel = mock()

    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context
    private val connectedDevicesState = MutableLiveData<ConnectedDevicesState>()
    private val selectedDevice = MutableLiveData<DeviceDataSource>()

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        whenever(viewModel.connectedDevicesState).then { connectedDevicesState }
        whenever(viewModel.selectedDevice).then { selectedDevice }
        selectedDevice.postValue(DeviceDataSource("Pixel 8", isCurrentDevice = true))
        connectedDevicesState.postValue(
            ConnectedDevicesState.Success(
                listOf(DeviceDataSource("Pixel 8", isCurrentDevice = true))
            )
        )
    }

    @Test
    fun header_isDisplayed() {
        launchFragment<DeviceManagementFragment>(Bundle())

        onView(withText("Pixel 8")).check(matches(isDisplayed()))
    }

    @Test
    fun stepTrackingSwitch_isDisplayed() {
        launchFragment<DeviceManagementFragment>(Bundle())

        onView(withText("Steps")).check(matches(isDisplayed()))
    }

    @Test
    fun footer_isDisplayed() {
        launchFragment<DeviceManagementFragment>(Bundle())

        onView(
                withText(
                    "Data collected by this device will be stored in Health Connect, where connected" +
                        " apps will access it"
                )
            )
            .check(matches(isDisplayed()))
    }
}
