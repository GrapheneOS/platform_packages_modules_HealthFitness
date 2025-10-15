/**
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.viewmodels

import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.testapps.toolbox.seed.SeedData
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.isMatchmakingPossible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragmentViewModel : ViewModel() {

    private val _seedAllDataState = MutableLiveData<SeedAllDataState>()
    val seedAllDataState: LiveData<SeedAllDataState>
        get() = _seedAllDataState

    private val _isMatchmakingPossible = MutableLiveData<Boolean>(false)
    val isMatchmakingPossible: LiveData<Boolean>
        get() = _isMatchmakingPossible

    fun seedAllDataViewModel(context: Context, manager: HealthConnectManager) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { SeedData(context, manager).seedAllData() }
                _seedAllDataState.postValue(SeedAllDataState.Success)
            } catch (ex: Exception) {
                _seedAllDataState.postValue(SeedAllDataState.Error(ex.localizedMessage!!))
            }
        }
    }

    fun loadMatchmakingStatus(manager: HealthConnectManager) {
        viewModelScope.launch {
            try {
                val isMatchmakingPossible = isMatchmakingPossible(manager)
                _isMatchmakingPossible.postValue(isMatchmakingPossible)
            } catch (ex: Exception) {
                _isMatchmakingPossible.postValue(false)
                Log.e("MATCHMAKING", "isMatchmakingPossible failed with $ex")
            }
        }
    }

    fun createMatchmakingIntent(manager: HealthConnectManager): Intent {
        return manager.createMatchmakingIntent(emptySet())
    }

    sealed class SeedAllDataState {
        data class Error(val errorMessage: String) : SeedAllDataState()

        object Success : SeedAllDataState()
    }
}
