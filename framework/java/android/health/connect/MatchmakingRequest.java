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

import static com.android.healthfitness.flags.Flags.FLAG_MATCHMAKING;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.OutcomeReceiver;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Represents a request to determine if there are matching data sources for a given set of record
 * types. This request is used with {@link
 * HealthConnectManager#isMatchmakingPossible(MatchmakingRequest, Executor, OutcomeReceiver)} and
 * {@link HealthConnectManager#createMatchmakingIntent(MatchmakingRequest)}.
 */
@FlaggedApi(FLAG_MATCHMAKING)
public final class MatchmakingRequest implements Parcelable {
    @NonNull private final Set<Class<? extends Record>> mRecordTypes;
    @Nullable private final String mCallingPackageName;

    /**
     * Private constructor to create a {@link MatchmakingRequest} instance. Use the {@link Builder}
     * to construct new instances.
     *
     * @param recordTypes The set of record types for which to find matching data sources.
     * @param callingPackageName The package name that initiated matchmaking. Can be null if not
     *     specified. This parameter is only intended to be set by the Health Connect controller
     *     APK.
     */
    private MatchmakingRequest(
            @NonNull Set<Class<? extends Record>> recordTypes,
            @Nullable String callingPackageName) {
        mRecordTypes = Set.copyOf(recordTypes);
        mCallingPackageName = callingPackageName;
    }

    /**
     * Private constructor to reconstruct a {@link MatchmakingRequest} from a {@link Parcel}.
     *
     * @param in The Parcel from which to read the object data.
     */
    private MatchmakingRequest(@NonNull Parcel in) {
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
        mCallingPackageName = in.readString();
    }

    @NonNull
    public static final Creator<MatchmakingRequest> CREATOR =
            new Creator<>() {
                @Override
                public MatchmakingRequest createFromParcel(Parcel in) {
                    return new MatchmakingRequest(in);
                }

                @Override
                public MatchmakingRequest[] newArray(int size) {
                    return new MatchmakingRequest[size];
                }
            };

    /** Returns the set of record types for which matching data sources are being requested. */
    @NonNull
    public Set<Class<? extends Record>> getRecordTypes() {
        return mRecordTypes;
    }

    /** @hide */
    @Nullable
    public String getCallingPackageName() {
        return mCallingPackageName;
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
        dest.writeString(mCallingPackageName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchmakingRequest that)) return false;
        return mRecordTypes.equals(that.mRecordTypes)
                && Objects.equals(mCallingPackageName, that.mCallingPackageName);
    }

    @Override
    public int hashCode() {
        return hash(mRecordTypes, mCallingPackageName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append(",recordTypes=").append(mRecordTypes);
        sb.append(",callingPackageName=").append(mCallingPackageName);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Builder class for {@link MatchmakingRequest}.
     *
     * <p>By default, a builder initializes with an empty set of record types.
     */
    public static final class Builder {
        private final Set<Class<? extends Record>> mRecordTypes = new HashSet<>();
        @Nullable private String mCallingPackageName;

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
         * Sets the calling package name for the request. This is only to be set by Health Connect
         * controller APK(s).
         *
         * @param callingPackageName The package name to set.
         * @return This builder.
         * @hide
         */
        @NonNull
        public Builder setCallingPackageName(@NonNull String callingPackageName) {
            mCallingPackageName = requireNonNull(callingPackageName);
            return this;
        }

        /**
         * Builds the {@link MatchmakingRequest} instance.
         *
         * <p>By default, {@code MatchmakingRequest.Builder().build()} creates a request with an
         * empty set of record types. See description at {@link
         * HealthConnectManager#createMatchmakingIntent(MatchmakingRequest)}.
         *
         * @return A new instance of {@link MatchmakingRequest}.
         */
        @NonNull
        public MatchmakingRequest build() {
            return new MatchmakingRequest(mRecordTypes, mCallingPackageName);
        }
    }
}
