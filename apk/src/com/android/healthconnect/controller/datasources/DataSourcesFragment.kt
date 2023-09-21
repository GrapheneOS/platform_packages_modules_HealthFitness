/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.datasources

import android.health.connect.HealthDataCategory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategoriesFragment.Companion.CATEGORY_KEY
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PotentialAppSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.AggregationCardsState
import com.android.healthconnect.controller.datasources.appsources.AppSourcesPreference
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel.NewPriorityListState
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.lowercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.CardContainerPreference
import com.android.healthconnect.controller.shared.preference.HeaderPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtilsImpl
import com.android.healthconnect.controller.utils.TimeSource
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsSpinnerAdapter
import com.android.settingslib.widget.SettingsSpinnerPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(HealthPreferenceFragment::class)
class DataSourcesFragment: Hilt_DataSourcesFragment() {

    companion object {
        private const val DATA_TOTALS_PREFERENCE_GROUP = "data_totals_group"
        private const val DATA_TOTALS_PREFERENCE_KEY = "data_totals_preference"
        private const val APP_SOURCES_PREFERENCE_GROUP = "app_sources_group"
        private const val APP_SOURCES_PREFERENCE_KEY = "app_sources"
        private const val ADD_AN_APP_PREFERENCE_KEY = "add_an_app"
        private const val NON_EMPTY_FOOTER_PREFERENCE_KEY = "data_sources_footer"
        private const val EMPTY_STATE_HEADER_PREFERENCE_KEY = "empty_state_header"
        private const val EMPTY_STATE_FOOTER_PREFERENCE_KEY = "empty_state_footer"

        private val dataSourcesCategories = arrayListOf(
                HealthDataCategory.ACTIVITY,
                HealthDataCategory.SLEEP)
    }

    private val healthPermissionsViewModel: HealthPermissionTypesViewModel by activityViewModels()
    private val dataSourcesViewModel: DataSourcesViewModel by activityViewModels()
    private lateinit var spinnerPreference: SettingsSpinnerPreference
    private lateinit var dataSourcesCategoriesStrings: List<String>
    private var currentCategorySelection: @HealthDataCategoryInt Int = HealthDataCategory.ACTIVITY
    @Inject
    lateinit var timeSource : TimeSource

    private val dataTotalsPreferenceGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(DATA_TOTALS_PREFERENCE_GROUP)
    }

    private val appSourcesPreferenceGroup: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(APP_SOURCES_PREFERENCE_GROUP)
    }

    private val nonEmptyFooterPreference: FooterPreference? by lazy {
        preferenceScreen.findPreference(NON_EMPTY_FOOTER_PREFERENCE_KEY)
    }

    private val mediator = MediatorLiveData<Pair<NewPriorityListState?, AggregationCardsState?>>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.data_sources_and_priority_screen, rootKey)
        dataSourcesCategoriesStrings = dataSourcesCategories.map {
            category -> getString(category.uppercaseTitle())
        }

        setupSpinnerPreference()

        mediator.addSource(healthPermissionsViewModel.newPriorityList) { result ->
            if (result.shouldObserve) {
                mediator.value = Pair(result, mediator.value?.second)
            }
        }

        mediator.addSource(dataSourcesViewModel.aggregationCardsData) { result ->
            mediator.value = Pair(mediator.value?.first, result)
        }
    }

    override fun onResume() {
        super.onResume()
        healthPermissionsViewModel.loadData(currentCategorySelection)
        dataSourcesViewModel.loadPotentialAppSources(currentCategorySelection)
        dataSourcesViewModel.loadMostRecentAggregations()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentStringSelection = spinnerPreference.selectedItem
        currentCategorySelection = dataSourcesCategories[dataSourcesCategoriesStrings.indexOf(currentStringSelection)]

        healthPermissionsViewModel.loadData(currentCategorySelection)
        dataSourcesViewModel.loadPotentialAppSources(currentCategorySelection)

        // we only show DataTotalsCards if the current selection is Activity
        if (currentCategorySelection == HealthDataCategory.ACTIVITY) {
            dataSourcesViewModel.loadMostRecentAggregations()
        }

        dataSourcesViewModel.potentialAppSources.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PotentialAppSourcesState.Loading -> {}
                is PotentialAppSourcesState.LoadingFailed -> {}
                is PotentialAppSourcesState.WithData -> {
                    updateAddApp(state.appSources.isNotEmpty())
                }
            }
        }

        mediator.observe(viewLifecycleOwner) { (priorityListState, aggregationInfoState) ->
            when {
                priorityListState is NewPriorityListState.Loading ||
                        aggregationInfoState is AggregationCardsState.Loading ->
                    setLoading(true)

                priorityListState is NewPriorityListState.LoadingFailed ||
                        aggregationInfoState is AggregationCardsState.LoadingFailed-> {
                    setLoading(false)
                    setError(true)
                }

                priorityListState is NewPriorityListState.WithData &&
                        aggregationInfoState is AggregationCardsState.WithData -> {

                    setLoading(false)
                    val priorityList = priorityListState.priorityList
                    val cardInfos = aggregationInfoState.dataTotals

                    if (priorityList.isEmpty()) {
                        addEmptyState()
                    } else {
                        updatePriorityList(priorityList)
                        updateCards(cardInfos)
                    }
                }
            }

        }
    }

    private fun updatePriorityList(appSources: List<AppMetadata>) {
        removeEmptyState()
        appSourcesPreferenceGroup?.isVisible = true
        appSourcesPreferenceGroup?.removePreferenceRecursively(APP_SOURCES_PREFERENCE_KEY)

        healthPermissionsViewModel.setEditedPriorityList(appSources)
        appSourcesPreferenceGroup?.addPreference(
                AppSourcesPreference(requireContext(),
                        healthPermissionsViewModel,
                        currentCategorySelection).also {
                    it.key = APP_SOURCES_PREFERENCE_KEY
                })

        nonEmptyFooterPreference?.isVisible = true
    }


    private fun updateAddApp(shouldAdd: Boolean) {
        appSourcesPreferenceGroup?.removePreferenceRecursively(ADD_AN_APP_PREFERENCE_KEY)

        if (shouldAdd) {
            appSourcesPreferenceGroup?.addPreference(
                HealthPreference(requireContext()).also {
                    it.icon = AttributeResolver.getDrawable(requireContext(), R.attr.addIcon)
                    it.title = getString(R.string.data_sources_add_app)
                    it.key = ADD_AN_APP_PREFERENCE_KEY
                    it.order = 100 // Arbitrary number to ensure the button is added at the end of the priority list
                    it.setOnPreferenceClickListener {
                        findNavController().navigate(
                            R.id.action_dataSourcesFragment_to_addAnAppFragment,
                            bundleOf(CATEGORY_KEY to currentCategorySelection))
                        true
                    }
                }
            )
        }
    }

    private fun updateCards(cardInfos: List<AggregationCardInfo>) {
        dataTotalsPreferenceGroup?.removePreferenceRecursively(DATA_TOTALS_PREFERENCE_KEY)

        // Temporary condition while priority list fetched from old API
        // to avoid showing data cards when there are no apps on the priority list
        if (appSourcesPreferenceGroup?.isVisible == false) {
            return
        }

        if (cardInfos.isEmpty() || currentCategorySelection == HealthDataCategory.SLEEP) {
            dataTotalsPreferenceGroup?.isVisible = false
        } else {
            dataTotalsPreferenceGroup?.isVisible = true
            dataTotalsPreferenceGroup?.addPreference(
                CardContainerPreference(requireContext(), timeSource)
                    .also {
                        it.setAggregationCardInfo(cardInfos)
                        it.key = DATA_TOTALS_PREFERENCE_KEY
                    }
            )
        }
    }

    private fun addEmptyState() {
        removeNonEmptyState()
        removeEmptyState()

        preferenceScreen.addPreference(getEmptyStateHeaderPreference())
        preferenceScreen.addPreference(getEmptyStateFooterPreference())
    }

    private fun removeEmptyState() {
        preferenceScreen.removePreferenceRecursively(EMPTY_STATE_HEADER_PREFERENCE_KEY)
        preferenceScreen.removePreferenceRecursively(EMPTY_STATE_FOOTER_PREFERENCE_KEY)
    }

    private fun removeNonEmptyState() {
        preferenceScreen.removePreferenceRecursively(APP_SOURCES_PREFERENCE_KEY)
        preferenceScreen.removePreferenceRecursively(ADD_AN_APP_PREFERENCE_KEY)
        preferenceScreen.removePreferenceRecursively(DATA_TOTALS_PREFERENCE_KEY)

        // Need to keep the preference group headers and footer
        appSourcesPreferenceGroup?.isVisible = false
        dataTotalsPreferenceGroup?.isVisible = false
        nonEmptyFooterPreference?.isVisible = false
    }

    private fun getEmptyStateHeaderPreference(): HeaderPreference {
        return HeaderPreference(requireContext()).also {
            it.setHeaderText(
                    getString(R.string.data_sources_empty_state))
            it.key = EMPTY_STATE_HEADER_PREFERENCE_KEY
        }
    }

    private fun getEmptyStateFooterPreference(): FooterPreference {
        return FooterPreference(context)
            .also {
                it.title = getString(R.string.data_sources_empty_state_footer,
                    getString(currentCategorySelection.lowercaseTitle()))
                it.setLearnMoreText(getString(R.string.data_sources_help_link))
                it.setLearnMoreAction { DeviceInfoUtilsImpl().openHCGetStartedLink(requireActivity())}
                it.key = EMPTY_STATE_FOOTER_PREFERENCE_KEY
            }
    }

    private fun setupSpinnerPreference() {
        spinnerPreference = SettingsSpinnerPreference(context)
        spinnerPreference.setAdapter(
            SettingsSpinnerAdapter<String>(context).also {
                it.addAll(dataSourcesCategoriesStrings)
            })

        spinnerPreference.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val currentCategory = dataSourcesCategories[position]
                currentCategorySelection = dataSourcesCategories[position]

                healthPermissionsViewModel.loadData(currentCategory)
                dataSourcesViewModel.loadPotentialAppSources(currentCategory)
                dataSourcesViewModel.setCurrentSelection(currentCategory)
                if (currentCategory == HealthDataCategory.ACTIVITY) {
                    dataTotalsPreferenceGroup?.isVisible = true
                    dataSourcesViewModel.loadMostRecentAggregations()
                } else {
                    dataTotalsPreferenceGroup?.isVisible = false
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        })

        spinnerPreference.setSelection(
            dataSourcesCategories.indexOf(
                dataSourcesViewModel.getCurrentSelection()))

        preferenceScreen.addPreference(spinnerPreference)
    }
}