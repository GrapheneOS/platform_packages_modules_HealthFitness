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

package com.android.healthconnect.controller.tests.onboarding

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.ACTION_SHOW_ONBOARDING
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.ConnectAppsOnboardingFragment
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
class ConnectAppsOnboardingFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @BindValue val viewModel: OnboardingViewModel = mock()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()
    private lateinit var context: Context
    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().context
        (deviceInfoUtils as FakeDeviceInfoUtils).setHealthConnectAvailable(true)
        toggleAnimation(false)
        Intents.init()
        navHostController = TestNavHostController(context)
    }

    @After
    fun tearDown() {
        toggleAnimation(true)
        Intents.release()
    }

    @Test
    fun noFitnessAppsConnected_showsConnectFirstTwoApps() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.WithData(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, false),
                        ConnectedFitnessAppMetadata(TEST_APP_2, false),
                    )
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>()

        onView(withText("Connect your first 2 apps")).check(matches(isDisplayed()))
        onView(withText("Connect these apps to start sharing health and fitness data between them"))
            .check(matches(isDisplayed()))
        onView(withText("Available apps to connect")).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))

        onView(
                withText(
                    "Health\u00A0Connect lets you store your health and fitness data in one " +
                        "place and share it between apps on your phone, making your apps " +
                        "work better together."
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withText("More about Health\u00A0Connect"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Set up later")).check(matches(isDisplayed()))
    }

    @Test
    fun oneFitnessAppsConnected_showsConnectSecondApp() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.WithData(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, false),
                        ConnectedFitnessAppMetadata(TEST_APP_2, true),
                    )
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>()

        onView(withText("Connect a second app")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Set up one more app to share health and fitness data with ${TEST_APP_2.appName}"
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("Available apps to connect")).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))

        onView(
                withText(
                    "Health\u00A0Connect lets you store your health and fitness data in one " +
                        "place and share it between apps on your phone, making your apps " +
                        "work better together."
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withText("More about Health\u00A0Connect"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Set up later")).check(matches(isDisplayed()))
    }

    @Test
    fun twoFitnessAppsConnected_showsAlmostDone() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.WithData(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, true),
                        ConnectedFitnessAppMetadata(TEST_APP_2, true),
                    )
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>()

        onView(withText("Almost done")).check(matches(isDisplayed()))
        onView(withText("Open these apps now to finish setup and start data sharing"))
            .check(matches(isDisplayed()))
        onView(withText("Available apps to connect")).check(matches(isDisplayed()))
        onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))

        onView(
                withText(
                    "Health\u00A0Connect lets you store your health and fitness data in one " +
                        "place and share it between apps on your phone, making your apps " +
                        "work better together."
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withText("More about Health\u00A0Connect"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Done")).check(matches(isDisplayed()))
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun appWithOnboardingIntent_redirectsToApp() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.WithData(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, false),
                        ConnectedFitnessAppMetadata(TEST_APP_2, false),
                    )
                )
            )
        }

        val testIntent = Intent(ACTION_SHOW_ONBOARDING)
        testIntent.setPackage(TEST_APP.packageName)
        // Assume that the client onboarding activity completes normally.
        Intents.intending(hasAction(ACTION_SHOW_ONBOARDING))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(testIntent)

        launchFragment<ConnectAppsOnboardingFragment>()
        onView(withText(TEST_APP_NAME)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())

        Intents.intended(hasAction(ACTION_SHOW_ONBOARDING))
        Intents.intended(hasPackage(TEST_APP_PACKAGE_NAME))

        verify(viewModel).setAppInteractedWith(TEST_APP_PACKAGE_NAME)
    }

    @Test
    fun appWithNoOnboardingIntent_redirectsToFitnessAppOnboardingFragment() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.WithData(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, false),
                        ConnectedFitnessAppMetadata(TEST_APP_2, false),
                    )
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment> {
            navHostController.setGraph(R.navigation.onboarding_nav_graph)
            navHostController.setCurrentDestination(R.id.connectAppsOnboardingFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.fitnessAppOnboardingFragment)
    }
}
