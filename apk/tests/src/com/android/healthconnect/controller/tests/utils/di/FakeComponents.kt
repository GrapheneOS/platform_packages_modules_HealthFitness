/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.utils.di

import android.health.connect.HealthConnectException
import android.health.connect.exportimport.ScheduledExportSettings
import android.net.Uri
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadLatestEntryDateUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadLatestEntryDateInput
import com.android.healthconnect.controller.data.entries.api.LoadLatestSymptomEntryDateInput
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataInput
import com.android.healthconnect.controller.data.entries.api.LoadSymptomDataEntriesInput
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportFrequency.EXPORT_FREQUENCY_NEVER
import com.android.healthconnect.controller.exportimport.api.IUpdateExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.ImportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.migration.api.DEFAULT_MIGRATION_RESTORE_STATE
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.permissions.additionalaccess.ExerciseRouteState
import com.android.healthconnect.controller.permissions.additionalaccess.ILoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.IGetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.toInstant
import java.time.Instant
import kotlinx.coroutines.Dispatchers

class FakeHealthPermissionAppsUseCase : ILoadHealthPermissionApps {
    private var list: List<ConnectedAppMetadata> = emptyList()
    var numberOfInvocations = 0
    private var forceFail = false

    fun setForceFail(forceFail: Boolean) {
        this.forceFail = forceFail
    }

    fun updateList(list: List<ConnectedAppMetadata>) {
        this.list = list
    }

    fun addToList(connectedAppMetadata: ConnectedAppMetadata) {
        this.list = list + connectedAppMetadata
    }

    override suspend fun invoke(input: Unit): UseCaseResults<List<ConnectedAppMetadata>> {
        numberOfInvocations += 1
        return if (forceFail) {
            UseCaseResults.Failed(IllegalStateException("Force fail recent access."))
        } else {
            UseCaseResults.Success(list)
        }
    }

    fun reset() {
        this.list = emptyList()
        this.numberOfInvocations = 0
        this.forceFail = false
    }
}

class FakeLoadAppPermissionsStatusUseCase : ILoadAppPermissionsStatusUseCase {
    private var internalMap = mutableMapOf<String, List<HealthPermissionStatus>>()
    var numberOfInvocations = 0

    fun updatePackageName(packageName: String, permissions: List<HealthPermissionStatus>) {
        internalMap[packageName] = permissions
    }

    override suspend fun invoke(packageName: String): List<HealthPermissionStatus> {
        numberOfInvocations += 1
        return internalMap[packageName] ?: emptyList()
    }

    fun reset() {
        internalMap.clear()
        this.numberOfInvocations = 0
    }
}

class FakeLoadDataEntriesUseCase : ILoadDataEntriesUseCase {
    private var formattedList = listOf<FormattedEntry>()
    var wasInvoked = false
        private set

    fun updateList(list: List<FormattedEntry>) {
        formattedList = list
    }

    override suspend fun invoke(input: LoadDataEntriesInput): UseCaseResults<List<FormattedEntry>> {
        wasInvoked = true
        return UseCaseResults.Success(formattedList)
    }

    override suspend fun execute(input: LoadDataEntriesInput): List<FormattedEntry> {
        wasInvoked = true
        return formattedList
    }

    fun reset() {
        formattedList = emptyList()
        wasInvoked = false
    }
}

class FakeLoadSymptomDataEntriesUseCase :
    FakeUseCase<LoadSymptomDataEntriesInput, List<FormattedEntry>>(
        dispatcher = Dispatchers.Unconfined
    ) {
    private var formattedList = listOf<FormattedEntry>()

    fun updateList(list: List<FormattedEntry>) {
        formattedList = list
    }

    override suspend fun successValue(input: LoadSymptomDataEntriesInput): List<FormattedEntry> {
        return formattedList
    }

    override fun reset() {
        super.reset()
        formattedList = emptyList()
    }
}

class FakeLoadLatestSymptomEntryDateUseCase :
    FakeUseCase<LoadLatestSymptomEntryDateInput, Instant>(dispatcher = Dispatchers.Unconfined) {

    private var instant = System.currentTimeMillis().toInstant()

    fun updateInstant(instant: Instant) {
        this.instant = instant
    }

    override suspend fun successValue(input: LoadLatestSymptomEntryDateInput): Instant {
        return instant
    }
}

class FakeLoadMenstruationDataUseCase : ILoadMenstruationDataUseCase {
    private var list: List<FormattedEntry> = emptyList()

    fun updateList(list: List<FormattedEntry>) {
        this.list = list
    }

    override suspend fun invoke(
        input: LoadMenstruationDataInput
    ): UseCaseResults<List<FormattedEntry>> {
        return UseCaseResults.Success(list)
    }

    override suspend fun execute(input: LoadMenstruationDataInput): List<FormattedEntry> {
        return list
    }
}

class FakeLoadDataAggregationsUseCase : ILoadDataAggregationsUseCase {
    private var aggregation: FormattedEntry.FormattedAggregation =
        FormattedEntry.FormattedAggregation("100 steps", "100 steps", "Test App")

    private var aggregations: List<FormattedEntry.FormattedAggregation> = listOf(aggregation)
    var invocationCount = 0
    private var forceFail = false
    private var exceptionMessage = ""

    fun updateAggregation(aggregation: FormattedEntry.FormattedAggregation) {
        this.aggregations = listOf(aggregation)
    }

    /** Used for subsequent invocations when we need different responses */
    fun updateAggregationResponses(aggregations: List<FormattedEntry.FormattedAggregation>) {
        this.aggregations = aggregations
    }

    fun setFailure(exceptionMessage: String) {
        forceFail = true
        this.exceptionMessage = exceptionMessage
    }

    override suspend fun invoke(
        input: LoadAggregationInput
    ): UseCaseResults<FormattedEntry.FormattedAggregation> {
        return if (invocationCount >= this.aggregations.size) {
            UseCaseResults.Failed(
                IllegalStateException(
                    "AggregationResponsesSize = ${this.aggregations.size}, " +
                        "invocationCount = $invocationCount. Please update aggregation responses before invoking."
                )
            )
        } else if (forceFail) {
            UseCaseResults.Failed(IllegalStateException(exceptionMessage))
        } else {
            val result = UseCaseResults.Success(aggregations[invocationCount])
            invocationCount += 1
            result
        }
    }

    override suspend fun execute(input: LoadAggregationInput): FormattedEntry.FormattedAggregation {
        return aggregation
    }

    fun reset() {
        this.invocationCount = 0
        this.aggregations = listOf(aggregation)
        exceptionMessage = ""
        forceFail = false
    }
}

class FakeLoadMedicalEntriesUseCase : ILoadMedicalEntriesUseCase {
    private var formattedList = listOf<FormattedEntry>()

    fun updateList(list: List<FormattedEntry>) {
        formattedList = list
    }

    override suspend fun invoke(
        input: LoadMedicalEntriesInput
    ): UseCaseResults<List<FormattedEntry>> {
        return UseCaseResults.Success(formattedList)
    }

    override suspend fun execute(input: LoadMedicalEntriesInput): List<FormattedEntry> {
        return formattedList
    }
}

class FakeLoadLatestEntryDateUseCase : ILoadLatestEntryDateUseCase {
    private var instant = System.currentTimeMillis().toInstant()

    fun updateInstant(instant: Instant) {
        this.instant = instant
    }

    override suspend fun invoke(input: LoadLatestEntryDateInput): UseCaseResults<Instant> {
        return UseCaseResults.Success(instant)
    }

    override suspend fun execute(input: LoadLatestEntryDateInput): Instant {
        return instant
    }
}

class FakeFailureLoadLatestEntryDateUseCase : ILoadLatestEntryDateUseCase {
    private var instant = System.currentTimeMillis().toInstant()

    fun updateInstant(instant: Instant) {
        this.instant = instant
    }

    override suspend fun invoke(input: LoadLatestEntryDateInput): UseCaseResults<Instant> {
        return UseCaseResults.Failed(Exception())
    }

    override suspend fun execute(input: LoadLatestEntryDateInput): Instant {
        return instant
    }
}

class FakeGetGrantedHealthPermissionsUseCase : IGetGrantedHealthPermissionsUseCase {

    private var permissionsPerApp: MutableMap<String, List<String>> = mutableMapOf()

    override fun invoke(packageName: String): List<String> {
        return permissionsPerApp.getOrDefault(packageName, listOf())
    }

    fun updateData(packageName: String, permissions: List<String>) {
        permissionsPerApp[packageName] = permissions
    }

    fun reset() {
        this.permissionsPerApp = mutableMapOf()
    }
}

class FakeGetContributorAppInfoUseCase : IGetContributorAppInfoUseCase {

    private var appInfoMap: Map<String, AppMetadata> = emptyMap()

    fun setAppInfo(appInfoMap: Map<String, AppMetadata>) {
        this.appInfoMap = appInfoMap
    }

    override suspend fun invoke(): Map<String, AppMetadata> {
        return appInfoMap
    }

    fun reset() {
        this.appInfoMap = emptyMap()
    }
}

class FakeQueryRecentAccessLogsUseCase : IQueryRecentAccessLogsUseCase {
    private var recentAccessMap: Map<String, Instant> = emptyMap()

    fun recentAccessMap(recentAccessMap: Map<String, Instant>) {
        this.recentAccessMap = recentAccessMap
    }

    override suspend fun invoke(): Map<String, Instant> {
        return recentAccessMap
    }

    fun reset() {
        this.recentAccessMap = emptyMap()
    }
}

class FakeLoadExportSettingsUseCase :
    FakeUseCase<Unit, ExportFrequency>(dispatcher = Dispatchers.Unconfined) {
    private var exportFrequency = EXPORT_FREQUENCY_NEVER

    fun updateExportFrequency(frequency: ExportFrequency) {
        this.exportFrequency = frequency
    }

    override suspend fun successValue(input: Unit): ExportFrequency {
        return this.exportFrequency
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

class FakeLoadScheduledExportStatusUseCase :
    FakeUseCase<Unit, ScheduledExportUiState>(dispatcher = Dispatchers.Unconfined) {
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
    FakeUseCase<Unit, List<DocumentProvider>>(dispatcher = Dispatchers.Unconfined) {
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

class FakeLoadExerciseRoute : ILoadExerciseRoutePermissionUseCase {

    private var state =
        ExerciseRouteState(
            exercisePermissionState = PermissionUiState.ASK_EVERY_TIME,
            exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
        )

    fun setExerciseRouteState(state: ExerciseRouteState) {
        this.state = state
    }

    override suspend fun execute(input: String): ExerciseRouteState {
        return this.state
    }

    override suspend fun invoke(input: String): UseCaseResults<ExerciseRouteState> {
        return UseCaseResults.Success(this.state)
    }
}

class FakeLoadImportStatusUseCase :
    FakeUseCase<Unit, ImportUiState>(dispatcher = Dispatchers.Unconfined) {
    private var importState: ImportUiState =
        ImportUiState(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)

    override fun reset() {
        importState = ImportUiState(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)
    }

    fun updateExportStatus(importState: ImportUiState) {
        this.importState = importState
    }

    override suspend fun successValue(input: Unit): ImportUiState {
        return this.importState
    }
}

class FakeLoadFitnessPermissionAppsUseCase :
    FakeUseCase<Unit, List<ConnectedFitnessAppMetadata>>(dispatcher = Dispatchers.Unconfined) {
    private var connectedApps: List<ConnectedFitnessAppMetadata> = emptyList()

    override suspend fun successValue(input: Unit): List<ConnectedFitnessAppMetadata> {
        return this.connectedApps
    }

    fun setConnectedApps(connectedApps: List<ConnectedFitnessAppMetadata>) {
        this.connectedApps = connectedApps
    }
}

class FakeLoadOnboardingStateUseCase :
    FakeUseCase<Unit, OnboardingState>(dispatcher = Dispatchers.Unconfined) {
    private var onboardingState = OnboardingState.ONBOARDING_BANNER_STATE_HIDE

    fun setOnboardingBannerState(onboardingState: OnboardingState) {
        this.onboardingState = onboardingState
    }

    override suspend fun successValue(input: Unit): OnboardingState {
        return this.onboardingState
    }
}

class FakeTriggerImportUseCase : FakeUseCase<Uri, Unit>(dispatcher = Dispatchers.Unconfined) {
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

class FakeLoadMigrationStateUseCase :
    FakeUseCase<Unit, MigrationRestoreState>(dispatcher = Dispatchers.Unconfined) {
    private var migrationState = DEFAULT_MIGRATION_RESTORE_STATE

    fun setMigrationState(migrationState: MigrationRestoreState) {
        this.migrationState = migrationState
    }

    override suspend fun successValue(input: Unit): MigrationRestoreState {
        return migrationState
    }

    override fun reset() {
        super.reset()
        migrationState = DEFAULT_MIGRATION_RESTORE_STATE
    }
}
