/*
 * Copyright (C) 2024 The Android Open Source Project
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
 *
 *
 */
package com.android.healthconnect.controller.tests.shared.app

import android.content.Context
import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.MedicalDataSource
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MedicalDataSourceReaderTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var medicalDataSourceReader: MedicalDataSourceReader
    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        hiltRule.inject()
        medicalDataSourceReader = MedicalDataSourceReader(healthConnectManager, Dispatchers.Main)
    }

    @Test
    fun fromPackageName_noData_returnsEmptyList() = runTest {
        val packageName = "com.example.package"
        val readMedicalDataSourcesResponse = emptyList<MedicalDataSource>()

        Mockito.doAnswer(prepareAnswer(readMedicalDataSourcesResponse))
            .`when`(healthConnectManager)
            .getMedicalDataSources(any<GetMedicalDataSourcesRequest>(), any(), any())

        val result = medicalDataSourceReader.fromPackageName(packageName)
        assertThat(result).isEmpty()
    }

    @Test
    fun fromPackageName_returnsData() = runTest {
        val expectedDataSources = listOf(TEST_MEDICAL_DATA_SOURCE)

        Mockito.doAnswer(prepareAnswer(expectedDataSources))
            .`when`(healthConnectManager)
            .getMedicalDataSources(any<GetMedicalDataSourcesRequest>(), any(), any())

        val result = medicalDataSourceReader.fromPackageName(TEST_APP_PACKAGE_NAME)
        assertThat(result).containsExactlyElementsIn(expectedDataSources)
    }

    @Test
    fun fromDataSourceId_noData_returnsEmptyList() = runTest {
        val dataSourceId = "data_source_id"
        val readMedicalDataSourcesResponse = emptyList<MedicalDataSource>()

        Mockito.doAnswer(prepareAnswer(readMedicalDataSourcesResponse))
            .`when`(healthConnectManager)
            .getMedicalDataSources(any<List<String>>(), any(), any())

        val result = medicalDataSourceReader.fromDataSourceId(dataSourceId)
        assertThat(result).isEmpty()
    }

    @Test
    fun fromDataSourceId_returnsData() = runTest {
        val dataSourceId = "data_source_id"
        val expectedDataSources = listOf(TEST_MEDICAL_DATA_SOURCE)

        Mockito.doAnswer(prepareAnswer(expectedDataSources))
            .`when`(healthConnectManager)
            .getMedicalDataSources(any<List<String>>(), any(), any())

        val result = medicalDataSourceReader.fromDataSourceId(dataSourceId)
        assertThat(result).containsExactlyElementsIn(expectedDataSources)
    }

    private fun prepareAnswer(
        medicalDataSourcesResponse: List<MedicalDataSource>
    ): (InvocationOnMock) -> Unit {
        return { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<MedicalDataSource>, *>
            receiver.onResult(medicalDataSourcesResponse)
        }
    }
}
