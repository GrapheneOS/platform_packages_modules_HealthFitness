/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.deletion.api

import android.healthconnect.DeleteUsingFiltersRequest
import android.healthconnect.HealthConnectManager
import android.healthconnect.TimeRangeFilter
import android.healthconnect.datatypes.ActiveCaloriesBurnedRecord
import android.healthconnect.datatypes.DistanceRecord
import android.healthconnect.datatypes.PowerRecord
import android.healthconnect.datatypes.SpeedRecord
import android.healthconnect.datatypes.StepsCadenceRecord
import android.healthconnect.datatypes.StepsRecord
import android.healthconnect.datatypes.TotalCaloriesBurnedRecord
import android.os.OutcomeReceiver
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.deletion.api.DeleteCategoryUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@HiltAndroidTest
class DeleteCategoryUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeleteCategoryUseCase
    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = DeleteCategoryUseCase(manager, Dispatchers.Main)
    }

    @Test
    fun invoke_deleteCategoryData_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any(DeleteUsingFiltersRequest::class.java), any(), any())

        val startTime = Instant.now().minusSeconds(10)
        val endTime = Instant.now()

        val deleteCategory = DeletionType.DeletionTypeCategoryData(HealthDataCategory.ACTIVITY)

        useCase.invoke(deleteCategory, TimeRangeFilter.Builder(startTime, endTime).build())

        Mockito.verify(manager, Mockito.times(1))
            .deleteRecords(filtersCaptor.capture(), any(), any())

        assertThat(filtersCaptor.value.timeRangeFilter.startTime).isEqualTo(startTime)
        assertThat(filtersCaptor.value.timeRangeFilter.endTime).isEqualTo(endTime)
        assertThat(filtersCaptor.value.dataOrigins).isEmpty()
        // TODO update when more records available
        assertThat(filtersCaptor.value.recordTypes)
            .containsExactly(
                TotalCaloriesBurnedRecord::class.java,
                ActiveCaloriesBurnedRecord::class.java,
                DistanceRecord::class.java,
                StepsRecord::class.java,
                StepsCadenceRecord::class.java,
                SpeedRecord::class.java,
                PowerRecord::class.java)
    }

    private fun prepareAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<Void, *>
            receiver.onResult(null)
            null
        }
        return answer
    }
}
