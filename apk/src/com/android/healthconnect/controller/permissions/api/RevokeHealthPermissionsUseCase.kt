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
/** Use case to revoke multiple health permissions for an app. */
class RevokeHealthPermissionsUseCase
@Inject
constructor(
    private val healthPermissionManager: HealthPermissionManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<RevokeHealthPermissionsInput, List<String>>(dispatcher),
    IRevokeHealthPermissionsUseCase {

    override suspend fun execute(input: RevokeHealthPermissionsInput): List<String> {
        return healthPermissionManager.revokeHealthPermissions(input.packageName, input.permissions)
    }
}

data class RevokeHealthPermissionsInput(val packageName: String, val permissions: List<String>)

interface IRevokeHealthPermissionsUseCase :
    UseCaseContract<RevokeHealthPermissionsInput, List<String>>
