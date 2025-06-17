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

package com.android.server.healthconnect.phr.validations;

import static android.healthconnect.testing.shared.phr.ObservationBuilder.LOINC;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.ObservationCategory.LABORATORY;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.ObservationCategory.SOCIAL_HISTORY;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.ObservationCategory.VITAL_SIGNS;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.BEATS_PER_MINUTE;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.BREATHS_PER_MINUTE;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.CELSIUS;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.CENTIMETERS;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.KILOGRAMS;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.KILOGRAMS_PER_M2;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.MILLIMETERS_OF_MERCURY;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.PERCENT;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits.POUNDS;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.SNOMED_CT;
import static android.healthconnect.testing.shared.phr.ObservationBuilder.makeCodeableConcept;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION_FIELD_MISSING_INVALID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION_ID_EMPTY;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION_ID_NOT_EXISTS;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION_RESOURCE_TYPE_NOT_EXISTS;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION_UNSUPPORTED_RESOURCE_TYPE;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_RESOURCE_TYPE_UNSUPPORTED;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4B;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_UNSUPPORTED;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getUpsertMedicalResourceRequest;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getUpsertMedicalResourceRequestBuilder;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;

import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.internal.datatypes.utils.FhirResourceTypeStringToIntMapper;
import android.healthconnect.testing.shared.phr.ConditionBuilder;
import android.healthconnect.testing.shared.phr.DeviceBuilder;
import android.healthconnect.testing.shared.phr.EncountersBuilder;
import android.healthconnect.testing.shared.phr.ImmunizationBuilder;
import android.healthconnect.testing.shared.phr.MedicationsBuilder;
import android.healthconnect.testing.shared.phr.ObservationBuilder;
import android.healthconnect.testing.shared.phr.ObservationBuilder.QuantityUnits;
import android.healthconnect.testing.shared.phr.PatientBuilder;
import android.healthconnect.testing.shared.phr.PractitionerBuilder;
import android.healthconnect.testing.shared.phr.ProcedureBuilder;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.phr.UpsertMedicalResourceInternalRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

// We use AndroidJUnit4, because TestParameterInjector didn't work for
// robolectric tests in combination with the android.util.Xml class
// (b/424162678).
@RunWith(AndroidJUnit4.class)
public class MedicalResourceValidatorTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private FhirResourceValidator mFhirResourceValidator = null;

    @Test
    public void testValidateAndCreateInternalRequest_validAndR4_populatesInternalRequest() {
        UpsertMedicalResourceRequest upsertRequest =
                new UpsertMedicalResourceRequest.Builder(
                        DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION)
                        .build();
        UpsertMedicalResourceInternalRequest expected =
                new UpsertMedicalResourceInternalRequest()
                        .setDataSourceId(DATA_SOURCE_ID)
                        .setMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setFhirResourceType(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION)
                        .setFhirResourceId(FHIR_RESOURCE_ID_IMMUNIZATION)
                        .setFhirVersion(FHIR_VERSION_R4)
                        .setData(FHIR_DATA_IMMUNIZATION);

        MedicalResourceValidator validator =
                new MedicalResourceValidator(upsertRequest, getFhirResourceValidator());
        UpsertMedicalResourceInternalRequest validatedRequest =
                validator.validateAndCreateInternalRequest();

        assertThat(validatedRequest).isEqualTo(expected);
    }

    @Test
    public void testValidateAndCreateInternalRequest_validAndR4B_populatesInternalRequest() {
        UpsertMedicalResourceRequest upsertRequest =
                new UpsertMedicalResourceRequest.Builder(
                        DATA_SOURCE_ID, FHIR_VERSION_R4B, FHIR_DATA_IMMUNIZATION)
                        .build();
        UpsertMedicalResourceInternalRequest expected =
                new UpsertMedicalResourceInternalRequest()
                        .setDataSourceId(DATA_SOURCE_ID)
                        .setMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setFhirResourceType(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION)
                        .setFhirResourceId(FHIR_RESOURCE_ID_IMMUNIZATION)
                        .setFhirVersion(FHIR_VERSION_R4B)
                        .setData(FHIR_DATA_IMMUNIZATION);

        MedicalResourceValidator validator =
                new MedicalResourceValidator(upsertRequest, getFhirResourceValidator());
        UpsertMedicalResourceInternalRequest validatedRequest =
                validator.validateAndCreateInternalRequest();

        assertThat(validatedRequest).isEqualTo(expected);
    }

    @Test
    public void testConstructor_nullValidator_succeeds() {
        new MedicalResourceValidator(getUpsertMedicalResourceRequest(), null);
    }

    @Test
    public void testValidateAndCreateInternalRequest_nullValidator_succeeds() {
        UpsertMedicalResourceRequest upsertRequest =
                new UpsertMedicalResourceRequest.Builder(
                        DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION)
                        .build();
        UpsertMedicalResourceInternalRequest expected =
                new UpsertMedicalResourceInternalRequest()
                        .setDataSourceId(DATA_SOURCE_ID)
                        .setMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setFhirResourceType(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION)
                        .setFhirResourceId(FHIR_RESOURCE_ID_IMMUNIZATION)
                        .setFhirVersion(FHIR_VERSION_R4)
                        .setData(FHIR_DATA_IMMUNIZATION);

        MedicalResourceValidator validator = new MedicalResourceValidator(upsertRequest, null);
        UpsertMedicalResourceInternalRequest validatedRequest =
                validator.validateAndCreateInternalRequest();

        assertThat(validatedRequest).isEqualTo(expected);
    }

    @Test
    public void testValidateAndCreateInternalRequest_nullValidator_unknownFieldSucceeds() {
        String immunizationWithUnknownField =
                new ImmunizationBuilder()
                        .setId(FHIR_RESOURCE_ID_IMMUNIZATION)
                        .set("unknown", "test")
                        .toJson();
        UpsertMedicalResourceRequest upsertRequest =
                new UpsertMedicalResourceRequest.Builder(
                        DATA_SOURCE_ID, FHIR_VERSION_R4, immunizationWithUnknownField)
                        .build();
        UpsertMedicalResourceInternalRequest expected =
                new UpsertMedicalResourceInternalRequest()
                        .setDataSourceId(DATA_SOURCE_ID)
                        .setMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setFhirResourceType(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION)
                        .setFhirResourceId(FHIR_RESOURCE_ID_IMMUNIZATION)
                        .setFhirVersion(FHIR_VERSION_R4)
                        .setData(immunizationWithUnknownField);

        MedicalResourceValidator validator = new MedicalResourceValidator(upsertRequest, null);
        UpsertMedicalResourceInternalRequest validatedRequest =
                validator.validateAndCreateInternalRequest();

        assertThat(validatedRequest).isEqualTo(expected);
    }

    @Test
    public void testValidateAndCreateInternalRequest_nonNullValidator_unknownFieldThrows() {
        String immunizationWithUnknownField =
                new ImmunizationBuilder().set("unknown", "test").toJson();

        UpsertMedicalResourceRequest request =
                new UpsertMedicalResourceRequest.Builder(
                        DATA_SOURCE_ID, FHIR_VERSION_R4, immunizationWithUnknownField)
                        .build();
        MedicalResourceValidator validator =
                new MedicalResourceValidator(request, getFhirResourceValidator());

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> validator.validateAndCreateInternalRequest());
        assertThat(thrown).hasMessageThat().contains("Found unexpected field ");
    }

    @Test
    public void testValidateAndCreateInternalRequest_fhirResourceWithoutType_throws() {
        MedicalResourceValidator validator =
                makeValidator(FHIR_DATA_IMMUNIZATION_RESOURCE_TYPE_NOT_EXISTS);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Missing resourceType field for resource with id "
                                + FHIR_RESOURCE_ID_IMMUNIZATION);
    }

    @Test
    public void testValidateAndCreateInternalRequest_fhirResourceWithoutId_throws() {
        MedicalResourceValidator validator = makeValidator(FHIR_DATA_IMMUNIZATION_ID_NOT_EXISTS);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown).hasMessageThat().contains("Resource is missing id field");
    }

    @Test
    public void testValidateAndCreateInternalRequest_nullValidatorMissingId_throws() {
        UpsertMedicalResourceRequest request =
                new UpsertMedicalResourceRequest.Builder(
                        DATA_SOURCE_ID,
                        FHIR_VERSION_R4,
                        FHIR_DATA_IMMUNIZATION_ID_NOT_EXISTS)
                        .build();
        MedicalResourceValidator validator = new MedicalResourceValidator(request, null);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown).hasMessageThat().contains("Resource is missing id field");
    }

    @Test
    public void testValidateAndCreateInternalRequest_invalidJson_throws() {
        MedicalResourceValidator validator =
                makeValidator(FHIR_DATA_IMMUNIZATION_FIELD_MISSING_INVALID);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown).hasMessageThat().contains("invalid json");
    }

    @Test
    public void testValidateAndCreateInternalRequest_emptyId_throws() {
        MedicalResourceValidator validator = makeValidator(FHIR_DATA_IMMUNIZATION_ID_EMPTY);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown).hasMessageThat().contains("id cannot be empty");
    }

    @Test
    public void testValidateAndCreateInternalRequest_nullId_throws() {
        String immunizationJson = new ImmunizationBuilder().set("id", JSONObject.NULL).toJson();
        MedicalResourceValidator validator = makeValidator(immunizationJson);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown).hasMessageThat().contains("id should be a string");
    }

    @Test
    public void testValidateAndCreateInternalRequest_nonStringId_throws() {
        String immunizationJson = new ImmunizationBuilder().set("id", 123).toJson();
        MedicalResourceValidator validator = makeValidator(immunizationJson);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown).hasMessageThat().contains("id should be a string");
    }

    @Test
    public void testValidateAndCreateInternalRequest_nullString_succeeds() {
        String immunizationJson = new ImmunizationBuilder().set("id", "null").toJson();
        MedicalResourceValidator validator = makeValidator(immunizationJson);

        UpsertMedicalResourceInternalRequest internalRequest =
                validator.validateAndCreateInternalRequest();

        assertThat(internalRequest.getFhirResourceId()).isEqualTo("null");
    }

    @Test
    public void testValidateAndCreateInternalRequest_unsupportedFhirVersion_throws() {
        UpsertMedicalResourceRequest upsertRequest =
                getUpsertMedicalResourceRequestBuilder()
                        .setFhirVersion(FHIR_VERSION_UNSUPPORTED)
                        .build();
        MedicalResourceValidator validator =
                new MedicalResourceValidator(upsertRequest, getFhirResourceValidator());

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Unsupported FHIR version "
                                + FHIR_VERSION_UNSUPPORTED
                                + " for resource with id "
                                + FHIR_RESOURCE_ID_IMMUNIZATION);
    }

    @Test
    public void testValidateAndCreateInternalRequest_unsupportedResourceType_throws() {
        MedicalResourceValidator validator =
                makeValidator(FHIR_DATA_IMMUNIZATION_UNSUPPORTED_RESOURCE_TYPE);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Unsupported FHIR resource type "
                                + FHIR_RESOURCE_TYPE_UNSUPPORTED
                                + " for resource with id "
                                + FHIR_RESOURCE_ID_IMMUNIZATION);
    }

    @Test
    public void testValidateAndCreateInternalRequest_nullResourceType_throws() {
        String immunizationJson =
                new ImmunizationBuilder()
                        .setId(FHIR_RESOURCE_ID_IMMUNIZATION)
                        .set("resourceType", JSONObject.NULL)
                        .toJson();
        MedicalResourceValidator validator = makeValidator(immunizationJson);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Unsupported FHIR resource type null for resource with id "
                                + FHIR_RESOURCE_ID_IMMUNIZATION);
    }

    @Test
    public void testValidateAndCreateInternalRequest_containedResourceInResource_throws() {
        String resourceId = "id1";
        String medicationStatementWithContainedResource =
                new MedicationsBuilder.MedicationStatementR4Builder()
                        .setId(resourceId)
                        .setContainedMedication(new MedicationsBuilder.MedicationBuilder())
                        .toJson();
        MedicalResourceValidator validator =
                makeValidator(medicationStatementWithContainedResource);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Contained resources are not supported. Found contained resource for"
                                + " resource with id "
                                + resourceId);
    }

    @Test
    public void testValidateAndCreateInternalRequest_containedFieldNotArray_throws()
            throws JSONException {
        String resourceId = "id1";
        String medicationStatementWithInvalidContained =
                new MedicationsBuilder.MedicationStatementR4Builder()
                        .setId(resourceId)
                        .set("contained", new JSONObject("{}"))
                        .toJson();
        MedicalResourceValidator validator = makeValidator(medicationStatementWithInvalidContained);

        var thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        validator::validateAndCreateInternalRequest);
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Contained resources are not supported. Found contained field for resource"
                                + " with id "
                                + resourceId);
    }

    @Test
    public void testCalculateMedicalResourceType_allergy() {
        MedicalResourceValidator validator = makeValidator(FHIR_DATA_ALLERGY);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
    }

    @Test
    public void testCalculateMedicalResourceType_condition() {
        String fhirData = new ConditionBuilder().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS);
    }

    @Test
    public void testCalculateMedicalResourceType_procedure() {
        String fhirData = new ProcedureBuilder().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES);
    }

    @Test
    public void testCalculateMedicalResourceType_medication() {
        String fhirData = MedicationsBuilder.medication().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS);
    }

    @Test
    public void testCalculateMedicalResourceType_medicationStatement() {
        String fhirData = MedicationsBuilder.statementR4().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS);
    }

    @Test
    public void testCalculateMedicalResourceType_medicationRequest() {
        String fhirData = MedicationsBuilder.request().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS);
    }

    @Test
    public void testCalculateMedicalResourceType_patient() {
        String fhirData = new PatientBuilder().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS);
    }

    @Test
    public void testCalculateMedicalResourceType_practitioner() {
        String fhirData = new PractitionerBuilder().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS);
    }

    @Test
    public void testCalculateMedicalResourceType_practitionerRole() {
        String fhirData = PractitionerBuilder.role().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS);
    }

    @Test
    public void testCalculateMedicalResourceType_encounter() {
        String fhirData = EncountersBuilder.encounter().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS);
    }

    @Test
    public void testCalculateMedicalResourceType_location() {
        String fhirData = EncountersBuilder.location().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS);
    }

    @Test
    public void testCalculateMedicalResourceType_organization() {
        String fhirData = EncountersBuilder.organization().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_RESOURCE)
    public void testCalculateMedicalResourceType_device() {
        FhirResourceTypeStringToIntMapper.reset();
        String fhirData = new DeviceBuilder().toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_DEVICES);
        FhirResourceTypeStringToIntMapper.reset();
    }

    // IPS artifacts: https://build.fhir.org/ig/HL7/fhir-ips/artifacts.html
    // pregnancy outcome,  status and expected delivery date.
    enum PregnancyStatusTestValue {
        PREGNANT("82810-3", "77386006"), // Pregnancy status, pregnant
        NOT_PREGNANT("82810-3", "60001007"); // Pregnancy status, not pregnant
        private final String mCode;
        private final String mValue;

        PregnancyStatusTestValue(String code, String value) {
            mCode = code;
            mValue = value;
        }
    }

    enum PregnancyOutcomeTestValue {
        LIVE_BIRTH("11636-8", 1),
        NO_LIVE_BIRTH("11636-8", 0),
        PRETERM("11637-6", 1),
        NO_PRETERM("11637-6", 0),
        STILL_LIVING("11638-4", 1),
        NO_STILL_LIVING("11638-4", 0),
        TERM("11639-2", 1),
        NO_TERM("11639-2", 0),
        TOTAL("11640-0", 1),
        NO_TOTAL("11640-0", 0),
        ABORTIONS("11612-9", 1),
        NO_ABORTIONS("11612-9", 0),
        INDUCED_ABORTION("11613-7", 1),
        NO_INDUCED_ABORTIONS("11613-7", 0),
        SPONTANEOUS_ABORTION("11614-5", 1),
        NO_SPONTANEOUS_ABORTIONS("11614-5", 0),
        ECTOPIC_PREGNANCY("33065-4", 1),
        NO_ECTOPIC_PREGNANCIES("33065-4", 0),
        ;
        private final String mCode;
        private final int mCount;

        PregnancyOutcomeTestValue(String code, int count) {
            mCode = code;
            mCount = count;
        }
    }

    enum SmokingTestValue {
        FORMER_SMOKER_LOINC("72166-2", makeCodeableConcept(LOINC, "LA15920-4", "Former smoker")),
        // https://build.fhir.org/ig/HL7/fhir-ips/ValueSet-current-smoking-status-uv-ips.html
        DAILY(
                "72166-2",
                makeCodeableConcept(SNOMED_CT, "449868002", "Smokes tobacco daily (finding)")),
        OCCASIONAL(
                "72166-2",
                makeCodeableConcept(
                        SNOMED_CT, "428041000124106", "Occasional tobacco smoker (finding)")),
        EX_SMOKER("72166-2", makeCodeableConcept(SNOMED_CT, "8517006", "Ex-smoker (finding)")),
        NEVER(
                "72166-2",
                makeCodeableConcept(SNOMED_CT, "266919005", "Never smoked tobacco (finding)")),
        SMOKER("72166-2", makeCodeableConcept(SNOMED_CT, "77176002", "Smoker (finding)")),
        UNKNOWN(
                "72166-2",
                makeCodeableConcept(
                        SNOMED_CT, "266927001", "Tobacco smoking consumption unknown (finding)")),
        HEAVY(
                "72166-2",
                makeCodeableConcept(SNOMED_CT, "230063004", "Heavy cigarette Smoker (finding)")),
        LIGHT(
                "72166-2",
                makeCodeableConcept(SNOMED_CT, "230060001", "Light cigarette Smoker (finding)")),
        ;
        private final String mCode;
        private final JSONObject mValueCodeableConcept;

        SmokingTestValue(String code, JSONObject value) {
            mCode = code;
            mValueCodeableConcept = value;
        }
    }

    enum VitalSignsTestValue {
        // https://hl7.org/fhir/R5/observation-vitalsigns.html
        SAT_O2("2708-6", 95, PERCENT),
        RESPIRATORY_RATE("9279-1", 26, BREATHS_PER_MINUTE),
        HEART_RATE("8867-4", 100, BEATS_PER_MINUTE),
        BODY_TEMPERATURE("8867-4", 36.4, CELSIUS),
        BODY_HEIGHT("8302-2", 152, CENTIMETERS),
        HEAD_CIRCUMFERENCE("9843-4", 51.2, CENTIMETERS),
        BODY_WEIGHT_LBS("29463-7", 185, POUNDS),
        BODY_WEIGHT_KG("29463-7", 77, KILOGRAMS),
        BODY_MASS_INDEX("39156-5", 16.2, KILOGRAMS_PER_M2),
        SYSTOLIC_BLOOD_PRESSURE("8480-6", 26, MILLIMETERS_OF_MERCURY),
        DIASTOLIC_BLOOD_PRESSURE("8462-4", 26, MILLIMETERS_OF_MERCURY),
        ;
        private final String mCode;
        private final Number mValue;
        private final QuantityUnits mUnits;

        VitalSignsTestValue(String code, Number value, QuantityUnits units) {
            mCode = code;
            mValue = value;
            mUnits = units;
        }
    }

    @Test
    public void testCalculateMedicalResourceType_pregnancyStatus_pregnancy() {
        for (PregnancyStatusTestValue testValue : PregnancyStatusTestValue.values()) {
            String fhirData =
                    new ObservationBuilder()
                            .setCode(LOINC, testValue.mCode)
                            .setValueCodeableConcept(SNOMED_CT, testValue.mValue)
                            .toJson();
            MedicalResourceValidator validator = makeValidator(fhirData);

            int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

            assertWithMessage("Failed for PregnancyStatusTestValue: " + testValue.name())
                    .that(type)
                    .isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY);
        }
    }

    @Test
    public void testCalculateMedicalResourceType_pregnancyOutcome_pregnancy() {
        for (PregnancyOutcomeTestValue testValue : PregnancyOutcomeTestValue.values()) {
            String fhirData =
                    new ObservationBuilder()
                            .setCode(LOINC, testValue.mCode)
                            .setValueQuantity(testValue.mCount, QuantityUnits.COUNT)
                            .toJson();
            MedicalResourceValidator validator = makeValidator(fhirData);

            int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

            assertWithMessage("Failed for PregnancyOutcomeTestValue: " + testValue.name())
                    .that(type)
                    .isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY);
        }
    }

    @Test
    public void testCalculateMedicalResourceType_expectedDeliveryDate_pregnancy() {
        String[] codes = {"11778-8", "11779-6", "11780-4"};
        for (String code : codes) {
            // https://build.fhir.org/ig/HL7/fhir-ips/ValueSet-edd-method-uv-ips.html
            String fhirData =
                    new ObservationBuilder()
                            .setCode(LOINC, code)
                            .removeAllEffectiveMultiTypeFields()
                            .set("effectiveDateTime", "2021-04-20")
                            .removeAllValueMultiTypeFields()
                            .set("valueDateTime", "2021-08-07")
                            .toJson();
            MedicalResourceValidator validator = makeValidator(fhirData);

            int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

            assertWithMessage("Failed for Expected Delivery Date Code: " + code)
                    .that(type)
                    .isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY);
        }
    }

    @Test
    public void testCalculateMedicalResourceType_categorySocialHistory_socialHistory() {
        String fhirData = new ObservationBuilder().setCategory(SOCIAL_HISTORY).toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY);
    }

    @Test
    public void testCalculateMedicalResourceType_smoking_socialHistory() {
      // https://build.fhir.org/ig/HL7/fhir-ips/StructureDefinition-Observation-tobaccouse-uv-ips.html
        for (SmokingTestValue value : SmokingTestValue.values()) {
            String fhirData =
                    new ObservationBuilder()
                            .setCode(LOINC, value.mCode)
                            .removeAllValueMultiTypeFields()
                            .set("valueCodeableConcept", value.mValueCodeableConcept)
                            .toJson();
            MedicalResourceValidator validator = makeValidator(fhirData);

            int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

            assertWithMessage("Failed for SmokingTestValue: " + value.name())
                    .that(type)
                    .isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY);
        }
    }

    @Test
    public void testCalculateMedicalResourceType_ipsAlcohol_socialHistory() throws JSONException {
        // https://build.fhir.org/ig/HL7/fhir-ips/StructureDefinition-Observation-alcoholuse-uv-ips.html
        String fhirData =
                new ObservationBuilder()
                        .setCode(LOINC, "74013-4")
                        .setValueQuantity(2, QuantityUnits.GLASSES_OF_WINE_PER_DAY)
                        .toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY);
    }

    @Test
    public void testCalculateMedicalResourceType_categoryVitals_vitalSigns() {
        String fhirData = new ObservationBuilder().setCategory(VITAL_SIGNS).toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS);
    }

    @Test
    public void testCalculateMedicalResourceType_vitalSigns_vitalSigns() {
        for (VitalSignsTestValue value : VitalSignsTestValue.values()) {
            // From https://hl7.org/fhir/R5/observation-vitalsigns.html
            String fhirData =
                    new ObservationBuilder()
                            .setCode(LOINC, value.mCode)
                            .setValueQuantity(value.mValue, value.mUnits)
                            .toJson();
            MedicalResourceValidator validator = makeValidator(fhirData);

            int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

            assertWithMessage("Failed for VitalSignsTestValue: " + value.name())
                    .that(type)
                    .isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS);
        }
    }

    @Test
    public void testCalculateMedicalResourceType_vitalSignsPanel_vitalSigns() {
        // From https://hl7.org/fhir/R5/observation-example-vitals-panel.json.html
        String fhirData =
                "{  \"resourceType\" : \"Observation\",  \"id\" : \"vitals-panel\",  \"meta\" : {  "
                    + "  \"profile\" : [\"http://hl7.org/fhir/StructureDefinition/vitalsigns\"]  },"
                    + "  \"status\" : \"final\","
                        // category deliberately left out to check categorization by code
                        + "  \"code\" : {    \"coding\" : [{      \"system\" :"
                        + " \"http://loinc.org\",      \"code\" : \"85353-1\",      \"display\" :"
                        + " \"Vital signs, weight, height, head circumference, oxygen saturation"
                        + " and BMI panel\"    }],    \"text\" : \"Vital signs Panel\"  }, "
                        + " \"subject\" : {    \"reference\" : \"Patient/example\"  }, "
                        + " \"effectiveDateTime\" : \"1999-07-02\",  \"hasMember\" : [{   "
                        + " \"reference\" : \"Observation/respiratory-rate\",    \"display\" :"
                        + " \"Respiratory Rate\"  },  {    \"reference\" :"
                        + " \"Observation/heart-rate\",    \"display\" : \"Heart Rate\"  },  {   "
                        + " \"reference\" : \"Observation/blood-pressure\",    \"display\" :"
                        + " \"Blood Pressure\"  },  {    \"reference\" :"
                        + " \"Observation/body-temperature\",    \"display\" : \"Body Temperature\""
                        + "  }]}";
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS);
    }

    @Test
    public void testCalculateMedicalResourceType_bloodPressurePanel_vitalSigns() {
        // From https://hl7.org/fhir/R5/observation-example-bloodpressure.json.html
        String fhirData =
                "{  \"resourceType\" : \"Observation\",  \"id\" : \"blood-pressure\",  \"meta\" : {"
                    + "    \"profile\" : [\"http://hl7.org/fhir/StructureDefinition/vitalsigns\"] "
                    + " },  \"identifier\" : [{    \"system\" : \"urn:ietf:rfc:3986\",    \"value\""
                    + " : \"urn:uuid:187e0c12-8dd2-67e2-99b2-bf273c878281\"  }],  \"basedOn\" : [{ "
                    + "   \"identifier\" : {      \"system\" : \"https://acme.org/identifiers\",   "
                    + "   \"value\" : \"1234\"    }  }],  \"status\" : \"final\","
                        // category deliberately left out to test categorization by code
                        + "  \"code\" : {    \"coding\" : [{      \"system\" :"
                        + " \"http://loinc.org\",      \"code\" : \"85354-9\",      \"display\" :"
                        + " \"Blood pressure panel with all children optional\"    }],    \"text\""
                        + " : \"Blood pressure systolic & diastolic\"  },  \"subject\" : {   "
                        + " \"reference\" : \"Patient/example\"  },  \"effectiveDateTime\" :"
                        + " \"2012-09-17\",  \"performer\" : [{    \"reference\" :"
                        + " \"Practitioner/example\"  }],  \"interpretation\" : [{    \"coding\" :"
                        + " [{      \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation\","
                        + "      \"code\" : \"L\",      \"display\" : \"low\"    }],    \"text\" :"
                        + " \"Below low normal\"  }],  \"bodySite\" : {    \"coding\" : [{     "
                        + " \"system\" : \"http://snomed.info/sct\",      \"code\" : \"368209003\","
                        + "      \"display\" : \"Right arm\"    }]  },  \"component\" : [{   "
                        + " \"code\" : {      \"coding\" : [{        \"system\" :"
                        + " \"http://loinc.org\",        \"code\" : \"8480-6\",        \"display\""
                        + " : \"Systolic blood pressure\"      },      {        \"system\" :"
                        + " \"http://snomed.info/sct\",        \"code\" : \"271649006\",       "
                        + " \"display\" : \"Systolic blood pressure\"      },      {       "
                        + " \"system\" : \"http://acme.org/devices/clinical-codes\",       "
                        + " \"code\" : \"bp-s\",        \"display\" : \"Systolic Blood pressure\"  "
                        + "    }]    },    \"valueQuantity\" : {      \"value\" : 107,     "
                        + " \"unit\" : \"mmHg\",      \"system\" : \"http://unitsofmeasure.org\",  "
                        + "    \"code\" : \"mm[Hg]\"    },    \"interpretation\" : [{     "
                        + " \"coding\" : [{        \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation\","
                        + "        \"code\" : \"N\",        \"display\" : \"normal\"      }],     "
                        + " \"text\" : \"Normal\"    }]  },  {    \"code\" : {      \"coding\" : [{"
                        + "        \"system\" : \"http://loinc.org\",        \"code\" : \"8462-4\","
                        + "        \"display\" : \"Diastolic blood pressure\"      }]    },   "
                        + " \"valueQuantity\" : {      \"value\" : 60,      \"unit\" : \"mmHg\",   "
                        + "   \"system\" : \"http://unitsofmeasure.org\",      \"code\" :"
                        + " \"mm[Hg]\"    },    \"interpretation\" : [{      \"coding\" : [{       "
                        + " \"system\" :"
                        + " \"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation\","
                        + "        \"code\" : \"L\",        \"display\" : \"low\"      }],     "
                        + " \"text\" : \"Below low normal\"    }]  }]}";
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS);
    }

    @Test
    public void testCalculateMedicalResourceType_categoryLaboratory_laboratoryResults() {
        String fhirData = new ObservationBuilder().setCategory(LABORATORY).toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS);
    }

    @Test
    public void testPregnancyHigherPriorityThanSocialHistory() {
        PregnancyStatusTestValue status = PregnancyStatusTestValue.PREGNANT;
        String fhirData =
                new ObservationBuilder()
                        .setCode(LOINC, status.mCode)
                        .setValueCodeableConcept(SNOMED_CT, status.mValue)
                        .setCategory(SOCIAL_HISTORY)
                        .toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY);
    }

    @Test
    public void testSocialHistoryHigherPriorityThanVitalSigns() throws JSONException {
        JSONArray categories =
                new JSONArray(
                        List.of(
                                VITAL_SIGNS.toFhirCodeableConcept(),
                                SOCIAL_HISTORY.toFhirCodeableConcept()));
        String fhirData = new ObservationBuilder().set("category", categories).toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY);
    }

    @Test
    public void testVitalSignsHigherPriorityThanLabResults() throws JSONException {
        JSONArray categories =
                new JSONArray(
                        List.of(
                                LABORATORY.toFhirCodeableConcept(),
                                VITAL_SIGNS.toFhirCodeableConcept()));
        String fhirData = new ObservationBuilder().set("category", categories).toJson();
        MedicalResourceValidator validator = makeValidator(fhirData);

        int type = validator.validateAndCreateInternalRequest().getMedicalResourceType();

        assertThat(type).isEqualTo(MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS);
    }

    private MedicalResourceValidator makeValidator(String fhirData) {
        UpsertMedicalResourceRequest request =
                new UpsertMedicalResourceRequest.Builder(DATA_SOURCE_ID, FHIR_VERSION_R4, fhirData)
                        .build();
        return new MedicalResourceValidator(request, getFhirResourceValidator());
    }

    private FhirResourceValidator getFhirResourceValidator() {
        if (mFhirResourceValidator == null) {
            mFhirResourceValidator = new FhirResourceValidator();
        }
        return mFhirResourceValidator;
    }
}