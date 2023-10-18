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

import android.health.connect.HealthConnectManager
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataUseCase
import com.android.healthconnect.controller.dataentries.formatters.DistanceFormatter
import com.android.healthconnect.controller.dataentries.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.dataentries.formatters.StepsFormatter
import com.android.healthconnect.controller.dataentries.formatters.TotalCaloriesBurnedFormatter
import com.android.healthconnect.controller.datasources.api.ILoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.IUpdatePriorityListUseCase
import com.android.healthconnect.controller.datasources.api.LoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.datasources.api.LoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.UpdatePriorityListUseCase
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.LoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.shared.QueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.permissiontypes.api.ILoadPriorityListUseCase
import com.android.healthconnect.controller.permissiontypes.api.LoadPriorityListUseCase
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.recentaccess.LoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.GetContributorAppInfoUseCase
import com.android.healthconnect.controller.utils.TimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {
    @Provides
    fun providesLoadRecentAccessUseCase(
        manager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        timeSource: TimeSource
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
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ILoadHealthPermissionApps {
        return LoadHealthPermissionApps(
            healthPermissionReader,
            loadGrantedPermissionsUseCase,
            getContributorAppInfoUseCase,
            queryRecentAccessUseCase,
            appInfoReader,
            dispatcher)
    }

    @Provides
    fun providesLoadDataEntriesUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        loadEntriesHelper: LoadEntriesHelper
    ): ILoadDataEntriesUseCase {
        return LoadDataEntriesUseCase(dispatcher, loadEntriesHelper)
    }

    @Provides
    fun providesLoadDataAggregationsUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        stepsFormatter: StepsFormatter,
        totalCaloriesBurnedFormatter: TotalCaloriesBurnedFormatter,
        distanceFormatter: DistanceFormatter,
        healthConnectManager: HealthConnectManager,
        appInfoReader: AppInfoReader,
        loadEntriesHelper: LoadEntriesHelper
    ): ILoadDataAggregationsUseCase {
        return LoadDataAggregationsUseCase(
            loadEntriesHelper,
            stepsFormatter,
            totalCaloriesBurnedFormatter,
            distanceFormatter,
            healthConnectManager,
            appInfoReader,
            dispatcher)
    }

    @Provides
    fun providesLoadMenstruationDataUseCase(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        menstruationPeriodFormatter: MenstruationPeriodFormatter,
        loadEntriesHelper: LoadEntriesHelper
    ): ILoadMenstruationDataUseCase {
        return LoadMenstruationDataUseCase(
            loadEntriesHelper, menstruationPeriodFormatter, dispatcher)
    }

    @Provides
    fun providesMostRecentAggregationsUseCase(
        healthConnectManager: HealthConnectManager,
        loadDataAggregationsUseCase: LoadDataAggregationsUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) : ILoadMostRecentAggregationsUseCase {
        return LoadMostRecentAggregationsUseCase(
            healthConnectManager, loadDataAggregationsUseCase, dispatcher)
    }

    @Provides
    fun providesLoadPotentialPriorityListUseCase(
        appInfoReader: AppInfoReader,
        healthConnectManager: HealthConnectManager,
        healthPermissionReader: HealthPermissionReader,
        loadGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase,
        loadPriorityListUseCase: LoadPriorityListUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) : ILoadPotentialPriorityListUseCase {
        return LoadPotentialPriorityListUseCase(appInfoReader, healthConnectManager,
            healthPermissionReader, loadGrantedHealthPermissionsUseCase, loadPriorityListUseCase,
            dispatcher)
    }

    @Provides
    fun providesPriorityListUseCase(
        appInfoReader: AppInfoReader,
        healthConnectManager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) : ILoadPriorityListUseCase {
        return LoadPriorityListUseCase(healthConnectManager, appInfoReader, dispatcher)
    }

    @Provides
    fun updatePriorityListUseCase(
        healthConnectManager: HealthConnectManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): IUpdatePriorityListUseCase {
        return UpdatePriorityListUseCase(healthConnectManager, dispatcher)
    }
}
