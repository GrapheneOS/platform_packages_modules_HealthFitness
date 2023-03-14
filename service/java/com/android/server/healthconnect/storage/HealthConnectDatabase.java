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
import android.util.Log;

import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ActivityDateHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MigrationEntityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;

import java.io.File;
import java.util.Collection;

/**
 * Class to maintain the health connect DB. Actual operations are performed by {@link
 * TransactionManager}
 *
 * @hide
 */
public class HealthConnectDatabase extends SQLiteOpenHelper {
    private static final String TAG = "HealthConnectDatabase";
    private static final int DATABASE_VERSION = 5; // Last bumped on 2023-03-13T11:47:48Z
    private static final String DATABASE_NAME = "healthconnect.db";

    @NonNull private final Collection<RecordHelper<?>> mRecordHelpers;
    private final Context mContext;

    /** Runs create table request on database. */
    public static void createTable(SQLiteDatabase db, CreateTableRequest createTableRequest) {
        db.execSQL(createTableRequest.getCreateCommand());
        createTableRequest
                .getChildTableRequests()
                .forEach(
                        (childTableRequest) -> {
                            createTable(db, childTableRequest);
                        });
        createTableRequest.getCreateIndexStatements().forEach(db::execSQL);
    }

    public HealthConnectDatabase(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mRecordHelpers = RecordHelperProvider.getInstance().getRecordHelpers().values();
        mContext = context;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        mRecordHelpers.forEach(
                recordHelper -> {
                    createTable(db, recordHelper.getCreateTableRequest());
                });
        createTable(db, DeviceInfoHelper.getInstance().getCreateTableRequest());
        createTable(db, AppInfoHelper.getInstance().getCreateTableRequest());
        createTable(db, ActivityDateHelper.getInstance().getCreateTableRequest());
        createTable(db, ChangeLogsHelper.getInstance().getCreateTableRequest());
        createTable(db, ChangeLogsRequestHelper.getInstance().getCreateTableRequest());
        createTable(db, HealthDataCategoryPriorityHelper.getInstance().getCreateTableRequest());
        createTable(db, PreferenceHelper.getInstance().getCreateTableRequest());
        createTable(db, AccessLogsHelper.getInstance().getCreateTableRequest());
        createTable(db, MigrationEntityHelper.getInstance().getCreateTableRequest());
        createTable(db, PriorityMigrationHelper.getInstance().getCreateTableRequest());
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        mRecordHelpers.forEach(
                recordHelper -> {
                    recordHelper.onUpgrade(db, oldVersion, newVersion);
                });
        DeviceInfoHelper.getInstance().onUpgrade(newVersion, db);
        AppInfoHelper.getInstance().onUpgrade(newVersion, db);
        ChangeLogsHelper.getInstance().onUpgrade(newVersion, db);
        ChangeLogsRequestHelper.getInstance().onUpgrade(newVersion, db);
        HealthDataCategoryPriorityHelper.getInstance().onUpgrade(newVersion, db);
        ActivityDateHelper.getInstance().onUpgrade(newVersion, db);
        MigrationEntityHelper.getInstance().onUpgrade(db, oldVersion);
        PriorityMigrationHelper.getInstance().onUpgrade(db, oldVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        // Enforce FK constraints for DB writes as we want to enforce FK constraints on DB write.
        // This is also required for when we delete entries, for cascade to work
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "onDowngrade oldVersion = " + oldVersion + " newVersion = " + newVersion);
    }

    public File getDatabasePath() {
        return mContext.getDatabasePath(DATABASE_NAME);
    }
}
