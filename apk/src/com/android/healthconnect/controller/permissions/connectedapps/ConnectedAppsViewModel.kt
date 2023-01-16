/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.permissions.connectedapps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.connectedapps.searchapps.SearchHealthPermissionApps
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectedAppsViewModel
@Inject
constructor(
    private val loadHealthPermissionApps: LoadHealthPermissionApps,
    private val searchHealthPermissionApps: SearchHealthPermissionApps,
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase
) : ViewModel() {

    private val _connectedApps = MutableLiveData<List<ConnectedAppMetadata>>()
    val connectedApps: LiveData<List<ConnectedAppMetadata>>
        get() = _connectedApps

    init {
        loadConnectedApps()
    }

    fun loadConnectedApps() {
        viewModelScope.launch { _connectedApps.postValue(loadHealthPermissionApps.invoke()) }
    }

    fun searchConnectedApps(searchValue: String) {
        viewModelScope.launch {
            _connectedApps.postValue(
                searchHealthPermissionApps.search(loadHealthPermissionApps.invoke(), searchValue))
        }
    }

    fun disconnectAllApps(apps: List<ConnectedAppMetadata>) {
        viewModelScope.launch {
            apps.forEach { app ->
                revokeAllHealthPermissionsUseCase.invoke(app.appMetadata.packageName)
            }
            loadConnectedApps()
        }
    }
}
