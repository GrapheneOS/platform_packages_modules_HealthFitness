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

package com.android.healthconnect.controller.tests.devices

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.StepsRecord
import android.os.OutcomeReceiver
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.api.SetTrackingEnabledInput
import com.android.healthconnect.controller.matchmaking.api.SetTrackingEnabledUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SetTrackingEnabledUseCaseTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var setTrackingEnabledUseCase: SetTrackingEnabledUseCase
    private val healthConnectManager: HealthConnectManager = mock()

    @Before
    fun setup() {
        setTrackingEnabledUseCase =
            SetTrackingEnabledUseCase(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun execute_success_callsSetTrackingEnabled() = runTest {
        val input = SetTrackingEnabledInput(StepsRecord::class.java, true)

        whenever(healthConnectManager.setTrackingEnabled(any(), any(), any(), any())).thenAnswer {
            val receiver = it.arguments[3] as OutcomeReceiver<Void, HealthConnectException>
            receiver.onResult(null)
            null
        }

        val result = setTrackingEnabledUseCase.invoke(input)

        Truth.assertThat(result is UseCaseResults.Success).isTrue()
        verify(healthConnectManager)
            .setTrackingEnabled(eq(StepsRecord::class.java), eq(true), any(), any())
    }

    @Test
    fun execute_healthConnectException_returnsFailed() = runTest {
        val input = SetTrackingEnabledInput(StepsRecord::class.java, true)
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)

        whenever(healthConnectManager.setTrackingEnabled(any(), any(), any(), any())).thenAnswer {
            val receiver = it.arguments[3] as OutcomeReceiver<Void, HealthConnectException>
            receiver.onError(exception)
            null
        }

        val result = setTrackingEnabledUseCase.invoke(input) as UseCaseResults.Failed

        Truth.assertThat(result.exception).isEqualTo(exception)
    }
}
