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
import android.health.connect.ReadRecordsRequest
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
import com.android.healthconnect.controller.tests.devices.api.FakeGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthconnect.controller.tests.utils.forDataType
import com.android.healthconnect.controller.tests.utils.fromDataSource
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.getStepsCadenceRecord
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
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadLatestEntryDateUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()
    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter
    @Inject lateinit var menstruationPeriodFormatter: MenstruationPeriodFormatter
    @Inject lateinit var dataSourceReader: MedicalDataSourceReader
    private val healthConnectManager: HealthConnectManager = mock()

    private lateinit var context: Context
    private lateinit var loadEntriesHelper: LoadEntriesHelper
    private lateinit var loadLatestEntryDateUseCase: LoadLatestEntryDateUseCase
    private val fakeGetCurrentDeviceIdUseCase =
        fakeUseCaseRule.watch(FakeGetCurrentDeviceIdUseCase())

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        loadEntriesHelper =
            LoadEntriesHelper(
                context,
                Dispatchers.Main,
                healthDataEntryFormatter,
                menstruationPeriodFormatter,
                healthConnectManager,
                dataSourceReader,
                fakeGetCurrentDeviceIdUseCase,
            )
        loadLatestEntryDateUseCase = LoadLatestEntryDateUseCase(Dispatchers.Main, loadEntriesHelper)
        if (deviceDataProvidersApi()) {
            fakeGetCurrentDeviceIdUseCase.updateDeviceId("deviceId")
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

        healthConnectManager.stub {
            on {
                readRecords<Record>(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsRecord::class.java
                    },
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult
                Result.success(ReadRecordsResponse(listOf(stepsRecordOld, stepsRecordNew), -1))

            on {
                readRecords<Record>(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsCadenceRecord::class.java
                    },
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

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

        healthConnectManager.stub {
            on {
                readRecords<Record>(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.fromDataSource(DEVICE_DATA_PROVIDER_PACKAGE_NAME) == true &&
                            request.forDataType(dataType = StepsRecord::class.java)
                    },
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(listOf(stepsRecordA), -1))

            on {
                readRecords<Record>(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.dataOrigins?.size == 0 &&
                            request.forDataType(dataType = StepsRecord::class.java)
                    },
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult
                Result.success(ReadRecordsResponse(listOf(stepsRecordA, stepsRecordB), -1))

            on {
                readRecords<Record>(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.forDataType(dataType = StepsCadenceRecord::class.java) == true
                    },
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(listOf(), -1))
        }

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

        healthConnectManager.stub {
            on {
                readRecords<Record>(
                    any<ReadRecordsRequest<Record>>(),
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        val result = loadLatestEntryDateUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEqualTo(currentTime.toInstant())
    }

    @Test
    fun invoke_whenLoadEntriesHelperUseCaseFails_returnsFailure() = runTest {
        val sleepDate = LocalDate.of(2021, 9, 13)

        healthConnectManager.stub {
            on {
                readRecords<Record>(
                    any<ReadRecordsRequest<Record>>(),
                    any<Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult
                Result.failure<ReadRecordsResponse<Record>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }

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

    @Test
    @Ignore("b/488075288")
    fun invoke_multipleDataTypes_returnsLatestDateAcrossAll() = runTest {
        val input =
            LoadLatestEntryDateInput(
                permissionType = FitnessPermissionType.STEPS,
                displayedStartTime = currentTime.toInstant(),
            )
        val stepsDate = LocalDate.of(2023, 10, 10)
        val stepsRecord = getStepsRecord(100, stepsDate.toInstantAtStartOfDay())
        val cadenceDate = LocalDate.of(2023, 10, 10)
        val cadenceRecord = getStepsCadenceRecord(cadenceDate.toInstantAtStartOfDay())

        healthConnectManager.stub {
            on {
                readRecords<Record>(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsRecord::class.java
                    },
                    any<Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(listOf(stepsRecord), -1))

            on {
                readRecords<Record>(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsCadenceRecord::class.java
                    },
                    any<Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(listOf(cadenceRecord), -1))
        }

        val result = loadLatestEntryDateUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEqualTo(cadenceDate)
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
