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
import com.android.healthconnect.controller.R
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import dagger.hilt.android.AndroidEntryPoint

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(CollapsingToolbarBaseActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        setTitle(R.string.permissions_and_data_header)
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.permission_content, PermissionsFragment.newInstance())
                .commit()
    }
}
