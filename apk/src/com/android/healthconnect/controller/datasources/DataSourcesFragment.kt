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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.AggregationCardsState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PotentialAppSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PriorityListState
import com.android.healthconnect.controller.datasources.appsources.AppSourcesPreferenceCategory
import com.android.healthconnect.controller.navigation.CATEGORY_KEY
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.lowercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppUtils
import com.android.healthconnect.controller.shared.preference.CardContainerPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.buttonPreference
import com.android.healthconnect.controller.shared.preference.topIntroPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtilsImpl
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.logging.DataSourcesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.navigateSafe
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsSpinnerAdapter
import com.android.settingslib.widget.SettingsSpinnerPreference
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.ValuePreference
import com.android.settingslib.widget.ZeroStatePreference
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint(HealthPreferenceFragment::class)
class DataSourcesFragment : Hilt_DataSourcesFragment() {

    companion object {
        private const val DATA_TYPE_SPINNER_PREFERENCE_GROUP = "data_type_spinner_group"
        private const val DATA_TOTALS_PREFERENCE_GROUP = "data_totals_group"
        private const val DATA_TOTALS_PREFERENCE_LEGACY_KEY = "data_totals_preference_legacy"
        private const val DATA_TOTALS_PREFERENCE_ONE_KEY = "data_totals_preference_one"
        private const val DATA_TOTALS_PREFERENCE_TWO_KEY = "data_totals_preference_two"
        private const val APP_SOURCES_CATEGORY_KEY = "app_sources_category"
        private const val ZERO_STATE_PREFERENCE_KEY = "zero_state"
        private const val ADD_AN_APP_PREFERENCE_KEY = "add_an_app"
        private const val NON_EMPTY_FOOTER_PREFERENCE_KEY = "data_sources_footer"
        private const val EMPTY_STATE_HEADER_PREFERENCE_KEY = "empty_state_header"
        private const val EMPTY_STATE_FOOTER_PREFERENCE_KEY = "empty_state_footer"

        private val dataSourcesCategories =
            arrayListOf(HealthDataCategory.ACTIVITY, HealthDataCategory.SLEEP)
    }

    init {
        this.setPageName(PageName.DATA_SOURCES_PAGE)
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var appUtils: AppUtils
    @Inject lateinit var timeSource: TimeSource

    private val dataSourcesViewModel: DataSourcesViewModel by activityViewModels()
    private lateinit var spinnerPreference: SettingsSpinnerPreference
    private lateinit var dataSourcesCategoriesStrings: List<String>
    private var currentCategorySelection: @HealthDataCategoryInt Int = HealthDataCategory.ACTIVITY

    private val dataTypeSpinnerPreferenceGroup: PreferenceGroup by
        pref(DATA_TYPE_SPINNER_PREFERENCE_GROUP)

    private val dataTotalsPreferenceGroup: PreferenceGroup by pref(DATA_TOTALS_PREFERENCE_GROUP)

    private val zeroStatePreference: ZeroStatePreference by pref(ZERO_STATE_PREFERENCE_KEY)

    private val appSourcesCategory: AppSourcesPreferenceCategory by pref(APP_SOURCES_CATEGORY_KEY)

    private val nonEmptyFooterPreference: FooterPreference by pref(NON_EMPTY_FOOTER_PREFERENCE_KEY)

    private var cardContainerPreference: CardContainerPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.data_sources_and_priority_screen, rootKey)
        dataSourcesCategoriesStrings =
            dataSourcesCategories.map { category -> getString(category.uppercaseTitle()) }

        setupSpinnerPreference()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setLoading(true)
        val currentStringSelection = spinnerPreference.selectedItem
        currentCategorySelection =
            dataSourcesCategories[dataSourcesCategoriesStrings.indexOf(currentStringSelection)]
        dataSourcesViewModel.loadData(currentCategorySelection)

        dataSourcesViewModel.dataSourcesAndAggregationsInfo.observe(viewLifecycleOwner) {
            dataSourcesInfo ->
            if (dataSourcesInfo.isLoading()) {
                setLoading(true)
            } else if (dataSourcesInfo.isLoadingFailed()) {
                setLoading(false)
                setError(true)
            } else if (dataSourcesInfo.isWithData()) {
                setLoading(false)

                val priorityList =
                    (dataSourcesInfo.priorityListState as PriorityListState.WithData).priorityList
                val potentialAppSources =
                    (dataSourcesInfo.potentialAppSourcesState as PotentialAppSourcesState.WithData)
                        .appSources
                val cardInfos =
                    (dataSourcesInfo.aggregationCardsState as AggregationCardsState.WithData)
                        .dataTotals

                if (priorityList.isEmpty() && potentialAppSources.isEmpty()) {
                    addEmptyState()
                } else {
                    updateAppSourcesSection(potentialAppSources)
                    updateDataTotalsSection(cardInfos)
                }
            }
        }

        dataSourcesViewModel.updatedAggregationCardsData.observe(viewLifecycleOwner) {
            aggregationCardsData ->
            when (aggregationCardsData) {
                is AggregationCardsState.Loading -> {
                    updateAggregations(listOf(), true)
                }
                is AggregationCardsState.LoadingFailed -> {
                    updateDataTotalsSection(listOf())
                }
                is AggregationCardsState.WithData -> {
                    updateAggregations(aggregationCardsData.dataTotals, false)
                }
            }
        }

        dataSourcesViewModel.shouldShowAddAnAppButton.observe(viewLifecycleOwner) {
            showAddAnAppButton ->
            if (showAddAnAppButton) {
                updateAddApp(true)
            }
        }

        // Prevents incorrect item indexing in the priority list item content descriptions.
        val recyclerView = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)
        recyclerView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onResume() {
        super.onResume()
        dataSourcesViewModel.loadData(currentCategorySelection)
    }

    /** Updates the priority list preference. */
    private fun updateAppSourcesSection(potentialAppSources: List<AppMetadata>) {
        removeEmptyState()
        appSourcesCategory.initialize(
            logger,
            appUtils,
            dataSourcesViewModel,
            currentCategorySelection,
        )
        appSourcesCategory.updateApps()
        appSourcesCategory.also {
            it.isVisible = true
            it.order = 4
        }

        updateAddApp(potentialAppSources.isNotEmpty())
        nonEmptyFooterPreference.isVisible = true
    }

    /**
     * Shows the "Add an app" button when there is at least one potential app for the priority list.
     *
     * <p> Hides the button when there are no other potential apps for the priority list.
     */
    private fun updateAddApp(shouldShow: Boolean) {
        val button = preferenceScreen.findPreference<Preference>(ADD_AN_APP_PREFERENCE_KEY)
        val currentVisibility = button?.isVisible ?: false
        if (currentVisibility == shouldShow) {
            return
        }

        preferenceScreen.removePreferenceRecursively(ADD_AN_APP_PREFERENCE_KEY)

        if (!shouldShow) {
            return
        }

        preferenceScreen.addPreference(
            buttonPreference(
                context = requireContext(),
                icon = AttributeResolver.getDrawable(requireContext(), R.attr.addIcon),
                title = getString(R.string.data_sources_add_app),
                logName = DataSourcesElement.ADD_AN_APP_BUTTON,
                key = ADD_AN_APP_PREFERENCE_KEY,
                order = 5,
                listener = {
                    findNavController()
                        .navigateSafe(
                            R.id.dataSourcesFragment,
                            R.id.action_dataSourcesFragment_to_addAnAppFragment,
                            Bundle().apply { putInt(CATEGORY_KEY, currentCategorySelection) },
                        )
                    true
                },
            )
        )
    }

    /** Populates the data totals section with aggregation cards if needed. */
    private fun updateDataTotalsSection(cardInfos: List<AggregationCardInfo>) {
        dataTotalsPreferenceGroup.removePreferenceRecursively(DATA_TOTALS_PREFERENCE_LEGACY_KEY)
        dataTotalsPreferenceGroup.removePreferenceRecursively(DATA_TOTALS_PREFERENCE_ONE_KEY)
        dataTotalsPreferenceGroup.removePreferenceRecursively(DATA_TOTALS_PREFERENCE_TWO_KEY)
        // Do not show data cards when there are no apps on the priority list
        if (!appSourcesCategory.isVisible) {
            dataTotalsPreferenceGroup.isVisible = false
        }

        if (cardInfos.isEmpty()) {
            dataTotalsPreferenceGroup.isVisible = false
        } else {
            dataTotalsPreferenceGroup.isVisible = true
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                addValuePreferences(cardInfos)
            } else {
                addLegacyCardContainer(cardInfos)
            }
        }
    }

    private fun addValuePreferences(cardInfos: List<AggregationCardInfo>) {
        if (cardInfos.isEmpty()) {
            return
        }
        logger.logImpression(DataSourcesElement.DATA_TOTALS_CARD)
        addValuePreference(cardInfos.getOrNull(0), DATA_TOTALS_PREFERENCE_ONE_KEY)
        addValuePreference(cardInfos.getOrNull(1), DATA_TOTALS_PREFERENCE_TWO_KEY)
    }

    private fun addValuePreference(cardInfo: AggregationCardInfo?, key: String) {
        if (cardInfo == null) {
            return
        }
        dataTotalsPreferenceGroup.addPreference(
            ValuePreference(requireContext()).also {
                it.key = key
                it.title = cardInfo.aggregation.aggregation
                it.firstContentDescription = getAggregationA11yContentDescription(cardInfo)
                it.summary = formatDateText(cardInfo.startDate, cardInfo.endDate)
                it.isSelectable = false
            }
        )
    }

    private fun addLegacyCardContainer(cardInfos: List<AggregationCardInfo>) {
        cardContainerPreference =
            CardContainerPreference(requireContext(), timeSource).also {
                it.setAggregationCardInfo(cardInfos)
                it.key = DATA_TOTALS_PREFERENCE_LEGACY_KEY
            }
        dataTotalsPreferenceGroup.addPreference(
            (cardContainerPreference as CardContainerPreference)
        )
    }

    /** Updates the aggregation cards after a priority list change. */
    private fun updateAggregations(cardInfos: List<AggregationCardInfo>, isLoading: Boolean) {
        if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            updateValuePreferenceAggregations(isLoading, cardInfos)
        } else {
            updateLegacyAggregations(isLoading, cardInfos)
        }
    }

    private fun updateValuePreferenceAggregations(
        isLoading: Boolean,
        cardInfos: List<AggregationCardInfo>,
    ) {
        if (isLoading) {
            updateValuePreferenceToLoading(DATA_TOTALS_PREFERENCE_ONE_KEY)
            updateValuePreferenceToLoading(DATA_TOTALS_PREFERENCE_TWO_KEY)
        } else {
            if (cardInfos.isEmpty()) {
                dataTotalsPreferenceGroup.isVisible = false
            } else {
                dataTotalsPreferenceGroup.isVisible = true
                updateValuePreference(DATA_TOTALS_PREFERENCE_ONE_KEY, cardInfos.getOrNull(0))
                updateValuePreference(DATA_TOTALS_PREFERENCE_TWO_KEY, cardInfos.getOrNull(1))
            }
        }
    }

    private fun updateValuePreference(key: String, cardInfo: AggregationCardInfo?) {
        val preference = dataTotalsPreferenceGroup.findPreference<ValuePreference>(key)
        preference?.also {
            if (cardInfo == null) {
                it.isVisible = false
            } else {
                it.isVisible = true
                it.title = cardInfo.aggregation.aggregation
                it.firstContentDescription = getAggregationA11yContentDescription(cardInfo)
                it.summary = formatDateText(cardInfo.startDate, cardInfo.endDate)
                it.isSelectable = false
            }
        }
    }

    private fun getAggregationA11yContentDescription(cardInfo: AggregationCardInfo): String {
        return "${cardInfo.aggregation.aggregationA11y}, ${formatDateText(cardInfo.startDate, cardInfo.endDate)}"
    }

    private fun updateValuePreferenceToLoading(key: String) {
        val preference = dataTotalsPreferenceGroup.findPreference<ValuePreference>(key)
        preference?.also {
            it.title = " "
            it.summary = getString(R.string.loading)
        }
    }

    private fun updateLegacyAggregations(isLoading: Boolean, cardInfos: List<AggregationCardInfo>) {
        if (isLoading) {
            cardContainerPreference?.setLoading(true)
        } else {
            if (cardInfos.isEmpty()) {
                dataTotalsPreferenceGroup.isVisible = false
            } else {
                dataTotalsPreferenceGroup.isVisible = true
                cardContainerPreference?.setAggregationCardInfo(cardInfos)
                cardContainerPreference?.setLoading(false)
            }
        }
    }

    /**
     * The empty state of this fragment is represented by:
     * - no apps with write permissions for this category
     * - no apps with data for this category
     */
    private fun addEmptyState() {
        removeNonEmptyState()
        removeEmptyState()

        addEmptyHeader()
        preferenceScreen.addPreference(getEmptyStateFooterPreference())
    }

    private fun addEmptyHeader() {
        if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            zeroStatePreference.isVisible = true
        } else {
            preferenceScreen.addPreference(getEmptyStateHeaderPreference())
        }
    }

    private fun removeEmptyState() {
        zeroStatePreference.isVisible = false
        preferenceScreen.removePreferenceRecursively(EMPTY_STATE_HEADER_PREFERENCE_KEY)
        preferenceScreen.removePreferenceRecursively(EMPTY_STATE_FOOTER_PREFERENCE_KEY)
    }

    private fun removeNonEmptyState() {
        preferenceScreen.removePreferenceRecursively(ADD_AN_APP_PREFERENCE_KEY)
        preferenceScreen.removePreferenceRecursively(DATA_TOTALS_PREFERENCE_LEGACY_KEY)

        // We hide the preference group headers and footer instead of removing them
        appSourcesCategory.isVisible = false
        dataTotalsPreferenceGroup.isVisible = false
        nonEmptyFooterPreference.isVisible = false
    }

    private fun getEmptyStateHeaderPreference(): Preference {
        return topIntroPreference(
            context = requireContext(),
            preferenceTitle = getString(R.string.data_sources_empty_state),
            preferenceKey = EMPTY_STATE_HEADER_PREFERENCE_KEY,
        )
    }

    private fun getEmptyStateFooterPreference(): FooterPreference {
        return FooterPreference(context).also {
            it.title =
                getString(
                    R.string.data_sources_empty_state_footer,
                    getString(currentCategorySelection.lowercaseTitle()),
                )
            it.setLearnMoreText(getString(R.string.data_sources_help_link))
            it.setLearnMoreAction { DeviceInfoUtilsImpl().openHCGetStartedLink(requireActivity()) }
            it.key = EMPTY_STATE_FOOTER_PREFERENCE_KEY
        }
    }

    private fun setupSpinnerPreference() {
        spinnerPreference = SettingsSpinnerPreference(requireContext())
        spinnerPreference.setAdapter(
            SettingsSpinnerAdapter<String>(context).also { it.addAll(dataSourcesCategoriesStrings) }
        )

        spinnerPreference.setOnItemSelectedListener(
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    logger.logInteraction(DataSourcesElement.DATA_TYPE_SPINNER)

                    val currentCategory = dataSourcesCategories[position]
                    currentCategorySelection = dataSourcesCategories[position]

                    // Clear the screen before loading new data
                    removeNonEmptyState()
                    removeEmptyState()

                    // Reload the data sources information when a new category has been selected
                    dataSourcesViewModel.loadData(currentCategory)
                    dataSourcesViewModel.setCurrentSelection(currentCategory)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        )

        spinnerPreference.setSelection(
            dataSourcesCategories.indexOf(dataSourcesViewModel.getCurrentSelection())
        )

        dataTypeSpinnerPreferenceGroup.isVisible = true
        dataTypeSpinnerPreferenceGroup.addPreference(spinnerPreference)
        logger.logImpression(DataSourcesElement.DATA_TYPE_SPINNER)
    }

    private fun formatDateText(startDate: Instant, endDate: Instant?): String {
        val dateFormatter = LocalDateTimeFormatter(requireContext())
        return dateFormatter.formatDate(startDate, endDate, timeSource)
    }
}
