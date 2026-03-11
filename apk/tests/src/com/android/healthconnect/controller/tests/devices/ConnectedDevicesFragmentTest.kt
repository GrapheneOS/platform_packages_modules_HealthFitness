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
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.devices.ConnectedDevicesFragment
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel.ConnectedDevicesState
import com.android.healthconnect.controller.devices.DeviceDataSource
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
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ConnectedDevicesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val viewModel: ConnectedDevicesViewModel = mock()

    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context
    private val connectedDevicesState = MutableLiveData<ConnectedDevicesState>()

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        whenever(viewModel.connectedDevicesState).then { connectedDevicesState }
    }

    @Test
    fun connectedDevicesFragment_launchable() {
        connectedDevicesState.postValue(
            ConnectedDevicesState.Success(
                listOf(DeviceDataSource("Pixel 8", isCurrentDevice = true, trackerStatus = mapOf()))
            )
        )

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {}
    }

    @Test
    fun loadingState_showsLoading() {
        connectedDevicesState.postValue(ConnectedDevicesState.Loading)

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {
            onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun errorState_showsError() {
        connectedDevicesState.postValue(ConnectedDevicesState.Error)

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {
            onView(withId(R.id.error_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun emptyState_displaysNothingWhenDataSourcesIsEmpty() {
        connectedDevicesState.postValue(ConnectedDevicesState.Success(emptyList()))

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {
            onView(withText("Pixel 8")).check(doesNotExist())
            onView(withText("Pixel 7 Pro")).check(doesNotExist())
        }
    }

    @Test
    fun withDevice_showsDevice() {
        connectedDevicesState.postValue(
            ConnectedDevicesState.Success(
                listOf(DeviceDataSource("Pixel 8", isCurrentDevice = true, trackerStatus = mapOf()))
            )
        )

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {
            onView(withText("Pixel 8")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun multipleDevices_showsAllDevices() {
        connectedDevicesState.postValue(
            ConnectedDevicesState.Success(
                listOf(
                    DeviceDataSource("Pixel 8", isCurrentDevice = true, trackerStatus = mapOf()),
                    DeviceDataSource(
                        "Pixel 7 Pro",
                        isCurrentDevice = false,
                        trackerStatus = mapOf(),
                    ),
                )
            )
        )

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {
            onView(withText("Pixel 8")).check(matches(isDisplayed()))
            onView(withText("Pixel 7 Pro")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun currentDevice_includesSummaryText() {
        connectedDevicesState.postValue(
            ConnectedDevicesState.Success(
                listOf(
                    DeviceDataSource("Pixel 7 Pro", isCurrentDevice = true, trackerStatus = mapOf())
                )
            )
        )

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {
            onView(withText("Current device")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun notCurrentDevice_excludesSummaryText() {
        connectedDevicesState.postValue(
            ConnectedDevicesState.Success(
                listOf(
                    DeviceDataSource(
                        "Pixel 7 Pro",
                        isCurrentDevice = false,
                        trackerStatus = mapOf(),
                    )
                )
            )
        )

        launchFragment<ConnectedDevicesFragment>(Bundle()).use {
            onView(withText("Current device")).check(doesNotExist())
        }
    }

    @Test
    fun clickDevice_navigatesToDeviceManagementFragment() {
        connectedDevicesState.postValue(
            ConnectedDevicesState.Success(
                listOf(DeviceDataSource("Pixel 8", isCurrentDevice = true, trackerStatus = mapOf()))
            )
        )
        launchFragment<ConnectedDevicesFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.connectedDevicesFragment)
                Navigation.setViewNavController(requireView(), navHostController)
            }
            .use {
                onView(withText("Pixel 8")).perform(click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.deviceManagementFragment)
            }
    }
}
