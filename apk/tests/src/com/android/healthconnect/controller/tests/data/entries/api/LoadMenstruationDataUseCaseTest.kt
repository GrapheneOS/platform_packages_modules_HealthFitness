/*
 * Copyright (C) 2024 The Android Open Source Project
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
 *
 *
 */
package com.android.healthconnect.controller.tests.data.entries.api

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.Record
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataInput
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.forDataType
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Duration.ofDays
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class LoadMenstruationDataUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var loadMenstruationDataUseCase: LoadMenstruationDataUseCase

    @Inject lateinit var loadEntriesHelper: LoadEntriesHelper

    @BindValue
    val healthConnectManager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        loadMenstruationDataUseCase =
            LoadMenstruationDataUseCase(loadEntriesHelper, Dispatchers.Main)
    }

    @Test
    fun invoke_noData_returnsEmptyList() = runTest {
        Mockito.doAnswer(prepareRecordsAnswer(listOf()))
            .`when`(healthConnectManager)
            .readRecords<MenstruationFlowRecord>(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )

        Mockito.doAnswer(prepareRecordsAnswer(listOf()))
            .`when`(healthConnectManager)
            .readRecords<MenstruationPeriodRecord>(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )

        val input =
            LoadMenstruationDataInput(
                packageName = TEST_APP_PACKAGE_NAME,
                displayedStartTime = NOW,
                period = DateNavigationPeriod.PERIOD_MONTH,
                showDataOrigin = true,
            )

        val result = loadMenstruationDataUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_periodDay_returnsFormattedData() = runTest {
        val menstruationPeriodRecords =
            listOf(
                MenstruationPeriodRecord.Builder(getMetaData(), NOW, NOW.plus(ofDays(5))).build(),
                MenstruationPeriodRecord.Builder(
                        getMetaData(),
                        NOW.minus(ofDays(1)),
                        NOW.plus(ofDays(2)),
                    )
                    .build(),
            )
        val menstruationFlowRecords =
            listOf(
                MenstruationFlowRecord.Builder(
                        getMetaData(),
                        NOW,
                        MenstruationFlowRecord.MenstruationFlowType.FLOW_HEAVY,
                    )
                    .build()
            )
        Mockito.doAnswer(prepareRecordsAnswer(menstruationPeriodRecords))
            .`when`(healthConnectManager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = MenstruationPeriodRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )
        Mockito.doAnswer(prepareRecordsAnswer(menstruationFlowRecords))
            .`when`(healthConnectManager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = MenstruationFlowRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )

        val input =
            LoadMenstruationDataInput(
                packageName = TEST_APP_PACKAGE_NAME,
                displayedStartTime = NOW,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
            )

        val result = loadMenstruationDataUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "Oct 20 – 25 • $TEST_APP_NAME",
                        headerA11y = "Oct 20 – 25 • $TEST_APP_NAME",
                        title = "Period day 1 of 6",
                        titleA11y = "Period day 1 of 6",
                        dataType = MenstruationPeriodRecord::class,
                        startTime = NOW,
                        endTime = NOW.plus(ofDays(5)),
                    ),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "Oct 19 – 22 • $TEST_APP_NAME",
                        headerA11y = "Oct 19 – 22 • $TEST_APP_NAME",
                        title = "Period day 2 of 4",
                        titleA11y = "Period day 2 of 4",
                        dataType = MenstruationPeriodRecord::class,
                        startTime = NOW.minus(ofDays(1)),
                        endTime = NOW.plus(ofDays(2)),
                    ),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "7:06 AM • $TEST_APP_NAME",
                        headerA11y = "7:06 AM • $TEST_APP_NAME",
                        title = "Heavy flow",
                        titleA11y = "Heavy flow",
                        dataType = MenstruationFlowRecord::class,
                    ),
                )
            )
    }

    @Test
    fun invoke_periodWeek_returnsFormattedData() = runTest {
        // NOW is on a Thursday
        val menstruationPeriodRecords =
            listOf(
                MenstruationPeriodRecord.Builder(getMetaData(), NOW, NOW.plus(ofDays(5))).build(),
                MenstruationPeriodRecord.Builder(
                        getMetaData(),
                        NOW.minus(ofDays(1)),
                        NOW.plus(ofDays(2)),
                    )
                    .build(),
            )
        val menstruationFlowRecords =
            listOf(
                MenstruationFlowRecord.Builder(
                        getMetaData(),
                        NOW,
                        MenstruationFlowRecord.MenstruationFlowType.FLOW_HEAVY,
                    )
                    .build(),
                MenstruationFlowRecord.Builder(
                        getMetaData(),
                        NOW.minus(ofDays(2)),
                        MenstruationFlowRecord.MenstruationFlowType.FLOW_LIGHT,
                    )
                    .build(),
            )
        Mockito.doAnswer(prepareRecordsAnswer(menstruationPeriodRecords))
            .`when`(healthConnectManager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = MenstruationPeriodRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )
        Mockito.doAnswer(prepareRecordsAnswer(menstruationFlowRecords))
            .`when`(healthConnectManager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = MenstruationFlowRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )

        val input =
            LoadMenstruationDataInput(
                packageName = TEST_APP_PACKAGE_NAME,
                displayedStartTime = NOW,
                period = DateNavigationPeriod.PERIOD_WEEK,
                showDataOrigin = true,
            )

        val result = loadMenstruationDataUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    FormattedEntry.EntryDateSectionHeader(date = "Today"),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "Oct 20 – 25 • $TEST_APP_NAME",
                        headerA11y = "Oct 20 – 25 • $TEST_APP_NAME",
                        title = "Period (6 days)",
                        titleA11y = "Period (6 days)",
                        dataType = MenstruationPeriodRecord::class,
                        startTime = NOW,
                        endTime = NOW.plus(ofDays(5)),
                    ),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "7:06 AM • $TEST_APP_NAME",
                        headerA11y = "7:06 AM • $TEST_APP_NAME",
                        title = "Heavy flow",
                        titleA11y = "Heavy flow",
                        dataType = MenstruationFlowRecord::class,
                    ),
                    FormattedEntry.EntryDateSectionHeader(date = "Yesterday"),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "Oct 19 – 22 • $TEST_APP_NAME",
                        headerA11y = "Oct 19 – 22 • $TEST_APP_NAME",
                        title = "Period (4 days)",
                        titleA11y = "Period (4 days)",
                        dataType = MenstruationPeriodRecord::class,
                        startTime = NOW.minus(ofDays(1)),
                        endTime = NOW.plus(ofDays(2)),
                    ),
                    FormattedEntry.EntryDateSectionHeader(date = "October 18, 2022"),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "7:06 AM • $TEST_APP_NAME",
                        headerA11y = "7:06 AM • $TEST_APP_NAME",
                        title = "Light flow",
                        titleA11y = "Light flow",
                        dataType = MenstruationFlowRecord::class,
                    ),
                )
            )
    }

    @Test
    fun invoke_periodMonth_returnsFormattedData() = runTest {
        val menstruationPeriodRecords =
            listOf(
                MenstruationPeriodRecord.Builder(getMetaData(), NOW, NOW.plus(ofDays(5))).build(),
                MenstruationPeriodRecord.Builder(
                        getMetaData(),
                        NOW.minus(ofDays(1)),
                        NOW.plus(ofDays(2)),
                    )
                    .build(),
                MenstruationPeriodRecord.Builder(
                        getMetaData(),
                        NOW.minus(ofDays(10)),
                        NOW.minus(ofDays(10)),
                    )
                    .build(),
            )
        val menstruationFlowRecords =
            listOf(
                MenstruationFlowRecord.Builder(
                        getMetaData(),
                        NOW,
                        MenstruationFlowRecord.MenstruationFlowType.FLOW_HEAVY,
                    )
                    .build(),
                MenstruationFlowRecord.Builder(
                        getMetaData(),
                        NOW.minus(ofDays(2)),
                        MenstruationFlowRecord.MenstruationFlowType.FLOW_LIGHT,
                    )
                    .build(),
            )
        Mockito.doAnswer(prepareRecordsAnswer(menstruationPeriodRecords))
            .`when`(healthConnectManager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = MenstruationPeriodRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )
        Mockito.doAnswer(prepareRecordsAnswer(menstruationFlowRecords))
            .`when`(healthConnectManager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = MenstruationFlowRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )

        val input =
            LoadMenstruationDataInput(
                packageName = TEST_APP_PACKAGE_NAME,
                displayedStartTime = NOW,
                period = DateNavigationPeriod.PERIOD_WEEK,
                showDataOrigin = true,
            )

        val result = loadMenstruationDataUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    FormattedEntry.EntryDateSectionHeader(date = "Today"),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "Oct 20 – 25 • $TEST_APP_NAME",
                        headerA11y = "Oct 20 – 25 • $TEST_APP_NAME",
                        title = "Period (6 days)",
                        titleA11y = "Period (6 days)",
                        dataType = MenstruationPeriodRecord::class,
                        startTime = NOW,
                        endTime = NOW.plus(ofDays(5)),
                    ),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "7:06 AM • $TEST_APP_NAME",
                        headerA11y = "7:06 AM • $TEST_APP_NAME",
                        title = "Heavy flow",
                        titleA11y = "Heavy flow",
                        dataType = MenstruationFlowRecord::class,
                    ),
                    FormattedEntry.EntryDateSectionHeader(date = "Yesterday"),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "Oct 19 – 22 • $TEST_APP_NAME",
                        headerA11y = "Oct 19 – 22 • $TEST_APP_NAME",
                        title = "Period (4 days)",
                        titleA11y = "Period (4 days)",
                        dataType = MenstruationPeriodRecord::class,
                        startTime = NOW.minus(ofDays(1)),
                        endTime = NOW.plus(ofDays(2)),
                    ),
                    FormattedEntry.EntryDateSectionHeader(date = "October 18, 2022"),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "7:06 AM • $TEST_APP_NAME",
                        headerA11y = "7:06 AM • $TEST_APP_NAME",
                        title = "Light flow",
                        titleA11y = "Light flow",
                        dataType = MenstruationFlowRecord::class,
                    ),
                    FormattedEntry.EntryDateSectionHeader(date = "October 10, 2022"),
                    FormattedEntry.FormattedDataEntry(
                        uuid = "test_id",
                        header = "October 10 • $TEST_APP_NAME",
                        headerA11y = "October 10 • $TEST_APP_NAME",
                        title = "Period (1 day)",
                        titleA11y = "Period (1 day)",
                        dataType = MenstruationPeriodRecord::class,
                        startTime = NOW.minus(ofDays(10)),
                        endTime = NOW.minus(ofDays(10)),
                    ),
                )
            )
    }

    private fun prepareRecordsAnswer(records: List<Record>): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<ReadRecordsResponse<Record>, *>
            receiver.onResult(ReadRecordsResponse(records, -1))
            null
        }
        return answer
    }
}
