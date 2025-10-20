/*
 * Copyright (C) 2025 The Android Open Source Project
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
 */

package android.healthconnect.testing.cts.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.HealthConnectManager
import android.healthconnect.testing.cts.ui.UiTestUtils.pressBack
import android.healthconnect.testing.cts.ui.UiTestUtils.waitDataActivityDisplayed
import android.healthconnect.testing.cts.ui.UiTestUtils.waitForIdle
import android.healthconnect.testing.cts.ui.UiTestUtils.waitHomeScreenDisplayed
import android.healthconnect.testing.cts.ui.UiTestUtils.waitManageHealthPermissionActivityDisplayed
import android.healthconnect.testing.cts.ui.UiTestUtils.waitRequestPermissionsDisplayed
import com.android.compatibility.common.util.SystemUtil

/** A class that provides a way to launch the Health Connect [MainActivity] in tests. */
object ActivityLauncher {
    /** Launches the Main activity and exits it once [block] completes. */
    fun Context.launchMainActivity(block: () -> Unit) {
        val intent =
            Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        executeBlockAndExit(block) {
            startActivity(intent)
            waitForIdle()
            waitHomeScreenDisplayed()
        }
    }

    fun Context.launchDataActivity(block: () -> Unit) {
        val intent =
            Intent("android.health.connect.action.MANAGE_HEALTH_DATA")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        executeBlockAndExit(block) {
            startActivity(intent)
            waitForIdle()
            waitDataActivityDisplayed()
        }
    }

    fun Context.launchRequestPermissionActivity(
        packageName: String = UiTestUtils.TEST_APP_PACKAGE_NAME,
        permissions: List<String>,
        block: () -> Unit,
    ) {
        val intent =
            Intent(HealthConnectManager.ACTION_REQUEST_HEALTH_PERMISSIONS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES, permissions.toTypedArray())
                putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            }
        executeBlockAndExit(block) {
            SystemUtil.runWithShellPermissionIdentity(
                {
                    startActivity(intent)
                    waitForIdle()
                    waitRequestPermissionsDisplayed()
                },
                Manifest.permission.GRANT_RUNTIME_PERMISSIONS,
            )
        }
    }

    fun Context.launchManageHealthPermissionActivity(block: () -> Unit) {
        val intent =
            Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        executeBlockAndExit(block) {
            SystemUtil.runWithShellPermissionIdentity(
                {
                    startActivity(intent)
                    waitForIdle()
                    waitManageHealthPermissionActivityDisplayed()
                },
                Manifest.permission.GRANT_RUNTIME_PERMISSIONS,
            )
        }
    }

    private fun executeBlockAndExit(block: () -> Unit, launchActivity: () -> Unit) {
        waitForIdle()
        launchActivity()
        waitForIdle()
        block()
        pressBack()
        waitForIdle()
    }
}
