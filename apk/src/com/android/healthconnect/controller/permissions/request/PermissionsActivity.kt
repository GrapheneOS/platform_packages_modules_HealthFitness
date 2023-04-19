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
package com.android.healthconnect.controller.permissions.request

import android.app.Activity
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.PackageManager
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS
import android.health.connect.HealthConnectDataState
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.DataMigrationState
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.onboarding.OnboardingActivity.Companion.maybeRedirectToOnboardingActivity
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.activity.EmbeddingUtils.maybeRedirectIntoTwoPaneSettings
import com.android.healthconnect.controller.utils.logging.ErrorPageElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    companion object {
        private const val TAG = "PermissionsActivity"
    }

    @Inject lateinit var logger: HealthConnectLogger
    private val viewModel: RequestPermissionViewModel by viewModels()
    private val migrationViewModel: MigrationViewModel by viewModels()
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_permissions)

        if (!intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            Log.e(TAG, "Invalid Intent Extras, finishing")
            finish()
        }

        if (maybeRedirectIntoTwoPaneSettings(this)) {
            return
        }

        if (maybeRedirectToOnboardingActivity(this, intent)) {
            return
        }

        val rationalIntentDeclared =
            healthPermissionReader.isRationalIntentDeclared(getPackageNameExtra())
        if (!rationalIntentDeclared) {
            Log.e(TAG, "App should support rational intent, finishing!")
            finish()
        }

        viewModel.init(getPackageNameExtra(), getPermissionStrings())
        viewModel.permissionsList.observe(this) { notGrantedPermissions ->
            if (notGrantedPermissions.isEmpty()) {
                handleResults(viewModel.request(getPackageNameExtra()))
            }
        }
        migrationViewModel.migrationState.observe(this) { migrationState ->
            maybeShowMigrationDialog(migrationState)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.permission_content, PermissionsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        setupAllowButton()
        setupCancelButton()
    }

    private fun setupCancelButton() {
        val cancelButton: View = findViewById(R.id.cancel)
        logger.logImpression(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)

        cancelButton.setOnClickListener {
            logger.logInteraction(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
            viewModel.updatePermissions(false)
            handleResults(viewModel.request(getPackageNameExtra()))
        }
    }

    private fun setupAllowButton() {
        val allowButton: View = findViewById(R.id.allow)
        logger.logImpression(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
        viewModel.grantedPermissions.observe(this) { grantedPermissions ->
            allowButton.isEnabled = grantedPermissions.isNotEmpty()
        }
        allowButton.setOnClickListener {
            logger.logInteraction(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
            handleResults(viewModel.request(getPackageNameExtra()))
        }
    }

    private fun maybeShowMigrationDialog(migrationState: @DataMigrationState Int) {
        when (migrationState) {
            // TODO (b/273745755) Expose real UI states
            HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS -> {
                // TODO (b/275853443) Uncomment when CTS fixed
                // showMigrationInProgressDialog()
            }
            HealthConnectDataState.MIGRATION_STATE_ALLOWED -> {
                // TODO (b/275853443) Uncomment when CTS fixed
                // showMigrationPendingDialog()
            }
            HealthConnectDataState.MIGRATION_STATE_APP_UPGRADE_REQUIRED -> {
                // TODO (b/275853443) Uncomment when CTS fixed
                // showMigrationPendingDialog()
            }
            HealthConnectDataState.MIGRATION_STATE_MODULE_UPGRADE_REQUIRED -> {
                // TODO (b/275853443) Uncomment when CTS fixed
                // showMigrationPendingDialog()
            }
        }
    }

    private fun showMigrationPendingDialog() {
        val message =
            getString(
                R.string.migration_pending_permissions_dialog_content,
                viewModel.appMetadata.value?.appName)
        AlertDialogBuilder(this)
            .setLogName(ErrorPageElement.UNKNOWN_ELEMENT)
            .setTitle(R.string.migration_pending_permissions_dialog_title)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel, ErrorPageElement.UNKNOWN_ELEMENT) { _, _ ->
                viewModel.updatePermissions(false)
                handleResults(viewModel.request(getPackageNameExtra()))
                finish()
            }
            .setPositiveButton(
                R.string.migration_pending_permissions_dialog_button_continue,
                ErrorPageElement.UNKNOWN_ELEMENT)
            .create()
            .show()
    }

    private fun showMigrationInProgressDialog() {
        val message =
            getString(
                R.string.migration_in_progress_permissions_dialog_content,
                viewModel.appMetadata.value?.appName)
        AlertDialogBuilder(this)
            .setLogName(ErrorPageElement.UNKNOWN_ELEMENT)
            .setTitle(R.string.migration_in_progress_permissions_dialog_title)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(
                R.string.migration_in_progress_permissions_dialog_button_got_it,
                ErrorPageElement.UNKNOWN_ELEMENT) { _, _ ->
                    finish()
                }
            .create()
            .show()
    }

    fun handleResults(results: Map<HealthPermission, PermissionState>) {
        val grants =
            results.values
                .map { permissionSelection ->
                    if (PermissionState.GRANTED == permissionSelection) {
                        PackageManager.PERMISSION_GRANTED
                    } else {
                        PackageManager.PERMISSION_DENIED
                    }
                }
                .toIntArray()
        val result = Intent()
        result.putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, getPermissionStrings())
        result.putExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS, grants)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun getPermissionStrings(): Array<out String> {
        return intent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES).orEmpty()
    }

    private fun getPackageNameExtra(): String {
        return intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
    }
}
