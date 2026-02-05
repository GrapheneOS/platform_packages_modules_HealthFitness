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
package com.android.healthconnect.controller.tests.migration

import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeLoadMigrationStateUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MigrationViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var viewModel: MigrationViewModel
    private val loadMigrationRestoreStateUseCase = FakeLoadMigrationStateUseCase()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        loadMigrationRestoreStateUseCase.reset()
    }

    @Test
    fun init_useCaseSuccess_setsWithDataState() = runTest {
        val expectedState =
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        loadMigrationRestoreStateUseCase.setMigrationState(expectedState)

        viewModel = MigrationViewModel(loadMigrationRestoreStateUseCase)
        val testObserver = TestObserver<MigrationViewModel.MigrationFragmentState>()
        viewModel.migrationState.observeForever(testObserver)
        advanceUntilIdle()

        val result = testObserver.getLastValue()
        assertThat(result)
            .isInstanceOf(MigrationViewModel.MigrationFragmentState.WithData::class.java)
        val withData = result as MigrationViewModel.MigrationFragmentState.WithData
        assertThat(withData.migrationRestoreState).isEqualTo(expectedState)
    }

    @Test
    fun init_useCaseFails_setsErrorState() = runTest {
        loadMigrationRestoreStateUseCase.setForceFail(true)

        viewModel = MigrationViewModel(loadMigrationRestoreStateUseCase)
        val testObserver = TestObserver<MigrationViewModel.MigrationFragmentState>()
        viewModel.migrationState.observeForever(testObserver)
        advanceUntilIdle()

        val result = testObserver.getLastValue()
        assertThat(result).isInstanceOf(MigrationViewModel.MigrationFragmentState.Error::class.java)
    }
}
