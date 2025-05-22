/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.permissions.request

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.request.AdditionalScreenState
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.permissions.request.SingleAdditionalPermissionFragment
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SingleAdditionalPermissionFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: RequestPermissionViewModel = Mockito.mock(RequestPermissionViewModel::class.java)
    @BindValue
    val healthConnectLogger: HealthConnectLogger = Mockito.mock(HealthConnectLogger::class.java)

    private lateinit var appMetadata: AppMetadata

    @Before
    fun setup() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        appMetadata =
            AppMetadata(
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                context.getDrawable(R.drawable.health_connect_logo),
            )

        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(AdditionalScreenState.NoAdditionalData)
        }
        toggleAnimation(false)
    }

    @After
    fun teardown() {
        Mockito.reset(healthConnectLogger)
        toggleAnimation(true)
    }

    @Test
    fun requestHistory_whenNoMedicalDeclared_displaysCorrectText() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowHistory(
                    appMetadata = appMetadata,
                    hasMedical = false,
                    isMedicalReadGranted = false,
                    dataAccessDate = NOW,
                )
            )
        }

        launchFragment<SingleAdditionalPermissionFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access past data?")).check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, this app can access Health Connect data added before October 20, 2022."
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME can already access past data for your health records"))
            .check(doesNotExist())

        verify(healthConnectLogger, atLeast(1))
            .setPageId(PageName.REQUEST_HISTORY_READ_PERMISSION_PAGE)
        verify(healthConnectLogger, times(0))
            .setPageId(PageName.REQUEST_BACKGROUND_READ_PERMISSION_PAGE)
        verify(healthConnectLogger).logPageImpression()
    }

    @Test
    fun requestHistory_whenMedicalDeclared_displaysCorrectText() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowHistory(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = false,
                    dataAccessDate = NOW,
                )
            )
        }

        launchFragment<SingleAdditionalPermissionFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access past data?")).check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, this app can access fitness and wellness data added before October 20, 2022."
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME can already access past data for your health records"))
            .check(doesNotExist())
    }

    @Test
    fun requestHistory_whenMedicalReadGranted_showsFooter() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowHistory(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }

        launchFragment<SingleAdditionalPermissionFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access past data?")).check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, this app can access fitness and wellness data added before October 20, 2022."
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME can already access past data for your health records"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun requestBackground_whenNoMedicalDeclared_displaysCorrectText() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowBackground(
                    appMetadata = appMetadata,
                    hasMedical = false,
                    isMedicalReadGranted = false,
                    isFitnessReadGranted = true,
                )
            )
        }

        launchFragment<SingleAdditionalPermissionFragment>(bundleOf())
        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, this app can access Health Connect data when you're not using the app."
                )
            )
            .check(matches(isDisplayed()))

        verify(healthConnectLogger, atLeast(1))
            .setPageId(PageName.REQUEST_BACKGROUND_READ_PERMISSION_PAGE)
        verify(healthConnectLogger, times(0))
            .setPageId(PageName.REQUEST_HISTORY_READ_PERMISSION_PAGE)
        verify(healthConnectLogger).logPageImpression()
    }

    @Test
    fun requestBackground_whenMedicalDeclared_whenFitnessReadGranted_displaysCorrectText() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowBackground(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = false,
                    isFitnessReadGranted = true,
                )
            )
        }

        launchFragment<SingleAdditionalPermissionFragment>(bundleOf())
        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, this app can access fitness and wellness data when you're not using the app."
                )
            )
            .check(matches(isDisplayed()))
    }

    @Test
    fun requestBackground_whenMedicalDeclaredAndReadGranted_displaysCorrectText() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowBackground(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = false,
                )
            )
        }

        launchFragment<SingleAdditionalPermissionFragment>(bundleOf())
        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, this app can access health records when you're not using the app."
                )
            )
            .check(matches(isDisplayed()))
    }

    @Test
    fun requestBackground_whenMedicalDeclared_whenFitnessAndMedicalReadGranted_displaysCorrectText() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowBackground(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                )
            )
        }

        launchFragment<SingleAdditionalPermissionFragment>(bundleOf())
        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, this app can access fitness and wellness data and health records when you're not using the app."
                )
            )
            .check(matches(isDisplayed()))
    }

    // TODO
    // requestMultipleAdditionalPermissions_showsCombinedFragment()
}
