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
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_R4;

import static com.android.healthfitness.flags.Flags.FLAG_PHR_ALLOW_NULLS_IN_PRIMITIVE_VALUE_ARRAYS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_VALIDATION_DISALLOW_EMPTY_OBJECTS_ARRAYS;
import static com.android.server.healthconnect.proto.Kind.KIND_COMPLEX_TYPE;
import static com.android.server.healthconnect.proto.Kind.KIND_PRIMITIVE_TYPE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ANNOTATION;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_BOOLEAN;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODEABLE_CONCEPT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODING;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ELEMENT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_IDENTIFIER;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_STRING;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.proto.FhirComplexTypeConfig;
import com.android.server.healthconnect.proto.FhirDataType;
import com.android.server.healthconnect.proto.FhirFieldConfig;
import com.android.server.healthconnect.proto.FhirResourceSpec;
import com.android.server.healthconnect.proto.MultiTypeFieldConfig;
import com.android.server.healthconnect.proto.R4FhirType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class FhirObjectTypeValidatorTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final FhirComplexTypeConfig DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG =
            FhirComplexTypeConfig.newBuilder()
                    .addRequiredFields("status")
                    .putAllAllowedFieldNamesToConfig(
                            Map.ofEntries(
                                    Map.entry("id", createFhirFieldConfig(false, R4_FHIR_TYPE_ID)),
                                    Map.entry(
                                            "resourceType",
                                            createFhirFieldConfig(false, R4_FHIR_TYPE_STRING)),
                                    Map.entry(
                                            "identifier",
                                            createFhirFieldConfig(true, R4_FHIR_TYPE_IDENTIFIER)),
                                    Map.entry(
                                            "status",
                                            createFhirFieldConfig(false, R4_FHIR_TYPE_CODE)),
                                    Map.entry(
                                            "statusReason",
                                            createFhirFieldConfig(
                                                    false, R4_FHIR_TYPE_CODEABLE_CONCEPT)),
                                    Map.entry(
                                            "primarySource",
                                            createFhirFieldConfig(false, R4_FHIR_TYPE_BOOLEAN))))
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_IDENTIFIER =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_IDENTIFIER)
                    .setKind(KIND_COMPLEX_TYPE)
                    .setFhirComplexTypeConfig(
                            FhirComplexTypeConfig.newBuilder()
                                    .putAllowedFieldNamesToConfig(
                                            "use", createFhirFieldConfig(false, R4_FHIR_TYPE_CODE))
                                    .putAllowedFieldNamesToConfig(
                                            "value",
                                            createFhirFieldConfig(false, R4_FHIR_TYPE_STRING))
                                    .putAllowedFieldNamesToConfig(
                                            "type",
                                            createFhirFieldConfig(
                                                    false, R4_FHIR_TYPE_CODEABLE_CONCEPT)))
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_CODEABLE_CONCEPT =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_CODEABLE_CONCEPT)
                    .setKind(KIND_COMPLEX_TYPE)
                    .setFhirComplexTypeConfig(
                            FhirComplexTypeConfig.newBuilder()
                                    .putAllAllowedFieldNamesToConfig(
                                            Map.ofEntries(
                                                    Map.entry(
                                                            "id",
                                                            createFhirFieldConfig(
                                                                    false, R4_FHIR_TYPE_ID)),
                                                    Map.entry(
                                                            "coding",
                                                            createFhirFieldConfig(
                                                                    true, R4_FHIR_TYPE_CODING)),
                                                    Map.entry(
                                                            "text",
                                                            createFhirFieldConfig(
                                                                    false, R4_FHIR_TYPE_STRING)))))
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_CODING =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_CODING)
                    .setKind(KIND_COMPLEX_TYPE)
                    .setFhirComplexTypeConfig(
                            FhirComplexTypeConfig.newBuilder()
                                    .putAllAllowedFieldNamesToConfig(
                                            Map.ofEntries(
                                                    Map.entry(
                                                            "display",
                                                            createFhirFieldConfig(
                                                                    false, R4_FHIR_TYPE_STRING)))))
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_ELEMENT =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_ELEMENT)
                    .setKind(KIND_COMPLEX_TYPE)
                    .setFhirComplexTypeConfig(
                            FhirComplexTypeConfig.newBuilder()
                                    .putAllAllowedFieldNamesToConfig(
                                            Map.ofEntries(
                                                    Map.entry(
                                                            "id",
                                                            createFhirFieldConfig(
                                                                    false, R4_FHIR_TYPE_STRING)))))
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_STRING =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_STRING)
                    .setKind(KIND_PRIMITIVE_TYPE)
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_CODE =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_CODE)
                    .setKind(KIND_PRIMITIVE_TYPE)
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_DATE_TIME =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_DATE_TIME)
                    .setKind(KIND_PRIMITIVE_TYPE)
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_ID =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_ID)
                    .setKind(KIND_PRIMITIVE_TYPE)
                    .build();

    private static final FhirDataType FHIR_DATA_TYPE_BOOLEAN =
            FhirDataType.newBuilder()
                    .setFhirType(R4_FHIR_TYPE_BOOLEAN)
                    .setKind(KIND_PRIMITIVE_TYPE)
                    .build();

    private static final String DEFAULT_IMMUNIZATION_JSON =
            """
            {
                \"resourceType\": \"Immunization\",
                \"id\": \"immunization-1\",
                \"status\": \"completed\",
                \"identifier\": [{
                    \"use\": \"secondary\",
                    \"value\": \"123\"
                }]
            }
            """;

    private static final List<FhirDataType> DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS =
            List.of(
                    FHIR_DATA_TYPE_IDENTIFIER,
                    FHIR_DATA_TYPE_CODEABLE_CONCEPT,
                    FHIR_DATA_TYPE_CODING,
                    FHIR_DATA_TYPE_ID,
                    FHIR_DATA_TYPE_STRING,
                    FHIR_DATA_TYPE_DATE_TIME,
                    FHIR_DATA_TYPE_CODE,
                    FHIR_DATA_TYPE_BOOLEAN,
                    FHIR_DATA_TYPE_ELEMENT);

    @Test
    public void testValidate_byImmunizationResourceType_succeeds() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson = new JSONObject(DEFAULT_IMMUNIZATION_JSON);

        validator.validate(immunizationJson, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_VERSION_R4);
    }

    @Test
    public void testValidate_missingRequiredPrimitiveField_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .addRequiredFields("status")
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson = new JSONObject(DEFAULT_IMMUNIZATION_JSON);
        immunizationJson.remove("status");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception).hasMessageThat().contains("Missing required field status");
    }

    @Test
    public void testValidate_missingRequiredComplexTypeField_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .addRequiredFields("vaccineCode")
                                        .putAllowedFieldNamesToConfig(
                                                "vaccineCode",
                                                createFhirFieldConfig(
                                                        false, R4_FHIR_TYPE_CODEABLE_CONCEPT))
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .addFhirDataTypeConfigs(FHIR_DATA_TYPE_CODEABLE_CONCEPT)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson = new JSONObject(DEFAULT_IMMUNIZATION_JSON);
        immunizationJson.remove("vaccineCode");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception).hasMessageThat().contains("Missing required field vaccineCode");
    }

    @Test
    public void testValidate_primitiveTypeExtensionForRequiredField_succeeds()
            throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put("_status", new JSONObject("{\"id\": \"123\"}"));

        validator.validate(immunizationJson, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_VERSION_R4);
    }

    @Test
    public void testValidate_oneRequiredMultiTypeFieldPresent_succeeds() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .addMultiTypeFields(
                                                MultiTypeFieldConfig.newBuilder()
                                                        .setName("occurrence[x]")
                                                        .addAllTypedFieldNames(
                                                                List.of(
                                                                        "occurrenceDateTime",
                                                                        "occurrenceString"))
                                                        .setIsRequired(true))
                                        .putAllowedFieldNamesToConfig(
                                                "occurrenceString",
                                                createFhirFieldConfig(false, R4_FHIR_TYPE_STRING))
                                        .putAllowedFieldNamesToConfig(
                                                "occurrenceDateTime",
                                                createFhirFieldConfig(
                                                        false, R4_FHIR_TYPE_DATE_TIME))
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("occurrenceString", "2024");

        validator.validate(immunizationJson, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_VERSION_R4);
    }

    @Test
    public void testValidate_requiredMultiTypeFieldMissing_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .addMultiTypeFields(
                                                MultiTypeFieldConfig.newBuilder()
                                                        .setName("occurrence[x]")
                                                        .addAllTypedFieldNames(
                                                                List.of(
                                                                        "occurrenceDateTime",
                                                                        "occurrenceString"))
                                                        .setIsRequired(true))
                                        .putAllowedFieldNamesToConfig(
                                                "occurrenceString",
                                                createFhirFieldConfig(false, R4_FHIR_TYPE_STRING))
                                        .putAllowedFieldNamesToConfig(
                                                "occurrenceDateTime",
                                                createFhirFieldConfig(
                                                        false, R4_FHIR_TYPE_DATE_TIME))
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson = new JSONObject(DEFAULT_IMMUNIZATION_JSON);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception).hasMessageThat().contains("Missing required field occurrence[x]");
    }

    @Test
    public void testValidate_moreThanOneMultiTypeFieldPresent_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .addMultiTypeFields(
                                                MultiTypeFieldConfig.newBuilder()
                                                        .setName("occurrence[x]")
                                                        .addAllTypedFieldNames(
                                                                List.of(
                                                                        "occurrenceDateTime",
                                                                        "occurrenceString"))
                                                        .setIsRequired(true))
                                        .putAllowedFieldNamesToConfig(
                                                "occurrenceString",
                                                createFhirFieldConfig(false, R4_FHIR_TYPE_STRING))
                                        .putAllowedFieldNamesToConfig(
                                                "occurrenceDateTime",
                                                createFhirFieldConfig(
                                                        false, R4_FHIR_TYPE_DATE_TIME))
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put("occurrenceString", "2024")
                        .put("occurrenceDateTime", "2024-11");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Only one type should be set for field occurrence[x]");
    }

    @Test
    public void testValidate_unknownField_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("unknown_field", "test");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception).hasMessageThat().contains("Found unexpected field unknown_field");
    }

    @Test
    public void testValidate_complexTypeFieldNotJsonObject_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("statusReason", "simple_string");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Invalid resource structure. Expected object in field: statusReason");
    }

    @Test
    public void testValidate_complexTypeFieldIsNull_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("statusReason", JSONObject.NULL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception).hasMessageThat().contains("Found null value in field: statusReason");
    }

    @Test
    public void testValidate_primitiveTypeExtensionNotJsonObject_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("_status", "completed");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Invalid resource structure. Expected object in field: _status");
    }

    @Test
    public void testValidate_arrayField_succeeds() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "identifier",
                                new JSONArray(
                                        """
                                        [{
                                            \"use\": \"secondary\",
                                            \"value\": \"123\"
                                         }, {
                                            \"use\": \"secondary\",
                                            \"value\": \"456\"
                                        }]
                                        """));

        validator.validate(immunizationJson, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_VERSION_R4);
    }

    @Test
    public void testValidate_primitiveTypeExtensionArrayForArrayField_canContainNull()
            throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .putAllowedFieldNamesToConfig(
                                                "primitiveArrayField",
                                                createFhirFieldConfig(true, R4_FHIR_TYPE_STRING))
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "_primitiveArrayField",
                                new JSONArray(
                                        """
                                        [{
                                            \"id\": \"123\"
                                         }, null
                                         ]
                                        """));

        validator.validate(immunizationJson, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_VERSION_R4);
    }

    @EnableFlags(FLAG_PHR_ALLOW_NULLS_IN_PRIMITIVE_VALUE_ARRAYS)
    @Test
    public void testValidate_primitiveTypeValueArray_canContainNull()
            throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .putAllowedFieldNamesToConfig(
                                                "primitiveArrayField",
                                                createFhirFieldConfig(true, R4_FHIR_TYPE_STRING))
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "primitiveArrayField",
                                new JSONArray("[\"value1\", null, \"value3\"]"));

        validator.validate(immunizationJson, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_VERSION_R4);
    }

    @DisableFlags(FLAG_PHR_ALLOW_NULLS_IN_PRIMITIVE_VALUE_ARRAYS)
    @Test
    public void testValidate_allowNullsInPrimitiveValueArraysDisabled_canNotContainNull()
            throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG.toBuilder()
                                        .putAllowedFieldNamesToConfig(
                                                "primitiveArrayField",
                                                createFhirFieldConfig(true, R4_FHIR_TYPE_STRING))
                                        .build())
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "primitiveArrayField",
                                new JSONArray("[\"value1\", null, \"value3\"]"));

        assertThrows(
            IllegalArgumentException.class,
            () ->
                    validator.validate(
                            immunizationJson,
                            FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            FHIR_VERSION_R4));
    }

    @Test
    public void testValidate_arrayFieldIsNotArray_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "identifier",
                                new JSONObject(
                                        """
                                        {
                                            \"use\": \"secondary\",
                                            \"value\": \"123\"
                                         }
                                        """));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Invalid resource structure. Expected array for field: identifier");
    }

    @Test
    public void testValidate_complexTypeArrayItemNotJsonObject_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "identifier",
                                new JSONArray(
                                        """
                                        [{
                                            \"use\": \"secondary\",
                                            \"value\": \"123\"
                                         }, \"simple_string_not_array\"
                                         ]
                                        """));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Invalid resource structure. Expected object in field: identifier");
    }

    @Test
    public void testValidate_primitiveTypeFieldIsJsonObject_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put("primarySource", new JSONObject("{\"id\": \"123\"}"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Invalid resource structure. Found json object but expected primitive type"
                                + " in field: primarySource");
    }

    @Test
    public void testValidate_primitiveTypeFieldIsNull_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("primarySource", JSONObject.NULL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception).hasMessageThat().contains("Found null value in field: primarySource");
    }

    @Test
    public void testValidate_primitiveTypeFieldIsArray_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put("primarySource", new JSONArray("[True]"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Invalid resource structure. Found json array but expected primitive type"
                                + " in field: primarySource");
    }

    @EnableFlags({
        FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION
    })
    @Test
    public void testValidate_primitiveTypeFieldIsWrongType_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("primarySource", "yes");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Invalid resource structure. Found non boolean object in field:"
                                + " primarySource");
    }

    @EnableFlags({
        FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION
    })
    @Test
    public void testValidate_unexpectedFieldInNestedType_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "statusReason",
                                new JSONObject(
                                        """
                                        {
                                            \"coding\": [{
                                                \"display\" : \"display_string\",
                                                \"unexpected\": \"unexpected_field\"
                                            }],
                                            \"text\": \"123\"
                                        }
                                        """));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Found unexpected field statusReason.coding.unexpected");
    }

    @EnableFlags({
        FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION
    })
    @Test
    public void testValidate_moreThanOneOneOfFieldSetInNestedType_throws() throws JSONException {
        FhirComplexTypeConfig annotationComplexTypeConfig =
                FhirComplexTypeConfig.newBuilder()
                        .addMultiTypeFields(
                                MultiTypeFieldConfig.newBuilder()
                                        .setName("author[x]")
                                        .addAllTypedFieldNames(
                                                List.of("authorDateTime", "authorString")))
                        .putAllAllowedFieldNamesToConfig(
                                Map.ofEntries(
                                        Map.entry(
                                                "authorDateTime",
                                                createFhirFieldConfig(
                                                        false, R4_FHIR_TYPE_DATE_TIME)),
                                        Map.entry(
                                                "authorString",
                                                createFhirFieldConfig(false, R4_FHIR_TYPE_STRING))))
                        .build();
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                                FhirComplexTypeConfig.newBuilder()
                                        .putAllAllowedFieldNamesToConfig(
                                                Map.ofEntries(
                                                        Map.entry(
                                                                "id",
                                                                createFhirFieldConfig(
                                                                        false, R4_FHIR_TYPE_ID)),
                                                        Map.entry(
                                                                "resourceType",
                                                                createFhirFieldConfig(
                                                                        false,
                                                                        R4_FHIR_TYPE_STRING)),
                                                        Map.entry(
                                                                "note",
                                                                createFhirFieldConfig(
                                                                        true,
                                                                        R4_FHIR_TYPE_ANNOTATION))))
                                        .build())
                        .addFhirDataTypeConfigs(FHIR_DATA_TYPE_ID)
                        .addFhirDataTypeConfigs(FHIR_DATA_TYPE_STRING)
                        .addFhirDataTypeConfigs(
                                FhirDataType.newBuilder()
                                        .setFhirType(R4_FHIR_TYPE_ANNOTATION)
                                        .setKind(KIND_COMPLEX_TYPE)
                                        .setFhirComplexTypeConfig(annotationComplexTypeConfig))
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject allergyJson =
                new JSONObject(
                        """
                                {
                                    \"resourceType\": \"AllergyIntolerance\",
                                    \"id\": \"allergy-1\",
                                    \"note\": [{
                                        \"authorDateTime\": \"2024-12-01\",
                                        \"authorString\": \"yesterday\"
                                    }]
                                }
                        """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        allergyJson,
                                        FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Only one type should be set for field note.author[x]");
    }

    @EnableFlags({
        FLAG_PHR_FHIR_VALIDATION_DISALLOW_EMPTY_OBJECTS_ARRAYS
    })
    @Test
    public void testValidate_emptyComplexTypeObject_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("statusReason", new JSONObject());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Found empty object in field: statusReason");
    }

    @EnableFlags({
        FLAG_PHR_FHIR_VALIDATION_DISALLOW_EMPTY_OBJECTS_ARRAYS
    })
    @Test
    public void testValidate_emptyArray_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON).put("identifier", new JSONArray());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception).hasMessageThat().contains("Found empty array in field: identifier");
    }

    @EnableFlags({
        FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION,
        FLAG_PHR_FHIR_VALIDATION_DISALLOW_EMPTY_OBJECTS_ARRAYS
    })
    @Test
    public void testValidate_emptyNestedObject_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "identifier",
                                new JSONArray(
                                        """
                                        [
                                            {
                                              \"value\":
                                                \"urn:oid:1.3.6.1.4.1.21367.2005.3.7.1234\",
                                              \"type\": {}
                                            }
                                        ]
                                        """));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Found empty object in field: identifier.type");
    }

    @EnableFlags({
        FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION,
        FLAG_PHR_FHIR_VALIDATION_DISALLOW_EMPTY_OBJECTS_ARRAYS
    })
    @Test
    public void testValidate_emptyNestedArray_throws() throws JSONException {
        FhirResourceSpec fhirSpec =
                FhirResourceSpec.newBuilder()
                        .putResourceTypeToConfig(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                DEFAULT_IMMUNIZATION_COMPLEX_TYPE_CONFIG)
                        .addAllFhirDataTypeConfigs(DEFAULT_IMMUNIZATION_DATA_TYPE_CONFIGS)
                        .build();
        FhirObjectTypeValidator validator =
                new FhirObjectTypeValidator(new FhirSpecProvider(fhirSpec));
        JSONObject immunizationJson =
                new JSONObject(DEFAULT_IMMUNIZATION_JSON)
                        .put(
                                "statusReason",
                                new JSONObject(
                                        """
                                        {
                                          \"coding\": [{}]
                                        }
                                        """));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validator.validate(
                                        immunizationJson,
                                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                        FHIR_VERSION_R4));

        assertThat(exception)
                .hasMessageThat()
                .contains("Found empty object in field: statusReason.coding");
    }

    private static FhirFieldConfig createFhirFieldConfig(boolean isArray, R4FhirType r4Type) {
        return FhirFieldConfig.newBuilder().setIsArray(isArray).setR4Type(r4Type).build();
    }
}
