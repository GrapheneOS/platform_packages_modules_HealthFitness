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

package com.android.healthconnect.controller.migration.api

import android.health.connect.HealthConnectDataState
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.migration.HealthConnectMigrationUiState
import android.os.OutcomeReceiver
import java.util.concurrent.Executor
import javax.inject.Inject

class HealthMigrationManagerImpl @Inject constructor(private val manager: HealthConnectManager) :
    HealthMigrationManager {

    override fun getHealthDataState(
        executor: Executor,
        callback: OutcomeReceiver<HealthConnectDataState, HealthConnectException>
    ) {
        manager.getHealthConnectDataState(executor, callback)
    }

    override fun getHealthConnectMigrationUiState(
        executor: Executor,
        callback: OutcomeReceiver<HealthConnectMigrationUiState, HealthConnectException>
    ) {
        manager.getHealthConnectMigrationUiState(executor, callback)
    }
}
