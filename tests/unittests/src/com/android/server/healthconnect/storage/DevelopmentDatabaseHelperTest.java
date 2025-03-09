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

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE;
import static com.android.server.healthconnect.storage.DatabaseTestUtils.createEmptyDatabase;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.base.Preconditions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class DevelopmentDatabaseHelperTest {

    private static final SQLiteDatabase.OpenParams READ_ONLY =
            new SQLiteDatabase.OpenParams.Builder()
                    .setOpenFlags(SQLiteDatabase.OPEN_READONLY)
                    .build();

    private HealthConnectContext mHcContext;

    @Rule(order = 0)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mHcContext =
                HealthConnectContext.create(
                        context,
                        context.getUser(),
                        /* databaseDirName= */ null,
                        mTemporaryFolder.getRoot());
    }

    @Test
    @EnableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testChangesIdempotent() {
        // Database changes should be idempotent so you don't leave a teammate on a development
        // database that can't be fixed after switching the flag on or off.
        // Test this is true by running onOpen twice, dropping the version in between.

        try (HealthConnectDatabase helper = new HealthConnectDatabase(mHcContext)) {
            // make sure a database file exists
            SQLiteDatabase db = helper.getWritableDatabase();
            // Drop the settings table to make sure the update code is run completely a second time.
            DevelopmentDatabaseHelper.dropDevelopmentSettingsTable(db);
            // Force a second run of onOpen(), and make sure there are no errors
            DevelopmentDatabaseHelper.onOpen(db);
        }
    }

    @Test
    @EnableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testOnOpen_readOnlyDatabase_successful() {
        // GIVEN we have a guaranteed read only database.
        File databaseFile;
        try (HealthConnectDatabase helper = new HealthConnectDatabase(mHcContext)) {
            // make sure a database file exists
            helper.getWritableDatabase();
            databaseFile = helper.getDatabasePath();
        }
        // Change it to read only
        Preconditions.checkState(databaseFile.setReadOnly());
        // Check the above code works
        Preconditions.checkState(databaseFile.canRead());
        Preconditions.checkState(!databaseFile.canWrite());
        try (SQLiteDatabase readOnlyDatabase =
                SQLiteDatabase.openDatabase(databaseFile, READ_ONLY)) {
            Preconditions.checkState(readOnlyDatabase.isReadOnly());

            // WHEN we call onOpen on the read only database THEN there are no errors.
            DevelopmentDatabaseHelper.onOpen(readOnlyDatabase);
        }
    }

    @Test
    @EnableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testGetOldVersionIfExists_nonExistent() {
        try (SQLiteDatabase db = createEmptyDatabase()) {

            int version = DevelopmentDatabaseHelper.getOldVersionIfExists(db);

            assertThat(version).isEqualTo(DevelopmentDatabaseHelper.NO_DEV_VERSION);
        }
    }

    @Test
    @EnableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testDropAndCreateDevelopmentSettings_nonExistent_creates() {
        try (SQLiteDatabase db = createEmptyDatabase()) {
            int version = 26;

            DevelopmentDatabaseHelper.dropAndCreateDevelopmentSettingsTable(db, version);

            assertThat(DevelopmentDatabaseHelper.getOldVersionIfExists(db)).isEqualTo(version);
        }
    }

    @Test
    @EnableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testDropAndCreateDevelopmentSettings_existent_overwrites() {
        try (HealthConnectDatabase helper = new HealthConnectDatabase(mHcContext)) {
            // getWriteableDatabase() triggers onOpen(), so the dev database with
            // version CURRENT_VERSION should be created.
            SQLiteDatabase db = helper.getWritableDatabase();
            int version = 26;

            DevelopmentDatabaseHelper.dropAndCreateDevelopmentSettingsTable(db, version);

            assertThat(DevelopmentDatabaseHelper.getOldVersionIfExists(db)).isEqualTo(version);
        }
    }

    @Test
    @DisableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testOnOpen_notDevelopment_deletesDevelopmentTables() {
        try (HealthConnectDatabase helper = new HealthConnectDatabase(mHcContext)) {
            // Calling getWritableDatabase() triggers onOpen(). With the flag off,
            // should delete the development database.
            SQLiteDatabase db = helper.getWritableDatabase();
            // Now the development database should not be present.
            // Create a table that looks like some old development settings.
            // GIVEN we have some old development database settings
            DevelopmentDatabaseHelper.dropAndCreateDevelopmentSettingsTable(
                    db, DevelopmentDatabaseHelper.CURRENT_VERSION);

            // WHEN onOpen is called
            DevelopmentDatabaseHelper.onOpen(db);

            // THEN the settings table should be deleted
            assertThat(DevelopmentDatabaseHelper.getOldVersionIfExists(db))
                    .isEqualTo(DevelopmentDatabaseHelper.NO_DEV_VERSION);
        }
    }

    @Test
    @EnableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testOnOpen_isDevelopmentHasDevelopmentTables_noChange() {
        // GIVEN we have some current development database settings, and the flags are enabled
        try (SQLiteDatabase db = createEmptyDatabase()) {
            DevelopmentDatabaseHelper.dropAndCreateDevelopmentSettingsTable(
                    db, DevelopmentDatabaseHelper.CURRENT_VERSION);

            // WHEN onOpen is called
            DevelopmentDatabaseHelper.onOpen(db);

            // THEN the settings table should be left, and nothing changed
            assertThat(DevelopmentDatabaseHelper.getOldVersionIfExists(db))
                    .isEqualTo(DevelopmentDatabaseHelper.CURRENT_VERSION);
        }
    }

    @Test
    @EnableFlags(FLAG_DEVELOPMENT_DATABASE)
    public void testOnOpen_oldDevelopmentSettingsTable_createsNew() {
        try (SQLiteDatabase db = createEmptyDatabase()) {
            DevelopmentDatabaseHelper.dropAndCreateDevelopmentSettingsTable(
                    db, DevelopmentDatabaseHelper.CURRENT_VERSION - 1);

            DevelopmentDatabaseHelper.onOpen(db);

            assertThat(DevelopmentDatabaseHelper.getOldVersionIfExists(db))
                    .isEqualTo(DevelopmentDatabaseHelper.CURRENT_VERSION);
        }
    }
}
