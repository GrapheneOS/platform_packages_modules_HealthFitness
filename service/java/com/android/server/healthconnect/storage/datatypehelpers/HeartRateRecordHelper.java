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

import android.content.ContentValues;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.HeartRateRecordInternal;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for HeartRateRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_HEART_RATE)
public class HeartRateRecordHelper
        extends SeriesRecordHelper<
                HeartRateRecordInternal, HeartRateRecordInternal.HeartRateSample> {
    public static final int NUM_LOCAL_COLUMNS = 2;
    private static final String TABLE_NAME = "heart_rate_record_table";
    private static final String SERIES_TABLE_NAME = "HeartRateRecordSeriesTable";
    private static final String BEATS_PER_MINUTE_COLUMN_NAME = "beats_per_minute";
    private static final String EPOCH_MILLIS_COLUMN_NAME = "epoch_millis";

    @Override
    String getMainTableName() {
        return TABLE_NAME;
    }

    @Override
    List<Pair<String, String>> getSeriesRecordColumnInfo() {
        List<Pair<String, String>> columnInfo = new ArrayList<>(NUM_LOCAL_COLUMNS);
        columnInfo.add(new Pair<>(BEATS_PER_MINUTE_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(EPOCH_MILLIS_COLUMN_NAME, INTEGER));
        return columnInfo;
    }

    @Override
    String getSeriesDataTableName() {
        return SERIES_TABLE_NAME;
    }

    @Override
    void populateSampleTo(
            ContentValues contentValues, HeartRateRecordInternal.HeartRateSample heartRateSample) {
        contentValues.put(BEATS_PER_MINUTE_COLUMN_NAME, heartRateSample.getBeatsPerMinute());
        contentValues.put(EPOCH_MILLIS_COLUMN_NAME, heartRateSample.getEpochMillis());
    }
}
