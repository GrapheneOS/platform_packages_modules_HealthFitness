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
 *
 *
 */
package com.android.healthconnect.controller.permissions.request

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RequestCombinedAdditionalPermissionsElement
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(PermissionsFragment::class)
class SingleAdditionalPermissionFragment : Hilt_SingleAdditionalPermissionFragment() {

    companion object {
        private const val HEADER = "request_permissions_header"
        private const val FOOTER = "request_permissions_footer"
    }

    private val viewModel: RequestPermissionViewModel by activityViewModels()

    private val header: RequestPermissionHeaderPreference by pref(HEADER)
    private val footer: FooterPreference by pref(FOOTER)
    @Inject lateinit var logger: HealthConnectLogger

    private var allowButtonName: ElementName =
        RequestCombinedAdditionalPermissionsElement.ALLOW_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON
    private var dontAllowButtonName: ElementName =
        RequestCombinedAdditionalPermissionsElement.CANCEL_COMBINED_ADDITIONAL_PERMISSIONS_BUTTON
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.additional_permissions_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.additionalScreenState.observe(viewLifecycleOwner) { screenState ->
            when (screenState) {
                is AdditionalScreenState.NoAdditionalData -> {
                    requireActivity()
                        .supportFragmentManager
                        .beginTransaction()
                        .remove(this)
                        .commit()
                }
                is AdditionalScreenState.ShowCombined -> {
                    requireActivity()
                        .supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.permission_content, CombinedAdditionalPermissionsFragment())
                        .commit()
                }
                is AdditionalScreenState.ShowHistory -> {
                    setupHistoryScreen(screenState)
                    this.setPageName(PageName.REQUEST_HISTORY_READ_PERMISSION_PAGE)
                }
                is AdditionalScreenState.ShowBackground -> {
                    setupBackgroundScreen(screenState)
                    this.setPageName(PageName.REQUEST_BACKGROUND_READ_PERMISSION_PAGE)
                }
            }
        }
    }

    private fun setupHistoryScreen(screenState: AdditionalScreenState.ShowHistory) {
        header.bind(screenState.appMetadata.appName, screenState)

        setupAllowButton()
        setupDontAllowButton()
        maybeShowFooter(screenState.isMedicalReadGranted, screenState.appMetadata.appName)
    }

    private fun maybeShowFooter(isMedicalReadGranted: Boolean, appName: String) {
        if (!isMedicalReadGranted) {
            footer.isVisible = false
            return
        }

        footer.isVisible = true
        footer.title = getString(R.string.history_read_medical_combined_request_footer, appName)
        footer.setLearnMoreText(
            getString(R.string.history_read_medical_combined_request_footer_link)
        )
        footer.setLearnMoreAction { deviceInfoUtils.openHCLearnMoreLink(requireActivity()) }
    }

    private fun setupBackgroundScreen(screenState: AdditionalScreenState.ShowBackground) {
        header.bind(screenState.appMetadata.appName, screenState)

        setupAllowButton()
        setupDontAllowButton()
    }

    private fun setupAllowButton() {
        logger.logImpression(allowButtonName)
        getAllowButton().isEnabled = true

        getAllowButton().setOnClickListener {
            logger.logInteraction(allowButtonName)
            viewModel.updateAdditionalPermissions(true)
            viewModel.requestAdditionalPermissions(getPackageNameExtra())
            handlePermissionResults(viewModel.getPermissionGrants())
        }
    }

    private fun setupDontAllowButton() {
        logger.logImpression(dontAllowButtonName)

        getDontAllowButton().setOnClickListener {
            logger.logInteraction(dontAllowButtonName)
            viewModel.updateAdditionalPermissions(false)
            viewModel.requestAdditionalPermissions(this.getPackageNameExtra())
            handlePermissionResults(viewModel.getPermissionGrants())
        }
    }
}
