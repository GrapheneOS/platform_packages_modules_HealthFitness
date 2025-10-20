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
 */

package com.android.healthconnect.controller.exportimport.api

import android.health.connect.exportimport.ScheduledExportStatus
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class LoadScheduledExportStatusUseCase
@Inject
constructor(
    private val healthDataExportManager: HealthDataExportManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadScheduledExportStatusUseCase, BaseUseCase<Unit, ScheduledExportUiState>(dispatcher) {

    companion object {
        private const val TAG = "LoadScheduledExportStatusUseCase"
    }

    override suspend fun execute(input: Unit): ScheduledExportUiState {
        val scheduledExportStatus: ScheduledExportStatus =
            suspendCancellableCoroutine { continuation ->
                healthDataExportManager.getScheduledExportStatus(
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        val dataExportError: ScheduledExportUiState.DataExportError =
            when (scheduledExportStatus.dataExportError) {
                ScheduledExportStatus.DATA_EXPORT_ERROR_UNKNOWN ->
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_UNKNOWN
                ScheduledExportStatus.DATA_EXPORT_ERROR_NONE ->
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE
                ScheduledExportStatus.DATA_EXPORT_LOST_FILE_ACCESS ->
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS
                else -> {
                    ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_UNKNOWN
                }
            }
        return ScheduledExportUiState(
            scheduledExportStatus.lastSuccessfulExportTime,
            dataExportError,
            scheduledExportStatus.periodInDays,
            scheduledExportStatus.lastExportFileName,
            scheduledExportStatus.lastExportAppName,
            scheduledExportStatus.nextExportFileName,
            scheduledExportStatus.nextExportAppName,
            scheduledExportStatus.lastFailedExportTime,
            scheduledExportStatus.nextExportSequentialNumber,
        )
    }
}

interface ILoadScheduledExportStatusUseCase {
    /** Returns the stored scheduled export status. */
    suspend fun invoke(input: Unit): UseCaseResults<ScheduledExportUiState>

    suspend fun execute(input: Unit): ScheduledExportUiState
}
