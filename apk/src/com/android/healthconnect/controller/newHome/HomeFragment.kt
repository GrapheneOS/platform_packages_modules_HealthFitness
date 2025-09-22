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

package com.android.healthconnect.controller.newHome

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
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
import com.android.healthconnect.controller.shared.preference.HealthButtonPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.NoAppsPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.CommonOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.tryLaunchAppOnboardingActivity
import com.android.healthfitness.flags.Flags.stepTrackingEnabled
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(HealthPreferenceFragment::class)
class HomeFragment : Hilt_HomeFragment() {

    companion object {
        private const val YOUR_HEALTH_APPS_CATEGORY = "your_health_apps"
        private const val EMPTY_HEALTH_APPS_PREFERENCE = "empty_health_apps"
        private const val YOUR_HEALTH_DATA_CATEGORY = "your_health_data"
        private const val DATA_AND_ACCESS = "data_and_access"
        private const val RECENT_ACCESS = "recent_access"
        private const val PREFERENCES_CATEGORY = "preferences"
        private const val DEVICES = "devices"
        private const val MANAGE_DATA = "manage_data"
        private const val FOOTER = "footer"
    }

    private val homeViewModel: HomeViewModel by viewModels()

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var healthConnectLogger: HealthConnectLogger
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private val yourHealthAppsCategory: PreferenceGroup by pref(YOUR_HEALTH_APPS_CATEGORY)
    private val dataAndAccessPreference: HealthPreference by pref(DATA_AND_ACCESS)
    private val recentAccessPreference: HealthPreference by pref(RECENT_ACCESS)
    private val devicesPreference: HealthPreference by pref(DEVICES)
    private val manageDataPreference: HealthPreference by pref(MANAGE_DATA)
    private val footer: FooterPreference by pref(FOOTER)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.new_home_screen, rootKey)

        dataAndAccessPreference.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_newHomeFragment_to_dataAndAccess)
            true
        }

        recentAccessPreference.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_newHomeFragment_to_recentAccess)
            true
        }

        if (stepTrackingEnabled()) {
            devicesPreference.isVisible = true
            devicesPreference.setOnPreferenceClickListener {
                findNavController()
                    .navigate(R.id.action_newHomeFragment_to_connectedDevicesFragment)
                true
            }
        } else {
            devicesPreference.isVisible = false
        }

        manageDataPreference.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_newHomeFragment_to_manageData)
            true
        }

        footer.setLearnMoreText(getString(R.string.home_screen_footer_link))
        footer.setLearnMoreAction {
            // TODO add telemetry
            healthConnectLogger.logInteraction(
                CommonOnboardingPageElement.MORE_ABOUT_HEALTH_CONNECT_BUTTON
            )
            deviceInfoUtils.openHCGetStartedLink(requireActivity())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.homeFragmentState.observe(viewLifecycleOwner) { homeFragmentState ->
            when (homeFragmentState) {
                is HomeViewModel.HomeFragmentState.Loading -> setLoading(isLoading = true)
                is HomeViewModel.HomeFragmentState.Error -> {
                    setLoading(false)
                    setError(true)
                }
                is HomeViewModel.HomeFragmentState.WithData -> {
                    setLoading(false)
                    updateScreen(homeFragmentState)
                }
            }
        }
    }

    private fun updateScreen(homeFragmentState: HomeViewModel.HomeFragmentState.WithData) {
        yourHealthAppsCategory.removeAll()
        if (homeFragmentState.connectedApps.isEmpty()) {
            yourHealthAppsCategory.addPreference(getNoAppsPreference())
            return
        }

        homeFragmentState.connectedApps.take(5).forEach { app ->
            yourHealthAppsCategory.addPreference(
                HealthAppPreference(context = requireContext(), appMetadata = app.appMetadata)
                    .also {
                        if (app.status == ConnectedAppStatus.DENIED) {
                            it.summary = getString(R.string.app_not_connected_summary)
                        }
                        it.setOnPreferenceClickListener {
                            navigateToAppInfoOrOnboarding(app)
                            true
                        }
                    }
            )
        }

        if (homeFragmentState.connectedApps.size > 5) {
            yourHealthAppsCategory.addPreference(getSeeAllPreference())
        }
    }

    private fun getNoAppsPreference(): NoAppsPreference {
        return NoAppsPreference(requireContext()).also {
            it.title = getString(R.string.empty_apps_section_title)
            it.setLearnMoreText(getString(R.string.empty_apps_section_link))
            it.setLearnMoreAction {
                findNavController().navigate(R.id.action_newHomeFragment_to_playStoreActivity)
                true
            }
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
                            .navigate(R.id.action_newHomeFragment_to_connectedAppsFragment)
                    }
                }
            } else {
                HealthPreference(requireContext()).also {
                    it.setTitle(R.string.see_all_connected_apps_button)
                    it.setIcon(AttributeResolver.getResource(requireContext(), R.attr.seeAllIcon))
                    it.setOnPreferenceClickListener {
                        findNavController()
                            .navigate(R.id.action_newHomeFragment_to_connectedAppsFragment)
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
            .navigate(
                navigationId,
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to app.appMetadata.packageName,
                    Constants.EXTRA_APP_NAME to app.appMetadata.appName,
                ),
            )
    }
}
