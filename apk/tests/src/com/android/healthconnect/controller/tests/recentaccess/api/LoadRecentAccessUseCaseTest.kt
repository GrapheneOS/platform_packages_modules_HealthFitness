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

package com.android.healthconnect.controller.tests.recentaccess.api

import android.health.connect.Constants
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.accesslog.AccessLog
import android.health.connect.datatypes.RecordTypeIdentifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.recentaccess.api.LoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadRecentAccessUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    private lateinit var useCase: LoadRecentAccessUseCase
    private val timeSource = TestTimeSource

    private val healthConnectManager: HealthConnectManager = mock()

    @Before
    fun setup() {
        hiltRule.inject()
        useCase = LoadRecentAccessUseCase(healthConnectManager, Dispatchers.Main, timeSource)
    }

    @Test
    fun callsHealthConnectManager_success() = runTest {
        val accessLog1 =
            AccessLog(
                "pkg1",
                listOf(RecordTypeIdentifier.RECORD_TYPE_STEPS),
                timeSource.currentTimeMillis(),
                Constants.READ,
            )
        val accessLog2 =
            AccessLog(
                "pkg2",
                listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                timeSource.currentTimeMillis() - 1000,
                Constants.READ,
            )
        val logs = listOf(accessLog1, accessLog2)

        healthConnectManager.stub {
            on { queryAccessLogs(any(), any()) } doReturnResult Result.success(logs)
        }

        val result = useCase.invoke(Unit)
        assertThat(result is UseCaseResults.Success).isTrue()
        val data = (result as UseCaseResults.Success).data
        assertThat(data).isEqualTo(logs)
    }

    @Test
    fun callsHealthConnectManager_failure() = runTest {
        healthConnectManager.stub {
            on { queryAccessLogs(any(), any()) } doReturnResult
                Result.failure<List<AccessLog>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }

        val result = useCase.invoke(Unit)
        assertThat(result is UseCaseResults.Failed).isTrue()
    }

    @Test
    fun callsHealthConnectManager_accessLogOlderThan24Hours_doesNotReturn() = runTest {
        val now = timeSource.currentTimeMillis()
        val recentLog =
            AccessLog("pkg1", listOf(RecordTypeIdentifier.RECORD_TYPE_STEPS), now, Constants.READ)
        val oldLog =
            AccessLog(
                "pkg2",
                listOf(RecordTypeIdentifier.RECORD_TYPE_STEPS),
                now - Duration.ofDays(1).toMillis() - 1000,
                Constants.READ,
            )
        val logs = listOf(recentLog, oldLog)

        healthConnectManager.stub {
            on { queryAccessLogs(any(), any()) } doReturnResult Result.success(logs)
        }

        val result = useCase.invoke(Unit)
        assertThat(result is UseCaseResults.Success).isTrue()
        val data = (result as UseCaseResults.Success).data
        assertThat(data).containsExactly(recentLog)
    }
}
