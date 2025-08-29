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

import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.app.IGetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class LoadHealthPermissionApps
@Inject
constructor(
    private val healthPermissionReader: HealthPermissionReader,
    private val loadGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
    private val getContributorAppInfoUseCase: IGetContributorAppInfoUseCase,
    private val queryRecentAccessLogsUseCase: IQueryRecentAccessLogsUseCase,
    private val appInfoReader: AppInfoReader,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadHealthPermissionApps, BaseUseCase<Unit, List<ConnectedAppMetadata>>(dispatcher) {

    companion object {
        private const val TAG = "LoadHealthPermissionApps"
    }

    /** Returns a list of [ConnectedAppMetadata]. */
    override suspend fun execute(input: Unit): List<ConnectedAppMetadata> {
        val appsWithHealthPermissions = healthPermissionReader.getAppsWithHealthPermissions()
        val appsWithData = getContributorAppInfoUseCase.invoke()
        val connectedApps = mutableListOf<ConnectedAppMetadata>()
        val recentAccess = queryRecentAccessLogsUseCase.invoke()
        val appsWithOldHealthPermissions = healthPermissionReader.getAppsWithOldHealthPermissions()

        connectedApps.addAll(
            appsWithHealthPermissions.map { (packageName, isSystem) ->
                val metadata = appInfoReader.getAppMetadata(packageName)
                val grantedPermissions = loadGrantedHealthPermissionsUseCase(packageName)
                val isConnected =
                    if (grantedPermissions.isNotEmpty()) {
                        ConnectedAppStatus.ALLOWED
                    } else {
                        ConnectedAppStatus.DENIED
                    }
                val appPermissionsType = healthPermissionReader.getAppPermissionsType(packageName)
                ConnectedAppMetadata(
                    metadata,
                    isConnected,
                    appPermissionsType,
                    recentAccess[metadata.packageName],
                    isSystem = isSystem,
                )
            }
        )

        val inactiveApps =
            appsWithData.values
                .filter { !appsWithHealthPermissions.contains(it.packageName) }
                .map { ConnectedAppMetadata(it, ConnectedAppStatus.INACTIVE) }

        val appsThatNeedUpdating =
            appsWithOldHealthPermissions
                .map { packageName ->
                    val metadata = appInfoReader.getAppMetadata(packageName)
                    ConnectedAppMetadata(
                        appMetadata = metadata,
                        status = ConnectedAppStatus.NEEDS_UPDATE,
                        healthUsageLastAccess = recentAccess[metadata.packageName],
                    )
                }
                .filter { !appsWithHealthPermissions.contains(it.appMetadata.packageName) }

        connectedApps.addAll(inactiveApps)
        connectedApps.addAll(appsThatNeedUpdating)
        return connectedApps
    }
}

interface ILoadHealthPermissionApps {
    suspend fun invoke(input: Unit): UseCaseResults<List<ConnectedAppMetadata>>

    suspend fun execute(input: Unit): List<ConnectedAppMetadata>
}
