/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.tests.navigation

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.makeMainActivity
import android.health.connect.HealthConnectManager.ACTION_HEALTH_HOME_SETTINGS
import android.health.connect.HealthConnectManager.ACTION_MANAGE_HEALTH_DATA
import android.health.connect.HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS
import android.health.connect.HealthDataCategory
import android.os.Build
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.alldata.AllDataViewModel
import com.android.healthconnect.controller.data.appdata.PermissionTypesPerCategory
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.home.HomeViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.WithData
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.navigation.TrampolineActivity
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.showOnboarding
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(DeviceInfoUtilsModule::class)
class TrampolineActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue val migrationViewModel: MigrationViewModel = mock(MigrationViewModel::class.java)
    @BindValue
    val exportStatusViewModel: ExportStatusViewModel = mock(ExportStatusViewModel::class.java)

    @BindValue
    val appPermissionViewModel: AppPermissionViewModel = mock(AppPermissionViewModel::class.java)
    @BindValue
    val connectedAppsViewModel: ConnectedAppsViewModel = mock(ConnectedAppsViewModel::class.java)
    @BindValue
    val additionalAccessViewModel: AdditionalAccessViewModel =
        mock(AdditionalAccessViewModel::class.java)
    @BindValue val allDataViewModel: AllDataViewModel = mock(AllDataViewModel::class.java)
    @BindValue val homeViewModel: HomeViewModel = mock(HomeViewModel::class.java)
    @BindValue
    val recentAccessViewModel: RecentAccessViewModel = mock(RecentAccessViewModel::class.java)
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun setup() {
        hiltRule.inject()

        // Required for aconfig flag reading for tests run on pre V devices
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .adoptShellPermissionIdentity(Manifest.permission.READ_DEVICE_CONFIG)

        showOnboarding(context, show = false)
        (deviceInfoUtils as FakeDeviceInfoUtils).setHealthConnectAvailable(true)

        // Disable migration to show MainActivity and DataManagementActivity
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        NOW,
                        ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        1,
                    )
                )
            )
        }
        val writePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        val readPermission =
            FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(appPermissionViewModel.appInfo).then { MutableLiveData(TEST_APP) }
        whenever(
                appPermissionViewModel.shouldNavigateToAppPermissionsFragment(TEST_APP_PACKAGE_NAME)
            )
            .then { true }
        whenever(appPermissionViewModel.fitnessPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(appPermissionViewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(writePermission))
        }
        whenever(appPermissionViewModel.revokeAllHealthPermissionsState).then {
            MutableLiveData(AppPermissionViewModel.RevokeAllState.NotStarted)
        }
        whenever(appPermissionViewModel.allFitnessPermissionsGranted).then {
            MediatorLiveData(false)
        }
        whenever(appPermissionViewModel.atLeastOneFitnessPermissionGranted).then {
            MediatorLiveData(true)
        }
        whenever(appPermissionViewModel.atLeastOneHealthPermissionGranted).then {
            MediatorLiveData(true)
        }
        val accessDate = Instant.parse("2022-10-20T18:40:13.00Z")
        whenever(appPermissionViewModel.loadAccessDate(anyString())).thenReturn(accessDate)
        whenever(appPermissionViewModel.lastReadPermissionDisconnected).then {
            MutableLiveData(false)
        }
        whenever(connectedAppsViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(
                        TEST_APP,
                        ConnectedAppStatus.ALLOWED,
                        AppPermissionsType.FITNESS_PERMISSIONS_ONLY,
                        accessDate,
                    )
                )
            )
        }
        whenever(connectedAppsViewModel.disconnectAllState).then {
            MutableLiveData(ConnectedAppsViewModel.DisconnectAllState.NotStarted)
        }
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }
        whenever(allDataViewModel.allData).then {
            MutableLiveData<AllDataViewModel.AllDataState>(
                AllDataViewModel.AllDataState.WithData(
                    listOf(
                        PermissionTypesPerCategory(
                            HealthDataCategory.ACTIVITY,
                            listOf(FitnessPermissionType.STEPS),
                        )
                    )
                )
            )
        }
        whenever(allDataViewModel.setOfPermissionTypesToBeDeleted).then {
            MutableLiveData<Set<FitnessPermissionType>>(emptySet())
        }
        whenever(allDataViewModel.deletionScreenState).then {
            MutableLiveData(DeletionDataViewModel.DeletionScreenState.VIEW)
        }
        whenever(allDataViewModel.getDeletionScreenStateValue())
            .thenReturn(DeletionDataViewModel.DeletionScreenState.VIEW)
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)))
        }
        whenever(homeViewModel.hasAnyMedicalData).then { MutableLiveData(false) }
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.NoBanner)
        }
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData(RecentAccessViewModel.RecentAccessState.WithData(listOf()))
        }
    }

    @Test
    fun startingActivity_healthConnectNotAvailable_finishesActivity() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setHealthConnectAvailable(false)

        launchActivityForResult<TrampolineActivity>(createStartIntent()).use { scenario ->
            onIdle()
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun startingActivity_noAction_finishesActivity() {
        launchActivityForResult<TrampolineActivity>(createStartIntent("no_action")).use { scenario
            ->
            onIdle()
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun homeSettingsAction_onboardingNotDone_redirectsToOnboarding() {
        showOnboarding(context, true)

        launchActivityForResult<TrampolineActivity>(createStartIntent()).use {
            onIdle()
            onView(withId(R.id.onboarding)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun homeSettingsIntent_launchesMainActivity() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setHealthConnectAvailable(true)

        launchActivityForResult<TrampolineActivity>(createStartIntent(ACTION_HEALTH_HOME_SETTINGS))
            .use {
                onIdle()
                // TODO (b/390212615) update once we can use settings flag
                if (SettingsThemeHelper.isExpressiveTheme(context)) {
                    onView(withText("No recent access"))
                        .perform(scrollTo())
                        .check(matches(isDisplayed()))
                } else {
                    onView(withText("No apps recently accessed Health\u00A0Connect"))
                        .perform(scrollTo())
                        .check(matches(isDisplayed()))
                }
                onView(withText("Permissions and data")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun manageHealthDataIntent_launchesDataManagementActivity_newIA() {
        // setup data management screen.
        launchActivityForResult<TrampolineActivity>(createStartIntent(ACTION_MANAGE_HEALTH_DATA))
            .use {
                onIdle()
                onView(withText("Activity")).check(matches(isDisplayed()))
                onView(withText("Steps")).check(matches(isDisplayed()))
            }
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun manageHealthPermissions_launchesSettingsActivity_healthConnectBrand() {
        launchActivityForResult<TrampolineActivity>(
                createStartIntent(ACTION_MANAGE_HEALTH_PERMISSIONS)
            )
            .use {
                onView(
                        withText(
                            "Apps with this permission can read and write your" +
                                " health and fitness data."
                        )
                    )
                    .check(matches(isDisplayed()))
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun manageHealthPermissions_launchesSettingsActivity_healthFitnessBrand() {
        launchActivityForResult<TrampolineActivity>(
                createStartIntent(ACTION_MANAGE_HEALTH_PERMISSIONS)
            )
            .use {
                onView(
                        withText(
                            "Apps with this permission can read and write your health, fitness and wellness data. This includes data tracked from your devices and data stored in Health Connect"
                        )
                    )
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun manageHealthPermissions_withPackageName_launchesSettingsActivity() {
        val intent = createStartIntent(ACTION_MANAGE_HEALTH_PERMISSIONS)
        intent.putExtra(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
        whenever(appPermissionViewModel.showDisableExerciseRouteEvent)
            .thenReturn(MediatorLiveData(AppPermissionViewModel.DisableExerciseRouteDialogEvent()))

        launchActivityForResult<TrampolineActivity>(intent).use {
            onIdle()
            onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        }
    }

    private fun createStartIntent(action: String = ACTION_HEALTH_HOME_SETTINGS): Intent {
        return makeMainActivity(ComponentName(context, TrampolineActivity::class.java))
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
            .setAction(action)
    }
}
