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

package com.android.server.healthconnect.backuprestore;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_FULL;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_INCREMENTAL;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_UNSPECIFIED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_NONE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_PARTIAL_BACKUP;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_UNSPECIFIED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_STARTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_CONVERSION_FAILED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_NONE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_PARTIAL_RESTORE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_UNSPECIFIED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_STARTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTORE_ELIGIBILITY_CHECKED;

import android.annotation.IntDef;
import android.health.HealthFitnessStatsLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class to log metrics for Backup and Restore
 *
 * @hide
 */
public class BackupRestoreLogger {
    /**
     * Status enums used in logging.
     *
     * @hide
     */
    public static final class BackupRestoreEnums {
        // enums for DataBackupType
        public static final int DATA_BACKUP_TYPE_UNSPECIFIED =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_UNSPECIFIED;
        public static final int DATA_BACKUP_TYPE_FULL =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_FULL;
        public static final int DATA_BACKUP_TYPE_INCREMENTAL =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_INCREMENTAL;
        // enums for DataBackupStatus
        public static final int DATA_BACKUP_STATUS_ERROR_UNSPECIFIED =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_UNSPECIFIED;
        public static final int DATA_BACKUP_STATUS_ERROR_NONE =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_NONE;
        public static final int DATA_BACKUP_STATUS_ERROR_UNKNOWN =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_UNKNOWN;
        public static final int DATA_BACKUP_STATUS_ERROR_PARTIAL_BACKUP =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_PARTIAL_BACKUP;
        public static final int DATA_BACKUP_STATUS_STARTED =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_STARTED;
        // enums for DataRestoreStatus
        public static final int DATA_RESTORE_STATUS_ERROR_UNSPECIFIED =
                HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_UNSPECIFIED;
        public static final int DATA_RESTORE_STATUS_ERROR_NONE =
                HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_NONE;
        public static final int DATA_RESTORE_STATUS_ERROR_UNKNOWN =
                HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_UNKNOWN;
        public static final int DATA_RESTORE_STATUS_ERROR_CONVERSION_FAILED =
                HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_CONVERSION_FAILED;
        public static final int DATA_RESTORE_STATUS_ERROR_PARTIAL_RESTORE =
                HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_PARTIAL_RESTORE;
        public static final int DATA_RESTORE_STATUS_STARTED =
                HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_STARTED;

        @IntDef({
            DATA_BACKUP_STATUS_ERROR_UNSPECIFIED,
            DATA_BACKUP_STATUS_ERROR_NONE,
            DATA_BACKUP_STATUS_ERROR_UNKNOWN,
            DATA_BACKUP_STATUS_ERROR_PARTIAL_BACKUP,
            DATA_BACKUP_STATUS_STARTED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DataBackupState {}

        @IntDef({
            DATA_RESTORE_STATUS_ERROR_UNSPECIFIED,
            DATA_RESTORE_STATUS_ERROR_NONE,
            DATA_RESTORE_STATUS_ERROR_UNKNOWN,
            DATA_RESTORE_STATUS_ERROR_CONVERSION_FAILED,
            DATA_RESTORE_STATUS_ERROR_PARTIAL_RESTORE,
            DATA_RESTORE_STATUS_STARTED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DataRestoreState {}

        @IntDef({DATA_BACKUP_TYPE_UNSPECIFIED, DATA_BACKUP_TYPE_FULL, DATA_BACKUP_TYPE_INCREMENTAL})
        @Retention(RetentionPolicy.SOURCE)
        public @interface DataBackupType {}
    }

    private final HealthFitnessStatsLog mStatsLog;

    public BackupRestoreLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    /**
     * Log the data backup metrics.
     *
     * @param dataBackupStatus the status of the invoked data backup
     * @param dataBackupType the type of data backup that was invoked
     * @param totalRecordCount the number of records being backed up
     */
    public void logDataBackupStatus(
            @BackupRestoreEnums.DataBackupState int dataBackupStatus,
            @BackupRestoreEnums.DataBackupType int dataBackupType,
            int totalRecordCount) {
        mStatsLog.write(
                HEALTH_CONNECT_DATA_BACKUP_INVOKED,
                dataBackupStatus,
                dataBackupType,
                totalRecordCount);
    }

    /**
     * Log the data restore metrics.
     *
     * @param dataRestoreStatus the status of the invoked data restore
     * @param timeToSucceedOrFailMillis time between invoking a data restore and the status being
     *     returned
     * @param totalRecordCount the total number of records in this restore attempt
     * @param successfulRecordCount the total number of records successfully restored
     */
    public void logDataRestoreStatus(
            @BackupRestoreEnums.DataRestoreState int dataRestoreStatus,
            int timeToSucceedOrFailMillis,
            int totalRecordCount,
            int successfulRecordCount) {
        mStatsLog.write(
                HEALTH_CONNECT_DATA_RESTORE_INVOKED,
                dataRestoreStatus,
                timeToSucceedOrFailMillis,
                totalRecordCount,
                successfulRecordCount);
    }

    /**
     * Log the data restore eligibility metrics.
     *
     * @param eligibility Whether or not the system is eligible for a data restore.
     */
    public void logRestoreEligibility(boolean eligibility) {
        mStatsLog.write(HEALTH_CONNECT_RESTORE_ELIGIBILITY_CHECKED, eligibility);
    }
}
