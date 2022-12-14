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
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.WheelchairPushesRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for WheelchairPushesRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES)
public final class WheelchairPushesRecordHelper
        extends IntervalRecordHelper<WheelchairPushesRecordInternal> {
    private static final String WHEELCHAIR_PUSHES_RECORD_TABLE_NAME =
            "wheelchair_pushes_record_table";
    private static final String COUNT_COLUMN_NAME = "count";

    @Override
    @NonNull
    public String getMainTableName() {
        return WHEELCHAIR_PUSHES_RECORD_TABLE_NAME;
    }

    @Override
    void populateSpecificRecordValue(
            @NonNull Cursor cursor,
            @NonNull WheelchairPushesRecordInternal wheelchairPushesRecord) {
        wheelchairPushesRecord.setCount(getCursorLong(cursor, COUNT_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull WheelchairPushesRecordInternal wheelchairPushesRecord) {
        contentValues.put(COUNT_COLUMN_NAME, wheelchairPushesRecord.getCount());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Collections.singletonList(new Pair<>(COUNT_COLUMN_NAME, INTEGER));
    }
}
