/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.datasources

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthDataCategory
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
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
import com.android.healthconnect.controller.datasources.AddAnAppFragment
import com.android.healthconnect.controller.datasources.DataSourcesViewModel
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.DataSourcesInfo
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.DeviceDataSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PotentialAppSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PriorityListState
import com.android.healthconnect.controller.navigation.CATEGORY_KEY
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_APP
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_3
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_APP
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_WATCH_SPN
import com.android.healthconnect.controller.tests.utils.getDeviceDataSourcesInfo
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.logging.AddAnAppElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddAnAppFragmentTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    @BindValue val dataSourcesViewModel: DataSourcesViewModel = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        navHostController =
            TestNavHostController(InstrumentationRegistry.getInstrumentation().context)
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun fragmentIsDisplayed_ddpFlagsOff() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, TEST_APP_3),
                        ),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(getDeviceDataSourcesInfo()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_3)).check(matches(isDisplayed()))

                verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ADD_AN_APP_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger, times(3))
                    .logImpression(AddAnAppElement.POTENTIAL_PRIORITY_APP_BUTTON)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun fragmentIsDisplayed_ddpFlagsOn() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, TEST_APP_3, TEST_PHONE_APP),
                        ),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(getDeviceDataSourcesInfo()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_3)).check(matches(isDisplayed()))
                onView(withText(TEST_PHONE_APP_NAME)).check(matches(isDisplayed()))

                verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ADD_AN_APP_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger, times(4))
                    .logImpression(AddAnAppElement.POTENTIAL_PRIORITY_APP_BUTTON)
            }
    }

    @Test
    fun showsLoading_whenAppSourcesLoading() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState = PotentialAppSourcesState.Loading(true),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(setOf<DeviceDataSourceInfo>()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(ViewMatchers.withId(R.id.progress_indicator)).check(matches(isDisplayed()))
            }
    }

    @Test
    fun showsError_whenAppSourcesError() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState = PotentialAppSourcesState.LoadingFailed(true),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(setOf<DeviceDataSourceInfo>()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use { onView(ViewMatchers.withId(R.id.error_view)).check(matches(isDisplayed())) }
    }

    @Test
    fun addAnApp_navigatesBackToDataSourcesFragment() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, TEST_APP_3),
                        ),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(setOf<DeviceDataSourceInfo>()),
                )
            )
        }

        launchFragmentWithNavigation().use {
            onView(withText(TEST_APP_NAME_2)).perform(click())
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.dataSourcesFragment)
            verify(healthConnectLogger)
                .logInteraction(AddAnAppElement.POTENTIAL_PRIORITY_APP_BUTTON)
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun showsCurrentDevice_whenDisplayingLegacyDdpPackage_displaysSummary() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, DEVICE_DATA_PROVIDER_APP, TEST_PHONE_APP),
                        ),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(getDeviceDataSourcesInfo()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))

                onView(withText(DEVICE_DATA_PROVIDER_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(R.string.devices_current_device)).check(matches(isDisplayed()))

                onView(withText(TEST_PHONE_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(R.string.devices_this_phone)).check(doesNotExist())
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun showsCurrentDevice_whenDisplayingCurrentDeviceSpn_displaysSummary() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, TEST_PHONE_APP),
                        ),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(getDeviceDataSourcesInfo()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))

                onView(withText(TEST_PHONE_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(R.string.devices_this_phone)).check(matches(isDisplayed()))
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun whenDisplayingDeviceSpn_withDdpFlags_notTheCurrentDevice_doesNotAddSummary() {
        val watchApp = AppMetadata(packageName = TEST_WATCH_SPN, appName = "A watch", icon = null)

        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, watchApp),
                        ),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(getDeviceDataSourcesInfo()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))

                onView(withText(watchApp.appName)).check(matches(isDisplayed()))
                onView(withText(R.string.devices_this_phone)).check(doesNotExist())
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun whenDisplayingLegacyDdp_withDdpFlags_noCurrentDeviceSource_addsSummary() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, DEVICE_DATA_PROVIDER_APP),
                        ),
                    deviceDataSourcesState =
                        // Device sources without a current device
                        DeviceDataSourcesState.WithData(
                            getDeviceDataSourcesInfo().filterNot { it.isCurrentDevice }.toSet()
                        ),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))

                onView(withText(DEVICE_DATA_PROVIDER_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(R.string.devices_current_device)).check(matches(isDisplayed()))
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun showsCurrentDevice_whenDisplayingLegacyAndSpnDeviceIdentifier_displaysSummaryForBoth() {
        whenever(dataSourcesViewModel.dataSourcesInfo).then {
            MutableLiveData(
                DataSourcesInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(
                            true,
                            listOf(TEST_APP, TEST_APP_2, DEVICE_DATA_PROVIDER_APP, TEST_PHONE_APP),
                        ),
                    deviceDataSourcesState =
                        DeviceDataSourcesState.WithData(getDeviceDataSourcesInfo()),
                )
            )
        }

        launchFragment<AddAnAppFragment>(
                Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
            )
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))

                onView(withText(DEVICE_DATA_PROVIDER_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(R.string.devices_current_device)).check(matches(isDisplayed()))

                onView(withText(TEST_PHONE_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(R.string.devices_this_phone)).check(matches(isDisplayed()))
            }
    }

    private fun launchFragmentWithNavigation(): ActivityScenario<TestActivity> =
        launchFragment<AddAnAppFragment>(
            Bundle().apply { putInt(CATEGORY_KEY, HealthDataCategory.ACTIVITY) }
        ) {
            navHostController.setGraph(R.navigation.data_sources_nav_graph)
            navHostController.setCurrentDestination(R.id.addAnAppFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
}
