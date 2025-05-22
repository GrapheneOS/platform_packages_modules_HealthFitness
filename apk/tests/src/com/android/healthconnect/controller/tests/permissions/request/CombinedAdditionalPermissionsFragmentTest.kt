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
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.request.AdditionalScreenState
import com.android.healthconnect.controller.permissions.request.CombinedAdditionalPermissionsFragment
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.any
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RequestCombinedAdditionalPermissionsElement
import com.android.healthconnect.controller.utils.logging.UIAction
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
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CombinedAdditionalPermissionsFragmentTest {

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
        whenever(viewModel.grantedAdditionalPermissions).then {
            MutableLiveData(emptySet<AdditionalPermission>())
        }
        toggleAnimation(false)
    }

    @After
    fun teardown() {
        Mockito.reset(healthConnectLogger)
        toggleAnimation(true)
    }

    @Test
    fun withoutMedical_withAccessDate_displaysCorrectly() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = false,
                    isMedicalReadGranted = false,
                    isFitnessReadGranted = false,
                    dataAccessDate = NOW,
                )
            )
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))

        onView(withText("Access past data")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Allow this app to access Health Connect data added before October 20, 2022"
                )
            )
            .check(matches(isDisplayed()))

        onView(withText("Access data in the background")).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(
                        withText(
                            "Allow this app to access Health Connect data when you're not using the app"
                        )
                    )
                )
            )
        onView(
                withText(
                    "Allow this app to access Health Connect data when you're not using the app"
                )
            )
            .check(matches(isDisplayed()))

        onView(withText("Don't allow")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))

        verify(healthConnectLogger, atLeast(1))
            .setPageId(PageName.REQUEST_COMBINED_ADDITIONAL_PERMISSIONS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger)
            .logImpression(
                RequestCombinedAdditionalPermissionsElement
                    .ALLOW_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON
            )
        verify(healthConnectLogger)
            .logImpression(
                RequestCombinedAdditionalPermissionsElement
                    .CANCEL_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON
            )
        verify(healthConnectLogger)
            .logImpression(RequestCombinedAdditionalPermissionsElement.BACKGROUND_READ_BUTTON)
        verify(healthConnectLogger)
            .logImpression(RequestCombinedAdditionalPermissionsElement.HISTORY_READ_BUTTON)
    }

    @Test
    fun withoutMedical_withoutAccessDate_displaysFallback() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = false,
                    isMedicalReadGranted = false,
                    isFitnessReadGranted = false,
                    dataAccessDate = null,
                )
            )
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))

        onView(withText("Access past data")).check(matches(isDisplayed()))
        onView(withText("Allow this app to access all past Health Connect data"))
            .check(matches(isDisplayed()))

        onView(withText("Access data in the background")).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(
                        withText(
                            "Allow this app to access Health Connect data when you're not using the app"
                        )
                    )
                )
            )
        onView(
                withText(
                    "Allow this app to access Health Connect data when you're not using the app"
                )
            )
            .check(matches(isDisplayed()))

        onView(withText("Don't allow")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @Test
    fun medicalNotGranted_fitnessGranted_withAccessDate_displaysCorrectly() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = false,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))

        onView(withText("Access past fitness and wellness data")).check(matches(isDisplayed()))
        onView(withText("Allow this app to access data added before October 20, 2022"))
            .check(matches(isDisplayed()))

        onView(withText("Access fitness and wellness data in the background"))
            .check(matches(isDisplayed()))
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(
                        withText("Allow this app to access this data when you're not using the app")
                    )
                )
            )
        onView(withText("Allow this app to access this data when you're not using the app"))
            .check(matches(isDisplayed()))

        onView(withText("Don't allow")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @Test
    fun medicalNotGranted_fitnessGranted_withoutAccessDate_displaysFallback() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = false,
                    isFitnessReadGranted = true,
                    dataAccessDate = null,
                )
            )
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))

        onView(withText("Access past fitness and wellness data")).check(matches(isDisplayed()))
        onView(withText("Allow this app to access all past data")).check(matches(isDisplayed()))

        onView(withText("Access fitness and wellness data in the background"))
            .check(matches(isDisplayed()))
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(
                        withText("Allow this app to access this data when you're not using the app")
                    )
                )
            )
        onView(withText("Allow this app to access this data when you're not using the app"))
            .check(matches(isDisplayed()))

        onView(withText("Don't allow")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @Test
    fun medicalGranted_fitnessGranted_withAccessDate_displaysFooter() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))

        onView(withText("Access past fitness and wellness data")).check(matches(isDisplayed()))
        onView(withText("Allow this app to access data added before October 20, 2022"))
            .check(matches(isDisplayed()))

        onView(withText("Access all data in the background")).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(
                        withText(
                            "Allow this app to access fitness and wellness data and health records " +
                                "when you're not using the app"
                        )
                    )
                )
            )
        onView(
                withText(
                    "Allow this app to access fitness and wellness data and health records " +
                        "when you're not using the app"
                )
            )
            .check(matches(isDisplayed()))
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
        Espresso.onIdle()
        onView(withText("$TEST_APP_NAME can already access past data for your health records"))
            .check(matches(isDisplayed()))

        onView(withText("Don't allow")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @Test
    fun medicalGranted_fitnessGranted_withoutAccessDate_displaysFallback() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                    dataAccessDate = null,
                )
            )
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))

        onView(withText("Access past fitness and wellness data")).check(matches(isDisplayed()))
        onView(withText("Allow this app to access all past data")).check(matches(isDisplayed()))

        onView(withText("Access all data in the background")).check(matches(isDisplayed()))

        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(
                        withText(
                            "Allow this app to access fitness and wellness data and health records " +
                                "when you're not using the app"
                        )
                    )
                )
            )
        onView(
                withText(
                    "Allow this app to access fitness and wellness data and health records " +
                        "when you're not using the app"
                )
            )
            .check(matches(isDisplayed()))

        onView(withText("Don't allow")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @Test
    fun toggleOn_updatesAdditionalPermission() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }
        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(withText("Access all data in the background"))
                )
            )
        onView(withText("Access all data in the background")).check(matches(isDisplayed()))
        onView(withText("Access all data in the background")).perform(click())

        Mockito.verify(viewModel)
            .updateHealthPermission(any(AdditionalPermission::class.java), eq(true))

        verify(healthConnectLogger)
            .logInteraction(
                RequestCombinedAdditionalPermissionsElement.BACKGROUND_READ_BUTTON,
                UIAction.ACTION_TOGGLE_ON,
            )
    }

    @Test
    fun toggleOff_updatesAdditionalPermission() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }

        whenever(viewModel.grantedAdditionalPermissions).then {
            MutableLiveData(setOf(AdditionalPermission.READ_HEALTH_DATA_HISTORY))
        }
        whenever(
                viewModel.isPermissionLocallyGranted(
                    eq(AdditionalPermission.READ_HEALTH_DATA_HISTORY)
                )
            )
            .thenReturn(true)

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())
        onView(withText("Access past fitness and wellness data")).check(matches(isDisplayed()))
        onView(withText("Access past fitness and wellness data")).perform(click())

        Mockito.verify(viewModel)
            .updateHealthPermission(any(AdditionalPermission::class.java), eq(false))

        verify(healthConnectLogger)
            .logInteraction(
                RequestCombinedAdditionalPermissionsElement.HISTORY_READ_BUTTON,
                UIAction.ACTION_TOGGLE_OFF,
            )
    }

    @Test
    fun allowButton_noAdditionalPermissionsSelected_isDisabled() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }

        whenever(viewModel.grantedAdditionalPermissions).then {
            MutableLiveData(emptySet<AdditionalPermission>())
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow")).check(matches(ViewMatchers.isNotEnabled()))
    }

    @Test
    fun allowButton_additionalPermissionsSelected_isEnabled() {
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }
        whenever(viewModel.grantedAdditionalPermissions).then {
            MutableLiveData(setOf(AdditionalPermission.READ_HEALTH_DATA_HISTORY))
        }

        launchFragment<CombinedAdditionalPermissionsFragment>(bundleOf())

        onView(withText("Allow")).check(matches(ViewMatchers.isEnabled()))
    }
}
