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
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_ALCOHOL_CONSUMPTION;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_DEVICE_DATA_PROVIDERS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_DEVICE_UDI;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_ECOSYSTEM_METRICS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_GENERATED_LOCAL_TIME;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_MENSTRUAL_CYCLE_PHASE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_MINDFULNESS_SESSION;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_NICOTINE_INTAKE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_PLANNED_EXERCISE_SESSIONS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_SKIN_TEMPERATURE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_SYMPTOMS;
import static com.android.healthfitness.flags.DatabaseVersions.MIN_SUPPORTED_DB_VERSION;
import static com.android.server.healthconnect.common.accesslog.AccessLogsHelper.getAlterTableRequestForPhrAccessLogs;
import static com.android.server.healthconnect.fitness.recordhelpers.PlannedExerciseSessionRecordHelper.PLANNED_EXERCISE_SESSION_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.storage.HealthConnectDatabase.createTable;
import static com.android.server.healthconnect.storage.TransactionManager.runAsTransaction;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkColumnExists;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkTableExists;

import static java.util.Map.entry;

import android.database.sqlite.SQLiteDatabase;

import com.android.server.healthconnect.backuprestore.BackupChangeTokenHelper;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataSourcesHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.helpers.RecordDateHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.ActivityIntensityRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSegmentRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.MenstrualCyclePhaseRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.MindfulnessSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.NicotineIntakeRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.PlannedExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SkinTemperatureRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SymptomRecordHelper;
import com.android.server.healthconnect.migration.MigrationEntityHelper;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.storage.request.AlterTableRequest;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DropTableRequest;

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

    private static final Upgrader UPGRADE_TO_EXERCISE_SEGMENT_WEIGHT =
            DatabaseUpgradeHelper::applyExerciseSegmentImprovementsDatabaseUpgrade;

    private static final Upgrader UPGRADE_TO_PHR_CHANGE_LOGS =
            DatabaseUpgradeHelper::applyPhrChangeLogsDatabaseUpgrade;

    private static final Upgrader UPGRADE_TO_NICOTINE_INTAKE =
            db -> new NicotineIntakeRecordHelper().applyNicotineIntakeUpgrade(db);

    private static final Upgrader UPGRADE_TO_SYMPTOMS =
            db -> createTable(db, new SymptomRecordHelper().getCreateTableRequest());

    private static final Upgrader UPGRADE_TO_ALCOHOL_CONSUMPTION =
            db -> createTable(db, new AlcoholConsumptionRecordHelper().getCreateTableRequest());

    private static final Upgrader UPGRADE_TO_MENSTRUAL_CYCLE_PHASE =
            db -> createTable(db, new MenstrualCyclePhaseRecordHelper().getCreateTableRequest());

    private static final Upgrader UPGRADE_TO_DEVICE_DATA_PROVIDERS =
            DatabaseUpgradeHelper::applyDeviceDataProvidersDatabaseUpgrade;

    private static final Upgrader UPGRADE_TO_DEVICE_UDI =
            DatabaseUpgradeHelper::applyDeviceUdiDatabaseUpgrade;

    /**
     * A list of db version -> Upgrader to upgrade the db from the previous version to the version.
     * The upgrades must be executed one by one in the numeric order of db versions, hence TreeMap.
     */
    private static final TreeMap<Integer, Upgrader> UPGRADERS =
            new TreeMap<>(
                    Map.ofEntries(
                            entry(DB_VERSION_GENERATED_LOCAL_TIME, UPGRADE_TO_GENERATED_LOCAL_TIME),
                            entry(DB_VERSION_SKIN_TEMPERATURE, UPGRADE_TO_SKIN_TEMPERATURE),
                            entry(
                                    DB_VERSION_PLANNED_EXERCISE_SESSIONS,
                                    UPGRADE_TO_PLANNED_EXERCISE_SESSIONS),
                            entry(DB_VERSION_MINDFULNESS_SESSION, UPGRADE_TO_MINDFULNESS_SESSION),
                            entry(
                                    DB_VERSION_PERSONAL_HEALTH_RECORD,
                                    UPGRADE_TO_PERSONAL_HEALTH_RECORD),
                            entry(DB_VERSION_ACTIVITY_INTENSITY, UPGRADE_TO_ACTIVITY_INTENSITY),
                            entry(DB_VERSION_ECOSYSTEM_METRICS, UPGRADE_TO_ECOSYSTEM_METRICS),
                            entry(
                                    DB_VERSION_CLOUD_BACKUP_AND_RESTORE,
                                    UPGRADE_TO_CLOUD_BACKUP_AND_RESTORE),
                            entry(
                                    DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS,
                                    UPGRADE_TO_EXERCISE_SEGMENT_WEIGHT),
                            entry(DB_VERSION_PHR_CHANGE_LOGS, UPGRADE_TO_PHR_CHANGE_LOGS),
                            entry(DB_VERSION_NICOTINE_INTAKE, UPGRADE_TO_NICOTINE_INTAKE),
                            entry(DB_VERSION_SYMPTOMS, UPGRADE_TO_SYMPTOMS),
                            entry(DB_VERSION_ALCOHOL_CONSUMPTION, UPGRADE_TO_ALCOHOL_CONSUMPTION),
                            entry(
                                    DB_VERSION_MENSTRUAL_CYCLE_PHASE,
                                    UPGRADE_TO_MENSTRUAL_CYCLE_PHASE),
                            entry(
                                    DB_VERSION_DEVICE_DATA_PROVIDERS,
                                    UPGRADE_TO_DEVICE_DATA_PROVIDERS),
                            entry(DB_VERSION_DEVICE_UDI, UPGRADE_TO_DEVICE_UDI)));

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
        UPGRADERS.entrySet().stream()
                .filter(entry -> shouldUpgrade(entry.getKey(), effectiveOldVersion, newVersion))
                .forEach(entry -> entry.getValue().upgrade(db));
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
        requests.add(RecordDateHelper.getCreateTableRequest());
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
                        .getAddColumnsCommands());
        ExerciseSessionRecordHelper exerciseRecordHelper = new ExerciseSessionRecordHelper();
        executeSqlStatements(
                db,
                exerciseRecordHelper
                        .getAlterTableRequestForPlannedExerciseFeature()
                        .getAddColumnsCommands());
    }

    private static void applyExerciseSegmentImprovementsDatabaseUpgrade(SQLiteDatabase db) {
        if (checkColumnExists(
                db,
                ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_RECORD_TABLE_NAME,
                ExerciseSegmentRecordHelper.EXERCISE_SEGMENT_WEIGHT_GRAMS)) {
            // Upgrade has already been applied. Return early.
            // This is necessary as the ALTER TABLE ... ADD COLUMN statements below are not
            // idempotent, as SQLite does not support ADD COLUMN IF NOT EXISTS.
            return;
        }
        executeSqlStatements(
                db,
                ExerciseSegmentRecordHelper.getAlterTableRequestForExerciseSegmentImprovements()
                        .getAddColumnsCommands());
        executeSqlStatements(
                db,
                ExerciseSessionRecordHelper.getAlterTableRequestForRateOfPerceivedExertion()
                        .getAddColumnsCommands());
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
                db, getAlterTableRequestForPhrAccessLogs().getAddColumnsCommands());
    }

    private static void applyPhrChangeLogsDatabaseUpgrade(SQLiteDatabase db) {
        if (checkColumnExists(
                db,
                ChangeLogsRequestHelper.TABLE_NAME,
                ChangeLogsRequestHelper.MEDICAL_RESOURCE_TYPES_COLUMN_NAME)) {
            return;
        }

        var changeLogsRequestStatements =
                ChangeLogsRequestHelper.getAlterTableRequestForPhrChangeLogs();
        executeSqlStatements(db, changeLogsRequestStatements.getAddColumnsCommands());
        var changeLogsStatements = ChangeLogsHelper.getAlterTableRequestForPhrChangeLogs();
        executeSqlStatements(db, changeLogsStatements.getAddColumnsCommands());
    }

    private static void applyDeviceDataProvidersDatabaseUpgrade(SQLiteDatabase db) {
        if (checkColumnExists(
                db, AppInfoHelper.TABLE_NAME, AppInfoHelper.DEVICE_INFO_ID_COLUMN_NAME)) {
            return;
        }

        // Add the column which links app info entries to device info entries.
        executeSqlStatements(
                db, AppInfoHelper.getAlterTableRequestForDdpInfo().getAddColumnsCommands());

        // Add display name and device ID columns to the device info table.
        executeSqlStatements(
                db, DeviceInfoHelper.getAlterTableRequestForDdpColumns().getAddColumnsCommands());

        // Create table for storing metadata about device data providers.
        createTable(db, DeviceDataProviderMetadataHelper.getCreateTableRequest());

        final InternalHealthConnectMappings mInternalHealthConnectMappings =
                InternalHealthConnectMappings.getInstance();

        // Add the column to all record tables that stores the ID of the writing DDP.
        for (RecordHelper<?> recordHelper : mInternalHealthConnectMappings.getRecordHelpers()) {
            if (!checkTableExists(db, recordHelper.getMainTableName())) {
                // Data types under development may have a record helper, but no database changes
                // outside of the DevelopmentDatabaseHelper, so we avoid attempting to alter such
                // tables, as they don't exist.
                continue;
            }

            // For fresh database creation, the column may already have been added at creation time.
            if (checkColumnExists(
                    db, recordHelper.getMainTableName(), RecordHelper.DDP_ID_COLUMN_NAME)) {
                continue;
            }
            AlterTableRequest alterRecordHelperRequest =
                    recordHelper.getAlterTableRequestForDdpName();
            executeSqlStatements(db, alterRecordHelperRequest.getAddColumnsCommands());
        }

        createTable(db, DeviceDataSourcesHelper.getCreateTableRequest());
    }

    private static void applyDeviceUdiDatabaseUpgrade(SQLiteDatabase db) {
        if (checkColumnExists(db, DeviceInfoHelper.TABLE_NAME, DeviceInfoHelper.UDI_COLUMN_NAME)) {
            // Upgrade has already been applied. Return early.
            return;
        }
        executeSqlStatements(
                db, DeviceInfoHelper.getAlterTableRequestForUdiColumn().getAddColumnsCommands());
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
