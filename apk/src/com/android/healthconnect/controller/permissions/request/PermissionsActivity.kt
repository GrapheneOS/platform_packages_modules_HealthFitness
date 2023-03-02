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
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    companion object {
        private const val TAG = "PermissionsActivity"
    }

    @Inject lateinit var logger: HealthConnectLogger
    private val viewModel: RequestPermissionViewModel by viewModels()
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        if (!intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            Log.e(TAG, "Invalid Intent Extras, finishing")
            finish()
        }

        val rationalIntentDeclared =
            healthPermissionReader.isRationalIntentDeclared(getPackageNameExtra())
        if (!rationalIntentDeclared) {
            Log.e(TAG, "App should support rational intent, finishing!")
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
        logger.logImpression(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)

        cancelButton.setOnClickListener {
            logger.logInteraction(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
            handleResults(viewModel.request(getPackageNameExtra()))
        }
    }

    private fun setupAllowButton() {
        val allowButton: View = findViewById(R.id.allow)
        logger.logImpression(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
        viewModel.grantedPermissions.observe(this) { grantedPermissions ->
            allowButton.isEnabled = grantedPermissions.isNotEmpty()
        }
        allowButton.setOnClickListener {
            logger.logInteraction(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
            handleResults(viewModel.request(getPackageNameExtra()))
        }
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
