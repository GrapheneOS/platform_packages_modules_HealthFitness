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
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.util.ArrayMap;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/** @hide */
public final class AggregateRecordsGroupedByPeriodResponse<T> {
    private final LocalDateTime mStartTime;
    private final LocalDateTime mEndTime;
    private final Map<AggregationType<T>, AggregateResult<T>> mResult;

    /** @hide */
    @SuppressWarnings("unchecked")
    public AggregateRecordsGroupedByPeriodResponse(
            @NonNull LocalDateTime startTime,
            @NonNull LocalDateTime endTime,
            @NonNull Map<Integer, AggregateResult<?>> result) {
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
        Objects.requireNonNull(result);

        mStartTime = startTime;
        mEndTime = endTime;
        mResult = new ArrayMap<>(result.size());
        result.forEach(
                (key, value) ->
                        mResult.put(
                                (AggregationType<T>)
                                        AggregationTypeIdMapper.getInstance()
                                                .getAggregationTypeFor(key),
                                (AggregateResult<T>) value));
    }

    @NonNull
    public LocalDateTime getStartTime() {
        return mStartTime;
    }

    @NonNull
    public LocalDateTime getEndTime() {
        return mEndTime;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public T get(@NonNull AggregationType<T> aggregationType) {
        Objects.requireNonNull(aggregationType);

        AggregateResult<T> result = mResult.get(aggregationType);

        if (result == null) {
            return null;
        }

        return result.getResult();
    }
}
