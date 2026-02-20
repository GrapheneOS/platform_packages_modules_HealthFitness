/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.healthconnect.controller.data.access.api

import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.isDevicePackage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Use case to load a map of [com.android.healthconnect.controller.data.access.AppAccessState] to a
 * list of [com.android.healthconnect.controller.data.access.AppAccessMetadata].
 */
@Singleton
class LoadAccessUseCase
@Inject
constructor(
    private val loadFitnessTypeContributorAppsUseCase: ILoadFitnessTypeContributorAppsUseCase,
    private val loadMedicalTypeContributorAppsUseCase: ILoadMedicalTypeContributorAppsUseCase,
    private val loadGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
    private val healthPermissionReader: HealthPermissionReader,
    private val appInfoReader: AppInfoReader,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<HealthPermissionType, Map<AppAccessState, List<AppAccessMetadata>>>(dispatcher),
    ILoadAccessUseCase {

    override suspend fun execute(
        input: HealthPermissionType
    ): Map<AppAccessState, List<AppAccessMetadata>> {
        val appsWithHealthPermissions: List<String> = appWithPermissions(input)
        val contributingApps: List<AppMetadata> = contributingApps(input)

        val readAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()
        val writeAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()
        val readOrWriteAppPackageNameSet: MutableSet<String> = mutableSetOf()
        val inactiveAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()

        appsWithHealthPermissions.forEach {
            val permissionsPerPackage: List<String> = loadGrantedHealthPermissionsUseCase(it)
            val appPermissionsType = healthPermissionReader.getAppPermissionsType(it)
            val appAccessMetadata =
                AppAccessMetadata(appInfoReader.getAppMetadata(it), appPermissionsType)

            // Apps that can READ the given healthPermissionType.
            if (canRead(input, permissionsPerPackage)) {
                readAppMetadataSet.add(appAccessMetadata)
                readOrWriteAppPackageNameSet.add(it)
            }
            // Apps that can WRITE the given healthPermissionType.
            if (canWrite(input, permissionsPerPackage)) {
                writeAppMetadataSet.add(appAccessMetadata)
                readOrWriteAppPackageNameSet.add(it)
            }
        }
        // Apps that are inactive: can no longer READ or WRITE, but still have data in
        // Health Connect. Excludes devices, as permissions are irrelevant to them.
        // However, devices are seen as inactive if all their providers have disabled all of
        // their data types.
        // TODO(b/478259450): Check disabled devices
        contributingApps.forEach { app ->
            if (
                !readOrWriteAppPackageNameSet.contains(app.packageName) &&
                    !isDevicePackage(app.packageName)
            ) {
                // Inactive apps don't navigate to appInfoScreen hence no need to specify
                // appPermissionsType.
                val appAccessMetadata = AppAccessMetadata(appMetadata = app)
                inactiveAppMetadataSet.add(appAccessMetadata)
            }
        }

        return mapOf(
            AppAccessState.Read to alphabeticallySortedMetadataList(readAppMetadataSet),
            AppAccessState.Write to alphabeticallySortedMetadataList(writeAppMetadataSet),
            AppAccessState.Inactive to alphabeticallySortedMetadataList(inactiveAppMetadataSet),
        )
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
                loadFitnessTypeContributorAppsUseCase.invoke(healthPermissionType).let { result ->
                    (result as UseCaseResults.Success).data
                }
            is MedicalPermissionType ->
                loadMedicalTypeContributorAppsUseCase.invoke(healthPermissionType).let { result ->
                    (result as UseCaseResults.Success).data
                }
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
                    HealthPermission.FitnessPermission(
                            healthPermissionType,
                            PermissionsAccessType.READ,
                        )
                        .toString()
                )
            is MedicalPermissionType ->
                permissionsPerPackage.contains(
                    HealthPermission.MedicalPermission(healthPermissionType).toString()
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
                    HealthPermission.FitnessPermission(
                            healthPermissionType,
                            PermissionsAccessType.WRITE,
                        )
                        .toString()
                )
            is MedicalPermissionType ->
                permissionsPerPackage.contains(
                    HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
                        .toString()
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

interface ILoadAccessUseCase :
    UseCaseContract<HealthPermissionType, Map<AppAccessState, List<AppAccessMetadata>>>
