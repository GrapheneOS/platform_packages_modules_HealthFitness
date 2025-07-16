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

package android.health.connect;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a request to determine if there are matching applications for a given set of record
 * types and/or package name.
 *
 * @hide
 */
public final class GetMatchingAppsRequest implements Parcelable {
    @NonNull private final Set<Class<? extends Record>> mRecordTypes;
    @Nullable private final String mPackageName;

    /**
     * Private constructor to create a {@link GetMatchingAppsRequest} instance. Use the {@link
     * Builder} to construct new instances.
     *
     * @param recordTypes The set of record types for which to find matching applications.
     * @param packageName The package name for which to find matching applications, null if not
     *     specified.
     */
    private GetMatchingAppsRequest(
            @NonNull Set<Class<? extends Record>> recordTypes, @Nullable String packageName) {
        mRecordTypes = Set.copyOf(recordTypes);
        mPackageName = packageName;
    }

    /**
     * Private constructor to reconstruct a {@link GetMatchingAppsRequest} from a {@link Parcel}.
     *
     * @param in The Parcel from which to read the object data.
     */
    private GetMatchingAppsRequest(@NonNull Parcel in) {
        requireNonNull(in);
        int[] recordTypeIds = requireNonNull(in.createIntArray());
        mRecordTypes =
                Arrays.stream(recordTypeIds)
                        .mapToObj(
                                recordTypeId ->
                                        HealthConnectMappings.getInstance()
                                                .getRecordIdToExternalRecordClassMap()
                                                .get(recordTypeId))
                        .collect(Collectors.toUnmodifiableSet());
        mPackageName = in.readString();
    }

    @NonNull
    public static final Creator<GetMatchingAppsRequest> CREATOR =
            new Creator<>() {
                @Override
                public GetMatchingAppsRequest createFromParcel(Parcel in) {
                    return new GetMatchingAppsRequest(in);
                }

                @Override
                public GetMatchingAppsRequest[] newArray(int size) {
                    return new GetMatchingAppsRequest[size];
                }
            };

    @NonNull
    public Set<Class<? extends Record>> getRecordTypes() {
        return mRecordTypes;
    }

    @Nullable
    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeIntArray(
                mRecordTypes.stream()
                        .mapToInt(
                                recordTypeClass ->
                                        HealthConnectMappings.getInstance()
                                                .getRecordType(recordTypeClass))
                        .toArray());
        dest.writeString(mPackageName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetMatchingAppsRequest that)) return false;
        return mRecordTypes.equals(that.mRecordTypes)
                && Objects.equals(mPackageName, that.mPackageName);
    }

    @Override
    public int hashCode() {
        return hash(mRecordTypes, mPackageName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append(",recordTypes=").append(mRecordTypes);
        sb.append(",packageName=").append(mPackageName);
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link GetMatchingAppsRequest}. */
    public static final class Builder {
        private final Set<Class<? extends Record>> mRecordTypes = new HashSet<>();
        @Nullable private String mPackageName;

        /**
         * Adds a record type to the request.
         *
         * @param recordType The record type to add.
         * @return This builder.
         */
        @NonNull
        public Builder addRecordType(@NonNull Class<? extends Record> recordType) {
            mRecordTypes.add(requireNonNull(recordType));
            return this;
        }

        /**
         * Adds a list of record types to the request.
         *
         * @param recordTypes The list of record types to add.
         * @return This builder.
         */
        @NonNull
        public Builder addRecordTypes(@NonNull Set<Class<? extends Record>> recordTypes) {
            mRecordTypes.addAll(requireNonNull(recordTypes));
            return this;
        }

        /**
         * Sets the package name for the request.
         *
         * @param packageName The package name to set.
         * @return This builder.
         */
        @NonNull
        public Builder setPackageName(@NonNull String packageName) {
            mPackageName = requireNonNull(packageName);
            return this;
        }

        /**
         * Builds the {@link GetMatchingAppsRequest} instance.
         *
         * @return A new instance of {@link GetMatchingAppsRequest}.
         */
        @NonNull
        public GetMatchingAppsRequest build() {
            return new GetMatchingAppsRequest(mRecordTypes, mPackageName);
        }
    }
}
