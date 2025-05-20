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
import com.android.healthconnect.controller.permissions.connectedapps.HealthAppPreference
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.Constants.SHOW_MANAGE_APP_SECTION
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.shared.preference.HealthSetupHeaderPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.AlmostDonePageElement
import com.android.healthconnect.controller.utils.logging.CommonOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.ConnectSecondAddOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.ConnectTwoAppsOnboardingPageElement
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.tryLaunchAppOnboardingActivity
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
    @Inject lateinit var healthConnectLogger: HealthConnectLogger

    init {
        this.setPageName(PageName.CONNECT_TWO_APPS_ONBOARDING_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.connect_apps_onboarding_screen, rootKey)
        healthConnectLogger.logImpression(
            CommonOnboardingPageElement.MORE_ABOUT_HEALTH_CONNECT_BUTTON
        )
        footer.setLearnMoreText(getString(R.string.apps_onboarding_footer_link))
        footer.setLearnMoreAction {
            healthConnectLogger.logInteraction(
                CommonOnboardingPageElement.MORE_ABOUT_HEALTH_CONNECT_BUTTON
            )
            deviceInfoUtils.openHCGetStartedLink(requireActivity())
        }
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
                is OnboardingViewModel.OnboardingFragmentState.NoApps -> {
                    requireActivity().finish()
                }

                is OnboardingViewModel.OnboardingFragmentState.ZeroAppsConnected -> {
                    setLoading(false)
                    setupZeroAppsConnected(state.potentialApps)
                }

                is OnboardingViewModel.OnboardingFragmentState.OneAppConnected -> {
                    setLoading(false)
                    setupOneAppConnected(state.connectedApp, state.potentialApps)
                }

                is OnboardingViewModel.OnboardingFragmentState.AlmostDone -> {
                    setLoading(false)
                    setupAlmostDone(state.connectedApps)
                }
            }
        }
    }

    private fun setupZeroAppsConnected(potentialApps: List<ConnectedFitnessAppMetadata>) {
        this.setPageName(PageName.CONNECT_TWO_APPS_ONBOARDING_PAGE)

        header.title = getString(R.string.connect_first_two_apps_title)
        header.icon = AttributeResolver.getDrawable(requireContext(), R.attr.healthConnectIcon)
        topIntro.title = getString(R.string.connect_first_two_apps_description)

        showContents()
        updateSetupLaterButton(
            ConnectTwoAppsOnboardingPageElement
                .CONNECT_FIRST_TWO_APPS_ONBOARDING_SET_UP_LATER_BUTTON
        )

        showZeroAppsConnected(potentialApps)
    }

    private fun showZeroAppsConnected(potentialApps: List<ConnectedFitnessAppMetadata>) {
        appsCategory.removeAll()
        appsCategory.isVisible = true
        potentialApps.forEach { app -> appsCategory.addPreference(getPotentialAppPreference(app)) }
    }

    private fun setupOneAppConnected(
        connectedApp: ConnectedFitnessAppMetadata,
        potentialApps: List<ConnectedFitnessAppMetadata>,
    ) {
        this.setPageName(PageName.CONNECT_ONE_APP_ONBOARDING_PAGE)

        header.title = getString(R.string.connect_second_app_title)
        header.icon = AttributeResolver.getDrawable(requireContext(), R.attr.syncIcon)
        topIntro.title =
            getString(R.string.connect_second_app_description, connectedApp.appMetadata.appName)

        showContents()
        updateSetupLaterButton(
            ConnectSecondAddOnboardingPageElement.CONNECT_SECOND_APP_ONBOARDING_SET_UP_LATER_BUTTON
        )

        showOneAppConnected(connectedApp, potentialApps)
    }

    private fun showOneAppConnected(
        connectedApp: ConnectedFitnessAppMetadata,
        potentialApps: List<ConnectedFitnessAppMetadata>,
    ) {
        appsCategory.removeAll()
        appsCategory.isVisible = true
        appsCategory.addPreference(
            getConnectedAppPreference(connectedApp.appMetadata).also {
                it.isEnabled = false
                it.logName = ConnectSecondAddOnboardingPageElement.CONNECTED_APP_BUTTON
            }
        )
        potentialApps.forEach { appsCategory.addPreference(getPotentialAppPreference(it)) }
    }

    private fun setupAlmostDone(allowedApps: List<ConnectedFitnessAppMetadata>) {
        this.setPageName(PageName.ALMOST_DONE_PAGE)

        header.title = getString(R.string.almost_done_title)
        header.icon = AttributeResolver.getDrawable(requireContext(), R.attr.checkmarkIcon)
        topIntro.title = getString(R.string.almost_done_description)

        showContents()
        updateDoneButton()

        showAlmostDoneApps(allowedApps)
    }

    private fun showAlmostDoneApps(allowedApps: List<ConnectedFitnessAppMetadata>) {
        appsCategory.removeAll()
        appsCategory.isVisible = true
        allowedApps.forEach {
            appsCategory.addPreference(
                getConnectedAppPreference(it.appMetadata).also {
                    it.logName = AlmostDonePageElement.ONBOARDING_APP_BUTTON
                }
            )
        }
    }

    private fun updateSetupLaterButton(elementName: ElementName) {
        healthConnectLogger.logImpression(elementName)
        val setupLaterButton = getPrimaryButtonOutline()
        hideSecondaryButton()
        setupLaterButton.text = getString(R.string.set_up_later)
        setupLaterButton.setOnClickListener {
            healthConnectLogger.logInteraction(elementName)
            // TODO (b/399086212) set banner seen
            requireActivity().finish()
        }
    }

    private fun updateDoneButton() {
        healthConnectLogger.logImpression(AlmostDonePageElement.ONBOARDING_DONE_BUTTON)
        val doneButton = getPrimaryButtonFull()
        hideSecondaryButton()
        doneButton.text = getString(R.string.delete_dialog_done_button)
        doneButton.setOnClickListener {
            healthConnectLogger.logInteraction(AlmostDonePageElement.ONBOARDING_DONE_BUTTON)
            // TODO (b/399086212) set banner seen?
            requireActivity().finish()
        }
    }

    private fun getAppPreference(appMetadata: AppMetadata): HealthAppPreference {
        return HealthAppPreference(requireContext(), appMetadata)
    }

    private fun getConnectedAppPreference(appMetadata: AppMetadata): HealthAppPreference {
        return getAppPreference(appMetadata).also {
            it.widgetLayoutResource = R.layout.widget_onboarding_app_connected_preference
            it.setOnPreferenceClickListener {
                // Start the activities
                val launchIntent =
                    requireContext()
                        .packageManager
                        .getLaunchIntentForPackage(appMetadata.packageName)
                launchIntent?.let { requireContext().startActivity(it) }
                true
            }
        }
    }

    private fun getPotentialAppPreference(
        connectedApp: ConnectedFitnessAppMetadata
    ): HealthAppPreference {
        return getAppPreference(connectedApp.appMetadata).also {
            if (connectedApp.hasOnboarding) {
                it.logName = CommonOnboardingPageElement.APP_WITH_ONBOARDING_BUTTON
            } else {
                it.logName = CommonOnboardingPageElement.APP_WITHOUT_ONBOARDING_BUTTON
            }
            it.setOnPreferenceClickListener {
                val onboardingLaunched =
                    tryLaunchAppOnboardingActivity(
                        healthPermissionReader,
                        connectedApp.appMetadata.packageName,
                    )
                if (!onboardingLaunched) {
                    findNavController()
                        .navigate(
                            R.id
                                .action_connectAppsOnboardingFragment_to_fitnessAppOnboardingFragment,
                            bundleOf(
                                EXTRA_PACKAGE_NAME to connectedApp.appMetadata.packageName,
                                EXTRA_APP_NAME to connectedApp.appMetadata.appName,
                                SHOW_MANAGE_APP_SECTION to false,
                            ),
                        )
                } else {
                    // Mark app as having been interacted with
                    viewModel.setAppInteractedWith(connectedApp.appMetadata.packageName)
                }

                true
            }
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
