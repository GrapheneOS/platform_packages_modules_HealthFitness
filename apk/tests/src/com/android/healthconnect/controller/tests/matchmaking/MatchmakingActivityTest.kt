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
import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_STEPS
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.matchmaking.MatchmakingActivity
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
import com.android.healthconnect.controller.matchmaking.api.MatchmakingAppData
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.shared.BottomSheetIdlingResource
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
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
    private val enabledDevicePackages = MutableLiveData<Set<String>>(emptySet())
    private val hasSelectedDevice = MutableLiveData(false)
    private val hasSelectedApp = MutableLiveData(false)
    private val ddpOnboardingState =
        MutableLiveData<MatchmakingViewModel.DdpOnboardingState>(
            MatchmakingViewModel.DdpOnboardingState.Setup
        )
    private val matchingAppsCount = MutableLiveData(0)
    private lateinit var context: Context
    private var bottomSheetIdlingResource: BottomSheetIdlingResource? = null

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().targetContext
        whenever(viewModel.matchmakingState).thenReturn(matchmakingState)
        whenever(viewModel.expandedPreferenceKeys).thenReturn(expandedKeys)
        whenever(viewModel.atLeastOnePermissionGranted).thenReturn(atLeastOnePermissionGranted)
        whenever(viewModel.allPermissionsGranted).thenReturn(allPermissionsGranted)
        whenever(viewModel.grantedPermissions).thenReturn(grantedPermissions)
        whenever(viewModel.matchingAppsCount).thenReturn(matchingAppsCount)
        whenever(viewModel.enabledDevicePackages).thenReturn(enabledDevicePackages)
        whenever(viewModel.hasSelectedDevice).thenReturn(hasSelectedDevice)
        whenever(viewModel.hasSelectedApp).thenReturn(hasSelectedApp)
        whenever(viewModel.ddpOnboardingState).thenReturn(ddpOnboardingState)
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
                emptyList(), // Add an empty list for matchingDevices
            )
        )
        matchingAppsCount.postValue(apps.size)
    }

    @After
    fun tearDown() {
        matchmakingState.postValue(MatchmakingViewModel.MatchmakingState.Loading)
        bottomSheetIdlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
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
    fun matchmakingActivity_withZeroMatchingApps_finishesWithCanceledResult() {
        matchmakingState.postValue(
            MatchmakingViewModel.MatchmakingState.WithData(
                AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null),
                emptyList(),
                emptyList(),
            )
        )
        matchingAppsCount.postValue(0)
        launchMatchmakingActivity().use { scenario ->
            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingActivity_nullRecordTypes_loadsAppsWithNull() {
        val intent = Intent(context, MatchmakingActivity::class.java)
        launchActivityForResult<MatchmakingActivity>(intent).use {
            verify(viewModel).loadMatchmakingData(any(), eq(null), eq(null), eq(null))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingActivity_invalidRecordTypes_loadsAppsWithInvalidRecordType() {
        val intent =
            Intent(context, MatchmakingActivity::class.java).apply {
                putExtra(HealthConnectManager.EXTRA_RECORD_TYPES, arrayOf("invalid.record.type"))
            }
        launchActivityForResult<MatchmakingActivity>(intent).use {
            verify(viewModel)
                .loadMatchmakingData(any(), eq(arrayOf("invalid.record.type")), eq(null), eq(null))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingActivity_validRecordTypes_loadsAppsWithCorrectRecordTypes() {
        val intent =
            Intent(context, MatchmakingActivity::class.java).apply {
                putExtra(
                    HealthConnectManager.EXTRA_RECORD_TYPES,
                    arrayOf(HeartRateRecord::class.java.name, StepsRecord::class.java.name),
                )
            }
        launchActivityForResult<MatchmakingActivity>(intent).use {
            verify(viewModel)
                .loadMatchmakingData(
                    any(),
                    eq(arrayOf(HeartRateRecord::class.java.name, StepsRecord::class.java.name)),
                    eq(null),
                    eq(null),
                )
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    fun matchmakingActivity_validAndInvalidRecordTypes_loadsAppsWithValidRecordType() {
        val intent =
            Intent(context, MatchmakingActivity::class.java).apply {
                putExtra(
                    HealthConnectManager.EXTRA_RECORD_TYPES,
                    arrayOf(HeartRateRecord::class.java.name, "invalid.record.type"),
                )
            }
        launchActivityForResult<MatchmakingActivity>(intent).use {
            verify(viewModel)
                .loadMatchmakingData(
                    any(),
                    eq(arrayOf(HeartRateRecord::class.java.name, "invalid.record.type")),
                    eq(null),
                    eq(null),
                )
        }
    }

    @Test
    @EnableFlags(
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun matchmakingActivity_bothIncludedAndExcludedSources_finishesWithCanceledResult() {
        val intent =
            Intent(context, MatchmakingActivity::class.java).apply {
                putExtra(HealthConnectManager.EXTRA_INCLUDED_DATA_SOURCES, arrayOf("pkg1"))
                putExtra(HealthConnectManager.EXTRA_EXCLUDED_DATA_SOURCES, arrayOf("pkg2"))
            }
        launchActivityForResult<MatchmakingActivity>(intent).use { scenario ->
            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        }
    }

    @EnableFlags(Flags.FLAG_MATCHMAKING)
    @Test
    fun matchmakingScreen_dontAllowButton_isClicked_finishesWithResultCanceled() {
        launchMatchmakingActivity().use { scenario ->
            registerBottomSheetIdlingResource(scenario)
            atLeastOnePermissionGranted.postValue(true)

            onView(withText("Don\u0027t allow")).inRoot(isDialog()).perform(click())

            assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
        }
    }

    private fun launchMatchmakingActivity(): ActivityScenario<MatchmakingActivity> {
        val context = getInstrumentation().targetContext
        val intent =
            Intent(context, MatchmakingActivity::class.java).apply {
                putExtra(
                    HealthConnectManager.EXTRA_RECORD_TYPES,
                    arrayOf(StepsRecord::class.java.name),
                )
            }
        return launchActivityForResult<MatchmakingActivity>(intent)
    }

    private fun registerBottomSheetIdlingResource(scenario: ActivityScenario<MatchmakingActivity>) {
        scenario.onActivity { activity ->
            bottomSheetIdlingResource =
                BottomSheetIdlingResource(activity, "MatchmakingBottomSheet")
            IdlingRegistry.getInstance().register(bottomSheetIdlingResource)
        }
    }
}
