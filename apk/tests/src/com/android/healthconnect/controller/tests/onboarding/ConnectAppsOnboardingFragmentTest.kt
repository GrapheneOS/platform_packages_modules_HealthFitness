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
import android.health.connect.HealthConnectManager
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
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.ConnectAppsOnboardingFragment
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.scrollToBottomOfPreferenceScreen
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.AlmostDonePageElement
import com.android.healthconnect.controller.utils.logging.CommonOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.ConnectSecondAddOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.ConnectTwoAppsOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
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
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ConnectAppsOnboardingFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @BindValue val viewModel: OnboardingViewModel = mock()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
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
        reset(healthConnectLogger)
    }

    @Test
    fun noFitnessAppsConnected_showsConnectFirstTwoApps() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected(
                    listOf(
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP,
                            isConnected = false,
                            hasOnboarding = true,
                        ),
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP_2,
                            isConnected = false,
                            hasOnboarding = false,
                        ),
                    )
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>()

        onView(withText("Connect your first app")).check(matches(isDisplayed()))
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
        onView(withText("Available apps to connect"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))

        onView(withText("Set up later")).check(matches(isDisplayed()))

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.CONNECT_TWO_APPS_ONBOARDING_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.APP_WITH_ONBOARDING_BUTTON)
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.APP_WITHOUT_ONBOARDING_BUTTON)
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.MORE_ABOUT_HEALTH_CONNECT_BUTTON)
        verify(healthConnectLogger)
            .logImpression(
                ConnectTwoAppsOnboardingPageElement
                    .CONNECT_FIRST_TWO_APPS_ONBOARDING_SET_UP_LATER_BUTTON
            )
    }

    @Test
    fun oneFitnessAppsConnected_showsConnectSecondApp() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.OneAppConnected(
                    connectedApp =
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP_2,
                            isConnected = true,
                            hasOnboarding = false,
                        ),
                    potentialApps =
                        listOf(
                            // TODO logging here is not set correctly
                            ConnectedFitnessAppMetadata(
                                appMetadata = TEST_APP,
                                isConnected = false,
                                hasOnboarding = true,
                            )
                        ),
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>()

        onView(withText("Connect a second app")).check(matches(isDisplayed()))
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
        onView(withText("Available apps to connect"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))

        onView(withText("Set up later")).check(matches(isDisplayed()))

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.CONNECT_ONE_APP_ONBOARDING_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.APP_WITH_ONBOARDING_BUTTON)
        verify(healthConnectLogger)
            .logImpression(ConnectSecondAddOnboardingPageElement.CONNECTED_APP_BUTTON)
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.MORE_ABOUT_HEALTH_CONNECT_BUTTON)
        verify(healthConnectLogger)
            .logImpression(
                ConnectSecondAddOnboardingPageElement
                    .CONNECT_SECOND_APP_ONBOARDING_SET_UP_LATER_BUTTON
            )
    }

    @Test
    fun twoFitnessAppsConnected_showsAlmostDone() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.AlmostDone(
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
        onView(withText("Connected apps")).check(matches(isDisplayed()))
        onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Available apps to connect")).check(doesNotExist())

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

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ALMOST_DONE_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(2))
            .logImpression(AlmostDonePageElement.ONBOARDING_APP_BUTTON)
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.MORE_ABOUT_HEALTH_CONNECT_BUTTON)
        verify(healthConnectLogger).logImpression(AlmostDonePageElement.ONBOARDING_DONE_BUTTON)
    }

    @Test
    fun twoFitnessAppsConnected_moreToConnect_showsAlmostDone() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.AlmostDone(
                    connectedApps =
                        listOf(
                            ConnectedFitnessAppMetadata(TEST_APP, true),
                            ConnectedFitnessAppMetadata(TEST_APP_2, true),
                        ),
                    potentialApps = listOf(ConnectedFitnessAppMetadata(TEST_APP_3, false)),
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>()

        onView(withText("Almost done")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Open these apps now to finish setup and start data sharing, or keep connecting available apps"
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("Connected apps")).check(matches(isDisplayed()))
        onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Available apps to connect"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        scrollToBottomOfPreferenceScreen()
        onView(withText(TEST_APP_3.appName)).perform(scrollTo()).check(matches(isDisplayed()))

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

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ALMOST_DONE_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(2))
            .logImpression(AlmostDonePageElement.ONBOARDING_APP_BUTTON)
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.APP_WITHOUT_ONBOARDING_BUTTON)
        verify(healthConnectLogger)
            .logImpression(CommonOnboardingPageElement.MORE_ABOUT_HEALTH_CONNECT_BUTTON)
        verify(healthConnectLogger).logImpression(AlmostDonePageElement.ONBOARDING_DONE_BUTTON)
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun appWithOnboardingIntent_redirectsToApp() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected(
                    listOf(
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP,
                            isConnected = false,
                            hasOnboarding = true,
                        ),
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP_2,
                            isConnected = false,
                            hasOnboarding = false,
                        ),
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

        verify(healthConnectLogger)
            .logInteraction(CommonOnboardingPageElement.APP_WITH_ONBOARDING_BUTTON)
    }

    @Test
    fun appWithNoOnboardingIntent_redirectsToFitnessAppOnboardingFragment() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected(
                    listOf(
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP,
                            isConnected = false,
                            hasOnboarding = true,
                        ),
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP_2,
                            isConnected = false,
                            hasOnboarding = false,
                        ),
                    )
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment> {
            navHostController.setGraph(R.navigation.onboarding_nav_graph)
            navHostController.setCurrentDestination(R.id.connectAppsOnboardingFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME_2)).perform(click())
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.fitnessAppOnboardingFragment)
        verify(healthConnectLogger)
            .logInteraction(CommonOnboardingPageElement.APP_WITHOUT_ONBOARDING_BUTTON)
    }

    @Test
    fun setupLaterButton_finishesActivity_andLaunchesHealthIntent() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected(
                    listOf(
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP,
                            isConnected = false,
                            hasOnboarding = true,
                        ),
                        ConnectedFitnessAppMetadata(
                            appMetadata = TEST_APP_2,
                            isConnected = false,
                            hasOnboarding = false,
                        ),
                    )
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>().use { scenario ->
            onView(withText("Connect your first app")).check(matches(isDisplayed()))
            onView(withText("Available apps to connect"))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
            onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))

            onView(withText("Set up later")).check(matches(isDisplayed()))

            onView(withText("Set up later")).perform(click())

            Intents.intended(hasAction(HealthConnectManager.ACTION_HEALTH_HOME_SETTINGS))

            verify(healthConnectLogger)
                .logInteraction(
                    ConnectTwoAppsOnboardingPageElement
                        .CONNECT_FIRST_TWO_APPS_ONBOARDING_SET_UP_LATER_BUTTON
                )
        }
    }

    @Test
    fun doneButton_finishesActivity_andLaunchesHealthIntent() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.AlmostDone(
                    connectedApps =
                        listOf(
                            ConnectedFitnessAppMetadata(TEST_APP, true),
                            ConnectedFitnessAppMetadata(TEST_APP_2, true),
                        ),
                    potentialApps = listOf(ConnectedFitnessAppMetadata(TEST_APP_3, false)),
                )
            )
        }

        launchFragment<ConnectAppsOnboardingFragment>().use { scenario ->
            onView(withText("Almost done")).check(matches(isDisplayed()))
            onView(withText("Connected apps")).check(matches(isDisplayed()))
            onView(withText(TEST_APP.appName)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText(TEST_APP_2.appName)).perform(scrollTo()).check(matches(isDisplayed()))

            onView(withText("Done")).check(matches(isDisplayed()))

            onView(withText("Done")).perform(click())

            Intents.intended(hasAction(HealthConnectManager.ACTION_HEALTH_HOME_SETTINGS))

            verify(healthConnectLogger).logInteraction(AlmostDonePageElement.ONBOARDING_DONE_BUTTON)
        }
    }
}
