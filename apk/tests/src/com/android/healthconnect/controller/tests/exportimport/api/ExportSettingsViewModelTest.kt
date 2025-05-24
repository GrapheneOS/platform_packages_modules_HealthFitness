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

import android.health.connect.Constants.DEFAULT_INT
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.DocumentProviderInfo
import com.android.healthconnect.controller.exportimport.api.DocumentProviderRoot
import com.android.healthconnect.controller.exportimport.api.DocumentProviders
import com.android.healthconnect.controller.exportimport.api.ExportFrequency.EXPORT_FREQUENCY_DAILY
import com.android.healthconnect.controller.exportimport.api.ExportFrequency.EXPORT_FREQUENCY_MONTHLY
import com.android.healthconnect.controller.exportimport.api.ExportFrequency.EXPORT_FREQUENCY_WEEKLY
import com.android.healthconnect.controller.exportimport.api.ExportSettings
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeLoadExportSettingsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeQueryDocumentProvidersUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeUpdateExportSettingsUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExportSettingsViewModelTest {
    companion object {
        private val TEST_URI: Uri = Uri.parse("content://com.android.server.healthconnect/testuri")

        private val TEST_DOCUMENT_PROVIDER_TITLE = "Document provider"
        private val TEST_DOCUMENT_PROVIDER_AUTHORITY = "documentprovider.com"
        private val TEST_DOCUMENT_PROVIDER_ICON_RESOURCE = 1
        private val TEST_DOCUMENT_PROVIDER_ROOT_SUMMARY = "Account"
        private val TEST_DOCUMENT_PROVIDER_ROOT_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider.documents/root/account"
            )
    }

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ExportSettingsViewModel
    private val loadExportSettingsUseCase = FakeLoadExportSettingsUseCase()
    private val updateExportSettingsUseCase = FakeUpdateExportSettingsUseCase()
    private val queryDocumentProvidersUseCase = FakeQueryDocumentProvidersUseCase()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel =
            ExportSettingsViewModel(
                loadExportSettingsUseCase,
                updateExportSettingsUseCase,
                queryDocumentProvidersUseCase,
            )
    }

    @After
    fun tearDown() {
        updateExportSettingsUseCase.reset()
        loadExportSettingsUseCase.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun loadExportSettings() = runTest {
        val testObserver = TestObserver<ExportSettings>()
        viewModel.storedExportSettings.observeForever(testObserver)
        loadExportSettingsUseCase.updateExportFrequency(EXPORT_FREQUENCY_WEEKLY)

        viewModel.loadExportSettings()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(ExportSettings.WithData(EXPORT_FREQUENCY_WEEKLY))
    }

    @Test
    fun loadDocumentProviders() = runTest {
        val testObserver = TestObserver<DocumentProviders>()
        viewModel.documentProviders.observeForever(testObserver)
        val documentProviders =
            listOf(
                DocumentProvider(
                    DocumentProviderInfo(
                        TEST_DOCUMENT_PROVIDER_TITLE,
                        TEST_DOCUMENT_PROVIDER_AUTHORITY,
                        TEST_DOCUMENT_PROVIDER_ICON_RESOURCE,
                    ),
                    listOf(
                        DocumentProviderRoot(
                            TEST_DOCUMENT_PROVIDER_ROOT_SUMMARY,
                            TEST_DOCUMENT_PROVIDER_ROOT_URI,
                        )
                    ),
                )
            )
        queryDocumentProvidersUseCase.updateDocumentProviders(documentProviders)

        viewModel.loadDocumentProviders()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(DocumentProviders.WithData(documentProviders))
    }

    @Test
    fun updateExportUri() = runTest {
        viewModel.updateExportUri(TEST_URI)
        advanceUntilIdle()

        assertThat(updateExportSettingsUseCase.mostRecentSettings.uri).isEqualTo(TEST_URI)
        assertThat(updateExportSettingsUseCase.mostRecentSettings.periodInDays)
            .isEqualTo(DEFAULT_INT)
    }

    @Test
    fun updateExportUri_keepsExistingFrequencySetting() = runTest {
        val testObserver = TestObserver<ExportSettings>()
        viewModel.storedExportSettings.observeForever(testObserver)
        loadExportSettingsUseCase.updateExportFrequency(EXPORT_FREQUENCY_MONTHLY)
        viewModel.loadExportSettings()

        viewModel.updateExportUri(TEST_URI)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(ExportSettings.WithData(EXPORT_FREQUENCY_MONTHLY))
    }

    @Test
    fun updateExportFrequency() = runTest {
        viewModel.updateExportFrequency(EXPORT_FREQUENCY_DAILY)
        advanceUntilIdle()

        assertThat(updateExportSettingsUseCase.mostRecentSettings.uri).isNull()
        assertThat(updateExportSettingsUseCase.mostRecentSettings.periodInDays)
            .isEqualTo(EXPORT_FREQUENCY_DAILY.periodInDays)
    }

    @Test
    fun updateExportFrequency_updatesStoredExportFrequency() = runTest {
        val testObserver = TestObserver<ExportSettings>()
        viewModel.storedExportSettings.observeForever(testObserver)
        loadExportSettingsUseCase.updateExportFrequency(EXPORT_FREQUENCY_MONTHLY)
        viewModel.loadExportSettings()

        viewModel.updateExportFrequency(EXPORT_FREQUENCY_DAILY)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(ExportSettings.WithData(EXPORT_FREQUENCY_DAILY))
    }

    @Test
    fun updatePreviousExportFrequency_updatesStoredPreviousExportFrequency() {
        viewModel.updatePreviousExportFrequency(EXPORT_FREQUENCY_DAILY)

        assertThat(viewModel.previousExportFrequency.value).isEqualTo(EXPORT_FREQUENCY_DAILY)
    }

    @Test
    fun updateSelectedExportFrequency() {
        viewModel.updateSelectedFrequency(EXPORT_FREQUENCY_DAILY)

        assertThat(viewModel.selectedExportFrequency.value).isEqualTo(EXPORT_FREQUENCY_DAILY)
    }

    @Test
    fun updateExportUriWithSelectedFrequency_updatesSetting() = runTest {
        viewModel.updateSelectedFrequency(EXPORT_FREQUENCY_DAILY)
        viewModel.updateExportUriWithSelectedFrequency(TEST_URI)
        advanceUntilIdle()

        assertThat(updateExportSettingsUseCase.mostRecentSettings.uri).isEqualTo(TEST_URI)
        assertThat(updateExportSettingsUseCase.mostRecentSettings.periodInDays)
            .isEqualTo(EXPORT_FREQUENCY_DAILY.periodInDays)
    }

    @Test
    fun updateSelectedDocumentProvider_selectedDocumentProviderUpdated() {
        val documentProviderInfo =
            DocumentProviderInfo(
                TEST_DOCUMENT_PROVIDER_TITLE,
                TEST_DOCUMENT_PROVIDER_AUTHORITY,
                TEST_DOCUMENT_PROVIDER_ICON_RESOURCE,
            )
        val documentProviderRoot =
            DocumentProviderRoot(
                TEST_DOCUMENT_PROVIDER_ROOT_SUMMARY,
                TEST_DOCUMENT_PROVIDER_ROOT_URI,
            )
        viewModel.updateSelectedDocumentProvider(documentProviderInfo, documentProviderRoot)

        assertThat(viewModel.selectedDocumentProvider.value).isEqualTo(documentProviderInfo)
    }

    @Test
    fun updateSelectedDocumentProvider_selectedDocumentProviderRootUpdated() {
        val documentProviderInfo =
            DocumentProviderInfo(
                TEST_DOCUMENT_PROVIDER_TITLE,
                TEST_DOCUMENT_PROVIDER_AUTHORITY,
                TEST_DOCUMENT_PROVIDER_ICON_RESOURCE,
            )
        val documentProviderRoot =
            DocumentProviderRoot(
                TEST_DOCUMENT_PROVIDER_ROOT_SUMMARY,
                TEST_DOCUMENT_PROVIDER_ROOT_URI,
            )
        viewModel.updateSelectedDocumentProvider(documentProviderInfo, documentProviderRoot)

        assertThat(viewModel.selectedDocumentProviderRoot.value).isEqualTo(documentProviderRoot)
    }

    @Test
    fun updateSelectedDocumentProvider_updated() {
        val documentProviderInfo =
            DocumentProviderInfo(
                TEST_DOCUMENT_PROVIDER_TITLE,
                TEST_DOCUMENT_PROVIDER_AUTHORITY,
                TEST_DOCUMENT_PROVIDER_ICON_RESOURCE,
            )
        val documentProviderRoot =
            DocumentProviderRoot(
                TEST_DOCUMENT_PROVIDER_ROOT_SUMMARY,
                TEST_DOCUMENT_PROVIDER_ROOT_URI,
            )
        viewModel.updateSelectedDocumentProvider(documentProviderInfo, documentProviderRoot)

        assertThat(viewModel.selectedRootsForDocumentProviders.value?.entries?.size).isEqualTo(1)
        assertThat(
                viewModel.selectedRootsForDocumentProviders.value?.get(TEST_DOCUMENT_PROVIDER_TITLE)
            )
            .isEqualTo(documentProviderRoot)
    }
}
