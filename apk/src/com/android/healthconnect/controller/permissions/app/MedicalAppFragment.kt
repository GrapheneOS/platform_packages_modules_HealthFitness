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
package com.android.healthconnect.controller.permissions.app

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.shared.DisconnectHealthPermissionsDialogFragment
import com.android.healthconnect.controller.permissions.shared.DisconnectHealthPermissionsDialogFragment.Companion.DISCONNECT_ALL_EVENT
import com.android.healthconnect.controller.permissions.shared.DisconnectHealthPermissionsDialogFragment.Companion.DISCONNECT_CANCELED_EVENT
import com.android.healthconnect.controller.permissions.shared.DisconnectHealthPermissionsDialogFragment.Companion.KEY_DELETE_DATA
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.Constants.SHOW_MANAGE_APP_SECTION
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.shared.preference.addIntroOrAppHeaderPreference
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.logging.AppAccessElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment for a screen that shows the permissions for an app that has medical permissions but no
 * medical permissions.
 */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class MedicalAppFragment : Hilt_MedicalAppFragment() {

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val MANAGE_DATA_PREFERENCE_KEY = "manage_app"
        private const val FOOTER_KEY = "connected_app_footer"
        private const val KEY_ADDITIONAL_ACCESS = "additional_access"
        private const val PARAGRAPH_SEPARATOR = "\n\n"
    }

    init {
        setPageName(PageName.MEDICAL_APP_ACCESS_PAGE)
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private var packageName: String = ""
    private var appName: String = ""
    private var showManageAppSection: Boolean = true
    private val appPermissionViewModel: AppPermissionViewModel by activityViewModels()
    private val additionalAccessViewModel: AdditionalAccessViewModel by activityViewModels()
    private val permissionMap: MutableMap<MedicalPermission, HealthSwitchPreference> =
        mutableMapOf()

    private val allowAllPreference: HealthMainSwitchPreference by pref(ALLOW_ALL_PREFERENCE)
    private val readPermissionCategory: PreferenceGroup by pref(READ_CATEGORY)
    private val writePermissionCategory: PreferenceGroup by pref(WRITE_CATEGORY)
    private val manageDataCategory: PreferenceGroup by pref(MANAGE_DATA_PREFERENCE_KEY)
    private val connectedAppFooter: FooterPreference by pref(FOOTER_KEY)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.connected_app_screen, rootKey)

        allowAllPreference.logNameActive = AppAccessElement.ALLOW_ALL_PERMISSIONS_SWITCH_ACTIVE
        allowAllPreference.logNameInactive = AppAccessElement.ALLOW_ALL_PERMISSIONS_SWITCH_INACTIVE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (
            requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
                requireArguments().getString(EXTRA_PACKAGE_NAME) != null
        ) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
        if (
            requireArguments().containsKey(EXTRA_APP_NAME) &&
                requireArguments().getString(EXTRA_APP_NAME) != null
        ) {
            appName = requireArguments().getString(EXTRA_APP_NAME)!!
        }
        if (requireArguments().containsKey(SHOW_MANAGE_APP_SECTION)) {
            showManageAppSection = requireArguments().getBoolean(SHOW_MANAGE_APP_SECTION)
        }

        appPermissionViewModel.loadPermissionsForPackage(packageName)

        appPermissionViewModel.medicalPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        appPermissionViewModel.grantedMedicalPermissions.observe(viewLifecycleOwner) { granted ->
            permissionMap.forEach { (healthPermission, switchPreference) ->
                switchPreference.isChecked = healthPermission in granted
            }
        }
        appPermissionViewModel.lastReadPermissionDisconnected.observe(viewLifecycleOwner) { lastRead
            ->
            if (lastRead) {
                Toast.makeText(
                        requireContext(),
                        R.string.removed_additional_permissions_toast,
                        Toast.LENGTH_LONG,
                    )
                    .show()
                appPermissionViewModel.markLastReadShown()
            }
        }

        appPermissionViewModel.revokeAllHealthPermissionsState.observe(viewLifecycleOwner) { state
            ->
            when (state) {
                is RevokeAllState.Loading -> {
                    showLoadingDialog()
                }
                else -> {
                    dismissLoadingDialog()
                }
            }
        }

        childFragmentManager.setFragmentResultListener(DISCONNECT_CANCELED_EVENT, this) { _, _ ->
            allowAllPreference.isChecked = true
        }

        childFragmentManager.setFragmentResultListener(DISCONNECT_ALL_EVENT, this) { _, bundle ->
            val permissionsUpdated = revokeAllPermissions()
            if (!permissionsUpdated) {
                Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT).show()
            }
            if (bundle.containsKey(KEY_DELETE_DATA) && bundle.getBoolean(KEY_DELETE_DATA)) {
                appPermissionViewModel.deleteAppData(packageName, appName)
            }
        }

        setupHeader()
        setupAllowAllPreference()
        setupManageDataPreferenceCategory()
        setupFooter()
    }

    private fun revokeAllPermissions(): Boolean {
        return appPermissionViewModel.revokeAllMedicalAndMaybeAdditionalPermissions(packageName)
    }

    private fun setupHeader() {
        appPermissionViewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            addIntroOrAppHeaderPreference(preferenceScreen, requireContext(), appMetadata)
            // To prevent flickering, only show the other preferences once the header is added.
            // TODO(b/394567790): Add loading screen instead.
            showHiddenPreferences()
        }
    }

    private fun showHiddenPreferences() {
        allowAllPreference.isVisible = true
        readPermissionCategory.isVisible = true
        writePermissionCategory.isVisible = true
        connectedAppFooter.isVisible = true
    }

    private fun setupManageDataPreferenceCategory() {
        if (!showManageAppSection) {
            manageDataCategory.isVisible = false
            return
        }

        manageDataCategory.isVisible = true
        manageDataCategory.removeAll()

        additionalAccessViewModel.loadAdditionalAccessPreferences(packageName)
        additionalAccessViewModel.additionalAccessState.observe(viewLifecycleOwner) { state ->
            if (state.isAvailable() && shouldAddAdditionalAccessPref()) {
                val additionalAccessPref =
                    HealthPreference(requireContext()).also {
                        it.key = KEY_ADDITIONAL_ACCESS
                        it.logName = AppAccessElement.ADDITIONAL_ACCESS_BUTTON
                        it.setTitle(R.string.additional_access_label)
                        it.setOnPreferenceClickListener { _ ->
                            val extras = bundleOf(EXTRA_PACKAGE_NAME to packageName)
                            findNavController()
                                .navigate(
                                    R.id.action_medicalAppFragment_to_additionalAccessFragment,
                                    extras,
                                )
                            true
                        }
                    }
                manageDataCategory.addPreference(additionalAccessPref)
            }
            manageDataCategory.children.find { it.key == KEY_ADDITIONAL_ACCESS }?.isVisible =
                state.isAvailable()
        }

        manageDataCategory.addPreference(
            HealthPreference(requireContext()).also {
                it.title = getString(R.string.see_app_data)
                it.setOnPreferenceClickListener {
                    findNavController()
                        .navigate(
                            R.id.action_medicalApp_to_appData,
                            bundleOf(EXTRA_PACKAGE_NAME to packageName, EXTRA_APP_NAME to appName),
                        )
                    true
                }
            }
        )
    }

    private fun shouldAddAdditionalAccessPref(): Boolean {
        return manageDataCategory.children.none { it.key == KEY_ADDITIONAL_ACCESS }
    }

    private fun setupAllowAllPreference() {

        val onChecked = suspend {
            val permissionsUpdated = appPermissionViewModel.grantAllMedicalPermissions(packageName)
            if (!permissionsUpdated) {
                Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT).show()
                false
            }
            true
        }
        val onUnchecked = suspend {
            showRevokeAllPermissions()
            true
        }

        allowAllPreference.setUpStateManagement(
            viewLifecycleOwner,
            appPermissionViewModel.allMedicalPermissionsGranted,
            onChecked,
            onUnchecked,
        )
    }

    private fun showRevokeAllPermissions() {
        DisconnectHealthPermissionsDialogFragment(
                appName,
                enableDeleteData = true,
                DisconnectHealthPermissionsDialogFragment.DisconnectType.MEDICAL,
            )
            .show(childFragmentManager, DisconnectHealthPermissionsDialogFragment.TAG)
    }

    private fun updatePermissions(permissions: List<MedicalPermission>) {
        readPermissionCategory.removeAll()
        writePermissionCategory.removeAll()
        permissionMap.clear()

        permissions
            .sortByLocale {
                requireContext()
                    .getString(fromPermissionType(it.medicalPermissionType).uppercaseLabel)
            }
            .forEach { permission ->
                val category =
                    if (
                        permission.medicalPermissionType == MedicalPermissionType.ALL_MEDICAL_DATA
                    ) {
                        writePermissionCategory
                    } else {
                        readPermissionCategory
                    }

                val preference =
                    HealthSwitchPreference(requireContext()).also { it ->
                        it.icon = permission.medicalPermissionType.icon(requireContext())
                        it.setTitle(
                            fromPermissionType(permission.medicalPermissionType).uppercaseLabel
                        )
                        it.logNameActive = AppAccessElement.PERMISSION_SWITCH_ACTIVE
                        it.logNameInactive = AppAccessElement.PERMISSION_SWITCH_INACTIVE
                        it.permission = permission
                        it.setOnPreferenceChangeListener { _, newValue ->
                            val checked = newValue as Boolean
                            val permissionUpdated =
                                appPermissionViewModel.updatePermission(
                                    packageName,
                                    permission,
                                    checked,
                                )
                            if (!permissionUpdated) {
                                Toast.makeText(
                                        requireContext(),
                                        R.string.default_error,
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                            permissionUpdated
                        }
                    }
                permissionMap[permission] = preference
                category.addPreference(preference)
            }

        readPermissionCategory.apply { isVisible = (preferenceCount != 0) }
        writePermissionCategory.apply { isVisible = (preferenceCount != 0) }
    }

    private fun setupFooter() {
        val title =
            getString(R.string.other_android_permissions) +
                PARAGRAPH_SEPARATOR +
                getString(R.string.manage_permissions_rationale, appName)
        val contentDescription =
            getString(R.string.other_android_permissions_content_description) +
                PARAGRAPH_SEPARATOR +
                getString(R.string.manage_permissions_rationale, appName)
        connectedAppFooter.title = title
        connectedAppFooter.setContentDescription(contentDescription)
        if (healthPermissionReader.isRationaleIntentDeclared(packageName)) {
            connectedAppFooter.setLearnMoreText(getString(R.string.manage_permissions_learn_more))
            logger.logImpression(AppAccessElement.PRIVACY_POLICY_LINK)
            connectedAppFooter.setLearnMoreAction {
                logger.logInteraction(AppAccessElement.PRIVACY_POLICY_LINK)
                val startRationaleIntent =
                    healthPermissionReader.getApplicationRationaleIntent(packageName)
                startActivity(startRationaleIntent)
            }
        }
    }
}
