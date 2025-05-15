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

package com.android.healthconnect.controller.onboarding.api

import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class LoadOnboardingStateUseCase
@Inject
constructor(
    private val manager: HealthOnboardingManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadOnboardingStateUseCase, BaseUseCase<Unit, OnboardingState>(dispatcher) {

    override suspend fun execute(input: Unit): OnboardingState {
        val state = suspendCancellableCoroutine { continuation ->
            manager.getHealthConnectOnboardingState(Runnable::run, continuation.asOutcomeReceiver())
        }
        return when (state.onboardingState) {
            0 -> OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
            1 -> OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
            else -> OnboardingState.ONBOARDING_BANNER_STATE_HIDE
        }
    }
}

enum class OnboardingState {
    ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED,
    ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED,
    ONBOARDING_BANNER_STATE_HIDE,
}

interface ILoadOnboardingStateUseCase {
    suspend fun invoke(input: Unit): UseCaseResults<OnboardingState>

    suspend fun execute(input: Unit): OnboardingState
}
