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

import static com.android.healthfitness.flags.AconfigFlagHelper.getDbVersion;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Slog;

import androidx.annotation.VisibleForTesting;

import com.android.server.healthconnect.storage.request.CreateTableRequest;

import java.io.File;

/**
 * Class to maintain the health connect DB. Actual operations are performed by {@link
 * TransactionManager}
 *
 * @hide
 */
public final class HealthConnectDatabase extends SQLiteOpenHelper {
    private static final String TAG = "HealthConnectDatabase";

    @VisibleForTesting public static final String DEFAULT_DATABASE_NAME = "healthconnect.db";
    private final HealthConnectContext mContext;

    public HealthConnectDatabase(HealthConnectContext context) {
        this(context, DEFAULT_DATABASE_NAME);
    }

    public HealthConnectDatabase(HealthConnectContext context, String databaseName) {
        super(context, databaseName, null, getDbVersion());
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        DatabaseUpgradeHelper.onUpgrade(db, 0, getDbVersion());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DatabaseUpgradeHelper.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        // Enforce FK constraints for DB writes
        // This is also required for when we delete entries, for cascade to work
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Slog.i(TAG, "onDowngrade oldVersion = " + oldVersion + " newVersion = " + newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        DevelopmentDatabaseHelper.onOpen(db);
    }

    public File getDatabasePath() {
        return mContext.getDatabasePath(getDatabaseName());
    }

    /** Runs create table request on database. */
    public static void createTable(SQLiteDatabase db, CreateTableRequest createTableRequest) {
        db.execSQL(createTableRequest.getCreateCommand());
        createTableRequest.getCreateIndexStatements().forEach(db::execSQL);
        for (CreateTableRequest childRequest : createTableRequest.getChildTableRequests()) {
            createTable(db, childRequest);
        }
    }
}
