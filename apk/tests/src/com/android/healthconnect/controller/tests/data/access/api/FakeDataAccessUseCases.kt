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
package com.android.healthconnect.controller.tests.data.access.api

import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.data.access.api.ILoadAccessUseCase
import com.android.healthconnect.controller.data.access.api.ILoadFitnessTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.api.ILoadMedicalTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.api.ILoadSymptomAccessUseCase
import com.android.healthconnect.controller.data.access.api.ILoadSymptomContributorAppsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeLoadAccessUseCase :
    FakeUseCase<HealthPermissionType, Map<AppAccessState, List<AppAccessMetadata>>>(
        dispatcher = Dispatchers.Unconfined
    ),
    ILoadAccessUseCase {
    private var appDataMap: Map<AppAccessState, List<AppAccessMetadata>> = emptyMap()

    override suspend fun successValue(
        input: HealthPermissionType
    ): Map<AppAccessState, List<AppAccessMetadata>> {
        return appDataMap
    }

    fun updateMap(map: Map<AppAccessState, List<AppAccessMetadata>>) {
        appDataMap = map
    }

    override fun reset() {
        super.reset()
        appDataMap = emptyMap()
    }
}

class FakeLoadSymptomAccessUseCase :
    FakeUseCase<Unit, Map<AppAccessState, List<AppAccessMetadata>>>(
        dispatcher = Dispatchers.Unconfined
    ),
    ILoadSymptomAccessUseCase {
    private var appDataMap: Map<AppAccessState, List<AppAccessMetadata>> = emptyMap()

    override suspend fun successValue(input: Unit): Map<AppAccessState, List<AppAccessMetadata>> {
        return appDataMap
    }

    fun updateMap(map: Map<AppAccessState, List<AppAccessMetadata>>) {
        appDataMap = map
    }

    override fun reset() {
        super.reset()
        appDataMap = emptyMap()
    }
}

class FakeLoadFitnessTypeContributorAppsUseCase :
    FakeUseCase<FitnessPermissionType, List<AppMetadata>>(dispatcher = Dispatchers.Unconfined),
    ILoadFitnessTypeContributorAppsUseCase {
    private var contributorApps: List<AppMetadata> = emptyList()

    override suspend fun successValue(input: FitnessPermissionType): List<AppMetadata> {
        return contributorApps
    }

    fun updateList(list: List<AppMetadata>) {
        contributorApps = list
    }

    override fun reset() {
        super.reset()
        contributorApps = emptyList()
    }
}

class FakeLoadMedicalTypeContributorAppsUseCase :
    FakeUseCase<MedicalPermissionType, List<AppMetadata>>(dispatcher = Dispatchers.Unconfined),
    ILoadMedicalTypeContributorAppsUseCase {
    private var contributorApps: List<AppMetadata> = emptyList()

    override suspend fun successValue(input: MedicalPermissionType): List<AppMetadata> {
        return contributorApps
    }

    fun updateList(list: List<AppMetadata>) {
        contributorApps = list
    }

    override fun reset() {
        super.reset()
        contributorApps = emptyList()
    }
}

class FakeLoadSymptomTypeContributorAppsUseCase :
    FakeUseCase<Unit, List<AppMetadata>>(dispatcher = Dispatchers.Unconfined),
    ILoadSymptomContributorAppsUseCase {

    private var contributorApps: List<AppMetadata> = emptyList()

    override suspend fun successValue(input: Unit): List<AppMetadata> {
        return contributorApps
    }

    fun updateList(list: List<AppMetadata>) {
        contributorApps = list
    }

    override fun reset() {
        super.reset()
        contributorApps = emptyList()
    }
}
