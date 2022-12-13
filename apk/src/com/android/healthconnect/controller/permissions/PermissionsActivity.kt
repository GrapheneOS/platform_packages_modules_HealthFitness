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
package com.android.healthconnect.controller.permissions

import android.app.Activity
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.settings.SettingsActivity
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.shared.HealthPermissionReader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    private val viewModel: RequestPermissionViewModel by viewModels()
    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    private lateinit var appPackageName: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        // TODO(b/260629311) replace with new intent
        if (!intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            val newIntent = Intent(intent).setClass(this, SettingsActivity::class.java)
            startActivity(newIntent)
            finish()
        }

        appPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val permissionSelection = viewModel.getPermissionSelection()
        val permissions = permissionSelection.ifEmpty { getPermissions().associateWith { false } }
        val permissionsFragment = PermissionsFragment.newInstance(permissions)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.permission_content, permissionsFragment)
            .commit()

        setupAllowButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.savePermissionSelection(getPermissionsFragment().getPermissionAssignments())
    }

    private fun setupAllowButton() {
        val allowButton: View? = findViewById(R.id.allow)
        allowButton?.setOnClickListener {
            val permissions = getPermissionsFragment().getPermissionAssignments()
            viewModel.request(appPackageName, permissions)
            viewModel.permissionResults.observe(this) { results -> handleResults(results) }
        }
    }

    private fun handleResults(results: Map<HealthPermission, PermissionState>) {
        val grants =
            getPermissions()
                .map { permission ->
                    if (results[permission] == PermissionState.GRANTED)
                        PackageManager.PERMISSION_GRANTED
                    else PackageManager.PERMISSION_DENIED
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

    private fun getPermissions(): List<HealthPermission> {
        return getPermissionStrings().mapNotNull { permissionString ->
            HealthPermission.fromPermissionString(permissionString)
        }
    }

    private fun getPermissionsFragment(): PermissionsFragment {
        return supportFragmentManager.findFragmentById(R.id.permission_content)
            as PermissionsFragment
    }
}
