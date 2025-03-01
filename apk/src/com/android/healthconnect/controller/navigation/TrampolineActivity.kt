/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.navigation

import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.Intent.EXTRA_REASON
import android.health.connect.HealthConnectManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.MainActivity
import com.android.healthconnect.controller.data.DataManagementActivity
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import com.android.healthconnect.controller.onboarding.OnboardingActivity.Companion.shouldRedirectToOnboardingActivity
import com.android.healthconnect.controller.onboarding.SkipOnboardingActivity
import com.android.healthconnect.controller.onboarding.SkipOnboardingActivity.Companion.SKIP_HEALTH_CONNECT_ONBOARDING
import com.android.healthconnect.controller.permissions.app.wear.WearViewAppInfoPermissionsActivity
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearSettingsPermissionActivity
import com.android.healthconnect.controller.permissions.shared.SettingsActivity
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.activity.EmbeddingUtils.maybeRedirectIntoTwoPaneSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A proxy activity that starts health connect screens. This activity validates intents and handles
 * errors starting health connect screens.
 */
@AndroidEntryPoint(FragmentActivity::class)
class TrampolineActivity : Hilt_TrampolineActivity() {

    companion object {
        private const val TAG = "TrampolineActivity"
        private const val REASON_PRIVACY_DASHBOARD = "privacy_dashboard"
    }

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(
            WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
        )
        // Handles unsupported devices and user profiles.
        if (!deviceInfoUtils.isHealthConnectAvailable(this)) {
            Log.e(TAG, "Health connect is not available for this user or hardware, finishing!")
            finish()
            return
        }
        val isOnWatch = deviceInfoUtils.isOnWatch(this)

        // Handles large screen support in settings.
        if (!isOnWatch && maybeRedirectIntoTwoPaneSettings(this)) {
            finish()
            return
        }

        val targetIntent =
            if (isOnWatch) {
                getWearTargetIntent()
            } else {
                getHandheldTargetIntent()
            }

        if (SKIP_HEALTH_CONNECT_ONBOARDING == intent.action) {
            startActivity(
                Intent(this, SkipOnboardingActivity::class.java).apply {
                    action = SKIP_HEALTH_CONNECT_ONBOARDING
                }
            )
            finish()
            return
        }

        // Handles showing Health Connect Onboarding.
        // Do not show onboarding UI on watch.
        if (!isOnWatch && shouldRedirectToOnboardingActivity(this)) {
            startActivity(OnboardingActivity.createIntent(this, targetIntent))
            finish()
            return
        }

        startActivity(targetIntent)
        finish()
    }

    private fun getHandheldTargetIntent(): Intent {
        return when (intent.action) {
            HealthConnectManager.ACTION_HEALTH_HOME_SETTINGS -> {
                Intent(this, MainActivity::class.java)
            }
            HealthConnectManager.ACTION_MANAGE_HEALTH_DATA -> {
                Intent(this, DataManagementActivity::class.java)
            }
            HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS -> {
                val extraPackageName: String? = intent.getStringExtra(EXTRA_PACKAGE_NAME)

                Intent(this, SettingsActivity::class.java).apply {
                    if (extraPackageName != null) {
                        putExtra(EXTRA_PACKAGE_NAME, extraPackageName)
                    }
                }
            }
            else -> {
                // Default to open Health Connect MainActivity
                Intent(this, MainActivity::class.java)
            }
        }
    }

    // Returns an intent to handle Wear request.
    private fun getWearTargetIntent(): Intent {
        if (intent.action != HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS) {
            Log.e(TAG, "getWearTargetIntent() has unexpected intent action: " + intent.action)
            // If unexpected intent, fall default to WearPermissionManagerActivity.
        }
        val extraPackageName: String? = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val extraReason: String? = intent.getStringExtra(EXTRA_REASON)

        return if (extraPackageName != null) {
            // AppInfo page.
            Intent(this, WearViewAppInfoPermissionsActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, extraPackageName)
            }
        } else if (extraReason == REASON_PRIVACY_DASHBOARD) {
            // PrivacyDashboard page.
            Intent(this, WearSettingsPermissionActivity::class.java).apply {
                putExtra(EXTRA_REASON, REASON_PRIVACY_DASHBOARD)
            }
        } else {
            // PermissionManager page.
            Intent(this, WearSettingsPermissionActivity::class.java)
        }
    }
}
