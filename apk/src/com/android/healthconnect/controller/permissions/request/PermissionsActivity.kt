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

package com.android.healthconnect.controller.permissions.request

import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.PackageManager
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS
import android.content.pm.PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED
import android.health.connect.HealthPermissions
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.maybeShowWhatsNewDialog
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.showDataRestoreInProgressDialog
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.showMigrationInProgressDialog
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.showMigrationPendingDialog
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import com.android.healthconnect.controller.onboarding.OnboardingActivity.Companion.shouldRedirectToOnboardingActivity
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.permissions.request.wear.WearGrantPermissionsActivity
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.activity.EmbeddingUtils.maybeRedirectIntoTwoPaneSettings
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled
import com.android.healthfitness.flags.Flags
import com.android.settingslib.collapsingtoolbar.EdgeToEdgeUtils
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Permissions activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class PermissionsActivity : Hilt_PermissionsActivity() {

    companion object {
        private const val TAG = "PermissionsActivity"
    }

    @Inject lateinit var logger: HealthConnectLogger

    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase

    private val requestPermissionsViewModel: RequestPermissionViewModel by viewModels()

    private val migrationViewModel: MigrationViewModel by viewModels()

    private val openOnboardingActivity =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_CANCELED) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.enable(this)
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_HealthConnect_Expressive)
        }
        super.onCreate(savedInstanceState)

        // If device is enabled on watch, redirect to WearGrantPermissionsActivity.
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            if (!Flags.replaceBodySensorPermissionEnabled()) {
                Log.e(TAG, "Health connect is not available on watch, finishing!")
                finish()
                return
            }
            val wearIntent =
                Intent(this, WearGrantPermissionsActivity::class.java).apply {
                    putExtra(EXTRA_PACKAGE_NAME, getPackageNameExtra())
                    putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, getPermissionStrings())
                }
            startActivity(wearIntent)
            finish()
            return
        }

        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS)

        // Handles unsupported devices and user profiles.
        if (!deviceInfoUtils.isHealthConnectAvailable(this)) {
            Log.e(TAG, "Health connect is not available for this user or hardware, finishing!")
            finish()
            return
        }

        if (!intent.hasExtra(EXTRA_PACKAGE_NAME)) {
            Log.e(TAG, "Invalid Intent Extras, finishing")
            finish()
            return
        }

        if (maybeRedirectIntoTwoPaneSettings(this)) {
            return
        }

        setContentView(R.layout.activity_permissions)

        // Some actions don't apply to apps that get health permissions via split-permission.
        if (!isRequestFromSplitPermission()) {
            // Check if we need to show onboarding screen.
            if (savedInstanceState == null && shouldRedirectToOnboardingActivity(this)) {
                openOnboardingActivity.launch(OnboardingActivity.createIntent(this))
            }

            // Check that app has declared rationale intent.
            val rationaleIntentDeclared =
                healthPermissionReader.isRationaleIntentDeclared(getPackageNameExtra())
            if (!rationaleIntentDeclared) {
                Log.e(TAG, "App should support rationale intent, finishing!")
                finish()
            }
        }

        requestPermissionsViewModel.init(getPackageNameExtra(), getPermissionStrings())
        if (
            requestPermissionsViewModel.isAnyPermissionUserFixed(
                getPackageNameExtra(),
                getPermissionStrings(),
            )
        ) {
            if (isPersonalHealthRecordEnabled()) {
                // First check if we are already in a permission request flow.
                // Without this check, if any permissions from the previous screen
                // were USER_FIXED, we would terminate the request without showing
                // the subsequent screens.
                if (
                    !requestPermissionsViewModel.isFitnessPermissionRequestConcluded() &&
                        !requestPermissionsViewModel.isMedicalPermissionRequestConcluded()
                ) {
                    Log.e(TAG, "App has at least one USER_FIXED permission, finishing!")
                    requestPermissionsViewModel.updatePermissionGrants()
                    handlePermissionResults()
                }
            } else {
                Log.e(TAG, "App has at least one USER_FIXED permission, finishing!")
                requestPermissionsViewModel.requestHealthPermissions(getPackageNameExtra())
                handlePermissionResults()
            }
        }

        requestPermissionsViewModel.permissionsActivityState.observe(this) { screenState ->
            when (screenState) {
                is PermissionsActivityState.ShowMedical -> {
                    if (screenState.isWriteOnly) {
                        showFragment(MedicalWritePermissionFragment())
                    } else {
                        showFragment(MedicalPermissionsFragment())
                    }
                }
                is PermissionsActivityState.ShowFitness -> {
                    showFragment(FitnessPermissionsFragment())
                }
                is PermissionsActivityState.ShowAdditional -> {
                    if (screenState.singlePermission) {
                        showFragment(SingleAdditionalPermissionFragment())
                    } else {
                        showFragment(CombinedAdditionalPermissionsFragment())
                    }
                }
                else -> {
                    // No permissions
                    requestPermissionsViewModel.updatePermissionGrants()
                    handlePermissionResults()
                }
            }
        }

        migrationViewModel.migrationState.observe(this) { migrationState ->
            when (migrationState) {
                is MigrationViewModel.MigrationFragmentState.WithData -> {
                    maybeShowMigrationDialog(migrationState.migrationRestoreState)
                }
                else -> {
                    // do nothing
                }
            }
        }
    }

    private fun maybeShowMigrationDialog(migrationRestoreState: MigrationRestoreState) {
        val (migrationUiState, dataRestoreUiState, dataErrorState) = migrationRestoreState

        if (dataRestoreUiState == DataRestoreUiState.IN_PROGRESS) {
            showDataRestoreInProgressDialog(this) { _, _ -> finish() }
        } else if (migrationUiState == MigrationUiState.IN_PROGRESS) {
            showMigrationInProgressDialog(
                this,
                getString(
                    R.string.migration_in_progress_permissions_dialog_content,
                    requestPermissionsViewModel.appMetadata.value?.appName,
                ),
            ) { _, _ ->
                finish()
            }
        } else if (
            migrationUiState in
                listOf(
                    MigrationUiState.ALLOWED_PAUSED,
                    MigrationUiState.ALLOWED_NOT_STARTED,
                    MigrationUiState.MODULE_UPGRADE_REQUIRED,
                    MigrationUiState.APP_UPGRADE_REQUIRED,
                )
        ) {
            showMigrationPendingDialog(
                this,
                getString(
                    R.string.migration_pending_permissions_dialog_content,
                    requestPermissionsViewModel.appMetadata.value?.appName,
                ),
                null,
            ) { _, _ ->
                if (requestPermissionsViewModel.isFitnessPermissionRequestConcluded()) {
                    requestPermissionsViewModel.updateAdditionalPermissions(false)
                    requestPermissionsViewModel.requestAdditionalPermissions(getPackageNameExtra())
                } else {
                    requestPermissionsViewModel.updateFitnessPermissions(false)
                    requestPermissionsViewModel.requestFitnessPermissions(getPackageNameExtra())
                }

                handlePermissionResults()
                finish()
            }
        } else if (migrationUiState == MigrationUiState.COMPLETE) {
            maybeShowWhatsNewDialog(this)
        }
    }

    /**
     * Returns true if the request is being made as the result of a split permission from
     * BODY_SENSORS call.
     */
    private fun isRequestFromSplitPermission(): Boolean {
        if (!Flags.replaceBodySensorPermissionEnabled()) {
            return false
        }

        val declaredPermissions =
            healthPermissionReader.getDeclaredHealthPermissions(getPackageNameExtra())
        // Split permission only applies to READ_HEART_RATE.
        if (!declaredPermissions.contains(HealthPermissions.READ_HEART_RATE)) {
            return false
        }

        // If there are other health permissions (other than READ_HEALTH_DATA_IN_BACKGROUND)
        // don't consider this a pure split-permission request.
        if (declaredPermissions.size > 2) {
            return false
        }

        val declaresBackgroundPermission =
            declaredPermissions.contains(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND)
        // If there are two health permissions declared, make sure the other is
        // READ_HEALTH_DATA_IN_BACKGROUND.
        if (declaredPermissions.size == 2 && !declaresBackgroundPermission) {
            return false
        }

        val permissionsToCheck =
            if (declaresBackgroundPermission) {
                listOf(
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                )
            } else {
                listOf(HealthPermissions.READ_HEART_RATE)
            }

        // Check the READ_HEART_RATE permission flag to see if it's a split-permission.
        val permissionToFlags =
            getHealthPermissionsFlagsUseCase(getPackageNameExtra(), permissionsToCheck)

        if (declaresBackgroundPermission) {
            // READ_HEALTH_DATA_IN_BACKGROUND is not due to split-permission.
            if (
                permissionToFlags.get(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND)?.let { flags
                    ->
                    (flags and FLAG_PERMISSION_REVOKE_WHEN_REQUESTED) == 0
                } ?: true
            ) {
                return false
            }
        }

        // READ_HEART_RATE is not due to split-permission.
        return permissionToFlags.get(HealthPermissions.READ_HEART_RATE)?.let { flags ->
            (flags and FLAG_PERMISSION_REVOKE_WHEN_REQUESTED) != 0
        } ?: false
    }

    private fun handlePermissionResults(resultCode: Int = RESULT_OK) {
        val results = requestPermissionsViewModel.getPermissionGrants()
        val grants = mutableListOf<Int>()
        val permissionStrings = mutableListOf<String>()

        for ((permission, state) in results) {
            if (state == PermissionState.GRANTED) {
                grants.add(PackageManager.PERMISSION_GRANTED)
            } else {
                grants.add(PackageManager.PERMISSION_DENIED)
            }

            permissionStrings.add(permission.toString())
        }

        val result = Intent()
        result.putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissionStrings.toTypedArray())
        result.putExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS, grants.toIntArray())
        setResult(resultCode, result)
        finish()
    }

    private fun getPermissionStrings(): Array<out String> {
        return intent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES).orEmpty()
    }

    private fun getPackageNameExtra(): String {
        return intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.permission_content, fragment)
            .commit()
    }
}
