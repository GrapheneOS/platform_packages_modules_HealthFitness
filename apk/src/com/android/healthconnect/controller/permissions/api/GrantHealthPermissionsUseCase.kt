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

package com.android.healthconnect.controller.permissions.api

import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
/** Use case to grant multiple health permissions for an app. */
class GrantHealthPermissionsUseCase
@Inject
constructor(
    private val healthPermissionManager: HealthPermissionManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<GrantHealthPermissionsInput, List<String>>(dispatcher),
    IGrantHealthPermissionsUseCase {

    override suspend fun execute(input: GrantHealthPermissionsInput): List<String> {
        return healthPermissionManager.grantHealthPermissions(input.packageName, input.permissions)
    }
}

data class GrantHealthPermissionsInput(val packageName: String, val permissions: List<String>)

interface IGrantHealthPermissionsUseCase :
    UseCaseContract<GrantHealthPermissionsInput, List<String>>
