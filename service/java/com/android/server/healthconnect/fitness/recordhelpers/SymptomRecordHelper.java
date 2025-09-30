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

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.SymptomRecordInternal;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.List;

/**
 * A helper class for SymptomsRecord.
 *
 * @hide
 */
public final class SymptomRecordHelper extends IntervalRecordHelper<SymptomRecordInternal> {

    @VisibleForTesting public static final String TABLE_NAME = "symptom_record_table";
    @VisibleForTesting public static final String SYMPTOM_TYPE_COLUMN_NAME = "symptom_type";
    @VisibleForTesting public static final String NOTES_COLUMN_NAME = "notes";
    @VisibleForTesting public static final String SEVERITY_COLUMN_NAME = "severity";
    @VisibleForTesting public static final String COUNT_COLUMN_NAME = "count";
    @VisibleForTesting public static final String TEMPORAL_TYPE_COLUMN_NAME = "temporal_type";

    public SymptomRecordHelper() {
        super(RecordTypeIdentifier.RECORD_TYPE_SYMPTOM);
    }

    @Override
    public String getMainTableName() {
        return TABLE_NAME;
    }

    @Override
    SymptomRecordInternal populateSpecificRecordValue(Cursor cursor) {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(getCursorInt(cursor, SYMPTOM_TYPE_COLUMN_NAME));
        record.setNotes(getCursorString(cursor, NOTES_COLUMN_NAME));
        record.setSeverity(getCursorInt(cursor, SEVERITY_COLUMN_NAME));
        record.setCount(getCursorInt(cursor, COUNT_COLUMN_NAME));
        record.setTemporalType(getCursorInt(cursor, TEMPORAL_TYPE_COLUMN_NAME));
        return record;
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues, SymptomRecordInternal symptomRecord) {
        contentValues.put(SYMPTOM_TYPE_COLUMN_NAME, symptomRecord.getSymptomType());
        contentValues.put(NOTES_COLUMN_NAME, symptomRecord.getNotes());
        contentValues.put(SEVERITY_COLUMN_NAME, symptomRecord.getSeverity());
        contentValues.put(COUNT_COLUMN_NAME, symptomRecord.getCount());
        contentValues.put(TEMPORAL_TYPE_COLUMN_NAME, symptomRecord.getTemporalType());
    }

    @Override
    List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Arrays.asList(
                new Pair<>(SYMPTOM_TYPE_COLUMN_NAME, INTEGER),
                new Pair<>(NOTES_COLUMN_NAME, TEXT_NULL),
                new Pair<>(SEVERITY_COLUMN_NAME, INTEGER),
                new Pair<>(COUNT_COLUMN_NAME, INTEGER),
                new Pair<>(TEMPORAL_TYPE_COLUMN_NAME, INTEGER));
    }
}
