/**
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
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setTitle

/** Home fragment for Health Connect. */
class HomeFragment : PreferenceFragmentCompat() {

    private val mDataAndAccessPreference : Preference? by lazy {
        preferenceScreen.findPreference(DATA_AND_ACCESS_PREFERENCE_KEY)
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

    companion object {
        private const val  DATA_AND_ACCESS_PREFERENCE_KEY = "data_and_access"
        @JvmStatic fun newInstance() = HomeFragment()
    }
}
