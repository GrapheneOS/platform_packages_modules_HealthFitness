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

package com.android.healthconnect.controller.permissions.request.wear

import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.PackageManager
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS
import android.health.connect.HealthPermissions
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Wear Grant Permissions activity for Health Connect. */
@AndroidEntryPoint(ComponentActivity::class)
class WearGrantPermissionsActivity : Hilt_WearGrantPermissionsActivity() {

    companion object {
        private const val TAG = "WearGrantPermissionsActivity"
    }

    private val requestPermissionsViewModel: RequestPermissionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            !getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH) ||
                !Flags.replaceBodySensorPermissionEnabled()
        ) {
            Log.e(
                TAG,
                "Health connect is not available on watch, activity should not have been started, " +
                    "finishing!",
            )
            return
        }

        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS)
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)

        // Load permissions for this package.
        val packageName = getPackageNameExtra()
        val rawPermissionStrings = getPermissionStrings()
        requestPermissionsViewModel.init(packageName, rawPermissionStrings)

        val grantableHealthPermissions =
            requestPermissionsViewModel.grantableHealthPermissionsList.value
        // Dismiss this request if there are no valid permissions requested.
        if (grantableHealthPermissions.isNullOrEmpty()) {
            finish()
            return
        }

        // Dismiss this request if any permission is USER_FIXED.
        if (
            requestPermissionsViewModel.isAnyPermissionUserFixed(
                packageName,
                grantableHealthPermissions.map { it.toString() }.toTypedArray(),
            )
        ) {
            handlePermissionResults()
            finish()
            return
        }

        // Launch composable UI.
        val root = ComposeView(this)
        root.setContent {
            WearGrantPermissionsScreen(requestPermissionsViewModel) {
                requestPermissionsViewModel.requestHealthPermissions(packageName)
                handlePermissionResults()
                finish()
            }
        }
        setContentView(root)
    }

    // TODO: b/376845793 - Reuse handlePermissionResults code in phone and wear, potentially move
    // this method to RequestPermissionViewModel.
    private fun handlePermissionResults(resultCode: Int = RESULT_OK) {
        val results = requestPermissionsViewModel.getPermissionGrants()
        val grants = mutableListOf<Int>()
        val permissionStrings = mutableListOf<String>()

        for ((permission, state) in results) {
            if (state == PermissionState.GRANTED) {
                grants.add(PackageManager.PERMISSION_GRANTED)
            } else {
                grants.add(PackageManager.PERMISSION_DENIED)
            }

            permissionStrings.add(permission.toString())
        }

        val result = Intent()
        result.putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissionStrings.toTypedArray())
        result.putExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS, grants.toIntArray())
        setResult(resultCode, result)
        finish()
    }

    private fun getPermissionStrings(): Array<out String> {
        return intent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES).orEmpty()
    }

    private fun getPackageNameExtra(): String {
        return intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
    }
}
