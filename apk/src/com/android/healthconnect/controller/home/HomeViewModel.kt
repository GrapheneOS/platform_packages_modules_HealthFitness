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

package com.android.healthconnect.controller.home

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.KeyguardManagerUtil
import com.android.healthconnect.controller.utils.postValueIfUpdated
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val loadHealthPermissionApps: ILoadHealthPermissionApps,
    private val loadAllDataUseCase: AllDataUseCase,
    private val keyguardManagerUtil: KeyguardManagerUtil,
) : ViewModel() {

    private val _connectedApps = MutableLiveData<List<ConnectedAppMetadata>>()
    val connectedApps: LiveData<List<ConnectedAppMetadata>>
        get() = _connectedApps

    private val _hasAnyFitnessData: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _hasAnyMedicalData: MutableLiveData<Boolean> = MutableLiveData(false)
    private var _isBannerSeenWithFitnessData = true
    private var _isBannerSeenWithMedicalData = true
    private var _isDeviceSecure = true

    val showLockScreenBanner =
        MediatorLiveData<LockScreenBannerState>(LockScreenBannerState.NoBanner).apply {
            addSource(_hasAnyFitnessData) { postValue(shouldShowLockScreenBanner()) }
            addSource(_hasAnyMedicalData) { postValue(shouldShowLockScreenBanner()) }
        }

    val hasAnyMedicalData: LiveData<Boolean>
        get() = _hasAnyMedicalData

    init {
        loadConnectedApps()
    }

    fun loadConnectedApps() {
        viewModelScope.launch {
            _connectedApps.postValueIfUpdated(
                loadHealthPermissionApps.invoke().filter { !it.appMetadata.isSystem }
            )
        }
    }

    fun loadHasAnyMedicalData() {
        _hasAnyMedicalData.postValue(false)
        viewModelScope.launch {
            when (val result = loadAllDataUseCase.loadHasAnyMedicalData()) {
                is UseCaseResults.Success -> {
                    _hasAnyMedicalData.postValue(result.data)
                }

                is UseCaseResults.Failed -> {
                    _hasAnyMedicalData.postValue(false)
                }
            }
        }
    }

    fun loadShouldShowLockScreenBanner(sharedPreference: SharedPreferences, context: Context) {
        _isDeviceSecure = keyguardManagerUtil.isDeviceSecure(context) ?: true
        _isBannerSeenWithFitnessData =
            sharedPreference.getBoolean(LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
        _isBannerSeenWithMedicalData =
            sharedPreference.getBoolean(LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)
        loadHasAnyFitnessData()
        loadHasAnyMedicalData()
    }

    private fun loadHasAnyFitnessData() {
        _hasAnyFitnessData.postValue(false)
        viewModelScope.launch {
            when (val result = loadAllDataUseCase.loadHasAnyFitnessData()) {
                is UseCaseResults.Success -> {
                    _hasAnyFitnessData.postValue(result.data)
                }

                is UseCaseResults.Failed -> {
                    _hasAnyFitnessData.postValue(false)
                }
            }
        }
    }

    /**
     * The lock screen banner appears at most twice: 1) when the user has some fitness data, and
     * also 2) when the user has some medical data (while the device is not secure).
     *
     * <p> Example flow: the user has some fitness data -> the banner appears -> user dismisses it
     * -> later the user has some medical data -> the banner appears for the second time.
     */
    private fun shouldShowLockScreenBanner(): LockScreenBannerState {
        if (_isDeviceSecure) {
            return LockScreenBannerState.NoBanner
        }

        val showBannerWhenFitnessData =
            _hasAnyFitnessData.value == true && !_isBannerSeenWithFitnessData
        val showBannerWhenMedicalData =
            _hasAnyMedicalData.value == true && !_isBannerSeenWithMedicalData

        if (showBannerWhenFitnessData || showBannerWhenMedicalData) {
            return LockScreenBannerState.ShowBanner(
                _hasAnyFitnessData.value ?: false,
                _hasAnyMedicalData.value ?: false,
            )
        }

        return LockScreenBannerState.NoBanner
    }

    sealed class LockScreenBannerState {
        object NoBanner : LockScreenBannerState()

        data class ShowBanner(
            val hasAnyFitnessData: Boolean = false,
            val hasAnyMedicalData: Boolean = false,
        ) : LockScreenBannerState()
    }
}
