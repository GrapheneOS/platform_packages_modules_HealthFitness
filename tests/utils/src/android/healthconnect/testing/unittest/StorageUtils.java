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

package android.healthconnect.testing.unittest;

import static com.google.common.truth.Truth.assertThat;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;

import java.io.File;
import java.util.List;

public class StorageUtils {

    static final File MOCK_DATABASE_PATH =
            InstrumentationRegistry.getInstrumentation().getContext().getDatabasePath("mock");
    private final TransactionManager mTransactionManager;

    public StorageUtils(HealthConnectInjector healthConnectInjector) {
        mTransactionManager = healthConnectInjector.getTransactionManager();
    }

    /** Creates a database with no tables and no data. */
    public static SQLiteDatabase createEmptyDatabase() {
        clearDatabase();
        return SQLiteDatabase.openOrCreateDatabase(MOCK_DATABASE_PATH, /* cursorFactory= */ null);
    }

    /** Deletes the database file created using createEmptyDatabase. */
    public static void clearDatabase() {
        if (MOCK_DATABASE_PATH.exists()) {
            assertThat(MOCK_DATABASE_PATH.delete()).isTrue();
        }
    }

    /** Asserts that a list of {@code columns} exist in the specified {@code table}. */
    public static void assertColumnsExist(SQLiteDatabase db, String table, List<String> columns) {
        try (Cursor cursor =
                db.rawQuery("SELECT * FROM " + table + " LIMIT 1", /* selectArgs */ null)) {
            for (String column : columns) {
                assertThat(cursor.getColumnIndex(column)).isNotEqualTo(-1);
            }
        }
    }

    /** Asserts the number of tables in the database. */
    public static void assertNumberOfTables(SQLiteDatabase db, int expected) {
        Cursor cursor =
                db.rawQuery(
                        "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND"
                                + " name != 'android_metadata' AND name != 'sqlite_sequence';",
                        null);

        cursor.moveToNext();
        assertThat(cursor.getInt(0)).isEqualTo(expected);
    }

    /** Asserts that a list of {@code tables} exist. */
    public static void assertTablesExists(SQLiteDatabase db, List<String> tables) {
        for (String table : tables) {
            long numEntries =
                    DatabaseUtils.queryNumEntries(
                            db,
                            "sqlite_master",
                            /* selection= */ "type = 'table' AND name == '" + table + "'",
                            /* selectionArgs= */ null);
            assertThat(numEntries).isGreaterThan(0);
        }
    }

    /** Returns the number of rows in the specified table. */
    public static long queryNumEntries(HealthConnectDatabase database, String tableName) {
        return DatabaseUtils.queryNumEntries(database.getReadableDatabase(), tableName);
    }

    /** Returns the number of rows in the specified table. */
    public long queryNumEntries(String tableName) {
        return mTransactionManager.queryNumEntries(tableName);
    }
}
