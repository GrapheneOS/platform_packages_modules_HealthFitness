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

package com.android.healthconnect.controller.tests.exportimport.api

import android.health.connect.HealthConnectException
import android.health.connect.exportimport.ScheduledExportSettings
import android.net.Uri
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportFrequency.EXPORT_FREQUENCY_NEVER
import com.android.healthconnect.controller.exportimport.api.ILoadExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.ILoadImportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.ILoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.IQueryDocumentProvidersUseCase
import com.android.healthconnect.controller.exportimport.api.ITriggerImportUseCase
import com.android.healthconnect.controller.exportimport.api.IUpdateExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.ImportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeLoadExportSettingsUseCase :
    FakeUseCase<Unit, ExportFrequency>(dispatcher = Dispatchers.Unconfined),
    ILoadExportSettingsUseCase {
    private var exportFrequency = EXPORT_FREQUENCY_NEVER

    override fun reset() {
        super.reset()
        this.exportFrequency = EXPORT_FREQUENCY_NEVER
    }

    fun updateExportFrequency(frequency: ExportFrequency) {
        this.exportFrequency = frequency
    }

    override suspend fun successValue(input: Unit): ExportFrequency {
        return this.exportFrequency
    }
}

class FakeLoadImportStatusUseCase :
    FakeUseCase<Unit, ImportUiState>(dispatcher = Dispatchers.Unconfined),
    ILoadImportStatusUseCase {
    private var importState: ImportUiState =
        ImportUiState(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)

    override fun reset() {
        super.reset()
        importState = ImportUiState(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)
    }

    fun updateImportStatus(importState: ImportUiState) {
        this.importState = importState
    }

    override suspend fun successValue(input: Unit): ImportUiState {
        return this.importState
    }
}

class FakeLoadScheduledExportStatusUseCase :
    FakeUseCase<Unit, ScheduledExportUiState>(dispatcher = Dispatchers.Unconfined),
    ILoadScheduledExportStatusUseCase {
    private var exportState: ScheduledExportUiState =
        ScheduledExportUiState(
            null,
            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
            0,
            "0",
        )

    override fun reset() {
        super.reset()
        exportState =
            ScheduledExportUiState(
                null,
                ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                0,
                "0",
            )
    }

    fun updateExportStatus(exportState: ScheduledExportUiState) {
        this.exportState = exportState
    }

    override suspend fun successValue(input: Unit): ScheduledExportUiState {
        return this.exportState
    }
}

class FakeQueryDocumentProvidersUseCase :
    FakeUseCase<Unit, List<DocumentProvider>>(dispatcher = Dispatchers.Unconfined),
    IQueryDocumentProvidersUseCase {
    private var documentProviders: List<DocumentProvider> = listOf()

    override fun reset() {
        super.reset()
        documentProviders = listOf()
    }

    fun updateDocumentProviders(documentProviders: List<DocumentProvider>) {
        this.documentProviders = documentProviders
    }

    override suspend fun successValue(input: Unit): List<DocumentProvider> {
        return this.documentProviders
    }
}

class FakeTriggerImportUseCase :
    FakeUseCase<Uri, Unit>(dispatcher = Dispatchers.Unconfined), ITriggerImportUseCase {
    var lastUri: Uri? = null

    override fun reset() {
        super.reset()
        lastUri = null
    }

    override suspend fun successValue(input: Uri) {
        this.lastUri = input
        return
    }
}

class FakeUpdateExportSettingsUseCase : IUpdateExportSettingsUseCase {
    private var forceFail = false
    var mostRecentSettings: ScheduledExportSettings =
        ScheduledExportSettings.Builder()
            .setPeriodInDays(EXPORT_FREQUENCY_NEVER.periodInDays)
            .build()

    override suspend fun invoke(settings: ScheduledExportSettings): UseCaseResults<Unit> {
        return if (forceFail) {
            UseCaseResults.Failed(HealthConnectException(HealthConnectException.ERROR_UNKNOWN))
        } else {
            mostRecentSettings = settings
            UseCaseResults.Success(Unit)
        }
    }

    override suspend fun execute(settings: ScheduledExportSettings) {
        mostRecentSettings = settings
    }

    fun reset() {
        mostRecentSettings =
            ScheduledExportSettings.Builder()
                .setPeriodInDays(EXPORT_FREQUENCY_NEVER.periodInDays)
                .build()
        forceFail = false
    }

    fun setForceFail(forceFail: Boolean) {
        this.forceFail = forceFail
    }
}
