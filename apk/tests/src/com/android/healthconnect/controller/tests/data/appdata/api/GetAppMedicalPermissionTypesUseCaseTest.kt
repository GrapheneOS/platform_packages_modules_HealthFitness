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

package com.android.healthconnect.controller.tests.data.appdata.api

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.datatypes.MedicalResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.appdata.api.GetAppMedicalPermissionTypesUseCase
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GetAppMedicalPermissionTypesUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    private val healthConnectManager: HealthConnectManager = mock()
    private lateinit var getAppMedicalPermissionTypesUseCase: GetAppMedicalPermissionTypesUseCase

    @Before
    fun setup() = runTest {
        hiltRule.inject()
        getAppMedicalPermissionTypesUseCase =
            GetAppMedicalPermissionTypesUseCase(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun invoke_managerError_returnsFailure() = runTest {
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.failure<List<MedicalResourceTypeInfo>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }

        val result = getAppMedicalPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(HealthConnectException::class.java)
    }

    @Test
    fun invoke_emptyContributingDataSources_returnsEmptyList() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES, emptySet())
            )
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(medicalResourceTypeInfos)
        }

        val result = getAppMedicalPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).isEmpty()
    }

    @Test
    fun invoke_noMatchingContributingDataSources_returnsEmptyList() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(mock { on { packageName } doReturn "other.package" }),
                )
            )
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(medicalResourceTypeInfos)
        }

        val result = getAppMedicalPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).isEmpty()
    }

    @Test
    fun invoke_matchingContributingDataSources_returnsMultiplePermissionTypes() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                ),
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                ),
            )
        healthConnectManager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(medicalResourceTypeInfos)
        }

        val result = getAppMedicalPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(1)
        assertThat(data[0].category).isEqualTo(MEDICAL)
        assertThat(data[0].data)
            .containsExactly(
                MedicalPermissionType.VACCINES,
                MedicalPermissionType.ALLERGIES_INTOLERANCES,
            )
    }
}
