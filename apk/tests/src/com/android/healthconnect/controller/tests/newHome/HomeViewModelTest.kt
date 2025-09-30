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
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MedicalResource
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.WeightRecord
import android.os.OutcomeReceiver
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
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
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_4
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadMigrationStateUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadOnboardingStateUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.tests.utils.getDataOrigin
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
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class HomeViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var viewModel: HomeViewModel
    private lateinit var loadAllDataUseCase: AllDataUseCase

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val loadHealthPermissionApps = FakeHealthPermissionAppsUseCase()
    private val keyguardManagerUtil: KeyguardManagerUtil = mock()
    private val deviceInfoUtils = FakeDeviceInfoUtils()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val manager: HealthConnectManager = mock()

    private val loadMigrationRestoreStateUseCase = FakeLoadMigrationStateUseCase()
    private val loadScheduledExportStatusUseCase = FakeLoadScheduledExportStatusUseCase()
    private val loadOnboardingStateUseCase = FakeLoadOnboardingStateUseCase()

    private val mockFitnessData =
        mapOf(
            StepsRecord::class.java to
                RecordTypeInfoResponse(
                    HealthPermissionCategory.STEPS,
                    HealthDataCategory.ACTIVITY,
                    listOf(
                        getDataOrigin(TEST_APP_PACKAGE_NAME),
                        getDataOrigin(TEST_APP_PACKAGE_NAME_2),
                    ),
                ),
            WeightRecord::class.java to
                RecordTypeInfoResponse(
                    HealthPermissionCategory.WEIGHT,
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_2))),
                ),
            HeartRateRecord::class.java to
                RecordTypeInfoResponse(
                    HealthPermissionCategory.HEART_RATE,
                    HealthDataCategory.VITALS,
                    listOf((getDataOrigin(TEST_APP_PACKAGE_NAME))),
                ),
        )

    private val mockMedicalData =
        listOf(
            MedicalResourceTypeInfo(
                MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                setOf(TEST_MEDICAL_DATA_SOURCE),
            )
        )

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        loadAllDataUseCase = AllDataUseCase(manager, Dispatchers.Main)
        mockNoBanners()
        mockLoadAllDataUseCase(listOf(), mapOf())
        deviceInfoUtils.setIntentHandlerAvailability(true)

        viewModel =
            HomeViewModel(
                context,
                loadHealthPermissionApps,
                loadAllDataUseCase,
                keyguardManagerUtil,
                deviceInfoUtils,
                loadMigrationRestoreStateUseCase,
                loadScheduledExportStatusUseCase,
                loadOnboardingStateUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        loadMigrationRestoreStateUseCase.reset()
        loadScheduledExportStatusUseCase.reset()
        loadOnboardingStateUseCase.reset()
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
    fun loadData_success_sortsAppsByStatusThenByName() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.DENIED)
        loadHealthPermissionApps.updateList(listOf(app1, app2, app3, app4))

        val state = loadHomeFragmentState()
        assertThat(state).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        assertThat((state as HomeViewModel.HomeFragmentState.WithData).connectedApps)
            .containsExactly(app1, app3, app2, app4)
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

    // region Migration banner
    @Test
    fun loadMigrationBanners_error_doesNotAddMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setForceFail(true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadMigrationBanners_whenDataRestorePendingAndErrorVersionDiff_addsDataRestoreBanner() =
        runTest {
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
                .containsExactly(BannerData.DataRestorePendingBanner)
        }

    @Test
    fun loadMigrationBanners_whenDataRestoreNotPendingAndErrorVersionDiff_doesNotDataRestoreBanner() =
        runTest {
            loadMigrationRestoreStateUseCase.setMigrationState(
                MigrationRestoreState(
                    migrationUiState = MigrationUiState.IDLE,
                    dataRestoreState = MigrationRestoreState.DataRestoreUiState.IN_PROGRESS,
                    dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_UNKNOWN,
                )
            )
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
        }

    @Test
    fun loadMigrationBanners_whenMigrationAllowedPaused_addsMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.ALLOWED_PAUSED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.MigrationBanner)
    }

    @Test
    fun loadMigrationBanners_whenMigrationAllowedNotStarted_addsMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.ALLOWED_NOT_STARTED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.MigrationBanner)
    }

    @Test
    fun loadMigrationBanners_whenMigrationModuleUpgradeRequired_addsMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.MODULE_UPGRADE_REQUIRED,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IDLE,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
        assertThat((state as HomeBannerState.ShowBanners).banners)
            .containsExactly(BannerData.MigrationBanner)
    }

    @Test
    fun loadMigrationBanners_whenMigrationAppUpgradeRequired_addsMigrationBanner() = runTest {
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
            .containsExactly(BannerData.MigrationBanner)
    }

    @Test
    fun loadMigrationBanners_whenMigrationStateIdleOrDone_doesNotAddMigrationBanner() = runTest {
        loadMigrationRestoreStateUseCase.setMigrationState(
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IN_PROGRESS,
                dataRestoreState = MigrationRestoreState.DataRestoreUiState.IN_PROGRESS,
                dataRestoreError = MigrationRestoreState.DataRestoreUiError.ERROR_NONE,
            )
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
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
        doAnswer(prepareFailureAnswer()).`when`(manager).queryAllRecordTypesInfo(any(), any())

        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadLockScreenBanner_whenAnyMedicalDataError_doesNotAddLockScreenBanner() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        doAnswer(prepareFailureAnswer())
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    fun loadLockScreenBanner_whenFitnessDataAndFitnessBannerSeen_doesNotAddLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, true)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
            mockLoadAllDataUseCase(
                medicalResourceTypeInfo = listOf(),
                recordTypeInfoMap = mockFitnessData,
            )
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
        }

    @Test
    fun loadLockScreenBanner_whenMedicalDataAndMedicalBannerSeen_doesNotAddLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, true)
            mockLoadAllDataUseCase(
                medicalResourceTypeInfo = mockMedicalData,
                recordTypeInfoMap = mapOf(),
            )
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
        }

    @Test
    fun loadLockScreenBanner_whenFitnessDataAndFitnessBannerNotSeen_addsLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
            mockLoadAllDataUseCase(
                medicalResourceTypeInfo = listOf(),
                recordTypeInfoMap = mockFitnessData,
            )
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
            assertThat((state as HomeBannerState.ShowBanners).banners)
                .containsExactly(
                    BannerData.LockScreenBanner(hasAnyFitnessData = true, hasAnyMedicalData = false)
                )
        }

    @Test
    fun loadLockScreenBanner_whenMedicalDataAndMedicalBannerNotSeen_addsLockScreenBanner() =
        runTest {
            whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
            setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
            mockLoadAllDataUseCase(
                medicalResourceTypeInfo = mockMedicalData,
                recordTypeInfoMap = mapOf(),
            )
            val state = loadBannerState()
            assertThat(state).isInstanceOf(HomeBannerState.ShowBanners::class.java)
            assertThat((state as HomeBannerState.ShowBanners).banners)
                .containsExactly(
                    BannerData.LockScreenBanner(hasAnyFitnessData = false, hasAnyMedicalData = true)
                )
        }

    @Test
    fun loadLockScreenBanner_whenCombinedDataAndNoBannerNotSeen_addsLockScreenBanner() = runTest {
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        mockLoadAllDataUseCase(
            medicalResourceTypeInfo = mockMedicalData,
            recordTypeInfoMap = mockFitnessData,
        )
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
    @DisableFlags(Flags.FLAG_ONBOARDING)
    fun loadOnboardingBanner_whenOnboardingFlagOff_doesNotAddOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_ONBOARDING)
    fun loadOnboardingBanner_whenOnboardingError_doesNotAddOnboardingBanner() = runTest {
        setPreferenceSeen(context, Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        setPreferenceSeen(context, Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadOnboardingStateUseCase.setForceFail(true)
        val state = loadBannerState()
        assertThat(state).isInstanceOf(HomeBannerState.NoBanner::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_ONBOARDING)
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
    @EnableFlags(Flags.FLAG_ONBOARDING)
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
    @EnableFlags(Flags.FLAG_ONBOARDING)
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
    @EnableFlags(Flags.FLAG_ONBOARDING)
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
    @EnableFlags(Flags.FLAG_ONBOARDING)
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
        mockLoadAllDataUseCase(
            medicalResourceTypeInfo = mockMedicalData,
            recordTypeInfoMap = mockFitnessData,
        )

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
    @EnableFlags(Flags.FLAG_ONBOARDING, Flags.FLAG_STEP_TRACKING_ENABLED)
    fun loadBanners_canAddAllBanners() = runTest {
        // Lock screen banner
        whenever(keyguardManagerUtil.isDeviceSecure(any())).thenReturn(false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        setPreferenceSeen(context, Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        mockLoadAllDataUseCase(
            medicalResourceTypeInfo = mockMedicalData,
            recordTypeInfoMap = mockFitnessData,
        )

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

    private fun mockLoadAllDataUseCase(
        medicalResourceTypeInfo: List<MedicalResourceTypeInfo>,
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
    ) {
        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(any(), any())
        doAnswer(prepareAnswer(medicalResourceTypeInfo))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(any(), any())
    }

    private fun prepareAnswer(
        medicalResourceTypeInfo: List<MedicalResourceTypeInfo>
    ): (InvocationOnMock) -> List<MedicalResourceTypeInfo> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(medicalResourceTypeInfo)
            medicalResourceTypeInfo
        }
        return answer
    }

    private fun prepareAnswer(
        map: Map<Class<out Record>, RecordTypeInfoResponse>
    ): (InvocationOnMock) -> Map<Class<out Record>, RecordTypeInfoResponse> {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[1]
                    as OutcomeReceiver<Map<Class<out Record>, RecordTypeInfoResponse>, *>
            receiver.onResult(map)
            map
        }
        return answer
    }

    private fun prepareFailureAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, HealthConnectException>
            receiver.onError(HealthConnectException(HealthConnectException.ERROR_UNKNOWN))
            null
        }
        return answer
    }

    private fun setPreferenceSeen(context: Context, preferenceName: String, seen: Boolean) {
        val sharedPreference =
            context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putBoolean(preferenceName, seen)
        editor.apply()
    }
}
