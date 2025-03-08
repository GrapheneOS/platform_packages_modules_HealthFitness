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

package com.android.healthconnect.controller.exportimport

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.settingslib.collapsingtoolbar.EdgeToEdgeUtils
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint

/** Export setup activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class ExportSetupActivity : Hilt_ExportSetupActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.enable(this)
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_HealthConnect_Expressive)
        }
        super.onCreate(savedInstanceState)

        // Setting an empty string with a space avoids TalkBack announcing the app title and instead
        // only announces the page heading.
        title = " "

        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(
            WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
        )
        setContentView(R.layout.activity_export)
    }
}
