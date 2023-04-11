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
package com.android.healthconnect.controller

import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectDataState
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.findNavController
import com.android.healthconnect.controller.migration.DataMigrationState
import com.android.healthconnect.controller.migration.MigrationActivity
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.navigation.DestinationChangedListener
import com.android.healthconnect.controller.onboarding.OnboardingActivity.Companion.maybeRedirectToOnboardingActivity
import com.android.healthconnect.controller.utils.activity.EmbeddingUtils.maybeRedirectIntoTwoPaneSettings
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Entry point activity for Health Connect. */
@AndroidEntryPoint(CollapsingToolbarBaseActivity::class)
class MainActivity : Hilt_MainActivity() {
    @Inject lateinit var logger: HealthConnectLogger
    private val migrationViewModel: MigrationViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setTitle(R.string.app_label)

        if (maybeRedirectIntoTwoPaneSettings(this)) {
            return
        }

        if (maybeRedirectToOnboardingActivity(this, intent)) {
            return
        }

        //         TODO (b/271377785) uncomment for migration flows
        //        migrationViewModel.migrationState.observe(this) { migrationState ->
        //            maybeNavigateToMigration(migrationState)
        //        }
    }

    override fun onStart() {
        super.onStart()
        findNavController(R.id.nav_host_fragment)
            .addOnDestinationChangedListener(DestinationChangedListener(this))
    }

    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment)
        if (!navController.popBackStack()) {
            finish()
        }
    }

    override fun onNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        if (!navController.popBackStack()) {
            finish()
        }
        return true
    }

    // TODO (b/270864219): implement interaction logging for the menu button
    //    override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
    //        logger.logInteraction(ElementName.TOOLBAR_SETTINGS_BUTTON)
    //        return super.onMenuOpened(featureId, menu)
    //    }

    private fun maybeNavigateToMigration(migrationState: @DataMigrationState Int) {
        val migrationUpdatesNeeded =
            (migrationState != HealthConnectDataState.MIGRATION_STATE_IDLE) &&
                (migrationState != HealthConnectDataState.MIGRATION_STATE_COMPLETE)

        if (migrationState == HealthConnectDataState.MIGRATION_STATE_MODULE_UPGRADE_REQUIRED) {
            val sharedPreference =
                getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
            val moduleUpdateSeen =
                sharedPreference.getBoolean(getString(R.string.module_update_needed_seen), false)

            if (!moduleUpdateSeen) {
                startMigrationActivity()
            }
        } else if (migrationState == HealthConnectDataState.MIGRATION_STATE_APP_UPGRADE_REQUIRED) {
            val sharedPreference =
                getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
            val appUpdateSeen =
                sharedPreference.getBoolean(getString(R.string.app_update_needed_seen), false)

            if (!appUpdateSeen) {
                startMigrationActivity()
            }
        } else if (migrationUpdatesNeeded) {
            startMigrationActivity()
        }
    }

    private fun startMigrationActivity() {
        val intent = Intent(this, MigrationActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        startActivity(intent)
        finish()
    }
}
