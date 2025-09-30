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

package com.android.healthconnect.controller.newHome

import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_SECURITY_SETTINGS
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.exportimport.api.ILoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.LoadOnboardingStateUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.KeyguardManagerUtil
import com.android.healthfitness.flags.Flags.onboarding
import com.android.healthfitness.flags.Flags.stepTrackingEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val loadHealthPermissionApps: ILoadHealthPermissionApps,
    private val loadAllDataUseCase: AllDataUseCase,
    private val keyguardManagerUtil: KeyguardManagerUtil,
    private val deviceInfoUtils: DeviceInfoUtils,
    private val loadMigrationRestoreStateUseCase: BaseUseCase<Unit, MigrationRestoreState>,
    private val loadScheduledExportStatusUseCase: ILoadScheduledExportStatusUseCase,
    @LoadOnboardingStateUseCase
    private val loadOnboardingStateUseCase: BaseUseCase<Unit, OnboardingState>,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val sharedPreferences =
        context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)

    private val _connectedApps = MutableStateFlow<List<ConnectedAppMetadata>>(emptyList())
    private val _banners = MutableStateFlow<List<BannerData>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _appLoadingError = MutableStateFlow(false)

    var showSystemApps = false

    val homeFragmentState: StateFlow<HomeFragmentState> =
        combine(_connectedApps, _banners, _isLoading, _appLoadingError) {
                apps,
                banners,
                loading,
                error ->
                if (error) {
                    HomeFragmentState.Error
                } else if (loading) {
                    HomeFragmentState.Loading
                } else {
                    HomeFragmentState.WithData(
                        connectedApps = apps,
                        bannerState =
                            if (banners.isNotEmpty()) HomeBannerState.ShowBanners(banners)
                            else HomeBannerState.NoBanner,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeFragmentState.Loading,
            )

    init {
        loadInitialData()
    }

    fun setShouldShowSystemApps(shouldShowSystemApps: Boolean) {
        this.showSystemApps = shouldShowSystemApps
        viewModelScope.launch { loadConnectedApps() }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch { loadMigrationBanners() }
            launch { loadExportErrorBanner() }
            launch { loadNativeStepsBanner() }
            launch { loadLockScreenBanner() }
            launch { loadOnboardingBanners() }

            launch { loadConnectedApps() }

            _isLoading.value = false
        }
    }

    private suspend fun loadConnectedApps() {
        when (val appsResult = loadHealthPermissionApps.invoke(Unit)) {
            is UseCaseResults.Failed -> {
                _appLoadingError.value = true
            }
            is UseCaseResults.Success -> {
                _connectedApps.value =
                    appsResult.data
                        .filter {
                            it.status != ConnectedAppStatus.INACTIVE &&
                                it.status != ConnectedAppStatus.NEEDS_UPDATE
                        }
                        .filter { if (showSystemApps) true else !it.isSystem }
                        .sortedWith(
                            compareBy<ConnectedAppMetadata> { getSortOrder(it.status) }
                                .thenBy { it.appMetadata.appName }
                        )
            }
        }
    }

    private fun addBanner(banner: BannerData) {
        _banners.update { currentBanners ->
            if (banner.id in currentBanners.map { it.id }) {
                currentBanners
            } else {
                currentBanners + banner
            }
        }
    }

    private fun loadMigrationBanners() {
        viewModelScope.launch {
            // migration and restore banners cannot be dismissed
            when (val migrationResult = loadMigrationRestoreStateUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    val (migrationUiState, dataRestoreUiState, dataRestoreError) =
                        migrationResult.data
                    if (
                        dataRestoreUiState == DataRestoreUiState.PENDING &&
                            dataRestoreError ==
                                MigrationRestoreState.DataRestoreUiError.ERROR_VERSION_DIFF
                    ) {
                        addBanner(BannerData.DataRestorePendingBanner)
                    } else if (
                        migrationUiState in
                            listOf(
                                MigrationUiState.ALLOWED_PAUSED,
                                MigrationUiState.ALLOWED_NOT_STARTED,
                                MigrationUiState.MODULE_UPGRADE_REQUIRED,
                                MigrationUiState.APP_UPGRADE_REQUIRED,
                            )
                    ) {
                        addBanner(BannerData.MigrationBanner)
                    }
                }
                is UseCaseResults.Failed -> {
                    // Don't add banner on failure
                    Log.e(TAG, "Failed to load migration banners", migrationResult.exception)
                }
            }
        }
    }

    private fun loadExportErrorBanner() {
        // export error banner cannot be dismissed
        viewModelScope.launch {
            when (val exportResult = loadScheduledExportStatusUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    val exportStatus = exportResult.data
                    if (
                        exportStatus.dataExportError !=
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE
                    ) {
                        exportStatus.lastFailedExportTime?.let {
                            addBanner(BannerData.ExportErrorBanner(it))
                        }
                    }
                }
                is UseCaseResults.Failed -> {
                    // Don't add banner on failure
                    Log.e(TAG, "Failed to load export banners", exportResult.exception)
                }
            }
        }
    }

    private fun loadOnboardingBanners() {
        viewModelScope.launch {
            if (!onboarding()) {
                return@launch
            }
            val onboardingZeroAppsBannerSeen =
                sharedPreferences.getBoolean(Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
            val onboardingOneAppBannerSeen =
                sharedPreferences.getBoolean(Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
            when (val onboardingResult = loadOnboardingStateUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    if (
                        !onboardingZeroAppsBannerSeen &&
                            onboardingResult.data ==
                                OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
                    ) {
                        addBanner(BannerData.ZeroAppsOnboardingBanner)
                    } else if (
                        !onboardingOneAppBannerSeen &&
                            onboardingResult.data ==
                                OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
                    ) {
                        addBanner(BannerData.OneAppOnboardingBanner)
                    }
                    // Otherwise do not add a banner
                }
                is UseCaseResults.Failed -> {
                    // Don't add banner on failure
                    Log.e(TAG, "Failed to load migration banners", onboardingResult.exception)
                }
            }
        }
    }

    private fun loadNativeStepsBanner() {
        viewModelScope.launch {
            if (!stepTrackingEnabled()) {
                return@launch
            }
            val nativeStepsBannerSeen =
                sharedPreferences.getBoolean(Constants.NATIVE_STEPS_BANNER_SEEN, false)
            if (!nativeStepsBannerSeen) {
                addBanner(BannerData.NativeStepsBanner)
            }
        }
    }

    private fun loadLockScreenBanner() {
        viewModelScope.launch {
            // TODO security intent
            val isDeviceSecure = keyguardManagerUtil.isDeviceSecure(context) != false
            if (isDeviceSecure) {
                return@launch
            }
            val securitySettingsIntent = Intent(ACTION_SECURITY_SETTINGS)
            if (!deviceInfoUtils.isIntentHandlerAvailable(context, securitySettingsIntent)) {
                return@launch
            }

            val hasAnyFitnessDataResult = loadAllDataUseCase.loadHasAnyFitnessData()
            val hasAnyMedicalDataResult = loadAllDataUseCase.loadHasAnyMedicalData()
            if (
                hasAnyFitnessDataResult is UseCaseResults.Success &&
                    hasAnyMedicalDataResult is UseCaseResults.Success
            ) {
                val hasAnyFitnessData = hasAnyFitnessDataResult.data
                val hasAnyMedicalData = hasAnyMedicalDataResult.data
                val isBannerSeenWithFitnessData =
                    sharedPreferences.getBoolean(LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
                val isBannerSeenWithMedicalData =
                    sharedPreferences.getBoolean(LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)

                val showBannerWhenFitnessData = hasAnyFitnessData && !isBannerSeenWithFitnessData
                val showBannerWhenMedicalData = hasAnyMedicalData && !isBannerSeenWithMedicalData

                if (showBannerWhenFitnessData || showBannerWhenMedicalData) {
                    addBanner(BannerData.LockScreenBanner(hasAnyFitnessData, hasAnyMedicalData))
                }
            }
        }
    }

    fun onDismissBanner(banner: BannerData) {
        // TODO set all of them as seen
        val key =
            when (banner) {
                is BannerData.LockScreenBanner -> {
                    sharedPreferences.edit().apply {
                        if (banner.hasAnyFitnessData) {
                            putBoolean(LOCK_SCREEN_BANNER_SEEN_FITNESS, true)
                        }
                        if (banner.hasAnyMedicalData) {
                            putBoolean(LOCK_SCREEN_BANNER_SEEN_MEDICAL, true)
                        }
                        apply()
                    }
                    null
                }
                BannerData.NativeStepsBanner -> Constants.NATIVE_STEPS_BANNER_SEEN
                BannerData.ZeroAppsOnboardingBanner -> Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN
                BannerData.OneAppOnboardingBanner -> Constants.ONBOARDING_ONE_APP_BANNER_SEEN
                BannerData.MigrationBanner,
                BannerData.DataRestorePendingBanner,
                is BannerData.ExportErrorBanner ->
                    // TODO(b/337749314): Add shared preference keys for these banners if needed
                    null
            }
        key?.let { sharedPreferences.edit { putBoolean(it, true) } }
        _banners.update { currentBanners -> currentBanners.filterNot { it.id == banner.id } }
    }

    private fun getSortOrder(status: ConnectedAppStatus): Int {
        return when (status) {
            ConnectedAppStatus.ALLOWED -> 1
            ConnectedAppStatus.DENIED -> 2
            else -> 3
        }
    }

    sealed class HomeFragmentState {
        object Loading : HomeFragmentState()

        object Error : HomeFragmentState()

        data class WithData(
            val connectedApps: List<ConnectedAppMetadata>,
            val bannerState: HomeBannerState = HomeBannerState.NoBanner,
        ) : HomeFragmentState()
    }

    sealed class HomeBannerState {
        object NoBanner : HomeBannerState()

        data class ShowBanners(val banners: List<BannerData>) : HomeBannerState()
    }

    sealed class BannerData(val id: String) {
        data class LockScreenBanner(
            val hasAnyFitnessData: Boolean,
            val hasAnyMedicalData: Boolean,
        ) : BannerData("LockScreenBanner")

        object NativeStepsBanner : BannerData("NativeStepsBanner")

        object ZeroAppsOnboardingBanner : BannerData("ZeroAppsOnboardingBanner")

        object OneAppOnboardingBanner : BannerData("OneAppOnboardingBanner")

        object MigrationBanner : BannerData("MigrationBanner")

        object DataRestorePendingBanner : BannerData("DataRestorePendingBanner")

        data class ExportErrorBanner(val lastFailedExportTime: Instant) :
            BannerData("ExportErrorBanner")
    }
}
