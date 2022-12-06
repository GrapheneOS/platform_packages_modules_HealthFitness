/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.deletion

import android.healthconnect.TimeRangeFilter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.deletion.api.DeleteAllDataUseCase
import com.android.healthconnect.controller.deletion.api.DeleteAppDataUseCase
import com.android.healthconnect.controller.deletion.api.DeleteCategoryUseCase
import com.android.healthconnect.controller.deletion.api.DeleteEntryUseCase
import com.android.healthconnect.controller.deletion.api.DeletePermissionTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DeletionViewModel
@Inject
constructor(
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val deletePermissionTypeUseCase: DeletePermissionTypeUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val deleteAppDataUseCase: DeleteAppDataUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "DeletionViewModel"
    }

    private val _deletionParameters = MutableLiveData(DeletionParameters())

    val deletionParameters: LiveData<DeletionParameters>
        get() = _deletionParameters

    val showTimeRangeDialogFragment: Boolean
        get() = currentDeletionParameters().showTimeRangePickerDialog

    private fun currentDeletionParameters() = _deletionParameters.value!!

    fun setDeletionType(deletionType: DeletionType) {
        val showTimeRangePickerDialog =
            when (deletionType) {
                is DeletionType.DeleteDataEntry -> false
                // TODO (teog) cover other flows
                else -> true
            }
        _deletionParameters.value =
            currentDeletionParameters()
                .copy(
                    showTimeRangePickerDialog = showTimeRangePickerDialog,
                    deletionType = deletionType)
    }

    fun setChosenRange(chosenRange: ChosenRange) {
        _deletionParameters.value = _deletionParameters.value?.copy(chosenRange = chosenRange)
    }

    fun setEndTime(endTime: Instant) {
        _deletionParameters.value =
            _deletionParameters.value?.copy(endTimeMs = endTime.toEpochMilli())
    }

    private fun setDeletionState(newState: DeletionState) {
        _deletionParameters.value = currentDeletionParameters().copy(deletionState = newState)
    }

    fun delete() {

        viewModelScope.launch {
            setDeletionState(DeletionState.STATE_DELETION_STARTED)

            try {
                setDeletionState(DeletionState.STATE_PROGRESS_INDICATOR_STARTED)

                val timeRangeFilter =
                    TimeRangeFilter.Builder(
                            currentDeletionParameters().getStartTimeInstant(),
                            currentDeletionParameters().getEndTimeInstant())
                        .build()

                when (val deletionType = currentDeletionParameters().deletionType) {
                    is DeletionType.DeleteDataEntry -> {
                        _deletionParameters.value?.let { deleteEntryUseCase.invoke(deletionType) }
                    }
                    is DeletionType.DeletionTypeAllData -> {
                        _deletionParameters.value?.let {
                            deleteAllDataUseCase.invoke(timeRangeFilter)
                        }
                    }
                    is DeletionType.DeletionTypeCategoryData -> {
                        deletionParameters.value?.let {
                            deleteCategoryUseCase.invoke(deletionType, timeRangeFilter)
                        }
                    }
                    is DeletionType.DeletionTypeHealthPermissionTypeData -> {
                        deletionParameters.value?.let {
                            deletePermissionTypeUseCase.invoke(deletionType, timeRangeFilter)
                        }
                    }
                    is DeletionType.DeletionTypeAppData -> {
                        deletionParameters.value?.let {
                            deleteAppDataUseCase.invoke(deletionType, timeRangeFilter)
                        }
                    }
                    else -> {
                        // TODO other deletion flows
                    }
                }
                setDeletionState(DeletionState.STATE_DELETION_SUCCESSFUL)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to delete data ${currentDeletionParameters()}", error)
                setDeletionState(DeletionState.STATE_DELETION_FAILED)
            } finally {
                setDeletionState(DeletionState.STATE_PROGRESS_INDICATOR_CAN_END)
            }
        }
    }
}
