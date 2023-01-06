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
package com.android.healthconnect.controller

import android.content.Context
import android.content.Intent
import android.healthconnect.HealthConnectManager
import android.os.Bundle
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import dagger.hilt.android.AndroidEntryPoint

/** Entry point activity for Health Connect. */
@AndroidEntryPoint(CollapsingToolbarBaseActivity::class)
class MainActivity : Hilt_MainActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle(R.string.app_label)
        /** Displaying onboarding screen if user is opening Health Connect app for the first time */
        val sharedPreference = getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
        val previouslyOpened =
            sharedPreference.getBoolean(getString(R.string.previously_opened), false)
        if (!previouslyOpened) {
            val editor = sharedPreference.edit()
            editor.putBoolean(getString(R.string.previously_opened), true)
            editor.apply()
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (intent.action == HealthConnectManager.ACTION_MANAGE_HEALTH_DATA) {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_deeplink_to_healthDataCategoriesFragment)
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
