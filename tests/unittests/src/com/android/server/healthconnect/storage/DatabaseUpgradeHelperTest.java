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

import static android.database.DatabaseUtils.queryNumEntries;

import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_MINDFULNESS_SESSION;
import static com.android.healthfitness.flags.DatabaseVersions.MIN_SUPPORTED_DB_VERSION;
import static com.android.healthfitness.flags.Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES;
import static com.android.server.healthconnect.storage.DatabaseTestUtils.assertNumberOfTables;
import static com.android.server.healthconnect.storage.DatabaseTestUtils.clearDatabase;
import static com.android.server.healthconnect.storage.DatabaseTestUtils.createEmptyDatabase;
import static com.android.server.healthconnect.storage.DatabaseUpgradeHelper.onUpgrade;

import static com.google.common.truth.Truth.assertThat;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.FlagsParameterization;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import platform.test.runner.parameterized.ParameterizedAndroidJunit4;
import platform.test.runner.parameterized.Parameters;

import java.util.List;

@RunWith(ParameterizedAndroidJunit4.class)
public class DatabaseUpgradeHelperTest {
    @Rule public final SetFlagsRule mSetFlagsRule;

    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.allCombinationsOf(FLAG_INFRA_TO_GUARD_DB_CHANGES);
    }

    public DatabaseUpgradeHelperTest(FlagsParameterization flags) {
        mSetFlagsRule = new SetFlagsRule(flags);
    }

    private static final int NUM_OF_TABLES_AT_MIN_SUPPORTED_VERSION = 57;
    private static final int NUM_OF_TABLES_AT_MINDFULNESS_VERSION = 64;
    private static final int NUM_OF_TABLES_IN_STAGING = 70;
    private static final int LATEST_DB_VERSION_IN_STAGING = DB_VERSION_CLOUD_BACKUP_AND_RESTORE;

    private SQLiteDatabase mSQLiteDatabase;

    @Before
    public void setUp() {
        mSQLiteDatabase = createEmptyDatabase();
        assertNumberOfTables(mSQLiteDatabase, 0);
    }

    @After
    public void tearDown() {
        clearDatabase();
    }

    /*
     * If you find that this test is failing, it means that your database upgrade cannot be applied
     * multiple times. Making a database upgrade idempotent can often be easily achieved by
     * specifying e.g. 'IF NOT EXISTS'.
     */
    @Test
    public void onUpgrade_calledMultipleTimes_eachOneIsIdempotent() {
        onUpgrade(mSQLiteDatabase, 0, LATEST_DB_VERSION_IN_STAGING);

        // We do idempotent upgrades above MIN_SUPPORTED_DB_VERSION
        onUpgrade(mSQLiteDatabase, MIN_SUPPORTED_DB_VERSION, LATEST_DB_VERSION_IN_STAGING);
        // TODO(b/338031465): Improve testing, check that schema indeed match.
        assertDbSchemaUpToDate();
    }

    // For historical reasons, we don't have schema tests before mindfulness session, so we opt for
    // testing the easiest: number of table.
    @Test
    @EnableFlags(FLAG_INFRA_TO_GUARD_DB_CHANGES)
    public void onUpgrade_upToMindfulnessSession_numOfTablesMatches() {
        onUpgrade(mSQLiteDatabase, 0, DB_VERSION_MINDFULNESS_SESSION);
        assertNumberOfTables(mSQLiteDatabase, NUM_OF_TABLES_AT_MINDFULNESS_VERSION);
    }

    @Test
    public void onUpgrade_newVersionGreaterThanMaxSupportedVersion_upgradeToMaxSupportedVersion() {
        onUpgrade(mSQLiteDatabase, 0, Integer.MAX_VALUE);
        assertDbSchemaUpToDate();
    }

    @Test
    @EnableFlags(FLAG_INFRA_TO_GUARD_DB_CHANGES)
    public void onUpgrade_newVersionSpecified_upgradeUntilNewVersionReached() {
        onUpgrade(mSQLiteDatabase, 0, MIN_SUPPORTED_DB_VERSION);
        assertNumberOfTables(mSQLiteDatabase, NUM_OF_TABLES_AT_MIN_SUPPORTED_VERSION);
    }

    /**
     * Asserts that the db schema of {@link #LATEST_DB_VERSION_IN_STAGING} matches the desired
     * schema.
     */
    private void assertDbSchemaUpToDate() {
        assertNumberOfTables(mSQLiteDatabase, NUM_OF_TABLES_IN_STAGING);

        // PHR
        assertTablesExists(
                mSQLiteDatabase,
                List.of(
                        MedicalDataSourceHelper.getMainTableName(),
                        MedicalResourceHelper.getMainTableName(),
                        MedicalResourceIndicesHelper.getTableName(),
                        ReadAccessLogsHelper.TABLE_NAME));
        assertColumnsExist(
                mSQLiteDatabase,
                AccessLogsHelper.TABLE_NAME,
                List.of("medical_resource_type", "medical_data_source_accessed"));
    }

    /** Asserts that a list of {@code columns} exist in the specified {@code table}. */
    private static void assertColumnsExist(SQLiteDatabase db, String table, List<String> columns) {
        try (Cursor cursor =
                db.rawQuery("SELECT * FROM " + table + " LIMIT 1", /* selectArgs */ null)) {
            for (String column : columns) {
                assertThat(cursor.getColumnIndex(column)).isNotEqualTo(-1);
            }
        }
    }

    /** Asserts that a list of {@code tables} exist. */
    private static void assertTablesExists(SQLiteDatabase db, List<String> tables) {
        for (String table : tables) {
            long numEntries =
                    queryNumEntries(
                            db,
                            "sqlite_master",
                            /* selection= */ "type = 'table' AND name == '" + table + "'",
                            /* selectionArgs= */ null);
            assertThat(numEntries).isGreaterThan(0);
        }
    }
}
