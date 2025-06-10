/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.data.access

import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class LoadAccessUseCase
@Inject
constructor(
    private val loadFitnessTypeContributorAppsUseCase: ILoadFitnessTypeContributorAppsUseCase,
    private val loadMedicalTypeContributorAppsUseCase: ILoadMedicalTypeContributorAppsUseCase,
    private val loadGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
    private val healthPermissionReader: HealthPermissionReader,
    private val appInfoReader: AppInfoReader,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadAccessUseCase {
    /** Returns a map of [AppAccessState] to apps. */
    override suspend operator fun invoke(
        permissionType: HealthPermissionType
    ): UseCaseResults<Map<AppAccessState, List<AppAccessMetadata>>> =
        withContext(dispatcher) {
            try {
                val appsWithHealthPermissions: List<String> = appWithPermissions(permissionType)
                val contributingApps: List<AppMetadata> = contributingApps(permissionType)

                val readAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()
                val writeAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()
                val readOrWriteAppPackageNameSet: MutableSet<String> = mutableSetOf()
                val inactiveAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()

                appsWithHealthPermissions.forEach {
                    val permissionsPerPackage: List<String> =
                        loadGrantedHealthPermissionsUseCase(it)
                    val appPermissionsType = healthPermissionReader.getAppPermissionsType(it)
                    val appAccessMetadata =
                        AppAccessMetadata(appInfoReader.getAppMetadata(it), appPermissionsType)

                    // Apps that can READ the given healthPermissionType.
                    if (canRead(permissionType, permissionsPerPackage)) {
                        readAppMetadataSet.add(appAccessMetadata)
                        readOrWriteAppPackageNameSet.add(it)
                    }
                    // Apps that can WRITE the given healthPermissionType.
                    if (canWrite(permissionType, permissionsPerPackage)) {
                        writeAppMetadataSet.add(appAccessMetadata)
                        readOrWriteAppPackageNameSet.add(it)
                    }
                }
                // Apps that are inactive: can no longer READ or WRITE, but still have data in
                // Health Connect.
                contributingApps.forEach { app ->
                    if (
                        !readOrWriteAppPackageNameSet.contains(app.packageName) &&
                            // Permissions are irrelevant to the device data provider package.
                            app.packageName != Constants.DEVICE_DATA_PROVIDER_PACKAGE
                    ) {
                        // Inactive apps don't navigate to appInfoScreen hence no need to specify
                        // appPermissionsType.
                        val appAccessMetadata = AppAccessMetadata(appMetadata = app)
                        inactiveAppMetadataSet.add(appAccessMetadata)
                    }
                }

                val appAccess =
                    mapOf(
                        AppAccessState.Read to alphabeticallySortedMetadataList(readAppMetadataSet),
                        AppAccessState.Write to
                            alphabeticallySortedMetadataList(writeAppMetadataSet),
                        AppAccessState.Inactive to
                            alphabeticallySortedMetadataList(inactiveAppMetadataSet),
                    )
                UseCaseResults.Success(appAccess)
            } catch (ex: Exception) {
                UseCaseResults.Failed(ex)
            }
        }

    private fun appWithPermissions(healthPermissionType: HealthPermissionType): List<String> {
        return when (healthPermissionType) {
            is FitnessPermissionType -> healthPermissionReader.getAppsWithFitnessPermissions()
            is MedicalPermissionType -> healthPermissionReader.getAppsWithMedicalPermissions()
            else -> throw IllegalArgumentException(exceptionMessage(healthPermissionType))
        }
    }

    private suspend fun contributingApps(
        healthPermissionType: HealthPermissionType
    ): List<AppMetadata> {
        return when (healthPermissionType) {
            is FitnessPermissionType ->
                loadFitnessTypeContributorAppsUseCase.invoke(healthPermissionType)
            is MedicalPermissionType ->
                loadMedicalTypeContributorAppsUseCase.invoke(healthPermissionType)
            else -> throw IllegalArgumentException(exceptionMessage(healthPermissionType))
        }
    }

    private fun canRead(
        healthPermissionType: HealthPermissionType,
        permissionsPerPackage: List<String>,
    ): Boolean {
        return when (healthPermissionType) {
            is FitnessPermissionType ->
                permissionsPerPackage.contains(
                    FitnessPermission(healthPermissionType, PermissionsAccessType.READ).toString()
                )
            is MedicalPermissionType ->
                permissionsPerPackage.contains(
                    MedicalPermission(healthPermissionType).toString()
                ) && healthPermissionType != MedicalPermissionType.ALL_MEDICAL_DATA
            else -> throw IllegalArgumentException(exceptionMessage(healthPermissionType))
        }
    }

    private fun canWrite(
        healthPermissionType: HealthPermissionType,
        permissionsPerPackage: List<String>,
    ): Boolean {
        return when (healthPermissionType) {
            is FitnessPermissionType ->
                permissionsPerPackage.contains(
                    FitnessPermission(healthPermissionType, PermissionsAccessType.WRITE).toString()
                )
            is MedicalPermissionType ->
                permissionsPerPackage.contains(
                    MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA).toString()
                )
            else -> throw IllegalArgumentException(exceptionMessage(healthPermissionType))
        }
    }

    private fun exceptionMessage(healthPermissionType: HealthPermissionType): String =
        "healthPermissionType $healthPermissionType not supported"

    private fun alphabeticallySortedMetadataList(
        packageNames: Set<AppAccessMetadata>
    ): List<AppAccessMetadata> {
        return packageNames.sortedBy { appAccessMetadata -> appAccessMetadata.appMetadata.appName }
    }
}

interface ILoadAccessUseCase {
    suspend fun invoke(
        permissionType: HealthPermissionType
    ): UseCaseResults<Map<AppAccessState, List<AppAccessMetadata>>>
}
