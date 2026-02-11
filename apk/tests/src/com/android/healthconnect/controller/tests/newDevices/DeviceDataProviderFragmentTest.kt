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
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.EXTRA_DATA_LABEL
import com.android.healthconnect.controller.newDevices.DeviceDataProviderFragment
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel
import com.android.healthconnect.controller.newDevices.DeviceSourcesViewModel.SelectedDeviceSourceInfoState
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.TEST_WATCH_SPN
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
class DeviceDataProviderFragmentTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: DeviceSourcesViewModel = mock()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()

    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context
    private val selectedDeviceSourceState =
        MutableStateFlow<SelectedDeviceSourceInfoState>(SelectedDeviceSourceInfoState.Loading)

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        whenever(viewModel.selectedDeviceSourceInfoState).thenReturn(selectedDeviceSourceState)
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun loadingState_showsLoading() {
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.Loading
        launchFragment<DeviceDataProviderFragment>(Bundle()).use {
            onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun errorState_showsError() {
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.Error
        launchFragment<DeviceDataProviderFragment>(Bundle()).use {
            onView(withId(R.id.error_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun errorAfterLoadingState_showsError() {
        // Regression test for b/481964580

        // Start with Error state
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.Error
        launchFragment<DeviceDataProviderFragment>(Bundle()).use {
            // Transition to Loading state
            selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.Loading
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // Transition back to Error state
            selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.Error
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withId(R.id.error_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun withData_headerDisplayed() {
        val device = createDeviceWithMultipleProviders()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use { onView(withText("My Device")).check(matches(isDisplayed())) }
    }

    @Test
    fun withData_seeDeviceDataButtonDisplayed() {
        val device = createDeviceWithMultipleProviders()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use { onView(withText("See device data")).check(matches(isDisplayed())) }
    }

    @Test
    fun withData_deviceWithNoMoreAdvertisements_onlyDeviceDataButtonDisplayed() {
        val device = createDeviceNoLongerAdvertisedButWithData()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use {
                onView(withText("My Device settings")).check(doesNotExist())
                onView(withText("See device data")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun seeDeviceData_navigatesToAppData() {
        val device = createDeviceWithMultipleProviders()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            ) {
                navHostController.setGraph(R.navigation.devices_nav_graph)
                navHostController.setCurrentDestination(R.id.deviceDataProviderFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("See device data")).perform(click())

                assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.appDataFragment)

                val args = navHostController.currentBackStackEntry?.arguments
                assertThat(args).isNotNull()

                assertThat(args?.getString(EXTRA_PACKAGE_NAME)).isEqualTo(TEST_WATCH_SPN)
                assertThat(args?.getString(EXTRA_APP_NAME)).isEqualTo("My Device")
                assertThat(args?.getInt(EXTRA_DATA_LABEL))
                    .isEqualTo(R.string.device_data_screen_title)
            }
    }

    @Test
    fun singleProvider_showsManagementButtonWithDeviceDisplayName() {
        val device = createDeviceWithSingleProvider()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use { onView(withText("My Device settings")).check(matches(isDisplayed())) }
    }

    @Test
    fun multipleProviders_showsManagementButtons() {
        val device = createDeviceWithMultipleProviders()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use {
                onView(withText("Provider 1 Settings")).check(matches(isDisplayed()))
                onView(withText("Provider 2 Settings")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun multipleProviders_showsCorrectSummary() {
        val device = createDeviceWithMultipleProviders()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use {
                // Provider 1: 1 of 1 enabled (Steps enabled=true)
                onView(withText("1 of 1 enabled")).check(matches(isDisplayed()))

                // Provider 2: 0 of 2 enabled (Steps enabled=false, Sleep enabled=false)
                onView(withText("0 of 2 enabled")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun clickSettingsButton_startsIntent() {
        val device = createDeviceWithMultipleProviders()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)
        val expectedIntent = Intent("android.intent.action.VIEW")
        expectedIntent.setPackage("com.example.provider1")

        whenever(
                healthPermissionReader.getDeviceManagementActivityIntent(
                    any(),
                    eq("com.example.provider1"),
                    eq("id1"),
                    eq(ArrayList(listOf(StepsRecord::class.java))),
                    eq(ArrayList(listOf<Int>())),
                )
            )
            .thenReturn(expectedIntent)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use {
                onView(withText("Provider 1 Settings")).perform(click())

                Intents.intended(hasPackage("com.example.provider1"))
            }
    }

    @Test
    fun clickSettingsButton_withSymptomsAdvertised_startsIntentWithCorrectExtras() {
        val device = createDeviceWithSymptoms()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)
        val expectedIntent = Intent("android.intent.action.VIEW")
        expectedIntent.setPackage("com.example.provider1")

        whenever(
                healthPermissionReader.getDeviceManagementActivityIntent(
                    any(),
                    eq("com.example.provider1"),
                    eq("id1"),
                    argThat { list: ArrayList<Class<out Record>> ->
                        list.containsAll(
                            listOf(StepsRecord::class.java, SymptomRecord::class.java)
                        ) && list.size == 2
                    },
                    argThat { list: ArrayList<Int> ->
                        list.containsAll(
                            listOf(
                                SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN,
                                SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN,
                                SymptomRecord.SYMPTOM_TYPE_FATIGUE,
                            )
                        ) && list.size == 3
                    },
                )
            )
            .thenReturn(expectedIntent)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use {
                onView(withText("My Device settings")).perform(click())

                Intents.intended(hasPackage("com.example.provider1"))
            }
    }

    @Test
    fun systemProvider_navigatesToCurrentDeviceManagement() {
        val device = createDeviceWithSystemProvider()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            ) {
                navHostController.setGraph(R.navigation.devices_nav_graph)
                navHostController.setCurrentDestination(R.id.deviceDataProviderFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("My Device settings")).perform(click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.currentDeviceManagementFragment)
                val args = navHostController.currentBackStackEntry?.arguments
                assertThat(args?.getString(EXTRA_PACKAGE_NAME)).isEqualTo(TEST_WATCH_SPN)
            }
    }

    @Test
    fun systemProvider_showsCorrectLabel() {
        val device = createDeviceWithSystemProvider()
        selectedDeviceSourceState.value = SelectedDeviceSourceInfoState.WithData(device)

        launchFragment<DeviceDataProviderFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_WATCH_SPN) }
            )
            .use { onView(withText("My Device settings")).check(matches(isDisplayed())) }
    }

    private fun createDeviceNoLongerAdvertisedButWithData(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_WATCH_SPN).build(),
            Device.Builder()
                .setDisplayName("My Device")
                .setModel("Model")
                .setManufacturer("Some Manufacturer")
                .setType(Device.DEVICE_TYPE_WATCH)
                .build(),
            true,
            listOf(),
        )
    }

    private fun createDeviceWithSingleProvider(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_WATCH_SPN).build(),
            Device.Builder()
                .setDisplayName("My Device")
                .setModel("Model")
                .setManufacturer("Some Manufacturer")
                .setType(Device.DEVICE_TYPE_WATCH)
                .build(),
            true,
            listOf(
                DeviceDataProviderInfo(
                    "com.example.provider1",
                    "id1",
                    "Provider 1 Onboarding",
                    "Provider 1 Settings",
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

    private fun createDeviceWithSymptoms(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_WATCH_SPN).build(),
            Device.Builder()
                .setDisplayName("My Device")
                .setModel("Model")
                .setManufacturer("Some Manufacturer")
                .setType(Device.DEVICE_TYPE_WATCH)
                .build(),
            true,
            listOf(
                DeviceDataProviderInfo(
                    "com.example.provider1",
                    "id1",
                    "Provider 1 Onboarding",
                    "Provider 1 Settings",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .build(),
                        DeviceDataTypeAdvertisement.Builder(SymptomRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .setSymptomType(SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN)
                            .build(),
                        DeviceDataTypeAdvertisement.Builder(SymptomRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .setSymptomType(SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN)
                            .build(),
                        DeviceDataTypeAdvertisement.Builder(SymptomRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .setSymptomType(SymptomRecord.SYMPTOM_TYPE_FATIGUE)
                            .build(),
                        DeviceDataTypeAdvertisement.Builder(SymptomRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .setSymptomType(SymptomRecord.SYMPTOM_TYPE_FATIGUE)
                            .build(),
                    ),
                )
            ),
        )
    }

    private fun createDeviceWithMultipleProviders(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_WATCH_SPN).build(),
            Device.Builder()
                .setDisplayName("My Device")
                .setModel("Model")
                .setManufacturer("Some Manufacturer")
                .setType(Device.DEVICE_TYPE_WATCH)
                .build(),
            true,
            listOf(
                DeviceDataProviderInfo(
                    "com.example.provider1",
                    "id1",
                    "Provider 1 Onboarding",
                    "Provider 1 Settings",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(true)
                            .build()
                    ),
                ),
                DeviceDataProviderInfo(
                    "com.example.provider2",
                    "id2",
                    "Provider 2 Onboarding",
                    "Provider 2 Settings",
                    setOf(
                        DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(false)
                            .build(),
                        DeviceDataTypeAdvertisement.Builder(SleepSessionRecord::class.java)
                            .setAvailable(true)
                            .setUserEnabled(false)
                            .build(),
                    ),
                ),
            ),
        )
    }

    private fun createDeviceWithSystemProvider(): DeviceDataSourceInfo {
        return DeviceDataSourceInfo(
            DataOrigin.Builder().setPackageName(TEST_WATCH_SPN).build(),
            Device.Builder()
                .setDisplayName("My Device")
                .setModel("Model")
                .setManufacturer("Some Manufacturer")
                .setType(Device.DEVICE_TYPE_WATCH)
                .build(),
            true,
            listOf(
                DeviceDataProviderInfo(
                    DEVICE_DATA_PROVIDER_PACKAGE,
                    "id_system",
                    "System Onboarding",
                    "System Settings", // Should be ignored
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
}
