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

import android.content.Context
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthDataCategory
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.matchmaking.api.IGetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.healthPermissionTypes
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.categorizeAndInsertToAppAccessState
import com.android.healthconnect.controller.utils.isDevicePackage
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Use case to load a list of apps that have access to symptom data types.
 *
 * This use case categorizes apps into three states: Read, Write, and Inactive, based on their
 * granted permissions for all symptom types.
 */
@Singleton
class LoadSymptomAccessUseCase
@Inject
constructor(
    private val loadGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
    private val loadSymptomContributorAppsUseCase: ILoadSymptomContributorAppsUseCase,
    private val loadDeviceDataSourcesInfosUseCase: IGetDeviceDataSourcesInfoUseCase,
    private val healthPermissionReader: HealthPermissionReader,
    private val appInfoReader: AppInfoReader,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<Unit, Map<AppAccessState, List<AppAccessMetadata>>>(dispatcher),
    ILoadSymptomAccessUseCase {

    override suspend fun execute(input: Unit): Map<AppAccessState, List<AppAccessMetadata>> {
        val appsWithHealthPermissions: List<String> =
            healthPermissionReader.getAppsWithFitnessPermissions()

        val contributingAppsResult = loadSymptomContributorAppsUseCase.invoke(Unit)
        val contributingApps: List<AppMetadata> =
            when (contributingAppsResult) {
                is UseCaseResults.Success -> {
                    contributingAppsResult.data
                }
                // TODO(b/486156654): Think of and implement alternative error handling
                else -> emptyList<AppMetadata>()
            }

        val readAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()
        val writeAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()
        val readOrWriteAppPackageNameSet: MutableSet<String> = mutableSetOf()
        val inactiveAppMetadataSet: MutableSet<AppAccessMetadata> = mutableSetOf()

        val symptomPermissionTypes =
            HealthDataCategory.SYMPTOMS.healthPermissionTypes()
                .filterIsInstance<FitnessPermissionType>()

        appsWithHealthPermissions.forEach { packageName ->
            val permissionsPerPackage: List<String> =
                loadGrantedHealthPermissionsUseCase(packageName)
            val appPermissionsType = healthPermissionReader.getAppPermissionsType(packageName)
            val appAccessMetadata =
                AppAccessMetadata(appInfoReader.getAppMetadata(packageName), appPermissionsType)

            if (
                symptomPermissionTypes.any { symptomType ->
                    permissionsPerPackage.contains(
                        HealthPermission.FitnessPermission(symptomType, PermissionsAccessType.READ)
                            .toString()
                    )
                }
            ) {
                readAppMetadataSet.add(appAccessMetadata)
                readOrWriteAppPackageNameSet.add(packageName)
            }
            if (
                symptomPermissionTypes.any { symptomType ->
                    permissionsPerPackage.contains(
                        HealthPermission.FitnessPermission(symptomType, PermissionsAccessType.WRITE)
                            .toString()
                    )
                }
            ) {
                writeAppMetadataSet.add(appAccessMetadata)
                readOrWriteAppPackageNameSet.add(packageName)
            }
        }

        contributingApps.forEach { app ->
            if (
                // Apps that are inactive: can no longer READ or WRITE, but still have data in
                // Health Connect.
                !readOrWriteAppPackageNameSet.contains(app.packageName) &&
                    !isDevicePackage(app.packageName)
            ) {
                // Inactive apps don't navigate to appInfoScreen hence no need to specify
                // appPermissionsType.
                val appAccessMetadata = AppAccessMetadata(appMetadata = app)
                inactiveAppMetadataSet.add(appAccessMetadata)
            }
        }

        if (deviceDataProvidersApi()) {
            val deviceSourcesInfo = loadDeviceDataSourcesInfo()

            deviceSourcesInfo.categorizeAndInsertToAppAccessState(
                targetRecordTypes = listOf(SymptomRecord::class.java),
                contributingApps = contributingApps,
                writeAppMetadataSet = writeAppMetadataSet,
                inactiveAppMetadataSet = inactiveAppMetadataSet,
                context = context,
            )
        }

        return mapOf(
            AppAccessState.Read to alphabeticallySortedMetadataList(readAppMetadataSet),
            AppAccessState.Write to alphabeticallySortedMetadataList(writeAppMetadataSet),
            AppAccessState.Inactive to alphabeticallySortedMetadataList(inactiveAppMetadataSet),
        )
    }

    private suspend fun loadDeviceDataSourcesInfo(): Set<DeviceDataSourceInfo> {
        val deviceSources = loadDeviceDataSourcesInfosUseCase.invoke(Unit)
        return if (deviceSources is UseCaseResults.Success) {
            deviceSources.data
        } else {
            // TODO(b/486156654): Think of and implement alternative error handling
            emptySet()
        }
    }

    private fun alphabeticallySortedMetadataList(
        packageNames: Set<AppAccessMetadata>
    ): List<AppAccessMetadata> {
        return packageNames.sortedBy { appAccessMetadata -> appAccessMetadata.appMetadata.appName }
    }
}

interface ILoadSymptomAccessUseCase :
    UseCaseContract<Unit, Map<AppAccessState, List<AppAccessMetadata>>>
