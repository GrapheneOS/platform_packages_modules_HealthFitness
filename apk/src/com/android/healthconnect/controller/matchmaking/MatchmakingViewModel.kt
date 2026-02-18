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

import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING
import android.health.connect.HealthConnectManager.EXTRA_DEVICE_ID
import android.health.connect.HealthConnectManager.RESULT_DEVICE_ONBOARDING_ABORTED
import android.health.connect.HealthConnectManager.RESULT_DEVICE_ONBOARDING_ALLOWED
import android.health.connect.HealthConnectManager.RESULT_DEVICE_ONBOARDING_DENIED
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Record
import androidx.annotation.VisibleForTesting
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
import com.android.healthconnect.controller.utils.toDeviceTypeString
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import com.android.healthfitness.flags.Flags.deviceDataProvidersUiMatchmakingScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MatchmakingViewModel
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
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

    val hasSelectedDevice = MutableLiveData(false)
    val hasSelectedApp = MutableLiveData(false)

    @get:VisibleForTesting val ddpIntentQueue = MutableLiveData<List<Intent>>(emptyList())

    private val _ddpOnboardingState = MutableLiveData<DdpOnboardingState>(DdpOnboardingState.Setup)
    val ddpOnboardingState: LiveData<DdpOnboardingState>
        get() = _ddpOnboardingState

    private var _atLeastOneGrantSucceeded = false

    val expandedPreferenceKeys: MutableLiveData<Set<String>> =
        savedStateHandle.getLiveData(EXPANDED_PREFERENCE_KEYS, emptySet())

    val matchingAppsCount = MutableLiveData(0)

    fun consumeDdpOnboardingEvent() {
        _ddpOnboardingState.value = DdpOnboardingState.Setup
    }

    fun updateExpandedPreferenceKey(key: String, isExpanded: Boolean) {
        val currentKeys = expandedPreferenceKeys.value.orEmpty().toMutableSet()
        if (isExpanded) {
            currentKeys.add(key)
        } else {
            currentKeys.remove(key)
        }
        savedStateHandle[EXPANDED_PREFERENCE_KEYS] = currentKeys
    }

    fun loadMatchmakingData(
        packageName: String,
        recordTypeNames: Array<String>?,
        includedDataSources: Array<String>? = null,
        excludedDataSources: Array<String>? = null,
    ) {
        if (_matchmakingState.value is MatchmakingState.WithData) {
            return
        }

        viewModelScope.launch {
            _matchmakingState.value = MatchmakingState.Loading
            val recordTypes = parseRecordTypeNames(recordTypeNames)
            val includedOrigins =
                includedDataSources
                    ?.map { DataOrigin.Builder().setPackageName(it).build() }
                    ?.toSet() ?: emptySet()
            val excludedOrigins =
                excludedDataSources
                    ?.map { DataOrigin.Builder().setPackageName(it).build() }
                    ?.toSet() ?: emptySet()

            when (
                val result =
                    getMatchingDataSourcesUseCase.invoke(
                        GetMatchingDataSourcesInput(
                            packageName,
                            recordTypes,
                            includedOrigins,
                            excludedOrigins,
                        )
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

                    val matchingDevices =
                        result.data.matchingDevices.sortedBy {
                            it.deviceDataSourceInfo.device.displayName
                                ?: it.deviceDataSourceInfo.device.type.toDeviceTypeString(context)
                        }
                    if (grantedPermissions.value == null) {
                        grantedPermissions.value = emptyMap()
                    }
                    if (enabledDevicePackages.value == null) {
                        enabledDevicePackages.value = emptySet()
                    }
                    atLeastOnePermissionGranted.value =
                        grantedPermissions.value?.isNotEmpty() == true ||
                            enabledDevicePackages.value?.isNotEmpty() == true
                    updateAllPermissionsGrantedStatus()
                    matchingAppsCount.value = sortedApps.size
                    _matchmakingState.value =
                        MatchmakingState.WithData(appMetadata, sortedApps, matchingDevices)
                }
                is UseCaseResults.Failed -> {
                    _matchmakingState.value = MatchmakingState.LoadingFailed
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
        hasSelectedApp.value = true
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
            grantedPermissions.value?.isNotEmpty() == true ||
                enabledDevicePackages.value?.isNotEmpty() == true
        hasSelectedApp.value = grantedPermissions.value?.isNotEmpty() == true
        updateAllPermissionsGrantedStatus()
    }

    fun addAllPermissionsToGrantedList(packageName: String) {
        val state = (_matchmakingState.value as? MatchmakingState.WithData)
        val allPermissionsForApp =
            state?.matchingApps?.firstOrNull { it.metadata.packageName == packageName }?.permissions

        val currentPermissions = grantedPermissions.value?.toMutableMap() ?: mutableMapOf()
        if (allPermissionsForApp != null) {
            currentPermissions[packageName] = allPermissionsForApp
            hasSelectedApp.value = true
        } else {
            val isDevice =
                state?.matchingDevices?.any {
                    it.deviceDataSourceInfo.deviceDataOrigin.packageName == packageName
                } ?: false
            if (isDevice) {
                addDevicePermissionToGrantedList(packageName)
                return
            } else {
                return
            }
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
        hasSelectedDevice.value = true
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
        hasSelectedApp.value = grantedPermissions.value?.isNotEmpty() == true
        hasSelectedDevice.value = enabledDevicePackages.value?.isNotEmpty() == true
        updateAllPermissionsGrantedStatus()
    }

    private fun updateAllPermissionsGrantedStatus() {
        val grantedMap = grantedPermissions.value ?: emptyMap()
        val state = _matchmakingState.value as? MatchmakingState.WithData
        if (state == null) {
            allPermissionsGranted.value = false
            return
        }
        val allApps = state.matchingApps
        val allDevices = state.matchingDevices
        val allAppPermissionsMap = allApps.associate { it.metadata.packageName to it.permissions }
        val allGrantedApps =
            allAppPermissionsMap.all { (packageName, allPerms) ->
                val grantedPerms = grantedMap[packageName]
                grantedPerms?.toSet() == allPerms.toSet()
            }
        val allGrantedDevices =
            allDevices.all { deviceData ->
                enabledDevicePackages.value?.contains(
                    deviceData.deviceDataSourceInfo.deviceDataOrigin.packageName
                ) == true
            }
        allPermissionsGranted.value = allGrantedApps && allGrantedDevices
    }

    fun grantPermissions() {
        viewModelScope.launch {
            _atLeastOneGrantSucceeded = false
            val grantedPermissionsSnapshot = grantedPermissions.value ?: emptyMap()
            val enabledDevicePackagesSnapshot = enabledDevicePackages.value ?: emptySet()
            recordDenialForUngrantedPermissions(
                grantedPermissionsSnapshot,
                enabledDevicePackagesSnapshot,
            )
            grantAppPermissions(grantedPermissionsSnapshot)
            if (deviceDataProvidersApi() && deviceDataProvidersUiMatchmakingScreen()) {
                grantDevicePermissions(enabledDevicePackagesSnapshot)
            } else {
                _ddpOnboardingState.value =
                    DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            }
        }
    }

    private suspend fun grantAppPermissions(
        grantedPermissionsMap: Map<String, List<FitnessPermission>>
    ) {
        if (grantedPermissionsMap.isNotEmpty()) {
            _atLeastOneGrantSucceeded = true
        }
        grantedPermissionsMap.forEach { (packageName, permissions) ->
            permissions.forEach { permission ->
                healthPermissionManager.grantHealthPermission(packageName, permission.toString())
            }
        }
        grantedPermissions.value = emptyMap()
        atLeastOnePermissionGranted.value = false
        hasSelectedApp.value = false
    }

    private fun grantDevicePermissions(enabledDevicePackagesSnapshot: Set<String>) {
        if (enabledDevicePackagesSnapshot.isEmpty()) {
            _ddpOnboardingState.value = DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            return
        }
        val state = (_matchmakingState.value as? MatchmakingState.WithData)
        if (state == null) {
            _ddpOnboardingState.value = DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            return
        }
        val intents =
            state.matchingDevices
                .filter {
                    enabledDevicePackagesSnapshot.contains(
                        it.deviceDataSourceInfo.deviceDataOrigin.packageName
                    )
                }
                .flatMap { deviceData ->
                    deviceData.deviceDataSourceInfo.deviceDataProviderInfos.map { providerInfo ->
                        Intent(ACTION_SHOW_DEVICE_ONBOARDING)
                            .setPackage(providerInfo.packageName)
                            .putExtra(EXTRA_DEVICE_ID, providerInfo.deviceId)
                    }
                }
        if (intents.isNotEmpty()) {
            ddpIntentQueue.value = intents
            _ddpOnboardingState.value = DdpOnboardingState.Onboarding(intents.first())
        } else {
            _ddpOnboardingState.value = DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
        }
    }

    fun onDdpIntentFinished(resultCode: Int) {
        val currentQueue = ddpIntentQueue.value.orEmpty().toMutableList()
        val intentToFinish = currentQueue.firstOrNull()
        val ddpPackageName = intentToFinish?.getPackage()
        when (resultCode) {
            android.app.Activity.RESULT_OK,
            RESULT_DEVICE_ONBOARDING_ALLOWED -> {
                _atLeastOneGrantSucceeded = true
                currentQueue.removeFirstOrNull()
                ddpIntentQueue.value = currentQueue
                val nextIntent = currentQueue.firstOrNull()
                if (nextIntent == null) {
                    enabledDevicePackages.value = emptySet()
                    hasSelectedDevice.value = false
                    _ddpOnboardingState.value =
                        DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
                } else {
                    _ddpOnboardingState.value = DdpOnboardingState.Onboarding(nextIntent)
                }
            }
            RESULT_DEVICE_ONBOARDING_ABORTED -> {
                recordDeviceDenial(ddpPackageName)
                ddpIntentQueue.value = emptyList()
                enabledDevicePackages.value = emptySet()
                hasSelectedDevice.value = false
                if (_atLeastOneGrantSucceeded) {
                    _ddpOnboardingState.value =
                        DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
                } else {
                    _ddpOnboardingState.value =
                        DdpOnboardingState.Finished(android.app.Activity.RESULT_CANCELED)
                }
            }
            RESULT_DEVICE_ONBOARDING_DENIED -> {
                recordDeviceDenial(ddpPackageName)
                currentQueue.removeFirstOrNull()
                ddpIntentQueue.value = currentQueue
                val nextIntent = currentQueue.firstOrNull()
                if (nextIntent == null) {
                    enabledDevicePackages.value = emptySet()
                    hasSelectedDevice.value = false
                    if (_atLeastOneGrantSucceeded) {
                        _ddpOnboardingState.value =
                            DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
                    } else {
                        _ddpOnboardingState.value =
                            DdpOnboardingState.Finished(android.app.Activity.RESULT_CANCELED)
                    }
                } else {
                    _ddpOnboardingState.value = DdpOnboardingState.Onboarding(nextIntent)
                }
            }
            else -> {
                currentQueue.removeFirstOrNull()
                ddpIntentQueue.value = currentQueue
                val nextIntent = currentQueue.firstOrNull()
                if (nextIntent == null) {
                    enabledDevicePackages.value = emptySet()
                    hasSelectedDevice.value = false
                    if (_atLeastOneGrantSucceeded) {
                        _ddpOnboardingState.value =
                            DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
                    } else {
                        _ddpOnboardingState.value =
                            DdpOnboardingState.Finished(android.app.Activity.RESULT_CANCELED)
                    }
                } else {
                    _ddpOnboardingState.value = DdpOnboardingState.Onboarding(nextIntent)
                }
            }
        }
    }

    private fun recordDeviceDenial(ddpPackageName: String?) {
        val state = _matchmakingState.value as? MatchmakingState.WithData ?: return
        val deviceData =
            state.matchingDevices.firstOrNull {
                it.deviceDataSourceInfo.deviceDataProviderInfos.any { provider ->
                    provider.packageName == ddpPackageName
                }
            } ?: return
        val devicePackageName = deviceData.deviceDataSourceInfo.deviceDataOrigin.packageName
        val permissions = deviceData.permissions.map { it.toString() }
        viewModelScope.launch {
            recordMatchmakingDenialUseCase.invoke(
                RecordMatchmakingDenialInput(
                    state.callingAppMetaData.packageName,
                    deniedDataSources = mapOf(devicePackageName to permissions),
                )
            )
        }
    }

    private suspend fun recordDenialForUngrantedPermissions(
        grantedPermissionsSnapshot: Map<String, List<FitnessPermission>> =
            grantedPermissions.value ?: emptyMap(),
        enabledDevicePackagesSnapshot: Set<String> = enabledDevicePackages.value ?: emptySet(),
    ) {
        val state = _matchmakingState.value
        if (state is MatchmakingState.WithData) {
            val allAppPermissionsByPackage =
                state.matchingApps.associate { it.metadata.packageName to it.permissions.toSet() }
            val deniedDataSources =
                allAppPermissionsByPackage.entries
                    .map { (packageName, allPerms) ->
                        val grantedPerms =
                            grantedPermissionsSnapshot[packageName]?.toSet() ?: emptySet()
                        val currentDeniedPerms = allPerms - grantedPerms
                        packageName to currentDeniedPerms.map { it.toString() }
                    }
                    .filter { (_, deniedPerms) -> deniedPerms.isNotEmpty() }
                    .toMap()
                    .toMutableMap()

            // Record denials for unselected devices
            state.matchingDevices.forEach { deviceData ->
                val devicePackage = deviceData.deviceDataSourceInfo.deviceDataOrigin.packageName
                if (!enabledDevicePackagesSnapshot.contains(devicePackage)) {
                    deniedDataSources[devicePackage] = deviceData.permissions.map { it.toString() }
                }
            }

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

    sealed class DdpOnboardingState {
        object Setup : DdpOnboardingState()

        data class Onboarding(val intent: Intent) : DdpOnboardingState()

        data class Finished(val resultCode: Int) : DdpOnboardingState()
    }
}
