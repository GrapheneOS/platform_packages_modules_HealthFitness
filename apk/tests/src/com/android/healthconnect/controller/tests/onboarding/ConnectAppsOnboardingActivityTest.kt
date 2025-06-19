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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.onboarding.ConnectAppsOnboardingActivity
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
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
class ConnectAppsOnboardingActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val viewModel: OnboardingViewModel = mock()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().context
        (deviceInfoUtils as FakeDeviceInfoUtils).setHealthConnectAvailable(true)
    }

    @Test
    fun noAppsConnected_intentLaunchesConnectAppsOnboardingActivity() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, false),
                        ConnectedFitnessAppMetadata(TEST_APP_2, false),
                    )
                )
            )
        }

        // TODO (b/416731816) replace with correct action
        val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        ConnectAppsOnboardingActivity::class.java,
                    )
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        launchActivityForResult<ConnectAppsOnboardingActivity>(startActivityIntent).use {
            onIdle()
            onView(withText("Connect your first app")).check(matches(isDisplayed()))
        }
    }
}
