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
package com.android.healthconnect.controller.autodelete

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Fragment displaying auto delete settings. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class AutoDeleteFragment : Hilt_AutoDeleteFragment() {

    companion object {
        private const val AUTO_DELETE_SECTION = "auto_delete_section"
    }

    private val viewModel: AutoDeleteViewModel by viewModels()

    private lateinit var autoDeleteRange: AutoDeleteRange

    private val mAutoDeleteSection: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(AUTO_DELETE_SECTION)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.auto_delete_screen, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.auto_delete_title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.autoDeleteRange.observe(viewLifecycleOwner) { _autoDeleteRange ->
            autoDeleteRange = _autoDeleteRange
            mAutoDeleteSection?.addPreference(
                AutoDeleteRangePickerPreference(
                    requireContext(), childFragmentManager, autoDeleteRange))
        }
    }
}
