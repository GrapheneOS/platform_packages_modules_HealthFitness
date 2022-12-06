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
package com.android.healthconnect.controller.tests.dataentries

import android.healthconnect.HealthConnectManager
import android.healthconnect.ReadRecordsRequestUsingFilters
import android.healthconnect.ReadRecordsResponse
import android.healthconnect.datatypes.HeartRateRecord
import android.os.OutcomeReceiver
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.dataentries.FormattedDataEntry
import com.android.healthconnect.controller.dataentries.LoadDataEntriesInput
import com.android.healthconnect.controller.dataentries.LoadDataEntriesUseCase
import com.android.healthconnect.controller.dataentries.formatters.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.HEART_RATE
import com.android.healthconnect.controller.shared.DataType
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.CoroutineTestRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getHeartRateRecord
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Matchers.any
import org.mockito.Mockito.*
import org.mockito.invocation.InvocationOnMock

@ExperimentalCoroutinesApi
@HiltAndroidTest
class LoadDataEntriesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val coroutineTestRule = CoroutineTestRule()

    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)
    @Inject lateinit var formatter: HealthDataEntryFormatter

    lateinit var usecase: LoadDataEntriesUseCase

    @Before
    fun setup() {
        hiltRule.inject()
        InstrumentationRegistry.getInstrumentation().context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        usecase = LoadDataEntriesUseCase(formatter, manager, Dispatchers.Main)
    }

    @Test
    fun invoke_noData_returnsEmptyList() = runTest {
        doAnswer(prepareAnswer(emptyList()))
            .`when`(manager)
            .readRecords(any(ReadRecordsRequestUsingFilters::class.java), any(), any())

        val response = usecase.invoke(LoadDataEntriesInput(HEART_RATE, NOW))

        assertThat(response).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((response as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_withData_returnsCorrectList() = runTest {
        val records = listOf(getHeartRateRecord(listOf(100)))
        doAnswer(prepareAnswer(records))
            .`when`(manager)
            .readRecords(any(ReadRecordsRequestUsingFilters::class.java), any(), any())

        val response = usecase.invoke(LoadDataEntriesInput(HEART_RATE, NOW))

        assertThat(response).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (response as UseCaseResults.Success).data
        assertThat(data.size).isEqualTo(1)
        assertThat(data)
            .containsExactly(
                FormattedDataEntry(
                    uuid = records[0].metadata.id,
                    header = "7:06 AM - 7:06 AM • Health Connect test app",
                    headerA11y = "from 7:06 AM to 7:06 AM • Health Connect test app",
                    title = "100 bpm",
                    titleA11y = "100 beats per minute",
                    dataType = DataType.HEART_RATE),
            )
    }

    private fun prepareAnswer(records: List<HeartRateRecord>): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<ReadRecordsResponse<HeartRateRecord>, *>
            receiver.onResult(ReadRecordsResponse(records))
            null
        }
        return answer
    }
}
