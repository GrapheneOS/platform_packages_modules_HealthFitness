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

package com.android.healthconnect.controller.tests.permissions.api

import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionsInput
import com.android.healthconnect.controller.permissions.api.IGrantHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.IRevokeHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionsInput
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeGrantHealthPermissionsUseCase :
    FakeUseCase<GrantHealthPermissionsInput, List<String>>(dispatcher = Dispatchers.Unconfined),
    IGrantHealthPermissionsUseCase {
    private var grantedPermissions: List<String> = emptyList()

    fun updateGrantedPermissions(grantedPermissions: List<String>) {
        this.grantedPermissions = grantedPermissions
    }

    override suspend fun successValue(input: GrantHealthPermissionsInput): List<String> {
        return grantedPermissions
    }

    override fun reset() {
        super.reset()
        grantedPermissions = emptyList()
    }
}

class FakeRevokeHealthPermissionsUseCase :
    FakeUseCase<RevokeHealthPermissionsInput, List<String>>(dispatcher = Dispatchers.Unconfined),
    IRevokeHealthPermissionsUseCase {
    private var revokedPermissions: List<String> = emptyList()

    fun updateRevokedPermissions(revokedPermissions: List<String>) {
        this.revokedPermissions = revokedPermissions
    }

    override suspend fun successValue(input: RevokeHealthPermissionsInput): List<String> {
        return revokedPermissions
    }

    override fun reset() {
        super.reset()
        revokedPermissions = emptyList()
    }
}
