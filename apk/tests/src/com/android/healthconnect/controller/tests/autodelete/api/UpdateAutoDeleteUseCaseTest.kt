/**
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.autodelete.api

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.autodelete.api.UpdateAutoDeleteUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UpdateAutoDeleteUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private val healthConnectManager: HealthConnectManager = mock()

    private lateinit var updateAutoDeleteUseCase: UpdateAutoDeleteUseCase

    private val captor = argumentCaptor<Int>()

    @Before
    fun setup() {
        hiltRule.inject()
        updateAutoDeleteUseCase = UpdateAutoDeleteUseCase(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun updateAutoDeleteUseCase_3months_callsManagerWithCorrectArgs() = runTest {
        healthConnectManager.stub {
            on { setRecordRetentionPeriodInDays(any(), any(), any()) } doReturnResult
                Result.success<Void?>(null)
        }

        val result = updateAutoDeleteUseCase.invoke(3)

        verify(healthConnectManager, times(1))
            .setRecordRetentionPeriodInDays(captor.capture(), any(), any())
        assertThat(captor.firstValue).isEqualTo(90)
        assertThat(result is UseCaseResults.Success)
    }

    @Test
    fun updateAutoDeleteUseCase_18months_callsManagerWithCorrectArgs() = runTest {
        healthConnectManager.stub {
            on { setRecordRetentionPeriodInDays(any(), any(), any()) } doReturnResult
                Result.success<Void?>(null)
        }

        val result = updateAutoDeleteUseCase.invoke(18)

        verify(healthConnectManager, times(1))
            .setRecordRetentionPeriodInDays(captor.capture(), any(), any())
        assertThat(captor.firstValue).isEqualTo(540)
        assertThat(result is UseCaseResults.Success)
    }

    @Test
    fun updateAutoDeleteUseCase_0months_callsManagerWithCorrectArgs() = runTest {
        healthConnectManager.stub {
            on { setRecordRetentionPeriodInDays(any(), any(), any()) } doReturnResult
                Result.success<Void?>(null)
        }

        val result = updateAutoDeleteUseCase.invoke(0)

        verify(healthConnectManager, times(1))
            .setRecordRetentionPeriodInDays(captor.capture(), any(), any())
        assertThat(captor.firstValue).isEqualTo(0)
        assertThat(result is UseCaseResults.Success)
    }

    @Test
    fun updateAutoDeleteUseCase_whenSetRecordRetentionFails_returnsFailure() = runTest {
        healthConnectManager.stub {
            on { setRecordRetentionPeriodInDays(any(), any(), any()) } doReturnResult
                Result.failure<Void?>(HealthConnectException(HealthConnectException.ERROR_UNKNOWN))
        }
        val result = updateAutoDeleteUseCase.invoke(1)
        assertThat(result is UseCaseResults.Failed).isTrue()
        assertThat((result as UseCaseResults.Failed).exception is HealthConnectException).isTrue()
        Truth.assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }
}
