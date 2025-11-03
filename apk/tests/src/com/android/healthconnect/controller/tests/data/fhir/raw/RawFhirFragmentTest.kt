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
package com.android.healthconnect.controller.tests.data.fhir.raw

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedRawFhir
import com.android.healthconnect.controller.data.fhir.raw.RawFhirFragment
import com.android.healthconnect.controller.data.fhir.raw.RawFhirViewModel
import com.android.healthconnect.controller.data.fhir.raw.RawFhirViewModel.RawFhirState.Error
import com.android.healthconnect.controller.data.fhir.raw.RawFhirViewModel.RawFhirState.Loading
import com.android.healthconnect.controller.data.fhir.raw.RawFhirViewModel.RawFhirState.WithData
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RawFhirPageElement
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RawFhirFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: RawFhirViewModel = mock(RawFhirViewModel::class.java)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock(HealthConnectLogger::class.java)

    private val fhirResource =
        "{\n" +
            "    \"resourceType\": \"Immunization\",\n" +
            "    \"id\": \"immunization_1\",\n" +
            "    \"status\": \"completed\",\n" +
            "    \"vaccineCode\": {\n" +
            "        \"coding\": [\n" +
            "            {\n" +
            "                \"system\": \"http://hl7.org/fhir/sid/cvx\",\n" +
            "                \"code\": \"115\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"system\": \"http://hl7.org/fhir/sid/ndc\",\n" +
            "                \"code\": \"58160-842-11\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"text\": \"Tdap\"\n" +
            "    },\n" +
            "    \"patient\": {\n" +
            "        \"reference\": \"Patient/patient_1\",\n" +
            "        \"display\": \"Example, Anne\"\n" +
            "    },\n" +
            "    \"encounter\": {\n" +
            "        \"reference\": \"Encounter/encounter_unk\",\n" +
            "        \"display\": \"GP Visit\"\n" +
            "    },\n" +
            "    \"occurrenceDateTime\": \"2018-05-21\",\n" +
            "    \"primarySource\": true,\n" +
            "    \"manufacturer\": {\n" +
            "        \"display\": \"Sanofi Pasteur\"\n" +
            "    },\n" +
            "    \"lotNumber\": \"1\",\n" +
            "    \"site\": {\n" +
            "        \"coding\": [\n" +
            "            {\n" +
            "                \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActSite\",\n" +
            "                \"code\": \"LA\",\n" +
            "                \"display\": \"Left Arm\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"text\": \"Left Arm\"\n" +
            "    },\n" +
            "    \"route\": {\n" +
            "        \"coding\": [\n" +
            "            {\n" +
            "                \"system\": \"http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration\",\n" +
            "                \"code\": \"IM\",\n" +
            "                \"display\": \"Injection, intramuscular\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"text\": \"Injection, intramuscular\"\n" +
            "    },\n" +
            "    \"doseQuantity\": {\n" +
            "        \"value\": 0.5,\n" +
            "        \"unit\": \"mL\"\n" +
            "    },\n" +
            "    \"performer\": [\n" +
            "        {\n" +
            "            \"function\": {\n" +
            "                \"coding\": [\n" +
            "                    {\n" +
            "                        \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0443\",\n" +
            "                        \"code\": \"AP\",\n" +
            "                        \"display\": \"Administering Provider\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"text\": \"Administering Provider\"\n" +
            "            },\n" +
            "            \"actor\": {\n" +
            "                \"reference\": \"Practitioner/practitioner_1\",\n" +
            "                \"type\": \"Practitioner\",\n" +
            "                \"display\": \"Dr Maria Hernandez\"\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}"
    private val contentDescription =
        "Detailed source code: Open bracket. Field resourceType Value: Immunization. Field id Value: immunization_1. Field status Value: completed. Field vaccineCode ValueDetailed source code:   Open bracket.   Field coding ValueDetailed source code:     Open bracket.     Field system Value    : http://hl7.org/fhir/sid/cvx.     Field code Value    : 115.    Closed bracket.Detailed source code:     Open bracket.     Field system Value    : http://hl7.org/fhir/sid/ndc.     Field code Value    : 58160-842-11.    Closed bracket.   Field text Value  : Tdap.  Closed bracket. Field patient ValueDetailed source code:   Open bracket.   Field reference Value  : Patient/patient_1.   Field display Value  : Example, Anne.  Closed bracket. Field encounter ValueDetailed source code:   Open bracket.   Field reference Value  : Encounter/encounter_unk.   Field display Value  : GP Visit.  Closed bracket. Field occurrenceDateTime Value: 2018-05-21. Field primarySource Value: true. Field manufacturer ValueDetailed source code:   Open bracket.   Field display Value  : Sanofi Pasteur.  Closed bracket. Field lotNumber Value: 1. Field site ValueDetailed source code:   Open bracket.   Field coding ValueDetailed source code:     Open bracket.     Field system Value    : http://terminology.hl7.org/CodeSystem/v3-ActSite.     Field code Value    : LA.     Field display Value    : Left Arm.    Closed bracket.   Field text Value  : Left Arm.  Closed bracket. Field route ValueDetailed source code:   Open bracket.   Field coding ValueDetailed source code:     Open bracket.     Field system Value    : http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration.     Field code Value    : IM.     Field display Value    : Injection, intramuscular.    Closed bracket.   Field text Value  : Injection, intramuscular.  Closed bracket. Field doseQuantity ValueDetailed source code:   Open bracket.   Field value Value  : 0.5.   Field unit Value  : mL.  Closed bracket. Field performer ValueDetailed source code:   Open bracket.   Field function ValueDetailed source code:     Open bracket.     Field coding ValueDetailed source code:       Open bracket.       Field system Value      : http://terminology.hl7.org/CodeSystem/v2-0443.       Field code Value      : AP.       Field display Value      : Administering Provider.      Closed bracket.     Field text Value    : Administering Provider.    Closed bracket.   Field actor ValueDetailed source code:     Open bracket.     Field reference Value    : Practitioner/practitioner_1.     Field type Value    : Practitioner.     Field display Value    : Dr Maria Hernandez.    Closed bracket.  Closed bracket.Closed bracket."

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    fun error_errorMessageDisplayed() {
        whenever(viewModel.rawFhir).then { MutableLiveData(Error) }

        launchFragment<RawFhirFragment>(
                bundleOf(
                    RawFhirFragment.MEDICAL_RESOURCE_ID_KEY to TEST_MEDICAL_RESOURCE_IMMUNIZATION.id
                )
            )
            .use {
                onView(withText("Something went wrong. Please try again."))
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun loading_loadingDisplayed() {
        whenever(viewModel.rawFhir).then { MutableLiveData(Loading) }

        launchFragment<RawFhirFragment>(
                bundleOf(
                    RawFhirFragment.MEDICAL_RESOURCE_ID_KEY to TEST_MEDICAL_RESOURCE_IMMUNIZATION.id
                )
            )
            .use {
                onView(ViewMatchers.withId(R.id.loading)).check(matches(isDisplayed()))
                onView(withSubstring("resourceType")).check(doesNotExist())
            }
    }

    @Test
    fun fhirResourcePresent_displaysFhirResource() {
        launchFragmentWithData().use {
            onView(withText(fhirResource)).check(matches(isDisplayed()))
            onView(withContentDescription(contentDescription)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun logPageImpression() {
        launchFragmentWithData().use {
            onView(withText(fhirResource)).check(matches(isDisplayed()))
            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.RAW_FHIR_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger).logImpression(RawFhirPageElement.RAW_FHIR_RESOURCE)
        }
    }

    private fun launchFragmentWithData(): ActivityScenario<TestActivity> {
        whenever(viewModel.rawFhir).then {
            MutableLiveData(
                WithData(
                    listOf(
                        FormattedRawFhir(
                            fhir = fhirResource,
                            fhirContentDescription = contentDescription,
                        )
                    )
                )
            )
        }

        return launchFragment<RawFhirFragment>(
            bundleOf(
                RawFhirFragment.MEDICAL_RESOURCE_ID_KEY to TEST_MEDICAL_RESOURCE_IMMUNIZATION.id
            )
        )
    }
}
