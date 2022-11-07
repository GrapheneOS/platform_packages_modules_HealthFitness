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
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Record;
import android.os.OutcomeReceiver;
import android.util.ArraySet;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Class to represent a request for {@link
 * HealthConnectManager#readRecords(ReadRecordsRequestUsingFilters, Executor, OutcomeReceiver)}
 */
public final class ReadRecordsRequestUsingFilters<T extends Record> {
    /** Builder class for {@link ReadRecordsRequestUsingFilters} */
    public static final class Builder<T extends Record> {
        private final Class<T> mRecordType;
        private final Set<DataOrigin> mDataOrigins = new ArraySet<>();
        private TimeRangeFilter mTimeRangeFilter;

        /**
         * @param recordType Class object of {@link Record} type that needs to be read
         */
        public Builder(@NonNull Class<T> recordType) {
            mRecordType = recordType;
        }

        /**
         * @param dataOrigin Adds {@link DataOrigin} for which to read records.
         *     <p>If no {@link DataOrigin} is added then records by all {@link DataOrigin}s will be
         *     read
         */
        @NonNull
        public Builder<T> addDataOrigins(@NonNull DataOrigin dataOrigin) {
            Objects.requireNonNull(dataOrigin);
            mDataOrigins.add(dataOrigin);

            return this;
        }

        /**
         * @param timeRangeFilter Time range b/w which the read operation is to be performed.
         *     <p>If not time range filter is present all the records will be read without any time
         *     constraints.
         */
        @NonNull
        public Builder<T> setTimeRangeFilter(@Nullable TimeRangeFilter timeRangeFilter) {
            mTimeRangeFilter = timeRangeFilter;
            return this;
        }

        /**
         * @return Object of {@link ReadRecordsRequestUsingFilters}
         */
        @NonNull
        public ReadRecordsRequestUsingFilters<T> build() {
            return new ReadRecordsRequestUsingFilters<>(
                    mTimeRangeFilter, mRecordType, mDataOrigins);
        }
    }

    private final TimeRangeFilter mTimeRangeFilter;
    private final Class<T> mRecordType;
    private final Set<DataOrigin> mDataOrigins;

    /**
     * @see Builder
     */
    private ReadRecordsRequestUsingFilters(
            @NonNull TimeRangeFilter timeRangeFilter,
            @NonNull Class<T> recordType,
            @NonNull Set<DataOrigin> dataOrigins) {
        Objects.requireNonNull(recordType);
        Objects.requireNonNull(dataOrigins);

        mTimeRangeFilter = timeRangeFilter;
        mRecordType = recordType;
        mDataOrigins = dataOrigins;
    }

    /**
     * @return record type on which read is to be performed
     */
    @NonNull
    public Class<T> getRecordType() {
        return mRecordType;
    }

    /**
     * @return time range b/w which the read operation is to be performed
     */
    @Nullable
    public TimeRangeFilter getTimeRangeFilter() {
        return mTimeRangeFilter;
    }

    /**
     * @return list of {@link DataOrigin}s to be read, or empty list for no filter
     */
    @NonNull
    public Set<DataOrigin> getDataOrigins() {
        return mDataOrigins;
    }
}
