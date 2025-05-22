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
import android.health.connect.exportimport.ScheduledExportStatus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.ExportImportUseCaseResult
import com.android.healthconnect.controller.exportimport.api.HealthDataExportManager
import com.android.healthconnect.controller.exportimport.api.LoadScheduledExportStatusUseCase
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.service.HealthDataExportManagerModule
import com.android.healthconnect.controller.tests.utils.di.FakeHealthDataExportManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@UninstallModules(HealthDataExportManagerModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadScheduledExportStatusUseCaseTest {

    @BindValue val healthDataExportManager: HealthDataExportManager = FakeHealthDataExportManager()
    private val fakeHealthDataExportManager = healthDataExportManager as FakeHealthDataExportManager

    private lateinit var useCase: LoadScheduledExportStatusUseCase

    @Before
    fun setup() {
        useCase = LoadScheduledExportStatusUseCase(healthDataExportManager)
    }

    @After
    fun teardown() {
        fakeHealthDataExportManager.reset()
    }

    @Test
    fun invoke_callsHealthDataExportManagerSuccessfully() = runTest {
        val scheduledExportStatus =
            ScheduledExportStatus.Builder()
                .setLastSuccessfulExportTime(Instant.ofEpochMilli(100))
                .setLastFailedExportTime(Instant.ofEpochMilli(1000))
                .setDataExportError(ScheduledExportStatus.DATA_EXPORT_LOST_FILE_ACCESS)
                .setPeriodInDays(7)
                .setLastExportFileName("healthconnect.zip")
                .setLastExportAppName("Drive")
                .setNextExportFileName("hc.zip")
                .setNextExportAppName("Dropbox")
                .setNextExportSequentialNumber(5)
                .build()
        fakeHealthDataExportManager.setScheduledExportStatus(scheduledExportStatus)
        val result = useCase.invoke()

        assertThat(result is ExportImportUseCaseResult.Success).isTrue()
        val exportStatus = (result as ExportImportUseCaseResult.Success).data
        assertThat(exportStatus.lastSuccessfulExportTime).isEqualTo(Instant.ofEpochMilli(100))
        assertThat(exportStatus.dataExportError)
            .isEqualTo(ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS)
        assertThat(exportStatus.periodInDays).isEqualTo(7)
        assertThat(exportStatus.lastExportFileName).isEqualTo("healthconnect.zip")
        assertThat(exportStatus.lastExportAppName).isEqualTo("Drive")
        assertThat(exportStatus.nextExportFileName).isEqualTo("hc.zip")
        assertThat(exportStatus.nextExportAppName).isEqualTo("Dropbox")
        assertThat(exportStatus.nextExportSequentialNumber).isEqualTo(5)
        assertThat(exportStatus.lastFailedExportTime).isEqualTo(Instant.ofEpochMilli(1000))
    }

    @Test
    fun invoke_callsHealthDataExportManager_returnsFailure() = runTest {
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
        fakeHealthDataExportManager.setScheduledExportStatusException(exception)

        val result = useCase.invoke()

        assertThat(result is ExportImportUseCaseResult.Failed).isTrue()
        assertThat((result as ExportImportUseCaseResult.Failed).exception is HealthConnectException)
            .isTrue()
        assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }
}
