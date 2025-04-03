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

import static com.google.common.truth.Truth.assertThat;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;

public final class DatabaseTestUtils {
    // The number of table we released to the public. This number can only increase, as we are not
    // allowed to make changes that remove tables or columns.
    // Development tables that haven't reached prod are excluded.
    static final int NUM_OF_TABLES = 67;
    static final File MOCK_DATABASE_PATH =
            InstrumentationRegistry.getInstrumentation().getContext().getDatabasePath("mock");

    static void assertNumberOfTables(SQLiteDatabase db, int expected) {
        Cursor cursor =
                db.rawQuery(
                        "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND"
                                + " name != 'android_metadata' AND name != 'sqlite_sequence';",
                        null);

        cursor.moveToNext();
        assertThat(cursor.getInt(0)).isEqualTo(expected);
    }

    static @NonNull SQLiteDatabase createEmptyDatabase() {
        return createEmptyDatabase(MOCK_DATABASE_PATH);
    }

    static @NonNull SQLiteDatabase createEmptyDatabase(File databasePath) {
        clearDatabase(databasePath);
        return SQLiteDatabase.openOrCreateDatabase(databasePath, /* cursorFactory= */ null);
    }

    static void clearDatabase() {
        clearDatabase(MOCK_DATABASE_PATH);
    }

    static void clearDatabase(File databasePath) {
        if (databasePath.exists()) {
            assertThat(databasePath.delete()).isTrue();
        }
    }
}
