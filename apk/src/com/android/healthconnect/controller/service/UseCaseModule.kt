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
import com.android.healthconnect.controller.exportimport.api.HealthDataExportManager
import com.android.healthconnect.controller.exportimport.api.IUpdateExportSettingsUseCase
import com.android.healthconnect.controller.exportimport.api.UpdateExportSettingsUseCase
import com.android.healthconnect.controller.migration.api.LoadMigrationRestoreStateUseCase
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.LoadFitnessPermissionAppsUseCase
import com.android.healthconnect.controller.onboarding.api.LoadOnboardingStateUseCase
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
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
    fun providesLoadFitnessPermissionAppsUseCase(
        useCase: LoadFitnessPermissionAppsUseCase
    ): BaseUseCase<Unit, List<ConnectedFitnessAppMetadata>> {
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
