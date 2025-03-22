/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.ActivityIntensityRecordInternal;
import android.util.Pair;

import com.android.server.healthconnect.fitness.aggregation.AggregateParams;

import java.util.List;

/**
 * Helper class for {@link android.health.connect.datatypes.ActivityIntensityRecord}.
 *
 * @hide
 */
public class ActivityIntensityRecordHelper
        extends IntervalRecordHelper<ActivityIntensityRecordInternal> {
    private static final String TABLE_NAME = "activity_intensity_record_table";
    public static final String TYPE_COLUMN_NAME = "type";

    public ActivityIntensityRecordHelper() {
        super(RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY);
    }

    @Override
    void populateSpecificRecordValue(
            Cursor cursor, ActivityIntensityRecordInternal recordInternal) {
        recordInternal.setActivityIntensityType(getCursorInt(cursor, TYPE_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues, ActivityIntensityRecordInternal recordInternal) {
        contentValues.put(TYPE_COLUMN_NAME, recordInternal.getActivityIntensityType());
    }

    @Override
    List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return List.of(Pair.create(TYPE_COLUMN_NAME, INTEGER));
    }

    @Override
    public String getMainTableName() {
        return TABLE_NAME;
    }

    @Override
    AggregateParams getAggregateParams(AggregationType<?> aggregateRequest) {
        return new AggregateParams(TABLE_NAME, List.of(TYPE_COLUMN_NAME));
    }
}
