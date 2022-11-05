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

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;

import java.util.Collection;

/**
 * Class to maintain the health connect DB. Actual operations are performed by {@link
 * TransactionManager}
 *
 * @hide
 */
public class HealthConnectDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "healthconnect.db";

    @NonNull private final Collection<RecordHelper<?>> mRecordHelpers;

    public HealthConnectDatabase(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mRecordHelpers = RecordHelperProvider.getInstance().getRecordHelpers().values();
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        mRecordHelpers.forEach(
                recordHelper -> {
                    createTable(db, recordHelper.getCreateTableRequest());
                });
        createTable(db, DeviceInfoHelper.getInstance().getCreateTableRequest());
        createTable(db, AppInfoHelper.getInstance().getCreateTableRequest());
        createTable(db, ChangeLogsHelper.getInstance().getCreateTableRequest());
        createTable(db, ChangeLogsRequestHelper.getInstance().getCreateTableRequest());
        createTable(db, HealthDataCategoryPriorityHelper.getInstance().getCreateTableRequest());
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        mRecordHelpers.forEach(
                recordHelper -> {
                    recordHelper.onUpgrade(newVersion, db);
                });
        DeviceInfoHelper.getInstance().onUpgrade(newVersion, db);
        AppInfoHelper.getInstance().onUpgrade(newVersion, db);
        ChangeLogsHelper.getInstance().onUpgrade(newVersion, db);
        ChangeLogsRequestHelper.getInstance().onUpgrade(newVersion, db);
        HealthDataCategoryPriorityHelper.getInstance().onUpgrade(newVersion, db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        // Enforce FK constraints for DB writes as we want to enforce FK constraints on DB write.
        // This is also required for when we delete entries, for cascade to work
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }

    private void createTable(SQLiteDatabase db, CreateTableRequest createTableRequest) {
        db.execSQL(createTableRequest.getCreateCommand());
        createTableRequest
                .getChildTableRequests()
                .forEach(
                        (childTableRequest) -> {
                            createTable(db, childTableRequest);
                        });
        createTableRequest.getCreateIndexStatements().forEach(db::execSQL);
    }
}
