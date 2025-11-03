/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.autodelete

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.autodelete.AutoDeleteRange
import com.android.healthconnect.controller.autodelete.AutoDeleteViewModel
import com.android.healthconnect.controller.tests.autodelete.api.FakeLoadAutoDeleteUseCase
import com.android.healthconnect.controller.tests.autodelete.api.FakeUpdateAutoDeleteUseCase
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
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
class AutoDeleteViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AutoDeleteViewModel
    private val loadAutoDeleteUseCase = FakeLoadAutoDeleteUseCase()
    private val updateAutoDeleteUseCase = FakeUpdateAutoDeleteUseCase()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadAutoDeleteUseCaseSuccess_storedAutoDeleteRangeIsSet() = runTest {
        loadAutoDeleteUseCase.setAutoDeleteRange(3)
        viewModel = AutoDeleteViewModel(loadAutoDeleteUseCase, updateAutoDeleteUseCase)
        val testObserver = TestObserver<AutoDeleteViewModel.AutoDeleteState>()
        viewModel.storedAutoDeleteRange.observeForever(testObserver)

        advanceUntilIdle()

        val state = testObserver.getLastValue()
        assertThat(state is AutoDeleteViewModel.AutoDeleteState.WithData).isTrue()
        assertThat((state as AutoDeleteViewModel.AutoDeleteState.WithData).autoDeleteRange)
            .isEqualTo(AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS)
    }

    @Test
    fun init_loadAutoDeleteUseCaseFails_storedAutoDeleteRangeIsLoadingFailed() = runTest {
        loadAutoDeleteUseCase.setForceFail(true)
        viewModel = AutoDeleteViewModel(loadAutoDeleteUseCase, updateAutoDeleteUseCase)
        val testObserver = TestObserver<AutoDeleteViewModel.AutoDeleteState>()
        viewModel.storedAutoDeleteRange.observeForever(testObserver)

        advanceUntilIdle()

        val state = testObserver.getLastValue()
        assertThat(state is AutoDeleteViewModel.AutoDeleteState.LoadingFailed).isTrue()
    }

    @Test
    fun updateAutoDeleteRange_updateUseCaseSuccess_storedAutoDeleteRangeIsUpdated() = runTest {
        viewModel = AutoDeleteViewModel(loadAutoDeleteUseCase, updateAutoDeleteUseCase)
        val testObserver = TestObserver<AutoDeleteViewModel.AutoDeleteState>()
        viewModel.storedAutoDeleteRange.observeForever(testObserver)
        advanceUntilIdle() // for init

        viewModel.updateAutoDeleteRange(AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS)
        advanceUntilIdle()

        val state = testObserver.getLastValue()
        assertThat(state is AutoDeleteViewModel.AutoDeleteState.WithData).isTrue()
        assertThat((state as AutoDeleteViewModel.AutoDeleteState.WithData).autoDeleteRange)
            .isEqualTo(AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS)
    }

    @Test
    fun updateAutoDeleteRange_updateUseCaseFails_storedAutoDeleteRangeIsLoadingFailed() = runTest {
        viewModel = AutoDeleteViewModel(loadAutoDeleteUseCase, updateAutoDeleteUseCase)
        val testObserver = TestObserver<AutoDeleteViewModel.AutoDeleteState>()
        viewModel.storedAutoDeleteRange.observeForever(testObserver)
        advanceUntilIdle() // for init

        updateAutoDeleteUseCase.setForceFail(true)
        viewModel.updateAutoDeleteRange(AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS)
        advanceUntilIdle()

        val state = testObserver.getLastValue()
        assertThat(state is AutoDeleteViewModel.AutoDeleteState.LoadingFailed).isTrue()
    }

    @Test
    fun postAutoDeleteRange_oldAutoDeleteRangeIsUpdated() = runTest {
        loadAutoDeleteUseCase.setAutoDeleteRange(3)
        viewModel = AutoDeleteViewModel(loadAutoDeleteUseCase, updateAutoDeleteUseCase)
        val testObserver = TestObserver<AutoDeleteRange>()
        viewModel.oldAutoDeleteRange.observeForever(testObserver)

        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS)
    }

    @Test
    fun updateAutoDeleteDialogArgument_newAutoDeleteRangeIsUpdated() {
        loadAutoDeleteUseCase.setAutoDeleteRange(0)
        viewModel = AutoDeleteViewModel(loadAutoDeleteUseCase, updateAutoDeleteUseCase)
        val testObserver = TestObserver<AutoDeleteRange>()
        viewModel.newAutoDeleteRange.observeForever(testObserver)

        viewModel.updateAutoDeleteDialogArgument(AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS)

        assertThat(testObserver.getLastValue())
            .isEqualTo(AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS)
    }
}
