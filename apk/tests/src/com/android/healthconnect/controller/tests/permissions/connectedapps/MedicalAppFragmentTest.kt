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
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.MedicalAppFragment
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType.ALL_MEDICAL_DATA
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType.VACCINES
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.Constants.SHOW_MANAGE_APP_SECTION
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
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
class MedicalAppFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

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
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(false) }
        whenever(viewModel.atLeastOneMedicalPermissionGranted).then { MediatorLiveData(true) }
        whenever(viewModel.showDisableExerciseRouteEvent)
            .thenReturn(MediatorLiveData(AppPermissionViewModel.DisableExerciseRouteDialogEvent()))
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(emptySet<MedicalPermission>())
        }
        val accessDate = Instant.parse("2022-10-20T18:40:13.00Z")
        whenever(viewModel.loadAccessDate(anyString())).thenReturn(accessDate)
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
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
        // enable animations
        toggleAnimation(true)
        Intents.release()
    }

    @Test
    fun noPermissions() {
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf<HealthPermissionStatus>())
        }

        val scenario =
            launchFragment<MedicalAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as MedicalAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(0)
            assertThat(writeCategory?.preferenceCount).isEqualTo(0)
        }
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun readPermission() {
        val permission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedMedicalPermissions).then { MutableLiveData(setOf(permission)) }

        val scenario =
            launchFragment<MedicalAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as MedicalAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(1)
            assertThat(writeCategory?.preferenceCount).isEqualTo(0)
        }
        onView(withText("Vaccines")).check(matches(isDisplayed()))
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun logPageImpression() {
        val permission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedMedicalPermissions).then { MutableLiveData(setOf(permission)) }
        launchFragment<MedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.MEDICAL_APP_ACCESS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(1))
            .logImpression(AppAccessElement.PERMISSION_SWITCH_INACTIVE)
        verify(healthConnectLogger)
            .logImpression(AppAccessElement.ALLOW_ALL_PERMISSIONS_SWITCH_INACTIVE)
    }

    @Test
    fun writePermission() {
        val permission = MedicalPermission(ALL_MEDICAL_DATA)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedMedicalPermissions).then { MutableLiveData(setOf(permission)) }

        val scenario =
            launchFragment<MedicalAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as MedicalAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(0)
            assertThat(writeCategory?.preferenceCount).isEqualTo(1)
        }
        onView(withText("All health records")).check(matches(isDisplayed()))
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun readAndWritePermission() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }

        val scenario =
            launchFragment<MedicalAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as MedicalAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(1)
            assertThat(writeCategory?.preferenceCount).isEqualTo(1)
        }
        onView(withText("All health records")).check(matches(isDisplayed()))
        onView(withText("Vaccines")).check(matches(isDisplayed()))
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun allowAllToggleOn_whenAllPermissionsOn() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }

        val scenario =
            launchFragment<MedicalAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as MedicalAppFragment
            val mainSwitchPreference =
                fragment.preferenceScreen.findPreference("allow_all_preference")
                    as HealthMainSwitchPreference?

            assertThat(mainSwitchPreference?.isChecked).isTrue()
        }
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun allowAllToggleOff_whenAtLeastOnePermissionOff() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(false) }

        val scenario =
            launchFragment<MedicalAppFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as MedicalAppFragment

            val mainSwitchPreference =
                fragment.preferenceScreen.findPreference("allow_all_preference")
                    as HealthMainSwitchPreference?

            assertThat(mainSwitchPreference?.isChecked).isFalse()
        }
    }

    @Test
    fun allowAll_toggleOff_withAdditionalPermissions_showsDisconnectDialog() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeMedicalShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeMedicalShouldIncludePastData()).thenReturn(true)
        launchFragment<MedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all health record permissions?")).check(matches(isDisplayed()))
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
        onView(withText("Also delete health records from " + "$TEST_APP_NAME from Health Connect"))
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
    fun allowAll_toggleOff_withBackgroundPermission_showsDisconnectDialog() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeMedicalShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeMedicalShouldIncludePastData()).thenReturn(false)
        launchFragment<MedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all health record permissions?")).check(matches(isDisplayed()))
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
        onView(withText("Also delete health records from " + "$TEST_APP_NAME from Health Connect"))
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
    fun allowAll_toggleOff_withPastDataPermission_showsDisconnectDialog() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeMedicalShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeMedicalShouldIncludePastData()).thenReturn(true)
        launchFragment<MedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all health record permissions?")).check(matches(isDisplayed()))
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
        onView(withText("Also delete health records from " + "$TEST_APP_NAME from Health Connect"))
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
    fun allowAll_toggleOff_noAdditionalPermissions_showsDisconnectDialog() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeMedicalShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeMedicalShouldIncludePastData()).thenReturn(false)
        launchFragment<MedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all health record permissions?")).check(matches(isDisplayed()))
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
        onView(withText("Also delete health records from " + "$TEST_APP_NAME from Health Connect"))
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
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeMedicalShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeMedicalShouldIncludePastData()).thenReturn(true)
        launchFragment<MedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        onView(withText("Remove all")).perform(click())
        verify(healthConnectLogger)
            .logInteraction(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON)

        onView(withText("All health records")).check(matches(not(isChecked())))
        onView(withText("Vaccines")).check(matches(not(isChecked())))
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    @Ignore("b/369796531 - unignore when more tests added")
    fun allowAll_toggleOff_deleteDataSelected_onDialogRemoveAllClicked_deleteIsCalled() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        launchFragment<MedicalAppFragment>(
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
    fun footerWithGrantTime_whenNoHistoryRead_isNotDisplayed() {
        val permission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedMedicalPermissions).then { MutableLiveData(setOf(permission)) }
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent())
        launchFragment<MedicalAppFragment>(
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
    fun footerWithGrantTime_whenHistoryRead_isNotDisplayed() {
        val permission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedMedicalPermissions).then { MutableLiveData(setOf(permission)) }
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
        launchFragment<MedicalAppFragment>(
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
        val permission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData<Set<MedicalPermission>>(setOf())
        }
        whenever(viewModel.atLeastOneMedicalPermissionGranted).then { MediatorLiveData(false) }
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent())
        launchFragment<MedicalAppFragment>(
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
    @Ignore("b/353512381") // TODO(b/353512381): Unignore when not flaky.
    fun whenClickOnPrivacyPolicyLink_startsRationaleActivity() {
        val rationaleAction = "android.intent.action.VIEW_PERMISSION_USAGE"
        val permission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(permission)) }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData<Set<MedicalPermission>>(setOf())
        }
        whenever(viewModel.atLeastOneMedicalPermissionGranted).then { MediatorLiveData(false) }
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent(rationaleAction))

        launchFragment<MedicalAppFragment>(
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
    fun seeAppData_shouldShowManageDataSection_displayed() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        launchFragment<MedicalAppFragment>(
            bundleOf(
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                EXTRA_APP_NAME to TEST_APP_NAME,
                SHOW_MANAGE_APP_SECTION to true,
            )
        )
        onView(withText("Manage app")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Delete app data")).check(doesNotExist())
    }

    @Test
    fun seeAppData_shouldNotSowManageDataSection_notDisplayed() {
        val writePermission = MedicalPermission(ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }
        launchFragment<MedicalAppFragment>(
            bundleOf(
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                EXTRA_APP_NAME to TEST_APP_NAME,
                SHOW_MANAGE_APP_SECTION to false,
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

        launchFragment<MedicalAppFragment>(
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

        launchFragment<MedicalAppFragment>(
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

        launchFragment<MedicalAppFragment>(
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

        launchFragment<MedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        ) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.medicalAppFragment)
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
}
