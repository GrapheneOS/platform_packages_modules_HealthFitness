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
package com.android.healthconnect.controller.service

import android.content.Context
import android.health.connect.HealthConnectManager
import android.net.Uri
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadLatestEntryDateUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadLatestEntryDateUseCase
import com.android.healthconnect.controller.data.entries.api.LoadLatestSymptomEntryDateInput
import com.android.healthconnect.controller.data.entries.api.LoadLatestSymptomEntryDateUseCase
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadSymptomDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadSymptomDataEntriesUseCase
import com.android.healthconnect.controller.data.formatters.DistanceFormatter
import com.android.healthconnect.controller.data.formatters.MindfulnessSessionFormatter
import com.android.healthconnect.controller.data.formatters.SleepSessionFormatter
import com.android.healthconnect.controller.data.formatters.StepsFormatter
import com.android.healthconnect.controller.data.formatters.TotalCaloriesBurnedFormatter
import com.android.healthconnect.controller.data.formatters.medical.MedicalEntryFormatter
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.HealthDataExportManager
import com.android.healthconnect.controller.exportimport.api.IUpdateExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.ImportUiState
import com.android.healthconnect.controller.exportimport.api.LoadExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.LoadImportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.LoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.QueryDocumentProvidersUseCase
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.TriggerImportUseCase
import com.android.healthconnect.controller.exportimport.api.UpdateExportSettingsUseCase
import com.android.healthconnect.controller.migration.api.LoadMigrationRestoreStateUseCase
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.LoadFitnessPermissionAppsUseCase
import com.android.healthconnect.controller.onboarding.api.LoadOnboardingStateUseCase
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.permissions.additionalaccess.ILoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.LoadDeclaredHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.LoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.LoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.permissions.shared.QueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.GetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.app.IGetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {

    @Provides
    fun providesLoadHealthPermissionAppsUseCase(
        healthPermissionReader: HealthPermissionReader,
        loadGrantedPermissionsUseCase: GetGrantedHealthPermissionsUseCase,
        getContributorAppInfoUseCase: GetContributorAppInfoUseCase,
        queryRecentAccessUseCase: QueryRecentAccessLogsUseCase,
        appInfoReader: AppInfoReader,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadHealthPermissionApps {
        return LoadHealthPermissionApps(
            healthPermissionReader,
            loadGrantedPermissionsUseCase,
            getContributorAppInfoUseCase,
            queryRecentAccessUseCase,
            appInfoReader,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadAppPermissionsStatusUseCase(
        loadGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
        healthPermissionReader: HealthPermissionReader,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadAppPermissionsStatusUseCase {
        return LoadAppPermissionsStatusUseCase(
            loadGrantedHealthPermissionsUseCase,
            healthPermissionReader,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadDataEntriesUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        loadEntriesHelper: LoadEntriesHelper,
    ): ILoadDataEntriesUseCase {
        return LoadDataEntriesUseCase(dispatcher, loadEntriesHelper)
    }

    @Provides
    fun providesLatestEntryDateUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        loadEntriesHelper: LoadEntriesHelper,
    ): ILoadLatestEntryDateUseCase {
        return LoadLatestEntryDateUseCase(dispatcher, loadEntriesHelper)
    }

    @Provides
    fun providesLoadSymptomDataEntriesUseCase(
        useCase: LoadSymptomDataEntriesUseCase
    ): BaseUseCase<LoadSymptomDataEntriesInput, List<FormattedEntry>> {
        return useCase
    }

    @Provides
    fun providesLatestSymptomEntryDateUseCase(
        useCase: LoadLatestSymptomEntryDateUseCase
    ): BaseUseCase<LoadLatestSymptomEntryDateInput, Instant> {
        return useCase
    }

    @Provides
    fun providesLoadMedicalEntriesUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        loadEntriesHelper: LoadEntriesHelper,
        medicalEntryFormatter: MedicalEntryFormatter,
    ): ILoadMedicalEntriesUseCase {
        return LoadMedicalEntriesUseCase(dispatcher, medicalEntryFormatter, loadEntriesHelper)
    }

    @Provides
    fun providesExerciseRoutePermissionUseCase(
        loadDeclaredHealthPermissionUseCase: LoadDeclaredHealthPermissionUseCase,
        getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase,
        getGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadExerciseRoutePermissionUseCase {
        return LoadExerciseRoutePermissionUseCase(
            loadDeclaredHealthPermissionUseCase,
            getHealthPermissionsFlagsUseCase,
            getGrantedHealthPermissionsUseCase,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadDataAggregationsUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        stepsFormatter: StepsFormatter,
        totalCaloriesBurnedFormatter: TotalCaloriesBurnedFormatter,
        distanceFormatter: DistanceFormatter,
        sleepSessionFormatter: SleepSessionFormatter,
        mindfulnessSessionFormatter: MindfulnessSessionFormatter,
        healthConnectManager: HealthConnectManager,
        appInfoReader: AppInfoReader,
        loadEntriesHelper: LoadEntriesHelper,
    ): ILoadDataAggregationsUseCase {
        return LoadDataAggregationsUseCase(
            loadEntriesHelper,
            stepsFormatter,
            totalCaloriesBurnedFormatter,
            distanceFormatter,
            sleepSessionFormatter,
            mindfulnessSessionFormatter,
            healthConnectManager,
            appInfoReader,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadMenstruationDataUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        loadEntriesHelper: LoadEntriesHelper,
    ): ILoadMenstruationDataUseCase {
        return LoadMenstruationDataUseCase(loadEntriesHelper, dispatcher)
    }

    @Provides
    fun providesGetGrantedHealthPermissionsUseCase(
        healthPermissionManager: HealthPermissionManager
    ): IGetGrantedHealthPermissionsUseCase {
        return GetGrantedHealthPermissionsUseCase(healthPermissionManager)
    }

    @Provides
    fun providesQueryRecentAccessLogsUseCase(
        healthConnectManager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): IQueryRecentAccessLogsUseCase {
        return QueryRecentAccessLogsUseCase(healthConnectManager, dispatcher)
    }

    @Provides
    fun providesGetContributorAppInfoUseCase(
        healthConnectManager: HealthConnectManager,
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): IGetContributorAppInfoUseCase {
        return GetContributorAppInfoUseCase(healthConnectManager, context, dispatcher)
    }

    @Provides
    fun providesLoadExportSettingsUseCase(
        useCase: LoadExportSettingsUseCase
    ): BaseUseCase<Unit, ExportFrequency> {
        return useCase
    }

    @Provides
    fun providesUpdateExportSettingsUseCase(
        healthDataExportManager: HealthDataExportManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): IUpdateExportSettingsUseCase {
        return UpdateExportSettingsUseCase(healthDataExportManager, dispatcher)
    }

    @Provides
    fun providesLoadScheduledExportStatusUseCase(
        useCase: LoadScheduledExportStatusUseCase
    ): BaseUseCase<Unit, ScheduledExportUiState> {
        return useCase
    }

    @Provides
    fun providesQueryDocumentProvidersUseCase(
        useCase: QueryDocumentProvidersUseCase
    ): BaseUseCase<Unit, List<DocumentProvider>> {
        return useCase
    }

    @Provides
    fun providesTriggerImportUseCase(useCase: TriggerImportUseCase): BaseUseCase<Uri, Unit> {
        return useCase
    }

    @Provides
    fun providesLoadImportStatusUseCase(
        useCase: LoadImportStatusUseCase
    ): BaseUseCase<Unit, ImportUiState> {
        return useCase
    }

    @Provides
    fun providesLoadFitnessPermissionAppsUseCase(
        useCase: LoadFitnessPermissionAppsUseCase
    ): BaseUseCase<Unit, List<ConnectedFitnessAppMetadata>> {
        return useCase
    }

    @Provides
    fun provideLoadOnboardingStateUseCase(
        useCase: LoadOnboardingStateUseCase
    ): BaseUseCase<Unit, OnboardingState> {
        return useCase
    }

    @Provides
    fun provideLoadMigrationRestoreStateUseCase(
        useCase: LoadMigrationRestoreStateUseCase
    ): BaseUseCase<Unit, MigrationRestoreState> {
        return useCase
    }
}
