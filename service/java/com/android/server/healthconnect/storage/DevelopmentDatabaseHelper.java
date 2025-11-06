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

package com.android.server.healthconnect.storage;

import static com.android.server.healthconnect.storage.DatabaseUpgradeHelper.executeSqlStatements;
import static com.android.server.healthconnect.storage.HealthConnectDatabase.createTable;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkColumnExists;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkTableExists;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.storage.request.AlterTableRequest;

/**
 * Code to manage development features of the Health Connect database before they are ready for
 * release.
 *
 * <p>Note this functionality should not be used for database upgrades or downgrades that effect
 * users or for rolling out flags. This should only be used for managing changes while a feature is
 * in development, and only developers will be affected.
 *
 * @hide
 */
public final class DevelopmentDatabaseHelper {
    private static final String TAG = "HealthConnectDevDb";

    /**
     * The current version number for the development database features. Increment this whenever you
     * make a breaking schema change to a development feature.
     */
    @VisibleForTesting static final int CURRENT_VERSION = 23;

    /** The name of the table to store development specific key value pairs. */
    private static final String SETTINGS_TABLE_NAME = "development_database_settings";

    /**
     * Indicates that a development version could not read, and there was no development version of
     * the database.
     */
    @VisibleForTesting static final int NO_DEV_VERSION = -1;

    /** The key in the settings table for the database development version. */
    private static final String VERSION_KEY = "version";

    /**
     * On opening a database, look at the current features in development and see if any
     * modifications need to be made to the database.
     *
     * <p>This is similar to {@link android.database.sqlite.SQLiteOpenHelper#onUpgrade} and {@link
     * android.database.sqlite.SQLiteOpenHelper#onDowngrade} but uses different versioning which
     * only depends on features still in development.
     */
    public static void onOpen(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            return;
        }
        if (!Flags.developmentDatabase()) {
            // Use straight SQL to isolate development infrastructure from prod code.
            try {
                dropDevelopmentSettingsTable(db);
            } catch (SQLException ex) {
                // In the event of failure for a non development user, carry on silently.
                // There is nothing that can be done.
                Slog.w(TAG, "Unable to drop dev db settings table", ex);
            }
            return;
        }
        // Deliberately don't catch SQLExceptions beyond this point. For users here
        // they have the development flag on, so crashing in the event of bugs is useful.
        int oldVersion = getOldVersionIfExists(db);
        if (oldVersion == CURRENT_VERSION) {
            return;
        }

        // Beyond this point are the development database changes
        dropAndCreateDevelopmentSettingsTable(db, CURRENT_VERSION);

        // Code for under development schema changes goes in this method but below this comment
        applyDdpAppInfoDatabaseUpgrade(db);
        applyDeviceInfoEnhancementsDatabaseUpgrade(db);
        applyDdpDatabaseUpgrade(db, oldVersion);
        applyDdpMetadataDatabaseUpgrade(db);
    }

    private static void applyDdpAppInfoDatabaseUpgrade(SQLiteDatabase db) {
        if (checkColumnExists(
                db, AppInfoHelper.TABLE_NAME, AppInfoHelper.DEVICE_INFO_ID_COLUMN_NAME)) {
            return;
        }

        AlterTableRequest alterAppInfoRequest = AppInfoHelper.getAlterTableRequestForDdpInfo();
        executeSqlStatements(db, alterAppInfoRequest.getAddColumnsCommands());
    }

    private static void applyDeviceInfoEnhancementsDatabaseUpgrade(SQLiteDatabase db) {
        if (checkColumnExists(
                db, DeviceInfoHelper.TABLE_NAME, DeviceInfoHelper.DEVICE_ID_COLUMN_NAME)) {
            // Upgrade has already been applied. Return early.
            return;
        }
        executeSqlStatements(db, DeviceInfoHelper.getAlterTableRequest().getAddColumnsCommands());
    }

    private static void applyDdpDatabaseUpgrade(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 17) {
            // Version 16 adds unique column constraints
            // Version 17 adds a new column
            dropTableIfExists(db, DeviceDataProviderHelper.TABLE_NAME);
        }

        if (checkTableExists(db, DeviceDataProviderHelper.TABLE_NAME)) {
            // Upgrade has already been applied. Return early.
            return;
        }
        createTable(db, DeviceDataProviderHelper.getCreateTableRequest());
    }

    private static void applyDdpMetadataDatabaseUpgrade(SQLiteDatabase db) {
        if (checkTableExists(db, DeviceDataProviderMetadataHelper.TABLE_NAME)) {
            return;
        }

        createTable(db, DeviceDataProviderMetadataHelper.getCreateTableRequest());
    }

    @VisibleForTesting
    static void dropAndCreateDevelopmentSettingsTable(SQLiteDatabase db, int version) {
        // We are now on a development device moving either from a prod version to a development
        // version, or between two development versions. Drop and recreate the relevant tables.
        dropDevelopmentSettingsTable(db);
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + SETTINGS_TABLE_NAME
                        + "(name TEXT PRIMARY KEY, value TEXT);");
        db.execSQL(
                "INSERT INTO " + SETTINGS_TABLE_NAME + " (name,value) VALUES (?,?)",
                new Object[] {VERSION_KEY, Integer.toString(version)});
    }

    @VisibleForTesting
    static void dropDevelopmentSettingsTable(SQLiteDatabase db) {
        dropTableIfExists(db, SETTINGS_TABLE_NAME);
    }

    @VisibleForTesting
    static int getOldVersionIfExists(SQLiteDatabase db) {
        if (!settingsTableExists(db)) {
            return NO_DEV_VERSION;
        }
        return readIntSetting(db, NO_DEV_VERSION);
    }

    private static void dropTableIfExists(SQLiteDatabase db, String table) {
        db.execSQL("DROP TABLE IF EXISTS " + table);
    }

    private static int readIntSetting(SQLiteDatabase db, int defaultValue) {
        try (Cursor cursor =
                db.rawQuery(
                        "SELECT value FROM " + SETTINGS_TABLE_NAME + " WHERE name=?",
                        new String[] {VERSION_KEY})) {
            if (cursor.moveToFirst()) {
                try {
                    return Integer.parseInt(cursor.getString(0));
                } catch (NumberFormatException ex) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        }
    }

    private static boolean settingsTableExists(SQLiteDatabase db) {
        try (Cursor cursor =
                db.rawQuery(
                        "SELECT count(*) FROM sqlite_master WHERE type=? AND name=?",
                        new String[] {"table", SETTINGS_TABLE_NAME})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) == 1;
            } else {
                return false;
            }
        }
    }
}
