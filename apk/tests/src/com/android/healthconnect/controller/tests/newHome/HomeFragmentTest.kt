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
package com.android.healthconnect.controller.tests.newhome

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.ACTION_SHOW_ONBOARDING
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.newHome.HomeFragment
import com.android.healthconnect.controller.newHome.HomeViewModel
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_4
import com.android.healthconnect.controller.tests.utils.TEST_APP_5
import com.android.healthconnect.controller.tests.utils.TEST_APP_6
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(DeviceInfoUtilsModule::class)
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
    }

    @After
    fun tearDown() {
        Intents.release()
        reset(healthConnectLogger)
    }

    // region General display
    @Test
    fun whenLoading_showsLoading() {
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.Loading
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle())

        onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
    }

    @Test
    fun whenError_showsError() {
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(HomeViewModel.HomeFragmentState.Error)
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle())

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun withData_showsAllSections() {
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(
                    listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED))
                )
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)
        launchFragment<HomeFragment>(Bundle())

        onView(withText("Your health apps")).check(matches(isDisplayed()))
        onView(withText("Health Connect test app")).check(matches(isDisplayed()))
        onView(withText("Your health data")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))
        onView(withText("Recent access")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Preferences")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Manage data")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(
                withText(
                    "Health Connect lets you share your health and fitness data between " +
                        "multiple apps. This helps you unlock insights and experiences while keeping " +
                        "your data secure."
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("More about Health Connect"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    // endregion

    // region Your Health Apps
    @Test
    fun whenNoApps_showsNoAppsPreference() {
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(emptyList())
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle())

        onView(withText("Install apps that work with Health Connect to " + "see them here"))
            .check(matches(isDisplayed()))
        onView(withText("See compatible apps")).check(matches(isDisplayed()))
    }

    @Test
    fun whenThreeApps_showsThreeApps_andNoSeeAllButton() {
        val apps =
            listOf(
                ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED),
            )
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle())

        onView(withText(TEST_APP.appName)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_3.appName)).check(matches(isDisplayed()))
        onView(withText("See all")).check(doesNotExist())
    }

    @Test
    fun whenSixApps_showsFiveApps_andSeeAllButton() {
        val apps =
            listOf(
                ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.ALLOWED),
                ConnectedAppMetadata(TEST_APP_6, ConnectedAppStatus.ALLOWED),
            )
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle())

        onView(withText(TEST_APP.appName)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_3.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_4.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_5.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_6.appName)).check(doesNotExist())
        onView(withText("See all")).perform(scrollTo()).check(matches(isDisplayed()))
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
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.fitnessAppFragment)
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
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.medicalAppFragment)
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
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.combinedPermissionsFragment)
    }

    @Test
    fun whenClickOnDisconnectedAppWithOnboarding_launchesOnboardingIntent() {
        val apps = listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.DENIED))
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        val testIntent = Intent(ACTION_SHOW_ONBOARDING)
        testIntent.setPackage(TEST_APP.packageName)

        // Assume that the client onboarding activity completes normally.
        Intents.intending(hasAction(ACTION_SHOW_ONBOARDING))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(testIntent)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).perform(click())
        intended(hasAction(ACTION_SHOW_ONBOARDING))
        intended(hasPackage(TEST_APP_PACKAGE_NAME))
        // We should remain where we started.
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.newHomeFragment)
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
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(null)

        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.fitnessAppFragment)
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
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(null)

        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.medicalAppFragment)
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
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(null)

        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.combinedPermissionsFragment)
    }

    // endregion

    // Navigation
    @Test
    fun dataAndAccess_navigatesToDataAndAccess() {
        setupFragmentForNavigation()
        onView(withText("Data and access")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.data_activity)
    }

    @Test
    fun recentAccess_navigatesToRecentAccess() {
        setupFragmentForNavigation()
        onView(withText("Recent access")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.recentAccessFragment)
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun devices_navigatesToConnectedDevices() {
        setupFragmentForNavigation()
        onView(withText("Devices")).perform(click())
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.connectedDevicesFragment)
    }

    @Test
    @DisableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun devices_whenFlagDisabled_isNotDisplayed() {
        setupFragmentForNavigation()
        onView(withText("Devices")).check(doesNotExist())
    }

    @Test
    fun manageData_navigatesToManageData() {
        setupFragmentForNavigation()
        onView(withText("Manage data")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.manageDataFragment)
    }

    @Test
    fun seeCompatibleApps_navigatesToPlayStore() {
        setupFragmentForNavigation()

        onView(withText("See compatible apps")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.playstore_activity)
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
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(apps)
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("See all")).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.connectedAppsFragment)
    }

    @Test
    fun moreAboutHealthConnect_navigatesToHelpPage() {
        // TODO: Implement this test.
    }

    // endregion

    // region Banner tests
    // TODO: Implement tests
    // endregion

    private fun setupFragmentForNavigation() {
        val liveData =
            MutableLiveData<HomeViewModel.HomeFragmentState>(
                HomeViewModel.HomeFragmentState.WithData(emptyList())
            )
        whenever(homeViewModel.homeFragmentState).thenReturn(liveData)

        launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.newHomeFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
    }
}
