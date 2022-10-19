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

import android.annotation.NonNull;
import android.content.ContentValues;
import android.healthconnect.internal.datatypes.IntervalRecordInternal;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Parent class for all the Interval type records
 *
 * @hide
 */
abstract class IntervalRecordHelper<T extends IntervalRecordInternal<?>> extends RecordHelper<T> {
    private static final String START_TIME_COLUMN_NAME = "start_time";
    private static final String START_ZONE_OFFSET_COLUMN_NAME = "start_zone_offset";
    private static final String END_TIME_COLUMN_NAME = "end_time";
    private static final String END_ZONE_OFFSET_COLUMN_NAME = "end_zone_offset";

    IntervalRecordHelper() {
        super();
    }

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @Override
    @NonNull
    final List<Pair<String, SQLiteType>> getSpecificColumnInfo() {
        ArrayList<Pair<String, SQLiteType>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(START_TIME_COLUMN_NAME, SQLiteType.INTEGER));
        columnInfo.add(new Pair<>(START_ZONE_OFFSET_COLUMN_NAME, SQLiteType.INTEGER));
        columnInfo.add(new Pair<>(END_TIME_COLUMN_NAME, SQLiteType.INTEGER));
        columnInfo.add(new Pair<>(END_ZONE_OFFSET_COLUMN_NAME, SQLiteType.INTEGER));

        columnInfo.addAll(getIntervalRecordColumnInfo());

        return columnInfo;
    }

    @Override
    final void populateContentValues(
            @NonNull ContentValues contentValues, @NonNull T intervalRecord) {
        contentValues.put(START_TIME_COLUMN_NAME, intervalRecord.getStartTimeInMillis());
        contentValues.put(
                START_ZONE_OFFSET_COLUMN_NAME, intervalRecord.getStartZoneOffsetInSeconds());
        contentValues.put(END_TIME_COLUMN_NAME, intervalRecord.getEndTimeInMillis());
        contentValues.put(END_ZONE_OFFSET_COLUMN_NAME, intervalRecord.getEndZoneOffsetInSeconds());

        populateSpecificContentValues(contentValues, intervalRecord);
    }

    abstract void populateSpecificContentValues(
            @NonNull ContentValues contentValues, @NonNull T intervalRecordInternal);

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @NonNull
    abstract List<Pair<String, SQLiteType>> getIntervalRecordColumnInfo();
}
