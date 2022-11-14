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

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import dagger.hilt.android.AndroidEntryPoint

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(CollapsingToolbarBaseActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        setTitle(R.string.permissions_and_data_header)

        val permissionsStrings: Array<out String> =
            getIntent()
                .getStringArrayExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES)
                .orEmpty()
        val permissions: List<HealthPermission> =
            permissionsStrings.mapNotNull { permissionString ->
                HealthPermission.fromPermissionString(permissionString)
            }
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.permission_content, PermissionsFragment.newInstance(permissions))
            .commit()
        val allowButton: View? = findViewById(R.id.allow)
        val grants = Array<Int?>(permissionsStrings.size) { PackageManager.PERMISSION_DENIED }
        allowButton?.setOnClickListener {
            val result = Intent()
            result.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES, permissionsStrings)
            result.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS, grants)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }
}
