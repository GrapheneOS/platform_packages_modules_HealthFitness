/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.home

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.ACTION_SHOW_ONBOARDING
import android.health.connect.HealthDataCategory
import android.os.Build
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.ACTION_SECURITY_SETTINGS
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.devices.NativeStepsNotificationViewModel
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.home.HomeFragment
import com.android.healthconnect.controller.home.HomeViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.WithData
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessEntry
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel.RecentAccessState
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL
import com.android.healthconnect.controller.shared.Constants.NATIVE_STEPS_BANNER_SEEN
import com.android.healthconnect.controller.shared.Constants.ONBOARDING_ONE_APP_BANNER_SEEN
import com.android.healthconnect.controller.shared.Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.checkTextIsDisplayed
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.scrollToText
import com.android.healthconnect.controller.tests.utils.scrollToTextAndClick
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.setPreferenceSeen
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.NavigationUtils
import com.android.healthconnect.controller.utils.SettingsTransitionHelper.createMainlineServiceUpdateSettingsIntent
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HomePageElement
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RecentAccessElement
import com.android.healthfitness.flags.Flags
import com.android.settingslib.widget.theme.flags.Flags as SettingsThemeFlags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(DeviceInfoUtilsModule::class)
@RunWith(AndroidJUnit4::class)
class HomeFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    private lateinit var context: Context

    @BindValue val homeViewModel: HomeViewModel = mock()

    @BindValue val recentAccessViewModel: RecentAccessViewModel = mock()

    @BindValue val healthPermissionReader: HealthPermissionReader = mock()

    @BindValue val migrationViewModel: MigrationViewModel = mock()

    @BindValue val exportStatusViewModel: ExportStatusViewModel = mock()
    @BindValue val onboardingViewModel: OnboardingViewModel = mock()
    @BindValue val nativeStepsNotificationViewModel: NativeStepsNotificationViewModel = mock()

    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()

    @BindValue val timeSource = TestTimeSource
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var navHostController: TestNavHostController
    @BindValue val navigationUtils: NavigationUtils = mock()

    companion object {
        private const val TEST_EXPORT_FREQUENCY_IN_DAYS = 1
    }

    @Before
    fun setup() {
        hiltRule.inject()
        // Required for aconfig flag reading for tests run on pre V devices
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .adoptShellPermissionIdentity(Manifest.permission.READ_DEVICE_CONFIG)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
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
                        null,
                        ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        /** periodInDays= */
                        0,
                    )
                )
            )
        }
        whenever(homeViewModel.hasAnyMedicalData).then { MutableLiveData(false) }
        navHostController = TestNavHostController(context)
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)))
        }
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.NoBanner)
        }
        whenever(onboardingViewModel.onboardingBannerState).then {
            MediatorLiveData(OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner)
        }
        whenever(nativeStepsNotificationViewModel.wasSeen).then { MutableLiveData<Boolean>(false) }
        (deviceInfoUtils as FakeDeviceInfoUtils).setIntentHandlerAvailability(true)

        Intents.init()

        setBannersSeen(true)
    }

    @After
    fun teardown() {
        timeSource.reset()
        Intents.release()
        reset(healthConnectLogger)
        setBannersSeen(false)
    }

    // region Navigation tests
    @Test
    fun appPermissions_navigatesToConnectedApps() {
        setupFragmentForNavigation().use {
            checkTextIsDisplayed("App permissions")
            onView(withText("App permissions")).perform(click())
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.connectedAppsFragment)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun devices_navigatesToDevices() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Devices")
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.connectedDevicesFragment)
        }
    }

    @Test
    fun dataAndAccess_navigatesToDataAndAccess() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Data and access")
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.data_activity)
        }
    }

    @Test
    fun browseMedicalData_navigatesToBrowseMedicalData() {
        whenever(homeViewModel.hasAnyMedicalData).then { MutableLiveData(true) }
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Browse medical records")

            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.medicalAllDataFragment)
        }
    }

    @Test
    @DisableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun legacySeeAllRecentAccess_navigatesToRecentAccess() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("See all recent access")
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.recentAccessFragment)
        }
    }

    @Test
    fun recentAccessApp_navigatesToFitnessAppFragment() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                scrollToTextAndClick(TEST_APP_NAME)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.fitnessAppFragment)
            }
    }

    @Test
    fun recentAccessApp_navigatesToMedicalAppFragment() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.MEDICAL_PERMISSIONS_ONLY,
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                scrollToTextAndClick(TEST_APP_NAME)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.medicalAppFragment)
            }
    }

    @Test
    fun recentAccessApp_navigatesToCombinedPermissionsFragment() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
                shouldLaunchAppOnboardingIfAvailable = false,
            )
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                scrollToTextAndClick(TEST_APP_NAME)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.combinedPermissionsFragment)
            }
    }

    @Test
    fun manageData_navigatesToManageData() {
        setupFragmentForNavigation().use {
            scrollToTextAndClick("Manage data")
            assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.manageDataFragment)
        }
    }

    // endregion

    // region Display tests
    @Test
    @DisableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun legacyWhenRecentAccessApps_in12HourFormat_showsCorrectTime() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        timeSource.setIs24Hour(false)

        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("Recent access")
            checkTextIsDisplayed(TEST_APP_NAME)
            checkTextIsDisplayed("6:40 PM")
            checkTextIsDisplayed("See all recent access")
        }
    }

    @Test
    @DisableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun legacyWhenRecentAccessAppsError_showsError() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.Error)
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("Recent access")
            checkTextIsDisplayed("Could not load recent access")
            onView(withText("See all recent access")).check(doesNotExist())
        }
    }

    @Test
    @DisableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun withNoRecentAccessApps() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("Recent access")
            checkTextIsDisplayed("No apps recently accessed Health\u00A0Connect")

            checkTextIsDisplayed("App permissions")
            checkTextIsDisplayed("2 apps have access")
            checkTextIsDisplayed("Data and access")
            checkTextIsDisplayed("Manage data")

            onView(withText("No recent access")).check(doesNotExist())
            onView(
                    withText(
                        "Apps which have recently accessed your data will automatically show here"
                    )
                )
                .check(doesNotExist())
        }
    }

    @Test
    fun whenOneAppConnected_showsOneAppHasPermissions() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf(ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)))
        }

        launchFragment<HomeFragment>(Bundle()).use {
            onView(
                    withText(
                        "Manage the health and fitness data on your device, and control which apps can access it"
                    )
                )
                .check(doesNotExist())
            checkTextIsDisplayed("App permissions")
            checkTextIsDisplayed("1 app has access")
            checkTextIsDisplayed("Data and access")
            checkTextIsDisplayed("See data and which apps can access it")
            checkTextIsDisplayed("Manage data")
        }
    }

    // endregion

    // region Migration tests
    @Test
    fun whenMigrationStatePending_showsMigrationBanner() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.ALLOWED_PAUSED,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Resume integration")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Tap to continue integrating Health Connect with the Android system."
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(withText("Continue")).check(matches(isDisplayed()))

                verify(healthConnectLogger).logImpression(MigrationElement.MIGRATION_RESUME_BANNER)
                verify(healthConnectLogger)
                    .logImpression(MigrationElement.MIGRATION_RESUME_BANNER_BUTTON)

                onView(withText("Continue")).perform(click())
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.migrationActivity)
                verify(healthConnectLogger, atLeast(1))
                    .logInteraction(MigrationElement.MIGRATION_RESUME_BANNER_BUTTON)
            }
    }

    @Test
    fun whenDataRestoreStatePending_andErrorVersionDiff_showsRestoreBanner() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.PENDING,
                        dataRestoreError = DataRestoreUiError.ERROR_VERSION_DIFF,
                    )
                )
            )
        }
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Update needed")).check(matches(isDisplayed()))
                onView(withText("Before continuing restoring your data, update your phone system."))
                    .check(matches(isDisplayed()))
                onView(withText("Update now")).check(matches(isDisplayed()))
                verify(healthConnectLogger).logImpression(DataRestoreElement.RESTORE_PENDING_BANNER)
                verify(healthConnectLogger)
                    .logImpression(DataRestoreElement.RESTORE_PENDING_BANNER_UPDATE_BUTTON)

                onView(withText("Update now")).perform(click())

                intended(hasAction(context.createMainlineServiceUpdateSettingsIntent().action))
                verify(healthConnectLogger)
                    .logInteraction(DataRestoreElement.RESTORE_PENDING_BANNER_UPDATE_BUTTON)
            }
    }

    @Test
    fun whenDataRestoreStatePending_noError_doesNotShowRestoreBanner() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.PENDING,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Update needed")).check(doesNotExist())
                onView(withText("Before continuing restoring your data, update your phone system."))
                    .check(doesNotExist())
                onView(withText("Update now")).check(doesNotExist())
            }
    }

    @Test
    fun whenMigrationStateComplete_showsDialog() {
        setPreferenceSeen(context, Constants.WHATS_NEW_DIALOG_SEEN, false)

        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.COMPLETE,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()).use { scenario ->
            onView(withText("What's new"))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "You can now access Health Connect directly from your settings. Uninstall the Health Connect app any time to free up storage space."
                    )
                )
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_DONE_DIALOG_CONTAINER)
            verify(healthConnectLogger).logImpression(MigrationElement.MIGRATION_DONE_DIALOG_BUTTON)

            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).perform(click())
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_DONE_DIALOG_BUTTON)

            scenario.onActivity { activity ->
                val preferences =
                    activity.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
                assertThat(preferences.getBoolean("Whats New Seen", false)).isTrue()
            }
        }
    }

    @Test
    fun whenMigrationStateNotComplete_showsDialog() {
        setPreferenceSeen(context, Constants.MIGRATION_NOT_COMPLETE_DIALOG_SEEN, false)
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.ALLOWED_ERROR,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()).use { scenario ->
            onView(withText("Health Connect integration didn't complete"))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(withText("You'll get a notification when it becomes available again."))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_CONTAINER)
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_BUTTON)

            onView(withText("Got it")).inRoot(RootMatchers.isDialog()).perform(click())
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_BUTTON)

            scenario.onActivity { activity ->
                val preferences =
                    activity.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
                assertThat(preferences.getBoolean("Migration Not Complete Seen", false)).isTrue()
            }
        }
    }

    // endregion

    // region Medical data tests
    @Test
    fun browseMedicalData_errorFetchingMedicalDataOrEmptyMedicalData_notDisplayed() {
        whenever(homeViewModel.hasAnyMedicalData).then { MutableLiveData(false) }

        setupFragmentForNavigation().use {
            onView(withText("Browse medical records")).check(doesNotExist())
            onView(withText("View your medical records and which apps can access them"))
                .check(doesNotExist())
            verify(healthConnectLogger, times(0))
                .logImpression(HomePageElement.BROWSE_HEALTH_RECORDS_BUTTON)
        }
    }

    @Test
    fun browseMedicalData_medicalDataExists_isDisplayed() {
        whenever(homeViewModel.hasAnyMedicalData).then { MutableLiveData(true) }

        setupFragmentForNavigation().use {
            checkTextIsDisplayed("Browse medical records")
            checkTextIsDisplayed("View your medical records and which apps can access them")
            verify(healthConnectLogger).logImpression(HomePageElement.BROWSE_HEALTH_RECORDS_BUTTON)
        }
    }

    // endregion

    // region Import/Export tests
    @Test
    fun lastExportWithoutError_exportFileAccessErrorBannerIsNotShown() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        NOW,
                        ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        TEST_EXPORT_FREQUENCY_IN_DAYS,
                    )
                )
            )
        }

        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use { onView(withText("Couldn't export data")).check(doesNotExist()) }
    }

    @Test
    fun lastFailedExportTimeIsNull_exportFileAccessErrorBannerIsNotShown() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                        periodInDays = TEST_EXPORT_FREQUENCY_IN_DAYS,
                        lastFailedExportTime = null,
                    )
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use { onView(withText("Couldn't export data")).check(doesNotExist()) }
    }

    @Test
    fun lastExportWithUnknownErrorAndDate_showsExportErrorBanner() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_UNKNOWN,
                        periodInDays = TEST_EXPORT_FREQUENCY_IN_DAYS,
                        lastFailedExportTime = NOW,
                    )
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Couldn't export data")).check(matches(isDisplayed()))
                onView(withText("Set up")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "There was a problem with the export for October 20, 2022. Please set up a new scheduled export and try again."
                        )
                    )
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun showsManageDataSummary() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_FREQUENCY_IN_DAYS,
                        lastFailedExportTime = NOW,
                    )
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use { checkTextIsDisplayed("Auto-delete, data sources, backup and restore") }
    }

    @Test
    fun lastExportWithLostFileAccessErrorAndDate_showsExportErrorBanner() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                        periodInDays = TEST_EXPORT_FREQUENCY_IN_DAYS,
                        lastFailedExportTime = NOW,
                    )
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Couldn't export data")).check(matches(isDisplayed()))
                onView(withText("Set up")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "There was a problem with the export for October 20, 2022. Please set up a new scheduled export and try again."
                        )
                    )
                    .check(matches(isDisplayed()))
                verify(healthConnectLogger).logImpression(HomePageElement.EXPORT_ERROR_BANNER)
                verify(healthConnectLogger)
                    .logImpression(HomePageElement.EXPORT_ERROR_BANNER_BUTTON)
            }
    }

    @Test
    fun lastExportWithValidErrorTypeAndDate_showsExportErrorBanner_clicksSetupAndNavigatesToExportFlow() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                        periodInDays = TEST_EXPORT_FREQUENCY_IN_DAYS,
                        lastFailedExportTime = NOW,
                    )
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Set up")).perform(click())
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.exportSetupActivity)
            }
    }

    // endregion

    // region Logging

    @Test
    fun homeFragmentLogging_impressionsLogged() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        timeSource.setIs24Hour(true)

        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.HOME_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger).logImpression(HomePageElement.APP_PERMISSIONS_BUTTON)
            verify(healthConnectLogger).logImpression(HomePageElement.DATA_AND_ACCESS_BUTTON)
            verify(healthConnectLogger).logImpression(HomePageElement.MANAGE_DATA_BUTTON)
            verify(healthConnectLogger).logImpression(HomePageElement.SEE_ALL_RECENT_ACCESS_BUTTON)
            verify(healthConnectLogger)
                .logImpression(RecentAccessElement.RECENT_ACCESS_ENTRY_BUTTON)
        }
    }

    // endregion

    // region lock screen banner
    @Test
    fun lockScreenBanner_shouldNotShowBanner_bannerNotShown() {
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.NoBanner)
        }

        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText("Set a screen lock")).check(doesNotExist())
        }
    }

    @Test
    fun lockScreenBanner_securityIntentNotHandled_bannerNotShown() {
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        (deviceInfoUtils as FakeDeviceInfoUtils).setIntentHandlerAvailability(false)
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.ShowBanner())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText("Set a screen lock")).check(doesNotExist())
        }
    }

    @Test
    fun lockScreenBanner_bannerShown() {
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.ShowBanner())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText("Set a screen lock")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(
                    withText(
                        "For added security for your health data, set a PIN, pattern, or password for this device"
                    )
                )
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withText("Set screen lock")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Not now")).perform(scrollTo()).check(matches(isDisplayed()))
            verify(healthConnectLogger).logImpression(HomePageElement.LOCK_SCREEN_BANNER)
            verify(healthConnectLogger).logImpression(HomePageElement.LOCK_SCREEN_BANNER_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.LOCK_SCREEN_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun lockScreenBanner_whenMedicalAndFitnessData_bannerShown() {
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = true,
                    hasAnyMedicalData = true,
                )
            )
        }

        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText("Set a screen lock")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun lockScreenBanner_startsNewPasswordIntent() {
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.ShowBanner())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText("Set screen lock")).perform(scrollTo()).perform(click())
            verify(healthConnectLogger).logInteraction(HomePageElement.LOCK_SCREEN_BANNER_BUTTON)

            intended(hasAction(ACTION_SECURITY_SETTINGS))
        }
    }

    @Test
    fun lockScreenBanner_dismissBanner_bannerDisappears() {
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.ShowBanner())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText("Set screen lock")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(
                    withId(
                        com.android.settingslib.widget.preference.banner.R.id.banner_negative_btn
                    )
                )
                .perform(scrollTo())
                .perform(click())
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.LOCK_SCREEN_BANNER_DISMISS_BUTTON)

            onView(withText("Set screen lock")).check(doesNotExist())
        }
    }

    // endregion

    // region BannerGroup
    @Test
    @Ignore("b/447652645")
    fun multipleBanners_canExpand_andCollapseGroup() {
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)

        // Export and Lock Screen banners
        whenever(homeViewModel.showLockScreenBanner).then {
            MediatorLiveData(HomeViewModel.LockScreenBannerState.ShowBanner())
        }
        whenever(onboardingViewModel.onboardingBannerState).then {
            MutableLiveData(OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner)
        }
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                        periodInDays = TEST_EXPORT_FREQUENCY_IN_DAYS,
                        lastFailedExportTime = NOW,
                    )
                )
            )
        }

        launchFragment<HomeFragment>(Bundle()).use {

            // Export banner shown first
            onView(withText("Couldn't export data")).check(matches(isDisplayed()))
            onView(withText("Set up")).check(matches(isDisplayed()))
            onView(
                    withText(
                        "There was a problem with the export for October 20, 2022. Please set up a new scheduled export and try again."
                    )
                )
                .check(matches(isDisplayed()))
            verify(healthConnectLogger).logImpression(HomePageElement.EXPORT_ERROR_BANNER)
            verify(healthConnectLogger).logImpression(HomePageElement.EXPORT_ERROR_BANNER_BUTTON)

            onView(withText("Set a screen lock")).check(doesNotExist())
            onView(
                    withText(
                        "For added security for your health data, set a PIN, pattern, or password for this device"
                    )
                )
                .check(doesNotExist())
            onView(withText("Set screen lock")).check(doesNotExist())

            checkTextIsDisplayed("More items")

            // Expand group
            onView(
                    withId(
                        com.android.settingslib.widget.preference.button.R.id
                            .settingslib_number_button
                    )
                )
                .perform(click())
            onIdle()

            // Check appearance of second banner
            onView(withText("Set a screen lock")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(
                    withText(
                        "For added security for your health data, set a PIN, pattern, or password for this device"
                    )
                )
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withText("Set screen lock")).perform(scrollTo()).check(matches(isDisplayed()))

            verify(healthConnectLogger).logImpression(HomePageElement.LOCK_SCREEN_BANNER)
            verify(healthConnectLogger).logImpression(HomePageElement.LOCK_SCREEN_BANNER_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.LOCK_SCREEN_BANNER_DISMISS_BUTTON)

            onView(withText("Fewer items")).perform(scrollTo()).check(matches(isDisplayed()))

            // Collapse group
            onView(withText("Fewer items")).perform(click())

            // Check export banner still shown
            onView(withText("Couldn't export data")).check(matches(isDisplayed()))
            onView(withText("Set up")).check(matches(isDisplayed()))
            onView(
                    withText(
                        "There was a problem with the export for October 20, 2022. Please set up a new scheduled export and try again."
                    )
                )
                .check(matches(isDisplayed()))

            // But not the lock screen banner
            onView(withText("Set a screen lock")).check(doesNotExist())
            onView(
                    withText(
                        "For added security for your health data, set a PIN, pattern, or password for this device"
                    )
                )
                .check(doesNotExist())
            onView(withText("Set screen lock")).check(doesNotExist())

            onView(withText("More items")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    // endregion

    // region Expressive display tests
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun expressiveViewAllRecentAccess_navigatesToRecentAccess() {
        setupFragmentForNavigation().use {
            scrollToText("View all")
            onView(withText("View all")).perform(click())
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.recentAccessFragment)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun expressiveWhenRecentAccessApps_in12HourFormat_showsCorrectTime() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        timeSource.setIs24Hour(false)

        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("Recent access")
            checkTextIsDisplayed(TEST_APP_NAME)
            checkTextIsDisplayed("6:40 PM")
            checkTextIsDisplayed("View all")
            onView(withText("See all recent access")).check(doesNotExist())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun expressive_withNoRecentAccessApps() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(emptyList()))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("No recent access")
            checkTextIsDisplayed(
                "Apps which have recently accessed your data will automatically show here"
            )
            checkTextIsDisplayed("App permissions")
            checkTextIsDisplayed("2 apps have access")
            checkTextIsDisplayed("Data and access")
            checkTextIsDisplayed("Manage data")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags(SettingsThemeFlags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun expressive_withErrorInRecentAccessApps() {
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.Error)
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED),
                )
            )
        }
        launchFragment<HomeFragment>(Bundle()).use {
            checkTextIsDisplayed("Recent access")
            checkTextIsDisplayed("Could not load recent access")

            onView(withText("No apps recently accessed Health\u00A0Connect")).check(doesNotExist())
        }
    }

    // endregion

    // region Onboarding
    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun onboardingActivityAvailable_navigatesToOnboardingActivityInsteadOfPermissionManagement() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                shouldLaunchAppOnboardingIfAvailable = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
            )
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        val testIntent = Intent(ACTION_SHOW_ONBOARDING)
        testIntent.setPackage(TEST_APP.packageName)

        // Assume that the client onboarding activity completes normally.
        Intents.intending(hasAction(ACTION_SHOW_ONBOARDING))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(testIntent)

        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                scrollToTextAndClick(TEST_APP_NAME)
                intended(hasAction(ACTION_SHOW_ONBOARDING))
                intended(hasPackage(TEST_APP_PACKAGE_NAME))
                // We should remain where we started.
                assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.homeFragment)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun onboardingActivityAvailable_appAlreadyConnected_doesNotLaunchOnboarding() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                isInactive = false,
                shouldLaunchAppOnboardingIfAvailable = false,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                appPermissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
            )
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        val testIntent = Intent(ACTION_SHOW_ONBOARDING)
        testIntent.setPackage(TEST_APP.packageName)

        // Assume that the client onboarding activity completes normally.
        Intents.intending(hasAction(ACTION_SHOW_ONBOARDING))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(any(), eq(TEST_APP.packageName))
            )
            .thenReturn(testIntent)

        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                scrollToTextAndClick(TEST_APP_NAME)

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.combinedPermissionsFragment)
            }
    }

    @Test
    fun onboardingBannerStateHide_noOnboardingBanner() {
        setPreferenceSeen(context, ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, ONBOARDING_ONE_APP_BANNER_SEEN, false)
        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText("See your health data across apps")).check(doesNotExist())
            onView(withText("Connect a second app")).check(doesNotExist())
        }
    }

    @Test
    fun onboardingBannerStateZeroApps_showsZeroAppsBanner() {
        setPreferenceSeen(context, ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        whenever(onboardingViewModel.onboardingBannerState).then {
            MediatorLiveData(OnboardingViewModel.OnboardingBannerState.ZeroAppsOnboardingBanner)
        }
        launchFragment<HomeFragment>(Bundle()).use { scenario ->
            onView(withText("See your health data across apps")).check(matches(isDisplayed()))
            onView(withText("Start sharing fitness and wellness data between your apps"))
                .check(matches(isDisplayed()))
            onView(withText("Set up")).check(matches(isDisplayed()))
            onView(withText("Not now")).check(matches(isDisplayed()))
            onView(withText("Connect a second app")).check(doesNotExist())

            verify(healthConnectLogger).logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER_SET_UP_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER_DISMISS_BUTTON)

            onView(
                    withId(
                        com.android.settingslib.widget.preference.banner.R.id.banner_negative_btn
                    )
                )
                .perform(scrollTo())
                .perform(click())
            scenario.onActivity { activity ->
                val preferences =
                    activity.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
                assertThat(preferences.getBoolean(ONBOARDING_ZERO_APPS_BANNER_SEEN, false)).isTrue()
            }

            onView(withText("See your health data across apps")).check(doesNotExist())
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.ZERO_APPS_CONNECTED_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun onboardingBannerStateZeroApps_clickOnSetup_navigatesToOnboardingActivity() {
        setPreferenceSeen(context, ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        whenever(onboardingViewModel.onboardingBannerState).then {
            MediatorLiveData(OnboardingViewModel.OnboardingBannerState.ZeroAppsOnboardingBanner)
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use { scenario ->
                onView(withText("See your health data across apps")).check(matches(isDisplayed()))
                onView(withText("Start sharing fitness and wellness data between your apps"))
                    .check(matches(isDisplayed()))
                onView(withText("Set up")).check(matches(isDisplayed()))
                onView(withText("Not now")).check(matches(isDisplayed()))
                onView(withText("Connect a second app")).check(doesNotExist())

                verify(healthConnectLogger)
                    .logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER)
                verify(healthConnectLogger)
                    .logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER_SET_UP_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(HomePageElement.ZERO_APPS_CONNECTED_BANNER_DISMISS_BUTTON)

                onView(withText("Set up")).perform(scrollTo()).perform(click())
                verify(healthConnectLogger)
                    .logInteraction(HomePageElement.ZERO_APPS_CONNECTED_BANNER_SET_UP_BUTTON)
                intended(hasAction("android.health.connect.action.SYNC_MORE_APPS"))
            }
    }

    @Test
    fun onboardingBannerStateOneApp_showsOneAppBanner() {
        setPreferenceSeen(context, ONBOARDING_ONE_APP_BANNER_SEEN, false)
        whenever(onboardingViewModel.onboardingBannerState).then {
            MediatorLiveData(OnboardingViewModel.OnboardingBannerState.OneAppOnboardingBanner)
        }
        launchFragment<HomeFragment>(Bundle()).use { scenario ->
            onView(withText("Connect a second app")).check(matches(isDisplayed()))
            onView(withText("Set up another app so it can start sharing fitness and wellness data"))
                .check(matches(isDisplayed()))
            onView(withText("Continue")).check(matches(isDisplayed()))
            onView(withText("Not now")).check(matches(isDisplayed()))
            onView(withText("See your health data across apps")).check(doesNotExist())

            verify(healthConnectLogger).logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER_SET_UP_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER_DISMISS_BUTTON)

            onView(
                    withId(
                        com.android.settingslib.widget.preference.banner.R.id.banner_negative_btn
                    )
                )
                .perform(scrollTo())
                .perform(click())
            scenario.onActivity { activity ->
                val preferences =
                    activity.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
                assertThat(preferences.getBoolean(ONBOARDING_ONE_APP_BANNER_SEEN, false)).isTrue()
            }

            onView(withText("Connect a second app")).check(doesNotExist())
            verify(healthConnectLogger)
                .logInteraction(HomePageElement.ONE_APP_CONNECTED_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    fun onboardingBannerStateOneApp_clickOnContinue_navigatesToOnboardingActivity() {
        setPreferenceSeen(context, ONBOARDING_ONE_APP_BANNER_SEEN, false)
        whenever(onboardingViewModel.onboardingBannerState).then {
            MediatorLiveData(OnboardingViewModel.OnboardingBannerState.OneAppOnboardingBanner)
        }
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use { scenario ->
                onView(withText("Connect a second app")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Set up another app so it can start sharing fitness and wellness data"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(withText("Continue")).check(matches(isDisplayed()))
                onView(withText("Not now")).check(matches(isDisplayed()))
                onView(withText("See your health data across apps")).check(doesNotExist())

                verify(healthConnectLogger).logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER)
                verify(healthConnectLogger)
                    .logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER_SET_UP_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(HomePageElement.ONE_APP_CONNECTED_BANNER_DISMISS_BUTTON)

                onView(withText("Continue")).perform(scrollTo()).perform(click())

                verify(healthConnectLogger)
                    .logInteraction(HomePageElement.ONE_APP_CONNECTED_BANNER_SET_UP_BUTTON)
                intended(hasAction("android.health.connect.action.SYNC_MORE_APPS"))
            }
    }

    // endregion onboarding

    // region native step tracking
    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun devices_whenFlagEnabled_isDisplayed() {
        setupFragmentForNavigation().use { checkTextIsDisplayed("Devices") }
    }

    @Test
    @DisableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun devices_whenFlagDisabled_isNotDisplayed() {
        setupFragmentForNavigation().use { onView(withText("Devices")).check(doesNotExist()) }
    }

    @Test
    @DisableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun nativeStepsBanner_whenFlagDisabled_isNotDisplayed() {
        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText(R.string.native_steps_banner_title)).check(doesNotExist())
            onView(withText(R.string.native_steps_banner_summary)).check(doesNotExist())
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun nativeStepsBannerWasSeen_noNativeStepsBanner() {
        setPreferenceSeen(context, NATIVE_STEPS_BANNER_SEEN, true)
        whenever(nativeStepsNotificationViewModel.wasSeen).then { MutableLiveData<Boolean>(true) }
        launchFragment<HomeFragment>(Bundle()).use {
            onView(withText(R.string.native_steps_banner_title)).check(doesNotExist())
            onView(withText(R.string.native_steps_banner_summary)).check(doesNotExist())
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun nativeStepsBannerWasNotSeen_showsNativeStepsBanner() {
        setPreferenceSeen(context, NATIVE_STEPS_BANNER_SEEN, false)
        launchFragment<HomeFragment>(Bundle()).use { scenario ->
            onView(withText(R.string.native_steps_banner_title)).check(matches(isDisplayed()))
            onView(withText(R.string.native_steps_banner_summary)).check(matches(isDisplayed()))
            onView(withText(R.string.native_steps_banner_dismiss_button))
                .check(matches(isDisplayed()))
            onView(withText(R.string.native_steps_banner_review_button))
                .check(matches(isDisplayed()))

            verify(healthConnectLogger).logImpression(HomePageElement.NATIVE_STEPS_BANNER)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.NATIVE_STEPS_BANNER_REVIEW_BUTTON)
            verify(healthConnectLogger)
                .logImpression(HomePageElement.NATIVE_STEPS_BANNER_DISMISS_BUTTON)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun nativeStepsBannerWasNotSeen_dismissesNativeStepsBanner() {
        setPreferenceSeen(context, NATIVE_STEPS_BANNER_SEEN, false)
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use { scenario ->
                onView(
                        withId(
                            com.android.settingslib.widget.preference.banner.R.id
                                .banner_negative_btn
                        )
                    )
                    .perform(scrollTo())
                    .perform(click())
                scenario.onActivity { activity ->
                    val preferences =
                        activity.getSharedPreferences(
                            Constants.USER_ACTIVITY_TRACKER,
                            Context.MODE_PRIVATE,
                        )
                    assertThat(preferences.getBoolean(NATIVE_STEPS_BANNER_SEEN, false)).isTrue()
                }

                onView(withText(R.string.native_steps_banner_title)).check(doesNotExist())
                onView(withText(R.string.native_steps_banner_summary)).check(doesNotExist())
                verify(healthConnectLogger)
                    .logInteraction(HomePageElement.NATIVE_STEPS_BANNER_DISMISS_BUTTON)
                assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.homeFragment)
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun nativeStepsBannerWasNotSeen_navigatesToManageDevices() {
        setPreferenceSeen(context, NATIVE_STEPS_BANNER_SEEN, false)
        launchFragment<HomeFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.homeFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use { scenario ->
                onView(
                        withId(
                            com.android.settingslib.widget.preference.banner.R.id
                                .banner_positive_btn
                        )
                    )
                    .perform(scrollTo())
                    .perform(click())
                scenario.onActivity { activity ->
                    val preferences =
                        activity.getSharedPreferences(
                            Constants.USER_ACTIVITY_TRACKER,
                            Context.MODE_PRIVATE,
                        )
                    assertThat(preferences.getBoolean(NATIVE_STEPS_BANNER_SEEN, false)).isTrue()
                }

                verify(healthConnectLogger)
                    .logInteraction(HomePageElement.NATIVE_STEPS_BANNER_REVIEW_BUTTON)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.connectedDevicesFragment)
            }
    }

    // endregion

    private fun setupFragmentForNavigation(): ActivityScenario<TestActivity> {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle(),
                        HealthDataCategory.VITALS.uppercaseTitle(),
                    ),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle(),
                        HealthDataCategory.NUTRITION.uppercaseTitle(),
                    ),
                shouldLaunchAppOnboardingIfAvailable = false,
            )

        timeSource.setIs24Hour(true)

        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<RecentAccessState>(RecentAccessState.WithData(listOf(recentApp)))
        }
        whenever(homeViewModel.connectedApps).then {
            MutableLiveData(listOf<ConnectedAppMetadata>())
        }

        return launchFragment<HomeFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
    }

    private fun setBannersSeen(seen: Boolean) {
        setPreferenceSeen(context, Constants.APP_UPDATE_NEEDED_SEEN, seen)
        setPreferenceSeen(context, Constants.MODULE_UPDATE_NEEDED_SEEN, seen)
        setPreferenceSeen(context, Constants.INTEGRATION_PAUSED_SEEN_KEY, seen)

        setPreferenceSeen(context, NATIVE_STEPS_BANNER_SEEN, seen)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_MEDICAL, seen)
        setPreferenceSeen(context, LOCK_SCREEN_BANNER_SEEN_FITNESS, seen)

        setPreferenceSeen(context, ONBOARDING_ZERO_APPS_BANNER_SEEN, seen)
        setPreferenceSeen(context, ONBOARDING_ONE_APP_BANNER_SEEN, seen)
    }
}
