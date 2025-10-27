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

import android.net.Uri
import android.util.Slog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for Import flow fragments. */
@HiltViewModel
class ImportFlowViewModel
@Inject
constructor(private val triggerImportUseCase: BaseUseCase<Uri, Unit>) : ViewModel() {

    private val _lastImportCompletionInstant = MutableLiveData<Instant?>()

    companion object {
        const val TAG = "ImportFlowViewModel"
    }

    val lastImportCompletionInstant: LiveData<Instant?>
        get() = _lastImportCompletionInstant

    fun setLastCompletionInstant(newCompletionInstant: Instant?) {
        _lastImportCompletionInstant.postValue(newCompletionInstant)
    }

    fun triggerImportOfSelectedFile(uri: Uri) {
        Slog.i(TAG, "$uri")
        viewModelScope.launch {
            when (triggerImportUseCase.invoke(uri)) {
                is UseCaseResults.Success -> {
                    Slog.i(TAG, "import succeeded")
                    // TODO(b/356652714): Change to use TimeSource instead
                    setLastCompletionInstant(Instant.now())
                }
                is UseCaseResults.Failed -> {
                    Slog.i(TAG, "import failed")
                }
            }
        }
    }
}
