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
 */

package android.healthconnect.cts.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.healthconnect.cts.utils.AssumptionCheckerRule
import android.healthconnect.cts.utils.DeviceSupportUtils
import android.server.wm.WindowManagerStateHelper
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.android.compatibility.common.util.FreezeRotationRule
import com.android.compatibility.common.util.SystemUtil.eventually
import com.android.compatibility.common.util.SystemUtil.runShellCommandOrThrow
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule

open class HealthConnectBaseTest {
    @get:Rule val freezeRotationRule = FreezeRotationRule()

    @get:Rule
    var mSupportedHardwareRule =
        AssumptionCheckerRule(
            { DeviceSupportUtils.isHealthConnectFullySupported() },
            "Tests should run on supported hardware only.",
        )

    companion object {
        private const val TAG = "HealthConnectBaseTest"
    }

    protected val context: Context = ApplicationProvider.getApplicationContext()
    private val wmState = WindowManagerStateHelper()

    @Before
    fun setUpClass() {
        // If the status bar is showing, hide it (swipe it back up)
        runShellCommandOrThrow("cmd statusbar collapse")

        unlockDevice()
    }

    /** This assumes that the lock method is SWIPE or NONE. */
    private fun unlockDevice() {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        // Wakes up the device in case setup failed
        runShellCommandOrThrow("input keyevent KEYCODE_WAKEUP")
        runShellCommandOrThrow("wm dismiss-keyguard")
        // Check if there is a lock screen set (we assume SWIPE)
        if ("false".equals(runShellCommandOrThrow("cmd lock_settings get-disabled"))) {
            // Unlock screen only when it's lock settings enabled to prevent showing "wallpaper
            // picker" which may cover another UI elements on freeform window configuration.
            Log.i(
                TAG,
                "keyguardManager.isKeyguardLocked (aka is keyguard showing) = ${keyguardManager.isKeyguardLocked}",
            )
            Log.i(TAG, "Lock screen not disabled, send unlock screen event")
            runShellCommandOrThrow("input keyevent 82")
        } else {
            Log.i(TAG, "Lock screen disabled, screen should be awake")
        }

        eventually {
            assertWithMessage("device is locked").that(keyguardManager.isDeviceLocked).isFalse()
            assertWithMessage("keyguard is locked").that(keyguardManager.isKeyguardLocked).isFalse()
        }
    }

    fun assertPermissionGranted(permission: String, packageName: String) {
        assertWithMessage("$permission for $packageName is not granted")
            .that(context.packageManager.checkPermission(permission, packageName))
            .isEqualTo(PackageManager.PERMISSION_GRANTED)
    }

    fun assertPermissionDenied(permission: String, packageName: String) {
        assertWithMessage("$permission for $packageName is not denied")
            .that(context.packageManager.checkPermission(permission, packageName))
            .isEqualTo(PackageManager.PERMISSION_DENIED)
    }
}
