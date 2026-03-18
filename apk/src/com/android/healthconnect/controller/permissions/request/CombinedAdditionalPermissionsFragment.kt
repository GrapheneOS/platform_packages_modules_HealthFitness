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
package com.android.healthconnect.controller.permissions.request

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceCategory
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.AccessType
import com.android.healthconnect.controller.permissions.data.AdditionalPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.additionalPermissionString
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RequestCombinedAdditionalPermissionsElement
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject

/** Fragment that shows a combined additional permission request screen. */
@AndroidEntryPoint(PermissionsFragment::class)
class CombinedAdditionalPermissionsFragment : Hilt_CombinedAdditionalPermissionsFragment() {

    companion object {
        private const val HEADER = "request_permissions_header"
        private const val CATEGORY = "additional_permissions_category"
        private const val FOOTER = "request_permissions_footer"
    }

    private val viewModel: RequestPermissionViewModel by activityViewModels()

    private val header: RequestPermissionHeaderPreference by pref(HEADER)
    private val category: PreferenceCategory by pref(CATEGORY)
    private val footer: FooterPreference by pref(FOOTER)

    private var allowButtonName: ElementName =
        RequestCombinedAdditionalPermissionsElement.ALLOW_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON
    private var cancelButtonName: ElementName =
        RequestCombinedAdditionalPermissionsElement.CANCEL_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON

    private val dateFormatter by lazy { LocalDateTimeFormatter(requireContext()) }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    init {
        this.setPageName(PageName.REQUEST_COMBINED_ADDITIONAL_PERMISSIONS_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.additional_permissions_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.additionalScreenState.observe(viewLifecycleOwner) { screenState ->
            when (screenState) {
                is AdditionalScreenState.NoAdditionalData -> {
                    // Handled by PermissionsActivity
                }
                is AdditionalScreenState.ShowCombined -> {
                    setupScreen(screenState)
                }
                else -> {
                    // Handled by PermissionsActivity
                }
            }
        }
    }

    private fun setupScreen(screenState: AdditionalScreenState.ShowCombined) {
        header.bind(appName = screenState.appMetadata.appName, screenState = screenState)

        setupAllowButton()
        setupDontAllowButton()
        showCombinedAdditionalPermissions(screenState)
        maybeShowFooter(screenState.isMedicalReadGranted, screenState.appMetadata.appName)
    }

    private fun maybeShowFooter(shouldShow: Boolean, appName: String) {
        if (!shouldShow) {
            footer.isVisible = false
            return
        }

        footer.title = getString(R.string.history_read_medical_combined_request_footer, appName)
        footer.setLearnMoreText(
            getString(R.string.history_read_medical_combined_request_footer_link)
        )
        footer.setLearnMoreAction { deviceInfoUtils.openHCLearnMoreLink(requireActivity()) }
        footer.isVisible = true
    }

    private fun setupAllowButton() {
        logger.logImpression(allowButtonName)

        viewModel.grantedAdditionalPermissions.observe(viewLifecycleOwner) { grantedPermissions ->
            getAllowButton().isEnabled = grantedPermissions.isNotEmpty()
        }

        getAllowButton().setOnClickListener {
            logger.logInteraction(allowButtonName)
            viewModel.requestAdditionalPermissions(getPackageNameExtra())
            handlePermissionResults(viewModel.getPermissionGrants())
        }
    }

    private fun setupDontAllowButton() {
        logger.logImpression(cancelButtonName)

        getDontAllowButton().setOnClickListener {
            logger.logInteraction(cancelButtonName)
            viewModel.updateAdditionalPermissions(false)
            viewModel.requestAdditionalPermissions(this.getPackageNameExtra())
            handlePermissionResults(viewModel.getPermissionGrants())
        }
    }

    private fun showCombinedAdditionalPermissions(screenState: AdditionalScreenState.ShowCombined) {
        this.setPageName(PageName.REQUEST_COMBINED_ADDITIONAL_PERMISSIONS_PAGE)
        allowButtonName =
            RequestCombinedAdditionalPermissionsElement.ALLOW_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON
        cancelButtonName =
            RequestCombinedAdditionalPermissionsElement
                .CANCEL_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON

        category.removeAll()
        category.isVisible = true

        category.addPreference(getHistoryReadPreference(screenState))
        category.addPreference(getBackgroundReadPreference(screenState))
    }

    private fun getBackgroundReadPreference(
        screenState: AdditionalScreenState.ShowCombined
    ): HealthSwitchPreference {
        val additionalPermission = AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND
        val additionalPermissionStrings =
            additionalPermissionString(
                additionalPermission,
                type = AccessType.COMBINED_REQUEST,
                hasMedicalPermissions = screenState.hasMedical,
                isMedicalReadGranted = screenState.isMedicalReadGranted,
                isFitnessReadGranted = screenState.isFitnessReadGranted,
            )
        val value = viewModel.isPermissionLocallyGranted(additionalPermission)

        return HealthSwitchPreference(requireContext()).also { switchPreference ->
            switchPreference.setDefaultValue(value)
            switchPreference.title = getString(additionalPermissionStrings.title)
            switchPreference.summary = getString(additionalPermissionStrings.description)
            switchPreference.logNameActive =
                RequestCombinedAdditionalPermissionsElement.BACKGROUND_READ_BUTTON
            switchPreference.logNameInactive =
                RequestCombinedAdditionalPermissionsElement.BACKGROUND_READ_BUTTON

            switchPreference.setOnPreferenceChangeListener { _, newValue ->
                viewModel.updateHealthPermission(additionalPermission, newValue as Boolean)
                true
            }
        }
    }

    private fun getHistoryReadPreference(
        screenState: AdditionalScreenState.ShowCombined
    ): HealthSwitchPreference {
        val additionalPermission = AdditionalPermission.READ_HEALTH_DATA_HISTORY
        val additionalPermissionStrings =
            additionalPermissionString(
                additionalPermission,
                type = AccessType.COMBINED_REQUEST,
                hasMedicalPermissions = screenState.hasMedical,
                isMedicalReadGranted = screenState.isMedicalReadGranted,
                isFitnessReadGranted = screenState.isFitnessReadGranted,
            )
        val value = viewModel.isPermissionLocallyGranted(additionalPermission)
        val summary =
            getHistoryReadPermissionPreferenceSummary(
                additionalPermissionStrings,
                screenState.dataAccessDate,
            )

        return HealthSwitchPreference(requireContext()).also { switchPreference ->
            switchPreference.setDefaultValue(value)
            switchPreference.title = getString(additionalPermissionStrings.title)
            switchPreference.summary = summary
            switchPreference.logNameActive =
                RequestCombinedAdditionalPermissionsElement.HISTORY_READ_BUTTON
            switchPreference.logNameInactive =
                RequestCombinedAdditionalPermissionsElement.HISTORY_READ_BUTTON

            switchPreference.setOnPreferenceChangeListener { _, newValue ->
                viewModel.updateHealthPermission(additionalPermission, newValue as Boolean)
                true
            }
        }
    }

    private fun getHistoryReadPermissionPreferenceSummary(
        additionalPermissionStrings: AdditionalPermissionStrings,
        dataAccessDate: Instant?,
    ): String {
        return if (dataAccessDate != null) {
            val formattedDate = dateFormatter.formatLongDate(dataAccessDate)
            getString(additionalPermissionStrings.description, formattedDate)
        } else {
            getString(additionalPermissionStrings.descriptionFallback)
        }
    }
}
