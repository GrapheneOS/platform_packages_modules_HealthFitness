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

public final class ProcedureBuilder extends FhirResourceBuilder<ProcedureBuilder> {

    private static final String DEFAULT_JSON =
            "{"
                    + "  \"resourceType\": \"Procedure\","
                    + "  \"id\": \"f001\","
                    + "  \"status\": \"completed\","
                    + "  \"code\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\": \"http://snomed.info/sct\","
                    + "        \"code\": \"34068001\","
                    + "        \"display\": \"Heart valve replacement\""
                    + "      }"
                    + "    ]"
                    + "  },"
                    + "  \"subject\": {"
                    + "    \"reference\": \"Patient/f001\","
                    + "    \"display\": \"P. van de Heuvel\""
                    + "  },"
                    + "  \"encounter\": {"
                    + "    \"reference\": \"Encounter/f001\""
                    + "  },"
                    + "  \"performedPeriod\": {"
                    + "    \"start\": \"2011-06-26\","
                    + "    \"end\": \"2011-06-27\""
                    + "  },"
                    + "  \"performer\": ["
                    + "    {"
                    + "      \"function\": {"
                    + "        \"coding\": ["
                    + "          {"
                    + "            \"system\": \"urn:oid:2.16.840.1.113883.2.4.15.111\","
                    + "            \"code\": \"01.000\","
                    + "            \"display\": \"Arts\""
                    + "          }"
                    + "        ],"
                    + "        \"text\": \"Care role\""
                    + "      },"
                    + "      \"actor\": {"
                    + "        \"reference\": \"Practitioner/f002\","
                    + "        \"display\": \"P. Voigt\""
                    + "      }"
                    + "    }"
                    + "  ],"
                    + "  \"reasonCode\": ["
                    + "    {"
                    + "      \"text\": \"Heart valve disorder\""
                    + "    }"
                    + "  ],"
                    + "  \"bodySite\": ["
                    + "    {"
                    + "      \"coding\": ["
                    + "        {"
                    + "          \"system\": \"http://snomed.info/sct\","
                    + "          \"code\": \"17401000\","
                    + "          \"display\": \"Heart valve structure\""
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ],"
                    + "  \"outcome\": {"
                    + "    \"text\": \"improved blood circulation\""
                    + "  },"
                    + "  \"report\": ["
                    + "    {"
                    + "      \"reference\": \"DiagnosticReport/f001\","
                    + "      \"display\": \"Lab results blood test\""
                    + "    }"
                    + "  ],"
                    + "  \"followUp\": ["
                    + "    {"
                    + "      \"text\": \"described in care plan\""
                    + "    }"
                    + "  ]"
                    + "}";

    /**
     * Creates a default valid FHIR Procedure.
     *
     * <p>All that should be relied on is that the Procedure is valid. To rely on anything else set
     * it with the other methods.
     */
    public ProcedureBuilder() {
        super(DEFAULT_JSON);
    }

    @Override
    protected ProcedureBuilder returnThis() {
        return this;
    }
}
