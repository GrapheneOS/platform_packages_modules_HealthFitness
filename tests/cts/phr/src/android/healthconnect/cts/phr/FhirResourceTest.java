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

package android.healthconnect.cts.phr;

import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_CONDITION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_ENCOUNTER;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_LOCATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_MEDICATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_REQUEST;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_OBSERVATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_ORGANIZATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_PATIENT;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_PROCEDURE;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getFhirResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getFhirResourceBuilder;
import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.FhirResource;
import android.healthconnect.cts.phr.utils.ConditionBuilder;
import android.healthconnect.cts.phr.utils.EncountersBuilder;
import android.healthconnect.cts.phr.utils.MedicationsBuilder;
import android.healthconnect.cts.phr.utils.ObservationBuilder;
import android.healthconnect.cts.phr.utils.PatientBuilder;
import android.healthconnect.cts.phr.utils.PractitionerBuilder;
import android.healthconnect.cts.phr.utils.ProcedureBuilder;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FhirResourceTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testFhirResourceBuilder() {
        FhirResource resource =
                new FhirResource.Builder(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION,
                                FHIR_DATA_IMMUNIZATION)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_IMMUNIZATION);
        assertThat(resource.getId()).isEqualTo(FHIR_RESOURCE_ID_IMMUNIZATION);
        assertThat(resource.getData()).isEqualTo(FHIR_DATA_IMMUNIZATION);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsObservation() {
        String id = "myId123";
        String fhirData = new ObservationBuilder().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_OBSERVATION)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_OBSERVATION);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsCondition() {
        String id = "myId123";
        String fhirData = new ConditionBuilder().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_CONDITION)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_CONDITION);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsProcedure() {
        String id = "myId123";
        String fhirData = new ProcedureBuilder().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_PROCEDURE)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_PROCEDURE);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsMedication() {
        String id = "myId123";
        String fhirData = MedicationsBuilder.medication().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_MEDICATION)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_MEDICATION);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsMedicationStatement() {
        String id = "myId123";
        String fhirData = MedicationsBuilder.statementR4().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsMedicationRequest() {
        String id = "myId123";
        String fhirData = MedicationsBuilder.request().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_MEDICATION_REQUEST)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_MEDICATION_REQUEST);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsPatient() {
        String id = "myId123";
        String fhirData = new PatientBuilder().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_PATIENT)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_PATIENT);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsPractitioner() {
        String id = "myId123";
        String fhirData = new PractitionerBuilder().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_PRACTITIONER)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_PRACTITIONER);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsPractitionerRole() {
        String id = "myId123";
        String fhirData = PractitionerBuilder.role().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_PRACTITIONER)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_PRACTITIONER);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsEncounter() {
        String id = "myId123";
        String fhirData = EncountersBuilder.encounter().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_ENCOUNTER)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_ENCOUNTER);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsLocation() {
        String id = "myId123";
        String fhirData = EncountersBuilder.location().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_LOCATION)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_LOCATION);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFieldsOrganization() {
        String id = "myId123";
        String fhirData = EncountersBuilder.organization().setId(id).toJson();
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_ORGANIZATION)
                        .setId(id)
                        .setData(fhirData)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_ORGANIZATION);
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getData()).isEqualTo(fhirData);
    }

    @Test
    public void testFhirResourceBuilder_setAllFields_allergy() {
        FhirResource resource =
                getFhirResourceBuilder()
                        .setType(FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE)
                        .setId(FHIR_RESOURCE_ID_ALLERGY)
                        .setData(FHIR_DATA_ALLERGY)
                        .build();

        assertThat(resource.getType()).isEqualTo(FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE);
        assertThat(resource.getId()).isEqualTo(FHIR_RESOURCE_ID_ALLERGY);
        assertThat(resource.getData()).isEqualTo(FHIR_DATA_ALLERGY);
    }

    @Test
    public void testFhirResourceBuilder_fromExistingBuilder() {
        FhirResource.Builder original = getFhirResourceBuilder();
        FhirResource resource = new FhirResource.Builder(original).build();

        assertThat(resource).isEqualTo(original.build());
    }

    @Test
    public void testFhirResourceBuilder_fromExistingInstance() {
        FhirResource original = getFhirResource();
        FhirResource resource = new FhirResource.Builder(original).build();

        assertThat(resource).isEqualTo(original);
    }

    @Test
    public void testFhirResource_toString() {
        FhirResource resource =
                new FhirResource.Builder(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION,
                                FHIR_DATA_IMMUNIZATION)
                        .build();
        String typeString = "type=1";
        String idString = "id=" + FHIR_RESOURCE_ID_IMMUNIZATION;
        String dataString = "data=" + FHIR_DATA_IMMUNIZATION;

        assertThat(resource.toString()).contains(typeString);
        assertThat(resource.toString()).contains(idString);
        assertThat(resource.toString()).contains(dataString);
    }

    @Test
    public void testFhirResource_equals() {
        FhirResource resource1 = getFhirResource();
        FhirResource resource2 = getFhirResource();

        assertThat(resource1.equals(resource2)).isTrue();
        assertThat(resource1.hashCode()).isEqualTo(resource2.hashCode());
    }

    @Test
    public void testFhirResource_equals_comparesAllValues() {
        FhirResource resource = getFhirResource();
        FhirResource resourceDifferentType =
                new FhirResource.Builder(resource)
                        .setType(FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE)
                        .build();
        FhirResource resourceDifferentId =
                new FhirResource.Builder(resource).setId(FHIR_RESOURCE_ID_ALLERGY).build();
        FhirResource resourceDifferentData =
                new FhirResource.Builder(resource).setData(FHIR_DATA_ALLERGY).build();

        assertThat(resourceDifferentType.equals(resource)).isFalse();
        assertThat(resourceDifferentId.equals(resource)).isFalse();
        assertThat(resourceDifferentData.equals(resource)).isFalse();
        assertThat(resourceDifferentType.hashCode()).isNotEqualTo(resource.hashCode());
        assertThat(resourceDifferentId.hashCode()).isNotEqualTo(resource.hashCode());
        assertThat(resourceDifferentData.hashCode()).isNotEqualTo(resource.hashCode());
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        FhirResource original = getFhirResource();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        FhirResource restored = FhirResource.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testRestoreInvalidFhirResourceTypeFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        FhirResource original = getFhirResource();
        setFieldValueUsingReflection(original, "mType", -1);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> FhirResource.CREATOR.createFromParcel(parcel));
    }
}
