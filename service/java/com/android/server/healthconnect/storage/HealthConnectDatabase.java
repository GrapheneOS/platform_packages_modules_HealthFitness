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

package com.android.server.healthconnect.storage;

import android.annotation.NonNull;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;

import java.util.List;

/**
 * Class to maintain the health connect DB. Actual operations are performed by {@link
 * TransactionManager}
 *
 * @hide
 */
public class HealthConnectDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "healthconnect.db";

    @NonNull private final List<RecordHelper<?>> mRecordHelpers;

    public HealthConnectDatabase(
            @NonNull Context context, @NonNull List<RecordHelper<?>> recordHelpers) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mRecordHelpers = recordHelpers;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        mRecordHelpers.forEach(
                recordHelper -> {
                    db.execSQL(recordHelper.getCreateTableCommand());
                });
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        mRecordHelpers.forEach(
                recordHelper -> {
                    recordHelper.onUpgrade(newVersion, db);
                });
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }
}
