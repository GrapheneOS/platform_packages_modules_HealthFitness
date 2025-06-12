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

import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_BASE64_BINARY;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_BOOLEAN;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CANONICAL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DECIMAL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_INSTANT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_INTEGER;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_MARKDOWN;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_OID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_POSITIVE_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_STRING;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UNSIGNED_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_URI;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_URL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UUID;

import android.annotation.Nullable;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.proto.R4FhirType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs validation on FHIR primitive values.
 *
 * @hide
 */
public class FhirPrimitiveTypeValidator {
    private static final Map<R4FhirType, Integer> sR4PrimitiveIntegerTypeToMinValueMap =
            new HashMap<>();
    private static final Map<R4FhirType, Pattern> sR4PrimitiveStringTypeToPatternMap =
            new HashMap<>();

    // All regex below are copied from https://hl7.org/fhir/R4/datatypes.html. Please keep the regex
    // patterns below SORTED.
    private static final Pattern BASE64_BINARY_R4_PATTERN =
            Pattern.compile("(\\s*([0-9a-zA-Z\\+\\=]){4}\\s*)+");
    private static final Pattern CANONICAL_R4_PATTERN = Pattern.compile("\\S*");
    private static final Pattern CODE_R4_PATTERN = Pattern.compile("[^\\s]+(\\s[^\\s]+)*");
    private static final Pattern DATE_R4_PATTERN =
            Pattern.compile(
                    "([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)"
                            + "(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1]))?)?");
    // With the R4 regex, if a time is specified, a timezone offset must be populated.
    private static final Pattern DATE_TIME_R4_PATTERN =
            Pattern.compile(
                    "([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)"
                            + "(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])"
                            + "(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)"
                            + "(\\.[0-9]+)?(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?");
    private static final Pattern ID_R4_PATTERN = Pattern.compile("[A-Za-z0-9\\-\\.]{1,64}");
    private static final Pattern INSTANT_R4_PATTERN =
            Pattern.compile(
                    "([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)"
                            + "-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])"
                            + "T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)"
                            + "(\\.[0-9]+)?(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))");
    private static final Pattern MARKDOWN_R4_PATTERN = Pattern.compile("\\s*(\\S|\\s)*");
    private static final Pattern OID_R4_PATTERN =
            Pattern.compile("urn:oid:[0-2](\\.(0|[1-9][0-9]*))+");
    private static final Pattern STRING_R4_PATTERN = Pattern.compile("[ \\r\\n\\t\\S]+");
    private static final Pattern TIME_R4_PATTERN =
            Pattern.compile("([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\\.[0-9]+)?");
    private static final Pattern URI_R4_PATTERN = Pattern.compile("\\S*");
    private static final Pattern URL_R4_PATTERN = Pattern.compile("\\S*");
    private static final Pattern UUID_R4_PATTERN =
            Pattern.compile(
                    "urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    static void validate(Object fieldObject, String fullFieldName, R4FhirType type) {
        if (!Flags.phrFhirPrimitiveTypeValidation()) {
            throw new UnsupportedOperationException(
                    "Validating FHIR primitive types is not supported.");
        }
        if (fieldObject == null) {
            throw new IllegalStateException(
                    "The fieldObject cannot be null in primitive kind field: " + fullFieldName);
        }
        populateR4PrimitiveIntegerTypeToMinValueMap();
        populateR4PrimitiveStringTypeToPatternMap();
        switch (type) {
            case R4_FHIR_TYPE_BOOLEAN:
                validateBooleanType(fieldObject, fullFieldName);
                break;
            case R4_FHIR_TYPE_DECIMAL:
                validateDecimalType(fieldObject, fullFieldName);
                break;
            case R4_FHIR_TYPE_INTEGER:
            case R4_FHIR_TYPE_POSITIVE_INT:
            case R4_FHIR_TYPE_UNSIGNED_INT:
                validateIntegerType(fieldObject, fullFieldName);
                validateIntegerValueRange(
                        (Integer) fieldObject,
                        sR4PrimitiveIntegerTypeToMinValueMap.get(type),
                        fullFieldName);
                break;
            case R4_FHIR_TYPE_BASE64_BINARY:
            case R4_FHIR_TYPE_CANONICAL:
            case R4_FHIR_TYPE_CODE:
            case R4_FHIR_TYPE_DATE:
            case R4_FHIR_TYPE_DATE_TIME:
            case R4_FHIR_TYPE_ID:
            case R4_FHIR_TYPE_INSTANT:
            case R4_FHIR_TYPE_MARKDOWN:
            case R4_FHIR_TYPE_OID:
            case R4_FHIR_TYPE_STRING:
            case R4_FHIR_TYPE_TIME:
            case R4_FHIR_TYPE_URI:
            case R4_FHIR_TYPE_URL:
            case R4_FHIR_TYPE_UUID:
                validateStringType(fieldObject, fullFieldName);
                validateStringValuePattern(
                        fieldObject.toString(),
                        getR4PrimitiveStringTypePattern(type),
                        fullFieldName);
                break;
            case R4_FHIR_TYPE_XHTML:
                // TODO:b/401504262 - Consider additional xhtml type validations to minimise
                //  security risk to downstream apps. The xhtml type does not have a regex
                //  specified, but a constraint that specifies that only basic html tags should be
                //  allowed. See https://build.fhir.org/narrative.html#xhtml.
                validateStringType(fieldObject, fullFieldName);
                break;
            default:
                throw new IllegalStateException(
                        "Type is not supported. Found unexpected type "
                                + type.name()
                                + " in primitive kind field: "
                                + fullFieldName);
        }
    }

    private static void validateBooleanType(Object fieldObject, String fullFieldName) {
        if (!(fieldObject instanceof Boolean)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non boolean object in field: "
                            + fullFieldName);
        }
    }

    private static void validateDecimalType(Object fieldObject, String fullFieldName) {
        // According to the decimal regex and description from
        // https://hl7.org/fhir/R4/datatypes.html#decimal, the decimal data type allows values
        // without a decimal point, which means the valid values can then be parsed as an Integer,
        // Long, or Double. To make sure we don't reject any valid decimal types, we just check it's
        // an instance of "Number" here.
        if (!(fieldObject instanceof Number)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non decimal object in field: "
                            + fullFieldName);
        }
    }

    private static void validateIntegerType(Object fieldObject, String fullFieldName) {
        if (!(fieldObject instanceof Integer)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non integer object in field: "
                            + fullFieldName);
        }
    }

    private static void validateStringType(Object fieldObject, String fullFieldName) {
        if (!(fieldObject instanceof String)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non string object in field: "
                            + fullFieldName);
        }
    }

    private static void validateIntegerValueRange(
            Integer value, @Nullable Integer min, String fullFieldName) {
        if (min != null && value < min) {
            throw new IllegalArgumentException(
                    "Found invalid field value in primitive field: "
                            + fullFieldName
                            + ". The value found is: "
                            + value);
        }
    }

    private static void validateStringValuePattern(
            String value, Pattern pattern, String fullFieldName) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Found invalid field value in primitive field: "
                            + fullFieldName
                            + ". The value found is: "
                            + value);
        }
    }

    private static Pattern getR4PrimitiveStringTypePattern(R4FhirType type) {
        populateR4PrimitiveStringTypeToPatternMap();

        Pattern pattern = sR4PrimitiveStringTypeToPatternMap.get(type);
        if (pattern != null) {
            return pattern;
        }

        throw new IllegalStateException(
                "Could not find the regex pattern for primitive string type " + type.name());
    }

    private static synchronized void populateR4PrimitiveIntegerTypeToMinValueMap() {
        if (!sR4PrimitiveIntegerTypeToMinValueMap.isEmpty()) {
            return;
        }
        sR4PrimitiveIntegerTypeToMinValueMap.put(R4_FHIR_TYPE_POSITIVE_INT, 1);
        sR4PrimitiveIntegerTypeToMinValueMap.put(R4_FHIR_TYPE_UNSIGNED_INT, 0);
    }

    private static synchronized void populateR4PrimitiveStringTypeToPatternMap() {
        if (!sR4PrimitiveStringTypeToPatternMap.isEmpty()) {
            return;
        }
        sR4PrimitiveStringTypeToPatternMap.put(
                R4_FHIR_TYPE_BASE64_BINARY, BASE64_BINARY_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_CANONICAL, CANONICAL_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_CODE, CODE_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_DATE, DATE_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_DATE_TIME, DATE_TIME_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_ID, ID_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_INSTANT, INSTANT_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_MARKDOWN, MARKDOWN_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_OID, OID_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_STRING, STRING_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_TIME, TIME_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_URI, URI_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_URL, URL_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_UUID, UUID_R4_PATTERN);
    }
}
