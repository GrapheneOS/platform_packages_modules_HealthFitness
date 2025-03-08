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
package com.android.healthconnect.controller.data.rawfhir

import android.health.connect.MedicalResourceId
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.rawfhir.RawFhirViewModel.FormattedFhir
import com.android.healthconnect.controller.data.rawfhir.RawFhirViewModel.RawFhirState.Error
import com.android.healthconnect.controller.data.rawfhir.RawFhirViewModel.RawFhirState.Loading
import com.android.healthconnect.controller.data.rawfhir.RawFhirViewModel.RawFhirState.WithData
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewAdapter
import com.android.healthconnect.controller.shared.recyclerview.SimpleViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RawFhirPageElement
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

@AndroidEntryPoint(Fragment::class)
class RawFhirFragment : Hilt_RawFhirFragment() {

    companion object {
        const val MEDICAL_RESOURCE_ID_KEY = "entry_id_key"
    }

    @Inject lateinit var logger: HealthConnectLogger
    private val viewModel: RawFhirViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var medicalResourceId: MedicalResourceId
    private lateinit var loadingView: View
    private lateinit var errorView: View
    private lateinit var detailsAdapter: RecyclerViewAdapter
    private val rawFhirViewBinder by lazy { RawFhirViewBinder() }

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
        logger.setPageId(PageName.RAW_FHIR_PAGE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry_details, container, false)
        medicalResourceId =
            requireArguments().getParcelable(MEDICAL_RESOURCE_ID_KEY)
                ?: throw IllegalArgumentException("MEDICAL_RESOURCE_ID_KEY can't be null!")
        errorView = view.findViewById(R.id.error_view)
        loadingView = view.findViewById(R.id.loading)
        detailsAdapter =
            RecyclerViewAdapter.Builder()
                .setViewBinder(FormattedFhir::class.java, rawFhirViewBinder)
                .build()
        recyclerView =
            view.findViewById<RecyclerView?>(R.id.data_entries_list).apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = detailsAdapter
            }
        viewModel.loadFhirResource(medicalResourceId)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.rawFhir.observe(viewLifecycleOwner) { state -> updateUI(state) }
    }

    private fun updateUI(state: RawFhirViewModel.RawFhirState) {
        when (state) {
            is Loading -> {
                loadingView.isVisible = true
                errorView.isVisible = false
                recyclerView.isVisible = false
            }
            is Error -> {
                errorView.isVisible = true
                loadingView.isVisible = false
                recyclerView.isVisible = false
            }
            is WithData -> {
                recyclerView.isVisible = true
                detailsAdapter.updateData(state.fhirResource)
                errorView.isVisible = false
                loadingView.isVisible = false
            }
        }
    }

    class RawFhirViewBinder : SimpleViewBinder<FormattedFhir, View> {
        private lateinit var logger: HealthConnectLogger

        override fun newView(parent: ViewGroup): View {
            val hiltEntryPoint =
                EntryPointAccessors.fromApplication(
                    parent.context.applicationContext,
                    HealthConnectLoggerEntryPoint::class.java,
                )
            logger = hiltEntryPoint.logger()
            return LayoutInflater.from(parent.context)
                .inflate(R.layout.item_raw_fhir_entry, parent, false)
        }

        override fun bind(view: View, data: FormattedFhir, index: Int) {
            val rawFhir = view.findViewById<TextView>(R.id.item_raw_fhir)
            rawFhir.text = data.fhir
            rawFhir.contentDescription = data.fhirContentDescription
            logger.logImpression(RawFhirPageElement.RAW_FHIR_RESOURCE)
        }
    }
}
