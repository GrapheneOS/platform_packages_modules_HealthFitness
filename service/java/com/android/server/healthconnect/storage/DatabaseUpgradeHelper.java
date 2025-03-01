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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;

import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_ACTIVITY_INTENSITY;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_ECOSYSTEM_METRICS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_GENERATED_LOCAL_TIME;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_MINDFULNESS_SESSION;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_PLANNED_EXERCISE_SESSIONS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_SKIN_TEMPERATURE;
import static com.android.healthfitness.flags.DatabaseVersions.MIN_SUPPORTED_DB_VERSION;
import static com.android.server.healthconnect.storage.HealthConnectDatabase.createTable;
import static com.android.server.healthconnect.storage.TransactionManager.runAsTransaction;
import static com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper.getAlterTableRequestForPhrAccessLogs;
import static com.android.server.healthconnect.storage.datatypehelpers.PlannedExerciseSessionRecordHelper.PLANNED_EXERCISE_SESSION_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkTableExists;

import android.database.sqlite.SQLiteDatabase;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ActivityDateHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ActivityIntensityRecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.BackupChangeTokenHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ExerciseSessionRecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MigrationEntityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MindfulnessSessionRecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PlannedExerciseSessionRecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.SkinTemperatureRecordHelper;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DropTableRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Class that contains all database upgrades. */
final class DatabaseUpgradeHelper {
    private static final String SQLITE_MASTER_TABLE_NAME = "sqlite_master";

    private static final Upgrader UPGRADE_TO_GENERATED_LOCAL_TIME =
            db -> forEachInitialRecordHelper(it -> it.applyGeneratedLocalTimeUpgrade(db));

    private static final Upgrader UPGRADE_TO_SKIN_TEMPERATURE =
            db -> new SkinTemperatureRecordHelper().applySkinTemperatureUpgrade(db);

    private static final Upgrader UPGRADE_TO_PLANNED_EXERCISE_SESSIONS =
            DatabaseUpgradeHelper::applyPlannedExerciseDatabaseUpgrade;

    private static final Upgrader UPGRADE_TO_MINDFULNESS_SESSION =
            db -> new MindfulnessSessionRecordHelper().applyMindfulnessSessionUpgrade(db);

    private static final Upgrader UPGRADE_TO_PERSONAL_HEALTH_RECORD =
            DatabaseUpgradeHelper::applyPersonalHealthRecordDatabaseUpgrade;

    private static final Upgrader UPGRADE_TO_ACTIVITY_INTENSITY =
            db -> createTable(db, new ActivityIntensityRecordHelper().getCreateTableRequest());

    private static final Upgrader UPGRADE_TO_ECOSYSTEM_METRICS =
            db -> createTable(db, ReadAccessLogsHelper.getCreateTableRequest());

    private static final Upgrader UPGRADE_TO_CLOUD_BACKUP_AND_RESTORE =
            BackupChangeTokenHelper::applyBackupTokenUpgrade;

    /**
     * A list of db version -> Upgrader to upgrade the db from the previous version to the version.
     * The upgrades must be executed one by one in the numeric order of db versions, hence TreeMap.
     */
    private static final TreeMap<Integer, Upgrader> UPGRADERS =
            new TreeMap<>(
                    Map.of(
                            DB_VERSION_GENERATED_LOCAL_TIME, UPGRADE_TO_GENERATED_LOCAL_TIME,
                            DB_VERSION_SKIN_TEMPERATURE, UPGRADE_TO_SKIN_TEMPERATURE,
                            DB_VERSION_PLANNED_EXERCISE_SESSIONS,
                                    UPGRADE_TO_PLANNED_EXERCISE_SESSIONS,
                            DB_VERSION_MINDFULNESS_SESSION, UPGRADE_TO_MINDFULNESS_SESSION,
                            DB_VERSION_PERSONAL_HEALTH_RECORD, UPGRADE_TO_PERSONAL_HEALTH_RECORD,
                            DB_VERSION_ACTIVITY_INTENSITY, UPGRADE_TO_ACTIVITY_INTENSITY,
                            DB_VERSION_ECOSYSTEM_METRICS, UPGRADE_TO_ECOSYSTEM_METRICS,
                            DB_VERSION_CLOUD_BACKUP_AND_RESTORE,
                                    UPGRADE_TO_CLOUD_BACKUP_AND_RESTORE));

    /**
     * Applies db upgrades to bring the current schema to the latest supported version.
     *
     * <p>To upgrade an existing schema from a version before the {@link MIN_SUPPORTED_DB_VERSION},
     * it drops tables and brings it to the minimum supported version. Note that this is not
     * idempotent and might cause data loss.
     *
     * <p>Above the {@link MIN_SUPPORTED_DB_VERSION}, we keep the upgrades idempotent, since module
     * rollbacks can bring the version number (not schema) all the way back to the minimum supported
     * version, which mean that some upgrades are applied multiple times.
     *
     * <p>See go/hc-handling-database-upgrades for things to be taken care of when upgrading.
     */
    static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (isUnsupportedDbVersion(oldVersion)) {
            dropInitialSetOfTables(db);
        }
        if (oldVersion < MIN_SUPPORTED_DB_VERSION) {
            createTablesForMinSupportedVersion(db);
            // Sets version to include local time upgrade as the table create steps includes
            // these columns.
            oldVersion = DB_VERSION_GENERATED_LOCAL_TIME;
        }
        final int effectiveOldVersion = oldVersion;

        if (Flags.infraToGuardDbChanges()) {
            UPGRADERS.entrySet().stream()
                    .filter(entry -> shouldUpgrade(entry.getKey(), effectiveOldVersion, newVersion))
                    .forEach(entry -> entry.getValue().upgrade(db));
        } else {
            if (effectiveOldVersion < DB_VERSION_GENERATED_LOCAL_TIME) {
                UPGRADE_TO_GENERATED_LOCAL_TIME.upgrade(db);
            }
            if (effectiveOldVersion < DB_VERSION_SKIN_TEMPERATURE) {
                UPGRADE_TO_SKIN_TEMPERATURE.upgrade(db);
            }
            if (effectiveOldVersion < DB_VERSION_PLANNED_EXERCISE_SESSIONS) {
                UPGRADE_TO_PLANNED_EXERCISE_SESSIONS.upgrade(db);
            }
            if (effectiveOldVersion < DB_VERSION_MINDFULNESS_SESSION) {
                UPGRADE_TO_MINDFULNESS_SESSION.upgrade(db);
            }
            if (shouldUpgrade(DB_VERSION_PERSONAL_HEALTH_RECORD, effectiveOldVersion, newVersion)) {
                UPGRADE_TO_PERSONAL_HEALTH_RECORD.upgrade(db);
            }
            if (effectiveOldVersion < DB_VERSION_ACTIVITY_INTENSITY) {
                UPGRADE_TO_ACTIVITY_INTENSITY.upgrade(db);
            }
            if (effectiveOldVersion < DB_VERSION_ECOSYSTEM_METRICS) {
                UPGRADE_TO_ECOSYSTEM_METRICS.upgrade(db);
            }
            if (effectiveOldVersion < DB_VERSION_CLOUD_BACKUP_AND_RESTORE) {
                UPGRADE_TO_CLOUD_BACKUP_AND_RESTORE.upgrade(db);
            }
        }
    }

    private static boolean isUnsupportedDbVersion(int version) {
        return version != 0 && version < MIN_SUPPORTED_DB_VERSION;
    }

    private static boolean shouldUpgrade(int upgradeVersion, int oldVersion, int newVersion) {
        return oldVersion < upgradeVersion && upgradeVersion <= newVersion;
    }

    private static void createTablesForMinSupportedVersion(SQLiteDatabase db) {
        for (CreateTableRequest createTableRequest : getInitialCreateTableRequests()) {
            createTable(db, createTableRequest);
        }
    }

    private static void dropInitialSetOfTables(SQLiteDatabase db) {
        List<String> allTables =
                getInitialCreateTableRequests().stream()
                        .map(CreateTableRequest::getTableName)
                        .toList();
        for (String table : allTables) {
            db.execSQL(new DropTableRequest(table).getDropTableCommand());
        }
    }

    private static List<CreateTableRequest> getInitialCreateTableRequests() {
        List<CreateTableRequest> requests = new ArrayList<>();

        forEachInitialRecordHelper(helper -> requests.add(helper.getCreateTableRequest()));

        requests.add(DeviceInfoHelper.getCreateTableRequest());
        requests.add(AppInfoHelper.getCreateTableRequest());
        requests.add(ActivityDateHelper.getCreateTableRequest());
        requests.add(ChangeLogsHelper.getCreateTableRequest());
        requests.add(ChangeLogsRequestHelper.getCreateTableRequest());
        requests.add(HealthDataCategoryPriorityHelper.getCreateTableRequest());
        requests.add(PreferenceHelper.getCreateTableRequest());
        requests.add(AccessLogsHelper.getCreateTableRequest());
        requests.add(MigrationEntityHelper.getCreateTableRequest());
        requests.add(PriorityMigrationHelper.getCreateTableRequest());

        return requests;
    }

    // Retuurns all records that were part of the initial schema. This is everything added
    // before SKIN_TEMPERATURE.
    private static void forEachInitialRecordHelper(Consumer<RecordHelper<?>> action) {
        InternalHealthConnectMappings.getInstance().getRecordHelpers().stream()
                .filter(
                        helper ->
                                helper.getRecordIdentifier() > RECORD_TYPE_UNKNOWN
                                        && helper.getRecordIdentifier()
                                                < RECORD_TYPE_SKIN_TEMPERATURE)
                .forEach(action);
    }

    private static void applyPlannedExerciseDatabaseUpgrade(SQLiteDatabase db) {
        if (checkTableExists(db, PLANNED_EXERCISE_SESSION_RECORD_TABLE_NAME)) {
            // Upgrade has already been applied. Return early.
            // This is necessary as the ALTER TABLE ... ADD COLUMN statements below are not
            // idempotent, as SQLite does not support ADD COLUMN IF NOT EXISTS.
            return;
        }
        PlannedExerciseSessionRecordHelper recordHelper = new PlannedExerciseSessionRecordHelper();
        createTable(db, recordHelper.getCreateTableRequest());
        executeSqlStatements(
                db,
                recordHelper
                        .getAlterTableRequestForPlannedExerciseFeature()
                        .getAlterTableAddColumnsCommands());
        ExerciseSessionRecordHelper exerciseRecordHelper = new ExerciseSessionRecordHelper();
        executeSqlStatements(
                db,
                exerciseRecordHelper
                        .getAlterTableRequestForPlannedExerciseFeature()
                        .getAlterTableAddColumnsCommands());
    }

    private static void applyPersonalHealthRecordDatabaseUpgrade(SQLiteDatabase db) {
        if (checkTableExists(db, MedicalResourceHelper.getMainTableName())) {
            // Upgrade has already been applied. Return early.
            // This is necessary as the ALTER TABLE ... ADD COLUMN statements below are not
            // idempotent, as SQLite does not support ADD COLUMN IF NOT EXISTS.
            return;
        }

        MedicalDataSourceHelper.onInitialUpgrade(db);
        MedicalResourceHelper.onInitialUpgrade(db);
        DatabaseUpgradeHelper.executeSqlStatements(
                db, getAlterTableRequestForPhrAccessLogs().getAlterTableAddColumnsCommands());
    }

    /** Executes a list of SQL statements one after another, in a transaction. */
    public static void executeSqlStatements(SQLiteDatabase db, List<String> statements) {
        runAsTransaction(db, unused -> statements.forEach(db::execSQL));
    }

    /** Interface to implement upgrade actions from one db version to the next. */
    private interface Upgrader {
        void upgrade(SQLiteDatabase db);
    }
}
