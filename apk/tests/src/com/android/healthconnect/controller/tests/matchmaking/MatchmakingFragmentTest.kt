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

import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.EnableFlags
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.matchmaking.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.MatchmakingFragment
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
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
import org.hamcrest.core.IsNot.not
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
class MatchmakingFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

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

    @Before
    fun setup() {
        hiltRule.inject()

        whenever(viewModel.matchmakingState).thenReturn(matchmakingState)
        whenever(viewModel.expandedPreferenceKeys).thenReturn(expandedKeys)
        whenever(viewModel.atLeastOnePermissionGranted).thenReturn(atLeastOnePermissionGranted)
        whenever(viewModel.allPermissionsGranted).thenReturn(allPermissionsGranted)
        whenever(viewModel.grantedPermissions).thenReturn(grantedPermissions)
        whenever(deviceInfoUtils.isHealthConnectAvailable(any())).thenReturn(true)
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
            )
        )

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
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
                    "Allow the Calling App app to read data from other apps on this device using Health\u00A0Connect"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Data from $TEST_APP_NAME"))
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
        verify(logger, times(2)).logImpression(MatchmakingElement.MATCHMAKING_EXPANDABLE_PREFERENCE)
        verify(logger, times(3)).logImpression(PermissionsElement.PERMISSION_SWITCH)
        verify(logger).logImpression(MatchmakingElement.MATCHMAKING_SCREEN_FOOTER)
        verify(logger).logImpression(MatchmakingElement.MATCHMAKING_SCREEN_FOOTER_LINK)
        verify(logger).logImpression(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
        verify(logger).logImpression(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
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
            )
        )

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
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

        verify(viewModel).addAllPermissionsToGrantedList()
        verify(logger)
            .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_ON)
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
            )
        )
        allPermissionsGranted.postValue(true)

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
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

        verify(viewModel).removeAllPermissionsFromGrantedList()
        verify(logger)
            .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_OFF)
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
            )
        )
        atLeastOnePermissionGranted.postValue(true)

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
        scenario.onActivity { activity ->
            val fragment = MatchmakingFragment()
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow()
        }

        onView(withText("Data from $TEST_APP_NAME")).perform(scrollTo()).perform(click())
        onView(withText("Exercise")).perform(scrollTo()).perform(click())
        onView(withText("Allow")).perform(click())

        verify(viewModel).grantPermissions()
        verify(logger).logInteraction(MatchmakingElement.MATCHMAKING_EXPANDABLE_PREFERENCE)
        verify(logger)
            .logInteraction(PermissionsElement.PERMISSION_SWITCH, UIAction.ACTION_TOGGLE_ON)
        verify(logger).logInteraction(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
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
            )
        )

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
        scenario.onActivity { activity ->
            val fragment = MatchmakingFragment()
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow()
        }

        onView(withText("Don\'t allow")).perform(click())

        verify(viewModel).removeAllPermissionsFromGrantedList()
        verify(logger).logInteraction(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
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
            )
        )
        expandedKeys.postValue(emptySet())

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
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
            )
        )
        expandedKeys.postValue(emptySet())

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
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
            )
        )

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
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

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_withZeroMatchingApps_hidesIconView() {
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
            )
        )

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
        scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, MatchmakingFragment())
                .commitNow()
        }

        onView(withId(R.id.matchmaking_header_icon_view)).check(matches(not(isDisplayed())))
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
            )
        )

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
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

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun onCreatePreferences_nullRecordTypes_loadsAppsWithEmptySet() {
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
            )
        )

        val scenario =
            ActivityScenario.launch<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    // No EXTRA_RECORD_TYPES
                }
            )
        scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, MatchmakingFragment())
                .commitNow()
        }
        verify(viewModel).loadMatchmakingApps(any(), eq(emptySet()))
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun onCreatePreferences_invalidRecordTypes_loadsAppsWithEmptySet() {
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
            )
        )
        val intent =
            Intent(context, TestActivity::class.java).apply {
                putExtra(HealthConnectManager.EXTRA_RECORD_TYPES, arrayOf("invalid.record.type"))
            }
        val scenario = ActivityScenario.launch<TestActivity>(intent)
        scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, MatchmakingFragment())
                .commitNow()
        }

        verify(viewModel).loadMatchmakingApps(any(), eq(emptySet()))
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun onCreatePreferences_validRecordTypes_loadsAppsWithCorrectRecordTypes() {
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
            )
        )

        val intent =
            Intent(context, TestActivity::class.java).apply {
                putExtra(
                    HealthConnectManager.EXTRA_RECORD_TYPES,
                    arrayOf(HeartRateRecord::class.java.name, StepsRecord::class.java.name),
                )
            }
        val scenario = ActivityScenario.launch<TestActivity>(intent)
        scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, MatchmakingFragment())
                .commitNow()
        }

        verify(viewModel)
            .loadMatchmakingApps(
                any(),
                eq(setOf(HeartRateRecord::class.java, StepsRecord::class.java)),
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun onCreatePreferences_validAndInvalidRecordTypes_loadsAppsWithValidRecordType() {
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(CALLING_PACKAGE_NAME, CALLING_APP_NAME, null),
                emptyList(),
            )
        )

        val intent =
            Intent(context, TestActivity::class.java).apply {
                putExtra(
                    HealthConnectManager.EXTRA_RECORD_TYPES,
                    arrayOf(HeartRateRecord::class.java.name, "invalid.record.type"),
                )
            }
        val scenario = ActivityScenario.launch<TestActivity>(intent)
        scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, MatchmakingFragment())
                .commitNow()
        }

        verify(viewModel).loadMatchmakingApps(any(), eq(setOf(HeartRateRecord::class.java)))
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_nullCallingPackage_finishesWithCanceledResult() {
        val scenario =
            ActivityScenario.launchActivityForResult<TestActivity>(
                Intent(context, TestActivity::class.java).apply {
                    putExtra(
                        HealthConnectManager.EXTRA_RECORD_TYPES,
                        arrayOf(StepsRecord::class.java.name),
                    )
                }
            )
        scenario.onActivity { activity ->
            activity.callingPackageName = null
            activity.supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, MatchmakingFragment())
                .commitNow()
        }

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
    }
}
