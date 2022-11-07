/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for health data categories. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class ConnectedAppsFragment : Hilt_ConnectedAppsFragment(){

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.connected_apps_screen, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.connected_apps_title)
    }
}
