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
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.permissions.shared.DisconnectDialogFragment
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromHealthPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.AppHeaderPreference
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment to show granted/revoked health permissions for and app. It is used as an entry point
 * from PermissionController.
 */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class SettingsManageAppPermissionsFragment : Hilt_SettingsManageAppPermissionsFragment() {

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val PERMISSION_HEADER = "manage_app_permission_header"
    }

    init {
        this.setPageName(PageName.MANAGE_PERMISSIONS_PAGE)
    }

    private lateinit var packageName: String
    private lateinit var appName: String
    private val viewModel: AppPermissionViewModel by viewModels()
    private val permissionMap: MutableMap<HealthPermission, SwitchPreference> = mutableMapOf()

    private val allowAllPreference: HealthMainSwitchPreference? by lazy {
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
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.settings_manage_app_permission_screen, rootKey)

        allowAllPreference?.logNameActive = PermissionsElement.ALLOW_ALL_SWITCH
        allowAllPreference?.logNameInactive = PermissionsElement.ALLOW_ALL_SWITCH
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
                switchPreference.isChecked = healthPermission in granted
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
                if (!viewModel.revokeAllPermissions(packageName)) {
                    Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT)
                        .show()
                }
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

        permissions
            .sortedBy {
                requireContext()
                    .getString(
                        fromPermissionType(it.healthPermissionType)
                            .uppercaseLabel)
            }
            .forEach { permission ->
                val category =
                    if (permission.permissionsAccessType == PermissionsAccessType.READ) {
                        readPermissionCategory
                    } else {
                        writePermissionCategory
                    }
                val switchPreference =
                    HealthSwitchPreference(requireContext()).also {
                        val healthCategory =
                            fromHealthPermissionType(permission.healthPermissionType)
                        it.setIcon(healthCategory.icon())
                        it.setTitle(
                            fromPermissionType(
                                    permission.healthPermissionType)
                                .uppercaseLabel)
                        it.logNameActive = PermissionsElement.PERMISSION_SWITCH
                        it.logNameInactive = PermissionsElement.PERMISSION_SWITCH
                        it.setOnPreferenceChangeListener { _, newValue ->
                            val checked = newValue as Boolean
                            val permissionUpdated =
                                viewModel.updatePermission(packageName, permission, checked)
                            if (!permissionUpdated) {
                                Toast.makeText(
                                        requireContext(),
                                        R.string.default_error,
                                        Toast.LENGTH_SHORT)
                                    .show()
                            }
                            permissionUpdated
                        }
                    }
                permissionMap[permission] = switchPreference
                category?.addPreference(switchPreference)
            }
    }
}
