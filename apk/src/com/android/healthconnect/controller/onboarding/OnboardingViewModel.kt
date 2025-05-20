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
import android.util.Log
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
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
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

    private val appsInteractedWith = mutableSetOf<String>()

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
        if (
            internalOnboardingState == null ||
                internalOnboardingState == OnboardingState.ONBOARDING_BANNER_STATE_HIDE ||
                connectedApps == null ||
                (connectedApps !is OnboardingFragmentState.ZeroAppsConnected &&
                    connectedApps !is OnboardingFragmentState.OneAppConnected &&
                    connectedApps !is OnboardingFragmentState.AlmostDone)
        ) {
            return OnboardingBannerState.NoOnboardingBanner
        }

        val bannerSeen = onboardingBannerSeen(internalOnboardingState)
        if (bannerSeen) {
            return OnboardingBannerState.NoOnboardingBanner
        }

        return if (connectedApps is OnboardingFragmentState.ZeroAppsConnected) {
            OnboardingBannerState.ZeroAppsOnboardingBanner
        } else if (connectedApps is OnboardingFragmentState.OneAppConnected) {
            val connectedApp = connectedApps.connectedApp
            OnboardingBannerState.OneAppOnboardingBanner(connectedApp.appMetadata)
        } else {
            OnboardingBannerState.NoOnboardingBanner
        }
    }

    fun loadConnectedApps() {
        _connectedApps.postValue(OnboardingFragmentState.Loading)

        viewModelScope.launch {
            when (val result = loadFitnessPermissionApps.invoke(Unit)) {
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Error invoking LoadFitnessPermissionApps: " + result.exception)
                    _connectedApps.postValue(OnboardingFragmentState.Error)
                }
                is UseCaseResults.Success -> {
                    val potentialFitnessApps = result.data.toMutableList()

                    if (potentialFitnessApps.isEmpty()) {
                        _connectedApps.postValue(OnboardingFragmentState.NoApps)
                        return@launch
                    }

                    for (currentApp in potentialFitnessApps) {
                        if (currentApp.appMetadata.packageName in appsInteractedWith) {
                            currentApp.isConnected = true
                        }
                    }
                    potentialFitnessApps.sortWith(
                        // TODO (b/416744614) additional sorting criteria for apps
                        // Show connected apps first
                        compareBy<ConnectedFitnessAppMetadata> { if (it.isConnected) 0 else 1 }
                            .thenBy { it.appMetadata.appName }
                    )

                    val allowedApps =
                        potentialFitnessApps
                            .groupBy { it.isConnected }
                            .getOrDefault(true, emptyList())
                    if (allowedApps.isEmpty()) {
                        _connectedApps.postValue(
                            OnboardingFragmentState.ZeroAppsConnected(potentialFitnessApps)
                        )
                    } else if (allowedApps.size == 1) {
                        val connectedApp = potentialFitnessApps.filter { it.isConnected }[0]
                        val potentialApps = potentialFitnessApps.filter { !it.isConnected }
                        _connectedApps.postValue(
                            OnboardingFragmentState.OneAppConnected(connectedApp, potentialApps)
                        )
                    } else {
                        _connectedApps.postValue(OnboardingFragmentState.AlmostDone(allowedApps))
                    }
                }
            }
        }
    }

    fun loadOnboardingBannerState() {
        viewModelScope.launch {
            when (val result = loadOnboardingStateUseCase.invoke(Unit)) {
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Error invoking LoadOnboardingState: " + result.exception)
                    _internalOnboardingBannerState.postValue(
                        OnboardingState.ONBOARDING_BANNER_STATE_HIDE
                    )
                }
                is UseCaseResults.Success -> {
                    _internalOnboardingBannerState.postValue(result.data)
                }
            }
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

    fun setAppInteractedWith(packageName: String) {
        appsInteractedWith.add(packageName)
    }

    sealed class OnboardingFragmentState {
        object Loading : OnboardingFragmentState()

        object Error : OnboardingFragmentState()

        object NoApps : OnboardingFragmentState()

        data class ZeroAppsConnected(val potentialApps: List<ConnectedFitnessAppMetadata>) :
            OnboardingFragmentState()

        data class OneAppConnected(
            val connectedApp: ConnectedFitnessAppMetadata,
            val potentialApps: List<ConnectedFitnessAppMetadata>,
        ) : OnboardingFragmentState()

        data class AlmostDone(val connectedApps: List<ConnectedFitnessAppMetadata>) :
            OnboardingFragmentState()
    }

    sealed class OnboardingBannerState {
        object ZeroAppsOnboardingBanner : OnboardingBannerState()

        class OneAppOnboardingBanner(val connectedApp: AppMetadata) : OnboardingBannerState()

        object NoOnboardingBanner : OnboardingBannerState()
    }
}
