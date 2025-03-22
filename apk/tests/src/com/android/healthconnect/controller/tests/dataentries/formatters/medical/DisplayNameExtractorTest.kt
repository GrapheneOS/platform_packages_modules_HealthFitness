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
package com.android.healthconnect.controller.tests.dataentries.formatters.medical

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.dataentries.formatters.medical.DisplayNameExtractor
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class DisplayNameExtractorTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var displayNameExtractor: DisplayNameExtractor
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context

        hiltRule.inject()
    }

    @Test
    fun unknownResourceType() {
        val json =
            """{
            "resourceType": "UnknownResource"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    // AllergyIntolerance tests
    @Test
    fun allergyIntolerance_withoutCodeField() {
        val json =
            """{
            "resourceType": "AllergyIntolerance"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun allergyIntolerance_withEmptyCodeField() {
        val json =
            """{
            "resourceType": "AllergyIntolerance",
            "code": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun allergyIntolerance_withCodeText() {
        val json =
            """{
            "resourceType": "AllergyIntolerance",
            "code": {"text": "Peanut Allergy"}
        }"""
        assertEquals("Peanut Allergy", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun allergyIntolerance_withSnomedCoding() {
        val json =
            """{
            "resourceType": "AllergyIntolerance",
            "code": {
                "coding": [{"system": "http://snomed.info/sct", "display": "Dust Allergy"}]
            }
        }"""
        assertEquals("Dust Allergy", displayNameExtractor.getDisplayName(json))
    }

    // Condition tests
    @Test
    fun condition_withoutCodeField() {
        val json =
            """{
            "resourceType": "Condition"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun condition_withEmptyCodeField() {
        val json =
            """{
            "resourceType": "Condition",
            "code": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun condition_withCodeText() {
        val json =
            """{
            "resourceType": "Condition",
            "code": {"text": "Hypertension"}
        }"""
        assertEquals("Hypertension", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun condition_withCodingSystemSnomed() {
        val json =
            """{
            "resourceType": "Condition",
            "code": {
                "coding": [{"system": "http://snomed.info/sct", "display": "Diabetes"}]
            }
        }"""
        assertEquals("Diabetes", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun condition_withMultipleCodings() {
        val json =
            """{
            "resourceType": "Condition",
            "code": {
                "coding": [
                    {"system": "http://other.system", "display": "Other Condition"},
                    {"system": "http://snomed.info/sct", "display": "Asthma"}
                ]
            }
        }"""
        assertEquals("Asthma", displayNameExtractor.getDisplayName(json))
    }

    // Procedure tests
    @Test
    fun procedure_withoutCodeField() {
        val json =
            """{
            "resourceType": "Procedure"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun procedure_withEmptyCodeField() {
        val json =
            """{
            "resourceType": "Procedure",
            "code": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun procedure_withCodeText() {
        val json =
            """{
            "resourceType": "Procedure",
            "code": {"text": "Excision of appendix"}
        }"""
        assertEquals("Excision of appendix", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun procedure_withCodingSystemSnomed() {
        val json =
            """{
            "resourceType": "Procedure",
            "code": {
                "coding": [{"system": "http://snomed.info/sct", "display": "Excision of appendix"}]
            }
        }"""
        assertEquals("Excision of appendix", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun procedure_withMultipleCodings() {
        val json =
            """{
            "resourceType": "Procedure",
            "code": {
                "coding": [
                    {"system": "http://other.system", "display": "Appendicectomy"},
                    {"system": "http://snomed.info/sct", "display": "Excision of appendix"}
                ]
            }
        }"""
        assertEquals("Excision of appendix", displayNameExtractor.getDisplayName(json))
    }

    // Encounter tests
    @Test
    fun encounter_withoutClassOrServiceType() {
        val json =
            """{
            "resourceType": "Encounter"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun encounter_withEmptyClassAndServiceType() {
        val json =
            """{
            "resourceType": "Encounter",
            "class": {},
            "serviceType": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun encounter_withClassAndServiceType() {
        val json =
            """{
            "resourceType": "Encounter",
            "class": {"display": "Inpatient"},
            "serviceType": {"text": "Cardiology"}
        }"""
        assertEquals("Inpatient - Cardiology", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun encounter_withClassAndServiceTypeCoding() {
        val json =
            """{
            "resourceType": "Encounter",
            "class": {"display": "Outpatient"},
            "serviceType": {"coding": [{"display": "Radiology"}]}
        }"""
        assertEquals("Outpatient - Radiology", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun encounter_withClassWithoutServiceType() {
        val json =
            """{
            "resourceType": "Encounter",
            "class": {"display": "Outpatient"}
        }"""
        assertEquals("Outpatient", displayNameExtractor.getDisplayName(json))
    }

    // Immunization tests
    @Test
    fun immunization_withoutVaccineCodeField() {
        val json =
            """{
            "resourceType": "Immunization"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun immunization_withEmptyVaccineCodeField() {
        val json =
            """{
            "resourceType": "Immunization",
            "vaccineCode": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun immunization_withVaccineCodeText() {
        val json =
            """{
            "resourceType": "Immunization",
            "vaccineCode": {"text": "COVID-19 Vaccine"}
        }"""
        assertEquals("COVID-19 Vaccine", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun immunization_withVaccineCoding() {
        val json =
            """{
            "resourceType": "Immunization",
            "vaccineCode": {
                "coding": [{"system": "http://hl7.org/fhir/sid/cvx", "display": "Influenza"}]
            }
        }"""
        assertEquals("Influenza", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun immunization_withCVXAndSnomedCoding() {
        val json =
            """{
            "resourceType": "Immunization",
            "vaccineCode": {
                "coding": [
                    {"system": "http://snomed.info/sct", "display": "Influenza vaccine"},
                    {"system": "http://hl7.org/fhir/sid/cvx", "display": "Influenza"}
                ]
            }
        }"""
        assertEquals("Influenza", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun immunization_withSnomedAndOtherCoding() {
        val json =
            """{
            "resourceType": "Immunization",
            "vaccineCode": {
                "coding": [
                    {"system": "http://other.system", "display": "Other Vaccine"},
                    {"system": "http://snomed.info/sct", "display": "Influenza vaccine"}
                ]
            }
        }"""
        assertEquals("Influenza vaccine", displayNameExtractor.getDisplayName(json))
    }

    // Location tests
    @Test
    fun location_withoutNameOrAliasField() {
        val json =
            """{
            "resourceType": "Location"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun location_withEmptyAliasField() {
        val json =
            """{
            "resourceType": "Location",
            "alias": []
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun location_withName() {
        val json =
            """{
            "resourceType": "Location",
            "name": "Main Hospital"
        }"""
        assertEquals("Main Hospital", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun location_withAlias() {
        val json =
            """{
            "resourceType": "Location",
            "alias": ["Emergency Room"]
        }"""
        assertEquals("Emergency Room", displayNameExtractor.getDisplayName(json))
    }

    // MedicationRequest tests
    @Test
    fun medicationRequest_withoutMedicationField() {
        val json =
            """{
            "resourceType": "MedicationRequest"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medicationRequest_withEmptyMedicationCodeableConcept() {
        val json =
            """{
            "resourceType": "MedicationRequest",
            "medicationCodeableConcept": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medicationRequest_withMedicationText() {
        val json =
            """{
            "resourceType": "MedicationRequest",
            "medicationCodeableConcept": {"text": "Aspirin"}
        }"""
        assertEquals("Aspirin", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medicationRequest_withSnomedCoding() {
        val json =
            """{
            "resourceType": "MedicationRequest",
            "medicationCodeableConcept": {
                "coding": [{"system": "http://snomed.info/sct", "display": "Ibuprofen"}]
            }
        }"""
        assertEquals("Ibuprofen", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medicationRequest_withMedicationReferenceText() {
        val json =
            """{
            "resourceType": "MedicationRequest",
            "medicationReference": {
                "reference": "medication-1",
                "display": "Azithromycin 250mg"
            }
        }"""
        assertEquals("Azithromycin 250mg", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medicationRequest_withMedicationReferenceReferenceOnly() {
        val json =
            """{
            "resourceType": "MedicationRequest",
            "medicationReference": {
                "reference": "medication-1"
            }
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    // Medication tests
    @Test
    fun medication_withoutCodeField() {
        val json =
            """{
            "resourceType": "Medication"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medication_withEmptyCodeField() {
        val json =
            """{
            "resourceType": "Medication",
            "code": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medication_withCodeText() {
        val json =
            """{
            "resourceType": "Medication",
            "code": {"text": "Paracetamol"}
        }"""
        assertEquals("Paracetamol", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun medication_withSnomedCoding() {
        val json =
            """{
            "resourceType": "Medication",
            "code": {
                "coding": [{"system": "http://snomed.info/sct", "display": "Amoxicillin"}]
            }
        }"""
        assertEquals("Amoxicillin", displayNameExtractor.getDisplayName(json))
    }

    // Observation tests
    @Test
    fun observation_withoutCodeField() {
        val json =
            """{
            "resourceType": "Observation"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun observation_withEmptyCodeField() {
        val json =
            """{
            "resourceType": "Observation",
            "code": {}
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun observation_withLoincCoding() {
        val json =
            """{
            "resourceType": "Observation",
            "code": {
                "coding": [{"system": "http://loinc.org", "display": "Hemoglobin"}]
            }
        }"""
        assertEquals("Hemoglobin", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun observation_withSnomedCoding() {
        val json =
            """{
            "resourceType": "Observation",
            "code": {
                "coding": [{"system": "http://snomed.info/sct", "display": "Blood Pressure"}]
            }
        }"""
        assertEquals("Blood Pressure", displayNameExtractor.getDisplayName(json))
    }

    // Patient tests
    @Test
    fun patient_withoutNameField() {
        val json =
            """{
            "resourceType": "Patient"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun patient_withEmptyNameField() {
        val json =
            """{
            "resourceType": "Patient",
            "name": []
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun patient_withNameText() {
        val json =
            """{
            "resourceType": "Patient",
            "name": [
                {"use": "maiden","text": "Anne Onymous"},
                {"use": "usual", "text": "Anne Example"}
            ]
        }"""
        assertEquals("Anne Example", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun patient_withNameFamilyAndGiven() {
        val json =
            """{
            "resourceType": "Patient",
            "name": [{
                "family": "Example",
                "given": ["Anne", "Other"]
            }]
        }"""
        assertEquals("Anne Example", displayNameExtractor.getDisplayName(json))
    }

    // Practitioner tests
    @Test
    fun practitioner_withoutNameField() {
        val json =
            """{
            "resourceType": "Practitioner"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun practitioner_withEmptyNameField() {
        val json =
            """{
            "resourceType": "Practitioner",
            "name": []
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun practitioner_withNameText() {
        val json =
            """{
            "resourceType": "Practitioner",
            "name": [
                {"use": "old","text": "M Hernandez"},
                {"use": "usual", "text": "Dr Maria Hernandez"}
            ]
        }"""
        assertEquals("Dr Maria Hernandez", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun practitioner_withNamePrefixFamilyAndGiven() {
        val json =
            """{
            "resourceType": "Practitioner",
            "name": [{
                "family": "Hernandez",
                "given": ["Maria"],
                "prefix": ["Dr"]
            }]
        }"""
        assertEquals("Dr Maria Hernandez", displayNameExtractor.getDisplayName(json))
    }

    // PractitionerRole tests
    @Test
    fun practitionerRole_withoutCodeOrSpecialty() {
        val json =
            """{
            "resourceType": "PractitionerRole"
        }"""
        assertEquals("(No title)", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun practitionerRole_withCodeWithoutSpecialty() {
        val json =
            """{
            "resourceType": "PractitionerRole",
            "code": [{
                "coding": [{
                    "system": "http://terminology.hl7.org/CodeSystem/practitioner-role",
                    "code": "doctor",
                    "display": "Doctor"
                }]
            }]
        }"""
        assertEquals("Doctor", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun practitionerRole_withSpecialtyWithoutCode() {
        val json =
            """{
            "resourceType": "PractitionerRole",
            "specialty": [{
                "coding": [{
                    "system": "http://snomed.info/sct",
                    "code": "394583002",
                    "display": "Endocrinology"
                }]
            }]
        }"""
        assertEquals("Endocrinology", displayNameExtractor.getDisplayName(json))
    }

    @Test
    fun practitionerRole_withCodeAndSpecialty() {
        val json =
            """{
            "resourceType": "PractitionerRole",
            "code": [{
                "coding": [{
                    "system": "http://terminology.hl7.org/CodeSystem/practitioner-role",
                    "code": "doctor",
                    "display": "Doctor"
                }]
            }],
            "specialty": [{
                "coding": [{
                    "system": "http://snomed.info/sct",
                    "code": "394583002",
                    "display": "Endocrinology"
                }]
            }]
        }"""
        assertEquals("Doctor - Endocrinology", displayNameExtractor.getDisplayName(json))
    }

}