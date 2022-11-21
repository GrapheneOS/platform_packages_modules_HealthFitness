/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.autodelete

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for [AutoDeleteFragment]. */
@HiltViewModel
class AutoDeleteViewModel
@Inject
constructor(private val loadAutoDeleteUseCase: LoadAutoDeleteUseCase) : ViewModel() {

    private val _autoDeleteRange = MutableLiveData<AutoDeleteRange>()

    val autoDeleteRange: LiveData<AutoDeleteRange>
        get() = _autoDeleteRange

    init {
        loadAutoDeleteRange()
    }

    private fun loadAutoDeleteRange() {
        viewModelScope.launch { _autoDeleteRange.postValue(loadAutoDeleteUseCase.invoke()) }
    }
}
