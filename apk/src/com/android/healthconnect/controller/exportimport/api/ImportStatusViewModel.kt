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

package com.android.healthconnect.controller.exportimport.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for import status. */
@HiltViewModel
class ImportStatusViewModel
@Inject
constructor(private val loadImportStatusUseCase: ILoadImportStatusUseCase) : ViewModel() {
    private val _storedImportStatus = MutableLiveData<ImportUiStatus>()

    /** Holds the import status that is stored in the Health Connect service. */
    val storedImportStatus: LiveData<ImportUiStatus>
        get() = _storedImportStatus

    init {
        loadImportStatus()
    }

    /** Triggers a load of import status. */
    fun loadImportStatus() {
        _storedImportStatus.postValue(ImportUiStatus.Loading)
        viewModelScope.launch {
            when (val result = loadImportStatusUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    _storedImportStatus.postValue(ImportUiStatus.WithData(result.data))
                }
                is UseCaseResults.Failed -> {
                    _storedImportStatus.postValue(ImportUiStatus.LoadingFailed)
                }
            }
        }
    }
}
