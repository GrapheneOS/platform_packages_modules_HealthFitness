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

import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_NONE;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_UNKNOWN;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_VERSION_MISMATCH;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_WRONG_FILE;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_STARTED;

import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_IN_PROGRESS;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_GENERIC_ERROR;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_VERSION_MISMATCH;
import static com.android.server.healthconnect.exportimport.ExportManager.LOCAL_EXPORT_DATABASE_FILE_NAME;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.OpenableColumns;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Clock;
import java.util.zip.ZipException;

/**
 * Manages import related tasks.
 *
 * @hide
 */
public class ImportManager {

    @VisibleForTesting static final String IMPORT_DATABASE_DIR_NAME = "export_import";

    @VisibleForTesting static final String IMPORT_DATABASE_FILE_NAME = "health_connect_import.db";

    private static final String TAG = "HealthConnectImportManager";

    private final Context mContext;
    private final DatabaseMerger mDatabaseMerger;
    private final TransactionManager mTransactionManager;
    private final HealthConnectNotificationSender mNotificationSender;
    private final ExportImportSettingsStorage mExportImportSettingsStorage;
    @Nullable private final Clock mClock;

    public ImportManager(
            AppInfoHelper appInfoHelper,
            Context context,
            ExportImportSettingsStorage exportImportSettingsStorage,
            TransactionManager transactionManager,
            DeviceInfoHelper deviceInfoHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            @Nullable Clock clock,
            HealthConnectNotificationSender notificationSender) {
        mContext = context;
        mDatabaseMerger =
                new DatabaseMerger(
                        appInfoHelper,
                        deviceInfoHelper,
                        healthDataCategoryPriorityHelper,
                        transactionManager);
        mTransactionManager = transactionManager;
        mExportImportSettingsStorage = exportImportSettingsStorage;
        mClock = clock;
        mNotificationSender = notificationSender;
    }

    /** Reads and merges the backup data from a local file. */
    public synchronized void runImport(UserHandle userHandle, Uri uri) {
        Slog.i(TAG, "Import started.");
        long startTimeMillis = mClock != null ? mClock.millis() : -1;
        mExportImportSettingsStorage.setImportState(DATA_IMPORT_STARTED);
        mNotificationSender.sendNotificationAsUser(
                NOTIFICATION_TYPE_IMPORT_IN_PROGRESS, userHandle);

        ExportImportLogger.logImportStatus(
                DATA_IMPORT_STARTED,
                ExportImportLogger.NO_VALUE_RECORDED,
                ExportImportLogger.NO_VALUE_RECORDED,
                ExportImportLogger.NO_VALUE_RECORDED);

        Context userContext = mContext.createContextAsUser(userHandle, 0);
        HealthConnectContext dbContext =
                HealthConnectContext.create(mContext, userHandle, IMPORT_DATABASE_DIR_NAME);
        File importDbFile = dbContext.getDatabasePath(IMPORT_DATABASE_FILE_NAME);

        int zipFileSize = getZipFileSize(userContext, uri);

        try {
            try {
                Slog.d(TAG, "Starting to unzip file: " + importDbFile.getAbsolutePath());
                Compressor.decompress(
                        uri, LOCAL_EXPORT_DATABASE_FILE_NAME, importDbFile, userContext);
                Slog.i(TAG, "Import file unzipped: " + importDbFile.getAbsolutePath());
            } catch (IllegalArgumentException e) {
                Slog.e(
                        TAG,
                        "Failed to decompress zip file as a null-value entry was found and could "
                                + "not be processed. The file may be corrupted. Details: ",
                        e);
                notifyAndLogInvalidFileError(
                        userHandle, startTimeMillis, intSizeInKb(importDbFile), zipFileSize);
                return;
            } catch (ZipException e) {
                Slog.d(
                        TAG,
                        "Failed to decompress zip file due to a zip file format error occurring "
                                + "whilst attempting to process the input/output streams. The "
                                + "file may be corrupted. Details: ",
                        e);
                notifyAndLogInvalidFileError(
                        userHandle, startTimeMillis, intSizeInKb(importDbFile), zipFileSize);
            } catch (IOException e) {
                Slog.d(
                        TAG,
                        "Failed to decompress zip file due to an unknown IO error occurring "
                                + "whilst attempting to process the input/output streams. The "
                                + "file may be corrupted. Details: ",
                        e);
                notifyAndLogInvalidFileError(
                        userHandle, startTimeMillis, intSizeInKb(importDbFile), zipFileSize);
            } catch (Exception e) {
                Slog.e(
                        TAG,
                        "Failed to decompress zip file. Was unable to get a copy to the "
                                + "destination: "
                                + importDbFile.getAbsolutePath(),
                        e);
                notifyAndLogUnknownError(
                        userHandle, startTimeMillis, intSizeInKb(importDbFile), zipFileSize);
            }

            try {
                Slog.d(TAG, "Starting to merge database");
                if (canMerge(importDbFile)) {
                    HealthConnectDatabase stagedDatabase =
                            new HealthConnectDatabase(dbContext, IMPORT_DATABASE_FILE_NAME);
                    mDatabaseMerger.merge(stagedDatabase);
                }
            } catch (SQLiteException e) {
                Slog.d(
                        TAG,
                        "Import failed during database merge. Selected import file is not"
                                + "a database. Details: ",
                        e);
                notifyAndLogInvalidFileError(
                        userHandle, startTimeMillis, intSizeInKb(importDbFile), zipFileSize);
                return;
            } catch (IllegalStateException e) {
                Slog.d(
                        TAG,
                        "Import failed during database merge. Existing database has a smaller"
                                + " version number than the database being imported. Details: ",
                        e);
                sendNotificationAsUser(
                        NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_VERSION_MISMATCH, userHandle);
                recordError(
                        DATA_IMPORT_ERROR_VERSION_MISMATCH,
                        startTimeMillis,
                        intSizeInKb(importDbFile),
                        zipFileSize);
                return;
            } catch (Exception e) {
                Slog.d(
                        TAG,
                        "Import failed during database merge due to an unknown error. "
                                + "Details: ",
                        e);
                notifyAndLogUnknownError(
                        userHandle, startTimeMillis, intSizeInKb(importDbFile), zipFileSize);
                return;
            }
            Slog.i(TAG, "Import completed");
            sendNotificationAsUser(NOTIFICATION_TYPE_IMPORT_COMPLETE, userHandle);
            recordSuccess(startTimeMillis, intSizeInKb(importDbFile), zipFileSize);
        } finally {
            // Delete the staged db as we are done merging.
            Slog.i(TAG, "Deleting staged db after merging");
            SQLiteDatabase.deleteDatabase(importDbFile);
        }
    }

    private int getZipFileSize(Context userContext, Uri uri) {
        if (Flags.exportImportFastFollow()) {
            try {
                return getFileSizeInKb(userContext.getContentResolver(), uri);
            } catch (IllegalArgumentException e) {
                Slog.d(
                        TAG,
                        "Unable to get the file size of the zip file due to a null-value"
                                + " cursor being found. File may be corrupted. Setting to -1 as"
                                + " currently only used for logging. Details: ",
                        e);
                return -1;
            } catch (Exception e) {
                Slog.d(
                        TAG,
                        "Unable to get the file size of the zip file due to an unknown"
                                + " error. Setting to -1 as currently only used for logging."
                                + " Details: ",
                        e);
                return -1;
            }
        } else {
            return -1;
        }
    }

    private boolean canMerge(File importDbFile)
            throws FileNotFoundException, IllegalStateException, SQLiteException {
        int currentDbVersion = mTransactionManager.getDatabaseVersion();
        if (importDbFile.exists()) {
            try (SQLiteDatabase importDb =
                    SQLiteDatabase.openDatabase(
                            importDbFile, new SQLiteDatabase.OpenParams.Builder().build())) {
                int stagedDbVersion = importDb.getVersion();
                Slog.i(
                        TAG,
                        "merging staged data, current version = "
                                + currentDbVersion
                                + ", staged version = "
                                + stagedDbVersion);
                if (currentDbVersion < stagedDbVersion) {
                    Slog.d(
                            TAG,
                            "Import failed when attempting to start merge. The imported database"
                                    + " has a greater version number than the existing database.");
                    throw new IllegalStateException(
                            "Unable to merge database - module needs"
                                    + "upgrade for merging to version. Current database has smaller"
                                    + "version number than database being imported.");
                }
            }
        } else {
            Slog.d(
                    TAG,
                    "Import failed when attempting to start merge, as database file was"
                            + "not found.");
            throw new FileNotFoundException("No database file found to merge.");
        }

        Slog.i(TAG, "File can be merged.");
        return true;
    }

    /***
     * Returns the size of a file in Kb for logging
     * To keep the log size small, the data type is an int32 rather than a long (int64).
     * Using an int allows logging sizes up to 2TB, which is sufficient for our use cases,
     */
    private int intSizeInKb(File file) {
        return (int) (file.length() / 1024.0);
    }

    @VisibleForTesting
    int getFileSizeInKb(ContentResolver contentResolver, Uri zip) {
        try (Cursor cursor = contentResolver.query(zip, null, null, null, null)) {
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                cursor.moveToFirst();
                return (int) (cursor.getLong(sizeIndex) / 1024.0);
            } else {
                throw new IllegalArgumentException("Unable to find cursor, returned null.");
            }
        }
    }

    private void recordError(
            int importStatus,
            long startTimeMillis,
            int originalDataSizeKb,
            int compressedDataSizeKb) {
        mExportImportSettingsStorage.setImportState(importStatus);
        if (!Flags.exportImportFastFollow()) return;
        // Convert to int to save on logs storage, int can hold about 68 years
        int timeToErrorMillis = mClock != null ? (int) (mClock.millis() - startTimeMillis) : -1;
        ExportImportLogger.logImportStatus(
                importStatus, timeToErrorMillis, originalDataSizeKb, compressedDataSizeKb);
    }

    private void recordSuccess(
            long startTimeMillis, int originalDataSizeKb, int compressedDataSizeKb) {
        mExportImportSettingsStorage.setImportState(DATA_IMPORT_ERROR_NONE);
        if (!Flags.exportImportFastFollow()) return;
        // Convert to int to save on logs storage, int can hold about 68 years
        int timeToErrorMillis = mClock != null ? (int) (mClock.millis() - startTimeMillis) : -1;
        ExportImportLogger.logImportStatus(
                DATA_IMPORT_ERROR_NONE,
                timeToErrorMillis,
                originalDataSizeKb,
                compressedDataSizeKb);
    }

    private void sendNotificationAsUser(int notificationType, UserHandle userHandle) {
        mNotificationSender.clearNotificationsAsUser(userHandle);
        mNotificationSender.sendNotificationAsUser(notificationType, userHandle);
    }

    private void notifyAndLogUnknownError(
            UserHandle userHandle,
            long startTimeMillis,
            int originalFileSize,
            int compressedFileSize) {
        sendNotificationAsUser(NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_GENERIC_ERROR, userHandle);
        recordError(
                DATA_IMPORT_ERROR_UNKNOWN, startTimeMillis, originalFileSize, compressedFileSize);
    }

    private void notifyAndLogInvalidFileError(
            UserHandle userHandle,
            long startTimeMillis,
            int originalFileSize,
            int compressedFileSize) {
        sendNotificationAsUser(NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE, userHandle);
        recordError(
                DATA_IMPORT_ERROR_WRONG_FILE,
                startTimeMillis,
                originalFileSize,
                compressedFileSize);
    }
}
