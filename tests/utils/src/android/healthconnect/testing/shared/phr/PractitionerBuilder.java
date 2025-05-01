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
 * Test helper class for making practitioner related FHIR data, including <a
 * href="https://www.hl7.org/fhir/practitioner.html">Practitioner</a> and <a
 * href="https://www.hl7.org/fhir/practitionerrole.html">PractitionerRole</a> - see {@link #role()}.
 */
public final class PractitionerBuilder extends FhirResourceBuilder<PractitionerBuilder> {

    private static final String DEFAULT_PRACTITIONER_JSON =
            "{"
                    + "  \"resourceType\": \"Practitioner\","
                    + "  \"id\": \"f001\","
                    + "  \"identifier\": ["
                    + "    {"
                    + "      \"use\": \"official\","
                    + "      \"system\": \"urn:oid:2.16.528.1.1007.3.1\","
                    + "      \"value\": \"938273695\""
                    + "    },"
                    + "    {"
                    + "      \"use\": \"usual\","
                    + "      \"system\": \"urn:oid:2.16.840.1.113883.2.4.6.3\","
                    + "      \"value\": \"129IDH4OP733\""
                    + "    }"
                    + "  ],"
                    + "  \"name\": ["
                    + "    {"
                    + "      \"use\": \"official\","
                    + "      \"family\": \"van den broek\","
                    + "      \"given\": ["
                    + "        \"Eric\""
                    + "      ],"
                    + "      \"suffix\": ["
                    + "        \"MD\""
                    + "      ]"
                    + "    }"
                    + "  ],"
                    + "  \"telecom\": ["
                    + "    {"
                    + "      \"system\": \"phone\","
                    + "      \"value\": \"0205568263\","
                    + "      \"use\": \"work\""
                    + "    },"
                    + "    {"
                    + "      \"system\": \"email\","
                    + "      \"value\": \"E.M.vandenbroek@bmc.nl\","
                    + "      \"use\": \"work\""
                    + "    },"
                    + "    {"
                    + "      \"system\": \"fax\","
                    + "      \"value\": \"0205664440\","
                    + "      \"use\": \"work\""
                    + "    }"
                    + "  ],"
                    + "  \"address\": ["
                    + "    {"
                    + "      \"use\": \"work\","
                    + "      \"line\": ["
                    + "        \"Galapagosweg 91\""
                    + "      ],"
                    + "      \"city\": \"Den Burg\","
                    + "      \"postalCode\": \"9105 PZ\","
                    + "      \"country\": \"NLD\""
                    + "    }"
                    + "  ],"
                    + "  \"gender\": \"male\","
                    + "  \"birthDate\": \"1975-12-07\""
                    + "}";

    private static final String DEFAULT_PRACTITIONER_ROLE_JSON =
            "{"
                    + "  \"resourceType\": \"PractitionerRole\","
                    + "  \"id\": \"example\","
                    + "  \"identifier\": ["
                    + "    {"
                    + "      \"system\": \"http://www.acme.org/practitioners\","
                    + "      \"value\": \"23\""
                    + "    }"
                    + "  ],"
                    + "  \"active\": true,"
                    + "  \"period\": {"
                    + "    \"start\": \"2012-01-01\","
                    + "    \"end\": \"2012-03-31\""
                    + "  },"
                    + "  \"practitioner\": {"
                    + "    \"reference\": \"Practitioner/example\","
                    + "    \"display\": \"Dr Adam Careful\""
                    + "  },"
                    + "  \"organization\": {"
                    + "    \"reference\": \"Organization/f001\""
                    + "  },"
                    + "  \"code\": ["
                    + "    {"
                    + "      \"coding\": ["
                    + "        {"
                    + "          \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0286\","
                    + "          \"code\": \"RP\""
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ],"
                    + "  \"specialty\": ["
                    + "    {"
                    + "      \"coding\": ["
                    + "        {"
                    + "          \"system\": \"http://snomed.info/sct\","
                    + "          \"code\": \"408443003\","
                    + "          \"display\": \"General medical practice\""
                    + "        }"
                    + "      ]"
                    + "    }"
                    + "  ],"
                    + "  \"location\": ["
                    + "    {"
                    + "      \"reference\": \"Location/1\","
                    + "      \"display\": \"South Wing, second floor\""
                    + "    }"
                    + "  ],"
                    + "  \"healthcareService\": ["
                    + "    {"
                    + "      \"reference\": \"HealthcareService/example\""
                    + "    }"
                    + "  ],"
                    + "  \"telecom\": ["
                    + "    {"
                    + "      \"system\": \"phone\","
                    + "      \"value\": \"(03) 5555 6473\","
                    + "      \"use\": \"work\""
                    + "    },"
                    + "    {"
                    + "      \"system\": \"email\","
                    + "      \"value\": \"adam.southern@example.org\","
                    + "      \"use\": \"work\""
                    + "    }"
                    + "  ],"
                    + "  \"availableTime\": ["
                    + "    {"
                    + "      \"daysOfWeek\": ["
                    + "        \"mon\","
                    + "        \"tue\","
                    + "        \"wed\""
                    + "      ],"
                    + "      \"availableStartTime\": \"09:00:00\","
                    + "      \"availableEndTime\": \"16:30:00\""
                    + "    },"
                    + "    {"
                    + "      \"daysOfWeek\": ["
                    + "        \"thu\","
                    + "        \"fri\""
                    + "      ],"
                    + "      \"availableStartTime\": \"09:00:00\","
                    + "      \"availableEndTime\": \"12:00:00\""
                    + "    }"
                    + "  ],"
                    + "  \"notAvailable\": ["
                    + "    {"
                    + "      \"description\": \"Adam will be on extended leave during May 2017\","
                    + "      \"during\": {"
                    + "        \"start\": \"2017-05-01\","
                    + "        \"end\": \"2017-05-20\""
                    + "      }"
                    + "    }"
                    + "  ],"
                    + "  \"availabilityExceptions\":"
                    + "\"Adam is generally unavailable on public holidays\","
                    + "  \"endpoint\": ["
                    + "    {"
                    + "      \"reference\": \"Endpoint/example\""
                    + "    }"
                    + "  ]"
                    + "}";

    public static class PractitionerRoleBuilder
            extends FhirResourceBuilder<PractitionerRoleBuilder> {

        private PractitionerRoleBuilder() {
            super(DEFAULT_PRACTITIONER_ROLE_JSON);
        }

        @Override
        protected PractitionerRoleBuilder returnThis() {
            return this;
        }
    }

    /**
     * Returns a helper class that can build FHIR test data for <a
     * href="https://www.hl7.org/fhir/practitionerrole.html">PractitionerRole</a> resources.
     */
    public static PractitionerRoleBuilder role() {
        return new PractitionerRoleBuilder();
    }

    public PractitionerBuilder() {
        super(DEFAULT_PRACTITIONER_JSON);
    }

    @Override
    protected PractitionerBuilder returnThis() {
        return this;
    }
}
