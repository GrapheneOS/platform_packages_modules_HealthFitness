/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Record;
import android.util.ArraySet;

import java.util.Objects;
import java.util.Set;

/**
 * A delete request based on record type and/or time and/or data origin. This is only for controller
 * APK to use
 *
 * @hide
 */
@SystemApi
public class DeleteUsingFiltersRequest {
    private final TimeRangeFilter mTimeRangeFilter;
    private final Set<Class<? extends Record>> mRecordTypes;
    private final Set<DataOrigin> mDataOrigins;
    /**
     * @see Builder
     */
    private DeleteUsingFiltersRequest(
            @Nullable TimeRangeFilter timeRangeFilter,
            @NonNull Set<Class<? extends Record>> recordTypes,
            @NonNull Set<DataOrigin> dataOrigins) {
        Objects.requireNonNull(recordTypes);
        Objects.requireNonNull(dataOrigins);

        mTimeRangeFilter = timeRangeFilter;
        mRecordTypes = recordTypes;
        mDataOrigins = dataOrigins;
    }

    /** Returns record types on which delete is to be performed */
    @NonNull
    public Set<Class<? extends Record>> getRecordTypes() {
        return mRecordTypes;
    }

    /**
     * @return time range b/w which the delete operation is to be performed
     */
    @Nullable
    public TimeRangeFilter getTimeRangeFilter() {
        return mTimeRangeFilter;
    }

    /**
     * @return list of {@link DataOrigin}s to delete, or empty list for no filter
     */
    @NonNull
    public Set<DataOrigin> getDataOrigins() {
        return mDataOrigins;
    }

    /** Builder class for {@link DeleteUsingFiltersRequest} */
    public static final class Builder {
        private final Set<DataOrigin> mDataOrigins = new ArraySet<>();
        private final Set<Class<? extends Record>> mRecordTypes = new ArraySet<>();
        private TimeRangeFilter mTimeRangeFilter;

        /**
         * @param dataOrigin List of {@link DataOrigin}s to delete, or empty list for no filter
         */
        @NonNull
        public Builder addDataOrigin(@NonNull DataOrigin dataOrigin) {
            Objects.requireNonNull(dataOrigin);

            mDataOrigins.add(dataOrigin);
            return this;
        }

        /**
         * @param timeRangeFilter Time range b/w which the delete operation is to be performed
         */
        @NonNull
        public Builder setTimeRangeFilter(@Nullable TimeRangeFilter timeRangeFilter) {
            mTimeRangeFilter = timeRangeFilter;
            return this;
        }

        /**
         * @param recordType List of {@link Record} classes that on which to perform delete. Empty
         *     list means no filter
         */
        @NonNull
        public Builder addRecordType(@NonNull Class<? extends Record> recordType) {
            Objects.requireNonNull(recordType);

            mRecordTypes.add(recordType);
            return this;
        }

        /**
         * @return Object of {@link DeleteUsingFiltersRequest}
         */
        @NonNull
        public DeleteUsingFiltersRequest build() {
            return new DeleteUsingFiltersRequest(mTimeRangeFilter, mRecordTypes, mDataOrigins);
        }
    }
}
