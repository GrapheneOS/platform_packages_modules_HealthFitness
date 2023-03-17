/*
 * Copyright (C) 2022 The Android Open Source Project
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
 */
package com.android.healthconnect.controller.home

import android.content.Intent
import android.health.HealthFitnessStatsLog.*
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.shared.Constants
import com.android.healthconnect.controller.recentaccess.RecentAccessEntry
import com.android.healthconnect.controller.recentaccess.RecentAccessPreference
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel.RecentAccessState
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.logging.HomePageElement
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.AndroidEntryPoint

/** Home fragment for Health Connect. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class HomeFragment : Hilt_HomeFragment() {

    companion object {
        private const val DATA_AND_ACCESS_PREFERENCE_KEY = "data_and_access"
        private const val RECENT_ACCESS_PREFERENCE_KEY = "recent_access"
        private const val CONNECTED_APPS_PREFERENCE_KEY = "connected_apps"

        @JvmStatic fun newInstance() = HomeFragment()
    }

    init {
        this.setPageName(PageName.HOME_PAGE)
    }

    private val recentAccessViewModel: RecentAccessViewModel by viewModels()
    private val homeFragmentViewModel: HomeFragmentViewModel by viewModels()

    private val mDataAndAccessPreference: HealthPreference? by lazy {
        preferenceScreen.findPreference(DATA_AND_ACCESS_PREFERENCE_KEY)
    }

    private val mRecentAccessPreference: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(RECENT_ACCESS_PREFERENCE_KEY)
    }

    private val mConnectedAppsPreference: HealthPreference? by lazy {
        preferenceScreen.findPreference(CONNECTED_APPS_PREFERENCE_KEY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.home_preference_screen, rootKey)
        mDataAndAccessPreference?.logName = HomePageElement.DATA_AND_ACCESS_BUTTON
        mDataAndAccessPreference?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_healthDataCategoriesFragment)
            true
        }
        mConnectedAppsPreference?.logName = HomePageElement.APP_PERMISSIONS_BUTTON
        mConnectedAppsPreference?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_connectedAppsFragment)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        recentAccessViewModel.loadRecentAccessApps(maxNumEntries = 3)
        homeFragmentViewModel.loadConnectedApps()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recentAccessViewModel.loadRecentAccessApps(maxNumEntries = 3)
        recentAccessViewModel.recentAccessApps.observe(viewLifecycleOwner) { recentAppsState ->
            when (recentAppsState) {
                is RecentAccessState.WithData -> {
                    updateRecentApps(recentAppsState.recentAccessEntries)
                }
                else -> {
                    updateRecentApps(emptyList())
                }
            }
        }
        homeFragmentViewModel.connectedApps.observe(viewLifecycleOwner) { connectedApps ->
            updateConnectedApps(connectedApps)
        }
    }

    private fun updateConnectedApps(connectedApps: List<ConnectedAppMetadata>) {
        val connectedAppsGroup = connectedApps.groupBy { it.status }
        val numAllowedApps = connectedAppsGroup[ConnectedAppStatus.ALLOWED].orEmpty().size
        val numNotAllowedApps = connectedAppsGroup[ConnectedAppStatus.DENIED].orEmpty().size
        val numTotalApps = numAllowedApps + numNotAllowedApps

        if (numTotalApps == 0) {
            mConnectedAppsPreference?.summary =
                getString(R.string.connected_apps_button_no_permissions_subtitle)
        } else if (numAllowedApps == 1 && numAllowedApps == numTotalApps) {
            mConnectedAppsPreference?.summary =
                getString(
                    R.string.connected_apps_one_app_connected_subtitle, numAllowedApps.toString())
        } else if (numAllowedApps == numTotalApps) {
            mConnectedAppsPreference?.summary =
                getString(
                    R.string.connected_apps_all_apps_connected_subtitle, numAllowedApps.toString())
        } else {
            mConnectedAppsPreference?.summary =
                getString(
                    R.string.connected_apps_button_subtitle,
                    numAllowedApps.toString(),
                    numTotalApps.toString())
        }
    }

    private fun updateRecentApps(recentAppsList: List<RecentAccessEntry>) {
        mRecentAccessPreference?.removeAll()

        if (recentAppsList.isEmpty()) {
            mRecentAccessPreference?.addPreference(
                Preference(requireContext()).also { it.setSummary(R.string.no_recent_access) })
        } else {
            recentAppsList.forEach { recentApp ->
                val newRecentAccessPreference =
                    RecentAccessPreference(requireContext(), recentApp, false).also { newPreference
                        ->
                        if (!recentApp.isInactive) {
                            newPreference.setOnPreferenceClickListener {
                                findNavController()
                                    .navigate(
                                        R.id.action_homeFragment_to_connectedAppFragment,
                                        bundleOf(
                                            Intent.EXTRA_PACKAGE_NAME to
                                                recentApp.metadata.packageName,
                                            Constants.EXTRA_APP_NAME to recentApp.metadata.appName))
                                true
                            }
                        }
                    }
                mRecentAccessPreference?.addPreference(newRecentAccessPreference)
            }
            val seeAllPreference =
                HealthPreference(requireContext()).also {
                    it.setTitle(R.string.show_recent_access_entries_button_title)
                    it.setIcon(AttributeResolver.getResource(requireContext(), R.attr.seeAllIcon))
                    it.logName = HomePageElement.SEE_ALL_RECENT_ACCESS_BUTTON
                }
            seeAllPreference.setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_recentAccessFragment)
                true
            }
            mRecentAccessPreference?.addPreference(seeAllPreference)
        }
    }
}
