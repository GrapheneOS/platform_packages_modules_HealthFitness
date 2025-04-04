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

import android.health.connect.DeleteUsingFiltersRequest
import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.MedicalDataSource
import android.os.OutcomeReceiver
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllDataUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllFitnessDataUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllMedicalDataUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_2
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.whenever

@HiltAndroidTest
class DeleteAllDataUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)

    private lateinit var useCase: DeleteAllDataUseCase
    private lateinit var deleteAllFitnessDataUseCase: DeleteAllFitnessDataUseCase
    private lateinit var deleteAllMedicalDataUseCase: DeleteAllMedicalDataUseCase
    private val healthPermissionReader: HealthPermissionReader =
        mock((HealthPermissionReader::class.java))

    @Captor lateinit var dataSourceIdCaptor: ArgumentCaptor<String>
    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        deleteAllFitnessDataUseCase = DeleteAllFitnessDataUseCase(manager, Dispatchers.Main)
        deleteAllMedicalDataUseCase =
            DeleteAllMedicalDataUseCase(
                manager,
                healthPermissionReader,
                MedicalDataSourceReader(manager, Dispatchers.Main),
                Dispatchers.Main,
            )
        useCase =
            DeleteAllDataUseCase(
                deleteAllFitnessDataUseCase,
                deleteAllMedicalDataUseCase,
                Dispatchers.Main,
            )
    }

    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    @Test
    fun invoke_deleteAllData_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any(DeleteUsingFiltersRequest::class.java), any(), any())
        doAnswer(prepareAnswer(listOf(TEST_MEDICAL_DATA_SOURCE, TEST_MEDICAL_DATA_SOURCE_2)))
            .`when`(manager)
            .getMedicalDataSources(any(GetMedicalDataSourcesRequest::class.java), any(), any())
        whenever(healthPermissionReader.getAppsWithMedicalPermissions())
            .thenReturn(listOf(TEST_MEDICAL_DATA_SOURCE.packageName))

        useCase.invoke()

        verify(manager, times(1)).deleteRecords(filtersCaptor.capture(), any(), any())
        assertThat(filtersCaptor.value.timeRangeFilter).isNull()
        assertThat(filtersCaptor.value.dataOrigins).isEmpty()
        assertThat(filtersCaptor.value.recordTypes).isEmpty()
        verify(manager, times(2))
            .deleteMedicalDataSourceWithData(dataSourceIdCaptor.capture(), any(), any())
        assertThat(dataSourceIdCaptor.value).isEqualTo(TEST_MEDICAL_DATA_SOURCE_2.id)
    }

    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    @Test
    fun invoke_deleteAllData_phrFlagDisabled_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any(DeleteUsingFiltersRequest::class.java), any(), any())
        doAnswer(prepareAnswer(listOf(TEST_MEDICAL_DATA_SOURCE, TEST_MEDICAL_DATA_SOURCE_2)))
            .`when`(manager)
            .getMedicalDataSources(any(GetMedicalDataSourcesRequest::class.java), any(), any())
        whenever(healthPermissionReader.getAppsWithMedicalPermissions())
            .thenReturn(listOf(TEST_MEDICAL_DATA_SOURCE.packageName))

        useCase.invoke()

        verify(manager).deleteRecords(filtersCaptor.capture(), any(), any())
        assertThat(filtersCaptor.value.timeRangeFilter).isNull()
        assertThat(filtersCaptor.value.dataOrigins).isEmpty()
        assertThat(filtersCaptor.value.recordTypes).isEmpty()
        verify(manager, times(0))
            .deleteMedicalDataSourceWithData(dataSourceIdCaptor.capture(), any(), any())
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