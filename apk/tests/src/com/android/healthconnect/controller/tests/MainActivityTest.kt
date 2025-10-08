package com.android.healthconnect.controller.tests

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.MainActivity
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.WithData
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.newHome.HomeViewModel
import com.android.healthconnect.controller.newHome.HomeViewModel.BannerData
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.checkTextIsDisplayed
import com.android.healthconnect.controller.tests.utils.scrollToTextAndClick
import com.android.healthconnect.controller.tests.utils.showNativeSteps
import com.android.healthconnect.controller.tests.utils.showOnboarding
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthfitness.flags.Flags
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: MigrationViewModel = mock()
    @BindValue val exportStatusViewModel: ExportStatusViewModel = mock()
    @BindValue val recentAccessViewModel: RecentAccessViewModel = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    @BindValue val onboardingViewModel: OnboardingViewModel = mock()
    @BindValue val newHomeViewModel: HomeViewModel = mock()

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        // Required for aconfig flag reading for tests run on pre V devices
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .adoptShellPermissionIdentity(Manifest.permission.READ_DEVICE_CONFIG)
        context = InstrumentationRegistry.getInstrumentation().context
        whenever(viewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(viewModel.migrationState).then {
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
        whenever(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData(RecentAccessViewModel.RecentAccessState.WithData(listOf()))
        }
        whenever(onboardingViewModel.connectedApps).then {
            MutableLiveData(
                OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, false),
                        ConnectedFitnessAppMetadata(TEST_APP_2, false),
                    )
                )
            )
        }
        whenever(onboardingViewModel.onboardingBannerState).then {
            MediatorLiveData(OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner)
        }
        setPreferenceSeen(context, Constants.SEE_MORE_COMPATIBLE_APPS_BANNER_SEEN, true)
        setPreferenceSeen(context, Constants.START_USING_HC_BANNER_SEEN, true)
        setPreferenceSeen(context, Constants.CONNECT_MORE_APPS_BANNER_SEEN, true)

        whenever(newHomeViewModel.homeFragmentState).then {
            MutableStateFlow(HomeViewModel.HomeFragmentState.WithData(emptyList()))
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    fun homeSettingsIntent_launchesMainActivity() = runTest {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent).use {
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                checkTextIsDisplayed("No recent access")
            } else {
                checkTextIsDisplayed("No apps recently accessed Health\u00A0Connect")
            }
            checkTextIsDisplayed("Permissions and data")
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    fun homeSettingsIntent_launchesMainActivity_withNewHomeScreen() = runTest {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent).use {
            checkTextIsDisplayed("Your health apps")
            checkTextIsDisplayed("Your health data")
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    fun homeSettingsIntent_launchesMainActivity_retainsFragmentDestinationAfterRotation() =
        runTest {
            val startActivityIntent =
                Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            launchActivityForResult<MainActivity>(startActivityIntent).use { scenario ->
                checkTextIsDisplayed("Your health apps")
                checkTextIsDisplayed("Your health data")
                scrollToTextAndClick("Recent access")
                checkTextIsDisplayed("See which apps have accessed your data in the past 24 hours")
                scenario.recreate()
                checkTextIsDisplayed("See which apps have accessed your data in the past 24 hours")
            }
        }

    @Test
    fun homeSettingsIntent_migrationInProgress_redirectsToMigrationInProgress() = runTest {
        showOnboarding(context, false)
        showNativeSteps(context, false)
        whenever(viewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IN_PROGRESS,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(viewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IN_PROGRESS,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }

        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent).use {
            checkTextIsDisplayed("Integration in progress")
        }
    }

    @Test
    fun homeSettingsIntent_dataRestoreInProgress_redirectsToRestoreInProgress() = runTest {
        showOnboarding(context, false)
        showNativeSteps(context, false)
        whenever(viewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(viewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }

        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent).use {
            checkTextIsDisplayed("Restore in progress")
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    fun homeSettingsIntent_migrationPending_moduleUpdateSeen_launchesMainActivity() = runTest {
        showOnboarding(context, false)
        showNativeSteps(context, false)
        setPreferenceSeen(context, Constants.MODULE_UPDATE_NEEDED_SEEN, true)
        whenever(viewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.MODULE_UPGRADE_REQUIRED,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(viewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.MODULE_UPGRADE_REQUIRED,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent).use {
            checkTextIsDisplayed("Resume integration")
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                checkTextIsDisplayed("No recent access")
            } else {
                checkTextIsDisplayed("No apps recently accessed Health\u00A0Connect")
            }
            checkTextIsDisplayed("Permissions and data")
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    @Ignore("b/445923123 - enable when banners working")
    fun homeSettingsIntent_migrationPending_moduleUpdateSeen_launchesMainActivity_withNewHomeScreen() =
        runTest {
            showOnboarding(context, false)
            showNativeSteps(context, false)
            setPreferenceSeen(context, Constants.MODULE_UPDATE_NEEDED_SEEN, true)
            whenever(viewModel.getCurrentMigrationUiState()).then {
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.MODULE_UPGRADE_REQUIRED,
                    dataRestoreState = DataRestoreUiState.IDLE,
                    dataRestoreError = DataRestoreUiError.ERROR_NONE,
                )
            }
            whenever(viewModel.migrationState).then {
                MutableLiveData(
                    WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.MODULE_UPGRADE_REQUIRED,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
                )
            }
            whenever(newHomeViewModel.homeFragmentState).then {
                MutableStateFlow(
                    HomeViewModel.HomeFragmentState.WithData(
                        connectedApps = emptyList(),
                        bannerState =
                            HomeViewModel.HomeBannerState.ShowBanners(
                                listOf(BannerData.MigrationBanner)
                            ),
                    )
                )
            }

            val startActivityIntent =
                Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            launchActivityForResult<MainActivity>(startActivityIntent)

            checkTextIsDisplayed("Resume integration")
            checkTextIsDisplayed("Your health apps")
            checkTextIsDisplayed("Your health data")
        }

    @Test
    @DisableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    fun homeSettingsIntent_migrationPending_appUpgradeSeen_launchesMainActivity() = runTest {
        showOnboarding(context, false)
        showNativeSteps(context, false)
        setPreferenceSeen(context, Constants.APP_UPDATE_NEEDED_SEEN, true)
        whenever(viewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(viewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }

        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent).use {
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                checkTextIsDisplayed("No recent access")
            } else {
                checkTextIsDisplayed("No apps recently accessed Health\u00A0Connect")
            }
            checkTextIsDisplayed("Permissions and data")
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    @Ignore("b/445923123 - enable when banners working")
    fun homeSettingsIntent_migrationPending_appUpgradeSeen_launchesMainActivity_withNewHomeScreen() =
        runTest {
            showOnboarding(context, false)
            showNativeSteps(context, false)
            setPreferenceSeen(context, Constants.APP_UPDATE_NEEDED_SEEN, true)
            whenever(viewModel.getCurrentMigrationUiState()).then {
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                    dataRestoreState = DataRestoreUiState.IDLE,
                    dataRestoreError = DataRestoreUiError.ERROR_NONE,
                )
            }
            whenever(viewModel.migrationState).then {
                MutableLiveData(
                    WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
                )
            }
            whenever(newHomeViewModel.homeFragmentState).then {
                MutableStateFlow(
                    HomeViewModel.HomeFragmentState.WithData(
                        connectedApps = emptyList(),
                        bannerState =
                            HomeViewModel.HomeBannerState.ShowBanners(
                                listOf(BannerData.MigrationBanner)
                            ),
                    )
                )
            }

            val startActivityIntent =
                Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            launchActivityForResult<MainActivity>(startActivityIntent)

            checkTextIsDisplayed("Resume integration")
            checkTextIsDisplayed("Your health apps")
            checkTextIsDisplayed("Your health data")
        }

    @Test
    @DisableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    fun homeSettingsIntent_migrationPending_integrationPausedSeen_launchesMainActivity() = runTest {
        showOnboarding(context, false)
        showNativeSteps(context, false)
        setPreferenceSeen(context, Constants.INTEGRATION_PAUSED_SEEN_KEY, true)
        whenever(viewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.ALLOWED_PAUSED,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(viewModel.migrationState).then {
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

        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent).use {
            checkTextIsDisplayed("Resume integration")
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                checkTextIsDisplayed("No recent access")
            } else {
                checkTextIsDisplayed("No apps recently accessed Health\u00A0Connect")
            }
            checkTextIsDisplayed("Permissions and data")
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_NEW_HOME_SCREEN)
    @Ignore("b/445923123 - enable when banners working")
    fun homeSettingsIntent_migrationPending_integrationPausedSeen_launchesMainActivity_withNewHomeScreen() =
        runTest {
            showOnboarding(context, false)
            showNativeSteps(context, false)
            setPreferenceSeen(context, Constants.INTEGRATION_PAUSED_SEEN_KEY, true)
            whenever(viewModel.getCurrentMigrationUiState()).then {
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.ALLOWED_PAUSED,
                    dataRestoreState = DataRestoreUiState.IDLE,
                    dataRestoreError = DataRestoreUiError.ERROR_NONE,
                )
            }
            whenever(viewModel.migrationState).then {
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
            whenever(newHomeViewModel.homeFragmentState).then {
                MutableStateFlow(
                    HomeViewModel.HomeFragmentState.WithData(
                        connectedApps = emptyList(),
                        bannerState =
                            HomeViewModel.HomeBannerState.ShowBanners(
                                listOf(BannerData.MigrationBanner)
                            ),
                    )
                )
            }

            val startActivityIntent =
                Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            launchActivityForResult<MainActivity>(startActivityIntent)

            checkTextIsDisplayed("Resume integration")
            checkTextIsDisplayed("Your health apps")
            checkTextIsDisplayed("Your health data")
        }

    @After
    fun tearDown() {
        showOnboarding(context, false)
        showNativeSteps(context, false)
        setPreferenceSeen(context, Constants.APP_UPDATE_NEEDED_SEEN, false)
        setPreferenceSeen(context, Constants.MODULE_UPDATE_NEEDED_SEEN, false)
        setPreferenceSeen(context, Constants.INTEGRATION_PAUSED_SEEN_KEY, false)

        setPreferenceSeen(context, Constants.SEE_MORE_COMPATIBLE_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.START_USING_HC_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.CONNECT_MORE_APPS_BANNER_SEEN, false)
    }

    private fun setPreferenceSeen(context: Context, preferenceName: String, seen: Boolean) {
        val sharedPreference =
            context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putBoolean(preferenceName, seen)
        editor.apply()
    }
}
