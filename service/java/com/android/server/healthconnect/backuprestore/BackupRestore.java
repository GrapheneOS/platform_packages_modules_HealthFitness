/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_FETCHING_DATA;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_NONE;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_UNKNOWN;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_VERSION_DIFF;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IN_PROGRESS;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_PENDING;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_COMPLETE;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_FAILED;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_RETRY;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STARTED;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STATE_UNKNOWN;

import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.EXTRA_JOB_NAME_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.EXTRA_USER_ID;

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager.DataDownloadState;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.restore.BackupFileNamesSet;
import android.health.connect.restore.StageRemoteDataException;
import android.health.connect.restore.StageRemoteDataRequest;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.exportimport.DatabaseMerger;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.GrantTimeXmlHelper;
import com.android.server.healthconnect.permission.UserGrantTimeState;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.utils.FilesUtil;
import com.android.server.healthconnect.utils.RunnableWithThrowable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that takes up the responsibility to perform backup / restore related tasks.
 *
 * @hide
 */
public final class BackupRestore {
    // Key for storing the current data download state
    @VisibleForTesting
    public static final String DATA_DOWNLOAD_STATE_KEY = "data_download_state_key";

    // The below values for the IntDef are defined in chronological order of the restore process.
    @VisibleForTesting public static final int INTERNAL_RESTORE_STATE_UNKNOWN = 0;
    @VisibleForTesting public static final int INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING = 1;
    @VisibleForTesting public static final int INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS = 2;
    @VisibleForTesting public static final int INTERNAL_RESTORE_STATE_STAGING_DONE = 3;
    @VisibleForTesting public static final int INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS = 4;
    // See b/290172311 for details.
    @VisibleForTesting public static final int INTERNAL_RESTORE_STATE_MERGING_DONE_OLD_CODE = 5;

    @VisibleForTesting public static final int INTERNAL_RESTORE_STATE_MERGING_DONE = 6;

    @VisibleForTesting
    static final long DATA_DOWNLOAD_TIMEOUT_INTERVAL_MILLIS = 14 * DateUtils.DAY_IN_MILLIS;

    @VisibleForTesting
    static final long DATA_STAGING_TIMEOUT_INTERVAL_MILLIS = DateUtils.DAY_IN_MILLIS;

    @VisibleForTesting
    static final long DATA_MERGING_TIMEOUT_INTERVAL_MILLIS = 5 * DateUtils.DAY_IN_MILLIS;

    @VisibleForTesting
    static final long DATA_MERGING_RETRY_DELAY_MILLIS = 12 * DateUtils.HOUR_IN_MILLIS;

    // Used in #setOverrideDeadline to set a minimum window of 24 hours. See b/311402873,
    // b/319721118
    @VisibleForTesting
    static final long MINIMUM_LATENCY_WINDOW_MILLIS = 24 * DateUtils.HOUR_IN_MILLIS;

    @VisibleForTesting static final String DATA_DOWNLOAD_TIMEOUT_KEY = "data_download_timeout_key";

    @VisibleForTesting static final String DATA_STAGING_TIMEOUT_KEY = "data_staging_timeout_key";
    @VisibleForTesting static final String DATA_MERGING_TIMEOUT_KEY = "data_merging_timeout_key";

    @VisibleForTesting
    static final String DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY = "data_download_timeout_cancelled_key";

    @VisibleForTesting
    static final String DATA_STAGING_TIMEOUT_CANCELLED_KEY = "data_staging_timeout_cancelled_key";

    @VisibleForTesting
    static final String DATA_MERGING_TIMEOUT_CANCELLED_KEY = "data_merging_timeout_cancelled_key";

    @VisibleForTesting static final String DATA_MERGING_RETRY_KEY = "data_merging_retry_key";
    private static final String DATA_MERGING_RETRY_CANCELLED_KEY =
            "data_merging_retry_cancelled_key";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        INTERNAL_RESTORE_STATE_UNKNOWN,
        INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING,
        INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS,
        INTERNAL_RESTORE_STATE_STAGING_DONE,
        INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS,
        INTERNAL_RESTORE_STATE_MERGING_DONE_OLD_CODE,
        INTERNAL_RESTORE_STATE_MERGING_DONE
    })
    public @interface InternalRestoreState {}

    // Key for storing the current data restore state on disk.
    public static final String DATA_RESTORE_STATE_KEY = "data_restore_state_key";
    // Key for storing the error restoring HC data.
    public static final String DATA_RESTORE_ERROR_KEY = "data_restore_error_key";

    @VisibleForTesting
    static final String GRANT_TIME_FILE_NAME = "health-permissions-first-grant-times.xml";

    @VisibleForTesting static final String STAGED_DATABASE_DIR = "remote_staged";

    @VisibleForTesting static final String STAGED_DATABASE_NAME = "healthconnect_staged.db";

    private static final String DATABASE_BACKUP_FILE_NAME = "healthconnect_backup.db";
    private static final String BACKUP_DIR = "backup";
    private static final List<String> PHR_TABLES_TO_CLEAR =
            List.of(
                    MedicalDataSourceHelper.getMainTableName(),
                    MedicalResourceHelper.getMainTableName(),
                    MedicalResourceIndicesHelper.getTableName());

    private static final String TAG = "HealthConnectBackupRestore";
    private final ReentrantReadWriteLock mStatesLock = new ReentrantReadWriteLock(true);
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final MigrationStateManager mMigrationStateManager;

    private final Context mContext;
    private final Object mMergingLock = new Object();

    private final DatabaseMerger mDatabaseMerger;

    private final PreferenceHelper mPreferenceHelper;
    private final TransactionManager mTransactionManager;
    private final File mEnvironmentDataDirectory;

    private boolean mActivelyStagingRemoteData = false;

    private volatile UserHandle mCurrentForegroundUser;
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final BackupRestoreJobScheduler mJobScheduler;

    @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
    public BackupRestore(
            AppInfoHelper appInfoHelper,
            FirstGrantTimeManager firstGrantTimeManager,
            MigrationStateManager migrationStateManager,
            PreferenceHelper preferenceHelper,
            TransactionManager transactionManager,
            FitnessRecordReadHelper fitnessRecordReadHelper,
            Context context,
            DeviceInfoHelper deviceInfoHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            HealthConnectThreadScheduler threadScheduler,
            File environmentDataDirectory) {
        this(
                appInfoHelper,
                firstGrantTimeManager,
                migrationStateManager,
                preferenceHelper,
                transactionManager,
                fitnessRecordReadHelper,
                context,
                deviceInfoHelper,
                healthDataCategoryPriorityHelper,
                threadScheduler,
                environmentDataDirectory,
                new BackupRestoreJobScheduler());
    }

    @VisibleForTesting
    BackupRestore(
            AppInfoHelper appInfoHelper,
            FirstGrantTimeManager firstGrantTimeManager,
            MigrationStateManager migrationStateManager,
            PreferenceHelper preferenceHelper,
            TransactionManager transactionManager,
            FitnessRecordReadHelper fitnessRecordReadHelper,
            Context context,
            DeviceInfoHelper deviceInfoHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            HealthConnectThreadScheduler threadScheduler,
            File environmentDataDirectory,
            BackupRestoreJobScheduler jobScheduler) {
        mFirstGrantTimeManager = firstGrantTimeManager;
        mMigrationStateManager = migrationStateManager;
        mContext = context;
        mCurrentForegroundUser = mContext.getUser();
        mDatabaseMerger =
                new DatabaseMerger(
                        appInfoHelper,
                        deviceInfoHelper,
                        healthDataCategoryPriorityHelper,
                        transactionManager,
                        fitnessRecordReadHelper);
        mPreferenceHelper = preferenceHelper;
        mTransactionManager = transactionManager;
        mThreadScheduler = threadScheduler;
        mEnvironmentDataDirectory = environmentDataDirectory;
        mJobScheduler = jobScheduler;
    }

    public void setupForUser(UserHandle currentForegroundUser) {
        Slog.d(TAG, "Performing user switch operations.");
        mCurrentForegroundUser = currentForegroundUser;
        mThreadScheduler.scheduleInternalTask(this::scheduleAllJobs);
    }

    /**
     * Prepares for staging all health connect remote data.
     *
     * @return true if the preparation was successful. false either if staging already in progress
     *     or done.
     */
    public boolean prepForStagingIfNotAlreadyDone() {
        mStatesLock.writeLock().lock();
        try {
            Slog.d(TAG, "Prepping for staging.");
            setDataDownloadState(DATA_DOWNLOAD_COMPLETE, false /* force */);
            @InternalRestoreState int curDataRestoreState = getInternalRestoreState();
            if (curDataRestoreState >= INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS) {
                if (curDataRestoreState >= INTERNAL_RESTORE_STATE_STAGING_DONE) {
                    Slog.w(TAG, "Staging is already done. Cur state " + curDataRestoreState);
                } else {
                    // Maybe the caller died and is trying to stage the data again.
                    Slog.w(TAG, "Already in the process of staging.");
                }
                return false;
            }
            mActivelyStagingRemoteData = true;
            setInternalRestoreState(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS, false /* force */);
            return true;
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    /**
     * Stages all health connect remote data for merging later.
     *
     * <p>This should be called on the proper thread.
     */
    public void stageAllHealthConnectRemoteData(
            Map<String, ParcelFileDescriptor> pfdsByFileName,
            Map<String, HealthConnectException> exceptionsByFileName,
            UserHandle userHandle,
            IDataStagingFinishedCallback callback) {
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext, userHandle, STAGED_DATABASE_DIR, mEnvironmentDataDirectory);
        File stagedRemoteDataDir = dbContext.getDataDir();
        try {
            stagedRemoteDataDir.mkdirs();

            // Now that we have the dir we can try to copy all the data.
            // Any exceptions we face will be collected and shared with the caller.
            pfdsByFileName.forEach(
                    (fileName, pfd) -> {
                        File destination = new File(stagedRemoteDataDir, fileName);
                        try (FileInputStream inputStream =
                                new FileInputStream(pfd.getFileDescriptor())) {
                            Path destinationPath =
                                    FileSystems.getDefault().getPath(destination.getAbsolutePath());
                            Files.copy(
                                    inputStream,
                                    destinationPath,
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            Slog.e(
                                    TAG,
                                    "Failed to get copy to destination: " + destination.getName(),
                                    e);
                            destination.delete();
                            exceptionsByFileName.put(
                                    fileName,
                                    new HealthConnectException(
                                            HealthConnectException.ERROR_IO, e.getMessage()));
                        } catch (SecurityException e) {
                            Slog.e(
                                    TAG,
                                    "Failed to get copy to destination: " + destination.getName(),
                                    e);
                            destination.delete();
                            exceptionsByFileName.put(
                                    fileName,
                                    new HealthConnectException(
                                            HealthConnectException.ERROR_SECURITY, e.getMessage()));
                        } finally {
                            try {
                                pfd.close();
                            } catch (IOException e) {
                                exceptionsByFileName.put(
                                        fileName,
                                        new HealthConnectException(
                                                HealthConnectException.ERROR_IO, e.getMessage()));
                            }
                        }
                    });
        } finally {
            // We are done staging all the remote data, update the data restore state.
            // Even if we encountered any exception we still say that we are "done" as
            // we don't expect the caller to retry and see different results.
            setInternalRestoreState(INTERNAL_RESTORE_STATE_STAGING_DONE, false);
            mActivelyStagingRemoteData = false;

            // Share the result / exception with the caller.
            try {
                if (exceptionsByFileName.isEmpty()) {
                    callback.onResult();
                    Slog.i(TAG, "Restore response sent successfully to caller.");
                } else {
                    Slog.i(TAG, "Exceptions encountered during staging.");
                    setDataRestoreError(RESTORE_ERROR_FETCHING_DATA);
                    callback.onError(new StageRemoteDataException(exceptionsByFileName));
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Restore response could not be sent to the caller.", e);
            } catch (SecurityException e) {
                Log.e(
                        TAG,
                        "Restore response could not be sent due to conflicting AIDL definitions",
                        e);
            } finally {
                // Now that the callback for the stageAllHealthConnectRemoteData API has been called
                // we can start the merging process.
                merge();
            }
        }
    }

    /** Writes the backup data into files represented by the passed file descriptors. */
    public void getAllDataForBackup(
            StageRemoteDataRequest stageRemoteDataRequest, UserHandle userHandle) {
        Slog.i(
                TAG,
                "getAllDataForBackup, number of files to backup = "
                        + stageRemoteDataRequest.getPfdsByFileName().size());

        Map<String, ParcelFileDescriptor> pfdsByFileName =
                stageRemoteDataRequest.getPfdsByFileName();

        // If PERSONAL_HEALTH_RECORD_DISABLE_D2D is enabled, create a temporary copy of the
        // HC database and delete all the PHR tables content.
        // Set the default to the original database path, if the PERSONAL_HEALTH_RECORD_DISABLE_D2D
        // is enabled, it will be updated to be database copy path.
        File databasePath = mTransactionManager.getDatabasePath();
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext, userHandle, BACKUP_DIR, mEnvironmentDataDirectory);
        File backupDataDir = dbContext.getDataDir();
        if (Flags.personalHealthRecordDisableD2d()) {
            databasePath = new File(backupDataDir, DATABASE_BACKUP_FILE_NAME);
            try {
                // Copies the HC database to the temp file.
                copyDatabase(databasePath);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create local file for backup", e);
                return;
            }

            try {
                // Deletes the PHR tables content from the temp file.
                deletePhrTablesContent(dbContext);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to clear PHR tables.", e);
                return;
            }
        }

        var backupFilesByFileNames =
                getBackupFilesByFileNames(userHandle, backupDataDir, databasePath);
        pfdsByFileName.forEach(
                (fileName, pfd) -> {
                    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
                    Path sourceFilePath = backupFilesByFileNames.get(fileName).toPath();
                    try (FileOutputStream outputStream =
                            new FileOutputStream(pfd.getFileDescriptor())) {
                        Files.copy(sourceFilePath, outputStream);
                    } catch (IOException | SecurityException e) {
                        Slog.e(TAG, "Failed to send " + fileName + " for backup", e);
                    } finally {
                        try {
                            pfd.close();
                        } catch (IOException e) {
                            Slog.e(TAG, "Failed to close " + fileName + " for backup", e);
                        }
                    }
                });

        if (Flags.d2dFileDeletionBugFix()) {
            deleteBackupFiles(backupDataDir);
        }
    }

    /** Get the file names of all the files that are transported during backup / restore. */
    public BackupFileNamesSet getAllBackupFileNames(boolean forDeviceToDevice) {
        Slog.i(TAG, "getAllBackupFileNames, forDeviceToDevice = " + forDeviceToDevice);
        ArraySet<String> backupFileNames = new ArraySet<>();
        if (forDeviceToDevice) {
            backupFileNames.add(STAGED_DATABASE_NAME);
        }
        backupFileNames.add(GRANT_TIME_FILE_NAME);
        return new BackupFileNamesSet(backupFileNames);
    }

    private void copyDatabase(File destination) throws IOException {
        Slog.i(TAG, "Database copying started.");

        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Unable to create directory for the database copy.");
        }

        Files.copy(
                mTransactionManager.getDatabasePath().toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        Slog.i(TAG, "Database copying completed: " + destination.toPath().toAbsolutePath());
    }

    private void deletePhrTablesContent(HealthConnectContext dbContext) {
        // Throwing a exception when calling this method implies that it was not possible to
        // create a HC database from the file and, therefore, most probably the database was
        // corrupted during the file copy.
        try (HealthConnectDatabase exportDatabase =
                new HealthConnectDatabase(dbContext, DATABASE_BACKUP_FILE_NAME)) {
            SQLiteDatabase db = exportDatabase.getReadableDatabase();
            for (String tableName : PHR_TABLES_TO_CLEAR) {
                db.execSQL("DELETE FROM " + tableName + ";");
            }
        }
        Slog.i(TAG, "Drop PHR tables completed.");
    }

    /** Updates the download state of the remote data. */
    public void updateDataDownloadState(@DataDownloadState int downloadState) {
        setDataDownloadState(downloadState, false /* force */);

        if (downloadState == DATA_DOWNLOAD_COMPLETE) {
            setInternalRestoreState(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING, false /* force */);
        } else if (downloadState == DATA_DOWNLOAD_FAILED) {
            setInternalRestoreState(INTERNAL_RESTORE_STATE_MERGING_DONE, false /* force */);
            setDataRestoreError(RESTORE_ERROR_FETCHING_DATA);
        }
    }

    /** Deletes all the staged data and resets all the states. */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public void deleteAndResetEverything(UserHandle userHandle) {
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext, userHandle, STAGED_DATABASE_DIR, mEnvironmentDataDirectory);

        // Don't delete anything while we are in the process of merging staged data.
        synchronized (mMergingLock) {
            dbContext.deleteDatabase(STAGED_DATABASE_NAME);
            FilesUtil.deleteDir(dbContext.getDataDir());
        }
        setDataDownloadState(DATA_DOWNLOAD_STATE_UNKNOWN, true /* force */);
        setInternalRestoreState(INTERNAL_RESTORE_STATE_UNKNOWN, true /* force */);
        setDataRestoreError(RESTORE_ERROR_NONE);
    }

    /** Shares the {@link HealthConnectDataState} in the provided callback. */
    public @HealthConnectDataState.DataRestoreState int getDataRestoreState() {
        @InternalRestoreState int currentRestoreState = getInternalRestoreState();
        @DataDownloadState int currentDownloadState = getDataDownloadState();

        // Return IDLE if neither the download or restore has started yet.
        if (currentRestoreState == INTERNAL_RESTORE_STATE_UNKNOWN
                && currentDownloadState == DATA_DOWNLOAD_STATE_UNKNOWN) {
            return RESTORE_STATE_IDLE;
        }

        // Return IDLE if restore is complete.
        if (currentRestoreState == INTERNAL_RESTORE_STATE_MERGING_DONE) {
            return RESTORE_STATE_IDLE;
        }
        // Return IN_PROGRESS if merging is currently in progress.
        if (currentRestoreState == INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS) {
            return RESTORE_STATE_IN_PROGRESS;
        }

        // In all other cases, return restore pending.
        return RESTORE_STATE_PENDING;
    }

    /** Get the current data restore error. */
    public @HealthConnectDataState.DataRestoreError int getDataRestoreError() {
        @HealthConnectDataState.DataRestoreError int dataRestoreError = RESTORE_ERROR_NONE;
        String restoreErrorOnDisk = mPreferenceHelper.getPreference(DATA_RESTORE_ERROR_KEY);

        if (restoreErrorOnDisk == null) {
            return dataRestoreError;
        }
        try {
            dataRestoreError = Integer.parseInt(restoreErrorOnDisk);
        } catch (Exception e) {
            Slog.e(TAG, "Exception parsing restoreErrorOnDisk " + restoreErrorOnDisk, e);
        }
        return dataRestoreError;
    }

    /** Returns the file names of all the staged files. */
    @VisibleForTesting
    public Set<String> getStagedRemoteFileNames(UserHandle userHandle) {
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext, userHandle, STAGED_DATABASE_DIR, mEnvironmentDataDirectory);
        File[] allFiles = dbContext.getDataDir().listFiles();
        if (allFiles == null) {
            return Collections.emptySet();
        }
        return Stream.of(allFiles)
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    /** Returns true if restore merging is in progress. API calls are blocked when this is true. */
    public boolean isRestoreMergingInProgress() {
        return getInternalRestoreState() == INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS;
    }

    /** Schedules any pending jobs. */
    public void scheduleAllJobs() {
        scheduleDownloadStateTimeoutJob();
        scheduleStagingTimeoutJob();
        scheduleMergingTimeoutJob();

        // We can schedule "retry merging" only if we are in the STAGING_DONE state.  However, if we
        // are in STAGING_DONE state, then we should definitely attempt merging now - and that's
        // what we will do below.
        // So, there's no point in scheduling a "retry merging" job.  If Migration is going on then
        // the merge attempt will take care of that automatically (and schedule the retry job as
        // needed).
        triggerMergingIfApplicable();
    }

    /** Cancel all the jobs and sets the cancelled time. */
    public void cancelAllJobs() {
        mJobScheduler.cancelAllJobs(mContext);
        setJobCancelledTimeIfExists(DATA_DOWNLOAD_TIMEOUT_KEY, DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY);
        setJobCancelledTimeIfExists(DATA_STAGING_TIMEOUT_KEY, DATA_STAGING_TIMEOUT_CANCELLED_KEY);
        setJobCancelledTimeIfExists(DATA_MERGING_TIMEOUT_KEY, DATA_MERGING_TIMEOUT_CANCELLED_KEY);
        setJobCancelledTimeIfExists(DATA_MERGING_RETRY_KEY, DATA_MERGING_RETRY_CANCELLED_KEY);
    }

    public UserHandle getCurrentUserHandle() {
        return mCurrentForegroundUser;
    }

    void setInternalRestoreState(@InternalRestoreState int dataRestoreState, boolean force) {
        @InternalRestoreState int currentRestoreState = getInternalRestoreState();
        mStatesLock.writeLock().lock();
        try {
            if (!force && currentRestoreState >= dataRestoreState) {
                Slog.w(
                        TAG,
                        "Attempt to update data restore state in wrong order from "
                                + currentRestoreState
                                + " to "
                                + dataRestoreState);
                return;
            }
            mPreferenceHelper.insertOrReplacePreference(
                    DATA_RESTORE_STATE_KEY, String.valueOf(dataRestoreState));

            if (dataRestoreState == INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING
                    || dataRestoreState == INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS) {
                scheduleStagingTimeoutJob();
            } else if (dataRestoreState == INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS) {
                scheduleMergingTimeoutJob();
            }
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    @InternalRestoreState
    int getInternalRestoreState() {
        mStatesLock.readLock().lock();
        try {
            String restoreStateOnDisk = mPreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY);
            @InternalRestoreState int currentRestoreState = INTERNAL_RESTORE_STATE_UNKNOWN;
            if (restoreStateOnDisk == null) {
                return currentRestoreState;
            }
            try {
                currentRestoreState = Integer.parseInt(restoreStateOnDisk);
            } catch (Exception e) {
                Slog.e(TAG, "Exception parsing restoreStateOnDisk: " + restoreStateOnDisk, e);
            }
            // If we are not actively staging the data right now but the disk still reflects that we
            // are then that means we died in the middle of staging.  We should be waiting for the
            // remote data to be staged now.
            if (!mActivelyStagingRemoteData
                    && currentRestoreState == INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS) {
                currentRestoreState = INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING;
            }
            return currentRestoreState;
        } finally {
            mStatesLock.readLock().unlock();
        }
    }

    /** Returns true if this job needs rescheduling; false otherwise. */
    @VisibleForTesting
    boolean handleJob(PersistableBundle extras) {
        String jobName = extras.getString(EXTRA_JOB_NAME_KEY);
        switch (jobName) {
            case DATA_DOWNLOAD_TIMEOUT_KEY -> executeDownloadStateTimeoutJob();
            case DATA_STAGING_TIMEOUT_KEY -> executeStagingTimeoutJob();
            case DATA_MERGING_TIMEOUT_KEY -> executeMergingTimeoutJob();
            case DATA_MERGING_RETRY_KEY -> executeRetryMergingJob();
            default -> Slog.w(TAG, "Unknown job" + jobName + " delivered.");
        }
        // None of the jobs want to reschedule.
        return false;
    }

    @VisibleForTesting
    boolean shouldAttemptMerging() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState == INTERNAL_RESTORE_STATE_STAGING_DONE
                || internalRestoreState == INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS
                || internalRestoreState == INTERNAL_RESTORE_STATE_MERGING_DONE_OLD_CODE) {
            Slog.i(TAG, "Should attempt merging now with state = " + internalRestoreState);
            return true;
        }
        return false;
    }

    @VisibleForTesting
    void merge() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState >= INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS) {
            Slog.i(TAG, "Not merging as internalRestoreState is " + internalRestoreState);
            return;
        }

        if (mMigrationStateManager.isMigrationInProgress()) {
            Slog.i(TAG, "Not merging as Migration in progress.");
            scheduleRetryMergingJob();
            return;
        }

        int currentDbVersion = mTransactionManager.getDatabaseVersion();
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext,
                        mCurrentForegroundUser,
                        STAGED_DATABASE_DIR,
                        mEnvironmentDataDirectory);
        File stagedDbFile = dbContext.getDatabasePath(STAGED_DATABASE_NAME);
        if (stagedDbFile.exists()) {
            try (SQLiteDatabase stagedDb =
                    SQLiteDatabase.openDatabase(
                            stagedDbFile, new SQLiteDatabase.OpenParams.Builder().build())) {
                int stagedDbVersion = stagedDb.getVersion();
                Slog.i(
                        TAG,
                        "merging staged data, current version = "
                                + currentDbVersion
                                + ", staged version = "
                                + stagedDbVersion);
                if (currentDbVersion < stagedDbVersion) {
                    Slog.i(TAG, "Module needs upgrade for merging to version " + stagedDbVersion);
                    setDataRestoreError(RESTORE_ERROR_VERSION_DIFF);
                    return;
                }
            }
        } else {
            Slog.i(TAG, "No database file found to merge.");
        }

        Slog.i(TAG, "Starting the data merge.");
        setInternalRestoreState(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS, false);
        mergeGrantTimes(dbContext);
        mergeDatabase(dbContext);
        setInternalRestoreState(INTERNAL_RESTORE_STATE_MERGING_DONE, false);

        // Reset the error in case it was due to version diff.
        // TODO(b/327170886): Should we always set it to NONE once merging is done?
        if (getDataRestoreError() == RESTORE_ERROR_VERSION_DIFF) {
            setDataRestoreError(RESTORE_ERROR_NONE);
        }
    }

    private Map<String, File> getBackupFilesByFileNames(
            UserHandle userHandle, File backupDataDir, File databasePath) {
        ArrayMap<String, File> backupFilesByFileNames = new ArrayMap<>();

        backupFilesByFileNames.put(STAGED_DATABASE_NAME, databasePath);

        File grantTimeFile = new File(backupDataDir, GRANT_TIME_FILE_NAME);
        try {
            grantTimeFile.createNewFile();
            GrantTimeXmlHelper.serializeGrantTimes(
                    grantTimeFile, mFirstGrantTimeManager.getGrantTimeStateForUser(userHandle));
            backupFilesByFileNames.put(grantTimeFile.getName(), grantTimeFile);
        } catch (IOException e) {
            Slog.e(TAG, "Could not create the grant time file for backup.", e);
        }

        return backupFilesByFileNames;
    }

    private void deleteBackupFiles(File backupDataDir) {
        if (Flags.personalHealthRecordDisableD2d()) {
            File databaseBackupFile = new File(backupDataDir, DATABASE_BACKUP_FILE_NAME);
            databaseBackupFile.delete();
        }
        // We only create a backup copy for grant times. DB is copied from source.
        File grantTimeFile = new File(backupDataDir, GRANT_TIME_FILE_NAME);
        grantTimeFile.delete();
    }

    @DataDownloadState
    private int getDataDownloadState() {
        mStatesLock.readLock().lock();
        try {
            String downloadStateOnDisk = mPreferenceHelper.getPreference(DATA_DOWNLOAD_STATE_KEY);
            @DataDownloadState int currentDownloadState = DATA_DOWNLOAD_STATE_UNKNOWN;
            if (downloadStateOnDisk == null) {
                return currentDownloadState;
            }
            try {
                currentDownloadState = Integer.parseInt(downloadStateOnDisk);
            } catch (Exception e) {
                Slog.e(TAG, "Exception parsing downloadStateOnDisk " + downloadStateOnDisk, e);
            }
            return currentDownloadState;
        } finally {
            mStatesLock.readLock().unlock();
        }
    }

    private void setDataDownloadState(@DataDownloadState int downloadState, boolean force) {
        mStatesLock.writeLock().lock();
        try {
            @DataDownloadState int currentDownloadState = getDataDownloadState();
            if (!force
                    && (currentDownloadState == DATA_DOWNLOAD_FAILED
                            || currentDownloadState == DATA_DOWNLOAD_COMPLETE)) {
                Slog.w(TAG, "HC data download already in terminal state.");
                return;
            }
            mPreferenceHelper.insertOrReplacePreference(
                    DATA_DOWNLOAD_STATE_KEY, String.valueOf(downloadState));

            if (downloadState == DATA_DOWNLOAD_STARTED || downloadState == DATA_DOWNLOAD_RETRY) {
                mPreferenceHelper.insertOrReplacePreference(
                        DATA_DOWNLOAD_TIMEOUT_KEY, Long.toString(Instant.now().toEpochMilli()));
                scheduleDownloadStateTimeoutJob();
            }
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    // Creating a separate single line method to keep this code close to the rest of the code that
    // uses PreferenceHelper to keep data on the disk.
    private void setDataRestoreError(
            @HealthConnectDataState.DataRestoreError int dataRestoreError) {
        mPreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_ERROR_KEY, String.valueOf(dataRestoreError));
    }

    /** Schedule timeout for data download state so that we are not stuck in the current state. */
    private void scheduleDownloadStateTimeoutJob() {
        @DataDownloadState int currentDownloadState = getDataDownloadState();
        if (currentDownloadState != DATA_DOWNLOAD_STARTED
                && currentDownloadState != DATA_DOWNLOAD_RETRY) {
            Slog.i(
                    TAG,
                    "Attempt to schedule download timeout job with state: " + currentDownloadState);
            // We are not in the correct state. There's no need to set the timer.
            return;
        }

        // We might be here because the device rebooted or the user switched. If a timer was already
        // going on then we want to continue that timer.
        long timeoutMillis =
                getRemainingTimeoutMillis(
                        DATA_DOWNLOAD_TIMEOUT_KEY,
                        DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY,
                        DATA_DOWNLOAD_TIMEOUT_INTERVAL_MILLIS);

        int userId = mCurrentForegroundUser.getIdentifier();
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userId);
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_DOWNLOAD_TIMEOUT_KEY);
        JobInfo.Builder jobInfoBuilder =
                new JobInfo.Builder(
                                BackupRestoreJobService.BACKUP_RESTORE_JOB_ID + userId,
                                new ComponentName(mContext, BackupRestoreJobService.class))
                        .setExtras(extras)
                        .setMinimumLatency(timeoutMillis)
                        .setOverrideDeadline(timeoutMillis + MINIMUM_LATENCY_WINDOW_MILLIS);
        Slog.i(
                TAG,
                "Scheduling download state timeout job with period: " + timeoutMillis + " millis");
        mJobScheduler.schedule(mContext, jobInfoBuilder.build(), this);

        // Set the start time
        mPreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_TIMEOUT_KEY, Long.toString(Instant.now().toEpochMilli()));
    }

    private void executeDownloadStateTimeoutJob() {
        @DataDownloadState int currentDownloadState = getDataDownloadState();
        if (currentDownloadState == DATA_DOWNLOAD_STARTED
                || currentDownloadState == DATA_DOWNLOAD_RETRY) {
            Slog.i(TAG, "Executing download state timeout job");
            setDataDownloadState(DATA_DOWNLOAD_FAILED, false);
            setDataRestoreError(RESTORE_ERROR_FETCHING_DATA);
            // Remove the remaining timeouts from the disk
            mPreferenceHelper.insertOrReplacePreference(DATA_DOWNLOAD_TIMEOUT_KEY, "");
            mPreferenceHelper.insertOrReplacePreference(DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY, "");
        } else {
            Slog.i(TAG, "Download state timeout job fired in state: " + currentDownloadState);
        }
    }

    /** Schedule timeout for data staging state so that we are not stuck in the current state. */
    private void scheduleStagingTimeoutJob() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState != INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING
                && internalRestoreState != INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS) {
            // We are not in the correct state. There's no need to set the timer.
            Slog.i(
                    TAG,
                    "Attempt to schedule staging timeout job with state: " + internalRestoreState);
            return;
        }

        // We might be here because the device rebooted or the user switched. If a timer was already
        // going on then we want to continue that timer.
        long timeoutMillis =
                getRemainingTimeoutMillis(
                        DATA_STAGING_TIMEOUT_KEY,
                        DATA_STAGING_TIMEOUT_CANCELLED_KEY,
                        DATA_STAGING_TIMEOUT_INTERVAL_MILLIS);

        int userId = mCurrentForegroundUser.getIdentifier();
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userId);
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_STAGING_TIMEOUT_KEY);
        JobInfo.Builder jobInfoBuilder =
                new JobInfo.Builder(
                                BackupRestoreJobService.BACKUP_RESTORE_JOB_ID + userId,
                                new ComponentName(mContext, BackupRestoreJobService.class))
                        .setExtras(extras)
                        .setMinimumLatency(timeoutMillis)
                        .setOverrideDeadline(timeoutMillis + MINIMUM_LATENCY_WINDOW_MILLIS);
        Slog.i(TAG, "Scheduling staging timeout job with period: " + timeoutMillis + " millis");
        mJobScheduler.schedule(mContext, jobInfoBuilder.build(), this);

        // Set the start time
        mPreferenceHelper.insertOrReplacePreference(
                DATA_STAGING_TIMEOUT_KEY, Long.toString(Instant.now().toEpochMilli()));
    }

    private void executeStagingTimeoutJob() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState == INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING
                || internalRestoreState == INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS) {
            Slog.i(TAG, "Executing staging timeout job");
            setInternalRestoreState(INTERNAL_RESTORE_STATE_MERGING_DONE, false);
            setDataRestoreError(RESTORE_ERROR_UNKNOWN);
            // Remove the remaining timeouts from the disk
            mPreferenceHelper.insertOrReplacePreference(DATA_STAGING_TIMEOUT_KEY, "");
            mPreferenceHelper.insertOrReplacePreference(DATA_STAGING_TIMEOUT_CANCELLED_KEY, "");
        } else {
            Slog.i(TAG, "Staging timeout job fired in state: " + internalRestoreState);
        }
    }

    /** Schedule timeout for data merging state so that we are not stuck in the current state. */
    private void scheduleMergingTimeoutJob() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState != INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS) {
            // We are not in the correct state. There's no need to set the timer.
            Slog.i(
                    TAG,
                    "Attempt to schedule merging timeout job with state: " + internalRestoreState);
            return;
        }

        // We might be here because the device rebooted or the user switched. If a timer was already
        // going on then we want to continue that timer.
        long timeoutMillis =
                getRemainingTimeoutMillis(
                        DATA_MERGING_TIMEOUT_KEY,
                        DATA_MERGING_TIMEOUT_CANCELLED_KEY,
                        DATA_MERGING_TIMEOUT_INTERVAL_MILLIS);

        int userId = mCurrentForegroundUser.getIdentifier();
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userId);
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_MERGING_TIMEOUT_KEY);
        JobInfo.Builder jobInfoBuilder =
                new JobInfo.Builder(
                                BackupRestoreJobService.BACKUP_RESTORE_JOB_ID + userId,
                                new ComponentName(mContext, BackupRestoreJobService.class))
                        .setExtras(extras)
                        .setMinimumLatency(timeoutMillis)
                        .setOverrideDeadline(timeoutMillis + MINIMUM_LATENCY_WINDOW_MILLIS);
        Slog.i(TAG, "Scheduling merging timeout job with period: " + timeoutMillis + " millis");
        mJobScheduler.schedule(mContext, jobInfoBuilder.build(), this);

        // Set the start time
        mPreferenceHelper.insertOrReplacePreference(
                DATA_MERGING_TIMEOUT_KEY, Long.toString(Instant.now().toEpochMilli()));
    }

    private void executeMergingTimeoutJob() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState == INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS) {
            Slog.i(TAG, "Executing merging timeout job");
            setInternalRestoreState(INTERNAL_RESTORE_STATE_MERGING_DONE, false);
            setDataRestoreError(RESTORE_ERROR_UNKNOWN);
            // Remove the remaining timeouts from the disk
            mPreferenceHelper.insertOrReplacePreference(DATA_MERGING_TIMEOUT_KEY, "");
            mPreferenceHelper.insertOrReplacePreference(DATA_MERGING_TIMEOUT_CANCELLED_KEY, "");
        } else {
            Slog.i(TAG, "Merging timeout job fired in state: " + internalRestoreState);
        }
    }

    private void scheduleRetryMergingJob() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState != INTERNAL_RESTORE_STATE_STAGING_DONE) {
            // We can do merging only if we are in the STAGING_DONE state.
            Slog.i(
                    TAG,
                    "Attempt to schedule merging retry job with state: " + internalRestoreState);
            return;
        }

        int userId = mCurrentForegroundUser.getIdentifier();
        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, userId);
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_MERGING_RETRY_KEY);

        // We might be here because the device rebooted or the user switched. If a timer was already
        // going on then we want to continue that timer.
        long timeoutMillis =
                getRemainingTimeoutMillis(
                        DATA_MERGING_RETRY_KEY,
                        DATA_MERGING_RETRY_CANCELLED_KEY,
                        DATA_MERGING_RETRY_DELAY_MILLIS);
        JobInfo.Builder jobInfoBuilder =
                new JobInfo.Builder(
                                BackupRestoreJobService.BACKUP_RESTORE_JOB_ID + userId,
                                new ComponentName(mContext, BackupRestoreJobService.class))
                        .setExtras(extras)
                        .setMinimumLatency(timeoutMillis)
                        .setOverrideDeadline(timeoutMillis + MINIMUM_LATENCY_WINDOW_MILLIS);
        Slog.i(TAG, "Scheduling retry merging job with period: " + timeoutMillis + " millis");
        mJobScheduler.schedule(mContext, jobInfoBuilder.build(), this);

        // Set the start time
        mPreferenceHelper.insertOrReplacePreference(
                DATA_MERGING_RETRY_KEY, Long.toString(Instant.now().toEpochMilli()));
    }

    private void executeRetryMergingJob() {
        @InternalRestoreState int internalRestoreState = getInternalRestoreState();
        if (internalRestoreState == INTERNAL_RESTORE_STATE_STAGING_DONE) {
            Slog.i(TAG, "Retrying merging");
            merge();

            if (getInternalRestoreState() == INTERNAL_RESTORE_STATE_MERGING_DONE) {
                // Remove the remaining timeouts from the disk
                mPreferenceHelper.insertOrReplacePreference(DATA_MERGING_RETRY_KEY, "");
                mPreferenceHelper.insertOrReplacePreference(DATA_MERGING_RETRY_CANCELLED_KEY, "");
            }
        } else {
            Slog.i(TAG, "Merging retry job fired in state: " + internalRestoreState);
        }
    }

    private void triggerMergingIfApplicable() {
        mThreadScheduler.scheduleInternalTask(
                () -> {
                    if (shouldAttemptMerging()) {
                        Slog.i(TAG, "Attempting merging.");
                        setInternalRestoreState(INTERNAL_RESTORE_STATE_STAGING_DONE, true);
                        merge();
                    }
                });
    }

    private long getRemainingTimeoutMillis(
            String startTimeKey, String cancelledTimeKey, long stdTimeout) {
        String startTimeStr = mPreferenceHelper.getPreference(startTimeKey);
        if (startTimeStr == null || startTimeStr.trim().isEmpty()) {
            return stdTimeout;
        }
        long currTime = Instant.now().toEpochMilli();
        String cancelledTimeStr = mPreferenceHelper.getPreference(cancelledTimeKey);
        if (cancelledTimeStr == null || cancelledTimeStr.trim().isEmpty()) {
            return Math.max(0, stdTimeout - (currTime - Long.parseLong(startTimeStr)));
        }
        long spentTime = Long.parseLong(cancelledTimeStr) - Long.parseLong(startTimeStr);
        return Math.max(0, stdTimeout - spentTime);
    }

    private void setJobCancelledTimeIfExists(String startTimeKey, String cancelTimeKey) {
        if (mPreferenceHelper.getPreference(startTimeKey) != null) {
            mPreferenceHelper.insertOrReplacePreference(
                    cancelTimeKey, Long.toString(Instant.now().toEpochMilli()));
        }
    }

    private void mergeGrantTimes(HealthConnectContext dbContext) {
        File restoredGrantTimeFile = new File(dbContext.getDataDir(), GRANT_TIME_FILE_NAME);
        Slog.i(TAG, "Merging grant times.");

        UserGrantTimeState userGrantTimeState =
                GrantTimeXmlHelper.parseGrantTime(restoredGrantTimeFile);
        mFirstGrantTimeManager.applyAndStageGrantTimeStateForUser(
                mCurrentForegroundUser, userGrantTimeState);

        if (Flags.d2dFileDeletionBugFix()) {
            Slog.i(TAG, "Deleting staged grant times after merging.");
            restoredGrantTimeFile.delete();
        }
    }

    private void mergeDatabase(HealthConnectContext dbContext) {
        synchronized (mMergingLock) {
            if (!dbContext.getDatabasePath(STAGED_DATABASE_NAME).exists()) {
                Slog.i(TAG, "No staged db found.");
                // no db was staged
                return;
            }
            Slog.i(TAG, "Merging health connect db.");

            mDatabaseMerger.merge(new HealthConnectDatabase(dbContext, STAGED_DATABASE_NAME));

            // Delete the staged db as we are done merging.
            Slog.i(TAG, "Deleting staged db after merging.");
            dbContext.deleteDatabase(STAGED_DATABASE_NAME);
        }
    }

    /** Execute the task as critical section by holding read lock. */
    public <E extends Throwable> void runWithStatesReadLock(RunnableWithThrowable<E> task)
            throws E {
        mStatesLock.readLock().lock();
        try {
            task.run();
        } finally {
            mStatesLock.readLock().unlock();
        }
    }

    /** Inner class to separate out the scheduling layer to make it more testable. */
    static class BackupRestoreJobScheduler {
        void schedule(Context context, JobInfo jobInfo, BackupRestore backupRestore) {
            BackupRestoreJobService.setCurrentBackupRestore(backupRestore);
            final long token = Binder.clearCallingIdentity();
            try {
                int result =
                        requireNonNull(context.getSystemService(JobScheduler.class))
                                .forNamespace(BackupRestoreJobService.BACKUP_RESTORE_JOBS_NAMESPACE)
                                .schedule(jobInfo);

                if (result != JobScheduler.RESULT_SUCCESS) {
                    Slog.e(
                            TAG,
                            "Failed to schedule: "
                                    + jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY));
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /** Cancels all jobs for our namespace. */
        void cancelAllJobs(Context context) {
            requireNonNull(context.getSystemService(JobScheduler.class))
                    .forNamespace(BackupRestoreJobService.BACKUP_RESTORE_JOBS_NAMESPACE)
                    .cancelAll();
        }
    }

    /** Schedules the jobs for {@link BackupRestore} */
    public static final class BackupRestoreJobService extends JobService {
        public static final String BACKUP_RESTORE_JOBS_NAMESPACE = "BACKUP_RESTORE_JOBS_NAMESPACE";
        public static final String EXTRA_USER_ID = "user_id";
        public static final String EXTRA_JOB_NAME_KEY = "job_name";
        private static final int BACKUP_RESTORE_JOB_ID = 1000;

        @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
        private static volatile BackupRestore sBackupRestore;

        /**
         * Set the current backup restore.
         *
         * <p>Used for detecting user changes.
         *
         * @param backupRestore the current {@link BackupRestore}
         */
        public static void setCurrentBackupRestore(BackupRestore backupRestore) {
            sBackupRestore = backupRestore;
        }

        @Override
        public boolean onStartJob(JobParameters params) {
            int userId = params.getExtras().getInt(EXTRA_USER_ID, DEFAULT_INT);
            if (userId != sBackupRestore.getCurrentUserHandle().getIdentifier()) {
                Slog.w(
                        TAG,
                        "Got onStartJob for non active user: "
                                + userId
                                + ", but the current active user is: "
                                + sBackupRestore.getCurrentUserHandle().getIdentifier());
                return false;
            }

            String jobName = params.getExtras().getString(EXTRA_JOB_NAME_KEY);
            if (Objects.isNull(jobName)) {
                Slog.w(TAG, "Got onStartJob for a nameless job");
                return false;
            }

            sBackupRestore.mThreadScheduler.scheduleInternalTask(
                    () -> jobFinished(params, sBackupRestore.handleJob(params.getExtras())));

            return true;
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            return false;
        }
    }
}
