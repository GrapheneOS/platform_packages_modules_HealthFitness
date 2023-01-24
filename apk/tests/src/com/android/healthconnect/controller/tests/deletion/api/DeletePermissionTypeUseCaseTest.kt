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
package com.android.healthconnect.controller.tests.deletion.api

import android.healthconnect.DeleteUsingFiltersRequest
import android.healthconnect.HealthConnectManager
import android.healthconnect.TimeRangeFilter
import android.healthconnect.datatypes.StepsCadenceRecord
import android.healthconnect.datatypes.StepsRecord
import android.os.OutcomeReceiver
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.deletion.api.DeletePermissionTypeUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import java.time.Instant

@HiltAndroidTest
class DeletePermissionTypeUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeletePermissionTypeUseCase
    var manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = DeletePermissionTypeUseCase(manager, Dispatchers.Main)
    }

    @Test
    fun invoke_deletePermissionTypeData_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any(DeleteUsingFiltersRequest::class.java), any(), any())

        val startTime = Instant.now().minusSeconds(10)
        val endTime = Instant.now()

        val deletePermissionType =
            DeletionType.DeletionTypeHealthPermissionTypeData(HealthPermissionType.STEPS)

        useCase.invoke(deletePermissionType, TimeRangeFilter.Builder(startTime, endTime).build())

        Mockito.verify(manager, Mockito.times(1))
            .deleteRecords(filtersCaptor.capture(), any(), any())

        Truth.assertThat(filtersCaptor.value.timeRangeFilter.startTime).isEqualTo(startTime)
        Truth.assertThat(filtersCaptor.value.timeRangeFilter.endTime).isEqualTo(endTime)
        Truth.assertThat(filtersCaptor.value.dataOrigins).isEmpty()
        // TODO update when more records available
        Truth.assertThat(filtersCaptor.value.recordTypes)
            .containsExactly(StepsRecord::class.java, StepsCadenceRecord::class.java)
    }

    private fun prepareAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<Any?, *>
            receiver.onResult(Any())
            null
        }
        return answer
    }
}
