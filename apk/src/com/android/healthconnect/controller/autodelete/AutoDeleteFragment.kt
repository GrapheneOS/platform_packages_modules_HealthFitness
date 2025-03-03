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

import android.icu.text.MessageFormat
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment.Companion.AUTO_DELETE_CANCELLED_EVENT
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment.Companion.AUTO_DELETE_SAVED_EVENT
import com.android.healthconnect.controller.autodelete.AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS
import com.android.healthconnect.controller.autodelete.AutoDeleteRange.AUTO_DELETE_RANGE_NEVER
import com.android.healthconnect.controller.autodelete.AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.RadioButtonPreferenceCategory
import com.android.healthconnect.controller.shared.preference.topIntroPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtilsImpl
import com.android.healthconnect.controller.utils.logging.AutoDeleteElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.settingslib.widget.SelectorWithWidgetPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment displaying auto delete settings. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class AutoDeleteFragment : Hilt_AutoDeleteFragment() {

    init {
        this.setPageName(PageName.AUTO_DELETE_PAGE)
    }

    companion object {
        const val AUTO_DELETE_RANGE_PICKER_PREFERENCE_KEY = "auto_delete_range_picker"
    }

    @Inject lateinit var logger: HealthConnectLogger

    private val viewModel: AutoDeleteViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.auto_delete_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceScreen.addPreference(
            topIntroPreference(
                context = requireContext(),
                preferenceTitle = getString(R.string.auto_delete_header),
                learnMoreText = getString(R.string.auto_delete_learn_more),
                learnMoreAction = { DeviceInfoUtilsImpl().openHCGetStartedLink(requireActivity()) },
            )
        )

        viewModel.storedAutoDeleteRange.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AutoDeleteViewModel.AutoDeleteState.Loading -> {
                    // do nothing
                }
                is AutoDeleteViewModel.AutoDeleteState.LoadingFailed -> {
                    Toast.makeText(activity, R.string.default_error, Toast.LENGTH_LONG).show()
                }
                is AutoDeleteViewModel.AutoDeleteState.WithData -> {
                    updateRadioButtonPreferenceCategory(state.autoDeleteRange)
                }
            }
        }

        childFragmentManager.setFragmentResultListener(AUTO_DELETE_SAVED_EVENT, this) { _, bundle ->
            bundle.getSerializable(AUTO_DELETE_SAVED_EVENT)?.let {
                viewModel.updateAutoDeleteRange(it as AutoDeleteRange)
            }
            viewModel.newAutoDeleteRange.value?.let {
                Toast.makeText(requireContext(), buildMessage(it), Toast.LENGTH_LONG).show()
            }
        }

        childFragmentManager.setFragmentResultListener(AUTO_DELETE_CANCELLED_EVENT, this) {
            _,
            bundle ->
            bundle.getSerializable(AUTO_DELETE_CANCELLED_EVENT)?.let {
                viewModel.updateAutoDeleteRange(it as AutoDeleteRange)
            }
        }
    }

    private fun updateRadioButtonPreferenceCategory(state: AutoDeleteRange) {
        if (
            preferenceScreen.findPreference<Preference>(AUTO_DELETE_RANGE_PICKER_PREFERENCE_KEY) ==
                null
        ) {
            val options =
                listOf(
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = AUTO_DELETE_RANGE_THREE_MONTHS.name,
                        title = createRangeString(AUTO_DELETE_RANGE_THREE_MONTHS),
                        element = AutoDeleteElement.AUTO_DELETE_3_MONTHS_BUTTON,
                        listener = askForConfirmationListener(AUTO_DELETE_RANGE_THREE_MONTHS),
                    ),
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = AUTO_DELETE_RANGE_EIGHTEEN_MONTHS.name,
                        title = createRangeString(AUTO_DELETE_RANGE_EIGHTEEN_MONTHS),
                        element = AutoDeleteElement.AUTO_DELETE_18_MONTHS_BUTTON,
                        listener = askForConfirmationListener(AUTO_DELETE_RANGE_EIGHTEEN_MONTHS),
                    ),
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = AUTO_DELETE_RANGE_NEVER.name,
                        title = getString(R.string.range_never),
                        element = AutoDeleteElement.AUTO_DELETE_NEVER_BUTTON,
                        listener = setToNeverListener(),
                    ),
                )

            val autoDeletePreferenceCategory =
                RadioButtonPreferenceCategory(
                    context = requireContext(),
                    childFragmentManager = childFragmentManager,
                    options = options,
                    logger = logger,
                    preferenceKey = AUTO_DELETE_RANGE_PICKER_PREFERENCE_KEY,
                    preferenceTitleResId = R.string.auto_delete_section,
                    currentSelectedKey = state.name,
                )
            autoDeletePreferenceCategory.order = 1
            preferenceScreen.addPreference(autoDeletePreferenceCategory)
        } else {
            val autoDeletePreference =
                preferenceScreen.findPreference<RadioButtonPreferenceCategory>(
                    AUTO_DELETE_RANGE_PICKER_PREFERENCE_KEY
                )
            autoDeletePreference?.updateSelectedOption(state.name)
        }
    }

    private fun setToNeverListener() =
        SelectorWithWidgetPreference.OnClickListener {
            viewModel.updateAutoDeleteRange(AUTO_DELETE_RANGE_NEVER)
            Toast.makeText(requireContext(), R.string.auto_delete_off_toast, Toast.LENGTH_LONG)
                .show()
        }

    private fun askForConfirmationListener(autoDeleteRange: AutoDeleteRange) =
        SelectorWithWidgetPreference.OnClickListener {
            viewModel.updateAutoDeleteDialogArgument(autoDeleteRange)
            AutoDeleteConfirmationDialogFragment()
                .show(childFragmentManager, AutoDeleteConfirmationDialogFragment.TAG)
        }

    private fun createRangeString(range: AutoDeleteRange): String {
        return MessageFormat.format(
            getString(R.string.range_after_x_months),
            mapOf("count" to range.numberOfMonths),
        )
    }

    private fun buildMessage(autoDeleteRange: AutoDeleteRange): String {
        val count = autoDeleteRange.numberOfMonths
        return MessageFormat.format(
            requireContext().getString(R.string.auto_delete_confirmation_toast),
            mapOf("count" to count),
        )
    }
}
