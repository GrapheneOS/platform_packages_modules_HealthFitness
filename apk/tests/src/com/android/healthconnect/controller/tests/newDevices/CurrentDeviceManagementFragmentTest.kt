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

import android.content.Context
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.newDevices.CurrentDeviceManagementFragment
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel.SelectedDeviceSourceInfoState
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_SPN
import com.android.healthconnect.controller.tests.utils.getDeviceDataSourcesInfo
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.ToastManager
import com.android.healthconnect.controller.utils.ToastManagerModule
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(ToastManagerModule::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
class CurrentDeviceManagementFragmentTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: DeviceSourcesViewModel = mock()

    @BindValue var toastManager: ToastManager = mock()

    private lateinit var phoneDevice: DeviceDataSourceInfo
    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context
    private lateinit var selectedDeviceSourceState: MutableStateFlow<SelectedDeviceSourceInfoState>

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        phoneDevice =
            getDeviceDataSourcesInfo().first { it.device.type == Device.DEVICE_TYPE_PHONE }
        selectedDeviceSourceState =
            MutableStateFlow<SelectedDeviceSourceInfoState>(
                SelectedDeviceSourceInfoState.WithData(phoneDevice)
            )
        whenever(viewModel.selectedDeviceSourceInfoState).thenReturn(selectedDeviceSourceState)
    }

    @Test
    fun loadingState_showsLoading() {
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.Loading

        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun errorState_showsError() {
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.Error

        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withId(R.id.error_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun noSystemInfo_displaysError() {
        val noSystemDevice = createNonSystemDevice()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(noSystemDevice)

        launchCurrentDeviceManagementFragment(noSystemDevice).use {
            onView(withText("Something went wrong. Please try again."))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun noPedometerBanner_hasSensor_isNotDisplayed() {
        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("This device doesn't track steps")).check(doesNotExist())
        }
    }

    @Test
    fun noPedometerBanner_hasNoSensor_isDisplayed() {
        val noSensorDevice = createCurrentDeviceWithNoPedometer()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(noSensorDevice)

        launchCurrentDeviceManagementFragment(noSensorDevice).use {
            onView(withText("This device doesn't track steps")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun header_isDisplayed() {
        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("Some phone")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun stepTrackingSwitch_hasSensor_isDisplayedAndEnabled() {
        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("Steps")).check(matches(isDisplayed()))
            onView(withText("Steps")).check(matches(isEnabled()))
        }
    }

    @Test
    fun stepTrackingSwitch_hasNoSensor_isDisplayedAndDisabled() {
        val noSensorDevice = createCurrentDeviceWithNoPedometer()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(noSensorDevice)

        launchCurrentDeviceManagementFragment(noSensorDevice).use {
            onView(withText("Steps")).check(matches(isDisplayed()))
            onView(withText("Steps")).check(matches(isNotEnabled()))
        }
    }

    @Test
    fun stepTrackingSwitch_whenClicked_callsViewModel() {
        runBlocking { doReturn(true).whenever(viewModel).setNativeTrackingEnabled(any(), any()) }

        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("Steps")).perform(click())
            verifyBlocking(viewModel) { setNativeTrackingEnabled(StepsRecord::class.java, false) }
        }
    }

    @Test
    fun stepTrackingSwitch_whenClickedAndSuccess_changesToggle() {
        runBlocking { doReturn(true).whenever(viewModel).setNativeTrackingEnabled(any(), any()) }

        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("Steps")).perform(click())

            verifyBlocking(viewModel) { setNativeTrackingEnabled(StepsRecord::class.java, false) }

            it.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("")
                        as CurrentDeviceManagementFragment
                val stepsCheckboxPreference =
                    fragment.preferenceScreen.findPreference(StepsRecord::class.java.name)
                        as HealthSwitchPreference?
                assertThat(stepsCheckboxPreference?.isChecked).isFalse()
            }

            onView(withText("Steps")).perform(click())
            verifyBlocking(viewModel) { setNativeTrackingEnabled(StepsRecord::class.java, true) }

            it.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("")
                        as CurrentDeviceManagementFragment
                val stepsCheckboxPreference =
                    fragment.preferenceScreen.findPreference(StepsRecord::class.java.name)
                        as HealthSwitchPreference?
                assertThat(stepsCheckboxPreference?.isChecked).isTrue()
            }
        }
    }

    @Test
    fun stepTrackingSwitch_whenClickedAndFailed_doesNotToggle() {
        runBlocking { doReturn(false).whenever(viewModel).setNativeTrackingEnabled(any(), any()) }

        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("Steps")).perform(click())

            verifyBlocking(viewModel) { setNativeTrackingEnabled(StepsRecord::class.java, false) }

            it.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("")
                        as CurrentDeviceManagementFragment
                val stepsCheckboxPreference =
                    fragment.preferenceScreen.findPreference(StepsRecord::class.java.name)
                        as HealthSwitchPreference?
                assertThat(stepsCheckboxPreference?.isChecked).isTrue()
            }
        }
    }

    @Test
    fun stepTrackingSwitch_whenClickedAndFails_displaysToast() {
        runBlocking { doReturn(false).whenever(viewModel).setNativeTrackingEnabled(any(), any()) }

        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("Steps")).perform(click())
            verifyBlocking(viewModel) { setNativeTrackingEnabled(StepsRecord::class.java, false) }
            it.onActivity { activity: TestActivity ->
                verify(toastManager).showToast(any(), eq(R.string.default_error), any())
            }
        }
    }

    @Test
    fun withMoreTypeAds_displaysAllAsSwitches() {
        val multipleTypeDevice = createCurrentDeviceWithMultipleTypeAds()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(multipleTypeDevice)

        launchCurrentDeviceManagementFragment(multipleTypeDevice).use {
            onView(withText("Steps")).check(matches(isDisplayed()))
            onView(withText("Steps")).check(matches(isEnabled()))

            onView(
                    allOf(
                        isAssignableFrom(android.widget.CompoundButton::class.java),
                        withParent(hasSibling(hasDescendant(withText("Steps")))),
                    )
                )
                .check(matches(isChecked()))

            onView(withText("Sleep")).check(matches(isDisplayed()))
            onView(withText("Sleep")).check(matches(isEnabled()))
            onView(
                    allOf(
                        isAssignableFrom(android.widget.CompoundButton::class.java),
                        withParent(hasSibling(hasDescendant(withText("Sleep")))),
                    )
                )
                .check(matches(isNotChecked()))

            onView(withText("Total calories burned")).check(matches(isDisplayed()))
            onView(withText("Total calories burned")).check(matches(isNotEnabled()))
            onView(
                    allOf(
                        isAssignableFrom(android.widget.CompoundButton::class.java),
                        withParent(hasSibling(hasDescendant(withText("Total calories burned")))),
                    )
                )
                .check(matches(isNotChecked()))
        }
    }

    @Test
    fun seeDeviceData_isDisplayed() {
        launchCurrentDeviceManagementFragment(phoneDevice).use {
            onView(withText("See device data")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun seeDeviceData_navigatesToAppData() {
        launchFragment<CurrentDeviceManagementFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, phoneDevice.deviceDataOrigin.packageName)
                }
            ) {
                navHostController.setGraph(R.navigation.devices_nav_graph)
                navHostController.setCurrentDestination(R.id.currentDeviceManagementFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("See device data")).perform(click())
                assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.appDataFragment)
            }
    }

    @Test
    fun footer_hasSensor_displaysCorrectText() {
        launchCurrentDeviceManagementFragment(phoneDevice).use {
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
        val noSensorDevice = createCurrentDeviceWithNoPedometer()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(noSensorDevice)

        launchCurrentDeviceManagementFragment(noSensorDevice).use {
            onView(
                    withText(
                        "This device doesn't support step tracking, but still has data stored in Health\u00A0Connect"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }

    private fun createCurrentDeviceWithNoPedometer(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
            Device.Builder()
                .setDisplayName("Some phone")
                .setModel("Some model")
                .setManufacturer("Some manufacturer")
                .setType(Device.DEVICE_TYPE_PHONE)
                .build(),
            true,
            listOf(
                DeviceDataProviderInfo(
                    DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                    "phoneId",
                    "",
                    "",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(false)
                            .setUserEnabled(true)
                            .build()
                    ),
                )
            ),
        )
    }

    private fun createCurrentDeviceWithMultipleTypeAds(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
            Device.Builder()
                .setDisplayName("Some phone")
                .setModel("Some model")
                .setManufacturer("Some manufacturer")
                .setType(Device.DEVICE_TYPE_PHONE)
                .build(),
            true,
            listOf(
                DeviceDataProviderInfo(
                    DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                    "phoneId",
                    "",
                    "",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .build(),
                        DeviceDataTypeAdvertisement.Builder(SleepSessionRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(false)
                            .build(),
                        DeviceDataTypeAdvertisement.Builder(TotalCaloriesBurnedRecord::class.java)
                            .setAvailable(false)
                            .setUserEnabled(false)
                            .build(),
                    ),
                )
            ),
        )
    }

    private fun createNonSystemDevice(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
            Device.Builder()
                .setDisplayName("Some phone")
                .setModel("Some model")
                .setManufacturer("Some manufacturer")
                .setType(Device.DEVICE_TYPE_PHONE)
                .build(),
            true,
            listOf(
                DeviceDataProviderInfo(
                    "some.package",
                    "id",
                    "",
                    "",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(false)
                            .setUserEnabled(true)
                            .build()
                    ),
                )
            ),
        )
    }

    private fun launchCurrentDeviceManagementFragment(
        deviceDataSourceInfo: DeviceDataSourceInfo
    ): ActivityScenario<TestActivity> {
        return launchFragment<CurrentDeviceManagementFragment>(
            Bundle().apply {
                putString(EXTRA_PACKAGE_NAME, deviceDataSourceInfo.deviceDataOrigin.packageName)
            }
        )
    }
}
