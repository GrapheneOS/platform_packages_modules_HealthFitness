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
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppMetadata
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.ALLOWED
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.DENIED
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppsViewModel
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment to show allowed and denied apps for health permissions. It is used as an entry point
 * from PermissionController.
 */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class SettingsManagePermissionFragment : Hilt_SettingsManagePermissionFragment() {

    companion object {
        const val ALLOWED_APPS_GROUP = "allowed_apps"
        const val DENIED_APPS_GROUP = "denied_apps"
    }

    private val allowedAppsGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(ALLOWED_APPS_GROUP)
    }

    private val deniedAppsGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(DENIED_APPS_GROUP)
    }

    private val viewModel: ConnectedAppsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_manage_permission_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.connectedApps.observe(viewLifecycleOwner) { connectedApps ->
            val connectedAppsGroup = connectedApps.groupBy { it.status }
            updateAllowedApps(connectedAppsGroup[ALLOWED].orEmpty())
            updateDeniedApps(connectedAppsGroup[DENIED].orEmpty())
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.app_label)
        viewModel.loadConnectedApps()
    }

    private fun updateAllowedApps(appsList: List<ConnectedAppMetadata>) {
        allowedAppsGroup?.removeAll()
        if (appsList.isEmpty()) {
            allowedAppsGroup?.addPreference(getNoAppsPreference(R.string.no_apps_allowed))
        } else {
            appsList.forEach { app -> allowedAppsGroup?.addPreference(getAppPreference(app)) }
        }
    }

    private fun updateDeniedApps(appsList: List<ConnectedAppMetadata>) {
        deniedAppsGroup?.removeAll()

        if (appsList.isEmpty()) {
            deniedAppsGroup?.addPreference(getNoAppsPreference(R.string.no_apps_denied))
        } else {
            appsList.forEach { app -> deniedAppsGroup?.addPreference(getAppPreference(app)) }
        }
    }

    private fun getNoAppsPreference(@StringRes res: Int): Preference {
        return Preference(context).also {
            it.setTitle(res)
            it.isSelectable = false
        }
    }

    private fun getAppPreference(app: ConnectedAppMetadata): Preference {
        return Preference(requireContext()).also {
            it.title = app.appMetadata.appName
            it.icon = app.appMetadata.icon
            it.setOnPreferenceClickListener {
                findNavController()
                    .navigate(
                        R.id.action_settingsManagePermission_to_settingsManageAppPermissions,
                        bundleOf(EXTRA_PACKAGE_NAME to app.appMetadata.packageName))
                true
            }
        }
    }
}
