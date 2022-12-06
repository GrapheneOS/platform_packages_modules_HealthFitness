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
package com.android.healthconnect.controller.permissiontypes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HealthPermissionTypesViewModel
@Inject
constructor(
    private val loadPermissionTypesUseCase: LoadPermissionTypesUseCase,
) : ViewModel() {

    private val _permissionTypesData = MutableLiveData<PermissionTypesFragmentState>()

    /** Provides a list of [HealthPermissionType]s displayed in [HealthPermissionTypesFragment]. */
    val permissionTypesData: LiveData<PermissionTypesFragmentState>
        get() = _permissionTypesData

    fun loadData(category: HealthDataCategory) {
        _permissionTypesData.postValue(PermissionTypesFragmentState.Loading)
        viewModelScope.launch {
            val permissionTypes = loadPermissionTypesUseCase.invoke(category)
            _permissionTypesData.postValue(PermissionTypesFragmentState.WithData(permissionTypes))
        }
    }

    sealed class PermissionTypesFragmentState {
        object Loading : PermissionTypesFragmentState()
        data class WithData(val permissionTypes: List<HealthPermissionType>) :
            PermissionTypesFragmentState()
    }
}
