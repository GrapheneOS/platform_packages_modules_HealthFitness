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
 * Helper class for building FHIR data related to encounters, including <a
 * href="https://www.hl7.org/fhir/encounter.html">Encounter</a> (see {@link #encounter}), <a
 * href="https://www.hl7.org/fhir/location.html">Location</a> (see {@link #location}), and <a
 * href="https://www.hl7.org/fhir/organization.html">Organization</a> (see {@link #organization})
 */
public class EncountersBuilder {

    private static final String DEFAULT_ENCOUNTER_JSON =
            "{"
                    + "  \"resourceType\": \"Encounter\","
                    + "  \"id\": \"example\","
                    + "  \"status\": \"in-progress\","
                    + "  \"class\": {"
                    + "    \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\","
                    + "    \"code\": \"IMP\","
                    + "    \"display\": \"inpatient encounter\""
                    + "  },"
                    + "  \"subject\": {"
                    + "    \"reference\": \"Patient/example\""
                    + "  },"
                    + "  \"diagnosis\": ["
                    + "    {"
                    + "      \"condition\": {"
                    + "        \"display\":"
                    + "\"Complications from Roel's TPF chemotherapy on January 28th, 2013\""
                    + "      },"
                    + "      \"use\": {"
                    + "        \"coding\": ["
                    + "          {"
                    + "            \"system\":"
                    + " \"http://terminology.hl7.org/CodeSystem/diagnosis-role\","
                    + "            \"code\": \"AD\","
                    + "            \"display\": \"Admission diagnosis\""
                    + "          }"
                    + "        ]"
                    + "      },"
                    + "      \"rank\": 2"
                    + "    },"
                    + "    {"
                    + "      \"condition\": {"
                    + "        \"display\": \"The patient is treated for a tumor\""
                    + "      },"
                    + "      \"use\": {"
                    + "        \"coding\": ["
                    + "          {"
                    + "            \"system\":"
                    + "\"http://terminology.hl7.org/CodeSystem/diagnosis-role\","
                    + "            \"code\": \"CC\","
                    + "            \"display\": \"Chief complaint\""
                    + "          }"
                    + "        ]"
                    + "      },"
                    + "      \"rank\": 1"
                    + "    }"
                    + "  ]"
                    + "}";

    private static final String DEFAULT_LOCATION_JSON =
            "{"
                    + "  \"resourceType\": \"Location\","
                    + "  \"id\": \"1\","
                    + "  \"identifier\": ["
                    + "    {"
                    + "      \"value\": \"B1-S.F2\""
                    + "    }"
                    + "  ],"
                    + "  \"status\": \"active\","
                    + "  \"name\": \"South Wing, second floor\","
                    + "  \"alias\": ["
                    + "    \"BU MC, SW, F2\","
                    + "    \"Burgers University Medical Center, South Wing, second floor\""
                    + "  ],"
                    + "  \"description\": \"Second floor of the Old South Wing\","
                    + "  \"mode\": \"instance\","
                    + "  \"telecom\": ["
                    + "    {"
                    + "      \"system\": \"phone\","
                    + "      \"value\": \"2328\","
                    + "      \"use\": \"work\""
                    + "    },"
                    + "    {"
                    + "      \"system\": \"fax\","
                    + "      \"value\": \"2329\","
                    + "      \"use\": \"work\""
                    + "    },"
                    + "    {"
                    + "      \"system\": \"email\","
                    + "      \"value\": \"second wing admissions\""
                    + "    },"
                    + "    {"
                    + "      \"system\": \"url\","
                    + "      \"value\": \"http://sampleorg.com/southwing\","
                    + "      \"use\": \"work\""
                    + "    }"
                    + "  ],"
                    + "  \"address\": {"
                    + "    \"use\": \"work\","
                    + "    \"line\": ["
                    + "      \"Galapagosweg 91, Building A\""
                    + "    ],"
                    + "    \"city\": \"Den Burg\","
                    + "    \"postalCode\": \"9105 PZ\","
                    + "    \"country\": \"NLD\""
                    + "  },"
                    + "  \"physicalType\": {"
                    + "    \"coding\": ["
                    + "      {"
                    + "        \"system\":"
                    + "\"http://terminology.hl7.org/CodeSystem/location-physical-type\","
                    + "        \"code\": \"wi\","
                    + "        \"display\": \"Wing\""
                    + "      }"
                    + "    ]"
                    + "  },"
                    + "  \"position\": {"
                    + "    \"longitude\": -83.6945691,"
                    + "    \"latitude\": 42.25475478,"
                    + "    \"altitude\": 0"
                    + "  },"
                    + "  \"managingOrganization\": {"
                    + "    \"reference\": \"Organization/f001\""
                    + "  },"
                    + "  \"endpoint\": ["
                    + "    {"
                    + "      \"reference\": \"Endpoint/example\""
                    + "    }"
                    + "  ]"
                    + "}";

    private static final String DEFAULT_ORGANIZATION_JSON =
            "{"
                    + "  \"resourceType\": \"Organization\","
                    + "  \"id\": \"2.16.840.1.113883.19.5\","
                    + "  \"identifier\": ["
                    + "    {"
                    + "      \"system\": \"urn:ietf:rfc:3986\","
                    + "      \"value\": \"urn:oid:2.16.840.1.113883.19.5\""
                    + "    }"
                    + "  ],"
                    + "  \"name\": \"Good Health Clinic\""
                    + "}";

    public static class EncounterBuilder extends FhirResourceBuilder<EncounterBuilder> {
        private EncounterBuilder() {
            super(DEFAULT_ENCOUNTER_JSON);
        }

        @Override
        protected EncounterBuilder returnThis() {
            return this;
        }
    }

    public static class LocationBuilder extends FhirResourceBuilder<LocationBuilder> {
        private LocationBuilder() {
            super(DEFAULT_LOCATION_JSON);
        }

        @Override
        protected LocationBuilder returnThis() {
            return this;
        }
    }

    public static class OrganizationBuilder extends FhirResourceBuilder<OrganizationBuilder> {
        private OrganizationBuilder() {
            super(DEFAULT_ORGANIZATION_JSON);
        }

        @Override
        protected OrganizationBuilder returnThis() {
            return this;
        }
    }

    /**
     * Returns a helper class useful for making FHIR <a
     * href="https://www.hl7.org/fhir/encounter.html">Encounter</a> data for use in tests.
     */
    public static EncounterBuilder encounter() {
        return new EncounterBuilder();
    }

    /**
     * Returns a helper class useful for making FHIR <a
     * href="https://www.hl7.org/fhir/location.html">Location</a> data for use in tests.
     */
    public static LocationBuilder location() {
        return new LocationBuilder();
    }

    /**
     * Returns a helper class useful for making FHIR <a
     * href="https://www.hl7.org/fhir/organization.html">Organization</a> data for use in tests.
     */
    public static OrganizationBuilder organization() {
        return new OrganizationBuilder();
    }
}
