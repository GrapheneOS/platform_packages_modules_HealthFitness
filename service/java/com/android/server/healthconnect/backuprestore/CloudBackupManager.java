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
package com.android.server.healthconnect.backuprestore;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.server.healthconnect.backuprestore.RecordProtoConverter.PROTO_VERSION;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.database.sqlite.SQLiteException;
import android.health.connect.backuprestore.BackupSettings;
import android.health.connect.backuprestore.GetChangesForBackupResponse;
import android.health.connect.backuprestore.GetSettingsForBackupResponse;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.Slog;

import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.logging.BackupRestoreLogger;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.BackupChangeTokenHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import java.time.Clock;

/**
 * Manages Cloud Backup operations.
 *
 * @hide
 */
public final class CloudBackupManager {

    private static final String TAG = "CloudBackupManager";

    private final TransactionManager mTransactionManager;
    private final CloudBackupDatabaseHelper mDatabaseHelper;
    private final HealthDataCategoryPriorityHelper mPriorityHelper;
    private final PreferenceHelper mPreferenceHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final Clock mClock;
    private final BackupRestoreLogger mBackupRestoreLogger;

    public CloudBackupManager(
            TransactionManager transactionManager,
            FitnessRecordReadHelper fitnessRecordReadHelper,
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper,
            HealthConnectMappings healthConnectMappings,
            InternalHealthConnectMappings internalHealthConnectMappings,
            ChangeLogsHelper changeLogsHelper,
            ChangeLogsRequestHelper changeLogsRequestHelper,
            HealthDataCategoryPriorityHelper priorityHelper,
            PreferenceHelper preferenceHelper,
            Clock clock,
            BackupRestoreLogger backupRestoreLogger) {
        mTransactionManager = transactionManager;
        mPriorityHelper = priorityHelper;
        mPreferenceHelper = preferenceHelper;
        mAppInfoHelper = appInfoHelper;
        mDatabaseHelper =
                new CloudBackupDatabaseHelper(
                        transactionManager,
                        fitnessRecordReadHelper,
                        appInfoHelper,
                        deviceInfoHelper,
                        healthConnectMappings,
                        internalHealthConnectMappings,
                        changeLogsHelper,
                        changeLogsRequestHelper,
                        priorityHelper,
                        preferenceHelper);
        mClock = clock;
        mBackupRestoreLogger = backupRestoreLogger;
    }

    /**
     * The changeToken returned by the previous call should be passed in to resume the upload. A
     * null or empty changeToken means we are doing a fresh backup, and should start from the
     * beginning.
     *
     * <p>If the changeToken is not valid, it means that HealthConnect can no longer resume the
     * backup from this point, and will respond with an IllegalArgumentException. The caller should
     * restart the backup in this case.
     *
     * <p>If no changes are returned by the API, this means that the client has synced all changes
     * as of now.
     */
    @NonNull
    public GetChangesForBackupResponse getChangesForBackup(@Nullable String changeToken) {
        try {
            if (changeToken == null) {
                return mDatabaseHelper.getChangesAndTokenFromDataTables();
            }
            BackupChangeTokenHelper.BackupChangeToken backupChangeToken =
                    BackupChangeTokenHelper.getBackupChangeToken(mTransactionManager, changeToken);
            if (!mDatabaseHelper.isChangeLogsTokenValid(
                    backupChangeToken.getChangeLogsRequestToken())) {
                throw new IllegalArgumentException("Change token invalid");
            }
            var recordType = backupChangeToken.getRecordType();
            if (recordType != RECORD_TYPE_UNKNOWN) {
                return mDatabaseHelper.getChangesAndTokenFromDataTables(
                        recordType,
                        backupChangeToken.getDataTablePageToken(),
                        backupChangeToken.getChangeLogsRequestToken());
            }
            return mDatabaseHelper.getIncrementalChanges(
                    backupChangeToken.getChangeLogsRequestToken());
        } catch (SQLiteException exception) {
            Slog.e(TAG, "Failed to read or write to database", exception);
            throw exception;
        } catch (IllegalStateException exception) {
            // This case is impossible because the database enforces uuid's non-nullity but
            // within the RecordInternal class this is defined as nullable.
            Slog.e(TAG, "Missing uuid for record", exception);
            throw exception;
        }
    }

    /** Returns all user settings bundled as a single byte array. */
    @NonNull
    public GetSettingsForBackupResponse getSettingsForBackup() {
        Slog.i(TAG, "Formatting user settings for export.");
        CloudBackupSettingsHelper cloudBackupSettingsHelper =
                new CloudBackupSettingsHelper(mPriorityHelper, mPreferenceHelper, mAppInfoHelper);

        byte[] data = cloudBackupSettingsHelper.collectUserSettings().toByteArray();

        return new GetSettingsForBackupResponse(PROTO_VERSION, new BackupSettings(data));
    }
}
