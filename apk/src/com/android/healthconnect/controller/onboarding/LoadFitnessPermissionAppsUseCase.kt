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

package com.android.healthconnect.controller.onboarding

import android.content.Context
import android.content.pm.PackageManager
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Loads all apps which have at least one fitness permission and whether at least one fitness
 * permission is allowed.
 */
@Singleton
class LoadFitnessPermissionAppsUseCase
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val healthPermissionReader: HealthPermissionReader,
    private val loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase,
    private val appInfoReader: AppInfoReader,
    private val getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<Unit, List<ConnectedFitnessAppMetadata>>(dispatcher) {

    override suspend fun execute(unit: Unit): List<ConnectedFitnessAppMetadata> {
        val appsWithHealthPermissions = healthPermissionReader.getAppsWithHealthPermissions()
        val connectedApps = mutableListOf<ConnectedFitnessAppMetadata>()

        connectedApps.addAll(
            appsWithHealthPermissions
                .filterNot { it.value }
                .keys
                .mapNotNull { packageName ->
                    val metadata = appInfoReader.getAppMetadata(packageName)

                    val healthPermissionsList = loadAppPermissionsStatusUseCase.invoke(packageName)
                    val fitnessPermissions =
                        healthPermissionsList
                            .map { it.healthPermission }
                            .filterIsInstance<HealthPermission.FitnessPermission>()

                    if (fitnessPermissions.isEmpty()) {
                        // not a fitness app
                        return@mapNotNull null
                    }

                    val grantedFitnessPermissions =
                        healthPermissionsList
                            .filter { it.isGranted }
                            .map { it.healthPermission }
                            .filterIsInstance<HealthPermission.FitnessPermission>()

                    val isConnected = grantedFitnessPermissions.isNotEmpty()
                    if (!isConnected) {
                        // Check if any permission has been denied
                        val permissionFlags =
                            getHealthPermissionsFlagsUseCase.invoke(
                                packageName,
                                fitnessPermissions.map { it.toString() }.toList(),
                            )

                        val userSetOrFixedPermissions =
                            permissionFlags
                                .filter { (_, flags) ->
                                    flags.and(PackageManager.FLAG_PERMISSION_USER_FIXED) != 0 ||
                                        flags.and(PackageManager.FLAG_PERMISSION_USER_SET) != 0
                                }
                                .keys
                                .toList()

                        if (userSetOrFixedPermissions.isNotEmpty()) {
                            return@mapNotNull null
                        }
                    }

                    val onboardingIntent =
                        healthPermissionReader.getOnboardingActivityIntent(context, packageName)
                    if (onboardingIntent != null) {
                        ConnectedFitnessAppMetadata(metadata, isConnected, true)
                    } else {
                        ConnectedFitnessAppMetadata(metadata, isConnected, false)
                    }
                }
        )
        return connectedApps.sortedWith(
            // TODO (b/416744614) additional sorting criteria for apps
            // Show connected apps first
            compareBy<ConnectedFitnessAppMetadata> { if (it.isConnected) 0 else 1 }
                .thenBy { it.appMetadata.appName }
        )
    }
}

data class ConnectedFitnessAppMetadata(
    val appMetadata: AppMetadata,
    var isConnected: Boolean,
    val hasOnboarding: Boolean = false,
)
