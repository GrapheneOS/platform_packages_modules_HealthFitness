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
package com.android.healthconnect.controller.permissions.connectedapps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.AppMetadata
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for connected apps screen. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class ConnectedAppsFragment : Hilt_ConnectedAppsFragment() {

    companion object {
        const val ALLOWED_APPS_CATEGORY = "allowed_apps"
        private const val NOT_ALLOWED_APPS = "not_allowed_apps"
        private const val CANT_SEE_ALL_YOUR_APPS = "cant_see_apps"
    }

    private val viewModel: ConnectedAppsViewModel by viewModels()

    private val mAllowedAppsCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(ALLOWED_APPS_CATEGORY)
    }

    private val mNotAllowedAppsCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(NOT_ALLOWED_APPS)
    }

    private val mCantSeeAllYourAppsPreference: Preference? by lazy {
        preferenceScreen.findPreference(CANT_SEE_ALL_YOUR_APPS)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.connected_apps_screen, rootKey)
        mCantSeeAllYourAppsPreference?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_connectedApps_to_cantSeeAllYourApps)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.connected_apps_title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.allowedApps.observe(viewLifecycleOwner) { apps -> updateAllowedApps(apps) }

        viewModel.notAllowedApps.observe(viewLifecycleOwner) { apps -> updateNotAllowedApps(apps) }
    }

    private fun updateAllowedApps(appsList: List<AppMetadata>) {
        mAllowedAppsCategory?.removeAll()

        appsList.forEach { app ->
            mAllowedAppsCategory?.addPreference(
                Preference(requireContext()).also {
                    it.setTitle(app.appName)
                    it.setIcon(app.icon)
                    it.setOnPreferenceClickListener {
                        findNavController()
                            .navigate(
                                R.id.action_connectedApps_to_connectedApp,
                                getBundle(app.packageNane))
                        true
                    }
                })
        }
    }
    private fun updateNotAllowedApps(appsList: List<AppMetadata>) {
        mNotAllowedAppsCategory?.removeAll()

        appsList.forEach { app ->
            mNotAllowedAppsCategory?.addPreference(
                Preference(requireContext()).also {
                    it.setTitle(app.appName)
                    it.setIcon(app.icon)
                    it.setOnPreferenceClickListener {
                        findNavController()
                            .navigate(
                                R.id.action_connectedApps_to_connectedApp,
                                getBundle(app.packageNane))
                        true
                    }
                })
        }
    }

    private fun getBundle(packageName: String): Bundle {
        val bundle = Bundle()
        bundle.putString("packageName", packageName)
        return bundle
    }
}
