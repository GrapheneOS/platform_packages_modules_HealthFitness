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
package com.android.server.healthconnect.storage.datatypehelpers;

import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL;

import static com.android.server.healthconnect.storage.utils.StorageUtils.REAL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorDouble;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.AggregateResult;
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.TotalCaloriesBurnedRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for TotalCaloriesBurnedRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED)
public final class TotalCaloriesBurnedRecordHelper
        extends IntervalRecordHelper<TotalCaloriesBurnedRecordInternal> {
    private static final String TOTAL_CALORIES_BURNED_RECORD_TABLE_NAME =
            "total_calories_burned_record_table";
    private static final String ENERGY_COLUMN_NAME = "energy";

    @Override
    public AggregateResult<?> getAggregateResult(
            Cursor results, AggregationType<?> aggregationType) {
        double aggregateValue;
        switch (aggregationType.getAggregationTypeIdentifier()) {
            case TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL:
                aggregateValue = results.getDouble(results.getColumnIndex(ENERGY_COLUMN_NAME));
                break;
            default:
                return null;
        }
        return new AggregateResult<>(aggregateValue).setZoneOffset(getZoneOffset(results));
    }

    @Override
    @NonNull
    public String getMainTableName() {
        return TOTAL_CALORIES_BURNED_RECORD_TABLE_NAME;
    }

    @Override
    AggregateParams getAggregateParams(AggregationType<?> aggregateRequest) {
        List<String> columnNames;
        switch (aggregateRequest.getAggregationTypeIdentifier()) {
            case TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL:
                columnNames = Collections.singletonList(ENERGY_COLUMN_NAME);
                break;
            default:
                return null;
        }
        return new AggregateParams(
                TOTAL_CALORIES_BURNED_RECORD_TABLE_NAME, columnNames, START_TIME_COLUMN_NAME);
    }

    @Override
    void populateSpecificRecordValue(
            @NonNull Cursor cursor,
            @NonNull TotalCaloriesBurnedRecordInternal totalCaloriesBurnedRecord) {
        totalCaloriesBurnedRecord.setEnergy(getCursorDouble(cursor, ENERGY_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull TotalCaloriesBurnedRecordInternal totalCaloriesBurnedRecord) {
        contentValues.put(ENERGY_COLUMN_NAME, totalCaloriesBurnedRecord.getEnergy());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Collections.singletonList(new Pair<>(ENERGY_COLUMN_NAME, REAL));
    }
}
