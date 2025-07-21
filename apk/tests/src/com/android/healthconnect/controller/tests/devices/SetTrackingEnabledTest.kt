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
package com.android.healthconnect.controller.tests.devices

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.StepsRecord
import android.os.OutcomeReceiver
import android.platform.test.annotations.EnableFlags
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.devices.SetTrackingEnabled
import com.android.healthfitness.flags.Flags
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
class SetTrackingEnabledTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var setTrackingEnabled: SetTrackingEnabled
    private var manager: HealthConnectManager = mock()

    @Before
    fun setup() {
        hiltRule.inject()
        setTrackingEnabled = SetTrackingEnabled(manager, Dispatchers.IO)
        runBlocking {
            whenever(manager.setTrackingEnabled(any(), any(), any(), any())).thenAnswer { invocation
                ->
                val receiver =
                    invocation.getArgument<OutcomeReceiver<Void, HealthConnectException>>(3)
                receiver.onResult(null)
                null
            }
        }
    }

    @Test
    fun execute_enableStepTracking_callsManagerWithCorrectParameters() {
        runBlocking {
            val input = SetTrackingEnabled.Input(StepsRecord::class.java, true)
            setTrackingEnabled.execute(input)

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
    fun execute_disableStepTracking_callsManagerWithCorrectParameters() {
        runBlocking {
            val input = SetTrackingEnabled.Input(StepsRecord::class.java, false)
            setTrackingEnabled.execute(input)

            verify(manager)
                .setTrackingEnabled(
                    eq(StepsRecord::class.java),
                    eq(false),
                    any(),
                    any<OutcomeReceiver<Void, HealthConnectException>>(),
                )
        }
    }
}
