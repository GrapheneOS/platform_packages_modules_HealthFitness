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
package com.android.healthconnect.controller.tests.data.entries.api

import android.content.Context
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadMedicalResourcesInitialRequest
import android.health.connect.ReadMedicalResourcesResponse
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.data.formatters.medical.MedicalEntryFormatter
import com.android.healthconnect.controller.data.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.devices.api.FakeGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadMedicalEntriesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    private lateinit var context: Context
    private lateinit var loadMedicalEntriesUseCase: LoadMedicalEntriesUseCase
    private lateinit var loadEntriesHelper: LoadEntriesHelper
    private val fakeGetCurrentDeviceIdUseCase =
        fakeUseCaseRule.watch(FakeGetCurrentDeviceIdUseCase())

    @Inject lateinit var medicalEntryFormatter: MedicalEntryFormatter
    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter
    @Inject lateinit var menstruationPeriodFormatter: MenstruationPeriodFormatter
    @Inject lateinit var dataSourceReader: MedicalDataSourceReader

    private val healthConnectManager: HealthConnectManager = mock()

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        loadEntriesHelper =
            LoadEntriesHelper(
                context,
                Dispatchers.Main,
                healthDataEntryFormatter,
                menstruationPeriodFormatter,
                healthConnectManager,
                dataSourceReader,
                fakeGetCurrentDeviceIdUseCase,
            )
        loadMedicalEntriesUseCase =
            LoadMedicalEntriesUseCase(Dispatchers.Main, medicalEntryFormatter, loadEntriesHelper)
    }

    @Test
    fun invoke_noData_returnsEmptyList() = runTest {
        val input =
            LoadMedicalEntriesInput(
                medicalPermissionType = MedicalPermissionType.VACCINES,
                packageName = null,
                showDataOrigin = true,
            )
        val readMedicalResourcesResponse =
            ReadMedicalResourcesResponse(emptyList(), "nextPageToken", 1)

        healthConnectManager.stub {
            on {
                readMedicalResources(
                    any<ReadMedicalResourcesInitialRequest>(),
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadMedicalResourcesResponse, HealthConnectException>>(),
                )
            } doReturnResult Result.success(readMedicalResourcesResponse)
        }

        val result = loadMedicalEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_returnsFormattedData() = runTest {
        val input =
            LoadMedicalEntriesInput(
                medicalPermissionType = MedicalPermissionType.VACCINES,
                packageName = null,
                showDataOrigin = true,
            )
        val readMedicalResourcesResponse =
            ReadMedicalResourcesResponse(
                listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION),
                "nextPageToken",
                2,
            )

        healthConnectManager.stub {
            on {
                readMedicalResources(
                    any<ReadMedicalResourcesInitialRequest>(),
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadMedicalResourcesResponse, HealthConnectException>>(),
                )
            } doReturnResult Result.success(readMedicalResourcesResponse)
            on {
                getMedicalDataSources(
                    eq(listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION.dataSourceId)),
                    any<java.util.concurrent.Executor>(),
                    any<
                        OutcomeReceiver<
                            List<android.health.connect.datatypes.MedicalDataSource>,
                            HealthConnectException,
                        >
                    >(),
                )
            } doReturnResult
                Result.success(
                    listOf(
                        com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
                    )
                )
        }

        val result = loadMedicalEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    FormattedEntry.FormattedMedicalDataEntry(
                        // TODO (b/488090474) check why data source is not displayed in test
                        header = "May 21, 2018",
                        headerA11y = "May 21, 2018",
                        title = "Tdap",
                        titleA11y = "Tdap",
                        medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
                    )
                )
            )
    }

    @Test
    fun invoke_withShowDataOriginFalse_returnsFormattedData() = runTest {
        val input =
            LoadMedicalEntriesInput(
                medicalPermissionType = MedicalPermissionType.VACCINES,
                packageName = null,
                showDataOrigin = false,
            )
        val readMedicalResourcesResponse =
            ReadMedicalResourcesResponse(
                listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION),
                "nextPageToken",
                2,
            )

        healthConnectManager.stub {
            on {
                readMedicalResources(
                    any<ReadMedicalResourcesInitialRequest>(),
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadMedicalResourcesResponse, HealthConnectException>>(),
                )
            } doReturnResult Result.success(readMedicalResourcesResponse)

            on {
                getMedicalDataSources(
                    eq(listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION.dataSourceId)),
                    any<java.util.concurrent.Executor>(),
                    any<
                        OutcomeReceiver<
                            List<android.health.connect.datatypes.MedicalDataSource>,
                            HealthConnectException,
                        >
                    >(),
                )
            } doReturnResult
                Result.success(
                    listOf(
                        com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
                    )
                )
        }

        val result = loadMedicalEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()

        // Since showDataOrigin is false, the expected header should not include the app name.
        val expectedHeader = "May 21, 2018"
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    FormattedEntry.FormattedMedicalDataEntry(
                        header = expectedHeader,
                        headerA11y = expectedHeader,
                        title = "Tdap",
                        titleA11y = "Tdap",
                        medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
                    )
                )
            )
    }

    @Test
    fun invoke_whenFormatterThrowsException_filtersOutNullEntries() = runTest {
        val input =
            LoadMedicalEntriesInput(
                medicalPermissionType = MedicalPermissionType.VACCINES,
                packageName = null,
                showDataOrigin = true,
            )
        val readMedicalResourcesResponse =
            ReadMedicalResourcesResponse(
                listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION),
                "nextPageToken",
                2,
            )

        healthConnectManager.stub {
            on {
                readMedicalResources(
                    any<ReadMedicalResourcesInitialRequest>(),
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadMedicalResourcesResponse, HealthConnectException>>(),
                )
            } doReturnResult Result.success(readMedicalResourcesResponse)
        }

        val mockFormatter: MedicalEntryFormatter = mock()
        whenever(mockFormatter.formatResource(any(), any()))
            .thenThrow(RuntimeException("Formatter error"))

        val useCaseWithMockFormatter =
            LoadMedicalEntriesUseCase(Dispatchers.Main, mockFormatter, loadEntriesHelper)

        val result = useCaseWithMockFormatter.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        // If the formatter throws, the UseCase catches it, logs it, and returns null for that item,
        // filtering it out.
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }
}
