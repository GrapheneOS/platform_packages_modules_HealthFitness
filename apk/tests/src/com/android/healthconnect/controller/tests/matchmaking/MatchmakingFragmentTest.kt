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

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
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
import com.android.healthconnect.controller.tests.utils.scrollToText
import com.android.healthconnect.controller.tests.utils.scrollToTextAndClick
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
    private val atLeastOnePermissionGranted = MutableLiveData(false)
    private val allPermissionsGranted = MutableLiveData(false)
    private val grantedPermissions =
        MutableLiveData<Map<String, List<FitnessPermission>>>(emptyMap())

    val matchingAppsCount = MutableLiveData(0)

    @Before
    fun setup() {
        hiltRule.inject()

        whenever(viewModel.matchmakingState).thenReturn(matchmakingState)
        whenever(viewModel.expandedPreferenceKeys).thenReturn(expandedKeys)
        whenever(viewModel.atLeastOnePermissionGranted).thenReturn(atLeastOnePermissionGranted)
        whenever(viewModel.allPermissionsGranted).thenReturn(allPermissionsGranted)
        whenever(viewModel.grantedPermissions).thenReturn(grantedPermissions)
        whenever(viewModel.matchingAppsCount).thenReturn(matchingAppsCount)
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

                onView(withText("Share data between apps"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withText("Share data between apps"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow the Calling App app to read data from other apps on this device using Health\u00A0Connect. This data can also be read by other apps you give access to."
                        )
                    )
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withText("Data from $TEST_APP_NAME"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withText("1 of 2 selected"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
                onView(withText("Data from $TEST_APP_NAME")).perform(scrollTo()).perform(click())
                onView(withText("Exercise")).perform(scrollTo()).check(matches(isDisplayed()))
                onView(withText("Steps")).perform(scrollTo()).check(matches(isDisplayed()))
                val policyString = context.getString(R.string.request_permissions_privacy_policy)
                val rationaleText =
                    context.resources.getString(
                        R.string.app_privacy_policy_footer,
                        TEST_APP_NAME,
                        policyString,
                    )
                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
                onView(withText(rationaleText)).check(matches(isDisplayed()))
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

                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText("Allow all"))
                        )
                    )
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

                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText("Allow all"))
                        )
                    )
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
        atLeastOnePermissionGranted.postValue(true)

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

                scrollToTextAndClick("Data from $TEST_APP_NAME")
                scrollToTextAndClick("Exercise")
                onView(withText("Allow")).perform(click())

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

                onView(withText("Don\u0027t allow")).perform(click())

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

                scrollToText("Data from $TEST_APP_NAME")
                onView(allOf(withId(R.id.switch_widget))).perform(click())

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
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                apps,
                emptyList(),
            )
        )
        // Initially grant all permissions for this app so we can toggle them off
        whenever(viewModel.grantedPermissions)
            .thenReturn(
                MutableLiveData(
                    mapOf(TEST_APP_PACKAGE_NAME to appWithMultiplePermissions.permissions)
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

                scrollToText("Data from $TEST_APP_NAME")
                onView(allOf(withId(R.id.switch_widget))).perform(click())

                verify(viewModel, times(1))
                    .removeAllPermissionsFromGrantedList(TEST_APP_PACKAGE_NAME)
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
                            .setType(2)
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
                            .setType(2)
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
                            .setType(2)
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

                onView(allOf(withId(R.id.switch_widget), isDisplayed())).check(matches(isChecked()))
            }
    }
}
