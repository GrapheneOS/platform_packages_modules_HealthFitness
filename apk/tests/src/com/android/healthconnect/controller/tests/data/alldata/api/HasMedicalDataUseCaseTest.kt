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

package com.android.healthconnect.controller.tests.data.alldata.api

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.datatypes.MedicalResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.alldata.api.HasMedicalDataUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HasMedicalDataUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    private val healthConnectManager: HealthConnectManager = mock()
    private lateinit var hasMedicalDataUseCase: HasMedicalDataUseCase

    @Before
    fun setup() {
        hiltRule.inject()
        hasMedicalDataUseCase = HasMedicalDataUseCase(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun invoke_managerError_returnsFailure() = runTest {
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.failure<List<MedicalResourceTypeInfo>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }

        val result = hasMedicalDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(HealthConnectException::class.java)
    }

    @Test
    fun invoke_noMedicalData_returnsFalse() = runTest {
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(emptyList<MedicalResourceTypeInfo>())
        }

        val result = hasMedicalDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isFalse()
    }

    @Test
    fun invoke_emptyContributingDataSources_returnsFalse() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES, emptySet())
            )
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(medicalResourceTypeInfos)
        }

        val result = hasMedicalDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isFalse()
    }

    @Test
    fun invoke_withContributingDataSources_returnsTrue() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                )
            )
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(medicalResourceTypeInfos)
        }

        val result = hasMedicalDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isTrue()
    }

    @Test
    fun invoke_someWithDataSources_returnsTrue() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                    emptySet(),
                ),
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                ),
            )
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(medicalResourceTypeInfos)
        }

        val result = hasMedicalDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isTrue()
    }
}
