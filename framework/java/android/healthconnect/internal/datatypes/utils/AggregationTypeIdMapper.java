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

import static android.healthconnect.datatypes.ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL;
import static android.healthconnect.datatypes.BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL;
import static android.healthconnect.datatypes.DistanceRecord.DISTANCE_TOTAL;
import static android.healthconnect.datatypes.ElevationGainedRecord.ELEVATION_GAINED_TOTAL;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_AVG;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_MAX;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_MIN;
import static android.healthconnect.datatypes.PowerRecord.POWER_AVG;
import static android.healthconnect.datatypes.PowerRecord.POWER_MAX;
import static android.healthconnect.datatypes.PowerRecord.POWER_MIN;
import static android.healthconnect.datatypes.StepsRecord.COUNT_TOTAL;

import android.annotation.NonNull;
import android.healthconnect.AggregateResult;
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.datatypes.units.Energy;
import android.healthconnect.datatypes.units.Length;
import android.healthconnect.datatypes.units.Power;
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
    private static final int MAP_SIZE = 11;
    private static AggregationTypeIdMapper sAggregationTypeIdMapper;
    private final Map<Integer, AggregationResultCreator> mIdToAggregateResult;
    private final Map<Integer, AggregationType<?>> mIdDataAggregationTypeMap;
    private final Map<AggregationType<?>, Integer> mDataAggregationTypeIdMap;

    private AggregationTypeIdMapper() {
        mIdToAggregateResult = new HashMap<>(MAP_SIZE);
        mIdToAggregateResult.put(
                BPM_MAX.getAggregationTypeIdentifier(), result -> getLongResult(result.readLong()));
        mIdToAggregateResult.put(
                BPM_MIN.getAggregationTypeIdentifier(), result -> getLongResult(result.readLong()));
        mIdToAggregateResult.put(
                COUNT_TOTAL.getAggregationTypeIdentifier(),
                result -> getLongResult(result.readLong()));
        mIdToAggregateResult.put(
                ACTIVE_CALORIES_TOTAL.getAggregationTypeIdentifier(),
                result -> getEnergyResult(result.readDouble()));
        mIdToAggregateResult.put(
                BASAL_CALORIES_TOTAL.getAggregationTypeIdentifier(),
                result -> getPowerResult(result.readDouble()));
        mIdToAggregateResult.put(
                DISTANCE_TOTAL.getAggregationTypeIdentifier(),
                result -> getLengthResult(result.readDouble()));
        mIdToAggregateResult.put(
                ELEVATION_GAINED_TOTAL.getAggregationTypeIdentifier(),
                result -> getLengthResult(result.readDouble()));
        mIdToAggregateResult.put(
                BPM_AVG.getAggregationTypeIdentifier(), result -> getLongResult(result.readLong()));
        mIdToAggregateResult.put(
                POWER_MIN.getAggregationTypeIdentifier(),
                result -> getPowerResult(result.readDouble()));
        mIdToAggregateResult.put(
                POWER_MAX.getAggregationTypeIdentifier(),
                result -> getPowerResult(result.readDouble()));
        mIdToAggregateResult.put(
                POWER_AVG.getAggregationTypeIdentifier(),
                result -> getPowerResult(result.readDouble()));

        mIdDataAggregationTypeMap = new HashMap<>(MAP_SIZE);

        mIdDataAggregationTypeMap.put(BPM_MIN.getAggregationTypeIdentifier(), BPM_MIN);
        mIdDataAggregationTypeMap.put(BPM_MAX.getAggregationTypeIdentifier(), BPM_MAX);
        mIdDataAggregationTypeMap.put(COUNT_TOTAL.getAggregationTypeIdentifier(), COUNT_TOTAL);
        mIdDataAggregationTypeMap.put(
                ACTIVE_CALORIES_TOTAL.getAggregationTypeIdentifier(), ACTIVE_CALORIES_TOTAL);
        mIdDataAggregationTypeMap.put(
                BASAL_CALORIES_TOTAL.getAggregationTypeIdentifier(), BASAL_CALORIES_TOTAL);
        mIdDataAggregationTypeMap.put(
                DISTANCE_TOTAL.getAggregationTypeIdentifier(), DISTANCE_TOTAL);
        mIdDataAggregationTypeMap.put(
                ELEVATION_GAINED_TOTAL.getAggregationTypeIdentifier(), ELEVATION_GAINED_TOTAL);
        mIdDataAggregationTypeMap.put(BPM_AVG.getAggregationTypeIdentifier(), BPM_AVG);
        mIdDataAggregationTypeMap.put(POWER_MIN.getAggregationTypeIdentifier(), POWER_MIN);
        mIdDataAggregationTypeMap.put(POWER_MAX.getAggregationTypeIdentifier(), POWER_MAX);
        mIdDataAggregationTypeMap.put(POWER_AVG.getAggregationTypeIdentifier(), POWER_AVG);

        mDataAggregationTypeIdMap = new HashMap<>(MAP_SIZE);
        mIdDataAggregationTypeMap.forEach(
                (key, value) -> mDataAggregationTypeIdMap.put(value, key));
    }

    @NonNull
    public static AggregationTypeIdMapper getInstance() {
        if (sAggregationTypeIdMapper == null) {
            sAggregationTypeIdMapper = new AggregationTypeIdMapper();
        }

        return sAggregationTypeIdMapper;
    }

    @NonNull
    public AggregateResult<?> getAggregateResultFor(
            @AggregationType.AggregationTypeIdentifier.Id int id, @NonNull Parcel parcel) {
        return mIdToAggregateResult.get(id).getAggregateResult(parcel);
    }

    @NonNull
    public AggregationType<?> getAggregationTypeFor(
            @AggregationType.AggregationTypeIdentifier.Id int id) {
        return mIdDataAggregationTypeMap.get(id);
    }

    @NonNull
    @AggregationType.AggregationTypeIdentifier.Id
    public int getIdFor(AggregationType<?> aggregationType) {
        return mDataAggregationTypeIdMap.get(aggregationType);
    }

    @NonNull
    private AggregateResult<Long> getLongResult(long result) {
        return new AggregateResult<>(result);
    }

    @NonNull
    private AggregateResult<Double> getDoubleResult(double result) {
        return new AggregateResult<>(result);
    }

    @NonNull
    private AggregateResult<Energy> getEnergyResult(double result) {
        return new AggregateResult<>(Energy.fromJoules(result));
    }

    @NonNull
    private AggregateResult<Power> getPowerResult(double result) {
        return new AggregateResult<>(Power.fromWatts(result));
    }

    @NonNull
    private AggregateResult<Length> getLengthResult(double result) {
        return new AggregateResult<>(Length.fromMeters(result));
    }

    /**
     * Implementation should get and covert result to appropriate type (such as long, double etc.)
     * using {@code result}
     */
    private interface AggregationResultCreator {
        AggregateResult<?> getAggregateResult(Parcel result);
    }
}
