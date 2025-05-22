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
package com.android.healthconnect.controller.tests.permissions.api

import android.content.Context
import android.health.connect.HealthPermissions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class RevokeHealthPermissionUseCaseTest {
    private lateinit var context: Context
    private lateinit var useCase: RevokeHealthPermissionUseCase
    private val healthPermissionManager: HealthPermissionManager =
        mock(HealthPermissionManager::class.java)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        useCase = RevokeHealthPermissionUseCase(healthPermissionManager)
    }

    @Test
    fun invoke_withFitnessPermission_callsHealthPermissionManager() {
        useCase.invoke(
            "TEST_APP",
            FitnessPermission(FitnessPermissionType.HEIGHT, PermissionsAccessType.WRITE).toString())

        verify(healthPermissionManager)
            .revokeHealthPermission("TEST_APP", "android.permission.health.WRITE_HEIGHT")
    }

    @Test
    fun invoke_withAdditionalPermission_callsHealthPermissionManager() {
        useCase.invoke("TEST_APP", HealthPermissions.READ_HEALTH_DATA_HISTORY)

        verify(healthPermissionManager)
            .revokeHealthPermission(
                "TEST_APP", "android.permission.health.READ_HEALTH_DATA_HISTORY")
    }
}
