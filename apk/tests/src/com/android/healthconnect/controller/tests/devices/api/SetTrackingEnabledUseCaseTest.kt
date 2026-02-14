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

package com.android.healthconnect.controller.tests.devices.api

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.StepsRecord
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.api.SetTrackingEnabledInput
import com.android.healthconnect.controller.matchmaking.api.SetTrackingEnabledUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthfitness.flags.Flags
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
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
    fun invoke_success_callsSetTrackingEnabled() = runTest {
        val input = SetTrackingEnabledInput(StepsRecord::class.java, true)

        healthConnectManager.stub {
            on { setTrackingEnabled(any(), any(), any(), any()) } doReturnResult
                Result.success<Void?>(null)
        }

        val result = setTrackingEnabledUseCase.invoke(input)

        Truth.assertThat(result is UseCaseResults.Success).isTrue()
        verify(healthConnectManager)
            .setTrackingEnabled(eq(StepsRecord::class.java), eq(true), any(), any())
    }

    @Test
    fun invoke_healthConnectException_returnsFailed() = runTest {
        val input = SetTrackingEnabledInput(StepsRecord::class.java, true)
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)

        healthConnectManager.stub {
            on { setTrackingEnabled(any(), any(), any(), any()) } doReturnResult
                Result.failure<Void?>(exception)
        }

        val result = setTrackingEnabledUseCase.invoke(input) as UseCaseResults.Failed

        Truth.assertThat(result.exception).isEqualTo(exception)
    }
}
