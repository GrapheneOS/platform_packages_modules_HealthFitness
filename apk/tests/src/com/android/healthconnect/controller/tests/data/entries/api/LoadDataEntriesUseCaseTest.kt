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
package com.android.healthconnect.controller.tests.data.entries.api

import android.content.Context
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.data.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.data.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.devices.api.FakeGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.forDataType
import com.android.healthconnect.controller.tests.utils.getStepsRecord
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.randomInstant
import com.android.healthconnect.controller.utils.toInstantAtStartOfDay
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadDataEntriesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()
    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter
    @Inject lateinit var menstruationPeriodFormatter: MenstruationPeriodFormatter
    @Inject lateinit var dataSourceReader: MedicalDataSourceReader
    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)

    private lateinit var context: Context
    private lateinit var loadEntriesHelper: LoadEntriesHelper
    private lateinit var loadDataEntriesUseCase: LoadDataEntriesUseCase
    private val fakeGetCurrentDeviceIdUseCase =
        fakeUseCaseRule.watch(FakeGetCurrentDeviceIdUseCase())

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        loadEntriesHelper =
            LoadEntriesHelper(
                context,
                healthDataEntryFormatter,
                menstruationPeriodFormatter,
                healthConnectManager,
                dataSourceReader,
                fakeGetCurrentDeviceIdUseCase,
            )
        loadDataEntriesUseCase = LoadDataEntriesUseCase(Dispatchers.Main, loadEntriesHelper)
    }

    @Test
    fun invoke_returnsFormattedData() = runTest {
        val stepsDate = LocalDate.of(2023, 4, 5)
        val input =
            LoadDataEntriesInput(
                permissionType = FitnessPermissionType.STEPS,
                packageName = null,
                displayedStartTime = stepsDate.toInstantAtStartOfDay(),
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
            )

        val stepsRecord = getStepsRecord(100, stepsDate.randomInstant())

        Mockito.doAnswer(prepareRecordsAnswer(listOf(stepsRecord)))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsRecord::class.java)
                },
                any(),
                any(),
            )

        Mockito.doAnswer(prepareRecordsAnswer(listOf()))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsCadenceRecord::class.java)
                },
                any(),
                any(),
            )

        val expectedFormattedEntry =
            healthDataEntryFormatter.format(stepsRecord, showDataOrigin = true)
        val result = loadDataEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(listOf(expectedFormattedEntry))
    }

    @Test
    fun invoke_whenLoadEntriesHelperUseCaseFails_returnsFailure() = runTest {
        val sleepDate = LocalDate.of(2021, 9, 13)

        Mockito.doAnswer(prepareFailureAnswer())
            .`when`(healthConnectManager)
            .readRecords<StepsRecord>(any(), any(), any())

        val input =
            LoadDataEntriesInput(
                permissionType = FitnessPermissionType.SLEEP,
                packageName = null,
                displayedStartTime = sleepDate.toInstantAtStartOfDay(),
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
            )

        val result = loadDataEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Failed).isTrue()
        assertThat((result as UseCaseResults.Failed).exception is HealthConnectException).isTrue()
        assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }

    private fun prepareRecordsAnswer(records: List<Record>): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<ReadRecordsResponse<Record>, *>
            receiver.onResult(ReadRecordsResponse(records, -1))
            null
        }
        return answer
    }

    private fun prepareFailureAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<List<LocalDate>, HealthConnectException>
            receiver.onError(HealthConnectException(HealthConnectException.ERROR_UNKNOWN))
            null
        }
        return answer
    }
}
