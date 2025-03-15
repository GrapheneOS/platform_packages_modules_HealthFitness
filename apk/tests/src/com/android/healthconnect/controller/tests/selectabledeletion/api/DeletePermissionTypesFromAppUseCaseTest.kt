/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.selectabledeletion.api

import android.health.connect.DeleteUsingFiltersRequest
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.api.DeleteFitnessPermissionTypesFromAppUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteMedicalPermissionTypesFromAppUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeletePermissionTypesFromAppUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class DeletePermissionTypesFromAppUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeletePermissionTypesFromAppUseCase

    private val deleteFitnessPermissionTypesFromAppUseCase:
        DeleteFitnessPermissionTypesFromAppUseCase =
        mock(DeleteFitnessPermissionTypesFromAppUseCase::class.java)
    private val deleteMedicalPermissionTypesFromAppUseCase:
        DeleteMedicalPermissionTypesFromAppUseCase =
        mock(DeleteMedicalPermissionTypesFromAppUseCase::class.java)
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase = mock()

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase =
            DeletePermissionTypesFromAppUseCase(
                deleteFitnessPermissionTypesFromAppUseCase,
                deleteMedicalPermissionTypesFromAppUseCase,
                revokeAllHealthPermissionsUseCase,
                Dispatchers.Main,
            )
    }

    @Test
    fun permissionTypes_emptyDeleteMethod_noDeletionInvoked() = runTest {
        useCase.invoke("package.name", setOf())

        advanceUntilIdle()

        verifyNoMoreInteractions(deleteFitnessPermissionTypesFromAppUseCase)
        verifyNoMoreInteractions(deleteMedicalPermissionTypesFromAppUseCase)
    }

    @Test
    fun permissionTypes_deleteSomeData_deletionInvokedCorrectly() = runTest {
        val permissions = setOf(FitnessPermissionType.DISTANCE)
        useCase.invoke("package.name", permissions)
        advanceUntilIdle()

        verify(deleteFitnessPermissionTypesFromAppUseCase).invoke("package.name", permissions)
        verifyNoMoreInteractions(deleteMedicalPermissionTypesFromAppUseCase)
    }

    @Test
    fun permissionTypes_deleteSomeFitnessAndMedical_deletionInvokedCorrectly() = runTest {
        useCase.invoke(
            packageName = "package.name",
            permissions = setOf(FitnessPermissionType.DISTANCE, MedicalPermissionType.VACCINES),
        )
        advanceUntilIdle()

        verify(deleteFitnessPermissionTypesFromAppUseCase)
            .invoke("package.name", setOf(FitnessPermissionType.DISTANCE))
        verify(deleteMedicalPermissionTypesFromAppUseCase)
            .invoke("package.name", setOf(MedicalPermissionType.VACCINES))
    }

    @Test
    fun permissionTypes_deleteSomeMedical_deletionInvokedCorrectly() = runTest {
        useCase.invoke(
            packageName = "package.name",
            permissions = setOf(MedicalPermissionType.VACCINES),
        )
        advanceUntilIdle()

        val expectedDeletionType = setOf(MedicalPermissionType.VACCINES)
        verify(deleteMedicalPermissionTypesFromAppUseCase)
            .invoke("package.name", expectedDeletionType)
        verifyNoMoreInteractions(deleteFitnessPermissionTypesFromAppUseCase)
    }

    @Test
    fun permissionTypes_removesPermissions_callsRevokeAllHealthPermissionsUseCase() = runTest {
        useCase.invoke(
            packageName = "package.name",
            permissions = setOf(MedicalPermissionType.VACCINES),
            removePermissions = true,
        )

        advanceUntilIdle()
        verify(revokeAllHealthPermissionsUseCase).invoke("package.name")
    }

    @Test
    fun permissionTypes_DoNotRemovesPermissions_DoesNotCallRevokeAllHealthPermissionsUseCase() =
        runTest {
            useCase.invoke(
                packageName = "package.name",
                permissions = setOf(MedicalPermissionType.VACCINES),
                removePermissions = false,
            )
            advanceUntilIdle()
            verifyNoMoreInteractions(revokeAllHealthPermissionsUseCase)
        }
}
