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

import static android.health.connect.datatypes.FhirResource.FhirResourceType;

import static com.android.server.healthconnect.phr.validations.FhirSpecProvider.FHIR_TYPE_PRIMITIVE_EXTENSION;

import android.health.connect.datatypes.FhirVersion;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.proto.FhirComplexTypeConfig;
import com.android.server.healthconnect.proto.FhirFieldConfig;
import com.android.server.healthconnect.proto.MultiTypeFieldConfig;
import com.android.server.healthconnect.proto.R4FhirType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Performs validation on a FHIR JSON Object, either for a resource type, or FHIR complex type by
 * validating against the FhirComplexTypeConfig of that type.
 *
 * @hide
 */
public class FhirObjectTypeValidator {

    // FHIR resources don't have a max allowed nesting level. They are however only expected to be
    // nested a few levels deep, so we set the max nesting level to 20, which we don't expect to be
    // exceeded. This is to ensure we don't use too much memory during validation of complex types.
    // The size of a resource is limited by the max record limit, but if a field is very deeply
    // nested, this could without a limit lead to twice the size of the JSONObject needing to be
    // stored in memory.
    private static final int MAX_ALLOWED_NESTING_LEVEL = 20;

    private final FhirSpecProvider mFhirSpec;

    FhirObjectTypeValidator(FhirSpecProvider fhirSpec) {
        mFhirSpec = fhirSpec;
    }

    /**
     * Validates the provided {@code fhirJsonObject} against the schema of the provided {@code
     * fhirResourceType}.
     *
     * <p>If {@link Flags#FLAG_PHR_FHIR_COMPLEX_TYPE_VALIDATION} is enabled, nested types are also
     * validated, otherwise just the top level resource fields.
     *
     * @throws IllegalArgumentException if the resource is invalid.
     */
    void validate(
            JSONObject fhirJsonObject,
            @FhirResourceType int fhirResourceType,
            FhirVersion fhirVersion) {
        List<FhirComplexTypeJsonObject> nestedObjects =
                validateTopLevelFieldsAndGetNestedObjects(
                        fhirJsonObject,
                        mFhirSpec.getFhirResourceTypeConfig(fhirResourceType),
                        "",
                        0);

        if (!Flags.phrFhirComplexTypeValidation()) {
            return;
        }

        // We use a queue to keep track of nested objects that still need to be validated instead of
        // validating recursively to prevent the risk of getting stack overflow from too many
        // recursive calls.
        // Reverse the items order to ensure we validate fields in order of occurrence in the json,
        // as we are validating from the end of the queue.
        Collections.reverse(nestedObjects);
        ArrayDeque<FhirComplexTypeJsonObject> nestedObjectsToValidate =
                new ArrayDeque<>(nestedObjects);

        while (!nestedObjectsToValidate.isEmpty()) {
            // Validate the last queue item next, so that we validate each top level field in full
            // depth first and don't need to store all level 1 fields, then all level 2 fields etc.
            FhirComplexTypeJsonObject nestedObject = nestedObjectsToValidate.removeLast();

            if (nestedObject.getNestingLevel() > MAX_ALLOWED_NESTING_LEVEL) {
                throw new IllegalArgumentException(
                        "Found data nested deeper than the max allowed nesting level: "
                                + MAX_ALLOWED_NESTING_LEVEL);
            }

            FhirComplexTypeConfig complexTypeConfig =
                    mFhirSpec.getFhirComplexTypeConfig(nestedObject.getR4FhirType(), fhirVersion);
            if (complexTypeConfig == null) {
                // If the FhirComplexTypeConfig is null, this means that the type should not be
                // validated.
                continue;
            }

            List<FhirComplexTypeJsonObject> childObjects =
                    validateTopLevelFieldsAndGetNestedObjects(
                            nestedObject.getJsonObject(),
                            complexTypeConfig,
                            nestedObject.getObjectPath(),
                            nestedObject.getNestingLevel());
            Collections.reverse(childObjects);
            nestedObjectsToValidate.addAll(childObjects);
        }
    }

    /**
     * Validates the provided {@code fhirJsonObject} against the schema of the provided {@code
     * config}.
     *
     * <p>This method validates the top level fields.
     *
     * @param fhirJsonObject The JSONObject to validate
     * @param config The FHIR config of the object type
     * @param objectPath The full path to the object, used in error messages
     * @param objectNestingLevel The nesting level of the object in the FHIR resource
     * @return Any nested objects that still need validation in a list of {@link
     *     FhirComplexTypeJsonObject}s.
     */
    private List<FhirComplexTypeJsonObject> validateTopLevelFieldsAndGetNestedObjects(
            JSONObject fhirJsonObject,
            FhirComplexTypeConfig config,
            String objectPath,
            int objectNestingLevel) {
        Map<String, FhirFieldConfig> fieldToConfig = config.getAllowedFieldNamesToConfigMap();

        validatePresenceOfRequiredFields(
                fhirJsonObject, config.getRequiredFieldsList(), fieldToConfig, objectPath);

        validateMultiTypeFields(
                fhirJsonObject, config.getMultiTypeFieldsList(), fieldToConfig, objectPath);

        return validateFhirObjectStructureAndGetNestedObjects(
                fhirJsonObject, fieldToConfig, objectPath, objectNestingLevel);
    }

    /**
     * Validates that all {@code requiredFields} are present in the {@code fhirJsonObject}.
     *
     * <p>For primitive type fields, either the value field or the primitive type extension starting
     * with underscore needs to be present.
     *
     * @param fhirJsonObject The JSONObject to validate
     * @param requiredFields The list of required fields
     * @param fieldToConfig A map from field to field config, used to find out if the field is a
     *     primitive type
     * @param objectPath The full path to the object, used in error messages
     */
    private void validatePresenceOfRequiredFields(
            JSONObject fhirJsonObject,
            List<String> requiredFields,
            Map<String, FhirFieldConfig> fieldToConfig,
            String objectPath) {
        for (String requiredField : requiredFields) {
            FhirFieldConfig fieldConfig = fieldToConfig.get(requiredField);
            if (fieldConfig == null) {
                throw new IllegalStateException(
                        "Could not find field config for required field " + requiredField);
            }
            boolean fieldIsPrimitiveType = mFhirSpec.isPrimitiveType(fieldConfig.getR4Type());

            if (!fieldIsPresent(fhirJsonObject, requiredField, fieldIsPrimitiveType)) {
                throw new IllegalArgumentException(
                        "Missing required field "
                                + joinFieldPathAndFieldName(objectPath, requiredField));
            }
        }

        // TODO: b/377717422 - If the field is an array also check that it's not empty.
        // This case does not happen for top level resource field validation, so should be
        // handled as part of implementing complex type validation.
    }

    /**
     * Validates that only one type is set for each multi type field and that required multi type
     * fields are present.
     *
     * @param fhirJsonObject The JSONObject to validate
     * @param multiTypeFieldConfigs The list of multi type field configs
     * @param fieldToConfig A map from field to field config, used to find out if the field is a
     *     primitive type
     * @param objectPath The full path to the object, used in error messages
     */
    private void validateMultiTypeFields(
            JSONObject fhirJsonObject,
            List<MultiTypeFieldConfig> multiTypeFieldConfigs,
            Map<String, FhirFieldConfig> fieldToConfig,
            String objectPath) {
        // Validate multi type fields to make sure required multi type fields are present and only
        // one of each multi type field is set.
        // The field types are validated as part of
        // validateFhirObjectStructureAndGetNestedObjects(), where they are
        // validated the same as other fields.
        for (MultiTypeFieldConfig multiTypeFieldConfig : multiTypeFieldConfigs) {
            int presentFieldCount = 0;
            for (String typedField : multiTypeFieldConfig.getTypedFieldNamesList()) {
                FhirFieldConfig fieldConfig = fieldToConfig.get(typedField);
                if (fieldConfig == null) {
                    throw new IllegalStateException(
                            "Could not find field config for field "
                                    + joinFieldPathAndFieldName(objectPath, typedField));
                }
                boolean fieldIsPrimitiveType = mFhirSpec.isPrimitiveType(fieldConfig.getR4Type());

                if (fieldIsPresent(fhirJsonObject, typedField, fieldIsPrimitiveType)) {
                    presentFieldCount++;
                }
            }

            if (multiTypeFieldConfig.getIsRequired() && presentFieldCount == 0) {
                throw new IllegalArgumentException(
                        "Missing required field "
                                + joinFieldPathAndFieldName(
                                        objectPath, multiTypeFieldConfig.getName()));
            }

            if (presentFieldCount > 1) {
                throw new IllegalArgumentException(
                        "Only one type should be set for field "
                                + joinFieldPathAndFieldName(
                                        objectPath, multiTypeFieldConfig.getName()));
            }
        }
    }

    /**
     * Validates the structure of the provided {@code fhirJsonObject} against the schema.
     *
     * <p>This method validates that each field in the {@code fhirJsonObject} is an allowed field
     * and that the type of this field is as expected.
     *
     * <p>Null values are not allowed, except in the case of primitive type extension or value
     *  arrays.
     *
     * @param fhirJsonObject The JSONObject to validate
     * @param fieldToConfig The map of allowed field to field config for this object
     * @param objectPath The full path to the object, used in error messages
     * @param objectNestingLevel The nesting level of the object in the FHIR resource
     * @return Any nested JSONObjects as a {@link FhirComplexTypeJsonObject} for further validation.
     */
    private List<FhirComplexTypeJsonObject> validateFhirObjectStructureAndGetNestedObjects(
            JSONObject fhirJsonObject,
            Map<String, FhirFieldConfig> fieldToConfig,
            String objectPath,
            int objectNestingLevel) {
        List<FhirComplexTypeJsonObject> nestedObjectsToValidate = new ArrayList<>();

        Iterator<String> fieldIterator = fhirJsonObject.keys();

        // We loop over all fields that are present in the object to identify any unexpected fields
        // and because this is more efficient that looping over all allowed fields and checking if
        // they are present.
        while (fieldIterator.hasNext()) {
            String fieldName = fieldIterator.next();
            String fullFieldName = joinFieldPathAndFieldName(objectPath, fieldName);
            boolean fieldStartsWithUnderscore = fieldName.startsWith("_");

            // Strip leading underscore in case the field is a primitive type extension, which will
            // have a leading underscore, e.g. _status, see
            // https://build.fhir.org/json.html#primitive.
            String fieldWithoutLeadingUnderscore =
                    fieldStartsWithUnderscore ? fieldName.substring(1) : fieldName;
            FhirFieldConfig fieldConfig = fieldToConfig.get(fieldWithoutLeadingUnderscore);
            if (fieldConfig == null) {
                // TODO: b/374953896 - Improve error message to include type and id.
                throw new IllegalArgumentException("Found unexpected field " + fullFieldName);
            }
            R4FhirType fieldType = fieldConfig.getR4Type();
            boolean fieldIsPrimitiveType = mFhirSpec.isPrimitiveType(fieldType);
            if (fieldStartsWithUnderscore && !fieldIsPrimitiveType) {
                throw new IllegalArgumentException("Found unexpected field " + fullFieldName);
            }

            Object fieldObject;
            try {
                fieldObject = fhirJsonObject.get(fieldName);
            } catch (JSONException exception) {
                throw new IllegalStateException(
                        "Expected field to be present in json object: " + fullFieldName);
            }

            List<Object> objectsToValidate =
                    fieldConfig.getIsArray()
                            ? validateIsNonEmptyArrayAndGetContents(fieldObject, fullFieldName)
                            : List.of(fieldObject);
            boolean fieldIsPrimitiveTypeExtension =
                    fieldIsPrimitiveType && fieldStartsWithUnderscore;
            // Primitive type extension arrays and value arrays are allowed to have
            // NULL values. See https://build.fhir.org/json.html#primitive.
            boolean jsonNullAllowed;
            if (Flags.phrAllowNullsInPrimitiveValueArrays()) {
                jsonNullAllowed = fieldIsPrimitiveType && fieldConfig.getIsArray();
            } else {
                jsonNullAllowed = fieldIsPrimitiveTypeExtension && fieldConfig.getIsArray();
            }

            for (Object object : objectsToValidate) {
                if (object.equals(JSONObject.NULL) && jsonNullAllowed) {
                    continue;
                }
                if (fieldIsPrimitiveType && !fieldIsPrimitiveTypeExtension) {
                    // If the field is a primitive type extension (starts with "_"), then it
                    // will be an object of type "Element" with fields "id" and/or "extension"
                    // fields.
                    validatePrimitiveTypeField(object, fullFieldName, fieldType);
                } else {
                    JSONObject jsonObject =
                            validateObjectIsNonEmptyJSONObject(object, fullFieldName);
                    nestedObjectsToValidate.add(
                            new FhirComplexTypeJsonObject(
                                    fullFieldName,
                                    objectNestingLevel + 1,
                                    fieldIsPrimitiveTypeExtension
                                            ? FHIR_TYPE_PRIMITIVE_EXTENSION
                                            : fieldType,
                                    jsonObject));
                }
            }
        }

        return nestedObjectsToValidate;
    }

    private static List<Object> validateIsNonEmptyArrayAndGetContents(
            Object fieldObject, String fullFieldName) {
        if (!(fieldObject instanceof JSONArray)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Expected array for field: " + fullFieldName);
        }
        JSONArray jsonArray = (JSONArray) fieldObject;
        if (Flags.phrFhirValidationDisallowEmptyObjectsArrays() && jsonArray.length() == 0) {
            throw new IllegalArgumentException("Found empty array in field: " + fullFieldName);
        }
        List<Object> arrayObjects = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object objectInArray;
            try {
                objectInArray = jsonArray.get(i);
            } catch (JSONException exception) {
                throw new IllegalStateException(
                        "Expected item to be present in json array at index "
                                + i
                                + " for field "
                                + fullFieldName);
            }

            arrayObjects.add(objectInArray);
        }

        return arrayObjects;
    }

    private static JSONObject validateObjectIsNonEmptyJSONObject(
            Object fieldObject, String fullFieldName) {
        if (fieldObject.equals(JSONObject.NULL)) {
            throw new IllegalArgumentException("Found null value in field: " + fullFieldName);
        }
        if (!(fieldObject instanceof JSONObject)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Expected object in field: " + fullFieldName);
        }
        JSONObject jsonObject = (JSONObject) fieldObject;
        if (Flags.phrFhirValidationDisallowEmptyObjectsArrays() && jsonObject.length() == 0) {
            throw new IllegalArgumentException("Found empty object in field: " + fullFieldName);
        }

        return jsonObject;
    }

    private static void validatePrimitiveTypeField(
            Object fieldObject, String fullFieldName, R4FhirType type) {
        if (fieldObject.equals(JSONObject.NULL)) {
            throw new IllegalArgumentException("Found null value in field: " + fullFieldName);
        }
        if (fieldObject instanceof JSONObject) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found json object but expected"
                            + " primitive type in field: "
                            + fullFieldName);
        }
        if (fieldObject instanceof JSONArray) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found json array but expected"
                            + " primitive type in field: "
                            + fullFieldName);
        }

        if (Flags.phrFhirPrimitiveTypeValidation()) {
            FhirPrimitiveTypeValidator.validate(fieldObject, fullFieldName, type);
        }
    }

    private static boolean fieldIsPresent(
            JSONObject fhirJsonObject, String fieldName, boolean isPrimitiveType) {
        boolean fieldIsPresent = fhirJsonObject.has(fieldName);

        // For primitive type fields, a primitive type extension with leading underscore may be
        // present instead. See https://build.fhir.org/extensibility.html#primitives, which
        // states that "extensions may appear in place of the value of the primitive datatype".
        if (isPrimitiveType) {
            fieldIsPresent = fieldIsPresent || fhirJsonObject.has("_" + fieldName);
        }

        return fieldIsPresent;
    }

    private static String joinFieldPathAndFieldName(String fieldPath, String fieldName) {
        if (fieldPath.isEmpty()) {
            return fieldName;
        }
        return String.join(".", List.of(fieldPath, fieldName));
    }

    private static final class FhirComplexTypeJsonObject {
        private final String mObjectPath;
        private final int mNestingLevel;
        private final R4FhirType mR4FhirType;
        private final JSONObject mJsonObject;

        FhirComplexTypeJsonObject(
                String objectPath, int nestingLevel, R4FhirType r4FhirType, JSONObject jsonObject) {
            mObjectPath = objectPath;
            mNestingLevel = nestingLevel;
            mR4FhirType = r4FhirType;
            mJsonObject = jsonObject;
        }

        String getObjectPath() {
            return mObjectPath;
        }

        int getNestingLevel() {
            return mNestingLevel;
        }

        R4FhirType getR4FhirType() {
            return mR4FhirType;
        }

        JSONObject getJsonObject() {
            return mJsonObject;
        }
    }
}
