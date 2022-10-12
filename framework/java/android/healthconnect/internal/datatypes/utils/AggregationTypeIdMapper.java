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

package android.healthconnect.internal.datatypes.utils;

import static android.healthconnect.datatypes.HeartRateRecord.BPM_MAX;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_MIN;

import android.annotation.NonNull;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.datatypes.AggregationType;
import android.os.Parcel;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates and maintains a map of {@link AggregationType.AggregationTypeIdentifier} to {@link
 * AggregationType} and its result creator {@link AggregationResultCreator}
 *
 * @hide
 */
public final class AggregationTypeIdMapper {
    private static final int MAP_SIZE = 2;
    private static AggregationTypeIdMapper sAggregationTypeIdMapper;
    private final Map<Integer, AggregationResultCreator> mIdToAggregateResult;
    private final Map<Integer, AggregationType<?>> mIdDataAggregationTypeMap;

    private AggregationTypeIdMapper() {
        mIdToAggregateResult = new HashMap<>(MAP_SIZE);
        mIdToAggregateResult.put(
                BPM_MAX.getAggregationTypeIdentifier(), result -> getLongResult(result.readLong()));
        mIdToAggregateResult.put(
                BPM_MIN.getAggregationTypeIdentifier(), result -> getLongResult(result.readLong()));

        mIdDataAggregationTypeMap = new HashMap<>(MAP_SIZE);
        mIdDataAggregationTypeMap.put(BPM_MIN.getAggregateOperationType(), BPM_MIN);
        mIdDataAggregationTypeMap.put(BPM_MAX.getAggregateOperationType(), BPM_MAX);
    }

    @NonNull
    public static AggregationTypeIdMapper getInstance() {
        if (sAggregationTypeIdMapper == null) {
            sAggregationTypeIdMapper = new AggregationTypeIdMapper();
        }

        return sAggregationTypeIdMapper;
    }

    @NonNull
    public AggregateRecordsResponse.AggregateResult<?> getAggregateResultFor(
            @AggregationType.AggregationTypeIdentifier.Id int id, @NonNull Parcel parcel) {
        return mIdToAggregateResult.get(id).getAggregateResult(parcel);
    }

    @NonNull
    public AggregationType<?> getAggregationTypeFor(
            @AggregationType.AggregationTypeIdentifier.Id int id) {
        return mIdDataAggregationTypeMap.get(id);
    }

    @NonNull
    private AggregateRecordsResponse.AggregateResult<Long> getLongResult(long result) {
        return new AggregateRecordsResponse.AggregateResult<>(result);
    }

    /**
     * Implementation should get and covert result to appropriate type (such as long, double etc.)
     * using {@code result}
     */
    private interface AggregationResultCreator {
        AggregateRecordsResponse.AggregateResult<?> getAggregateResult(Parcel result);
    }
}
