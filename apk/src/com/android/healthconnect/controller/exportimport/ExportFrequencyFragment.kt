/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.healthconnect.controller.exportimport

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.shared.preference.RadioButtonPreferenceCategory
import com.android.healthconnect.controller.utils.logging.ExportFrequencyElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.settingslib.widget.SelectorWithWidgetPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Export frequency fragment for Health Connect. */
@AndroidEntryPoint(HealthSetupFragment::class)
class ExportFrequencyFragment : Hilt_ExportFrequencyFragment() {

    @Inject lateinit var logger: HealthConnectLogger

    private val viewModel: ExportSettingsViewModel by activityViewModels()

    companion object {
        const val EXPORT_FREQ_KEY = "EXPORT_FREQUENCY_GROUP"
    }

    init {
        this.setPageName(PageName.EXPORT_FREQUENCY_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.export_frequency_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedExportFrequency.observe(viewLifecycleOwner) {
            exportFrequency: ExportFrequency? ->
            exportFrequency?.let { setupRadioButtons(exportFrequency) }
        }

        val nextButton: Button = getPrimaryButtonFull()
        val cancelButton: Button = getSecondaryButton()

        cancelButton.text = getString(R.string.export_cancel_button)
        nextButton.text = getString(R.string.export_next_button)

        logger.logImpression(ExportFrequencyElement.EXPORT_FREQUENCY_BACK_BUTTON)
        logger.logImpression(ExportFrequencyElement.EXPORT_FREQUENCY_NEXT_BUTTON)

        cancelButton.setOnClickListener {
            viewModel.updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_NEVER)
            logger.logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_BACK_BUTTON)
            requireActivity().finish()
        }

        nextButton.setOnClickListener {
            val exportFrequencyCategory =
                preferenceScreen.findPreference<RadioButtonPreferenceCategory>(EXPORT_FREQ_KEY)
            val selection = exportFrequencyCategory?.getSelectedOption()
            when (selection) {
                ExportFrequency.EXPORT_FREQUENCY_DAILY.name ->
                    viewModel.updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_DAILY)
                ExportFrequency.EXPORT_FREQUENCY_WEEKLY.name ->
                    viewModel.updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_WEEKLY)
                ExportFrequency.EXPORT_FREQUENCY_MONTHLY.name ->
                    viewModel.updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_MONTHLY)
                else -> {
                    viewModel.updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_DAILY)
                }
            }
            logger.logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_NEXT_BUTTON)
            findNavController()
                .navigate(R.id.action_exportFrequencyFragment_to_exportDestinationFragment)
        }
    }

    private fun setupRadioButtons(exportFrequency: ExportFrequency) {
        if (preferenceScreen.findPreference<Preference>(EXPORT_FREQ_KEY) == null) {
            val options =
                listOf(
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = ExportFrequency.EXPORT_FREQUENCY_DAILY.name,
                        title = getString(R.string.frequency_daily),
                        element = ExportFrequencyElement.EXPORT_FREQUENCY_DAILY_BUTTON,
                        listener = getRadioButtonListener(ExportFrequency.EXPORT_FREQUENCY_DAILY),
                    ),
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = ExportFrequency.EXPORT_FREQUENCY_WEEKLY.name,
                        title = getString(R.string.frequency_weekly),
                        element = ExportFrequencyElement.EXPORT_FREQUENCY_WEEKLY_BUTTON,
                        listener = getRadioButtonListener(ExportFrequency.EXPORT_FREQUENCY_WEEKLY),
                    ),
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = ExportFrequency.EXPORT_FREQUENCY_MONTHLY.name,
                        title = getString(R.string.frequency_monthly),
                        element = ExportFrequencyElement.EXPORT_FREQUENCY_MONTHLY_BUTTON,
                        listener = getRadioButtonListener(ExportFrequency.EXPORT_FREQUENCY_MONTHLY),
                    ),
                )

            val currentSelectedKey =
                if (exportFrequency == ExportFrequency.EXPORT_FREQUENCY_NEVER) {
                    ExportFrequency.EXPORT_FREQUENCY_DAILY.name
                } else {
                    exportFrequency.name
                }

            val exportFrequencyPreference =
                RadioButtonPreferenceCategory(
                    context = requireContext(),
                    childFragmentManager = childFragmentManager,
                    options = options,
                    logger = logger,
                    preferenceKey = EXPORT_FREQ_KEY,
                    currentSelectedKey = currentSelectedKey,
                )
            exportFrequencyPreference.order = 2
            preferenceScreen.addPreference(exportFrequencyPreference)
        } else {
            val exportFrequencyPreference =
                preferenceScreen.findPreference<RadioButtonPreferenceCategory>(EXPORT_FREQ_KEY)
            exportFrequencyPreference?.updateSelectedOption(exportFrequency.name)
        }
    }

    private fun getRadioButtonListener(exportFrequency: ExportFrequency) =
        SelectorWithWidgetPreference.OnClickListener {
            viewModel.updateSelectedFrequency(exportFrequency)
        }

    override fun onResume() {
        super.onResume()
        logger.logPageImpression()
    }
}
