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

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MENSTRUAL_CYCLE_PHASE;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.isNullValue;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.datatypes.MenstrualCyclePhaseRecord;
import android.health.connect.internal.datatypes.MenstrualCyclePhaseRecordInternal;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class for {@link MenstrualCyclePhaseRecord}
 *
 * @hide
 */
public final class MenstrualCyclePhaseRecordHelper
        extends IntervalRecordHelper<MenstrualCyclePhaseRecordInternal> {
    @VisibleForTesting public static final String TABLE_NAME = "menstrual_cycle_phase_record_table";

    @VisibleForTesting public static final String PHASE_COLUMN_NAME = "phase";
    @VisibleForTesting public static final String DAY_OF_CYCLE_COLUMN_NAME = "day_of_cycle";

    public MenstrualCyclePhaseRecordHelper() {
        super(RECORD_TYPE_MENSTRUAL_CYCLE_PHASE);
    }

    @Override
    public String getMainTableName() {
        return TABLE_NAME;
    }

    @Override
    List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Arrays.asList(
                new Pair<>(PHASE_COLUMN_NAME, INTEGER_NOT_NULL),
                new Pair<>(DAY_OF_CYCLE_COLUMN_NAME, INTEGER));
    }

    @Override
    MenstrualCyclePhaseRecordInternal populateSpecificRecordValue(Cursor cursor) {
        MenstrualCyclePhaseRecordInternal recordInternal =
                new MenstrualCyclePhaseRecordInternal()
                        .setPhase(getCursorInt(cursor, PHASE_COLUMN_NAME));
        if (!isNullValue(cursor, DAY_OF_CYCLE_COLUMN_NAME)) {
            recordInternal.setDayOfCycle(getCursorInt(cursor, DAY_OF_CYCLE_COLUMN_NAME));
        }
        return recordInternal;
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues, MenstrualCyclePhaseRecordInternal recordInternal) {
        contentValues.put(PHASE_COLUMN_NAME, recordInternal.getPhase());
        int dayOfCycle = recordInternal.getDayOfCycle();
        contentValues.put(
                DAY_OF_CYCLE_COLUMN_NAME, (dayOfCycle != DEFAULT_INT) ? dayOfCycle : null);
    }
}
