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

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.exportimport.api.ImportFlowViewModel
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeTriggerImportUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
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
class ImportFlowViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ImportFlowViewModel
    private val triggerImportUseCase = FakeTriggerImportUseCase()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel = ImportFlowViewModel(triggerImportUseCase)
    }

    @After
    fun tearDown() {
        triggerImportUseCase.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun setLastCompletionInstant_updatesLiveData() {
        val testObserver = TestObserver<Instant?>()
        viewModel.lastImportCompletionInstant.observeForever(testObserver)
        val testInstant = Instant.now()

        viewModel.setLastCompletionInstant(testInstant)

        assertThat(testObserver.getLastValue()).isEqualTo(testInstant)
    }

    @Test
    fun triggerImportOfSelectedFile_success_updatesLastCompletionInstant() = runTest {
        val testObserver = TestObserver<Instant?>()
        viewModel.lastImportCompletionInstant.observeForever(testObserver)
        val testUri = Uri.parse("content://test")

        viewModel.triggerImportOfSelectedFile(testUri)
        advanceUntilIdle()

        assertThat(triggerImportUseCase.lastUri).isEqualTo(testUri)
        assertThat(testObserver.getLastValue()).isNotNull()
    }

    @Test
    fun triggerImportOfSelectedFile_failure_doesNotUpdateLastCompletionInstant() = runTest {
        val testObserver = TestObserver<Instant?>()
        viewModel.lastImportCompletionInstant.observeForever(testObserver)
        viewModel.setLastCompletionInstant(null)
        triggerImportUseCase.setForceFail(true)
        val testUri = Uri.parse("content://test")

        viewModel.triggerImportOfSelectedFile(testUri)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isNull()
    }
}
