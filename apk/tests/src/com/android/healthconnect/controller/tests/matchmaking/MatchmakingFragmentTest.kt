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
import android.health.connect.HealthConnectManager
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
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.matchmaking.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.MatchmakingFragment
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.READ
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MatchmakingFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: MatchmakingViewModel = mock<MatchmakingViewModel>()
    @BindValue val appInfoReader: AppInfoReader = mock<AppInfoReader>()

    @BindValue val deviceInfoUtils: DeviceInfoUtils = mock<DeviceInfoUtils>()

    private val matchmakingState = MutableLiveData<MatchmakingViewModel.MatchmakingState>()
    private val expandedKeys = MutableLiveData<Set<String>>(emptySet())
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        whenever(viewModel.matchmakingState).thenReturn(matchmakingState)
        whenever(viewModel.expandedPreferenceKeys).thenReturn(expandedKeys)
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingFragment_withDataState_showsCorrectContent() {
        val apps =
            setOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    setOf(
                        HealthPermission.FitnessPermission(FitnessPermissionType.EXERCISE, READ),
                        HealthPermission.FitnessPermission(FitnessPermissionType.STEPS, READ),
                    ),
                )
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(TEST_APP_NAME, apps)
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
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())

        onView(withText(context.getString(R.string.matchmaking_screen_title)))
            .check(matches(isDisplayed()))
        onView(withText(context.getString(R.string.matchmaking_screen_summary, TEST_APP_NAME)))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    context.getString(R.string.matchmaking_screen_data_from_app, TEST_APP_NAME)
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(
                withText(
                    context.getString(R.string.matchmaking_screen_data_from_app, TEST_APP_NAME)
                )
            )
            .perform(scrollTo())
            .perform(click())

        onView(
                withText(
                    context.getString(
                        FitnessPermissionStrings.fromPermissionType(FitnessPermissionType.EXERCISE)
                            .uppercaseLabel
                    )
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    context.getString(
                        FitnessPermissionStrings.fromPermissionType(FitnessPermissionType.STEPS)
                            .uppercaseLabel
                    )
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }
}
