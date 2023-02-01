/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 *
 */

/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.permissions.request

import android.app.Activity
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import dagger.hilt.android.AndroidEntryPoint

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    companion object {
        private const val TAG = "PermissionsActivity"
    }

    private val viewModel: RequestPermissionViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        if (!intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            Log.e(TAG, "Invalid Intent Extras, finishing")
            finish()
        }

        viewModel.init(getPackageNameExtra(), getPermissionStrings())

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.permission_content, PermissionsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        setupAllowButton()
        setupCancelButton()
    }

    private fun setupCancelButton() {
        val cancelButton: View = findViewById(R.id.cancel)
        cancelButton.setOnClickListener { handleResults(viewModel.request(getPackageNameExtra())) }
    }

    private fun setupAllowButton() {
        val allowButton: View = findViewById(R.id.allow)
        viewModel.grantedPermissions.observe(this) { grantedPermissions ->
            allowButton.isEnabled = grantedPermissions.isNotEmpty()
        }
        allowButton.setOnClickListener { handleResults(viewModel.request(getPackageNameExtra())) }
    }

    private fun handleResults(results: Map<HealthPermission, PermissionState>) {
        val grants =
            results.values
                .map { permissionSelection ->
                    if (PermissionState.GRANTED == permissionSelection) {
                        PackageManager.PERMISSION_GRANTED
                    } else {
                        PackageManager.PERMISSION_DENIED
                    }
                }
                .toIntArray()
        val result = Intent()
        result.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES, getPermissionStrings())
        result.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS, grants)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun getPermissionStrings(): Array<out String> {
        return intent.getStringArrayExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES).orEmpty()
    }

    private fun getPackageNameExtra(): String {
        return intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
    }
}
