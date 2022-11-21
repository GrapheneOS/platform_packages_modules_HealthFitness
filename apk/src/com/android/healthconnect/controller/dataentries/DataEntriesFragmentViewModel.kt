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
package com.android.healthconnect.controller.dataentries

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link DataEntriesFragment} . */
@HiltViewModel
class DataEntriesFragmentViewModel
@Inject
constructor(private val loadDataEntriesUseCase: LoadDataEntriesUseCase) : ViewModel() {

    companion object {
        private const val TAG = "DataEntriesFragmentView"
    }

    private val _dataEntries = MutableLiveData<List<FormattedDataEntry>>()
    val dataEntries: LiveData<List<FormattedDataEntry>>
        get() = _dataEntries

    fun loadData(permissionType: HealthPermissionType, selectedDate: Instant) {
        viewModelScope.launch {
            _dataEntries.value = loadDataEntriesUseCase.invoke(permissionType, selectedDate)
        }
    }
}
