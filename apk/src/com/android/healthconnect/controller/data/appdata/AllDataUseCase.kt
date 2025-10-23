/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.data.appdata

import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SymptomRecord
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.data.entries.api.SymptomTypeMapper
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.fromHealthPermissionCategory
import com.android.healthconnect.controller.permissions.data.fromMedicalResourceType
import com.android.healthconnect.controller.shared.FITNESS_DATA_CATEGORIES
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.healthPermissionTypes
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class AllDataUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    // A map of SymptomType IntDef to its corresponding FitnessPermissionType
    @VisibleForTesting
    val symptomTypeToPermissionMap by lazy {
        if (Flags.symptoms()) {
            FitnessPermissionType.values()
                .filter { it.name.startsWith("SYMPTOM_") }
                .associateBy { SymptomTypeMapper.getSymptomType(it.category) }
        } else {
            emptyMap()
        }
    }

    /** Returns list of all fitness categories and permission types to be shown on the HC UI. */
    suspend fun loadAllFitnessData(): UseCaseResults<List<PermissionTypesPerCategory>> =
        withContext(dispatcher) {
            try {
                val recordTypeInfoMap = getRecordTypeInfoMap()
                val categories =
                    FITNESS_DATA_CATEGORIES.map {
                        PermissionTypesPerCategory(
                            it,
                            getPermissionTypesPerCategory(it, recordTypeInfoMap, packageName = null),
                        )
                    }
                UseCaseResults.Success(categories)
            } catch (e: Exception) {
                Log.e("TAG_ERROR", "Loading error ", e)
                UseCaseResults.Failed(e)
            }
        }

    /** Returns list of all medical permission types to be shown on the HC UI. */
    suspend fun loadAllMedicalData(): UseCaseResults<List<PermissionTypesPerCategory>> =
        withContext(dispatcher) {
            try {
                val medicalResourceTypeInfos = getMedicalResourceTypeInfos()
                val medicalPermissionTypes =
                    medicalResourceTypeInfos
                        .filter { it.contributingDataSources.isNotEmpty() }
                        .map { fromMedicalResourceType(it.medicalResourceType) }
                if (medicalPermissionTypes.isEmpty()) {
                    UseCaseResults.Success(listOf())
                } else {
                    UseCaseResults.Success(
                        listOf(PermissionTypesPerCategory(MEDICAL, medicalPermissionTypes))
                    )
                }
            } catch (e: Exception) {
                Log.e("TAG_ERROR", "Loading error ", e)
                UseCaseResults.Failed(e)
            }
        }

    /** Returns whether there is any fitness data in HC. */
    suspend fun loadHasAnyFitnessData(): UseCaseResults<Boolean> =
        withContext(dispatcher) {
            try {
                val recordTypeInfoMap = getRecordTypeInfoMap()
                val anyFitnessData =
                    recordTypeInfoMap.any { it.value.contributingPackages.isNotEmpty() }
                UseCaseResults.Success(anyFitnessData)
            } catch (e: Exception) {
                Log.e("TAG_ERROR", "Loading error ", e)
                UseCaseResults.Failed(e)
            }
        }

    /** Returns whether there is any medical data in HC. */
    suspend fun loadHasAnyMedicalData(): UseCaseResults<Boolean> =
        withContext(dispatcher) {
            try {
                val medicalResourceTypeInfos = getMedicalResourceTypeInfos()
                val anyMedicalData =
                    medicalResourceTypeInfos.any { it.contributingDataSources.isNotEmpty() }
                UseCaseResults.Success(anyMedicalData)
            } catch (e: Exception) {
                Log.e("TAG_ERROR", "Loading error ", e)
                UseCaseResults.Failed(e)
            }
        }

    /**
     * Returns list of fitness categories and permission types written by the given app to be shown
     * on the HC UI.
     */
    suspend fun loadFitnessAppData(
        packageName: String
    ): UseCaseResults<List<PermissionTypesPerCategory>> =
        withContext(dispatcher) {
            try {
                val recordTypeInfoMap = getRecordTypeInfoMap()
                val categories =
                    FITNESS_DATA_CATEGORIES.map {
                        PermissionTypesPerCategory(
                            it,
                            getPermissionTypesPerCategory(it, recordTypeInfoMap, packageName),
                        )
                    }
                UseCaseResults.Success(categories)
            } catch (e: Exception) {
                UseCaseResults.Failed(e)
            }
        }

    /**
     * Returns list of medical categories and permission types written by the given app to be shown
     * on the HC UI.
     */
    suspend fun loadMedicalAppData(
        packageName: String
    ): UseCaseResults<List<PermissionTypesPerCategory>> =
        withContext(dispatcher) {
            try {
                val medicalResourceTypeInfos = getMedicalResourceTypeInfos()
                val medicalPermissionTypes =
                    filterMedicalPermissionTypes(medicalResourceTypeInfos, packageName)
                if (medicalPermissionTypes.isEmpty()) {
                    UseCaseResults.Success(listOf())
                } else {
                    UseCaseResults.Success(
                        listOf(PermissionTypesPerCategory(MEDICAL, medicalPermissionTypes))
                    )
                }
            } catch (e: Exception) {
                UseCaseResults.Failed(e)
            }
        }

    private suspend fun getRecordTypeInfoMap(): Map<Class<out Record>, RecordTypeInfoResponse> {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.queryAllRecordTypesInfo(
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        return recordTypeInfoMap
    }

    private suspend fun getMedicalResourceTypeInfos() =
        suspendCancellableCoroutine { continuation ->
            healthConnectManager.queryAllMedicalResourceTypeInfos(
                Runnable::run,
                continuation.asOutcomeReceiver(),
            )
        }

    private fun filterMedicalPermissionTypes(
        medicalResourceTypeInfos: List<MedicalResourceTypeInfo>,
        packageName: String,
    ): List<MedicalPermissionType> =
        medicalResourceTypeInfos
            .filter { medicalResourceTypeInfo ->
                val contributingPackageNames =
                    medicalResourceTypeInfo.contributingDataSources.map { it.packageName }.toSet()
                contributingPackageNames.contains(packageName)
            }
            .map { fromMedicalResourceType(it.medicalResourceType) }

    /**
     * Returns those [HealthPermissionType]s that have some data written by the given [packageName]
     * app. If the is no app provided then return all data.
     */
    private suspend fun getPermissionTypesPerCategory(
        category: @HealthDataCategoryInt Int,
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
        packageName: String?,
    ): List<HealthPermissionType> {
        if (Flags.symptoms() && category == HealthDataCategory.SYMPTOMS) {
            return getSymptomPermissionTypesWithData(packageName)
        }
        val types = category.healthPermissionTypes()
        if (packageName == null) {
            return types.filter { hasData(it, recordTypeInfoMap) }
        }
        return types.filter { hasDataByApp(it, recordTypeInfoMap, packageName) }
    }

    private suspend fun getSymptomPermissionTypesWithData(
        packageName: String? = null
    ): List<HealthPermissionType> {
        val requestBuilder =
            ReadRecordsRequestUsingFilters.Builder(SymptomRecord::class.java)
                .setTimeRangeFilter(
                    TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build()
                )
        if (packageName != null) {
            requestBuilder.addDataOrigins(DataOrigin.Builder().setPackageName(packageName).build())
        }
        val request = requestBuilder.build()
        try {
            val records =
                suspendCancellableCoroutine<ReadRecordsResponse<SymptomRecord>> { continuation ->
                    healthConnectManager.readRecords(
                        request,
                        Runnable::run,
                        continuation.asOutcomeReceiver(),
                    )
                }
            val symptomTypesWithData = records.records.map { it.symptomType }.toSet()

            return symptomTypesWithData.mapNotNull { symptomType ->
                symptomTypeToPermissionMap[symptomType]
            }
        } catch (e: Exception) {
            Log.e("AllDataUseCase", "Error reading symptom data", e)
            return emptyList()
        }
    }

    private fun hasData(
        permissionType: HealthPermissionType,
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
    ): Boolean =
        recordTypeInfoMap.values.firstOrNull {
            fromHealthPermissionCategory(it.permissionCategory) == permissionType &&
                it.contributingPackages.isNotEmpty()
        } != null

    private fun hasDataByApp(
        permissionType: HealthPermissionType,
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
        packageName: String,
    ): Boolean =
        recordTypeInfoMap.values.firstOrNull {
            fromHealthPermissionCategory(it.permissionCategory) == permissionType &&
                it.contributingPackages.isNotEmpty() &&
                it.contributingPackages
                    .map { contributingApp -> contributingApp.packageName }
                    .contains(packageName)
        } != null
}

/**
 * Represents Health Category group to be shown in health connect screens.
 *
 * @param category Category id
 * @param data [HealthPermissionType]s within the category that have data written by given app.
 */
data class PermissionTypesPerCategory(
    val category: @HealthDataCategoryInt Int,
    val data: List<HealthPermissionType>,
)
