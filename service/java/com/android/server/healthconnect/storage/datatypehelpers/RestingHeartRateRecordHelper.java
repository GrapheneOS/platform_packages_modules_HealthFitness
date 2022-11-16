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
import android.healthconnect.internal.datatypes.RestingHeartRateRecordInternal;
import android.util.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class for RestingHeartRateRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE)
public final class RestingHeartRateRecordHelper
        extends InstantRecordHelper<RestingHeartRateRecordInternal> {
    private static final String RESTING_HEART_RATE_RECORD_TABLE_NAME =
            "resting_heart_rate_record_table";
    private static final String BEATS_PER_MINUTE_COLUMN_NAME = "beats_per_minute";

    @Override
    @NonNull
    public String getMainTableName() {
        return RESTING_HEART_RATE_RECORD_TABLE_NAME;
    }

    @Override
    void populateSpecificRecordValue(
            @NonNull Cursor cursor,
            @NonNull RestingHeartRateRecordInternal restingHeartRateRecord) {
        restingHeartRateRecord.setBeatsPerMinute(
                getCursorLong(cursor, BEATS_PER_MINUTE_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull RestingHeartRateRecordInternal restingHeartRateRecord) {
        contentValues.put(BEATS_PER_MINUTE_COLUMN_NAME, restingHeartRateRecord.getBeatsPerMinute());
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getInstantRecordColumnInfo() {
        return Arrays.asList(new Pair<>(BEATS_PER_MINUTE_COLUMN_NAME, INTEGER));
    }
}
