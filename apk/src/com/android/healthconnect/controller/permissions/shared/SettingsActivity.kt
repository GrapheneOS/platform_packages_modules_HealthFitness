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
package com.android.healthconnect.controller.permissions.shared

import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.Intent.EXTRA_REASON
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.navigation.DestinationChangedListener
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.app.wear.WearViewAppInfoPermissionsActivity
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearSettingsPermissionActivity
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.activity.EmbeddingUtils.maybeRedirectIntoTwoPaneSettings
import com.android.modules.utils.build.SdkLevel
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint(CollapsingToolbarBaseActivity::class)
class SettingsActivity : Hilt_SettingsActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
        private const val REASON_PRIVACY_DASHBOARD = "privacy_dashboard"
    }

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private val viewModel: AppPermissionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SdkLevel.isAtLeastB() && deviceInfoUtils.isOnWatch(this)) {
            startActivity(getWearTargetIntent())
            finish()
            return
        }

        // Allow profiles, as health permissions can be used outside Health Connect.
        if (!deviceInfoUtils.isHardwareSupported(this)) {
            Log.e(TAG, "Health connect is not available for this hardware, finishing!")
            finish()
            return
        }

        if (maybeRedirectIntoTwoPaneSettings(this)) {
            finish()
            return
        }

        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(
            WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
        )
        setContentView(R.layout.activity_settings)
        setTitle(R.string.permgrouplab_health)
        if (savedInstanceState == null && intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)!!
                    maybeNavigateToAppPermissions(
                        viewModel.shouldNavigateToAppPermissionsFragment(packageName),
                        packageName,
                    )
                }
            }
        }
    }

    private fun getWearTargetIntent(): Intent {
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

    override fun onStart() {
        super.onStart()
        findNavController(R.id.nav_host_fragment)
            .addOnDestinationChangedListener(DestinationChangedListener(this))
    }

    private fun maybeNavigateToAppPermissions(shouldNavigate: Boolean, packageName: String) {
        val navController = findNavController(R.id.nav_host_fragment)

        if (shouldNavigate) {
            val appPermissionType = healthPermissionReader.getAppPermissionsType(packageName)
            val navigationId: Int =
                when (appPermissionType) {
                    AppPermissionsType.FITNESS_PERMISSIONS_ONLY ->
                        R.id.action_deeplink_to_settingsFitnessApp
                    AppPermissionsType.MEDICAL_PERMISSIONS_ONLY ->
                        R.id.action_deeplink_to_settingsMedicalApp
                    AppPermissionsType.COMBINED_PERMISSIONS ->
                        R.id.action_deeplink_to_settingsCombinedApp
                }
            navController.navigate(
                navigationId,
                bundleOf(EXTRA_PACKAGE_NAME to intent.getStringExtra(EXTRA_PACKAGE_NAME)),
            )
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment)
        if (!navController.popBackStack()) {
            finish()
        }
    }

    override fun onNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        if (!navController.popBackStack()) {
            finish()
        }
        return true
    }
}
