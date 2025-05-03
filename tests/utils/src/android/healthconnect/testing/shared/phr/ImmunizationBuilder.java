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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class that supports making FHIR Immunization data for tests.
 *
 * <p>The Default result will be a valid FHIR Immunization, but that is all that should be relied
 * upon. Anything else that is relied upon by a test should be set by one of the methods.
 */
public final class ImmunizationBuilder extends FhirResourceBuilder<ImmunizationBuilder> {
    private static final String DEFAULT_JSON =
            "{"
                    + "  \"resourceType\": \"Immunization\","
                    + "  \"id\": \"immunization-1\","
                    + "  \"status\": \"completed\","
                    + "  \"vaccineCode\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\": \"http://hl7.org/fhir/sid/cvx\","
                    + "        \"code\": \"115\""
                    + "      },"
                    + "      {"
                    + "        \"system\": \"http://hl7.org/fhir/sid/ndc\","
                    + "        \"code\": \"58160-842-11\""
                    + "      }"
                    + "    ],"
                    + "    \"text\": \"Tdap\""
                    + "  },"
                    + "  \"patient\": {"
                    + "    \"reference\": \"Patient/patient_1\","
                    + "    \"display\": \"Example, Anne\""
                    + "  },"
                    + "  \"encounter\": {"
                    + "    \"reference\": \"Encounter/encounter_unk\","
                    + "    \"display\": \"GP Visit\""
                    + "  },"
                    + "  \"occurrenceDateTime\": \"2018-05-21\","
                    + "  \"primarySource\": true,"
                    + "  \"manufacturer\": {"
                    + "    \"display\": \"Sanofi Pasteur\""
                    + "  },"
                    + "  \"lotNumber\": \"1\","
                    + "  \"site\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActSite\","
                    + "        \"code\": \"LA\","
                    + "        \"display\": \"Left Arm\""
                    + "      }"
                    + "    ],"
                    + "    \"text\": \"Left Arm\""
                    + "  },"
                    + "  \"route\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\":"
                    + "\"http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration\","
                    + "        \"code\": \"IM\","
                    + "        \"display\": \"Injection, intramuscular\""
                    + "      }"
                    + "    ],"
                    + "    \"text\": \"Injection, intramuscular\""
                    + "  },"
                    + "  \"doseQuantity\": {"
                    + "    \"value\": 0.5,"
                    + "    \"unit\": \"mL\""
                    + "  },"
                    + "  \"performer\": ["
                    + "    {"
                    + "      \"function\": {"
                    + "        \"coding\": ["
                    + "          {"
                    + "            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0443\","
                    + "            \"code\": \"AP\","
                    + "            \"display\": \"Administering Provider\""
                    + "          }"
                    + "        ],"
                    + "        \"text\": \"Administering Provider\""
                    + "      },"
                    + "      \"actor\": {"
                    + "        \"reference\": \"Practitioner/practitioner_1\","
                    + "        \"type\": \"Practitioner\","
                    + "        \"display\": \"Dr Maria Hernandez\""
                    + "      }"
                    + "    }"
                    + "  ]"
                    + "}";

    private static final String DEFAULT_TEXT_NARRATIVE_XHTML =
            """
            <div xmlns=\"http://www.w3.org/1999/xhtml\">
                <p><b>Generated Narrative with Details</b></p>
                <p><b>id</b>: example</p>
                <p><b>identifier</b>: urn:oid:1.3.6.1.4.1.21367.2005.3.7.1234</p>
                <p><b>status</b>: completed</p>
                <p><b>vaccineCode</b>:
                    Fluvax (Influenza)
                    <span>(Details : {urn:oid:1.2.36.1.2001.1005.17 code 'FLUVAX' =
                        'Fluvax)</span></p>
                <p><b>patient</b>: <a>Patient/example</a></p>
                <p><b>encounter</b>: <a>Encounter/example</a></p>
                <p><b>occurrence</b>: 10/01/2013</p>
                <p><b>primarySource</b>: true</p>
                <p><b>location</b>: <a>Location/1</a></p>
                <p><b>manufacturer</b>: <a>Organization/hl7</a></p>
                <p><b>lotNumber</b>: AAJN11K</p>
                <p><b>expirationDate</b>: 15/02/2015</p>
                <p><b>site</b>: left arm
                <span>(Details :
                {http://terminology.hl7.org/CodeSystem/v3-ActSite code 'LA' = 'left arm',
                        given as 'left arm'})</span></p>
                <p><b>route</b>: Injection, intramuscular <span>(Details :
                        {http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration
                        code 'IM' = 'Injection, intramuscular',
                        given as 'Injection, intramuscular'})</span></p>
                <p><b>doseQuantity</b>: 5 mg<span> (Details: UCUM code mg = 'mg')</span></p>
                <blockquote>
                    <p><b>performer</b></p>
                    <p><b>function</b>: Ordering Provider <span>(Details :
                    {http://terminology.hl7.org/CodeSystem/v2-0443 code 'OP'
                            = 'Ordering Provider)</span></p>
                    <p><b>actor</b>: <a>Practitioner/example</a></p>
                </blockquote>
                <blockquote>
                    <p><b>performer</b></p>
                    <p><b>function</b>: Administering Provider <span>(Details :
                    {http://terminology.hl7.org/CodeSystem/v2-0443 code
                            'AP' = 'Administering Provider)</span></p>
                    <p><b>actor</b>: <a>Practitioner/example</a></p>
                </blockquote>
                <p><b>note</b>: Notes on administration of vaccine</p>
                <p><b>reasonCode</b>: Procedure to meet occupational requirement
                <span>(Details : {SNOMED CT code '429060002' =
                        'Procedure to meet occupational requirement)</span></p>
                <p><b>isSubpotent</b>: true</p>
                <h3>Educations</h3>
                <table>
                    <tr>
                        <td>-</td>
                        <td><b>DocumentType</b></td>
                        <td><b>PublicationDate</b></td>
                        <td><b>PresentationDate</b></td>
                    </tr>
                    <tr>
                        <td>*</td>
                        <td>253088698300010311120702</td>
                        <td>02/07/2012</td>
                        <td>10/01/2013</td>
                    </tr>
                </table>
                <p><b>programEligibility</b>: Not Eligible <span>(Details :
                        {http://terminology.hl7.org/CodeSystem/immunization-program-eligibility
                        code 'ineligible' = 'Not
                        Eligible)</span></p>
                <p><b>fundingSource</b>: Private <span>(Details :
                {http://terminology.hl7.org/CodeSystem/immunization-funding-source
                        code 'private' = 'Private)</span></p>
            </div>
            """;

    /**
     * Creates a default valid FHIR Immunization.
     *
     * <p>All that should be relied on is that the Immunization is valid. To rely on anything else
     * set it with the other methods.
     */
    public ImmunizationBuilder() {
        super(DEFAULT_JSON);
    }

    /** Sets the "text" field to a valid Narrative with populated xhtml. */
    public ImmunizationBuilder setTextNarrative() {
        JSONObject textNarrative = new JSONObject();
        try {
            textNarrative.put("status", "generated").put("div", DEFAULT_TEXT_NARRATIVE_XHTML);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }

        return set("text", textNarrative);
    }

    @Override
    protected ImmunizationBuilder returnThis() {
        return this;
    }
}
