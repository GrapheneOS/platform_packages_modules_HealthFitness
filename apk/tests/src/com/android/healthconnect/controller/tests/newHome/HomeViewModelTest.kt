/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.newhome

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.newHome.HomeViewModel
import com.android.healthconnect.controller.newHome.HomeViewModel.BannerData
import com.android.healthconnect.controller.newHome.HomeViewModel.HomeBannerState
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.data.alldata.api.FakeHasFitnessDataUseCase
import com.android.healthconnect.controller.tests.data.alldata.api.FakeHasMedicalDataUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_4
import com.android.healthconnect.controller.tests.utils.TEST_APP_5
import com.android.healthconnect.controller.tests.utils.TEST_APP_6
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadMigrationStateUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadOnboardingStateUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.utils.KeyguardManagerUtil
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class HomeViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    private lateinit var viewModel: HomeViewModel

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val loadHealthPermissionApps = FakeHealthPermissionAppsUseCase()
    private val keyguardManagerUtil: KeyguardManagerUtil = mock()
    private val deviceInfoUtils = FakeDeviceInfoUtils()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val loadMigrationRestoreStateUseCase = FakeLoadMigrationStateUseCase()
    private val loadScheduledExportStatusUseCase = FakeLoadScheduledExportStatusUseCase()
    private val loadOnboardingStateUseCase = fakeUseCaseRule.watch(FakeLoadOnboardingStateUseCase())
    private val hasFitnessDataUseCase = fakeUseCaseRule.watch(FakeHasFitnessDataUseCase())
    private val hasMedicalDataUseCase = fakeUseCaseRule.watch(FakeHasMedicalDataUseCase())

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        mockNoBanners()
        deviceInfoUtils.setIntentHandlerAvailability(true)

        viewModel =
            HomeViewModel(
                context,
                loadHealthPermissionApps,
                keyguardManagerUtil,
                deviceInfoUtils,
                loadMigrationRestoreStateUseCase,
                loadScheduledExportStatusUseCase,
                loadOnboardingStateUseCase,
                hasFitnessDataUseCase,
                hasMedicalDataUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        loadMigrationRestoreStateUseCase.reset()
        loadScheduledExportStatusUseCase.reset()
    }

    // region Connected apps
    @Test
    fun loadConnectedApps_whenAppLoadFails_setsErrorState() = runTest {
        loadHealthPermissionApps.setForceFail(true)

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.Error::class.java)
    }

    @Test
    fun loadData_success_filtersInactiveAndNeedsUpdateApps() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.INACTIVE)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.NEEDS_UPDATE)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app2)
    }

    @Test
    fun loadData_success_filtersSystemApps() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED, isSystem = true)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.DENIED, isSystem = true)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app2)
        assertThat(state.showSeeMoreHealthApps).isTrue()
    }

    @Test
    fun loadData_success_onlyAllowedApps_showsAllInAlphabeticOrder() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.ALLOWED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.ALLOWED)
        val app5 = ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.ALLOWED)
        val app6 = ConnectedAppMetadata(TEST_APP_6, ConnectedAppStatus.ALLOWED)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4, app5, app6))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app2, app3, app4, app5, app6)
            .inOrder()
        assertThat(state.showSeeMoreHealthApps).isTrue()
    }

    @Test
    fun loadData_success_onlyDeniedApps_showsAllInAlphabeticOrder() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.DENIED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.DENIED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.DENIED)
        val app5 = ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.DENIED)
        val app6 = ConnectedAppMetadata(TEST_APP_6, ConnectedAppStatus.DENIED)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4, app5, app6))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app2, app3, app4, app5, app6)
            .inOrder()
        assertThat(state.showSeeMoreHealthApps).isTrue()
    }

    @Test
    fun loadData_success_oneDeniedApp_showsDeniedAppInTop5() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.ALLOWED)
        val app5 = ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.ALLOWED)
        val app6 = ConnectedAppMetadata(TEST_APP_6, ConnectedAppStatus.ALLOWED)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4, app5, app6))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app3, app4, app5, app2, app6)
            .inOrder()
        assertThat(state.showSeeMoreHealthApps).isTrue()
    }

    @Test
    fun loadData_success_3allowed2denied() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.DENIED)
        val app5 = ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.ALLOWED)
        val app6 = ConnectedAppMetadata(TEST_APP_6, ConnectedAppStatus.ALLOWED)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4, app5, app6))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app3, app5, app2, app4, app6)
            .inOrder()
        assertThat(state.showSeeMoreHealthApps).isTrue()
    }

    @Test
    fun loadData_success_2allowed3denied() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.DENIED)
        val app5 = ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.DENIED)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4, app5))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app3, app2, app4, app5)
            .inOrder()
        assertThat(state.showSeeMoreHealthApps).isTrue()
    }

    @Test
    fun loadData_success_1allowed4denied() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.DENIED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.DENIED)
        val app5 = ConnectedAppMetadata(TEST_APP_5, ConnectedAppStatus.DENIED)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4, app5))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app2, app3, app4, app5)
            .inOrder()
        assertThat(state.showSeeMoreHealthApps).isTrue()
    }

    @Test
    fun loadData_success_noApps_showSeeMoreHealthAppsFalse() = runTest {
        loadHealthPermissionApps.updateList(listOf())
        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps).isEmpty()
        assertThat(state.showSeeMoreHealthApps).isFalse()
    }

    // endregion

    // region Migration data
    @Test
    fun loadMigrationData_error_doesNotAddMigrationBanner_orDialogs() = runTest {
        loadMigrationRestoreStateUseCase.setForceFail(true)
        val homeFragmentState = loadMigrationData()
        assertThat(homeFragmentState.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
        assertThat(homeFragmentState.second)
            .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
    }

    @Test
    fun loadMigrationData_whenDataRestorePendingAndErrorVersionDiff_addsDataRestoreBanner() =
        runTest {
            loadMigrationRestoreStateUseCase.setMigrationState(
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.IDLE,
                    dataRestoreState = MigrationRestoreState.DataRestoreUiState.PENDING,
                    dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_VERSION_DIFF,
                )
            )
            val state = loadMigrationData()
            assertThat(state.first).isInstanceOf(HomeBannerState.ShowBanners::class.java)
            assertThat((state.first as HomeBannerState.ShowBanners).banners)
                .containsExactly(BannerData.DataRestorePendingBanner)
            assertThat(state.second)
                .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
        }

    @Test
    fun loadMigrationData_whenDataRestoreNotPendingAndErrorVersionDiff_doesNotDataRestoreBanner() =
        runTest {
            loadMigrationRestoreStateUseCase.setMigrationState(
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.IDLE,
                    dataRestoreState = MigrationRestoreState.DataRestoreUiState.IN_PROGRESS,
                    dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_UNKNOWN,
                )
            )
            val state = loadMigrationData()
            assertThat(state.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
            assertThat(state.second)
                .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
        }

    @Test
    fun loadMigrationData_whenMigrationAllowedPaused_addsMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.ALLOWED_PAUSED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadMigrationData()
        assertThat(state.first).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state.first as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.MigrationBanner)
        assertThat(state.second)
            .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
    }

    @Test
    fun loadMigrationData_whenMigrationAllowedNotStarted_addsMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.ALLOWED_NOT_STARTED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadMigrationData()
        assertThat(state.first).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state.first as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.MigrationBanner)
        assertThat(state.second)
            .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
    }

    @Test
    fun loadMigrationData_whenMigrationModuleUpgradeRequired_addsMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.MODULE_UPGRADE_REQUIRED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadMigrationData()
        assertThat(state.first).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state.first as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.MigrationBanner)
        assertThat(state.second)
            .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
    }

    @Test
    fun loadMigrationData_whenMigrationAppUpgradeRequired_addsMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadMigrationData()
        assertThat(state.first).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state.first as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.MigrationBanner)
        assertThat(state.second)
            .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
    }

    @Test
    fun loadMigrationBanners_whenMigrationStateIdle_doesNotAddMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadMigrationData()
        assertThat(state.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
        assertThat(state.second)
            .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
    }

    @Test
    fun loadMigrationBanners_whenMigrationStateCompleteIdle_doesNotAddMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.COMPLETE_IDLE,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadMigrationData()
        assertThat(state.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
        assertThat(state.second)
            .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
    }

    @Test
    fun loadMigrationData_whenMigrationStateComplete_andDialogNotSeen_addsMigrationCompleteDialog() =
        runTest {
            setPreferenceSeen(context, Constants.WHATS_NEW_DIALOG_SEEN, false)
            loadMigrationRestoreStateUseCase.setMigrationState(
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.COMPLETE,
                    dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                    dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
                )
            )
            val state = loadMigrationData()
            assertThat(state.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
            assertThat(state.second)
                .isInstanceOf(HomeViewModel.MigrationDialog.MigrationCompleteDialog::class.java)
        }

    @Test
    fun loadMigrationData_whenMigrationStateComplete_andDialogSeen_doesNotAddMigrationCompleteDialog() =
        runTest {
            setPreferenceSeen(context, Constants.WHATS_NEW_DIALOG_SEEN, true)
            loadMigrationRestoreStateUseCase.setMigrationState(
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.COMPLETE,
                    dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                    dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
                )
            )
            val state = loadMigrationData()
            assertThat(state.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
            assertThat(state.second)
                .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
        }

    @Test
    fun loadMigrationData_whenMigrationStateError_andDialogNotSeen_addsMigrationNotCompleteDialog() =
        runTest {
            setPreferenceSeen(context, Constants.MIGRATION_NOT_COMPLETE_DIALOG_SEEN, false)
            loadMigrationRestoreStateUseCase.setMigrationState(
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.ALLOWED_ERROR,
                    dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                    dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
                )
            )
            val state = loadMigrationData()
            assertThat(state.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
            assertThat(state.second)
                .isInstanceOf(HomeViewModel.MigrationDialog.MigrationNotCompleteDialog::class.java)
        }

    @Test
    fun loadMigrationData_whenMigrationStateError_andDialogSeen_addsMigrationCompleteDialog() =
        runTest {
            setPreferenceSeen(context, Constants.MIGRATION_NOT_COMPLETE_DIALOG_SEEN, true)
            loadMigrationRestoreStateUseCase.setMigrationState(
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.ALLOWED_ERROR,
                    dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                    dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
                )
            )
            val state = loadMigrationData()
            assertThat(state.first).isInstanceOf(HomeBannerState.NoBanner::class.java)
            assertThat(state.second)
                .isInstanceOf(HomeViewModel.MigrationDialog.NoMigrationDialog::class.java)
        }

    @Test
    fun onDismissBanner_migrationBanner_doesNotSetAnyPreference() = runTest {
        // Setup to show migration banner
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .contains(BannerData.MigrationBanner)

        val preferences =
            context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, MODE_PRIVATE)
        val preferencesBefore = preferences.all

        // Dismiss banner
        viewModel.onDismissBanner(BannerData.MigrationBanner)
        advanceUntilIdle()

        // Assert banner is removed from UI
        val stateAfter =
            (viewModel.homeFragmentState.value as HomeViewModel.HomeFragmentState.WithData)
                .bannerState
        assertThat(stateAfter).isInstanceOf(HomeBannerState.NoBanner::class.java)

        // Assert no preferences have changed
        val preferencesAfter = preferences.all
        assertThat(preferencesAfter).isEqualTo(preferencesBefore)
    }

    @Test
    fun onDismissBanner_dataRestorePendingBanner_doesNotSetAnyPreference() = runTest {
        // Setup to show data restore banner
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.PENDING,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_VERSION_DIFF,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .contains(BannerData.DataRestorePendingBanner)

        val preferences =
            context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, MODE_PRIVATE)
        val preferencesBefore = preferences.all

        // Dismiss banner
        viewModel.onDismissBanner(BannerData.DataRestorePendingBanner)
        advanceUntilIdle()

        // Assert banner is removed from UI
        val stateAfter =
            (viewModel.homeFragmentState.value as HomeViewModel.HomeFragmentState.WithData)
                .bannerState
        assertThat(stateAfter).isInstanceOf(HomeBannerState.NoBanner::class.java)

        // Assert no preferences have changed
        val preferencesAfter = preferences.all
        assertThat(preferencesAfter).isEqualTo(preferencesBefore)
    }

    // endregion

    // region Export banner
    @Test
    fun loadExportBanners_errorLoading_doesNotAddExportErrorBanner() = runTest {
        loadScheduledExportStatusUseCase.setForceFail(true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadExportBanners_whenDataExportError_addsExportErrorBanner() = runTest {
        loadScheduledExportStatusUseCase.updateExportStatus(
            ScheduledExportUiState(
                dataExportError =
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                periodInDays = 3,
                lastFailedExportTime = NOW,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.ExportErrorBanner(NOW))
    }

    @Test
    fun loadExportBanners_whenNoDataExportError_doesNotAddExportErrorBanner() = runTest {
        loadScheduledExportStatusUseCase.updateExportStatus(
            ScheduledExportUiState(
                dataExportError = ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                periodInDays = 3,
                lastFailedExportTime = NOW,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun onDismissBanner_exportErrorBanner_doesNotSetAnyPreference() = runTest {
        // Setup to show export error banner
        loadScheduledExportStatusUseCase.updateExportStatus(
            ScheduledExportUiState(
                dataExportError =
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                periodInDays = 3,
                lastFailedExportTime = NOW,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .contains(BannerData.ExportErrorBanner(NOW))

        val preferences =
            context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, MODE_PRIVATE)
        val preferencesBefore = preferences.all

        // Dismiss banner
        viewModel.onDismissBanner(BannerData.ExportErrorBanner(NOW))
        advanceUntilIdle()

        // Assert banner is removed from UI
        val stateAfter =
            (viewModel.homeFragmentState.value as HomeViewModel.HomeFragmentState.WithData)
                .bannerState
        assertThat(stateAfter).isInstanceOf(HomeBannerState.NoBanner::class.java)

        // Assert no preferences have changed
        val preferencesAfter = preferences.all
        assertThat(preferencesAfter).isEqualTo(preferencesBefore)
    }

    // endregion

    // region Steps banner
    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun loadNativeStepsBanner_whenNativeStepsEnabledAndBannerNotSeen_addsNativeStepsBanner() =
        runTest {
            setPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN, false)
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
            assertThat((state as HomeBannerState.ShowBanners).banners)
                .containsExactly(BannerData.NativeStepsBanner)
        }

    @Test
    @DisableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun loadNativeStepsBanner_whenNativeStepsDisabled_doesNotAddNativeStepsBanner() = runTest {
        setPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN, false)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun loadNativeStepsBanner_whenNativeStepsBannerSeen_doesNotAddNativeStepsBanner() = runTest {
        setPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN, true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun onDismissBanner_nativeStepsBanner_setsSharedPreferenceSeen() = runTest {
        setPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN, false)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.NativeStepsBanner)
        viewModel.onDismissBanner(BannerData.NativeStepsBanner)
        advanceUntilIdle()
        assertPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN)
    }

    // endregion

    // region Lock screen banner
    @Test
    fun loadLockScreenBanner_whenDeviceSecure_doesNotAddLockScreenBanner() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(true)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadLockScreenBanner_whenIntentNotHandled_doesNotAddLockScreenBanner() = runTest {
        deviceInfoUtils.setIntentHandlerAvailability(false)
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadLockScreenBanner_whenAnyFitnessDataError_doesNotAddLockScreenBanner() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasFitnessDataUseCase.setForceFail(true)

        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadLockScreenBanner_whenAnyMedicalDataError_doesNotAddLockScreenBanner() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasMedicalDataUseCase.setForceFail(true)

        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadLockScreenBanner_whenFitnessDataAndFitnessBannerSeen_doesNotAddLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, true)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
            hasFitnessDataUseCase.setHasFitnessData(true)
            hasMedicalDataUseCase.setHasMedicalData(false)
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
        }

    @Test
    fun loadLockScreenBanner_whenMedicalDataAndMedicalBannerSeen_doesNotAddLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, true)
            hasFitnessDataUseCase.setHasFitnessData(false)
            hasMedicalDataUseCase.setHasMedicalData(true)
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
        }

    @Test
    fun loadLockScreenBanner_whenFitnessDataAndFitnessBannerNotSeen_addsLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
            hasFitnessDataUseCase.setHasFitnessData(true)
            hasMedicalDataUseCase.setHasMedicalData(false)
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
            assertThat((state as HomeBannerState.ShowBanners).banners)
                .containsExactly(
                    BannerData.LockScreenBanner(hasAnyFitnessData = true, hasAnyMedicalData = false)
                )
        }

    @Test
    fun onDismissBanner_fitnessLockScreenBanner_setsSharedPreferenceSeen() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasFitnessDataUseCase.setHasFitnessData(true)
        hasMedicalDataUseCase.setHasMedicalData(false)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(
                BannerData.LockScreenBanner(hasAnyFitnessData = true, hasAnyMedicalData = false)
            )
        viewModel.onDismissBanner(
            BannerData.LockScreenBanner(hasAnyFitnessData = true, hasAnyMedicalData = false)
        )
        advanceUntilIdle()
        assertPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS)
    }

    @Test
    fun loadLockScreenBanner_whenMedicalDataAndMedicalBannerNotSeen_addsLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
            hasFitnessDataUseCase.setHasFitnessData(false)
            hasMedicalDataUseCase.setHasMedicalData(true)
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
            assertThat((state as HomeBannerState.ShowBanners).banners)
                .containsExactly(
                    BannerData.LockScreenBanner(hasAnyFitnessData = false, hasAnyMedicalData = true)
                )
        }

    @Test
    fun onDismissBanner_medicalLockScreenBanner_setsSharedPreferenceSeen() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasFitnessDataUseCase.setHasFitnessData(false)
        hasMedicalDataUseCase.setHasMedicalData(true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(
                BannerData.LockScreenBanner(hasAnyFitnessData = false, hasAnyMedicalData = true)
            )
        viewModel.onDismissBanner(
            BannerData.LockScreenBanner(hasAnyFitnessData = false, hasAnyMedicalData = true)
        )
        advanceUntilIdle()
        assertPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL)
    }

    @Test
    fun onDismissBanner_combinedLockScreenBanner_setsBothSharedPreferenceSeen() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasFitnessDataUseCase.setHasFitnessData(true)
        hasMedicalDataUseCase.setHasMedicalData(true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .contains(
                BannerData.LockScreenBanner(hasAnyFitnessData = true, hasAnyMedicalData = true)
            )
        viewModel.onDismissBanner(
            BannerData.LockScreenBanner(hasAnyFitnessData = true, hasAnyMedicalData = true)
        )
        advanceUntilIdle()
        assertPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS)
        assertPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL)
    }

    @Test
    fun loadLockScreenBanner_whenCombinedDataAndNoBannerNotSeen_addsLockScreenBanner() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasFitnessDataUseCase.setHasFitnessData(true)
        hasMedicalDataUseCase.setHasMedicalData(true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(
                BannerData.LockScreenBanner(hasAnyFitnessData = true, hasAnyMedicalData = true)
            )
    }

    // endregion

    // region Onboarding banner

    @Test
    fun loadOnboardingBanner_whenOnboardingError_doesNotAddOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setForceFail(true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadOnboardingBanner_whenZeroAppsBannerSeen_doesNotAddOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, true)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadOnboardingBanner_whenOneAppBannerSeen_doesNotAddOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, true)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadOnboardingBanner_whenZeroAppsBannerNotSeen_addsOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.ZeroAppsOnboardingBanner)
    }

    @Test
    fun onDismissBanner_zeroAppsOnboardingBanner_setsSharedPreferenceSeen() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.ZeroAppsOnboardingBanner)
        viewModel.onDismissBanner(BannerData.ZeroAppsOnboardingBanner)
        advanceUntilIdle()
        assertPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN)
    }

    @Test
    fun loadOnboardingBanner_whenOneAppBannerNotSeen_addsOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.OneAppOnboardingBanner)
    }

    @Test
    fun onDismissBanner_oneAppOnboardingBanner_setsSharedPreferenceSeen() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.OneAppOnboardingBanner)
        viewModel.onDismissBanner(BannerData.OneAppOnboardingBanner)
        advanceUntilIdle()
        assertPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN)
    }

    @Test
    fun loadOnboardingBanner_whenOnboardingStateHide_doesNotAddOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_HIDE
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    // endregion

    @Test
    fun loadBanners_canAddMultipleBanners() = runTest {
        // Lock screen banner
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasFitnessDataUseCase.setHasFitnessData(true)
        hasMedicalDataUseCase.setHasMedicalData(true)

        // Export error banner
        loadScheduledExportStatusUseCase.updateExportStatus(
            ScheduledExportUiState(
                dataExportError =
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                periodInDays = 3,
                lastFailedExportTime = NOW,
            )
        )

        // Migration banner
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )

        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactlyElementsIn(
                listOf(
                    BannerData.MigrationBanner,
                    BannerData.ExportErrorBanner(NOW),
                    BannerData.LockScreenBanner(true, true),
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun loadBanners_canAddAllBanners() = runTest {
        // Lock screen banner
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasFitnessDataUseCase.setHasFitnessData(true)
        hasMedicalDataUseCase.setHasMedicalData(true)

        // Export error banner
        loadScheduledExportStatusUseCase.updateExportStatus(
            ScheduledExportUiState(
                dataExportError =
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                periodInDays = 3,
                lastFailedExportTime = NOW,
            )
        )

        // Migration banner
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )

        // Onboarding banner
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )

        // Native steps banner
        setPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN, false)

        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactlyElementsIn(
                listOf(
                    BannerData.MigrationBanner,
                    BannerData.ExportErrorBanner(NOW),
                    BannerData.LockScreenBanner(true, true),
                    BannerData.OneAppOnboardingBanner,
                    BannerData.NativeStepsBanner,
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    fun loadInitialData_doesNotDuplicateBanners() = runTest {
        // Lock screen banner
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        hasMedicalDataUseCase.setHasMedicalData(true)
        hasFitnessDataUseCase.setHasFitnessData(true)

        // Export error banner
        loadScheduledExportStatusUseCase.updateExportStatus(
            ScheduledExportUiState(
                dataExportError =
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                periodInDays = 3,
                lastFailedExportTime = NOW,
            )
        )

        // Migration banner
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )

        // Onboarding banner
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )

        // Native steps banner
        setPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN, false)

        // Load first time
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactlyElementsIn(
                listOf(
                    BannerData.MigrationBanner,
                    BannerData.ExportErrorBanner(NOW),
                    BannerData.LockScreenBanner(true, true),
                    BannerData.OneAppOnboardingBanner,
                    BannerData.NativeStepsBanner,
                )
            )

        // Load second time
        val state2 = loadBannerState()
        assertThat(state2).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state2 as HomeBannerState.ShowBanners).banners)
            .containsExactlyElementsIn(
                listOf(
                    BannerData.MigrationBanner,
                    BannerData.ExportErrorBanner(NOW),
                    BannerData.LockScreenBanner(true, true),
                    BannerData.OneAppOnboardingBanner,
                    BannerData.NativeStepsBanner,
                )
            )
    }

    private fun TestScope.loadHomeFragmentState(): HomeViewModel.HomeFragmentState {
        viewModel.loadInitialData()
        advanceUntilIdle()
        val actualState = mutableListOf<HomeViewModel.HomeFragmentState>()
        val homeFragmentStateCollectJob = launch {
            viewModel.homeFragmentState.collect { value -> actualState.add(value) }
        }

        advanceUntilIdle()
        homeFragmentStateCollectJob.cancel()
        return actualState.last()
    }

    private fun TestScope.loadBannerState(): HomeBannerState {
        val homeFragmentState = loadHomeFragmentState()
        assertThat(homeFragmentState)
            .isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        val state = homeFragmentState as HomeViewModel.HomeFragmentState.WithData
        return state.bannerState
    }

    private fun TestScope.loadMigrationData():
        Pair<HomeBannerState, HomeViewModel.MigrationDialog> {
        val homeFragmentState = loadHomeFragmentState()
        assertThat(homeFragmentState)
            .isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        val state = homeFragmentState as HomeViewModel.HomeFragmentState.WithData
        return Pair(state.bannerState, state.migrationDialog)
    }

    private fun mockNoBanners() {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )

        loadScheduledExportStatusUseCase.updateExportStatus(
            ScheduledExportUiState(
                dataExportError = ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                periodInDays = 3,
            )
        )

        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, true)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, true)
        setPreferenceSeen(context, Constants.NATIVE_STEPS_BANNER_SEEN, true)
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, true)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, true)
    }

    private fun setPreferenceSeen(context: Context, preferenceName: String, seen: Boolean) {
        val sharedPreference =
            context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putBoolean(preferenceName, seen)
        editor.apply()
    }

    private fun assertPreferenceSeen(context: Context, preferenceName: String) {
        val preferences = context.getSharedPreferences("USER_ACTIVITY_TRACKER", MODE_PRIVATE)
        assertThat(preferences.getBoolean(preferenceName, false)).isTrue()
    }
}
