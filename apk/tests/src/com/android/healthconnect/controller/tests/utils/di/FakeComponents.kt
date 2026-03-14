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

import com.android.healthconnect.controller.migration.api.DEFAULT_MIGRATION_RESTORE_STATE
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.IGetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
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

class FakeGetGrantedHealthPermissionsUseCase : IGetGrantedHealthPermissionsUseCase {

    private var permissionsPerApp: MutableMap<String, List<String>> = mutableMapOf()
    var forceFail = false

    override fun invoke(packageName: String): List<String> {
        if (forceFail) {
            throw DEFAULT_USE_CASE_EXCEPTION
        }

        return permissionsPerApp.getOrDefault(packageName, listOf())
    }

    fun updateData(packageName: String, permissions: List<String>) {
        permissionsPerApp[packageName] = permissions
    }

    fun reset() {
        this.permissionsPerApp = mutableMapOf()
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
