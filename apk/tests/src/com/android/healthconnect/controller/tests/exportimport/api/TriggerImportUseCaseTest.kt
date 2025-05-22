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
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.ExportImportUseCaseResult
import com.android.healthconnect.controller.exportimport.api.HealthDataImportManager
import com.android.healthconnect.controller.exportimport.api.TriggerImportUseCase
import com.android.healthconnect.controller.service.HealthDataImportManagerModule
import com.android.healthconnect.controller.tests.utils.di.FakeHealthDataImportManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@UninstallModules(HealthDataImportManagerModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TriggerImportUseCaseTest {

    companion object {
        private val TEST_DOCUMENT_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider1.documents" +
                    "/root/account1/document")
    }

    @BindValue val healthDataImportManager: HealthDataImportManager = FakeHealthDataImportManager()

    private lateinit var useCase: TriggerImportUseCase

    @Before
    fun setup() {
        useCase = TriggerImportUseCase(healthDataImportManager)
    }

    @After
    fun teardown() {
        (healthDataImportManager as FakeHealthDataImportManager).reset()
    }

    @Test
    fun invoke_callsHealthDataImportManager() = runTest {
        val result = useCase.invoke(TEST_DOCUMENT_URI)

        assertThat(result is ExportImportUseCaseResult.Success).isTrue()
    }

    @Test
    fun invoke_callsHealthDataImportManager_returnsFailure() = runTest {
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
        (healthDataImportManager as FakeHealthDataImportManager).setRunImportException(exception)

        val result = useCase.invoke(TEST_DOCUMENT_URI)

        assertThat(result is ExportImportUseCaseResult.Failed).isTrue()
        assertThat((result as ExportImportUseCaseResult.Failed).exception is HealthConnectException)
            .isTrue()
        assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }
}
