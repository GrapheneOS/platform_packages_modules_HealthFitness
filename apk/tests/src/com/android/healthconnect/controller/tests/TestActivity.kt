/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.settingslib.collapsingtoolbar.EdgeToEdgeUtils
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(FragmentActivity::class)
class TestActivity : Hilt_TestActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.enable(this)
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_HealthConnect_Expressive)
        } else {
            setTheme(R.style.Theme_HealthConnect)
        }
        super.onCreate(savedInstanceState)
    }
}
