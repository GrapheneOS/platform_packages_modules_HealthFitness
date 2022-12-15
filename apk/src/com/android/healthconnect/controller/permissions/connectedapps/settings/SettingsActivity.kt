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
package com.android.healthconnect.controller.permissions.connectedapps.settings

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.android.healthconnect.controller.R
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(CollapsingToolbarBaseActivity::class)
class SettingsActivity : Hilt_SettingsActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle(R.string.permgrouplab_health)
    }

    override fun onStart() {
        super.onStart()
        if (intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            findNavController(R.id.nav_host_fragment)
                .navigate(
                    R.id.action_deeplink_to_settingsManageAppPermissionsFragment,
                    bundleOf(EXTRA_PACKAGE_NAME to intent.getStringExtra(EXTRA_PACKAGE_NAME)))
        }
    }

    override fun onNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        if (!navController.popBackStack()) {
            finish()
        }
        return true
    }
}
