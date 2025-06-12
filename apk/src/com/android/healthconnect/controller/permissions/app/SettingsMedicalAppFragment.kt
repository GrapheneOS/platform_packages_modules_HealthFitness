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
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceGroup
import androidx.preference.TwoStatePreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.maybeShowMigrationDialog
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.*
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.shared.DisconnectHealthPermissionsDialogFragment
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.shared.preference.addIntroOrAppHeaderPreference
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import com.android.healthconnect.controller.utils.NavigationUtils
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.logging.AppAccessElement.ADDITIONAL_ACCESS_BUTTON
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment to show granted/revoked [MedicalPermission]s for and app. It is used as an entry point
 * from PermissionController or from [SettingsCombinedPermissionsFragment].
 *
 * For apps that declares health connect permissions without the rational intent, we only show
 * granted permissions to allow the user to revoke this app permissions.
 */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class SettingsMedicalAppFragment : Hilt_SettingsMedicalAppFragment() {

    init {
        setPageName(PageName.SETTINGS_MANAGE_MEDICAL_APP_PERMISSIONS_PAGE)
    }

    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var navigationUtils: NavigationUtils

    private lateinit var packageName: String
    private var appName: String = ""
    private var showManageAppSection = true

    private val viewModel: AppPermissionViewModel by activityViewModels()
    private val permissionMap: MutableMap<MedicalPermission, TwoStatePreference> = mutableMapOf()
    private val additionalAccessViewModel: AdditionalAccessViewModel by viewModels()
    private val migrationViewModel: MigrationViewModel by viewModels()
    private val allowAllPreference: HealthMainSwitchPreference by pref(ALLOW_ALL_PREFERENCE)
    private val readPermissionCategory: PreferenceGroup by pref(READ_CATEGORY)
    private val writePermissionCategory: PreferenceGroup by pref(WRITE_CATEGORY)
    private val manageAppCategory: PreferenceGroup by pref(MANAGE_APP_CATEGORY)
    private val footer: FooterPreference by pref(FOOTER)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.settings_manage_app_permission_screen, rootKey)

        allowAllPreference.apply {
            logNameActive = PermissionsElement.ALLOW_ALL_SWITCH
            logNameInactive = PermissionsElement.ALLOW_ALL_SWITCH
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (
            requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
                requireArguments().getString(EXTRA_PACKAGE_NAME) != null
        ) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
        if (requireArguments().containsKey(Constants.SHOW_MANAGE_APP_SECTION)) {
            showManageAppSection = requireArguments().getBoolean(Constants.SHOW_MANAGE_APP_SECTION)
        }

        viewModel.loadPermissionsForPackage(packageName)
        additionalAccessViewModel.loadAdditionalAccessPreferences(packageName)

        viewModel.medicalPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        viewModel.grantedMedicalPermissions.observe(viewLifecycleOwner) { granted ->
            permissionMap.forEach { (healthPermission, switchPreference) ->
                switchPreference.isChecked = healthPermission in granted
            }
        }
        viewModel.lastReadPermissionDisconnected.observe(viewLifecycleOwner) { lastRead ->
            if (lastRead) {
                Toast.makeText(
                        requireContext(),
                        R.string.removed_additional_permissions_toast,
                        Toast.LENGTH_LONG,
                    )
                    .show()
                viewModel.markLastReadShown()
            }
        }

        viewModel.revokeAllHealthPermissionsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RevokeAllState.Loading -> {
                    showLoadingDialog()
                }
                else -> {
                    dismissLoadingDialog()
                }
            }
        }

        migrationViewModel.migrationState.observe(viewLifecycleOwner) { migrationState ->
            when (migrationState) {
                is WithData -> {
                    maybeShowMigrationDialog(
                        migrationState.migrationRestoreState,
                        requireActivity(),
                        viewModel.appInfo.value?.appName!!,
                    )
                }
                else -> {
                    // do nothing
                }
            }
        }

        childFragmentManager.setFragmentResultListener(
            DisconnectHealthPermissionsDialogFragment.DISCONNECT_CANCELED_EVENT,
            this,
        ) { _, _ ->
            allowAllPreference.isChecked = true
        }

        childFragmentManager.setFragmentResultListener(
            DisconnectHealthPermissionsDialogFragment.DISCONNECT_ALL_EVENT,
            this,
        ) { _, bundle ->
            if (!viewModel.revokeAllMedicalAndMaybeAdditionalPermissions(packageName)) {
                Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT).show()
            }

            if (
                bundle.containsKey(DisconnectHealthPermissionsDialogFragment.KEY_DELETE_DATA) &&
                    bundle.getBoolean(DisconnectHealthPermissionsDialogFragment.KEY_DELETE_DATA)
            ) {
                viewModel.deleteAppData(packageName, appName)
            }
        }

        setupHeader()
        setupManageAppCategory()
    }

    private fun setupHeader() {
        viewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            packageName = appMetadata.packageName
            appName = appMetadata.appName
            setupAllowAllPreference()
            setupFooter(appMetadata.appName)
            addIntroOrAppHeaderPreference(preferenceScreen, requireContext(), appMetadata)
            // To prevent flickering, only show the other preferences once the header is added.
            // TODO(b/394567790): Add loading screen instead.
            showHiddenPreferences()
        }
    }

    private fun showHiddenPreferences() {
        allowAllPreference.isVisible = true
    }

    private fun setupFooter(appName: String) {
        if (viewModel.isPackageSupported(packageName)) {
            viewModel.atLeastOneMedicalPermissionGranted.observe(viewLifecycleOwner) {
                isAtLeastOneGranted ->
                updateFooter(isAtLeastOneGranted, appName)
            }
        } else {
            preferenceScreen.removePreferenceRecursively(FOOTER)
        }
    }

    private fun setupManageAppCategory() {
        if (!showManageAppSection) {
            manageAppCategory.isVisible = false
            return
        }
        additionalAccessViewModel.additionalAccessState.observe(viewLifecycleOwner) { state ->
            manageAppCategory.isVisible = state.isAvailable()
            manageAppCategory.removeAll()
            if (state.isAvailable()) {
                val additionalAccessPref =
                    HealthPreference(requireContext()).also {
                        it.key = KEY_ADDITIONAL_ACCESS
                        it.logName = ADDITIONAL_ACCESS_BUTTON
                        it.setTitle(R.string.additional_access_label)
                        it.setOnPreferenceClickListener { _ ->
                            val extras = bundleOf(EXTRA_PACKAGE_NAME to packageName)
                            navigationUtils.navigate(
                                fragment = this,
                                action = R.id.action_settingsMedicalApp_to_additionalAccessFragment,
                                bundle = extras,
                            )
                            true
                        }
                    }
                manageAppCategory.addPreference(additionalAccessPref)
            }
        }
    }

    private fun setupAllowAllPreference() {

        val onChecked = suspend {
            val permissionsUpdated = viewModel.grantAllMedicalPermissions(packageName)
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
            viewModel.allMedicalPermissionsGranted,
            onChecked,
            onUnchecked,
        )
    }

    private fun showRevokeAllPermissions() {
        DisconnectHealthPermissionsDialogFragment(
                appName = appName,
                enableDeleteData = false,
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
                val switchPreference =
                    HealthSwitchPreference(requireContext()).also {
                        it.icon = permission.medicalPermissionType.icon(requireContext())
                        it.setTitle(
                            fromPermissionType(permission.medicalPermissionType).uppercaseLabel
                        )
                        it.logNameActive = PermissionsElement.PERMISSION_SWITCH
                        it.logNameInactive = PermissionsElement.PERMISSION_SWITCH
                        it.permission = permission
                        it.setOnPreferenceChangeListener { _, newValue ->
                            val checked = newValue as Boolean
                            val permissionUpdated =
                                viewModel.updatePermission(packageName, permission, checked)
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
                permissionMap[permission] = switchPreference
                category.addPreference(switchPreference)
            }

        // Hide category if it contains no permissions
        readPermissionCategory.apply { isVisible = (preferenceCount != 0) }
        writePermissionCategory.apply { isVisible = (preferenceCount != 0) }
    }

    private fun updateFooter(isAtLeastOneGranted: Boolean, appName: String) {
        footer.isVisible = true
        footer.title = getString(R.string.manage_permissions_rationale, appName)
        if (healthPermissionReader.isRationaleIntentDeclared(packageName)) {
            footer.setLearnMoreText(getString(R.string.manage_permissions_learn_more))
            footer.setLearnMoreAction {
                val startRationaleIntent =
                    healthPermissionReader.getApplicationRationaleIntent(packageName)
                startActivity(startRationaleIntent)
            }
        }
    }

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val MANAGE_APP_CATEGORY = "manage_app_category"
        private const val KEY_ADDITIONAL_ACCESS = "additional_access"
        private const val FOOTER = "manage_app_permission_footer"
    }
}
