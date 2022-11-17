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
import android.healthconnect.internal.datatypes.BasalMetabolicRateRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for BasalMetabolicRateRecord.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE)
public final class BasalMetabolicRateRecordHelper
        extends InstantRecordHelper<BasalMetabolicRateRecordInternal> {
    private static final String BASAL_METABOLIC_RATE_RECORD_TABLE_NAME =
            "basal_metabolic_rate_record_table";
    private static final String BASAL_METABOLIC_RATE_COLUMN_NAME = "basal_metabolic_rate";

    @Override
    @NonNull
    public String getMainTableName() {
        return BASAL_METABOLIC_RATE_RECORD_TABLE_NAME;
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull BasalMetabolicRateRecordInternal basalMetabolicRateRecord) {
        contentValues.put(
                BASAL_METABOLIC_RATE_COLUMN_NAME, basalMetabolicRateRecord.getBasalMetabolicRate());
    }

    @Override
    protected void populateSpecificRecordValue(
            @NonNull Cursor cursor, @NonNull BasalMetabolicRateRecordInternal recordInternal) {
        recordInternal.setBasalMetabolicRate(
                getCursorDouble(cursor, BASAL_METABOLIC_RATE_COLUMN_NAME));
    }

    @Override
    @NonNull
    protected List<Pair<String, String>> getInstantRecordColumnInfo() {
        return Collections.singletonList(new Pair<>(BASAL_METABOLIC_RATE_COLUMN_NAME, REAL));
    }
}
