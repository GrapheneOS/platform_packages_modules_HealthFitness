/**
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.viewmodels

import android.health.connect.datatypes.Device
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.DEVICE_DATA_SOURCES

class AdvertiseDevicesViewModel : ViewModel() {

    data class DataTypeConfig(
        var advertisedDataType: Class<out Record>,
        var isAvailable: Boolean = true,
        var isUserEnabled: Boolean = false,
        var isVisibleByDefaultInMatchmaking: Boolean = true,
        var symptomType: Int = 0, // SymptomRecord.SYMPTOM_TYPE_UNKNOWN
    )

    data class DeviceAdvertisementConfig(
        var manufacturer: String?,
        var model: String?,
        var type: Int,
        var displayName: String?,
        var deviceId: String,
        var advertisedDataTypes: MutableList<DataTypeConfig>,
        var isEnabled: Boolean = true,
    )

    private val _deviceConfigs =
        MutableLiveData<MutableList<DeviceAdvertisementConfig>>(
            DEVICE_DATA_SOURCES.map { source ->
                    DeviceAdvertisementConfig(
                        manufacturer = source.device.manufacturer,
                        model = source.device.model,
                        type = source.device.type,
                        displayName = source.device.displayName,
                        deviceId = source.deviceId,
                        advertisedDataTypes =
                            mutableListOf(
                                DataTypeConfig(advertisedDataType = source.advertisedDataType)
                            ),
                    )
                }
                .toMutableList()
        )
    val deviceConfigs: LiveData<MutableList<DeviceAdvertisementConfig>> = _deviceConfigs

    private val _selectedDeviceDataSourceInfo =
        MutableLiveData<android.health.connect.DeviceDataSourceInfo?>(null)
    val selectedDeviceDataSourceInfo: LiveData<android.health.connect.DeviceDataSourceInfo?> =
        _selectedDeviceDataSourceInfo

    fun setSelectedDeviceDataSourceInfo(info: android.health.connect.DeviceDataSourceInfo?) {
        _selectedDeviceDataSourceInfo.value = info
    }

    private var _isReAdvertiseMode: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val isReAdvertiseMode: LiveData<Boolean> = _isReAdvertiseMode

    fun initializeReAdvertiseMode(newConfigs: List<DeviceAdvertisementConfig>) {
        _deviceConfigs.value = newConfigs.toMutableList()
        _isReAdvertiseMode.value = true
    }

    fun addDevice() {
        val newList = _deviceConfigs.value ?: mutableListOf()
        newList.add(
            DeviceAdvertisementConfig(
                manufacturer = "Manufacturer",
                model = "Model",
                type = Device.DEVICE_TYPE_WATCH,
                displayName = "Display Name",
                deviceId = "device_id_${System.currentTimeMillis()}",
                advertisedDataTypes =
                    mutableListOf(DataTypeConfig(advertisedDataType = StepsRecord::class.java)),
            )
        )
        _deviceConfigs.value = newList
    }

    fun removeDevice(index: Int) {
        val newList = _deviceConfigs.value ?: return
        if (index in newList.indices) {
            newList.removeAt(index)
            _deviceConfigs.value = newList
        }
    }

    fun updateDevice(index: Int, config: DeviceAdvertisementConfig) {
        val newList = _deviceConfigs.value ?: return
        if (index in newList.indices) {
            newList[index] = config
            _deviceConfigs.value = newList
        }
    }

    fun addDataType(deviceIndex: Int) {
        val configs = _deviceConfigs.value ?: return
        if (deviceIndex in configs.indices) {
            configs[deviceIndex]
                .advertisedDataTypes
                .add(DataTypeConfig(advertisedDataType = StepsRecord::class.java))
            _deviceConfigs.value = configs
        }
    }

    fun removeDataType(deviceIndex: Int, dataTypeIndex: Int) {
        val configs = _deviceConfigs.value ?: return
        if (deviceIndex in configs.indices) {
            val dataTypes = configs[deviceIndex].advertisedDataTypes
            if (dataTypeIndex in dataTypes.indices) {
                dataTypes.removeAt(dataTypeIndex)
                _deviceConfigs.value = configs
            }
        }
    }
}
