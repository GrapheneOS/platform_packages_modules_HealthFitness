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

import static com.android.healthfitness.flags.Flags.FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_XHTML_VALIDATION;
import static com.android.server.healthconnect.phr.validations.FhirPrimitiveTypeValidator.validate;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_BOOLEAN;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CANONICAL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODEABLE_CONCEPT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DECIMAL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_INSTANT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_INTEGER;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_POSITIVE_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_STRING;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UNSIGNED_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_XHTML;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.healthconnect.testing.shared.phr.EncountersBuilder;
import android.healthconnect.testing.shared.phr.ImmunizationBuilder;
import android.healthconnect.testing.shared.phr.MedicationsBuilder;
import android.healthconnect.testing.shared.phr.ObservationBuilder;
import android.healthconnect.testing.shared.phr.PractitionerBuilder;
import android.healthconnect.testing.shared.phr.ProcedureBuilder;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FhirPrimitiveTypeValidatorTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String BOOLEAN_TYPE_EXCEPTION_MESSAGE =
            "Invalid resource structure. Found non boolean object in field: ";
    private static final String DECIMAL_TYPE_EXCEPTION_MESSAGE =
            "Invalid resource structure. Found non decimal object in field: ";
    private static final String INTEGER_TYPE_EXCEPTION_MESSAGE =
            "Invalid resource structure. Found non integer object in field: ";
    private static final String STRING_TYPE_EXCEPTION_MESSAGE =
            "Invalid resource structure. Found non string object in field: ";

    @DisableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_primitiveValidationFlagDisabled_throws() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> validate(new Object(), "field", R4_FHIR_TYPE_BOOLEAN));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_notPrimitive_complexType_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> validate(null, "field", R4_FHIR_TYPE_CODEABLE_CONCEPT));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_objectIsNull_throws() {
        assertThrows(
                IllegalStateException.class, () -> validate(null, "field", R4_FHIR_TYPE_BOOLEAN));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4BooleanValid_true_succeeds() throws JSONException {
        String booleanFieldName = "primarySource";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(booleanFieldName, true).toJson());

        validate(jsonObject.get(booleanFieldName), booleanFieldName, R4_FHIR_TYPE_BOOLEAN);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4BooleanValid_false_succeeds() throws JSONException {
        String booleanFieldName = "primarySource";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(booleanFieldName, false).toJson());

        validate(jsonObject.get(booleanFieldName), booleanFieldName, R4_FHIR_TYPE_BOOLEAN);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4BooleanInvalid_asString_throws() throws JSONException {
        String booleanFieldName = "primarySource";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(booleanFieldName, "true").toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(booleanFieldName),
                                        booleanFieldName,
                                        R4_FHIR_TYPE_BOOLEAN));

        assertThat(exception)
                .hasMessageThat()
                .contains(BOOLEAN_TYPE_EXCEPTION_MESSAGE + booleanFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4BooleanInvalid_objectIsJsonNull_throws() throws JSONException {
        String booleanFieldName = "primarySource";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(booleanFieldName, JSONObject.NULL).toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(booleanFieldName),
                                        booleanFieldName,
                                        R4_FHIR_TYPE_BOOLEAN));

        assertThat(exception)
                .hasMessageThat()
                .contains(BOOLEAN_TYPE_EXCEPTION_MESSAGE + booleanFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CanonicalValid_succeeds() throws JSONException {
        String canonicalFieldName = "instantiatesCanonical";
        JSONObject jsonObject =
                new JSONObject(
                        new ProcedureBuilder()
                                .set(canonicalFieldName, "PlanDefinition/KDN5")
                                .toJson());

        validate(jsonObject.get(canonicalFieldName), canonicalFieldName, R4_FHIR_TYPE_CANONICAL);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CanonicalInvalid_containWhiteSpace_throws() throws JSONException {
        String canonicalFieldName = "instantiatesCanonical";
        JSONObject jsonObject =
                new JSONObject(
                        new ProcedureBuilder()
                                .set(canonicalFieldName, "PlanDefinition KDN5")
                                .toJson());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(canonicalFieldName),
                                canonicalFieldName,
                                R4_FHIR_TYPE_CANONICAL));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CanonicalInvalid_objectIsJsonNull_throws() throws JSONException {
        String canonicalFieldName = "instantiatesCanonical";
        JSONObject jsonObject =
                new JSONObject(
                        new ProcedureBuilder().set(canonicalFieldName, JSONObject.NULL).toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(canonicalFieldName),
                                        canonicalFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + canonicalFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CodeValid_singleWord_succeeds() throws JSONException {
        String codeFieldName = "status";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(codeFieldName, "completed").toJson());

        validate(jsonObject.get(codeFieldName), codeFieldName, R4_FHIR_TYPE_CODE);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CodeValid_multiWords_succeeds() throws JSONException {
        String codeFieldName = "status";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(codeFieldName, "all done").toJson());

        validate(jsonObject.get(codeFieldName), codeFieldName, R4_FHIR_TYPE_CODE);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CodeInvalid_emptyString_throws() throws JSONException {
        String codeFieldName = "status";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(codeFieldName, "").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(codeFieldName), codeFieldName, R4_FHIR_TYPE_CODE));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CodeInvalid_leadingWhiteSpaces_throws() throws JSONException {
        String codeFieldName = "status";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(codeFieldName, " completed").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(codeFieldName), codeFieldName, R4_FHIR_TYPE_CODE));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CodeInvalid_trailingWhiteSpaces_throws() throws JSONException {
        String codeFieldName = "status";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(codeFieldName, "completed ").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(codeFieldName), codeFieldName, R4_FHIR_TYPE_CODE));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CodeInvalid_moreThanSingleWhiteSpaces_throws() throws JSONException {
        String codeFieldName = "status";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(codeFieldName, "all  done").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(codeFieldName), codeFieldName, R4_FHIR_TYPE_CODE));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4CodeInvalid_objectIsJsonNull_throws() throws JSONException {
        String codeFieldName = "status";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(codeFieldName, JSONObject.NULL).toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(codeFieldName),
                                        codeFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + codeFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateValid_year_succeeds() throws JSONException {
        String dateFieldName = "birthDate";
        JSONObject jsonObject =
                new JSONObject(new PractitionerBuilder().set(dateFieldName, "2018").toJson());

        validate(jsonObject.get(dateFieldName), dateFieldName, R4_FHIR_TYPE_DATE);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateValid_yearMonth_succeeds() throws JSONException {
        String dateFieldName = "birthDate";
        JSONObject jsonObject =
                new JSONObject(new PractitionerBuilder().set(dateFieldName, "1973-06").toJson());

        validate(jsonObject.get(dateFieldName), dateFieldName, R4_FHIR_TYPE_DATE);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateValid_yearMonthDay_succeeds() throws JSONException {
        String dateFieldName = "birthDate";
        JSONObject jsonObject =
                new JSONObject(new PractitionerBuilder().set(dateFieldName, "1905-08-23").toJson());

        validate(jsonObject.get(dateFieldName), dateFieldName, R4_FHIR_TYPE_DATE);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateInvalid_month20_throws() throws JSONException {
        String dateFieldName = "birthDate";
        JSONObject jsonObject =
                new JSONObject(new PractitionerBuilder().set(dateFieldName, "2018-20-01").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(dateFieldName), dateFieldName, R4_FHIR_TYPE_DATE));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateInvalid_objectIsJsonNull_throws() throws JSONException {
        String dateFieldName = "birthDate";
        JSONObject jsonObject =
                new JSONObject(
                        new PractitionerBuilder().set(dateFieldName, JSONObject.NULL).toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(dateFieldName),
                                        dateFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + dateFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeValid_year_succeeds() throws JSONException {
        String dateFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(dateFieldName, "2018").toJson());

        validate(jsonObject.get(dateFieldName), dateFieldName, R4_FHIR_TYPE_DATE_TIME);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeValid_yearMonth_succeeds() throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(dateTimeFieldName, "1973-06").toJson());

        validate(jsonObject.get(dateTimeFieldName), dateTimeFieldName, R4_FHIR_TYPE_DATE_TIME);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeValid_yearMonthDay_succeeds() throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(dateTimeFieldName, "1905-08-23").toJson());

        validate(jsonObject.get(dateTimeFieldName), dateTimeFieldName, R4_FHIR_TYPE_DATE_TIME);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeValid_yearMonthDayTime_withUtcTimezone_succeeds()
            throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder()
                                .set(dateTimeFieldName, "2015-02-07T13:28:17Z")
                                .toJson());

        validate(jsonObject.get(dateTimeFieldName), dateTimeFieldName, R4_FHIR_TYPE_DATE_TIME);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeValid_yearMonthDayTime_withNonUtcTimezone_succeeds()
            throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder()
                                .set(dateTimeFieldName, "2015-02-07T13:28:17-05:00")
                                .toJson());

        validate(jsonObject.get(dateTimeFieldName), dateTimeFieldName, R4_FHIR_TYPE_DATE_TIME);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeValid_yearMonthDayTime_withMilliSeconds_succeeds()
            throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder()
                                .set(dateTimeFieldName, "2015-02-07T13:28:17.000Z")
                                .toJson());

        validate(jsonObject.get(dateTimeFieldName), dateTimeFieldName, R4_FHIR_TYPE_DATE_TIME);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeValid_yearMonthDayTime_withoutTimezone_throws()
            throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder()
                                .set(dateTimeFieldName, "2015-02-07T13:28:17")
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(dateTimeFieldName),
                                dateTimeFieldName,
                                R4_FHIR_TYPE_DATE_TIME));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeInvalid_noSeconds_throws() throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder()
                                .set(dateTimeFieldName, "2015-02-07T13:28")
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(dateTimeFieldName),
                                dateTimeFieldName,
                                R4_FHIR_TYPE_DATE_TIME));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeInvalid_time24_throws() throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder()
                                .set(dateTimeFieldName, "2015-02-07T24:00:00")
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(dateTimeFieldName),
                                dateTimeFieldName,
                                R4_FHIR_TYPE_DATE_TIME));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DateTimeInvalid_objectIsJsonNull_throws() throws JSONException {
        String dateTimeFieldName = "occurrenceDateTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(dateTimeFieldName, JSONObject.NULL).toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(dateTimeFieldName),
                                        dateTimeFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + dateTimeFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DecimalValid_decimal_succeeds() throws JSONException {
        String decimalFieldName = "longitude";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.location().toJson())
                        .getJSONObject("position")
                        .put(decimalFieldName, -83.6945691);

        validate(jsonObject.get(decimalFieldName), decimalFieldName, R4_FHIR_TYPE_DECIMAL);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DecimalValid_zero_succeeds() throws JSONException {
        String decimalFieldName = "longitude";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.location().toJson())
                        .getJSONObject("position")
                        .put(decimalFieldName, 0);

        validate(jsonObject.get(decimalFieldName), decimalFieldName, R4_FHIR_TYPE_DECIMAL);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DecimalValid_integer_succeeds() throws JSONException {
        String decimalFieldName = "longitude";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.location().toJson())
                        .getJSONObject("position")
                        .put(decimalFieldName, -83);

        validate(jsonObject.get(decimalFieldName), decimalFieldName, R4_FHIR_TYPE_DECIMAL);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DecimalValid_long_succeeds() throws JSONException {
        String decimalFieldName = "longitude";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.location().toJson())
                        .getJSONObject("position")
                        .put(decimalFieldName, 2147483648L);

        validate(jsonObject.get(decimalFieldName), decimalFieldName, R4_FHIR_TYPE_DECIMAL);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DecimalInvalid_asString_throws() throws JSONException {
        String decimalFieldName = "longitude";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.location().toJson())
                        .getJSONObject("position")
                        .put(decimalFieldName, "-83.6945691");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(decimalFieldName),
                                        decimalFieldName,
                                        R4_FHIR_TYPE_DECIMAL));

        assertThat(exception)
                .hasMessageThat()
                .contains(DECIMAL_TYPE_EXCEPTION_MESSAGE + decimalFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4DecimalInvalid_objectIsJsonNull_throws() throws JSONException {
        String decimalFieldName = "longitude";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.location().toJson())
                        .getJSONObject("position")
                        .put(decimalFieldName, JSONObject.NULL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(decimalFieldName),
                                        decimalFieldName,
                                        R4_FHIR_TYPE_DECIMAL));

        assertThat(exception)
                .hasMessageThat()
                .contains(DECIMAL_TYPE_EXCEPTION_MESSAGE + decimalFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IdValid_succeeds() throws JSONException {
        String idFieldName = "id";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(idFieldName, "immunization-1").toJson());

        validate(jsonObject.get(idFieldName), idFieldName, R4_FHIR_TYPE_ID);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IdInvalid_emptyString_throws() throws JSONException {
        String idFieldName = "id";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(idFieldName, "").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(idFieldName), idFieldName, R4_FHIR_TYPE_ID));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IdInvalid_specialCharacter_throws() throws JSONException {
        String idFieldName = "id";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(idFieldName, "immunization/1").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(idFieldName), idFieldName, R4_FHIR_TYPE_ID));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IdInvalid_objectIsJsonNull_throws() throws JSONException {
        String idFieldName = "id";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(idFieldName, JSONObject.NULL).toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(idFieldName),
                                        idFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + idFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4InstantValid_withUtcTimezone_succeeds() throws JSONException {
        String instantFieldName = "effectiveInstant";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllEffectiveMultiTypeFields()
                                .set(instantFieldName, "2015-02-07T13:28:17.239Z")
                                .toJson());

        validate(jsonObject.get(instantFieldName), instantFieldName, R4_FHIR_TYPE_INSTANT);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4InstantValid_withNonUtcTimezone_succeeds() throws JSONException {
        String instantFieldName = "effectiveInstant";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllEffectiveMultiTypeFields()
                                .set(instantFieldName, "2015-02-07T13:28:17.239+02:00")
                                .toJson());

        validate(jsonObject.get(instantFieldName), instantFieldName, R4_FHIR_TYPE_INSTANT);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4InstantInvalid_dateOnly_succeeds() throws JSONException {
        String instantFieldName = "effectiveInstant";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllEffectiveMultiTypeFields()
                                .set(instantFieldName, "2015-02-07")
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(instantFieldName),
                                instantFieldName,
                                R4_FHIR_TYPE_INSTANT));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4InstantInvalid_objectIsJsonNull_throws() throws JSONException {
        String instantFieldName = "effectiveInstant";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllEffectiveMultiTypeFields()
                                .set(instantFieldName, JSONObject.NULL)
                                .toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(instantFieldName),
                                        instantFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + instantFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IntegerValid_succeeds() throws JSONException {
        String integerFieldName = "valueInteger";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(integerFieldName, 0)
                                .toJson());

        validate(jsonObject.get(integerFieldName), integerFieldName, R4_FHIR_TYPE_INTEGER);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IntegerInvalid_greaterThanMax_throws() throws JSONException {
        String integerFieldName = "valueInteger";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(integerFieldName, 2147483648L)
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(integerFieldName),
                                integerFieldName,
                                R4_FHIR_TYPE_INTEGER));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IntegerInvalid_lessThanMin_throws() throws JSONException {
        String integerFieldName = "valueInteger";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(integerFieldName, -2147483649L)
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(integerFieldName),
                                integerFieldName,
                                R4_FHIR_TYPE_INTEGER));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IntegerInvalid_asString_throws() throws JSONException {
        String integerFieldName = "valueInteger";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(integerFieldName, "0")
                                .toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(integerFieldName),
                                        integerFieldName,
                                        R4_FHIR_TYPE_INTEGER));

        assertThat(exception)
                .hasMessageThat()
                .contains(INTEGER_TYPE_EXCEPTION_MESSAGE + integerFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4IntegerInvalid_objectIsJsonNull_throws() throws JSONException {
        String integerFieldName = "valueInteger";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(integerFieldName, JSONObject.NULL)
                                .toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(integerFieldName),
                                        integerFieldName,
                                        R4_FHIR_TYPE_INTEGER));

        assertThat(exception)
                .hasMessageThat()
                .contains(INTEGER_TYPE_EXCEPTION_MESSAGE + integerFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4StringValid_succeeds() throws JSONException {
        String stringFieldName = "lotNumber";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(stringFieldName, "1").toJson());

        validate(jsonObject.get(stringFieldName), stringFieldName, R4_FHIR_TYPE_STRING);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4StringInvalid_emptyString_throws() throws JSONException {
        String stringFieldName = "lotNumber";
        JSONObject jsonObject =
                new JSONObject(new ImmunizationBuilder().set(stringFieldName, "").toJson());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(stringFieldName),
                                stringFieldName,
                                R4_FHIR_TYPE_STRING));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4StringInvalid_objectIsJsonNull_throws() throws JSONException {
        String stringFieldName = "lotNumber";
        JSONObject jsonObject =
                new JSONObject(
                        new ImmunizationBuilder().set(stringFieldName, JSONObject.NULL).toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(stringFieldName),
                                        stringFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + stringFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4PositiveIntValid_succeeds() throws JSONException {
        String positiveIntFieldName = "rank";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.encounter().toJson())
                        .getJSONArray("diagnosis")
                        .getJSONObject(0)
                        .put(positiveIntFieldName, 1);

        validate(
                jsonObject.get(positiveIntFieldName),
                positiveIntFieldName,
                R4_FHIR_TYPE_POSITIVE_INT);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4PositiveIntInvalid_greaterThanMax_throws() throws JSONException {
        String positiveIntFieldName = "rank";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.encounter().toJson())
                        .getJSONArray("diagnosis")
                        .getJSONObject(0)
                        .put(positiveIntFieldName, 2147483648L);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(positiveIntFieldName),
                                positiveIntFieldName,
                                R4_FHIR_TYPE_POSITIVE_INT));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4PositiveIntInvalid_lessThan1_throws() throws JSONException {
        String positiveIntFieldName = "rank";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.encounter().toJson())
                        .getJSONArray("diagnosis")
                        .getJSONObject(0)
                        .put(positiveIntFieldName, 0);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(positiveIntFieldName),
                                positiveIntFieldName,
                                R4_FHIR_TYPE_POSITIVE_INT));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4PositiveIntInvalid_asString_throws() throws JSONException {
        String positiveIntFieldName = "rank";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.encounter().toJson())
                        .getJSONArray("diagnosis")
                        .getJSONObject(0)
                        .put(positiveIntFieldName, "1");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(positiveIntFieldName),
                                        positiveIntFieldName,
                                        R4_FHIR_TYPE_POSITIVE_INT));

        assertThat(exception)
                .hasMessageThat()
                .contains(INTEGER_TYPE_EXCEPTION_MESSAGE + positiveIntFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4PositiveIntInvalid_objectIsJsonNull_throws() throws JSONException {
        String positiveIntFieldName = "rank";
        JSONObject jsonObject =
                new JSONObject(EncountersBuilder.encounter().toJson())
                        .getJSONArray("diagnosis")
                        .getJSONObject(0)
                        .put(positiveIntFieldName, JSONObject.NULL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(positiveIntFieldName),
                                        positiveIntFieldName,
                                        R4_FHIR_TYPE_POSITIVE_INT));

        assertThat(exception)
                .hasMessageThat()
                .contains(INTEGER_TYPE_EXCEPTION_MESSAGE + positiveIntFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4TimeValid_succeeds() throws JSONException {
        String timeFieldName = "valueTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(timeFieldName, "09:00:00")
                                .toJson());

        validate(jsonObject.get(timeFieldName), timeFieldName, R4_FHIR_TYPE_TIME);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4TimeInvalid_noSeconds_throws() throws JSONException {
        String timeFieldName = "valueTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(timeFieldName, "09:00")
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(timeFieldName), timeFieldName, R4_FHIR_TYPE_TIME));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4TimeInvalid_time24_throws() throws JSONException {
        String timeFieldName = "valueTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(timeFieldName, "24:00:00")
                                .toJson());

        assertThrows(
                IllegalArgumentException.class,
                () -> validate(jsonObject.get(timeFieldName), timeFieldName, R4_FHIR_TYPE_TIME));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4TimeInvalid_objectIsJsonNull_throws() throws JSONException {
        String timeFieldName = "valueTime";
        JSONObject jsonObject =
                new JSONObject(
                        new ObservationBuilder()
                                .removeAllValueMultiTypeFields()
                                .set(timeFieldName, JSONObject.NULL)
                                .toJson());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(timeFieldName),
                                        timeFieldName,
                                        R4_FHIR_TYPE_STRING));

        assertThat(exception)
                .hasMessageThat()
                .contains(STRING_TYPE_EXCEPTION_MESSAGE + timeFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4UnsignedIntValid_succeeds() throws JSONException {
        String unsignedIntFieldName = "numberOfRepeatsAllowed";
        JSONObject jsonObject =
                new JSONObject(MedicationsBuilder.request().toJson())
                        .getJSONObject("dispenseRequest")
                        .put(unsignedIntFieldName, 0);

        validate(
                jsonObject.get(unsignedIntFieldName),
                unsignedIntFieldName,
                R4_FHIR_TYPE_UNSIGNED_INT);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4UnsignedIntInvalid_greaterThanMax_throws() throws JSONException {
        String unsignedIntFieldName = "numberOfRepeatsAllowed";
        JSONObject jsonObject =
                new JSONObject(MedicationsBuilder.request().toJson())
                        .getJSONObject("dispenseRequest")
                        .put(unsignedIntFieldName, 2147483648L);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(unsignedIntFieldName),
                                unsignedIntFieldName,
                                R4_FHIR_TYPE_UNSIGNED_INT));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4UnsignedIntInvalid_lessThan0_throws() throws JSONException {
        String unsignedIntFieldName = "numberOfRepeatsAllowed";
        JSONObject jsonObject =
                new JSONObject(MedicationsBuilder.request().toJson())
                        .getJSONObject("dispenseRequest")
                        .put(unsignedIntFieldName, -1);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        validate(
                                jsonObject.get(unsignedIntFieldName),
                                unsignedIntFieldName,
                                R4_FHIR_TYPE_UNSIGNED_INT));
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4UnsignedIntInvalid_asString_throws() throws JSONException {
        String unsignedIntFieldName = "numberOfRepeatsAllowed";
        JSONObject jsonObject =
                new JSONObject(MedicationsBuilder.request().toJson())
                        .getJSONObject("dispenseRequest")
                        .put(unsignedIntFieldName, "1");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(unsignedIntFieldName),
                                        unsignedIntFieldName,
                                        R4_FHIR_TYPE_UNSIGNED_INT));

        assertThat(exception)
                .hasMessageThat()
                .contains(INTEGER_TYPE_EXCEPTION_MESSAGE + unsignedIntFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4UnsignedIntInvalid_objectIsJsonNull_throws() throws JSONException {
        String unsignedIntFieldName = "numberOfRepeatsAllowed";
        JSONObject jsonObject =
                new JSONObject(MedicationsBuilder.request().toJson())
                        .getJSONObject("dispenseRequest")
                        .put(unsignedIntFieldName, JSONObject.NULL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObject.get(unsignedIntFieldName),
                                        unsignedIntFieldName,
                                        R4_FHIR_TYPE_UNSIGNED_INT));

        assertThat(exception)
                .hasMessageThat()
                .contains(INTEGER_TYPE_EXCEPTION_MESSAGE + unsignedIntFieldName);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4XhtmlValid_succeeds() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject(new ImmunizationBuilder().setTextNarrative().toJson())
                        .getJSONObject("text");

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4Xhtml_emptyString_succeeds() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject(
                        """
                        {
                            \"status\": \"generated\",
                            \"div\": \"\"
                        }
                        """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_objectIsInt_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject(
                        """
                        {
                            \"status\": \"generated\",
                            \"div\": 123
                        }
                        """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception).hasMessageThat().contains(STRING_TYPE_EXCEPTION_MESSAGE + "text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_containsDoctypeDeclaration_throws()
            throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"
                                    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative</p>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains("Found invalid xhtml containing DOCTYPE declaration in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_containsProcessingInstruction_throws()
            throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?>
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative</p>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml containing processing instruction in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlWithCDATA_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>
                                    <![CDATA[In this section, we can use <, >, and & freely]]>
                                    </p>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains("Found invalid xhtml containing CDATA section in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION})
    @DisableFlags({FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalidMissingClosingTag_flagDisabled_succeeds()
            throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        // Missing </p> closing tab
                        .put(
                                "div",
                                "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>Narrative</div>");

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_missingClosingTag_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        // Missing </p> closing tab
                        .put(
                                "div",
                                "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>Narrative</div>");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception).hasMessageThat().contains("Failed to parse xhtml in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_mismatchingClosingTag_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        // Mismatching </p> closing tag
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative</P>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception).hasMessageThat().contains("Failed to parse xhtml in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_unquotedAttributeValue_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        // Unquoted attribute value for "class=test"
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p class=test>Narrative</p>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception).hasMessageThat().contains("Failed to parse xhtml in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_unterminatedCharacterReference_throws()
            throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative &</p>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception).hasMessageThat().contains("Failed to parse xhtml in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4Xhtml_unknownCharacterReference_succeeds() throws JSONException {
        // TODO (b/402780942) Right now unknown character references (such as html entity `&nbsp;`)
        //  don't throw an exception. Consider validating that they can be resolved.
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative &nbsp;</p>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlInvalid_invalidCharacter_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        // Invalid <> around the class attribute value
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p class:"<test>">Narrative</p>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception).hasMessageThat().contains("Failed to parse xhtml in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlCharacterReference_succeeds() throws JSONException {
        // &#160; character reference resolves to a space, so should be valid
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative &#160; Narrative</p>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlNamespacePrefix_resolvesAndSucceeds() throws JSONException {
        // myprefix:p namespace prefix should resolve to the declared prefix
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns:myprefix=\"http://www.w3.org/1999/xhtml\">
                                    <myprefix:p myprefix:class="MyClass">Narrative</myprefix:p>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlWithComment_succeeds() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <!-- This is a comment -->
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative</p></div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlWithSelfClosingTag_succeeds() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <br/>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XhtmlWitOpenTagLeftAtEnd_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p>Narrative</p>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains("Missing closing tag for xhtml element in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlIllegalAttributeFormat_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <p :illegalAttr=\"test\">Narrative</p>
                                    </div>

                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .isEqualTo("Failed to parse xhtml in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlWithDisallowedTag_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <body>Body</body>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml containing disallowed element body in field:"
                                + " text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlWithIncorrectCase_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                    <P>paragraph</P>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains("Found invalid xhtml containing disallowed element P in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlWithDisallowedAttribute_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a onclick="showAlert()"></a>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml containing disallowed attribute a.onclick in field:"
                                + " text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlAllowedAttributeOnDifferentElement_throws()
            throws JSONException {
        // The border attribute is allowed on the table element, but not on the col element
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <table border="1">
                                            <col border="1"></col>
                                        </table>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml containing disallowed attribute col.border in field:"
                                + " text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlAttributeAllowedOnAllElements_succeeds() throws JSONException {
        // The class attribute is allowed on all elements
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <table class=\"MyClass\"></table>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLink_allowsRelativeUri() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a href=\"#observation_1\"></a>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLink_allowsHttpScheme() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a href=\"http://example-uri\"></a>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLink_allowsHttpsScheme() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a href=\"https://example-uri.com\"></a>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLink_allowsMailToScheme() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a href=\"mailto://example@gmail.com\"></a>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLink_allowsTelScheme() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a href=\"tel://0123456789\"></a>
                                    </div>
                                """);

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLinkUnknownScheme_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a href=\"unknown://scheme\"></a>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to disallowed unknown scheme in field:"
                                + " text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLinkInvalidUriChar_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <img src=\"https://www.exam{ple.com/\"></img>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains("Found invalid xhtml link uri in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLinkImgLongDesc_doesNotAllowDataScheme() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <img longdesc=\"data://image/png;base64;ABCD\"></img>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to disallowed data scheme in field:"
                                + " text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLinkAHref_doesNotAllowDataScheme() throws JSONException {
        JSONObject jsonObjectNarrative =
                new JSONObject()
                        .put("status", "generated")
                        .put(
                                "div",
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <a href=\"data://image/png;base64;ABCD\"></a>
                                    </div>
                                """);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to disallowed data scheme in field:"
                                + " text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLinkImgSrc_allowsDataScheme() throws JSONException {
        JSONObject jsonObjectNarrative = buildNarrativeWithImgSrc("data:image/png,ABCD");

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlLinkImgSrc_dataUrlMediaTypeNotCaseSensitive()
            throws JSONException {
        JSONObject jsonObjectNarrative = buildNarrativeWithImgSrc("data:IMAGE/Png,ABCD");

        validate(jsonObjectNarrative.get("div"), "text.div", R4_FHIR_TYPE_XHTML);
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlImgSrcDataUrl_nonImageMediaType_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                buildNarrativeWithImgSrc("data:text/plain;base64,SGVsbG8sIFdvcmxkIQ==");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to missing or non-image media type in data"
                                + " url in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlImgSrcDataUrl_svgMediaType_throws() throws JSONException {
        JSONObject jsonObjectNarrative =
                buildNarrativeWithImgSrc("data:image/svg+xml;base64,PHN2Zy8+");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to disallowed media type `image/svg+xml` in"
                                + " data url in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlImgSrcDataUrl_svgMediaTypeWithParams_throws()
            throws JSONException {
        JSONObject jsonObjectNarrative =
                buildNarrativeWithImgSrc("data:image/svg+xml;charset=UTF-8;base64,PHN2Zy8+");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to disallowed media type `image/svg+xml` in"
                                + " data url in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlImgSrcDataUrl_invalidUriFormat_throws() throws JSONException {
        JSONObject jsonObjectNarrative = buildNarrativeWithImgSrc("data://image/png;base64,ABCD");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        // This fails because `//image/png` is not a valid media type start.
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to missing or non-image media type in data"
                                + " url in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlImgSrcDataUrl_emptyMediaType_throws() throws JSONException {
        JSONObject jsonObjectNarrative = buildNarrativeWithImgSrc("data:,ABCD");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to missing or non-image media type in data"
                                + " url in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlImgSrcDataUrl_emptyMediaTypeNoContent_throws()
            throws JSONException {
        JSONObject jsonObjectNarrative = buildNarrativeWithImgSrc("data:,");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to missing or non-image media type in data"
                                + " url in field: text.div");
    }

    @EnableFlags({FLAG_PHR_FHIR_PRIMITIVE_TYPE_VALIDATION, FLAG_PHR_XHTML_VALIDATION})
    @Test
    public void testValidate_r4XHtmlImgSrcDataUrl_emptyMediaTypeNoParameters_throws()
            throws JSONException {
        JSONObject jsonObjectNarrative = buildNarrativeWithImgSrc("data:;,");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                validate(
                                        jsonObjectNarrative.get("div"),
                                        "text.div",
                                        R4_FHIR_TYPE_XHTML));
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Found invalid xhtml link due to missing or non-image media type in data"
                                + " url in field: text.div");
    }

    private static JSONObject buildNarrativeWithImgSrc(String imgSrc) throws JSONException {
        return new JSONObject()
                .put("status", "generated")
                .put(
                        "div",
                        String.format(
                                """
                                    <div xmlns=\"http://www.w3.org/1999/xhtml\">
                                        <img src=\"%s\"></img>
                                    </div>
                                """,
                                imgSrc));
    }
}
