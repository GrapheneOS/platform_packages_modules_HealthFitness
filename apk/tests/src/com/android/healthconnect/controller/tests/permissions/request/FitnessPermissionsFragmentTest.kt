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

import android.health.connect.HealthPermissions.READ_SLEEP
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_HEART_RATE
import android.os.Build
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.request.FitnessPermissionsFragment
import com.android.healthconnect.controller.permissions.request.FitnessScreenState
import com.android.healthconnect.controller.permissions.request.PermissionsFragment
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.any
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.logging.UIAction
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@HiltAndroidTest
class FitnessPermissionsFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: RequestPermissionViewModel = mock(RequestPermissionViewModel::class.java)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock(HealthConnectLogger::class.java)

    private lateinit var appMetadata: AppMetadata
    private lateinit var fitnessReadPermissions: List<FitnessPermission>
    private lateinit var fitnessWritePermissions: List<FitnessPermission>
    private lateinit var fitnessReadWritePermissions: List<FitnessPermission>

    @Before
    fun setup() {
        hiltRule.inject()
        val context = getInstrumentation().context
        context.setLocale(Locale.US)
        appMetadata =
            AppMetadata(
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                context.getDrawable(R.drawable.health_connect_logo),
            )
        fitnessReadPermissions =
            listOf(fromPermissionString(READ_STEPS), fromPermissionString(READ_SLEEP))
        fitnessWritePermissions =
            listOf(fromPermissionString(WRITE_HEART_RATE), fromPermissionString(WRITE_EXERCISE))
        fitnessReadWritePermissions = fitnessReadPermissions + fitnessWritePermissions

        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(emptySet<FitnessPermission>())
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(FitnessScreenState.NoFitnessData)
        }

        toggleAnimation(false)
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
        toggleAnimation(true)
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun fitnessReadAndWrite_noMedical_noHistory_displaysCategories_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
            .check(matches(isDisplayed()))
        onView(withText("Choose data you want this app to read or write to Health Connect"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                )
            )
            .check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to read"))
                )
            )
        Espresso.onIdle()
        onView(withText("Allow \u201C$TEST_APP_NAME\u201D to read")).check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to write"))
                )
            )
        Espresso.onIdle()
        onView(withText("Allow \u201C$TEST_APP_NAME\u201D to write")).check(matches(isDisplayed()))

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.REQUEST_PERMISSIONS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(4)).logImpression(PermissionsElement.PERMISSION_SWITCH)
        verify(healthConnectLogger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun fitnessReadAndWrite_noMedical_noHistory_displaysCategories_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more"
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                )
            )
            .check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to read"))
                )
            )
        Espresso.onIdle()
        onView(withText("Allow \u201C$TEST_APP_NAME\u201D to read")).check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to write"))
                )
            )
        Espresso.onIdle()
        onView(withText("Allow \u201C$TEST_APP_NAME\u201D to write")).check(matches(isDisplayed()))

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.REQUEST_PERMISSIONS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(4)).logImpression(PermissionsElement.PERMISSION_SWITCH)
        verify(healthConnectLogger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
    }

    @Test
    fun whenMedical_headerDisplaysCorrectTitle() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access fitness and wellness data?"))
            .check(matches(isDisplayed()))
        onView(withText("Allow $TEST_APP_NAME to access Health Connect?")).check(doesNotExist())
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenNoMedical_headerDisplaysCorrectTitle_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
            .check(doesNotExist())
        onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
            .check(matches(isDisplayed()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenNoMedical_headerDisplaysCorrectTitle_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
            .check(matches(isDisplayed()))
        onView(withText("Allow $TEST_APP_NAME to access Health Connect?")).check(doesNotExist())
    }

    @Test
    fun whenHistoryReadGranted_headerDisplaysCorrectText() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("If you give read access, the app can read new and past data"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun whenHistoryReadNotGranted_headerDisplaysCorrectText() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("If you give read access, the app can read new and past data"))
            .check(doesNotExist())
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(matches(isDisplayed()))
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenOnlyReadPermissionsRequested_headerDisplaysCorrectText_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessRead(
                    historyGranted = false,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadPermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Choose data you want this app to read from Health Connect"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(matches(isDisplayed()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenOnlyReadPermissionsRequested_headerDisplaysCorrectText_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessRead(
                    historyGranted = false,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadPermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(
                withText(
                    "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more"
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(matches(isDisplayed()))
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenOnlyWritePermissionsRequested_headerDisplaysCorrectText_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Choose data you want this app to write to Health Connect"))
            .check(matches(isDisplayed()))
        onView(withText("If you give read access, the app can read new and past data"))
            .check(doesNotExist())
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(doesNotExist())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenOnlyWritePermissionsRequested_headerDisplaysCorrectText_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(
                withText(
                    "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more"
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("If you give read access, the app can read new and past data"))
            .check(doesNotExist())
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(doesNotExist())
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenReadAndWritePermissionsRequested_headerDisplaysCorrectText_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
            .check(matches(isDisplayed()))
        onView(withText("Choose data you want this app to read or write to Health Connect"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                )
            )
            .check(matches(isDisplayed()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenReadAndWritePermissionsRequested_headerDisplaysCorrectText_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more"
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, the app can read new data and data from the past 30 days"
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                )
            )
            .check(matches(isDisplayed()))
    }

    @Test
    fun displaysReadPermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessRead(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadPermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Steps"))
                )
            )
        Espresso.onIdle()
        onView(withText("Steps")).check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Sleep"))
                )
            )
        Espresso.onIdle()
        onView(withText("Sleep")).check(matches(isDisplayed()))
    }

    @Test
    fun displaysWritePermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Heart rate"))
                )
            )
        Espresso.onIdle()
        onView(withText("Heart rate")).check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Exercise"))
                )
            )
        Espresso.onIdle()
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun togglesPermissions_callsUpdatePermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(bundleOf())
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Sleep"))
                )
            )
        Espresso.onIdle()
        onView(withText("Sleep")).perform(click())

        verify(viewModel).updateHealthPermission(any(FitnessPermission::class.java), eq(true))
        verify(healthConnectLogger)
            .logInteraction(PermissionsElement.PERMISSION_SWITCH, UIAction.ACTION_TOGGLE_ON)
    }

    @Test
    fun allowAllToggleOn_updatesAllPermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        val activityScenario = launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Allow all"))
                )
            )
        var allowAllPreference: HealthMainSwitchPreference? = null
        activityScenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as PermissionsFragment
            allowAllPreference = fragment.preferenceScreen.findPreference("allow_all_preference")
            allowAllPreference?.isChecked =
                false // makes sure the preference is on so OnPreferenceChecked is triggered
        }

        onView(withText(allowAllPreference?.title?.toString())).perform(click())

        verify(viewModel).updateFitnessPermissions(eq(true))
        // TODO (b/325680041) this is not triggered?
        //
        // verify(healthConnectLogger).logInteraction(PermissionsElement.ALLOW_ALL_SWITCH,
        // UIAction.ACTION_TOGGLE_ON)
    }

    @Test
    fun allowAllToggleOff_updatesAllPermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        val activityScenario = launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Allow all"))
                )
            )
        var allowAllPreference: HealthMainSwitchPreference? = null
        activityScenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as PermissionsFragment
            allowAllPreference = fragment.preferenceScreen.findPreference("allow_all_preference")
            allowAllPreference?.isChecked =
                true // makes sure the preference is on so OnPreferenceChecked is triggered
        }

        onView(withText(allowAllPreference?.title?.toString())).perform(click())

        assertThat(viewModel.grantedFitnessPermissions.value).isEmpty()
    }

    @Test
    fun allowButton_noFitnessPermissionsSelected_isDisabled() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(emptySet<FitnessPermission>())
        }

        launchFragment<FitnessPermissionsFragment>(bundleOf())
        onView(withText("Allow")).check(matches(ViewMatchers.isNotEnabled()))
    }

    @Test
    fun allowButton_fitnessPermissionsSelected_isEnabled() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(HealthPermission.fromPermissionString(READ_STEPS)))
        }

        launchFragment<FitnessPermissionsFragment>(bundleOf())

        onView(withText("Allow")).check(matches(ViewMatchers.isEnabled()))
    }
}
