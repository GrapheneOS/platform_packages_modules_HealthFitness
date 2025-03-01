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

import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PREGNANCY
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PROCEDURES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VISITS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS
import android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA
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
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.request.MedicalPermissionsFragment
import com.android.healthconnect.controller.permissions.request.MedicalScreenState
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
class MedicalPermissionsFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: RequestPermissionViewModel = mock(RequestPermissionViewModel::class.java)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock(HealthConnectLogger::class.java)

    private lateinit var appMetadata: AppMetadata
    private lateinit var allMedicalReadPermissions: List<MedicalPermission>
    private lateinit var allMedicalWritePermissions: List<MedicalPermission>
    private lateinit var allMedicalPermissions: List<MedicalPermission>

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
        allMedicalReadPermissions =
            listOf(
                fromPermissionString(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES),
                fromPermissionString(READ_MEDICAL_DATA_CONDITIONS),
                fromPermissionString(READ_MEDICAL_DATA_LABORATORY_RESULTS),
                fromPermissionString(READ_MEDICAL_DATA_MEDICATIONS),
                fromPermissionString(READ_MEDICAL_DATA_PERSONAL_DETAILS),
                fromPermissionString(READ_MEDICAL_DATA_PRACTITIONER_DETAILS),
                fromPermissionString(READ_MEDICAL_DATA_PREGNANCY),
                fromPermissionString(READ_MEDICAL_DATA_PROCEDURES),
                fromPermissionString(READ_MEDICAL_DATA_SOCIAL_HISTORY),
                fromPermissionString(READ_MEDICAL_DATA_VACCINES),
                fromPermissionString(READ_MEDICAL_DATA_VISITS),
                fromPermissionString(READ_MEDICAL_DATA_VITAL_SIGNS),
            )
        allMedicalWritePermissions = listOf(fromPermissionString(WRITE_MEDICAL_DATA))
        allMedicalPermissions = allMedicalReadPermissions + allMedicalWritePermissions
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(MedicalScreenState.NoMedicalData)
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MutableLiveData(false) }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(emptySet<MedicalPermission>())
        }
        toggleAnimation(false)
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
        toggleAnimation(true)
    }

    @Test
    fun medicalReadAndWrite_displaysMedicalCategories() {
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalReadWrite(
                    appMetadata = appMetadata,
                    medicalPermissions = allMedicalPermissions,
                )
            )
        }

        launchFragment<MedicalPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access your health records?"))
            .check(matches(isDisplayed()))
        onView(withText("Choose data you want this app to read or write to Health Connect"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give access, the app can read and write data such as allergies, lab results, vaccines and more\nAbout health records"
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
    }

    @Test
    fun medicalReadAndWrite_phrUiTelemetryFlagEnabled_phrTelemetry() {
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalReadWrite(
                    appMetadata = appMetadata,
                    medicalPermissions = allMedicalPermissions,
                )
            )
        }
        launchFragment<MedicalPermissionsFragment>(bundleOf())

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.REQUEST_MEDICAL_PERMISSIONS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(13)).logImpression(PermissionsElement.PERMISSION_SWITCH)
        verify(healthConnectLogger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
    }

    @Test
    fun medicalRead_displaysOnlyReadPermissions() {
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalRead(
                    appMetadata = appMetadata,
                    medicalPermissions = allMedicalReadPermissions,
                )
            )
        }

        launchFragment<MedicalPermissionsFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access your health records?"))
            .check(matches(isDisplayed()))
        onView(withText("Choose data you want this app to read from Health Connect"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give access, the app can read data such as allergies, lab results, vaccines and more\nAbout health records"
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
            .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
        Espresso.onIdle()
        onView(withText("Allow \u201C$TEST_APP_NAME\u201D to write")).check(doesNotExist())
        onView(withText("All health records")).check(doesNotExist())
    }

    @Test
    fun togglesPermissions_callsUpdatePermissions() {
        val permissions =
            listOf(
                fromPermissionString(READ_MEDICAL_DATA_VACCINES),
                fromPermissionString(WRITE_MEDICAL_DATA),
            )
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalRead(
                    appMetadata = appMetadata,
                    medicalPermissions = permissions,
                )
            )
        }
        launchFragment<MedicalPermissionsFragment>(bundleOf())
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Vaccines"))
                )
            )
        Espresso.onIdle()
        onView(withText("Vaccines")).perform(click())

        verify(viewModel).updateHealthPermission(any(MedicalPermission::class.java), eq(true))
        verify(healthConnectLogger)
            .logInteraction(PermissionsElement.PERMISSION_SWITCH, UIAction.ACTION_TOGGLE_ON)
    }

    @Test
    fun allowAllToggleOn_updatesAllPermissions() {
        val permissions =
            listOf(
                fromPermissionString(READ_MEDICAL_DATA_VACCINES),
                fromPermissionString(WRITE_MEDICAL_DATA),
            )
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalRead(
                    appMetadata = appMetadata,
                    medicalPermissions = permissions,
                )
            )
        }
        val activityScenario = launchFragment<MedicalPermissionsFragment>(bundleOf())

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

        verify(viewModel).updateMedicalPermissions(eq(true))
        // TODO (b/325680041) this is not triggered?
        //
        // verify(healthConnectLogger).logInteraction(PermissionsElement.ALLOW_ALL_SWITCH,
        // UIAction.ACTION_TOGGLE_ON)
    }

    @Test
    fun allowAllToggleOff_updatesAllPermissions() {
        val permissions =
            listOf(
                fromPermissionString(READ_MEDICAL_DATA_VACCINES),
                fromPermissionString(WRITE_MEDICAL_DATA),
            )
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalRead(
                    appMetadata = appMetadata,
                    medicalPermissions = permissions,
                )
            )
        }
        val activityScenario = launchFragment<MedicalPermissionsFragment>(bundleOf())

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

        assertThat(viewModel.grantedMedicalPermissions.value).isEmpty()
    }

    @Test
    fun allowButton_noMedicalPermissionsSelected_isDisabled() {
        val permissions =
            listOf(
                fromPermissionString(READ_MEDICAL_DATA_VACCINES),
                fromPermissionString(WRITE_MEDICAL_DATA),
            )
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalRead(
                    appMetadata = appMetadata,
                    medicalPermissions = permissions,
                )
            )
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(emptySet<MedicalPermission>())
        }

        launchFragment<MedicalPermissionsFragment>(bundleOf())
        onView(withText("Allow")).check(matches(ViewMatchers.isNotEnabled()))
    }

    @Test
    fun allowButton_medicalPermissionsSelected_isEnabled() {
        val permissions =
            listOf(
                fromPermissionString(READ_MEDICAL_DATA_VACCINES),
                fromPermissionString(WRITE_MEDICAL_DATA),
            )
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalRead(
                    appMetadata = appMetadata,
                    medicalPermissions = permissions,
                )
            )
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(fromPermissionString(READ_MEDICAL_DATA_VACCINES)))
        }

        launchFragment<MedicalPermissionsFragment>(bundleOf())

        onView(withText("Allow")).check(matches(ViewMatchers.isEnabled()))
    }
}
