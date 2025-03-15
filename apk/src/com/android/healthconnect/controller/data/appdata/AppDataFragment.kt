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
package com.android.healthconnect.controller.data.appdata

import android.content.Intent.EXTRA_PACKAGE_NAME
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
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionConstants
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.DELETE
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.VIEW
import com.android.healthconnect.controller.selectabledeletion.DeletionFragment
import com.android.healthconnect.controller.selectabledeletion.DeletionPermissionTypesPreference
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.selectabledeletion.SelectAllCheckboxPreference
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.NoDataPreference
import com.android.healthconnect.controller.shared.preference.addIntroOrAppHeaderPreference
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import com.android.healthconnect.controller.utils.logging.AppDataElement
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

/** Fragment to display data in Health Connect written by a given app. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
open class AppDataFragment : Hilt_AppDataFragment() {

    companion object {
        private const val TAG = "AppDataFragmentTag"
        const val PERMISSION_TYPE_NAME_KEY = "permission_type_name_key"
        private const val DELETION_TAG = "DeletionTag"
        private const val KEY_SELECT_ALL = "key_select_all"
        private const val KEY_PERMISSION_TYPES = "key_permission_types"
        private const val KEY_NO_DATA = "no_data_preference"
        private const val KEY_ZERO_STATE = "zero_state_preference"
        private const val KEY_FOOTER = "key_footer"
    }

    init {
        this.setPageName(PageName.APP_DATA_PAGE)
    }

    @Inject lateinit var logger: HealthConnectLogger
    private var packageName: String = ""
    private var appName: String = ""

    private val viewModel: AppDataViewModel by viewModels()
    private val deletionViewModel: DeletionViewModel by activityViewModels()

    private val selectAllCheckboxPreference: SelectAllCheckboxPreference by pref(KEY_SELECT_ALL)
    private val permissionTypesListGroup: PreferenceCategory by pref(KEY_PERMISSION_TYPES)

    private val noDataPreference: NoDataPreference by pref(KEY_NO_DATA)
    private val zeroStatePreference: ZeroStatePreference by pref(KEY_ZERO_STATE)
    private val footerPreference: FooterPreference by pref(KEY_FOOTER)

    // Not in deletion state
    private val onMenuSetup: (MenuItem) -> Boolean = { menuItem ->
        when (menuItem.itemId) {
            R.id.menu_enter_deletion_state -> {
                logger.logInteraction(ToolbarElement.TOOLBAR_ENTER_DELETION_STATE_BUTTON)
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
                triggerDeletionState(VIEW)
                true
            }

            else -> false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.app_data_screen, rootKey)

        if (
            requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
            requireArguments().getString(EXTRA_PACKAGE_NAME) != null
        ) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
        if (
            requireArguments().containsKey(Constants.EXTRA_APP_NAME) &&
            requireArguments().getString(Constants.EXTRA_APP_NAME) != null
        ) {
            appName = requireArguments().getString(Constants.EXTRA_APP_NAME)!!
        }

        if (childFragmentManager.findFragmentByTag(DELETION_TAG) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), DELETION_TAG) }
        }
        selectAllCheckboxPreference.logName = AppDataElement.SELECT_ALL_BUTTON
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadAppInfo(packageName)
        viewModel.loadAppData(packageName)

        viewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            addIntroOrAppHeaderPreference(preferenceScreen, requireContext(), appMetadata)
            val footerString = getString(R.string.app_data_no_data_footer, appMetadata.appName)
            footerPreference.title = footerString
            zeroStatePreference.summary = footerString
        }

        viewModel.fitnessAndMedicalData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AppDataViewModel.AppDataState.Loading -> {
                    setLoading(isLoading = true)
                }
                is AppDataViewModel.AppDataState.Error -> {
                    setError(hasError = true)
                }
                is AppDataViewModel.AppDataState.WithData -> {
                    setLoading(isLoading = false)
                    setError(hasError = false)
                    updatePreferenceScreen(state.dataMap)
                }
            }
        }

        viewModel.deletionScreenState.observe(viewLifecycleOwner) { screenState ->
            updateMenu(screenState)
        }

        deletionViewModel.appPermissionTypesReloadNeeded.observe(viewLifecycleOwner) {
                isReloadNeeded ->
            if (isReloadNeeded) {
                viewModel.setDeletionScreenStateValue(VIEW)
                viewModel.loadAppData(packageName)
                deletionViewModel.resetAppPermissionTypesReloadNeeded()
            }
        }
    }

    private fun updatePreferenceScreen(
        permissionTypesPerCategoryList: List<PermissionTypesPerCategory>
    ) {
        val context = requireContext()
        permissionTypesListGroup.removeAll()

        val populatedCategories = sortAndAddMedicalToLast(permissionTypesPerCategoryList)
        if (populatedCategories.isEmpty()) {
            setupEmptyState()
            return
        }
        setupSelectAllPreference(screenState = viewModel.getDeletionScreenStateValue())
        updateMenu(viewModel.getDeletionScreenStateValue())
        noDataPreference.isVisible = false
        zeroStatePreference.isVisible = false
        footerPreference.isVisible = false

        populatedCategories.forEach { permissionTypesPerCategory ->
            val category = permissionTypesPerCategory.category

            val preferenceCategory =
                PreferenceCategory(context).also { it.setTitle(category.uppercaseTitle()) }
            permissionTypesListGroup.addPreference(preferenceCategory)

            permissionTypesPerCategory.data
                .sortByLocale { getString(it.upperCaseLabel()) }
                .forEach { permissionType ->
                    preferenceCategory.addPreference(
                        getPermissionTypePreference(permissionType, permissionType.icon(context))
                    )
                }
        }
    }

    /** Sorts fitness categories alphabetically and appends the medical category to the end. */
    private fun sortAndAddMedicalToLast(
        permissionTypesPerCategoryList: List<PermissionTypesPerCategory>
    ): List<PermissionTypesPerCategory> {
        val populatedFitnessCategories =
            permissionTypesPerCategoryList
                .filter { it.data.isNotEmpty() && it.category != MEDICAL }
                .sortByLocale { getString(it.category.uppercaseTitle()) }

        val medicalCategory =
            permissionTypesPerCategoryList.find { it.category == MEDICAL && it.data.isNotEmpty() }

        return if (medicalCategory != null) {
            populatedFitnessCategories + medicalCategory
        } else {
            populatedFitnessCategories
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
        updateMenu(screenState = VIEW, hasData = false)
        setupSelectAllPreference(screenState = VIEW)
    }

    private fun updateMenu(screenState: DeletionScreenState, hasData: Boolean = true) {
        if (!hasData) {
            setupSharedMenu(viewLifecycleOwner, logger)
            return
        }

        if (screenState == VIEW) {
            logger.logImpression(ToolbarElement.TOOLBAR_ENTER_DELETION_STATE_BUTTON)
            setupMenu(R.menu.app_data_menu, viewLifecycleOwner, logger, onMenuSetup)
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
        updateMenu(screenState)
        setupSelectAllPreference(screenState)
        viewModel.setDeletionScreenStateValue(screenState)

        iterateThroughPreferenceGroup { permissionTypePreference ->
            permissionTypePreference.setShowCheckbox(screenState == DELETE)
        }
    }

    private fun deleteData() {
        deletionViewModel.setDeletionType(
            DeletionType.DeleteHealthPermissionTypesFromApp(
                viewModel.setOfPermissionTypesToBeDeleted.value.orEmpty(),
                viewModel.getTheNumOfPermissionTypes(),
                packageName,
                appName,
            )
        )
        childFragmentManager.setFragmentResult(DeletionConstants.START_DELETION_KEY, bundleOf())
    }

    private fun getPermissionTypePreference(
        permissionType: HealthPermissionType,
        categoryIcon: Drawable?,
    ): Preference {
        val pref =
            DeletionPermissionTypesPreference(requireContext(), viewModel) {
                findNavController()
                    .navigate(
                        R.id.action_appData_to_appEntries,
                        bundleOf(
                            EXTRA_PACKAGE_NAME to packageName,
                            Constants.EXTRA_APP_NAME to appName,
                            PERMISSION_TYPE_NAME_KEY to permissionType.name,
                        ),
                    )
                true
            }

        pref.apply {
            setShowCheckbox(viewModel.getDeletionScreenStateValue() == DELETE)
            setLogNameCheckbox(AppDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
            setLogNameNoCheckbox(AppDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
            setHealthPermissionType(permissionType)

            viewModel.setOfPermissionTypesToBeDeleted.observe(viewLifecycleOwner) { deleteSet ->
                setIsChecked(permissionType in deleteSet)
            }

            icon = categoryIcon
            setTitle(permissionType.upperCaseLabel())
        }

        return pref
    }

    private fun setupSelectAllPreference(screenState: DeletionScreenState) {
        selectAllCheckboxPreference.isVisible = screenState == DELETE
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
