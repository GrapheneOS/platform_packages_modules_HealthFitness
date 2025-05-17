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

package com.android.healthconnect.controller.onboarding

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromFitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.IntroPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Shows all the [FitnessPermission]s of an app. The user can toggle on/off any or all permissions.
 * The permissions are only changed in the [PackageManager] if the user clicks on "Done".
 */
@AndroidEntryPoint(HealthSetupFragment::class)
class FitnessAppOnboardingFragment : Hilt_FitnessAppOnboardingFragment() {

    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val APP_HEADER_PREFERENCE = "app_header_preference"
        private const val DETAILS_PREFERENCE = "details_preference"
    }

    private var packageName: String = ""
    private var appName: String = ""

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()
    private val viewModel: FitnessAppOnboardingViewModel by viewModels()
    private val detailsPreference: PermissionDetailsPreference by pref(DETAILS_PREFERENCE)
    private val allowAllPreference: HealthMainSwitchPreference by pref(ALLOW_ALL_PREFERENCE)
    private val readPermissionCategory: PreferenceGroup by pref(READ_CATEGORY)
    private val writePermissionCategory: PreferenceGroup by pref(WRITE_CATEGORY)
    private val appHeaderPreference: IntroPreference by pref(APP_HEADER_PREFERENCE)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fitness_app_onboarding_screen, rootKey)
        allowAllPreference.isChecked = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val doneButton = primaryButtonFull
        doneButton.text = getString(R.string.delete_dialog_done_button)
        doneButton.setOnClickListener {
            findNavController().popBackStack()
            viewModel.done()
            // The user interacted with this app, so regardless of permissions
            // status we should mark it as connected
            onboardingViewModel.setAppInteractedWith(packageName)
        }

        val backButton = secondaryButton
        backButton.text = getString(R.string.back_button)
        backButton.setOnClickListener { findNavController().popBackStack() }

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

        viewModel.init(packageName)

        viewModel.fitnessAppOnboardingFragmentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.NoFitnessData -> {
                    // TODO handle back stack in tests
                    // findNavController().popBackStack()
                }
                is FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessRead -> {
                    setupHeader(state.appMetadata, state)
                    updatePermissions(state.permissionsMap)
                }
                is FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessWrite -> {
                    setupHeader(state.appMetadata, state)
                    updatePermissions(state.permissionsMap)
                }
                is FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessReadWrite -> {
                    setupHeader(state.appMetadata, state)
                    updatePermissions(state.permissionsMap)
                }
            }
        }

        setupAllowAllPreference()
    }

    private fun setupHeader(
        appMetadata: AppMetadata,
        screenState: FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState,
    ) {
        appHeaderPreference.icon = appMetadata.icon
        appHeaderPreference.title = appMetadata.appName

        val onRationaleLinkClicked = {
            val startRationaleIntent =
                healthPermissionReader.getApplicationRationaleIntent(appMetadata.packageName)
            // TODO (b/417206188) telemetry
            // logger.logInteraction(PermissionsElement.APP_RATIONALE_LINK)
            startActivity(startRationaleIntent)
        }
        detailsPreference.bind(
            appMetadata = appMetadata,
            screenState = screenState,
            onRationaleLinkClicked = onRationaleLinkClicked,
            onLearnMoreClicked = {
                deviceInfoUtils.openHealthFitnessPermissionsLearnMoreLink(requireActivity())
            },
        )
    }

    private val onSwitchChangeListener = OnCheckedChangeListener { buttonView, isChecked ->
        viewModel.updateAllPermissions(isChecked)
    }

    private fun setupAllowAllPreference() {
        allowAllPreference.isVisible = true
        allowAllPreference.addOnSwitchChangeListener(onSwitchChangeListener)
        viewModel.allFitnessPermissionsGranted.observe(viewLifecycleOwner) { allGranted ->
            allowAllPreference.removeOnSwitchChangeListener(onSwitchChangeListener)
            allowAllPreference.isChecked = allGranted
            allowAllPreference.addOnSwitchChangeListener(onSwitchChangeListener)
        }
    }

    private fun updatePermissions(permsMap: Map<FitnessPermission, Boolean>) {
        readPermissionCategory.removeAll()
        writePermissionCategory.removeAll()

        permsMap.entries
            .sortByLocale {
                requireContext()
                    .getString(fromPermissionType(it.key.fitnessPermissionType).uppercaseLabel)
            }
            .forEach { (permission, isGranted) ->
                val category =
                    if (permission.permissionsAccessType == PermissionsAccessType.READ) {
                        readPermissionCategory
                    } else {
                        writePermissionCategory
                    }

                val preference =
                    HealthSwitchPreference(requireContext()).also { it ->
                        it.isChecked = isGranted
                        val healthCategory =
                            fromFitnessPermissionType(permission.fitnessPermissionType)
                        it.icon = healthCategory.icon(requireContext())
                        it.setTitle(
                            fromPermissionType(permission.fitnessPermissionType).uppercaseLabel
                        )
                        // TODO (b/417206188) telemetry
                        //                    it.logNameActive =
                        // AppAccessElement.PERMISSION_SWITCH_ACTIVE
                        //                    it.logNameInactive =
                        // AppAccessElement.PERMISSION_SWITCH_INACTIVE
                        it.setOnPreferenceChangeListener { _, newValue ->
                            val checked = newValue as Boolean
                            viewModel.updatePermission(permission, checked)
                            true
                        }
                    }
                category.addPreference(preference)
            }
        readPermissionCategory.apply { isVisible = (preferenceCount != 0) }
        writePermissionCategory.apply { isVisible = (preferenceCount != 0) }
    }
}
