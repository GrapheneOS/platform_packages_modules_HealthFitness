/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.shared.usecase.UseCaseResults.Failed
import com.android.healthconnect.controller.shared.usecase.UseCaseResults.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** View model for {@link DataEntriesFragment} . */
@HiltViewModel
class DataEntriesFragmentViewModel
@Inject
constructor(
    private val loadDataEntriesUseCase: LoadDataEntriesUseCase,
    private val loadMenstruationDataUseCase: LoadMenstruationDataUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "DataEntriesFragmentView"
    }

    private val _dataEntries = MutableLiveData<DataEntriesFragmentState>()
    val dataEntries: LiveData<DataEntriesFragmentState>
        get() = _dataEntries

    fun loadData(permissionType: HealthPermissionType, selectedDate: Instant) {
        _dataEntries.postValue(DataEntriesFragmentState.Loading)
        viewModelScope.launch {
            val result =
                when (permissionType) {
                    // Special-casing Menstruation as it spans multiple days
                    HealthPermissionType.MENSTRUATION -> {
                        loadMenstruationDataUseCase.invoke(selectedDate)
                    }
                    else -> {
                        val input = LoadDataEntriesInput(permissionType, selectedDate)
                        loadDataEntriesUseCase.invoke(input)
                    }
                }
            when (result) {
                is Success -> {
                    _dataEntries.postValue(
                        if (result.data.isEmpty()) {
                            DataEntriesFragmentState.Empty
                        } else {
                            DataEntriesFragmentState.WithData(result.data)
                        })
                }
                is Failed -> {
                    Log.e(TAG, "Load error ", result.exception)
                    _dataEntries.postValue(DataEntriesFragmentState.LoadingFailed)
                }
            }
        }
    }

    sealed class DataEntriesFragmentState {
        object Loading : DataEntriesFragmentState()
        object Empty : DataEntriesFragmentState()
        object LoadingFailed : DataEntriesFragmentState()
        data class WithData(val entries: List<FormattedEntry>) : DataEntriesFragmentState()
    }
}
