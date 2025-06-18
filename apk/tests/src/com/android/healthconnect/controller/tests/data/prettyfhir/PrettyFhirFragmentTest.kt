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

package com.android.healthconnect.controller.tests.data.prettyfhir

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation.setViewNavController
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhir
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhirDetailsHeader
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonGroup
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonLine
import com.android.healthconnect.controller.data.prettyfhir.PrettyFhirFragment
import com.android.healthconnect.controller.data.rawfhir.RawFhirViewModel
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PrettyFhirFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val viewModel: RawFhirViewModel = mock<RawFhirViewModel>()
    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        context.setLocale(Locale.US)
    }

    @Test
    fun error_errorMessageDisplayed() {
        whenever(viewModel.prettyFhir).then {
            MutableLiveData(RawFhirViewModel.PrettyFhirState.Error)
        }

        launchFragment<PrettyFhirFragment>(
            PrettyFhirFragment.createBundle(
                header = "header",
                headerA11y = "header a11y",
                title = "title",
                titleA11y = "title a11y",
                medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
            )
        )

        onView(withId(R.id.loading)).check(matches(not(isDisplayed())))
        onView(withId(R.id.item_pretty_fhir_entry_header)).check(doesNotExist())
        onView(withText("Something went wrong. Please try again.")).check(matches(isDisplayed()))
    }

    @Test
    fun loading_loadingDisplayed() {
        whenever(viewModel.prettyFhir).then {
            MutableLiveData(RawFhirViewModel.PrettyFhirState.Loading)
        }

        launchFragment<PrettyFhirFragment>(
            PrettyFhirFragment.createBundle(
                header = "header",
                headerA11y = "header a11y",
                title = "title",
                titleA11y = "title a11y",
                medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
            )
        )

        onView(withId(R.id.loading)).check(matches(isDisplayed()))
        onView(withId(R.id.item_pretty_fhir_entry_header)).check(doesNotExist())
        onView(withText("Something went wrong. Please try again."))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun medicalEntry_headerCorrectlyDisplayed() {
        val formattedPrettyFhirDetailsHeader =
            FormattedPrettyFhirDetailsHeader(header = "header", title = "title")
        whenever(viewModel.prettyFhir).then {
            MutableLiveData(
                RawFhirViewModel.PrettyFhirState.WithData(listOf(formattedPrettyFhirDetailsHeader))
            )
        }
        whenever(viewModel.rawFhir).then {
            MutableLiveData(
                RawFhirViewModel.RawFhirState.WithData(
                    listOf(
                        FormattedEntry.FormattedRawFhir(
                            fhir = fhirResource,
                            fhirContentDescription = contentDescription,
                        )
                    )
                )
            )
        }

        launchFragment<PrettyFhirFragment>(
            PrettyFhirFragment.createBundle(
                header = "header",
                headerA11y = "header a11y",
                title = "title",
                titleA11y = "title a11y",
                medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
            )
        )

        onView(withId(R.id.item_pretty_fhir_entry_header)).check(matches(isDisplayed()))
        onView(withText("Something went wrong. Please try again."))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun medicalEntry_EntriesAreDisplayed_withCorrectViews() {
        val itemPrettyFhirHeaderId = R.id.item_pretty_fhir_header
        val itemPrettyFhirContentLevel1Id = R.id.item_pretty_fhir_content_level1
        val itemPrettyFhirContentLevel2Id = R.id.item_pretty_fhir_content_level2
        val itemPrettyFhirContentLevel3Id = R.id.item_pretty_fhir_content_level3
        val formattedPrettyFhirDetailsHeader =
            FormattedPrettyFhirDetailsHeader(header = "header", title = "title")
        val formattedPrettyFhirEntriesList =
            listOf(
                FormattedPrettyFhir("Resource Type: Immunization", PrettyJsonGroup(emptyList())),
                FormattedPrettyFhir("Id: immunization_1", PrettyJsonGroup(emptyList())),
                FormattedPrettyFhir("Status: completed", PrettyJsonGroup(emptyList())),
                FormattedPrettyFhir(
                    "Vaccine Code:",
                    PrettyJsonGroup(
                        nestedLines =
                            listOf(
                                PrettyJsonLine(1, "Coding:"),
                                PrettyJsonLine(2, "System: http://hl7.org/fhir/sid/cvx"),
                                PrettyJsonLine(3, "Code: 115"),
                                PrettyJsonLine(1, "Coding:"),
                                PrettyJsonLine(2, "System: http://hl7.org/fhir/sid/ndc"),
                                PrettyJsonLine(3, "Code: 58160-842-11"),
                                PrettyJsonLine(1, "Text: Tdap"),
                            )
                    ),
                ),
            )
        val formattedEntries: List<FormattedEntry> =
            listOf(formattedPrettyFhirDetailsHeader, FormattedEntry.ItemDataEntrySeparator()) +
                formattedPrettyFhirEntriesList
        whenever(viewModel.prettyFhir).then {
            MutableLiveData(RawFhirViewModel.PrettyFhirState.WithData(formattedEntries))
        }
        whenever(viewModel.rawFhir).then {
            MutableLiveData(
                RawFhirViewModel.RawFhirState.WithData(
                    listOf(
                        FormattedEntry.FormattedRawFhir(
                            fhir = fhirResource,
                            fhirContentDescription = contentDescription,
                        )
                    )
                )
            )
        }

        launchFragment<PrettyFhirFragment>(
            PrettyFhirFragment.createBundle(
                header = "header",
                headerA11y = "header a11y",
                title = "title",
                titleA11y = "title a11y",
                medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
            )
        )

        onView(withText("Resource Type: Immunization"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirHeaderId)))
        onView(withText("Status: completed"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirHeaderId)))
        onView(withText("Id: immunization_1"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirHeaderId)))
        onView(withText("Status: completed"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirHeaderId)))
        onView(withText("Vaccine Code:"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirHeaderId)))
        onView(withText("• System: http://hl7.org/fhir/sid/cvx"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirContentLevel2Id)))
        onView(withText("• Code: 58160-842-11"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirContentLevel3Id)))
        onView(withText("Text: Tdap"))
            .check(matches(isDisplayed()))
            .check(matches(withId(itemPrettyFhirContentLevel1Id)))
        onView(withId(R.id.loading)).check(matches(not(isDisplayed())))
        onView(withText("Something went wrong. Please try again."))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun clickingOnViewSourceData_navigatesToRawFhirFragment() {
        val formattedPrettyFhirDetailsHeader =
            FormattedPrettyFhirDetailsHeader(header = "header", title = "title")
        whenever(viewModel.prettyFhir).then {
            MutableLiveData(
                RawFhirViewModel.PrettyFhirState.WithData(listOf(formattedPrettyFhirDetailsHeader))
            )
        }

        val bundle =
            PrettyFhirFragment.createBundle(
                header = "header",
                headerA11y = "header a11y",
                title = "title",
                titleA11y = "title a11y",
                medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
            )

        launchFragment<PrettyFhirFragment>(bundle) {
            navHostController.setGraph(R.navigation.entries_and_access_nav_graph)
            navHostController.setCurrentDestination(R.id.prettyFhirFragment)

            setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("View source data")).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.rawFhirFragment)
    }

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
}
