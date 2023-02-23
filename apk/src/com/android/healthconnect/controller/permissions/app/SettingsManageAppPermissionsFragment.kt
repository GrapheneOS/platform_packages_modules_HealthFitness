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
package com.android.healthconnect.controller.permissions.app

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.permissions.shared.DisconnectDialogFragment
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromHealthPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.AppHeaderPreference
import com.android.settingslib.widget.MainSwitchPreference
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment to show granted/revoked health permissions for and app. It is used as an entry point
 * from PermissionController.
 */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class SettingsManageAppPermissionsFragment : Hilt_SettingsManageAppPermissionsFragment() {

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val PERMISSION_HEADER = "manage_app_permission_header"
    }

    private lateinit var packageName: String
    private lateinit var appName: String
    private val viewModel: AppPermissionViewModel by viewModels()
    private val permissionMap: MutableMap<HealthPermission, SwitchPreference> = mutableMapOf()

    private val allowAllPreference: MainSwitchPreference? by lazy {
        preferenceScreen.findPreference(ALLOW_ALL_PREFERENCE)
    }

    private val readPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }

    private val writePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }

    private val header: AppHeaderPreference? by lazy {
        preferenceScreen.findPreference(PERMISSION_HEADER)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_manage_app_permission_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
            requireArguments().getString(EXTRA_PACKAGE_NAME) != null) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
        if (requireArguments().containsKey(EXTRA_APP_NAME) &&
            requireArguments().getString(EXTRA_APP_NAME) != null) {
            appName = requireArguments().getString(EXTRA_APP_NAME)!!
        }

        viewModel.loadAppInfo(packageName)
        viewModel.loadForPackage(packageName)
        viewModel.appPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        viewModel.grantedPermissions.observe(viewLifecycleOwner) { granted ->
            permissionMap.forEach { (healthPermission, switchPreference) ->
                if (healthPermission in granted && !switchPreference.isChecked) {
                    switchPreference.isChecked = true
                }
            }
        }

        viewModel.revokeAllPermissionsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AppPermissionViewModel.RevokeAllState.Loading -> {
                    showLoadingDialog()
                }
                else -> {
                    dismissLoadingDialog()
                }
            }
        }
        setupAllowAllPreference()
        setupHeader()
    }

    private fun setupHeader() {
        viewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            appName = appMetadata.appName
            packageName = appMetadata.packageName
            header?.apply {
                setIcon(appMetadata.icon)
                setTitle(appMetadata.appName)
            }
        }
    }

    private fun setupAllowAllPreference() {
        allowAllPreference?.addOnSwitchChangeListener { preference, grantAll ->
            if (preference.isPressed) {
                if (grantAll) {
                    viewModel.grantAllPermissions(packageName)
                } else {
                    showRevokeAllPermissions()
                }
            }
        }
        viewModel.allAppPermissionsGranted.observe(viewLifecycleOwner) { isAllGranted ->
            allowAllPreference?.isChecked = isAllGranted
        }
    }

    private fun showRevokeAllPermissions() {
        childFragmentManager.setFragmentResultListener(
            DisconnectDialogFragment.DISCONNECT_CANCELED_EVENT, this) { _, _ ->
                allowAllPreference?.isChecked = true
            }

        childFragmentManager.setFragmentResultListener(
            DisconnectDialogFragment.DISCONNECT_ALL_EVENT, this) { _, bundle ->
                viewModel.revokeAllPermissions(packageName)
                if (bundle.containsKey(DisconnectDialogFragment.KEY_DELETE_DATA) &&
                    bundle.getBoolean(DisconnectDialogFragment.KEY_DELETE_DATA)) {
                    viewModel.deleteAppData(packageName, appName)
                }
            }

        DisconnectDialogFragment(appName = appName, enableDeleteData = false)
            .show(childFragmentManager, DisconnectDialogFragment.TAG)
    }

    private fun updatePermissions(permissions: List<HealthPermission>) {
        readPermissionCategory?.removeAll()
        writePermissionCategory?.removeAll()
        permissionMap.clear()

        permissions.forEach { permission ->
            val category =
                if (permission.permissionsAccessType == PermissionsAccessType.READ) {
                    readPermissionCategory
                } else {
                    writePermissionCategory
                }
            val switchPreference =
                SwitchPreference(requireContext()).also {
                    val healthCategory = fromHealthPermissionType(permission.healthPermissionType)
                    it.setIcon(healthCategory.icon())
                    it.setTitle(
                        HealthPermissionStrings.fromPermissionType(permission.healthPermissionType)
                            .uppercaseLabel)
                    it.setOnPreferenceChangeListener { _, newValue ->
                        val checked = newValue as Boolean
                        viewModel.updatePermission(packageName, permission, checked)
                        true
                    }
                }
            permissionMap[permission] = switchPreference
            category?.addPreference(switchPreference)
        }
    }
}
