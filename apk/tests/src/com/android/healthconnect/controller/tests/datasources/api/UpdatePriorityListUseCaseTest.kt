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
package com.android.healthconnect.controller.tests.datasources.api

import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.UpdateDataOriginPriorityOrderRequest
import android.health.connect.datatypes.DataOrigin
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.datasources.api.UpdatePriorityListInput
import com.android.healthconnect.controller.datasources.api.UpdatePriorityListUseCase
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_3
import com.android.healthconnect.controller.tests.utils.doReturnResult
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
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UpdatePriorityListUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    private lateinit var useCase: UpdatePriorityListUseCase
    private val healthConnectManager: HealthConnectManager = mock()

    private val requestCaptor = argumentCaptor<UpdateDataOriginPriorityOrderRequest>()

    @Before
    fun setup() {
        hiltRule.inject()
        useCase = UpdatePriorityListUseCase(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun invoke_callsHealthConnectManager() = runTest {
        healthConnectManager.stub {
            on { updateDataOriginPriorityOrder(any(), any(), any()) } doReturnResult
                Result.success<Void?>(null)
        }

        val priorityList = listOf(TEST_APP_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_3)
        useCase.invoke(
            UpdatePriorityListInput(
                priorityList = priorityList,
                category = HealthDataCategory.ACTIVITY,
            )
        )
        val expectedPriorityList =
            priorityList
                .map { packageName -> DataOrigin.Builder().setPackageName(packageName).build() }
                .toList()

        verify(healthConnectManager)
            .updateDataOriginPriorityOrder(requestCaptor.capture(), any(), any())
        assertThat(requestCaptor.firstValue.dataCategory).isEqualTo(HealthDataCategory.ACTIVITY)
        assertThat(requestCaptor.firstValue.dataOriginInOrder).isEqualTo(expectedPriorityList)
    }
}
