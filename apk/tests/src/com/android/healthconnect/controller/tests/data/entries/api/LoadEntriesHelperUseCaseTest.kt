/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.data.entries.api

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.SleepSessionRecord
import android.os.OutcomeReceiver
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.dataentries.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.verifySleepSessionListsEqual
import com.android.healthconnect.controller.utils.atStartOfDay
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class LoadEntriesHelperUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)
    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter

    private lateinit var context: Context
    private lateinit var loadEntriesHelper: LoadEntriesHelper

    @Captor
    lateinit var requestCaptor: ArgumentCaptor<ReadRecordsRequestUsingFilters<SleepSessionRecord>>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        loadEntriesHelper =
            LoadEntriesHelper(context, healthDataEntryFormatter, healthConnectManager)
    }

    // TODO (b/309288325) add tests for other permission types

    @Test
    fun loadSleepData_withinDay_returnsListOfRecords_sortedByDescendingStartTime() = runTest {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        val startTime = Instant.parse("2023-06-12T22:30:00Z").atStartOfDay()
        val input =
            LoadDataEntriesInput(
                displayedStartTime = startTime,
                packageName = null,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
                permissionType = HealthPermissionType.SLEEP)

        val expectedTimeRangeFilter =
            loadEntriesHelper.getTimeFilter(startTime, DateNavigationPeriod.PERIOD_DAY, true)

        Mockito.doAnswer(prepareDaySleepAnswer())
            .`when`(healthConnectManager)
            .readRecords(
                ArgumentMatchers.any(ReadRecordsRequestUsingFilters::class.java),
                ArgumentMatchers.any(),
                ArgumentMatchers.any())

        val actual = loadEntriesHelper.readRecords(input)
        val expected =
            listOf(
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T22:30:00Z"),
                        Instant.parse("2023-06-13T07:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T21:00:00Z"),
                        Instant.parse("2023-06-12T21:20:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T16:00:00Z"),
                        Instant.parse("2023-06-12T17:45:00Z"))
                    .build(),
            )

        Mockito.verify(healthConnectManager, Mockito.times(1))
            .readRecords(requestCaptor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any())
        assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).startTime)
            .isEqualTo(expectedTimeRangeFilter.startTime)
        assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).endTime)
            .isEqualTo(expectedTimeRangeFilter.endTime)
        assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).isBounded)
            .isEqualTo(expectedTimeRangeFilter.isBounded)
        verifySleepSessionListsEqual(actual, expected)
    }

    @Test
    fun loadSleepDataUseCase_withinWeek_returnsListOfRecords_sortedByDescendingStartTime() =
        runTest {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

            val startTime = Instant.parse("2023-06-12T22:30:00Z").atStartOfDay()
            val input =
                LoadDataEntriesInput(
                    displayedStartTime = startTime,
                    packageName = null,
                    period = DateNavigationPeriod.PERIOD_WEEK,
                    showDataOrigin = true,
                    permissionType = HealthPermissionType.SLEEP)

            val expectedTimeRangeFilter =
                loadEntriesHelper.getTimeFilter(startTime, DateNavigationPeriod.PERIOD_WEEK, true)

            Mockito.doAnswer(prepareWeekSleepAnswer())
                .`when`(healthConnectManager)
                .readRecords(
                    ArgumentMatchers.any(ReadRecordsRequestUsingFilters::class.java),
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any())

            val actual = loadEntriesHelper.readRecords(input)
            val expected =
                listOf(
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-14T22:30:00Z"),
                            Instant.parse("2023-06-15T07:45:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-12T22:30:00Z"),
                            Instant.parse("2023-06-13T07:45:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-12T21:00:00Z"),
                            Instant.parse("2023-06-12T21:20:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-12T16:00:00Z"),
                            Instant.parse("2023-06-12T17:45:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-11T22:30:00Z"),
                            Instant.parse("2023-06-13T07:45:00Z"))
                        .build())

            Mockito.verify(healthConnectManager, Mockito.times(1))
                .readRecords(
                    requestCaptor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any())
            assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).startTime)
                .isEqualTo(expectedTimeRangeFilter.startTime)
            assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).endTime)
                .isEqualTo(expectedTimeRangeFilter.endTime)
            assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).isBounded)
                .isEqualTo(expectedTimeRangeFilter.isBounded)
            verifySleepSessionListsEqual(actual, expected)
        }

    @Test
    fun loadSleepDataUseCase_withinMonth_returnsListOfRecords_sortedByDescendingStartTime() =
        runTest {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

            val startTime = Instant.parse("2023-06-12T22:30:00Z").atStartOfDay()
            val input =
                LoadDataEntriesInput(
                    displayedStartTime = startTime,
                    packageName = null,
                    period = DateNavigationPeriod.PERIOD_MONTH,
                    showDataOrigin = true,
                    permissionType = HealthPermissionType.SLEEP)

            val expectedTimeRangeFilter =
                loadEntriesHelper.getTimeFilter(startTime, DateNavigationPeriod.PERIOD_MONTH, true)

            Mockito.doAnswer(prepareMonthSleepAnswer())
                .`when`(healthConnectManager)
                .readRecords(
                    ArgumentMatchers.any(ReadRecordsRequestUsingFilters::class.java),
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any())

            val actual = loadEntriesHelper.readRecords(input)
            val expected =
                listOf(
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-07-09T22:30:00Z"),
                            Instant.parse("2023-07-13T07:45:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-14T22:30:00Z"),
                            Instant.parse("2023-06-15T07:45:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-12T22:30:00Z"),
                            Instant.parse("2023-06-13T07:45:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-12T21:00:00Z"),
                            Instant.parse("2023-06-12T21:20:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-12T16:00:00Z"),
                            Instant.parse("2023-06-12T17:45:00Z"))
                        .build(),
                    SleepSessionRecord.Builder(
                            getMetaData(),
                            Instant.parse("2023-06-11T22:30:00Z"),
                            Instant.parse("2023-06-13T07:45:00Z"))
                        .build())

            Mockito.verify(healthConnectManager, Mockito.times(1))
                .readRecords(
                    requestCaptor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any())
            assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).startTime)
                .isEqualTo(expectedTimeRangeFilter.startTime)
            assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).endTime)
                .isEqualTo(expectedTimeRangeFilter.endTime)
            assertThat((requestCaptor.value.timeRangeFilter as TimeInstantRangeFilter).isBounded)
                .isEqualTo(expectedTimeRangeFilter.isBounded)
            verifySleepSessionListsEqual(actual, expected)
        }

    private fun prepareDaySleepAnswer():
        (InvocationOnMock) -> ReadRecordsResponse<SleepSessionRecord> {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<ReadRecordsResponse<SleepSessionRecord>, *>
            receiver.onResult(getDaySleepRecords())
            getDaySleepRecords()
        }
        return answer
    }

    private fun prepareWeekSleepAnswer():
        (InvocationOnMock) -> ReadRecordsResponse<SleepSessionRecord> {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<ReadRecordsResponse<SleepSessionRecord>, *>
            receiver.onResult(getWeekSleepRecords())
            getWeekSleepRecords()
        }
        return answer
    }

    private fun prepareMonthSleepAnswer():
        (InvocationOnMock) -> ReadRecordsResponse<SleepSessionRecord> {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<ReadRecordsResponse<SleepSessionRecord>, *>
            receiver.onResult(getMonthSleepRecords())
            getMonthSleepRecords()
        }
        return answer
    }

    private fun getDaySleepRecords(): ReadRecordsResponse<SleepSessionRecord> {
        return ReadRecordsResponse<SleepSessionRecord>(
            listOf(
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T22:30:00Z"),
                        Instant.parse("2023-06-13T07:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T21:00:00Z"),
                        Instant.parse("2023-06-12T21:20:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T16:00:00Z"),
                        Instant.parse("2023-06-12T17:45:00Z"))
                    .build()),
            -1)
    }

    private fun getWeekSleepRecords(): ReadRecordsResponse<SleepSessionRecord> {
        return ReadRecordsResponse<SleepSessionRecord>(
            listOf(
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T22:30:00Z"),
                        Instant.parse("2023-06-13T07:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T21:00:00Z"),
                        Instant.parse("2023-06-12T21:20:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T16:00:00Z"),
                        Instant.parse("2023-06-12T17:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-11T22:30:00Z"),
                        Instant.parse("2023-06-13T07:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-14T22:30:00Z"),
                        Instant.parse("2023-06-15T07:45:00Z"))
                    .build()),
            -1)
    }

    private fun getMonthSleepRecords(): ReadRecordsResponse<SleepSessionRecord> {
        return ReadRecordsResponse<SleepSessionRecord>(
            listOf(
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T22:30:00Z"),
                        Instant.parse("2023-06-13T07:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T21:00:00Z"),
                        Instant.parse("2023-06-12T21:20:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-12T16:00:00Z"),
                        Instant.parse("2023-06-12T17:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-11T22:30:00Z"),
                        Instant.parse("2023-06-13T07:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-06-14T22:30:00Z"),
                        Instant.parse("2023-06-15T07:45:00Z"))
                    .build(),
                SleepSessionRecord.Builder(
                        getMetaData(),
                        Instant.parse("2023-07-09T22:30:00Z"),
                        Instant.parse("2023-07-13T07:45:00Z"))
                    .build()),
            -1)
    }
}
