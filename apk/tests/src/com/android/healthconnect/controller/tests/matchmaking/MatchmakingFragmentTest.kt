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

package com.android.healthconnect.controller.tests.matchmaking

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectManager
import android.health.connect.HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceCategory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.matchmaking.MatchmakingDevicePreference
import com.android.healthconnect.controller.matchmaking.MatchmakingFragment
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
import com.android.healthconnect.controller.matchmaking.api.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.api.MatchmakingDeviceData
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.READ
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthExpandablePreference
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.CALLING_APP_NAME
import com.android.healthconnect.controller.tests.utils.CALLING_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.checkTextIsDisplayed
import com.android.healthconnect.controller.tests.utils.clickSwitchOnRecyclerViewItemWithText
import com.android.healthconnect.controller.tests.utils.scrollToText
import com.android.healthconnect.controller.tests.utils.scrollToTextAndClick
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MatchmakingElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.logging.UIAction
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MatchmakingFragmentTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: MatchmakingViewModel = mock<MatchmakingViewModel>()

    @BindValue val appInfoReader: AppInfoReader = mock<AppInfoReader>()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock<HealthPermissionReader>()
    @BindValue val logger: HealthConnectLogger = mock<HealthConnectLogger>()

    @BindValue val deviceInfoUtils: DeviceInfoUtils = mock<DeviceInfoUtils>()

    private val matchmakingState = MutableLiveData<MatchmakingViewModel.MatchmakingState>()
    private val expandedKeys = MutableLiveData<Set<String>>(emptySet())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val atLeastOneDataSourceSelected = MutableLiveData(false)
    private val allPermissionsGranted = MutableLiveData(false)
    private val hasSelectedDevice = MutableLiveData(false)
    private val hasSelectedApp = MutableLiveData(false)
    private val ddpOnboardingState =
        MutableLiveData<MatchmakingViewModel.DdpOnboardingState>(
            MatchmakingViewModel.DdpOnboardingState.Setup
        )
    private val grantedPermissions =
        MutableLiveData<Map<String, List<FitnessPermission>>>(emptyMap())

    val matchingAppsCount = MutableLiveData(0)

    @Before
    fun setup() {
        hiltRule.inject()

        whenever(viewModel.matchmakingState).thenReturn(matchmakingState)
        whenever(viewModel.expandedPreferenceKeys).thenReturn(expandedKeys)
        whenever(viewModel.atLeastOneDataSourceSelected).thenReturn(atLeastOneDataSourceSelected)
        whenever(viewModel.allPermissionsGranted).thenReturn(allPermissionsGranted)
        whenever(viewModel.grantedPermissions).thenReturn(grantedPermissions)
        whenever(viewModel.matchingAppsCount).thenReturn(matchingAppsCount)
        whenever(viewModel.hasSelectedDevice).thenReturn(hasSelectedDevice)
        whenever(viewModel.hasSelectedApp).thenReturn(hasSelectedApp)
        whenever(viewModel.ddpOnboardingState).thenReturn(ddpOnboardingState)
        whenever(deviceInfoUtils.isHealthConnectAvailable(any())).thenReturn(true)
        whenever(viewModel.enabledDevicePackages).thenReturn(MutableLiveData(emptySet()))
    }

    @After
    fun teardown() {
        reset(logger)
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_withDataState_showsCorrectContent() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                ),
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null),
                    listOf(FitnessPermission(FitnessPermissionType.DISTANCE, READ)),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList<MatchmakingDeviceData>(),
            )
        )
        grantedPermissions.postValue(
            mapOf(
                TEST_APP_PACKAGE_NAME to
                    listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ))
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                checkTextIsDisplayed(context.getString(R.string.matchmaking_screen_title))
                checkTextIsDisplayed(
                    context.getString(R.string.matchmaking_screen_summary, CALLING_APP_NAME)
                )
                checkTextIsDisplayed(
                    context.getString(
                        R.string.matchmaking_screen_data_from_data_source,
                        TEST_APP_NAME,
                    )
                )
                checkTextIsDisplayed("1 of 2 selected")
                scrollToTextAndClick(TEST_APP_NAME)
                checkTextIsDisplayed("Exercise")
                checkTextIsDisplayed("Steps")
                val policyString = context.getString(R.string.request_permissions_privacy_policy)
                val rationaleText =
                    context.resources.getString(
                        R.string.app_privacy_policy_footer,
                        TEST_APP_NAME,
                        policyString,
                    )
                checkTextIsDisplayed(rationaleText)
                verify(logger, atLeast(1)).setPageId(PageName.MATCHMAKING_PAGE)
                verify(logger).logPageImpression()
                verify(logger).logImpression(MatchmakingElement.MATCHMAKING_SCREEN_HEADER)
                verify(logger).logImpression(MatchmakingElement.MATCHMAKING_SCREEN_HEADER_ICON_VIEW)
                verify(logger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
                verify(logger, times(2))
                    .logImpression(MatchmakingElement.MATCHMAKING_EXPANDABLE_PREFERENCE)
                verify(logger, times(3)).logImpression(PermissionsElement.PERMISSION_SWITCH)
                verify(logger).logImpression(MatchmakingElement.MATCHMAKING_SCREEN_FOOTER)
                verify(logger).logImpression(MatchmakingElement.MATCHMAKING_SCREEN_FOOTER_LINK)
                verify(logger).logImpression(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
                verify(logger).logImpression(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_allowAllToggleOn_updatesAllPermissions() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                ),
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null),
                    listOf(FitnessPermission(FitnessPermissionType.DISTANCE, READ)),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()
                }

                scrollToText("Allow all")
                onView(withText("Allow all")).perform(click())

                verify(viewModel).addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
                verify(viewModel).addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME_2)
                verify(logger)
                    .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_ON)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_allowAllToggleOff_updatesAllPermissions() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                ),
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null),
                    listOf(FitnessPermission(FitnessPermissionType.DISTANCE, READ)),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )
        allPermissionsGranted.postValue(true)

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()
                }

                scrollToText("Allow all")
                onView(withText("Allow all")).perform(click())

                verify(viewModel).removeAllPermissionsFromGrantedList(TEST_APP_PACKAGE_NAME)
                verify(viewModel).removeAllPermissionsFromGrantedList(TEST_APP_PACKAGE_NAME_2)
                verify(logger)
                    .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_OFF)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_allowPermissionsClicked_interactionIsLogged() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                ),
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null),
                    listOf(FitnessPermission(FitnessPermissionType.DISTANCE, READ)),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )
        atLeastOneDataSourceSelected.postValue(true)

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()
                }

                scrollToTextAndClick(TEST_APP_NAME)
                scrollToTextAndClick("Exercise")
                onView(allOf(withText("Allow"), isDescendantOfA(withId(R.id.action_container))))
                    .perform(click())

                verify(viewModel).grantPermissions()
                verify(logger).logInteraction(MatchmakingElement.MATCHMAKING_EXPANDABLE_PREFERENCE)
                verify(logger)
                    .logInteraction(PermissionsElement.PERMISSION_SWITCH, UIAction.ACTION_TOGGLE_ON)
                verify(logger).logInteraction(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_cancelPermissionsClicked_interactionIsLogged() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                ),
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null),
                    listOf(FitnessPermission(FitnessPermissionType.DISTANCE, READ)),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )

        ActivityScenario.launchActivityForResult<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()
                }

                onView(
                        allOf(
                            withText("Don\u0027t allow"),
                            isDescendantOfA(withId(R.id.action_container)),
                        )
                    )
                    .perform(click())

                verify(viewModel).removeAllPermissionsFromGrantedList(CALLING_PACKAGE_NAME)
                verify(logger).logInteraction(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_withSingleApp_expandsPreferenceByDefault() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                )
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )
        expandedKeys.postValue(emptySet())

        ActivityScenario.launchActivityForResult<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()

                    val expandablePreference =
                        fragment.findPreference<HealthExpandablePreference>(TEST_APP_PACKAGE_NAME)
                    assertThat(expandablePreference?.mIsExpanded).isTrue()
                }
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_withMultipleApps_doesNotExpandPreferenceByDefault() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                ),
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null),
                    listOf(FitnessPermission(FitnessPermissionType.DISTANCE, READ)),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )
        expandedKeys.postValue(emptySet())

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()

                    val expandablePreference1 =
                        fragment.findPreference<HealthExpandablePreference>(TEST_APP_PACKAGE_NAME)
                    assertThat(expandablePreference1?.mIsExpanded).isFalse()

                    val expandablePreference2 =
                        fragment.findPreference<HealthExpandablePreference>(TEST_APP_PACKAGE_NAME_2)
                    assertThat(expandablePreference2?.mIsExpanded).isFalse()
                }
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingHeaderIconView_correctlyDisplaysIcons() {
        val numberOfMatchedApps = 3
        val mockAppIcon: Drawable = ColorDrawable(Color.RED)
        val apps =
            (1..numberOfMatchedApps).map { i ->
                MatchmakingAppData(
                    AppMetadata("com.example.app$i", "App $i", mockAppIcon),
                    listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ)),
                )
            }
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, ColorDrawable(Color.BLUE)),
                apps,
                emptyList(),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                onView(withId(R.id.matchmaking_header_icon_view)).check(matches(isDisplayed()))
                onView(withId(R.id.requesting_app_icon)).check(matches(isDisplayed()))
                onView(withId(R.id.health_connect_icon)).check(matches(isDisplayed()))
                onView(withId(R.id.line_1)).check(matches(isDisplayed()))
                onView(withId(R.id.line_2)).check(matches(isDisplayed()))
                onView(withId(R.id.matched_icons_group)).check(matches(isDisplayed()))
                onView(withId(R.id.matched_app_icon_1_container))
                    .check(matches(withEffectiveVisibility(VISIBLE)))
                onView(withId(R.id.matched_app_icon_2_container))
                    .check(matches(withEffectiveVisibility(GONE)))
                onView(withId(R.id.plus_n_container)).check(matches(isDisplayed()))
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingHeaderIconView_activityRecreated_correctlyDisplaysIcons() {
        val numberOfMatchedApps = 3
        val mockAppIcon: Drawable = ColorDrawable(Color.RED)
        val apps =
            (1..numberOfMatchedApps).map { i ->
                MatchmakingAppData(
                    AppMetadata("com.example.app$i", "App $i", mockAppIcon),
                    listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ)),
                )
            }
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, ColorDrawable(Color.BLUE)),
                apps,
                emptyList(),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }
                onView(withId(R.id.matchmaking_header_icon_view)).check(matches(isDisplayed()))

                scenario.recreate()

                onView(withId(R.id.matchmaking_header_icon_view)).check(matches(isDisplayed()))
                onView(withId(R.id.requesting_app_icon)).check(matches(isDisplayed()))
                onView(withId(R.id.health_connect_icon)).check(matches(isDisplayed()))
                onView(withId(R.id.line_1)).check(matches(isDisplayed()))
                onView(withId(R.id.line_2)).check(matches(isDisplayed()))
                onView(withId(R.id.matched_icons_group)).check(matches(isDisplayed()))
                onView(withId(R.id.matched_app_icon_1_container))
                    .check(matches(withEffectiveVisibility(VISIBLE)))
                onView(withId(R.id.matched_app_icon_2_container))
                    .check(matches(withEffectiveVisibility(GONE)))
                onView(withId(R.id.plus_n_container)).check(matches(isDisplayed()))
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_partialAppPermissions_switchOff() {
        val app =
            MatchmakingAppData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                listOf(
                    FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                    FitnessPermission(FitnessPermissionType.STEPS, READ),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                listOf(app),
                emptyList(),
            )
        )
        // Only one of two permissions granted
        grantedPermissions.postValue(
            mapOf(
                TEST_APP_PACKAGE_NAME to
                    listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ))
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()

                    val expandablePreference =
                        fragment.findPreference<HealthExpandablePreference>(TEST_APP_PACKAGE_NAME)
                    assertThat(expandablePreference?.isChecked).isFalse()
                    assertThat(expandablePreference?.summary).isEqualTo("1 of 2 selected")
                }
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_allAppPermissions_switchOn() {
        val app =
            MatchmakingAppData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                listOf(
                    FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                    FitnessPermission(FitnessPermissionType.STEPS, READ),
                ),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                listOf(app),
                emptyList(),
            )
        )
        // All permissions granted
        grantedPermissions.postValue(mapOf(TEST_APP_PACKAGE_NAME to app.permissions))

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()

                    val expandablePreference =
                        fragment.findPreference<HealthExpandablePreference>(TEST_APP_PACKAGE_NAME)
                    assertThat(expandablePreference?.isChecked).isTrue()
                    assertThat(expandablePreference?.summary).isEqualTo("2 of 2 selected")
                }
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_expandablePreferenceSwitchOn_addsAppPermissions() {
        val appWithMultiplePermissions =
            MatchmakingAppData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                listOf(
                    FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                    FitnessPermission(FitnessPermissionType.STEPS, READ),
                ),
            )
        val apps = listOf(appWithMultiplePermissions)

        ActivityScenario.launchActivityForResult<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    matchmakingState.value =
                        MatchmakingViewModel.MatchmakingState.WithData(
                            AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                            apps,
                            emptyList(),
                        )
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()
                }

                clickSwitchOnRecyclerViewItemWithText(TEST_APP_NAME)

                verify(viewModel, times(1)).addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_expandablePreferenceSwitchOff_removesAppPermissions() {
        val appWithMultiplePermissions =
            MatchmakingAppData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                listOf(
                    FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                    FitnessPermission(FitnessPermissionType.STEPS, READ),
                ),
            )
        val apps = listOf(appWithMultiplePermissions)

        // Initially grant all permissions for this app so we can toggle them off
        grantedPermissions.postValue(
            mapOf(TEST_APP_PACKAGE_NAME to appWithMultiplePermissions.permissions)
        )

        ActivityScenario.launchActivityForResult<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    matchmakingState.value =
                        MatchmakingViewModel.MatchmakingState.WithData(
                            AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                            apps,
                            emptyList(),
                        )
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()
                }

                clickSwitchOnRecyclerViewItemWithText(TEST_APP_NAME)

                verify(viewModel, times(1))
                    .removeAllPermissionsFromGrantedList(TEST_APP_PACKAGE_NAME)
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_deviceSwitchOn_notifiesViewModel() {
        val deviceData =
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                    Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Watch")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build(),
                    false,
                    emptyList(),
                ),
                emptyList(),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
                listOf(deviceData),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                scrollToTextAndClick("Watch")

                verify(viewModel).addDevicePermissionToGrantedList("com.example.watchdevice")
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_deviceSwitchOff_notifiesViewModel() {
        val deviceData =
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                    Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Watch")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build(),
                    false,
                    emptyList(),
                ),
                emptyList(),
            )
        whenever(viewModel.enabledDevicePackages)
            .thenReturn(MutableLiveData(setOf("com.example.watchdevice")))
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
                listOf(deviceData),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                scrollToTextAndClick("Watch")

                verify(viewModel).removeAllPermissionsFromGrantedList("com.example.watchdevice")
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_withDdpFlagDisabled_hidesDevicesSection() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                )
            )
        val devices =
            listOf(
                MatchmakingDeviceData(
                    DeviceDataSourceInfo(
                        DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                        Device.Builder()
                            .setManufacturer("Google")
                            .setModel("Pixel Watch")
                            .setType(Device.DEVICE_TYPE_WATCH)
                            .build(),
                        false,
                        listOf(
                            DeviceDataProviderInfo(
                                "com.google.android.apps.fitness",
                                "MyFit",
                                "",
                                "",
                                emptySet(),
                            )
                        ),
                    ),
                    emptyList(),
                )
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                devices,
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                onView(withText(R.string.matchmaking_screen_devices_category_title))
                    .check(doesNotExist())
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_withDdpFlagEnabled_showsDevicesSection() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                )
            )
        val devices =
            listOf(
                MatchmakingDeviceData(
                    DeviceDataSourceInfo(
                        DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                        Device.Builder()
                            .setManufacturer("Google")
                            .setModel("Pixel Watch")
                            .setType(Device.DEVICE_TYPE_WATCH)
                            .build(),
                        false,
                        listOf(
                            DeviceDataProviderInfo(
                                "com.google.android.apps.fitness",
                                "MyFit",
                                "",
                                "",
                                emptySet(),
                            )
                        ),
                    ),
                    emptyList(),
                )
            )

        whenever(viewModel.enabledDevicePackages).thenReturn(MutableLiveData(emptySet()))
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                devices,
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                scrollToText(context.getString(R.string.matchmaking_screen_devices_category_title))
                onView(withText(R.string.matchmaking_screen_devices_category_title))
                    .check(matches(withEffectiveVisibility(VISIBLE)))
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun deviceSwitch_statePreservedOnRotation() {
        val devices =
            listOf(
                MatchmakingDeviceData(
                    DeviceDataSourceInfo(
                        DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                        Device.Builder()
                            .setManufacturer("Google")
                            .setModel("Pixel Watch")
                            .setType(Device.DEVICE_TYPE_WATCH)
                            .build(),
                        false,
                        listOf(
                            DeviceDataProviderInfo(
                                "com.google.android.apps.fitness",
                                "MyFit",
                                "",
                                "",
                                emptySet(),
                            )
                        ),
                    ),
                    emptyList(),
                )
            )
        whenever(viewModel.enabledDevicePackages)
            .thenReturn(MutableLiveData(setOf("com.example.watchdevice")))
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
                devices,
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                scenario.recreate()

                scrollToText("Watch")
                onView(allOf(withId(R.id.switch_widget), isDisplayed())).check(matches(isChecked()))
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_allowAndContinuePermissionsClicked_interactionIsLogged() {

        val selectedDevice =
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                    Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel Watch")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build(),
                    false,
                    listOf(
                        DeviceDataProviderInfo(
                            "com.google.android.apps.fitness",
                            "MyFit",
                            "",
                            "",
                            emptySet(),
                        )
                    ),
                ),
                emptyList(),
            )
        val app =
            MatchmakingAppData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ)),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                listOf(app),
                listOf(selectedDevice),
            )
        )
        hasSelectedApp.postValue(true)
        hasSelectedDevice.postValue(true)
        atLeastOneDataSourceSelected.postValue(true)

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()

                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()
                }

                onView(withText("Allow and continue")).perform(click())
                verify(viewModel).grantPermissions()
                verify(logger).logInteraction(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_ddpIntentResult_notifiesViewModel() {
        val ddpIntent = Intent(ACTION_SHOW_DEVICE_ONBOARDING)
        ddpIntent.setPackage("com.example.provider")

        Intents.init()
        try {
            val result = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
            intending(hasAction(ACTION_SHOW_DEVICE_ONBOARDING)).respondWith(result)

            ActivityScenario.launch<TestActivity>(
                    Intent(context, TestActivity::class.java).apply {
                        putExtra(
                            HealthConnectManager.EXTRA_RECORD_TYPES,
                            arrayOf(StepsRecord::class.java.name),
                        )
                    }
                )
                .use { scenario ->
                    scenario.onActivity { activity ->
                        activity.supportFragmentManager
                            .beginTransaction()
                            .add(android.R.id.content, MatchmakingFragment())
                            .commitNow()
                    }

                    // Trigger the intent launch via ViewModel state
                    scenario.onActivity {
                        ddpOnboardingState.value =
                            MatchmakingViewModel.DdpOnboardingState.Onboarding(ddpIntent)
                    }
                    // Verify intent was launched
                    intended(
                        hasAction(
                            android.health.connect.HealthConnectManager
                                .ACTION_SHOW_DEVICE_ONBOARDING
                        )
                    )
                    // Verify the onboarding event was consumed
                    verify(viewModel).consumeDdpOnboardingEvent()
                    // Verify ViewModel was notified of the result
                    verify(viewModel).onDdpIntentFinished(Activity.RESULT_OK)
                }
        } finally {
            Intents.release()
        }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_ddpFlowFinished_finishesActivity() {
        ActivityScenario.launchActivityForResult<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                // Trigger the flow finished state
                scenario.onActivity {
                    ddpOnboardingState.value =
                        MatchmakingViewModel.DdpOnboardingState.Finished(Activity.RESULT_OK)
                }

                // Verify the finished event was consumed
                verify(viewModel).consumeDdpOnboardingEvent()
                // Verify the activity is finished (Scenario.getResult() will provide the result
                // once finished)
                assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_buttonText_updatesCorrectly() {
        val app =
            MatchmakingAppData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ)),
            )
        val device =
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                    Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Watch")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build(),
                    false,
                    emptyList(),
                ),
                emptyList(),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                listOf(app),
                listOf(device),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                // Initial state: nothing selected, Allow button disabled
                onView(withId(R.id.primary_button_outline))
                    .check(matches(withText(R.string.request_permissions_allow)))

                // Select App only
                hasSelectedApp.postValue(true)
                hasSelectedDevice.postValue(false)
                onView(withId(R.id.primary_button_outline))
                    .check(matches(withText(R.string.request_permissions_allow)))

                // Select Device only
                hasSelectedApp.postValue(false)
                hasSelectedDevice.postValue(true)
                onView(withId(R.id.primary_button_outline))
                    .check(
                        matches(
                            withText(R.string.migration_pending_permissions_dialog_button_continue)
                        )
                    )

                // Select Both
                hasSelectedApp.postValue(true)
                hasSelectedDevice.postValue(true)
                onView(withId(R.id.primary_button_outline))
                    .check(matches(withText(R.string.allow_and_continue)))
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_withOneDeviceAndZeroApps_hidesAllowAll() {
        val selectedDevice =
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                    Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel Watch")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build(),
                    false,
                    listOf(
                        DeviceDataProviderInfo(
                            "com.google.android.apps.fitness",
                            "MyFit",
                            "",
                            "",
                            emptySet(),
                        )
                    ),
                ),
                emptyList(),
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
                listOf(selectedDevice),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, MatchmakingFragment())
                        .commitNow()
                }

                onView(withText("Allow all")).check(doesNotExist())
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun grantedPermissionsUpdate_appNotFound_doesNotCrash() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ)),
                )
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()

                    // Manually add a preference with a key that is not in the apps list
                    val category =
                        fragment.findPreference<PreferenceCategory>("matchmaking_apps_category")
                    val fakePreference = HealthExpandablePreference(context, null)
                    fakePreference.key = "fake.package"
                    category?.addPreference(fakePreference)
                }

                // Update granted permissions for the fake package - should not crash
                grantedPermissions.postValue(
                    mapOf(
                        "fake.package" to
                            listOf(FitnessPermission(FitnessPermissionType.EXERCISE, READ))
                    )
                )

                onView(withText(context.getString(R.string.matchmaking_screen_title)))
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingFragment_showsCorrectDeviceIcon() {
        val watchPackageName = "com.example.watchdevice"
        val deviceData =
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName(watchPackageName).build(),
                    Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel Watch")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build(),
                    false,
                    listOf(
                        DeviceDataProviderInfo(
                            "com.google.android.apps.fitness",
                            "MyFit",
                            "",
                            "",
                            emptySet(),
                        )
                    ),
                ),
                emptyList(),
            )

        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
                listOf(deviceData),
            )
        )

        ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = MatchmakingFragment()
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, fragment)
                        .commitNow()

                    val devicePreference =
                        fragment.findPreference<MatchmakingDevicePreference>(watchPackageName)

                    assertThat(devicePreference?.title)
                        .isEqualTo(
                            context.getString(
                                R.string.matchmaking_screen_data_from_data_source,
                                "Watch",
                            )
                        )
                    assertThat(devicePreference?.icon).isNotNull()

                    val expectedIconResId =
                        AttributeResolver.getResource(activity, R.attr.deviceWatchIcon)
                    assertThat(expectedIconResId).isEqualTo(R.drawable.ic_device_watch)

                    val defaultAppIcon =
                        androidx.appcompat.content.res.AppCompatResources.getDrawable(
                            activity,
                            R.drawable.ic_apps,
                        )
                    assertThat(devicePreference?.icon?.constantState)
                        .isNotEqualTo(defaultAppIcon?.constantState)

                    assertThat(devicePreference?.summary)
                        .isEqualTo(
                            context.getString(
                                R.string.matchmaking_screen_continue_to_device_preferences_to_enable
                            )
                        )
                }
            }
    }
}
