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
import android.healthconnect.datatypes.DataOrigin
import android.os.OutcomeReceiver
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.deletion.api.DeleteAppDataUseCase
import com.google.common.truth.Truth
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
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@HiltAndroidTest
class DeleteAppDataUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeleteAppDataUseCase

    var manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = DeleteAppDataUseCase(manager, Dispatchers.Main)
    }

    @Test
    fun invoke_deleteCategoryData_callsHealthManager() = runTest {
        Mockito.doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(
                Mockito.any(DeleteUsingFiltersRequest::class.java), Mockito.any(), Mockito.any())

        val startTime = Instant.now().minusSeconds(10)
        val endTime = Instant.now()

        val deleteAppData =
            DeletionType.DeletionTypeAppData(packageName = "package.name", appName = "APP_NAME")

        useCase.invoke(deleteAppData, TimeRangeFilter.Builder(startTime, endTime).build())

        Mockito.verify(manager, Mockito.times(1))
            .deleteRecords(filtersCaptor.capture(), Mockito.any(), Mockito.any())
        Truth.assertThat(filtersCaptor.value.timeRangeFilter.startTime).isEqualTo(startTime)
        Truth.assertThat(filtersCaptor.value.timeRangeFilter.endTime).isEqualTo(endTime)
        Truth.assertThat(filtersCaptor.value.dataOrigins)
            .containsExactly(DataOrigin.Builder().setPackageName("package.name").build())
        Truth.assertThat(filtersCaptor.value.recordTypes).isEmpty()
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
