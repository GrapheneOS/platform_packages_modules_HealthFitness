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

import static android.healthconnect.testing.unittest.StorageUtils.assertColumnsExist;
import static android.healthconnect.testing.unittest.StorageUtils.assertNumberOfTables;
import static android.healthconnect.testing.unittest.StorageUtils.assertTablesExists;
import static android.healthconnect.testing.unittest.StorageUtils.clearDatabase;
import static android.healthconnect.testing.unittest.StorageUtils.createEmptyDatabase;

import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_ALCOHOL_CONSUMPTION;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_MINDFULNESS_SESSION;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_NICOTINE_INTAKE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_SYMPTOMS;
import static com.android.healthfitness.flags.DatabaseVersions.MIN_SUPPORTED_DB_VERSION;
import static com.android.healthfitness.flags.Flags.FLAG_ALCOHOL_CONSUMPTION_DB;
import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING_DB;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS_DB;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.ALCOHOL_CONSUMPTION_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.storage.DatabaseUpgradeHelper.onUpgrade;

import android.database.sqlite.SQLiteDatabase;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSegmentRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.NicotineIntakeRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SymptomRecordHelper;
import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceIndicesHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DatabaseUpgradeHelperTest {
    private static final int NUM_OF_TABLES_AT_MIN_SUPPORTED_VERSION = 57;
    private static final int NUM_OF_TABLES_AT_MINDFULNESS_VERSION = 64;
    private static final int NUM_OF_TABLES_AT_EXERCISE_SEGMENT_IMPROVEMENTS_VERSION = 70;
    private static final int NUM_OF_TABLES_AT_NICOTINE_INTAKE_VERSION = 71;
    private static final int NUM_OF_TABLES_AT_SYMPTOMS_VERSION = 72;
    private static final int NUM_OF_TABLES_AT_ALCOHOL_CONSUMPTION_VERSION = 73;
    private static final int NUM_OF_TABLES_IN_STAGING =
            NUM_OF_TABLES_AT_ALCOHOL_CONSUMPTION_VERSION;
    private static final int LATEST_DB_VERSION_IN_STAGING = DB_VERSION_ALCOHOL_CONSUMPTION;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

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
        assertNumberOfTables(mSQLiteDatabase, NUM_OF_TABLES_IN_STAGING);
    }

    // For historical reasons, we don't have schema tests before mindfulness session, so we opt for
    // testing the easiest: number of table.
    @Test
    public void onUpgrade_upToMindfulnessSession_numOfTablesMatches() {
        onUpgrade(mSQLiteDatabase, 0, DB_VERSION_MINDFULNESS_SESSION);
        assertNumberOfTables(mSQLiteDatabase, NUM_OF_TABLES_AT_MINDFULNESS_VERSION);
    }

    @Test
    public void onUpgrade_newVersionGreaterThanMaxSupportedVersion_upgradeToMaxSupportedVersion() {
        onUpgrade(mSQLiteDatabase, 0, Integer.MAX_VALUE);
        assertNumberOfTables(mSQLiteDatabase, NUM_OF_TABLES_IN_STAGING);
    }

    @Test
    public void onUpgrade_newVersionSpecified_upgradeUntilNewVersionReached() {
        onUpgrade(mSQLiteDatabase, 0, MIN_SUPPORTED_DB_VERSION);
        assertNumberOfTables(mSQLiteDatabase, NUM_OF_TABLES_AT_MIN_SUPPORTED_VERSION);
    }

    @Test
    @EnableFlags({
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void onUpgrade_addingNewColumn_calledMultipleTimes() {
        onUpgrade(mSQLiteDatabase, 0, DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS);
        assertColumnsExist(
                mSQLiteDatabase,
                ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_RECORD_TABLE_NAME,
                List.of(
                        ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_WEIGHT_GRAMS,
                        ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_SET_INDEX,
                        ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_RATE_OF_PERCEIVED_EXERTION));
        assertColumnsExist(
                mSQLiteDatabase,
                ExerciseSessionRecordHelper.EXERCISE_SESSION_RECORD_TABLE_NAME,
                List.of(ExerciseSessionRecordHelper.RATE_OF_PERCEIVED_EXERTION_COLUMN_NAME));

        onUpgrade(mSQLiteDatabase, 0, DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS);
        assertColumnsExist(
                mSQLiteDatabase,
                ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_RECORD_TABLE_NAME,
                List.of(
                        ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_WEIGHT_GRAMS,
                        ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_SET_INDEX,
                        ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_RATE_OF_PERCEIVED_EXERTION));
        assertColumnsExist(
                mSQLiteDatabase,
                ExerciseSessionRecordHelper.EXERCISE_SESSION_RECORD_TABLE_NAME,
                List.of(ExerciseSessionRecordHelper.RATE_OF_PERCEIVED_EXERTION_COLUMN_NAME));
        assertNumberOfTables(
                mSQLiteDatabase, NUM_OF_TABLES_AT_EXERCISE_SEGMENT_IMPROVEMENTS_VERSION);
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void onUpgrade_phrChangeLogs_schemaUpToDate() {
        try (var db = createEmptyDatabase()) {
            onUpgrade(db, 0, DB_VERSION_PHR_CHANGE_LOGS);

            assertNumberOfTables(db, NUM_OF_TABLES_AT_EXERCISE_SEGMENT_IMPROVEMENTS_VERSION);
            assertPHRTablesExist(db);
            assertColumnsExist(
                    db,
                    ChangeLogsRequestHelper.TABLE_NAME,
                    List.of(ChangeLogsRequestHelper.MEDICAL_RESOURCE_TYPES_COLUMN_NAME));
            assertColumnsExist(
                    db,
                    ChangeLogsHelper.TABLE_NAME,
                    List.of(
                            ChangeLogsHelper.MEDICAL_RESOURCE_TYPE_COLUMN_NAME,
                            ChangeLogsHelper.MEDICAL_DATA_SOURCE_ID_COLUMN_NAME));
        }
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void onUpgrade_phrChangeLogs_idempotent() {
        try (var db = createEmptyDatabase()) {
            onUpgrade(db, 0, DB_VERSION_PHR_CHANGE_LOGS);
            onUpgrade(db, 0, DB_VERSION_PHR_CHANGE_LOGS);

            assertNumberOfTables(db, NUM_OF_TABLES_AT_EXERCISE_SEGMENT_IMPROVEMENTS_VERSION);
            assertPHRTablesExist(db);
            assertColumnsExist(
                    db,
                    ChangeLogsRequestHelper.TABLE_NAME,
                    List.of(ChangeLogsRequestHelper.MEDICAL_RESOURCE_TYPES_COLUMN_NAME));
            assertColumnsExist(
                    db,
                    ChangeLogsHelper.TABLE_NAME,
                    List.of(
                            ChangeLogsHelper.MEDICAL_RESOURCE_TYPE_COLUMN_NAME,
                            ChangeLogsHelper.MEDICAL_DATA_SOURCE_ID_COLUMN_NAME));
        }
    }

    @Test
    @EnableFlags({
        FLAG_SMOKING,
        FLAG_SMOKING_DB,
    })
    public void onUpgrade_nicotineIntake_schemaUpToDate() {
        try (var db = createEmptyDatabase()) {
            onUpgrade(db, 0, DB_VERSION_NICOTINE_INTAKE);

            assertNumberOfTables(db, NUM_OF_TABLES_AT_NICOTINE_INTAKE_VERSION);
            assertColumnsExist(
                    db,
                    NicotineIntakeRecordHelper.TABLE_NAME,
                    List.of(
                            NicotineIntakeRecordHelper.NICOTINE_INTAKE_TYPE_COLUMN_NAME,
                            NicotineIntakeRecordHelper.QUANTITY_COLUMN_NAME,
                            NicotineIntakeRecordHelper.QUANTITY_COLUMN_NAME));
        }
    }

    @Test
    @EnableFlags({FLAG_SMOKING_DB, FLAG_SYMPTOMS_DB})
    public void onUpgrade_symptoms_schemaUpToDate() {
        try (var db = createEmptyDatabase()) {
            onUpgrade(db, 0, DB_VERSION_SYMPTOMS);

            assertNumberOfTables(db, NUM_OF_TABLES_AT_SYMPTOMS_VERSION);
            assertColumnsExist(
                    db,
                    SymptomRecordHelper.TABLE_NAME,
                    List.of(
                            SymptomRecordHelper.SYMPTOM_TYPE_COLUMN_NAME,
                            SymptomRecordHelper.NOTES_COLUMN_NAME,
                            SymptomRecordHelper.SEVERITY_COLUMN_NAME,
                            SymptomRecordHelper.COUNT_COLUMN_NAME,
                            SymptomRecordHelper.TEMPORAL_TYPE_COLUMN_NAME));
        }
    }

    @Test
    @EnableFlags({FLAG_ALCOHOL_CONSUMPTION_DB, FLAG_SYMPTOMS_DB, FLAG_SMOKING_DB})
    public void onUpgrade_alcohol_consumption_schemaUpToDate() {
        try (var db = createEmptyDatabase()) {
            onUpgrade(db, 0, DB_VERSION_ALCOHOL_CONSUMPTION);

            assertNumberOfTables(db, NUM_OF_TABLES_AT_ALCOHOL_CONSUMPTION_VERSION);
            assertColumnsExist(
                    db,
                    ALCOHOL_CONSUMPTION_RECORD_TABLE_NAME,
                    List.of(
                            AlcoholConsumptionRecordHelper.TEMPORAL_TYPE_COLUMN_NAME,
                            AlcoholConsumptionRecordHelper.SERVING_COUNT_COLUMN_NAME,
                            AlcoholConsumptionRecordHelper.BEVERAGE_TYPE_COLUMN_NAME,
                            AlcoholConsumptionRecordHelper.SERVING_SIZE_COLUMN_NAME,
                            AlcoholConsumptionRecordHelper.SERVING_VOLUME_LITERS_COLUMN_NAME,
                            AlcoholConsumptionRecordHelper.ALCOHOL_BY_VOLUME_COLUMN_NAME,
                            AlcoholConsumptionRecordHelper.NOTE_COLUMN_NAME));
        }
    }

    /** Asserts that PHR tables exist */
    private static void assertPHRTablesExist(SQLiteDatabase db) {
        assertTablesExists(
                db,
                List.of(
                        MedicalDataSourceHelper.getMainTableName(),
                        MedicalResourceHelper.getMainTableName(),
                        MedicalResourceIndicesHelper.getTableName(),
                        ReadAccessLogsHelper.TABLE_NAME));
        assertColumnsExist(
                db,
                AccessLogsHelper.TABLE_NAME,
                List.of("medical_resource_type", "medical_data_source_accessed"));
    }
}
