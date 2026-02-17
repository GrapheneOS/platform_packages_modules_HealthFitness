/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.data.access.api

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.access.api.LoadSymptomContributorAppsUseCase
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
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
class LoadSymptomContributorAppsUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue lateinit var appInfoReader: AppInfoReader
    private lateinit var context: Context
    private val healthConnectManager: HealthConnectManager = mock()
    private lateinit var loadSymptomContributorAppsUseCase: LoadSymptomContributorAppsUseCase

    @Before
    fun setup() = runTest {
        context = InstrumentationRegistry.getInstrumentation().context
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        loadSymptomContributorAppsUseCase =
            LoadSymptomContributorAppsUseCase(appInfoReader, healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun invoke_noRecordsStored_returnsEmptyList() = runTest {
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(mapOf<Class<out Record>, RecordTypeInfoResponse>())
        }

        val result = loadSymptomContributorAppsUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success<List<AppMetadata>>).data
        assertThat(data).isEmpty()
    }

    @Test
    fun invoke_noSymptomRecordsStored_returnsEmptyList() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME_2)),
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

        val result = loadSymptomContributorAppsUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success<List<AppMetadata>>).data
        assertThat(data).isEmpty()
    }

    @Test
    fun invoke_returnsCorrectApps() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(
                            HealthPermissionCategory.SYMPTOM_COUGH,
                            HealthPermissionCategory.SYMPTOM_HEADACHE,
                        ),
                        HealthDataCategory.SYMPTOMS,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin(TEST_APP_PACKAGE_NAME_2),
                        ),
                    )
            )
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        val result = loadSymptomContributorAppsUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success<List<AppMetadata>>).data
        assertThat(data).hasSize(2)
        assertThat(data[0].packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(data[1].packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
    }

    @Test
    fun loadSymptomContributorAppsUseCase_error_returnsEmptyList() = runTest {
        healthConnectManager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.failure<Map<Class<out Record>, RecordTypeInfoResponse>>(
                    RuntimeException("Test exception")
                )
        }

        val result = loadSymptomContributorAppsUseCase.invoke(Unit)
        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(RuntimeException::class.java)
    }
}
