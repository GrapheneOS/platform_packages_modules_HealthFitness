/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.healthconnect.controller.exportimport.api

import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class LoadExportSettingsUseCase
@Inject
constructor(
    private val healthDataExportManager: HealthDataExportManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<Unit, ExportFrequency>(dispatcher) {
    companion object {
        private const val TAG = "LoadExportSettingsUseCase"
    }

    /** Returns the stored export settings. */
    override suspend fun execute(input: Unit): ExportFrequency {
        val periodInDays = healthDataExportManager.getScheduledExportPeriodInDays()
        val frequency = fromPeriodInDays(periodInDays)
        return frequency
    }
}
