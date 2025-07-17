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

import android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION
import android.health.connect.datatypes.Record
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase.GetMatchMakingAppsInput
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MatchMakingViewModel
@Inject
constructor(private val getMatchingAppsUseCase: GetMatchingAppsUseCase) : ViewModel() {

    private val _matchmakingState = MutableLiveData<MatchMakingState>()
    val matchmakingState: LiveData<MatchMakingState>
        get() = _matchmakingState

    @androidx.annotation.RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    fun loadMatchmakingApps(packageName: String, recordTypes: Set<Class<out Record>>) {
        _matchmakingState.postValue(MatchMakingState.Loading)
        viewModelScope.launch {
            when (
                val result =
                    getMatchingAppsUseCase.invoke(GetMatchMakingAppsInput(packageName, recordTypes))
            ) {
                is UseCaseResults.Success -> {
                    _matchmakingState.postValue(MatchMakingState.WithData(result.data))
                }
                is UseCaseResults.Failed -> {
                    _matchmakingState.postValue(MatchMakingState.LoadingFailed)
                }
            }
        }
    }

    sealed class MatchMakingState {
        object Loading : MatchMakingState()

        object LoadingFailed : MatchMakingState()

        data class WithData(val apps: Map<AppMetadata, Set<HealthPermission>>) : MatchMakingState()
    }
}
