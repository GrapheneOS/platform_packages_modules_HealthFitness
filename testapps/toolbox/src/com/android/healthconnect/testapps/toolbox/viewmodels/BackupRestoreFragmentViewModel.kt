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

package com.android.healthconnect.testapps.toolbox.viewmodels

import android.health.connect.HealthConnectManager
import android.health.connect.backuprestore.GetChangesForBackupResponse
import android.health.connect.backuprestore.GetLatestMetadataForBackupResponse
import android.health.connect.backuprestore.RestoreChange
import androidx.core.os.asOutcomeReceiver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class BackupRestoreFragmentViewModel : ViewModel() {

    companion object {
        private const val TAG = "BackupRestoreFragmentViewModel"
    }

    private val _backupResponse = MutableLiveData<GetChangesForBackupResponse>()
    private val _backupMetadataResponse = MutableLiveData<GetLatestMetadataForBackupResponse>()
    val backupResponse: LiveData<GetChangesForBackupResponse>
        get() = _backupResponse

    val backupMetadataResponse: LiveData<GetLatestMetadataForBackupResponse>
        get() = _backupMetadataResponse

    fun storeBackupResponse(manager: HealthConnectManager, callback: (message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val response =
                    suspendCancellableCoroutine<GetChangesForBackupResponse> { continuation ->
                        manager.getChangesForBackup(
                            null,
                            Runnable::run,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                _backupResponse.postValue(response)
                callback("Backup is successful")
            } catch (ex: Exception) {
                callback(ex.toString())
            }
        }
    }

    fun restore(manager: HealthConnectManager, callback: (message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val previousResponse = backupResponse.value
                if (previousResponse != null) {
                    val restoreChanges =
                        previousResponse.changes
                            .stream()
                            .map { change -> RestoreChange(change.data!!) }
                            .toList()
                    suspendCancellableCoroutine<Void> { continuation ->
                        manager.restoreChanges(
                            restoreChanges,
                            Runnable::run,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                callback("Restore is successful")
            } catch (ex: Exception) {
                callback(ex.toString())
            }
        }
    }

    fun storeBackupMetadataResponse(
        manager: HealthConnectManager,
        callback: (message: String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val response =
                    suspendCancellableCoroutine<GetLatestMetadataForBackupResponse> { continuation
                        ->
                        manager.getLatestMetadataForBackup(
                            Runnable::run,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                _backupMetadataResponse.postValue(response)
                callback("Metadata backup is successful")
            } catch (ex: Exception) {
                callback(ex.toString())
            }
        }
    }

    fun restoreMetadata(manager: HealthConnectManager, callback: (message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val previousResponse = backupMetadataResponse.value
                if (previousResponse != null) {
                    suspendCancellableCoroutine<Void> { continuation ->
                        manager.restoreLatestMetadata(
                            previousResponse.metadata,
                            Runnable::run,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                }
                callback("Metadata restore is successful")
            } catch (ex: Exception) {
                callback(ex.toString())
            }
        }
    }
}
