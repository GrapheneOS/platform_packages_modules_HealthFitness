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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationActivity.Companion.maybeShowMigrationDialog
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.WithData
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.shared.DisconnectHealthPermissionsDialogFragment
import com.android.healthconnect.controller.permissions.shared.DisconnectHealthPermissionsDialogFragment.Companion.DISCONNECT_ALL_EVENT
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.Constants.SHOW_MANAGE_APP_SECTION
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.addIntroOrAppHeaderPreference
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.logging.AppAccessElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.navigateSafe
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment to show granted/revoked health permissions for and apps that declare both
 * [FitnessPermission]s and[MedicalPermission]s. It is used as an entry point from
 * PermissionController.
 *
 * For apps that declares health connect permissions without the rational intent, we only show
 * granted permissions to allow the user to revoke this app permissions.
 */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class SettingsCombinedPermissionsFragment : Hilt_SettingsCombinedPermissionsFragment() {

    init {
        setPageName(PageName.SETTINGS_MANAGE_COMBINED_APP_PERMISSIONS_PAGE)
    }

    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private lateinit var packageName: String
    private var appName: String = ""

    private val viewModel: AppPermissionViewModel by activityViewModels()
    private val additionalAccessViewModel: AdditionalAccessViewModel by viewModels()
    private val migrationViewModel: MigrationViewModel by viewModels()
    private val managePermissionsCategory: PreferenceGroup by pref(MANAGE_PERMISSIONS_CATEGORY)
    private val manageAppCategory: PreferenceGroup by pref(MANAGE_APP_CATEGORY)
    private val footer: FooterPreference by pref(FOOTER)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.settings_combined_permissions_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (
            requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
                requireArguments().getString(EXTRA_PACKAGE_NAME) != null
        ) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
        viewModel.loadPermissionsForPackage(packageName)

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

        viewModel.atLeastOneHealthPermissionGranted.observe(viewLifecycleOwner) { granted ->
            setupManageAppPreferenceCategory(granted)
        }

        childFragmentManager.setFragmentResultListener(DISCONNECT_ALL_EVENT, this) { _, bundle ->
            val permissionsUpdated = revokeAllPermissions()
            val toastString =
                if (!permissionsUpdated) {
                    R.string.default_error
                } else {
                    R.string.disconnect_all_health_permissions_success_toast
                }
            Toast.makeText(requireContext(), toastString, Toast.LENGTH_SHORT).show()
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
        setupHeader()
        setupManagePermissionsPreferenceCategory()
    }

    private fun setupHeader() {
        viewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            addIntroOrAppHeaderPreference(preferenceScreen, requireContext(), appMetadata)
            setupFooter(appMetadata.appName)
            // To prevent flickering, only show the other preferences once the header is added.
            // TODO(b/394567790): Add loading screen instead.
            showHiddenPreferences()
        }
    }

    private fun showHiddenPreferences() {
        managePermissionsCategory.isVisible = true
        manageAppCategory.isVisible = true
    }

    private fun setupManagePermissionsPreferenceCategory() {
        managePermissionsCategory.removeAll()

        managePermissionsCategory.addPreference(
            HealthPreference(requireContext()).also {
                it.title = getString(R.string.fitness_permissions)
                it.summary = getString(R.string.fitness_permissions_summary)
                it.setOnPreferenceClickListener {
                    findNavController()
                        .navigateSafe(
                            R.id.settingsCombinedPermissionsFragment,
                            R.id.action_settingsCombinedPermissions_to_FitnessAppFragment,
                            Bundle().apply {
                                putString(EXTRA_PACKAGE_NAME, packageName)
                                putString(Constants.EXTRA_APP_NAME, appName)
                                putBoolean(SHOW_MANAGE_APP_SECTION, false)
                            },
                        )
                    true
                }
            }
        )

        managePermissionsCategory.addPreference(
            HealthPreference(requireContext()).also {
                it.title = getString(R.string.medical_permissions)
                it.summary = getString(R.string.medical_permissions_summary)
                it.setOnPreferenceClickListener {
                    findNavController()
                        .navigateSafe(
                            R.id.settingsCombinedPermissionsFragment,
                            R.id.action_settingsCombinedPermissions_to_MedicalAppFragment,
                            Bundle().apply {
                                putString(EXTRA_PACKAGE_NAME, packageName)
                                putString(Constants.EXTRA_APP_NAME, appName)
                                putBoolean(SHOW_MANAGE_APP_SECTION, false)
                            },
                        )
                    true
                }
            }
        )

        additionalAccessViewModel.loadAdditionalAccessPreferences(packageName)
        additionalAccessViewModel.additionalAccessState.observe(viewLifecycleOwner) { state ->
            if (state.isAvailable() && shouldAddAdditionalAccessPref()) {
                val additionalAccessPref =
                    HealthPreference(requireContext()).also {
                        it.key = KEY_ADDITIONAL_ACCESS
                        it.logName = AppAccessElement.ADDITIONAL_ACCESS_BUTTON
                        it.title = getString(R.string.additional_access_label)
                        it.summary = getString(R.string.additional_access_summary)
                        it.setOnPreferenceClickListener { _ ->
                            val extras =
                                Bundle().apply { putString(EXTRA_PACKAGE_NAME, packageName) }
                            findNavController()
                                .navigateSafe(
                                    R.id.settingsCombinedPermissionsFragment,
                                    R.id
                                        .action_settingsCombinedPermissions_to_additionalAccessFragment,
                                    extras,
                                )
                            true
                        }
                    }
                managePermissionsCategory.addPreference(additionalAccessPref)
            }
            managePermissionsCategory.children.find { it.key == KEY_ADDITIONAL_ACCESS }?.isVisible =
                state.isAvailable()
        }
    }

    private fun shouldAddAdditionalAccessPref(): Boolean {
        return managePermissionsCategory.children.none { it.key == KEY_ADDITIONAL_ACCESS }
    }

    private fun setupFooter(appName: String) {
        if (viewModel.isPackageSupported(packageName)) {
            updateFooter(appName)
        } else {
            preferenceScreen.removePreferenceRecursively(FOOTER)
        }
    }

    private fun updateFooter(appName: String) {
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

    private fun showRevokeAllPermissions() {
        DisconnectHealthPermissionsDialogFragment(
                viewModel.appInfo.value?.appName!!,
                enableDeleteData = false,
                disconnectType = DisconnectHealthPermissionsDialogFragment.DisconnectType.ALL,
            )
            .show(childFragmentManager, DisconnectHealthPermissionsDialogFragment.TAG)
    }

    private fun revokeAllPermissions(): Boolean {
        return viewModel.revokeAllHealthPermissions(packageName)
    }

    private fun setupManageAppPreferenceCategory(isEnabled: Boolean) {
        manageAppCategory.removeAll()
        manageAppCategory.addPreference(
            HealthPreference(requireContext()).also {
                it.title = getString(R.string.remove_access_for_this_app)
                it.setOnPreferenceClickListener {
                    showRevokeAllPermissions()
                    true
                }
                it.isEnabled = isEnabled
            }
        )
    }

    companion object {
        private const val MANAGE_PERMISSIONS_CATEGORY = "manage_permissions"
        private const val MANAGE_APP_CATEGORY = "manage_app"
        private const val FOOTER = "manage_app_permission_footer"
        private const val KEY_ADDITIONAL_ACCESS = "key_additional_access"
    }
}
