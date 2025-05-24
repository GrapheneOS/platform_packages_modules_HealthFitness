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

package com.android.healthconnect.controller.tests.exportimport.api

import android.health.connect.HealthConnectException
import android.health.connect.exportimport.ScheduledExportSettings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportImportUseCaseResult
import com.android.healthconnect.controller.exportimport.api.HealthDataExportManager
import com.android.healthconnect.controller.exportimport.api.UpdateExportSettingsUseCase
import com.android.healthconnect.controller.service.HealthDataExportManagerModule
import com.android.healthconnect.controller.tests.utils.di.FakeHealthDataExportManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@UninstallModules(HealthDataExportManagerModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UpdateExportSettingsUseCaseTest {

    @BindValue val healthDataExportManager: HealthDataExportManager = FakeHealthDataExportManager()

    private lateinit var useCase: UpdateExportSettingsUseCase

    @Before
    fun setup() {
        useCase = UpdateExportSettingsUseCase(healthDataExportManager)
    }

    @After
    fun teardown() {
        (healthDataExportManager as FakeHealthDataExportManager).reset()
    }

    @Test
    fun invoke_callsHealthDataExportManager() = runTest {
        val settings =
            ScheduledExportSettings.Builder()
                .setPeriodInDays(ExportFrequency.EXPORT_FREQUENCY_DAILY.periodInDays)
                .build()

        val result = useCase.invoke(settings)

        assertThat(result is ExportImportUseCaseResult.Success).isTrue()
        assertThat(healthDataExportManager.getScheduledExportPeriodInDays())
            .isEqualTo(ExportFrequency.EXPORT_FREQUENCY_DAILY.periodInDays)
    }

    @Test
    fun invoke_callsHealthDataExportManager_returnsFailure() = runTest {
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
        (healthDataExportManager as FakeHealthDataExportManager)
            .setConfigureScheduledExportException(exception)

        val settings =
            ScheduledExportSettings.Builder()
                .setPeriodInDays(ExportFrequency.EXPORT_FREQUENCY_DAILY.periodInDays)
                .build()
        val result = useCase.invoke(settings)

        assertThat(result is ExportImportUseCaseResult.Failed).isTrue()
        assertThat((result as ExportImportUseCaseResult.Failed).exception is HealthConnectException)
            .isTrue()
        assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }
}
