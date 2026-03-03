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

import android.health.connect.HealthConnectManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.devices.api.GetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.devices.api.IGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GetCurrentDeviceIdUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private val healthConnectManager: HealthConnectManager = mock()
    private lateinit var useCase: IGetCurrentDeviceIdUseCase

    @Before
    fun setup() {
        useCase = GetCurrentDeviceIdUseCase(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun invoke_success_callsCurrentDeviceId() = runTest {
        whenever(healthConnectManager.currentDeviceId).thenReturn("some id")

        val result = useCase(Unit)

        Truth.assertThat(result is UseCaseResults.Success).isTrue()
        verify(healthConnectManager).currentDeviceId
    }

    @Test
    fun invoke_cachesResult() = runTest {
        val expectedDeviceId = "test_device_id"
        whenever(healthConnectManager.currentDeviceId).thenReturn(expectedDeviceId)

        val result1 = (useCase(Unit) as UseCaseResults.Success).data
        Truth.assertThat(result1).isEqualTo(expectedDeviceId)

        whenever(healthConnectManager.currentDeviceId).thenReturn("some new impossible id")

        val result2 = (useCase(Unit) as UseCaseResults.Success).data
        Truth.assertThat(result2).isEqualTo(expectedDeviceId)

        verify(healthConnectManager, times(1)).currentDeviceId
    }
}
