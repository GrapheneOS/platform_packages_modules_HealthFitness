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
package com.android.healthconnect.controller.permissions.shared

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.HealthConnectManager
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.navigation.DestinationChangedListener
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(CollapsingToolbarBaseActivity::class)
class SettingsActivity : Hilt_SettingsActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle(R.string.permgrouplab_health)
    }

    override fun onStart() {
        super.onStart()
        /** Displaying onboarding screen if user is opening Health Connect app for the first time */
        val sharedPreference = getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
        val previouslyOpened =
            sharedPreference.getBoolean(getString(R.string.previously_opened), false)
        if (!previouslyOpened) {
            val onboardingIntent = Intent(this, OnboardingActivity::class.java)
            onboardingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val intentAfterOnboarding =
                Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS)
            if (intent.hasExtra(EXTRA_PACKAGE_NAME)) {
                intentAfterOnboarding.putExtra(
                    EXTRA_PACKAGE_NAME, intent.getStringExtra(EXTRA_PACKAGE_NAME)!!)
            }
            onboardingIntent.putExtra(Intent.EXTRA_INTENT, intentAfterOnboarding)
            startActivity(onboardingIntent)
            finish()
        }

        val navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener(DestinationChangedListener(this))
        if (intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            enforceRationalIntent(intent.getStringExtra(EXTRA_PACKAGE_NAME)!!)
            navController.navigate(
                R.id.action_deeplink_to_settingsManageAppPermissionsFragment,
                bundleOf(EXTRA_PACKAGE_NAME to intent.getStringExtra(EXTRA_PACKAGE_NAME)))
        }
    }

    private fun enforceRationalIntent(appPackageName: String) {
        val rationalIntentDeclared = healthPermissionReader.isRationalIntentDeclared(appPackageName)
        if (!rationalIntentDeclared) {
            Log.e(TAG, "App should support rational intent!")
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
