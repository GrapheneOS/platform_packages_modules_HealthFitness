/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.permissions.connectedapps

import android.content.Intent
import android.content.Intent.*
import android.platform.test.flag.junit.SetFlagsRule
import androidx.core.os.bundleOf
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.preference.PreferenceCategory
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState.NotStarted
import com.android.healthconnect.controller.permissions.app.FitnessAppFragment
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.EXERCISE
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.READ
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.WRITE
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.Constants.SHOW_MANAGE_APP_SECTION
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.logging.AppAccessElement
import com.android.healthconnect.controller.utils.logging.DisconnectAppDialogElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
class FitnessAppFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: AppPermissionViewModel = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()
    @BindValue val additionalAccessViewModel: AdditionalAccessViewModel = mock()
    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        navHostController = TestNavHostController(context)
        hiltRule.inject()

        whenever(viewModel.revokeAllHealthPermissionsState).then { MutableLiveData(NotStarted) }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(false) }
        whenever(viewModel.atLeastOneFitnessPermissionGranted).then { MediatorLiveData(true) }
        whenever(viewModel.showDisableExerciseRouteEvent)
            .thenReturn(MediatorLiveData(AppPermissionViewModel.DisableExerciseRouteDialogEvent()))
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(emptySet<FitnessPermission>())
        }
        val accessDate = Instant.parse("2022-10-20T18:40:13.00Z")
        whenever(viewModel.loadAccessDate(anyString())).thenReturn(accessDate)
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.appInfo).then {
            MutableLiveData(
                AppMetadata(
                    TEST_APP_PACKAGE_NAME,
                    TEST_APP_NAME,
                    context.getDrawable(R.drawable.health_connect_logo),
                )
            )
        }

        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }
        whenever(viewModel.lastReadPermissionDisconnected).then { MutableLiveData(false) }

        // disable animations
        toggleAnimation(false)
        Intents.init()
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
        reset(viewModel)
        reset(additionalAccessViewModel)
        reset(healthPermissionReader)
        // enable animations
        toggleAnimation(true)
        Intents.release()
    }

    @Test
    fun test_noPermissions() {
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf<HealthPermissionStatus>())
        }

        val scenario =
            launchFragment<FitnessAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment = getFragment(activity)
            val readCategory = getPreferenceCategory(fragment, "read_permission_category")

            val writeCategory = getPreferenceCategory(fragment, "write_permission_category")
            assertThat(readCategory?.preferenceCount).isEqualTo(0)
            assertThat(writeCategory?.preferenceCount).isEqualTo(0)
        }
    }

    @Test
    fun test_readPermission() {
        val permission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedFitnessPermissions).then { MutableLiveData(setOf(permission)) }

        val scenario =
            launchFragment<FitnessAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment = getFragment(activity)
            val readCategory = getPreferenceCategory(fragment, "read_permission_category")

            val writeCategory = getPreferenceCategory(fragment, "write_permission_category")
            assertThat(readCategory?.preferenceCount).isEqualTo(1)
            assertThat(writeCategory?.preferenceCount).isEqualTo(0)
        }

        onView(withText("Distance")).check(matches(isDisplayed()))
    }

    @Test
    fun test_writePermission() {
        val permission = FitnessPermission(EXERCISE, WRITE)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedFitnessPermissions).then { MutableLiveData(setOf(permission)) }

        val scenario =
            launchFragment<FitnessAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment = getFragment(activity)
            val readCategory = getPreferenceCategory(fragment, "read_permission_category")
            val writeCategory = getPreferenceCategory(fragment, "write_permission_category")

            assertThat(readCategory?.preferenceCount).isEqualTo(0)
            assertThat(writeCategory?.preferenceCount).isEqualTo(1)
        }
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun test_readAndWritePermission() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }

        val scenario =
            launchFragment<FitnessAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment = getFragment(activity)
            val readCategory = getPreferenceCategory(fragment, "read_permission_category")
            val writeCategory = getPreferenceCategory(fragment, "write_permission_category")

            assertThat(readCategory?.preferenceCount).isEqualTo(1)
            assertThat(writeCategory?.preferenceCount).isEqualTo(1)
        }
        onView(withText("Exercise")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.APP_ACCESS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        // TODO (b/325680041) investigate why these are not active
        verify(healthConnectLogger, times(2))
            .logImpression(AppAccessElement.PERMISSION_SWITCH_INACTIVE)
        verify(healthConnectLogger)
            .logImpression(AppAccessElement.ALLOW_ALL_PERMISSIONS_SWITCH_INACTIVE)
    }

    @Test
    fun test_allowAllToggleOn_whenAllPermissionsOn() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }

        val scenario =
            launchFragment<FitnessAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment = getFragment(activity)
            val mainSwitchPreference =
                fragment.preferenceScreen.findPreference("allow_all_preference")
                    as HealthMainSwitchPreference?

            assertThat(mainSwitchPreference?.isChecked).isTrue()
        }
        // TODO (b/325680041) investigate why not active
        verify(healthConnectLogger)
            .logImpression(AppAccessElement.ALLOW_ALL_PERMISSIONS_SWITCH_INACTIVE)
    }

    @Test
    fun test_allowAllToggleOff_whenAtLeastOnePermissionOff() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(false) }

        val scenario =
            launchFragment<FitnessAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment = getFragment(activity)

            val mainSwitchPreference =
                fragment.preferenceScreen.findPreference("allow_all_preference")
                    as HealthMainSwitchPreference?

            assertThat(mainSwitchPreference?.isChecked).isFalse()
        }
    }

    @Test
    fun allowAll_toggleOff_withAdditional_andMedicalPermissions_showsDisconnectDialog() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all fitness and wellness permissions?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " this data from Health Connect, including background and past data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Also delete fitness data from " + "$TEST_APP_NAME from Health Connect"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
    }

    @Test
    fun allowAll_toggleOff_withBackground_andMedicalPermissions_showsDisconnectDialog() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all fitness and wellness permissions?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " this data from Health Connect, including background data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Also delete fitness data from " + "$TEST_APP_NAME from Health Connect"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
    }

    @Test
    fun allowAll_toggleOff_withPastData_andMedicalPermissions_showsDisconnectDialog() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all fitness and wellness permissions?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " this data from Health Connect, including past data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Also delete fitness data from " + "$TEST_APP_NAME from Health Connect"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
    }

    @Test
    fun allowAll_toggleOff_withAdditional_andNoMedicalPermissions_showsDisconnectDialog() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData<List<HealthPermission.MedicalPermission>>(emptyList())
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all permissions?")).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " any data from Health Connect, including background and past data." +
                        "\n\nThis doesn't affect other permissions this app may have, like location, " +
                        "camera, or microphone."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Also delete " + "$TEST_APP_NAME data from Health Connect"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
    }

    @Test
    fun allowAll_toggleOff_noAdditional_andMedicalPermissions_showsDisconnectDialog() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all fitness and wellness permissions?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " this data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Also delete fitness data from " + "$TEST_APP_NAME from Health Connect"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
    }

    @Test
    fun allowAll_toggleOff_withBackground_andNoMedicalPermissions_showsDisconnectDialog() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData<List<HealthPermission.MedicalPermission>>(emptyList())
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all permissions?")).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " any data from Health Connect, including background data." +
                        "\n\nThis doesn't affect other permissions this app may have, like location, " +
                        "camera, or microphone."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Also delete " + "$TEST_APP_NAME data from Health Connect"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
    }

    @Test
    fun allowAll_toggleOff_noAdditional_andNoMedical_showsDisconnectDialog() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData<List<HealthPermission.MedicalPermission>>(emptyList())
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all permissions?")).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " any data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone, or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Also delete " + "$TEST_APP_NAME data from Health Connect"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
    }

    @Test
    fun allowAll_toggleOff_onDialogRemoveAllClicked_disconnectAllPermissions() {
        whenever(viewModel.revokeFitnessShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeFitnessShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all")).perform(click())
        verify(healthConnectLogger)
            .logInteraction(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)

        onView(withText("Exercise")).check(matches(not(isChecked())))
        onView(withText("Distance")).check(matches(not(isChecked())))
    }

    @Test
    @Ignore("b/369796531 - unignore when more tests added")
    fun allowAll_toggleOff_deleteDataSelected_onDialogRemoveAllClicked_deleteIsCalled() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withId(R.id.dialog_checkbox)).perform(click())
        onView(withText("Remove all")).perform(click())
        verify(healthConnectLogger)
            .logInteraction(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)
        verify(healthConnectLogger)
            .logInteraction(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)

        verify(viewModel).deleteAppData(eq(TEST_APP_PACKAGE_NAME), eq(TEST_APP_NAME))
    }

    @Test
    fun footerWithGrantTime_whenNoHistoryRead_isDisplayed() {
        val permission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedFitnessPermissions).then { MutableLiveData(setOf(permission)) }
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent())
        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onView(
                withText(
                    "$TEST_APP_NAME can read data added after October 20, 2022" +
                        "\n\n" +
                        "To manage other Android permissions this app can " +
                        "access, go to Settings > Apps" +
                        "\n\n" +
                        "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).perform(scrollTo()).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.PRIVACY_POLICY_LINK)
    }

    @Test
    fun footerWithGrantTime_whenHistoryRead_isNotDisplayed() {
        val permission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedFitnessPermissions).then { MutableLiveData(setOf(permission)) }
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent())
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(
                AdditionalAccessViewModel.State(
                    historyReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = false,
                            isGranted = false,
                        )
                )
            )
        }
        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onIdle()
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()
        onView(
                withText(
                    "To manage other Android permissions this app can " +
                        "access, go to Settings > Apps" +
                        "\n\n" +
                        "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.PRIVACY_POLICY_LINK)
    }

    @Test
    fun footerWithoutGrantTime_isDisplayed() {
        val permission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData<Set<FitnessPermission>>(setOf())
        }
        whenever(viewModel.atLeastOneFitnessPermissionGranted).then { MediatorLiveData(false) }
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent())
        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onIdle()
        onView(
                withText(
                    "To manage other Android permissions this app can " +
                        "access, go to Settings > Apps" +
                        "\n\n" +
                        "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).perform(scrollTo()).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.PRIVACY_POLICY_LINK)
    }

    @Test
    @Ignore("b/353512381") // TODO(b/353512381): Unignore when not flaky.
    fun whenClickOnPrivacyPolicyLink_startsRationaleActivity() {
        val rationaleAction = "android.intent.action.VIEW_PERMISSION_USAGE"
        val permission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData<Set<FitnessPermission>>(setOf())
        }
        whenever(viewModel.atLeastOneFitnessPermissionGranted).then { MediatorLiveData(false) }
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent(rationaleAction))

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onIdle()
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()
        onView(
                withText(
                    "To manage other Android permissions this app can " +
                        "access, go to Settings > Apps" +
                        "\n\n" +
                        "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.PRIVACY_POLICY_LINK)

        onView(withText("Read privacy policy")).perform(click())
        intended(hasAction(rationaleAction))
    }

    @Test
    fun seeAppData_showManageAppSectionEnabled_isEnabled_buttonDisplayed() {

        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        launchFragment<FitnessAppFragment>(
            bundleOf(
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                EXTRA_APP_NAME to TEST_APP_NAME,
                SHOW_MANAGE_APP_SECTION to true, // shows manage app permission
            )
        )
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.SEE_APP_DATA_BUTTON)
        onView(withText("Delete app data")).check(doesNotExist())
    }

    @Test
    fun seeAppData_hideManageAppPermissionEnabled_isEnabled_buttonDisplayed() {
        val writePermission = FitnessPermission(EXERCISE, WRITE)
        val readPermission = FitnessPermission(DISTANCE, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }
        launchFragment<FitnessAppFragment>(
            bundleOf(
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                EXTRA_APP_NAME to TEST_APP_NAME,
                SHOW_MANAGE_APP_SECTION to false, // hides manage app permission
            )
        )
        onView(withText("Manage app")).check(doesNotExist())
        onView(withText("See app data")).check(doesNotExist())
    }

    @Test
    fun additionalAccessState_notValid_hidesAdditionalAccess() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onView(withText(R.string.additional_access_label)).check(doesNotExist())
    }

    @Test
    fun additionalAccessState_valid_showsAdditionalAccess() {
        val validState =
            AdditionalAccessViewModel.State(
                exerciseRoutePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
                exercisePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
            )
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(validState)
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onIdle()
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()
        onView(withText(R.string.additional_access_label)).check(matches(isDisplayed()))
    }

    @Test
    fun additionalAccessState_onlyOneAdditionalPermission_showsAdditionalAccess() {
        val validState =
            AdditionalAccessViewModel.State(
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    )
            )
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(validState)
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onIdle()
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()
        onView(withText(R.string.additional_access_label)).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.ADDITIONAL_ACCESS_BUTTON)
    }

    @Test
    fun additionalAccessState_onClick_navigatesToAdditionalAccessFragment() {
        val validState =
            AdditionalAccessViewModel.State(
                exerciseRoutePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
                exercisePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
            )
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(validState)
        }

        launchFragment<FitnessAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        ) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.fitnessAppFragment)
            Navigation.setViewNavController(requireView(), navHostController)
        }
        onIdle()
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()
        onView(withText(R.string.additional_access_label)).perform(click())

        onIdle()
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.additionalAccessFragment)
        verify(healthConnectLogger).logInteraction(AppAccessElement.ADDITIONAL_ACCESS_BUTTON)
    }

    private fun getFragment(activity: TestActivity): HealthPreferenceFragment {
        return activity.supportFragmentManager.findFragmentById(android.R.id.content)
            as HealthPreferenceFragment
    }

    private fun getPreferenceCategory(
        fragment: HealthPreferenceFragment,
        id: String,
    ): PreferenceCategory? {
        return fragment.preferenceScreen.findPreference(id) as PreferenceCategory?
    }
}
