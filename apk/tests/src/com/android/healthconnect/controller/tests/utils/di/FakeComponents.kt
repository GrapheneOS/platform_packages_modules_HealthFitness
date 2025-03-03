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

import android.health.connect.HealthDataCategory
import android.health.connect.accesslog.AccessLog
import android.health.connect.datatypes.Record
import android.health.connect.exportimport.ScheduledExportSettings
import android.net.Uri
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.data.access.ILoadAccessUseCase
import com.android.healthconnect.controller.data.access.ILoadFitnessTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.ILoadMedicalTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataInput
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.api.ILoadLastDateWithPriorityDataUseCase
import com.android.healthconnect.controller.datasources.api.ILoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPriorityEntriesUseCase
import com.android.healthconnect.controller.datasources.api.ISleepSessionHelper
import com.android.healthconnect.controller.datasources.api.IUpdatePriorityListUseCase
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportFrequency.EXPORT_FREQUENCY_NEVER
import com.android.healthconnect.controller.exportimport.api.ExportImportUseCaseResult
import com.android.healthconnect.controller.exportimport.api.ILoadExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.ILoadImportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.ILoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.IQueryDocumentProvidersUseCase
import com.android.healthconnect.controller.exportimport.api.ITriggerImportUseCase
import com.android.healthconnect.controller.exportimport.api.IUpdateExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.ImportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.permissions.additionalaccess.ExerciseRouteState
import com.android.healthconnect.controller.permissions.additionalaccess.ILoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.permissiontypes.api.ILoadPriorityListUseCase
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.IGetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import java.time.Instant
import java.time.LocalDate

class FakeRecentAccessUseCase : ILoadRecentAccessUseCase {
    private var list: List<AccessLog> = emptyList()
    private var forceFail = false

    fun updateList(list: List<AccessLog>) {
        this.list = list
    }

    override suspend fun execute(input: Unit): List<AccessLog> {
        return list
    }

    override suspend fun invoke(input: Unit): UseCaseResults<List<AccessLog>> {
        return if (forceFail) {
            UseCaseResults.Failed(IllegalStateException("Force fail recent access."))
        } else {
            UseCaseResults.Success(list)
        }
    }

    fun setForceFail(forceFail: Boolean) {
        this.forceFail = forceFail
    }
}

class FakeHealthPermissionAppsUseCase : ILoadHealthPermissionApps {
    private var list: List<ConnectedAppMetadata> = emptyList()

    fun updateList(list: List<ConnectedAppMetadata>) {
        this.list = list
    }

    override suspend fun invoke(): List<ConnectedAppMetadata> {
        return list
    }
}

class FakeLoadDataEntriesUseCase : ILoadDataEntriesUseCase {
    private var formattedList = listOf<FormattedEntry>()

    fun updateList(list: List<FormattedEntry>) {
        formattedList = list
    }

    override suspend fun invoke(input: LoadDataEntriesInput): UseCaseResults<List<FormattedEntry>> {
        return UseCaseResults.Success(formattedList)
    }

    override suspend fun execute(input: LoadDataEntriesInput): List<FormattedEntry> {
        return formattedList
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

class FakeLoadMostRecentAggregationsUseCase : ILoadMostRecentAggregationsUseCase {

    private var mostRecentAggregations = listOf<AggregationCardInfo>()

    override suspend fun invoke(
        healthDataCategory: @HealthDataCategoryInt Int
    ): UseCaseResults<List<AggregationCardInfo>> {
        return UseCaseResults.Success(mostRecentAggregations)
    }

    fun updateMostRecentAggregations(aggregations: List<AggregationCardInfo>) {
        this.mostRecentAggregations = aggregations
    }

    fun reset() {
        this.mostRecentAggregations = listOf()
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

class FakeSleepSessionHelper : ISleepSessionHelper {

    private var forceFail = false
    private var exceptionMessage = ""
    private var datePair = Pair(Instant.EPOCH, Instant.EPOCH)

    fun setDatePair(minDate: Instant, maxDate: Instant) {
        datePair = Pair(minDate, maxDate)
    }

    fun setFailure(exceptionMessage: String) {
        forceFail = true
        this.exceptionMessage = exceptionMessage
    }

    override suspend fun clusterSleepSessions(
        lastDateWithData: LocalDate
    ): UseCaseResults<Pair<Instant, Instant>> {
        return if (forceFail) UseCaseResults.Failed(Exception(this.exceptionMessage))
        else UseCaseResults.Success(datePair)
    }

    fun reset() {
        datePair = Pair(Instant.EPOCH, Instant.EPOCH)
        exceptionMessage = ""
        forceFail = false
    }
}

class FakeLoadPriorityEntriesUseCase : ILoadPriorityEntriesUseCase {

    private var priorityEntries = mutableMapOf<LocalDate, List<Record>>()
    private var forceFail = false
    private var exceptionMessage = ""

    override suspend fun invoke(
        fitnessPermissionType: FitnessPermissionType,
        localDate: LocalDate,
    ): UseCaseResults<List<Record>> {
        return if (forceFail) UseCaseResults.Failed(Exception(this.exceptionMessage))
        else UseCaseResults.Success(priorityEntries.getOrDefault(localDate, listOf()))
    }

    fun setEntriesList(localDate: LocalDate, list: List<Record>) {

        priorityEntries[localDate] = list
    }

    fun setFailure(exceptionMessage: String) {
        forceFail = true
        this.exceptionMessage = exceptionMessage
    }

    fun reset() {
        priorityEntries.clear()
        exceptionMessage = ""
        forceFail = false
    }
}

class FakeLoadPotentialPriorityListUseCase : ILoadPotentialPriorityListUseCase {

    private var potentialPriorityList = listOf<AppMetadata>()

    override suspend fun invoke(
        category: @HealthDataCategoryInt Int
    ): UseCaseResults<List<AppMetadata>> {
        return UseCaseResults.Success(potentialPriorityList)
    }

    fun updatePotentialPriorityList(potentialList: List<AppMetadata>) {
        this.potentialPriorityList = potentialList
    }

    fun reset() {
        this.potentialPriorityList = listOf()
    }
}

class FakeLoadPriorityListUseCase : ILoadPriorityListUseCase {

    private var priorityList = listOf<AppMetadata>()
    private var forceFail = false
    private var exceptionMessage = ""

    override suspend fun invoke(
        input: @HealthDataCategoryInt Int
    ): UseCaseResults<List<AppMetadata>> {
        return if (forceFail) UseCaseResults.Failed(Exception(this.exceptionMessage))
        else UseCaseResults.Success(priorityList)
    }

    override suspend fun execute(input: Int): List<AppMetadata> {
        return priorityList
    }

    fun updatePriorityList(priorityList: List<AppMetadata>) {
        this.priorityList = priorityList
    }

    fun setFailure(exceptionMessage: String) {
        forceFail = true
        this.exceptionMessage = exceptionMessage
    }

    fun reset() {
        this.priorityList = listOf()
        exceptionMessage = ""
        forceFail = false
    }
}

class FakeUpdatePriorityListUseCase : IUpdatePriorityListUseCase {

    var priorityList = listOf<String>()
    var category = HealthDataCategory.UNKNOWN

    override suspend fun invoke(priorityList: List<String>, category: Int) {
        this.priorityList = priorityList
        this.category = category
    }

    fun reset() {
        this.priorityList = listOf()
        this.category = HealthDataCategory.UNKNOWN
    }
}

class FakeLoadAccessUseCase : ILoadAccessUseCase {

    private var appDataMap: Map<AppAccessState, List<AppAccessMetadata>> = mutableMapOf()

    override suspend fun invoke(
        permissionType: HealthPermissionType
    ): UseCaseResults<Map<AppAccessState, List<AppAccessMetadata>>> {
        return UseCaseResults.Success(appDataMap)
    }

    fun updateMap(map: Map<AppAccessState, List<AppAccessMetadata>>) {
        appDataMap = map
    }

    fun reset() {
        this.appDataMap = mutableMapOf()
    }
}

class FakeLoadFitnessTypeContributorAppsUseCase : ILoadFitnessTypeContributorAppsUseCase {

    private var contributorApps: List<AppMetadata> = listOf()

    override suspend fun invoke(permissionType: FitnessPermissionType): List<AppMetadata> {
        return contributorApps
    }

    fun updateList(list: List<AppMetadata>) {
        contributorApps = list
    }

    fun reset() {
        this.contributorApps = listOf()
    }
}

class FakeLoadMedicalTypeContributorAppsUseCase : ILoadMedicalTypeContributorAppsUseCase {

    private var contributorApps: List<AppMetadata> = listOf()

    override suspend fun invoke(permissionType: MedicalPermissionType): List<AppMetadata> {
        return contributorApps
    }

    fun updateList(list: List<AppMetadata>) {
        contributorApps = list
    }

    fun reset() {
        this.contributorApps = listOf()
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

class FakeLoadLastDateWithPriorityDataUseCase : ILoadLastDateWithPriorityDataUseCase {

    private var lastDateWithPriorityDataMap = mutableMapOf<FitnessPermissionType, LocalDate?>()
    private var forceFail = false
    private var exceptionMessage = ""

    fun setLastDateWithPriorityDataForHealthPermissionType(
        fitnessPermissionType: FitnessPermissionType,
        localDate: LocalDate?,
    ) {
        lastDateWithPriorityDataMap[fitnessPermissionType] = localDate
    }

    fun setFailure(exceptionMessage: String) {
        forceFail = true
        this.exceptionMessage = exceptionMessage
    }

    override suspend fun invoke(
        fitnessPermissionType: FitnessPermissionType
    ): UseCaseResults<LocalDate?> {
        if (forceFail) return UseCaseResults.Failed(Exception(this.exceptionMessage))
        return if (lastDateWithPriorityDataMap.containsKey(fitnessPermissionType))
            UseCaseResults.Success(lastDateWithPriorityDataMap[fitnessPermissionType])
        else UseCaseResults.Success(null)
    }

    fun reset() {
        lastDateWithPriorityDataMap.clear()
        exceptionMessage = ""
        forceFail = false
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

class FakeLoadExportSettingsUseCase : ILoadExportSettingsUseCase {
    private var exportFrequency = EXPORT_FREQUENCY_NEVER

    override suspend fun invoke(): ExportImportUseCaseResult<ExportFrequency> {
        return ExportImportUseCaseResult.Success(exportFrequency)
    }

    fun updateExportFrequency(frequency: ExportFrequency) {
        this.exportFrequency = frequency
    }

    fun reset() {
        this.exportFrequency = EXPORT_FREQUENCY_NEVER
    }
}

class FakeUpdateExportSettingsUseCase : IUpdateExportSettingsUseCase {
    var mostRecentSettings: ScheduledExportSettings =
        ScheduledExportSettings.Builder()
            .setPeriodInDays(EXPORT_FREQUENCY_NEVER.periodInDays)
            .build()

    override suspend fun invoke(
        settings: ScheduledExportSettings
    ): ExportImportUseCaseResult<Unit> {
        mostRecentSettings = settings
        return ExportImportUseCaseResult.Success(Unit)
    }

    fun reset() {
        mostRecentSettings =
            ScheduledExportSettings.Builder()
                .setPeriodInDays(EXPORT_FREQUENCY_NEVER.periodInDays)
                .build()
    }
}

class FakeLoadScheduledExportStatusUseCase : ILoadScheduledExportStatusUseCase {
    private var exportState: ScheduledExportUiState =
        ScheduledExportUiState(
            null,
            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
            0,
            "0",
        )

    fun reset() {
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

    override suspend fun invoke(): ExportImportUseCaseResult<ScheduledExportUiState> {
        return ExportImportUseCaseResult.Success(exportState)
    }
}

class FakeQueryDocumentProvidersUseCase : IQueryDocumentProvidersUseCase {
    private var documentProviders: List<DocumentProvider> = listOf()

    fun reset() {
        documentProviders = listOf()
    }

    fun updateDocumentProviders(documentProviders: List<DocumentProvider>) {
        this.documentProviders = documentProviders
    }

    override suspend fun invoke(): ExportImportUseCaseResult<List<DocumentProvider>> {
        return ExportImportUseCaseResult.Success(documentProviders)
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

class FakeTriggerImportUseCase : ITriggerImportUseCase {

    private var lastImportCompletionInstant: Instant? = null

    private var importState: ImportUiState =
        ImportUiState(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)

    fun reset() {
        lastImportCompletionInstant = null
    }

    fun updateLastImportCompletionInstant(instant: Instant) {
        this.lastImportCompletionInstant = instant
    }

    override suspend fun invoke(fileToImportUri: Uri): ExportImportUseCaseResult<Unit> {
        return ExportImportUseCaseResult.Success(Unit)
    }
}

class FakeLoadImportStatusUseCase : ILoadImportStatusUseCase {
    private var importState: ImportUiState =
        ImportUiState(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)

    fun reset() {
        importState = ImportUiState(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)
    }

    fun updateExportStatus(importState: ImportUiState) {
        this.importState = importState
    }

    override suspend fun invoke(): ExportImportUseCaseResult<ImportUiState> {
        return ExportImportUseCaseResult.Success(importState)
    }
}
