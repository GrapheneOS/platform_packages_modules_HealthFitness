/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.permissions.connectedapps.settings

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.ActivityInfo
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState
import com.android.healthconnect.controller.permissions.app.SettingsMedicalAppFragment
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.NavigationUtils
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.logging.UIAction
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsMedicalAppFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: AppPermissionViewModel = mock()
    @BindValue val navigationUtils: NavigationUtils = mock()
    @BindValue val migrationViewModel: MigrationViewModel = mock()
    @BindValue val additionalAccessViewModel: AdditionalAccessViewModel = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()

        whenever(viewModel.revokeAllHealthPermissionsState).then {
            MutableLiveData(RevokeAllState.NotStarted)
        }
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(emptyList<MedicalPermission>())
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(emptySet<MedicalPermission>())
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(false) }
        whenever(viewModel.atLeastOneMedicalPermissionGranted).then { MediatorLiveData(true) }
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

        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(viewModel.lastReadPermissionDisconnected).then { MutableLiveData(false) }
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    fun fragment_starts() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withText("Allow all")).check(matches(isDisplayed()))
        onView(withText("Allowed to read")).check(matches(isDisplayed()))
        onView(withText("Allowed to write")).check(matches(isDisplayed()))
    }

    @Test
    fun fragmentStarts_logPageImpression() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        verify(healthConnectLogger, atLeast(1))
            .setPageId(PageName.SETTINGS_MANAGE_MEDICAL_APP_PERMISSIONS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
        verify(healthConnectLogger, times(2)).logImpression(PermissionsElement.PERMISSION_SWITCH)
    }

    @Test
    fun doesNotShowWriteHeader_whenNoWritePermissions() {
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(readPermission)) }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(readPermission))
        }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withText("Allow all")).check(matches(isDisplayed()))
        onView(withText("Allowed to read")).check(matches(isDisplayed()))
        onView(withText("Allowed to write")).check(doesNotExist())
    }

    @Test
    fun doesNotShowReadHeader_whenNoReadPermissions() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        whenever(viewModel.medicalPermissions).then { MutableLiveData(listOf(writePermission)) }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withText("Allow all")).check(matches(isDisplayed()))
        onView(withText("Allowed to read")).check(doesNotExist())
        onView(withText("Allowed to write")).check(matches(isDisplayed()))
    }

    @Test
    fun permissionSwitchOn_contentDescription() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission, readPermission))
        }

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )

        onView(withContentDescription("All health records. Write Access. On"))
            .check(matches(isDisplayed()))
        onView(withContentDescription("Vaccines. Read Access. On")).check(matches(isDisplayed()))
    }

    @Test
    fun permissionSwitchOff_contentDescription() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )

        onView(withContentDescription("All health records. Write Access. Off"))
            .check(matches(isDisplayed()))
        onView(withContentDescription("Vaccines. Read Access. Off")).check(matches(isDisplayed()))
    }

    @Test
    fun unsupportedPackage_grantedPermissionsNotLoaded_onOrientationChange() {
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)

        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(readPermission, writePermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(readPermission, writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { false }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withText("Allow all")).check(matches(isDisplayed()))
        onView(withText("Vaccines")).check(matches(isDisplayed()))
        onView(withText("All health records")).check(matches(isDisplayed()))
        onView(withText("Vaccines")).perform(click())
        onView(withText("Vaccines")).check(matches(not(isChecked())))

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        onIdle()
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()
        onView(withText("Vaccines")).perform(scrollTo()).check(matches(not(isChecked())))
    }

    @Test
    fun unsupportedPackage_doesNotShowFooter() {
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)

        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(readPermission, writePermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(readPermission, writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { false }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .check(doesNotExist())
        onView(withText("Read privacy policy")).check(doesNotExist())
    }

    @Test
    fun supportedPackage_whenNoHistoryRead_showsFooterWithGrantTime() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun supportedPackage_whenHistoryRead_showsFooterWithoutGrantTime() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun toggleOnAllowAll_togglesAllPermissionsOn() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(readPermission, writePermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(false) }

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        verify(healthConnectLogger)
            .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_ON)
    }

    @Test
    fun toggleOffAllowAll_togglesAllPermissionsOff() {
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(readPermission, writePermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(readPermission, writePermission))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MediatorLiveData(true) }

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Allow all")).perform(click())

        verify(healthConnectLogger)
            .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_OFF)
    }

    @Test
    fun additionalAccessState_notValid_hidesAdditionalAccess() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
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

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )

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

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )

        onView(withText(R.string.additional_access_label)).check(matches(isDisplayed()))
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

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )
        onView(withText(R.string.additional_access_label)).perform(click())

        verify(navigationUtils)
            .navigate(
                fragment = any(),
                action = eq(R.id.action_settingsMedicalApp_to_additionalAccessFragment),
                bundle = any(),
            )
    }

    @Test
    fun whenMigrationPending_showsMigrationPendingDialog() {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        launchFragment<SettingsMedicalAppFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )

        onView(
                withText(
                    "Health Connect is ready to be integrated with your Android system. If you give $TEST_APP_NAME access now, some features may not work until integration is complete."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Start integration")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Continue")).inRoot(isDialog()).check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_PENDING_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_PENDING_DIALOG_CONTINUE_BUTTON)
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_PENDING_DIALOG_CANCEL_BUTTON)

        onView(withText("Continue")).inRoot(isDialog()).perform(click())
        onView(withText("Continue")).check(doesNotExist())
        verify(healthConnectLogger)
            .logInteraction(MigrationElement.MIGRATION_PENDING_DIALOG_CONTINUE_BUTTON)
    }

    @Test
    fun whenMigrationInProgress_showsMigrationInProgressDialog() {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IN_PROGRESS,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IN_PROGRESS,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )

        onView(
                withText(
                    "Health Connect is being integrated with the Android system.\n\nYou'll get a notification when the process is complete and you can use $TEST_APP_NAME with Health Connect."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Got it")).inRoot(isDialog()).check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_IN_PROGRESS_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_IN_PROGRESS_DIALOG_BUTTON)

        onView(withText("Got it")).inRoot(isDialog()).perform(click())
        verify(healthConnectLogger)
            .logInteraction(MigrationElement.MIGRATION_IN_PROGRESS_DIALOG_BUTTON)

        // Needed to makes sure activity has finished
        scenario.result
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }

    @Test
    fun whenRestoreInProgress_showsRestoreInProgressDialog() {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        val writePermission = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        val readPermission = MedicalPermission(MedicalPermissionType.VACCINES)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        val scenario =
            launchFragment<SettingsMedicalAppFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )

        onView(withText("Health Connect restore in progress"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Health Connect is restoring data and permissions. This may take some time to complete."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Got it")).inRoot(isDialog()).check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DataRestoreElement.RESTORE_IN_PROGRESS_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DataRestoreElement.RESTORE_IN_PROGRESS_DIALOG_BUTTON)

        onView(withText("Got it")).inRoot(isDialog()).perform(click())
        verify(healthConnectLogger)
            .logInteraction(DataRestoreElement.RESTORE_IN_PROGRESS_DIALOG_BUTTON)

        // Needed to makes sure activity has finished
        scenario.result
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }
}
