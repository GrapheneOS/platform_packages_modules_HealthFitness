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

import android.content.Context
import android.health.connect.accesslog.AccessLog
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_IN_BACKGROUND
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper.getPermissionType
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WearConnectedAppsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val loadHealthPermissionApps: ILoadHealthPermissionApps,
    private val loadAppPermissionsStatusUseCase: ILoadAppPermissionsStatusUseCase,
    private val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase,
    private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase,
    private val loadRecentAccessUseCase: ILoadRecentAccessUseCase,
    private val healthPermissionReader: HealthPermissionReader,
) : ViewModel() {

    /** A list of [WearHealthAppData] representing wear apps with Health permissions. */
    val wearHealthApps = MutableStateFlow<List<WearHealthAppData>>(emptyList())

    /** A list of [HealthPermission] that are at system level (not restricted to HC-only). */
    val systemHealthPermissions = MutableStateFlow<List<HealthPermission>>(emptyList())

    /** A state flow of whether user chooses to show system apps, which by default is not-show. */
    val showSystemFlow = MutableStateFlow(false)

    init {
        loadConnectedApps()
    }

    fun loadConnectedApps() {
        viewModelScope.launch {
            systemHealthPermissions.value =
                healthPermissionReader.getSystemHealthPermissions().map { perm ->
                    fromPermissionString(perm)
                }
            loadWearHealthApps()
            sortSystemHealthPermissions()
        }
    }

    private suspend fun loadWearHealthApps() {
        val newConnectedAppsInternal = mutableListOf<WearHealthAppData>()
        // Last 24 hours of access, sorted in descending order of time (most recent first)
        val allAccessLogs: List<AccessLog> =
            when (val loadAccessLogsResult = loadRecentAccessUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> loadAccessLogsResult.data
                else -> {
                    Log.e(TAG, "Error loading recent access logs ")
                    emptyList()
                }
            }.filter {
                it.operationType == AccessLog.OperationType.OPERATION_TYPE_READ &&
                    it.recordTypes.size == 1
            }

        // Display only valid permissions for Wear
        val validPermissions = systemHealthPermissions.value + READ_HEALTH_DATA_IN_BACKGROUND
        val validFitnessPermissionTypes =
            systemHealthPermissions.value
                .filterIsInstance<HealthPermission.FitnessPermission>()
                .map { it.fitnessPermissionType }
        val connectedApps =
            when (val res = loadHealthPermissionApps.invoke(Unit)) {
                is UseCaseResults.Success -> res.data
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Error loading connected apps", res.exception)
                    emptyList()
                }
            }
        connectedApps.forEach { connectedAppMetadata ->
            val packageName = connectedAppMetadata.appMetadata.packageName
            val healthPermissionStatus =
                loadAppPermissionsStatusUseCase.invoke(packageName).filter {
                    it.healthPermission in validPermissions
                }

            // get last access log for this app
            val healthPermissionTypesAccessLogs =
                allAccessLogs
                    .filter { it.packageName == packageName }
                    .map { it -> getPermissionType(it.recordTypes[0]) to it.accessTime }
                    .filter { (permissionType, _) -> permissionType in validFitnessPermissionTypes }
                    // group by recordType (Int)
                    .groupBy { it.first }
                    // pairs = (recordType to access times)
                    .mapValues { (_, pairs) -> pairs.maxOfOrNull { it.second } ?: Instant.EPOCH }
                    .filter { (permissionType, _) -> permissionType != null }
                    .map { (permissionType, mostRecentAccessTime) ->
                        PermissionsLastAccess(permissionType!!, mostRecentAccessTime)
                    }
                    .toList()

            newConnectedAppsInternal.add(
                WearHealthAppData(
                    packageName = packageName,
                    appMetadata = connectedAppMetadata.appMetadata,
                    isSystem = connectedAppMetadata.isSystem,
                    healthPermissionStatus = healthPermissionStatus,
                    accessLogs = healthPermissionTypesAccessLogs,
                    lastAccessTime =
                        healthPermissionTypesAccessLogs.maxOfOrNull { it.lastAccessTime },
                )
            )
        }

        wearHealthApps.value = newConnectedAppsInternal
    }

    private fun sortSystemHealthPermissions() {
        val nonSystemApps = wearHealthApps.value.filterNot { it.isSystem }
        systemHealthPermissions.value =
            systemHealthPermissions.value.sortedWith(
                compareBy<HealthPermission> { healthPermission ->
                        if (nonSystemApps.isPermissionRequested(healthPermission)) {
                            0
                        } else {
                            1
                        }
                    }
                    .thenBy { healthPermission ->
                        // For all health permissions that are requested by at least one app,
                        // sort by user-visible strings alphabetically.
                        context.getString(
                            FitnessPermissionStrings.fromPermissionType(
                                    (healthPermission as HealthPermission.FitnessPermission)
                                        .fitnessPermissionType
                                )
                                .uppercaseLabel
                        )
                    }
            )
    }

    fun updateShowSystem(showSystem: Boolean) {
        showSystemFlow.compareAndSet(!showSystem, showSystem)
    }

    /** Grant or revoke a specific permission for an app. */
    fun updatePermission(permission: HealthPermission, appMetadata: AppMetadata, grant: Boolean) {
        if (grant) {
            grantPermissionsStatusUseCase.invoke(appMetadata.packageName, permission.toString())
        } else {
            revokeHealthPermissionUseCase.invoke(appMetadata.packageName, permission.toString())
            val healthApp = wearHealthApps.value.firstOrNull { it.appMetadata == appMetadata }

            // If this was the last granted permission, also remove BG read
            if (healthApp?.shouldRevokeBackgroundReadAlongWith(permission) == true) {
                revokeHealthPermissionUseCase.invoke(
                    appMetadata.packageName,
                    READ_HEALTH_DATA_IN_BACKGROUND.toString(),
                )
            }
        }
        viewModelScope.launch { loadConnectedApps() }
    }

    /** Removes all non-system apps from accessing a specific fitness permission. */
    fun removeFitnessPermissionForAllApps(permission: HealthPermission) {
        val permissionStr = permission.toString()

        wearHealthApps.value
            .filterNot { it.isSystem }
            .forEach { wearHealthApp ->
                revokeHealthPermissionUseCase.invoke(
                    wearHealthApp.appMetadata.packageName,
                    permissionStr,
                )
                if (wearHealthApp.shouldRevokeBackgroundReadAlongWith(permission)) {
                    revokeHealthPermissionUseCase.invoke(
                        wearHealthApp.appMetadata.packageName,
                        READ_HEALTH_DATA_IN_BACKGROUND.toString(),
                    )
                }
            }

        viewModelScope.launch { loadConnectedApps() }
    }

    companion object {
        private const val TAG = "WearConnectedAppsViewModel"
    }
}
