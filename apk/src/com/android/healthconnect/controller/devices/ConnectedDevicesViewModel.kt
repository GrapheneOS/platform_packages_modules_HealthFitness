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
package com.android.healthconnect.controller.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectedDevicesViewModel
@Inject
constructor(
    private val loadDeviceDataSourcesUseCase: ILoadDeviceDataSources,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _connectedDevicesState = MutableLiveData<ConnectedDevicesState>()
    val connectedDevicesState: LiveData<ConnectedDevicesState>
        get() = _connectedDevicesState

    private val _selectedDevice = MutableLiveData<DeviceDataSource>()
    val selectedDevice: LiveData<DeviceDataSource>
        get() = _selectedDevice

    fun setSelectedDevice(device: DeviceDataSource) {
        _selectedDevice.postValue(device)
    }

    fun loadDeviceDataSources() {
        _connectedDevicesState.postValue(ConnectedDevicesState.Loading)
        viewModelScope.launch(ioDispatcher) {
            when (val result = loadDeviceDataSourcesUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    _connectedDevicesState.postValue(ConnectedDevicesState.Success(result.data))
                }
                is UseCaseResults.Failed -> {
                    _connectedDevicesState.postValue(ConnectedDevicesState.Error)
                }
            }
        }
    }

    sealed class ConnectedDevicesState {
        object Loading : ConnectedDevicesState()

        data class Success(val deviceDataSources: List<DeviceDataSource>) : ConnectedDevicesState()

        object Error : ConnectedDevicesState()
    }
}
