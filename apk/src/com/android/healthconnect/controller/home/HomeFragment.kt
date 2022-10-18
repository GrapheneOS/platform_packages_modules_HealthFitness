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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.recentaccess.RecentAccessApp
import com.android.healthconnect.controller.recentaccess.RecentAccessPreference
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Home fragment for Health Connect. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class HomeFragment : Hilt_HomeFragment() {

    companion object {
        private const val DATA_AND_ACCESS_PREFERENCE_KEY = "data_and_access"
        private const val RECENT_ACCESS_PREFERENCE_KEY = "recent_access"
        @JvmStatic fun newInstance() = HomeFragment()
    }

    private val viewModel: HomeFragmentViewModel by viewModels()

    private val mDataAndAccessPreference: Preference? by lazy {
        preferenceScreen.findPreference(DATA_AND_ACCESS_PREFERENCE_KEY)
    }

    private val mRecentAccessPreference: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(RECENT_ACCESS_PREFERENCE_KEY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home_preference_screen, rootKey)
        mDataAndAccessPreference?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_healthDataCategoriesFragment)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.app_label)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.recentAccessApps.observe(viewLifecycleOwner) { recentApps ->
            updateRecentApps(recentApps)
        }
    }

    private fun updateRecentApps(recentAppsList: List<RecentAccessApp>) {
        mRecentAccessPreference?.removeAll()
        if (recentAppsList.isEmpty()) {
            mRecentAccessPreference?.addPreference(
                Preference(requireContext()).also { it.setSummary(R.string.no_recent_access) })
        } else {
            recentAppsList.forEach { recentApp ->
                mRecentAccessPreference?.addPreference(
                    RecentAccessPreference(requireContext(), recentApp, false, false))
            }
            val seeAllPreference =
                Preference(requireContext()).also {
                    it.setTitle(R.string.see_all_recent_access)
                    it.setIcon(R.drawable.ic_arrow_forward)
                }
            seeAllPreference.setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_recentAccessFragment)
                true
            }
            mRecentAccessPreference?.addPreference(seeAllPreference)
        }
    }
}
