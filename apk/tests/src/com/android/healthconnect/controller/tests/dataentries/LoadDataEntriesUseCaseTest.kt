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
import android.healthconnect.datatypes.Record
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.dataentries.LoadDataEntriesUseCase
import com.android.healthconnect.controller.tests.utils.CoroutineTestRule
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
@HiltAndroidTest
class LoadDataEntriesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val coroutineTestRule = CoroutineTestRule()

    @Inject lateinit var manager: HealthConnectManager
    @Inject lateinit var usecase: LoadDataEntriesUseCase

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    // TODO(magdi) uncomment tests when delete function is implemented

    //    @Test
    //    fun invoke_noData_returnsEmptyList() = runTest {
    //        launch {
    //            val response = usecase.invoke(STEPS, NOW)
    //            assertThat(response).isEmpty()
    //        }
    //    }

    //    @Test
    //    fun invoke_withData_returnsCorrectList() = runTest {
    //        val records = insertRecords(listOf(getStepsRecord(100)))
    //        launch {
    //            val response = usecase.invoke(STEPS, NOW)
    //            assertThat(response[0])
    //                .isEqualTo(
    //                    FormattedDataEntry(
    //                        uuid = records[0]!!.metadata.id,
    //                        header = "7:06 AM - 7:06 AM • Health Connect",
    //                        headerA11y = "from 7:06 AM to 7:06 AM • Health Connect",
    //                        title = "100 steps",
    //                        titleA11y = "100 steps"))
    //        }
    //    }

    @Throws(InterruptedException::class)
    private fun insertRecords(records: List<Record>): List<Record?> {
        val latch = CountDownLatch(1)
        assertThat(manager).isNotNull()
        val response: AtomicReference<List<Record>> = AtomicReference()
        manager.insertRecords(records, Executors.newSingleThreadExecutor()) { result ->
            response.set(result.records)
            latch.countDown()
        }
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true)
        assertThat(response.get()).hasSize(records.size)
        return response.get()
    }
}
