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
import android.content.Intent
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.StepsRecord
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel.DeviceSourcesState
import com.android.healthconnect.controller.newDevices.DevicesFragment
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_SPN
import com.android.healthconnect.controller.tests.utils.TEST_WATCH_SPN
import com.android.healthconnect.controller.tests.utils.getDeviceDataSourcesInfo
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
class DevicesFragmentTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: DeviceSourcesViewModel = mock()

    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context
    private lateinit var deviceSourcesState: MutableStateFlow<DeviceSourcesState>

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        deviceSourcesState =
            MutableStateFlow<DeviceSourcesState>(
                DeviceSourcesState.WithData(getDeviceDataSourcesInfo())
            )
        whenever(viewModel.deviceSourcesState).thenReturn(deviceSourcesState)
    }

    @Test
    fun devicesFragment_launchable() {
        launchFragment<DevicesFragment>(Bundle()).use {}
    }

    @Test
    fun loadingState_showsLoading() {
        deviceSourcesState.value = DeviceSourcesState.Loading

        launchFragment<DevicesFragment>(Bundle()).use {
            onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun errorState_showsError() {
        deviceSourcesState.value = DeviceSourcesState.Error

        launchFragment<DevicesFragment>(Bundle()).use {
            onView(withId(R.id.error_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun emptyState_displaysNothingWhenDataSourcesIsEmpty() {
        deviceSourcesState.value = DeviceSourcesState.WithData(emptySet())

        launchFragment<DevicesFragment>(Bundle()).use {
            onView(withText("Some watch")).check(doesNotExist())
            onView(withText("Some phone")).check(doesNotExist())

            onView(withText("Enabled")).check(matches(isDisplayed()))
            onView(withText("Not enabled")).check(doesNotExist())
        }
    }

    @Test
    fun withDevices_showsAllDevices() {
        launchFragment<DevicesFragment>(Bundle()).use {
            onView(withText("Some watch")).check(matches(isDisplayed()))
            onView(withText("Some phone")).check(matches(isDisplayed()))

            onView(withText("Enabled")).check(matches(isDisplayed()))
            onView(withText("Not enabled")).check(doesNotExist())
        }
    }

    @Test
    fun currentDevice_includesSummaryText() {
        val currentDeviceInfo = getDeviceDataSourcesInfo().filter { it.isCurrentDevice }.toSet()
        deviceSourcesState.value = DeviceSourcesState.WithData(currentDeviceInfo)

        launchFragment<DevicesFragment>(Bundle()).use {
            onView(withText("This phone")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun notCurrentDevice_ignoresSummaryText() {
        val notCurrentDeviceInfo = getDeviceDataSourcesInfo().filter { !it.isCurrentDevice }.toSet()
        deviceSourcesState.value = DeviceSourcesState.WithData(notCurrentDeviceInfo)

        launchFragment<DevicesFragment>(Bundle()).use {
            onView(withText("This phone")).check(doesNotExist())
        }
    }

    @Test
    fun clickCurrentDevice_singleSystemProvider_navigatesToCurrentDeviceManagementFragment() {
        launchFragment<DevicesFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.newDevicesFragment)
                Navigation.setViewNavController(requireView(), navHostController)
            }
            .use {
                onView(withText("Some phone")).perform(click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.currentDeviceManagementFragment)

                val arguments = navHostController.currentBackStackEntry?.arguments
                assertThat(arguments).isNotNull()

                assertThat(arguments?.getString(Intent.EXTRA_PACKAGE_NAME))
                    .isEqualTo(TEST_PHONE_SPN)
            }
    }

    @Test
    fun clickCurrentDevice_multipleProvidersIncludingSystem_navigatesToDeviceDataProviderFragment() {
        val deviceDataSourceInfo = createCurrentDeviceWithMultipleProviders(true)
        deviceSourcesState.value = DeviceSourcesState.WithData(setOf(deviceDataSourceInfo))

        launchFragment<DevicesFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.newDevicesFragment)
                Navigation.setViewNavController(requireView(), navHostController)
            }
            .use {
                onView(withText("Some phone")).perform(click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.deviceDataProviderFragment)

                val arguments = navHostController.currentBackStackEntry?.arguments
                assertThat(arguments).isNotNull()

                assertThat(arguments?.getString(Intent.EXTRA_PACKAGE_NAME))
                    .isEqualTo(TEST_PHONE_SPN)
            }
    }

    @Test
    fun clickCurrentDevice_multipleProvidersWithoutSystem_navigatesToDeviceDataProviderFragment() {
        val deviceDataSourceInfo = createCurrentDeviceWithMultipleProviders(false)
        deviceSourcesState.value = DeviceSourcesState.WithData(setOf(deviceDataSourceInfo))

        launchFragment<DevicesFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.newDevicesFragment)
                Navigation.setViewNavController(requireView(), navHostController)
            }
            .use {
                onView(withText("Some phone")).perform(click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.deviceDataProviderFragment)

                val arguments = navHostController.currentBackStackEntry?.arguments
                assertThat(arguments).isNotNull()

                assertThat(arguments?.getString(Intent.EXTRA_PACKAGE_NAME))
                    .isEqualTo(TEST_PHONE_SPN)
            }
    }

    @Test
    fun clickNotCurrentDevice_navigatesToDeviceDataProviderFragment() {
        val deviceDataSourceInfo = createDisabledDevice()
        deviceSourcesState.value = DeviceSourcesState.WithData(setOf(deviceDataSourceInfo))

        launchFragment<DevicesFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.newDevicesFragment)
                Navigation.setViewNavController(requireView(), navHostController)
            }
            .use {
                onView(withText("Disabled Device")).perform(scrollTo(), click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.deviceDataProviderFragment)

                val arguments = navHostController.currentBackStackEntry?.arguments
                assertThat(arguments).isNotNull()

                assertThat(arguments?.getString(Intent.EXTRA_PACKAGE_NAME))
                    .isEqualTo(TEST_WATCH_SPN)
            }
    }

    @Test
    fun disabledDevice_showsInNotEnabledCategory() {
        deviceSourcesState.value =
            DeviceSourcesState.WithData(setOf(createCurrentDevice(), createDisabledDevice()))

        launchFragment<DevicesFragment>(Bundle()).use { scenario ->
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as DevicesFragment
                val enabledCategory =
                    fragment.preferenceScreen.findPreference("enabled_devices_category")
                        as PreferenceCategory?
                val notEnabledPreference =
                    fragment.preferenceScreen.findPreference("not_enabled_devices_category")
                        as PreferenceCategory?

                assertThat(notEnabledPreference?.preferenceCount).isEqualTo(1)
                assertThat(enabledCategory?.preferenceCount).isEqualTo(1)

                notEnabledPreference?.children?.firstOrNull().let { child ->
                    assertThat(child?.title).isEqualTo("Disabled Device")
                }

                enabledCategory?.children?.firstOrNull().let { child ->
                    assertThat(child?.title).isEqualTo("Some phone")
                }
            }
        }
    }

    private fun createCurrentDevice(): DeviceDataSourceInfo {
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
                            .build()
                    ),
                )
            ),
        )
    }

    private fun createCurrentDeviceWithMultipleProviders(
        includeSystemProvider: Boolean
    ): DeviceDataSourceInfo {
        val providers = mutableListOf<DeviceDataProviderInfo>()
        providers.add(
            DeviceDataProviderInfo(
                "com.example.provider1",
                "id1",
                "",
                "",
                setOf(
                    DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .build()
                ),
            )
        )
        if (includeSystemProvider) {
            providers.add(
                DeviceDataProviderInfo(
                    DEVICE_DATA_PROVIDER_PACKAGE,
                    "phoneId",
                    "",
                    "",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .build()
                    ),
                )
            )
        } else {
            providers.add(
                DeviceDataProviderInfo(
                    "com.example.provider2",
                    "id2",
                    "",
                    "",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .build()
                    ),
                )
            )
        }
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
            Device.Builder()
                .setDisplayName("Some phone")
                .setModel("Some model")
                .setManufacturer("Some manufacturer")
                .setType(Device.DEVICE_TYPE_PHONE)
                .build(),
            true,
            providers,
        )
    }

    private fun createDisabledDevice(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_WATCH_SPN).build(),
            Device.Builder()
                .setDisplayName("Disabled Device")
                .setModel("Some model")
                .setManufacturer("Some manufacturer")
                .setType(Device.DEVICE_TYPE_WATCH)
                .build(),
            false,
            listOf(
                DeviceDataProviderInfo(
                    "some.package",
                    "id",
                    "",
                    "",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(false)
                            .build()
                    ),
                )
            ),
        )
    }
}
