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
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.utils.convertTextViewIntoLink
import dagger.hilt.android.AndroidEntryPoint

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        val permissionsStrings: Array<out String> =
            getIntent()
                .getStringArrayExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES)
                .orEmpty()
        val permissions: List<HealthPermission> =
            permissionsStrings.mapNotNull { permissionString ->
                HealthPermission.fromPermissionString(permissionString)
            }
        val permissionsFragment = PermissionsFragment.newInstance(permissions)
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.permission_content, permissionsFragment)
            .commit()

        updateAppName(getIntent())

        val allowButton: View? = findViewById(R.id.allow)
        allowButton?.setOnClickListener {
            val result = Intent()
            val permissionMap = permissionsFragment.getPermissionAssignments()
            val grants =
                permissions
                    .map { permission ->
                        if (permissionMap[permission] == true) PackageManager.PERMISSION_GRANTED
                        else PackageManager.PERMISSION_DENIED
                    }
                    .toIntArray()
            result.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES, permissionsStrings)
            result.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS, grants)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun updateAppName(intent: Intent) {
        // TODO: get the name based on package name.
        val policyString = resources.getString(R.string.request_permissions_privacy_policy)
        val packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME).orEmpty()
        val rationaleText =
            resources.getString(R.string.request_permissions_rationale, packageName, policyString)
        convertTextViewIntoLink(
            findViewById<TextView>(R.id.privacy_policy),
            rationaleText,
            rationaleText.indexOf(policyString),
            rationaleText.indexOf(policyString) + policyString.length,
            { // TODO: Link to developer's policy
            })
        findViewById<TextView>(R.id.title)
            .setText(resources.getString(R.string.request_permissions_header_title, packageName))
    }
}
