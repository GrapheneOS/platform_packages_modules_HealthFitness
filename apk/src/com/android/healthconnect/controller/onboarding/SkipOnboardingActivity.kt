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

package com.android.healthconnect.controller.onboarding

import android.os.Bundle
import android.os.SystemProperties
import android.util.Log
import androidx.fragment.app.FragmentActivity

/**
 * A utility activity designed solely for testing purposes.
 *
 * This "faceless" activity is intended to programmatically bypass or disable the Health Connect
 * onboarding process. It should *never* be used in production environments.
 */
class SkipOnboardingActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isValidToSkipOnboarding()) {
            finish()
            return
        }
        OnboardingActivity.disableOnboarding(this)
        finish()
        return
    }

    private fun isValidToSkipOnboarding(): Boolean {
        if (intent != null) {
            val action = intent.action
            if (SKIP_HEALTH_CONNECT_ONBOARDING == action) {
                if (isDebuggable()) {
                    // Only allow skipping onboarding on debuggable builds.
                    Log.i(TAG, "This is valid. Skipping onboarding  ")
                    return true
                } else {
                    Log.w(TAG, "This is invalid, Skipping onboarding on non debuggable builds.")
                }
            }
        }
        return false
    }

    private fun isDebuggable(): Boolean {
        return SystemProperties.getInt("ro.debuggable", 0) == 1
    }

    companion object {
        private const val TAG = "SkipOnboardingActivity"
        const val SKIP_HEALTH_CONNECT_ONBOARDING =
            "android.health.connect.action.SKIP_HEALTH_CONNECT_ONBOARDING"
    }
}
