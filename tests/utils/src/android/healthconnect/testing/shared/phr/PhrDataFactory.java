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

package android.healthconnect.testing.shared.phr;

import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.FhirVersion.parseFhirVersion;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;

import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.MedicalResourceId;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.net.Uri;

import com.google.common.truth.Correspondence;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class PhrDataFactory {
    private static final int FHIR_BASE_URI_CHARACTER_LIMIT = 2000;
    private static final int MEDICAL_DATA_SOURCE_DISPLAY_NAME_CHARACTER_LIMIT = 90;

    public static final int MAX_ALLOWED_MEDICAL_DATA_SOURCES = 20;

    /**
     * String version code for FHIR version <a href="https://hl7.org/fhir/r4/versions.html">R4</a>.
     */
    public static final String R4_VERSION_STRING = "4.0.1";

    /**
     * {@link FhirVersion} for FHIR version <a href="https://hl7.org/fhir/r4/versions.html">R4</a>.
     */
    public static final FhirVersion FHIR_VERSION_R4 = parseFhirVersion(R4_VERSION_STRING);

    /** String version code for FHIR version <a href="https://www.hl7.org/fhir/R4B/">R4B</a>. */
    public static final String R4B_VERSION_STRING = "4.3.0";

    /** {@link FhirVersion} for FHIR version <a href="https://www.hl7.org/fhir/R4B/">R4B</a>. */
    public static final FhirVersion FHIR_VERSION_R4B = parseFhirVersion(R4B_VERSION_STRING);

    public static final String UNSUPPORTED_VERSION_STRING = "4.5.5";
    public static final FhirVersion FHIR_VERSION_UNSUPPORTED =
            parseFhirVersion(UNSUPPORTED_VERSION_STRING);

    public static final String RESOURCE_ID_FIELD_NAME = "id";

    public static final UUID DATA_SOURCE_UUID = UUID.randomUUID();
    public static final String DATA_SOURCE_ID = DATA_SOURCE_UUID.toString();
    public static final String DATA_SOURCE_PACKAGE_NAME = "com.example.app";
    public static final Uri DATA_SOURCE_FHIR_BASE_URI =
            Uri.parse("https://fhir.com/oauth/api/FHIR/R4/");
    public static final Uri DATA_SOURCE_FHIR_BASE_URI_MAX_CHARS =
            Uri.parse("d".repeat(FHIR_BASE_URI_CHARACTER_LIMIT));
    public static final Uri DATA_SOURCE_FHIR_BASE_URI_EXCEEDED_CHARS =
            Uri.parse("d".repeat(FHIR_BASE_URI_CHARACTER_LIMIT + 1));
    public static final String DATA_SOURCE_DISPLAY_NAME = "Hospital X";
    public static final String DATA_SOURCE_DISPLAY_NAME_MAX_CHARS =
            "d".repeat(MEDICAL_DATA_SOURCE_DISPLAY_NAME_CHARACTER_LIMIT);
    public static final String DATA_SOURCE_DISPLAY_NAME_EXCEEDED_CHARS =
            "d".repeat(MEDICAL_DATA_SOURCE_DISPLAY_NAME_CHARACTER_LIMIT + 1);
    public static final Instant DATA_SOURCE_LAST_DATA_UPDATE_TIME =
            Instant.parse("2024-09-10T00:02:00Z");
    public static final FhirVersion DATA_SOURCE_FHIR_VERSION = FHIR_VERSION_R4;
    public static final UUID DIFFERENT_DATA_SOURCE_UUID = UUID.randomUUID();
    public static final String DIFFERENT_DATA_SOURCE_ID = DIFFERENT_DATA_SOURCE_UUID.toString();
    public static final String DIFFERENT_DATA_SOURCE_PACKAGE_NAME = "com.other.app";
    public static final Uri DIFFERENT_DATA_SOURCE_BASE_URI =
            Uri.parse("https://fhir.com/oauth/api/FHIR/R5/");
    public static final String DIFFERENT_DATA_SOURCE_DISPLAY_NAME = "Doctor Y";
    public static final Instant DIFFERENT_DATA_SOURCE_LAST_DATA_UPDATE_TIME =
            Instant.parse("2023-01-01T00:02:00Z");
    public static final FhirVersion DIFFERENT_DATA_SOURCE_FHIR_VERSION = FHIR_VERSION_R4B;

    public static final String FHIR_RESOURCE_ID_IMMUNIZATION = "Immunization1";
    public static final String FHIR_DATA_IMMUNIZATION =
            new ImmunizationBuilder().setId(FHIR_RESOURCE_ID_IMMUNIZATION).toJson();
    public static final String DIFFERENT_FHIR_RESOURCE_ID_IMMUNIZATION = "Immunization2";
    public static final String DIFFERENT_FHIR_DATA_IMMUNIZATION =
            new ImmunizationBuilder().setId(DIFFERENT_FHIR_RESOURCE_ID_IMMUNIZATION).toJson();

    public static final String FHIR_DATA_IMMUNIZATION_ID_NOT_EXISTS =
            new ImmunizationBuilder().removeField(RESOURCE_ID_FIELD_NAME).toJson();
    public static final String FHIR_DATA_IMMUNIZATION_ID_EMPTY =
            new ImmunizationBuilder().setId("").toJson();
    public static final String FHIR_DATA_IMMUNIZATION_RESOURCE_TYPE_NOT_EXISTS =
            new ImmunizationBuilder()
                    .setId(FHIR_RESOURCE_ID_IMMUNIZATION)
                    .removeField("resourceType")
                    .toJson();
    public static final String FHIR_DATA_IMMUNIZATION_FIELD_MISSING_INVALID = "{\"id\" : }";
    public static final String FHIR_RESOURCE_TYPE_UNSUPPORTED = "StructureDefinition";
    public static final String FHIR_DATA_IMMUNIZATION_UNSUPPORTED_RESOURCE_TYPE =
            "{\"resourceType\" : \"StructureDefinition\", \"id\" : \"Immunization1\"}";

    public static final String FHIR_RESOURCE_ID_ALLERGY = "Allergy1";
    public static final String FHIR_DATA_ALLERGY =
            new AllergyBuilder().setId(FHIR_RESOURCE_ID_ALLERGY).toJson();
    public static final String DIFFERENT_FHIR_RESOURCE_ID_ALLERGY = "Allergy2";
    public static final String DIFFERENT_FHIR_DATA_ALLERGY =
            new AllergyBuilder().setId(DIFFERENT_FHIR_RESOURCE_ID_ALLERGY).toJson();
    public static final String FHIR_DATA_CONDITION = new ConditionBuilder().toJson();
    public static final String FHIR_DATA_MEDICATION =
            new MedicationsBuilder.MedicationBuilder().toJson();
    public static final String FHIR_DATA_Patient = new PatientBuilder().toJson();
    public static final String FHIR_DATA_PRACTITIONER = new PractitionerBuilder().toJson();
    public static final String FHIR_DATA_ENCOUNTER = EncountersBuilder.encounter().toJson();
    public static final String FHIR_DATA_PROCEDURE = new ProcedureBuilder().toJson();
    public static final String FHIR_DATA_OBSERVATION_PREGNANCY =
            new ObservationBuilder()
                    .setId("1")
                    .setPregnancyStatus(ObservationBuilder.PregnancyStatus.NOT_PREGNANT)
                    .toJson();
    public static final String FHIR_DATA_OBSERVATION_SOCIAL_HISTORY =
            new ObservationBuilder()
                    .setId("2")
                    .setCategory(ObservationBuilder.ObservationCategory.SOCIAL_HISTORY)
                    .toJson();
    public static final String FHIR_DATA_OBSERVATION_VITAL_SIGNS =
            new ObservationBuilder()
                    .setId("3")
                    .setCategory(ObservationBuilder.ObservationCategory.VITAL_SIGNS)
                    .toJson();
    public static final String FHIR_DATA_OBSERVATION_LABS =
            new ObservationBuilder()
                    .setId("4")
                    .setCategory(ObservationBuilder.ObservationCategory.LABORATORY)
                    .toJson();

    public static final String PAGE_TOKEN = "111";

    public static final Correspondence<MedicalDataSource, MedicalDataSource>
            MEDICAL_DATA_SOURCE_EQUIVALENCE =
                    Correspondence.from(
                            PhrDataFactory::isMedicalDataSourceEqual, "isMedicalDataSourceEqual");

    /** Creates and returns a {@link MedicalDataSource.Builder} with default arguments. */
    public static MedicalDataSource.Builder getMedicalDataSourceBuilderRequiredFieldsOnly() {
        return new MedicalDataSource.Builder(
                DATA_SOURCE_ID,
                DATA_SOURCE_PACKAGE_NAME,
                DATA_SOURCE_FHIR_BASE_URI,
                DATA_SOURCE_DISPLAY_NAME,
                DATA_SOURCE_FHIR_VERSION);
    }

    /** Creates and returns a {@link MedicalDataSource.Builder} with default arguments. */
    public static MedicalDataSource.Builder getMedicalDataSourceBuilderWithOptionalFields() {
        return getMedicalDataSourceBuilderRequiredFieldsOnly()
                .setLastDataUpdateTime(DATA_SOURCE_LAST_DATA_UPDATE_TIME);
    }

    /**
     * Creates and returns a {@link MedicalDataSource} with default arguments for required fields.
     */
    public static MedicalDataSource getMedicalDataSourceRequiredFieldsOnly() {
        return getMedicalDataSourceBuilderRequiredFieldsOnly().build();
    }

    /** Creates and returns a {@link MedicalDataSource} with default arguments. */
    public static MedicalDataSource getMedicalDataSourceWithOptionalFields() {
        return getMedicalDataSourceBuilderWithOptionalFields().build();
    }

    /**
     * Creates and returns a {@link GetMedicalDataSourcesRequest} with given {@code packageNames}.
     */
    public static GetMedicalDataSourcesRequest getGetMedicalDataSourceRequest(
            Set<String> packageNames) {
        GetMedicalDataSourcesRequest.Builder builder = new GetMedicalDataSourcesRequest.Builder();
        for (String packageName : packageNames) {
            builder.addPackageName(packageName);
        }
        return builder.build();
    }

    /**
     * Creates and returns a {@link CreateMedicalDataSourceRequest.Builder} with default arguments
     * for required fields.
     */
    public static CreateMedicalDataSourceRequest.Builder
            getCreateMedicalDataSourceRequestBuilder() {
        return new CreateMedicalDataSourceRequest.Builder(
                DATA_SOURCE_FHIR_BASE_URI, DATA_SOURCE_DISPLAY_NAME, DATA_SOURCE_FHIR_VERSION);
    }

    /**
     * Creates and returns a {@link CreateMedicalDataSourceRequest.Builder} with default arguments,
     * with the given suffix appended to the base URI and name, to enable different data sources to
     * be created.
     */
    public static CreateMedicalDataSourceRequest.Builder getCreateMedicalDataSourceRequestBuilder(
            String suffix) {
        Uri fhirBaseUri = Uri.withAppendedPath(DATA_SOURCE_FHIR_BASE_URI, "/" + suffix);
        return new CreateMedicalDataSourceRequest.Builder(
                fhirBaseUri, DATA_SOURCE_DISPLAY_NAME + " " + suffix, DATA_SOURCE_FHIR_VERSION);
    }

    /** Creates and returns a {@link CreateMedicalDataSourceRequest} with default arguments. */
    public static CreateMedicalDataSourceRequest getCreateMedicalDataSourceRequest() {
        return getCreateMedicalDataSourceRequestBuilder().build();
    }

    /**
     * Creates and returns a {@link CreateMedicalDataSourceRequest} with the default arguments, with
     * the given suffix appended to the base URI and name, to enable different data sources to be
     * created.
     */
    public static CreateMedicalDataSourceRequest getCreateMedicalDataSourceRequest(String suffix) {
        return getCreateMedicalDataSourceRequestBuilder(suffix).build();
    }

    /**
     * Creates and returns a {@link FhirResource.Builder} with default arguments.
     *
     * <p>By default, it contains the {@link PhrDataFactory#FHIR_DATA_IMMUNIZATION}.
     */
    public static FhirResource.Builder getFhirResourceBuilder() {
        return new FhirResource.Builder(
                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                FHIR_RESOURCE_ID_IMMUNIZATION,
                FHIR_DATA_IMMUNIZATION);
    }

    /**
     * Creates and returns a {@link FhirResource} with default arguments.
     *
     * <p>By default, it contains the {@link PhrDataFactory#FHIR_DATA_IMMUNIZATION}.
     */
    public static FhirResource getFhirResource() {
        return getFhirResourceBuilder().build();
    }

    /**
     * Creates and returns a {@link FhirResource} with the status field of the {@link
     * PhrDataFactory#FHIR_DATA_IMMUNIZATION} updated.
     */
    public static FhirResource getUpdatedImmunizationFhirResource() throws JSONException {
        return new FhirResource.Builder(
                        FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION,
                        addCompletedStatus(FHIR_DATA_IMMUNIZATION))
                .build();
    }

    /**
     * Creates and returns a {@link FhirResource} with the status field of the {@link
     * PhrDataFactory#FHIR_DATA_ALLERGY} updated.
     */
    public static FhirResource getUpdatedAllergyFhirResource() throws JSONException {
        return new FhirResource.Builder(
                        FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                        FHIR_RESOURCE_ID_ALLERGY,
                        addCompletedStatus(FHIR_DATA_ALLERGY))
                .build();
    }

    /**
     * Creates and returns a {@link FhirResource} with {@link
     * PhrDataFactory#DIFFERENT_FHIR_DATA_IMMUNIZATION} data.
     */
    public static FhirResource getFhirResourceDifferentImmunization() {
        return new FhirResource.Builder(
                        FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        DIFFERENT_FHIR_RESOURCE_ID_IMMUNIZATION,
                        DIFFERENT_FHIR_DATA_IMMUNIZATION)
                .build();
    }

    /**
     * Creates and returns a {@link FhirResource} with {@link PhrDataFactory#FHIR_DATA_ALLERGY}
     * data.
     */
    public static FhirResource getFhirResourceAllergy() {
        return new FhirResource.Builder(
                        FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                        FHIR_RESOURCE_ID_ALLERGY,
                        FHIR_DATA_ALLERGY)
                .build();
    }

    /**
     * Creates and returns a {@link FhirResource} with {@link
     * PhrDataFactory#DIFFERENT_FHIR_DATA_ALLERGY} data.
     */
    public static FhirResource getFhirResourceDifferentAllergy() {
        return new FhirResource.Builder(
                        FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                        DIFFERENT_FHIR_RESOURCE_ID_ALLERGY,
                        DIFFERENT_FHIR_DATA_ALLERGY)
                .build();
    }

    /**
     * Creates and returns a {@link MedicalResource.Builder} with default arguments.
     *
     * <p>By default, it contains the {@link PhrDataFactory#FHIR_DATA_IMMUNIZATION}.
     */
    public static MedicalResource.Builder getMedicalResourceBuilder() {
        return new MedicalResource.Builder(
                MEDICAL_RESOURCE_TYPE_VACCINES, DATA_SOURCE_ID, FHIR_VERSION_R4, getFhirResource());
    }

    /**
     * Creates and returns a {@link MedicalResource} with default arguments.
     *
     * <p>By default, it contains the {@link PhrDataFactory#FHIR_DATA_IMMUNIZATION}.
     */
    public static MedicalResource getMedicalResource() {
        return getMedicalResourceBuilder().build();
    }

    /**
     * Creates and returns a {@link MedicalResource} of type {@link
     * MedicalResource#MEDICAL_RESOURCE_TYPE_VACCINES} which contains {@link
     * PhrDataFactory#getFhirResource} data, with the given {@code dataSourceId}.
     */
    public static MedicalResource createVaccineMedicalResource(String dataSourceId) {
        return new MedicalResource.Builder(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        dataSourceId,
                        FHIR_VERSION_R4,
                        getFhirResource())
                .build();
    }

    /**
     * Creates and returns a {@link MedicalResource} of type {@link
     * MedicalResource#MEDICAL_RESOURCE_TYPE_VACCINES} which contains {@link
     * PhrDataFactory#getFhirResourceDifferentImmunization} data, with the given {@code
     * dataSourceId}.
     *
     * <p>The contained FHIR data has a different resource ID than the above {@link
     * PhrDataFactory#createVaccineMedicalResource}.
     */
    public static MedicalResource createDifferentVaccineMedicalResource(String dataSourceId) {
        return new MedicalResource.Builder(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        dataSourceId,
                        FHIR_VERSION_R4,
                        getFhirResourceDifferentImmunization())
                .build();
    }

    /**
     * Creates and returns a {@link MedicalResource} of type {@link
     * MedicalResource#MEDICAL_RESOURCE_TYPE_VACCINES} which contains {@link
     * PhrDataFactory#getUpdatedImmunizationFhirResource} data, with the given {@code dataSourceId}.
     *
     * <p>The contained FHIR data has the same resource ID as the above {@link
     * PhrDataFactory#createVaccineMedicalResource}, but data is updated with a "status" field
     * added.
     */
    public static MedicalResource createUpdatedVaccineMedicalResource(String dataSourceId)
            throws JSONException {
        return new MedicalResource.Builder(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        dataSourceId,
                        FHIR_VERSION_R4,
                        getUpdatedImmunizationFhirResource())
                .build();
    }

    /**
     * Creates and returns a {@link MedicalResource} of type {@link
     * MedicalResource#MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES} which contains {@link
     * PhrDataFactory#getFhirResourceAllergy} data, with the given {@code dataSourceId}.
     */
    public static MedicalResource createAllergyMedicalResource(String dataSourceId) {
        return new MedicalResource.Builder(
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                        dataSourceId,
                        FHIR_VERSION_R4,
                        getFhirResourceAllergy())
                .build();
    }

    /**
     * Creates and returns a {@link MedicalResource} of type {@link
     * MedicalResource#MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES} which contains {@link
     * PhrDataFactory#getFhirResourceDifferentAllergy} data, with the given {@code dataSourceId}.
     *
     * <p>The contained FHIR data has a different resource ID than the above {@link
     * PhrDataFactory#createAllergyMedicalResource}.
     */
    public static MedicalResource createDifferentAllergyMedicalResource(String dataSourceId) {
        return new MedicalResource.Builder(
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                        dataSourceId,
                        FHIR_VERSION_R4,
                        getFhirResourceDifferentAllergy())
                .build();
    }

    /**
     * Creates and returns a {@link MedicalResource} of type {@link
     * MedicalResource#MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES} which contains {@link
     * PhrDataFactory#getUpdatedAllergyFhirResource} data, with the given {@code dataSourceId}.
     *
     * <p>The contained FHIR data has the same resource ID as the above {@link
     * PhrDataFactory#createAllergyMedicalResource}, but data is updated with a "status" field
     * added.
     */
    public static MedicalResource createUpdatedAllergyMedicalResource(String dataSourceId)
            throws JSONException {
        return new MedicalResource.Builder(
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                        dataSourceId,
                        FHIR_VERSION_R4,
                        getUpdatedAllergyFhirResource())
                .build();
    }

    /**
     * Creates and returns a {@link UpsertMedicalResourceRequest.Builder} with default arguments.
     */
    public static UpsertMedicalResourceRequest.Builder getUpsertMedicalResourceRequestBuilder() {
        return new UpsertMedicalResourceRequest.Builder(
                DATA_SOURCE_ID, FHIR_VERSION_R4, FHIR_DATA_IMMUNIZATION);
    }

    /** Creates and returns a {@link UpsertMedicalResourceRequest} with default arguments. */
    public static UpsertMedicalResourceRequest getUpsertMedicalResourceRequest() {
        return getUpsertMedicalResourceRequestBuilder().build();
    }

    /**
     * Creates and returns a {@link MedicalResourceId} with default arguments.
     *
     * <p>By default, it contains the {@link FhirResource#FHIR_RESOURCE_TYPE_IMMUNIZATION} and
     * {@link PhrDataFactory#FHIR_RESOURCE_ID_IMMUNIZATION}.
     */
    public static MedicalResourceId getMedicalResourceId() {
        return new MedicalResourceId(
                DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_RESOURCE_ID_IMMUNIZATION);
    }

    /** Returns the FHIR resource id field from the given {@code fhirJSON} string. */
    public static String getFhirResourceId(String fhirJSON) throws JSONException {
        return new JSONObject(fhirJSON).getString(RESOURCE_ID_FIELD_NAME);
    }

    /** Returns an updated FHIR JSON string with an added status field. */
    public static String addCompletedStatus(String fhirJSON) throws JSONException {
        JSONObject jsonObj = new JSONObject(fhirJSON);
        jsonObj.put("status", "completed");
        return jsonObj.toString();
    }

    /**
     * Creates a number of vaccine resources based on the given {@code numOfResources} and {@code
     * dataSourceId}.
     */
    public static List<MedicalResource> createVaccineMedicalResources(
            int numOfResources, String dataSourceId) {
        FhirVersion fhirVersion = parseFhirVersion(R4_VERSION_STRING);
        List<MedicalResource> medicalResources = new ArrayList<>();
        for (int i = 0; i < numOfResources; i++) {
            String fhirResourceId = "id." + i;
            FhirResource fhirResource =
                    new FhirResource.Builder(
                                    FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                    fhirResourceId,
                                    new ImmunizationBuilder().setId(fhirResourceId).toJson())
                            .build();
            MedicalResource medicalResource =
                    new MedicalResource.Builder(
                                    MEDICAL_RESOURCE_TYPE_VACCINES,
                                    dataSourceId,
                                    fhirVersion,
                                    fhirResource)
                            .build();
            medicalResources.add(medicalResource);
        }
        return medicalResources;
    }

    /**
     * Creates a number of allergy resources based on the given {@code numOfResources} and {@code
     * dataSourceId}.
     */
    public static List<MedicalResource> createAllergyMedicalResources(
            int numOfResources, String dataSourceId) {
        FhirVersion fhirVersion = parseFhirVersion(R4_VERSION_STRING);
        List<MedicalResource> medicalResources = new ArrayList<>();
        for (int i = 0; i < numOfResources; i++) {
            FhirResource fhirResource =
                    new FhirResource.Builder(
                                    FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                                    "id/" + i,
                                    FHIR_DATA_ALLERGY)
                            .build();
            MedicalResource medicalResource =
                    new MedicalResource.Builder(
                                    MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                    dataSourceId,
                                    fhirVersion,
                                    fhirResource)
                            .build();
            medicalResources.add(medicalResource);
        }
        return medicalResources;
    }

    /**
     * Given two {@link MedicalDataSource}s, compare whether they are equal or not. This ignores the
     * {@link MedicalDataSource#getLastDataUpdateTime()}.
     */
    public static boolean isMedicalDataSourceEqual(
            MedicalDataSource actual, MedicalDataSource expected) {
        return Objects.equals(actual.getId(), expected.getId())
                && Objects.equals(actual.getFhirVersion(), expected.getFhirVersion())
                && Objects.equals(actual.getFhirBaseUri(), expected.getFhirBaseUri())
                && Objects.equals(actual.getPackageName(), expected.getPackageName())
                && Objects.equals(actual.getDisplayName(), expected.getDisplayName());
    }
}
