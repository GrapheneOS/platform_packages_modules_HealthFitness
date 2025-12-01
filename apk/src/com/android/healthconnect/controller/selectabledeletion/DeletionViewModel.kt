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
package com.android.healthconnect.controller.selectabledeletion

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteAllSymptomsDataFromInactiveApp
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteAppData
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteEntries
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteEntriesFromApp
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteHealthPermissionTypes
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteHealthPermissionTypesFromApp
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteInactiveAppData
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllSymptomsDataFromInactiveAppUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAppDataUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteEntriesUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeletePermissionTypesFromAppUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeletePermissionTypesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class DeletionViewModel
@Inject
constructor(
    private val deleteAppDataUseCase: DeleteAppDataUseCase,
    private val deletePermissionTypesUseCase: DeletePermissionTypesUseCase,
    private val deleteEntriesUseCase: DeleteEntriesUseCase,
    private val deletePermissionTypesFromAppUseCase: DeletePermissionTypesFromAppUseCase,
    private val deleteAllSymptomsDataFromInactiveAppUseCase:
        DeleteAllSymptomsDataFromInactiveAppUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "DeletionViewModel"
    }

    // Artificial delay for reloads, to give enough time for the deletion task to end
    private val defaultDelay = 2000L

    private lateinit var deletionType: DeletionType

    private var _permissionTypesReloadNeeded = MutableLiveData(false)

    private var _appPermissionTypesReloadNeeded = MutableLiveData(false)

    private var _entriesReloadNeeded = MutableLiveData(false)

    private var _appEntriesReloadNeeded = MutableLiveData(false)

    private var _connectedAppsReloadNeeded = MutableLiveData(false)

    private var _inactiveAppsReloadNeeded = MutableLiveData(false)

    private var _deletionProgress = MutableLiveData(DeletionProgress.NOT_STARTED)

    val deletionProgress: LiveData<DeletionProgress>
        get() = _deletionProgress

    val permissionTypesReloadNeeded: LiveData<Boolean>
        get() = _permissionTypesReloadNeeded

    val entriesReloadNeeded: LiveData<Boolean>
        get() = _entriesReloadNeeded

    val appPermissionTypesReloadNeeded: LiveData<Boolean>
        get() = _appPermissionTypesReloadNeeded

    val appEntriesReloadNeeded: LiveData<Boolean>
        get() = _appEntriesReloadNeeded

    val connectedAppsReloadNeeded: LiveData<Boolean>
        get() = _connectedAppsReloadNeeded

    val inactiveAppsReloadNeeded: LiveData<Boolean>
        get() = _inactiveAppsReloadNeeded

    var removePermissions = false

    fun delete() {
        viewModelScope.launch {
            _deletionProgress.postValue(DeletionProgress.STARTED)
            val currentDeletionType = deletionType
            try {
                _deletionProgress.postValue(DeletionProgress.PROGRESS_INDICATOR_CAN_START)

                when (currentDeletionType) {
                    is DeleteHealthPermissionTypes -> {
                        deletePermissionTypesUseCase.invoke(currentDeletionType)
                        delay(defaultDelay)
                        _permissionTypesReloadNeeded.postValue(true)
                    }

                    is DeleteEntries -> {
                        deleteEntriesUseCase.invoke(currentDeletionType)
                        delay(defaultDelay)
                        _entriesReloadNeeded.postValue(true)
                    }

                    is DeleteHealthPermissionTypesFromApp -> {
                        deletePermissionTypesFromAppUseCase.invoke(
                            currentDeletionType.packageName,
                            currentDeletionType.healthPermissionTypes,
                            removePermissions,
                        )
                        delay(defaultDelay)
                        _appPermissionTypesReloadNeeded.postValue(true)
                    }

                    is DeleteEntriesFromApp -> {
                        deleteEntriesUseCase.invoke(currentDeletionType.toDeleteEntries())
                        delay(defaultDelay)
                        _appEntriesReloadNeeded.postValue(true)
                    }

                    is DeleteAppData -> {
                        deleteAppDataUseCase.invoke(currentDeletionType)
                        delay(defaultDelay)
                        _connectedAppsReloadNeeded.postValue(true)
                    }

                    is DeleteInactiveAppData -> {
                        deletePermissionTypesFromAppUseCase.invoke(
                            currentDeletionType.packageName,
                            setOf(currentDeletionType.healthPermissionType),
                        )
                        delay(defaultDelay)
                        _inactiveAppsReloadNeeded.postValue(true)
                    }

                    is DeleteAllSymptomsDataFromInactiveApp -> {
                        deleteAllSymptomsDataFromInactiveAppUseCase.invoke(currentDeletionType)
                        delay(defaultDelay)
                        _inactiveAppsReloadNeeded.postValue(true)
                    }
                }
                _deletionProgress.postValue(DeletionProgress.COMPLETED)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to delete data", error)
                _deletionProgress.postValue(DeletionProgress.FAILED)
            } finally {
                // delay to ensure that the success/failed dialog has been shown
                delay(1000)
                _deletionProgress.postValue(DeletionProgress.PROGRESS_INDICATOR_CAN_END)
            }
        }
    }

    fun resetInactiveAppsReloadNeeded() {
        _inactiveAppsReloadNeeded.postValue(false)
    }

    fun resetPermissionTypesReloadNeeded() {
        _permissionTypesReloadNeeded.postValue(false)
    }

    fun resetConnectedAppsReloadNeeded() {
        _connectedAppsReloadNeeded.postValue(false)
    }

    fun resetEntriesReloadNeeded() {
        _entriesReloadNeeded.postValue(false)
    }

    fun setDeletionType(deletionType: DeletionType) {
        this.deletionType = deletionType
    }

    fun resetAppPermissionTypesReloadNeeded() {
        _appPermissionTypesReloadNeeded.postValue(false)
    }

    fun resetAppEntriesReloadNeeded() {
        _appEntriesReloadNeeded.postValue(false)
    }

    fun getDeletionType(): DeletionType {
        return deletionType
    }

    enum class DeletionProgress {
        NOT_STARTED,
        STARTED,
        PROGRESS_INDICATOR_CAN_START,
        PROGRESS_INDICATOR_CAN_END,
        COMPLETED,
        FAILED,
    }
}
