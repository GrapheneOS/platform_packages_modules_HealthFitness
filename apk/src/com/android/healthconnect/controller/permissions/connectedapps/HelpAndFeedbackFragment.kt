/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.connectedapps

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setupSharedMenu
import dagger.hilt.android.AndroidEntryPoint

/** Can't see all your apps fragment for Health Connect. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class HelpAndFeedbackFragment : Hilt_HelpAndFeedbackFragment() {

    companion object {
        const val CHECK_FOR_UPDATES = "check_for_updates"
        private const val SEE_ALL_COMPATIBLE_APPS = "see_all_compatible_apps"
        private const val SEND_FEEDBACK = "send_feedback"
    }

    private val mCheckForUpdates: Preference? by lazy {
        preferenceScreen.findPreference(CHECK_FOR_UPDATES)
    }

    private val mSeeAllCompatibleApps: Preference? by lazy {
        preferenceScreen.findPreference(SEE_ALL_COMPATIBLE_APPS)
    }

    private val mSendFeedback: Preference? by lazy {
        preferenceScreen.findPreference(SEND_FEEDBACK)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.help_and_feedback_screen, rootKey)

        mCheckForUpdates?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_cant_see_all_apps_to_updated_apps)
            true
        }

        mSeeAllCompatibleApps?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_cant_see_all_apps_to_play_store)
            true
        }

        mSendFeedback?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_BUG_REPORT)
            getActivity()?.startActivityForResult(intent, 0)
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSharedMenu(viewLifecycleOwner)
    }
}
