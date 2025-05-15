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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LoadOnboardingStateUseCase @Inject constructor(private val manager: HealthOnboardingManager) :
    ILoadOnboardingStateUseCase {

    override suspend operator fun invoke(): OnboardingState {
        return withContext(Dispatchers.IO) {
            val state = suspendCancellableCoroutine { continuation ->
                manager.getHealthConnectOnboardingState(
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
            when (state.onboardingState) {
                0 -> OnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED
                1 -> OnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED
                else -> OnboardingState.ONBOARDING_BANNER_STATE_HIDE
            }
        }
    }
}

enum class OnboardingState {
    ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED,
    ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED,
    ONBOARDING_BANNER_STATE_HIDE,
}

interface ILoadOnboardingStateUseCase {
    suspend operator fun invoke(): OnboardingState
}
