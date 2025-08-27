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
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MatchmakingViewModel
@Inject
constructor(
    private val getMatchingAppsUseCase: GetMatchingAppsUseCase,
    private val recordMatchmakingDenialUseCase: RecordMatchmakingDenialUseCase,
    private val appInfoReader: AppInfoReader,
    private val savedStateHandle: SavedStateHandle,
    private val healthPermissionManager: HealthPermissionManager,
) : ViewModel() {

    companion object {
        private const val EXPANDED_PREFERENCE_KEYS = "expanded_preference_keys"
    }

    private val _matchmakingState = MutableLiveData<MatchmakingState>()
    val matchmakingState: LiveData<MatchmakingState>
        get() = _matchmakingState

    private val _grantedPermissions =
        MutableLiveData<Map<String, List<FitnessPermission>>>(emptyMap())
    val grantedPermissions: LiveData<Map<String, List<FitnessPermission>>>
        get() = _grantedPermissions

    val atLeastOnePermissionGranted = MutableLiveData(false)
    val allPermissionsGranted = MutableLiveData(false)

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
                    val sortedApps =
                        result.data
                            .map { appData ->
                                appData.copy(
                                    permissions =
                                        appData.permissions.sortedBy {
                                            FitnessPermissionStrings.fromPermissionType(
                                                    it.fitnessPermissionType
                                                )
                                                .uppercaseLabel
                                        }
                                )
                            }
                            .sortedBy { it.metadata.appName }
                    _matchmakingState.postValue(MatchmakingState.WithData(appMetadata, sortedApps))
                }
                is UseCaseResults.Failed -> {
                    _matchmakingState.postValue(MatchmakingState.LoadingFailed)
                }
            }
        }
    }

    fun addPermissionToGrantedList(packageName: String, permission: FitnessPermission) {
        val currentPermissions = _grantedPermissions.value?.toMutableMap() ?: mutableMapOf()
        val appPermissions = currentPermissions[packageName]?.toMutableList() ?: mutableListOf()
        appPermissions.add(permission)
        currentPermissions[packageName] = appPermissions
        _grantedPermissions.value = currentPermissions
        atLeastOnePermissionGranted.value = true
        updateAllPermissionsGrantedStatus()
    }

    fun removePermissionFromGrantedList(packageName: String, permission: FitnessPermission) {
        val currentPermissions = _grantedPermissions.value?.toMutableMap() ?: mutableMapOf()
        val appPermissions = currentPermissions[packageName]?.toMutableList() ?: mutableListOf()
        appPermissions.remove(permission)
        if (appPermissions.isEmpty()) {
            currentPermissions.remove(packageName)
        } else {
            currentPermissions[packageName] = appPermissions
        }
        _grantedPermissions.value = currentPermissions
        atLeastOnePermissionGranted.value =
            _grantedPermissions.value?.values?.any { it.isNotEmpty() }
        updateAllPermissionsGrantedStatus()
    }

    fun addAllPermissionsToGrantedList() {
        val allPermissions =
            (matchmakingState.value as? MatchmakingState.WithData)?.matchingApps?.associate {
                it.metadata.packageName to it.permissions
            }
        _grantedPermissions.value = allPermissions ?: emptyMap()
        atLeastOnePermissionGranted.value = allPermissions?.isNotEmpty()
        updateAllPermissionsGrantedStatus()
    }

    fun removeAllPermissionsFromGrantedList() {
        _grantedPermissions.value = emptyMap()
        atLeastOnePermissionGranted.value = false
        updateAllPermissionsGrantedStatus()
    }

    private fun updateAllPermissionsGrantedStatus() {
        val grantedMap = _grantedPermissions.value ?: emptyMap()
        val allApps = (matchmakingState.value as? MatchmakingState.WithData)?.matchingApps
        val allPermissionsMap =
            allApps?.associate { it.metadata.packageName to it.permissions } ?: emptyMap()

        if (allPermissionsMap.isEmpty() || grantedMap.keys != allPermissionsMap.keys) {
            allPermissionsGranted.value = false
            return
        }

        val allGranted =
            allPermissionsMap.all { (packageName, allPerms) ->
                val grantedPerms = grantedMap[packageName]
                grantedPerms?.toSet() == allPerms.toSet()
            }

        allPermissionsGranted.value = allGranted
    }

    fun grantPermissions() {
        viewModelScope.launch {
            _grantedPermissions.value?.forEach { (packageName, permissions) ->
                permissions.forEach { permission ->
                    healthPermissionManager.grantHealthPermission(
                        packageName,
                        permission.toString(),
                    )
                }
            }
            _grantedPermissions.postValue(emptyMap())
            atLeastOnePermissionGranted.postValue(false)
        }
    }

    fun recordMatchmakingDenial() {
        val state = matchmakingState.value
        if (state is MatchmakingState.WithData) {
            val permissions =
                state.matchingApps.flatMap { it.permissions }.map { it.toString() }.distinct()
            recordMatchmakingDenial(state.callingAppMetaData.packageName, permissions)
        }
    }

    private fun recordMatchmakingDenial(packageName: String, permissions: List<String>) {
        viewModelScope.launch {
            recordMatchmakingDenialUseCase.invoke(
                RecordMatchmakingDenialInput(packageName, permissions)
            )
        }
    }

    sealed class MatchmakingState {
        object Loading : MatchmakingState()

        object LoadingFailed : MatchmakingState()

        data class WithData(
            val callingAppMetaData: AppMetadata,
            val matchingApps: List<MatchmakingAppData>,
        ) : MatchmakingState()
    }
}
