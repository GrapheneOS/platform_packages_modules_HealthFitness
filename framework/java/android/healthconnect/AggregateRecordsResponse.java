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
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.os.Parcel;
import android.util.ArrayMap;

import java.util.Map;
import java.util.Objects;

/**
 * A class representing response for {@link HealthConnectManager#aggregate}
 */
public final class AggregateRecordsResponse<T> {
    private final Map<AggregationType<T>, AggregateResult<T>> mAggregateResults;

    /**
     * We only support querying and fetching same type of aggregations, so we can cast blindly
     *
     * @hide
     */
    @SuppressWarnings("unchecked")
    public AggregateRecordsResponse(@NonNull Map<Integer, AggregateResult<?>> aggregateResults) {
        Objects.requireNonNull(aggregateResults);

        mAggregateResults = new ArrayMap<>(aggregateResults.size());
        aggregateResults.forEach(
                (key, value) ->
                        mAggregateResults.put(
                                (AggregationType<T>)
                                        AggregationTypeIdMapper.getInstance()
                                                .getAggregationTypeFor(key),
                                (AggregateResult<T>) value));
    }

    /**
     * @return a map of {@link AggregationType} -> {@link AggregateResult}
     * @hide
     */
    @NonNull
    public Map<AggregationType<T>, AggregateResult<T>> getAggregateResults() {
        return mAggregateResults;
    }

    /**
     * @return an aggregation result for {@code aggregationType}. *
     * @param aggregationType {@link AggregationType} for which to get the result
     */
    @Nullable
    public T get(@NonNull AggregationType<T> aggregationType) {
        Objects.requireNonNull(aggregationType);
        AggregateResult<T> result = mAggregateResults.get(aggregationType);

        if (result == null) {
            return null;
        }

        return result.getResult();
    }

    /**
     * A class to represent the results of {@link HealthConnectManager} aggregate APIs
     *
     * @hide
     */
    public static final class AggregateResult<T> {
        private final T mResult;

        /** Creates {@link AggregateResult}'s object with a long value */
        public AggregateResult(T result) {
            mResult = result;
        }

        public void putToParcel(@NonNull Parcel parcel) {
            if (mResult instanceof Long) {
                parcel.writeLong((Long) mResult);
            }
        }

        /**
         * @return an Object representing the result of an aggregation.
         */
        @NonNull
        T getResult() {
            return mResult;
        }
    }
}
