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
import android.health.connect.datatypes.StepsRecord
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel.ConnectedDevicesState
import com.android.healthconnect.controller.devices.DeviceDataSource
import com.android.healthconnect.controller.devices.DeviceManagementFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
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
        whenever(viewModel.hasStepsSensor).then { MutableLiveData(true) }
        val currentDevice =
            DeviceDataSource(
                deviceName = "Pixel 8",
                isCurrentDevice = true,
                trackerStatus = mapOf(StepsRecord::class.java to true),
            )
        selectedDevice.postValue(currentDevice)
        connectedDevicesState.postValue(ConnectedDevicesState.Success(listOf(currentDevice)))
    }

    @Test
    fun noStepsBanner_hasSensor_isNotDisplayed() {
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(withText("This device doesn't track steps")).check(doesNotExist())
        }
    }

    @Test
    fun noStepsBanner_hasNoSensor_isDisplayed() {
        whenever(viewModel.hasStepsSensor).then { MutableLiveData(false) }
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(withText("This device doesn't track steps")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun header_isDisplayed() {
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(withText("Pixel 8")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun stepTrackingSwitch_hasSensor_isDisplayed() {
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(withText("Steps")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun stepTrackingSwitch_hasNoSensor_isDisplayedAndDisabled() {
        whenever(viewModel.hasStepsSensor).then { MutableLiveData(false) }

        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(withText("Steps")).check(matches(isDisplayed()))
            onView(withText("Steps")).check(matches(ViewMatchers.isNotEnabled()))
        }
    }

    @Test
    fun stepTrackingSwitch_whenClicked_callsViewModel() {
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            // Disable, then re-enable steps tracking.
            onView(withText("Steps")).perform(click())
            verify(viewModel).setTrackingEnabled(StepsRecord::class.java, false)
            onView(withText("Steps")).perform(click())
            verify(viewModel).setTrackingEnabled(StepsRecord::class.java, true)
        }
    }

    @Test
    fun seeDeviceData_isDisplayed() {
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(withText("Manage device")).check(matches(isDisplayed()))
            onView(withText("See device data")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun seeDeviceData_navigatesToAppData() {
        launchFragment<DeviceManagementFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.device_management_nav_graph)
                navHostController.setCurrentDestination(R.id.deviceManagementFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("See device data")).perform(click())

                assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.appDataFragment)
            }
    }

    @Test
    fun footer_hasSensor_displaysCorrectText() {
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(
                    withText(
                        "Data collected by this device will be stored in Health\u00A0Connect, where connected" +
                            " apps will access it"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun footer_hasNoSensor_displaysCorrectText() {
        whenever(viewModel.hasStepsSensor).then { MutableLiveData(false) }
        launchFragment<DeviceManagementFragment>(Bundle()).use {
            onView(
                    withText(
                        "This device doesn't support step tracking, but still has data stored in Health\u00A0Connect"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }
}
