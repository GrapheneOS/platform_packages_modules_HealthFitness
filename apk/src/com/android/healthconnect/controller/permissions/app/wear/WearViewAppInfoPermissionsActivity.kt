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

package com.android.healthconnect.controller.permissions.app.wear

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.AndroidEntryPoint

/** Wear View App Info Permissions activity for Health&Fitness. */
@AndroidEntryPoint(ComponentActivity::class)
class WearViewAppInfoPermissionsActivity : Hilt_WearViewAppInfoPermissionsActivity() {

    companion object {
        private const val TAG = "WearViewAppInfoPermissionsActivity"
    }

    private val viewModel: AppPermissionViewModel by viewModels()

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
            finish()
            return
        }

        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS)

        val packageName = getPackageNameExtra()
        if (packageName.isEmpty()) {
            Log.e(TAG, "empty packageName extra from intent, unable to load permissions")
            finish()
            return
        }
        viewModel.loadPermissionsForPackage(getPackageNameExtra())

        val root = ComposeView(this)
        root.setContent {
            Box(modifier = Modifier.background(Color.Black)) {
                WearViewAppPermissionsScreen(viewModel)
            }
        }
        setContentView(root)
    }

    private fun getPackageNameExtra(): String {
        return intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
    }
}
