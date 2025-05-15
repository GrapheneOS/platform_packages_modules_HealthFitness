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

import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_CONDITION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_DEVICE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_LOCATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_MEDICATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_REQUEST;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_OBSERVATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_ORGANIZATION;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_PATIENT;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_PROCEDURE;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4B;

import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_EXTENSION_VALIDATION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ADDRESS;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_AGE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ANNOTATION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ATTACHMENT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_BASE64_BINARY;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_BOOLEAN;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CANONICAL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODEABLE_CONCEPT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODEABLE_REFERENCE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODING;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CONTACT_POINT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DECIMAL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DOSAGE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DURATION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_EXTENSION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_HUMAN_NAME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_IDENTIFIER;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_IMMUNIZATION_EDUCATION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_IMMUNIZATION_PERFORMER;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_IMMUNIZATION_PROTOCOLAPPLIED;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_IMMUNIZATION_REACTION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_INSTANT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_INTEGER;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_LOCATION_POSITION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_MARKDOWN;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_MEDICATIONREQUEST_DISPENSEREQUEST;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_MEDICATIONREQUEST_DISPENSEREQUEST_INITIALFILL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_MEDICATION_BATCH;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_MEDICATION_INGREDIENT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_META;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_NARRATIVE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_OBSERVATION_COMPONENT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_OBSERVATION_REFERENCERANGE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_OID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ORGANIZATION_CONTACT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_PATIENT_CONTACT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_PERIOD;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_POSITIVE_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_PRACTITIONERROLE_AVAILABLETIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_PRACTITIONER_QUALIFICATION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_QUANTITY;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_RANGE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_RATIO_RANGE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_REFERENCE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_RESOURCE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_STRING;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UNSIGNED_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UNSPECIFIED;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_URI;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_URL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UUID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_XHTML;
import static com.android.server.healthconnect.proto.R4FhirType.UNRECOGNIZED;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.FhirVersion;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.proto.FhirComplexTypeConfig;
import com.android.server.healthconnect.proto.FhirFieldConfig;
import com.android.server.healthconnect.proto.MultiTypeFieldConfig;
import com.android.server.healthconnect.proto.R4FhirType;

import com.google.common.truth.Correspondence;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class FhirSpecProviderTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final Set<R4FhirType> R4_PRIMITIVE_TYPES =
            Set.of(
                    R4_FHIR_TYPE_BOOLEAN,
                    R4_FHIR_TYPE_INTEGER,
                    R4_FHIR_TYPE_CANONICAL,
                    R4_FHIR_TYPE_CODE,
                    R4_FHIR_TYPE_DATE,
                    R4_FHIR_TYPE_DATE_TIME,
                    R4_FHIR_TYPE_ID,
                    R4_FHIR_TYPE_INSTANT,
                    R4_FHIR_TYPE_STRING,
                    R4_FHIR_TYPE_TIME,
                    R4_FHIR_TYPE_URI,
                    R4_FHIR_TYPE_DECIMAL,
                    R4_FHIR_TYPE_POSITIVE_INT,
                    R4_FHIR_TYPE_UNSIGNED_INT,
                    R4_FHIR_TYPE_BASE64_BINARY,
                    R4_FHIR_TYPE_MARKDOWN,
                    R4_FHIR_TYPE_OID,
                    R4_FHIR_TYPE_URL,
                    R4_FHIR_TYPE_UUID,
                    R4_FHIR_TYPE_XHTML);

    private static final Set<R4FhirType> R4_COMPLEX_TYPES_WITHOUT_VALIDATION =
            Set.of(R4_FHIR_TYPE_RESOURCE);

    private static final Correspondence<MultiTypeFieldConfig, MultiTypeFieldConfig>
            MULTI_TYPE_CONFIG_EQUIVALENCE =
                    Correspondence.from(
                            FhirSpecProviderTest::isMultiTypeFieldConfigEqual,
                            "isMultiTypeFieldConfigEqual");

    @Test
    public void testConstructor_r4FhirVersion_parsesProtoFileAndSucceeds() {
        new FhirSpecProvider(FHIR_VERSION_R4);
    }

    @Test
    public void testConstructor_r4BFhirVersion_succeeds() {
        new FhirSpecProvider(FHIR_VERSION_R4B);
    }

    @Test
    public void testConstructor_otherFhirVersion_throws() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new FhirSpecProvider(FhirVersion.parseFhirVersion("4.0.0")));
    }

    @Test
    public void testIsPrimitiveType_allPrimitiveTypes_returnTrue() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);

        for (R4FhirType type : R4_PRIMITIVE_TYPES) {
            assertWithMessage("Expected to be true for type: " + type.name())
                    .that(spec.isPrimitiveType(type))
                    .isTrue();
        }
    }

    @Test
    public void testIsPrimitiveType_allNonPrimitiveTypes_returnFalse() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);

        for (R4FhirType type : R4FhirType.values()) {
            if (R4_PRIMITIVE_TYPES.contains(type)) {
                continue;
            }

            assertWithMessage("Expected to be false for type: " + type.name())
                    .that(spec.isPrimitiveType(type))
                    .isFalse();
        }
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION, FLAG_PHR_FHIR_EXTENSION_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_allTypesWithoutValidation_returnNull() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);

        for (R4FhirType type : R4_COMPLEX_TYPES_WITHOUT_VALIDATION) {
            assertWithMessage("Expected to be null for type: " + type.name())
                    .that(spec.getFhirComplexTypeConfig(type, FHIR_VERSION_R4))
                    .isNull();
        }
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_allPrimitiveTypes_throwException() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);

        for (R4FhirType type : R4_PRIMITIVE_TYPES) {
            assertThrows(
                    "Expected exception for type: " + type.name(),
                    IllegalArgumentException.class,
                    () -> spec.getFhirComplexTypeConfig(type, FHIR_VERSION_R4));
        }
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION, FLAG_PHR_FHIR_EXTENSION_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_complexTypes_returnNonEmptyConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<R4FhirType> typesToSkip =
                new ArrayList<>(List.of(R4_FHIR_TYPE_UNSPECIFIED, UNRECOGNIZED));
        typesToSkip.addAll(R4_PRIMITIVE_TYPES);
        typesToSkip.addAll(R4_COMPLEX_TYPES_WITHOUT_VALIDATION);
        List<R4FhirType> expectedComplexTypesWithConfig =
                Arrays.stream(R4FhirType.values())
                        .filter(type -> !typesToSkip.contains(type))
                        .toList();

        for (R4FhirType type : expectedComplexTypesWithConfig) {
            FhirComplexTypeConfig config = spec.getFhirComplexTypeConfig(type, FHIR_VERSION_R4);

            assertWithMessage("Expected config for type: " + type.name())
                    .that(config)
                    .isInstanceOf(FhirComplexTypeConfig.class);
            assertWithMessage("Expected non empty config for type: " + type.name())
                    .that(config)
                    .isNotEqualTo(FhirComplexTypeConfig.getDefaultInstance());
        }
    }

    @Test
    public void testGetFhirResourceTypeConfig_immunization_returnsConfig() {
        // Compare the full expected Fhir spec for Immunization. For the other resources we just
        // check a number of fields are as expected instead of the full list.
        // The test is based on the published spec at - https://hl7.org/fhir/R4/immunization.html
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("status", "vaccineCode", "patient");
        List<MultiTypeFieldConfig> expectedMultiTypeFieldConfigs =
                List.of(
                        MultiTypeFieldConfig.newBuilder()
                                .setName("occurrence[x]")
                                .addAllTypedFieldNames(
                                        List.of("occurrenceDateTime", "occurrenceString"))
                                .setIsRequired(true)
                                .build());
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.ofEntries(
                        Map.entry("id", createFhirFieldConfig(false, R4_FHIR_TYPE_ID)),
                        Map.entry(
                                "resourceType", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING)),
                        Map.entry("meta", createFhirFieldConfig(false, R4_FHIR_TYPE_META)),
                        Map.entry("implicitRules", createFhirFieldConfig(false, R4_FHIR_TYPE_URI)),
                        Map.entry("language", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE)),
                        Map.entry("text", createFhirFieldConfig(false, R4_FHIR_TYPE_NARRATIVE)),
                        Map.entry("contained", createFhirFieldConfig(true, R4_FHIR_TYPE_RESOURCE)),
                        Map.entry("extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION)),
                        Map.entry(
                                "modifierExtension",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION)),
                        Map.entry(
                                "identifier", createFhirFieldConfig(true, R4_FHIR_TYPE_IDENTIFIER)),
                        Map.entry("status", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE)),
                        Map.entry(
                                "statusReason",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry(
                                "vaccineCode",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry("patient", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE)),
                        Map.entry(
                                "encounter", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE)),
                        Map.entry(
                                "occurrenceDateTime",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME)),
                        Map.entry(
                                "occurrenceString",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_STRING)),
                        Map.entry("recorded", createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME)),
                        Map.entry(
                                "primarySource",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN)),
                        Map.entry(
                                "reportOrigin",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry("location", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE)),
                        Map.entry(
                                "manufacturer",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE)),
                        Map.entry("lotNumber", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING)),
                        Map.entry(
                                "expirationDate", createFhirFieldConfig(false, R4_FHIR_TYPE_DATE)),
                        Map.entry(
                                "site",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry(
                                "route",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        // Note that on the FHIR website this has the type "SimpleQuantity", but in
                        // the spec the type code is "Quantity" with profile "SimpleQuantity"
                        // This profile only adds an additional constraint, so is not needed for our
                        // validations.
                        Map.entry(
                                "doseQuantity",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_QUANTITY)),
                        Map.entry(
                                "performer",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_IMMUNIZATION_PERFORMER)),
                        Map.entry("note", createFhirFieldConfig(true, R4_FHIR_TYPE_ANNOTATION)),
                        Map.entry(
                                "reasonCode",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry(
                                "reasonReference",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_REFERENCE)),
                        Map.entry(
                                "isSubpotent", createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN)),
                        Map.entry(
                                "subpotentReason",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry(
                                "education",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_IMMUNIZATION_EDUCATION)),
                        Map.entry(
                                "programEligibility",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry(
                                "fundingSource",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                        Map.entry(
                                "reaction",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_IMMUNIZATION_REACTION)),
                        Map.entry(
                                "protocolApplied",
                                createFhirFieldConfig(
                                        true, R4_FHIR_TYPE_IMMUNIZATION_PROTOCOLAPPLIED)));

        FhirComplexTypeConfig immunizationConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_IMMUNIZATION);

        assertThat(immunizationConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(immunizationConfig.getMultiTypeFieldsList())
                .comparingElementsUsing(MULTI_TYPE_CONFIG_EQUIVALENCE)
                .containsExactlyElementsIn(expectedMultiTypeFieldConfigs);
        assertThat(immunizationConfig.getAllowedFieldNamesToConfigMap())
                .containsExactlyEntriesIn(expectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_allergy_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("patient");
        List<MultiTypeFieldConfig> expectedMultiTypeFieldConfigs =
                List.of(
                        MultiTypeFieldConfig.newBuilder()
                                .setName("onset[x]")
                                .addAllTypedFieldNames(
                                        List.of(
                                                "onsetDateTime",
                                                "onsetAge",
                                                "onsetPeriod",
                                                "onsetRange",
                                                "onsetString"))
                                .setIsRequired(false)
                                .build());
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_ID),
                        "resourceType", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "meta", createFhirFieldConfig(false, R4_FHIR_TYPE_META),
                        "clinicalStatus",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT),
                        "criticality", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE),
                        "onsetRange", createFhirFieldConfig(false, R4_FHIR_TYPE_RANGE),
                        "contained", createFhirFieldConfig(true, R4_FHIR_TYPE_RESOURCE));

        FhirComplexTypeConfig allergyConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE);

        assertThat(allergyConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(allergyConfig.getMultiTypeFieldsList())
                .comparingElementsUsing(MULTI_TYPE_CONFIG_EQUIVALENCE)
                .containsExactlyElementsIn(expectedMultiTypeFieldConfigs);
        assertThat(allergyConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_observation_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("status", "code");
        List<MultiTypeFieldConfig> expectedMultiTypeFieldConfigs =
                List.of(
                        MultiTypeFieldConfig.newBuilder()
                                .setName("effective[x]")
                                .addAllTypedFieldNames(
                                        List.of(
                                                "effectiveDateTime",
                                                "effectivePeriod",
                                                "effectiveTiming",
                                                "effectiveInstant"))
                                .setIsRequired(false)
                                .build(),
                        MultiTypeFieldConfig.newBuilder()
                                .setName("value[x]")
                                .addAllTypedFieldNames(
                                        List.of(
                                                "valueQuantity",
                                                "valueCodeableConcept",
                                                "valueString",
                                                "valueBoolean",
                                                "valueInteger",
                                                "valueRange",
                                                "valueRatio",
                                                "valueSampledData",
                                                "valueTime",
                                                "valueDateTime",
                                                "valuePeriod"))
                                .setIsRequired(false)
                                .build());
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "encounter",
                        createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE),
                        "valueQuantity",
                        createFhirFieldConfig(false, R4_FHIR_TYPE_QUANTITY),
                        "valueDateTime",
                        createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME),
                        "dataAbsentReason",
                        createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT),
                        "referenceRange",
                        createFhirFieldConfig(true, R4_FHIR_TYPE_OBSERVATION_REFERENCERANGE),
                        "component",
                        createFhirFieldConfig(true, R4_FHIR_TYPE_OBSERVATION_COMPONENT));

        FhirComplexTypeConfig observationConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_OBSERVATION);

        assertThat(observationConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(observationConfig.getMultiTypeFieldsList())
                .comparingElementsUsing(MULTI_TYPE_CONFIG_EQUIVALENCE)
                .containsExactlyElementsIn(expectedMultiTypeFieldConfigs);
        assertThat(observationConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_condition_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("subject");
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_ID),
                        "resourceType", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "code", createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT),
                        "onsetDateTime", createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME),
                        "onsetAge", createFhirFieldConfig(false, R4_FHIR_TYPE_AGE));

        FhirComplexTypeConfig conditionConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_CONDITION);

        assertThat(conditionConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(conditionConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_procedure_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("status", "subject");
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_ID),
                        "resourceType", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "subject", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE),
                        "performedRange", createFhirFieldConfig(false, R4_FHIR_TYPE_RANGE),
                        "note", createFhirFieldConfig(true, R4_FHIR_TYPE_ANNOTATION));

        FhirComplexTypeConfig procedureConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_PROCEDURE);

        assertThat(procedureConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(procedureConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_medication_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "manufacturer", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE),
                        "ingredient",
                                createFhirFieldConfig(true, R4_FHIR_TYPE_MEDICATION_INGREDIENT),
                        "batch", createFhirFieldConfig(false, R4_FHIR_TYPE_MEDICATION_BATCH));

        FhirComplexTypeConfig medicationConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_MEDICATION);

        assertThat(medicationConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(medicationConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_medicationRequest_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("status", "intent", "subject");
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "doNotPerform", createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN),
                        "medicationCodeableConcept",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT),
                        "medicationReference", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE),
                        "dosageInstruction", createFhirFieldConfig(true, R4_FHIR_TYPE_DOSAGE),
                        "priorPrescription", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE));

        FhirComplexTypeConfig medicationRequestConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_MEDICATION_REQUEST);

        assertThat(medicationRequestConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(medicationRequestConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_medicationStatement_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("status", "subject");
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "dateAsserted", createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME),
                        "dosage", createFhirFieldConfig(true, R4_FHIR_TYPE_DOSAGE));

        FhirComplexTypeConfig medicationStatementConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT);

        assertThat(medicationStatementConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(medicationStatementConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_patient_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "active", createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN),
                        "name", createFhirFieldConfig(true, R4_FHIR_TYPE_HUMAN_NAME),
                        "deceasedBoolean", createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN),
                        "photo", createFhirFieldConfig(true, R4_FHIR_TYPE_ATTACHMENT),
                        "contact", createFhirFieldConfig(true, R4_FHIR_TYPE_PATIENT_CONTACT));

        FhirComplexTypeConfig patientConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_PATIENT);

        assertThat(patientConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(patientConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_practitioner_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "name", createFhirFieldConfig(true, R4_FHIR_TYPE_HUMAN_NAME),
                        "telecom", createFhirFieldConfig(true, R4_FHIR_TYPE_CONTACT_POINT),
                        "qualification",
                                createFhirFieldConfig(
                                        true, R4_FHIR_TYPE_PRACTITIONER_QUALIFICATION));

        FhirComplexTypeConfig practitionerConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_PRACTITIONER);

        assertThat(practitionerConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(practitionerConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_practitionerRole_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "practitioner", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE),
                        "healthcareService", createFhirFieldConfig(true, R4_FHIR_TYPE_REFERENCE),
                        "availableTime",
                                createFhirFieldConfig(
                                        true, R4_FHIR_TYPE_PRACTITIONERROLE_AVAILABLETIME),
                        "endpoint", createFhirFieldConfig(true, R4_FHIR_TYPE_REFERENCE));

        FhirComplexTypeConfig practitionerRoleConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE);

        assertThat(practitionerRoleConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(practitionerRoleConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_location_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "operationalStatus", createFhirFieldConfig(false, R4_FHIR_TYPE_CODING),
                        "address", createFhirFieldConfig(false, R4_FHIR_TYPE_ADDRESS),
                        "position", createFhirFieldConfig(false, R4_FHIR_TYPE_LOCATION_POSITION));

        FhirComplexTypeConfig locationConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_LOCATION);

        assertThat(locationConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(locationConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    public void testGetFhirResourceTypeConfig_organization_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> atLeastExpectedFieldConfigsMap =
                Map.of(
                        "name", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "alias", createFhirFieldConfig(true, R4_FHIR_TYPE_STRING),
                        "contact", createFhirFieldConfig(true, R4_FHIR_TYPE_ORGANIZATION_CONTACT));

        FhirComplexTypeConfig organizationConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_ORGANIZATION);

        assertThat(organizationConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(organizationConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(atLeastExpectedFieldConfigsMap);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_RESOURCE)
    public void testGetFhirResourceTypeConfig_deviceFlagOn_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);

        FhirComplexTypeConfig deviceConfig =
                spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_DEVICE);

        assertThat(deviceConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(List.<String>of());
        assertThat(deviceConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(Map.<String, FhirFieldConfig>of());
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_RESOURCE)
    public void testGetFhirResourceTypeConfig_deviceFlagOff_throws() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);

        assertThrows(
                IllegalArgumentException.class,
                () -> spec.getFhirResourceTypeConfig(FHIR_RESOURCE_TYPE_DEVICE));
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_identifier_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "use", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE),
                        "type", createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT),
                        "system", createFhirFieldConfig(false, R4_FHIR_TYPE_URI),
                        "value", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "period", createFhirFieldConfig(false, R4_FHIR_TYPE_PERIOD),
                        "assigner", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_IDENTIFIER, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_period_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "start", createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME),
                        "end", createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_PERIOD, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_codeableConcept_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "coding", createFhirFieldConfig(true, R4_FHIR_TYPE_CODING),
                        "text", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_CODEABLE_CONCEPT, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_coding_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "system", createFhirFieldConfig(false, R4_FHIR_TYPE_URI),
                        "version", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "code", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE),
                        "display", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "userSelected", createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_CODING, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_annotation_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("text");
        List<MultiTypeFieldConfig> expectedMultiTypeFieldConfigs =
                List.of(
                        MultiTypeFieldConfig.newBuilder()
                                .setName("author[x]")
                                .addAllTypedFieldNames(List.of("authorReference", "authorString"))
                                .setIsRequired(false)
                                .build());
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "authorReference", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE),
                        "authorString", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "time", createFhirFieldConfig(false, R4_FHIR_TYPE_DATE_TIME),
                        "text", createFhirFieldConfig(false, R4_FHIR_TYPE_MARKDOWN));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_ANNOTATION, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getMultiTypeFieldsList())
                .comparingElementsUsing(MULTI_TYPE_CONFIG_EQUIVALENCE)
                .containsExactlyElementsIn(expectedMultiTypeFieldConfigs);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_quantity_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "value", createFhirFieldConfig(false, R4_FHIR_TYPE_DECIMAL),
                        "comparator", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE),
                        "unit", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "system", createFhirFieldConfig(false, R4_FHIR_TYPE_URI),
                        "code", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_QUANTITY, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_doubleChildTypeMedicationRequest_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "modifierExtension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "initialFill",
                                createFhirFieldConfig(
                                        false,
                                        R4_FHIR_TYPE_MEDICATIONREQUEST_DISPENSEREQUEST_INITIALFILL),
                        "dispenseInterval", createFhirFieldConfig(false, R4_FHIR_TYPE_DURATION),
                        "validityPeriod", createFhirFieldConfig(false, R4_FHIR_TYPE_PERIOD),
                        "numberOfRepeatsAllowed",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_UNSIGNED_INT),
                        "quantity", createFhirFieldConfig(false, R4_FHIR_TYPE_QUANTITY),
                        "expectedSupplyDuration",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_DURATION),
                        "performer", createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE));
        Map<String, FhirFieldConfig> expectedFieldConfigsMapDoubleChildType =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "modifierExtension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "quantity", createFhirFieldConfig(false, R4_FHIR_TYPE_QUANTITY),
                        "duration", createFhirFieldConfig(false, R4_FHIR_TYPE_DURATION));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(
                        R4_FHIR_TYPE_MEDICATIONREQUEST_DISPENSEREQUEST, FHIR_VERSION_R4);
        FhirComplexTypeConfig doubleChildTypeConfig =
                spec.getFhirComplexTypeConfig(
                        R4_FHIR_TYPE_MEDICATIONREQUEST_DISPENSEREQUEST_INITIALFILL,
                        FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
        assertThat(doubleChildTypeConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMapDoubleChildType);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION, FLAG_PHR_FHIR_EXTENSION_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_extensionForR4_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of("url");
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "url", createFhirFieldConfig(false, R4_FHIR_TYPE_URI),
                        "valueBoolean", createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN),
                        "valueCodeableConcept",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_EXTENSION, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .doesNotContainKey("valueRatioRange");
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .doesNotContainKey("valueCodeableReference");
        assertThat(receivedConfig.getMultiTypeFieldsList()).hasSize(1);
        MultiTypeFieldConfig multiTypeConfig = receivedConfig.getMultiTypeFieldsList().get(0);
        assertThat(multiTypeConfig.getIsRequired()).isEqualTo(false);
        assertThat(multiTypeConfig.getName()).isEqualTo("value[x]");
        assertThat(multiTypeConfig.getTypedFieldNamesList())
                .containsAtLeast("valueBoolean", "valueCodeableConcept");
        assertThat(multiTypeConfig.getTypedFieldNamesList()).doesNotContain("valueRatioRange");
        assertThat(multiTypeConfig.getTypedFieldNamesList())
                .doesNotContain("valueCodeableReference");
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION, FLAG_PHR_FHIR_EXTENSION_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_extensionForR4B_returnsConfigWithAdditionalFields() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4B);
        List<String> expectedRequiredFields = List.of("url");
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "url", createFhirFieldConfig(false, R4_FHIR_TYPE_URI),
                        "valueBoolean", createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN),
                        "valueCodeableConcept",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT),
                        "valueRatioRange", createFhirFieldConfig(false, R4_FHIR_TYPE_RATIO_RANGE),
                        "valueCodeableReference",
                                createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_REFERENCE));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_EXTENSION, FHIR_VERSION_R4B);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsAtLeastEntriesIn(expectedFieldConfigsMap);
        assertThat(receivedConfig.getMultiTypeFieldsList()).hasSize(1);
        MultiTypeFieldConfig multiTypeConfig = receivedConfig.getMultiTypeFieldsList().get(0);
        assertThat(multiTypeConfig.getIsRequired()).isEqualTo(false);
        assertThat(multiTypeConfig.getName()).isEqualTo("value[x]");
        assertThat(multiTypeConfig.getTypedFieldNamesList())
                .containsAtLeast(
                        "valueBoolean",
                        "valueCodeableConcept",
                        "valueRatioRange",
                        "valueCodeableReference");
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION, FLAG_PHR_FHIR_EXTENSION_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_ratioRange_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id", createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension", createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "lowNumerator", createFhirFieldConfig(false, R4_FHIR_TYPE_QUANTITY),
                        "highNumerator", createFhirFieldConfig(false, R4_FHIR_TYPE_QUANTITY),
                        "denominator", createFhirFieldConfig(false, R4_FHIR_TYPE_QUANTITY));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_RATIO_RANGE, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsExactlyEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION, FLAG_PHR_FHIR_EXTENSION_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_codeableReference_returnsConfig() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);
        List<String> expectedRequiredFields = List.of();
        Map<String, FhirFieldConfig> expectedFieldConfigsMap =
                Map.of(
                        "id",
                        createFhirFieldConfig(false, R4_FHIR_TYPE_STRING),
                        "extension",
                        createFhirFieldConfig(true, R4_FHIR_TYPE_EXTENSION),
                        "concept",
                        createFhirFieldConfig(false, R4_FHIR_TYPE_CODEABLE_CONCEPT),
                        "reference",
                        createFhirFieldConfig(false, R4_FHIR_TYPE_REFERENCE));

        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_CODEABLE_REFERENCE, FHIR_VERSION_R4);

        assertThat(receivedConfig.getRequiredFieldsList())
                .containsExactlyElementsIn(expectedRequiredFields);
        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsExactlyEntriesIn(expectedFieldConfigsMap);
    }

    @EnableFlags({FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION})
    @Test
    public void testGetFhirComplexTypeConfig_contentReferenceInSpec_configReferencesCorrectType() {
        FhirSpecProvider spec = new FhirSpecProvider(FHIR_VERSION_R4);

        // The Observation.component.referenceRange spec has a "contentReference" to the
        // Observation.referenceRange child type, so should have the same type
        FhirComplexTypeConfig receivedConfig =
                spec.getFhirComplexTypeConfig(R4_FHIR_TYPE_OBSERVATION_COMPONENT, FHIR_VERSION_R4);

        assertThat(receivedConfig.getAllowedFieldNamesToConfigMap())
                .containsEntry(
                        "referenceRange",
                        createFhirFieldConfig(true, R4_FHIR_TYPE_OBSERVATION_REFERENCERANGE));
    }

    private static FhirFieldConfig createFhirFieldConfig(boolean isArray, R4FhirType r4Type) {
        return FhirFieldConfig.newBuilder().setIsArray(isArray).setR4Type(r4Type).build();
    }

    /**
     * Given two {@link MultiTypeFieldConfig}s, compare whether they are equal or not. This ignores
     * the list order when comparing {@link MultiTypeFieldConfig#getTypedFieldNamesList()} ()}.
     */
    private static boolean isMultiTypeFieldConfigEqual(
            MultiTypeFieldConfig actual, MultiTypeFieldConfig expected) {
        List<String> actualFieldNames = actual.getTypedFieldNamesList();
        List<String> expectedFieldNames = expected.getTypedFieldNamesList();
        if (actualFieldNames.size() != expectedFieldNames.size()
                || !actualFieldNames.containsAll(expectedFieldNames)) {
            return false;
        }

        MultiTypeFieldConfig actualWithoutFieldNames =
                actual.toBuilder().clearTypedFieldNames().build();
        MultiTypeFieldConfig expectedWithoutFieldNames =
                expected.toBuilder().clearTypedFieldNames().build();

        return actualWithoutFieldNames.equals(expectedWithoutFieldNames);
    }
}
