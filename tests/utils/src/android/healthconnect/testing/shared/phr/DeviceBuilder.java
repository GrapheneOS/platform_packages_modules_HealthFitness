/*
 * Copyright (C) 2025 The Android Open Source Project
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

public class DeviceBuilder extends FhirResourceBuilder<DeviceBuilder> {
    private static final String DEFAULT_JSON =
            """
            {
              "resourceType": "Device",
              "id": "f001",
              "text": {
                "status": "generated",
                "div": "<div xmlns=\\"http://www.w3.org/1999/xhtml\\"><p><b>
                Generated Narrative with Details</b></p><p><b>id</b>: f001</p><p><b>identifier</b>:
                12345</p><p><b>status</b>: active</p></div>"
              },
              "identifier": [
                {
                  "system": "http:/goodhealthhospital/identifier/devices",
                  "value": "12345"
                }
              ],
              "status": "active"
            }""";

    /**
     * Creates a default valid FHIR Device.
     *
     * <p>All that should be relied on is that the Device is valid. To rely on anything else set it
     * with the other methods.
     */
    public DeviceBuilder() {
        super(DEFAULT_JSON);
    }

    @Override
    protected DeviceBuilder returnThis() {
        return this;
    }
}
