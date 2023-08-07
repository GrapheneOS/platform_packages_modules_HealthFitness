package com.android.healthconnect.controller.datasources

import android.health.connect.HealthDataCategory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.appsources.AppSourcesPreference
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.lowercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HeaderPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.DeviceInfoUtilsImpl
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsSpinnerAdapter
import com.android.settingslib.widget.SettingsSpinnerPreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(HealthPreferenceFragment::class)
class DataSourcesFragment: Hilt_DataSourcesFragment() {

    companion object {
        private const val DATA_TYPE_SPINNER_PREFERENCE_KEY = "data_type"
        private const val DATA_TOTALS_PREFERENCE_GROUP = "data_totals_group"
        private const val APP_SOURCES_PREFERENCE_GROUP = "app_sources_group"
        private const val APP_SOURCES_PREFERENCE_KEY = "app_sources"
        private const val FOOTER_PREFERENCE_KEY = "data_sources_footer"
        private const val EMPTY_STATE_HEADER_PREFERENCE_KEY = "empty_state_header"

        private val dataSourcesCategories = arrayListOf(
                HealthDataCategory.ACTIVITY,
                HealthDataCategory.SLEEP)
    }

    private val healthPermissionsViewModel: HealthPermissionTypesViewModel by activityViewModels()
    private lateinit var spinnerPreference: SettingsSpinnerPreference
    private lateinit var dataSourcesCategoriesStrings: List<String>
    private var currentCategorySelection: @HealthDataCategoryInt Int = HealthDataCategory.ACTIVITY

    private val dataTotalsPreferenceGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(DATA_TOTALS_PREFERENCE_GROUP)
    }

    private val appSourcesPreferenceGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(APP_SOURCES_PREFERENCE_GROUP)
    }

    private val footerPreference: FooterPreference? by lazy {
        preferenceScreen.findPreference(FOOTER_PREFERENCE_KEY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.data_sources_and_priority_screen, rootKey)
        dataSourcesCategoriesStrings = dataSourcesCategories.map {
            category -> getString(category.uppercaseTitle())
        }

        spinnerPreference = SettingsSpinnerPreference(context)
        spinnerPreference.setAdapter(
                SettingsSpinnerAdapter<String>(context).also {
                it.addAll(dataSourcesCategoriesStrings)
            })
        spinnerPreference.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                healthPermissionsViewModel.loadData(dataSourcesCategories[position])
                healthPermissionsViewModel.loadAppsWithData(dataSourcesCategories[position])
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        })
        preferenceScreen.addPreference(spinnerPreference)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentStringSelection = spinnerPreference.selectedItem // this gives string
        currentCategorySelection = dataSourcesCategories[dataSourcesCategoriesStrings.indexOf(currentStringSelection)]

        healthPermissionsViewModel.loadData(currentCategorySelection)
        healthPermissionsViewModel.loadAppsWithData(currentCategorySelection)

        healthPermissionsViewModel.priorityList.observe(viewLifecycleOwner) { state ->
            when (state) {
            is HealthPermissionTypesViewModel.PriorityListState.Loading -> {
                appSourcesPreferenceGroup?.removePreferenceRecursively(APP_SOURCES_PREFERENCE_KEY)
            }
            is HealthPermissionTypesViewModel.PriorityListState.LoadingFailed -> {}
            is HealthPermissionTypesViewModel.PriorityListState.WithData -> {
                updateAppSources(state.priorityList)
            }
        }
        }
    }

    private fun updateAppSources(appSources: List<AppMetadata>) {
        val currentStringSelection = spinnerPreference.selectedItem // this gives string
        currentCategorySelection = dataSourcesCategories[dataSourcesCategoriesStrings.indexOf(currentStringSelection)]
        if (appSources.isEmpty()) {
            setupEmptyState(currentCategorySelection)
        } else {
            // Remove empty state
            preferenceScreen.removePreferenceRecursively(EMPTY_STATE_HEADER_PREFERENCE_KEY)
            setCategoriesVisibility(true)
            healthPermissionsViewModel.setEditedPriorityList(appSources)
            appSourcesPreferenceGroup?.addPreference(
                    AppSourcesPreference(requireContext(),
                            healthPermissionsViewModel,
                            currentCategorySelection).also {
                        it.key = APP_SOURCES_PREFERENCE_KEY
                    })
        }
    }

    private fun setupEmptyState(category: Int) {
        setCategoriesVisibility(false)
        preferenceScreen.addPreference(getHeaderPreference(category))
    }

    private fun setCategoriesVisibility(isVisible: Boolean) {
        dataTotalsPreferenceGroup?.isVisible = isVisible
        appSourcesPreferenceGroup?.isVisible = isVisible
        footerPreference?.isVisible = isVisible
    }

    private fun getHeaderPreference(category: Int): HeaderPreference {

        return HeaderPreference(requireContext()).also {
            it.setHeaderText(
                    getString(R.string.data_sources_empty_state,
                            getString(category.lowercaseTitle())))
            it.setHeaderLinkText(getString(R.string.data_sources_help_link))
            it.setHeaderLinkAction {
                DeviceInfoUtilsImpl().openHCGetStartedLink(requireActivity())
            }
            it.key = EMPTY_STATE_HEADER_PREFERENCE_KEY
        }
    }
}