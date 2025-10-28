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

import android.content.Intent.EXTRA_PACKAGE_NAME
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.FitnessAppOnboardingFragment
import com.android.healthconnect.controller.onboarding.FitnessAppOnboardingViewModel
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.EXERCISE
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.READ
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.WRITE
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.scrollToBottomOfPreferenceScreen
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.FitnessAppOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.UIAction
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
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FitnessAppOnboardingFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val viewModel: FitnessAppOnboardingViewModel = mock()
    @BindValue val onboardingViewModel: OnboardingViewModel = mock()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        hiltRule.inject()
        navHostController = TestNavHostController(context)

        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessWrite(
                    TEST_APP,
                    mapOf(),
                )
            )
        }
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    fun noPermissions() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.NoFitnessData
            )
        }

        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scenario.onActivity { activity: TestActivity ->
                    val fragment = getFragment(activity)
                    val readCategory = getPreferenceCategory(fragment, "read_permission_category")

                    val writeCategory = getPreferenceCategory(fragment, "write_permission_category")
                    assertThat(readCategory?.preferenceCount).isEqualTo(0)
                    assertThat(writeCategory?.preferenceCount).isEqualTo(0)
                }
                verify(healthConnectLogger, atLeast(1))
                    .setPageId(PageName.FITNESS_APP_ONBOARDING_PAGE)
                verify(healthConnectLogger).logPageImpression()
            }
    }

    @Test
    fun onlyReadPermissions() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessRead(
                    TEST_APP,
                    mapOf(
                        FitnessPermission(DISTANCE, READ) to true,
                        FitnessPermission(EXERCISE, READ) to false,
                    ),
                    true,
                )
            )
        }
        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scenario.onActivity { activity: TestActivity ->
                    val fragment = getFragment(activity)
                    val readCategory = getPreferenceCategory(fragment, "read_permission_category")

                    val writeCategory = getPreferenceCategory(fragment, "write_permission_category")
                    assertThat(readCategory?.preferenceCount).isEqualTo(2)
                    assertThat(writeCategory?.preferenceCount).isEqualTo(0)
                }

                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                scrollToBottomOfPreferenceScreen()
                onView(withText("Distance")).perform(scrollTo()).check(matches(isDisplayed()))
                onView(withText("Exercise")).perform(scrollTo()).check(matches(isDisplayed()))
                verify(healthConnectLogger, atLeast(1))
                    .setPageId(PageName.FITNESS_APP_ONBOARDING_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger, times(2))
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_PERMISSION_BUTTON
                    )
            }
    }

    @Test
    fun onlyWritePermissions() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessWrite(
                    TEST_APP,
                    mapOf(
                        FitnessPermission(DISTANCE, WRITE) to true,
                        FitnessPermission(EXERCISE, WRITE) to false,
                    ),
                )
            )
        }

        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scenario.onActivity { activity: TestActivity ->
                    val fragment = getFragment(activity)
                    val readCategory = getPreferenceCategory(fragment, "read_permission_category")

                    val writeCategory = getPreferenceCategory(fragment, "write_permission_category")
                    assertThat(readCategory?.preferenceCount).isEqualTo(0)
                    assertThat(writeCategory?.preferenceCount).isEqualTo(2)
                }

                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                scrollToBottomOfPreferenceScreen()
                onView(withText("Distance")).perform(scrollTo()).check(matches(isDisplayed()))
                onView(withText("Exercise")).perform(scrollTo()).check(matches(isDisplayed()))
                verify(healthConnectLogger, atLeast(1))
                    .setPageId(PageName.FITNESS_APP_ONBOARDING_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger, times(2))
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_PERMISSION_BUTTON
                    )
            }
    }

    @Test
    fun readAndWritePermissions() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to true,
                            FitnessPermission(EXERCISE, WRITE) to false,
                        ),
                        true,
                    )
            )
        }

        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scenario.onActivity { activity: TestActivity ->
                    val fragment = getFragment(activity)
                    val readCategory = getPreferenceCategory(fragment, "read_permission_category")

                    val writeCategory = getPreferenceCategory(fragment, "write_permission_category")
                    assertThat(readCategory?.preferenceCount).isEqualTo(1)
                    assertThat(writeCategory?.preferenceCount).isEqualTo(1)
                }

                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                scrollToBottomOfPreferenceScreen()
                onView(withText("Distance")).perform(scrollTo()).check(matches(isDisplayed()))
                onView(withText("Exercise")).perform(scrollTo()).check(matches(isDisplayed()))
                verify(healthConnectLogger, atLeast(1))
                    .setPageId(PageName.FITNESS_APP_ONBOARDING_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger, times(2))
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_PERMISSION_BUTTON
                    )
            }
    }

    @Test
    fun whenPermissionSwitchIsOn_forReadWrite_correctContentDescriptionIsDisplayed() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(EXERCISE, WRITE) to true,
                            FitnessPermission(DISTANCE, READ) to true,
                        ),
                        true,
                    )
            )
        }

        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scrollToBottomOfPreferenceScreen()
                onView(withContentDescription("Exercise. Write Access. On"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withContentDescription("Distance. Read Access. On"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun whenPermissionSwitchIsOff_forReadWrite_correctContentDescriptionIsDisplayed() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(EXERCISE, WRITE) to false,
                            FitnessPermission(DISTANCE, READ) to false,
                        ),
                        true,
                    )
            )
        }

        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scrollToBottomOfPreferenceScreen()
                onView(withContentDescription("Exercise. Write Access. Off"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withContentDescription("Distance. Read Access. Off"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun whenAllPermissionsOn_allowAllToggleOn() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to true,
                            FitnessPermission(EXERCISE, WRITE) to true,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(true) }

        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scenario.onActivity { activity: TestActivity ->
                    val fragment = getFragment(activity)
                    val mainSwitchPreference =
                        fragment.preferenceScreen.findPreference("allow_all_preference")
                            as HealthMainSwitchPreference?

                    assertThat(mainSwitchPreference?.isChecked).isTrue()
                }
                verify(healthConnectLogger, atLeast(1))
                    .setPageId(PageName.FITNESS_APP_ONBOARDING_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger, times(2))
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_PERMISSION_BUTTON
                    )
                verify(healthConnectLogger)
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_ALLOW_ALL_BUTTON
                    )
                verify(healthConnectLogger)
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_LEARN_MORE_LINK
                    )
                verify(healthConnectLogger)
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_PRIVACY_POLICY_LINK
                    )
            }
    }

    @Test
    fun whenOnePermissionOff_allowToggleOff() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to false,
                            FitnessPermission(EXERCISE, WRITE) to true,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }

        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                scenario.onActivity { activity: TestActivity ->
                    val fragment = getFragment(activity)
                    val mainSwitchPreference =
                        fragment.preferenceScreen.findPreference("allow_all_preference")
                            as HealthMainSwitchPreference?

                    assertThat(mainSwitchPreference?.isChecked).isFalse()
                }
            }
    }

    @Test
    fun toggleOffAllowAll_togglesAllPermissionsOff() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to true,
                            FitnessPermission(EXERCISE, WRITE) to true,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(true) }
        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                onView(withText("Allow all")).perform(click())
                verify(viewModel).updateAllPermissions(false)
                verify(healthConnectLogger)
                    .logInteraction(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_ALLOW_ALL_BUTTON,
                        UIAction.ACTION_TOGGLE_OFF,
                    )
            }
    }

    @Test
    fun toggleOnAllowAll_togglesAllPermissionsOn() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to false,
                            FitnessPermission(EXERCISE, WRITE) to true,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                onView(withText("Allow all")).perform(click())
                verify(viewModel).updateAllPermissions(true)
                verify(healthConnectLogger)
                    .logInteraction(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_ALLOW_ALL_BUTTON,
                        UIAction.ACTION_TOGGLE_ON,
                    )
            }
    }

    @Test
    fun backButton_doesNotGrantOrRevokePermissions() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to false,
                            FitnessPermission(EXERCISE, WRITE) to true,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            ) {
                navHostController.setGraph(R.navigation.onboarding_nav_graph)
                navHostController.setCurrentDestination(R.id.fitnessAppOnboardingFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Back")).perform(click())
                verify(viewModel, never()).done()
                verify(healthConnectLogger)
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_DONE_BUTTON
                    )
                verify(healthConnectLogger)
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_BACK_BUTTON
                    )
                verify(healthConnectLogger)
                    .logInteraction(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_BACK_BUTTON
                    )
            }
    }

    @Test
    fun doneButton_grantsAndRevokesCorrectly() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to false,
                            FitnessPermission(EXERCISE, WRITE) to true,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            ) {
                navHostController.setGraph(R.navigation.onboarding_nav_graph)
                navHostController.setCurrentDestination(R.id.fitnessAppOnboardingFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onIdle()
                onView(withText("Done")).perform(click())
                verify(viewModel).done()
                verify(healthConnectLogger)
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_DONE_BUTTON
                    )
                verify(healthConnectLogger)
                    .logImpression(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_BACK_BUTTON
                    )
                verify(healthConnectLogger)
                    .logInteraction(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_DONE_BUTTON
                    )
            }
    }

    @Test
    fun togglePermissionOff_updatesViewModel() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to true,
                            FitnessPermission(EXERCISE, WRITE) to false,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                onView(withText("Distance")).perform(scrollTo()).perform(click())
                verify(viewModel).updatePermission(FitnessPermission(DISTANCE, READ), false)
                verify(healthConnectLogger)
                    .logInteraction(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_PERMISSION_BUTTON,
                        UIAction.ACTION_TOGGLE_OFF,
                    )
            }
    }

    @Test
    fun togglePermissionOn_updatesViewModel() {
        whenever(viewModel.fitnessAppOnboardingFragmentState).then {
            MutableLiveData(
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        TEST_APP,
                        mapOf(
                            FitnessPermission(DISTANCE, READ) to false,
                            FitnessPermission(EXERCISE, WRITE) to false,
                        ),
                        true,
                    )
            )
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        launchFragment<FitnessAppOnboardingFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
            .use { scenario ->
                onView(withText("Distance")).perform(scrollTo()).perform(click())
                verify(viewModel).updatePermission(FitnessPermission(DISTANCE, READ), true)
                verify(healthConnectLogger)
                    .logInteraction(
                        FitnessAppOnboardingPageElement.FITNESS_APP_ONBOARDING_PERMISSION_BUTTON,
                        UIAction.ACTION_TOGGLE_ON,
                    )
            }
    }

    private fun getFragment(activity: TestActivity): HealthPreferenceFragment {
        return activity.supportFragmentManager.findFragmentById(android.R.id.content)
            as HealthPreferenceFragment
    }

    private fun getPreferenceCategory(
        fragment: HealthPreferenceFragment,
        id: String,
    ): PreferenceCategory? {
        return fragment.preferenceScreen.findPreference(id) as PreferenceCategory?
    }
}
