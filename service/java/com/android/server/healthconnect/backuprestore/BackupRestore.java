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

import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_FETCHING_DATA;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_NONE;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IN_PROGRESS;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_PENDING;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_COMPLETE;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_FAILED;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STATE_UNKNOWN;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager.DataDownloadState;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.restore.BackupFileNamesSet;
import android.health.connect.restore.StageRemoteDataException;
import android.health.connect.restore.StageRemoteDataRequest;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.utils.FilesUtil;

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
import java.util.Map;
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
    public static final int INTERNAL_RESTORE_STATE_UNKNOWN = 0;
    public static final int INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING = 1;
    public static final int INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS = 2;
    public static final int INTERNAL_RESTORE_STATE_STAGING_DONE = 3;
    public static final int INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS = 4;
    public static final int INTERNAL_RESTORE_STATE_MERGING_DONE = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        INTERNAL_RESTORE_STATE_UNKNOWN,
        INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING,
        INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS,
        INTERNAL_RESTORE_STATE_STAGING_DONE,
        INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS,
        INTERNAL_RESTORE_STATE_MERGING_DONE
    })
    public @interface InternalRestoreState {}
    // Key for storing the current data restore state on disk.
    public static final String DATA_RESTORE_STATE_KEY = "data_restore_state_key";
    // Key for storing the error restoring HC data.
    public static final String DATA_RESTORE_ERROR_KEY = "data_restore_error_key";

    private static final String TAG = "HealthConnectBackupRestore";
    private final ReentrantReadWriteLock mStatesLock = new ReentrantReadWriteLock(true);
    private final FirstGrantTimeManager mFirstGrantTimeManager;

    private boolean mActivelyStagingRemoteData = false;

    public BackupRestore(FirstGrantTimeManager firstGrantTimeManager) {
        mFirstGrantTimeManager = firstGrantTimeManager;
    }

    /**
     * Prepares for staging all health connect remote data.
     *
     * @return true if the preparation was successful. false either if staging already in progress
     *     or done.
     */
    public boolean prepForStagingIfNotAlreadyDone(int userId) {
        mStatesLock.writeLock().lock();
        try {
            setDataDownloadState(DATA_DOWNLOAD_COMPLETE, userId, false /* force */);
            @InternalRestoreState int curDataRestoreState = getInternalRestoreState(userId);
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
            setInternalRestoreState(
                    INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS, userId, false /* force */);
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
            int userId,
            @NonNull IDataStagingFinishedCallback callback) {
        File stagedRemoteDataDir = getStagedRemoteDataDirectoryForUser(userId);
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
                            destination.delete();
                            exceptionsByFileName.put(
                                    fileName,
                                    new HealthConnectException(
                                            HealthConnectException.ERROR_IO, e.getMessage()));
                        } catch (SecurityException e) {
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
            setInternalRestoreState(INTERNAL_RESTORE_STATE_STAGING_DONE, userId, false);
            mActivelyStagingRemoteData = false;

            // Share the result / exception with the caller.
            try {
                if (exceptionsByFileName.isEmpty()) {
                    callback.onResult();
                } else {
                    setDataRestoreError(RESTORE_ERROR_FETCHING_DATA, userId);
                    callback.onError(new StageRemoteDataException(exceptionsByFileName));
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Restore response could not be sent to the caller.", e);
            } catch (SecurityException e) {
                Log.e(
                        TAG,
                        "Restore response could not be sent due to conflicting AIDL definitions",
                        e);
            }
        }
    }

    /** Writes the backup data into files represented by the passed file descriptors. */
    public void getAllDataForBackup(
            @NonNull StageRemoteDataRequest stageRemoteDataRequest,
            @NonNull UserHandle userHandle) {
        Map<String, ParcelFileDescriptor> pfdsByFileName =
                stageRemoteDataRequest.getPfdsByFileName();

        pfdsByFileName.forEach(
                (fileName, pfd) -> {
                    var backupFilesByFileNames = getBackupFilesByFileNames(userHandle, false);
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
    }

    /** Get the file names of all the files that are transported during backup / restore. */
    public BackupFileNamesSet getAllBackupFileNames(
            @NonNull UserHandle userHandle, boolean forDeviceToDevice) {
        return new BackupFileNamesSet(
                getBackupFilesByFileNames(userHandle, !forDeviceToDevice).keySet());
    }

    private Map<String, File> getBackupFilesByFileNames(
            UserHandle userHandle, boolean excludeLargeFiles) {
        ArrayMap<String, File> backupFilesByFileNames = new ArrayMap<>();
        if (!excludeLargeFiles) {
            File databasePath = TransactionManager.getInitialisedInstance().getDatabasePath();
            backupFilesByFileNames.put(databasePath.getName(), databasePath);
        }
        File grantTimeFile = mFirstGrantTimeManager.getFile(userHandle);
        backupFilesByFileNames.put(grantTimeFile.getName(), grantTimeFile);
        return backupFilesByFileNames;
    }

    /** Updates the download state of the remote data. */
    public void updateDataDownloadState(
            @DataDownloadState int downloadState, @NonNull UserHandle userHandle) {
        setDataDownloadState(downloadState, userHandle.getIdentifier(), false /* force */);

        if (downloadState == DATA_DOWNLOAD_COMPLETE) {
            setInternalRestoreState(
                    INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING,
                    userHandle.getIdentifier(),
                    false /* force */);
        } else if (downloadState == DATA_DOWNLOAD_FAILED) {
            setInternalRestoreState(
                    INTERNAL_RESTORE_STATE_MERGING_DONE,
                    userHandle.getIdentifier(),
                    false /* force */);
            setDataRestoreError(RESTORE_ERROR_FETCHING_DATA, userHandle.getIdentifier());
        }
    }

    /** Deletes all the staged data and resets all the states. */
    public void deleteAndResetEverything(@NonNull UserHandle userHandle) {
        FilesUtil.deleteDir(getStagedRemoteDataDirectoryForUser(userHandle.getIdentifier()));
        setDataDownloadState(
                DATA_DOWNLOAD_STATE_UNKNOWN, userHandle.getIdentifier(), true /* force */);
        setInternalRestoreState(
                INTERNAL_RESTORE_STATE_UNKNOWN, userHandle.getIdentifier(), true /* force */);
        setDataRestoreError(RESTORE_ERROR_NONE, userHandle.getIdentifier());
    }

    /** Shares the {@link HealthConnectDataState} in the provided callback. */
    public @HealthConnectDataState.DataRestoreState int getDataRestoreState(int userId) {
        @HealthConnectDataState.DataRestoreState int dataRestoreState = RESTORE_STATE_IDLE;

        @InternalRestoreState int currentRestoreState = getInternalRestoreState(userId);

        if (currentRestoreState == INTERNAL_RESTORE_STATE_MERGING_DONE) {
            // already with correct values.
        } else if (currentRestoreState == INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS) {
            dataRestoreState = RESTORE_STATE_IN_PROGRESS;
        } else if (currentRestoreState != INTERNAL_RESTORE_STATE_UNKNOWN) {
            dataRestoreState = RESTORE_STATE_PENDING;
        }

        @DataDownloadState int currentDownloadState = getDataDownloadState(userId);
        if (currentDownloadState == DATA_DOWNLOAD_FAILED) {
            // already with correct values.
        } else if (currentDownloadState != DATA_DOWNLOAD_STATE_UNKNOWN) {
            dataRestoreState = RESTORE_STATE_PENDING;
        }

        return dataRestoreState;
    }

    /** Get the current data restore error. */
    public @HealthConnectDataState.DataRestoreError int getDataRestoreError(int userId) {
        // TODO(b/264070899) Get on a per user basis when we have per user db
        @HealthConnectDataState.DataRestoreError int dataRestoreError = RESTORE_ERROR_NONE;
        String restoreErrorOnDisk =
                PreferenceHelper.getInstance().getPreference(DATA_RESTORE_ERROR_KEY);
        try {
            dataRestoreError = Integer.parseInt(restoreErrorOnDisk);
        } catch (Exception e) {
            Slog.e(TAG, "Exception parsing restoreErrorOnDisk " + restoreErrorOnDisk, e);
        }
        return dataRestoreError;
    }

    /** Returns the file names of all the staged files. */
    @VisibleForTesting
    public Set<String> getStagedRemoteFileNames(int userId) {
        return Stream.of(getStagedRemoteDataDirectoryForUser(userId).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    /** Returns true if restore is in progress. API calls are blocked when this is true, */
    public boolean isRestoreInProgress(int userId) {
        return getInternalRestoreState(userId) == INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS;
    }

    void setInternalRestoreState(
            @InternalRestoreState int dataRestoreState, int userID, boolean force) {
        @InternalRestoreState int currentRestoreState = getInternalRestoreState(userID);
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
            // TODO(b/264070899) Store on a per user basis when we have per user db
            PreferenceHelper.getInstance()
                    .insertOrReplacePreference(
                            DATA_RESTORE_STATE_KEY, String.valueOf(dataRestoreState));
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    @InternalRestoreState
    int getInternalRestoreState(int userId) {
        mStatesLock.readLock().lock();
        try {
            // TODO(b/264070899) Get on a per user basis when we have per user db
            String restoreStateOnDisk =
                    PreferenceHelper.getInstance().getPreference(DATA_RESTORE_STATE_KEY);
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

    @DataDownloadState
    private int getDataDownloadState(int userId) {
        mStatesLock.readLock().lock();
        try {
            // TODO(b/264070899) Get on a per user basis when we have per user db
            String downloadStateOnDisk =
                    PreferenceHelper.getInstance().getPreference(DATA_DOWNLOAD_STATE_KEY);
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

    private void setDataDownloadState(
            @DataDownloadState int downloadState, int userId, boolean force) {
        mStatesLock.writeLock().lock();
        try {
            @DataDownloadState int currentDownloadState = getDataDownloadState(userId);
            if (!force
                    && (currentDownloadState == DATA_DOWNLOAD_FAILED
                            || currentDownloadState == DATA_DOWNLOAD_COMPLETE)) {
                Slog.w(TAG, "HC data download already in terminal state.");
                return;
            }
            // TODO(b/264070899) Store on a per user basis when we have per user db
            PreferenceHelper.getInstance()
                    .insertOrReplacePreference(
                            DATA_DOWNLOAD_STATE_KEY, String.valueOf(downloadState));
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    // Creating a separate single line method to keep this code close to the rest of the code that
    // uses PreferenceHelper to keep data on the disk.
    private void setDataRestoreError(
            @HealthConnectDataState.DataRestoreError int dataRestoreError, int userId) {
        // TODO(b/264070899) Store on a per user basis when we have per user db
        PreferenceHelper.getInstance()
                .insertOrReplacePreference(
                        DATA_RESTORE_ERROR_KEY, String.valueOf(dataRestoreError));
    }

    /**
     * Get the dir for the user with all the staged data - either from the cloud restore or from the
     * d2d process.
     */
    private File getStagedRemoteDataDirectoryForUser(int userId) {
        File hcDirectoryForUser = FilesUtil.getDataSystemCeHCDirectoryForUser(userId);
        return new File(hcDirectoryForUser, "remote_staged");
    }
}
