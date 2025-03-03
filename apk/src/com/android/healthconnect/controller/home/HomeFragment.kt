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
package com.android.healthconnect.controller.home

import android.content.Context
import android.content.Intent
import android.icu.text.MessageFormat
import android.os.Bundle
import android.provider.Settings.ACTION_SECURITY_SETTINGS
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.alldata.AllDataFragment.Companion.IS_BROWSE_MEDICAL_DATA_SCREEN
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.home.HomeViewModel.LockScreenBannerState
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.maybeShowWhatsNewDialog
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.recentaccess.RecentAccessEntry
import com.android.healthconnect.controller.recentaccess.RecentAccessPreference
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel.RecentAccessState
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL
import com.android.healthconnect.controller.shared.Constants.MIGRATION_NOT_COMPLETE_DIALOG_SEEN
import com.android.healthconnect.controller.shared.Constants.USER_ACTIVITY_TRACKER
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.shared.preference.HealthBannerPreference
import com.android.healthconnect.controller.shared.preference.HealthButtonPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.formatRecentAccessTime
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HomePageElement
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RecentAccessElement
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import com.android.healthconnect.controller.utils.pref
import com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled
import com.android.healthfitness.flags.Flags.onboarding
import com.android.healthfitness.flags.Flags.personalHealthRecordLockScreenBanner
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.ZeroStatePreference
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject

/** Home fragment for Health Connect. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class HomeFragment : Hilt_HomeFragment() {

    companion object {
        private const val BANNER_GROUP = "banner_group"
        private const val NO_RECENT_ACCESS = "no_recent_access"
        private const val DATA_AND_ACCESS_PREFERENCE_KEY = "data_and_access"
        private const val RECENT_ACCESS_PREFERENCE_KEY = "recent_access"
        private const val CONNECTED_APPS_PREFERENCE_KEY = "connected_apps"
        private const val MIGRATION_BANNER_PREFERENCE_KEY = "migration_banner"
        private const val DATA_RESTORE_BANNER_PREFERENCE_KEY = "data_restore_banner"
        private const val MANAGE_DATA_PREFERENCE_KEY = "manage_data"
        private const val BROWSE_MEDICAL_DATA_PREFERENCE_KEY = "medical_data"
        private const val EXPORT_ERROR_BANNER_PREFERENCE_KEY = "export_error_banner"
        private const val START_USING_HC_BANNER_KEY = "start_using_hc"
        private const val CONNECT_MORE_APPS_BANNER_KEY = "connect_more_apps"
        private const val SEE_COMPATIBLE_APPS_BANNER_KEY = "see_compatible_apps"
        private const val LOCK_SCREEN_BANNER_KEY = "lock_screen_banner"
        private val securitySettingsIntent = Intent(ACTION_SECURITY_SETTINGS)

        @JvmStatic fun newInstance() = HomeFragment()
    }

    init {
        this.setPageName(PageName.HOME_PAGE)
    }

    @Inject lateinit var timeSource: TimeSource
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    private val recentAccessViewModel: RecentAccessViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val migrationViewModel: MigrationViewModel by activityViewModels()
    private val exportStatusViewModel: ExportStatusViewModel by activityViewModels()

    private val noRecentAccessPreference: ZeroStatePreference by pref(NO_RECENT_ACCESS)

    private val dataAndAccessPreference: HealthPreference by pref(DATA_AND_ACCESS_PREFERENCE_KEY)

    private val recentAccessPreferenceGroup: PreferenceGroup by pref(RECENT_ACCESS_PREFERENCE_KEY)

    private val appPermissionsPreference: HealthPreference by pref(CONNECTED_APPS_PREFERENCE_KEY)

    private val manageDataPreference: HealthPreference by pref(MANAGE_DATA_PREFERENCE_KEY)

    private val browseMedicalDataPreference: HealthPreference by
        pref(BROWSE_MEDICAL_DATA_PREFERENCE_KEY)

    private val dateFormatter: LocalDateTimeFormatter by lazy {
        LocalDateTimeFormatter(requireContext())
    }

    private val isLockScreenBannerAvailable: Boolean by lazy {
        personalHealthRecordLockScreenBanner() &&
            deviceInfoUtils.isIntentHandlerAvailable(requireContext(), securitySettingsIntent)
    }

    private val bannerGroup: BannerMessagePreferenceGroup by pref(BANNER_GROUP)
    private lateinit var migrationBannerSummary: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.home_preference_screen, rootKey)
        dataAndAccessPreference.logName = HomePageElement.DATA_AND_ACCESS_BUTTON
        dataAndAccessPreference.summary = getString(R.string.browse_data_subtitle)
        dataAndAccessPreference.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_healthDataCategoriesFragment)
            true
        }
        appPermissionsPreference.logName = HomePageElement.APP_PERMISSIONS_BUTTON
        appPermissionsPreference.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_connectedAppsFragment)
            true
        }

        manageDataPreference.logName = HomePageElement.MANAGE_DATA_BUTTON
        manageDataPreference.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_manageDataFragment)
            true
        }
        manageDataPreference.summary = getString(R.string.manage_data_summary)

        if (isPersonalHealthRecordEnabled()) {
            browseMedicalDataPreference.setOnPreferenceClickListener {
                findNavController()
                    .navigate(
                        R.id.action_homeFragment_to_medicalDataFragment,
                        bundleOf(IS_BROWSE_MEDICAL_DATA_SCREEN to true),
                    )
                true
            }
            browseMedicalDataPreference.isVisible = false
            browseMedicalDataPreference.logName = HomePageElement.BROWSE_HEALTH_RECORDS_BUTTON
        } else {
            preferenceScreen.removePreferenceRecursively(BROWSE_MEDICAL_DATA_PREFERENCE_KEY)
        }

        migrationBannerSummary = getString(R.string.resume_migration_banner_description_fallback)
    }

    override fun onResume() {
        super.onResume()
        recentAccessViewModel.loadRecentAccessApps(maxNumEntries = 3)
        homeViewModel.loadConnectedApps()
        exportStatusViewModel.loadScheduledExportStatus()
        if (isPersonalHealthRecordEnabled()) {
            homeViewModel.loadHasAnyMedicalData()
            if (isLockScreenBannerAvailable) {
                homeViewModel.loadShouldShowLockScreenBanner(
                    getSharedPreference(),
                    requireContext(),
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recentAccessViewModel.loadRecentAccessApps(maxNumEntries = 3)
        recentAccessViewModel.recentAccessApps.observe(viewLifecycleOwner) { recentAppsState ->
            when (recentAppsState) {
                is RecentAccessState.WithData -> {
                    updateRecentApps(recentAppsState.recentAccessEntries)
                }
                is RecentAccessState.Error -> {
                    updateRecentAppsWithError()
                }
                else -> {
                    updateRecentApps(emptyList())
                }
            }
        }
        homeViewModel.connectedApps.observe(viewLifecycleOwner) { connectedApps ->
            updateConnectedApps(connectedApps)
            updateOnboardingBanner(connectedApps)
        }
        migrationViewModel.migrationState.observe(viewLifecycleOwner) { migrationState ->
            when (migrationState) {
                is MigrationViewModel.MigrationFragmentState.WithData -> {
                    showMigrationState(migrationState.migrationRestoreState)
                }
                else -> {
                    // do nothing
                }
            }
        }
        exportStatusViewModel.storedScheduledExportStatus.observe(viewLifecycleOwner) {
            scheduledExportUiStatus ->
            when (scheduledExportUiStatus) {
                is ScheduledExportUiStatus.WithData -> {
                    maybeShowExportErrorBanner(scheduledExportUiStatus.scheduledExportUiState)
                }
                else -> {
                    // do nothing
                }
            }
        }

        if (isPersonalHealthRecordEnabled()) {
            homeViewModel.loadHasAnyMedicalData()
            homeViewModel.hasAnyMedicalData.observe(viewLifecycleOwner) { hasAnyMedicalData ->
                browseMedicalDataPreference.isVisible = hasAnyMedicalData ?: false
            }
            if (isLockScreenBannerAvailable) {
                val sharedPreference = getSharedPreference()
                homeViewModel.loadShouldShowLockScreenBanner(sharedPreference, requireContext())
                homeViewModel.showLockScreenBanner.observe(viewLifecycleOwner) { bannerState ->
                    if (bannerState is LockScreenBannerState.ShowBanner) {
                        addLockScreenBanner(bannerState)
                    } else {
                        removeLockScreenBanner()
                    }
                }
            }
        }
    }

    private fun isLockScreenBannerAlreadyAdded(): Boolean {
        return bannerGroup.findPreference<HealthBannerPreference>(LOCK_SCREEN_BANNER_KEY) != null
    }

    private fun addLockScreenBanner(bannerState: LockScreenBannerState.ShowBanner) {
        if (!isLockScreenBannerAlreadyAdded()) {
            bannerGroup.addPreference(getLockScreenBanner(bannerState))
        }
    }

    private fun removeLockScreenBanner() {
        bannerGroup.removePreferenceRecursively(LOCK_SCREEN_BANNER_KEY)
    }

    private fun showMigrationState(migrationRestoreState: MigrationRestoreState) {
        bannerGroup.removePreferenceRecursively(MIGRATION_BANNER_PREFERENCE_KEY)
        bannerGroup.removePreferenceRecursively(DATA_RESTORE_BANNER_PREFERENCE_KEY)

        val (migrationUiState, dataRestoreUiState, dataRestoreError) = migrationRestoreState

        if (
            dataRestoreUiState == DataRestoreUiState.PENDING &&
                dataRestoreError == MigrationRestoreState.DataRestoreUiError.ERROR_VERSION_DIFF
        ) {
            bannerGroup.addPreference(getDataRestorePendingBanner())
        } else if (
            migrationUiState in
                listOf(
                    MigrationUiState.ALLOWED_PAUSED,
                    MigrationUiState.ALLOWED_NOT_STARTED,
                    MigrationUiState.MODULE_UPGRADE_REQUIRED,
                    MigrationUiState.APP_UPGRADE_REQUIRED,
                )
        ) {
            bannerGroup.addPreference(getMigrationBanner())
        } else if (migrationUiState == MigrationUiState.COMPLETE) {
            maybeShowWhatsNewDialog(requireContext())
        } else if (migrationUiState == MigrationUiState.ALLOWED_ERROR) {
            maybeShowMigrationNotCompleteDialog()
        }
    }

    private fun maybeShowMigrationNotCompleteDialog() {
        val sharedPreference = getSharedPreference()
        val dialogSeen = sharedPreference.getBoolean(MIGRATION_NOT_COMPLETE_DIALOG_SEEN, false)

        if (!dialogSeen) {
            AlertDialogBuilder(this, MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_CONTAINER)
                .setTitle(R.string.migration_not_complete_dialog_title)
                .setMessage(R.string.migration_not_complete_dialog_content)
                .setCancelable(false)
                .setNegativeButton(
                    R.string.migration_whats_new_dialog_button,
                    MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_BUTTON,
                ) { _, _ ->
                    sharedPreference.edit().apply {
                        putBoolean(MIGRATION_NOT_COMPLETE_DIALOG_SEEN, true)
                        apply()
                    }
                }
                .create()
                .show()
        }
    }

    // region Banners
    private fun maybeShowExportErrorBanner(scheduledExportUiState: ScheduledExportUiState) {
        if (bannerGroup.findPreference<Preference>(EXPORT_ERROR_BANNER_PREFERENCE_KEY) != null) {
            bannerGroup.removePreferenceRecursively(EXPORT_ERROR_BANNER_PREFERENCE_KEY)
        }
        if (
            scheduledExportUiState.dataExportError !=
                ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE
        ) {
            scheduledExportUiState.lastFailedExportTime?.let {
                bannerGroup.addPreference(getExportFileAccessErrorBanner(it))
            }
        }
    }

    private fun getExportFileAccessErrorBanner(
        lastFailedExportTime: Instant
    ): HealthBannerPreference {
        return HealthBannerPreference(requireContext(), HomePageElement.EXPORT_ERROR_BANNER).also {
            banner ->
            banner.setPositiveButton(
                text = getString(R.string.export_file_access_error_banner_button),
                logName = HomePageElement.EXPORT_ERROR_BANNER_BUTTON,
            ) {
                findNavController().navigate(R.id.action_homeFragment_to_exportSetupActivity)
            }

            banner.title = getString(R.string.export_file_access_error_banner_title)
            banner.summary =
                getString(
                    R.string.export_file_access_error_banner_summary,
                    dateFormatter.formatLongDate(lastFailedExportTime),
                )
            banner.icon =
                AttributeResolver.getNullableDrawable(requireContext(), R.attr.warningIcon)
            banner.key = EXPORT_ERROR_BANNER_PREFERENCE_KEY
        }
    }

    private fun getMigrationBanner(): HealthBannerPreference {
        return HealthBannerPreference(requireContext(), MigrationElement.MIGRATION_RESUME_BANNER)
            .also { banner ->
                banner.setPositiveButton(
                    text = getString(R.string.resume_migration_banner_button),
                    logName = MigrationElement.MIGRATION_RESUME_BANNER_BUTTON,
                ) {
                    findNavController().navigate(R.id.action_homeFragment_to_migrationActivity)
                }

                banner.icon =
                    AttributeResolver.getNullableDrawable(
                        requireContext(),
                        R.attr.settingsAlertIcon,
                    )
                banner.title = getString(R.string.resume_migration_banner_title)
                banner.summary = migrationBannerSummary
                banner.key = MIGRATION_BANNER_PREFERENCE_KEY
            }
    }

    private fun getDataRestorePendingBanner(): HealthBannerPreference {
        return HealthBannerPreference(requireContext(), DataRestoreElement.RESTORE_PENDING_BANNER)
            .also { banner ->
                banner.setPositiveButton(
                    text = getString(R.string.data_restore_pending_banner_button),
                    logName = DataRestoreElement.RESTORE_PENDING_BANNER_UPDATE_BUTTON,
                ) {
                    findNavController().navigate(R.id.action_homeFragment_to_systemUpdateActivity)
                }

                banner.icon =
                    AttributeResolver.getNullableDrawable(requireContext(), R.attr.updateNeededIcon)
                banner.title = getString(R.string.data_restore_pending_banner_title)
                banner.summary = getString(R.string.data_restore_pending_banner_content)
                banner.key = DATA_RESTORE_BANNER_PREFERENCE_KEY
            }
    }

    private fun getStartUsingHealthConnectBanner(): HealthBannerPreference {
        return HealthBannerPreference(requireContext(), UnknownGenericElement.UNKNOWN_BANNER)
            .also { banner ->
                banner.title = resources.getString(R.string.start_using_hc_banner_title)
                banner.summary = resources.getString(R.string.start_using_hc_banner_content)
                banner.icon =
                    AttributeResolver.getNullableDrawable(
                        requireContext(),
                        R.attr.healthConnectIcon,
                    )
                banner.key = START_USING_HC_BANNER_KEY

                banner.setPositiveButton(
                    text = getString(R.string.start_using_hc_set_up_button),
                    logName = UnknownGenericElement.UNKNOWN_BANNER_BUTTON,
                ) {
                    findNavController().navigate(R.id.action_homeFragment_to_connectedAppsFragment)
                }

                banner.setDismissButtonVisible(true)
                banner.setDismissButton(logName = UnknownGenericElement.UNKNOWN_BANNER_BUTTON) {
                    setBannerSeen(Constants.START_USING_HC_BANNER_SEEN)
                    bannerGroup.removePreferenceRecursively(START_USING_HC_BANNER_KEY)
                }
            }
    }

    private fun getConnectMoreAppsBanner(appMetadata: AppMetadata): HealthBannerPreference {
        return HealthBannerPreference(requireContext(), UnknownGenericElement.UNKNOWN_BANNER)
            .also { banner ->
                banner.setAttentionLevel(BannerMessagePreference.AttentionLevel.NORMAL)
                banner.title = resources.getString(R.string.connect_more_apps_banner_title)
                banner.summary =
                    resources.getString(
                        R.string.connect_more_apps_banner_content,
                        appMetadata.appName,
                    )
                banner.icon =
                    AttributeResolver.getNullableDrawable(requireContext(), R.attr.syncIcon)
                banner.key = CONNECT_MORE_APPS_BANNER_KEY

                banner.setPositiveButton(
                    text = getString(R.string.connect_more_apps_set_up_button),
                    logName = UnknownGenericElement.UNKNOWN_BANNER_BUTTON,
                ) {
                    findNavController().navigate(R.id.action_homeFragment_to_connectedAppsFragment)
                }

                banner.setDismissButton(logName = UnknownGenericElement.UNKNOWN_BANNER_BUTTON) {
                    setBannerSeen(Constants.CONNECT_MORE_APPS_BANNER_SEEN)
                    bannerGroup.removePreferenceRecursively(CONNECT_MORE_APPS_BANNER_KEY)
                }
            }
    }

    private fun getSeeCompatibleAppsBanner(appMetadata: AppMetadata): HealthBannerPreference {
        return HealthBannerPreference(requireContext(), UnknownGenericElement.UNKNOWN_BANNER)
            .also { banner ->
                banner.title = resources.getString(R.string.see_compatible_apps_banner_title)
                banner.summary =
                    resources.getString(
                        R.string.see_compatible_apps_banner_content,
                        appMetadata.appName,
                    )
                banner.icon =
                    AttributeResolver.getNullableDrawable(
                        requireContext(),
                        R.attr.seeAllCompatibleAppsIcon,
                    )
                banner.key = SEE_COMPATIBLE_APPS_BANNER_KEY

                banner.setPositiveButtonText(getString(R.string.see_compatible_apps_set_up_button))
                banner.setPositiveButtonOnClickListener {
                    findNavController().navigate(R.id.action_homeFragment_to_playstoreActivity)
                }

                banner.setDismissButtonVisible(true)
                banner.setDismissButtonOnClickListener {
                    setBannerSeen(Constants.SEE_MORE_COMPATIBLE_APPS_BANNER_SEEN)
                    bannerGroup.removePreferenceRecursively(SEE_COMPATIBLE_APPS_BANNER_KEY)
                }
            }
    }

    private fun getLockScreenBanner(
        bannerState: LockScreenBannerState.ShowBanner
    ): HealthBannerPreference {
        return HealthBannerPreference(requireContext(), HomePageElement.LOCK_SCREEN_BANNER).also {
            banner ->
            banner.title = resources.getString(R.string.lock_screen_banner_title)
            banner.summary = resources.getString(R.string.lock_screen_banner_content)
            banner.icon = AttributeResolver.getNullableDrawable(requireContext(), R.attr.lockIcon)
            banner.key = LOCK_SCREEN_BANNER_KEY

            banner.setPositiveButton(
                text = getString(R.string.lock_screen_banner_button),
                logName = HomePageElement.LOCK_SCREEN_BANNER_BUTTON,
            ) {
                updateLockScreenBannerSeen(bannerState)
                navigateToSecuritySettings()
            }

            banner.setDismissButtonVisible(true)
            banner.setDismissButton(logName = HomePageElement.LOCK_SCREEN_BANNER_DISMISS_BUTTON) {
                updateLockScreenBannerSeen(bannerState)
                bannerGroup.removePreferenceRecursively(LOCK_SCREEN_BANNER_KEY)
            }
        }
    }

    private fun updateLockScreenBannerSeen(bannerState: LockScreenBannerState.ShowBanner) {
        val sharedPreference = getSharedPreference()
        sharedPreference.edit().apply {
            val anyFitnessData = bannerState.hasAnyFitnessData
            val anyMedicalData = bannerState.hasAnyMedicalData

            if (!(anyFitnessData || anyMedicalData)) {
                // This should not happen.
                putBoolean(LOCK_SCREEN_BANNER_SEEN_FITNESS, true)
                putBoolean(LOCK_SCREEN_BANNER_SEEN_MEDICAL, true)
            }
            if (anyFitnessData) {
                putBoolean(LOCK_SCREEN_BANNER_SEEN_FITNESS, true)
            }
            if (anyMedicalData) {
                putBoolean(LOCK_SCREEN_BANNER_SEEN_MEDICAL, true)
            }
            apply()
        }
    }

    // endregion

    private fun navigateToSecuritySettings() {
        startActivity(securitySettingsIntent)
    }

    private fun updateConnectedApps(connectedApps: List<ConnectedAppMetadata>) {
        val connectedAppsGroup = connectedApps.groupBy { it.status }
        val numAllowedApps = connectedAppsGroup[ConnectedAppStatus.ALLOWED].orEmpty().size
        val numNotAllowedApps = connectedAppsGroup[ConnectedAppStatus.DENIED].orEmpty().size
        val numTotalApps = numAllowedApps + numNotAllowedApps

        if (numTotalApps == 0) {
            appPermissionsPreference.summary =
                getString(R.string.connected_apps_button_no_permissions_subtitle)
        } else if (numAllowedApps == numTotalApps) {
            appPermissionsPreference.summary =
                MessageFormat.format(
                    getString(R.string.connected_apps_connected_subtitle),
                    mapOf("count" to numAllowedApps),
                )
        } else {
            appPermissionsPreference.summary =
                getString(
                    if (numAllowedApps == 1) R.string.only_one_connected_app_button_subtitle
                    else R.string.connected_apps_button_subtitle,
                    numAllowedApps.toString(),
                    numTotalApps.toString(),
                )
        }
    }

    private fun updateOnboardingBanner(connectedApps: List<ConnectedAppMetadata>) {
        removeAllOnboardingBanners()

        if (!onboarding()) {
            return
        }

        val connectedAppsGroup = connectedApps.groupBy { it.status }
        val numAllowedApps = connectedAppsGroup[ConnectedAppStatus.ALLOWED].orEmpty().size
        val numNotAllowedApps = connectedAppsGroup[ConnectedAppStatus.DENIED].orEmpty().size
        val numTotalApps = numAllowedApps + numNotAllowedApps

        val sharedPreference = getSharedPreference()

        if (numTotalApps > 0 && numAllowedApps == 0) {
            // No apps connected, one available
            // Show if not dismissed
            val bannerSeen =
                sharedPreference.getBoolean(Constants.START_USING_HC_BANNER_SEEN, false)
            if (!bannerSeen) {
                val banner = getStartUsingHealthConnectBanner()
                bannerGroup.addPreference(banner)
            }
        } else if (numAllowedApps == 1 && numNotAllowedApps > 0) {
            // 1 app connected, at least one available to connect
            val bannerSeen =
                sharedPreference.getBoolean(Constants.CONNECT_MORE_APPS_BANNER_SEEN, false)
            if (!bannerSeen) {
                val banner =
                    getConnectMoreAppsBanner(
                        connectedAppsGroup[ConnectedAppStatus.ALLOWED]!![0].appMetadata
                    )
                bannerGroup.addPreference(banner)
            }
        } else if (numAllowedApps == 1 && numTotalApps == 1) {
            // 1 app connected, no more available to connect
            if (deviceInfoUtils.isPlayStoreAvailable(requireContext())) {
                val bannerSeen =
                    sharedPreference.getBoolean(
                        Constants.SEE_MORE_COMPATIBLE_APPS_BANNER_SEEN,
                        false,
                    )
                if (!bannerSeen) {
                    val banner =
                        getSeeCompatibleAppsBanner(
                            connectedAppsGroup[ConnectedAppStatus.ALLOWED]!![0].appMetadata
                        )
                    bannerGroup.addPreference(banner)
                }
            }
        }
    }

    private fun removeAllOnboardingBanners() {
        bannerGroup.removePreferenceRecursively(START_USING_HC_BANNER_KEY)
        bannerGroup.removePreferenceRecursively(CONNECTED_APPS_PREFERENCE_KEY)
        bannerGroup.removePreferenceRecursively(SEE_COMPATIBLE_APPS_BANNER_KEY)
    }

    private fun updateRecentApps(recentAppsList: List<RecentAccessEntry>) {
        if (SettingsThemeHelper.isExpressiveTheme(requireContext()) && recentAppsList.isEmpty()) {
            noRecentAccessPreference.isVisible = true
            recentAccessPreferenceGroup.isVisible = false
            return
        }

        noRecentAccessPreference.isVisible = false
        recentAccessPreferenceGroup.isVisible = true
        recentAccessPreferenceGroup.removeAll()

        if (recentAppsList.isEmpty()) {
            recentAccessPreferenceGroup.addPreference(
                Preference(requireContext())
                    .also { it.setSummary(R.string.no_recent_access) }
                    .also { it.isSelectable = false }
            )
        } else {
            recentAppsList.forEach { recentApp ->
                val newRecentAccessPreference = getRecentAccessPreference(recentApp)
                recentAccessPreferenceGroup.addPreference(newRecentAccessPreference)
            }
            recentAccessPreferenceGroup.addPreference(getSeeAllPreference())
        }
    }

    private fun updateRecentAppsWithError() {
        noRecentAccessPreference.isVisible = false
        recentAccessPreferenceGroup.isVisible = true
        recentAccessPreferenceGroup.removeAll()
        recentAccessPreferenceGroup.addPreference(
            HealthPreference(requireContext()).also {
                it.title = getString(R.string.recent_access_error)
                it.isSelectable = false
                it.setIcon(AttributeResolver.getResource(requireContext(), R.attr.warningIcon))
            }
        )
    }

    private fun getSeeAllPreference(): Preference {
        val seeAllPreference =
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                HealthButtonPreference(requireContext()).also {
                    it.setTitle(R.string.recent_access_view_all_button)
                    it.setIcon(AttributeResolver.getResource(requireContext(), R.attr.optionsIcon))
                    it.logName = HomePageElement.SEE_ALL_RECENT_ACCESS_BUTTON
                    it.setOnClickListener {
                        findNavController()
                            .navigate(R.id.action_homeFragment_to_recentAccessFragment)
                    }
                }
            } else {
                HealthPreference(requireContext()).also {
                    it.setTitle(R.string.show_recent_access_entries_button_title)
                    it.setIcon(AttributeResolver.getResource(requireContext(), R.attr.seeAllIcon))
                    it.logName = HomePageElement.SEE_ALL_RECENT_ACCESS_BUTTON
                    it.setOnPreferenceClickListener {
                        findNavController()
                            .navigate(R.id.action_homeFragment_to_recentAccessFragment)
                        true
                    }
                }
            }

        return seeAllPreference
    }

    private fun getRecentAccessPreference(recentApp: RecentAccessEntry): HealthPreference {
        return if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            HealthPreference(requireContext()).also { newPreference ->
                newPreference.logName = RecentAccessElement.RECENT_ACCESS_ENTRY_BUTTON
                newPreference.title = recentApp.metadata.appName
                newPreference.icon = recentApp.metadata.icon
                newPreference.summary =
                    formatRecentAccessTime(recentApp.instantTime, timeSource, requireContext())
                if (!recentApp.isInactive) {
                    newPreference.setOnPreferenceClickListener {
                        navigateToAppInfoScreen(recentApp)
                        true
                    }
                }
            }
        } else {
            RecentAccessPreference(requireContext(), recentApp, timeSource, false).also {
                newPreference ->
                if (!recentApp.isInactive) {
                    newPreference.setOnPreferenceClickListener {
                        navigateToAppInfoScreen(recentApp)
                        true
                    }
                }
            }
        }
    }

    private fun navigateToAppInfoScreen(recentApp: RecentAccessEntry) {
        val appPermissionsType = recentApp.appPermissionsType
        val navigationId =
            when (appPermissionsType) {
                AppPermissionsType.FITNESS_PERMISSIONS_ONLY ->
                    R.id.action_homeFragment_to_fitnessAppFragment
                AppPermissionsType.MEDICAL_PERMISSIONS_ONLY ->
                    R.id.action_homeFragment_to_medicalAppFragment
                AppPermissionsType.COMBINED_PERMISSIONS ->
                    R.id.action_homeFragment_to_combinedPermissionsFragment
            }
        findNavController()
            .navigate(
                navigationId,
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to recentApp.metadata.packageName,
                    Constants.EXTRA_APP_NAME to recentApp.metadata.appName,
                ),
            )
    }

    private fun getSharedPreference() =
        requireActivity().getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)

    private fun setBannerSeen(sharedPrefKey: String, seen: Boolean = true) {
        val sharedPreference = getSharedPreference()
        sharedPreference.edit().apply {
            putBoolean(sharedPrefKey, seen)
            apply()
        }
    }
}
