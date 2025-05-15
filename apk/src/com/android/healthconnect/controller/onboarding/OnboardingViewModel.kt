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

package com.android.healthconnect.controller.onboarding

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.onboarding.api.ILoadOnboardingStateUseCase
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.shared.Constants.ONBOARDING_ONE_APP_BANNER_SEEN
import com.android.healthconnect.controller.shared.Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN
import com.android.healthconnect.controller.shared.Constants.USER_ACTIVITY_TRACKER
import com.android.healthconnect.controller.shared.app.AppMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val loadFitnessPermissionApps: ILoadFitnessPermissionAppsUseCase,
    private val loadOnboardingStateUseCase: ILoadOnboardingStateUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    private val _connectedApps = MutableLiveData<OnboardingFragmentState>()
    val connectedApps: LiveData<OnboardingFragmentState>
        get() = _connectedApps

    private val _internalOnboardingBannerState = MutableLiveData<OnboardingState>()

    private val _onboardingBannerState =
        MediatorLiveData<OnboardingBannerState>().apply {
            addSource(_internalOnboardingBannerState) { internalOnboardingState ->
                postValue(getOnboardingBannerState(internalOnboardingState, _connectedApps.value))
            }
            addSource(_connectedApps) { connectedApps ->
                postValue(
                    getOnboardingBannerState(_internalOnboardingBannerState.value, connectedApps)
                )
            }
        }
    val onboardingBannerState: LiveData<OnboardingBannerState>
        get() = _onboardingBannerState

    private val sharedPreferences =
        context.getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)

    private fun getOnboardingBannerState(
        internalOnboardingState: OnboardingState?,
        connectedApps: OnboardingFragmentState?,
    ): OnboardingBannerState {
        if (internalOnboardingState == null) {
            return OnboardingBannerState.NoOnboardingBanner
        }
        if (internalOnboardingState == OnboardingState.ONBOARDING_BANNER_STATE_HIDE) {
            return OnboardingBannerState.NoOnboardingBanner
        }
        if (connectedApps == null) {
            return OnboardingBannerState.NoOnboardingBanner
        }
        if (connectedApps !is OnboardingFragmentState.WithData) {
            return OnboardingBannerState.NoOnboardingBanner
        }
        val bannerSeen = onboardingBannerSeen(internalOnboardingState)
        if (bannerSeen) {
            return OnboardingBannerState.NoOnboardingBanner
        }

        val connectedAppsCount = connectedApps.connectedApps.count { it.isConnected }
        return if (connectedAppsCount == 0) {
            OnboardingBannerState.ZeroAppsOnboardingBanner
        } else if (connectedAppsCount == 1) {
            val connectedApp = connectedApps.connectedApps.filter { it.isConnected }[0].appMetadata
            OnboardingBannerState.OneAppOnboardingBanner(connectedApp)
        } else {
            OnboardingBannerState.NoOnboardingBanner
        }
    }

    init {
        loadConnectedApps()
    }

    fun loadConnectedApps() {
        _connectedApps.postValue(OnboardingFragmentState.Loading)

        viewModelScope.launch {
            // TODO (b/376085888) handle error from useCase
            val connectedFitnessApps = loadFitnessPermissionApps.invoke()
            _connectedApps.postValue(OnboardingFragmentState.WithData(connectedFitnessApps))
        }
    }

    fun loadOnboardingBannerState() {
        viewModelScope.launch {
            _internalOnboardingBannerState.postValue(loadOnboardingStateUseCase.invoke())
        }
    }

    private fun onboardingBannerSeen(state: OnboardingState): Boolean {
        return when (state) {
            OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED ->
                sharedPreferences.getBoolean(ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED ->
                sharedPreferences.getBoolean(ONBOARDING_ONE_APP_BANNER_SEEN, false)
            else -> false
        }
    }

    sealed class OnboardingFragmentState {
        object Loading : OnboardingFragmentState()

        object Error : OnboardingFragmentState()

        data class WithData(val connectedApps: List<ConnectedFitnessAppMetadata>) :
            OnboardingFragmentState()
    }

    sealed class OnboardingBannerState {
        object ZeroAppsOnboardingBanner : OnboardingBannerState()

        class OneAppOnboardingBanner(val connectedApp: AppMetadata) : OnboardingBannerState()

        object NoOnboardingBanner : OnboardingBannerState()
    }
}
