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
package com.android.healthconnect.controller.datasources.api

import android.health.connect.HealthConnectManager
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.healthPermissionTypes
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.LoadPriorityListUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LoadPotentialPriorityListUseCase
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    private val healthConnectManager: HealthConnectManager,
    private val healthPermissionReader: HealthPermissionReader,
    private val loadGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase,
    @LoadPriorityListUseCase
    private val loadPriorityListUseCase: BaseUseCase<@HealthDataCategoryInt Int, List<AppMetadata>>,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<@HealthDataCategoryInt Int, List<AppMetadata>>(dispatcher) {

    private val TAG = "LoadAppSourcesUseCase"

    /** Returns a list of unique [AppMetadata]s that are potential priority list candidates. */
    override suspend fun execute(category: @HealthDataCategoryInt Int): List<AppMetadata> {
        val appsWithDataResult = getAppsWithData(category)
        val appsWithWritePermissionResult = getAppsWithWritePermission(category)
        val appsOnPriorityListResult = loadPriorityListUseCase.invoke(category)

        // Propagate error if any calls fail
        if (appsWithDataResult is UseCaseResults.Failed) {
            throw appsWithDataResult.exception
        } else if (appsWithWritePermissionResult is UseCaseResults.Failed) {
            throw appsWithWritePermissionResult.exception
        } else if (appsOnPriorityListResult is UseCaseResults.Failed) {
            throw appsOnPriorityListResult.exception
        } else {
            val appsWithData = (appsWithDataResult as UseCaseResults.Success).data
            val appsWithWritePermission =
                (appsWithWritePermissionResult as UseCaseResults.Success).data
            val appsOnPriorityList =
                (appsOnPriorityListResult as UseCaseResults.Success)
                    .data
                    .map { it.packageName }
                    .toSet()

            val potentialPriorityListApps =
                appsWithData.union(appsWithWritePermission).minus(appsOnPriorityList).toList().map {
                    appInfoReader.getAppMetadata(it)
                }

            return potentialPriorityListApps
        }
    }

    /** Returns a list of unique packageNames that have data in this [HealthDataCategory]. */
    @VisibleForTesting
    suspend fun getAppsWithData(category: @HealthDataCategoryInt Int): UseCaseResults<Set<String>> =
        withContext(dispatcher) {
            try {
                val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.queryAllRecordTypesInfo(
                            Runnable::run,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                val packages =
                    recordTypeInfoMap.values
                        .filter {
                            it.contributingPackages.isNotEmpty() && it.dataCategory == category
                        }
                        .map { it.contributingPackages }
                        .flatten()
                UseCaseResults.Success(packages.map { it.packageName }.toSet())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get apps with data ", e)
                UseCaseResults.Failed(e)
            }
        }

    /**
     * Returns a set of packageNames which have at least one WRITE permission in this
     * [HealthDataCategory] *
     */
    @VisibleForTesting
    suspend fun getAppsWithWritePermission(
        category: @HealthDataCategoryInt Int
    ): UseCaseResults<Set<String>> =
        withContext(dispatcher) {
            try {
                val writeAppPackageNameSet: MutableSet<String> = mutableSetOf()
                val appsWithFitnessPermissions: List<String> =
                    healthPermissionReader.getAppsWithFitnessPermissions()
                val fitnessPermissionsInCategory: List<String> =
                    category
                        .healthPermissionTypes()
                        .filterIsInstance<FitnessPermissionType>()
                        .map { healthPermissionType ->
                            FitnessPermission(healthPermissionType, PermissionsAccessType.WRITE)
                                .toString()
                        }

                appsWithFitnessPermissions.forEach { packageName ->
                    val permissionsPerPackage: List<String> =
                        loadGrantedHealthPermissionsUseCase(packageName)

                    // Apps that can WRITE the given HealthDataCategory
                    if (fitnessPermissionsInCategory.any { permissionsPerPackage.contains(it) }) {
                        writeAppPackageNameSet.add(packageName)
                    }
                }

                UseCaseResults.Success(writeAppPackageNameSet)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get apps with write permission ", e)
                UseCaseResults.Failed(e)
            }
        }
}
