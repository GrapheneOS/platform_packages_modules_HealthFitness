/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.health.connect.DeleteMedicalResourcesRequest
import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.MedicalDataSource
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.api.DeleteMedicalPermissionTypesFromAppUseCase
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeleteMedicalPermissionTypesFromAppUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeleteMedicalPermissionTypesFromAppUseCase
    var manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)

    @Captor lateinit var medicalRequestCaptor: ArgumentCaptor<DeleteMedicalResourcesRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase =
            DeleteMedicalPermissionTypesFromAppUseCase(
                manager,
                MedicalDataSourceReader(manager, Dispatchers.Main),
                Dispatchers.Main,
            )
    }

    @Test
    fun invoke_deletePermissionTypes_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteMedicalResources(any<DeleteMedicalResourcesRequest>(), any(), any())
        doAnswer(prepareAnswer(listOf(TEST_MEDICAL_DATA_SOURCE)))
            .`when`(manager)
            .getMedicalDataSources(any<GetMedicalDataSourcesRequest>(), any(), any())

        val deletePermissionTypes =
            setOf(MedicalPermissionType.ALLERGIES_INTOLERANCES, MedicalPermissionType.VACCINES)

        useCase.invoke("package.name", deletePermissionTypes)

        Mockito.verify(manager).deleteMedicalResources(medicalRequestCaptor.capture(), any(), any())

        assertThat(medicalRequestCaptor.value.dataSourceIds)
            .containsExactly(TEST_MEDICAL_DATA_SOURCE.id)
        assertThat(medicalRequestCaptor.value.medicalResourceTypes)
            .containsExactly(
                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                MEDICAL_RESOURCE_TYPE_VACCINES,
            )
    }

    private fun prepareAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { _: InvocationOnMock -> null }
        return answer
    }

    private fun prepareAnswer(
        medicalDataSourcesResponse: List<MedicalDataSource>
    ): (InvocationOnMock) -> Unit {
        return { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<MedicalDataSource>, *>
            receiver.onResult(medicalDataSourcesResponse)
        }
    }
}
