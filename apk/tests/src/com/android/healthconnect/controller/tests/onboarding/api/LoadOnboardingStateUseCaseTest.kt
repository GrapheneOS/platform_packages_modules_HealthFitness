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

package com.android.healthconnect.controller.tests.onboarding.api

import android.health.connect.HealthConnectOnboardingState
import com.android.healthconnect.controller.onboarding.api.LoadOnboardingStateUseCase
import com.android.healthconnect.controller.onboarding.api.OnboardingState
import com.android.healthconnect.controller.tests.utils.di.FakeHealthOnboardingManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadOnboardingStateUseCaseTest {

    private val healthOnboardingManager = FakeHealthOnboardingManager()

    @Test
    fun invoke_onboardingStateZeroApps_mapsToZeroAppsConnected() = runTest {
        val useCase = LoadOnboardingStateUseCase(healthOnboardingManager)
        healthOnboardingManager.setOnboardingState(
            HealthConnectOnboardingState(
                HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
            )
        )

        assertThat(useCase.invoke())
            .isEqualTo(OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED)
    }

    @Test
    fun invoke_onboardingStateOneApp_mapsToOneAppConnected() = runTest {
        val useCase = LoadOnboardingStateUseCase(healthOnboardingManager)
        healthOnboardingManager.setOnboardingState(
            HealthConnectOnboardingState(
                HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
            )
        )

        assertThat(useCase.invoke())
            .isEqualTo(OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED)
    }

    @Test
    fun invoke_onboardingStateHide_mapsToHide() = runTest {
        val useCase = LoadOnboardingStateUseCase(healthOnboardingManager)
        healthOnboardingManager.setOnboardingState(
            HealthConnectOnboardingState(HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_HIDE)
        )

        assertThat(useCase.invoke()).isEqualTo(OnboardingState.ONBOARDING_BANNER_STATE_HIDE)
    }
}
