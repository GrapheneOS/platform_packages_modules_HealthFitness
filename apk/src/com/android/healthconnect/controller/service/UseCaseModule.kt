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
import com.android.healthconnect.controller.data.access.ILoadAccessUseCase
import com.android.healthconnect.controller.data.access.ILoadFitnessTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.ILoadMedicalTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.LoadAccessUseCase
import com.android.healthconnect.controller.data.access.LoadFitnessTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.LoadMedicalTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.formatters.DistanceFormatter
import com.android.healthconnect.controller.data.formatters.SleepSessionFormatter
import com.android.healthconnect.controller.data.formatters.StepsFormatter
import com.android.healthconnect.controller.data.formatters.TotalCaloriesBurnedFormatter
import com.android.healthconnect.controller.data.formatters.medical.MedicalEntryFormatter
import com.android.healthconnect.controller.datasources.api.ILoadLastDateWithPriorityDataUseCase
import com.android.healthconnect.controller.datasources.api.ILoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPriorityEntriesUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.ISleepSessionHelper
import com.android.healthconnect.controller.datasources.api.IUpdatePriorityListUseCase
import com.android.healthconnect.controller.datasources.api.LoadLastDateWithPriorityDataUseCase
import com.android.healthconnect.controller.datasources.api.LoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.datasources.api.LoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.LoadPriorityEntriesUseCase
import com.android.healthconnect.controller.datasources.api.LoadPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.SleepSessionHelper
import com.android.healthconnect.controller.datasources.api.UpdatePriorityListUseCase
import com.android.healthconnect.controller.exportimport.api.HealthDataExportManager
import com.android.healthconnect.controller.exportimport.api.HealthDataImportManager
import com.android.healthconnect.controller.exportimport.api.ILoadExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.ILoadImportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.ILoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.IQueryDocumentProvidersUseCase
import com.android.healthconnect.controller.exportimport.api.ITriggerImportUseCase
import com.android.healthconnect.controller.exportimport.api.IUpdateExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.LoadExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.LoadImportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.LoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.QueryDocumentProvidersUseCase
import com.android.healthconnect.controller.exportimport.api.TriggerImportUseCase
import com.android.healthconnect.controller.exportimport.api.UpdateExportSettingsUseCase
import com.android.healthconnect.controller.onboarding.ILoadFitnessPermissionAppsUseCase
import com.android.healthconnect.controller.onboarding.LoadFitnessPermissionAppsUseCase
import com.android.healthconnect.controller.onboarding.api.HealthOnboardingManager
import com.android.healthconnect.controller.onboarding.api.ILoadOnboardingStateUseCase
import com.android.healthconnect.controller.onboarding.api.LoadOnboardingStateUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.ILoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.LoadDeclaredHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.LoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.LoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.permissions.shared.QueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.recentaccess.LoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.GetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.app.IGetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.utils.TimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {
    @Provides
    fun providesLoadRecentAccessUseCase(
        manager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        timeSource: TimeSource,
    ): ILoadRecentAccessUseCase {
        return LoadRecentAccessUseCase(manager, dispatcher, timeSource)
    }

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
    fun providesLoadDataEntriesUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        loadEntriesHelper: LoadEntriesHelper,
    ): ILoadDataEntriesUseCase {
        return LoadDataEntriesUseCase(dispatcher, loadEntriesHelper)
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
    fun providesMostRecentAggregationsUseCase(
        loadDataAggregationsUseCase: LoadDataAggregationsUseCase,
        loadLastDateWithPriorityDataUseCase: LoadLastDateWithPriorityDataUseCase,
        sleepSessionHelper: SleepSessionHelper,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadMostRecentAggregationsUseCase {
        return LoadMostRecentAggregationsUseCase(
            loadDataAggregationsUseCase,
            loadLastDateWithPriorityDataUseCase,
            sleepSessionHelper,
            dispatcher,
        )
    }

    @Provides
    fun providesSleepSessionHelper(
        loadPriorityEntriesUseCase: LoadPriorityEntriesUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ISleepSessionHelper {
        return SleepSessionHelper(loadPriorityEntriesUseCase, dispatcher)
    }

    @Provides
    fun providesLoadPriorityEntriesUseCase(
        loadEntriesHelper: LoadEntriesHelper,
        loadPriorityListUseCase: LoadPriorityListUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadPriorityEntriesUseCase {
        return LoadPriorityEntriesUseCase(loadEntriesHelper, loadPriorityListUseCase, dispatcher)
    }

    @Provides
    fun providesLoadPotentialPriorityListUseCase(
        appInfoReader: AppInfoReader,
        healthConnectManager: HealthConnectManager,
        healthPermissionReader: HealthPermissionReader,
        loadGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase,
        loadPriorityListUseCase: LoadPriorityListUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadPotentialPriorityListUseCase {
        return LoadPotentialPriorityListUseCase(
            appInfoReader,
            healthConnectManager,
            healthPermissionReader,
            loadGrantedHealthPermissionsUseCase,
            loadPriorityListUseCase,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadLastDateWithPriorityDataUseCase(
        healthConnectManager: HealthConnectManager,
        loadEntriesHelper: LoadEntriesHelper,
        loadPriorityListUseCase: LoadPriorityListUseCase,
        timeSource: TimeSource,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadLastDateWithPriorityDataUseCase {
        return LoadLastDateWithPriorityDataUseCase(
            healthConnectManager,
            loadEntriesHelper,
            loadPriorityListUseCase,
            timeSource,
            dispatcher,
        )
    }

    @Provides
    fun providesPriorityListUseCase(
        appInfoReader: AppInfoReader,
        healthConnectManager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadPriorityListUseCase {
        return LoadPriorityListUseCase(healthConnectManager, appInfoReader, dispatcher)
    }

    @Provides
    fun updatePriorityListUseCase(
        healthConnectManager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): IUpdatePriorityListUseCase {
        return UpdatePriorityListUseCase(healthConnectManager, dispatcher)
    }

    @Provides
    fun providesLoadFitnessAccessUseCase(
        loadFitnessTypeContributorAppsUseCase: ILoadFitnessTypeContributorAppsUseCase,
        loadMedicalTypeContributorAppsUseCase: ILoadMedicalTypeContributorAppsUseCase,
        loadGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
        healthPermissionReader: HealthPermissionReader,
        appInfoReader: AppInfoReader,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadAccessUseCase {
        return LoadAccessUseCase(
            loadFitnessTypeContributorAppsUseCase,
            loadMedicalTypeContributorAppsUseCase,
            loadGrantedHealthPermissionsUseCase,
            healthPermissionReader,
            appInfoReader,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadFitnessTypeContributorAppsUseCase(
        appInfoReader: AppInfoReader,
        healthConnectManager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadFitnessTypeContributorAppsUseCase {
        return LoadFitnessTypeContributorAppsUseCase(
            appInfoReader,
            healthConnectManager,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadMedicalTypeContributorAppsUseCase(
        appInfoReader: AppInfoReader,
        healthConnectManager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadMedicalTypeContributorAppsUseCase {
        return LoadMedicalTypeContributorAppsUseCase(
            appInfoReader,
            healthConnectManager,
            dispatcher,
        )
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
        healthDataExportManager: HealthDataExportManager
    ): ILoadExportSettingsUseCase {
        return LoadExportSettingsUseCase(healthDataExportManager)
    }

    @Provides
    fun providesUpdateExportSettingsUseCase(
        healthDataExportManager: HealthDataExportManager
    ): IUpdateExportSettingsUseCase {
        return UpdateExportSettingsUseCase(healthDataExportManager)
    }

    @Provides
    fun providesLoadScheduledExportStatusUseCase(
        healthDataExportManager: HealthDataExportManager
    ): ILoadScheduledExportStatusUseCase {
        return LoadScheduledExportStatusUseCase(healthDataExportManager)
    }

    @Provides
    fun providesQueryDocumentProvidersUseCase(
        healthDataExportManager: HealthDataExportManager
    ): IQueryDocumentProvidersUseCase {
        return QueryDocumentProvidersUseCase(healthDataExportManager)
    }

    @Provides
    fun providesTriggerImportUseCase(
        healthDataImportManager: HealthDataImportManager
    ): ITriggerImportUseCase {
        return TriggerImportUseCase(healthDataImportManager)
    }

    @Provides
    fun providesLoadImportStatusUseCase(
        healthDataImportManager: HealthDataImportManager
    ): ILoadImportStatusUseCase {
        return LoadImportStatusUseCase(healthDataImportManager)
    }

    @Provides
    fun providesLoadFitnessPermissionAppsUseCase(
        @ApplicationContext context: Context,
        healthPermissionReader: HealthPermissionReader,
        loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase,
        appInfoReader: AppInfoReader,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadFitnessPermissionAppsUseCase {
        return LoadFitnessPermissionAppsUseCase(
            context,
            healthPermissionReader,
            loadAppPermissionsStatusUseCase,
            appInfoReader,
            dispatcher,
        )
    }

    @Provides
    fun providesLoadOnboardingStateUseCase(
        healthOnboardingManager: HealthOnboardingManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): ILoadOnboardingStateUseCase {
        return LoadOnboardingStateUseCase(healthOnboardingManager, dispatcher)
    }
}
