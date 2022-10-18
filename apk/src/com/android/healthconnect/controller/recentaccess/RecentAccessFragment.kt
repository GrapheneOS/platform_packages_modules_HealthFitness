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

package com.android.healthconnect.controller.recentaccess

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Recent access fragment showing a timeline of apps that have recently accessed Health Connect. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class RecentAccessFragment : Hilt_RecentAccessFragment() {

    companion object {
        private const val RECENT_ACCESS_TODAY_KEY = "recent_access_today"
        private const val RECENT_ACCESS_YESTERDAY_KEY = "recent_access_yesterday"
    }
    private val viewModel: RecentAccessViewModel by viewModels()

    private val mRecentAccessTodayPreferenceGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(RECENT_ACCESS_TODAY_KEY)
    }

    private val mRecentAccessYesterdayPreferenceGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(RECENT_ACCESS_YESTERDAY_KEY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.recent_access_preference_screen, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.recent_access_header)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.recentAccessApps.observe(viewLifecycleOwner) { recentApps ->
            updateRecentApps(recentApps)
        }
    }

    private fun updateRecentApps(recentAppsList: List<RecentAccessApp>) {
        mRecentAccessTodayPreferenceGroup?.removeAll()
        mRecentAccessYesterdayPreferenceGroup?.removeAll()

        if (recentAppsList.isEmpty()) {
            mRecentAccessTodayPreferenceGroup?.isVisible = false
            mRecentAccessYesterdayPreferenceGroup?.isVisible = false
            // TODO add empty screen state
        } else {
            // TODO add logic for separating records into `Today` and `Yesterday`
            // hide the `Yesterday` section while using predefined RecentAccessApps
            mRecentAccessYesterdayPreferenceGroup?.isVisible = false

            recentAppsList.forEachIndexed { index, recentApp ->
                mRecentAccessTodayPreferenceGroup?.addPreference(
                    RecentAccessPreference(
                        requireContext(), recentApp, true, index == recentAppsList.size - 1))
            }
        }
    }
}
