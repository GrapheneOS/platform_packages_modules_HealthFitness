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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(private val loadFitnessPermissionApps: ILoadFitnessPermissionAppsUseCase) :
    ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    private val _connectedApps = MutableLiveData<OnboardingFragmentState>()
    val connectedApps: LiveData<OnboardingFragmentState>
        get() = _connectedApps

    init {
        loadConnectedApps()
    }

    fun loadConnectedApps() {
        _connectedApps.postValue(OnboardingFragmentState.Loading)

        viewModelScope.launch {
            val connectedFitnessApps = loadFitnessPermissionApps.invoke()
            _connectedApps.postValue(OnboardingFragmentState.WithData(connectedFitnessApps))
        }
    }

    sealed class OnboardingFragmentState {
        object Loading : OnboardingFragmentState()

        object Error : OnboardingFragmentState()

        data class WithData(val connectedApps: List<ConnectedFitnessAppMetadata>) :
            OnboardingFragmentState()
    }
}
