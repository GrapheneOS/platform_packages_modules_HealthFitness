/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.migration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.migration.api.LoadMigrationStateUseCase
import com.android.healthconnect.controller.migration.api.LoadMigrationTimeoutUseCase
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.postValueIfUpdated
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MigrationViewModel
@Inject
constructor(
    private val loadMigrationStateUseCase: LoadMigrationStateUseCase,
    private val loadMigrationTimeoutUseCase: LoadMigrationTimeoutUseCase
) : ViewModel() {

    private val _migrationState = MutableLiveData<@DataMigrationState Int>()
    val migrationState: LiveData<@DataMigrationState Int>
        get() = _migrationState

    private val _migrationTimeout = MutableLiveData<Duration>()
    val migrationTimeout: LiveData<Duration>
        get() = _migrationTimeout

    init {
        loadHealthConnectDataState()
    }

    fun loadHealthConnectDataState() {
        viewModelScope.launch {
            _migrationState.postValueIfUpdated(loadMigrationStateUseCase.invoke())
        }
    }

    fun loadTimeout(timeSource: TimeSource) {

        viewModelScope.launch {
            val timeout = Instant.ofEpochMilli(loadMigrationTimeoutUseCase.invoke())
            // calculate duration between now and timeout
            val now = Instant.ofEpochMilli(timeSource.currentTimeMillis())
            val duration = Duration.between(now, timeout)
            _migrationTimeout.postValue(duration)
        }
    }
}
