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
package com.android.healthconnect.controller.tests.permissions.connectedapps.settings

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.ActivityInfo
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissions.READ_DISTANCE
import android.health.connect.HealthPermissions.READ_STEPS
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.DisableExerciseRouteDialogEvent
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState
import com.android.healthconnect.controller.permissions.app.SettingsFitnessAppFragment
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.HYDRATION
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.STEPS
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.READ
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.WRITE
import com.android.healthconnect.controller.permissions.request.PermissionGroupKey
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthToggleExpandablePreference
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.clickOnRecyclerViewItemWithText
import com.android.healthconnect.controller.tests.utils.clickSwitchOnRecyclerViewItemWithText
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.scrollToBottomOfPreferenceScreen
import com.android.healthconnect.controller.tests.utils.scrollToText
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.logging.UIAction
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsFitnessAppFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @BindValue val viewModel: AppPermissionViewModel = mock()
    @BindValue val migrationViewModel: MigrationViewModel = mock()
    @BindValue val additionalAccessViewModel: AdditionalAccessViewModel = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()
        navHostController = TestNavHostController(context)

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
        val accessDate = Instant.parse("2022-10-20T18:40:13.00Z")
        whenever(viewModel.loadAccessDate(Mockito.anyString())).thenReturn(accessDate)
        whenever(viewModel.showDisableExerciseRouteEvent)
            .thenReturn(MediatorLiveData(DisableExerciseRouteDialogEvent()))
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
        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(setOf(PermissionGroupKey(READ, HealthDataCategory.ACTIVITY).toString()))
        }
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                onView(withText("Allow all")).check(matches(isDisplayed()))
                onView(withText("Allowed to read")).check(matches(isDisplayed()))
                onView(withText("Allowed to write")).check(matches(isDisplayed()))
            }
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                verify(healthConnectLogger, atLeast(1)).setPageId(PageName.MANAGE_PERMISSIONS_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
                verify(healthConnectLogger, times(2))
                    .logImpression(PermissionsElement.PERMISSION_SWITCH)
            }
    }

    @Test
    fun doesNotShowWriteHeader_whenNoWritePermissions() {
        val readPermission =
            FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(readPermission)) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(readPermission))
        }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                onView(withText("Allow all")).check(matches(isDisplayed()))
                onView(withText("Allowed to read")).check(matches(isDisplayed()))
                onView(withText("Allowed to write")).check(doesNotExist())
            }
    }

    @Test
    fun doesNotShowReadHeader_whenNoReadPermissions() {
        val writePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(listOf(writePermission)) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                onView(withText("Allow all")).check(matches(isDisplayed()))
                onView(withText("Allowed to read")).check(doesNotExist())
                onView(withText("Allowed to write")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun toggleOnAllowAll_togglesAllPermissionsOn() {
        val readStepsPermission =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.READ)
        val writeSleepPermission =
            FitnessPermission(FitnessPermissionType.SLEEP, PermissionsAccessType.WRITE)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(readStepsPermission, writeSleepPermission))
        }
        whenever(viewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)).then { true }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use {
                onView(withText("Allow all")).perform(click())

                verify(healthConnectLogger)
                    .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_ON)
            }
    }

    @Test
    fun toggleOffAllowAll_togglesAllPermissionsOff() {
        val readStepsPermission =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.READ)
        val writeSleepPermission =
            FitnessPermission(FitnessPermissionType.SLEEP, PermissionsAccessType.WRITE)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(readStepsPermission, writeSleepPermission))
        }
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(emptyList<MedicalPermission>())
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(readStepsPermission, writeSleepPermission))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MediatorLiveData(true) }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                onView(withText("Allow all")).perform(click())

                verify(healthConnectLogger)
                    .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_OFF)
            }
    }

    @Test
    fun unsupportedPackage_doesNotShowFooter() {
        val readStepsPermission =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.READ)
        val writeSleepPermission =
            FitnessPermission(FitnessPermissionType.SLEEP, PermissionsAccessType.WRITE)

        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(readStepsPermission, writeSleepPermission))
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writeSleepPermission, readStepsPermission))
        }
        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { false }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
                onView(
                        withText(
                            "$TEST_APP_NAME can read data added after October 20, 2022" +
                                "\n\n" +
                                "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                        )
                    )
                    .check(doesNotExist())
                onView(withText("Read privacy policy")).check(doesNotExist())
            }
    }

    @Test
    fun supportedPackage_whenNoHistoryRead_showsFooterWithGrantTime() {
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                scrollToBottomOfPreferenceScreen()
                onView(
                        withText(
                            "$TEST_APP_NAME can read data added after October 20, 2022" +
                                "\n\n" +
                                "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                        )
                    )
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withText("Read privacy policy"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun supportedPackage_whenHistoryRead_showsFooterWithoutGrantTime() {
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                scrollToBottomOfPreferenceScreen()
                onView(
                        withText(
                            "$TEST_APP_NAME can read data added after October 20, 2022" +
                                "\n\n" +
                                "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                        )
                    )
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withText("Read privacy policy"))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun additionalAccessState_notValid_hidesAdditionalAccess() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { onView(withText(R.string.additional_access_label)).check(doesNotExist()) }
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use {
                onView(withText(R.string.additional_access_label)).check(matches(isDisplayed()))
            }
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use {
                onView(withText(R.string.additional_access_label)).check(matches(isDisplayed()))
            }
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            ) {
                navHostController.setGraph(R.navigation.settings_nav_graph)
                navHostController.setCurrentDestination(R.id.settingsFitnessAppFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText(R.string.additional_access_label)).perform(click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.additionalAccessFragment)
            }
    }

    @Test
    fun whenMigrationPending_showsMigrationPendingDialog() {
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use {
                onView(
                        withText(
                            "Health Connect is ready to be integrated with your Android system. If you give $TEST_APP_NAME access now, some features may not work until integration is complete."
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Start integration"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
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
    }

    @Test
    fun whenMigrationInProgress_showsMigrationInProgressDialog() {
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
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
    }

    @Test
    fun whenRestoreInProgress_showsRestoreInProgressDialog() {
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

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply { putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME) }
            )
            .use { scenario ->
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

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun displaysGroupedPermissions_whenFlagEnabled() {
        val writePermission = FitnessPermission(HYDRATION, WRITE)
        val readPermission = FitnessPermission(STEPS, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use { scenario ->
                // Activity category was marked expanded in the test Setup
                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText("Steps"))
                        )
                    )
                onView(withText("Steps")).check(matches(isDisplayed()))

                lateinit var expandablePreference: HealthToggleExpandablePreference
                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as SettingsFitnessAppFragment
                    expandablePreference =
                        fragment.preferenceScreen.findPreference(
                            PermissionGroupKey(READ, HealthDataCategory.ACTIVITY).toString()
                        )!!
                }
                assertThat(expandablePreference.mIsExpanded).isTrue()

                // Now expand Nutrition category (write permissions)
                clickOnRecyclerViewItemWithText("Nutrition")
                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText("Hydration"))
                        )
                    )
                onView(withText("Hydration")).check(matches(isDisplayed()))
            }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun togglePermissionInCategory_updatesViewModel_whenFlagEnabled() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val writePermission = FitnessPermission(HYDRATION, WRITE)
        val readPermission = FitnessPermission(STEPS, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                clickOnRecyclerViewItemWithText("Steps")

                verify(viewModel)
                    .updatePermission(TEST_APP_PACKAGE_NAME, stepsPermission, grant = true)
            }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun toggleCategorySwitch_updatesViewModel_whenFlagEnabled() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val writePermission = FitnessPermission(HYDRATION, WRITE)
        val readPermission = FitnessPermission(STEPS, READ)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                clickSwitchOnRecyclerViewItemWithText("Activity")

                verify(viewModel)
                    .updatePermissions(TEST_APP_PACKAGE_NAME, listOf(stepsPermission), grant = true)
            }
    }

    @Test
    fun permissionGrouping_correctlyDisplaysGrantedPermissionCount_partialPermissionsGranted() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(stepsPermission, distancePermission))
        }

        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(setOf(PermissionGroupKey(READ, HealthDataCategory.ACTIVITY).toString()))
        }

        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(stepsPermission))
        }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                scrollToText("1 of 2 selected")
                onView(withText("1 of 2 selected")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun permissionGrouping_correctlyDisplaysGrantedPermissionCount_allPermissionsGranted() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(stepsPermission, distancePermission))
        }

        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(setOf(PermissionGroupKey(READ, HealthDataCategory.ACTIVITY).toString()))
        }

        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(stepsPermission, distancePermission))
        }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                scrollToText("2 of 2 selected")
                onView(withText("2 of 2 selected")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun permissionGrouping_correctlyDisplaysGrantedPermissionCount_zeroPermissionsGranted() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        whenever(viewModel.fitnessPermissions).then {
            MutableLiveData(listOf(stepsPermission, distancePermission))
        }

        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(setOf(PermissionGroupKey(READ, HealthDataCategory.ACTIVITY).toString()))
        }

        whenever(viewModel.grantedFitnessPermissions).then { MutableLiveData<Set<String>>() }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                scrollToText("0 of 2 selected")
                onView(withText("0 of 2 selected")).check(matches(isDisplayed()))
            }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun toggleIndividualPermission_updatesParentSwitchState() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        val activityPermissions = listOf(stepsPermission, distancePermission)
        whenever(viewModel.fitnessPermissions).then { MutableLiveData(activityPermissions) }

        launchFragment<SettingsFitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use { scenario ->
                lateinit var expandablePreference: HealthToggleExpandablePreference
                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as SettingsFitnessAppFragment
                    expandablePreference =
                        fragment.preferenceScreen.findPreference(
                            PermissionGroupKey(READ, HealthDataCategory.ACTIVITY).toString()
                        )!!
                }
                assertThat(expandablePreference.isChecked).isFalse()

                // 1. Click "Steps" to turn it on
                clickOnRecyclerViewItemWithText("Steps")
                assertThat(expandablePreference.isChecked).isFalse()

                // 2. Click "Distance" to turn it on
                clickOnRecyclerViewItemWithText("Distance")
                assertThat(expandablePreference.mIsExpanded).isTrue()

                // 3. Click "Steps" to turn it off again
                clickOnRecyclerViewItemWithText("Steps")
                assertThat(expandablePreference.isChecked).isFalse()
            }
    }
}
