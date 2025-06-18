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

package com.android.healthconnect.controller.data.prettyfhir

import android.health.connect.MedicalResourceId
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
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
import com.android.healthconnect.controller.data.entries.MarginItemDecoration
import com.android.healthconnect.controller.data.entrydetails.ItemDataEntrySeparatorViewBinder
import com.android.healthconnect.controller.data.rawfhir.RawFhirFragment
import com.android.healthconnect.controller.data.rawfhir.RawFhirViewModel
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewAdapter
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(Fragment::class)
class PrettyFhirFragment : Hilt_PrettyFhirFragment() {

    @Inject lateinit var logger: HealthConnectLogger
    private val viewModel: RawFhirViewModel by viewModels()
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
            return bundleOf(
                MEDICAL_RESOURCE_ID_KEY to medicalResourceId,
                HEADER_KEY to header,
                HEADER_A11Y_KEY to headerA11y,
                TITLE_KEY to title,
                TITLE_A11Y_KEY to titleA11y,
            )
        }
    }

    // TODO: Add telemetry b/424459745
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
                ?: throw IllegalArgumentException("HEADER_KEY can't be null!")
        headerA11y =
            requireArguments().getString(HEADER_A11Y_KEY)
                ?: throw IllegalArgumentException("HEADER_A11Y_KEY can't be null!")
        title =
            requireArguments().getString(TITLE_KEY)
                ?: throw IllegalArgumentException("TITLE_KEY can't be null!")
        titleA11y =
            requireArguments().getString(TITLE_A11Y_KEY)
                ?: throw IllegalArgumentException("TITLE_A11Y_KEY can't be null!")
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
            .navigate(
                R.id.action_prettyFhirFragment_to_rawFhirFragment,
                bundleOf(RawFhirFragment.MEDICAL_RESOURCE_ID_KEY to medicalResourceId),
            )
    }

    private fun updateUI(state: RawFhirViewModel.PrettyFhirState) {
        when (state) {
            is RawFhirViewModel.PrettyFhirState.Loading -> {
                loadingView.isVisible = true
                errorView.isVisible = false
                recyclerView.isVisible = false
            }
            is RawFhirViewModel.PrettyFhirState.Error -> {
                errorView.isVisible = true
                loadingView.isVisible = false
                recyclerView.isVisible = false
            }
            is RawFhirViewModel.PrettyFhirState.WithData -> {
                recyclerView.isVisible = true
                detailsAdapter.updateData(state.prettyFhirResources)
                errorView.isVisible = false
                loadingView.isVisible = false
            }
        }
    }
}
