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
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.appdata.api.GetAppFitnessPermissionTypesUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.devices.api.FakeGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_WATCH_SPN
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GetAppFitnessPermissionTypesUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    private val healthConnectManager: HealthConnectManager = mock()
    private lateinit var getAppFitnessPermissionTypesUseCase: GetAppFitnessPermissionTypesUseCase
    private val fakeGetCurrentDeviceIdUseCase =
        fakeUseCaseRule.watch(FakeGetCurrentDeviceIdUseCase())

    @Before
    fun setup() = runTest {
        hiltRule.inject()
        getAppFitnessPermissionTypesUseCase =
            GetAppFitnessPermissionTypesUseCase(
                healthConnectManager,
                fakeGetCurrentDeviceIdUseCase,
                Dispatchers.Main,
            )
        fakeGetCurrentDeviceIdUseCase.updateDeviceId("current_device_id")
    }

    @Test
    fun invoke_managerError_returnsFailure() = runTest {
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.failure<Map<Class<out Record>, RecordTypeInfoResponse>>(
                    HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
                )
        }

        val result = getAppFitnessPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(HealthConnectException::class.java)
    }

    @Test
    fun invoke_forDifferentPackageName_returnsEmptyList() = runTest {
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin("other.package.name")),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getAppFitnessPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_noRecordTypes_returnsEmptyList() = runTest {
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(emptyMap<Class<out Record>, RecordTypeInfoResponse>())
        }

        val result = getAppFitnessPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_returnsMultipleCategories() = runTest {
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    ),
                SleepSessionRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SLEEP),
                        HealthDataCategory.SLEEP,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    ),
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getAppFitnessPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(2)
        assertThat(data[0].category).isEqualTo(HealthDataCategory.ACTIVITY)
        assertThat(data[0].data).containsExactly(FitnessPermissionType.STEPS)
        assertThat(data[1].category).isEqualTo(HealthDataCategory.SLEEP)
        assertThat(data[1].data).containsExactly(FitnessPermissionType.SLEEP)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun invoke_symptomsFlagOff_excludesSymptoms() = runTest {
        val recordTypeInfoMap =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        emptySet(),
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getAppFitnessPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun invoke_symptomsFlagOn_returnsOneSymptomCategory() = runTest {
        val recordTypeInfoMap =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(
                            HealthPermissionCategory.SYMPTOM_BRAIN_FOG,
                            HealthPermissionCategory.SYMPTOM_COUGH,
                        ),
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = getAppFitnessPermissionTypesUseCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(1)
        assertThat(data[0].category).isEqualTo(HealthDataCategory.SYMPTOMS)
        assertThat(data[0].data).containsExactly(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadFitnessAppData_ddpFlagsOn_currentDevicePackageName_includesLegacySourceInCategories() =
        runTest {
            val deviceId = "test_device_id"
            val recordTypeInfoMap =
                mapOf(
                    StepsRecord::class.java to
                        RecordTypeInfoResponse(
                            setOf(HealthPermissionCategory.STEPS),
                            HealthDataCategory.ACTIVITY,
                            listOf(getDataOrigin(DEVICE_DATA_PROVIDER_PACKAGE_NAME)),
                        )
                )

            healthConnectManager.stub {
                on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                    Result.success(recordTypeInfoMap)
            }
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(deviceId)

            val result = getAppFitnessPermissionTypesUseCase.invoke(deviceId)
            assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
            val data = (result as UseCaseResults.Success).data
            assertThat(data).hasSize(1)
            assertThat(data[0].category).isEqualTo(HealthDataCategory.ACTIVITY)
            assertThat(data[0].data).containsExactly(FitnessPermissionType.STEPS)
        }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadFitnessAppData_ddpFlagsOn_randomDevice_ignoresLegacySourceInCategories() = runTest {
        val testDeviceId = TEST_WATCH_SPN
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(DEVICE_DATA_PROVIDER_PACKAGE_NAME)),
                    )
            )

        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }
        fakeGetCurrentDeviceIdUseCase.updateDeviceId("NotDeviceId")

        val result = getAppFitnessPermissionTypesUseCase.invoke(testDeviceId)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadFitnessAppData_ddpFlagsOff_currentDevicePackageName_ignoresLegacySourceInCategories() =
        runTest {
            val deviceId = "test_device_id"
            val recordTypeInfoMap =
                mapOf(
                    StepsRecord::class.java to
                        RecordTypeInfoResponse(
                            setOf(HealthPermissionCategory.STEPS),
                            HealthDataCategory.ACTIVITY,
                            listOf(getDataOrigin(DEVICE_DATA_PROVIDER_PACKAGE_NAME)),
                        )
                )

            healthConnectManager.stub {
                on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                    Result.success(recordTypeInfoMap)
            }
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(deviceId)

            val result = getAppFitnessPermissionTypesUseCase.invoke(deviceId)
            assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
            assertThat((result as UseCaseResults.Success).data).isEmpty()
        }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadFitnessAppData_ddpFlagsOn_android_includesCurrentDeviceInCategories() = runTest {
        val testDeviceId = "test_device_id"
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(testDeviceId)),
                    )
            )

        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(testDeviceId)

        val result = getAppFitnessPermissionTypesUseCase.invoke(DEVICE_DATA_PROVIDER_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data).hasSize(1)
        assertThat(data[0].category).isEqualTo(HealthDataCategory.ACTIVITY)
        assertThat(data[0].data).containsExactly(FitnessPermissionType.STEPS)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadFitnessAppData_ddpFlagsOn_android_ignoresRandomDeviceHasDataInCategories() = runTest {
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_WATCH_SPN)),
                    )
            )

        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }
        fakeGetCurrentDeviceIdUseCase.updateDeviceId("NotDeviceId")

        val result = getAppFitnessPermissionTypesUseCase.invoke(DEVICE_DATA_PROVIDER_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadFitnessAppData_ddpFlagsOff_android_ignoresCurrentDeviceInCategories() = runTest {
        val testDeviceId = "test_device_id"
        val recordTypeInfoMap =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(testDeviceId)),
                    )
            )

        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(testDeviceId)

        val result = getAppFitnessPermissionTypesUseCase.invoke(DEVICE_DATA_PROVIDER_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }
}
