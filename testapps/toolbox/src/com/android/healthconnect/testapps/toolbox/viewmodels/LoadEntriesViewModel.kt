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

import android.app.Application
import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.Record
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.healthconnect.testapps.toolbox.read.controller.DataEntriesLoader
import com.android.healthconnect.testapps.toolbox.read.controller.LoadDataEntries
import com.android.healthconnect.testapps.toolbox.read.controller.LoadEntriesInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoadEntriesViewModel(private val loadDataEntries: DataEntriesLoader) : ViewModel() {

    private val _entriesState = MutableStateFlow<DataState>(DataState.Loading)
    val entriesState: StateFlow<DataState> = _entriesState

    fun loadEntries(input: LoadEntriesInput) {

        viewModelScope.launch {
            try {
                val response = loadDataEntries.load(input)
                _entriesState.value = DataState.Success(response)
            } catch (e: Exception) {
                _entriesState.value = DataState.Error(e)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val healthConnectManager =
                    application.getSystemService(Context.HEALTHCONNECT_SERVICE)
                        as HealthConnectManager
                LoadEntriesViewModel(loadDataEntries = LoadDataEntries(healthConnectManager))
            }
        }
    }
}

sealed class DataState {
    data class Success(val records: List<Record>) : DataState()

    data class Error(val exception: java.lang.Exception) : DataState()

    data object Loading : DataState()
}
