/*
 * Copyright (C) 2025 The Android Open Source Project
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
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadLatestEntryDateInput
import com.android.healthconnect.controller.data.entries.api.LoadLatestEntryDateUseCase
import com.android.healthconnect.controller.data.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.data.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.forDataType
import com.android.healthconnect.controller.tests.utils.fromDataSource
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.getStepsRecord
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.toInstant
import com.android.healthconnect.controller.utils.toInstantAtStartOfDay
import com.android.healthconnect.controller.utils.toLocalDate
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadLatestEntryDateUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter
    @Inject lateinit var menstruationPeriodFormatter: MenstruationPeriodFormatter
    @Inject lateinit var dataSourceReader: MedicalDataSourceReader
    private val healthConnectManager: HealthConnectManager = mock<HealthConnectManager>()

    private lateinit var context: Context
    private lateinit var loadEntriesHelper: LoadEntriesHelper
    private lateinit var loadLatestEntryDateUseCase: LoadLatestEntryDateUseCase

    @Before
    fun setup() {
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
            )
        loadLatestEntryDateUseCase = LoadLatestEntryDateUseCase(Dispatchers.Main, loadEntriesHelper)
        if (deviceDataProvidersApi()) {
            whenever(healthConnectManager.currentDeviceId).thenReturn("deviceId")
        }
    }

    @Test
    fun invoke_returnsLatestDate() = runTest {
        val input =
            LoadLatestEntryDateInput(
                permissionType = FitnessPermissionType.STEPS,
                displayedStartTime = currentTime.toInstant(),
            )
        val stepsDateOld = LocalDate.of(2021, 9, 13)
        val stepsRecordOld = getStepsRecord(100, stepsDateOld.toInstantAtStartOfDay())
        val stepsDateNew = LocalDate.of(2023, 10, 14)
        val stepsRecordNew = getStepsRecord(100, stepsDateNew.toInstantAtStartOfDay())

        doAnswer(prepareRecordsAnswer(listOf(stepsRecordOld, stepsRecordNew)))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsRecord::class.java)
                },
                any(),
                any(),
            )

        doAnswer(prepareRecordsAnswer(listOf()))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsCadenceRecord::class.java)
                },
                any(),
                any(),
            )

        val result = loadLatestEntryDateUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data.toLocalDate().year).isEqualTo(2023)
        assertThat(result.data.toLocalDate().dayOfYear).isEqualTo(stepsDateNew.dayOfYear)
    }

    @Test
    fun invoke_withPackageName_readsRecordsFilteringPackageName() = runTest {
        val input =
            LoadLatestEntryDateInput(
                permissionType = FitnessPermissionType.STEPS,
                displayedStartTime = currentTime.toInstant(),
                packageName = DEVICE_DATA_PROVIDER_PACKAGE_NAME,
            )
        val stepsDateA = LocalDate.of(2021, 9, 13)
        val stepsRecordA =
            getStepsRecordWithPackage(
                steps = 100,
                time = stepsDateA.toInstantAtStartOfDay(),
                packageName = DEVICE_DATA_PROVIDER_PACKAGE_NAME,
            )
        val stepsDateB = LocalDate.of(2023, 10, 14)
        val stepsRecordB =
            getStepsRecordWithPackage(
                steps = 100,
                time = stepsDateB.toInstantAtStartOfDay(),
                packageName = TEST_APP_PACKAGE_NAME,
            )

        doAnswer(prepareRecordsAnswer(listOf(stepsRecordA)))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.fromDataSource(DEVICE_DATA_PROVIDER_PACKAGE_NAME) &&
                        request.forDataType(dataType = StepsRecord::class.java)
                },
                any(),
                any(),
            )

        doAnswer(prepareRecordsAnswer(listOf(stepsRecordA, stepsRecordB)))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.dataOrigins?.size == 0 &&
                        request.forDataType(dataType = StepsRecord::class.java)
                },
                any(),
                any(),
            )

        doAnswer(prepareRecordsAnswer(listOf()))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsCadenceRecord::class.java)
                },
                any(),
                any(),
            )

        val result = loadLatestEntryDateUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data.toLocalDate()).isEqualTo(stepsDateA)
    }

    @Test
    fun invoke_returnsGivenDateForNoRecords() = runTest {
        val input =
            LoadLatestEntryDateInput(
                permissionType = FitnessPermissionType.STEPS,
                displayedStartTime = currentTime.toInstant(),
            )

        doAnswer(prepareRecordsAnswer(listOf()))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsRecord::class.java)
                },
                any(),
                any(),
            )

        doAnswer(prepareRecordsAnswer(listOf()))
            .`when`(healthConnectManager)
            .readRecords(
                argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsCadenceRecord::class.java)
                },
                any(),
                any(),
            )

        val result = loadLatestEntryDateUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEqualTo(currentTime.toInstant())
    }

    @Test
    fun invoke_whenLoadEntriesHelperUseCaseFails_returnsFailure() = runTest {
        val sleepDate = LocalDate.of(2021, 9, 13)

        doAnswer(prepareFailureAnswer())
            .`when`(healthConnectManager)
            .readRecords<StepsRecord>(any(), any(), any())

        val input =
            LoadLatestEntryDateInput(
                permissionType = FitnessPermissionType.STEPS,
                displayedStartTime = sleepDate.toInstantAtStartOfDay(),
            )

        val result = loadLatestEntryDateUseCase.invoke(input)
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

    private fun getStepsRecordWithPackage(
        steps: Long,
        time: Instant = NOW,
        packageName: String,
    ): StepsRecord {
        return StepsRecord.Builder(getMetaData(packageName), time, time.plusSeconds(2), steps)
            .build()
    }
}
