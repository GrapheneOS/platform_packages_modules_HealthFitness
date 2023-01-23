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

package com.android.health.connect.backuprestore;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import android.annotation.NonNull;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.health.connect.HealthConnectManager;
import android.health.connect.restore.StageRemoteDataException;
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * An intermediary to help with the transfer of HealthConnect data during device-to-device transfer.
 */
public class HealthConnectBackupAgent extends BackupAgent {
    private static final String TAG = "HealthConnectBackupAgent";
    private static final boolean DEBUG = false;

    private HealthConnectManager mHealthConnectManager;

    @Override
    public void onCreate() {
        if (DEBUG) {
            Slog.v(TAG, "onCreate()");
        }

        mHealthConnectManager = getHealthConnectService();
    }

    @Override
    public void onRestoreFinished() {
        Slog.v(TAG, "Staging all of HC data");
        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        File[] filesToTransfer = getD2dDir().listFiles();

        // We work with a flat dir structure where all files to be transferred are sitting in this
        // dir itself.
        for (var file : filesToTransfer) {
            try {
                pfdsByFileName.put(file.getName(), ParcelFileDescriptor.open(file, MODE_READ_ONLY));
            } catch (Exception e) {
                // this should never happen as we are reading files from our own dir on the disk.
                Slog.e(TAG, "Unable to open restored file from disk.", e);
            }
        }

        mHealthConnectManager.stageAllHealthConnectRemoteData(
                pfdsByFileName,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Void result) {
                        Slog.i(TAG, "D2D data successfully staged. Deleting all files.");
                        deleteD2dFiles();
                    }

                    @Override
                    public void onError(@NonNull StageRemoteDataException err) {
                        for (var fileNameToException : err.getExceptionsByFileNames().entrySet()) {
                            Slog.w(
                                    TAG,
                                    "Failed staging D2D file: "
                                            + fileNameToException.getKey()
                                            + " with error: "
                                            + fileNameToException.getValue());
                        }
                        deleteD2dFiles();
                    }
                });

        // close the FDs
        for (var pfdToFileName : pfdsByFileName.entrySet()) {
            try {
                pfdToFileName.getValue().close();
            } catch (IOException e) {
                Slog.e(TAG, "Unable to close restored file from disk.", e);
            }
        }
    }

    @Override
    public void onBackup(
            ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        // we don't do incremental backup / restore.
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) {
        // we don't do incremental backup / restore.
    }

    @VisibleForTesting
    File getD2dDir() {
        File d2dDir = new File(this.getFilesDir(), "d2d");
        d2dDir.mkdirs();
        return d2dDir;
    }

    @VisibleForTesting
    HealthConnectManager getHealthConnectService() {
        return this.getSystemService(HealthConnectManager.class);
    }

    @VisibleForTesting
    void deleteD2dFiles() {
        Slog.i(TAG, "Deleting all files.");
        File[] filesToTransfer = getD2dDir().listFiles();
        for (var file : filesToTransfer) {
            file.delete();
        }
    }
}
