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
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_STEPS
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.matchmaking.MatchmakingActivity
import com.android.healthconnect.controller.matchmaking.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
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
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MatchmakingActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: MatchmakingViewModel = mock<MatchmakingViewModel>()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()

    private val matchmakingState = MutableLiveData<MatchmakingViewModel.MatchmakingState>()
    private val expandedKeys = MutableLiveData<Set<String>>(emptySet())
    private val atLeastOnePermissionGranted = MutableLiveData(false)
    private val allPermissionsGranted = MutableLiveData(false)
    private val grantedPermissions =
        MutableLiveData<Map<String, List<FitnessPermission>>>(emptyMap())
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        whenever(viewModel.matchmakingState).thenReturn(matchmakingState)
        whenever(viewModel.expandedPreferenceKeys).thenReturn(expandedKeys)
        whenever(viewModel.atLeastOnePermissionGranted).thenReturn(atLeastOnePermissionGranted)
        whenever(viewModel.allPermissionsGranted).thenReturn(allPermissionsGranted)
        whenever(viewModel.grantedPermissions).thenReturn(grantedPermissions)
        setUpMatchingApps()
    }

    private fun setUpMatchingApps() {
        val apps =
            listOf(
                MatchmakingAppData(
                    AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                    listOf(
                        FitnessPermission.fromPermissionString(WRITE_EXERCISE),
                        FitnessPermission.fromPermissionString(WRITE_STEPS),
                    ),
                )
            )
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                apps,
            )
        )
    }

    @After
    fun tearDown() {
        matchmakingState.postValue(MatchmakingViewModel.MatchmakingState.Loading)
    }

    @Test
    @DisableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingActivity_whenFlagIsOff_finishesWithCancelledResult() {
        val intent = Intent(context, MatchmakingActivity::class.java)

        launchActivityForResult<MatchmakingActivity>(intent).use { scenario ->
            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingActivity_launchesBottomSheet() {
        val intent =
            Intent(context, MatchmakingActivity::class.java).apply {
                putExtra(
                    HealthConnectManager.EXTRA_RECORD_TYPES,
                    arrayOf(StepsRecord::class.java.name),
                )
            }

        launchActivityForResult<MatchmakingActivity>(intent).use {
            onView(withText(context.getString(R.string.matchmaking_screen_title)))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(context.getString(R.string.matchmaking_screen_summary, TEST_APP_NAME)))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        context.getString(R.string.matchmaking_screen_data_from_app, TEST_APP_NAME)
                    )
                )
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun onDestroy_callsViewModelReset() {
        val intent =
            Intent(context, MatchmakingActivity::class.java).apply {
                putExtra(
                    HealthConnectManager.EXTRA_RECORD_TYPES,
                    arrayOf(StepsRecord::class.java.name),
                )
            }

        launchActivityForResult<MatchmakingActivity>(intent).use {
            it.moveToState(Lifecycle.State.DESTROYED)

            verify(viewModel).reset()
        }
    }
}
