/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache personche License, Version 2.0 (the "License");
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
package com.android.healthconnect.controller.tests.home

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.ACTION_SHOW_ONBOARDING
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.home.HomeFragment
import com.android.healthconnect.controller.home.HomeViewModel
import com.android.healthconnect.controller.home.HomeViewModel.BannerData
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_4
import com.android.healthconnect.controller.tests.utils.TEST_APP_5
import com.android.healthconnect.controller.tests.utils.TEST_APP_6
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.checkTextIsDisplayed
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.scrollToBottomOfPreferenceScreen
import com.android.healthconnect.controller.tests.utils.scrollToTextAndClick
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.SettingsTransitionHelper.createMainlineServiceUpdateSettingsIntent
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HomePageElement
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.NewHomePageElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(DeviceInfoUtilsModule::class)
@RunWith(AndroidJUnit4::class)
class HomeFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val homeViewModel: HomeViewModel = mock()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()

    private lateinit var context: Context
    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        navHostController = TestNavHostController(context)
        Intents.init()
        (deviceInfoUtils as FakeDeviceInfoUtils).setIntentHandlerAvailability(true)
        deviceInfoUtils.setPlayStoreAvailability(true)
        deviceInfoUtils.setSendFeedbackAvailability(true)
    }

    @After
    fun tearDown() {
        Intents.release()
        reset(healthConnectLogger)
    }

    // region General display
    @Test
    fun whenLoading_showsLoading() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.Loading
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragmentWithNavigation().use {
            onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun whenError_showsError() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(HomeViewModel.HomeFragmentState.Error)
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragment<HomeFragment>(Bundle()).use {
            onView(withId(R.id.error_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun withData_showsAllSections() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps =
                        listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)),
                    showSeeMoreHealthApps = true,
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(
                                BannerData.LockScreenBanner(true, true),
                                BannerData.MigrationBanner,
                            )
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Set a screen lock")
            checkTextIsDisplayed("More items")
            checkTextIsDisplayed("Your health apps")
            checkTextIsDisplayed("Health Connect test app")
            checkTextIsDisplayed("See more health apps")
            checkTextIsDisplayed("Your health data")
            checkTextIsDisplayed("Data and access")
            checkTextIsDisplayed("Recent access")
            checkTextIsDisplayed("Preferences")
            checkTextIsDisplayed("Manage data")
            checkTextIsDisplayed(
                "Health Connect lets you share your health and fitness data between " +
                    "multiple data sources. This helps you unlock insights and experiences while " +
                    "keeping your data secure."
            )
            checkTextIsDisplayed("More about Health Connect")
            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.NEW_HOME_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger)
                .logImpression(NewHomePageElement.CONNECTED_APP_HOME_SCREEN_BUTTON)
            verify(healthConnectLogger).logImpression(NewHomePageElement.DATA_AND_ACCESS_BUTTON)
            verify(healthConnectLogger).logImpression(NewHomePageElement.RECENT_ACCESS_BUTTON)
            verify(healthConnectLogger).logImpression(NewHomePageElement.MANAGE_DATA_BUTTON)
            verify(healthConnectLogger).logImpression(NewHomePageElement.HOME_PAGE_FOOTER)
        }
    }

    // endregion

    // region Your Health Apps
    @Test
    fun whenNoApps_andPlayStoreAvailable_showsNoAppsPreferenceWithLink() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(true)
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = emptyList())
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("Install apps that work with Health Connect to " + "see them here")
            checkTextIsDisplayed("See compatible apps")
            verify(healthConnectLogger).logImpression(NewHomePageElement.NO_APPS_AVAILABLE_HEADER)
            verify(healthConnectLogger).logImpression(NewHomePageElement.NO_APPS_AVAILABLE_LINK)
        }
    }

    @Test
    fun whenNoApps_andPlayStoreNotAvailable_showsNoAppsPreferenceWithoutLink() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(false)
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(emptyList())
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("Install apps that work with Health Connect to " + "see them here")
            onView(withText("See compatible apps")).check(doesNotExist())
            verify(healthConnectLogger).logImpression(NewHomePageElement.NO_APPS_AVAILABLE_HEADER)
            verify(healthConnectLogger, never())
                .logImpression(NewHomePageElement.NO_APPS_AVAILABLE_LINK)
        }
    }

    @Test
    fun whenEmptyState_showsSeeMoreButton() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = listOf())
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragment<HomeFragment>(Bundle()).use { checkTextIsDisplayed("See more health apps") }
    }

    @Test
    fun whenThreeApps_showsThreeApps_andSeeMoreButton() {
        val apps =
            listOf(
                ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED),
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed(TEST_APP.appName)
            checkTextIsDisplayed(TEST_APP_2.appName)
            checkTextIsDisplayed(TEST_APP_3.appName)
            checkTextIsDisplayed("See more health apps")
            verify(healthConnectLogger, times(3))
                .logImpression(NewHomePageElement.CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenSixApps_showsFiveApps_andSeeMoreButton() {
        val apps =
            listOf(
                ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.DENIED),
                ConnectedAppMetadata(TEST_APP_6, ConnectedAppStatus.ALLOWED),
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed(TEST_APP.appName)
            checkTextIsDisplayed(TEST_APP_2.appName)
            checkTextIsDisplayed(TEST_APP_3.appName)
            checkTextIsDisplayed(TEST_APP_4.appName)
            checkTextIsDisplayed(TEST_APP_5.appName)
            onView(withText(TEST_APP_6.appName)).check(doesNotExist())
            checkTextIsDisplayed("See more health apps")
            verify(healthConnectLogger, times(4))
                .logImpression(NewHomePageElement.CONNECTED_APP_HOME_SCREEN_BUTTON)
            verify(healthConnectLogger)
                .logImpression(NewHomePageElement.NOT_CONNECTED_APP_HOME_SCREEN_BUTTON)
            verify(healthConnectLogger)
                .logImpression(NewHomePageElement.SEE_ALL_CONNECTED_APPS_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenClickOnConnectedFitnessApp_navigatesToFitnessAppPermissions() {
        val apps =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    ConnectedAppStatus.ALLOWED,
                    permissionsType = AppPermissionsType.FITNESS_PERMISSIONS_ONLY,
                ),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragmentWithNavigation().use {
            scrollToTextAndClick(TEST_APP_NAME)
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.fitnessAppFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenClickOnConnectedMedicalApp_navigatesToMedicalAppPermissions() {
        val apps =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    ConnectedAppStatus.ALLOWED,
                    permissionsType = AppPermissionsType.MEDICAL_PERMISSIONS_ONLY,
                ),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragmentWithNavigation().use {
            scrollToTextAndClick(TEST_APP_NAME)
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.medicalAppFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenClickOnConnectedCombinedApp_navigatesToCombinedAppPermissions() {
        val apps =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    ConnectedAppStatus.ALLOWED,
                    permissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
                ),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragmentWithNavigation().use {
            scrollToTextAndClick(TEST_APP_NAME)
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.combinedPermissionsFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenClickOnDisconnectedAppWithOnboarding_launchesOnboardingIntent() {
        val apps = listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.DENIED))
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        val testIntent = Intent(ACTION_SHOW_ONBOARDING)
        testIntent.setPackage(TEST_APP.packageName)

        // Assume that the client onboarding activity completes normally.
        Intents.intending(hasAction(ACTION_SHOW_ONBOARDING))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(testIntent)

        launchFragmentWithNavigation().use {
            scrollToTextAndClick(TEST_APP_NAME)
            intended(hasAction(ACTION_SHOW_ONBOARDING))
            intended(hasPackage(TEST_APP_PACKAGE_NAME))
            // We should remain where we started.
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.newHomeFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.NOT_CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenClickOnDisconnectedFitnessAppWithoutOnboarding_navigatesToFitnessPermissions() {
        val apps =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    ConnectedAppStatus.DENIED,
                    permissionsType = AppPermissionsType.FITNESS_PERMISSIONS_ONLY,
                ),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(null)
        launchFragmentWithNavigation().use {
            scrollToTextAndClick(TEST_APP_NAME)
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.fitnessAppFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.NOT_CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenClickOnDisconnectedMedicalAppWithoutOnboarding_navigatesToMedicalPermissions() {
        val apps =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    ConnectedAppStatus.DENIED,
                    permissionsType = AppPermissionsType.MEDICAL_PERMISSIONS_ONLY,
                )
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(null)

        launchFragmentWithNavigation().use {
            scrollToTextAndClick(TEST_APP_NAME)
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.medicalAppFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.NOT_CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun whenClickOnDisconnectedCombinedAppWithoutOnboarding_navigatesToCombinedPermissions() {
        val apps =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    ConnectedAppStatus.DENIED,
                    permissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
                )
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(null)
        launchFragmentWithNavigation().use {
            scrollToTextAndClick(TEST_APP_NAME)
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.combinedPermissionsFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.NOT_CONNECTED_APP_HOME_SCREEN_BUTTON)
        }
    }

    // endregion

    // Navigation
    @Test
    fun dataAndAccess_navigatesToDataAndAccess() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Data and access")
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.data_activity)
            verify(healthConnectLogger).logInteraction(NewHomePageElement.DATA_AND_ACCESS_BUTTON)
        }
    }

    @Test
    fun recentAccess_navigatesToRecentAccess() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Recent access")
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.recentAccessFragment)
            verify(healthConnectLogger).logInteraction(NewHomePageElement.RECENT_ACCESS_BUTTON)
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun devices_ddpFlagOff_navigatesToConnectedDevices() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Devices")
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.connectedDevicesFragment)
            verify(healthConnectLogger).logImpression(NewHomePageElement.DEVICES_BUTTON)
            verify(healthConnectLogger).logInteraction(NewHomePageElement.DEVICES_BUTTON)
        }
    }

    @Test
    fun devices_ddpFlagOn_navigatesToDeviceSources() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Devices")
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.newDevicesFragment)
            verify(healthConnectLogger).logImpression(NewHomePageElement.DEVICES_BUTTON)
            verify(healthConnectLogger).logInteraction(NewHomePageElement.DEVICES_BUTTON)
        }
    }

    @Test
    fun manageData_navigatesToManageData() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Manage data")
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.manageDataFragment)
            verify(healthConnectLogger).logInteraction(NewHomePageElement.MANAGE_DATA_BUTTON)
        }
    }

    @Test
    fun seeCompatibleApps_navigatesToPlayStore() {
        setupFragmentForNavigation().use {
            onView(withText("See compatible apps")).perform(click())
            onIdle()
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.playstore_activity)
            verify(healthConnectLogger).logInteraction(NewHomePageElement.NO_APPS_AVAILABLE_LINK)
        }
    }

    @Test
    fun seeAllApps_navigatesToConnectedAppsFragment() {
        val apps =
            listOf(
                ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_6, ConnectedAppStatus.ALLOWED),
            )
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        launchFragmentWithNavigation().use {
            onView(withText("See more health apps")).perform(click())
            onIdle()
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.connectedAppsFragment)
            verify(healthConnectLogger)
                .logInteraction(NewHomePageElement.SEE_ALL_CONNECTED_APPS_HOME_SCREEN_BUTTON)
        }
    }

    @Test
    fun moreAboutHealthConnect_navigatesToHelpPage() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setSendFeedbackAvailability(true)

        setupFragmentForNavigation().use {
            scrollToBottomOfPreferenceScreen()
            onView(withText("More about Health\u00A0Connect")).perform(click())
            verify(healthConnectLogger).logInteraction(NewHomePageElement.HOME_PAGE_FOOTER_LINK)
        }
    }

    // endregion

    // region Banner tests
    @Test
    fun exportBanner_displaysCorrectly_impressionsLogged() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.ExportErrorBanner(NOW))
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Couldn\'t export data")
            checkTextIsDisplayed(
                "There was a problem with the export for October 20, 2022. " +
                    "Please set up a new scheduled export and try again."
            )
            checkTextIsDisplayed("Set up")
            verify(healthConnectLogger).logImpression(HomePageElement.EXPORT_ERROR_BANNER)
            verify(healthConnectLogger).logImpression(HomePageElement.EXPORT_ERROR_BANNER_BUTTON)
        }
    }

    @Test
    fun exportBanner_whenClickOnSetUp_navigatesToExportActivity() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.ExportErrorBanner(NOW))
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Couldn\'t export data")
            checkTextIsDisplayed(
                "There was a problem with the export for October 20, 2022. " +
                    "Please set up a new scheduled export and try again."
            )
            onView(withText("Set up")).perform(click())
            onIdle()
            verify(healthConnectLogger).logInteraction(HomePageElement.EXPORT_ERROR_BANNER_BUTTON)
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.exportSetupActivity)
        }
    }

    @Test
    fun migrationBanner_displaysCorrectly_impressionsLogged() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.MigrationBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Resume integration")
            checkTextIsDisplayed(
                "Tap to continue integrating Health Connect with the Android system."
            )
            checkTextIsDisplayed("Continue")
            verify(healthConnectLogger).logImpression(MigrationElement.MIGRATION_RESUME_BANNER)
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_RESUME_BANNER_BUTTON)
        }
    }

    @Test
    fun migrationBanner_whenClickOnContinueButton_navigatesToMigrationActivity() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.MigrationBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Resume integration")
            checkTextIsDisplayed(
                "Tap to continue integrating Health Connect with the Android system."
            )
            onView(withText("Continue")).perform(click())
            onIdle()
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_RESUME_BANNER_BUTTON)
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.migrationActivity)
        }
    }

    @Test
    fun dataRestoreBanner_displaysCorrectly_impressionsLogged() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.DataRestorePendingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Update needed")
            checkTextIsDisplayed("To continue restoring your data, update your device system.")
            checkTextIsDisplayed("Check for updates")
            verify(healthConnectLogger).logImpression(DataRestoreElement.RESTORE_PENDING_BANNER)
            verify(healthConnectLogger)
                .logImpression(DataRestoreElement.RESTORE_PENDING_BANNER_UPDATE_BUTTON)
        }
    }

    @Test
    fun dataRestoreBanner_whenClickOnCheckForUpdates_navigatesToSystemUpdateActivity() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.DataRestorePendingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Update needed")
            checkTextIsDisplayed("To continue restoring your data, update your device system.")
            onView(withText("Check for updates")).perform(click())
            onIdle()
            verify(healthConnectLogger)
                .logInteraction(DataRestoreElement.RESTORE_PENDING_BANNER_UPDATE_BUTTON)
            intended(hasAction(context.createMainlineServiceUpdateSettingsIntent().action))
        }
    }

    @Test
    fun lockScreenBanner_displaysCorrectly_impressionsLogged() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.LockScreenBanner(true, true))
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Set a screen lock")
            onView(
                    withText(
                        "For added security for your health data, set a PIN, pattern, or password for this device"
                    )
                )
                .check(matches(isDisplayed()))
            checkTextIsDisplayed("Set screen lock")
            checkTextIsDisplayed("Not now")
            verify(healthConnectLogger).logImpression(HomePageElement.LOCK_SCREEN_BANNER)
            verify(healthConnectLogger).logImpression(HomePageElement.LOCK_SCREEN_BANNER_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.LOCK_SCREEN_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun lockScreenBanner_whenClickOnSetScreenLock_navigatesToSecuritySettings() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.LockScreenBanner(true, true))
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use { scenario ->
            checkTextIsDisplayed(
                "For added security for your health data, set a PIN, pattern, or password for this device"
            )
            onView(withText("Set screen lock")).perform(click())
            onIdle()
            intended(hasAction("android.settings.SECURITY_SETTINGS"))
            verify(healthConnectLogger).logInteraction(HomePageElement.LOCK_SCREEN_BANNER_BUTTON)
            verify(homeViewModel, never())
                .onDismissBanner(eq(BannerData.LockScreenBanner(true, true)))
            scenario.moveToState(Lifecycle.State.DESTROYED)
            onIdle()
            verify(homeViewModel).onDismissBanner(eq(BannerData.LockScreenBanner(true, true)))
        }
    }

    @Test
    fun lockScreenBanner_whenClickOnNotNow_dismissesBanner_setsBannerSeen() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.LockScreenBanner(true, true))
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed(
                "For added security for your health data, set a PIN, pattern, or password for this device"
            )
            onView(withText("Not now")).perform(click())
            onIdle()
            verify(homeViewModel).onDismissBanner(eq(BannerData.LockScreenBanner(true, true)))
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.LOCK_SCREEN_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun nativeStepsBanner_displaysCorrectly_impressionsLogged() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.NativeStepsBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Steps tracked on your phone will appear in Health Connect")
            checkTextIsDisplayed(
                "Steps tracked by this device are now stored in Health Connect for connected apps to access"
            )
            checkTextIsDisplayed("Review")
            checkTextIsDisplayed("Dismiss")
            verify(healthConnectLogger).logImpression(HomePageElement.NATIVE_STEPS_BANNER)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.NATIVE_STEPS_BANNER_REVIEW_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.NATIVE_STEPS_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun nativeStepsBanner_whenClickOnReview_navigatesToConnectedDevices() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.NativeStepsBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed(
                "Steps tracked by this device are now stored in Health Connect for connected apps to access"
            )
            onView(withText("Review")).perform(click())
            onIdle()
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.connectedDevicesFragment)
            verify(homeViewModel).onDismissBanner(eq(BannerData.NativeStepsBanner))
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.NATIVE_STEPS_BANNER_REVIEW_BUTTON)
        }
    }

    @Test
    fun nativeStepsBanner_whenClickOnDismiss_dismissesBanner_setsBannerSeen() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.NativeStepsBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed(
                "Steps tracked by this device are now stored in Health Connect for connected apps to access"
            )
            onView(withText("Dismiss")).perform(click())
            onIdle()
            verify(homeViewModel).onDismissBanner(eq(BannerData.NativeStepsBanner))
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.NATIVE_STEPS_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun zeroAppsOnboardingBanner_displaysCorrectly_impressionsLogged() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.ZeroAppsOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("See your health data across apps")
            checkTextIsDisplayed("Start sharing fitness and wellness data between your apps")
            checkTextIsDisplayed("Not now")
            checkTextIsDisplayed("Set up")
            verify(healthConnectLogger).logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER_SET_UP_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun zeroAppsOnboardingBanner_whenClickOnSetUp_navigatesToOnboarding() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.ZeroAppsOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("See your health data across apps")
            checkTextIsDisplayed("Start sharing fitness and wellness data between your apps")
            onView(withText("Set up")).perform(click())
            onIdle()
            intended(hasAction("android.health.connect.action.SYNC_MORE_APPS"))
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.ZERO_APPS_CONNECTED_BANNER_SET_UP_BUTTON)
        }
    }

    @Test
    fun zeroAppsOnboardingBanner_whenClickOnDismiss_dismissesBanner_setsBannerSeen() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.ZeroAppsOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("See your health data across apps")
            checkTextIsDisplayed("Start sharing fitness and wellness data between your apps")
            onView(withText("Not now")).perform(click())
            onIdle()
            verify(homeViewModel).onDismissBanner(BannerData.ZeroAppsOnboardingBanner)
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.ZERO_APPS_CONNECTED_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun oneAppOnboardingBanner_displaysCorrectly_impressionsLogged() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.OneAppOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Connect a second app")
            checkTextIsDisplayed(
                "Set up another app so it can start sharing fitness and wellness data"
            )
            checkTextIsDisplayed("Not now")
            checkTextIsDisplayed("Continue")
            verify(healthConnectLogger).logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER_SET_UP_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun oneAppOnboardingBanner_whenClickOnContinue_navigatesToOnboarding() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.OneAppOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Connect a second app")
            checkTextIsDisplayed(
                "Set up another app so it can start sharing fitness and wellness data"
            )
            onView(withText("Continue")).perform(click())
            onIdle()
            intended(hasAction("android.health.connect.action.SYNC_MORE_APPS"))
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.ONE_APP_CONNECTED_BANNER_SET_UP_BUTTON)
        }
    }

    @Test
    fun oneAppOnboardingBanner_whenClickOnDismiss_dismissesBanner_setsBannerSeen() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.OneAppOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            checkTextIsDisplayed("Connect a second app")
            checkTextIsDisplayed(
                "Set up another app so it can start sharing fitness and wellness data"
            )
            onView(withText("Not now")).perform(click())
            onIdle()
            verify(homeViewModel).onDismissBanner(BannerData.OneAppOnboardingBanner)
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.ONE_APP_CONNECTED_BANNER_DISMISS_BUTTON)
        }
    }

    // endregion

    // region Migration dialogs
    @Test
    fun showsMigrationCompleteDialog() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    migrationDialog = HomeViewModel.MigrationDialog.MigrationCompleteDialog,
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.OneAppOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            onView(withText("What's new"))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "You can now access Health Connect directly from your settings. Uninstall the Health Connect app any time to free up storage space."
                    )
                )
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_DONE_DIALOG_CONTAINER)
            verify(healthConnectLogger).logImpression(MigrationElement.MIGRATION_DONE_DIALOG_BUTTON)

            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).perform(click())
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_DONE_DIALOG_BUTTON)

            verify(homeViewModel)
                .onDismissDialog(HomeViewModel.MigrationDialog.MigrationCompleteDialog)
        }
    }

    @Test
    fun showsMigrationNotCompleteDialog() {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    connectedApps = listOf(),
                    migrationDialog = HomeViewModel.MigrationDialog.MigrationNotCompleteDialog,
                    bannerState =
                        HomeViewModel.HomeBannerState.ShowBanners(
                            listOf(BannerData.OneAppOnboardingBanner)
                        ),
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)
        launchFragmentWithNavigation().use {
            onView(withText("Health Connect integration didn't complete"))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(withText("You'll get a notification when it becomes available again."))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_CONTAINER)
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_BUTTON)

            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).perform(click())
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_BUTTON)

            verify(homeViewModel)
                .onDismissDialog(HomeViewModel.MigrationDialog.MigrationNotCompleteDialog)
        }
    }

    // endregion

    private fun setupFragmentForNavigation(): ActivityScenario<TestActivity> {
        val stateFlow =
            MutableStateFlow<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(connectedApps = emptyList())
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(stateFlow)

        return launchFragmentWithNavigation()
    }

    private fun launchFragmentWithNavigation(): ActivityScenario<TestActivity> =
        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
}
