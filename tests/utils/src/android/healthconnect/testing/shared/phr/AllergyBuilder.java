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

/**
 * A helper class that supports making FHIR AllergyIntolerance data for tests.
 *
 * <p>The Default result will be a valid FHIR AllergyIntolerance, but that is all that should be
 * relied upon. Anything else that is relied upon by a test should be set by one of the methods.
 */
public final class AllergyBuilder extends FhirResourceBuilder<AllergyBuilder> {
    private static final String DEFAULT_JSON =
            "{"
                    + "  \"resourceType\": \"AllergyIntolerance\","
                    + "  \"id\": \"allergyintolerance-1\","
                    + "  \"type\": \"allergy\","
                    + "  \"category\": ["
                    + "    \"medication\""
                    + "  ],"
                    + "  \"criticality\": \"high\","
                    + "  \"code\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\": \"http://snomed.info/sct\","
                    + "        \"code\": \"91936005\","
                    + "        \"display\": \"Penicillin allergy\""
                    + "      }"
                    + "    ],"
                    + "    \"text\": \"Penicillin allergy\""
                    + "  },"
                    + "  \"onsetDateTime\": \"1982\","
                    + "  \"recordedDate\": \"2015-10-09T14:58:00+00:00\","
                    + "  \"recorder\": {"
                    + "    \"display\": \"Dr Jose Rodriguez\""
                    + "  },"
                    + "  \"asserter\": {"
                    + "    \"reference\": \"Patient/patient-1\""
                    + "  },"
                    + "  \"lastOccurrence\": \"2015-10-09\","
                    + "  \"note\": ["
                    + "    {"
                    + "      \"text\": \"The criticality is high because of previous reaction\""
                    + "    }"
                    + "  ],"
                    + "  \"reaction\": ["
                    + "    {"
                    + "      \"substance\": {"
                    + "        \"coding\": ["
                    + "          {"
                    + "            \"system\": \"http://snomed.info/sct\","
                    + "            \"code\": \"39359008\","
                    + "            \"display\": \"Penicillin V-containing product\""
                    + "          }"
                    + "        ]"
                    + "      },"
                    + "      \"manifestation\": ["
                    + "        {"
                    + "          \"coding\": ["
                    + "            {"
                    + "              \"system\": \"http://snomed.info/sct\","
                    + "              \"code\": \"39579001\","
                    + "              \"display\": \"Anaphylactic reaction\""
                    + "            }"
                    + "          ]"
                    + "        }"
                    + "      ],"
                    + "      \"severity\": \"severe\","
                    + "      \"exposureRoute\": {"
                    + "        \"coding\": ["
                    + "          {"
                    + "            \"system\": \"http://snomed.info/sct\","
                    + "            \"code\": \"26643006\","
                    + "            \"display\": \"Oral use\""
                    + "          }"
                    + "        ]"
                    + "      }"
                    + "    }"
                    + "  ],"
                    + "  \"patient\": {"
                    + "    \"reference\": \"Patient/patient-1\","
                    + "    \"display\": \"Example, Anne\""
                    + "  }"
                    + "}";

    /**
     * Creates a default valid FHIR AllergyIntolerance.
     *
     * <p>All that should be relied on is that the AllergyIntolerance is valid. To rely on anything
     * else set it with the other methods.
     */
    public AllergyBuilder() {
        super(DEFAULT_JSON);
    }

    @Override
    protected AllergyBuilder returnThis() {
        return this;
    }
}
