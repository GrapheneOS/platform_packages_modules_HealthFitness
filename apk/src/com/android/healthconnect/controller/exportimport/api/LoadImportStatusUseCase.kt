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

import android.health.connect.exportimport.ImportStatus
import android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_NONE
import android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_UNKNOWN
import android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_VERSION_MISMATCH
import android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_WRONG_FILE
import android.health.connect.exportimport.ImportStatus.DATA_IMPORT_STARTED
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class LoadImportStatusUseCase
@Inject
constructor(
    private val healthDataImportManager: HealthDataImportManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<Unit, ImportUiState>(dispatcher) {

    override suspend fun execute(input: Unit): ImportUiState {
        val importStatus: ImportStatus = suspendCancellableCoroutine { continuation ->
            healthDataImportManager.getImportStatus(Runnable::run, continuation.asOutcomeReceiver())
        }
        // TODO verify why we need the ImportUiState at all, can we just use the server state?
        val dataImportState: ImportUiState.DataImportState =
            when (importStatus.dataImportState) {
                DATA_IMPORT_STARTED -> ImportUiState.DataImportState.DATA_IMPORT_STARTED
                DATA_IMPORT_ERROR_UNKNOWN -> ImportUiState.DataImportState.DATA_IMPORT_ERROR_UNKNOWN
                DATA_IMPORT_ERROR_NONE -> ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE
                DATA_IMPORT_ERROR_WRONG_FILE ->
                    ImportUiState.DataImportState.DATA_IMPORT_ERROR_WRONG_FILE
                DATA_IMPORT_ERROR_VERSION_MISMATCH ->
                    ImportUiState.DataImportState.DATA_IMPORT_ERROR_VERSION_MISMATCH
                else -> {
                    ImportUiState.DataImportState.DATA_IMPORT_ERROR_UNKNOWN
                }
            }
        return ImportUiState(dataImportState)
    }
}
