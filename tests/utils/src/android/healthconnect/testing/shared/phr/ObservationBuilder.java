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

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class that supports making FHIR Observations data for tests.
 *
 * <p>The Default result will be a valid FHIR Observation, but that is all that should be relied
 * upon. Anything else that is relied upon by a test should be set by one of the methods.
 */
public final class ObservationBuilder extends FhirResourceBuilder<ObservationBuilder> {

    /** URI representing the LOINC coding system. */
    public static final Uri LOINC = Uri.parse("http://loinc.org");

    /** URI representing the SNOMED coding system. */
    public static final Uri SNOMED_CT = Uri.parse("http://snomed.info/sct");

    /**
     * Values from the default <a
     * href="http://terminology.hl7.org/CodeSystem/observation-category">FHIR value set</a> for
     * observation category.
     */
    public enum ObservationCategory {
        SOCIAL_HISTORY("social-history", "Social History"),
        VITAL_SIGNS("vital-signs", "Vital Signs"),
        IMAGING("imaging", "Imaging"),
        LABORATORY("laboratory", "Laboratory"),
        PROCEDURE("procedure", "Procedure"),
        SURVEY("survey", "Survey"),
        EXAM("exam", "Exam"),
        THERAPY("therapy", "Therapy"),
        ACTIVITY("activity", "Activity"),
        ;
        private final String mCode;
        private final String mDisplay;

        ObservationCategory(String code, String display) {
            mCode = code;
            mDisplay = display;
        }

        public String getCode() {
            return mCode;
        }

        public String getDisplay() {
            return mDisplay;
        }

        /**
         * Returns a FHIR <a
         * href="https://www.hl7.org/fhir/R4/datatypes.html#CodeableConcept">CodeableConcept</a>
         * version of this category.
         */
        public JSONObject toFhirCodeableConcept() {
            return makeCodeableConcept(
                    Uri.parse("http://terminology.hl7.org/CodeSystem/observation-category"),
                    getCode(),
                    getDisplay());
        }
    }

    /** Various common units from http://unitsofmeasure.org. Not complete. */
    public enum QuantityUnits {
        COUNT("1", "1"),
        PERCENT("%", "%"),
        MMOL_PER_L("mmol/l", "mmol/l"),
        BREATHS_PER_MINUTE("breaths/minute", "/min"),
        BEATS_PER_MINUTE("beats/minute", "/min"),
        CELSIUS("C", "Cel"),
        CENTIMETERS("cm", "cm"),
        KILOGRAMS("kg", "kg"),
        POUNDS("lbs", "[lb_av]"),
        KILOGRAMS_PER_M2("kg/m2", "kg/m2"),
        MILLIMETERS_OF_MERCURY("mmHg", "mm[Hg]"),
        GLASSES_OF_WINE_PER_DAY("wine glasses per day", "/d"),
        ;

        private final String mUnit;
        private final String mUnitCode;

        QuantityUnits(String unit, String unitCode) {
            mUnit = unit;
            mUnitCode = unitCode;
        }

        public String getUnit() {
            return mUnit;
        }

        public String getUnitCode() {
            return mUnitCode;
        }

        /**
         * Returns a JSON Object representing a <a
         * href="https://hl7.org/fhir/R4/datatypes.html#Quantity">Quantity</a>, in these units..
         *
         * @param value the unitless value for the quantity
         * @throws JSONException if any JSON problem occurs.
         */
        public JSONObject makeFhirQuantity(Number value) throws JSONException {
            LinkedHashMap<String, Object> contents = new LinkedHashMap<>();
            contents.put("value", value);
            contents.put("unit", mUnit);
            contents.put("system", "http://unitsofmeasure.org");
            contents.put("code", mUnitCode);
            return new JSONObject(contents);
        }
    }

    /**
     * Representation of the International Patient Summary recommended <a
     * href="https://build.fhir.org/ig/HL7/fhir-ips/ValueSet-pregnancy-status-uv-ips.html">ValueSet</a>
     * for pregnancy status.
     */
    public enum PregnancyStatus {
        PREGNANT("77386006"),
        NOT_PREGNANT("60001007"),
        PREGNANCY_NOT_CONFIRMED("152231000119106"),
        POSSIBLE_PREGNANCY("146799005"),
        ;
        private final String mSnomedCode;

        PregnancyStatus(String snomedCode) {
            mSnomedCode = snomedCode;
        }

        public String getSnomedCode() {
            return mSnomedCode;
        }
    }

    /**
     * Representation of the International Patient Summary recommended <a
     * href="https://build.fhir.org/ig/HL7/fhir-ips/ValueSet-current-smoking-status-uv-ips.html">ValueSet</a>
     * for current smoking status.
     */
    public enum CurrentSmokingStatus {
        SMOKES_TOBACCO_DAILY("449868002"),
        OCCASIONAL_TOBACCO_SMOKER("428041000124106"),
        EX_SMOKER("8517006"),
        NEVER_SMOKED_TOBACCO("266919005"),
        SMOKER("77176002"),
        TOBACCO_SMOKING_CONSUMPTION_UNKNOWN("266927001"),
        HEAVY_CIGARETTE_SMOKER("230063004"),
        LIGHT_CIGARETTE_SMOKER("230060001"),
        ;
        private final String mSnomedCode;

        CurrentSmokingStatus(String snomedCode) {
            mSnomedCode = snomedCode;
        }

        public String getSnomedCode() {
            return mSnomedCode;
        }
    }

    private static final String DEFAULT_JSON =
            "{"
                    + "  \"resourceType\": \"Observation\","
                    + "  \"id\": \"f001\","
                    + "  \"identifier\": ["
                    + "    {"
                    + "      \"use\": \"official\","
                    + "      \"system\": \"http://www.bmc.nl/zorgportal/identifiers/observations\","
                    + "      \"value\": \"6323\""
                    + "    }"
                    + "  ],"
                    + "  \"status\": \"final\","
                    + "  \"subject\": {"
                    + "    \"reference\": \"Patient/f001\","
                    + "    \"display\": \"A. Lincoln\""
                    + "  },"
                    + "  \"effectivePeriod\": {"
                    + "    \"start\": \"2013-04-02T09:30:10+01:00\""
                    + "  },"
                    + "  \"issued\": \"2013-04-03T15:30:10+01:00\","
                    + "  \"performer\": ["
                    + "    {"
                    + "      \"reference\": \"Practitioner/f005\","
                    + "      \"display\": \"G. Washington\""
                    + "    }"
                    + "  ]"
                    + "}";

    /**
     * Creates a default valid FHIR Observation.
     *
     * <p>All that should be relied on is that the Observation is valid. To rely on anything else
     * set it with the other methods.
     */
    public ObservationBuilder() {
        super(DEFAULT_JSON);
        setBloodGlucose(6.3);
    }

    /**
     * Sets the category for this observation.
     *
     * @return this builder.
     */
    public ObservationBuilder setCategory(ObservationCategory category) {
        return set("category", new JSONArray(List.of(category.toFhirCodeableConcept())));
    }

    /** Sets this observation to represent a default blood glucose observation. */
    public ObservationBuilder setBloodGlucose() {
        return setBloodGlucose(6.3);
    }

    /**
     * Sets this observation to represent a blood glucose observation.
     *
     * @param mmolPerLitre the measurement of blood glucose in mmol/liter.
     */
    public ObservationBuilder setBloodGlucose(double mmolPerLitre) {
        return setBloodGlucose(mmolPerLitre, 3.1, 6.2);
    }

    /**
     * Sets this observation to represent blood glucose observation, including whether it is high,
     * low or normal.
     *
     * @param mmolPerLitre the measurement of blood glucose in mmol/liter.
     * @param lowBoundary the upper limit for a low reading (exclusive) in mmol/liter
     * @param highBoundary the lower limit for a high reading (exclusive) in mmol/liter
     */
    public ObservationBuilder setBloodGlucose(
            double mmolPerLitre, double lowBoundary, double highBoundary) {
        // See https://terminology.hl7.org/6.0.2/CodeSystem-v3-ObservationInterpretation.html
        // for these codes.
        String code;
        String display;
        if (mmolPerLitre > highBoundary) {
            code = "H";
            display = "High";
        } else if (mmolPerLitre < lowBoundary) {
            code = "L";
            display = "Low";
        } else {
            code = "N";
            display = "Normal";
        }
        try {
            setCode(LOINC, "15074-8");
            set("valueQuantity", QuantityUnits.MMOL_PER_L.makeFhirQuantity(mmolPerLitre));
            JSONObject interpretation =
                    makeCodeableConcept(
                            Uri.parse(
                                    "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation"),
                            code,
                            display);
            set("interpretation", new JSONArray(List.of(interpretation)));
            JSONObject range =
                    new JSONObject(
                            Map.of(
                                    "low",
                                    QuantityUnits.MMOL_PER_L.makeFhirQuantity(lowBoundary),
                                    "high",
                                    QuantityUnits.MMOL_PER_L.makeFhirQuantity(highBoundary)));
            set("referenceRange", new JSONArray(List.of(range)));

        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    /**
     * Set pregnancy status as recommended by the <a
     * href="https://build.fhir.org/ig/HL7/fhir-ips/StructureDefinition-Observation-pregnancy-status-uv-ips.html">Internation
     * Patient Summary</a>.
     *
     * @return this builder
     */
    public ObservationBuilder setPregnancyStatus(PregnancyStatus status) {
        setCode(LOINC, "82810-3");
        setValueCodeableConcept(SNOMED_CT, status.getSnomedCode());
        return this;
    }

    /**
     * Set tobacco use as recommended by the <a
     * href="https://build.fhir.org/ig/HL7/fhir-ips/StructureDefinition-Observation-tobaccouse-uv-ips.html">Internation
     * Patient Summary</a>.
     *
     * @return this builder
     */
    public ObservationBuilder setTobaccoUse(CurrentSmokingStatus status) {
        setCategory(ObservationCategory.SOCIAL_HISTORY);
        setCode(LOINC, "72166-2");
        setValueCodeableConcept(SNOMED_CT, status.getSnomedCode());
        return this;
    }

    /**
     * Set that a heart rate in beats per minute was observed. Uses the <a
     * href="https://hl7.org/fhir/R5/observation-vitalsigns.html">IPS Vital Signs</a>
     * recommendations.
     *
     * @return this builder
     */
    public ObservationBuilder setHeartRate(int beatsPerMinute) {
        setCategory(ObservationCategory.VITAL_SIGNS);
        setCode(LOINC, "8867-4", "Heart rate");
        setValueQuantity(beatsPerMinute, QuantityUnits.BEATS_PER_MINUTE);
        return this;
    }

    /**
     * Sets the code for the observation.
     *
     * @param system the Uri for the coding system the code comes from
     * @return this builder
     */
    public ObservationBuilder setCode(@NonNull Uri system, @NonNull String code) {
        return setCode(system, code, /* display= */ null);
    }

    /**
     * Sets the code for the observation.
     *
     * @param system the Uri for the coding system the code comes from
     * @param display A human readable display value for the code
     * @return this builder
     */
    public ObservationBuilder setCode(
            @NonNull Uri system, @NonNull String code, @Nullable String display) {
        return set("code", makeCodeableConcept(system, code, display));
    }

    /**
     * Set the value for an observation where the value should be represented as a codeable concept
     * with a single code.
     *
     * @param code the code for the value
     * @param system the coding system for the value
     * @return this builder
     */
    public ObservationBuilder setValueCodeableConcept(@NonNull Uri system, @NonNull String code) {
        return removeAllValueMultiTypeFields()
                .set(
                        "valueCodeableConcept",
                        makeCodeableConcept(system, code, /* display= */ null));
    }

    /**
     * Set the value for an observation where the value should be represented as a quantity in some
     * units.
     *
     * @return this builder
     */
    public ObservationBuilder setValueQuantity(Number quantity, QuantityUnits units) {
        try {
            return removeAllValueMultiTypeFields()
                    .set("valueQuantity", units.makeFhirQuantity(quantity));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Removes all fields that are part of the effective[x] multi type field, such as
     * effectiveDateTime and effectivePeriod.
     *
     * <p>This should be used before setting one of these fields, as only one is allowed to be set.
     *
     * @return this builder
     */
    public ObservationBuilder removeAllEffectiveMultiTypeFields() {
        return removeField("effectiveDateTime")
                .removeField("effectivePeriod")
                .removeField("effectiveTiming")
                .removeField("effectiveInstant");
    }

    /**
     * Removes all fields that are part of the value[x] multi type field, such as valueQuantity and
     * valueCodeableConcept.
     *
     * <p>This should be used before setting one of these fields, as only one is allowed to be set.
     *
     * @return this builder
     */
    public ObservationBuilder removeAllValueMultiTypeFields() {
        return removeField("valueQuantity")
                .removeField("valueCodeableConcept")
                .removeField("valueString")
                .removeField("valueBoolean")
                .removeField("valueInteger")
                .removeField("valueRange")
                .removeField("valueRatio")
                .removeField("valueSampledData")
                .removeField("valueTime")
                .removeField("valueDateTime")
                .removeField("valuePeriod");
    }

    /**
     * Returns a JSON Object representing a FHIR <a
     * href="https://www.hl7.org/fhir/R4/datatypes.html#CodeableConcept">CodeableConcept</a>.
     *
     * @param system the coding system for the value.
     * @param code the code for the value
     * @param display if non-null a display string for the value
     */
    @NonNull
    public static JSONObject makeCodeableConcept(
            @NonNull Uri system, @NonNull String code, @Nullable String display) {
        LinkedHashMap<String, String> content = new LinkedHashMap<>();
        content.put("system", system.toString());
        content.put("code", code);
        if (display != null) {
            content.put("display", display);
        }
        return new JSONObject(Map.of("coding", new JSONArray(List.of(new JSONObject(content)))));
    }

    @Override
    protected ObservationBuilder returnThis() {
        return this;
    }
}
