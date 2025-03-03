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
package com.android.healthconnect.controller.data.alldata

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.PERMISSION_TYPE_NAME_KEY
import com.android.healthconnect.controller.data.appdata.PermissionTypesPerCategory
import com.android.healthconnect.controller.data.entries.EntriesViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionConstants.START_DELETION_KEY
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.DELETE
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.VIEW
import com.android.healthconnect.controller.selectabledeletion.DeletionFragment
import com.android.healthconnect.controller.selectabledeletion.DeletionPermissionTypesPreference
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.selectabledeletion.SelectAllCheckboxPreference
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.EmptyPreferenceCategory
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.NoDataPreference
import com.android.healthconnect.controller.shared.preference.topIntroPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import com.android.healthconnect.controller.utils.logging.AllDataElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.ToolbarElement
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.setupMenu
import com.android.healthconnect.controller.utils.setupSharedMenu
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.ZeroStatePreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment for fitness permission types. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
open class AllDataFragment : Hilt_AllDataFragment() {

    companion object {
        private const val TAG = "AllDataFragmentTag"
        private const val DELETION_TAG = "DeletionTag"
        private const val KEY_SELECT_ALL = "key_select_all"
        private const val KEY_PERMISSION_TYPE = "key_permission_type"
        private const val KEY_NO_DATA = "no_data_preference"
        private const val KEY_ZERO_STATE = "zero_state_preference"
        private const val KEY_TOP_INTRO = "key_top_intro"
        private const val KEY_FOOTER = "key_footer"
        const val IS_BROWSE_MEDICAL_DATA_SCREEN = "key_is_browse_medical_data_screen"
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    /** Decides whether this screen is supposed to display Fitness data or Medical data. */
    private var showMedicalData = false

    @HealthDataCategoryInt private var category: Int = 0

    private val viewModel: AllDataViewModel by viewModels()

    private val deletionViewModel: DeletionViewModel by activityViewModels()

    private val selectAllCheckboxPreference: SelectAllCheckboxPreference by pref(KEY_SELECT_ALL)

    private val permissionTypesListGroup: PreferenceCategory by pref(KEY_PERMISSION_TYPE)

    private val noDataPreference: NoDataPreference by pref(KEY_NO_DATA)

    private val zeroStatePreference: ZeroStatePreference by pref(KEY_ZERO_STATE)

    private val footerPreference: FooterPreference by pref(KEY_FOOTER)

    private val entriesViewModel: EntriesViewModel by activityViewModels()

    // Empty state
    private val onDataSourcesClick: (MenuItem) -> Boolean = { menuItem ->
        when (menuItem.itemId) {
            R.id.menu_data_sources -> {
                logger.logInteraction(ToolbarElement.TOOLBAR_DATA_SOURCES_BUTTON)
                findNavController().navigate(R.id.action_allDataFragment_to_dataSourcesFragment)
                true
            }

            else -> false
        }
    }

    // Not in deletion state
    private val onMenuSetup: (MenuItem) -> Boolean = { menuItem ->
        when (menuItem.itemId) {
            R.id.menu_data_sources -> {
                logger.logInteraction(ToolbarElement.TOOLBAR_DATA_SOURCES_BUTTON)
                findNavController().navigate(R.id.action_allDataFragment_to_dataSourcesFragment)
                true
            }

            R.id.menu_enter_deletion_state -> {
                logger.logInteraction(ToolbarElement.TOOLBAR_ENTER_DELETION_STATE_BUTTON)
                // enter deletion state
                triggerDeletionState(DELETE)
                true
            }

            else -> false
        }
    }

    // In deletion state with data selected
    private val onEnterDeletionState: (MenuItem) -> Boolean = { menuItem ->
        when (menuItem.itemId) {
            R.id.delete -> {
                logger.logInteraction(ToolbarElement.TOOLBAR_DELETE_BUTTON)
                deleteData()
                true
            }

            else -> false
        }
    }

    // In deletion state without any data selected
    private val onEmptyDeleteSetSetup: (MenuItem) -> Boolean = { menuItem ->
        when (menuItem.itemId) {
            R.id.menu_exit_deletion_state -> {
                logger.logInteraction(ToolbarElement.TOOLBAR_EXIT_DELETION_STATE_BUTTON)
                // exit deletion state
                triggerDeletionState(VIEW)
                true
            }

            else -> false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.all_data_screen, rootKey)
        val hasBrowseMedicalDataKey = arguments?.containsKey(IS_BROWSE_MEDICAL_DATA_SCREEN) ?: false
        if (hasBrowseMedicalDataKey) {
            showMedicalData =
                arguments?.getBoolean(IS_BROWSE_MEDICAL_DATA_SCREEN)
                    ?: throw IllegalArgumentException(
                        "IS_BROWSE_MEDICAL_DATA_SCREEN can't be null!"
                    )
        }
        if (childFragmentManager.findFragmentByTag(DELETION_TAG) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), DELETION_TAG) }
        }
        if (showMedicalData) {
            setPageName(PageName.ALL_MEDICAL_DATA_PAGE)
        } else {
            setPageName(PageName.ALL_DATA_PAGE)
        }
        selectAllCheckboxPreference.logName = AllDataElement.SELECT_ALL_BUTTON
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAllData()
        setTopIntroVisibility(false)

        viewModel.allData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AllDataViewModel.AllDataState.Loading -> {
                    setLoading(isLoading = true)
                    if (viewModel.getDeletionScreenStateValue() == VIEW) {
                        updateMenu(screenState = VIEW)
                        triggerDeletionState(screenState = VIEW)
                    }
                }

                is AllDataViewModel.AllDataState.Error -> {
                    setError(hasError = true)
                }

                is AllDataViewModel.AllDataState.WithData -> {
                    setLoading(isLoading = false)
                    setError(hasError = false)
                    updatePreferenceScreen(state.dataMap)
                }
            }
        }

        viewModel.deletionScreenState.observe(viewLifecycleOwner) { screenState ->
            updateMenu(screenState)
        }

        deletionViewModel.permissionTypesReloadNeeded.observe(viewLifecycleOwner) { isReloadNeeded
            ->
            if (isReloadNeeded) {
                viewModel.setDeletionScreenStateValue(VIEW)
                loadAllData()
                deletionViewModel.resetPermissionTypesReloadNeeded()
            }
        }
    }

    private fun loadAllData() {
        if (showMedicalData) {
            viewModel.loadAllMedicalData()
        } else {
            viewModel.loadAllFitnessData()
        }
    }

    private fun setTopIntroVisibility(visible: Boolean) {
        if (visible) {
            if (findPreference<Preference>(KEY_TOP_INTRO) == null) {
                preferenceScreen.addPreference(
                    topIntroPreference(
                        preferenceKey = KEY_TOP_INTRO,
                        context = requireContext(),
                        preferenceTitle = getString(R.string.browse_health_records_intro),
                        learnMoreText = getString(R.string.medical_request_about_health_records),
                        learnMoreAction = {
                            deviceInfoUtils.openHCGetStartedLink(requireActivity())
                        },
                    )
                )
            }
        } else {
            val preference = findPreference<Preference>(KEY_TOP_INTRO)
            if (preference != null) {
                preferenceScreen.removePreference(preference)
            }
        }
    }

    private fun updatePreferenceScreen(
        permissionTypesPerCategoryList: List<PermissionTypesPerCategory>
    ) {
        permissionTypesListGroup.removeAll()

        val populatedCategories =
            permissionTypesPerCategoryList
                .filter { it.data.isNotEmpty() }
                .sortByLocale { getString(it.category.uppercaseTitle()) }

        if (populatedCategories.isEmpty()) {
            setupEmptyState()
            return
        }

        setupSelectAllPreference(screenState = viewModel.getDeletionScreenStateValue())

        updateMenu(screenState = viewModel.getDeletionScreenStateValue())
        noDataPreference.isVisible = false
        footerPreference.isVisible = false
        zeroStatePreference.isVisible = false

        populatedCategories.forEach { permissionTypesPerCategory ->
            val category = permissionTypesPerCategory.category

            val preferenceCategory =
                if (showMedicalData) {
                    EmptyPreferenceCategory(requireContext())
                } else {
                    PreferenceCategory(requireContext()).also {
                        it.setTitle(category.uppercaseTitle())
                    }
                }
            permissionTypesListGroup.addPreference(preferenceCategory)

            permissionTypesPerCategory.data
                .sortByLocale { getString(it.upperCaseLabel()) }
                .forEach { permissionType ->
                    val icon = permissionType.icon(requireContext())
                    preferenceCategory.addPreference(
                        getPermissionTypePreference(permissionType, icon)
                    )
                }
        }
    }

    private fun updateMenu(screenState: DeletionScreenState, hasData: Boolean = true) {
        if (!hasData && showMedicalData) {
            setupSharedMenu(viewLifecycleOwner, logger)
            return
        }

        if (!hasData) {
            logger.logImpression(ToolbarElement.TOOLBAR_DATA_SOURCES_BUTTON)
            setupMenu(
                R.menu.all_data_empty_state_menu,
                viewLifecycleOwner,
                logger,
                onDataSourcesClick,
            )
            return
        }

        if (screenState == VIEW && showMedicalData) {
            setupMenu(
                R.menu.all_data_menu_without_data_sources,
                viewLifecycleOwner,
                logger,
                onMenuSetup,
            )
            return
        }

        if (screenState == VIEW) {
            logger.logImpression(ToolbarElement.TOOLBAR_ENTER_DELETION_STATE_BUTTON)
            setupMenu(R.menu.all_data_menu, viewLifecycleOwner, logger, onMenuSetup)
            return
        }

        if (viewModel.setOfPermissionTypesToBeDeleted.value.orEmpty().isEmpty()) {
            logger.logImpression(ToolbarElement.TOOLBAR_EXIT_DELETION_STATE_BUTTON)
            setupMenu(
                R.menu.all_data_delete_menu,
                viewLifecycleOwner,
                logger,
                onEmptyDeleteSetSetup,
            )
            return
        }

        logger.logImpression(ToolbarElement.TOOLBAR_DELETE_BUTTON)
        setupMenu(R.menu.deletion_state_menu, viewLifecycleOwner, logger, onEnterDeletionState)
    }

    @VisibleForTesting
    fun triggerDeletionState(screenState: DeletionScreenState) {
        viewModel.setDeletionScreenStateValue(screenState)
        setupSelectAllPreference(screenState)
        updateMenu(screenState)

        iterateThroughPreferenceGroup { permissionTypePreference ->
            permissionTypePreference.setShowCheckbox(screenState == DELETE)
        }

        // scroll to top to show Select all preference when triggered
        if (screenState == DELETE) {
            scrollToPreference(KEY_SELECT_ALL)
        }
    }

    private fun setupEmptyState() {
        if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            zeroStatePreference.isVisible = true
            noDataPreference.isVisible = false
            footerPreference.isVisible = false
        } else {
            zeroStatePreference.isVisible = false
            noDataPreference.isVisible = true
            footerPreference.isVisible = true
        }
        setTopIntroVisibility(false)
        updateMenu(screenState = VIEW, hasData = false)
    }

    private fun deleteData() {
        deletionViewModel.setDeletionType(
            DeletionType.DeleteHealthPermissionTypes(
                viewModel.setOfPermissionTypesToBeDeleted.value.orEmpty(),
                viewModel.getTheNumOfPermissionTypes(),
            )
        )
        childFragmentManager.setFragmentResult(START_DELETION_KEY, bundleOf())
    }

    private fun getPermissionTypePreference(
        permissionType: HealthPermissionType,
        categoryIcon: Drawable?,
    ): Preference {
        val pref =
            DeletionPermissionTypesPreference(requireContext(), viewModel) {
                findNavController()
                    .navigate(
                        navigationDestination(permissionType),
                        bundleOf(PERMISSION_TYPE_NAME_KEY to permissionType.name),
                    )
                true
            }

        pref.apply {
            setShowCheckbox(viewModel.getDeletionScreenStateValue() == DELETE)
            setLogNameCheckbox(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
            setLogNameNoCheckbox(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)

            icon = categoryIcon
            setTitle(permissionType.upperCaseLabel())
            setHealthPermissionType(permissionType)

            viewModel.setOfPermissionTypesToBeDeleted.observe(viewLifecycleOwner) { deleteSet ->
                setIsChecked(permissionType in deleteSet)
            }

            entriesViewModel.setScreenState(EntriesViewModel.EntriesDeletionScreenState.VIEW)
            entriesViewModel.setAllEntriesSelectedValue(false)
            entriesViewModel.currentSelectedDate.value = null
        }

        return pref
    }

    private fun navigationDestination(permissionType: HealthPermissionType): Int {
        return if (permissionType is FitnessPermissionType) R.id.action_allData_to_entriesAndAccess
        else R.id.action_medicalAllData_to_entriesAndAccess
    }

    private fun setupSelectAllPreference(screenState: DeletionScreenState) {
        selectAllCheckboxPreference.isVisible = screenState == DELETE
        setTopIntroVisibility(showMedicalData && screenState == VIEW)
        if (screenState == DELETE) {
            viewModel.allPermissionTypesSelected.observe(viewLifecycleOwner) {
                allPermissionTypesSelected ->
                selectAllCheckboxPreference.removeOnPreferenceClickListener()
                selectAllCheckboxPreference.setIsChecked(allPermissionTypesSelected)
                selectAllCheckboxPreference.setOnPreferenceClickListenerWithCheckbox(
                    onSelectAllPermissionTypes()
                )
            }
            selectAllCheckboxPreference.setOnPreferenceClickListenerWithCheckbox(
                onSelectAllPermissionTypes()
            )
        }
    }

    private fun onSelectAllPermissionTypes(): () -> Unit {
        return {
            iterateThroughPreferenceGroup { permissionTypePreference ->
                if (selectAllCheckboxPreference.getIsChecked()) {
                    viewModel.addToDeletionSet(permissionTypePreference.getHealthPermissionType())
                } else {
                    viewModel.removeFromDeletionSet(
                        permissionTypePreference.getHealthPermissionType()
                    )
                }
            }
            updateMenu(DELETE)
        }
    }

    private fun iterateThroughPreferenceGroup(method: (DeletionPermissionTypesPreference) -> Unit) {
        permissionTypesListGroup.children.forEach { preference ->
            if (preference is PreferenceCategory) {
                preference.children.forEach { permissionTypePreference ->
                    if (permissionTypePreference is DeletionPermissionTypesPreference) {
                        method(permissionTypePreference)
                    }
                }
            }
        }
    }
}
