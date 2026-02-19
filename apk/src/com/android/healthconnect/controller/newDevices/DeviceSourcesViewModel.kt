/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.healthconnect.controller.newDevices

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.Record
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.matchmaking.api.IGetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.matchmaking.api.SetTrackingEnabledInput
import com.android.healthconnect.controller.matchmaking.api.SetTrackingEnabledUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.findSystemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceSourcesViewModel
@Inject
constructor(
    private val loadDeviceDataSourcesInfosUseCase: IGetDeviceDataSourcesInfoUseCase,
    private val setTrackingEnabledUseCase: SetTrackingEnabledUseCase,
) : ViewModel() {
    companion object {
        private const val TAG = "DeviceSourceViewModel"
    }

    private val _deviceSourcesInfos = MutableStateFlow<Set<DeviceDataSourceInfo>>(emptySet())
    private val _selectedDeviceSourceInfo = MutableStateFlow<DeviceDataSourceInfo?>(null)
    private val _isCurrentDeviceSynced = MutableStateFlow(false)

    private val _isLoading = MutableStateFlow(true)
    private val _hasError = MutableStateFlow(false)

    val deviceSourcesState: StateFlow<DeviceSourcesState> =
        combine(_deviceSourcesInfos, _isLoading, _hasError) { flows ->
                val deviceSourcesInfos = flows[0] as Set<DeviceDataSourceInfo>
                val loading = flows[1] as Boolean
                val error = flows[2] as Boolean

                if (error) {
                    DeviceSourcesState.Error
                } else if (loading) {
                    DeviceSourcesState.Loading
                } else {
                    // An empty source list implies an internal HC error, as the
                    // current device should always be included.
                    if (deviceSourcesInfos.isEmpty()) {
                        DeviceSourcesState.Error
                    } else {
                        DeviceSourcesState.WithData(deviceSourcesInfos)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DeviceSourcesState.Loading,
            )

    val selectedDeviceSourceInfoState: StateFlow<SelectedDeviceSourceInfoState> =
        combine(_selectedDeviceSourceInfo, _isLoading, _hasError) { flows ->
                val selectedInfo = flows[0] as DeviceDataSourceInfo?
                val loading = flows[1] as Boolean
                val error = flows[2] as Boolean

                if (error) {
                    SelectedDeviceSourceInfoState.Error
                } else if (loading) {
                    SelectedDeviceSourceInfoState.Loading
                } else {
                    if (selectedInfo == null) {
                        SelectedDeviceSourceInfoState.Error
                    } else {
                        SelectedDeviceSourceInfoState.WithData(selectedInfo)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SelectedDeviceSourceInfoState.Loading,
            )

    val isCurrentDeviceSynced: StateFlow<Boolean> =
        _isCurrentDeviceSynced.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    fun loadDeviceSourcesInfos() {
        viewModelScope.launch {
            _isLoading.value = true
            _hasError.value = false

            when (val result = loadDeviceDataSourcesInfosUseCase.invoke(Unit)) {
                is UseCaseResults.Failed -> {
                    _hasError.value = true
                    _isLoading.value = false
                    Log.e(TAG, "Failed to load device data sources infos")
                }
                is UseCaseResults.Success -> {
                    _hasError.value = false
                    _isLoading.value = false

                    _deviceSourcesInfos.value = result.data
                }
            }
        }
    }

    fun loadSelectedDeviceSourceInfo(devicePackageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _hasError.value = false

            when (val result = loadDeviceDataSourcesInfosUseCase.invoke(Unit)) {
                is UseCaseResults.Failed -> {
                    _hasError.value = true
                    _isLoading.value = false
                    Log.e(TAG, "Failed to load device data sources infos")
                }
                is UseCaseResults.Success -> {
                    val selectedDeviceSourceInfo =
                        result.data.find { it.deviceDataOrigin.packageName == devicePackageName }

                    if (selectedDeviceSourceInfo != null) {
                        _hasError.value = false
                        _isLoading.value = false
                        _selectedDeviceSourceInfo.value = selectedDeviceSourceInfo

                        loadIsCurrentDeviceSynced()
                    } else {
                        _hasError.value = true
                        _isLoading.value = false
                        _selectedDeviceSourceInfo.value = null
                        Log.e(TAG, "Failed to find device data source info with $devicePackageName")
                    }
                }
            }
        }
    }

    fun loadIsCurrentDeviceSynced() {
        val selectedDeviceSourceInfo = _selectedDeviceSourceInfo.value
        if (selectedDeviceSourceInfo == null || !selectedDeviceSourceInfo.isCurrentDevice) {
            return
        }

        viewModelScope.launch {
            val result = loadDeviceDataSourcesInfosUseCase.invoke(Unit)
            if (result is UseCaseResults.Success) {
                val currentSelectedDeviceSourceInfo =
                    result.data.find {
                        it.deviceDataOrigin.packageName ==
                            selectedDeviceSourceInfo.deviceDataOrigin.packageName
                    }

                val systemInfo = currentSelectedDeviceSourceInfo?.findSystemInfo()
                _isCurrentDeviceSynced.value =
                    systemInfo
                        ?.deviceDataTypeAdvertisements
                        ?.filter { it.isAvailable }
                        ?.all { it.isUserEnabled } ?: _isCurrentDeviceSynced.value
            }
        }
    }

    suspend fun setNativeTrackingEnabled(
        recordType: Class<out Record>,
        isEnabled: Boolean,
        shouldUpdateSyncedState: Boolean = true,
    ): Boolean {
        return when (
            setTrackingEnabledUseCase.invoke(SetTrackingEnabledInput(recordType, isEnabled))
        ) {
            is UseCaseResults.Failed -> {
                false
            }
            is UseCaseResults.Success -> {
                if (shouldUpdateSyncedState) {
                    loadIsCurrentDeviceSynced()
                }
                true
            }
        }
    }

    sealed class DeviceSourcesState {
        object Loading : DeviceSourcesState()

        object Error : DeviceSourcesState()

        data class WithData(val deviceSourcesInfos: Set<DeviceDataSourceInfo>) :
            DeviceSourcesState()
    }

    sealed class SelectedDeviceSourceInfoState {
        object Loading : SelectedDeviceSourceInfoState()

        object Error : SelectedDeviceSourceInfoState()

        data class WithData(val selectedDeviceSourceInfo: DeviceDataSourceInfo) :
            SelectedDeviceSourceInfoState()
    }
}
