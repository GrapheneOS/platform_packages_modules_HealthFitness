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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.connectedapps.searchapps.SearchHealthPermissionApps
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllDataUseCase
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.postValueIfUpdated
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectedAppsViewModel
@Inject
constructor(
    private val loadHealthPermissionApps: ILoadHealthPermissionApps,
    private val searchHealthPermissionApps: SearchHealthPermissionApps,
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    companion object {
        private const val TAG = "ConnectedAppsViewModel"
    }

    private val _connectedApps = MutableLiveData<List<ConnectedAppMetadata>>()
    val connectedApps: LiveData<List<ConnectedAppMetadata>>
        get() = _connectedApps

    private val _disconnectAllState =
        MutableLiveData<DisconnectAllState>(DisconnectAllState.NotStarted)
    val disconnectAllState: LiveData<DisconnectAllState>
        get() = _disconnectAllState

    private val _alertDialogActive = MutableLiveData(false)
    val alertDialogActive: LiveData<Boolean>
        get() = _alertDialogActive

    private val _alertDialogCheckBoxChecked = MutableLiveData(false)
    val alertDialogCheckBoxChecked: LiveData<Boolean>
        get() = _alertDialogCheckBoxChecked

    private val _showSystemApps = MutableLiveData(false)
    val showSystemApps: LiveData<Boolean>
        get() = _showSystemApps

    init {
        loadConnectedApps()
    }

    fun setAlertDialogStatus(isActive: Boolean) {
        _alertDialogActive.postValue(isActive)
    }

    fun setAlertDialogCheckBoxChecked(isChecked: Boolean) {
        _alertDialogCheckBoxChecked.postValue(isChecked)
    }

    fun setShowSystemApps(showSystem: Boolean) {
        _showSystemApps.postValue(showSystem)
        loadConnectedApps()
    }

    fun loadConnectedApps() {
        viewModelScope.launch {
            when (val res = loadHealthPermissionApps.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    _connectedApps.postValueIfUpdated(res.data.filterUnwantedApps())
                }
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Error loading connected apps", res.exception)
                    _connectedApps.postValueIfUpdated(emptyList())
                }
            }
        }
    }

    fun searchConnectedApps(searchValue: String) {
        viewModelScope.launch {
            when (val res = loadHealthPermissionApps.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    _connectedApps.postValueIfUpdated(
                        searchHealthPermissionApps.search(
                            res.data.filterUnwantedApps(),
                            searchValue,
                        )
                    )
                }
                is UseCaseResults.Failed -> {
                    _connectedApps.postValueIfUpdated(emptyList())
                }
            }
        }
    }

    fun disconnectAllApps(apps: List<ConnectedAppMetadata>): Boolean {
        try {
            viewModelScope.launch(ioDispatcher) {
                _disconnectAllState.postValue(DisconnectAllState.Loading)
                apps.forEach { app ->
                    revokeAllHealthPermissionsUseCase.invoke(app.appMetadata.packageName)
                }
                loadConnectedApps()
                _disconnectAllState.postValue(DisconnectAllState.Updated)
            }
            _alertDialogActive.postValue(false)
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to update permissions!", ex)
        }
        _alertDialogActive.postValue(false)
        return false
    }

    fun deleteAllData() {
        viewModelScope.launch { deleteAllDataUseCase.invoke() }
    }

    private fun List<ConnectedAppMetadata>.filterUnwantedApps(): List<ConnectedAppMetadata> {
        return this.filterSystemApps().filterDevices()
    }

    private fun List<ConnectedAppMetadata>.filterSystemApps(): List<ConnectedAppMetadata> {
        val showSystemAppsValue = _showSystemApps.value ?: false
        return if (showSystemAppsValue) {
            this
        } else {
            this.filter { !it.isSystem }
        }
    }

    private fun List<ConnectedAppMetadata>.filterDevices(): List<ConnectedAppMetadata> {
        return this.filterNot { it.appMetadata.packageName == DEVICE_DATA_PROVIDER_PACKAGE }
    }

    sealed class DisconnectAllState {
        object NotStarted : DisconnectAllState()

        object Loading : DisconnectAllState()

        object Updated : DisconnectAllState()
    }
}
