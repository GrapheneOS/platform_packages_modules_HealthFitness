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

package com.android.healthconnect.controller.tests.matchmaking.api

import android.health.connect.HealthConnectManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class RecordMatchmakingDenialUseCaseTest {

    @get:Rule val hiltRule = dagger.hilt.android.testing.HiltAndroidRule(this)

    private lateinit var useCase: RecordMatchmakingDenialUseCase

    private val healthConnectManager: HealthConnectManager = mock()

    @Before
    fun setup() {
        hiltRule.inject()
        useCase = RecordMatchmakingDenialUseCase(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun invoke_callsMatchmakingManager() = runTest {
        val permissions = listOf("permission1", "permission2")
        useCase.invoke(
            RecordMatchmakingDenialInput(
                TEST_APP_PACKAGE_NAME,
                listOf(TEST_APP_PACKAGE_NAME_2),
                permissions,
            )
        )

        verify(healthConnectManager).recordMatchmakingDenial(any(), any(), any(), any(), any())
    }

    @Test
    fun invoke_healthConnectManagerThrowsException_returnsFailed() = runTest {
        val permissions = listOf("permission1", "permission2")
        whenever(healthConnectManager.recordMatchmakingDenial(any(), any(), any(), any(), any()))
            .doThrow(RuntimeException("test"))

        val result =
            useCase.invoke(
                RecordMatchmakingDenialInput(
                    TEST_APP_PACKAGE_NAME,
                    listOf(TEST_APP_PACKAGE_NAME_2),
                    permissions,
                )
            )

        Truth.assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
    }
}
