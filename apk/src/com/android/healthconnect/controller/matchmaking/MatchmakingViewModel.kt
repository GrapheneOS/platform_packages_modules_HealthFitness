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

package com.android.healthconnect.controller.matchmaking

import android.health.connect.datatypes.Record
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase.GetMatchMakingAppsInput
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MatchmakingViewModel
@Inject
constructor(
    private val getMatchingAppsUseCase: GetMatchingAppsUseCase,
    private val appInfoReader: AppInfoReader,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val EXPANDED_PREFERENCE_KEYS = "expanded_preference_keys"
    }

    private val _matchmakingState = MutableLiveData<MatchmakingState>()
    val matchmakingState: LiveData<MatchmakingState>
        get() = _matchmakingState

    val expandedPreferenceKeys: MutableLiveData<Set<String>> =
        savedStateHandle.getLiveData(EXPANDED_PREFERENCE_KEYS, emptySet())

    fun updateExpandedPreferenceKey(key: String, isExpanded: Boolean) {
        val currentKeys = expandedPreferenceKeys.value.orEmpty().toMutableSet()
        if (isExpanded) {
            currentKeys.add(key)
        } else {
            currentKeys.remove(key)
        }
        savedStateHandle[EXPANDED_PREFERENCE_KEYS] = currentKeys
    }

    fun loadMatchmakingApps(packageName: String, recordTypes: Set<Class<out Record>>) {
        _matchmakingState.postValue(MatchmakingState.Loading)
        viewModelScope.launch {
            when (
                val result =
                    getMatchingAppsUseCase.invoke(GetMatchMakingAppsInput(packageName, recordTypes))
            ) {
                is UseCaseResults.Success -> {
                    val appMetadata = appInfoReader.getAppMetadata(packageName)
                    _matchmakingState.postValue(
                        MatchmakingState.WithData(appMetadata.appName, result.data)
                    )
                }
                is UseCaseResults.Failed -> {
                    _matchmakingState.postValue(MatchmakingState.LoadingFailed)
                }
            }
        }
    }

    sealed class MatchmakingState {
        object Loading : MatchmakingState()

        object LoadingFailed : MatchmakingState()

        data class WithData(val appName: String, val apps: Set<MatchmakingAppData>) :
            MatchmakingState()
    }
}
