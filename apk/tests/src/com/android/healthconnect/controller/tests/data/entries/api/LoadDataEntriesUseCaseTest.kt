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
import android.health.connect.ReadRecordsRequest
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
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
import com.android.healthconnect.controller.tests.utils.doReturnResult
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
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadDataEntriesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()
    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter
    @Inject lateinit var menstruationPeriodFormatter: MenstruationPeriodFormatter
    @Inject lateinit var dataSourceReader: MedicalDataSourceReader

    private val healthConnectManager: HealthConnectManager = mock()

    private lateinit var context: Context
    private lateinit var loadEntriesHelper: LoadEntriesHelper
    private lateinit var loadDataEntriesUseCase: LoadDataEntriesUseCase
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

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsRecord::class.java
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(listOf(stepsRecord), -1))

            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsCadenceRecord::class.java
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

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

        healthConnectManager.stub {
            on { readRecords(any<ReadRecordsRequest<Record>>(), any(), any()) } doReturnResult
                Result.failure<ReadRecordsResponse<Record>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }

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

    @Test
    fun invoke_withShowDataOriginFalse_returnsFormattedDataWithoutOrigin() = runTest {
        val stepsDate = LocalDate.of(2023, 4, 5)
        val input =
            LoadDataEntriesInput(
                permissionType = FitnessPermissionType.STEPS,
                packageName = null,
                displayedStartTime = stepsDate.toInstantAtStartOfDay(),
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = false, // Setting to false to verify the change
            )

        val stepsRecord = getStepsRecord(100, stepsDate.randomInstant())

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsRecord::class.java
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(listOf(stepsRecord), -1))

            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsCadenceRecord::class.java
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        val expectedFormattedEntry =
            healthDataEntryFormatter.format(stepsRecord, showDataOrigin = false)
        val result = loadDataEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(listOf(expectedFormattedEntry))
    }

    @Test
    fun invoke_withPeriodWeek_addsDateSectionHeaders() = runTest {
        val todayDate = LocalDate.now()
        val yesterdayDate = todayDate.minusDays(1)

        val input =
            LoadDataEntriesInput(
                permissionType = FitnessPermissionType.STEPS,
                packageName = null,
                displayedStartTime = yesterdayDate.toInstantAtStartOfDay(),
                period = DateNavigationPeriod.PERIOD_WEEK,
                showDataOrigin = true,
            )

        val recordToday = getStepsRecord(100, todayDate.toInstantAtStartOfDay().plusMillis(1000))
        val recordYesterday =
            getStepsRecord(200, yesterdayDate.toInstantAtStartOfDay().plusMillis(1000))

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsRecord::class.java
                    },
                    any(),
                    any(),
                )
            } doReturnResult
                Result.success(ReadRecordsResponse(listOf(recordToday, recordYesterday), -1))

            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request?.recordType == StepsCadenceRecord::class.java
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        val result = loadDataEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()

        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(4) // 2 headers + 2 records
        assertThat(data[0]).isInstanceOf(FormattedEntry.EntryDateSectionHeader::class.java)
        assertThat((data[0] as FormattedEntry.EntryDateSectionHeader).date)
            .isEqualTo(context.getString(R.string.today_header))
        assertThat(data[1]).isEqualTo(healthDataEntryFormatter.format(recordToday, true))

        assertThat(data[2]).isInstanceOf(FormattedEntry.EntryDateSectionHeader::class.java)
        assertThat((data[2] as FormattedEntry.EntryDateSectionHeader).date)
            .isEqualTo(context.getString(R.string.yesterday_header))
        assertThat(data[3]).isEqualTo(healthDataEntryFormatter.format(recordYesterday, true))
    }
}
