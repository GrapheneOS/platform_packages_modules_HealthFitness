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
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.alldata.api.GetFitnessPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthfitness.flags.Flags
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
class GetFitnessPermissionTypesWithDataUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val healthConnectManager: HealthConnectManager = mock()
    private lateinit var getFitnessPermissionTypesWithDataUseCase:
        GetFitnessPermissionTypesWithDataUseCase

    @Before
    fun setup() {
        hiltRule.inject()
        getFitnessPermissionTypesWithDataUseCase =
            GetFitnessPermissionTypesWithDataUseCase(healthConnectManager, Dispatchers.Main)
        healthConnectManager.stub { on { currentDeviceId } doReturn "current_device_id" }
    }

    @Test
    fun invoke_managerError_returnsFailure() = runTest {
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.failure<Map<Class<out Record>, RecordTypeInfoResponse>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }

        val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(HealthConnectException::class.java)
    }

    @Test
    fun invoke_noRecordTypes_returnsEmptyList() = runTest {
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(emptyMap<Class<out Record>, RecordTypeInfoResponse>())
        }

        val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_noFitnessRecordTypes_returnsEmptyList() = runTest {
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        emptyList(),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_recordTypes_returnsOneCategory() = runTest {
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(1)
        assertThat(data[0].category).isEqualTo(HealthDataCategory.ACTIVITY)
        assertThat(data[0].data).containsExactly(FitnessPermissionType.STEPS)
    }

    @Test
    fun invoke_recordTypes_returnsMultipleCategories() = runTest {
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    ),
                HeartRateRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.HEART_RATE),
                        HealthDataCategory.VITALS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    ),
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(2)
        val activity = data.find { it.category == HealthDataCategory.ACTIVITY }
        assertThat(activity).isNotNull()
        assertThat(activity!!.data).containsExactly(FitnessPermissionType.STEPS)

        val vitals = data.find { it.category == HealthDataCategory.VITALS }
        assertThat(vitals).isNotNull()
        assertThat(vitals!!.data).containsExactly(FitnessPermissionType.HEART_RATE)
    }

    @Test
    @DisableFlags(Flags.FLAG_SYMPTOMS)
    fun invoke_symptomsFlagOff_excludesSymptoms() = runTest {
        val recordTypeInfoMap =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(
                            HealthPermissionCategory.SYMPTOM_COUGH,
                            HealthPermissionCategory.SYMPTOM_CHILLS,
                        ),
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_SYMPTOMS)
    fun invoke_symptomsFlagOn_oneSymptomCategory() = runTest {
        val recordTypeInfoMap =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(
                            HealthPermissionCategory.SYMPTOM_COUGH,
                            HealthPermissionCategory.SYMPTOM_CHILLS,
                        ),
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(1)
        assertThat(data[0].category).isEqualTo(HealthDataCategory.SYMPTOMS)
        // Check Utils.kt getSymptomPermissionTypes for the expected permission type.
        // It uses FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN as a placeholder for the category.
        assertThat(data[0].data).containsExactly(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
    }
}
