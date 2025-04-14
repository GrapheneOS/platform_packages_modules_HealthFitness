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
 */
package android.health.connect.internal.datatypes.utils;

import static android.health.connect.internal.datatypes.utils.FhirResourceTypeStringToIntMapper.getFhirResourceTypeInt;


import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.FhirResource;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FhirResourceTypeStringToIntMapperTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void testFhirResourceTypeInt_immunizationType() {
        assertThat(getFhirResourceTypeInt("immunization"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION);
        assertThat(getFhirResourceTypeInt("Immunization"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION);
        assertThat(getFhirResourceTypeInt("IMMUNIZATION"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION);
    }

    @Test
    public void testFhirResourceTypeInt_allergyIntoleranceType() {
        assertThat(getFhirResourceTypeInt("allergyintolerance"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE);
        assertThat(getFhirResourceTypeInt("AllergyIntolerance"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE);
        assertThat(getFhirResourceTypeInt("ALLERGYINTOLERANCE"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE);
    }

    @Test
    public void testFhirResourceTypeInt_observationType() {
        assertThat(getFhirResourceTypeInt("observation"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_OBSERVATION);
        assertThat(getFhirResourceTypeInt("Observation"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_OBSERVATION);
        assertThat(getFhirResourceTypeInt("OBSERVATION"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_OBSERVATION);
    }

    @Test
    public void testFhirResourceTypeInt_conditionType() {
        assertThat(getFhirResourceTypeInt("condition"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_CONDITION);
        assertThat(getFhirResourceTypeInt("Condition"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_CONDITION);
        assertThat(getFhirResourceTypeInt("CONDITION"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_CONDITION);
    }

    @Test
    public void testFhirResourceTypeInt_procedureType() {
        assertThat(getFhirResourceTypeInt("procedure"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PROCEDURE);
        assertThat(getFhirResourceTypeInt("Procedure"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PROCEDURE);
        assertThat(getFhirResourceTypeInt("PROCEDURE"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PROCEDURE);
    }

    @Test
    public void testFhirResourceTypeInt_medicationType() {
        assertThat(getFhirResourceTypeInt("medication"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION);
        assertThat(getFhirResourceTypeInt("Medication"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION);
        assertThat(getFhirResourceTypeInt("MEDICATION"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION);
    }

    @Test
    public void testFhirResourceTypeInt_medicationStatementType() {
        assertThat(getFhirResourceTypeInt("medicationstatement"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT);
        assertThat(getFhirResourceTypeInt("MedicationStatement"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT);
        assertThat(getFhirResourceTypeInt("MEDICATIONSTATEMENT"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT);
    }

    @Test
    public void testFhirResourceTypeInt_medicationRequestType() {
        assertThat(getFhirResourceTypeInt("medicationrequest"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_REQUEST);
        assertThat(getFhirResourceTypeInt("MedicationRequest"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_REQUEST);
        assertThat(getFhirResourceTypeInt("MEDICATIONREQUEST"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_REQUEST);
    }

    @Test
    public void testFhirResourceTypeInt_patientType() {
        assertThat(getFhirResourceTypeInt("patient"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PATIENT);
        assertThat(getFhirResourceTypeInt("Patient"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PATIENT);
        assertThat(getFhirResourceTypeInt("PATIENT"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PATIENT);
    }

    @Test
    public void testFhirResourceTypeInt_practitionerType() {
        assertThat(getFhirResourceTypeInt("practitioner"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER);
        assertThat(getFhirResourceTypeInt("Practitioner"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER);
        assertThat(getFhirResourceTypeInt("PRACTITIONER"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER);
    }

    @Test
    public void testFhirResourceTypeInt_practitionerRoleType() {
        assertThat(getFhirResourceTypeInt("practitionerRole"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE);
        assertThat(getFhirResourceTypeInt("PractitionerRole"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE);
        assertThat(getFhirResourceTypeInt("PRACTITIONERROLE"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE);
    }

    @Test
    public void testFhirResourceTypeInt_encounterType() {
        assertThat(getFhirResourceTypeInt("encounter"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ENCOUNTER);
        assertThat(getFhirResourceTypeInt("Encounter"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ENCOUNTER);
        assertThat(getFhirResourceTypeInt("ENCOUNTER"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ENCOUNTER);
    }

    @Test
    public void testFhirResourceTypeInt_locationType() {
        assertThat(getFhirResourceTypeInt("location"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_LOCATION);
        assertThat(getFhirResourceTypeInt("Location"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_LOCATION);
        assertThat(getFhirResourceTypeInt("LOCATION"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_LOCATION);
    }

    @Test
    public void testFhirResourceTypeInt_organizationType() {
        assertThat(getFhirResourceTypeInt("organization"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ORGANIZATION);
        assertThat(getFhirResourceTypeInt("Organization"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ORGANIZATION);
        assertThat(getFhirResourceTypeInt("ORGANIZATION"))
                .isEqualTo(FhirResource.FHIR_RESOURCE_TYPE_ORGANIZATION);
    }

    @Test
    public void testFhirResourceTypeInt_unknownType_throws() {
        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> getFhirResourceTypeInt("researchstudy"));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Unsupported FHIR resource type: researchstudy");
    }
}
