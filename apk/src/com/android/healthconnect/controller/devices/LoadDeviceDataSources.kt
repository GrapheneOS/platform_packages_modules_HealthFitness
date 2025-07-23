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

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.provider.Settings
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class LoadDeviceDataSources
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectManager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadDeviceDataSources, BaseUseCase<Unit, List<DeviceDataSource>>(dispatcher) {
    override suspend fun execute(input: Unit): List<DeviceDataSource> {
        // TODO(b/421131223): Fetch the actual device data sources from the service.
        val currentDeviceName =
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?: context.getString(R.string.devices_unknown_device)
        val recordTypes: List<Class<out Record>> = listOf(StepsRecord::class.java)
        val trackingEnabledMap = healthConnectManager.isTrackingEnabled(recordTypes)
        val trackerStatus = recordTypes.associateWith { trackingEnabledMap.getOrDefault(it, false) }
        return listOf(
            DeviceDataSource(
                deviceName = currentDeviceName,
                isCurrentDevice = true,
                trackerStatus = trackerStatus,
            )
        )
    }
}

interface ILoadDeviceDataSources {
    suspend fun invoke(input: Unit): UseCaseResults<List<DeviceDataSource>>

    suspend fun execute(input: Unit): List<DeviceDataSource>
}
