/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.healthconnect.controller.permissions.connectedapps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.shared.AppMetadata
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectedAppsViewModel
@Inject
constructor(private val loadAllowedAppsUseCase: LoadAllowedAppsUseCase, private val loadNotAllowedAppsUseCase: LoadNotAllowedAppsUseCase) : ViewModel() {

    private val _allowedApps = MutableLiveData<List<AppMetadata>>()
    private val _notAllowedApps = MutableLiveData<List<AppMetadata>>()

    val allowedApps: LiveData<List<AppMetadata>>
        get() = _allowedApps
    val notAllowedApps: LiveData<List<AppMetadata>>
        get() = _notAllowedApps

    init {
        loadAllowedApps()
        loadNotAllowedApps()
    }

    private fun loadAllowedApps() {
        viewModelScope.launch { _allowedApps.postValue(loadAllowedAppsUseCase.invoke()) }
    }

    private fun loadNotAllowedApps() {
        viewModelScope.launch { _notAllowedApps.postValue(loadNotAllowedAppsUseCase.invoke()) }
    }
}
