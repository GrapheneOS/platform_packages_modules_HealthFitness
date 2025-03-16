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
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteHealthPermissionTypes
import com.android.healthconnect.controller.selectabledeletion.api.DeleteFitnessPermissionTypesUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteMedicalPermissionTypesUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeletePermissionTypesUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class DeletePermissionTypesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeletePermissionTypesUseCase

    private val deleteFitnessPermissionTypesUseCase: DeleteFitnessPermissionTypesUseCase =
        mock(DeleteFitnessPermissionTypesUseCase::class.java)
    private val deleteMedicalPermissionTypesUseCase: DeleteMedicalPermissionTypesUseCase =
        mock(DeleteMedicalPermissionTypesUseCase::class.java)

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase =
            DeletePermissionTypesUseCase(
                deleteFitnessPermissionTypesUseCase,
                deleteMedicalPermissionTypesUseCase,
                Dispatchers.Main,
            )
    }

    @Test
    fun permissionTypes_emptyDeleteMethod_noDeletionInvoked() = runTest {
        useCase.invoke(DeleteHealthPermissionTypes(setOf(), 0))
        advanceUntilIdle()

        verifyNoMoreInteractions(deleteFitnessPermissionTypesUseCase)
        verifyNoMoreInteractions(deleteMedicalPermissionTypesUseCase)
    }

    @Test
    fun permissionTypes_delete_deletionInvokedCorrectly() = runTest {
        useCase.invoke(DeleteHealthPermissionTypes(setOf(FitnessPermissionType.DISTANCE), 1))
        advanceUntilIdle()

        val expectedDeletionType =
            DeleteHealthPermissionTypes(setOf(FitnessPermissionType.DISTANCE), 1)
        verify(deleteFitnessPermissionTypesUseCase).invoke(expectedDeletionType)
        verifyNoMoreInteractions(deleteMedicalPermissionTypesUseCase)
    }

    @Test
    fun permissionTypes_deleteFitnessAndMedical_deletionInvokedCorrectly() = runTest {
        useCase.invoke(
            DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.DISTANCE, MedicalPermissionType.VACCINES),
                4,
            )
        )
        advanceUntilIdle()

        val expectedDeletionType =
            DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.DISTANCE, MedicalPermissionType.VACCINES),
                4,
            )
        verify(deleteFitnessPermissionTypesUseCase).invoke(expectedDeletionType)
        verify(deleteMedicalPermissionTypesUseCase).invoke(expectedDeletionType)
    }

    @Test
    fun permissionTypes_deleteMedical_deletionInvokedCorrectly() = runTest {
        useCase.invoke(DeleteHealthPermissionTypes(setOf(MedicalPermissionType.VACCINES), 3))
        advanceUntilIdle()

        val expectedDeletionType =
            DeleteHealthPermissionTypes(setOf(MedicalPermissionType.VACCINES), 3)
        verify(deleteMedicalPermissionTypesUseCase).invoke(expectedDeletionType)
        verifyNoMoreInteractions(deleteFitnessPermissionTypesUseCase)
    }
}
