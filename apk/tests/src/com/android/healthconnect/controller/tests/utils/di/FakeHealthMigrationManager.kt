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

package com.android.healthconnect.controller.tests.utils.di

import android.health.connect.HealthConnectDataState
import android.health.connect.HealthConnectException
import android.health.connect.migration.HealthConnectMigrationUiState
import android.os.OutcomeReceiver
import com.android.healthconnect.controller.migration.api.HealthMigrationManager
import java.util.concurrent.Executor

class FakeHealthMigrationManager : HealthMigrationManager {

    private var state: Int = HealthConnectMigrationUiState.MIGRATION_UI_STATE_IDLE
    private var dataRestoreError: Int = HealthConnectDataState.RESTORE_ERROR_NONE
    private var dataMigrationState: Int = HealthConnectDataState.MIGRATION_STATE_IDLE

    fun setMigrationState(state: Int) {
        this.state = state
    }

    fun setDataMigrationState(error: Int, migrationState: Int) {
        this.dataMigrationState = migrationState
        this.dataRestoreError = error
    }

    override fun getHealthDataState(
        executor: Executor,
        callback: OutcomeReceiver<HealthConnectDataState, HealthConnectException>
    ) {
        callback.onResult(
            HealthConnectDataState(
                HealthConnectDataState.RESTORE_STATE_IN_PROGRESS,
                dataRestoreError,
                dataMigrationState))
    }

    override fun getHealthConnectMigrationUiState(
        executor: Executor,
        callback: OutcomeReceiver<HealthConnectMigrationUiState, HealthConnectException>
    ) {
        callback.onResult(HealthConnectMigrationUiState(state))
    }
}
