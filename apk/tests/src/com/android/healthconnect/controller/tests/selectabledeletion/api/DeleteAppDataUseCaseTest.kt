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
import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.MedicalDataSource
import android.os.OutcomeReceiver
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteAppData
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAppDataUseCase
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
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeleteAppDataUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var useCase: DeleteAppDataUseCase

    private var dataManager: HealthConnectManager = mock(HealthConnectManager::class.java)
    private var permissionManager: HealthPermissionManager =
        mock(HealthPermissionManager::class.java)

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>
    @Captor lateinit var dataSourceIdCaptor: ArgumentCaptor<String>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val revokePermissionsUseCase = RevokeAllHealthPermissionsUseCase(permissionManager)
        useCase =
            DeleteAppDataUseCase(
                dataManager,
                MedicalDataSourceReader(dataManager, Dispatchers.Main),
                revokePermissionsUseCase,
                Dispatchers.Main,
            )
    }

    @Test
    fun invoke_deleteAppData_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(dataManager)
            .deleteRecords(any(DeleteUsingFiltersRequest::class.java), any(), any())
        doAnswer(prepareAnswer(listOf(TEST_MEDICAL_DATA_SOURCE, TEST_MEDICAL_DATA_SOURCE_2)))
            .`when`(dataManager)
            .getMedicalDataSources(any(GetMedicalDataSourcesRequest::class.java), any(), any())

        val deleteAppData = DeleteAppData(packageName = "package.name", appName = "App Name")

        useCase.invoke(deleteAppData)

        verify(dataManager).deleteRecords(filtersCaptor.capture(), any(), any())
        assertThat(filtersCaptor.value.timeRangeFilter).isNull()
        assertThat(filtersCaptor.value.dataOrigins)
            .containsExactly(DataOrigin.Builder().setPackageName("package.name").build())
        assertThat(filtersCaptor.value.recordTypes).isEmpty()
        verify(dataManager, times(2))
            .deleteMedicalDataSourceWithData(dataSourceIdCaptor.capture(), any(), any())
        assertThat(dataSourceIdCaptor.value).isEqualTo(TEST_MEDICAL_DATA_SOURCE_2.id)
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
