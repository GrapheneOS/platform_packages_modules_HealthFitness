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
package com.android.healthconnect.controller.permissions.connectedapps.wear

import android.health.connect.accesslog.AccessLog
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_IN_BACKGROUND
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.recentaccess.LoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.collections.MutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WearConnectedAppsViewModel
@Inject
constructor(
    private val loadHealthPermissionApps: ILoadHealthPermissionApps,
    private val loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase,
    private val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase,
    private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase,
    private val loadRecentAccessUseCase: LoadRecentAccessUseCase,
    private val healthPermissionReader: HealthPermissionReader,
) : ViewModel() {

    /** A list of [AppMetadata] of all the apps that requests health permissions. */
    val connectedApps = MutableStateFlow<List<ConnectedAppMetadata>>(emptyList())

    /** Mapping from [HealthPermission] to a list of [AppMetadata] of the allowed apps. */
    val dataTypeToAllowedApps =
        MutableStateFlow<Map<HealthPermission, MutableList<AppMetadata>>>(emptyMap())

    /** Mapping from [HealthPermission] to a list of [AppMetadata] of the denied apps. */
    val dataTypeToDeniedApps =
        MutableStateFlow<Map<HealthPermission, MutableList<AppMetadata>>>(emptyMap())

    /**
     * Mapping from [AppMetadata] of the all connected apps to a boolean representing whether
     * background permission is granted.
     */
    val appToBackgroundReadStatus = MutableStateFlow<Map<AppMetadata, Boolean>>(emptyMap())

    /**
     * Mapping from [AppMetadata] of the all connected apps to a list of all the allowed
     * [HealthPermission].
     */
    val appToAllowedDataTypes =
        MutableStateFlow<Map<AppMetadata, MutableList<HealthPermission>>>(emptyMap())

    /**
     * Holds the current state of health data access permissions for connected apps.
     *
     * This emits a list of [PermissionAccess] objects, where each object represents a health
     * permission and the apps that have accessed it, along with their last access times. Consumers
     * can observe this to stay updated on changes to app access for health permissions.
     */
    val dataTypeToAppToLastAccessTime = MutableStateFlow<List<PermissionAccess>>(emptyList())

    /** A list of [HealthPermission] that are at system level (not restricted to HC-only). */
    val systemHealthPermissions = MutableStateFlow<List<HealthPermission>>(emptyList())

    /** A state flow of whether user chooses to show system apps, which by default is not-show. */
    val showSystemFlow = MutableStateFlow(false)

    init {
        loadConnectedApps()
    }

    fun loadConnectedApps() {
        viewModelScope.launch {
            connectedApps.value = loadHealthPermissionApps.invoke()
            systemHealthPermissions.value =
                healthPermissionReader.getSystemHealthPermissions().map { perm ->
                    fromPermissionString(perm)
                }

            loadDataTypeToAppsMapping()
            loadRecentAccessMapping()
        }
    }

    fun updateShowSystem(showSystem: Boolean) {
        showSystemFlow.compareAndSet(!showSystem, showSystem)
    }

    /** Load system health permissions and granular permission to allowed and denied apps maps. */
    private suspend fun loadDataTypeToAppsMapping() {
        // Init dataTypeToAllowedApps and dataTypeToDeniedApps.
        // For each granular health permission, create a mapping of the allowed and denied apps.
        val allowedAppsMap = mutableMapOf<HealthPermission, MutableList<AppMetadata>>()
        val deniedAppsMap = mutableMapOf<HealthPermission, MutableList<AppMetadata>>()
        val backgroundReadPermissionStatus = mutableMapOf<AppMetadata, Boolean>()
        val allowedDataTypesMap = mutableMapOf<AppMetadata, MutableList<HealthPermission>>()

        connectedApps.value.forEach { connectedAppMetadata ->
            val packageName = connectedAppMetadata.appMetadata.packageName
            val healthPermissionStatus = loadAppPermissionsStatusUseCase.invoke(packageName)
            healthPermissionStatus
                .filter { systemHealthPermissions.value.contains(it.healthPermission) }
                .forEach { status ->
                    val permission = status.healthPermission
                    val appList = if (status.isGranted) allowedAppsMap else deniedAppsMap
                    appList
                        .getOrPut(permission) { mutableListOf() }
                        .add(connectedAppMetadata.appMetadata)
                    if (status.isGranted) {
                        allowedDataTypesMap
                            .getOrPut(connectedAppMetadata.appMetadata) { mutableListOf() }
                            .add(permission)
                    }
                }
            healthPermissionStatus
                .firstOrNull { it.healthPermission == READ_HEALTH_DATA_IN_BACKGROUND }
                ?.let {
                    backgroundReadPermissionStatus[connectedAppMetadata.appMetadata] = it.isGranted
                }
        }

        dataTypeToAllowedApps.value = allowedAppsMap
        dataTypeToDeniedApps.value = deniedAppsMap
        appToBackgroundReadStatus.value = backgroundReadPermissionStatus
        appToAllowedDataTypes.value = allowedDataTypesMap
    }

    /** Load app recent usage access logs. */
    private suspend fun loadRecentAccessMapping() {
        val permissionAccesses = mutableListOf<PermissionAccess>()

        val allAccessLogs: List<AccessLog> =
            when (val loadAccessLogsResult = loadRecentAccessUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> loadAccessLogsResult.data
                else -> {
                    Log.e(TAG, "Error loading recent access logs ")
                    emptyList()
                }
            }

        allAccessLogs.forEach { log: AccessLog ->
            val appMetadata =
                connectedApps.value
                    .firstOrNull { connectedApp ->
                        connectedApp.appMetadata.packageName == log.packageName
                    }
                    ?.appMetadata
            // Wear access logs are converted from app ops, and each log from app ops contains only
            // one record type.
            if (
                appMetadata != null &&
                    log.operationType == AccessLog.OperationType.OPERATION_TYPE_READ &&
                    log.recordTypes.size == 1
            ) {
                val healthPermissionType =
                    HealthPermissionToDatatypeMapper.getAllDataTypes()
                        .filterValues { it.contains(log.recordTypes[0]) }
                        .keys
                        .firstOrNull()
                // TODO: Dynamic mapping for scalability.
                when (healthPermissionType) {
                    FitnessPermissionType.HEART_RATE,
                    FitnessPermissionType.SKIN_TEMPERATURE,
                    FitnessPermissionType.OXYGEN_SATURATION -> {
                        val healthPermission =
                            HealthPermission.FitnessPermission(
                                healthPermissionType,
                                PermissionsAccessType.READ,
                            )
                        val permissionAccess =
                            getPermissionAccessForHealthPermission(
                                permissionAccesses,
                                healthPermission,
                            )
                        val appAccess =
                            getAppAccessForApp(permissionAccess, appMetadata, log.accessTime)

                        if (appAccess.lastAccessTime.isBefore(log.accessTime)) {
                            // Update the lastAccessTime if the current log has a later time
                            permissionAccess.appAccesses =
                                permissionAccess.appAccesses.map { appAccessRecord ->
                                    if (appAccessRecord.app == appAccess.app) {
                                        appAccessRecord.copy(lastAccessTime = log.accessTime)
                                    } else {
                                        appAccessRecord
                                    }
                                }
                        } else if (appAccess !in permissionAccess.appAccesses) {
                            // Add the new AppAccess if it doesn't exist
                            permissionAccess.appAccesses = permissionAccess.appAccesses + appAccess
                        }

                        // Update the permissionAccesses list
                        if (permissionAccess !in permissionAccesses) {
                            permissionAccesses.add(permissionAccess)
                        }
                    }
                    else -> {} // Do nothing
                }
            }
        }

        dataTypeToAppToLastAccessTime.value = permissionAccesses
    }

    /** Grant or revoke a specific permission for an app. */
    fun updatePermission(permission: HealthPermission, appMetadata: AppMetadata, grant: Boolean) {
        if (grant) {
            grantPermissionsStatusUseCase.invoke(appMetadata.packageName, permission.toString())
        } else {
            revokeHealthPermissionUseCase.invoke(appMetadata.packageName, permission.toString())
        }

        // Update app to background status map.
        if (permission == READ_HEALTH_DATA_IN_BACKGROUND) {
            appToBackgroundReadStatus.value =
                appToBackgroundReadStatus.value.toMutableMap().also { it[appMetadata] = grant }
            return
        }

        // Update app to allowed data types map.
        appToAllowedDataTypes.value =
            appToAllowedDataTypes.value.toMutableMap().also { map ->
                if (grant) {
                    map[appMetadata] =
                        (map[appMetadata] ?: mutableListOf()).also {
                            if (permission !in it) it.add(permission)
                        }
                } else {
                    map[appMetadata]?.remove(permission)
                    if (map[appMetadata]?.isEmpty() == true) {
                        map.remove(appMetadata)
                    }
                }
            }

        // Update data type to allowed/denied apps map.
        val mapToAdd =
            if (grant) {
                dataTypeToAllowedApps
            } else {
                dataTypeToDeniedApps
            }
        val mapToRemove =
            if (grant) {
                dataTypeToDeniedApps
            } else {
                dataTypeToAllowedApps
            }
        mapToAdd.value =
            mapToAdd.value.toMutableMap().also {
                it[permission] =
                    (it[permission] ?: mutableListOf()).also { appsList ->
                        if (appsList.none { it.packageName == appMetadata.packageName }) {
                            appsList.add(appMetadata)
                        }
                    }
            }
        mapToRemove.value =
            mapToRemove.value.toMutableMap().also {
                it[permission] =
                    (it[permission] ?: mutableListOf()).also { appsList ->
                        appsList.removeIf { it.packageName == appMetadata.packageName }
                        if (appsList.isEmpty()) {
                            it.remove(permission)
                        }
                    }
            }
    }

    /** Removes all apps from accessing a specific fitness permission. */
    fun removeFitnessPermissionForAllApps(permission: HealthPermission) {
        val permissionStr = permission.toString()

        // Update data type to allowed/denied apps map.
        val deniedAppsMap = dataTypeToDeniedApps.value.toMutableMap()
        dataTypeToAllowedApps.value[permission]?.forEach { appMetadata ->
            revokeHealthPermissionUseCase.invoke(appMetadata.packageName, permissionStr)
            deniedAppsMap.getOrPut(permission) { mutableListOf() }.add(appMetadata)
        }
        dataTypeToAllowedApps.value = dataTypeToAllowedApps.value - permission
        dataTypeToDeniedApps.value = deniedAppsMap

        // Update app to allowed data types map.
        appToAllowedDataTypes.value =
            appToAllowedDataTypes.value.toMutableMap().also { map ->
                map.keys.forEach { appMetadata ->
                    map[appMetadata]?.remove(permission)
                    if (map[appMetadata]?.isEmpty() == true) {
                        map.remove(appMetadata)
                    }
                }
            }
    }

    fun getAppMetadataByPackageName(packageName: String): MutableStateFlow<AppMetadata?> =
        MutableStateFlow<AppMetadata?>(null).also { flow ->
            flow.value =
                connectedApps.value
                    .firstOrNull { it.appMetadata.packageName == packageName }
                    ?.appMetadata
        }

    companion object {
        private const val TAG = "WearConnectedAppsViewModel"

        private fun getPermissionAccessForHealthPermission(
            permissionAccesses: List<PermissionAccess>,
            healthPermission: HealthPermission,
        ): PermissionAccess {
            return permissionAccesses.find { permissionAccessRecord ->
                permissionAccessRecord.permission == healthPermission
            } ?: PermissionAccess(healthPermission, mutableListOf())
        }

        private fun getAppAccessForApp(
            permissionAccess: PermissionAccess,
            appMetadata: AppMetadata,
            firstAccessTimeIfNotFound: Instant,
        ): AppAccess {
            return permissionAccess.appAccesses.find { appAccessRecord ->
                appAccessRecord.app == appMetadata
            } ?: AppAccess(appMetadata, firstAccessTimeIfNotFound)
        }
    }
}
