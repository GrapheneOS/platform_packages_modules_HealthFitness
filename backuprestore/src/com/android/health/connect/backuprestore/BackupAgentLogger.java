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

package com.android.health.connect.backuprestore;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_BACKUP;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_FULL_BACKUP;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_RESTORE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__ERROR__AGENT_ERROR_STAGING_FAILED_EXCEPTION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__ERROR__AGENT_ERROR_TIMEOUT_EXCEPTION;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__ERROR__AGENT_ERROR_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_FAILURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_STARTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_SUCCESS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_UNKNOWN;

import android.annotation.IntDef;
import android.health.HealthFitnessStatsLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** A logger for {@link HealthConnectBackupAgent} events. */
final class BackupAgentLogger {

    public static final int BACKUP_AGENT_OPERATION_UNKNOWN =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_UNKNOWN;
    public static final int BACKUP_AGENT_OPERATION_BACKUP =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_BACKUP;
    public static final int BACKUP_AGENT_OPERATION_FULL_BACKUP =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_FULL_BACKUP;
    public static final int BACKUP_AGENT_OPERATION_RESTORE =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__BACKUP_AGENT_OPERATION__AGENT_OPERATION_RESTORE;

    @IntDef({
        BACKUP_AGENT_OPERATION_UNKNOWN,
        BACKUP_AGENT_OPERATION_BACKUP,
        BACKUP_AGENT_OPERATION_FULL_BACKUP,
        BACKUP_AGENT_OPERATION_RESTORE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Operation {}

    public static final int BACKUP_AGENT_STATUS_UNKNOWN =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_UNKNOWN;
    public static final int BACKUP_AGENT_STATUS_STARTED =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_STARTED;
    public static final int BACKUP_AGENT_STATUS_SUCCESS =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_SUCCESS;
    public static final int BACKUP_AGENT_STATUS_FAILURE =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__STATUS__AGENT_STATUS_FAILURE;

    @IntDef({
        BACKUP_AGENT_STATUS_UNKNOWN,
        BACKUP_AGENT_STATUS_STARTED,
        BACKUP_AGENT_STATUS_SUCCESS,
        BACKUP_AGENT_STATUS_FAILURE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {}

    public static final int BACKUP_AGENT_ERROR_UNKNOWN =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__ERROR__AGENT_ERROR_UNKNOWN;
    public static final int BACKUP_AGENT_ERROR_STAGING_FAILED_EXCEPTION =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__ERROR__AGENT_ERROR_STAGING_FAILED_EXCEPTION;
    public static final int BACKUP_AGENT_ERROR_TIMEOUT_EXCEPTION =
            HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED__ERROR__AGENT_ERROR_TIMEOUT_EXCEPTION;

    @IntDef({
        BACKUP_AGENT_ERROR_UNKNOWN,
        BACKUP_AGENT_ERROR_STAGING_FAILED_EXCEPTION,
        BACKUP_AGENT_ERROR_TIMEOUT_EXCEPTION,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Error {}

    private final HealthFitnessStatsLog mStatsLog;

    BackupAgentLogger() {
        this(new HealthFitnessStatsLog());
    }

    BackupAgentLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    public void logStarted(@Operation int operation) {
        mStatsLog.write(
                HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED,
                operation,
                BACKUP_AGENT_STATUS_STARTED,
                BACKUP_AGENT_ERROR_UNKNOWN,
                /*latency*/ 0);
    }

    public void logSuccess(@Operation int operation, int latency) {
        mStatsLog.write(
                HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED,
                operation,
                BACKUP_AGENT_STATUS_SUCCESS,
                /*error*/ 0,
                latency);
    }

    public void logFailed(@Operation int operation, @Error int error, int latency) {
        mStatsLog.write(
                HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED,
                operation,
                BACKUP_AGENT_STATUS_FAILURE,
                error,
                latency);
    }
}
