/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

// Enables or disables animations in a test
fun toggleAnimation(isEnabled: Boolean) {
    with(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())) {
        executeShellCommand(
            "settings put global transition_animation_scale ${if (isEnabled) 1 else 0}"
        )
        executeShellCommand("settings put global window_animation_scale ${if (isEnabled) 1 else 0}")
        executeShellCommand(
            "settings put global animator_duration_scale ${if (isEnabled) 1 else 0}"
        )
    }
}
