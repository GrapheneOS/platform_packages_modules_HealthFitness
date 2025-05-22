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
package com.android.healthconnect.controller.tests.data.rawfhir

import android.content.Context
import android.health.connect.datatypes.FhirResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.rawfhir.RawFhirFormatter
import com.android.healthconnect.controller.tests.utils.TEST_FHIR_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TEST_FHIR_RESOURCE_IMMUNIZATION_LONG
import com.android.healthconnect.controller.tests.utils.TEST_FHIR_RESOURCE_INVALID_JSON
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RawFhirFormatterTest {
    private lateinit var rawFhirFormatter: RawFhirFormatter
    @get:Rule val hiltRule = HiltAndroidRule(this)
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        rawFhirFormatter = RawFhirFormatter(context)
    }

    @Test
    fun invalidJson_returnsTextUnchanged() {
        // Health Connect only stores valid JSONs so this should not occur.
        assertThat(rawFhirFormatter.format(TEST_FHIR_RESOURCE_INVALID_JSON))
            .isEqualTo(TEST_FHIR_RESOURCE_INVALID_JSON.data)
    }

    @Test
    fun emptyJson_returnsTextUnchanged() {
        // Health Connect only stores valid JSONs so this should not occur.
        assertThat(
                rawFhirFormatter.format(
                    FhirResource.Builder(
                            FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            "invalid_json",
                            "",
                        )
                        .build()
                )
            )
            .isEqualTo("")
    }

    @Test
    fun bracketsNestedIncorrectly_returnsTextUnchanged() {
        // Health Connect only stores valid JSONs so this should not occur.
        val invalidJson = "{\"resourceType\" : \"Immunization\"[, \"id\" : \"Immunization_3\"}]"
        assertThat(
                rawFhirFormatter.format(
                    FhirResource.Builder(
                            FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            "invalid_json",
                            invalidJson,
                        )
                        .build()
                )
            )
            .isEqualTo(invalidJson)
    }

    @Test
    fun incorrectQuotationMarks_returnsTextUnchanged() {
        // Health Connect only stores valid JSONs so this should not occur.
        val invalidJson = "{\"resourceType : \"Immunization\", \"id\" : \"Immunization_3\"}"
        assertThat(
                rawFhirFormatter.format(
                    FhirResource.Builder(
                            FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            "invalid_json",
                            invalidJson,
                        )
                        .build()
                )
            )
            .isEqualTo(invalidJson)
    }

    @Test
    fun specialCharactersOnly_returnsFormattedText() {
        // Health Connect only stores valid FHIR resources so this should not occur.
        val invalidJson = "{:::::::}"
        assertThat(
                rawFhirFormatter.format(
                    FhirResource.Builder(
                            FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            "invalid_json",
                            invalidJson,
                        )
                        .build()
                )
            )
            .isEqualTo("{:::::::}")
    }

    @Test
    fun smallImmunization_formatCorrectly() {
        assertThat(rawFhirFormatter.format(TEST_FHIR_RESOURCE_IMMUNIZATION))
            .isEqualTo(
                "{\n" +
                        "    \"resourceType\": \"Immunization\",\n" +
                        "    \"id\": \"immunization-1\",\n" +
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
                        "    \"occurrenceDateTime\": \"2018-05-21\"\n" +
                        "}"
            )
    }

    @Test
    fun longImmunization_formatCorrectly() {
        assertThat(rawFhirFormatter.format(TEST_FHIR_RESOURCE_IMMUNIZATION_LONG))
            .isEqualTo(
                "{\n" +
                    "    \"resourceType\": \"Immunization\",\n" +
                    "    \"id\": \"immunization-1\",\n" +
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
            )
    }

    @Test
    fun contentDescription_bracketsNestedIncorrectly_returnsTextUnchanged() {
        // Health Connect only stores valid JSONs so this should not occur.
        val invalidJson = "{\"resourceType\" : \"Immunization\"[, \"id\" : \"Immunization_3\"}]"
        assertThat(
                rawFhirFormatter.fhirContentDescription(
                    FhirResource.Builder(
                            FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            "invalid_json",
                            invalidJson,
                        )
                        .build()
                )
            )
            .isEqualTo(invalidJson)
    }

    @Test
    fun contentDescription_smallImmunization_formatCorrectly() {
        assertThat(rawFhirFormatter.fhirContentDescription(TEST_FHIR_RESOURCE_IMMUNIZATION))
            .isEqualTo(
                "Detailed source code: Open bracket. Field resourceType Value: Immunization. Field id Value: immunization-1. Field status Value: completed. Field vaccineCode ValueDetailed source code:   Open bracket.   Field coding ValueDetailed source code:     Open bracket.     Field system Value    : http://hl7.org/fhir/sid/cvx.     Field code Value    : 115.    Closed bracket.Detailed source code:     Open bracket.     Field system Value    : http://hl7.org/fhir/sid/ndc.     Field code Value    : 58160-842-11.    Closed bracket.   Field text Value  : Tdap.  Closed bracket. Field patient ValueDetailed source code:   Open bracket.   Field reference Value  : Patient/patient_1.   Field display Value  : Example, Anne.  Closed bracket. Field occurrenceDateTime Value: 2018-05-21.Closed bracket."
            )
    }

    @Test
    fun contentDescription_longImmunization_formatCorrectly() {
        assertThat(rawFhirFormatter.fhirContentDescription(TEST_FHIR_RESOURCE_IMMUNIZATION_LONG))
            .isEqualTo(
                "Detailed source code: Open bracket. Field resourceType Value: Immunization. Field id Value: immunization-1. Field status Value: completed. Field vaccineCode ValueDetailed source code:   Open bracket.   Field coding ValueDetailed source code:     Open bracket.     Field system Value    : http://hl7.org/fhir/sid/cvx.     Field code Value    : 115.    Closed bracket.Detailed source code:     Open bracket.     Field system Value    : http://hl7.org/fhir/sid/ndc.     Field code Value    : 58160-842-11.    Closed bracket.   Field text Value  : Tdap.  Closed bracket. Field patient ValueDetailed source code:   Open bracket.   Field reference Value  : Patient/patient_1.   Field display Value  : Example, Anne.  Closed bracket. Field encounter ValueDetailed source code:   Open bracket.   Field reference Value  : Encounter/encounter_unk.   Field display Value  : GP Visit.  Closed bracket. Field occurrenceDateTime Value: 2018-05-21. Field primarySource Value: true. Field manufacturer ValueDetailed source code:   Open bracket.   Field display Value  : Sanofi Pasteur.  Closed bracket. Field lotNumber Value: 1. Field site ValueDetailed source code:   Open bracket.   Field coding ValueDetailed source code:     Open bracket.     Field system Value    : http://terminology.hl7.org/CodeSystem/v3-ActSite.     Field code Value    : LA.     Field display Value    : Left Arm.    Closed bracket.   Field text Value  : Left Arm.  Closed bracket. Field route ValueDetailed source code:   Open bracket.   Field coding ValueDetailed source code:     Open bracket.     Field system Value    : http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration.     Field code Value    : IM.     Field display Value    : Injection, intramuscular.    Closed bracket.   Field text Value  : Injection, intramuscular.  Closed bracket. Field doseQuantity ValueDetailed source code:   Open bracket.   Field value Value  : 0.5.   Field unit Value  : mL.  Closed bracket. Field performer ValueDetailed source code:   Open bracket.   Field function ValueDetailed source code:     Open bracket.     Field coding ValueDetailed source code:       Open bracket.       Field system Value      : http://terminology.hl7.org/CodeSystem/v2-0443.       Field code Value      : AP.       Field display Value      : Administering Provider.      Closed bracket.     Field text Value    : Administering Provider.    Closed bracket.   Field actor ValueDetailed source code:     Open bracket.     Field reference Value    : Practitioner/practitioner_1.     Field type Value    : Practitioner.     Field display Value    : Dr Maria Hernandez.    Closed bracket.  Closed bracket.Closed bracket."
            )
    }
}
