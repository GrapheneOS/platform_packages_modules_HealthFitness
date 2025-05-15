/*
 * Copyright (C) 2024 The Android Open Source Project
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
 *
 *
 */
package com.android.healthconnect.controller.shared.app

import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.MedicalDataSource
import android.util.Log
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Reads MedicalDataSource from the DB. */
@Singleton
class MedicalDataSourceReader
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val TAG = "MedicalDataSourceReader"
    }

    suspend fun fromPackageName(packageName: String): List<MedicalDataSource> =
        withContext(dispatcher) {
            val request = GetMedicalDataSourcesRequest.Builder().addPackageName(packageName).build()
            try {
                val medicalDataSources =
                    suspendCancellableCoroutine<List<MedicalDataSource>> { continuation ->
                        healthConnectManager.getMedicalDataSources(
                            request,
                            Runnable::run,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                medicalDataSources
            } catch (e: Exception) {
                Log.e(TAG, "Error reading MedicalDataSource from package name.", e)
                emptyList()
            }
        }

    suspend fun fromDataSourceId(dataSourceId: String): List<MedicalDataSource> =
        withContext(dispatcher) {
            try {
                val medicalDataSources =
                    suspendCancellableCoroutine<List<MedicalDataSource>> { continuation ->
                        healthConnectManager.getMedicalDataSources(
                            listOf(dataSourceId),
                            Executors.newSingleThreadExecutor(),
                            continuation.asOutcomeReceiver(),
                        )
                    }
                medicalDataSources
            } catch (e: Exception) {
                Log.e(TAG, "Error reading MedicalDataSource from ID.", e)
                emptyList()
            }
        }
}
