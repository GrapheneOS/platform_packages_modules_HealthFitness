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

import android.annotation.Nullable;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.datatypes.MedicalResource;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Internal representation of {@link ReadMedicalResourcesResponse}.
 *
 * @hide
 */
public final class ReadMedicalResourcesInternalResponse {
    @Nullable String mPageToken;
    List<MedicalResource> mMedicalResources;
    int mRemainingCount;

    public ReadMedicalResourcesInternalResponse(
            List<MedicalResource> medicalResources,
            @Nullable String pageToken,
            int remainingCount) {
        if (pageToken == null && remainingCount > 0) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "Remaining count must be 0 to have a null next page token, but was %d",
                            remainingCount));
        }
        if (pageToken != null && remainingCount == 0) {
            throw new IllegalArgumentException("Next page token provided with no remaining data");
        }
        mMedicalResources = medicalResources;
        mPageToken = pageToken;
        mRemainingCount = remainingCount;
    }

    /** Returns the {@code mPageToken}. */
    @Nullable
    public String getPageToken() {
        return mPageToken;
    }

    /** Returns the list of {@link MedicalResource}s. */
    public List<MedicalResource> getMedicalResources() {
        return mMedicalResources;
    }

    /**
     * Returns the count of medical resources still remaining which were not returned due to
     * pagination.
     *
     * <p>For a response with a null next page token, this will be 0. This result is accurate at the
     * time the request was made, and with the permissions when the request was made. However, the
     * actual results may change if permissions change or resources are inserted or deleted.
     */
    public int getRemainingCount() {
        return mRemainingCount;
    }

    /** Indicates whether some other object is "equal to" this one. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadMedicalResourcesInternalResponse that)) return false;
        return Objects.equals(mPageToken, that.mPageToken)
                && Objects.equals(mMedicalResources, that.mMedicalResources)
                && mRemainingCount == that.mRemainingCount;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(mPageToken, mMedicalResources, mRemainingCount);
    }
}
