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
package com.android.healthconnect.controller.categories

import android.healthconnect.HealthConnectManager
import android.healthconnect.RecordTypeInfoResponse
import android.healthconnect.datatypes.Record
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HEALTH_DATA_CATEGORIES
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LoadCategoriesWithDataUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    /** Returns list of available data categories. */
    suspend operator fun invoke(): List<Int> =
        withContext(dispatcher) {
            try {
                val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
                    suspendCancellableCoroutine { continuation ->
                        healthConnectManager.queryAllRecordTypesInfo(
                            Runnable::run, continuation.asOutcomeReceiver())
                    }
                HEALTH_DATA_CATEGORIES.filter { hasData(it, recordTypeInfoMap) }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun hasData(
        category: Int,
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>
    ): Boolean =
        recordTypeInfoMap.values.firstOrNull {
            it.dataCategory == category && it.contributingPackages.isNotEmpty()
        } != null
}

@Singleton
class LoadCategoriesUseCase
@Inject
constructor(private val categoriesUseCase: LoadCategoriesWithDataUseCase) {
    /** Returns list of data categories that have data. */
    suspend fun invoke(): List<Int> = categoriesUseCase()
}

@Singleton
class LoadAllCategoriesUseCase
@Inject
constructor(private val categoriesUseCase: LoadCategoriesWithDataUseCase) {
    /** Returns list of all available data categories. */
    suspend fun invoke(): List<AllCategoriesScreenHealthDataCategory> {
        val categoriesWithData = categoriesUseCase()
        return HEALTH_DATA_CATEGORIES.map { category ->
            AllCategoriesScreenHealthDataCategory(category, category !in categoriesWithData)
        }
    }
}

/** Represents Category group for HealthConnect data in All Categories screen. */
data class AllCategoriesScreenHealthDataCategory(val category: Int, val noData: Boolean)
