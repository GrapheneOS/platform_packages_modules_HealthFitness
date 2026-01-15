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
package com.android.healthconnect.controller.tests.data.fhir.pretty

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceId
import android.health.connect.datatypes.MedicalResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhir
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhirDetailsHeader
import com.android.healthconnect.controller.data.entries.FormattedEntry.ItemDataEntrySeparator
import com.android.healthconnect.controller.data.fhir.api.FhirUseCase
import com.android.healthconnect.controller.data.fhir.pretty.PrettyFhirViewModel
import com.android.healthconnect.controller.data.formatters.medical.PrettyFhirFormatter
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonGroup
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonLine
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_FORMATTED_MEDICAL_DATA_ENTRY_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TEST_FORMATTED_MEDICAL_DATA_ENTRY_IMMUNIZATION_LONG
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.prepareAnswer
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import jakarta.inject.Inject
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PrettyFhirViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)

    @Inject lateinit var prettyFhirFormatter: PrettyFhirFormatter
    private lateinit var prettyFhirViewModel: PrettyFhirViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        prettyFhirViewModel =
            PrettyFhirViewModel(FhirUseCase(manager, Dispatchers.Main), prettyFhirFormatter)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun smallImmunizationFhir_returnsPrettyFhir() = runTest {
        val medicalResources: List<MedicalResource> = listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION)
        val medicalDataEntry = TEST_FORMATTED_MEDICAL_DATA_ENTRY_IMMUNIZATION
        doAnswer(prepareAnswer(medicalResources))
            .`when`(manager)
            .readMedicalResources(any<MutableList<MedicalResourceId>>(), any(), any())
        val testObserver = TestObserver<PrettyFhirViewModel.PrettyFhirState>()
        val emptyPrettyJsonGroup = PrettyJsonGroup(nestedLines = emptyList())
        val expected =
            listOf(
                FormattedPrettyFhirDetailsHeader("Test app • Test hospital", "Test immunization"),
                ItemDataEntrySeparator(),
                FormattedPrettyFhir(
                    header = "Resource Type: Immunization",
                    content = emptyPrettyJsonGroup,
                ),
                FormattedPrettyFhir(header = "Id: immunization-1", content = emptyPrettyJsonGroup),
                FormattedPrettyFhir(header = "Status: completed", content = emptyPrettyJsonGroup),
                FormattedPrettyFhir(
                    header = "Vaccine Code:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(depth = 1, line = "Coding:"),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line = "System: http://hl7.org/fhir/sid/cvx",
                                    ),
                                    PrettyJsonLine(depth = 2, line = "Code: 115"),
                                    PrettyJsonLine(depth = 2, line = ""),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line = "System: http://hl7.org/fhir/sid/ndc",
                                    ),
                                    PrettyJsonLine(depth = 2, line = "Code: 58160-842-11"),
                                    PrettyJsonLine(depth = 1, line = "Text: Tdap"),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Patient:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(
                                        depth = 1,
                                        line = "Reference: Patient/patient_1",
                                    ),
                                    PrettyJsonLine(depth = 1, line = "Display: Example, Anne"),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Occurrence Date Time: 2018-05-21",
                    content = emptyPrettyJsonGroup,
                ),
            )

        prettyFhirViewModel.prettyFhir.observeForever(testObserver)
        prettyFhirViewModel.loadPrettyFhirResource(medicalDataEntry)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(PrettyFhirViewModel.PrettyFhirState.WithData(expected))
    }

    @Test
    fun longImmunizationFhir_returnsPrettyFhir() = runTest {
        val medicalResources: List<MedicalResource> =
            listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG)
        val medicalDataEntry = TEST_FORMATTED_MEDICAL_DATA_ENTRY_IMMUNIZATION_LONG
        doAnswer(prepareAnswer(medicalResources))
            .`when`(manager)
            .readMedicalResources(any<MutableList<MedicalResourceId>>(), any(), any())
        val testObserver = TestObserver<PrettyFhirViewModel.PrettyFhirState>()
        val EMPTY_PRETTY_JSON_GROUP =
            PrettyJsonGroup(nestedLines = emptyList()) // More concise for standalone entries
        val expected: List<FormattedEntry> =
            listOf(
                FormattedPrettyFhirDetailsHeader(
                    "Test app • Test hospital",
                    "Test immunization long",
                ),
                ItemDataEntrySeparator(),
                FormattedPrettyFhir(
                    header = "Resource Type: Immunization",
                    content = EMPTY_PRETTY_JSON_GROUP,
                ),
                FormattedPrettyFhir(
                    header = "Id: immunization-1",
                    content = EMPTY_PRETTY_JSON_GROUP,
                ),
                FormattedPrettyFhir(
                    header = "Status: completed",
                    content = EMPTY_PRETTY_JSON_GROUP,
                ),
                FormattedPrettyFhir(
                    header = "Vaccine Code:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(depth = 1, line = "Coding:"),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line = "System: http://hl7.org/fhir/sid/cvx",
                                    ),
                                    PrettyJsonLine(depth = 2, line = "Code: 115"),
                                    PrettyJsonLine(depth = 2, line = ""),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line = "System: http://hl7.org/fhir/sid/ndc",
                                    ),
                                    PrettyJsonLine(depth = 2, line = "Code: 58160-842-11"),
                                    PrettyJsonLine(depth = 1, line = "Text: Tdap"),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Patient:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(
                                        depth = 1,
                                        line = "Reference: Patient/patient_1",
                                    ),
                                    PrettyJsonLine(depth = 1, line = "Display: Example, Anne"),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Encounter:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(
                                        depth = 1,
                                        line = "Reference: Encounter/encounter_unk",
                                    ),
                                    PrettyJsonLine(depth = 1, line = "Display: GP Visit"),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Occurrence Date Time: 2018-05-21",
                    content = EMPTY_PRETTY_JSON_GROUP,
                ),
                FormattedPrettyFhir(
                    header = "Primary Source: true",
                    content = EMPTY_PRETTY_JSON_GROUP,
                ),
                FormattedPrettyFhir(
                    header = "Manufacturer:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(PrettyJsonLine(depth = 1, line = "Display: Sanofi Pasteur"))
                        ),
                ),
                FormattedPrettyFhir(header = "Lot Number: 1", content = EMPTY_PRETTY_JSON_GROUP),
                FormattedPrettyFhir(
                    header = "Site:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(depth = 1, line = "Coding:"),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line =
                                            "System: http://terminology.hl7.org/CodeSystem/v3-ActSite",
                                    ),
                                    PrettyJsonLine(depth = 2, line = "Code: LA"),
                                    PrettyJsonLine(depth = 2, line = "Display: Left Arm"),
                                    PrettyJsonLine(depth = 1, line = "Text: Left Arm"),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Route:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(depth = 1, line = "Coding:"),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line =
                                            "System: http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration",
                                    ),
                                    PrettyJsonLine(depth = 2, line = "Code: IM"),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line = "Display: Injection, intramuscular",
                                    ),
                                    PrettyJsonLine(
                                        depth = 1,
                                        line = "Text: Injection, intramuscular",
                                    ),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Dose Quantity:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(depth = 1, line = "Value: 0.5"),
                                    PrettyJsonLine(depth = 1, line = "Unit: mL"),
                                )
                        ),
                ),
                FormattedPrettyFhir(
                    header = "Performer:",
                    content =
                        PrettyJsonGroup(
                            nestedLines =
                                listOf(
                                    PrettyJsonLine(depth = 1, line = "Function:"),
                                    PrettyJsonLine(depth = 2, line = "Coding:"),
                                    PrettyJsonLine(
                                        depth = 3,
                                        line =
                                            "System: http://terminology.hl7.org/CodeSystem/v2-0443",
                                    ),
                                    PrettyJsonLine(depth = 3, line = "Code: AP"),
                                    PrettyJsonLine(
                                        depth = 3,
                                        line = "Display: Administering Provider",
                                    ),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line = "Text: Administering Provider",
                                    ),
                                    PrettyJsonLine(depth = 1, line = "Actor:"),
                                    PrettyJsonLine(
                                        depth = 2,
                                        line = "Reference: Practitioner/practitioner_1",
                                    ),
                                    PrettyJsonLine(depth = 2, line = "Type: Practitioner"),
                                    PrettyJsonLine(depth = 2, line = "Display: Dr Maria Hernandez"),
                                )
                        ),
                ),
            )

        prettyFhirViewModel.prettyFhir.observeForever(testObserver)
        prettyFhirViewModel.loadPrettyFhirResource(medicalDataEntry)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(PrettyFhirViewModel.PrettyFhirState.WithData(expected))
    }
}
