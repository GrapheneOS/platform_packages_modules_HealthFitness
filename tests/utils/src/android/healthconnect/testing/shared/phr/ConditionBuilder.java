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
 * A helper class that supports making FHIR Condition data for tests.
 *
 * <p>The Default result will be a valid FHIR Condition, but that is all that should be relied upon.
 * Anything else that is relied upon by a test should be set by one of the methods.
 */
public final class ConditionBuilder extends FhirResourceBuilder<ConditionBuilder> {
    private static final String DEFAULT_JSON =
            "{"
                    + "  \"resourceType\": \"Condition\","
                    + "  \"id\": \"f201\","
                    + "  \"identifier\": ["
                    + "    {"
                    + "      \"value\": \"12345\""
                    + "    }"
                    + "  ],"
                    + "  \"clinicalStatus\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\":"
                    + "\"http://terminology.hl7.org/CodeSystem/condition-clinical\","
                    + "        \"code\": \"resolved\""
                    + "      }"
                    + "    ]"
                    + "  },"
                    + "  \"verificationStatus\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\":"
                    + "\"http://terminology.hl7.org/CodeSystem/condition-ver-status\","
                    + "        \"code\": \"confirmed\""
                    + "      }"
                    + "    ]"
                    + "  },"
                    + "  \"category\": ["
                    + "    {"
                    + "      \"coding\": ["
                    + "        {"
                    + "          \"system\": \"http://snomed.info/sct\","
                    + "          \"code\": \"55607006\","
                    + "          \"display\": \"Problem\""
                    + "        },"
                    + "        {"
                    + "          \"system\":"
                    + "\"http://terminology.hl7.org/CodeSystem/condition-category\","
                    + "          \"code\": \"problem-list-item\""
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ],"
                    + "  \"severity\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\": \"http://snomed.info/sct\","
                    + "        \"code\": \"255604002\","
                    + "        \"display\": \"Mild\""
                    + "      }"
                    + "    ]"
                    + "  },"
                    + "  \"code\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\": \"http://snomed.info/sct\","
                    + "        \"code\": \"386661006\","
                    + "        \"display\": \"Fever\""
                    + "      }"
                    + "    ]"
                    + "  },"
                    + "  \"bodySite\": ["
                    + "    {"
                    + "      \"coding\": ["
                    + "        {"
                    + "          \"system\": \"http://snomed.info/sct\","
                    + "          \"code\": \"38266002\","
                    + "          \"display\": \"Entire body as a whole\""
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ],"
                    + "  \"subject\": {"
                    + "    \"reference\": \"Patient/f201\","
                    + "    \"display\": \"Roel\""
                    + "  },"
                    + "  \"encounter\": {"
                    + "    \"reference\": \"Encounter/f201\""
                    + "  },"
                    + "  \"onsetDateTime\": \"2013-04-02\","
                    + "  \"abatementString\": \"around April 9, 2013\","
                    + "  \"recordedDate\": \"2013-04-04\","
                    + "  \"recorder\": {"
                    + "    \"reference\": \"Practitioner/f201\""
                    + "  },"
                    + "  \"asserter\": {"
                    + "    \"reference\": \"Practitioner/f201\""
                    + "  },"
                    + "  \"evidence\": ["
                    + "    {"
                    + "      \"code\": ["
                    + "        {"
                    + "          \"coding\": ["
                    + "            {"
                    + "              \"system\": \"http://snomed.info/sct\","
                    + "              \"code\": \"258710007\","
                    + "              \"display\": \"degrees C\""
                    + "            }"
                    + "          ]"
                    + "        }"
                    + "      ],"
                    + "      \"detail\": ["
                    + "        {"
                    + "          \"reference\": \"Observation/f202\","
                    + "          \"display\": \"Temperature\""
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ]"
                    + "}";

    /**
     * Creates a default valid FHIR Condition.
     *
     * <p>All that should be relied on is that the Observation is valid. To rely on anything else
     * set it with the other methods.
     */
    public ConditionBuilder() {
        super(DEFAULT_JSON);
    }

    @Override
    protected ConditionBuilder returnThis() {
        return this;
    }
}
