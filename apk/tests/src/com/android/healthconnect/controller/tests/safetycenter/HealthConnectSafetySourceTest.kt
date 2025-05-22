/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.safetycenter

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED
import android.safetycenter.SafetySourceData
import android.safetycenter.SafetySourceStatus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.safetycenter.HealthConnectSafetySource
import com.android.healthconnect.controller.safetycenter.HealthConnectSafetySource.Companion.HEALTH_CONNECT_SOURCE_ID
import com.android.healthconnect.controller.safetycenter.SafetyCenterManagerWrapper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class HealthConnectSafetySourceTest {

    private val mockSafetyCenterManagerWrapper = mock(SafetyCenterManagerWrapper::class.java)
    private lateinit var safetySource: HealthConnectSafetySource
    private lateinit var context: Context

    @Before
    fun setup() {
        safetySource = HealthConnectSafetySource(mockSafetyCenterManagerWrapper)
        context = InstrumentationRegistry.getInstrumentation().context

        whenever(mockSafetyCenterManagerWrapper.isEnabled(context)).thenReturn(true)
    }

    @Test
    fun setSafetySourceData_whenSafetyCenterIsDisabled_doesNotSetData() {
        whenever(mockSafetyCenterManagerWrapper.isEnabled(context)).thenReturn(false)

        safetySource.setSafetySourceData(context, EVENT_SOURCE_STATE_CHANGED)

        verify(mockSafetyCenterManagerWrapper, never())
            .setSafetySourceData(any(), any(), any(), any())
    }

    @Test
    fun setSafetySourceData_setsData() {
        safetySource.setSafetySourceData(context, EVENT_SOURCE_STATE_CHANGED)

        val expectedData =
            SafetySourceData.Builder()
                .setStatus(
                    SafetySourceStatus.Builder(
                            HEALTH_CONNECT_TITLE,
                            HEALTH_CONNECT_SUMMARY,
                            SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED,
                        )
                        .setPendingIntent(
                            PendingIntent.getActivity(
                                context,
                                /* requestCode= */ 0,
                                Intent(HEALTH_CONNECT_INTENT_ACTION),
                                FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
                            )
                        )
                        .build()
                )
                .build()

        verify(mockSafetyCenterManagerWrapper, times(1))
            .setSafetySourceData(
                context,
                HEALTH_CONNECT_SOURCE_ID,
                expectedData,
                EVENT_SOURCE_STATE_CHANGED,
            )
    }

    private fun <T> any(): T {
        return org.mockito.ArgumentMatchers.any()
    }

    companion object {
        private const val HEALTH_CONNECT_TITLE: String = "Health Connect"
        private const val HEALTH_CONNECT_SUMMARY: String = "Manage app access to health data"
        private const val HEALTH_CONNECT_INTENT_ACTION =
            "android.health.connect.action.HEALTH_HOME_SETTINGS"
        private val EVENT_SOURCE_STATE_CHANGED =
            SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build()
    }
}
