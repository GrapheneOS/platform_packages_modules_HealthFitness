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
package com.android.healthconnect.controller.permissions.connectedapps.settings

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedApp.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.connectedapps.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.connectedapps.PermissionHeaderPreference
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.utils.setTitle
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
    private val viewModel: AppPermissionViewModel by viewModels()

    private val allowAllPreference: SwitchPreference? by lazy {
        preferenceScreen.findPreference(ALLOW_ALL_PREFERENCE)
    }

    private val readPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }

    private val writePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }

    private val header : PermissionHeaderPreference? by lazy {
        preferenceScreen.findPreference(PERMISSION_HEADER)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_manage_app_permission_screen, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.health_permissions_title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
            requireArguments().getString(EXTRA_PACKAGE_NAME) != null) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
        val appMetadata = viewModel.loadAppInfo(packageName)
        header?.apply {
            setIcon(appMetadata.icon)
            setTitle(appMetadata.appName)
        }
        viewModel.loadForPackage(packageName)
        viewModel.appPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        allowAllPreference?.setOnPreferenceChangeListener { _, newValue ->
            val grant = newValue as Boolean
            if (grant) {
                viewModel.grantAllPermissions(packageName)
            } else {
                viewModel.revokeAllPermissions(packageName)
            }
            true
        }
        viewModel.allAppPermissionsGranted.observe(viewLifecycleOwner) { isAllGranted ->
            allowAllPreference?.isChecked = isAllGranted
        }
    }

    private fun updatePermissions(permissions: List<HealthPermissionStatus>) {
        readPermissionCategory?.removeAll()
        writePermissionCategory?.removeAll()

        permissions.forEach { permissionStatus ->
            val permission = permissionStatus.healthPermission
            val category =
                if (permission.permissionsAccessType == PermissionsAccessType.READ) {
                    readPermissionCategory
                } else {
                    writePermissionCategory
                }

            category?.addPreference(
                SwitchPreference(requireContext()).also {
                    it.setTitle(
                        HealthPermissionStrings.fromPermissionType(permission.healthPermissionType)
                            .uppercaseLabel)
                    it.isChecked = permissionStatus.isGranted
                    it.setOnPreferenceChangeListener { _, newValue ->
                        val checked = newValue as Boolean
                        viewModel.updatePermission(
                            packageName, permissionStatus.healthPermission, checked)
                        true
                    }
                })
        }
    }
}
