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

package com.android.server.healthconnect.phr;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.health.connect.MedicalResourceId;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.datatypes.FhirResource.FhirResourceType;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalResource.MedicalResourceType;

/**
 * Internal representation of {@link UpsertMedicalResourceRequest}.
 *
 * @hide
 */
public final class UpsertMedicalResourceInternalRequest {
    private String mDataSourceId = "";
    private String mData = "";
    @MedicalResourceType private int mMedicalResourceType;
    @FhirResourceType private int mFhirResourceType;
    private String mFhirResourceId = "";
    private String mFhirVersion = "";

    /** Returns The data source ID where the data comes from. */
    public String getDataSourceId() {
        return mDataSourceId;
    }

    /** Returns this object with the data source ID. */
    public UpsertMedicalResourceInternalRequest setDataSourceId(String dataSourceId) {
        requireNonNull(dataSourceId);
        mDataSourceId = dataSourceId;
        return this;
    }

    /** Returns the FHIR resource data in JSON representation. */
    public String getData() {
        return mData;
    }

    /** Returns this object with the FHIR resource data in JSON representation. */
    public UpsertMedicalResourceInternalRequest setData(String data) {
        requireNonNull(data);
        mData = data;
        return this;
    }

    /** Returns the {@code IntDef} {@link MedicalResourceType} of the {@code mData}. */
    @MedicalResourceType
    public int getMedicalResourceType() {
        return mMedicalResourceType;
    }

    /** Returns this object with the medical resource type. */
    public UpsertMedicalResourceInternalRequest setMedicalResourceType(
            @MedicalResourceType int medicalResourceType) {
        mMedicalResourceType = medicalResourceType;
        return this;
    }

    /**
     * Returns the FHIR resource type. This is extracted from the "resourceType" field in {@code
     * mData}, and mapped into an {@code IntDef} {@link FhirResourceType}.
     */
    @FhirResourceType
    public int getFhirResourceType() {
        return mFhirResourceType;
    }

    /** Returns this object with the FHIR resource type. */
    public UpsertMedicalResourceInternalRequest setFhirResourceType(
            @FhirResourceType int fhirResourceType) {
        mFhirResourceType = fhirResourceType;
        return this;
    }

    /** Returns the FHIR resource id extracted from the FHIR JSON. */
    public String getFhirResourceId() {
        return mFhirResourceId;
    }

    /** Returns this object with the FHIR resource id. */
    public UpsertMedicalResourceInternalRequest setFhirResourceId(String fhirResourceId) {
        requireNonNull(fhirResourceId);
        mFhirResourceId = fhirResourceId;
        return this;
    }

    /** Returns the FHIR version as string. */
    public String getFhirVersion() {
        return mFhirVersion;
    }

    /** Returns this object with the FHIR version string. */
    public UpsertMedicalResourceInternalRequest setFhirVersion(FhirVersion fhirVersion) {
        requireNonNull(fhirVersion);
        mFhirVersion = fhirVersion.toString();
        return this;
    }

    /**
     * Creates a {@link MedicalResourceId} from the provided values.
     *
     * @throws IllegalArgumentException if any values are invalid.
     */
    public MedicalResourceId getMedicalResourceId() {
        return new MedicalResourceId(mDataSourceId, mFhirResourceType, mFhirResourceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpsertMedicalResourceInternalRequest that)) return false;
        return getDataSourceId().equals(that.getDataSourceId())
                && getMedicalResourceType() == that.getMedicalResourceType()
                && getFhirResourceType() == that.getFhirResourceType()
                && getFhirResourceId().equals(that.getFhirResourceId())
                && getFhirVersion().equals(that.getFhirVersion())
                && getData().equals(that.getData());
    }

    @Override
    public int hashCode() {
        return hash(
                getDataSourceId(),
                getMedicalResourceType(),
                getFhirResourceType(),
                getFhirResourceId(),
                getFhirVersion(),
                getData());
    }
}
