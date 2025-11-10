/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.data.access

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.PERMISSION_TYPE_NAME_KEY
import com.android.healthconnect.controller.permissions.connectedapps.HealthAppPreference
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.fromPermissionTypeName
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.inactiveapp.InactiveAppPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.NoDataPreference
import com.android.healthconnect.controller.utils.logging.DataAccessElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment displaying health data access information. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class AccessFragment : Hilt_AccessFragment() {

    companion object {
        private const val CAN_READ_SECTION = "can_read"
        private const val CAN_WRITE_SECTION = "can_write"
        private const val INACTIVE_SECTION = "inactive"
        private const val NO_DATA = "no_data_preference"
        private const val DELETION_TAG = "DeletionTag"
        private const val START_DELETION_ENTRIES_AND_ACCESS_KEY =
            "START_DELETION_ENTRIES_AND_ACCESS_KEY"
    }

    @Inject lateinit var logger: HealthConnectLogger

    private val viewModel: AccessViewModel by viewModels()
    private val deletionViewModel: DeletionViewModel by activityViewModels()

    private lateinit var permissionType: HealthPermissionType

    private val mCanReadSection: PreferenceGroup by pref(CAN_READ_SECTION)

    private val mCanWriteSection: PreferenceGroup by pref(CAN_WRITE_SECTION)

    private val mInactiveSection: PreferenceGroup by pref(INACTIVE_SECTION)

    private val noDataPreference: NoDataPreference by pref(NO_DATA)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.access_screen, rootKey)
        if (requireArguments().containsKey(PERMISSION_TYPE_NAME_KEY)) {
            val permissionTypeName =
                arguments?.getString(PERMISSION_TYPE_NAME_KEY)
                    ?: throw IllegalArgumentException("PERMISSION_TYPE_NAME_KEY can't be null!")
            permissionType = fromPermissionTypeName(permissionTypeName)
        }

        mCanReadSection.isVisible = false
        mCanWriteSection.isVisible = false
        mInactiveSection.isVisible = false
        mCanReadSection.title =
            getString(R.string.can_read, getString(permissionType.lowerCaseLabel()))
        mCanWriteSection.title =
            getString(R.string.can_write, getString(permissionType.lowerCaseLabel()))

        if (permissionType is FitnessPermissionType) {
            setPageName(PageName.TAB_ACCESS_PAGE)
        } else if (permissionType is MedicalPermissionType) {
            setPageName(PageName.TAB_MEDICAL_ACCESS_PAGE)
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(permissionType.upperCaseLabel())
        viewModel.loadAppMetaDataMap(permissionType)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadAppMetaDataMap(permissionType)
        viewModel.appMetadataMap.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AccessViewModel.AccessScreenState.Loading -> {
                    setLoading(isLoading = true)
                }
                is AccessViewModel.AccessScreenState.Error -> {
                    setError(hasError = true)
                }
                is AccessViewModel.AccessScreenState.WithData -> {
                    setLoading(isLoading = false, animate = false)
                    updateDataAccess(state.appMetadata)
                }
            }
        }

        deletionViewModel.inactiveAppsReloadNeeded.observe(viewLifecycleOwner) { isReloadNeeded ->
            if (isReloadNeeded) {
                viewModel.loadAppMetaDataMap(permissionType)
                deletionViewModel.resetInactiveAppsReloadNeeded()
            }
        }
    }

    private fun updateDataAccess(appMetadataMap: Map<AppAccessState, List<AppAccessMetadata>>) {
        mCanReadSection.removeAll()
        mCanWriteSection.removeAll()
        mInactiveSection.removeAll()
        noDataPreference.isVisible = false

        if (appMetadataMap.containsKey(AppAccessState.Read)) {
            if (appMetadataMap[AppAccessState.Read]!!.isEmpty()) {
                mCanReadSection.isVisible = false
            } else {
                mCanReadSection.isVisible = true
                appMetadataMap[AppAccessState.Read]!!.forEach { appAccessMetadata ->
                    mCanReadSection.addPreference(createAppPreference(appAccessMetadata))
                }
            }
        }
        if (appMetadataMap.containsKey(AppAccessState.Write)) {
            if (appMetadataMap[AppAccessState.Write]!!.isEmpty()) {
                mCanWriteSection.isVisible = false
            } else {
                mCanWriteSection.isVisible = true
                appMetadataMap[AppAccessState.Write]!!.forEach { appAccessMetadata ->
                    mCanWriteSection.addPreference(createAppPreference(appAccessMetadata))
                }
            }
        }
        if (appMetadataMap.containsKey(AppAccessState.Inactive)) {
            if (appMetadataMap[AppAccessState.Inactive]!!.isEmpty()) {
                mInactiveSection.isVisible = false
            } else {
                mInactiveSection.isVisible = true
                mInactiveSection.summary =
                    getString(
                        R.string.inactive_apps_message,
                        getString(permissionType.lowerCaseLabel()),
                    )
                appMetadataMap[AppAccessState.Inactive]?.forEach { appAccessMetadata ->
                    val appMetadata = appAccessMetadata.appMetadata
                    mInactiveSection.addPreference(
                        InactiveAppPreference(requireContext()).also {
                            it.title = appMetadata.appName
                            it.icon = appMetadata.icon
                            it.logName = DataAccessElement.DATA_ACCESS_INACTIVE_APP_BUTTON
                            it.setOnDeleteButtonClickListener {
                                deletionViewModel.setDeletionType(
                                    DeletionType.DeleteInactiveAppData(
                                        healthPermissionType = permissionType,
                                        packageName = appMetadata.packageName,
                                        appName = appMetadata.appName,
                                    )
                                )
                                parentFragmentManager.setFragmentResult(
                                    START_DELETION_ENTRIES_AND_ACCESS_KEY,
                                    Bundle(),
                                )
                            }
                        }
                    )
                }
            }
        }

        noDataPreference.isVisible =
            !mCanReadSection.isVisible && !mCanWriteSection.isVisible && !mInactiveSection.isVisible
    }

    private fun createAppPreference(appAccessMetadata: AppAccessMetadata): HealthAppPreference {
        return HealthAppPreference(requireContext(), appAccessMetadata.appMetadata).also {
            it.logName = DataAccessElement.DATA_ACCESS_APP_BUTTON
            it.setOnPreferenceClickListener {
                navigateToAppInfoScreen(appAccessMetadata)
                true
            }
        }
    }

    private fun navigateToAppInfoScreen(appAccessMetadata: AppAccessMetadata) {
        val appPermissionsType = appAccessMetadata.appPermissionsType
        val navigationId =
            when (appPermissionsType) {
                AppPermissionsType.FITNESS_PERMISSIONS_ONLY ->
                    R.id.action_entriesAndAccessFragment_to_fitnessApp
                AppPermissionsType.MEDICAL_PERMISSIONS_ONLY ->
                    R.id.action_entriesAndAccessFragment_to_medicalApp
                AppPermissionsType.COMBINED_PERMISSIONS ->
                    R.id.action_entriesAndAccessFragment_to_combinedPermissions
            }
        findNavController()
            .navigate(
                navigationId,
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, appAccessMetadata.appMetadata.packageName)
                    putString(EXTRA_APP_NAME, appAccessMetadata.appMetadata.appName)
                },
            )
    }
}
