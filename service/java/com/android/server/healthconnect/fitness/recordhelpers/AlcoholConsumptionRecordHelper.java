/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static com.android.server.healthconnect.storage.HealthConnectDatabase.createTable;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.REAL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorDouble;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.AlcoholConsumptionRecordInternal;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class for {@link android.health.connect.datatypes.AlcoholConsumptionRecord}
 *
 * @hide
 */
public class AlcoholConsumptionRecordHelper
        extends IntervalRecordHelper<AlcoholConsumptionRecordInternal> {

    @VisibleForTesting
    public static final String ALCOHOL_CONSUMPTION_RECORD_TABLE_NAME =
            "alcohol_consumption_record_table";

    @VisibleForTesting public static final String TEMPORAL_TYPE_COLUMN_NAME = "temporal_type";

    @VisibleForTesting public static final String SERVING_COUNT_COLUMN_NAME = "serving_count";

    @VisibleForTesting public static final String BEVERAGE_TYPE_COLUMN_NAME = "beverage_type";
    @VisibleForTesting public static final String SERVING_SIZE_COLUMN_NAME = "serving_size";

    @VisibleForTesting
    public static final String SERVING_VOLUME_LITERS_COLUMN_NAME = "serving_volume_liters";

    @VisibleForTesting
    public static final String ALCOHOL_BY_VOLUME_COLUMN_NAME = "alcohol_by_volume";

    @VisibleForTesting public static final String NOTE_COLUMN_NAME = "note";

    public AlcoholConsumptionRecordHelper() {
        super(RecordTypeIdentifier.RECORD_TYPE_ALCOHOL_CONSUMPTION);
    }

    @Override
    AlcoholConsumptionRecordInternal populateSpecificRecordValue(Cursor cursor) {
        return new AlcoholConsumptionRecordInternal()
                .setServingCount(getCursorInt(cursor, SERVING_COUNT_COLUMN_NAME))
                .setBeverageType(getCursorInt(cursor, BEVERAGE_TYPE_COLUMN_NAME))
                .setServingSize(getCursorInt(cursor, SERVING_SIZE_COLUMN_NAME))
                .setServingVolumeLiters(getCursorDouble(cursor, SERVING_VOLUME_LITERS_COLUMN_NAME))
                .setAlcoholByVolume(getCursorDouble(cursor, ALCOHOL_BY_VOLUME_COLUMN_NAME))
                .setNote(getCursorString(cursor, NOTE_COLUMN_NAME))
                .setTemporalType(getCursorInt(cursor, TEMPORAL_TYPE_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues, AlcoholConsumptionRecordInternal recordInternal) {
        contentValues.put(TEMPORAL_TYPE_COLUMN_NAME, recordInternal.getTemporalType());
        contentValues.put(SERVING_COUNT_COLUMN_NAME, recordInternal.getServingCount());
        contentValues.put(BEVERAGE_TYPE_COLUMN_NAME, recordInternal.getBeverageType());
        contentValues.put(SERVING_SIZE_COLUMN_NAME, recordInternal.getServingSize());
        contentValues.put(
                SERVING_VOLUME_LITERS_COLUMN_NAME, recordInternal.getServingVolumeLiters());
        contentValues.put(ALCOHOL_BY_VOLUME_COLUMN_NAME, recordInternal.getAlcoholByVolume());
        contentValues.put(NOTE_COLUMN_NAME, (String) recordInternal.getNote());
    }

    @Override
    List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Arrays.asList(
                new Pair<>(SERVING_COUNT_COLUMN_NAME, INTEGER),
                new Pair<>(BEVERAGE_TYPE_COLUMN_NAME, INTEGER),
                new Pair<>(SERVING_SIZE_COLUMN_NAME, INTEGER),
                new Pair<>(SERVING_VOLUME_LITERS_COLUMN_NAME, REAL),
                new Pair<>(ALCOHOL_BY_VOLUME_COLUMN_NAME, REAL),
                new Pair<>(NOTE_COLUMN_NAME, TEXT_NULL),
                new Pair<>(TEMPORAL_TYPE_COLUMN_NAME, INTEGER));
    }

    @Override
    public String getMainTableName() {
        return ALCOHOL_CONSUMPTION_RECORD_TABLE_NAME;
    }

    /** Creates the alcohol consumption table. */
    public void applyUpgrade(SQLiteDatabase db) {
        createTable(db, getCreateTableRequest());
    }
}
