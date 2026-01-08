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

package com.android.healthconnect.controller.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SettingsTransitionHelper {

    /** Action used to launch the PlayStore update settings page. */
    const val ACTION_MAINLINE_UPDATE_SETTINGS: String = "android.settings.MODULE_UPDATE_SETTINGS"

    /**
     * Creates an intent to navigate to the Mainline Service Update Settings Page. In case Mainline
     * updates service is not available we direct the user to system settings.
     */
    fun Context.createMainlineServiceUpdateSettingsIntent(): Intent {
        val intent = Intent(ACTION_MAINLINE_UPDATE_SETTINGS)
        if (intent.resolveActivity(packageManager) == null) {
            return Intent(Settings.ACTION_SETTINGS)
        }

        return intent
    }
}
