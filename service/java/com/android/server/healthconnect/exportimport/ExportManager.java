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

package com.android.server.healthconnect.exportimport;

import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_CLEARING_LOG_TABLES;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_CLEARING_PHR_TABLES;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_NONE;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_UNKNOWN;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_LOST_FILE_ACCESS;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_STARTED;

import static com.android.healthfitness.flags.Flags.exportImportFastFollow;
import static com.android.healthfitness.flags.Flags.extendExportImportTelemetry;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR;
import static com.android.server.healthconnect.logging.ExportImportLogger.NO_VALUE_RECORDED;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.exportimport.ScheduledExportStatus.DataExportError;
import android.net.Uri;
import android.os.UserHandle;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.List;

/**
 * Class that manages export related tasks. In this context, export means to make an encrypted copy
 * of Health Connect data that the user can store in some online storage solution.
 *
 * @hide
 */
public class ExportManager {

    @VisibleForTesting static final String LOCAL_EXPORT_DIR_NAME = "export_import";

    static final String LOCAL_EXPORT_DATABASE_FILE_NAME = "health_connect_export.db";

    @VisibleForTesting static final String LOCAL_EXPORT_ZIP_FILE_NAME = "health_connect_export.zip";

    private static final String TAG = "HealthConnectExportImport";

    private final Context mContext;
    private final Clock mClock;
    private final TransactionManager mTransactionManager;
    private final ExportImportSettingsStorage mExportImportSettingsStorage;

    private final HealthConnectNotificationSender mNotificationSender;
    private final File mEnvironmentDataDirectory;
    private final ExportImportLogger mExportImportLogger;

    // Tables to drop instead of tables to keep to avoid risk of bugs if new data types are added.
    /**
     * Logs size is non-trivial, exporting them would make the process slower and the upload file
     * would need more storage. Furthermore, logs from a previous device don't provide the user with
     * useful information.
     */
    @VisibleForTesting
    public static final List<String> TABLES_TO_CLEAR =
            List.of(AccessLogsHelper.TABLE_NAME, ChangeLogsHelper.TABLE_NAME);

    private static final List<String> PHR_TABLES_TO_CLEAR =
            List.of(
                    MedicalDataSourceHelper.getMainTableName(),
                    MedicalResourceHelper.getMainTableName(),
                    MedicalResourceIndicesHelper.getTableName());

    public ExportManager(
            Context context,
            Clock clock,
            ExportImportSettingsStorage exportImportSettingsStorage,
            TransactionManager transactionManager,
            HealthConnectNotificationSender notificationSender,
            File environmentDataDirectory,
            ExportImportLogger exportImportLogger) {
        mContext = context;
        mClock = clock;
        mExportImportSettingsStorage = exportImportSettingsStorage;
        mTransactionManager = transactionManager;
        mNotificationSender = notificationSender;
        mEnvironmentDataDirectory = environmentDataDirectory;
        mExportImportLogger = exportImportLogger;
    }

    /**
     * Makes a local copy of the HC database, deletes the unnecessary data for export and sends the
     * data to a cloud provider.
     */
    public synchronized boolean runExport(UserHandle userHandle) {
        Slog.i(TAG, "Export started.");
        long startTimeMillis = mClock.millis();
        mExportImportLogger.logExportStatus(
                DATA_EXPORT_STARTED, NO_VALUE_RECORDED, NO_VALUE_RECORDED, NO_VALUE_RECORDED);

        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext, userHandle, LOCAL_EXPORT_DIR_NAME, mEnvironmentDataDirectory);
        File localExportDbFile = getLocalExportDbFile(dbContext);
        File localExportZipFile = getLocalExportZipFile(dbContext);

        try {
            try {
                exportLocally(localExportDbFile);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create local file for export", e);
                Slog.d(TAG, "original file size: " + intSizeInKb(localExportDbFile));
                recordError(
                        DATA_EXPORT_ERROR_UNKNOWN,
                        startTimeMillis,
                        intSizeInKb(localExportDbFile),
                        /* Compressed size will be 0, not yet compressed */
                        intSizeInKb(localExportZipFile));
                sendNotificationIfEnabled(
                        userHandle, NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR);
                return false;
            }

            try {
                deleteLogTablesContent(dbContext);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to clear log tables in preparation for export", e);
                Slog.d(TAG, "Original file size: " + intSizeInKb(localExportDbFile));
                recordError(
                        DATA_EXPORT_ERROR_CLEARING_LOG_TABLES,
                        startTimeMillis,
                        intSizeInKb(localExportDbFile),
                        /* Compressed size will be 0, not yet compressed */
                        intSizeInKb(localExportZipFile));
                sendNotificationIfEnabled(
                        userHandle, NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR);
                return false;
            }

            if (!Flags.personalHealthRecordEnableExportImport()
                    && Flags.personalHealthRecordDisableExportImport()) {
                try {
                    deletePhrTablesContent(dbContext);
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to clear phr tables in preparation for export", e);
                    Slog.d(TAG, "Original file size: " + intSizeInKb(localExportDbFile));
                    recordError(
                            DATA_EXPORT_ERROR_CLEARING_PHR_TABLES,
                            startTimeMillis,
                            intSizeInKb(localExportDbFile),
                            /* Compressed size will be 0, not yet compressed */
                            intSizeInKb(localExportZipFile));
                    sendNotificationIfEnabled(
                            userHandle, NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR);
                    return false;
                }
            }

            try {
                Compressor.compress(
                        localExportDbFile, LOCAL_EXPORT_DATABASE_FILE_NAME, localExportZipFile);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to compress local file for export", e);
                Slog.d(TAG, "Original file size: " + intSizeInKb(localExportDbFile));
                recordError(
                        DATA_EXPORT_ERROR_UNKNOWN,
                        startTimeMillis,
                        intSizeInKb(localExportDbFile),
                        /* Compressed size will be 0, not yet compressed */
                        intSizeInKb(localExportZipFile));
                sendNotificationIfEnabled(
                        userHandle, NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR);
                return false;
            }

            Uri destinationUri = mExportImportSettingsStorage.getUri();
            try {
                exportToUri(dbContext, localExportZipFile, destinationUri);
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "Lost access to export location", e);
                Slog.d(TAG, "Original file size: " + intSizeInKb(localExportDbFile));
                recordError(
                        DATA_EXPORT_LOST_FILE_ACCESS,
                        startTimeMillis,
                        intSizeInKb(localExportDbFile),
                        intSizeInKb(localExportZipFile));
                sendNotificationIfEnabled(
                        userHandle, NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR);
                return false;
            } catch (Exception e) {
                Slog.e(TAG, "Failed to export to URI", e);
                Slog.d(TAG, "Original file size: " + intSizeInKb(localExportDbFile));
                recordError(
                        DATA_EXPORT_ERROR_UNKNOWN,
                        startTimeMillis,
                        intSizeInKb(localExportDbFile),
                        intSizeInKb(localExportZipFile));
                sendNotificationIfEnabled(
                        userHandle, NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR);
                return false;
            }
            Slog.i(TAG, "Export completed.");
            Slog.d(TAG, "Original file size: " + intSizeInKb(localExportDbFile));
            recordSuccess(
                    startTimeMillis,
                    intSizeInKb(localExportDbFile),
                    intSizeInKb(localExportZipFile),
                    destinationUri);
            return true;
        } finally {
            deleteLocalExportFiles(userHandle);
        }
    }

    protected void recordSuccess(
            long startTimeMillis,
            int originalDataSizeKb,
            int compressedDataSizeKb,
            Uri destinationUri) {
        mExportImportSettingsStorage.setLastSuccessfulExport(mClock.instant(), destinationUri);

        if (extendExportImportTelemetry()) {
            mExportImportSettingsStorage.resetExportRepeatErrorOnRetryCount();
        }

        // The logging proto holds an int32 not an in64 to save on logs storage. The cast makes this
        // explicit. The int can hold 24.855 days worth of milli seconds, which
        // is sufficient because the system would kill the process earlier.
        int timeToSuccessMillis = (int) (mClock.millis() - startTimeMillis);
        mExportImportLogger.logExportStatus(
                DATA_EXPORT_ERROR_NONE,
                timeToSuccessMillis,
                originalDataSizeKb,
                compressedDataSizeKb);
    }

    protected void recordError(
            int exportStatus,
            long startTimeMillis,
            int originalDataSizeKb,
            int compressedDataSizeKb) {
        @DataExportError int previousError = mExportImportSettingsStorage.getLastExportError();

        if (extendExportImportTelemetry()) {
            // Only start counting duplicate errors the second time the same error happens.
            if (exportStatus != previousError) {
                mExportImportSettingsStorage.resetExportRepeatErrorOnRetryCount();
            } else {
                mExportImportSettingsStorage.increaseExportRepeatErrorOnRetryCount();
            }
        }

        mExportImportSettingsStorage.setLastExportError(exportStatus, mClock.instant());

        // Convert to int to save on logs storage, int can hold about 68 years
        int timeToErrorMillis = (int) (mClock.millis() - startTimeMillis);
        mExportImportLogger.logExportStatus(
                exportStatus, timeToErrorMillis, originalDataSizeKb, compressedDataSizeKb);
    }

    void deleteLocalExportFiles(UserHandle userHandle) {
        Slog.i(TAG, "Delete local export files started.");
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext, userHandle, LOCAL_EXPORT_DIR_NAME, mEnvironmentDataDirectory);
        File localExportDbFile = getLocalExportDbFile(dbContext);
        File localExportZipFile = getLocalExportZipFile(dbContext);
        if (localExportDbFile.exists()) {
            SQLiteDatabase.deleteDatabase(localExportDbFile);
        }
        if (localExportZipFile.exists()) {
            localExportZipFile.delete();
        }
        Slog.i(TAG, "Delete local export files completed.");
    }

    private File getLocalExportDbFile(HealthConnectContext dbContext) {
        return new File(dbContext.getDataDir(), LOCAL_EXPORT_DATABASE_FILE_NAME);
    }

    private File getLocalExportZipFile(HealthConnectContext dbContext) {
        return new File(dbContext.getDataDir(), LOCAL_EXPORT_ZIP_FILE_NAME);
    }

    private void exportLocally(File destination) throws IOException {
        Slog.i(TAG, "Local export started.");

        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Unable to create directory for local export.");
        }

        Files.copy(
                mTransactionManager.getDatabasePath().toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        Slog.i(TAG, "Local export completed: " + destination.toPath().toAbsolutePath());
    }

    private void exportToUri(HealthConnectContext dbContext, File source, Uri destination)
            throws IOException {
        Slog.i(TAG, "Export to URI started.");
        try (OutputStream outputStream =
                dbContext.getContentResolver().openOutputStream(destination)) {
            if (outputStream == null) {
                throw new IOException("Unable to copy data to URI for export.");
            }
            Files.copy(source.toPath(), outputStream);
            Slog.i(TAG, "Export to URI completed.");
        }
    }

    // TODO(b/325599879): Double check if we need to vacuum the database after clearing the tables.
    private void deleteLogTablesContent(HealthConnectContext dbContext) {
        // Throwing a exception when calling this method implies that it was not possible to
        // create a HC database from the file and, therefore, most probably the database was
        // corrupted during the file copy.
        try (HealthConnectDatabase exportDatabase =
                new HealthConnectDatabase(dbContext, LOCAL_EXPORT_DATABASE_FILE_NAME)) {
            for (String tableName : TABLES_TO_CLEAR) {
                exportDatabase.getWritableDatabase().execSQL("DELETE FROM " + tableName + ";");
            }
        }
        Slog.i(TAG, "Drop log tables completed.");
    }

    private void deletePhrTablesContent(HealthConnectContext dbContext) {
        try (HealthConnectDatabase exportDatabase =
                new HealthConnectDatabase(dbContext, LOCAL_EXPORT_DATABASE_FILE_NAME)) {
            for (String tableName : PHR_TABLES_TO_CLEAR) {
                exportDatabase.getWritableDatabase().execSQL("DELETE FROM " + tableName + ";");
            }
        }
        Slog.i(TAG, "Drop phr tables completed.");
    }

    /***
     * Returns the size of a file in Kb for logging
     *
     * To keep the log size small, the data type is an int32 rather than a long (int64).
     * Using an int allows logging sizes up to 2TB, which is sufficient for our use cases,
     */
    private int intSizeInKb(File file) {
        return (int) (file.length() / 1024.0);
    }

    /** Sends export status notification if export_import_fast_follow flag enabled. */
    private void sendNotificationIfEnabled(UserHandle userHandle, int notificationType) {
        if (exportImportFastFollow()) {
            mNotificationSender.sendNotificationAsUser(notificationType, userHandle);
        }
    }
}
