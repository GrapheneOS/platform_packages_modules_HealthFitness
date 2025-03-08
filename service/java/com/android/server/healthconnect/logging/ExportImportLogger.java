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

package com.android.server.healthconnect.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_ERROR_LOST_FILE_ACCESS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_ERROR_NONE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_ERROR_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_STARTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_NONE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_VERSION_MISMATCH;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_WRONG_FILE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_STARTED;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_NONE;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_UNKNOWN;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_VERSION_MISMATCH;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_WRONG_FILE;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_STARTED;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_NONE;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_UNKNOWN;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_LOST_FILE_ACCESS;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_STARTED;

import android.health.HealthFitnessStatsLog;
import android.util.SparseIntArray;

/**
 * Class to log metrics for Export Import
 *
 * @hide
 */
public class ExportImportLogger {

    // Proto values are set to 0 by default, log -1 to distinguish fields that no value was logged
    // for from real zero values.
    public static final int NO_VALUE_RECORDED = -1;

    public static final SparseIntArray EXPORT_STATUS_LOG_TAGS;

    static {
        EXPORT_STATUS_LOG_TAGS = new SparseIntArray();
        EXPORT_STATUS_LOG_TAGS.put(
                DATA_EXPORT_ERROR_NONE,
                HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_ERROR_NONE);
        EXPORT_STATUS_LOG_TAGS.put(
                DATA_EXPORT_ERROR_UNKNOWN,
                HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_ERROR_UNKNOWN);
        EXPORT_STATUS_LOG_TAGS.put(
                DATA_EXPORT_LOST_FILE_ACCESS,
                HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_ERROR_LOST_FILE_ACCESS);
        EXPORT_STATUS_LOG_TAGS.put(
                DATA_EXPORT_STARTED, HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_STARTED);
    }

    private static final SparseIntArray IMPORT_STATUS_LOG_TAGS;

    static {
        IMPORT_STATUS_LOG_TAGS = new SparseIntArray();
        IMPORT_STATUS_LOG_TAGS.put(
                DATA_IMPORT_ERROR_NONE,
                HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_NONE);
        IMPORT_STATUS_LOG_TAGS.put(
                DATA_IMPORT_ERROR_UNKNOWN,
                HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_UNKNOWN);
        IMPORT_STATUS_LOG_TAGS.put(
                DATA_IMPORT_ERROR_WRONG_FILE,
                HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_WRONG_FILE);
        IMPORT_STATUS_LOG_TAGS.put(
                DATA_IMPORT_ERROR_VERSION_MISMATCH,
                HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_VERSION_MISMATCH);
        IMPORT_STATUS_LOG_TAGS.put(
                DATA_IMPORT_STARTED, HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_STARTED);
    }

    private final HealthFitnessStatsLog mStatsLog;

    public ExportImportLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    /**
     * Log Export metrics.
     *
     * @param exportStatus The status of the Export as defined by the status codes in
     *     ExportStatus.java
     * @param timeToSucceedOrFailMillis Time between starting the export and recording success/error
     * @param originalDataSizeKb Size of the data that is being exported, before compression
     * @param compressedDataSizeKb Size of the data that is being exported, after compression
     */
    public void logExportStatus(
            int exportStatus,
            int timeToSucceedOrFailMillis,
            int originalDataSizeKb,
            int compressedDataSizeKb) {
        mStatsLog.write(
                HEALTH_CONNECT_EXPORT_INVOKED,
                ExportImportLogger.EXPORT_STATUS_LOG_TAGS.get(exportStatus),
                timeToSucceedOrFailMillis,
                originalDataSizeKb,
                compressedDataSizeKb);
    }

    /**
     * Log Import metrics.
     *
     * @param importStatus The status of the Import as defined by the status codes in
     *     ImportStatus.java
     * @param timeToSucceedOrFailMillis Time between starting the import and recording success/error
     * @param originalDataSizeKb Size of the data that is being imported, after decompression
     * @param compressedDataSizeKb Size of the data that is being imported, before decompression
     */
    public void logImportStatus(
            int importStatus,
            int timeToSucceedOrFailMillis,
            int originalDataSizeKb,
            int compressedDataSizeKb) {
        mStatsLog.write(
                HEALTH_CONNECT_IMPORT_INVOKED,
                IMPORT_STATUS_LOG_TAGS.get(importStatus),
                timeToSucceedOrFailMillis,
                originalDataSizeKb,
                compressedDataSizeKb);
    }
}
