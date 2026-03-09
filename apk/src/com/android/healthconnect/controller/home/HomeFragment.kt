/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.HealthAppPreference
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.shared.preference.HealthButtonPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.NoAppsPreference
import com.android.healthconnect.controller.shared.preference.NotConnectedAppPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.NewHomePageElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.navigateSafe
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.setupMenu
import com.android.healthconnect.controller.utils.tryLaunchAppOnboardingActivity
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import com.android.healthfitness.flags.Flags.stepTrackingEnabled
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint(HealthPreferenceFragment::class)
class HomeFragment : Hilt_HomeFragment() {

    companion object {
        private const val BANNER_GROUP = "banner_group"
        private const val YOUR_HEALTH_APPS_CATEGORY = "your_health_apps"
        private const val DATA_AND_ACCESS = "data_and_access"
        private const val RECENT_ACCESS = "recent_access"
        private const val DEVICES = "devices"
        private const val MANAGE_DATA = "manage_data"
        private const val FOOTER = "footer"
    }

    init {
        setPageName(PageName.NEW_HOME_PAGE)
    }

    private val homeViewModel: HomeViewModel by viewModels()

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var healthConnectLogger: HealthConnectLogger
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private val bannerGroup: BannerMessagePreferenceGroup by pref(BANNER_GROUP)
    private val yourHealthAppsCategory: PreferenceGroup by pref(YOUR_HEALTH_APPS_CATEGORY)
    private val dataAndAccessPreference: HealthPreference by pref(DATA_AND_ACCESS)
    private val recentAccessPreference: HealthPreference by pref(RECENT_ACCESS)
    private val devicesPreference: HealthPreference by pref(DEVICES)
    private val manageDataPreference: HealthPreference by pref(MANAGE_DATA)
    private val footer: FooterPreference by pref(FOOTER)
    private val dateFormatter: LocalDateTimeFormatter by lazy {
        LocalDateTimeFormatter(requireContext())
    }

    private val bannerFactory: BannerFactory by lazy {
        BannerFactory(requireContext(), dateFormatter, ::handleBannerAction)
    }

    private var bannerToDismissOnStop: HomeViewModel.BannerData? = null

    private fun handleBannerAction(action: BannerAction) {
        when (action) {
            is BannerAction.Navigate ->
                findNavController().navigateSafe(R.id.newHomeFragment, action.destinationId)
            is BannerAction.StartActivity -> startActivity(action.intent)
            is BannerAction.Dismiss -> homeViewModel.onDismissBanner(action.banner)
            is BannerAction.NavigateAndDismiss -> {
                findNavController().navigateSafe(R.id.newHomeFragment, action.destinationId)
                homeViewModel.onDismissBanner(action.banner)
            }
            is BannerAction.StartActivityAndDismiss -> {
                startActivity(action.intent)
                bannerToDismissOnStop = action.banner
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.home_screen, rootKey)

        dataAndAccessPreference.logName = NewHomePageElement.DATA_AND_ACCESS_BUTTON
        dataAndAccessPreference.setOnPreferenceClickListener {
            findNavController()
                .navigateSafe(R.id.newHomeFragment, R.id.action_newHomeFragment_to_dataAndAccess)
            true
        }

        recentAccessPreference.logName = NewHomePageElement.RECENT_ACCESS_BUTTON
        recentAccessPreference.setOnPreferenceClickListener {
            findNavController()
                .navigateSafe(R.id.newHomeFragment, R.id.action_newHomeFragment_to_recentAccess)
            true
        }

        if (stepTrackingEnabled() || deviceDataProvidersApi()) {
            devicesPreference.isVisible = true
            devicesPreference.logName = NewHomePageElement.DEVICES_BUTTON
            devicesPreference.setOnPreferenceClickListener {
                val action =
                    if (deviceDataProvidersApi()) R.id.action_newHomeFragment_to_newDevicesFragment
                    else R.id.action_newHomeFragment_to_connectedDevicesFragment
                findNavController().navigateSafe(R.id.newHomeFragment, action)
                true
            }
        } else {
            devicesPreference.isVisible = false
        }

        manageDataPreference.logName = NewHomePageElement.MANAGE_DATA_BUTTON
        manageDataPreference.setOnPreferenceClickListener {
            findNavController()
                .navigateSafe(R.id.newHomeFragment, R.id.action_newHomeFragment_to_manageData)
            true
        }

        healthConnectLogger.logImpression(NewHomePageElement.HOME_PAGE_FOOTER)
        healthConnectLogger.logImpression(NewHomePageElement.HOME_PAGE_FOOTER_LINK)
        footer.setLearnMoreText(getString(R.string.home_screen_footer_link))
        footer.setLearnMoreAction {
            healthConnectLogger.logInteraction(NewHomePageElement.HOME_PAGE_FOOTER_LINK)
            deviceInfoUtils.openHCGetStartedLink(requireActivity())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu(
            R.menu.show_system_with_send_feedback_and_help,
            viewLifecycleOwner,
            healthConnectLogger,
            onPrepareMenu = { menu ->
                val showHideSystemMenuItem = menu.findItem(R.id.menu_show_hide_system)
                showHideSystemMenuItem?.let {
                    val isShowingSystem = homeViewModel.showSystemApps
                    it.setTitle(
                        if (isShowingSystem) {
                            R.string.menu_hide_system
                        } else {
                            R.string.menu_show_system
                        }
                    )
                }
            },
        ) { menuItem ->
            if (menuItem.itemId == R.id.menu_show_hide_system) {
                homeViewModel.setShouldShowSystemApps(!homeViewModel.showSystemApps)
                requireActivity().invalidateMenu()
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.homeFragmentState.collect { state ->
                    when (state) {
                        is HomeViewModel.HomeFragmentState.Loading -> setLoading(isLoading = true)
                        is HomeViewModel.HomeFragmentState.Error -> {
                            setLoading(false)
                            setError(true)
                        }
                        is HomeViewModel.HomeFragmentState.WithData -> {
                            setLoading(false)
                            updateBanners(state.bannerState)
                            updateScreen(state)
                            maybeShowDialog(state.migrationDialog)
                        }
                    }
                }
            }
        }
    }

    private fun maybeShowDialog(dialog: HomeViewModel.MigrationDialog) {
        when (dialog) {
            is HomeViewModel.MigrationDialog.MigrationCompleteDialog -> {
                showMigrationCompleteDialog()
            }
            is HomeViewModel.MigrationDialog.MigrationNotCompleteDialog -> {
                showMigrationNotCompleteDialog()
            }
            else -> {
                // Do nothing
            }
        }
    }

    private fun showMigrationCompleteDialog() {
        AlertDialogBuilder(this, MigrationElement.MIGRATION_DONE_DIALOG_CONTAINER)
            .setTitle(R.string.migration_whats_new_dialog_title)
            .setMessage(R.string.migration_whats_new_dialog_content)
            .setCancelable(false)
            .setNegativeButton(
                R.string.migration_whats_new_dialog_button,
                MigrationElement.MIGRATION_DONE_DIALOG_BUTTON,
            ) { _, _ ->
                homeViewModel.onDismissDialog(HomeViewModel.MigrationDialog.MigrationCompleteDialog)
            }
            .create()
            .show()
    }

    private fun showMigrationNotCompleteDialog() {
        AlertDialogBuilder(this, MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_CONTAINER)
            .setTitle(R.string.migration_not_complete_dialog_title)
            .setMessage(R.string.migration_not_complete_dialog_content)
            .setCancelable(false)
            .setNegativeButton(
                R.string.migration_whats_new_dialog_button,
                MigrationElement.MIGRATION_NOT_COMPLETE_DIALOG_BUTTON,
            ) { _, _ ->
                homeViewModel.onDismissDialog(
                    HomeViewModel.MigrationDialog.MigrationNotCompleteDialog
                )
            }
            .create()
            .show()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadInitialData()
    }

    override fun onStop() {
        super.onStop()
        bannerToDismissOnStop?.let {
            homeViewModel.onDismissBanner(it)
            bannerToDismissOnStop = null
        }
    }

    private fun updateBanners(bannerState: HomeViewModel.HomeBannerState) {
        bannerGroup.removeAll()
        if (bannerState is HomeViewModel.HomeBannerState.ShowBanners) {
            bannerState.banners.forEach { bannerData ->
                bannerGroup.addPreference(bannerFactory.getBanner(bannerData))
            }
        }
    }

    private fun updateScreen(homeFragmentState: HomeViewModel.HomeFragmentState.WithData) {
        yourHealthAppsCategory.removeAll()
        if (homeFragmentState.connectedApps.isEmpty()) {
            yourHealthAppsCategory.addPreference(getNoAppsPreference())
        } else {
            homeFragmentState.connectedApps.take(5).forEach { app ->
                val appPreference =
                    if (app.status == ConnectedAppStatus.DENIED) {
                        NotConnectedAppPreference(requireContext(), appMetadata = app.appMetadata)
                            .also {
                                it.logName = NewHomePageElement.NOT_CONNECTED_APP_HOME_SCREEN_BUTTON
                            }
                    } else {
                        HealthAppPreference(requireContext(), app.appMetadata).also {
                            it.logName = NewHomePageElement.CONNECTED_APP_HOME_SCREEN_BUTTON
                        }
                    }
                appPreference.setOnPreferenceClickListener {
                    navigateToAppInfoOrOnboarding(app)
                    true
                }

                yourHealthAppsCategory.addPreference(appPreference)
            }
        }

        if (homeFragmentState.showSeeMoreHealthApps) {
            yourHealthAppsCategory.addPreference(getSeeAllPreference())
        }
    }

    private fun getNoAppsPreference(): NoAppsPreference {
        return NoAppsPreference(requireContext()).also {
            it.title = getString(R.string.empty_apps_section_title)
            if (deviceInfoUtils.isPlayStoreAvailable(requireContext())) {
                it.setLearnMoreText(getString(R.string.empty_apps_section_link))
                it.setLearnMoreAction {
                    findNavController()
                        .navigateSafe(
                            R.id.newHomeFragment,
                            R.id.action_newHomeFragment_to_playStoreActivity,
                        )
                    true
                }
            }
            it.setLogNames(
                textLogName = NewHomePageElement.NO_APPS_AVAILABLE_HEADER,
                linkLogName = NewHomePageElement.NO_APPS_AVAILABLE_LINK,
            )
        }
    }

    private fun getSeeAllPreference(): Preference {
        val seeAllPreference =
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                HealthButtonPreference(requireContext()).also {
                    it.setTitle(R.string.see_all_connected_apps_button)
                    it.setIcon(AttributeResolver.getResource(requireContext(), R.attr.optionsIcon))
                    it.setOnClickListener {
                        findNavController()
                            .navigateSafe(
                                R.id.newHomeFragment,
                                R.id.action_newHomeFragment_to_connectedAppsFragment,
                            )
                    }
                    it.logName = NewHomePageElement.SEE_ALL_CONNECTED_APPS_HOME_SCREEN_BUTTON
                }
            } else {
                HealthPreference(requireContext()).also {
                    it.setTitle(R.string.see_all_connected_apps_button)
                    it.setIcon(AttributeResolver.getResource(requireContext(), R.attr.seeAllIcon))
                    it.logName = NewHomePageElement.SEE_ALL_CONNECTED_APPS_HOME_SCREEN_BUTTON
                    it.setOnPreferenceClickListener {
                        findNavController()
                            .navigateSafe(
                                R.id.newHomeFragment,
                                R.id.action_newHomeFragment_to_connectedAppsFragment,
                            )
                        true
                    }
                }
            }

        return seeAllPreference
    }

    private fun navigateToAppInfoOrOnboarding(app: ConnectedAppMetadata) {
        val appPermissionsType = app.permissionsType
        val navigationId =
            when (appPermissionsType) {
                AppPermissionsType.FITNESS_PERMISSIONS_ONLY ->
                    R.id.action_newHomeFragment_to_fitnessAppFragment

                AppPermissionsType.MEDICAL_PERMISSIONS_ONLY ->
                    R.id.action_newHomeFragment_to_medicalAppFragment

                AppPermissionsType.COMBINED_PERMISSIONS ->
                    R.id.action_newHomeFragment_to_combinedPermissionsFragment
            }

        if (
            app.status == ConnectedAppStatus.DENIED &&
                tryLaunchAppOnboardingActivity(healthPermissionReader, app.appMetadata.packageName)
        ) {
            return
        }
        findNavController()
            .navigateSafe(
                R.id.newHomeFragment,
                navigationId,
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to app.appMetadata.packageName,
                    Constants.EXTRA_APP_NAME to app.appMetadata.appName,
                ),
            )
    }
}
