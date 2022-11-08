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
package com.android.healthconnect.controller.dataentries

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint

/** Fragment to show health data entries by date. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class DataEntriesFragment : Hilt_DataEntriesFragment() {

    companion object {
        private const val DATE_NAVIGATION_PREFERENCE = "date_navigation"
    }

    private val dateNavigationPreference: DateNavigationPreference? by lazy {
        preferenceScreen.findPreference(DATE_NAVIGATION_PREFERENCE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.data_entries_preference_screen, rootKey)
    }

}