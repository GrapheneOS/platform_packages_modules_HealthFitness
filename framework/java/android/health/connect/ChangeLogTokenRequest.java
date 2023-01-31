/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.NonNull;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.util.ArraySet;

import java.util.Objects;
import java.util.Set;

/**
 * A class to request changelog token using {@link HealthConnectManager#getChangeLogToken}
 *
 * @see HealthConnectManager#getChangeLogToken
 */
public final class ChangeLogTokenRequest {
    /** Builder for {@link ChangeLogTokenRequest} */
    public static final class Builder {
        private final Set<Class<? extends Record>> mRecordTypes = new ArraySet<>();
        private final Set<DataOrigin> mDataOriginFilters = new ArraySet<>();

        /**
         * @param recordType type of record for which change log is required. If not set includes
         *     all record types
         */
        @NonNull
        public Builder addRecordType(@NonNull Class<? extends Record> recordType) {
            Objects.requireNonNull(recordType);

            mRecordTypes.add(recordType);
            return this;
        }

        /**
         * @param dataOriginFilter list of package names on which to filter the data.
         *     <p>If not set logs from all the sources will be returned
         */
        @NonNull
        public Builder addDataOriginFilter(@NonNull DataOrigin dataOriginFilter) {
            Objects.requireNonNull(dataOriginFilter);

            mDataOriginFilters.add(dataOriginFilter);
            return this;
        }

        /**
         * @return Object of {@link ChangeLogTokenRequest}
         */
        @NonNull
        public ChangeLogTokenRequest build() {
            return new ChangeLogTokenRequest(mDataOriginFilters, mRecordTypes);
        }
    }

    private final Set<DataOrigin> mDataOriginFilters;
    private final Set<Class<? extends Record>> mRecordTypes;

    /**
     * @param dataOriginFilters list of package names to filter the data
     * @param recordTypes list of records for which change log is required
     */
    private ChangeLogTokenRequest(
            @NonNull Set<DataOrigin> dataOriginFilters,
            @NonNull Set<Class<? extends Record>> recordTypes) {
        Objects.requireNonNull(recordTypes);
        Objects.requireNonNull(dataOriginFilters);

        mDataOriginFilters = dataOriginFilters;
        mRecordTypes = recordTypes;
    }

    /**
     * @return list of package names corresponding to which the logs are required
     */
    @NonNull
    public Set<DataOrigin> getDataOriginFilters() {
        return mDataOriginFilters;
    }

    /**
     * @return list of record classes for which the logs are to be fetched
     */
    @NonNull
    public Set<Class<? extends Record>> getRecordTypes() {
        return mRecordTypes;
    }
}
