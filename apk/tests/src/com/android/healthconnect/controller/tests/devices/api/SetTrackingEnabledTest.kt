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
package com.android.healthconnect.controller.tests.devices.api

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.StepsRecord
import android.os.OutcomeReceiver
import android.platform.test.annotations.EnableFlags
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.devices.api.SetTrackingEnabledUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
class SetTrackingEnabledTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var setTrackingEnabled: SetTrackingEnabledUseCase
    private var manager: HealthConnectManager = mock()

    @Before
    fun setup() {
        hiltRule.inject()
        setTrackingEnabled = SetTrackingEnabledUseCase(manager, Dispatchers.IO)
        manager.stub {
            on { setTrackingEnabled(any(), any(), any(), any()) } doReturnResult
                Result.success<Void?>(null)
        }
    }

    @Test
    fun invoke_enableStepTracking_callsManagerWithCorrectParameters() {
        runBlocking {
            val input = SetTrackingEnabledUseCase.Input(StepsRecord::class.java, true)
            val result = setTrackingEnabled.invoke(input)

            assertThat(result is UseCaseResults.Success).isTrue()
            verify(manager)
                .setTrackingEnabled(
                    eq(StepsRecord::class.java),
                    eq(true),
                    any(),
                    any<OutcomeReceiver<Void, HealthConnectException>>(),
                )
        }
    }

    @Test
    fun invoke_disableStepTracking_callsManagerWithCorrectParameters() {
        runBlocking {
            val input = SetTrackingEnabledUseCase.Input(StepsRecord::class.java, false)
            val result = setTrackingEnabled.invoke(input)

            assertThat(result is UseCaseResults.Success).isTrue()
            verify(manager)
                .setTrackingEnabled(
                    eq(StepsRecord::class.java),
                    eq(false),
                    any(),
                    any<OutcomeReceiver<Void, HealthConnectException>>(),
                )
        }
    }

    @Test
    fun invoke_onException_returnsFailed() {
        runBlocking {
            val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
            manager.stub {
                on { setTrackingEnabled(any(), any(), any(), any()) } doReturnResult
                    Result.failure<Void?>(exception)
            }

            val input = SetTrackingEnabledUseCase.Input(StepsRecord::class.java, true)
            val result = setTrackingEnabled.invoke(input)

            assertThat(result is UseCaseResults.Failed).isTrue()
            assertThat((result as UseCaseResults.Failed).exception).isEqualTo(exception)
        }
    }
}
