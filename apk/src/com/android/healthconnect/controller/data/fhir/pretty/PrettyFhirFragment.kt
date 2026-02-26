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

package com.android.healthconnect.controller.data.fhir.pretty

import android.health.connect.MedicalResourceId
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.EntriesAdapter
import com.android.healthconnect.controller.data.entries.ExpressiveEntriesAdapter
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedMedicalDataEntry
import com.android.healthconnect.controller.data.entries.views.MarginItemDecoration
import com.android.healthconnect.controller.data.entrydetails.ItemDataEntrySeparatorViewBinder
import com.android.healthconnect.controller.data.fhir.raw.RawFhirFragment
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewAdapter
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.navigateSafe
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(Fragment::class)
class PrettyFhirFragment : Hilt_PrettyFhirFragment() {

    @Inject lateinit var logger: HealthConnectLogger
    private val viewModel: PrettyFhirViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var header: String
    private lateinit var headerA11y: String
    private lateinit var title: String
    private lateinit var titleA11y: String
    private lateinit var medicalResourceId: MedicalResourceId
    private lateinit var loadingView: View
    private lateinit var errorView: View
    private lateinit var medicalDataEntry: FormattedMedicalDataEntry
    private lateinit var detailsAdapter: RecyclerViewAdapter

    private val prettyFhirViewBinder by lazy { PrettyFhirViewBinder() }
    private val itemDataEntrySeparatorViewBinder by lazy { ItemDataEntrySeparatorViewBinder() }
    private val rawFhirViewBinder by lazy { RawFhirFragment.RawFhirViewBinder() }
    private val prettyFhirHeaderViewBinder by lazy {
        PrettyFhirHeaderViewBinder { onClickViewSourceDataListener() }
    }

    companion object {
        const val MEDICAL_RESOURCE_ID_KEY = "entry_id_key"
        const val HEADER_KEY = "header_key"
        const val HEADER_A11Y_KEY = "header_a11y_key"
        const val TITLE_KEY = "title_key"
        const val TITLE_A11Y_KEY = "title_a11y_key"

        fun createBundle(
            medicalResourceId: MedicalResourceId,
            header: String,
            headerA11y: String,
            title: String,
            titleA11y: String,
        ): Bundle {
            return Bundle().apply {
                putParcelable(MEDICAL_RESOURCE_ID_KEY, medicalResourceId)
                putString(HEADER_KEY, header)
                putString(HEADER_A11Y_KEY, headerA11y)
                putString(TITLE_KEY, title)
                putString(TITLE_A11Y_KEY, titleA11y)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPageId()
    }

    override fun onResume() {
        super.onResume()
        setPageId()
        logger.logPageImpression()
    }

    private fun setPageId() {
        logger.setPageId(PageName.PRETTY_FHIR_PAGE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry_details, container, false)
        medicalResourceId =
            requireArguments().getParcelable(MEDICAL_RESOURCE_ID_KEY)
                ?: throw IllegalArgumentException("MEDICAL_RESOURCE_ID_KEY is missing!")
        header =
            requireArguments().getString(HEADER_KEY)
                ?: throw IllegalArgumentException("HEADER_KEY is missing!")
        headerA11y =
            requireArguments().getString(HEADER_A11Y_KEY)
                ?: throw IllegalArgumentException("HEADER_A11Y_KEY is missing!")
        title =
            requireArguments().getString(TITLE_KEY)
                ?: throw IllegalArgumentException("TITLE_KEY is missing!")
        titleA11y =
            requireArguments().getString(TITLE_A11Y_KEY)
                ?: throw IllegalArgumentException("TITLE_A11Y_KEY is missing!")
        errorView = view.findViewById(R.id.error_view)
        loadingView = view.findViewById(R.id.loading)

        medicalDataEntry =
            FormattedMedicalDataEntry(
                header = header,
                headerA11y = headerA11y,
                title = title,
                titleA11y = titleA11y,
                medicalResourceId = medicalResourceId,
            )

        val isExpressiveThemeEnabled = SettingsThemeHelper.isExpressiveTheme(requireContext())
        detailsAdapter =
            if (isExpressiveThemeEnabled) {
                getExpressiveEntriesAdapter()
            } else {
                getEntriesAdapter()
            }
        recyclerView =
            view.findViewById<RecyclerView?>(R.id.data_entries_list).also {
                it.adapter = detailsAdapter
                it.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                if (isExpressiveThemeEnabled) {
                    it.addItemDecoration(MarginItemDecoration)
                }
            }

        viewModel.loadPrettyFhirResource(medicalDataEntry)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.prettyFhir.observe(viewLifecycleOwner) { state -> updateUI(state) }
    }

    private fun getEntriesAdapter(): EntriesAdapter {
        return EntriesAdapter.Builder()
            .setViewBinder(FormattedEntry.FormattedRawFhir::class.java, rawFhirViewBinder)
            .setViewBinder(
                FormattedEntry.ItemDataEntrySeparator::class.java,
                itemDataEntrySeparatorViewBinder,
            )
            .setViewBinder(FormattedEntry.FormattedPrettyFhir::class.java, prettyFhirViewBinder)
            .setViewBinder(
                FormattedEntry.FormattedPrettyFhirDetailsHeader::class.java,
                prettyFhirHeaderViewBinder,
            )
            .setViewModel(viewModel)
            .build()
    }

    private fun getExpressiveEntriesAdapter(): ExpressiveEntriesAdapter {
        return ExpressiveEntriesAdapter.Builder()
            .setViewBinder(FormattedEntry.FormattedRawFhir::class.java, rawFhirViewBinder)
            .setViewBinder(
                FormattedEntry.ItemDataEntrySeparator::class.java,
                itemDataEntrySeparatorViewBinder,
            )
            .setViewBinder(FormattedEntry.FormattedPrettyFhir::class.java, prettyFhirViewBinder)
            .setViewBinder(
                FormattedEntry.FormattedPrettyFhirDetailsHeader::class.java,
                prettyFhirHeaderViewBinder,
            )
            .setViewModel(viewModel)
            .build(requireContext())
    }

    private fun onClickViewSourceDataListener() {
        findNavController()
            .navigateSafe(
                R.id.prettyFhirFragment,
                R.id.action_prettyFhirFragment_to_rawFhirFragment,
                Bundle().apply {
                    putParcelable(RawFhirFragment.MEDICAL_RESOURCE_ID_KEY, medicalResourceId)
                },
            )
    }

    private fun updateUI(state: PrettyFhirViewModel.PrettyFhirState) {
        when (state) {
            is PrettyFhirViewModel.PrettyFhirState.Loading -> {
                loadingView.isVisible = true
                errorView.isVisible = false
                recyclerView.isVisible = false
            }
            is PrettyFhirViewModel.PrettyFhirState.Error -> {
                errorView.isVisible = true
                loadingView.isVisible = false
                recyclerView.isVisible = false
            }
            is PrettyFhirViewModel.PrettyFhirState.WithData -> {
                recyclerView.isVisible = true
                detailsAdapter.updateData(state.prettyFhirResources)
                errorView.isVisible = false
                loadingView.isVisible = false
            }
        }
    }
}
