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
package com.android.healthconnect.controller.data.entries

import android.health.connect.MedicalResourceId
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.PERMISSION_TYPE_NAME_KEY
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesDeletionScreenState.DELETE
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesDeletionScreenState.VIEW
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.Empty
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.Loading
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.LoadingFailed
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.With
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationView
import com.android.healthconnect.controller.data.entries.views.AggregationViewBinder
import com.android.healthconnect.controller.data.entries.views.EntryItemViewBinder
import com.android.healthconnect.controller.data.entries.views.ExerciseSessionItemViewBinder
import com.android.healthconnect.controller.data.entries.views.MarginItemDecoration
import com.android.healthconnect.controller.data.entries.views.MedicalEntryItemViewBinder
import com.android.healthconnect.controller.data.entries.views.PlannedExerciseSessionItemViewBinder
import com.android.healthconnect.controller.data.entries.views.SectionTitleViewBinder
import com.android.healthconnect.controller.data.entries.views.SelectAllViewBinder
import com.android.healthconnect.controller.data.entries.views.SeriesDataItemViewBinder
import com.android.healthconnect.controller.data.entries.views.SleepSessionItemViewBinder
import com.android.healthconnect.controller.data.entries.views.SymptomItemViewBinder
import com.android.healthconnect.controller.data.entrydetails.DataEntryDetailsFragment
import com.android.healthconnect.controller.data.fhir.pretty.PrettyFhirFragment
import com.android.healthconnect.controller.data.fhir.raw.RawFhirFragment.Companion.MEDICAL_RESOURCE_ID_KEY
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.fromPermissionTypeName
import com.android.healthconnect.controller.permissions.data.isSymptom
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.shared.DataType
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.ToolbarElement
import com.android.healthconnect.controller.utils.navigateSafe
import com.android.healthconnect.controller.utils.setTitle
import com.android.healthconnect.controller.utils.setupMenu
import com.android.healthconnect.controller.utils.setupSharedMenu
import com.android.healthconnect.controller.utils.toInstant
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject

/** Fragment to show health data entries. */
@AndroidEntryPoint(Fragment::class)
class AllEntriesFragment : Hilt_AllEntriesFragment() {

    companion object {
        private const val DELETION_TAG = "DeletionTag"
        private const val START_DELETION_ENTRIES_AND_ACCESS_KEY =
            "START_DELETION_ENTRIES_AND_ACCESS_KEY"
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var timeSource: TimeSource

    private lateinit var permissionType: HealthPermissionType
    private val entriesViewModel: EntriesViewModel by
        viewModels(ownerProducer = { requireParentFragment() })
    private val deletionViewModel: DeletionViewModel by activityViewModels()
    private lateinit var dateNavigationView: DateNavigationView
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var noDataView: View
    private lateinit var loadingView: View
    private lateinit var errorView: View
    private lateinit var adapter: EntriesAdapter
    private var aggregation: FormattedEntry.FormattedAggregation? = null

    // VIEW state click listener
    private val onClickEntryListener by lazy {
        object : OnClickEntryListener {
            override fun onItemClicked(id: String, index: Int) {
                findNavController()
                    .navigateSafe(
                        R.id.entriesAndAccessFragment,
                        R.id.action_entriesAndAccessFragment_to_dataEntryDetailsFragment,
                        DataEntryDetailsFragment.createBundle(
                            permissionType as FitnessPermissionType,
                            id,
                            showDataOrigin = true,
                        ),
                    )
            }
        }
    }

    // VIEW state click listener
    private val onClickMedicalEntryListener by lazy {
        object : OnClickMedicalEntryListener {

            override fun onItemClicked(id: MedicalResourceId, index: Int) {
                findNavController()
                    .navigateSafe(
                        R.id.entriesAndAccessFragment,
                        R.id.action_entriesAndAccessFragment_to_rawFhirFragment,
                        bundleOf(MEDICAL_RESOURCE_ID_KEY to id),
                    )
            }

            override fun onItemClicked(
                dataEntry: FormattedEntry.FormattedMedicalDataEntry,
                index: Int,
            ) {
                findNavController()
                    .navigateSafe(
                        R.id.entriesAndAccessFragment,
                        R.id.action_entriesAndAccessFragment_to_prettyFhirFragment,
                        PrettyFhirFragment.createBundle(
                            header = dataEntry.header,
                            headerA11y = dataEntry.headerA11y,
                            title = dataEntry.title,
                            titleA11y = dataEntry.titleA11y,
                            medicalResourceId = dataEntry.medicalResourceId,
                        ),
                    )
            }
        }
    }
    // DELETE state click listener
    private val onSelectEntryListener by lazy {
        object : OnSelectEntryListener {
            override fun onSelectEntry(
                id: String,
                dataType: DataType,
                index: Int,
                startTime: Instant?,
                endTime: Instant?,
            ) {
                if (id in entriesViewModel.mapOfEntriesToBeDeleted.value.orEmpty()) {
                    entriesViewModel.removeFromDeleteMap(id)
                } else {
                    entriesViewModel.addToDeleteMap(id, dataType)
                }
                updateMenu(screenState = DELETE)
            }
        }
    }

    // DELETE state select all
    private val onClickSelectAllListener by lazy {
        object : OnClickSelectAllListener {
            override fun onClicked(isChecked: Boolean) {
                entriesViewModel.setAllEntriesSelectedValue(isChecked)
                entriesViewModel.getEntriesList().forEach { entry ->
                    if (entry is FormattedEntry.HasDataType) {
                        if (isChecked) {
                            entriesViewModel.addToDeleteMap(entry.uuid, entry.dataType)
                        } else {
                            entriesViewModel.removeFromDeleteMap(entry.uuid)
                        }
                    }
                }
                updateMenu(screenState = DELETE)
            }
        }
    }

    private val aggregationViewBinder by lazy { AggregationViewBinder() }
    private val entryViewBinder by lazy {
        EntryItemViewBinder(onSelectEntryListener = onSelectEntryListener)
    }
    private val medicalEntryViewBinder by lazy {
        MedicalEntryItemViewBinder(onClickMedicalEntryListener = onClickMedicalEntryListener)
    }
    private val sectionTitleViewBinder by lazy { SectionTitleViewBinder() }
    private val sleepSessionViewBinder by lazy {
        SleepSessionItemViewBinder(
            onItemClickedListener = onClickEntryListener,
            onSelectEntryListener = onSelectEntryListener,
        )
    }
    private val exerciseSessionItemViewBinder by lazy {
        ExerciseSessionItemViewBinder(
            onItemClickedListener = onClickEntryListener,
            onSelectEntryListener = onSelectEntryListener,
        )
    }
    private val symptomItemViewBinder by lazy {
        SymptomItemViewBinder(onSelectEntryListener = onSelectEntryListener)
    }
    private val seriesDataItemViewBinder by lazy {
        SeriesDataItemViewBinder(
            onItemClickedListener = onClickEntryListener,
            onSelectEntryListener = onSelectEntryListener,
        )
    }
    private val plannedExerciseSessionItemViewBinder by lazy {
        PlannedExerciseSessionItemViewBinder(
            onSelectEntryListener = onSelectEntryListener,
            onItemClickedListener = onClickEntryListener,
        )
    }

    private val selectAllViewBinder by lazy { SelectAllViewBinder(onClickSelectAllListener) }

    // Not in deletion state
    private val onMenuSetup: (MenuItem) -> Boolean = { menuItem ->
        when (menuItem.itemId) {
            R.id.menu_enter_deletion_state -> {
                // enter deletion state
                logger.logInteraction(ToolbarElement.TOOLBAR_ENTER_DELETION_STATE_BUTTON)
                triggerDeletionState(DELETE)
                true
            }
            R.id.menu_open_units -> {
                logger.logInteraction(ToolbarElement.TOOLBAR_UNITS_BUTTON)
                findNavController()
                    .navigateSafe(
                        R.id.entriesAndAccessFragment,
                        R.id.action_entriesAndAccess_to_setUnitsFragment,
                    )
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
                // exit deletion state
                logger.logInteraction(ToolbarElement.TOOLBAR_EXIT_DELETION_STATE_BUTTON)
                triggerDeletionState(VIEW)
                true
            }
            else -> false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entries, container, false)
        val permissionTypeName =
            arguments?.getString(PERMISSION_TYPE_NAME_KEY)
                ?: throw IllegalArgumentException("PERMISSION_TYPE_NAME_KEY can't be null!")
        permissionType = fromPermissionTypeName(permissionTypeName)
        val isSymptomType = permissionType.isSymptom()
        if (isSymptomType) {
            setTitle(R.string.all_symptoms_uppercase_label)
        } else {
            setTitle(permissionType.upperCaseLabel())
        }
        logger.logImpression(ToolbarElement.TOOLBAR_SETTINGS_BUTTON)

        dateNavigationView = view.findViewById(R.id.date_navigation_view)
        setDateNavigationViewMaxDate()
        if (permissionType is MedicalPermissionType || isSymptomType) {
            dateNavigationView.isVisible = true
        } else {
            dateNavigationView.isVisible = false
        }
        val isExpressiveThemeEnabled = SettingsThemeHelper.isExpressiveTheme(requireContext())
        noDataView =
            if (isExpressiveThemeEnabled) {
                view.findViewById(R.id.zerostate_view)
            } else {
                view.findViewById(R.id.no_data_view)
            }
        errorView = view.findViewById(R.id.error_view)
        loadingView = view.findViewById(R.id.loading)
        adapter =
            if (isExpressiveThemeEnabled) {
                getExpressiveEntriesAdapter()
            } else {
                getEntriesAdapter()
            }
        entriesRecyclerView =
            view.findViewById<RecyclerView?>(R.id.data_entries_list).also {
                it.adapter = adapter
                it.layoutManager = LinearLayoutManager(context, VERTICAL, false)
                if (isExpressiveThemeEnabled) {
                    it.addItemDecoration(MarginItemDecoration)
                }
            }

        return view
    }

    private fun getEntriesAdapter(): EntriesAdapter {
        return EntriesAdapter.Builder()
            .setViewBinder(FormattedEntry.SelectAllHeader::class.java, selectAllViewBinder)
            .setViewBinder(FormattedEntry.FormattedDataEntry::class.java, entryViewBinder)
            .setViewBinder(
                FormattedEntry.FormattedMedicalDataEntry::class.java,
                medicalEntryViewBinder,
            )
            .setViewBinder(FormattedEntry.SleepSessionEntry::class.java, sleepSessionViewBinder)
            .setViewBinder(
                FormattedEntry.ExerciseSessionEntry::class.java,
                exerciseSessionItemViewBinder,
            )
            .setViewBinder(FormattedEntry.SymptomEntry::class.java, symptomItemViewBinder)
            .setViewBinder(FormattedEntry.SeriesDataEntry::class.java, seriesDataItemViewBinder)
            .setViewBinder(FormattedEntry.FormattedAggregation::class.java, aggregationViewBinder)
            .setViewBinder(
                FormattedEntry.EntryDateSectionHeader::class.java,
                sectionTitleViewBinder,
            )
            .setViewBinder(
                FormattedEntry.PlannedExerciseSessionEntry::class.java,
                plannedExerciseSessionItemViewBinder,
            )
            .setViewModel(entriesViewModel)
            .build()
    }

    private fun getExpressiveEntriesAdapter(): ExpressiveEntriesAdapter {
        return ExpressiveEntriesAdapter.Builder()
            .setViewBinder(FormattedEntry.SelectAllHeader::class.java, selectAllViewBinder)
            .setViewBinder(FormattedEntry.FormattedDataEntry::class.java, entryViewBinder)
            .setViewBinder(
                FormattedEntry.FormattedMedicalDataEntry::class.java,
                medicalEntryViewBinder,
            )
            .setViewBinder(FormattedEntry.SleepSessionEntry::class.java, sleepSessionViewBinder)
            .setViewBinder(
                FormattedEntry.ExerciseSessionEntry::class.java,
                exerciseSessionItemViewBinder,
            )
            .setViewBinder(FormattedEntry.SymptomEntry::class.java, symptomItemViewBinder)
            .setViewBinder(FormattedEntry.SeriesDataEntry::class.java, seriesDataItemViewBinder)
            .setViewBinder(FormattedEntry.FormattedAggregation::class.java, aggregationViewBinder)
            .setViewBinder(
                FormattedEntry.EntryDateSectionHeader::class.java,
                sectionTitleViewBinder,
            )
            .setViewBinder(
                FormattedEntry.PlannedExerciseSessionEntry::class.java,
                plannedExerciseSessionItemViewBinder,
            )
            .setViewModel(entriesViewModel)
            .build(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deletionViewModel.entriesReloadNeeded.observe(viewLifecycleOwner) { isReloadNeeded ->
            if (isReloadNeeded) {
                entriesViewModel.setScreenState(VIEW)
                reloadEntries()
                deletionViewModel.resetEntriesReloadNeeded()
            }
        }

        entriesViewModel.latestDate.observe(viewLifecycleOwner) { latestDate ->
            dateNavigationView.setDate(latestDate)
        }

        entriesViewModel.allEntriesSelected.observe(viewLifecycleOwner) { allEntriesSelected ->
            adapter.checkSelectAll(allEntriesSelected)
        }

        observeEntriesUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (permissionType.isSymptom()) {
            setTitle(R.string.all_symptoms_uppercase_label)
        } else {
            setTitle(permissionType.upperCaseLabel())
        }
        if (entriesViewModel.shouldReloadEntries) {
            reloadEntries()
            entriesViewModel.shouldReloadEntries = false
        }
        setLoggerPageId()
        logger.logPageImpression()
        dateNavigationView.setDateChangedListener(
            object : DateNavigationView.OnDateChangedListener {
                override fun onDateChanged(
                    displayedStartDate: Instant,
                    period: DateNavigationPeriod,
                ) {
                    entriesViewModel.loadEntries(displayedStartDate, period, permissionType)
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        dateNavigationView.setDateChangedListener(null)
    }

    private fun reloadEntries() {
        if (
            entriesViewModel.currentSelectedDate.value != null &&
                entriesViewModel.period.value != null
        ) {
            val date = entriesViewModel.currentSelectedDate.value!!
            val selectedPeriod = entriesViewModel.period.value!!
            dateNavigationView.setDate(date)
            dateNavigationView.setPeriod(selectedPeriod)
            entriesViewModel.loadEntries(date, selectedPeriod, permissionType)
        } else {
            entriesViewModel.loadLatestRecordDate(
                timeSource.currentTimeMillis().toInstant(),
                permissionType,
            )

            entriesViewModel.loadEntries(
                dateNavigationView.getDate(),
                dateNavigationView.getPeriod(),
                permissionType,
            )
        }
    }

    private fun setLoggerPageId() {
        when (permissionType) {
            is FitnessPermissionType -> {
                logger.setPageId(PageName.TAB_ENTRIES_PAGE)
            }
            is MedicalPermissionType -> {
                logger.setPageId(PageName.TAB_MEDICAL_ENTRIES_PAGE)
            }
            else -> {
                logger.setPageId(PageName.UNKNOWN_PAGE)
            }
        }
    }

    private fun updateMenu(
        screenState: EntriesViewModel.EntriesDeletionScreenState,
        hasData: Boolean = true,
    ) {
        if (permissionType is MedicalPermissionType || !hasData) {
            setupSharedMenu(viewLifecycleOwner, logger)
            return
        }

        if (screenState == VIEW) {
            logger.logImpression(ToolbarElement.TOOLBAR_ENTER_DELETION_STATE_BUTTON)
            setupMenu(R.menu.all_entries_menu, viewLifecycleOwner, logger, onMenuSetup)
            return
        }

        if (entriesViewModel.mapOfEntriesToBeDeleted.value.orEmpty().isEmpty()) {
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
    fun triggerDeletionState(screenState: EntriesViewModel.EntriesDeletionScreenState) {
        updateMenu(screenState)
        if (screenState == VIEW) {
            adapter.removeSelectAll()
            aggregation?.let { adapter.insertAggregation(it) }
        } else {
            aggregation?.let { adapter.removeAggregation() }
            adapter.insertSelectAll(FormattedEntry.SelectAllHeader())
        }
        adapter.showCheckBox(screenState == DELETE)
        entriesViewModel.setScreenState(screenState)
        if (entriesViewModel.getDateNavigationText() == null) {
            dateNavigationView.getDateNavigationText()?.let {
                entriesViewModel.setDateNavigationText(it)
            }
        }
        entriesViewModel.getDateNavigationText()?.let { dateSpinnerText ->
            dateNavigationView.setActive(isEnabled = screenState == VIEW)
        }
    }

    @VisibleForTesting
    fun deleteData() {
        deletionViewModel.setDeletionType(
            DeletionType.DeleteEntries(
                entriesViewModel.mapOfEntriesToBeDeleted.value.orEmpty().toMap(),
                entriesViewModel.getNumOfEntries(),
                dateNavigationView.getPeriod(),
                entriesViewModel.currentSelectedDate.value!!,
            )
        )
        parentFragmentManager.setFragmentResult(START_DELETION_ENTRIES_AND_ACCESS_KEY, bundleOf())
    }

    private fun setDateNavigationViewMaxDate() {
        if (permissionType == FitnessPermissionType.PLANNED_EXERCISE) {
            dateNavigationView.setMaxDate(null)
        } else {
            dateNavigationView.setMaxDate(timeSource.currentTimeMillis().toInstant())
        }
    }

    private fun observeEntriesUpdates() {
        entriesViewModel.entries.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Loading -> {
                    loadingView.isVisible = true
                    noDataView.isVisible = false
                    errorView.isVisible = false
                    entriesRecyclerView.isVisible = false
                    dateNavigationView.isVisible = false
                }
                is Empty -> {
                    noDataView.isVisible = true
                    loadingView.isVisible = false
                    errorView.isVisible = false
                    entriesRecyclerView.isVisible = false
                    dateNavigationView.isVisible = getNavigationVisibility()
                    updateMenu(screenState = VIEW, hasData = false)
                    entriesViewModel.getDateNavigationText()?.let { dateSpinnerText ->
                        dateNavigationView.setActive(isEnabled = true)
                    }
                }
                is With -> {
                    entriesRecyclerView.isVisible = true
                    adapter.updateData(state.entries)
                    // Save aggregation for re-adding to the entries list
                    // when exiting deletion without having deleted any entries
                    aggregation =
                        if (
                            state.entries.isNotEmpty() &&
                                state.entries[0] is FormattedEntry.FormattedAggregation
                        ) {
                            state.entries[0] as FormattedEntry.FormattedAggregation
                        } else {
                            null
                        }
                    entriesRecyclerView.scrollToPosition(0)
                    errorView.isVisible = false
                    noDataView.isVisible = false
                    loadingView.isVisible = false
                    dateNavigationView.isVisible = getNavigationVisibility()
                    entriesViewModel.screenState.value?.let {
                        triggerDeletionState(screenState = it)
                    }
                }
                is LoadingFailed -> {
                    errorView.isVisible = true
                    loadingView.isVisible = false
                    noDataView.isVisible = false
                    entriesRecyclerView.isVisible = false
                    dateNavigationView.isVisible = getNavigationVisibility()
                }
            }
        }
    }

    private fun getNavigationVisibility(): Boolean {
        return permissionType is FitnessPermissionType
    }
}
