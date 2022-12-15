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
import static android.healthconnect.datatypes.FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_AVG;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_MAX;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_MIN;
import static android.healthconnect.datatypes.HydrationRecord.VOLUME_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.BIOTIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CAFFEINE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CALCIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CHLORIDE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CHOLESTEROL_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CHROMIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.COPPER_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.DIETARY_FIBER_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.ENERGY_FROM_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.ENERGY_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.FOLATE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.FOLIC_ACID_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.IODINE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.IRON_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MAGNESIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MANGANESE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MOLYBDENUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MONOUNSATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.NIACIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.PANTOTHENIC_ACID_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.PHOSPHORUS_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.POLYUNSATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.POTASSIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.PROTEIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.RIBOFLAVIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SELENIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SODIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SUGAR_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.THIAMIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.TOTAL_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.UNSATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_A_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_B12_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_B6_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_C_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_D_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_E_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_K_TOTAL;
import static android.healthconnect.datatypes.PowerRecord.POWER_AVG;
import static android.healthconnect.datatypes.PowerRecord.POWER_MAX;
import static android.healthconnect.datatypes.PowerRecord.POWER_MIN;
import static android.healthconnect.datatypes.StepsRecord.COUNT_TOTAL;

import android.annotation.NonNull;
import android.healthconnect.AggregateResult;
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.datatypes.units.Energy;
import android.healthconnect.datatypes.units.Length;
import android.healthconnect.datatypes.units.Mass;
import android.healthconnect.datatypes.units.Power;
import android.healthconnect.datatypes.units.Volume;
import android.os.Parcel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and maintains a map of {@link AggregationType.AggregationTypeIdentifier} to {@link
 * AggregationType} and its result creator {@link AggregationResultCreator}
 *
 * @hide
 */
public final class AggregationTypeIdMapper {
    private static final int MAP_SIZE = 53;
    private static AggregationTypeIdMapper sAggregationTypeIdMapper;
    private final Map<Integer, AggregationResultCreator> mIdToAggregateResult;
    private final Map<Integer, AggregationType<?>> mIdDataAggregationTypeMap;
    private final Map<AggregationType<?>, Integer> mDataAggregationTypeIdMap;

    private AggregationTypeIdMapper() {
        mIdToAggregateResult = new HashMap<>(MAP_SIZE);
        mIdDataAggregationTypeMap = new HashMap<>(MAP_SIZE);
        mDataAggregationTypeIdMap = new HashMap<>(MAP_SIZE);

        addLongIdsToAggregateResultMap(
                Arrays.asList(BPM_MAX, BPM_MIN, COUNT_TOTAL, BPM_AVG, FLOORS_CLIMBED_TOTAL));
        addPowerIdsToAggregateResultMap(
                Arrays.asList(BASAL_CALORIES_TOTAL, POWER_MIN, POWER_MAX, POWER_AVG));
        addEnergyIdsToAggregateResultMap(
                Arrays.asList(ACTIVE_CALORIES_TOTAL, ENERGY_TOTAL, ENERGY_FROM_FAT_TOTAL));
        addVolumeIdsToAggregateResultMap(Arrays.asList(VOLUME_TOTAL));
        addLengthIdsToAggregateResultMap(Arrays.asList(DISTANCE_TOTAL, ELEVATION_GAINED_TOTAL));
        addMassIdsToAggregateResultMap(
                Arrays.asList(
                        BIOTIN_TOTAL,
                        CAFFEINE_TOTAL,
                        CALCIUM_TOTAL,
                        CHLORIDE_TOTAL,
                        CHOLESTEROL_TOTAL,
                        CHROMIUM_TOTAL,
                        COPPER_TOTAL,
                        DIETARY_FIBER_TOTAL,
                        FOLATE_TOTAL,
                        FOLIC_ACID_TOTAL,
                        IODINE_TOTAL,
                        IRON_TOTAL,
                        MAGNESIUM_TOTAL,
                        MANGANESE_TOTAL,
                        MOLYBDENUM_TOTAL,
                        MONOUNSATURATED_FAT_TOTAL,
                        NIACIN_TOTAL,
                        PANTOTHENIC_ACID_TOTAL,
                        PHOSPHORUS_TOTAL,
                        POLYUNSATURATED_FAT_TOTAL,
                        POTASSIUM_TOTAL,
                        PROTEIN_TOTAL,
                        RIBOFLAVIN_TOTAL,
                        SATURATED_FAT_TOTAL,
                        SELENIUM_TOTAL,
                        SODIUM_TOTAL,
                        SUGAR_TOTAL,
                        THIAMIN_TOTAL,
                        TOTAL_CARBOHYDRATE_TOTAL,
                        TOTAL_FAT_TOTAL,
                        UNSATURATED_FAT_TOTAL,
                        VITAMIN_A_TOTAL,
                        VITAMIN_B12_TOTAL,
                        VITAMIN_B6_TOTAL,
                        VITAMIN_C_TOTAL,
                        VITAMIN_D_TOTAL,
                        VITAMIN_E_TOTAL,
                        VITAMIN_K_TOTAL));
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

    @NonNull
    private AggregateResult<Volume> getVolumeResult(double result) {
        return new AggregateResult<>(Volume.fromMilliliters(result));
    }

    @NonNull
    private AggregateResult<Mass> getMassResult(double result) {
        return new AggregateResult<>(Mass.fromKilograms(result));
    }

    private void addLongIdsToAggregateResultMap(
            @NonNull List<AggregationType<?>> aggregationTypeList) {
        for (AggregationType<?> aggregationType : aggregationTypeList) {
            mIdToAggregateResult.put(
                    aggregationType.getAggregationTypeIdentifier(),
                    result -> getLongResult(result.readLong()));
            populateIdDataAggregationType(aggregationType);
        }
    }

    private void addEnergyIdsToAggregateResultMap(
            @NonNull List<AggregationType<?>> aggregationTypeList) {
        for (AggregationType<?> aggregationType : aggregationTypeList) {
            mIdToAggregateResult.put(
                    aggregationType.getAggregationTypeIdentifier(),
                    result -> getEnergyResult(result.readDouble()));
            populateIdDataAggregationType(aggregationType);
        }
    }

    private void addPowerIdsToAggregateResultMap(
            @NonNull List<AggregationType<?>> aggregationTypeList) {
        for (AggregationType<?> aggregationType : aggregationTypeList) {
            mIdToAggregateResult.put(
                    aggregationType.getAggregationTypeIdentifier(),
                    result -> getPowerResult(result.readDouble()));
            populateIdDataAggregationType(aggregationType);
        }
    }

    private void addLengthIdsToAggregateResultMap(
            @NonNull List<AggregationType<?>> aggregationTypeList) {
        for (AggregationType<?> aggregationType : aggregationTypeList) {
            mIdToAggregateResult.put(
                    aggregationType.getAggregationTypeIdentifier(),
                    result -> getLengthResult(result.readDouble()));
            populateIdDataAggregationType(aggregationType);
        }
    }

    private void addVolumeIdsToAggregateResultMap(
            @NonNull List<AggregationType<?>> aggregationTypeList) {
        for (AggregationType<?> aggregationType : aggregationTypeList) {
            mIdToAggregateResult.put(
                    aggregationType.getAggregationTypeIdentifier(),
                    result -> getVolumeResult(result.readDouble()));
            populateIdDataAggregationType(aggregationType);
        }
    }

    private void addMassIdsToAggregateResultMap(
            @NonNull List<AggregationType<?>> aggregationTypeList) {
        for (AggregationType<?> aggregationType : aggregationTypeList) {
            mIdToAggregateResult.put(
                    aggregationType.getAggregationTypeIdentifier(),
                    result -> getMassResult(result.readDouble()));
            populateIdDataAggregationType(aggregationType);
        }
    }

    private void populateIdDataAggregationType(AggregationType<?> aggregationType) {
        mIdDataAggregationTypeMap.put(
                aggregationType.getAggregationTypeIdentifier(), aggregationType);
        mDataAggregationTypeIdMap.put(
                aggregationType, aggregationType.getAggregationTypeIdentifier());
    }

    /**
     * Implementation should get and covert result to appropriate type (such as long, double etc.)
     * using {@code result}
     */
    private interface AggregationResultCreator {
        AggregateResult<?> getAggregateResult(Parcel result);
    }
}
