/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.permissions.connectedapps.wear

import android.content.Intent.EXTRA_REASON
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.AndroidEntryPoint

/** Wear Settings Permission activity for Health&Fitness. */
@AndroidEntryPoint(ComponentActivity::class)
class WearSettingsPermissionActivity : Hilt_WearSettingsPermissionActivity() {

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

        // If with EXTRA_REASON, show Privacy Dashboard; otherwise show Permission Manager.
        // They use the same UI layout and sequence except for labels:
        // Permission Manager shows "[xx] of [yy] apps allowed",
        // Privacy Dashboard shows "Used by xx apps", and "Accessed [time]".
        setContent {
            Box(modifier = Modifier.background(Color.Black)) {
                WearSettingsPermissionsNavGraph(!getReasonExtra().isEmpty())
            }
        }
    }

    private fun getReasonExtra(): String {
        return intent.getStringExtra(EXTRA_REASON).orEmpty()
    }

    companion object {
        private const val TAG = "WearSettingsPermissionActivity"
    }
}
