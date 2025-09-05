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
import android.health.connect.exportimport.ExportImportDocumentProvider
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.DocumentProviderInfo
import com.android.healthconnect.controller.exportimport.api.DocumentProviderRoot
import com.android.healthconnect.controller.exportimport.api.HealthDataExportManager
import com.android.healthconnect.controller.exportimport.api.QueryDocumentProvidersUseCase
import com.android.healthconnect.controller.service.HealthDataExportManagerModule
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.di.FakeHealthDataExportManager
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

@UninstallModules(HealthDataExportManagerModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class QueryDocumentProvidersUseCaseTest {
    companion object {
        private const val TEST_DOCUMENT_PROVIDER_1_TITLE = "Document provider 1"
        private const val TEST_DOCUMENT_PROVIDER_1_AUTHORITY = "documentprovider1.com"
        private const val TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE = 1
        private const val TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY = "Account 1"
        private val TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider1.documents/root/account1"
            )
        private const val TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY = "Account 2"
        private val TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider1.documents/root/account2"
            )

        private const val TEST_DOCUMENT_PROVIDER_2_TITLE = "Document provider 2"
        private const val TEST_DOCUMENT_PROVIDER_2_AUTHORITY = "documentprovider2.com"
        private const val TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE = 2
        private const val TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY = "Account"
        private val TEST_DOCUMENT_PROVIDER_2_ROOT_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider2.documents/root/account"
            )
    }

    @BindValue val healthDataExportManager: HealthDataExportManager = FakeHealthDataExportManager()

    private lateinit var useCase: QueryDocumentProvidersUseCase

    @Before
    fun setup() {
        useCase = QueryDocumentProvidersUseCase(healthDataExportManager, Dispatchers.Main)
    }

    @After
    fun teardown() {
        (healthDataExportManager as FakeHealthDataExportManager).reset()
    }

    @Test
    fun invoke_callsHealthDataExportManagerSuccessfully() = runTest {
        val exportImportDocumentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        (healthDataExportManager as FakeHealthDataExportManager).setExportImportDocumentProviders(
            exportImportDocumentProviders
        )

        val result = useCase.invoke(Unit)

        assertThat(result is UseCaseResults.Success).isTrue()
        val documentProviders = (result as UseCaseResults.Success).data
        assertThat(documentProviders).hasSize(1)
        assertThat(documentProviders[0])
            .isEqualTo(
                DocumentProvider(
                    DocumentProviderInfo(
                        TEST_DOCUMENT_PROVIDER_1_TITLE,
                        TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                        TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    ),
                    listOf(
                        DocumentProviderRoot(
                            TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                            TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                        )
                    ),
                )
            )
    }

    @Test
    fun invoke_callsHealthDataExportManagerSuccessfully_documentProvidersSorted() = runTest {
        val exportImportDocumentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
            )
        (healthDataExportManager as FakeHealthDataExportManager).setExportImportDocumentProviders(
            exportImportDocumentProviders
        )

        val result = useCase.invoke(Unit)

        assertThat(result is UseCaseResults.Success).isTrue()
        val documentProviders = (result as UseCaseResults.Success).data
        assertThat(documentProviders).hasSize(2)
        assertThat(documentProviders[0])
            .isEqualTo(
                DocumentProvider(
                    DocumentProviderInfo(
                        TEST_DOCUMENT_PROVIDER_1_TITLE,
                        TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                        TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    ),
                    listOf(
                        DocumentProviderRoot(
                            TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                            TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                        ),
                        DocumentProviderRoot(
                            TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                            TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                        ),
                    ),
                )
            )
        assertThat(documentProviders[1])
            .isEqualTo(
                DocumentProvider(
                    DocumentProviderInfo(
                        TEST_DOCUMENT_PROVIDER_2_TITLE,
                        TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                        TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    ),
                    listOf(
                        DocumentProviderRoot(
                            TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                            TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                        )
                    ),
                )
            )
    }

    @Test
    fun invoke_callsHealthDataExportManager_returnsFailure() = runTest {
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)
        (healthDataExportManager as FakeHealthDataExportManager).setQueryDocumentProvidersException(
            exception
        )
        val result = useCase.invoke(Unit)

        assertThat(result is UseCaseResults.Failed).isTrue()
        assertThat((result as UseCaseResults.Failed).exception is HealthConnectException).isTrue()
        assertThat((result.exception as HealthConnectException).errorCode)
            .isEqualTo(HealthConnectException.ERROR_UNKNOWN)
    }
}
