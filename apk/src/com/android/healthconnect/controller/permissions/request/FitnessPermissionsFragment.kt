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
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.getSortedDataCategoryToStringMap
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.ExpandablePreferenceAdapter
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthToggleExpandablePreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.pref
import com.android.healthfitness.flags.Flags.permissionRequestBottomSheet
import com.android.healthfitness.flags.Flags.permissionsGroupingUi
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

    private val customStylePreferences = mutableListOf<Preference>()
    private val permissionMap: MutableMap<FitnessPermission, TwoStatePreference> = mutableMapOf()

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
                    // We only need to handle this if the bottom sheet flag is disabled, as the
                    // bottom sheet permission fragment dialog already manages this state.
                    if (!permissionRequestBottomSheet()) {
                        removeFragment()
                    }
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

        viewModel.grantedFitnessPermissions.observe(viewLifecycleOwner) { grantedPermissions ->
            if (!viewModel.isFitnessPermissionRequestConcluded()) {
                getAllowButton().isEnabled = grantedPermissions.isNotEmpty()
            }
            permissionMap.forEach { (healthPermission, switchPreference) ->
                switchPreference.isChecked = healthPermission in grantedPermissions
            }
        }
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        if (permissionsGroupingUi()) {
            return ExpandablePreferenceAdapter(preferenceScreen, customStylePreferences)
        }
        return super.onCreateAdapter(preferenceScreen)
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

        val onChecked = suspend {
            toggleAllFitnessPermissions(true)
            true
        }
        val onUnchecked = suspend {
            toggleAllFitnessPermissions(false)
            true
        }

        allowAllPreference.setUpStateManagement(
            viewLifecycleOwner,
            viewModel.allFitnessPermissionsGranted,
            onChecked,
            onUnchecked,
        )
    }

    private fun toggleAllFitnessPermissions(isChecked: Boolean) {
        toggleCategoryPermissions(readPermissionCategory, isChecked)
        toggleCategoryPermissions(writePermissionCategory, isChecked)
        viewModel.updateFitnessPermissions(isChecked)

        // Notify the adapter that the data has changed to force a redraw of the visible items.
        // This is crucial because PreferenceFragmentCompat uses a RecyclerView.
        (listView.adapter as? PreferenceGroupAdapter)?.notifyDataSetChanged()
    }

    private fun toggleCategoryPermissions(preferenceGroup: PreferenceGroup, isChecked: Boolean) {
        if (permissionsGroupingUi()) {
            preferenceGroup.children.forEach { dataCategory ->
                (dataCategory as? HealthToggleExpandablePreference)?.isChecked = isChecked
                (dataCategory as? PreferenceGroup)?.children?.forEach { preference ->
                    (preference as? TwoStatePreference)?.isChecked = isChecked
                }
            }
        } else {
            preferenceGroup.children.forEach { preference ->
                (preference as? TwoStatePreference)?.isChecked = isChecked
            }
        }
    }

    private fun updateDataList(permissionsList: List<HealthPermission.FitnessPermission>) {
        readPermissionCategory.removeAll()
        writePermissionCategory.removeAll()
        permissionMap.clear()

        if (permissionsGroupingUi()) {
            customStylePreferences.clear()
            updateGroupedPermissionsUi(permissionsList)
        } else {
            updateFlatPermissionsUi(permissionsList)
        }

        readPermissionCategory.isVisible = readPermissionCategory.preferenceCount > 0
        writePermissionCategory.isVisible = writePermissionCategory.preferenceCount > 0
    }

    private fun updateFlatPermissionsUi(permissionsList: List<HealthPermission.FitnessPermission>) {
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
    }

    private fun updateGroupedPermissionsUi(
        permissionsList: List<HealthPermission.FitnessPermission>
    ) {
        val dataCategoryEnumToDataCategoryStringSortedMap =
            getSortedDataCategoryToStringMap(requireContext())

        val permissionGroupKeyToRequestedPermissions =
            permissionsList.groupBy {
                PermissionGroupKey(
                    it.permissionsAccessType,
                    HealthDataCategoryExtensions.fromFitnessPermissionType(it.fitnessPermissionType),
                )
            }

        populateGroupedPermissionsUi(
            permissionGroupKeyToRequestedPermissions,
            PermissionsAccessType.READ,
            readPermissionCategory,
            dataCategoryEnumToDataCategoryStringSortedMap,
        )

        populateGroupedPermissionsUi(
            permissionGroupKeyToRequestedPermissions,
            PermissionsAccessType.WRITE,
            writePermissionCategory,
            dataCategoryEnumToDataCategoryStringSortedMap,
        )
    }

    private fun populateGroupedPermissionsUi(
        permissionGroupKeyToRequestedPermissions:
            Map<PermissionGroupKey, List<HealthPermission.FitnessPermission>>,
        accessType: PermissionsAccessType,
        preferenceGroup: PreferenceGroup,
        sortedCategories: Map<Int, String>,
    ) {
        sortedCategories.keys
            .filter { dataCategory ->
                !permissionGroupKeyToRequestedPermissions[
                        PermissionGroupKey(accessType, dataCategory)]
                    .isNullOrEmpty()
            }
            .forEach { dataCategory ->
                val preferenceKey = PermissionGroupKey(accessType, dataCategory)
                val permissions = permissionGroupKeyToRequestedPermissions[preferenceKey]!!

                if (viewModel.expandedDataCategoryPreferenceKeys.value?.isEmpty() == true) {
                    viewModel.updateDataCategoryPreferenceKey(preferenceKey, /* isExpanded= */ true)
                }

                val expandablePreference =
                    getExpandablePreferenceForDataCategory(dataCategory, preferenceKey, permissions)
                customStylePreferences.add(expandablePreference)
                preferenceGroup.addPreference(expandablePreference)

                permissions
                    .sortByLocale {
                        requireContext()
                            .getString(
                                FitnessPermissionStrings.fromPermissionType(
                                        it.fitnessPermissionType
                                    )
                                    .uppercaseLabel
                            )
                    }
                    .forEachIndexed { index, permission ->
                        val value = viewModel.isPermissionLocallyGranted(permission)
                        val permissionPreference =
                            getPermissionPreference(
                                value,
                                permission,
                                isLastInGroup = index == permissions.size - 1,
                            )

                        customStylePreferences.add(permissionPreference)
                        expandablePreference.addPreference(permissionPreference)
                    }
            }
    }

    private fun getExpandablePreferenceForDataCategory(
        dataCategory: Int,
        preferenceKey: PermissionGroupKey,
        permissionsForCategory: List<FitnessPermission>,
    ): HealthToggleExpandablePreference {
        return HealthToggleExpandablePreference(requireContext()).apply {
            title = getExpandablePreferenceTitle(dataCategory)
            summary =
                getExpandablePreferenceSummary(
                    permissionsForCategory,
                    viewModel.grantedFitnessPermissions.value,
                )
            key = preferenceKey.toString()
            permissionType = preferenceKey.accessType
            setExpanded(
                viewModel.expandedDataCategoryPreferenceKeys.value?.contains(
                    preferenceKey.toString()
                ) == true
            )
            setOnExpandChangeListener { isExpanded ->
                viewModel.updateDataCategoryPreferenceKey(preferenceKey, isExpanded)
            }
            viewModel.grantedFitnessPermissions.observe(viewLifecycleOwner) { grantedPermissions ->
                isChecked = grantedPermissions.containsAll(permissionsForCategory)
                summary =
                    getExpandablePreferenceSummary(
                        permissionsForCategory,
                        viewModel.grantedFitnessPermissions.value,
                    )
            }
            setOnSwitchChangeListener { isChecked ->
                viewModel.updateHealthPermissions(permissionsForCategory, isChecked)
            }
        }
    }

    private fun getExpandablePreferenceTitle(dataCategory: Int): String {
        return requireContext()
            .getString(
                R.string.health_data_category_expandable_preference_title,
                getString(dataCategory.uppercaseTitle()),
            )
    }

    private fun getExpandablePreferenceSummary(
        permissionsRequestedForCategory: List<FitnessPermission>,
        allPermissionsGranted: Set<FitnessPermission>?,
    ): String {
        // TODO(b/461857819): Move this logic to the view model.
        val numberOfPermissionsGrantedForCategory =
            if (allPermissionsGranted.isNullOrEmpty()) {
                0
            } else {
                allPermissionsGranted.intersect(permissionsRequestedForCategory).size
            }
        return requireContext()
            .getString(
                R.string.app_permissions_granted_summary,
                numberOfPermissionsGrantedForCategory,
                permissionsRequestedForCategory.size,
            )
    }

    private fun getPermissionPreference(
        defaultValue: Boolean,
        permission: HealthPermission.FitnessPermission,
        isLastInGroup: Boolean = false,
    ): HealthSwitchPreference {
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
            it.permission = permission
            if (permissionsGroupingUi()) {
                it.isLastInGroup = isLastInGroup
            }
            it.setOnPreferenceChangeListener { preference, newValue ->
                viewModel.updateHealthPermission(permission, newValue as Boolean)
                true
            }
            permissionMap[permission] = it
        }
    }
}
