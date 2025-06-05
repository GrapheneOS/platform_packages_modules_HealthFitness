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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.OnboardingViewModel
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeLoadFitnessPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadOnboardingStateUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
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
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel
    private val loadFitnessPermissionApps = FakeLoadFitnessPermissionAppsUseCase()
    private val loadOnboardingStateUseCase = FakeLoadOnboardingStateUseCase()
    @Inject @ApplicationContext lateinit var applicationContext: Context

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        viewModel =
            OnboardingViewModel(
                applicationContext,
                loadFitnessPermissionApps,
                loadOnboardingStateUseCase,
            )
    }

    @After
    fun teardown() {
        loadFitnessPermissionApps.reset()
        loadOnboardingStateUseCase.reset()
    }

    @Test
    fun loadConnectedApps_invokesUseCase() = runTest {
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, false),
                ConnectedFitnessAppMetadata(TEST_APP_2, false),
            )
        )
        viewModel.loadConnectedApps()
        advanceUntilIdle()
        assertThat(loadFitnessPermissionApps.invocations).isEqualTo(1)
    }

    @Test
    fun loadConnectedApps_whenNoApps_onboardingFragmentStateNoApps() = runTest {
        loadFitnessPermissionApps.setConnectedApps(listOf())
        val testObserver = TestObserver<OnboardingViewModel.OnboardingFragmentState>()
        viewModel.connectedApps.observeForever(testObserver)
        viewModel.loadConnectedApps()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingFragmentState.NoApps).isTrue()
    }

    @Test
    fun loadConnectedApps_noAppsConnected_twoAvailable_onboardingFragmentStateZeroAppsConnected() =
        runTest {
            loadFitnessPermissionApps.setConnectedApps(
                listOf(
                    ConnectedFitnessAppMetadata(TEST_APP, false),
                    ConnectedFitnessAppMetadata(TEST_APP_2, false),
                    ConnectedFitnessAppMetadata(TEST_APP_3, false),
                )
            )
            val testObserver = TestObserver<OnboardingViewModel.OnboardingFragmentState>()
            viewModel.connectedApps.observeForever(testObserver)
            viewModel.loadConnectedApps()
            advanceUntilIdle()

            val actual = testObserver.getLastValue()
            assertThat(actual is OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected)
                .isTrue()
            assertThat(
                    (actual as OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected)
                        .potentialApps
                )
                .containsExactlyElementsIn(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP, false),
                        ConnectedFitnessAppMetadata(TEST_APP_2, false),
                        ConnectedFitnessAppMetadata(TEST_APP_3, false),
                    )
                )
        }

    @Test
    fun loadConnectedApps_noAppsConnected_oneAvailable_onboardingFragmentStateNoApps() = runTest {
        loadFitnessPermissionApps.setConnectedApps(
            listOf(ConnectedFitnessAppMetadata(TEST_APP, false))
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingFragmentState>()
        viewModel.connectedApps.observeForever(testObserver)
        viewModel.loadConnectedApps()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingFragmentState.NoApps).isTrue()
    }

    @Test
    fun loadConnectedApps_oneAppConnected_noneAvailable_onboardingFragmentStateNoApps() = runTest {
        loadFitnessPermissionApps.setConnectedApps(
            listOf(ConnectedFitnessAppMetadata(TEST_APP, true))
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingFragmentState>()
        viewModel.connectedApps.observeForever(testObserver)
        viewModel.loadConnectedApps()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingFragmentState.NoApps).isTrue()
    }

    @Test
    fun loadConnectedApps_oneAppConnected_oneAvailable_onboardingFragmentStateOneAppConnected() =
        runTest {
            loadFitnessPermissionApps.setConnectedApps(
                listOf(
                    ConnectedFitnessAppMetadata(TEST_APP, true),
                    ConnectedFitnessAppMetadata(TEST_APP_2, false),
                    ConnectedFitnessAppMetadata(TEST_APP_3, false),
                )
            )
            val testObserver = TestObserver<OnboardingViewModel.OnboardingFragmentState>()
            viewModel.connectedApps.observeForever(testObserver)
            viewModel.loadConnectedApps()
            advanceUntilIdle()

            val actual = testObserver.getLastValue()
            assertThat(actual is OnboardingViewModel.OnboardingFragmentState.OneAppConnected)
                .isTrue()
            assertThat(
                    (actual as OnboardingViewModel.OnboardingFragmentState.OneAppConnected)
                        .connectedApp
                )
                .isEqualTo(ConnectedFitnessAppMetadata(TEST_APP, true))
            assertThat(actual.potentialApps)
                .containsExactlyElementsIn(
                    listOf(
                        ConnectedFitnessAppMetadata(TEST_APP_2, false),
                        ConnectedFitnessAppMetadata(TEST_APP_3, false),
                    )
                )
        }

    @Test
    fun loadConnectedApps_twoAppsConnected_onboardingFragmentStateAlmostDone() = runTest {
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, true),
                ConnectedFitnessAppMetadata(TEST_APP_2, true),
                ConnectedFitnessAppMetadata(TEST_APP_3, false),
            )
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingFragmentState>()
        viewModel.connectedApps.observeForever(testObserver)
        viewModel.loadConnectedApps()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingFragmentState.AlmostDone).isTrue()
        assertThat((actual as OnboardingViewModel.OnboardingFragmentState.AlmostDone).connectedApps)
            .containsExactlyElementsIn(
                listOf(
                    ConnectedFitnessAppMetadata(TEST_APP, true),
                    ConnectedFitnessAppMetadata(TEST_APP_2, true),
                )
            )
    }

    @Test
    fun loadOnboardingBannerState_invokesUseCase() = runTest {
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_HIDE
        )
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()
        assertThat(loadOnboardingStateUseCase.invocations).isEqualTo(1)
    }

    @Test
    fun whenOnboardingStateIsHide_returnsNoOnboardingBanner() = runTest {
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, false),
                ConnectedFitnessAppMetadata(TEST_APP_2, false),
            )
        )
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_HIDE
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingBannerState>()
        viewModel.onboardingBannerState.observeForever(testObserver)
        viewModel.loadConnectedApps()
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner).isTrue()
    }

    @Test
    fun whenBannerSeen_noOnboardingBanner() = runTest {
        setPreferenceSeen(Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, true)
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, false),
                ConnectedFitnessAppMetadata(TEST_APP_2, false),
            )
        )
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingBannerState>()
        viewModel.onboardingBannerState.observeForever(testObserver)
        viewModel.loadConnectedApps()
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner).isTrue()
    }

    @Test
    fun whenConnectedAppsCountZero_zeroAppsBanner() = runTest {
        setPreferenceSeen(Constants.ONBOARDING_ZERO_APPS_BANNER_SEEN, false)
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, false),
                ConnectedFitnessAppMetadata(TEST_APP_2, false),
            )
        )
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingBannerState>()
        viewModel.onboardingBannerState.observeForever(testObserver)
        viewModel.loadConnectedApps()
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingBannerState.ZeroAppsOnboardingBanner)
            .isTrue()
    }

    @Test
    fun whenConnectedAppsCountOne_oneAppBanner() = runTest {
        setPreferenceSeen(Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, true),
                ConnectedFitnessAppMetadata(TEST_APP_2, false),
            )
        )
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingBannerState>()
        viewModel.onboardingBannerState.observeForever(testObserver)
        viewModel.loadConnectedApps()
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingBannerState.OneAppOnboardingBanner)
            .isTrue()
    }

    @Test
    fun whenConnectedAppsCountTwo_noBanner() = runTest {
        setPreferenceSeen(Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, true),
                ConnectedFitnessAppMetadata(TEST_APP_2, true),
            )
        )
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingBannerState>()
        viewModel.onboardingBannerState.observeForever(testObserver)
        viewModel.loadConnectedApps()
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner).isTrue()
    }

    @Test
    fun whenOnboardingLoadingError_noBanner() = runTest {
        setPreferenceSeen(Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadFitnessPermissionApps.setConnectedApps(
            listOf(
                ConnectedFitnessAppMetadata(TEST_APP, false),
                ConnectedFitnessAppMetadata(TEST_APP_2, false),
            )
        )
        loadOnboardingStateUseCase.setForceFail(true)
        val testObserver = TestObserver<OnboardingViewModel.OnboardingBannerState>()
        viewModel.onboardingBannerState.observeForever(testObserver)
        viewModel.loadConnectedApps()
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner).isTrue()
    }

    @Test
    fun whenConnectedAppsLoadingError_noBanner() = runTest {
        setPreferenceSeen(Constants.ONBOARDING_ONE_APP_BANNER_SEEN, false)
        loadFitnessPermissionApps.setForceFail(true)
        loadOnboardingStateUseCase.setOnboardingBannerState(
            OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
        )
        val testObserver = TestObserver<OnboardingViewModel.OnboardingBannerState>()
        viewModel.onboardingBannerState.observeForever(testObserver)
        viewModel.loadConnectedApps()
        viewModel.loadOnboardingBannerState()
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        assertThat(actual is OnboardingViewModel.OnboardingBannerState.NoOnboardingBanner).isTrue()
    }

    private fun setPreferenceSeen(preferenceName: String, seen: Boolean) {
        val sharedPreference =
            applicationContext.getSharedPreferences(
                Constants.USER_ACTIVITY_TRACKER,
                Context.MODE_PRIVATE,
            )
        val editor = sharedPreference.edit()
        editor.putBoolean(preferenceName, seen)
        editor.apply()
    }
}
