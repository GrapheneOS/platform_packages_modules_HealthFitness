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
package com.android.server.healthconnect.fitness.recordhelpers;

import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_BIOTIN_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CAFFEINE_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CALCIUM_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CHLORIDE_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CHOLESTEROL_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CHROMIUM_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_COPPER_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_DIETARY_FIBER_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_ENERGY_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_FOLATE_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_FOLIC_ACID_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_IODINE_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_IRON_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MAGNESIUM_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MANGANESE_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MOLYBDENUM_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_NIACIN_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_PANTOTHENIC_ACID_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_PHOSPHORUS_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_POLYUNSATURATED_FAT_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_POTASSIUM_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_PROTEIN_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_RIBOFLAVIN_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_SATURATED_FAT_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_SELENIUM_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_SODIUM_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_SUGAR_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_THIAMIN_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_TOTAL_CARBOHYDRATE_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_TOTAL_FAT_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_TRANS_FAT_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_UNSATURATED_FAT_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_VITAMIN_A_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_VITAMIN_B12_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_VITAMIN_B6_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_VITAMIN_C_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_VITAMIN_D_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_VITAMIN_E_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_VITAMIN_K_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_ZINC_TOTAL;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.REAL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.AggregateResult;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.NutritionRecordInternal;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.fitness.aggregation.AggregateParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Helper class for NutritionRecord.
 *
 * @hide
 */
public final class NutritionRecordHelper extends IntervalRecordHelper<NutritionRecordInternal> {
    @VisibleForTesting static final String NUTRITION_RECORD_TABLE_NAME = "nutrition_record_table";
    @VisibleForTesting static final String UNSATURATED_FAT_COLUMN_NAME = "unsaturated_fat";
    @VisibleForTesting static final String POTASSIUM_COLUMN_NAME = "potassium";
    @VisibleForTesting static final String THIAMIN_COLUMN_NAME = "thiamin";
    @VisibleForTesting static final String MEAL_TYPE_COLUMN_NAME = "meal_type";
    @VisibleForTesting static final String TRANS_FAT_COLUMN_NAME = "trans_fat";
    @VisibleForTesting static final String MANGANESE_COLUMN_NAME = "manganese";
    @VisibleForTesting static final String ENERGY_FROM_FAT_COLUMN_NAME = "energy_from_fat";
    @VisibleForTesting static final String CAFFEINE_COLUMN_NAME = "caffeine";
    @VisibleForTesting static final String DIETARY_FIBER_COLUMN_NAME = "dietary_fiber";
    @VisibleForTesting static final String SELENIUM_COLUMN_NAME = "selenium";
    @VisibleForTesting static final String VITAMIN_B6_COLUMN_NAME = "vitamin_b6";
    @VisibleForTesting static final String PROTEIN_COLUMN_NAME = "protein";
    @VisibleForTesting static final String CHLORIDE_COLUMN_NAME = "chloride";
    @VisibleForTesting static final String CHOLESTEROL_COLUMN_NAME = "cholesterol";
    @VisibleForTesting static final String COPPER_COLUMN_NAME = "copper";
    @VisibleForTesting static final String IODINE_COLUMN_NAME = "iodine";
    @VisibleForTesting static final String VITAMIN_B12_COLUMN_NAME = "vitamin_b12";
    @VisibleForTesting static final String ZINC_COLUMN_NAME = "zinc";
    @VisibleForTesting static final String RIBOFLAVIN_COLUMN_NAME = "riboflavin";
    @VisibleForTesting static final String ENERGY_COLUMN_NAME = "energy";
    @VisibleForTesting static final String MOLYBDENUM_COLUMN_NAME = "molybdenum";
    @VisibleForTesting static final String PHOSPHORUS_COLUMN_NAME = "phosphorus";
    @VisibleForTesting static final String CHROMIUM_COLUMN_NAME = "chromium";
    @VisibleForTesting static final String TOTAL_FAT_COLUMN_NAME = "total_fat";
    @VisibleForTesting static final String CALCIUM_COLUMN_NAME = "calcium";
    @VisibleForTesting static final String VITAMIN_C_COLUMN_NAME = "vitamin_c";
    @VisibleForTesting static final String VITAMIN_E_COLUMN_NAME = "vitamin_e";
    @VisibleForTesting static final String BIOTIN_COLUMN_NAME = "biotin";
    @VisibleForTesting static final String VITAMIN_D_COLUMN_NAME = "vitamin_d";
    @VisibleForTesting static final String NIACIN_COLUMN_NAME = "niacin";
    @VisibleForTesting static final String MAGNESIUM_COLUMN_NAME = "magnesium";
    @VisibleForTesting static final String TOTAL_CARBOHYDRATE_COLUMN_NAME = "total_carbohydrate";
    @VisibleForTesting static final String VITAMIN_K_COLUMN_NAME = "vitamin_k";
    @VisibleForTesting static final String POLYUNSATURATED_FAT_COLUMN_NAME = "polyunsaturated_fat";
    @VisibleForTesting static final String SATURATED_FAT_COLUMN_NAME = "saturated_fat";
    @VisibleForTesting static final String SODIUM_COLUMN_NAME = "sodium";
    @VisibleForTesting static final String FOLATE_COLUMN_NAME = "folate";
    @VisibleForTesting static final String MONOUNSATURATED_FAT_COLUMN_NAME = "monounsaturated_fat";
    @VisibleForTesting static final String PANTOTHENIC_ACID_COLUMN_NAME = "pantothenic_acid";
    private static final String MEAL_NAME_COLUMN_NAME = "meal_name";
    @VisibleForTesting static final String IRON_COLUMN_NAME = "iron";
    @VisibleForTesting static final String VITAMIN_A_COLUMN_NAME = "vitamin_a";
    @VisibleForTesting static final String FOLIC_ACID_COLUMN_NAME = "folic_acid";
    @VisibleForTesting static final String SUGAR_COLUMN_NAME = "sugar";

    public NutritionRecordHelper() {
        super(RecordTypeIdentifier.RECORD_TYPE_NUTRITION);
    }

    @Override
    @Nullable
    public AggregateResult<?> getNoPriorityAggregateResult(
            Cursor results, AggregationType<?> aggregationType, Set<DataOrigin> dataOrigins) {
        double aggregateValue;
        switch (aggregationType.getAggregationTypeIdentifier()) {
            case NUTRITION_RECORD_BIOTIN_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(BIOTIN_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_CAFFEINE_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(CAFFEINE_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_CALCIUM_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(CALCIUM_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_CHLORIDE_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(CHLORIDE_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_CHOLESTEROL_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(CHOLESTEROL_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_CHROMIUM_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(CHROMIUM_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_COPPER_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(COPPER_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_DIETARY_FIBER_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(DIETARY_FIBER_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_ENERGY_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(ENERGY_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(ENERGY_FROM_FAT_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_FOLATE_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(FOLATE_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_FOLIC_ACID_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(FOLIC_ACID_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_IODINE_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(IODINE_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_IRON_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(IRON_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_MAGNESIUM_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(MAGNESIUM_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_MANGANESE_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(MANGANESE_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_MOLYBDENUM_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(MOLYBDENUM_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(MONOUNSATURATED_FAT_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_NIACIN_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(NIACIN_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_PANTOTHENIC_ACID_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(PANTOTHENIC_ACID_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_PHOSPHORUS_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(PHOSPHORUS_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_POLYUNSATURATED_FAT_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(POLYUNSATURATED_FAT_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_POTASSIUM_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(POTASSIUM_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_PROTEIN_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(PROTEIN_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_RIBOFLAVIN_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(RIBOFLAVIN_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_SATURATED_FAT_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(SATURATED_FAT_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_SELENIUM_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(SELENIUM_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_SODIUM_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(SODIUM_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_SUGAR_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(SUGAR_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_THIAMIN_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(THIAMIN_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_TOTAL_CARBOHYDRATE_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(TOTAL_CARBOHYDRATE_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_TOTAL_FAT_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(TOTAL_FAT_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_TRANS_FAT_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(TRANS_FAT_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_UNSATURATED_FAT_TOTAL:
                aggregateValue =
                        results.getDouble(results.getColumnIndex(UNSATURATED_FAT_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_VITAMIN_A_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(VITAMIN_A_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_VITAMIN_B12_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(VITAMIN_B12_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_VITAMIN_B6_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(VITAMIN_B6_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_VITAMIN_C_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(VITAMIN_C_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_VITAMIN_D_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(VITAMIN_D_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_VITAMIN_E_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(VITAMIN_E_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_VITAMIN_K_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(VITAMIN_K_COLUMN_NAME));
                break;
            case NUTRITION_RECORD_ZINC_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(ZINC_COLUMN_NAME));
                break;
            default:
                return null;
        }
        return new AggregateResult<>(aggregateValue, getZoneOffset(results), dataOrigins);
    }

    @Override
    public String getMainTableName() {
        return NUTRITION_RECORD_TABLE_NAME;
    }

    @Override
    @Nullable
    AggregateParams getAggregateParams(AggregationType<?> aggregateRequest) {
        List<String> columnNames;
        switch (aggregateRequest.getAggregationTypeIdentifier()) {
            case NUTRITION_RECORD_BIOTIN_TOTAL:
                columnNames = Collections.singletonList(BIOTIN_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_CAFFEINE_TOTAL:
                columnNames = Collections.singletonList(CAFFEINE_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_CALCIUM_TOTAL:
                columnNames = Collections.singletonList(CALCIUM_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_CHLORIDE_TOTAL:
                columnNames = Collections.singletonList(CHLORIDE_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_CHOLESTEROL_TOTAL:
                columnNames = Collections.singletonList(CHOLESTEROL_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_CHROMIUM_TOTAL:
                columnNames = Collections.singletonList(CHROMIUM_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_COPPER_TOTAL:
                columnNames = Collections.singletonList(COPPER_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_DIETARY_FIBER_TOTAL:
                columnNames = Collections.singletonList(DIETARY_FIBER_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_ENERGY_TOTAL:
                columnNames = Collections.singletonList(ENERGY_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL:
                columnNames = Collections.singletonList(ENERGY_FROM_FAT_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_FOLATE_TOTAL:
                columnNames = Collections.singletonList(FOLATE_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_FOLIC_ACID_TOTAL:
                columnNames = Collections.singletonList(FOLIC_ACID_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_IODINE_TOTAL:
                columnNames = Collections.singletonList(IODINE_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_IRON_TOTAL:
                columnNames = Collections.singletonList(IRON_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_MAGNESIUM_TOTAL:
                columnNames = Collections.singletonList(MAGNESIUM_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_MANGANESE_TOTAL:
                columnNames = Collections.singletonList(MANGANESE_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_MOLYBDENUM_TOTAL:
                columnNames = Collections.singletonList(MOLYBDENUM_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL:
                columnNames = Collections.singletonList(MONOUNSATURATED_FAT_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_NIACIN_TOTAL:
                columnNames = Collections.singletonList(NIACIN_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_PANTOTHENIC_ACID_TOTAL:
                columnNames = Collections.singletonList(PANTOTHENIC_ACID_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_PHOSPHORUS_TOTAL:
                columnNames = Collections.singletonList(PHOSPHORUS_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_POLYUNSATURATED_FAT_TOTAL:
                columnNames = Collections.singletonList(POLYUNSATURATED_FAT_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_POTASSIUM_TOTAL:
                columnNames = Collections.singletonList(POTASSIUM_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_PROTEIN_TOTAL:
                columnNames = Collections.singletonList(PROTEIN_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_RIBOFLAVIN_TOTAL:
                columnNames = Collections.singletonList(RIBOFLAVIN_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_SATURATED_FAT_TOTAL:
                columnNames = Collections.singletonList(SATURATED_FAT_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_SELENIUM_TOTAL:
                columnNames = Collections.singletonList(SELENIUM_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_SODIUM_TOTAL:
                columnNames = Collections.singletonList(SODIUM_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_SUGAR_TOTAL:
                columnNames = Collections.singletonList(SUGAR_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_THIAMIN_TOTAL:
                columnNames = Collections.singletonList(THIAMIN_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_TOTAL_CARBOHYDRATE_TOTAL:
                columnNames = Collections.singletonList(TOTAL_CARBOHYDRATE_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_TOTAL_FAT_TOTAL:
                columnNames = Collections.singletonList(TOTAL_FAT_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_TRANS_FAT_TOTAL:
                columnNames = Collections.singletonList(TRANS_FAT_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_UNSATURATED_FAT_TOTAL:
                columnNames = Collections.singletonList(UNSATURATED_FAT_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_VITAMIN_A_TOTAL:
                columnNames = Collections.singletonList(VITAMIN_A_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_VITAMIN_B12_TOTAL:
                columnNames = Collections.singletonList(VITAMIN_B12_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_VITAMIN_B6_TOTAL:
                columnNames = Collections.singletonList(VITAMIN_B6_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_VITAMIN_C_TOTAL:
                columnNames = Collections.singletonList(VITAMIN_C_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_VITAMIN_D_TOTAL:
                columnNames = Collections.singletonList(VITAMIN_D_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_VITAMIN_E_TOTAL:
                columnNames = Collections.singletonList(VITAMIN_E_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_VITAMIN_K_TOTAL:
                columnNames = Collections.singletonList(VITAMIN_K_COLUMN_NAME);
                break;
            case NUTRITION_RECORD_ZINC_TOTAL:
                columnNames = Collections.singletonList(ZINC_COLUMN_NAME);
                break;
            default:
                return null;
        }
        return new AggregateParams(NUTRITION_RECORD_TABLE_NAME, columnNames);
    }

    @Override
    NutritionRecordInternal populateSpecificRecordValue(Cursor cursor) {
        NutritionRecordInternal nutritionRecord = new NutritionRecordInternal();
        setIfNonNull(
                nutritionRecord,
                cursor,
                UNSATURATED_FAT_COLUMN_NAME,
                NutritionRecordInternal::setUnsaturatedFat);
        setIfNonNull(
                nutritionRecord,
                cursor,
                POTASSIUM_COLUMN_NAME,
                NutritionRecordInternal::setPotassium);
        setIfNonNull(
                nutritionRecord, cursor, THIAMIN_COLUMN_NAME, NutritionRecordInternal::setThiamin);
        nutritionRecord.setMealType(getCursorInt(cursor, MEAL_TYPE_COLUMN_NAME));
        setIfNonNull(
                nutritionRecord,
                cursor,
                TRANS_FAT_COLUMN_NAME,
                NutritionRecordInternal::setTransFat);
        setIfNonNull(
                nutritionRecord,
                cursor,
                MANGANESE_COLUMN_NAME,
                NutritionRecordInternal::setManganese);
        setIfNonNull(
                nutritionRecord,
                cursor,
                ENERGY_FROM_FAT_COLUMN_NAME,
                NutritionRecordInternal::setEnergyFromFat);
        setIfNonNull(
                nutritionRecord,
                cursor,
                CAFFEINE_COLUMN_NAME,
                NutritionRecordInternal::setCaffeine);
        setIfNonNull(
                nutritionRecord,
                cursor,
                DIETARY_FIBER_COLUMN_NAME,
                NutritionRecordInternal::setDietaryFiber);
        setIfNonNull(
                nutritionRecord,
                cursor,
                SELENIUM_COLUMN_NAME,
                NutritionRecordInternal::setSelenium);
        setIfNonNull(
                nutritionRecord,
                cursor,
                VITAMIN_B6_COLUMN_NAME,
                NutritionRecordInternal::setVitaminB6);
        setIfNonNull(
                nutritionRecord, cursor, PROTEIN_COLUMN_NAME, NutritionRecordInternal::setProtein);
        setIfNonNull(
                nutritionRecord,
                cursor,
                CHLORIDE_COLUMN_NAME,
                NutritionRecordInternal::setChloride);
        setIfNonNull(
                nutritionRecord,
                cursor,
                CHOLESTEROL_COLUMN_NAME,
                NutritionRecordInternal::setCholesterol);
        setIfNonNull(
                nutritionRecord, cursor, COPPER_COLUMN_NAME, NutritionRecordInternal::setCopper);
        setIfNonNull(
                nutritionRecord, cursor, IODINE_COLUMN_NAME, NutritionRecordInternal::setIodine);
        setIfNonNull(
                nutritionRecord,
                cursor,
                VITAMIN_B12_COLUMN_NAME,
                NutritionRecordInternal::setVitaminB12);
        setIfNonNull(nutritionRecord, cursor, ZINC_COLUMN_NAME, NutritionRecordInternal::setZinc);
        setIfNonNull(
                nutritionRecord,
                cursor,
                RIBOFLAVIN_COLUMN_NAME,
                NutritionRecordInternal::setRiboflavin);
        setIfNonNull(
                nutritionRecord, cursor, ENERGY_COLUMN_NAME, NutritionRecordInternal::setEnergy);
        setIfNonNull(
                nutritionRecord,
                cursor,
                MOLYBDENUM_COLUMN_NAME,
                NutritionRecordInternal::setMolybdenum);
        setIfNonNull(
                nutritionRecord,
                cursor,
                PHOSPHORUS_COLUMN_NAME,
                NutritionRecordInternal::setPhosphorus);
        setIfNonNull(
                nutritionRecord,
                cursor,
                CHROMIUM_COLUMN_NAME,
                NutritionRecordInternal::setChromium);
        setIfNonNull(
                nutritionRecord,
                cursor,
                TOTAL_FAT_COLUMN_NAME,
                NutritionRecordInternal::setTotalFat);
        setIfNonNull(
                nutritionRecord, cursor, CALCIUM_COLUMN_NAME, NutritionRecordInternal::setCalcium);
        setIfNonNull(
                nutritionRecord,
                cursor,
                VITAMIN_C_COLUMN_NAME,
                NutritionRecordInternal::setVitaminC);
        setIfNonNull(
                nutritionRecord,
                cursor,
                VITAMIN_E_COLUMN_NAME,
                NutritionRecordInternal::setVitaminE);
        setIfNonNull(
                nutritionRecord, cursor, BIOTIN_COLUMN_NAME, NutritionRecordInternal::setBiotin);
        setIfNonNull(
                nutritionRecord,
                cursor,
                VITAMIN_D_COLUMN_NAME,
                NutritionRecordInternal::setVitaminD);
        setIfNonNull(
                nutritionRecord, cursor, NIACIN_COLUMN_NAME, NutritionRecordInternal::setNiacin);
        setIfNonNull(
                nutritionRecord,
                cursor,
                MAGNESIUM_COLUMN_NAME,
                NutritionRecordInternal::setMagnesium);
        setIfNonNull(
                nutritionRecord,
                cursor,
                TOTAL_CARBOHYDRATE_COLUMN_NAME,
                NutritionRecordInternal::setTotalCarbohydrate);
        setIfNonNull(
                nutritionRecord,
                cursor,
                VITAMIN_K_COLUMN_NAME,
                NutritionRecordInternal::setVitaminK);
        setIfNonNull(
                nutritionRecord,
                cursor,
                POLYUNSATURATED_FAT_COLUMN_NAME,
                NutritionRecordInternal::setPolyunsaturatedFat);
        setIfNonNull(
                nutritionRecord,
                cursor,
                SATURATED_FAT_COLUMN_NAME,
                NutritionRecordInternal::setSaturatedFat);
        setIfNonNull(
                nutritionRecord, cursor, SODIUM_COLUMN_NAME, NutritionRecordInternal::setSodium);
        setIfNonNull(
                nutritionRecord, cursor, FOLATE_COLUMN_NAME, NutritionRecordInternal::setFolate);
        setIfNonNull(
                nutritionRecord,
                cursor,
                MONOUNSATURATED_FAT_COLUMN_NAME,
                NutritionRecordInternal::setMonounsaturatedFat);
        setIfNonNull(
                nutritionRecord,
                cursor,
                PANTOTHENIC_ACID_COLUMN_NAME,
                NutritionRecordInternal::setPantothenicAcid);
        nutritionRecord.setMealName(getCursorString(cursor, MEAL_NAME_COLUMN_NAME));
        setIfNonNull(nutritionRecord, cursor, IRON_COLUMN_NAME, NutritionRecordInternal::setIron);
        setIfNonNull(
                nutritionRecord,
                cursor,
                VITAMIN_A_COLUMN_NAME,
                NutritionRecordInternal::setVitaminA);
        setIfNonNull(
                nutritionRecord,
                cursor,
                FOLIC_ACID_COLUMN_NAME,
                NutritionRecordInternal::setFolicAcid);
        setIfNonNull(nutritionRecord, cursor, SUGAR_COLUMN_NAME, NutritionRecordInternal::setSugar);
        return nutritionRecord;
    }

    private void setIfNonNull(
            NutritionRecordInternal nutritionRecord,
            Cursor cursor,
            String columnName,
            BiConsumer<NutritionRecordInternal, Double> setter) {
        int columnId = cursor.getColumnIndexOrThrow(columnName);
        if (!cursor.isNull(columnId)) {
            setter.accept(nutritionRecord, cursor.getDouble(columnId));
        }
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues, NutritionRecordInternal nutritionRecord) {
        contentValues.put(UNSATURATED_FAT_COLUMN_NAME, nutritionRecord.getUnsaturatedFat());
        contentValues.put(POTASSIUM_COLUMN_NAME, nutritionRecord.getPotassium());
        contentValues.put(THIAMIN_COLUMN_NAME, nutritionRecord.getThiamin());
        contentValues.put(MEAL_TYPE_COLUMN_NAME, nutritionRecord.getMealType());
        contentValues.put(TRANS_FAT_COLUMN_NAME, nutritionRecord.getTransFat());
        contentValues.put(MANGANESE_COLUMN_NAME, nutritionRecord.getManganese());
        contentValues.put(ENERGY_FROM_FAT_COLUMN_NAME, nutritionRecord.getEnergyFromFat());
        contentValues.put(CAFFEINE_COLUMN_NAME, nutritionRecord.getCaffeine());
        contentValues.put(DIETARY_FIBER_COLUMN_NAME, nutritionRecord.getDietaryFiber());
        contentValues.put(SELENIUM_COLUMN_NAME, nutritionRecord.getSelenium());
        contentValues.put(VITAMIN_B6_COLUMN_NAME, nutritionRecord.getVitaminB6());
        contentValues.put(PROTEIN_COLUMN_NAME, nutritionRecord.getProtein());
        contentValues.put(CHLORIDE_COLUMN_NAME, nutritionRecord.getChloride());
        contentValues.put(CHOLESTEROL_COLUMN_NAME, nutritionRecord.getCholesterol());
        contentValues.put(COPPER_COLUMN_NAME, nutritionRecord.getCopper());
        contentValues.put(IODINE_COLUMN_NAME, nutritionRecord.getIodine());
        contentValues.put(VITAMIN_B12_COLUMN_NAME, nutritionRecord.getVitaminB12());
        contentValues.put(ZINC_COLUMN_NAME, nutritionRecord.getZinc());
        contentValues.put(RIBOFLAVIN_COLUMN_NAME, nutritionRecord.getRiboflavin());
        contentValues.put(ENERGY_COLUMN_NAME, nutritionRecord.getEnergy());
        contentValues.put(MOLYBDENUM_COLUMN_NAME, nutritionRecord.getMolybdenum());
        contentValues.put(PHOSPHORUS_COLUMN_NAME, nutritionRecord.getPhosphorus());
        contentValues.put(CHROMIUM_COLUMN_NAME, nutritionRecord.getChromium());
        contentValues.put(TOTAL_FAT_COLUMN_NAME, nutritionRecord.getTotalFat());
        contentValues.put(CALCIUM_COLUMN_NAME, nutritionRecord.getCalcium());
        contentValues.put(VITAMIN_C_COLUMN_NAME, nutritionRecord.getVitaminC());
        contentValues.put(VITAMIN_E_COLUMN_NAME, nutritionRecord.getVitaminE());
        contentValues.put(BIOTIN_COLUMN_NAME, nutritionRecord.getBiotin());
        contentValues.put(VITAMIN_D_COLUMN_NAME, nutritionRecord.getVitaminD());
        contentValues.put(NIACIN_COLUMN_NAME, nutritionRecord.getNiacin());
        contentValues.put(MAGNESIUM_COLUMN_NAME, nutritionRecord.getMagnesium());
        contentValues.put(TOTAL_CARBOHYDRATE_COLUMN_NAME, nutritionRecord.getTotalCarbohydrate());
        contentValues.put(VITAMIN_K_COLUMN_NAME, nutritionRecord.getVitaminK());
        contentValues.put(POLYUNSATURATED_FAT_COLUMN_NAME, nutritionRecord.getPolyunsaturatedFat());
        contentValues.put(SATURATED_FAT_COLUMN_NAME, nutritionRecord.getSaturatedFat());
        contentValues.put(SODIUM_COLUMN_NAME, nutritionRecord.getSodium());
        contentValues.put(FOLATE_COLUMN_NAME, nutritionRecord.getFolate());
        contentValues.put(MONOUNSATURATED_FAT_COLUMN_NAME, nutritionRecord.getMonounsaturatedFat());
        contentValues.put(PANTOTHENIC_ACID_COLUMN_NAME, nutritionRecord.getPantothenicAcid());
        contentValues.put(MEAL_NAME_COLUMN_NAME, nutritionRecord.getMealName());
        contentValues.put(IRON_COLUMN_NAME, nutritionRecord.getIron());
        contentValues.put(VITAMIN_A_COLUMN_NAME, nutritionRecord.getVitaminA());
        contentValues.put(FOLIC_ACID_COLUMN_NAME, nutritionRecord.getFolicAcid());
        contentValues.put(SUGAR_COLUMN_NAME, nutritionRecord.getSugar());
    }

    @Override
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Arrays.asList(
                new Pair<>(UNSATURATED_FAT_COLUMN_NAME, REAL),
                new Pair<>(POTASSIUM_COLUMN_NAME, REAL),
                new Pair<>(THIAMIN_COLUMN_NAME, REAL),
                new Pair<>(MEAL_TYPE_COLUMN_NAME, INTEGER),
                new Pair<>(TRANS_FAT_COLUMN_NAME, REAL),
                new Pair<>(MANGANESE_COLUMN_NAME, REAL),
                new Pair<>(ENERGY_FROM_FAT_COLUMN_NAME, REAL),
                new Pair<>(CAFFEINE_COLUMN_NAME, REAL),
                new Pair<>(DIETARY_FIBER_COLUMN_NAME, REAL),
                new Pair<>(SELENIUM_COLUMN_NAME, REAL),
                new Pair<>(VITAMIN_B6_COLUMN_NAME, REAL),
                new Pair<>(PROTEIN_COLUMN_NAME, REAL),
                new Pair<>(CHLORIDE_COLUMN_NAME, REAL),
                new Pair<>(CHOLESTEROL_COLUMN_NAME, REAL),
                new Pair<>(COPPER_COLUMN_NAME, REAL),
                new Pair<>(IODINE_COLUMN_NAME, REAL),
                new Pair<>(VITAMIN_B12_COLUMN_NAME, REAL),
                new Pair<>(ZINC_COLUMN_NAME, REAL),
                new Pair<>(RIBOFLAVIN_COLUMN_NAME, REAL),
                new Pair<>(ENERGY_COLUMN_NAME, REAL),
                new Pair<>(MOLYBDENUM_COLUMN_NAME, REAL),
                new Pair<>(PHOSPHORUS_COLUMN_NAME, REAL),
                new Pair<>(CHROMIUM_COLUMN_NAME, REAL),
                new Pair<>(TOTAL_FAT_COLUMN_NAME, REAL),
                new Pair<>(CALCIUM_COLUMN_NAME, REAL),
                new Pair<>(VITAMIN_C_COLUMN_NAME, REAL),
                new Pair<>(VITAMIN_E_COLUMN_NAME, REAL),
                new Pair<>(BIOTIN_COLUMN_NAME, REAL),
                new Pair<>(VITAMIN_D_COLUMN_NAME, REAL),
                new Pair<>(NIACIN_COLUMN_NAME, REAL),
                new Pair<>(MAGNESIUM_COLUMN_NAME, REAL),
                new Pair<>(TOTAL_CARBOHYDRATE_COLUMN_NAME, REAL),
                new Pair<>(VITAMIN_K_COLUMN_NAME, REAL),
                new Pair<>(POLYUNSATURATED_FAT_COLUMN_NAME, REAL),
                new Pair<>(SATURATED_FAT_COLUMN_NAME, REAL),
                new Pair<>(SODIUM_COLUMN_NAME, REAL),
                new Pair<>(FOLATE_COLUMN_NAME, REAL),
                new Pair<>(MONOUNSATURATED_FAT_COLUMN_NAME, REAL),
                new Pair<>(PANTOTHENIC_ACID_COLUMN_NAME, REAL),
                new Pair<>(MEAL_NAME_COLUMN_NAME, TEXT_NULL),
                new Pair<>(IRON_COLUMN_NAME, REAL),
                new Pair<>(VITAMIN_A_COLUMN_NAME, REAL),
                new Pair<>(FOLIC_ACID_COLUMN_NAME, REAL),
                new Pair<>(SUGAR_COLUMN_NAME, REAL));
    }
}
