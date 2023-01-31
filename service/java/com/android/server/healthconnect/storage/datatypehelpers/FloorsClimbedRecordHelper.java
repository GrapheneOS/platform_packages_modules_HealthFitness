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

import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.AggregateResult;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.FloorsClimbedRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for FloorsClimbedRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED)
public final class FloorsClimbedRecordHelper
        extends IntervalRecordHelper<FloorsClimbedRecordInternal> {
    private static final String FLOORS_CLIMBED_RECORD_TABLE_NAME = "floors_climbed_record_table";
    private static final String FLOORS_COLUMN_NAME = "floors";

    @Override
    public AggregateResult<?> getAggregateResult(
            Cursor results, AggregationType<?> aggregationType) {
        switch (aggregationType.getAggregationTypeIdentifier()) {
            case FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL:
                return new AggregateResult<>(
                                results.getLong(results.getColumnIndex(FLOORS_COLUMN_NAME)))
                        .setZoneOffset(getZoneOffset(results));
            default:
                return null;
        }
    }

    @Override
    @NonNull
    public String getMainTableName() {
        return FLOORS_CLIMBED_RECORD_TABLE_NAME;
    }

    @Override
    AggregateParams getAggregateParams(AggregationType<?> aggregateRequest) {
        switch (aggregateRequest.getAggregationTypeIdentifier()) {
            case FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL:
                return new AggregateParams(
                        FLOORS_CLIMBED_RECORD_TABLE_NAME,
                        Collections.singletonList(FLOORS_COLUMN_NAME),
                        START_TIME_COLUMN_NAME);
            default:
                return null;
        }
    }

    @Override
    void populateSpecificRecordValue(
            @NonNull Cursor cursor, @NonNull FloorsClimbedRecordInternal floorsClimbedRecord) {
        floorsClimbedRecord.setFloors(getCursorInt(cursor, FLOORS_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull FloorsClimbedRecordInternal floorsClimbedRecord) {
        contentValues.put(FLOORS_COLUMN_NAME, floorsClimbedRecord.getFloors());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Collections.singletonList(new Pair<>(FLOORS_COLUMN_NAME, INTEGER));
    }
}
