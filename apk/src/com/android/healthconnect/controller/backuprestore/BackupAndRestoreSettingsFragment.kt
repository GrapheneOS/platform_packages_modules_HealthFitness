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

package com.android.healthconnect.controller.backuprestore

import android.app.Activity
import android.content.Intent
import android.icu.text.MessageFormat
import android.net.Uri
import android.os.Bundle
import android.util.Slog
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.ExportSetupActivity
import com.android.healthconnect.controller.exportimport.ExportStatusPreference
import com.android.healthconnect.controller.exportimport.ImportFlowActivity
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportSettings
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ImportFlowViewModel
import com.android.healthconnect.controller.exportimport.api.ImportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ImportUiState
import com.android.healthconnect.controller.exportimport.api.ImportUiStatus
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.shared.preference.HealthBannerPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.ToastManager
import com.android.healthconnect.controller.utils.logging.BackupAndRestoreElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.toInstant
import com.android.healthconnect.controller.utils.withinOneDayAfter
import com.android.healthconnect.controller.utils.withinOneHourAfter
import com.android.healthconnect.controller.utils.withinOneMinuteAfter
import com.android.healthconnect.controller.utils.withinOneYearAfter
import com.android.healthfitness.flags.Flags.exportImportFastFollow
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Fragment displaying backup and restore settings. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class BackupAndRestoreSettingsFragment : Hilt_BackupAndRestoreSettingsFragment() {

    companion object {
        const val SCHEDULED_EXPORT_PREFERENCE_KEY = "scheduled_export"
        const val IMPORT_DATA_PREFERENCE_KEY = "import_data"
        const val EXPORT_IMPORT_SETTINGS_CATEGORY_PREFERENCE_KEY = "settings_category"
        const val IMPORT_ERROR_BANNER_KEY = "import_error_banner"
        const val IMPORT_ERROR_BANNER_ORDER = 0
        const val PREVIOUS_EXPORT_STATUS_ORDER = 2
        const val IMPORT_FILE_URI_KEY = "selectedUri"
        const val BANNER_GROUP = "banner_group"
        const val TAG = "BackupAndRestoreSettingsFragment"
    }

    init {
        this.setPageName(PageName.BACKUP_AND_RESTORE_PAGE)
    }

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var toastManager: ToastManager
    @Inject lateinit var timeSource: TimeSource

    private val exportSettingsViewModel: ExportSettingsViewModel by viewModels()
    private val exportStatusViewModel: ExportStatusViewModel by viewModels()
    private val importStatusViewModel: ImportStatusViewModel by viewModels()
    private val importFlowViewModel: ImportFlowViewModel by viewModels()

    private val contract = ActivityResultContracts.StartActivityForResult()
    private val triggerImportLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract, ::onRequestImport)
    private val setUpExportLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract, ::onSetUpExport)

    private val scheduledExportPreference: HealthPreference by pref(SCHEDULED_EXPORT_PREFERENCE_KEY)

    private val importDataPreference: HealthPreference by pref(IMPORT_DATA_PREFERENCE_KEY)
    private val bannerGroup: BannerMessagePreferenceGroup by pref(BANNER_GROUP)

    private val settingsCategory: PreferenceGroup by
        pref(EXPORT_IMPORT_SETTINGS_CATEGORY_PREFERENCE_KEY)

    private val dateFormatter: LocalDateTimeFormatter by lazy {
        LocalDateTimeFormatter(requireContext())
    }

    private val footerPreference: FooterPreference by pref("backup_restore_footer")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.backup_and_restore_settings_screen, rootKey)

        footerPreference.setLearnMoreText(getString(R.string.backup_and_restore_footer_link_text))
        footerPreference.setLearnMoreAction {
            deviceInfoUtils.openHCBackupAndRestoreLink(requireActivity())
        }

        scheduledExportPreference.logName = BackupAndRestoreElement.SCHEDULED_EXPORT_BUTTON

        importDataPreference.logName = BackupAndRestoreElement.RESTORE_DATA_BUTTON
        importDataPreference.setOnPreferenceClickListener {
            triggerImport()
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        importStatusViewModel.storedImportStatus.observe(viewLifecycleOwner) {
            importUiStatus: ImportUiStatus ->
            when (importUiStatus) {
                is ImportUiStatus.WithData -> {
                    maybeShowImportErrorBanner(importUiStatus.importUiState)
                }
                else -> {
                    // Do nothing.
                }
            }
        }

        importFlowViewModel.lastImportCompletionInstant.observe(viewLifecycleOwner) {
            importDataPreference.isEnabled = true
            toastManager.showToast(requireActivity(), R.string.import_complete_toast_text)
            importStatusViewModel.loadImportStatus()
        }

        exportStatusViewModel.storedScheduledExportStatus.observe(viewLifecycleOwner) {
            scheduledExportUiStatus: ScheduledExportUiStatus ->
            when (scheduledExportUiStatus) {
                is ScheduledExportUiStatus.WithData -> {
                    maybeShowPreviousExportStatus(scheduledExportUiStatus.scheduledExportUiState)
                }
                else -> {
                    // do nothing
                }
            }
        }

        exportSettingsViewModel.storedExportSettings.observe(viewLifecycleOwner) {
            exportSettings: ExportSettings ->
            when (exportSettings) {
                is ExportSettings.WithData -> {
                    val frequency = exportSettings.frequency
                    if (frequency == ExportFrequency.EXPORT_FREQUENCY_NEVER) {
                        scheduledExportPreference.setOnPreferenceClickListener {
                            val exportSetupIntent =
                                Intent(requireActivity(), ExportSetupActivity::class.java)
                            setUpExportLauncher.launch(exportSetupIntent)

                            true
                        }
                    } else {
                        scheduledExportPreference.setOnPreferenceClickListener {
                            findNavController()
                                .navigate(
                                    R.id
                                        .action_backupAndRestoreSettingsFragment_to_scheduledExportFragment
                                )
                            true
                        }
                    }
                    scheduledExportPreference.summary = buildSummary(frequency)
                }
                is ExportSettings.LoadingFailed ->
                    Toast.makeText(activity, R.string.default_error, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        exportSettingsViewModel.loadExportSettings()
        importStatusViewModel.loadImportStatus()
        exportStatusViewModel.loadScheduledExportStatus()
    }

    private fun buildSummary(frequency: ExportFrequency): String {
        val on = getString(R.string.automatic_export_on)
        val automaticExportStatusId = R.string.automatic_export_status
        return when (frequency) {
            ExportFrequency.EXPORT_FREQUENCY_NEVER -> getString(R.string.automatic_export_off)
            ExportFrequency.EXPORT_FREQUENCY_DAILY ->
                getString(automaticExportStatusId, on, getString(R.string.frequency_daily))
            ExportFrequency.EXPORT_FREQUENCY_WEEKLY ->
                getString(automaticExportStatusId, on, getString(R.string.frequency_weekly))
            ExportFrequency.EXPORT_FREQUENCY_MONTHLY ->
                getString(automaticExportStatusId, on, getString(R.string.frequency_monthly))
        }
    }

    private fun maybeShowPreviousExportStatus(scheduledExportUiState: ScheduledExportUiState) {
        settingsCategory.removePreferenceRecursively(
            ExportStatusPreference.EXPORT_STATUS_PREFERENCE
        )
        val lastSuccessfulExportTime = scheduledExportUiState.lastSuccessfulExportTime
        if (lastSuccessfulExportTime != null) {
            val lastExportTime = getLastExportTime(lastSuccessfulExportTime)
            val exportLocation = getExportLocationString(scheduledExportUiState)
            settingsCategory.addPreference(
                ExportStatusPreference(requireContext(), lastExportTime, exportLocation).also {
                    it.order = PREVIOUS_EXPORT_STATUS_ORDER
                    it.isSelectable = false
                }
            )
        } else if (
            exportImportFastFollow() &&
                scheduledExportUiState.lastFailedExportTime == null &&
                scheduledExportUiState.periodInDays !=
                    ExportFrequency.EXPORT_FREQUENCY_NEVER.periodInDays
        ) {
            val lastExportMessage = getString(R.string.no_last_export_message)
            settingsCategory.addPreference(
                ExportStatusPreference(requireContext(), lastExportMessage, null).also {
                    it.order = PREVIOUS_EXPORT_STATUS_ORDER
                }
            )
        }
    }

    private fun getExportLocationString(scheduledExportUiState: ScheduledExportUiState): String? {
        if (
            scheduledExportUiState.lastExportAppName != null &&
                scheduledExportUiState.lastExportFileName != null
        ) {
            return getString(
                R.string.last_export_file_location,
                scheduledExportUiState.lastExportAppName,
                scheduledExportUiState.lastExportFileName,
            )
        } else if (scheduledExportUiState.lastExportFileName != null) {
            return scheduledExportUiState.lastExportFileName
        } else if (scheduledExportUiState.lastExportAppName != null) {
            return scheduledExportUiState.lastExportAppName
        }
        return null
    }

    private fun maybeShowImportErrorBanner(importUiState: ImportUiState) {
        val importErrorBanner = bannerGroup.findPreference<Preference>(IMPORT_ERROR_BANNER_KEY)
        if (importErrorBanner != null) {
            bannerGroup.removePreferenceRecursively(IMPORT_ERROR_BANNER_KEY)
        }
        when (importUiState.dataImportState) {
            ImportUiState.DataImportState.DATA_IMPORT_ERROR_WRONG_FILE -> {
                bannerGroup.addPreference(getImportWrongFileErrorBanner())
            }
            ImportUiState.DataImportState.DATA_IMPORT_ERROR_VERSION_MISMATCH -> {
                bannerGroup.addPreference(getImportVersionMismatchErrorBanner())
            }
            ImportUiState.DataImportState.DATA_IMPORT_ERROR_UNKNOWN -> {
                bannerGroup.addPreference(getImportOtherErrorBanner())
            }
            ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE -> {
                // Do nothing.
            }
            ImportUiState.DataImportState.DATA_IMPORT_STARTED -> {
                // Do nothing, import ongoing, no error so far
            }
        }
    }

    private fun getImportWrongFileErrorBanner(): HealthBannerPreference {
        return HealthBannerPreference(
                requireContext(),
                BackupAndRestoreElement.IMPORT_WRONG_FILE_ERROR_BANNER,
            )
            .also { banner ->
                banner.setPositiveButton(
                    getString(R.string.import_wrong_file_error_banner_button),
                    BackupAndRestoreElement.IMPORT_WRONG_FILE_ERROR_BANNER_BUTTON,
                ) {
                    triggerImport()
                }
                banner.title = getString(R.string.import_error_banner_title)
                banner.key = IMPORT_ERROR_BANNER_KEY
                banner.summary = getString(R.string.import_wrong_file_error_banner_summary)
                banner.icon =
                    AttributeResolver.getNullableDrawable(requireContext(), R.attr.warningIcon)
            }
    }

    private fun getImportVersionMismatchErrorBanner(): HealthBannerPreference {
        return HealthBannerPreference(
                requireContext(),
                BackupAndRestoreElement.IMPORT_VERSION_MISMATCH_ERROR_BANNER,
            )
            .also { banner ->
                banner.setPositiveButton(
                    getString(R.string.import_version_mismatch_error_banner_button),
                    BackupAndRestoreElement.IMPORT_VERSION_MISMATCH_ERROR_BANNER_BUTTON,
                ) {
                    findNavController()
                        .navigate(
                            R.id.action_backupAndRestoreSettingsFragment_to_systemUpdateActivity
                        )
                }
                banner.title = getString(R.string.import_error_banner_title)
                banner.key = IMPORT_ERROR_BANNER_KEY
                banner.summary = getString(R.string.import_version_mismatch_error_banner_summary)
                banner.icon =
                    AttributeResolver.getNullableDrawable(requireContext(), R.attr.warningIcon)
            }
    }

    private fun getImportOtherErrorBanner(): HealthBannerPreference {
        return HealthBannerPreference(
                requireContext(),
                BackupAndRestoreElement.IMPORT_GENERAL_ERROR_BANNER,
            )
            .also { banner ->
                banner.setPositiveButton(
                    getString(R.string.import_other_error_banner_button),
                    BackupAndRestoreElement.IMPORT_GENERAL_ERROR_BANNER_BUTTON,
                ) {
                    triggerImport()
                }
                banner.title = getString(R.string.import_error_banner_title)
                banner.key = IMPORT_ERROR_BANNER_KEY
                banner.summary = getString(R.string.import_other_error_banner_summary)
                banner.icon =
                    AttributeResolver.getNullableDrawable(requireContext(), R.attr.warningIcon)
            }
    }

    private fun getLastExportTime(lastSuccessfulExportTime: Instant): String {
        if (!exportImportFastFollow()) {
            return getString(
                R.string.last_export_time,
                dateFormatter.formatDateAndTime(lastSuccessfulExportTime),
            )
        }
        // Format for the last export string:
        // - Now (when <1min)
        // - X minutes ago (when >=1min & <1h)
        // - X hours ago (when >=1h & <24h)
        // - DD Mmmm, HH:MM (when >=24h & <1yr)
        // - DD Mmmm, YYYY (>=1yr)
        val now: Instant = timeSource.currentTimeMillis().toInstant()
        if (now.withinOneMinuteAfter(lastSuccessfulExportTime)) {
            return getString(R.string.last_export_time_now)
        } else if (now.withinOneHourAfter(lastSuccessfulExportTime)) {
            val timeDiffInMinutes = Duration.between(lastSuccessfulExportTime, now).toMinutes()
            return MessageFormat.format(
                requireContext().getString(R.string.last_export_time_minutes_ago),
                mapOf("count" to timeDiffInMinutes),
            )
        } else if (now.withinOneDayAfter(lastSuccessfulExportTime)) {
            val timeDiffInHours = Duration.between(lastSuccessfulExportTime, now).toHours()
            return MessageFormat.format(
                requireContext().getString(R.string.last_export_time_hours_ago),
                mapOf("count" to timeDiffInHours),
            )
        } else if (
            LocalDate.ofInstant(now, timeSource.deviceZoneOffset())
                .withinOneYearAfter(
                    LocalDate.ofInstant(lastSuccessfulExportTime, timeSource.deviceZoneOffset())
                )
        ) {
            return getString(
                R.string.last_export_time,
                dateFormatter.formatDateAndTime(lastSuccessfulExportTime),
            )
        } else {
            return getString(
                R.string.last_export_time,
                dateFormatter.formatLongDate(lastSuccessfulExportTime),
            )
        }
    }

    private fun triggerImport() {
        val importRequestIntent = Intent(requireActivity(), ImportFlowActivity::class.java)
        triggerImportLauncher.launch(importRequestIntent)
    }

    private fun onRequestImport(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.extras?.getString(IMPORT_FILE_URI_KEY)
            Slog.i(TAG, "uri: $uriString")
            if (uriString != null) {
                importDataPreference.setEnabled(false)
                toastManager.showToast(requireActivity(), R.string.import_in_progress_toast_text)
                importFlowViewModel.triggerImportOfSelectedFile(Uri.parse(uriString))
            }
        }
    }

    private fun onSetUpExport(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && exportImportFastFollow()) {
            toastManager.showToast(requireActivity(), R.string.scheduled_export_on_toast_text)
        }
    }
}
