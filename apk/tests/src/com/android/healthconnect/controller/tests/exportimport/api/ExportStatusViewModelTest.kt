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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeLoadScheduledExportStatusUseCase
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlinx.coroutines.Dispatchers
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

@HiltAndroidTest
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ExportStatusViewModelTest {
    companion object {
        private const val TEST_EXPORT_FREQUENCY_IN_DAYS = 7
    }

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ExportStatusViewModel
    private val loadScheduledExportStatusUseCase = FakeLoadScheduledExportStatusUseCase()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel = ExportStatusViewModel(loadScheduledExportStatusUseCase)
    }

    @After
    fun tearDown() {
        loadScheduledExportStatusUseCase.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun loadScheduledExportStatus() = runTest {
        val testObserver = TestObserver<ScheduledExportUiStatus>()
        viewModel.storedScheduledExportStatus.observeForever(testObserver)
        val scheduledExportUiState =
            ScheduledExportUiState(
                Instant.ofEpochMilli(100),
                ScheduledExportUiState.DataExportError.DATA_EXPORT_LOST_FILE_ACCESS,
                TEST_EXPORT_FREQUENCY_IN_DAYS,
                "hc.zip",
                "Drive",
                "test.zip",
                "Dropbox")
        loadScheduledExportStatusUseCase.updateExportStatus(scheduledExportUiState)

        viewModel.loadScheduledExportStatus()
        advanceUntilIdle()

        Truth.assertThat(testObserver.getLastValue())
            .isEqualTo(ScheduledExportUiStatus.WithData(scheduledExportUiState))
    }
}
