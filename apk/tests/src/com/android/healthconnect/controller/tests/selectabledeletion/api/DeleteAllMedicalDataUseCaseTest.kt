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

import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.MedicalDataSource
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllMedicalDataUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_2
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
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeleteAllMedicalDataUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeleteAllMedicalDataUseCase
    private val healthPermissionReader: HealthPermissionReader =
        mock((HealthPermissionReader::class.java))
    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)

    @Captor lateinit var dataSourceIdCaptor: ArgumentCaptor<String>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase =
            DeleteAllMedicalDataUseCase(
                manager,
                healthPermissionReader,
                MedicalDataSourceReader(manager, Dispatchers.Main),
                Dispatchers.Main,
            )
    }

    @Test
    fun invoke_deleteAllMedicalData_callsHealthManager() = runTest {
        doAnswer(prepareAnswer(listOf(TEST_MEDICAL_DATA_SOURCE, TEST_MEDICAL_DATA_SOURCE_2)))
            .`when`(manager)
            .getMedicalDataSources(any(GetMedicalDataSourcesRequest::class.java), any(), any())
        whenever(healthPermissionReader.getAppsWithMedicalPermissions())
            .thenReturn(listOf(TEST_MEDICAL_DATA_SOURCE.packageName))

        useCase.invoke()

        verify(manager, times(2))
            .deleteMedicalDataSourceWithData(dataSourceIdCaptor.capture(), any(), any())
        assertThat(dataSourceIdCaptor.value).isEqualTo(TEST_MEDICAL_DATA_SOURCE_2.id)
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