/**
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.onboarding

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.Constants.SHOW_MANAGE_APP_SECTION
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.shared.preference.HealthSetupHeaderPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.tryLaunchAppOnboardingActivity
import com.android.settingslib.widget.AppPreference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.TopIntroPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment for screens to sync the first two fitness apps to Health Connect. */
@AndroidEntryPoint(HealthSetupFragment::class)
class ConnectAppsOnboardingFragment : Hilt_ConnectAppsOnboardingFragment() {

    private val viewModel: OnboardingViewModel by activityViewModels()
    private val appsCategory: PreferenceGroup by pref("apps_category")
    private val topIntro: TopIntroPreference by pref("top_intro")
    private val header: HealthSetupHeaderPreference by pref("header_pref")
    private val footer: FooterPreference by pref("footer_pref")
    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    init {
        // TODO (b/403259167) add telemetry
        this.setPageName(PageName.UNKNOWN_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.connect_apps_onboarding_screen, rootKey)
        footer.setLearnMoreText(getString(R.string.apps_onboarding_footer_link))
        footer.setLearnMoreAction { deviceInfoUtils.openHCGetStartedLink(requireActivity()) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideButtons()

        viewModel.connectedApps.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OnboardingViewModel.OnboardingFragmentState.Loading -> {
                    setLoading(true)
                }
                is OnboardingViewModel.OnboardingFragmentState.Error -> {
                    setError(true)
                }
                is OnboardingViewModel.OnboardingFragmentState.WithData -> {
                    setLoading(false)
                    val appsToConnect = state.connectedApps
                    val allowedApps =
                        appsToConnect.groupBy { it.isConnected }.getOrDefault(true, emptyList())

                    if (appsToConnect.isEmpty()) {
                        requireActivity().finish()
                    }

                    updateHeaderAndTopIntro(allowedApps)

                    if (allowedApps.size >= 2) {
                        updateAppsForConnection(allowedApps, true)
                        updateDoneButton()
                    } else {
                        updateAppsForConnection(appsToConnect)
                        updateSetupLaterButton()
                    }
                }
            }
        }
    }

    private fun updateSetupLaterButton() {
        val setupLaterButton = primaryButtonOutline
        hideSecondaryButton()
        setupLaterButton.text = getString(R.string.set_up_later)
        setupLaterButton.setOnClickListener {
            // TODO (b/399086212) set banner state
            requireActivity().finish()
        }
    }

    private fun updateDoneButton() {
        val doneButton = getPrimaryButtonFull()
        hideSecondaryButton()
        doneButton.text = getString(R.string.delete_dialog_done_button)
        doneButton.setOnClickListener {
            // TODO (b/399086212) set banner state
            requireActivity().finish()
        }
    }

    private fun updateHeaderAndTopIntro(allowedApps: List<ConnectedFitnessAppMetadata>) {
        if (allowedApps.size == 0) {
            header.title = getString(R.string.connect_first_two_apps_title)
            header.icon = AttributeResolver.getDrawable(requireContext(), R.attr.healthConnectIcon)
            topIntro.title = getString(R.string.connect_first_two_apps_description)
        } else if (allowedApps.size == 1) {
            header.title = getString(R.string.connect_second_app_title)
            header.icon = AttributeResolver.getDrawable(requireContext(), R.attr.syncIcon)
            topIntro.title =
                getString(
                    R.string.connect_second_app_description,
                    allowedApps[0].appMetadata.appName,
                )
        } else {
            // TODO (b/399088239) maybe show only apps with intent and add navigation
            header.title = getString(R.string.almost_done_title)
            header.icon = AttributeResolver.getDrawable(requireContext(), R.attr.checkmarkIcon)
            topIntro.title = getString(R.string.almost_done_description)
        }
        showContents()
    }

    private fun updateAppsForConnection(
        appsToConnect: List<ConnectedFitnessAppMetadata>,
        onlyAllowed: Boolean = false,
    ) {
        appsCategory.removeAll()
        appsCategory.isVisible = true
        appsToConnect.forEach { app ->
            val newPreference = AppPreference(requireContext())

            appsCategory.addPreference(
                newPreference.also {
                    it.title = app.appMetadata.appName
                    it.icon = app.appMetadata.icon
                    if (app.isConnected) {
                        it.widgetLayoutResource =
                            R.layout.widget_onboarding_app_connected_preference
                        it.isEnabled = onlyAllowed
                    }
                    it.setOnPreferenceClickListener {
                        if (onlyAllowed) {
                            // Start the activities
                            val launchIntent =
                                requireContext()
                                    .packageManager
                                    .getLaunchIntentForPackage(app.appMetadata.packageName)
                            launchIntent?.let { requireContext().startActivity(it) }
                        } else {
                            val onboardingLaunched =
                                tryLaunchAppOnboardingActivity(
                                    healthPermissionReader,
                                    app.appMetadata.packageName,
                                )
                            if (!onboardingLaunched) {
                                findNavController()
                                    .navigate(
                                        R.id
                                            .action_connectAppsOnboardingFragment_to_fitnessAppOnboardingFragment,
                                        bundleOf(
                                            EXTRA_PACKAGE_NAME to app.appMetadata.packageName,
                                            EXTRA_APP_NAME to app.appMetadata.appName,
                                            SHOW_MANAGE_APP_SECTION to false,
                                        ),
                                    )
                            } else {
                                // Mark app as having been interacted with
                                viewModel.setAppInteractedWith(app.appMetadata.packageName)
                            }
                        }

                        true
                    }
                }
            )
        }
    }

    private fun showContents() {
        header.isVisible = true
        topIntro.isVisible = true
        footer.isVisible = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadConnectedApps()
    }
}
