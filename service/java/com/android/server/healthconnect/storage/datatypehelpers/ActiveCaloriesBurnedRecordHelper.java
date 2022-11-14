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

import static com.android.server.healthconnect.storage.utils.StorageUtils.REAL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorDouble;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for ActiveCaloriesBurnedRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED)
public final class ActiveCaloriesBurnedRecordHelper
        extends IntervalRecordHelper<ActiveCaloriesBurnedRecordInternal> {
    private static final String ACTIVE_CALORIES_BURNED_RECORD_TABLE_NAME =
            "active_calories_burned_record_table";
    private static final String ENERGY_COLUMN_NAME = "energy";

    @Override
    @NonNull
    public String getMainTableName() {
        return ACTIVE_CALORIES_BURNED_RECORD_TABLE_NAME;
    }

    @Override
    void populateSpecificRecordValue(
            @NonNull Cursor cursor,
            @NonNull ActiveCaloriesBurnedRecordInternal activeCaloriesBurnedRecord) {
        activeCaloriesBurnedRecord.setEnergy(getCursorDouble(cursor, ENERGY_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull ActiveCaloriesBurnedRecordInternal activeCaloriesBurnedRecord) {
        contentValues.put(ENERGY_COLUMN_NAME, activeCaloriesBurnedRecord.getEnergy());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Collections.singletonList(new Pair<>(ENERGY_COLUMN_NAME, REAL));
    }
}
