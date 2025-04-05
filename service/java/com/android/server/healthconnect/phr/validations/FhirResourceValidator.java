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

import static android.health.connect.datatypes.FhirResource.FhirResourceType;

import android.health.connect.datatypes.FhirVersion;

import org.json.JSONObject;

/**
 * Performs validation on a FHIR JSON Object, based on the FHIR version R4.
 *
 * @hide
 */
public class FhirResourceValidator {

    private final FhirObjectTypeValidator mFhirObjectTypeValidator;

    public FhirResourceValidator() {
        // TODO: b/374058373 - When we support R5 or other versions this needs to be updated to
        //  support other fhir versions.
        FhirSpecProvider fhirSpec = new FhirSpecProvider(FhirVersion.parseFhirVersion("4.0.1"));

        mFhirObjectTypeValidator = new FhirObjectTypeValidator(fhirSpec);
    }

    /**
     * Validates the provided {@code fhirJsonObject} against the schema of the provided {@code
     * fhirResourceType} and {@code fhirVersion}.
     *
     * @throws IllegalArgumentException if the resource is invalid.
     */
    public void validateFhirResource(
            JSONObject fhirJsonObject,
            @FhirResourceType int fhirResourceType,
            FhirVersion fhirVersion) {
        mFhirObjectTypeValidator.validate(fhirJsonObject, fhirResourceType, fhirVersion);
    }
}
