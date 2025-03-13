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
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.TwoStatePreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.pref
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(PermissionsFragment::class)
class FitnessPermissionsFragment : Hilt_FitnessPermissionsFragment() {

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val HEADER = "request_permissions_header"
    }

    @Inject lateinit var logger: HealthConnectLogger

    private val viewModel: RequestPermissionViewModel by activityViewModels()
    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    private val header: RequestPermissionHeaderPreference by pref(HEADER)

    private val allowAllPreference: HealthMainSwitchPreference by pref(ALLOW_ALL_PREFERENCE)

    private val readPermissionCategory: PreferenceGroup by pref(READ_CATEGORY)

    private val writePermissionCategory: PreferenceGroup by pref(WRITE_CATEGORY)

    private val onSwitchChangeListener = OnCheckedChangeListener { _, grant ->
        readPermissionCategory.children.forEach { preference ->
            (preference as TwoStatePreference).isChecked = grant
        }
        writePermissionCategory.children.forEach { preference ->
            (preference as TwoStatePreference).isChecked = grant
        }
        viewModel.updateFitnessPermissions(grant)
    }

    init {
        this.setPageName(PageName.REQUEST_PERMISSIONS_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.permissions_screen, rootKey)
        allowAllPreference.logNameActive = PermissionsElement.ALLOW_ALL_SWITCH
        allowAllPreference.logNameInactive = PermissionsElement.ALLOW_ALL_SWITCH
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.fitnessScreenState.observe(viewLifecycleOwner) { screenState ->
            when (screenState) {
                is FitnessScreenState.NoFitnessData -> {
                    requireActivity()
                        .supportFragmentManager
                        .beginTransaction()
                        .remove(this)
                        .commit()
                }
                is FitnessScreenState.ShowFitnessWrite -> {
                    setupHeader(screenState.appMetadata, screenState)
                    updateDataList(screenState.fitnessPermissions)
                    updateCategoryTitles(screenState.appMetadata.appName)
                    setupButtons()
                }
                is FitnessScreenState.ShowFitnessReadWrite -> {
                    setupHeader(screenState.appMetadata, screenState)
                    updateDataList(screenState.fitnessPermissions)
                    updateCategoryTitles(screenState.appMetadata.appName)
                    setupButtons()
                }
                is FitnessScreenState.ShowFitnessRead -> {
                    setupHeader(screenState.appMetadata, screenState)
                    updateDataList(screenState.fitnessPermissions)
                    updateCategoryTitles(screenState.appMetadata.appName)
                    setupButtons()
                }
            }
        }
    }

    private fun setupHeader(appMetadata: AppMetadata, screenState: RequestPermissionsScreenState) {
        val onRationaleLinkClicked = {
            val startRationaleIntent =
                healthPermissionReader.getApplicationRationaleIntent(appMetadata.packageName)
            logger.logInteraction(PermissionsElement.APP_RATIONALE_LINK)
            startActivity(startRationaleIntent)
        }

        header.bind(
            appMetadata.appName,
            screenState,
            onRationaleLinkClicked = onRationaleLinkClicked,
            onLearnMoreClicked = {
                deviceInfoUtils.openHealthFitnessPermissionsLearnMoreLink(requireActivity())
            },
        )
    }

    private fun updateCategoryTitles(appName: String) {
        readPermissionCategory.title = getString(R.string.read_permission_category, appName)
        writePermissionCategory.title = getString(R.string.write_permission_category, appName)
    }

    private fun setupButtons() {
        setupAllowAll()

        setupAllowButton()
        setupDontAllowButton()
    }

    private fun setupAllowButton() {
        logger.logImpression(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)

        if (!viewModel.isFitnessPermissionRequestConcluded()) {
            viewModel.grantedFitnessPermissions.observe(viewLifecycleOwner) { grantedPermissions ->
                getAllowButton().isEnabled = grantedPermissions.isNotEmpty()
            }
        }

        getAllowButton().setOnClickListener {
            viewModel.setFitnessPermissionRequestConcluded(true)
            // When fitness permissions are concluded we need to
            // grant/revoke only the fitness permissions, to trigger the
            // access date. We can't request all at once because we might accidentally
            // set the additional permissions USER_FIXED
            viewModel.requestFitnessPermissions(getPackageNameExtra())
            logger.logInteraction(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
        }
    }

    private fun setupDontAllowButton() {
        logger.logImpression(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)

        getDontAllowButton().setOnClickListener {
            viewModel.setFitnessPermissionRequestConcluded(true)
            logger.logInteraction(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
            viewModel.updateFitnessPermissions(false)
            viewModel.requestFitnessPermissions(getPackageNameExtra())
        }
    }

    private fun setupAllowAll() {
        viewModel.allFitnessPermissionsGranted.observe(viewLifecycleOwner) { allPermissionsGranted
            ->
            // does not trigger removing/enabling all permissions
            allowAllPreference.removeOnSwitchChangeListener(onSwitchChangeListener)
            allowAllPreference.isChecked = allPermissionsGranted
            allowAllPreference.addOnSwitchChangeListener(onSwitchChangeListener)
        }
        allowAllPreference.addOnSwitchChangeListener(onSwitchChangeListener)
    }

    private fun updateDataList(permissionsList: List<HealthPermission.FitnessPermission>) {
        readPermissionCategory.removeAll()
        writePermissionCategory.removeAll()

        permissionsList
            .sortByLocale {
                requireContext()
                    .getString(
                        FitnessPermissionStrings.fromPermissionType(it.fitnessPermissionType)
                            .uppercaseLabel
                    )
            }
            .forEach { permission ->
                val value = viewModel.isPermissionLocallyGranted(permission)
                if (PermissionsAccessType.READ == permission.permissionsAccessType) {
                    readPermissionCategory.addPreference(getPermissionPreference(value, permission))
                } else if (PermissionsAccessType.WRITE == permission.permissionsAccessType) {
                    writePermissionCategory.addPreference(
                        getPermissionPreference(value, permission)
                    )
                }
            }

        readPermissionCategory.apply { isVisible = (preferenceCount != 0) }
        writePermissionCategory.apply { isVisible = (preferenceCount != 0) }
    }

    private fun getPermissionPreference(
        defaultValue: Boolean,
        permission: HealthPermission.FitnessPermission,
    ): Preference {
        return HealthSwitchPreference(requireContext()).also {
            val healthCategory =
                HealthDataCategoryExtensions.fromFitnessPermissionType(
                    permission.fitnessPermissionType
                )
            it.icon = healthCategory.icon(requireContext())
            it.setDefaultValue(defaultValue)
            it.setTitle(
                FitnessPermissionStrings.fromPermissionType(permission.fitnessPermissionType)
                    .uppercaseLabel
            )
            it.logNameActive = PermissionsElement.PERMISSION_SWITCH
            it.logNameInactive = PermissionsElement.PERMISSION_SWITCH
            it.setOnPreferenceChangeListener { _, newValue ->
                viewModel.updateHealthPermission(permission, newValue as Boolean)
                true
            }
        }
    }
}
