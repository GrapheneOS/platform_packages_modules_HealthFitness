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

package com.android.healthconnect.controller.tests.data.appdata.api

import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.data.appdata.api.IGetAppFitnessPermissionTypesUseCase
import com.android.healthconnect.controller.data.appdata.api.IGetAppMedicalPermissionTypesUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeGetAppFitnessPermissionTypesUseCase :
    FakeUseCase<String, List<PermissionTypesPerCategory>>(dispatcher = Dispatchers.Unconfined),
    IGetAppFitnessPermissionTypesUseCase {

    private var permissionTypesMap = mutableMapOf<String, List<PermissionTypesPerCategory>>()

    override suspend fun successValue(input: String): List<PermissionTypesPerCategory> {
        return permissionTypesMap.getOrDefault(input, emptyList())
    }

    fun setPermissionTypesForApp(
        packageName: String,
        permissionTypesWithData: List<PermissionTypesPerCategory>,
    ) {
        permissionTypesMap[packageName] = permissionTypesWithData
    }

    override fun reset() {
        super.reset()
        permissionTypesMap.clear()
    }
}

class FakeGetAppMedicalPermissionTypesUseCase :
    FakeUseCase<String, List<PermissionTypesPerCategory>>(dispatcher = Dispatchers.Unconfined),
    IGetAppMedicalPermissionTypesUseCase {

    private var permissionTypesMap = mutableMapOf<String, List<PermissionTypesPerCategory>>()

    override suspend fun successValue(input: String): List<PermissionTypesPerCategory> {
        return permissionTypesMap.getOrDefault(input, emptyList())
    }

    fun setPermissionTypesForApp(
        packageName: String,
        permissionTypesWithData: List<PermissionTypesPerCategory>,
    ) {
        permissionTypesMap[packageName] = permissionTypesWithData
    }

    override fun reset() {
        super.reset()
        permissionTypesMap.clear()
    }
}
