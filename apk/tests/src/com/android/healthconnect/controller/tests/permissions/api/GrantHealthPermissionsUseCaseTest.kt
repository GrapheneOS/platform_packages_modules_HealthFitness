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

import android.health.connect.HealthPermissions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionsInput
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GrantHealthPermissionsUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: GrantHealthPermissionsUseCase
    private val healthPermissionManager: HealthPermissionManager = mock()

    @Before
    fun setup() {
        useCase = GrantHealthPermissionsUseCase(healthPermissionManager, Dispatchers.Main)
    }

    @Test
    fun invoke_success_callsGrantHealthPermissions() = runTest {
        val permissions = listOf(HealthPermissions.READ_STEPS, HealthPermissions.WRITE_STEPS)
        whenever(healthPermissionManager.grantHealthPermissions("TEST_APP", permissions))
            .thenReturn(permissions)

        val result = useCase.invoke(GrantHealthPermissionsInput("TEST_APP", permissions))

        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEqualTo(permissions)
        verify(healthPermissionManager).grantHealthPermissions("TEST_APP", permissions)
    }

    @Test
    fun invoke_onFailure_returnsFailed() = runTest {
        val permissions = listOf(HealthPermissions.READ_STEPS)
        val exception = RuntimeException("Error")
        whenever(healthPermissionManager.grantHealthPermissions(any(), any())).thenThrow(exception)

        val result = useCase.invoke(GrantHealthPermissionsInput("TEST_APP", permissions))

        assertThat(result is UseCaseResults.Failed).isTrue()
        assertThat((result as UseCaseResults.Failed).exception).isEqualTo(exception)
    }
}
