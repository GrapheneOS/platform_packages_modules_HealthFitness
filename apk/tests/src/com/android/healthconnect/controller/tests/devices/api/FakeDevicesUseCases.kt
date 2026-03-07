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
package com.android.healthconnect.controller.tests.devices.api

import android.hardware.Sensor
import android.health.connect.DeviceDataSourceInfo
import com.android.healthconnect.controller.devices.DeviceDataSource
import com.android.healthconnect.controller.devices.api.IGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.devices.api.ILoadDeviceDataSourcesUseCase
import com.android.healthconnect.controller.devices.api.ILoadSensorListUseCase
import com.android.healthconnect.controller.devices.api.ISetTrackingEnabledUseCase
import com.android.healthconnect.controller.devices.api.SetTrackingEnabledUseCase
import com.android.healthconnect.controller.matchmaking.api.IGetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeLoadDeviceDataSourcesUseCase :
    FakeUseCase<Unit, List<DeviceDataSource>>(dispatcher = Dispatchers.Unconfined),
    ILoadDeviceDataSourcesUseCase {
    private var list: List<DeviceDataSource> = emptyList()

    fun updateList(list: List<DeviceDataSource>) {
        this.list = list
    }

    override suspend fun successValue(input: Unit): List<DeviceDataSource> {
        return list
    }

    override fun reset() {
        super.reset()
        this.list = emptyList()
    }
}

class FakeLoadSensorListUseCase :
    FakeUseCase<Unit, List<Sensor>>(dispatcher = Dispatchers.Unconfined), ILoadSensorListUseCase {
    private var sensors: List<Sensor> = emptyList()

    override suspend fun successValue(input: Unit): List<Sensor> {
        return sensors
    }

    fun updateSensors(sensors: List<Sensor>) {
        this.sensors = sensors
    }

    override fun reset() {
        super.reset()
        this.sensors = emptyList()
    }
}

class FakeSetTrackingEnabledUseCase :
    FakeUseCase<SetTrackingEnabledUseCase.Input, Unit>(dispatcher = Dispatchers.Unconfined),
    ISetTrackingEnabledUseCase {
    var latestInput: SetTrackingEnabledUseCase.Input? = null

    override suspend fun successValue(input: SetTrackingEnabledUseCase.Input) {
        latestInput = input
    }

    override fun reset() {
        super.reset()
        latestInput = null
    }
}

class FakeGetDeviceDataSourcesInfoUseCase :
    FakeUseCase<Unit, Set<DeviceDataSourceInfo>>(dispatcher = Dispatchers.Unconfined),
    IGetDeviceDataSourcesInfoUseCase {
    private var deviceSources: Set<DeviceDataSourceInfo> = emptySet()

    fun updateSet(deviceSourcesSet: Set<DeviceDataSourceInfo>) {
        deviceSources = deviceSourcesSet
    }

    override suspend fun successValue(input: Unit): Set<DeviceDataSourceInfo> {
        return deviceSources
    }

    override fun reset() {
        super.reset()
        deviceSources = emptySet()
    }
}

class FakeGetCurrentDeviceIdUseCase :
    FakeUseCase<Unit, String>(dispatcher = Dispatchers.Unconfined), IGetCurrentDeviceIdUseCase {
    private var deviceId: String = "test_device_id"

    fun updateDeviceId(id: String) {
        deviceId = id
    }

    override suspend fun successValue(input: Unit): String {
        return deviceId
    }

    override fun reset() {
        super.reset()
        deviceId = "test_device_id"
    }

    override suspend fun isCurrentDevice(packageName: String?): Boolean {
        return deviceId == packageName
    }

    override suspend fun getOrNull(): String? {
        return deviceId
    }
}
