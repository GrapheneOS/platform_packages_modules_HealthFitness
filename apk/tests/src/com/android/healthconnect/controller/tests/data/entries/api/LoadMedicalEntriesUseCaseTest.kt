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
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@Ignore // b/343647465
@RunWith(AndroidJUnit4::class)
class LoadMedicalEntriesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var loadMedicalEntriesUseCase: LoadMedicalEntriesUseCase
    private lateinit var loadEntriesHelper: LoadEntriesHelper
    private lateinit var medicalEntryFormatter: MedicalEntryFormatter

    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter
    @Inject lateinit var menstruationPeriodFormatter: MenstruationPeriodFormatter
    @Inject lateinit var dataSourceReader: MedicalDataSourceReader

    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        loadEntriesHelper =
            LoadEntriesHelper(
                context,
                healthDataEntryFormatter,
                menstruationPeriodFormatter,
                healthConnectManager,
                dataSourceReader,
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
        Mockito.doAnswer(prepareAnswer(readMedicalResourcesResponse))
            .`when`(healthConnectManager)
            .readMedicalResources(any<ReadMedicalResourcesInitialRequest>(), any(), any())

        val result = loadMedicalEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(listOf<FormattedEntry.FormattedMedicalDataEntry>())
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
        Mockito.doAnswer(prepareAnswer(readMedicalResourcesResponse))
            .`when`(healthConnectManager)
            .readMedicalResources(any<ReadMedicalResourcesInitialRequest>(), any(), any())

        val result = loadMedicalEntriesUseCase.invoke(input)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    FormattedEntry.FormattedMedicalDataEntry(
                        header = "02 May 2023 • Health Connect Toolbox",
                        headerA11y = "02 May 2023 • Health Connect Toolbox",
                        title = "Covid vaccine",
                        titleA11y = "important vaccination",
                        medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
                    )
                )
            )
    }

    private fun prepareAnswer(
        readMedicalResourcesResponse: ReadMedicalResourcesResponse
    ): (InvocationOnMock) -> ReadMedicalResourcesResponse {
        return { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<ReadMedicalResourcesResponse, *>
            receiver.onResult(readMedicalResourcesResponse)
            readMedicalResourcesResponse
        }
    }
}
