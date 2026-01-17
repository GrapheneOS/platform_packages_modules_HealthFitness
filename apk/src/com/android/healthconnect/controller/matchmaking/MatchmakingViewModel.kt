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
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput
import com.android.healthconnect.controller.matchmaking.api.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.api.MatchmakingDeviceData
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import com.android.healthfitness.flags.Flags.deviceDataProvidersUiMatchmakingScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.collections.filter
import kotlinx.coroutines.launch

@HiltViewModel
class MatchmakingViewModel
@Inject
constructor(
    private val getMatchingDataSourcesUseCase: GetMatchingDataSourcesUseCase,
    private val recordMatchmakingDenialUseCase: RecordMatchmakingDenialUseCase,
    private val appInfoReader: AppInfoReader,
    private val savedStateHandle: SavedStateHandle,
    private val healthPermissionManager: HealthPermissionManager,
) : ViewModel() {

    companion object {
        private const val EXPANDED_PREFERENCE_KEYS = "expanded_preference_keys"
        private const val GRANTED_PERMISSIONS_KEY = "granted_permissions"
        private const val ENABLED_DEVICES_KEY = "enabled_devices"
    }

    private val _matchmakingState = MutableLiveData<MatchmakingState>()
    val matchmakingState: LiveData<MatchmakingState>
        get() = _matchmakingState

    val grantedPermissions: MutableLiveData<Map<String, List<FitnessPermission>>> =
        savedStateHandle.getLiveData(GRANTED_PERMISSIONS_KEY, emptyMap())
    val enabledDevicePackages: MutableLiveData<Set<String>> =
        savedStateHandle.getLiveData(ENABLED_DEVICES_KEY, emptySet())

    val atLeastOnePermissionGranted = MutableLiveData(false)
    val allPermissionsGranted = MutableLiveData(false)

    val expandedPreferenceKeys: MutableLiveData<Set<String>> =
        savedStateHandle.getLiveData(EXPANDED_PREFERENCE_KEYS, emptySet())

    val matchingAppsCount = MutableLiveData(0)

    fun updateExpandedPreferenceKey(key: String, isExpanded: Boolean) {
        val currentKeys = expandedPreferenceKeys.value.orEmpty().toMutableSet()
        if (isExpanded) {
            currentKeys.add(key)
        } else {
            currentKeys.remove(key)
        }
        savedStateHandle[EXPANDED_PREFERENCE_KEYS] = currentKeys
    }

    fun loadMatchmakingData(packageName: String, recordTypeNames: Array<String>?) {
        // Load data only if it has not been loaded before.
        if (_matchmakingState.value is MatchmakingState.WithData) {
            return
        }

        viewModelScope.launch {
            _matchmakingState.postValue(MatchmakingState.Loading)
            val recordTypes = parseRecordTypeNames(recordTypeNames)
            when (
                val result =
                    getMatchingDataSourcesUseCase.invoke(
                        GetMatchingDataSourcesInput(packageName, recordTypes)
                    )
            ) {
                is UseCaseResults.Success -> {
                    val appMetadata = appInfoReader.getAppMetadata(packageName)
                    val sortedApps =
                        result.data.matchingApps
                            .map { appData ->
                                appData.copy(
                                    permissions =
                                        appData.permissions.sortedBy {
                                            it.fitnessPermissionType.toString()
                                        }
                                )
                            }
                            .sortedBy { it.metadata.appName }

                    val matchingDevices = result.data.matchingDevices
                    if (grantedPermissions.value.isNullOrEmpty()) {
                        grantedPermissions.postValue(emptyMap())
                    }
                    if (enabledDevicePackages.value.isNullOrEmpty()) {
                        enabledDevicePackages.postValue(emptySet())
                    }
                    atLeastOnePermissionGranted.postValue(
                        grantedPermissions.value?.isNotEmpty() == true ||
                            enabledDevicePackages.value?.isNotEmpty() == true
                    )
                    updateAllPermissionsGrantedStatus()
                    matchingAppsCount.postValue(sortedApps.size)
                    _matchmakingState.postValue(
                        MatchmakingState.WithData(appMetadata, sortedApps, matchingDevices)
                    )
                }
                is UseCaseResults.Failed -> {
                    _matchmakingState.postValue(MatchmakingState.LoadingFailed)
                }
            }
        }
    }

    fun addAppPermissionToGrantedList(packageName: String, permission: FitnessPermission) {
        val currentPermissions = grantedPermissions.value?.toMutableMap() ?: mutableMapOf()
        val appPermissions = currentPermissions[packageName]?.toMutableList() ?: mutableListOf()
        appPermissions.add(permission)
        currentPermissions[packageName] = appPermissions
        grantedPermissions.value = currentPermissions
        atLeastOnePermissionGranted.value = true
        updateAllPermissionsGrantedStatus()
    }

    fun removePermissionFromGrantedList(packageName: String, permission: FitnessPermission) {
        val currentPermissions = grantedPermissions.value?.toMutableMap() ?: mutableMapOf()
        val appPermissions = currentPermissions[packageName]?.toMutableList() ?: mutableListOf()
        appPermissions.remove(permission)
        if (appPermissions.isEmpty()) {
            currentPermissions.remove(packageName)
        } else {
            currentPermissions[packageName] = appPermissions
        }
        grantedPermissions.value = currentPermissions
        atLeastOnePermissionGranted.value =
            grantedPermissions.value?.values?.any { it.isNotEmpty() }
        updateAllPermissionsGrantedStatus()
    }

    fun addAllPermissionsToGrantedList(packageName: String) {
        val state = (matchmakingState.value as? MatchmakingState.WithData)
        val allPermissionsForApp =
            state?.matchingApps?.firstOrNull { it.metadata.packageName == packageName }?.permissions

        val currentPermissions = grantedPermissions.value?.toMutableMap() ?: mutableMapOf()
        var isDevice = false
        if (state != null) {
            isDevice =
                state.matchingDevices.any {
                    it.deviceDataSourceInfo.deviceDataOrigin.packageName == packageName
                }
        }

        if (allPermissionsForApp != null) {
            currentPermissions[packageName] = allPermissionsForApp
        } else if (isDevice) {
            addDevicePermissionToGrantedList(packageName)
            return
        } else {
            // Not an app and not a device, do nothing.
            return
        }
        grantedPermissions.value = currentPermissions
        atLeastOnePermissionGranted.value = true
        updateAllPermissionsGrantedStatus()
    }

    fun addDevicePermissionToGrantedList(packageName: String) {
        val currentEnabledDevices = enabledDevicePackages.value?.toMutableSet() ?: mutableSetOf()
        currentEnabledDevices.add(packageName)
        enabledDevicePackages.value = currentEnabledDevices
        atLeastOnePermissionGranted.value = true
        updateAllPermissionsGrantedStatus()
    }

    fun removeAllPermissionsFromGrantedList(packageName: String) {
        val currentPermissions = grantedPermissions.value?.toMutableMap() ?: mutableMapOf()
        currentPermissions.remove(packageName)
        grantedPermissions.value = currentPermissions

        val currentEnabledDevices = enabledDevicePackages.value?.toMutableSet() ?: mutableSetOf()
        currentEnabledDevices.remove(packageName)
        enabledDevicePackages.value = currentEnabledDevices

        atLeastOnePermissionGranted.value =
            grantedPermissions.value?.isNotEmpty() == true ||
                enabledDevicePackages.value?.isNotEmpty() == true
        updateAllPermissionsGrantedStatus()
    }

    private fun updateAllPermissionsGrantedStatus() {
        val grantedMap = grantedPermissions.value ?: emptyMap()
        val state = matchmakingState.value as? MatchmakingState.WithData

        if (state == null) {
            allPermissionsGranted.value = false
            return
        }

        val allApps = state.matchingApps
        val allDevices = state.matchingDevices

        val allAppPermissionsMap = allApps.associate { it.metadata.packageName to it.permissions }
        val allDevicePackageNames =
            allDevices.map { it.deviceDataSourceInfo.deviceDataOrigin.packageName }.toSet()

        val allGrantedApps =
            allAppPermissionsMap.all { (packageName, allPerms) ->
                val grantedPerms = grantedMap[packageName]
                grantedPerms?.toSet() == allPerms.toSet()
            }

        val allGrantedDevices =
            allDevicePackageNames.all { packageName ->
                enabledDevicePackages.value?.contains(packageName) == true
            }

        allPermissionsGranted.value = allGrantedApps && allGrantedDevices
    }

    fun grantPermissions() {
        viewModelScope.launch {
            grantedPermissions.value?.forEach { (packageName, permissions) ->
                permissions.forEach { permission ->
                    healthPermissionManager.grantHealthPermission(
                        packageName,
                        permission.toString(),
                    )
                }
            }

            if (deviceDataProvidersApi() && deviceDataProvidersUiMatchmakingScreen()) {
                enabledDevicePackages.value?.forEach { packageName ->
                    // TODO(b/325752113): Implement DDP enabling logic
                }
            }

            recordDenialForUngrantedPermissions()

            grantedPermissions.postValue(emptyMap())
            atLeastOnePermissionGranted.postValue(false)
        }
    }

    private suspend fun recordDenialForUngrantedPermissions() {
        val state = matchmakingState.value
        if (state is MatchmakingState.WithData) {
            val allPermissionsByPackage =
                state.matchingApps.associate { it.metadata.packageName to it.permissions.toSet() }
            val grantedPermissionsByPackage = grantedPermissions.value ?: emptyMap()
            val deniedDataSources =
                allPermissionsByPackage.entries
                    .map { (packageName, allPerms) ->
                        val grantedPerms =
                            grantedPermissionsByPackage[packageName]?.toSet() ?: emptySet()
                        val currentDeniedPerms = allPerms - grantedPerms
                        packageName to currentDeniedPerms.map { it.toString() }
                    }
                    .filter { (_, deniedPerms) -> deniedPerms.isNotEmpty() }
                    .toMap()
                    .toMutableMap()

            if (deniedDataSources.isNotEmpty()) {
                recordMatchmakingDenialUseCase.invoke(
                    RecordMatchmakingDenialInput(
                        state.callingAppMetaData.packageName,
                        deniedDataSources = deniedDataSources,
                    )
                )
            }
        }
    }

    fun recordMatchmakingDenial() {
        viewModelScope.launch { recordDenialForUngrantedPermissions() }
    }

    /**
     * Parses an array of record type names into a set of `Class<out Record>`.
     *
     * @param recordTypeNames An array of class names for `Record` types, or null.
     * @return A set of `Class<out Record>` corresponding to the valid record type names, filtering
     *   out invalid names, or an empty set if `recordTypeNames` is null or empty.
     */
    private fun parseRecordTypeNames(recordTypeNames: Array<String>?): Set<Class<out Record>> {
        return (recordTypeNames ?: emptyArray())
            .mapNotNull {
                try {
                    Class.forName(it)
                } catch (e: ClassNotFoundException) {
                    null
                }
            }
            .filterIsInstance<Class<out Record>>()
            .toSet()
    }

    sealed class MatchmakingState {
        object Loading : MatchmakingState()

        object LoadingFailed : MatchmakingState()

        data class WithData(
            val callingAppMetaData: AppMetadata,
            val matchingApps: List<MatchmakingAppData>,
            val matchingDevices: List<MatchmakingDeviceData>,
        ) : MatchmakingState()
    }
}
