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

package com.android.healthconnect.controller.tests.route.api

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingIds
import android.health.connect.ReadRecordsResponse
import android.health.connect.datatypes.ExerciseRoute
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType
import android.health.connect.datatypes.Metadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.route.api.LoadExerciseRouteUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadExerciseRouteUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: LoadExerciseRouteUseCase
    var manager: HealthConnectManager = mock()

    private val listCaptor = argumentCaptor<ReadRecordsRequestUsingIds<ExerciseSessionRecord>>()

    @Before
    fun setup() {
        useCase = LoadExerciseRouteUseCase(manager, Dispatchers.Main)
    }

    @Test
    fun invoke_noSession() = runTest {
        manager.stub {
            on {
                readRecords(any<ReadRecordsRequestUsingIds<ExerciseSessionRecord>>(), any(), any())
            } doReturnResult
                Result.success(ReadRecordsResponse(listOf<ExerciseSessionRecord>(), -1))
        }

        val result = useCase.invoke("test_id")
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEqualTo(null)
        verify(manager).readRecords(listCaptor.capture(), any(), any())
        assertThat(listCaptor.firstValue.recordIdFilters[0].id).isEqualTo("test_id")
    }

    @Test
    fun invoke_noRouteInSession() = runTest {
        val start = Instant.ofEpochMilli(1234567891011)
        val end = start.plusMillis(123456)
        val expectedSession =
            listOf(
                ExerciseSessionRecord.Builder(
                        Metadata.Builder().build(),
                        start,
                        end,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING,
                    )
                    .build()
            )
        manager.stub {
            on {
                readRecords(any<ReadRecordsRequestUsingIds<ExerciseSessionRecord>>(), any(), any())
            } doReturnResult Result.success(ReadRecordsResponse(expectedSession, -1))
        }

        val result = useCase.invoke("test_id")
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEqualTo(null)
        verify(manager).readRecords(listCaptor.capture(), any(), any())
        assertThat(listCaptor.firstValue.recordIdFilters[0].id).isEqualTo("test_id")
    }

    @Test
    fun invoke_returnsRoute() = runTest {
        val start = Instant.ofEpochMilli(1234567891011)
        val end = start.plusMillis(123456)
        val expectedSession =
            ExerciseSessionRecord.Builder(
                    Metadata.Builder().build(),
                    start,
                    end,
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING,
                )
                .setRoute(
                    ExerciseRoute(
                        listOf(
                            ExerciseRoute.Location.Builder(
                                    start.plusSeconds(12),
                                    52.26019,
                                    21.02268,
                                )
                                .build(),
                            ExerciseRoute.Location.Builder(
                                    start.plusSeconds(40),
                                    52.26000,
                                    21.02360,
                                )
                                .build(),
                        )
                    )
                )
                .build()

        manager.stub {
            on {
                readRecords(any<ReadRecordsRequestUsingIds<ExerciseSessionRecord>>(), any(), any())
            } doReturnResult Result.success(ReadRecordsResponse(listOf(expectedSession), -1))
        }

        val result = useCase.invoke("test_id")
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEqualTo(expectedSession)
        verify(manager).readRecords(listCaptor.capture(), any(), any())
        assertThat(listCaptor.firstValue.recordIdFilters[0].id).isEqualTo("test_id")
    }

    @Test
    fun invoke_managerError_returnsFailure() = runTest {
        manager.stub {
            on {
                readRecords(any<ReadRecordsRequestUsingIds<ExerciseSessionRecord>>(), any(), any())
            } doReturnResult
                Result.failure<ReadRecordsResponse<ExerciseSessionRecord>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }
        val result = useCase.invoke("test_id")
        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(HealthConnectException::class.java)
    }
}
