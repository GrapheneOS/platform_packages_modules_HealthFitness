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
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.SystemUtil.eventually
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
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState.NotStarted
import com.android.healthconnect.controller.permissions.app.SettingsCombinedPermissionsFragment
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.NavigationUtils
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
class SettingsCombinedPermissionsFragmentTest {

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
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(emptyList<FitnessPermission>())
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(emptySet<FitnessPermission>())
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(false) }
        whenever(viewModel.atLeastOneFitnessPermissionGranted).then { MediatorLiveData(true) }
        whenever(viewModel.atLeastOneHealthPermissionGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeAllHealthPermissionsState).then { MutableLiveData(NotStarted) }
        val accessDate = Instant.parse("2022-10-20T18:40:13.00Z")
        whenever(viewModel.loadAccessDate(Mockito.anyString())).thenReturn(accessDate)
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

        // disable animations
        toggleAnimation(false)
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
        // enable animations
        toggleAnimation(true)
    }

    @Test
    fun fragment_starts() {
        val writePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        val readPermission =
            FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

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

        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )

        onView(withText("Permissions")).check(matches(isDisplayed()))
        onView(withText("Fitness and wellness")).check(matches(isDisplayed()))
        onView(withText("Exercise, sleep, nutrition and others")).check(matches(isDisplayed()))
        onView(withText("Health records")).check(matches(isDisplayed()))
        onView(withText("Lab results, medications, vaccines and others"))
            .check(matches(isDisplayed()))
        onView(withText("Additional access")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Past data, background data"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Manage app")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "You can learn how Health Connect test app handles your data in the developer's privacy policy"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun fragmentStarts_logPageImpression() {
        val writePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        val readPermission =
            FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

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

        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )

        verify(healthConnectLogger, atLeast(1))
            .setPageId(PageName.SETTINGS_MANAGE_COMBINED_APP_PERMISSIONS_PAGE)
        verify(healthConnectLogger).logPageImpression()
    }

    @Test
    fun additionalAccessState_notValid_hidesAdditionalAccess() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }

        launchFragment<SettingsCombinedPermissionsFragment>(
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

        launchFragment<SettingsCombinedPermissionsFragment>(
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

        launchFragment<SettingsCombinedPermissionsFragment>(
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

        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )
        onView(withText(R.string.additional_access_label)).perform(click())

        verify(navigationUtils)
            .navigate(
                fragment = any(),
                action = eq(R.id.action_settingsCombinedPermissions_to_additionalAccessFragment),
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
        val writePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        val readPermission =
            FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        launchFragment<SettingsCombinedPermissionsFragment>(
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
        val writePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        val readPermission =
            FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        val scenario =
            launchFragment<SettingsCombinedPermissionsFragment>(
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
        eventually { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
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
        val writePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        val readPermission =
            FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }

        val scenario =
            launchFragment<SettingsCombinedPermissionsFragment>(
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
        eventually { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
    }

    @Test
    fun noHealthPermissionsGranted_removeAccessButtonDisabled() {
        whenever(viewModel.atLeastOneHealthPermissionGranted).then { MediatorLiveData(false) }

        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isNotEnabled()))
    }

    @Test
    fun removeAccessButton_withAdditionalPermissions_showsConfirmationDialog() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(
                AdditionalAccessViewModel.State(
                    backgroundReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                    historyReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                )
            )
        }
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect, including background and past data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun removeAccessButton_withBackgroundPermission_showsConfirmationDialog() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(
                AdditionalAccessViewModel.State(
                    backgroundReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                    historyReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = false,
                            isEnabled = false,
                            isGranted = false,
                        ),
                )
            )
        }
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect, including background data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun removeAccessButton_withHistoricReadPermission_showsConfirmationDialog() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(
                AdditionalAccessViewModel.State(
                    backgroundReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = false,
                            isEnabled = false,
                            isGranted = false,
                        ),
                    historyReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                )
            )
        }
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect, including past data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun removeAccessButton_noAdditionalPermissions_showsConfirmationDialog() {
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<SettingsCombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun removeAccessButton_confirmationDialogNoCheckbox_remainsAfterRotation() {
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        val scenario =
            launchFragment<SettingsCombinedPermissionsFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(not(isDisplayed())))

        scenario.recreate()
        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(not(isDisplayed())))
    }
}
