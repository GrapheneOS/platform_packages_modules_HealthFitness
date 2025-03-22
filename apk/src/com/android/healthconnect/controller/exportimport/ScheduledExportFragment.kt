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
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportSettings
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.RadioButtonPreferenceCategory
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.ScheduledExportElement
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.toInstant
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Fragment showing the status of configured automatic fragment. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ScheduledExportFragment : Hilt_ScheduledExportFragment() {

    companion object {
        const val SCHEDULED_EXPORT_CONTROL_PREFERENCE_KEY = "scheduled_export_control_preference"
        const val CHOOSE_FREQUENCY_PREFERENCE_KEY = "choose_frequency"
        const val EXPORT_STATUS_PREFERENCE_ORDER = 1
        const val EXPORT_FREQ_KEY = "EXPORT_FREQUENCY_GROUP"
    }

    init {
        this.setPageName(PageName.EXPORT_SETTINGS_PAGE)
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var timeSource: TimeSource
    private val exportSettingsViewModel: ExportSettingsViewModel by viewModels()
    private val exportStatusViewModel: ExportStatusViewModel by viewModels()

    private val scheduledExportControlPreference: HealthMainSwitchPreference by
        pref(SCHEDULED_EXPORT_CONTROL_PREFERENCE_KEY)

    private val chooseFrequencyPreferenceGroup: PreferenceGroup by
        pref(CHOOSE_FREQUENCY_PREFERENCE_KEY)

    private val dateFormatter: LocalDateTimeFormatter by lazy {
        LocalDateTimeFormatter(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.scheduled_export_screen, rootKey)
        scheduledExportControlPreference.logNameActive =
            ScheduledExportElement.EXPORT_CONTROL_SWITCH_ON
        scheduledExportControlPreference.logNameInactive =
            ScheduledExportElement.EXPORT_CONTROL_SWITCH_OFF
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exportStatusViewModel.storedScheduledExportStatus.observe(viewLifecycleOwner) {
            scheduledExportUiStatus ->
            when (scheduledExportUiStatus) {
                is ScheduledExportUiStatus.WithData -> {
                    maybeShowNextExportStatus(scheduledExportUiStatus.scheduledExportUiState)
                }
                else -> {
                    // do nothing
                }
            }
        }

        exportSettingsViewModel.storedExportSettings.observe(viewLifecycleOwner) { exportSettings ->
            when (exportSettings) {
                is ExportSettings.WithData -> {
                    if (exportSettings.frequency != ExportFrequency.EXPORT_FREQUENCY_NEVER) {
                        scheduledExportControlPreference.isChecked = true
                        chooseFrequencyPreferenceGroup.setVisible(true)
                        preferenceScreen
                            .findPreference<Preference>(
                                ExportStatusPreference.EXPORT_STATUS_PREFERENCE
                            )
                            ?.setVisible(true)
                    } else {
                        scheduledExportControlPreference.isChecked = false
                        chooseFrequencyPreferenceGroup.setVisible(false)
                        preferenceScreen
                            .findPreference<Preference>(
                                ExportStatusPreference.EXPORT_STATUS_PREFERENCE
                            )
                            ?.setVisible(false)
                    }
                    exportSettingsViewModel.updatePreviousExportFrequency(exportSettings.frequency)
                    setupRadioButtons(exportSettings.frequency)
                }
                is ExportSettings.LoadingFailed ->
                    Toast.makeText(requireActivity(), R.string.default_error, Toast.LENGTH_LONG)
                        .show()
                is ExportSettings.Loading -> {
                    // Do nothing.
                }
            }
        }

        scheduledExportControlPreference.addOnSwitchChangeListener { _, isChecked ->
            if (isChecked) {
                exportSettingsViewModel.previousExportFrequency.value?.let { previousExportFrequency
                    ->
                    exportSettingsViewModel.updateExportFrequency(previousExportFrequency)
                }
            } else {
                exportSettingsViewModel.updateExportFrequency(
                    ExportFrequency.EXPORT_FREQUENCY_NEVER
                )
                preferenceScreen.removePreferenceRecursively(EXPORT_FREQ_KEY)
            }
        }
    }

    private fun setupRadioButtons(exportFrequency: ExportFrequency) {
        if (exportFrequency == ExportFrequency.EXPORT_FREQUENCY_NEVER) {
            preferenceScreen.removePreferenceRecursively(EXPORT_FREQ_KEY)
            return
        }

        if (preferenceScreen.findPreference<Preference>(EXPORT_FREQ_KEY) == null) {
            val options =
                listOf(
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = ExportFrequency.EXPORT_FREQUENCY_DAILY.name,
                        title = getString(R.string.frequency_daily),
                        element = ScheduledExportElement.EXPORT_SETTINGS_FREQUENCY_DAILY,
                        listener = getRadioButtonListener(ExportFrequency.EXPORT_FREQUENCY_DAILY),
                    ),
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = ExportFrequency.EXPORT_FREQUENCY_WEEKLY.name,
                        title = getString(R.string.frequency_weekly),
                        element = ScheduledExportElement.EXPORT_SETTINGS_FREQUENCY_WEEKLY,
                        listener = getRadioButtonListener(ExportFrequency.EXPORT_FREQUENCY_WEEKLY),
                    ),
                    RadioButtonPreferenceCategory.RadioButtonOption(
                        key = ExportFrequency.EXPORT_FREQUENCY_MONTHLY.name,
                        title = getString(R.string.frequency_monthly),
                        element = ScheduledExportElement.EXPORT_SETTINGS_FREQUENCY_MONTHLY,
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
            exportFrequencyPreference.order = 3
            preferenceScreen.addPreference(exportFrequencyPreference)
        } else {
            val exportFrequencyPreference =
                preferenceScreen.findPreference<RadioButtonPreferenceCategory>(EXPORT_FREQ_KEY)
            exportFrequencyPreference?.updateSelectedOption(exportFrequency.name)
        }
    }

    private fun getRadioButtonListener(exportFrequency: ExportFrequency) =
        SelectorWithWidgetPreference.OnClickListener {
            exportSettingsViewModel.updateExportFrequency(exportFrequency)
        }

    private fun maybeShowNextExportStatus(scheduledExportUiState: ScheduledExportUiState) {
        val lastSuccessfulExportTime = scheduledExportUiState.lastSuccessfulExportTime
        val periodInDays = scheduledExportUiState.periodInDays
        var nextExportText: String
        if (lastSuccessfulExportTime != null) {
            val scheduledExportTime =
                lastSuccessfulExportTime.plus(periodInDays.toLong(), ChronoUnit.DAYS)
            if (scheduledExportTime.isBefore(timeSource.currentTimeMillis().toInstant())) {
                nextExportText = getString(R.string.next_export_text)
            } else {
                nextExportText =
                    getString(
                        R.string.next_export_time,
                        dateFormatter.formatLongDate(scheduledExportTime),
                    )
            }
        } else {
            nextExportText = getString(R.string.next_export_text)
        }
        val nextExportLocation = getNextExportLocationString(scheduledExportUiState)
        preferenceScreen.addPreference(
            getExportStatusPreference(nextExportText, nextExportLocation)
        )
    }

    private fun getExportStatusPreference(
        nextExportText: String,
        nextExportLocation: String?,
    ): HealthPreference {
        val preference =
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                HealthPreference(requireContext()).also {
                    it.title = nextExportText
                    it.summary = nextExportLocation
                }
            } else {
                ExportStatusPreference(requireContext(), nextExportText, nextExportLocation).also {
                    it.isSelectable = false
                }
            }

        preference.order = EXPORT_STATUS_PREFERENCE_ORDER
        preference.key = ExportStatusPreference.EXPORT_STATUS_PREFERENCE
        return preference
    }

    private fun getNextExportLocationString(
        scheduledExportUiState: ScheduledExportUiState
    ): String? {
        if (
            scheduledExportUiState.nextExportAppName != null &&
                scheduledExportUiState.nextExportFileName != null
        ) {
            return getString(
                R.string.next_export_file_location,
                scheduledExportUiState.nextExportAppName,
                scheduledExportUiState.nextExportFileName,
            )
        } else if (scheduledExportUiState.nextExportFileName != null) {
            return scheduledExportUiState.nextExportFileName
        } else if (scheduledExportUiState.nextExportAppName != null) {
            return scheduledExportUiState.nextExportAppName
        }
        return null
    }
}
