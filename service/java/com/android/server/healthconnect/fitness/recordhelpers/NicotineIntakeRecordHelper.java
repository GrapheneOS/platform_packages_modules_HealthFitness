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
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorDouble;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.NicotineIntakeRecordInternal;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;

import java.util.List;

/**
 * Helper class for {@link android.health.connect.datatypes.NicotineIntakeRecord}.
 *
 * @hide
 */
public class NicotineIntakeRecordHelper extends IntervalRecordHelper<NicotineIntakeRecordInternal> {

    @VisibleForTesting public static final String TABLE_NAME = "nicotine_intake_record_table";

    @VisibleForTesting
    public static final String NICOTINE_INTAKE_TYPE_COLUMN_NAME = "nicotine_intake_type";

    @VisibleForTesting public static final String QUANTITY_COLUMN_NAME = "quantity";
    @VisibleForTesting public static final String NICOTINE_INTAKE_COLUMN_NAME = "nicotine_intake";

    public NicotineIntakeRecordHelper() {
        super(RecordTypeIdentifier.RECORD_TYPE_NICOTINE_INTAKE);
    }

    @Override
    NicotineIntakeRecordInternal populateSpecificRecordValue(Cursor cursor) {
        return new NicotineIntakeRecordInternal()
                .setNicotineIntakeType(getCursorInt(cursor, NICOTINE_INTAKE_TYPE_COLUMN_NAME))
                .setQuantity(getCursorInt(cursor, QUANTITY_COLUMN_NAME))
                .setNicotineIntakeGrams(getCursorDouble(cursor, NICOTINE_INTAKE_COLUMN_NAME));
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues, NicotineIntakeRecordInternal recordInternal) {
        contentValues.put(NICOTINE_INTAKE_TYPE_COLUMN_NAME, recordInternal.getNicotineIntakeType());
        contentValues.put(QUANTITY_COLUMN_NAME, recordInternal.getQuantity());
        contentValues.put(NICOTINE_INTAKE_COLUMN_NAME, recordInternal.getNicotineIntakeGrams());
    }

    @Override
    List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return List.of(
                new Pair<>(NICOTINE_INTAKE_TYPE_COLUMN_NAME, INTEGER),
                new Pair<>(QUANTITY_COLUMN_NAME, INTEGER),
                new Pair<>(NICOTINE_INTAKE_COLUMN_NAME, REAL));
    }

    @Override
    public String getMainTableName() {
        return TABLE_NAME;
    }

    /** Creates the nicotine intake table. */
    public void applyNicotineIntakeUpgrade(SQLiteDatabase db) {
        createTable(db, getCreateTableRequest());
    }
}
