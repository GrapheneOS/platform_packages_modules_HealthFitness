/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.recentaccess

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.ACTION_SHOW_ONBOARDING
import android.health.connect.HealthDataCategory
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
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.recentaccess.RecentAccessEntry
import com.android.healthconnect.controller.recentaccess.RecentAccessFragment
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel.RecentAccessState
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.*
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RecentAccessElement
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
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
@RunWith(AndroidJUnit4::class)
class RecentAccessFragmentTest {

    private val recentAccessAppsLiveData = MutableLiveData<RecentAccessState>()

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue
    val viewModel: RecentAccessViewModel = Mockito.mock(RecentAccessViewModel::class.java)
    @BindValue
    val healthPermissionReader: HealthPermissionReader =
        Mockito.mock(HealthPermissionReader::class.java)

    private lateinit var context: Context
    private lateinit var navHostController: TestNavHostController
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var activityScenario: ActivityScenario<TestActivity>

    @BindValue val timeSource = TestTimeSource

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        context.setLocale(Locale.US)
        Intents.init()
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    @After
    fun teardown() {
        timeSource.reset()
        Intents.release()
        reset(healthConnectLogger)
        activityScenario.close()
    }

    @Test
    fun displaysCorrectly() {
        val recentApp1 =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        val recentApp2 =
            RecentAccessEntry(
                metadata = TEST_APP_2,
                instantTime = Instant.parse("2022-10-20T19:40:13.00Z"),
                isToday = true,
                isInactive = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        timeSource.setIs24Hour(true)

        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(
                RecentAccessState.WithData(listOf(recentApp1, recentApp2))
            )
        }

        launchScenario()

        onView(withText("Today")).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText("18:40")).check(matches(isDisplayed()))
        onView(withText("Read: Nutrition, Sleep")).check(matches(isDisplayed()))
        onView(withText("Write: Activity, Vitals")).check(matches(isDisplayed()))

        onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
        onView(withText("19:40")).check(matches(isDisplayed()))
        onView(withText("Read: Activity, Vitals")).check(matches(isDisplayed()))
        onView(withText("Write: Nutrition, Sleep")).check(matches(isDisplayed()))

        onView(withText("Manage permissions")).check(matches(isDisplayed()))
        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.RECENT_ACCESS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(2))
            .logImpression(RecentAccessElement.RECENT_ACCESS_ENTRY_BUTTON)
        verify(healthConnectLogger).logImpression(RecentAccessElement.MANAGE_PERMISSIONS_FAB)
    }

    @Test
    fun inactiveApp_doesNotNavigate() {
        val recentApp1 =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        val recentApp2 =
            RecentAccessEntry(
                metadata = TEST_APP_2,
                instantTime = Instant.parse("2022-10-20T19:40:13.00Z"),
                isToday = true,
                isInactive = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(
                RecentAccessState.WithData(listOf(recentApp1, recentApp2))
            )
        }

        launchScenario()

        onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME_2)).perform(click())

        onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
    }

    @Test
    fun displays12HourFormatCorrectly() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        timeSource.setIs24Hour(false)
        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }

        launchScenario()

        onView(withText("Today")).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText("6:40 PM")).check(matches(isDisplayed()))
        onView(withText("Read: Nutrition, Sleep")).check(matches(isDisplayed()))
        onView(withText("Write: Activity, Vitals")).check(matches(isDisplayed()))
    }

    @Test
    fun appName_navigatesToFitnessAppPermissions() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }

        launchScenario()

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        Truth.assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.fitnessAppFragment)
    }

    @Test
    fun appName_navigatesToMedicalAppPermissions() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.MEDICAL_PERMISSIONS_ONLY,
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }

        launchScenario()

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        Truth.assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.medicalAppFragment)
    }

    @Test
    fun appName_navigatesToCombinedPermissions() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }

        launchScenario()

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        Truth.assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.combinedPermissionsFragment)
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun onboardingActivityAvailable_navigatesToOnboardingActivityInsteadOfPermissionManagement() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                shouldLaunchAppOnboardingIfAvailable = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
            )
        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
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

        launchScenario()

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())

        Intents.intended(hasAction(ACTION_SHOW_ONBOARDING))
        Intents.intended(hasPackage(TEST_APP_PACKAGE_NAME))
        Truth.assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.recentAccessFragment) // Verify nav didn't happen to permission screen
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun onboardingActivityAvailable_permissionsAlreadyGranted_doesNotLaunchOnboardingActivity() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                shouldLaunchAppOnboardingIfAvailable = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
            )
        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        val testIntent = Intent(ACTION_SHOW_ONBOARDING)
        testIntent.setPackage(TEST_APP.packageName)

        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(testIntent)

        launchScenario()

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())

        Truth.assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.combinedPermissionsFragment)
    }

    @Test
    fun managePermissionsButton_navigatesToConnectedAppsFragment_buttonIsNotVisible() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten = mutableSetOf(HealthDataCategory.ACTIVITY.uppercaseTitle()),
                dataTypesRead = mutableSetOf(HealthDataCategory.SLEEP.uppercaseTitle()),
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        whenever(viewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }

        launchScenario()

        onView(withText(R.string.manage_permissions)).check(matches(isDisplayed()))
        onView(withText(R.string.manage_permissions)).perform(click())
        Truth.assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.connectedAppsFragment)
        onView(withText(R.string.manage_permissions)).check(doesNotExist())
    }

    @Test
    fun updateFabState_whenDataIsEmpty_managePermissionsButtonHidden() {
        whenever(viewModel.recentAccessApps).thenReturn(recentAccessAppsLiveData)

        launchScenario()
        recentAccessAppsLiveData.postValue(RecentAccessState.WithData(emptyList()))

        onView(withText(R.string.manage_permissions)).check(doesNotExist())
        verify(healthConnectLogger, never())
            .logImpression(RecentAccessElement.MANAGE_PERMISSIONS_FAB)
    }

    @Test
    fun updateFabState_whenStateIsLoading_managePermissionsButtonHidden() {
        whenever(viewModel.recentAccessApps).thenReturn(recentAccessAppsLiveData)

        launchScenario()
        recentAccessAppsLiveData.postValue(RecentAccessState.Loading)

        onView(withText(R.string.manage_permissions)).check(doesNotExist())
        verify(healthConnectLogger, never())
            .logImpression(RecentAccessElement.MANAGE_PERMISSIONS_FAB)
    }

    @Test
    fun updateFabState_whenStateIsError_managePermissionsButtonHidden() {
        whenever(viewModel.recentAccessApps).thenReturn(recentAccessAppsLiveData)

        launchScenario()
        recentAccessAppsLiveData.postValue(RecentAccessState.Error)

        onView(withText(R.string.manage_permissions)).check(doesNotExist())
        verify(healthConnectLogger, never())
            .logImpression(RecentAccessElement.MANAGE_PERMISSIONS_FAB)
    }

    fun launchScenario() {
        activityScenario = ActivityScenario.launch(TestActivity::class.java)
        activityScenario.onActivity { activity ->
            navHostController = TestNavHostController(activity)
            navHostController.setLifecycleOwner(activity)
            navHostController.setViewModelStore(activity.viewModelStore)
            navHostController.setOnBackPressedDispatcher(activity.onBackPressedDispatcher)
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.recentAccessFragment)

            Navigation.setViewNavController(
                activity.findViewById(android.R.id.content),
                navHostController,
            )

            val fragment = RecentAccessFragment()
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow()
        }
    }
}
