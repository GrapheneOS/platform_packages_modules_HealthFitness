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
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MedicalWritePermissionPageElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.healthfitness.flags.Flags.permissionRequestBottomSheet
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment for displaying a medical WRITE permission request. */
@AndroidEntryPoint(PermissionsFragment::class)
class MedicalWritePermissionFragment : Hilt_MedicalWritePermissionFragment() {

    companion object {
        private const val HEADER_PREFERENCE = "request_permissions_header"
        private const val SUPPORTED_PERMS_PREFERENCE = "supported_medical_permissions"
        private const val FOOTER_PREFERENCE = "request_medical_write_footer"
        private val sampleMedicalPermissionTypes =
            setOf(
                MedicalPermissionType.ALLERGIES_INTOLERANCES,
                MedicalPermissionType.CONDITIONS,
                MedicalPermissionType.LABORATORY_RESULTS,
                MedicalPermissionType.MEDICATIONS,
                MedicalPermissionType.PROCEDURES,
                MedicalPermissionType.VACCINES,
                MedicalPermissionType.VITAL_SIGNS,
            )
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    private val viewModel: RequestPermissionViewModel by activityViewModels()

    private val header: RequestPermissionHeaderPreference by pref(HEADER_PREFERENCE)

    private val supportedMedicalPreference: HealthPreference by pref(SUPPORTED_PERMS_PREFERENCE)

    private val footer: FooterPreference by pref(FOOTER_PREFERENCE)

    init {
        this.setPageName(PageName.REQUEST_WRITE_MEDICAL_PERMISSION_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.permissions_screen_medical_write, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.medicalScreenState.observe(viewLifecycleOwner) { screenState ->
            when (screenState) {
                is MedicalScreenState.ShowMedicalWrite -> {
                    setupScreen(screenState)
                }
                is MedicalScreenState.ShowMedicalRead,
                is MedicalScreenState.ShowMedicalReadWrite -> {
                    requireActivity()
                        .supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.permission_content, MedicalPermissionsFragment())
                        .commit()
                }
                else -> {
                    // We only need to handle this if the bottom sheet flag is disabled, as the
                    // bottom sheet permission fragment dialog already manages this state.
                    if (!permissionRequestBottomSheet()) {
                        removeFragment()
                    }
                }
            }
        }
    }

    private fun setupScreen(screenState: MedicalScreenState.ShowMedicalWrite) {
        header.bind(appName = screenState.appMetadata.appName, screenState = screenState)

        val sampleMedicalPermissions =
            sampleMedicalPermissionTypes
                .filterNot { it == MedicalPermissionType.ALL_MEDICAL_DATA }
                .map { getString(it.upperCaseLabel()) }
                .sorted()
                .joinToString("\n")
        supportedMedicalPreference.summary = sampleMedicalPermissions
        supportedMedicalPreference.isSelectable = false

        footer.title = getString(R.string.medical_request_footer)
        footer.setLearnMoreText(getString(R.string.medical_request_about_health_records))
        footer.setLearnMoreAction { deviceInfoUtils.openHCGetStartedLink(requireActivity()) }

        setupAllowButton()
        setupDoNotAllowButton()
    }

    private fun setupAllowButton() {
        logger.logImpression(MedicalWritePermissionPageElement.ALLOW_WRITE_HEALTH_RECORDS_BUTTON)
        getAllowButton().isEnabled = true

        getAllowButton().setOnClickListener {
            viewModel.setMedicalPermissionRequestConcluded(true)
            viewModel.updateMedicalPermissions(true)
            viewModel.requestMedicalPermissions(getPackageNameExtra())
            logger.logInteraction(
                MedicalWritePermissionPageElement.ALLOW_WRITE_HEALTH_RECORDS_BUTTON
            )
        }
    }

    private fun setupDoNotAllowButton() {
        logger.logImpression(MedicalWritePermissionPageElement.CANCEL_WRITE_HEALTH_RECORDS_BUTTON)

        getDontAllowButton().setOnClickListener {
            viewModel.setMedicalPermissionRequestConcluded(true)
            viewModel.updateMedicalPermissions(false)
            viewModel.requestMedicalPermissions(getPackageNameExtra())
            logger.logInteraction(
                MedicalWritePermissionPageElement.CANCEL_WRITE_HEALTH_RECORDS_BUTTON
            )
        }
    }
}
