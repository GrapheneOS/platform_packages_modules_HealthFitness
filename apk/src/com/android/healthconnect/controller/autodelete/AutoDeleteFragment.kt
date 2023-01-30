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
package com.android.healthconnect.controller.autodelete

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment.Companion.AUTO_DELETE_CANCELLED_EVENT
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment.Companion.AUTO_DELETE_CONFIRMATION_DIALOG_EVENT
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment.Companion.AUTO_DELETE_SAVED_EVENT
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment.Companion.NEW_AUTO_DELETE_RANGE_BUNDLE
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment.Companion.OLD_AUTO_DELETE_RANGE_BUNDLE
import com.android.healthconnect.controller.autodelete.AutoDeleteRangePickerPreference.Companion.SET_TO_NEVER_EVENT
import com.android.healthconnect.controller.utils.setupSharedMenu
import dagger.hilt.android.AndroidEntryPoint

/** Fragment displaying auto delete settings. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class AutoDeleteFragment : Hilt_AutoDeleteFragment() {

    companion object {
        private const val AUTO_DELETE_SECTION = "auto_delete_section"
        private const val HEADER = "header"
    }

    private val viewModel: AutoDeleteViewModel by activityViewModels()

    private val mAutoDeleteSection: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(AUTO_DELETE_SECTION)
    }

    private val mHeaderSection: PreferenceGroup? by lazy { preferenceScreen.findPreference(HEADER) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.auto_delete_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSharedMenu(viewLifecycleOwner)

        viewModel.storedAutoDeleteRange.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AutoDeleteViewModel.AutoDeleteState.Loading -> {}
                is AutoDeleteViewModel.AutoDeleteState.LoadingFailed -> {}
                is AutoDeleteViewModel.AutoDeleteState.WithData -> {
                    mAutoDeleteSection?.removeAll()
                    mAutoDeleteSection?.addPreference(
                        AutoDeleteRangePickerPreference(
                            requireContext(), childFragmentManager, state.autoDeleteRange))
                }
            }
        }

        mHeaderSection?.removeAll()
        mHeaderSection?.addPreference(HeaderPreference(requireContext(), requireActivity()))

        childFragmentManager.setFragmentResultListener(SET_TO_NEVER_EVENT, this) { _, _ ->
            viewModel.updateAutoDeleteRange(AutoDeleteRange.AUTO_DELETE_RANGE_NEVER)
        }

        childFragmentManager.setFragmentResultListener(
            AUTO_DELETE_CONFIRMATION_DIALOG_EVENT, this) { _, bundle ->
                bundle.getSerializable(NEW_AUTO_DELETE_RANGE_BUNDLE)?.let { newAutoDeleteRange ->
                    bundle.getSerializable(OLD_AUTO_DELETE_RANGE_BUNDLE)?.let { oldAutoDeleteRange
                        ->
                        viewModel.updateAutoDeleteDialogArguments(
                            newAutoDeleteRange as AutoDeleteRange,
                            oldAutoDeleteRange as AutoDeleteRange)
                        AutoDeleteConfirmationDialogFragment()
                            .show(childFragmentManager, AutoDeleteConfirmationDialogFragment.TAG)
                    }
                }
            }

        childFragmentManager.setFragmentResultListener(AUTO_DELETE_SAVED_EVENT, this) { _, bundle ->
            bundle.getSerializable(AUTO_DELETE_SAVED_EVENT)?.let {
                viewModel.updateAutoDeleteRange(it as AutoDeleteRange)
            }
            DeletionStartedDialogFragment()
                .show(childFragmentManager, DeletionStartedDialogFragment.TAG)
        }

        childFragmentManager.setFragmentResultListener(AUTO_DELETE_CANCELLED_EVENT, this) {
            _,
            bundle ->
            bundle.getSerializable(AUTO_DELETE_CANCELLED_EVENT)?.let {
                viewModel.updateAutoDeleteRange(it as AutoDeleteRange)
            }
        }
    }
}
