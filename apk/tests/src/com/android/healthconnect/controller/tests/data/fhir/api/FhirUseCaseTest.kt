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

package com.android.healthconnect.controller.tests.data.fhir.api

import android.content.Context
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceId
import android.health.connect.datatypes.MedicalResource
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.data.fhir.api.FhirUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_DATASOURCE_ID
import com.android.healthconnect.controller.tests.utils.TEST_FHIR_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.initMocks
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FhirUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)

    private lateinit var mFhirUseCase: FhirUseCase
    private lateinit var context: Context

    @Before
    fun setup() {
        initMocks(this)
        context = getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        mFhirUseCase = FhirUseCase(manager, Dispatchers.Main)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyList_returnsFailedUseCaseResult() = runTest {
        doAnswer(prepareAnswer(listOf()))
            .`when`(manager)
            .readMedicalResources(any<MutableList<MedicalResourceId>>(), any(), any())

        val result = mFhirUseCase.loadFhirResource(TEST_MEDICAL_RESOURCE_IMMUNIZATION.id)

        assertThat((result as UseCaseResults.Failed).exception is IllegalStateException).isTrue()
        assertThat((result.exception as IllegalStateException).message)
            .isEqualTo(
                "No FHIR resource found for given MedicalResourceId{dataSourceId=${TEST_DATASOURCE_ID},fhirResourceType=1,fhirResourceId=Immunization1}"
            )
    }

    @Test
    fun error_returnsFailedUseCaseResult() = runTest {
        doAnswer(prepareFailureAnswer())
            .`when`(manager)
            .readMedicalResources(any<MutableList<MedicalResourceId>>(), any(), any())

        val result = mFhirUseCase.loadFhirResource(TEST_MEDICAL_RESOURCE_IMMUNIZATION.id)

        assertThat((result as UseCaseResults.Failed).exception is HealthConnectException).isTrue()
        assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }

    @Test
    fun validMedicalResourceId_returnsMedicalResource() = runTest {
        val medicalResources: List<MedicalResource> = listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION)
        doAnswer(prepareAnswer(medicalResources))
            .`when`(manager)
            .readMedicalResources(any<MutableList<MedicalResourceId>>(), any(), any())

        val result = mFhirUseCase.loadFhirResource(TEST_MEDICAL_RESOURCE_IMMUNIZATION.id)

        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data.id)
            .isEqualTo(TEST_FHIR_RESOURCE_IMMUNIZATION.id)
        assertThat((result).data.data).isEqualTo(TEST_FHIR_RESOURCE_IMMUNIZATION.data)
        assertThat((result).data.type).isEqualTo(TEST_FHIR_RESOURCE_IMMUNIZATION.type)
    }

    private fun prepareAnswer(
        medicalResources: List<MedicalResource>
    ): (InvocationOnMock) -> List<MedicalResource> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<Any?, *>
            receiver.onResult(medicalResources)
            medicalResources
        }
        return answer
    }

    private fun prepareFailureAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<List<LocalDate>, HealthConnectException>
            receiver.onError(HealthConnectException(HealthConnectException.ERROR_UNKNOWN))
            null
        }
        return answer
    }
}
