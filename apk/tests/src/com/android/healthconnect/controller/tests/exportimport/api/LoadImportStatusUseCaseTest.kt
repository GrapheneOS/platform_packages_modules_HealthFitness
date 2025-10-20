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
import android.health.connect.exportimport.ImportStatus
import android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_NONE
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.HealthDataImportManager
import com.android.healthconnect.controller.exportimport.api.ImportUiState
import com.android.healthconnect.controller.exportimport.api.LoadImportStatusUseCase
import com.android.healthconnect.controller.service.HealthDataImportManagerModule
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.di.FakeHealthDataImportManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@UninstallModules(HealthDataImportManagerModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadImportStatusUseCaseTest {
    @BindValue val healthDataImportManager: HealthDataImportManager = FakeHealthDataImportManager()

    private lateinit var useCase: LoadImportStatusUseCase

    @Before
    fun setup() {
        useCase = LoadImportStatusUseCase(healthDataImportManager, Dispatchers.Main)
    }

    @After
    fun teardown() {
        (healthDataImportManager as FakeHealthDataImportManager).reset()
    }

    @Test
    fun invoke_callsHealthDataImportManager() = runTest {
        val importStatus = ImportStatus(DATA_IMPORT_ERROR_NONE)
        (healthDataImportManager as FakeHealthDataImportManager).setImportStatus(importStatus)
        val result = useCase.invoke(Unit)

        assertThat(result is UseCaseResults.Success).isTrue()
        val importStatusResult = (result as UseCaseResults.Success).data
        assertThat(importStatusResult.dataImportState)
            .isEqualTo(ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE)
    }

    @Test
    fun invoke_callsHealthDataImportManager_returnsFailure() = runTest {
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
        (healthDataImportManager as FakeHealthDataImportManager).setGetImportStatusException(
            exception
        )
        val result = useCase.invoke(Unit)

        assertThat(result is UseCaseResults.Failed).isTrue()
        assertThat((result as UseCaseResults.Failed).exception is HealthConnectException).isTrue()
        assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }
}
