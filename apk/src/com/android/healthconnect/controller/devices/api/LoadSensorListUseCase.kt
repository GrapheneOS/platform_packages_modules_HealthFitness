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

package com.android.healthconnect.controller.devices.api

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Loads the list of all available sensors on the current device. */
@Singleton
class LoadSensorListUseCase
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<Unit, List<Sensor>>(dispatcher), ILoadSensorListUseCase {
    override suspend fun execute(input: Unit): List<Sensor> {
        val manager: SensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return manager.getSensorList(Sensor.TYPE_ALL)
    }
}

interface ILoadSensorListUseCase : UseCaseContract<Unit, List<Sensor>>
