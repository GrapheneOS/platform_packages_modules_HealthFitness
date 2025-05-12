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

package com.android.healthconnect.controller.tests.onboarding

import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.di.FakeLoadFitnessPermissionAppsUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class OnboardingViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel
    private val loadFitnessPermissionApps = FakeLoadFitnessPermissionAppsUseCase()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        viewModel = OnboardingViewModel(loadFitnessPermissionApps)
    }

    @After
    fun teardown() {
        loadFitnessPermissionApps.reset()
    }

    @Test
    fun loadConnectedApps_invokesUseCase() = runTest {
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, false),
                ConnectedFitnessAppMetadata(TEST_APP_2, false),
            )
        )
        assertThat(loadFitnessPermissionApps.invocations).isEqualTo(1)
        viewModel.loadConnectedApps()
        advanceUntilIdle()
        assertThat(loadFitnessPermissionApps.invocations).isEqualTo(2)
    }
}
