/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.data.formatters.medical

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonExtractor
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonGroup
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PrettyJsonExtractorTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var prettyJsonExtractor: PrettyJsonExtractor
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        hiltRule.inject()
    }

    // ***********************************************
    // Tests with not necessarily valid FHIR Jsons
    // ***********************************************

    @Test
    fun invalidJson_returnsEmptyList() {
        val invalidJson = """{ "key": "value" """
        val result = prettyJsonExtractor.extract(invalidJson)
        assertThat(result).isEmpty()
    }

    @Test
    fun checkTopLevelKeysVisibility() {
        val json =
            """{ "resourceType": "Patient", "text": "hidden", "meta": {"v": "1"}, "language": "en", "contained": [], "implicitRules": "uri", "name": "Example", "active": true }"""
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(3)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Resource Type: Patient")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Name: Example")
        assertGroupAtIndexHasLines(groups, 2, 0 to "Active: true")
    }

    @Test
    fun checkNestedKeysVisibility_nestedNotHidden() {
        val json =
            """
            {
                "resourceType": "Patient",
                "text": "Some narrative",
                "meta": {"versionId": "1"},
                "language": "en",
                "contained": [{"id":"c1"}],
                "implicitRules": "uri",
                "name": "Example",
                "active": true,
                "nested": {
                   "meta": { "v": 2},
                   "language": "es",
                   "text": "Nested Details"
                }
            }
            """
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(4)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Resource Type: Patient")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Name: Example")
        assertGroupAtIndexHasLines(groups, 2, 0 to "Active: true")
        assertGroupAtIndexHasLines(
            groups,
            3,
            0 to "Nested:",
            1 to "Meta:",
            2 to "V: 2",
            1 to "Language: es",
            1 to "Text: Nested Details",
        )
    }

    @Test
    fun preserveNestedHiddenKeys() {
        val json =
            """{ "name": "Example", "section": { "text": "Nested text", "code": "abc" } }"""
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(2)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Name: Example")
        assertGroupAtIndexHasLines(
            groups,
            1,
            0 to "Section:",
            1 to "Text: Nested text",
            1 to "Code: abc",
        )
    }

    @Test
    fun handleArrayWithPrimitives() {
        val json = """{ "tags": ["urgent", "review", "done"] }""".trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(
            groups,
            0,
            0 to "Tags:",
            1 to "urgent",
            1 to "review",
            1 to "done",
        )
    }

    @Test
    fun handleArraysOfObjects() {
        val json =
            """{ "items": [ { "name": "Item 1", "value": 1 }, { "name": "Item 2", "value": 2 } ] }"""
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(
            groups,
            0,
            0 to "Items:",
            1 to "Name: Item 1",
            1 to "Value: 1",
            1 to "", // Empty line separator
            1 to "Name: Item 2",
            1 to "Value: 2",
        )
    }

    @Test
    fun handleArrayWithMixedComplexAndPrimitive() {
        val json = """{ "mixed": [ {"a": 1}, "primitive", {"b": 2} ] }""".trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(1)
        // Array contains objects, so empty lines ARE inserted between ALL items after first
        assertGroupAtIndexHasLines(
            groups,
            0,
            0 to "Mixed:",
            1 to "A: 1",
            1 to "",
            1 to "primitive",
            1 to "",
            1 to "B: 2",
        )
    }

    @Test
    fun notExceedMaxDepth() {
        val json =
            """{ "rootKey": { "level1": { "level2": { "level3": { "level4": "Value at level 4" } } } } }"""
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(
            groups,
            0,
            0 to "Root Key:",
            1 to "Level1:",
            2 to "Level2:",
            3 to "Level3:", // Stops here, level4 key/value not processed
        )
    }

    @Test
    fun convertCamelCaseToProperCase() {
        val json =
            """{ "birthDate": "2000-01-01", "patientName": "John", "multipleWordKeyName": "Value", "aURL": "http://example.com" }"""
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(4)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Birth Date: 2000-01-01")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Patient Name: John")
        assertGroupAtIndexHasLines(groups, 2, 0 to "Multiple Word Key Name: Value")
        assertGroupAtIndexHasLines(groups, 3, 0 to "A URL: http://example.com")
    }

    @Test
    fun removeQuotesFromPrimitiveValuesOnly() {
        val json =
            """{ "stringValue": "This is a string", "stringWithQuotes": " There is a \"Quoted text inside\"" }"""
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(2)
        assertGroupAtIndexHasLines(groups, 0, 0 to "String Value: This is a string")
        assertGroupAtIndexHasLines(
            groups,
            1,
            0 to "String With Quotes: There is a \"Quoted text inside\"",
        )
    }

    @Test
    fun handleEmptyJson() {
        val result = prettyJsonExtractor.extract("{}")
        assertThat(result).isEmpty()
    }

    @Test
    fun handleEmptyNestedJson() {
        val json = """{ "patient": { }, "status": "active" }""".trimIndent()
        val groups = prettyJsonExtractor.extract(json)
        assertThat(groups).hasSize(2)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Patient:")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Status: active")
    }

    @Test
    fun handleEmptyArray() {
        val json = """ { "items": [] } """.trimIndent()
        val groups = prettyJsonExtractor.extract(json)
        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Items:")
    }

    @Test
    fun handleNestedEmptyArray() {
        val json = """{ "section": { "items": [] } }""".trimIndent()
        val groups = prettyJsonExtractor.extract(json)
        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Section:", 1 to "Items:")
    }

    @Test
    fun handleArrayWithNull_andComplexItem() {
        val json = """{ "items": [ null, { "name": "Item 2" } ] }""".trimIndent()
        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(
            groups,
            0,
            0 to "Items:",
            1 to "null",
            1 to "", // Separator because a complex item exists in the array
            1 to "Name: Item 2",
        )
    }

    @Test
    fun handleObjectWithNullValue() {
        val json = """{ "key1": null, "key2": "value2" } """.trimIndent()
        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(2)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Key1: null")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Key2: value2")
    }

    @Test
    fun handleArrayWithOnlyPrimitiveNulls() {
        val json = """{ "scores": [null, 10, null, null, 20] }""".trimIndent()
        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(
            groups,
            0,
            0 to "Scores:",
            1 to "null",
            1 to "10",
            1 to "null",
            1 to "null",
            1 to "20",
        ) // No empty line separators as it's an array of primitives/nulls
    }

    @Test
    fun handleArrayWithOnlyJSONObjectNULLs() {
        val json = """{ "dataPoints": [null, null, null] }""".trimIndent()
        val groups = prettyJsonExtractor.extract(json)
        assertThat(groups).hasSize(1)
        assertGroupAtIndexHasLines(
            groups,
            0,
            0 to "Data Points:",
            1 to "null",
            1 to "null",
            1 to "null",
        )
    }

    @Test
    fun handlePrimitiveTypeExtension() {
        val json =
            """
            {
              "name": "John",
              "_name": {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/given-name",
                    "valueString": "Jonny"
                  }
                ]
              }
            }
            """
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(2)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Name: John")
        assertGroupAtIndexHasLines(
            groups,
            1,
            // Current formatKey renders "_name" as "_name", not "_Name"
            0 to "_name:",
            1 to "Extension:",
            2 to "Url: http://hl7.org/fhir/StructureDefinition/given-name",
            2 to "Value String: Jonny",
        )
    }

    @Test
    fun handlePrimitiveTypeExtensionWithNullsInArrayAndFields() {
        val json =
            """
            {
              "birthDate": "1974-12-25",
              "_birthDate": {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/iso21090-uncertainty",
                    "valueCode": [null, "U", null]
                  },
                  {
                    "url": "http://example.com/some-other-flag",
                    "valueBoolean": null
                  },
                  {
                    "url": "http://example.com/empty-primitive-array",
                    "valueString": []
                  }
                ]
              }
            }
            """
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(2)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Birth Date: 1974-12-25")
        assertGroupAtIndexHasLines(
            groups,
            1,
            0 to "_birth Date:",
            1 to "Extension:",
            2 to "Url: http://hl7.org/fhir/StructureDefinition/iso21090-uncertainty",
            2 to "Value Code:", // Array of primitives/nulls
            3 to "null",
            3 to "U",
            3 to "null",
            2 to "",
            2 to "Url: http://example.com/some-other-flag",
            2 to "Value Boolean: null",
            2 to "",
            2 to "Url: http://example.com/empty-primitive-array",
            2 to "Value String:", // Empty array
        )
    }

    // ***********************************************
    // FHIR Resource Tests
    // ***********************************************

    @Test
    fun extractFromSimplifiedPatient() {
        val json =
            """
            {
              "resourceType": "Patient",
              "id": "example",
              "active": true,
              "name": [ { "use": "official", "family": "Chalmers", "given": [ "Peter", "James" ] }, { "use": "usual", "given": [ "Jim" ] } ],
              "gender": "male",
              "birthDate": "1974-12-25",
              "address": [ { "use": "home", "line": [ "534 Erewhon St" ], "city": "PleasantVille", "period": { "start": "1974-12-25", "end": "2000-01-01" } } ]
            }
            """
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(7)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Resource Type: Patient")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Id: example")
        assertGroupAtIndexHasLines(groups, 2, 0 to "Active: true")
        assertGroupAtIndexHasLines(
            groups,
            3,
            0 to "Name:",
            1 to "Use: official",
            1 to "Family: Chalmers",
            1 to "Given:", // Array contains only primitives -> NO empty lines
            2 to "Peter",
            2 to "James",
            1 to "", // Empty line (between complex name objects)
            1 to "Use: usual",
            1 to "Given:", // Array contains only one item -> NO empty lines
            2 to "Jim",
        )
        assertGroupAtIndexHasLines(groups, 4, 0 to "Gender: male")
        assertGroupAtIndexHasLines(groups, 5, 0 to "Birth Date: 1974-12-25")
        assertGroupAtIndexHasLines(
            groups,
            6,
            0 to "Address:",
            1 to "Use: home",
            1 to "Line:",
            2 to "534 Erewhon St",
            1 to "City: PleasantVille",
            1 to "Period:",
            2 to "Start: 1974-12-25",
            2 to "End: 2000-01-01",
        )
    }

    @Test
    fun extractFromSimplifiedObservation() {
        val json =
            """
        {
          "resourceType": "Observation",
          "id": "f001",
          "status": "final",
          "code": { "coding": [ { "system": "http://loinc.org", "code": "15074-8", "display": "Glucose" } ] },
          "subject": { "reference": "Patient/example" },
          "valueQuantity": { "value": 6.3, "unit": "mmol/l", "system": "http://unitsofmeasure.org" },
          "referenceRange": [ { "low": { "value": 3.1 }, "high": { "value": 6.2 } } ]
        }
        """
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(7)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Resource Type: Observation")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Id: f001")
        assertGroupAtIndexHasLines(groups, 2, 0 to "Status: final")
        assertGroupAtIndexHasLines(
            groups,
            3,
            0 to "Code:",
            1 to "Coding:",
            2 to "System: http://loinc.org",
            2 to "Code: 15074-8",
            2 to "Display: Glucose",
        )
        assertGroupAtIndexHasLines(groups, 4, 0 to "Subject:", 1 to "Reference: Patient/example")
        assertGroupAtIndexHasLines(
            groups,
            5,
            0 to "Value Quantity:",
            1 to "Value: 6.3",
            1 to "Unit: mmol/l",
            1 to "System: http://unitsofmeasure.org",
        )
        assertGroupAtIndexHasLines(
            groups,
            6,
            0 to "Reference Range:",
            1 to "Low:",
            2 to "Value: 3.1",
            1 to "High:",
            2 to "Value: 6.2",
        )
    }

    @Test
    fun patientA_immunization1() {
        val json =
            "{\n" +
                "  \"resourceType\": \"Immunization\",\n" +
                "  \"id\": \"immunization-1\",\n" +
                "  \"status\": \"completed\",\n" +
                "  \"vaccineCode\": {\n" +
                "    \"coding\": [\n" +
                "      {\n" +
                "        \"system\": \"http://hl7.org/fhir/sid/cvx\",\n" +
                "        \"code\": \"115\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"system\": \"http://hl7.org/fhir/sid/ndc\",\n" +
                "        \"code\": \"58160-842-11\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"text\": \"Tdap\"\n" +
                "  },\n" +
                "  \"patient\": {\n" +
                "    \"reference\": \"Patient/patient-1\",\n" +
                "    \"display\": \"Example, Anne\"\n" +
                "  },\n" +
                "  \"encounter\": {\n" +
                "    \"reference\": \"Encounter/encounter-unk\",\n" +
                "    \"display\": \"GP Visit\"\n" +
                "  },\n" +
                "  \"location\": {\n" +
                "    \"reference\": \"Location/location-unk\",\n" +
                "    \"display\": \"Nurse Room 1\"\n" +
                "  },\n" +
                "  \"occurrenceDateTime\": \"2018-05-21\",\n" +
                "  \"recorded\": \"2018-05-21T14:05:00Z\",\n" +
                "  \"primarySource\": true,\n" +
                "  \"manufacturer\": {\n" +
                "    \"display\": \"Sanofi Pasteur\"\n" +
                "  },\n" +
                "  \"lotNumber\": \"1\",\n" +
                "  \"site\": {\n" +
                "    \"coding\": [\n" +
                "      {\n" +
                "        \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActSite\",\n" +
                "        \"code\": \"LA\",\n" +
                "        \"display\": \"Left Arm\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"text\": \"Left Arm\"\n" +
                "  },\n" +
                "  \"route\": {\n" +
                "    \"coding\": [\n" +
                "      {\n" +
                "        \"system\": \"http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration\",\n" +
                "        \"code\": \"IM\",\n" +
                "        \"display\": \"Injection, intramuscular\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"text\": \"Injection, intramuscular\"\n" +
                "  },\n" +
                "  \"doseQuantity\": {\n" +
                "    \"value\": 0.5,\n" +
                "    \"unit\": \"mL\"\n" +
                "  },\n" +
                "  \"performer\": [\n" +
                "    {\n" +
                "      \"function\": {\n" +
                "        \"coding\": [\n" +
                "          {\n" +
                "            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0443\",\n" +
                "            \"code\": \"AP\",\n" +
                "            \"display\": \"Administering Provider\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"text\": \"Administering Provider\"\n" +
                "      },\n" +
                "      \"actor\": {\n" +
                "        \"reference\": \"Practitioner/practitioner-1\",\n" +
                "        \"type\": \"Practitioner\",\n" +
                "        \"display\": \"Dr Maria Hernandez\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"note\": [\n" +
                "    {\n" +
                "      \"text\": \"Patient given information leaflet and advised on signs of adverse reaction.\",\n" +
                "      \"authorString\": \"Dr Maria Hernandez\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"text\": \"Patients informed that vaccine protects against tetanus, diphtheria, and pertussis for up to 10 years, after which a booster is required.\",\n" +
                "      \"authorString\": \"Dr Maria Hernandez\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(17)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Resource Type: Immunization")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Id: immunization-1")
        assertGroupAtIndexHasLines(groups, 2, 0 to "Status: completed")
        assertGroupAtIndexHasLines(
            groups,
            3,
            0 to "Vaccine Code:",
            1 to "Coding:", // Array contains objects -> empty lines between items
            2 to "System: http://hl7.org/fhir/sid/cvx",
            2 to "Code: 115",
            2 to "",
            2 to "System: http://hl7.org/fhir/sid/ndc",
            2 to "Code: 58160-842-11",
            1 to "Text: Tdap",
        )
        assertGroupAtIndexHasLines(
            groups,
            4,
            0 to "Patient:",
            1 to "Reference: Patient/patient-1",
            1 to "Display: Example, Anne",
        )
        assertGroupAtIndexHasLines(
            groups,
            5,
            0 to "Encounter:",
            1 to "Reference: Encounter/encounter-unk",
            1 to "Display: GP Visit",
        )
        assertGroupAtIndexHasLines(
            groups,
            6,
            0 to "Location:",
            1 to "Reference: Location/location-unk",
            1 to "Display: Nurse Room 1",
        )
        assertGroupAtIndexHasLines(groups, 7, 0 to "Occurrence Date Time: 2018-05-21")
        assertGroupAtIndexHasLines(groups, 8, 0 to "Recorded: 2018-05-21T14:05:00Z")
        assertGroupAtIndexHasLines(groups, 9, 0 to "Primary Source: true")
        assertGroupAtIndexHasLines(groups, 10, 0 to "Manufacturer:", 1 to "Display: Sanofi Pasteur")
        assertGroupAtIndexHasLines(groups, 11, 0 to "Lot Number: 1")
        assertGroupAtIndexHasLines(
            groups,
            12,
            0 to "Site:",
            1 to "Coding:",
            2 to "System: http://terminology.hl7.org/CodeSystem/v3-ActSite",
            2 to "Code: LA",
            2 to "Display: Left Arm",
            1 to "Text: Left Arm",
        )
        assertGroupAtIndexHasLines(
            groups,
            13,
            0 to "Route:",
            1 to "Coding:",
            2 to "System: http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration",
            2 to "Code: IM",
            2 to "Display: Injection, intramuscular",
            1 to "Text: Injection, intramuscular",
        )
        assertGroupAtIndexHasLines(
            groups,
            14,
            0 to "Dose Quantity:",
            1 to "Value: 0.5",
            1 to "Unit: mL",
        )
        assertGroupAtIndexHasLines(
            groups,
            15,
            0 to "Performer:",
            1 to "Function:",
            2 to "Coding:",
            3 to "System: http://terminology.hl7.org/CodeSystem/v2-0443",
            3 to "Code: AP",
            3 to "Display: Administering Provider",
            2 to "Text: Administering Provider",
            1 to "Actor:",
            2 to "Reference: Practitioner/practitioner-1",
            2 to "Type: Practitioner",
            2 to "Display: Dr Maria Hernandez",
        )
        assertGroupAtIndexHasLines(
            groups,
            16,
            0 to "Note:",
            1 to
                "Text: Patient given information leaflet and advised on signs of adverse reaction.",
            1 to "Author String: Dr Maria Hernandez",
            1 to "",
            1 to
                "Text: Patients informed that vaccine protects against tetanus, diphtheria, and pertussis for up to 10 years, after which a booster is required.",
            1 to "Author String: Dr Maria Hernandez",
        )
    }

    @Test
    fun patientA_observation1() {
        val json =
            "{\n" +
                "  \"resourceType\": \"Observation\",\n" +
                "  \"id\": \"observation-1\",\n" +
                "  \"identifier\": [\n" +
                "    {\n" +
                "      \"use\": \"official\",\n" +
                "      \"system\": \"http://example.laboratory.com\",\n" +
                "      \"value\": \"1001\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"basedOn\": [\n" +
                "    {\n" +
                "      \"reference\": \"ServiceRequest/servicerequest_unk\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"status\": \"final\",\n" +
                "  \"category\": [\n" +
                "    {\n" +
                "      \"coding\": [\n" +
                "        {\n" +
                "          \"system\": \"http://terminology.hl7.org/CodeSystem/observation-category\",\n" +
                "          \"code\": \"laboratory\",\n" +
                "          \"display\": \"Laboratory\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"text\": \"Laboratory\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"coding\": [\n" +
                "        {\n" +
                "          \"system\": \"urn:oid:1.2.345.6789\",\n" +
                "          \"code\": \"Lab\",\n" +
                "          \"display\": \"Lab\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"text\": \"Laboratory\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"code\": {\n" +
                "    \"coding\": [\n" +
                "      {\n" +
                "        \"system\": \"http://loinc.org\",\n" +
                "        \"code\": \"4548-4\",\n" +
                "        \"display\": \"Hemoglobin A1c/Hemoglobin.total in Blood\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"system\": \"urn:oid:1.2.345.6789\",\n" +
                "        \"code\": \"10001\",\n" +
                "        \"display\": \"Hemoglobin A1c %\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"text\": \"Hemoglobin A1c/Hemoglobin.total in Blood\"\n" +
                "  },\n" +
                "  \"subject\": {\n" +
                "    \"reference\": \"Patient/patient-1\",\n" +
                "    \"display\": \"Example, Anne\"\n" +
                "  },\n" +
                "  \"encounter\": {\n" +
                "    \"reference\": \"Encounter/encounter-5\",\n" +
                "    \"identifier\": {\n" +
                "      \"use\": \"usual\",\n" +
                "      \"system\": \"urn:oid:1.2.345.6789\",\n" +
                "      \"value\": \"10005\"\n" +
                "    },\n" +
                "    \"display\": \"Endocrine outpatient appointment\"\n" +
                "  },\n" +
                "  \"performer\": [\n" +
                "    {\n" +
                "      \"display\": \"Outpatient Phlebotomist\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"effectiveDateTime\": \"2022-09-28T09:48:00Z\",\n" +
                "  \"issued\": \"2022-09-28T13:22:41Z\",\n" +
                "  \"valueQuantity\": {\n" +
                "    \"value\": 6.8,\n" +
                "    \"unit\": \"%\",\n" +
                "    \"system\": \"http://unitsofmeasure.org\",\n" +
                "    \"code\": \"%\"\n" +
                "  },\n" +
                "  \"referenceRange\": [\n" +
                "    {\n" +
                "      \"low\": {\n" +
                "        \"value\": 4.0,\n" +
                "        \"unit\": \"%\",\n" +
                "        \"system\": \"http://unitsofmeasure.org\",\n" +
                "        \"code\": \"%\"\n" +
                "      },\n" +
                "      \"high\": {\n" +
                "        \"value\": 5.6,\n" +
                "        \"unit\": \"%\",\n" +
                "        \"system\": \"http://unitsofmeasure.org\",\n" +
                "        \"code\": \"%\"\n" +
                "      },\n" +
                "      \"type\": {\n" +
                "        \"coding\": [\n" +
                "          {\n" +
                "            \"system\": \"http://terminology.hl7.org/CodeSystem/referencerange-meaning\",\n" +
                "            \"code\": \"normal\",\n" +
                "            \"display\": \"Normal Range\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"appliesTo\": [\n" +
                "        {\n" +
                "          \"coding\": [\n" +
                "            {\n" +
                "              \"system\": \"http://snomed.info/sct\",\n" +
                "              \"code\": \"248152002\",\n" +
                "              \"display\": \"Female\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"age\": {\n" +
                "        \"low\": {\n" +
                "          \"value\": 18,\n" +
                "          \"unit\": \"year\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"text\": \"Normal range: 4.0-5.6%\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"interpretation\": [\n" +
                "    {\n" +
                "      \"coding\": [\n" +
                "        {\n" +
                "          \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation\",\n" +
                "          \"code\": \"H\",\n" +
                "          \"display\": \"High\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"specimen\": {\n" +
                "    \"reference\": \"Specimen/specimen_unk\",\n" +
                "    \"display\": \"Specimen\"\n" +
                "  },\n" +
                "  \"note\": [\n" +
                "    {\n" +
                "      \"text\": \"No prior history of diabetes mellitus.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(17)

        assertGroupAtIndexHasLines(groups, 0, 0 to "Resource Type: Observation")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Id: observation-1")
        assertGroupAtIndexHasLines(
            groups,
            2,
            0 to "Identifier:",
            1 to "Use: official",
            1 to "System: http://example.laboratory.com",
            1 to "Value: 1001",
        )
        assertGroupAtIndexHasLines(
            groups,
            3,
            0 to "Based On:",
            1 to "Reference: ServiceRequest/servicerequest_unk",
        )
        assertGroupAtIndexHasLines(groups, 4, 0 to "Status: final")
        assertGroupAtIndexHasLines(
            groups,
            5,
            0 to "Category:",
            1 to "Coding:",
            2 to "System: http://terminology.hl7.org/CodeSystem/observation-category",
            2 to "Code: laboratory",
            2 to "Display: Laboratory",
            1 to "Text: Laboratory",
            1 to "",
            1 to "Coding:",
            2 to "System: urn:oid:1.2.345.6789",
            2 to "Code: Lab",
            2 to "Display: Lab",
            1 to "Text: Laboratory",
        )
        assertGroupAtIndexHasLines(
            groups,
            6,
            0 to "Code:",
            1 to "Coding:",
            2 to "System: http://loinc.org",
            2 to "Code: 4548-4",
            2 to "Display: Hemoglobin A1c/Hemoglobin.total in Blood",
            2 to "",
            2 to "System: urn:oid:1.2.345.6789",
            2 to "Code: 10001",
            2 to "Display: Hemoglobin A1c %",
            1 to "Text: Hemoglobin A1c/Hemoglobin.total in Blood",
        )
        assertGroupAtIndexHasLines(
            groups,
            7,
            0 to "Subject:",
            1 to "Reference: Patient/patient-1",
            1 to "Display: Example, Anne",
        )
        assertGroupAtIndexHasLines(
            groups,
            8,
            0 to "Encounter:",
            1 to "Reference: Encounter/encounter-5",
            1 to "Identifier:",
            2 to "Use: usual",
            2 to "System: urn:oid:1.2.345.6789",
            2 to "Value: 10005",
            1 to "Display: Endocrine outpatient appointment",
        )
        assertGroupAtIndexHasLines(
            groups,
            9,
            0 to "Performer:",
            1 to "Display: Outpatient Phlebotomist",
        )
        assertGroupAtIndexHasLines(groups, 10, 0 to "Effective Date Time: 2022-09-28T09:48:00Z")
        assertGroupAtIndexHasLines(groups, 11, 0 to "Issued: 2022-09-28T13:22:41Z")
        assertGroupAtIndexHasLines(
            groups,
            12,
            0 to "Value Quantity:",
            1 to "Value: 6.8",
            1 to "Unit: %",
            1 to "System: http://unitsofmeasure.org",
            1 to "Code: %",
        )
        assertGroupAtIndexHasLines(
            groups,
            13,
            0 to "Reference Range:",
            1 to "Low:",
            2 to "Value: 4.0",
            2 to "Unit: %",
            2 to "System: http://unitsofmeasure.org",
            2 to "Code: %",
            1 to "High:",
            2 to "Value: 5.6",
            2 to "Unit: %",
            2 to "System: http://unitsofmeasure.org",
            2 to "Code: %",
            1 to "Type:",
            2 to "Coding:",
            3 to "System: http://terminology.hl7.org/CodeSystem/referencerange-meaning",
            3 to "Code: normal",
            3 to "Display: Normal Range",
            1 to "Applies To:",
            2 to "Coding:",
            3 to "System: http://snomed.info/sct",
            3 to "Code: 248152002",
            3 to "Display: Female",
            1 to "Age:",
            2 to "Low:",
            3 to "Value: 18",
            3 to "Unit: year",
            1 to "Text: Normal range: 4.0-5.6%",
        )
        assertGroupAtIndexHasLines(
            groups,
            14,
            0 to "Interpretation:",
            1 to "Coding:",
            2 to "System: http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
            2 to "Code: H",
            2 to "Display: High",
        )
        assertGroupAtIndexHasLines(
            groups,
            15,
            0 to "Specimen:",
            1 to "Reference: Specimen/specimen_unk",
            1 to "Display: Specimen",
        )
        assertGroupAtIndexHasLines(
            groups,
            16,
            0 to "Note:",
            1 to "Text: No prior history of diabetes mellitus.",
        )
    }

    @Test
    fun patientA_medication1() {
        val json =
            """
        {
          "resourceType": "Medication",
          "id": "medication-1",
          "identifier": [
            {
              "use": "usual",
              "system": "urn:oid:1.2.345.6789",
              "value": "10001"
            }
          ],
          "code": {
            "coding": [
              {
                "system": "http://snomed.info/sct",
                "code": "1145423002",
                "display": "Azithromycin 250 mg oral tablet"
              },
              {
                "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                "code": "308460",
                "display": "azithromycin 250 MG Oral Tablet"
              },
              {
                "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                "code": "212446",
                "display": "Zithromax 250 MG Oral Tablet"
              }
            ]
          },
          "form": {
            "coding": [
              {
                "system": "http://snomed.info/sct",
                "code": "385055001",
                "display": "Tablet"
              },
              {
                "system": "urn:oid:1.2.345.6789",
                "code": "TABS",
                "display": "tablet"
              }
            ],
            "text": "tablet"
          },
          "ingredient": [
            {
              "itemCodeableConcept": {
                "coding": [
                  {
                    "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                    "code": "18631",
                    "display": "azithromycin"
                  }
                ]
              },
              "isActive": true,
              "strength": {
                "numerator": {
                  "value": 250,
                  "system": "http://unitsofmeasure.org",
                  "code": "mg"
                },
                "denominator": {
                  "value": 1,
                  "system": "http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm",
                  "code": "TAB"
                }
              }
            }
          ],
          "batch": {
            "lotNumber": "123456",
            "expirationDate": "2025-07-31"
          }
        }
        """
                .trimIndent()

        val groups = prettyJsonExtractor.extract(json)

        assertThat(groups).hasSize(7)
        assertGroupAtIndexHasLines(groups, 0, 0 to "Resource Type: Medication")
        assertGroupAtIndexHasLines(groups, 1, 0 to "Id: medication-1")
        assertGroupAtIndexHasLines(
            groups,
            2,
            0 to "Identifier:",
            1 to "Use: usual",
            1 to "System: urn:oid:1.2.345.6789",
            1 to "Value: 10001",
        )
        assertGroupAtIndexHasLines(
            groups,
            3,
            0 to "Code:",
            1 to "Coding:",
            2 to "System: http://snomed.info/sct",
            2 to "Code: 1145423002",
            2 to "Display: Azithromycin 250 mg oral tablet",
            2 to "", // Empty line separator
            2 to "System: http://www.nlm.nih.gov/research/umls/rxnorm",
            2 to "Code: 308460",
            2 to "Display: azithromycin 250 MG Oral Tablet",
            2 to "", // Empty line separator
            2 to "System: http://www.nlm.nih.gov/research/umls/rxnorm",
            2 to "Code: 212446",
            2 to "Display: Zithromax 250 MG Oral Tablet",
        )
        assertGroupAtIndexHasLines(
            groups,
            4,
            0 to "Form:",
            1 to "Coding:",
            2 to "System: http://snomed.info/sct",
            2 to "Code: 385055001",
            2 to "Display: Tablet",
            2 to "", // Empty line separator
            2 to "System: urn:oid:1.2.345.6789",
            2 to "Code: TABS",
            2 to "Display: tablet",
            1 to "Text: tablet",
        )
        assertGroupAtIndexHasLines(
            groups,
            5,
            0 to "Ingredient:",
            1 to "Item Codeable Concept:",
            2 to "Coding:",
            3 to "System: http://www.nlm.nih.gov/research/umls/rxnorm",
            3 to "Code: 18631",
            3 to "Display: azithromycin",
            1 to "Is Active: true",
            1 to "Strength:",
            2 to "Numerator:",
            3 to "Value: 250",
            3 to "System: http://unitsofmeasure.org",
            3 to "Code: mg",
            2 to "Denominator:",
            3 to "Value: 1",
            3 to "System: http://terminology.hl7.org/CodeSystem/v3-orderableDrugForm",
            3 to "Code: TAB",
        )
        assertGroupAtIndexHasLines(
            groups,
            6,
            0 to "Batch:",
            1 to "Lot Number: 123456",
            1 to "Expiration Date: 2025-07-31",
        )
    }

    /** Asserts content of a PrettyJsonGroup, expecting only lines. */
    private fun assertGroupLines(
        group: PrettyJsonGroup?,
        groupIndexForContext: Int, // Index needed for messages
        vararg expectedDepthLinePairs: Pair<Int, String>,
    ) {
        assertWithMessage("Group at index $groupIndexForContext should not be null")
            .that(group)
            .isNotNull()

        val actualLines = group!!.nestedLines
        val expectedSize = expectedDepthLinePairs.size

        assertWithMessage(
                "Group at index $groupIndexForContext: Incorrect number of lines. Expected $expectedSize, got ${actualLines.size}"
            )
            .that(actualLines.size)
            .isEqualTo(expectedSize)

        expectedDepthLinePairs.forEachIndexed { lineIndex, (expectedDepth, expectedLine) ->
            val actualLine = actualLines.getOrNull(lineIndex)

            assertWithMessage(
                    "Group $groupIndexForContext, Line index $lineIndex: Should not be null (expected $expectedSize lines total)"
                )
                .that(actualLine)
                .isNotNull()

            assertWithMessage(
                    "Group $groupIndexForContext, Line index $lineIndex: Line content mismatch."
                )
                .that(actualLine!!.line)
                .isEqualTo(expectedLine)

            assertWithMessage(
                    "Group $groupIndexForContext, Line index $lineIndex: Depth mismatch for line content '${actualLine.line}'"
                )
                .that(actualLine.depth)
                .isEqualTo(expectedDepth)
        }
    }

    /**
     * Gets group at the specified index and calls assertGroupLines with context.
     *
     * @param groups The list of PrettyJsonGroup.
     * @param index The index of the group to check.
     * @param expectedDepthLinePairs The expected sequence of (Depth, Line) pairs for the group at
     *   the index.
     */
    private fun assertGroupAtIndexHasLines(
        groups: List<PrettyJsonGroup>,
        index: Int,
        vararg expectedDepthLinePairs: Pair<Int, String>,
    ) {
        assertWithMessage("Groups list should not be empty when checking index $index")
            .that(groups)
            .isNotEmpty()

        assertWithMessage("Index $index out of bounds for group list size ${groups.size}")
            .that(index)
            .isLessThan(groups.size)

        assertGroupLines(groups[index], index, *expectedDepthLinePairs)
    }
}
