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

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.SwimmingStrokesRecordInternal;
import android.util.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class for SwimmingStrokesRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SWIMMING_STROKES)
public final class SwimmingStrokesRecordHelper
        extends IntervalRecordHelper<SwimmingStrokesRecordInternal> {
    private static final String SWIMMING_STROKES_RECORD_TABLE_NAME =
            "swimming_strokes_record_table";
    private static final String COUNT_COLUMN_NAME = "count";
    private static final String TYPE_COLUMN_NAME = "type";

    @Override
    @NonNull
    public String getMainTableName() {
        return SWIMMING_STROKES_RECORD_TABLE_NAME;
    }

    @Override
    void populateSpecificRecordValue(
            @NonNull Cursor cursor, @NonNull SwimmingStrokesRecordInternal swimmingStrokesRecord) {
        swimmingStrokesRecord.setCount(getCursorLong(cursor, COUNT_COLUMN_NAME));
        swimmingStrokesRecord.setType(getCursorInt(cursor, TYPE_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull SwimmingStrokesRecordInternal swimmingStrokesRecord) {
        contentValues.put(COUNT_COLUMN_NAME, swimmingStrokesRecord.getCount());
        contentValues.put(TYPE_COLUMN_NAME, swimmingStrokesRecord.getType());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Arrays.asList(
                new Pair<>(COUNT_COLUMN_NAME, INTEGER), new Pair<>(TYPE_COLUMN_NAME, INTEGER));
    }
}
